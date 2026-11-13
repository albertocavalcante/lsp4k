package io.lsp4k.server

import io.jsonrpc4k.core.Connection
import io.lsp4k.protocol.InitializeParams
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.SetTraceParams
import io.lsp4k.protocol.TraceValue
import io.lsp4k.protocol.WorkDoneProgressCancelParams
import io.lsp4k.server.transport.SessionLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.JsonElement
import kotlin.concurrent.Volatile

/**
 * A running language server instance.
 *
 * Created by [LanguageServerConfig.start]. It runs the LSP lifecycle state machine ([state]) and dispatches
 * inbound messages to the configured handlers with bounded concurrency: requests run concurrently,
 * notifications run in wire order after every earlier request finished, and a cancelled request is answered
 * with `RequestCancelled` (-32800):
 *
 * - Handlers may await client requests. The `initialized` hook additionally runs outside the dispatch queue,
 *   so a long running hook does not delay later messages.
 * - Before a successful `initialize`, requests fail with `ServerNotInitialized` (-32002) and notifications other
 *   than `exit` are dropped. A failed `initialize` may be retried; a second successful one is rejected with
 *   `InvalidRequest` (-32600).
 * - After `shutdown`, requests (including another `shutdown`) fail with `InvalidRequest` and notifications other
 *   than `exit` are dropped.
 * - `exit` is accepted in every state. It flushes pending output, records [exitCode] (0 after `shutdown`,
 *   otherwise 1), runs the exit hook and closes the session; the application decides whether to end its process.
 */
