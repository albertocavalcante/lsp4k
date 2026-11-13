package io.lsp4k.server

import io.lsp4k.protocol.ApplyWorkspaceEditParams
import io.lsp4k.protocol.ApplyWorkspaceEditResult
import io.lsp4k.protocol.ConfigurationParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.TextDocumentContentRefreshParams
import io.lsp4k.protocol.WorkspaceFolder
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import kotlin.time.Duration

/**
 * Workspace requests sent to the client, including the refresh requests that ask the client to re-request a
 * feature for all open documents.
 */
public class WorkspaceCalls internal constructor(
    private val sender: ClientSender,
) {
    /**
     * Sends `workspace/applyEdit` and returns whether the client applied the edit. By default it waits as long
     * as the client needs, since applying may involve user confirmation.
     */
    public suspend fun applyEdit(
        params: ApplyWorkspaceEditParams,
        timeout: Duration = Duration.INFINITE,
    ): ApplyWorkspaceEditResult {
        val result = sender.request(LspMethods.WORKSPACE_APPLY_EDIT, params, ApplyWorkspaceEditParams.serializer(), timeout)
        return sender.decode(LspMethods.WORKSPACE_APPLY_EDIT, result, ApplyWorkspaceEditResult.serializer())
    }

    /** Sends `workspace/configuration` and returns one value per requested item, in order. */
    public suspend fun configuration(
        params: ConfigurationParams,
        timeout: Duration = DEFAULT_REQUEST_TIMEOUT,
    ): List<JsonElement> {
        val result = sender.request(LspMethods.WORKSPACE_CONFIGURATION, params, ConfigurationParams.serializer(), timeout)
        return sender.decode(LspMethods.WORKSPACE_CONFIGURATION, result, ListSerializer(JsonElement.serializer()))
    }

    /** Sends `workspace/workspaceFolders`; null means no workspace is open, an empty list means no folders. */
    public suspend fun workspaceFolders(timeout: Duration = DEFAULT_REQUEST_TIMEOUT): List<WorkspaceFolder>? {
        val result = sender.request(LspMethods.WORKSPACE_WORKSPACE_FOLDERS, null, timeout)
        return sender.decodeOrNull(result, ListSerializer(WorkspaceFolder.serializer()))
    }

    /** Sends `workspace/codeLens/refresh`. */
    public suspend fun codeLensRefresh(timeout: Duration = DEFAULT_REQUEST_TIMEOUT) {
        sender.request(LspMethods.WORKSPACE_CODE_LENS_REFRESH, null, timeout)
    }

    /** Sends `workspace/semanticTokens/refresh`. */
    public suspend fun semanticTokensRefresh(timeout: Duration = DEFAULT_REQUEST_TIMEOUT) {
        sender.request(LspMethods.WORKSPACE_SEMANTIC_TOKENS_REFRESH, null, timeout)
    }

    /** Sends `workspace/inlayHint/refresh`. */
    public suspend fun inlayHintRefresh(timeout: Duration = DEFAULT_REQUEST_TIMEOUT) {
        sender.request(LspMethods.WORKSPACE_INLAY_HINT_REFRESH, null, timeout)
    }

    /** Sends `workspace/inlineValue/refresh`. */
    public suspend fun inlineValueRefresh(timeout: Duration = DEFAULT_REQUEST_TIMEOUT) {
        sender.request(LspMethods.WORKSPACE_INLINE_VALUE_REFRESH, null, timeout)
    }

    /** Sends `workspace/diagnostic/refresh`. */
    public suspend fun diagnosticRefresh(timeout: Duration = DEFAULT_REQUEST_TIMEOUT) {
        sender.request(LspMethods.WORKSPACE_DIAGNOSTIC_REFRESH, null, timeout)
    }

    /** Sends `workspace/foldingRange/refresh`. */
    public suspend fun foldingRangeRefresh(timeout: Duration = DEFAULT_REQUEST_TIMEOUT) {
        sender.request(LspMethods.WORKSPACE_FOLDING_RANGE_REFRESH, null, timeout)
    }

    /** Sends `workspace/textDocumentContent/refresh` for one document. */
    public suspend fun textDocumentContentRefresh(
        params: TextDocumentContentRefreshParams,
        timeout: Duration = DEFAULT_REQUEST_TIMEOUT,
    ) {
        sender.request(
            LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT_REFRESH,
            params,
            TextDocumentContentRefreshParams.serializer(),
            timeout,
        )
    }
}
