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
 * The expected methods come from [MetaModelMethods], generated from the meta model. `initialize`, `initialized`,
 * `shutdown`, `exit` and `$/setTrace` are lifecycle hooks; `$/cancelRequest` is consumed by the jsonrpc4k
 * connection. Every other method must appear in the registered handler maps.
 */
class ServerApiCoverageTest {
    private val lifecycleRequests = setOf(LspMethods.INITIALIZE, LspMethods.SHUTDOWN)
    private val lifecycleNotifications =
        setOf(LspMethods.INITIALIZED, LspMethods.EXIT, LspMethods.SET_TRACE, "\$/cancelRequest")
    private val requests = MetaModelMethods.requests - lifecycleRequests
    private val notifications = MetaModelMethods.notifications - lifecycleNotifications

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