public class LanguageServer internal constructor(
    private val config: LanguageServerConfig,
    private val connection: Connection,
    private val session: SessionLifecycle,
) {
    private val lifecycle = ServerLifecycle()
    private val progress = ProgressRegistry()
    private val sender = ClientSender(connection)

    /** The lifecycle state; see [ServerState]. */
    public val state: StateFlow<ServerState> = lifecycle.state

    /** The trace level from `initialize` or the latest `$/setTrace`; [LanguageClient.logTrace] follows it. */
    @Volatile
    public var traceLevel: TraceValue = TraceValue.Off
        private set

    /** The parameters of the successful `initialize` request, including the client capabilities; null before it. */
    public val initializeParams: InitializeParams? get() = lifecycle.initializeParams

    /**
     * The exit code the client asked for: 0 when `exit` followed `shutdown`, 1 when it did not, and null while no
     * `exit` arrived (for example after end of input or [close]). Read it after [awaitTermination].
     */
    public val exitCode: Int? get() = lifecycle.exitCode

    /** Proxy for requests and notifications sent to the client. */
    public val client: LanguageClient = LanguageClient(sender, progress) { traceLevel }

    internal suspend fun setup() {
        connection.onRequest(LspMethods.INITIALIZE) { params -> initialize(params) }
        connection.onNotification(LspMethods.INITIALIZED) { _ -> initialized() }
        connection.onRequest(LspMethods.SHUTDOWN) { _ -> shutdown() }
        connection.onNotification(LspMethods.EXIT) { _ -> exit() }
        connection.onNotification(LspMethods.SET_TRACE) { params -> setTrace(params) }
        connection.onNotification(LspMethods.WINDOW_WORK_DONE_PROGRESS_CANCEL) { params -> cancelProgress(params) }
        config.requestHandlers.forEach { (method, handler) ->
            connection.onRequest(method) { params -> dispatchRequest(method, handler, params) }
        }
        config.notificationHandlers.forEach { (method, handler) ->
            if (method != LspMethods.WINDOW_WORK_DONE_PROGRESS_CANCEL) {
                connection.onNotification(method) { params -> dispatchNotification(handler, params) }
            }
        }
    }

    /** Stops immediately, canceling pending work and discarding unsent frames; the transport remains caller-owned. */
    public fun close(): Unit = session.close()

    /**
     * Waits for session jobs to stop. Returns the first read or write failure, or null on EOF,
     * explicit close, graceful exit, or parent cancellation. The transport remains caller-owned
     * and its suspending operations must cooperate with coroutine cancellation. If a borrowed
     * transport blocks in a non-interruptible read, its owner may need to close it to unblock.
     */
    public suspend fun awaitTermination(): Throwable? = session.awaitTermination()

    private suspend fun initialize(params: JsonElement?): JsonElement =
        lifecycle.initialize {
            val decoded = decodeParams(LspMethods.INITIALIZE, InitializeParams.serializer(), params)
            // The requested trace level applies as soon as the request is decoded, so the hook can already log with
            // it and it stays after a failed attempt; an omitted level defaults to off, including on a retry.
            traceLevel = decoded.trace ?: TraceValue.Off
            val hook = config.lifecycle.initialize
            val result =
                if (hook == null) {
                    InitializeResult(capabilities = config.capabilities, serverInfo = config.serverInfo)
                } else {
                    val context = requestContext(LspMethods.INITIALIZE, params)
                    runHandler(context) { context.hook(decoded) }
                }
            val encoded = wireJson.encodeToJsonElement(InitializeResult.serializer(), result)
            lifecycle.initializeParams = decoded
            encoded
        }

    private suspend fun initialized() {
        if (!lifecycle.markInitialized()) return
        val hook = config.lifecycle.initialized ?: return
        val context = HandlerContext(this)
        // Off the receive loop, so the hook can await client requests such as registerCapability; it starts
        // inline and only continues in the session scope once it suspends. A failure ends the session.
        session.launchUndispatched {
            try {
                context.hook()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                session.fail(failure)
            }
        }
    }

    private suspend fun shutdown(): JsonElement? {
        lifecycle.shutdown()
        val hook = config.lifecycle.shutdown ?: return null
        val context = requestContext(LspMethods.SHUTDOWN, null)
        runHandler(context) { context.hook() }
        return null
    }

    private suspend fun exit() {
        val code = lifecycle.exit()
        try {
            connection.flushOutgoing()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            session.fail(failure.cause ?: failure)
            throw failure
        } finally {
            try {
                config.lifecycle.exit?.invoke(code)
            } finally {
                session.close()
            }
        }
    }

    private suspend fun setTrace(params: JsonElement?) {
        if (!lifecycle.isServing) return
        val decoded = decodeParams(LspMethods.SET_TRACE, SetTraceParams.serializer(), params)
        traceLevel = decoded.value
        val hook = config.lifecycle.setTrace ?: return
        val context = HandlerContext(this)
        context.hook(decoded)
    }

    private suspend fun cancelProgress(params: JsonElement?) {
        if (!lifecycle.isServing) return
        val method = LspMethods.WINDOW_WORK_DONE_PROGRESS_CANCEL
        progress.cancel(decodeParams(method, WorkDoneProgressCancelParams.serializer(), params).token)
        val handler = config.notificationHandlers[method] ?: return
        val context = HandlerContext(this)
        handler.handle(context, params)
    }

    private suspend fun dispatchRequest(
        method: String,
        handler: ServerRequestHandler,
        params: JsonElement?,
    ): JsonElement? {
        lifecycle.requireServing(method)
        val context = requestContext(method, params)
        return runHandler(context) { handler.handle(context, params) }
    }

    private suspend fun dispatchNotification(
        handler: ServerNotificationHandler,
        params: JsonElement?,
    ) {
        if (!lifecycle.isServing) return
        val context = HandlerContext(this)
        handler.handle(context, params)
    }

    private fun requestContext(
        method: String,
        params: JsonElement?,
    ): RequestContext {
        val workDone = progressTokenOf(params, WORK_DONE_TOKEN)?.let { WorkDoneProgress(it, sender, progress) }
        return RequestContext(this, method, workDone)
    }

    /** Runs a request handler and ends its request-bound work done progress if the handler left it running. */
    private suspend fun <T> runHandler(
        context: RequestContext,
        block: suspend () -> T,
    ): T =
        try {
            block()
        } finally {
            if (currentCoroutineContext().isActive) context.workDoneProgress?.endIfRunning()
        }
}
