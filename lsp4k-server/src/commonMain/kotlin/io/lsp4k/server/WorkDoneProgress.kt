package io.lsp4k.server

import io.lsp4k.protocol.ProgressToken
import io.lsp4k.protocol.WorkDoneProgressBegin
import io.lsp4k.protocol.WorkDoneProgressEnd
import io.lsp4k.protocol.WorkDoneProgressReport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MAX_PERCENTAGE = 100

private enum class Phase {
    Created,
    Running,
    Ended,
}

/**
 * One work done progress operation reported to the client with `$/progress`: a single [begin], any number of
 * [report] calls, then a single [end]. Calls out of that order throw [IllegalStateException].
 *
 * Obtain one from [WindowCalls.createWorkDoneProgress] for server initiated progress, or from
 * [RequestContext.workDoneProgress] for the `workDoneToken` of a request. When the client sends
 * `window/workDoneProgress/cancel` for [token] while the operation runs, [isCancelled] becomes true and
 * [awaitCancelled] returns; the operation should then stop and call [end].
 *
 * Limit: the receive loop handles one inbound message at a time, so while a request handler is running it
 * does not read the client's cancel notification. For the progress of a request handler, [isCancelled] therefore
 * flips only after the handler returned; use server initiated progress, reported from your own coroutine scope,
 * for work that must observe cancellation while it runs.
 *
 * @property token The progress token.
 */
public class WorkDoneProgress internal constructor(
    public val token: ProgressToken,
    private val sender: ClientSender,
    private val registry: ProgressRegistry,
) {
    private val mutex = Mutex()
    private var phase = Phase.Created
    private val cancellation = CompletableDeferred<Unit>()

    /** Whether the client asked to cancel this operation. */
    public val isCancelled: Boolean get() = cancellation.isCompleted

    /** Suspends until the client asks to cancel this operation. */
    public suspend fun awaitCancelled() {
        cancellation.await()
    }

    /**
     * Starts the operation.
     *
     * @param title Short title of the operation, for example "Indexing".
     * @param cancellable Whether the client should offer a cancel button.
     * @param message Optional detail, for example "3/25 files".
     * @param percentage Optional completion percentage from 0 to 100.
     */
    public suspend fun begin(
        title: String,
        cancellable: Boolean? = null,
        message: String? = null,
        percentage: Int? = null,
    ) {
        requirePercentage(percentage)
        mutex.withLock {
            check(phase == Phase.Created) { "Work done progress $token already began" }
            val value = WorkDoneProgressBegin(title = title, cancellable = cancellable, message = message, percentage = percentage)
            // Register before sending so a cancel that follows at once finds the operation; undo if the send fails.
            registry.add(this)
            var sent = false
            try {
                sender.progress(token, value, WorkDoneProgressBegin.serializer())
                sent = true
            } finally {
                if (!sent) registry.remove(token)
            }
            phase = Phase.Running
        }
    }

    /** Reports intermediate progress; null arguments leave the client's current values unchanged. */
    public suspend fun report(
        message: String? = null,
        percentage: Int? = null,
        cancellable: Boolean? = null,
    ) {
        requirePercentage(percentage)
        mutex.withLock {
            check(phase == Phase.Running) { "Work done progress $token is not running" }
            val value = WorkDoneProgressReport(cancellable = cancellable, message = message, percentage = percentage)
            sender.progress(token, value, WorkDoneProgressReport.serializer())
        }
    }

    /** Ends the operation with an optional final [message]. */
    public suspend fun end(message: String? = null) {
        check(endIfRunning(message)) { "Work done progress $token is not running" }
    }

    /** Ends the operation if it is running and reports whether it did. */
    internal suspend fun endIfRunning(message: String? = null): Boolean =
        mutex.withLock {
            val running = phase == Phase.Running
            if (running) {
                phase = Phase.Ended
                registry.remove(token)
                sender.progress(token, WorkDoneProgressEnd(message = message), WorkDoneProgressEnd.serializer())
            }
            running
        }

    internal fun markCancelled() {
        cancellation.complete(Unit)
    }
}

/** Tracks running work done progress so `window/workDoneProgress/cancel` can reach it. */
internal class ProgressRegistry {
    private val mutex = Mutex()
    private val running = mutableMapOf<ProgressToken, WorkDoneProgress>()

    suspend fun add(progress: WorkDoneProgress) {
        mutex.withLock { running[progress.token] = progress }
    }

    suspend fun remove(token: ProgressToken) {
        mutex.withLock { running.remove(token) }
    }

    /** Marks the running progress for [token] as cancelled; returns false when none is running. */
    suspend fun cancel(token: ProgressToken): Boolean {
        val progress = mutex.withLock { running[token] } ?: return false
        progress.markCancelled()
        return true
    }
}

private fun requirePercentage(percentage: Int?) {
    require(percentage == null || percentage in 0..MAX_PERCENTAGE) { "Percentage must be between 0 and 100: $percentage" }
}
