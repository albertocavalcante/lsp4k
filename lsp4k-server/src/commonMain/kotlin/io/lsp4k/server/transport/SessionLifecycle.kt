package io.lsp4k.server.transport

import io.jsonrpc4k.core.BoundedDispatchPolicy
import io.jsonrpc4k.core.Connection
import io.jsonrpc4k.core.InboundResourceLimits
import io.jsonrpc4k.transport.Transport
import io.lsp4k.server.SERVER_CONTROL_NOTIFICATIONS
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

/**
 * The least time the session waits for queued frames to be written after the end of input. Without it a zero drain
 * timeout would end the session before the responses already queued reach the transport.
 */
private val MIN_FLUSH_TIMEOUT = 1.seconds

/** Owns session jobs; the supplied scope and transport remain caller-owned. */
internal class SessionLifecycle(
    parent: CoroutineScope,
    policy: BoundedDispatchPolicy,
    limits: InboundResourceLimits?,
) {
    private data class End(
        val cause: Throwable?,
    )

    private val job = SupervisorJob(parent.coroutineContext[Job])
    private val scope = CoroutineScope(parent.coroutineContext + job)
    private val sessionPolicy = policy.copy(controlNotifications = policy.controlNotifications + SERVER_CONTROL_NOTIFICATIONS)
    val connection =
        if (limits == null) {
            Connection(policy = sessionPolicy, scope = scope)
        } else {
            Connection(policy = sessionPolicy, limits = limits, scope = scope)
        }
    private val end = MutableStateFlow<End?>(null)
    private val terminated = CompletableDeferred<Throwable?>()

    init {
        job.invokeOnCompletion {
            connection.close()
            terminated.complete(end.value?.cause)
        }
    }

    fun start(transport: Transport) {
        scope.launch {
            runPump {
                transport.incoming.collect { connection.receive(it) }
                drainAfterEndOfInput()
            }
        }
        scope.launch {
            runPump { connection.outgoing.collect { transport.send(it) } }
        }
    }

    /** Runs [block] in the session scope, inline until its first suspension; it is cancelled with the session. */
    fun launchUndispatched(block: suspend () -> Unit) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { block() }
    }

    /**
     * Lets the work admitted before the end of input finish and its responses reach the transport before the session
     * ends: [Connection.endInput] waits for admitted requests and notifications, bounded by the policy's drain timeout,
     * and [Connection.flushOutgoing] waits until the outgoing pump has written every queued frame. The flush gets its
     * own bound, the drain timeout but at least [MIN_FLUSH_TIMEOUT], so a transport whose send blocks (a half-closed
     * peer that stopped reading) cannot keep the session alive, and a zero drain timeout still writes the frames that
     * were queued when [Connection.endInput] returned; after the bound the session ends and the unsent frames are
     * dropped.
     */
    private suspend fun drainAfterEndOfInput() {
        connection.endInput()
        withTimeoutOrNull(sessionPolicy.drainTimeout.coerceAtLeast(MIN_FLUSH_TIMEOUT)) { connection.flushOutgoing() }
    }

    private suspend fun runPump(block: suspend () -> Unit) {
        try {
            block()
            finish(null)
        } catch (cancelled: CancellationException) {
            if (currentCoroutineContext().isActive) finish(cancelled) else throw cancelled
        } catch (failure: Exception) {
            finish(failure)
        } catch (fatal: Throwable) {
            finish(fatal)
            throw fatal
        }
    }

    fun close() = finish(null)

    fun fail(cause: Throwable) {
        if (job.isActive) finish(cause)
    }

    suspend fun awaitTermination(): Throwable? = terminated.await()

    private fun finish(cause: Throwable?) {
        if (end.compareAndSet(null, End(cause))) {
            connection.close()
            job.cancel()
        }
    }
}
