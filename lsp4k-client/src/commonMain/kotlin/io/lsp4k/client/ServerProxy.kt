package io.lsp4k.client

import io.jsonrpc4k.core.Connection
import io.lsp4k.protocol.CancelParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.ProgressParams
import io.lsp4k.protocol.SetTraceParams
import kotlinx.serialization.json.JsonElement

/**
 * Proxy for sending requests and notifications to the language server.
 *
 * Calls are grouped by LSP namespace so each group stays small and discoverable:
 *
 * | Property | Methods |
 * |---|---|
 * | [textDocument] | document synchronization, completion, hover, navigation |
 * | [editing] | code actions, lenses, links, colors, formatting, rename, ranges |
 * | [insights] | semantic tokens, inlay hints, diagnostics, monikers, inline values and completion |
 * | [hierarchy] | call hierarchy and type hierarchy |
 * | [workspace] | workspace symbols, commands, diagnostics, file operations, folders, content |
 * | [notebook] | notebook document synchronization |
 * | [window] | `window/workDoneProgress/cancel` |
 *
 * `initialize`, `initialized`, `shutdown` and `exit` belong to [LanguageClientSession] because they
 * drive the session lifecycle.
 */
public class ServerProxy internal constructor(
    connection: Connection,
) {
    private val caller = ServerCaller(connection)

    /** Text document synchronization, completion, hover and navigation requests. */
    public val textDocument: TextDocumentCalls = TextDocumentCalls(caller)

    /** Code action, lens, link, color, formatting, rename and range requests. */
    public val editing: EditingCalls = EditingCalls(caller)

    /** Semantic tokens, inlay hints, diagnostics, monikers, inline values and inline completion. */
    public val insights: InsightCalls = InsightCalls(caller)

    /** Call hierarchy and type hierarchy requests. */
    public val hierarchy: HierarchyCalls = HierarchyCalls(caller)

    /** Workspace requests and notifications. */
    public val workspace: WorkspaceCalls = WorkspaceCalls(caller)

    /** Notebook document synchronization notifications. */
    public val notebook: NotebookCalls = NotebookCalls(caller)

    /** Window notifications sent to the server. */
    public val window: WindowCalls = WindowCalls(caller)

    /**
     * Set the trace level on the server (`$/setTrace`).
     */
    public suspend fun setTrace(params: SetTraceParams): Unit = caller.notify(LspMethods.SET_TRACE, params)

    /**
     * Report progress to the server (`$/progress`), for example partial results or work done progress
     * for a token the server created.
     */
    public suspend fun progress(params: ProgressParams<JsonElement>): Unit = caller.notify(LspMethods.PROGRESS, params)

    /**
     * Ask the server to cancel the request with the given id (`$/cancelRequest`).
     *
     * The jsonrpc4k connection does not expose the ids it assigns, so this is the raw notification
     * for callers that track ids themselves. The inbound direction (the server cancelling a client
     * request) is consumed by the jsonrpc4k dispatcher and has no handler hook.
     */
    public suspend fun cancelRequest(params: CancelParams): Unit = caller.notify(LspMethods.CANCEL_REQUEST, params)
}
