package io.lsp4k.server

import io.lsp4k.protocol.DocumentDiagnosticReport
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.RelatedFullDocumentDiagnosticReport
import io.lsp4k.protocol.ServerCapabilities
import io.lsp4k.protocol.TextDocumentContentResult
import io.lsp4k.protocol.WorkspaceDiagnosticReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * G2.2 server side: every client-to-server request and notification of the LSP 3.18 meta model has a handler hook.
 *
 * `initialize`, `initialized`, `shutdown`, `exit` and `$/setTrace` are lifecycle hooks; `$/cancelRequest` is
 * consumed by the jsonrpc4k connection. Every other method must appear in the registered handler maps.
 */
class ServerApiCoverageTest {
    private val requests =
        setOf(
            LspMethods.CALL_HIERARCHY_INCOMING_CALLS,
            LspMethods.CALL_HIERARCHY_OUTGOING_CALLS,
            LspMethods.CODE_ACTION_RESOLVE,
            LspMethods.CODE_LENS_RESOLVE,
            LspMethods.COMPLETION_ITEM_RESOLVE,
            LspMethods.DOCUMENT_LINK_RESOLVE,
            LspMethods.INLAY_HINT_RESOLVE,
            LspMethods.TEXT_DOCUMENT_CODE_ACTION,
            LspMethods.TEXT_DOCUMENT_CODE_LENS,
            LspMethods.TEXT_DOCUMENT_COLOR_PRESENTATION,
            LspMethods.TEXT_DOCUMENT_COMPLETION,
            LspMethods.TEXT_DOCUMENT_DECLARATION,
            LspMethods.TEXT_DOCUMENT_DEFINITION,
            LspMethods.TEXT_DOCUMENT_DIAGNOSTIC,
            LspMethods.TEXT_DOCUMENT_DOCUMENT_COLOR,
            LspMethods.TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT,
            LspMethods.TEXT_DOCUMENT_DOCUMENT_LINK,
            LspMethods.TEXT_DOCUMENT_DOCUMENT_SYMBOL,
            LspMethods.TEXT_DOCUMENT_FOLDING_RANGE,
            LspMethods.TEXT_DOCUMENT_FORMATTING,
            LspMethods.TEXT_DOCUMENT_HOVER,
            LspMethods.TEXT_DOCUMENT_IMPLEMENTATION,
            LspMethods.TEXT_DOCUMENT_INLAY_HINT,
            LspMethods.TEXT_DOCUMENT_INLINE_COMPLETION,
            LspMethods.TEXT_DOCUMENT_INLINE_VALUE,
            LspMethods.TEXT_DOCUMENT_LINKED_EDITING_RANGE,
            LspMethods.TEXT_DOCUMENT_MONIKER,
            LspMethods.TEXT_DOCUMENT_ON_TYPE_FORMATTING,
            LspMethods.TEXT_DOCUMENT_PREPARE_CALL_HIERARCHY,
            LspMethods.TEXT_DOCUMENT_PREPARE_RENAME,
            LspMethods.TEXT_DOCUMENT_PREPARE_TYPE_HIERARCHY,
            LspMethods.TEXT_DOCUMENT_RANGE_FORMATTING,
            LspMethods.TEXT_DOCUMENT_RANGES_FORMATTING,
            LspMethods.TEXT_DOCUMENT_REFERENCES,
            LspMethods.TEXT_DOCUMENT_RENAME,
            LspMethods.TEXT_DOCUMENT_SELECTION_RANGE,
            LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL,
            LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL_DELTA,
            LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_RANGE,
            LspMethods.TEXT_DOCUMENT_SIGNATURE_HELP,
            LspMethods.TEXT_DOCUMENT_TYPE_DEFINITION,
            LspMethods.TEXT_DOCUMENT_WILL_SAVE_WAIT_UNTIL,
            LspMethods.TYPE_HIERARCHY_SUBTYPES,
            LspMethods.TYPE_HIERARCHY_SUPERTYPES,
            LspMethods.WORKSPACE_DIAGNOSTIC,
            LspMethods.WORKSPACE_EXECUTE_COMMAND,
            LspMethods.WORKSPACE_SYMBOL,
            LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT,
            LspMethods.WORKSPACE_WILL_CREATE_FILES,
            LspMethods.WORKSPACE_WILL_DELETE_FILES,
            LspMethods.WORKSPACE_WILL_RENAME_FILES,
            LspMethods.WORKSPACE_SYMBOL_RESOLVE,
        )

