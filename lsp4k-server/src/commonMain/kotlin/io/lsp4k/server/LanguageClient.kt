package io.lsp4k.server

import io.lsp4k.protocol.CancelParams
import io.lsp4k.protocol.LogTraceParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.ProgressParams
import io.lsp4k.protocol.ProgressToken
import io.lsp4k.protocol.TraceValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

/**
 * Proxy for the requests and notifications a server sends to its client, grouped by namespace:
 *
 * - [window]: `window/showMessage`, `window/showMessageRequest`, `window/logMessage`, `window/showDocument` and
 *   `window/workDoneProgress/create`.
 * - [workspace]: `workspace/applyEdit`, `workspace/configuration`, `workspace/workspaceFolders` and every refresh
 *   request, including `workspace/textDocumentContent/refresh`.
 * - [textDocument]: `textDocument/publishDiagnostics`.
 * - [registration]: `client/registerCapability` and `client/unregisterCapability`.
 *
 * `telemetry/event`, `$/logTrace`, `$/progress` and `$/cancelRequest` are members of this class.
 *
 * Requests suspend until the client answers. They may be awaited inside a handler: responses bypass the
 * dispatch queue. A request whose caller is cancelled or times out is cancelled on the client with `$/cancelRequest`.
 */
public class LanguageClient internal constructor(
    private val sender: ClientSender,
    progress: ProgressRegistry,
    private val traceLevel: () -> TraceValue,
) {
    /** Window requests and notifications. */
    public val window: WindowCalls = WindowCalls(sender, progress)

    /** Workspace requests, including the refresh requests. */
    public val workspace: WorkspaceCalls = WorkspaceCalls(sender)

    /** Text document notifications. */
    public val textDocument: TextDocumentCalls = TextDocumentCalls(sender)

    /** Dynamic capability registration. */
    public val registration: RegistrationCalls = RegistrationCalls(sender)

    /** Sends a `telemetry/event` notification with arbitrary [data]. */
    public suspend fun telemetryEvent(data: JsonElement) {
        sender.notify(LspMethods.TELEMETRY_EVENT, data)
    }

    /**
     * Sends `$/logTrace` according to [LanguageServer.traceLevel]: nothing when it is `off`, [params] without its
     * `verbose` text when it is `messages`, and [params] unchanged when it is `verbose`.
     *
     * @return whether a notification was sent.
     */
    public suspend fun logTrace(params: LogTraceParams): Boolean {
        val level = traceLevel()
        if (level == TraceValue.Off) return false
        val sent = if (level == TraceValue.Verbose) params else params.copy(verbose = null)
        sender.notify(LspMethods.LOG_TRACE, sent, LogTraceParams.serializer())
        return true
    }

    /**
     * Sends `$/logTrace` with [message] when tracing is on; [verbose] is evaluated only at the `verbose` level.
     *
     * @return whether a notification was sent.
     */
    public suspend fun logTrace(
        message: String,
        verbose: (() -> String)? = null,
    ): Boolean {
        val level = traceLevel()
        if (level == TraceValue.Off) return false
        val detail = if (level == TraceValue.Verbose) verbose?.invoke() else null
        sender.notify(LspMethods.LOG_TRACE, LogTraceParams(message, detail), LogTraceParams.serializer())
        return true
    }

    /** Sends a raw `$/progress` notification. */
    public suspend fun progress(params: ProgressParams<JsonElement>) {
        sender.notify(LspMethods.PROGRESS, params, ProgressParams.serializer(JsonElement.serializer()))
    }

    /** Sends a `$/progress` notification for [token] with [value] encoded by [serializer]. */
    public suspend fun <T> progress(
        token: ProgressToken,
        value: T,
        serializer: KSerializer<T>,
    ) {
        sender.progress(token, value, serializer)
    }

    /**
     * Sends `$/cancelRequest` for a request this server sent. jsonrpc4k does not expose the ids it assigns, so
     * this is the raw notification for callers that know the id.
     */
    public suspend fun cancelRequest(params: CancelParams) {
        sender.notify(LspMethods.CANCEL_REQUEST, params, CancelParams.serializer())
    }
}
