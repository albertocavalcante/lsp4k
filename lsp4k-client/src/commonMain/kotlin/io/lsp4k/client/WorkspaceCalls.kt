package io.lsp4k.client

import io.lsp4k.protocol.CreateFilesParams
import io.lsp4k.protocol.DeleteFilesParams
import io.lsp4k.protocol.DidChangeConfigurationParams
import io.lsp4k.protocol.DidChangeWatchedFilesParams
import io.lsp4k.protocol.DidChangeWorkspaceFoldersParams
import io.lsp4k.protocol.ExecuteCommandParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.RenameFilesParams
import io.lsp4k.protocol.TextDocumentContentParams
import io.lsp4k.protocol.TextDocumentContentResult
import io.lsp4k.protocol.WorkspaceDiagnosticParams
import io.lsp4k.protocol.WorkspaceDiagnosticReport
import io.lsp4k.protocol.WorkspaceEdit
import io.lsp4k.protocol.WorkspaceSymbol
import io.lsp4k.protocol.WorkspaceSymbolParams
import io.lsp4k.protocol.WorkspaceSymbolResult
import kotlinx.serialization.json.JsonElement

/**
 * Workspace requests and notifications (the `workspace` namespace).
 */
public class WorkspaceCalls internal constructor(
    private val caller: ServerCaller,
) {
    /**
     * Request workspace symbols matching a query.
     */
    public suspend fun symbol(params: WorkspaceSymbolParams): WorkspaceSymbolResult? =
        caller.request(LspMethods.WORKSPACE_SYMBOL, params)

    /**
     * Resolve additional information for a workspace symbol.
     */
    public suspend fun workspaceSymbolResolve(symbol: WorkspaceSymbol): WorkspaceSymbol? =
        caller.request(LspMethods.WORKSPACE_SYMBOL_RESOLVE, symbol)

    /**
     * Request the server to execute a command. The result is arbitrary JSON.
     */
    public suspend fun executeCommand(params: ExecuteCommandParams): JsonElement? =
        caller.requestRaw(LspMethods.WORKSPACE_EXECUTE_COMMAND, params)

    /**
     * Request workspace diagnostics.
     * @since 3.17.0
     */
    public suspend fun workspaceDiagnostic(params: WorkspaceDiagnosticParams): WorkspaceDiagnosticReport? =
        caller.request(LspMethods.WORKSPACE_DIAGNOSTIC, params)

    /**
     * Request the content of a text document owned by the server (`workspace/textDocumentContent`).
     * @since 3.18.0
     */
    public suspend fun textDocumentContent(params: TextDocumentContentParams): TextDocumentContentResult? =
        caller.request(LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT, params)

    /**
     * Notify the server that the configuration has changed.
     */
    public suspend fun didChangeConfiguration(params: DidChangeConfigurationParams): Unit =
        caller.notify(LspMethods.WORKSPACE_DID_CHANGE_CONFIGURATION, params)

    /**
     * Notify the server that watched files have changed.
     */
    public suspend fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit =
        caller.notify(LspMethods.WORKSPACE_DID_CHANGE_WATCHED_FILES, params)

    /**
     * Notify the server that workspace folders have changed.
     */
    public suspend fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams): Unit =
        caller.notify(LspMethods.WORKSPACE_DID_CHANGE_WORKSPACE_FOLDERS, params)

    /**
     * Request workspace edits before files are created.
     */
    public suspend fun willCreateFiles(params: CreateFilesParams): WorkspaceEdit? =
        caller.request(LspMethods.WORKSPACE_WILL_CREATE_FILES, params)

    /**
     * Notify the server that files were created.
     */
    public suspend fun didCreateFiles(params: CreateFilesParams): Unit =
        caller.notify(LspMethods.WORKSPACE_DID_CREATE_FILES, params)

    /**
     * Request workspace edits before files are renamed.
     */
    public suspend fun willRenameFiles(params: RenameFilesParams): WorkspaceEdit? =
        caller.request(LspMethods.WORKSPACE_WILL_RENAME_FILES, params)

    /**
     * Notify the server that files were renamed.
     */
    public suspend fun didRenameFiles(params: RenameFilesParams): Unit =
        caller.notify(LspMethods.WORKSPACE_DID_RENAME_FILES, params)

    /**
     * Request workspace edits before files are deleted.
     */
    public suspend fun willDeleteFiles(params: DeleteFilesParams): WorkspaceEdit? =
        caller.request(LspMethods.WORKSPACE_WILL_DELETE_FILES, params)

    /**
     * Notify the server that files were deleted.
     */
    public suspend fun didDeleteFiles(params: DeleteFilesParams): Unit =
        caller.notify(LspMethods.WORKSPACE_DID_DELETE_FILES, params)
}