    private val notifications =
        setOf(
            LspMethods.PROGRESS,
            LspMethods.NOTEBOOK_DOCUMENT_DID_CHANGE,
            LspMethods.NOTEBOOK_DOCUMENT_DID_CLOSE,
            LspMethods.NOTEBOOK_DOCUMENT_DID_OPEN,
            LspMethods.NOTEBOOK_DOCUMENT_DID_SAVE,
            LspMethods.TEXT_DOCUMENT_DID_CHANGE,
            LspMethods.TEXT_DOCUMENT_DID_CLOSE,
            LspMethods.TEXT_DOCUMENT_DID_OPEN,
            LspMethods.TEXT_DOCUMENT_DID_SAVE,
            LspMethods.TEXT_DOCUMENT_WILL_SAVE,
            LspMethods.WINDOW_WORK_DONE_PROGRESS_CANCEL,
            LspMethods.WORKSPACE_DID_CHANGE_CONFIGURATION,
            LspMethods.WORKSPACE_DID_CHANGE_WATCHED_FILES,
            LspMethods.WORKSPACE_DID_CHANGE_WORKSPACE_FOLDERS,
            LspMethods.WORKSPACE_DID_CREATE_FILES,
            LspMethods.WORKSPACE_DID_DELETE_FILES,
            LspMethods.WORKSPACE_DID_RENAME_FILES,
        )

    @Test
    fun `every client to server method has a handler hook`() {
        val config =
            languageServer {
                onInitialize { InitializeResult(ServerCapabilities()) }
                onInitialized {}
                onShutdown {}
                onExit {}
                onSetTrace {}
                onProgress {}
                textDocument {
                    didOpen {}
                    didChange {}
                    didClose {}
                    didSave {}
                    willSave {}
                    willSaveWaitUntil { null }
                    completion { null }
                    completionResolve { it }
                    hover { null }
                    signatureHelp { null }
                    declaration { null }
                    definition { null }
                    typeDefinition { null }
                    implementation { null }
                    references { null }
                    documentHighlight { null }
                    documentSymbol { null }
                }
                editing {
                    codeAction { null }
                    codeActionResolve { it }
                    codeLens { null }
                    codeLensResolve { it }
                    documentLink { null }
                    documentLinkResolve { it }
                    documentColor { emptyList() }
                    colorPresentation { emptyList() }
                    formatting { null }
                    rangeFormatting { null }
                    rangesFormatting { null }
                    onTypeFormatting { null }
                    rename { null }
                    prepareRename { null }
                    foldingRange { null }
                    selectionRange { null }
                    linkedEditingRange { null }
                }
                insights {
                    semanticTokensFull { null }
                    semanticTokensFullDelta { null }
                    semanticTokensRange { null }
                    inlayHint { null }
                    inlayHintResolve { it }
                    diagnostic { DocumentDiagnosticReport.Full(RelatedFullDocumentDiagnosticReport(items = emptyList())) }
                    moniker { null }
                    inlineValue { null }
                    inlineCompletion { null }
                }
                hierarchy {
                    prepareCallHierarchy { null }
                    callHierarchyIncomingCalls { null }
                    callHierarchyOutgoingCalls { null }
                    prepareTypeHierarchy { null }
                    typeHierarchySupertypes { null }
                    typeHierarchySubtypes { null }
                }
                workspace {
                    symbol { null }
                    workspaceSymbolResolve { it }
                    executeCommand { null }
                    workspaceDiagnostic { WorkspaceDiagnosticReport(items = emptyList()) }
                    textDocumentContent { TextDocumentContentResult("") }
                    didChangeConfiguration {}
                    didChangeWatchedFiles {}
                    didChangeWorkspaceFolders {}
                    willCreateFiles { null }
                    didCreateFiles {}
                    willRenameFiles { null }
                    didRenameFiles {}
                    willDeleteFiles { null }
                    didDeleteFiles {}
                }
                notebook {
                    didOpen {}
                    didChange {}
                    didSave {}
                    didClose {}
                }
                window {
                    workDoneProgressCancel {}
                }
            }

        assertEquals(requests, config.requestHandlers.keys)
        assertEquals(notifications, config.notificationHandlers.keys)
        assertNotNull(config.lifecycle.initialize)
        assertNotNull(config.lifecycle.initialized)
        assertNotNull(config.lifecycle.shutdown)
        assertNotNull(config.lifecycle.exit)
        assertNotNull(config.lifecycle.setTrace)
    }

    @Test
    fun `lifecycle hooks can be registered only once`() {
        assertFailsWith<IllegalArgumentException> {
            languageServer {
                onShutdown {}
                onShutdown {}
            }
        }
    }
}
