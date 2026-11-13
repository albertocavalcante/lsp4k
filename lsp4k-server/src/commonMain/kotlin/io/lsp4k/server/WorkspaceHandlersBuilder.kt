package io.lsp4k.server

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
import io.lsp4k.protocol.WorkspaceDiagnosticReportPartialResult
import io.lsp4k.protocol.WorkspaceEdit
import io.lsp4k.protocol.WorkspaceSymbol
import io.lsp4k.protocol.WorkspaceSymbolParams
import io.lsp4k.protocol.WorkspaceSymbolResult
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/**
 * Handlers for workspace symbols, commands, workspace diagnostics, text document content, configuration,
 * watched files, workspace folders and file operations.
 */
@LspServerDsl
public class WorkspaceHandlersBuilder internal constructor(
    registry: HandlerRegistry,
) : HandlersBuilder(registry) {
    /** Handles `workspace/symbol` requests; partial results use the same two shapes as the result. */
    public fun symbol(handler: suspend PartialResultContext<WorkspaceSymbolResult>.(WorkspaceSymbolParams) -> WorkspaceSymbolResult?) {
        partialRequest(LspMethods.WORKSPACE_SYMBOL, serializer<WorkspaceSymbolResult>(), handler)
    }

    /** Handles `workspaceSymbol/resolve` requests. */
    public fun workspaceSymbolResolve(handler: suspend RequestContext.(WorkspaceSymbol) -> WorkspaceSymbol) {
        request(LspMethods.WORKSPACE_SYMBOL_RESOLVE, handler)
    }

    /** Handles `workspace/executeCommand` requests; the result is any JSON value or null. */
    public fun executeCommand(handler: suspend RequestContext.(ExecuteCommandParams) -> JsonElement?) {
        request(LspMethods.WORKSPACE_EXECUTE_COMMAND, handler)
    }

    /** Handles `workspace/diagnostic` requests. */
    public fun workspaceDiagnostic(
        handler: suspend PartialResultContext<WorkspaceDiagnosticReportPartialResult>.(
            WorkspaceDiagnosticParams,
        ) -> WorkspaceDiagnosticReport,
    ) {
        partialRequest(LspMethods.WORKSPACE_DIAGNOSTIC, WorkspaceDiagnosticReportPartialResult.serializer(), handler)
    }

    /** Handles `workspace/textDocumentContent` requests for the schemes the server registered. */
    public fun textDocumentContent(handler: suspend RequestContext.(TextDocumentContentParams) -> TextDocumentContentResult) {
        request(LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT, handler)
    }

    /** Handles `workspace/didChangeConfiguration` notifications. */
    public fun didChangeConfiguration(handler: suspend HandlerContext.(DidChangeConfigurationParams) -> Unit) {
        notification(LspMethods.WORKSPACE_DID_CHANGE_CONFIGURATION, handler)
    }

    /** Handles `workspace/didChangeWatchedFiles` notifications. */
    public fun didChangeWatchedFiles(handler: suspend HandlerContext.(DidChangeWatchedFilesParams) -> Unit) {
        notification(LspMethods.WORKSPACE_DID_CHANGE_WATCHED_FILES, handler)
    }

    /** Handles `workspace/didChangeWorkspaceFolders` notifications. */
    public fun didChangeWorkspaceFolders(handler: suspend HandlerContext.(DidChangeWorkspaceFoldersParams) -> Unit) {
        notification(LspMethods.WORKSPACE_DID_CHANGE_WORKSPACE_FOLDERS, handler)
    }

    /** Handles `workspace/willCreateFiles` requests. */
    public fun willCreateFiles(handler: suspend RequestContext.(CreateFilesParams) -> WorkspaceEdit?) {
        request(LspMethods.WORKSPACE_WILL_CREATE_FILES, handler)
    }

    /** Handles `workspace/didCreateFiles` notifications. */
    public fun didCreateFiles(handler: suspend HandlerContext.(CreateFilesParams) -> Unit) {
        notification(LspMethods.WORKSPACE_DID_CREATE_FILES, handler)
    }

    /** Handles `workspace/willRenameFiles` requests. */
    public fun willRenameFiles(handler: suspend RequestContext.(RenameFilesParams) -> WorkspaceEdit?) {
        request(LspMethods.WORKSPACE_WILL_RENAME_FILES, handler)
    }

    /** Handles `workspace/didRenameFiles` notifications. */
    public fun didRenameFiles(handler: suspend HandlerContext.(RenameFilesParams) -> Unit) {
        notification(LspMethods.WORKSPACE_DID_RENAME_FILES, handler)
    }

    /** Handles `workspace/willDeleteFiles` requests. */
    public fun willDeleteFiles(handler: suspend RequestContext.(DeleteFilesParams) -> WorkspaceEdit?) {
        request(LspMethods.WORKSPACE_WILL_DELETE_FILES, handler)
    }

    /** Handles `workspace/didDeleteFiles` notifications. */
    public fun didDeleteFiles(handler: suspend HandlerContext.(DeleteFilesParams) -> Unit) {
        notification(LspMethods.WORKSPACE_DID_DELETE_FILES, handler)
    }
}
