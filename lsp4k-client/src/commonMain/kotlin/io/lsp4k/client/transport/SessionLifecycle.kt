package io.lsp4k.client.transport

import io.jsonrpc4k.core.BoundedDispatchPolicy
import io.jsonrpc4k.core.Connection
import io.jsonrpc4k.core.InboundResourceLimits
import io.jsonrpc4k.transport.Transport
import io.lsp4k.client.CLIENT_CONTROL_NOTIFICATIONS
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

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
    private val sessionPolicy = policy.copy(controlNotifications = policy.controlNotifications + CLIENT_CONTROL_NOTIFICATIONS)
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

    /**
     * Lets the work admitted before the end of input finish and its responses reach the transport before the session
     * ends: [Connection.endInput] waits for admitted requests and notifications, bounded by the policy's drain timeout,
     * and [Connection.flushOutgoing] waits until the outgoing pump has written every queued frame.
     */
    private suspend fun drainAfterEndOfInput() {
        connection.endInput()
        connection.flushOutgoing()
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
