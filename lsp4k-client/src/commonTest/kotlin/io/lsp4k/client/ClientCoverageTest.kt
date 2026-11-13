package io.lsp4k.client

import io.lsp4k.protocol.LspMethods
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the 3.18 meta model coverage: every `clientToServer` method must have a typed call and every
 * `serverToClient` method a handler hook. The lists are a snapshot of the method names in the 3.18
 * `metaModel.json`; regenerate them from the meta model when upgrading the protocol version.
 */
class ClientCoverageTest {
    @Test
    fun `every client to server method has a call or is a lifecycle method`() {
        val groups =
            listOf(
                textDocumentCases,
                editingCases,
                insightCases,
                hierarchyCases,
                workspaceCases,
                notebookCases,
                windowCases,
                protocolCases,
            )
        val covered = groups.flatten().map { it.method }
        assertEquals(covered.size, covered.toSet().size, "a method is covered twice")
        assertEquals(CLIENT_TO_SERVER.toSet() - LIFECYCLE, covered.toSet())
    }

    @Test
    fun `every server to client method has a handler hook`() {
        val config = languageClient { registerEveryHook() }
        val registered = config.notificationHandlers.keys + config.requestHandlers.keys
        assertEquals(SERVER_TO_CLIENT_WITH_HOOK.toSet(), registered)
    }

    @Test
    fun `lifecycle methods are owned by the session`() {
        assertEquals(
            setOf(LspMethods.INITIALIZE, LspMethods.SHUTDOWN, LspMethods.INITIALIZED, LspMethods.EXIT),
            LIFECYCLE,
        )
    }

    private companion object {
        val LIFECYCLE = setOf("initialize", "shutdown", "initialized", "exit")

        /** `$/cancelRequest` is both-direction but consumed by the jsonrpc4k dispatcher, so it has no hook. */
        val SERVER_TO_CLIENT_WITH_HOOK =
            listOf(
                "\$/logTrace",
                "\$/progress",
                "client/registerCapability",
                "client/unregisterCapability",
                "telemetry/event",
                "textDocument/publishDiagnostics",
                "window/logMessage",
                "window/showDocument",
                "window/showMessage",
                "window/showMessageRequest",
                "window/workDoneProgress/create",
                "workspace/applyEdit",
                "workspace/codeLens/refresh",
                "workspace/configuration",
                "workspace/diagnostic/refresh",
                "workspace/foldingRange/refresh",
                "workspace/inlayHint/refresh",
                "workspace/inlineValue/refresh",
                "workspace/semanticTokens/refresh",
                "workspace/textDocumentContent/refresh",
                "workspace/workspaceFolders",
            )

        val CLIENT_TO_SERVER =
            listOf(
                "\$/cancelRequest",
                "\$/progress",
                "\$/setTrace",
                "callHierarchy/incomingCalls",
                "callHierarchy/outgoingCalls",
                "codeAction/resolve",
                "codeLens/resolve",
                "completionItem/resolve",
                "documentLink/resolve",
                "exit",
                "initialize",
                "initialized",
                "inlayHint/resolve",
                "notebookDocument/didChange",
                "notebookDocument/didClose",
                "notebookDocument/didOpen",
                "notebookDocument/didSave",
                "shutdown",
                "textDocument/codeAction",
                "textDocument/codeLens",
                "textDocument/colorPresentation",
                "textDocument/completion",
                "textDocument/declaration",
                "textDocument/definition",
                "textDocument/diagnostic",
                "textDocument/didChange",
                "textDocument/didClose",
                "textDocument/didOpen",
                "textDocument/didSave",
                "textDocument/documentColor",
                "textDocument/documentHighlight",
                "textDocument/documentLink",
                "textDocument/documentSymbol",
                "textDocument/foldingRange",
                "textDocument/formatting",
                "textDocument/hover",
                "textDocument/implementation",
                "textDocument/inlayHint",
                "textDocument/inlineCompletion",
                "textDocument/inlineValue",
                "textDocument/linkedEditingRange",
                "textDocument/moniker",
                "textDocument/onTypeFormatting",
                "textDocument/prepareCallHierarchy",
                "textDocument/prepareRename",
                "textDocument/prepareTypeHierarchy",
                "textDocument/rangeFormatting",
                "textDocument/rangesFormatting",
                "textDocument/references",
                "textDocument/rename",
                "textDocument/selectionRange",
                "textDocument/semanticTokens/full",
                "textDocument/semanticTokens/full/delta",
                "textDocument/semanticTokens/range",
                "textDocument/signatureHelp",
                "textDocument/typeDefinition",
                "textDocument/willSave",
                "textDocument/willSaveWaitUntil",
                "typeHierarchy/subtypes",
                "typeHierarchy/supertypes",
                "window/workDoneProgress/cancel",
                "workspace/diagnostic",
                "workspace/didChangeConfiguration",
                "workspace/didChangeWatchedFiles",
                "workspace/didChangeWorkspaceFolders",
                "workspace/didCreateFiles",
                "workspace/didDeleteFiles",
                "workspace/didRenameFiles",
                "workspace/executeCommand",
                "workspace/symbol",
                "workspace/textDocumentContent",
                "workspace/willCreateFiles",
                "workspace/willDeleteFiles",
                "workspace/willRenameFiles",
                "workspaceSymbol/resolve",
            )
    }
}
