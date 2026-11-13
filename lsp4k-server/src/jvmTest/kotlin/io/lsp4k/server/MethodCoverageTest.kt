package io.lsp4k.server

import io.lsp4k.client.LanguageClientBuilder
import io.lsp4k.client.LanguageClientSession
import io.lsp4k.client.ServerProxy
import io.lsp4k.client.languageClient
import io.lsp4k.protocol.ApplyWorkspaceEditResult
import io.lsp4k.protocol.DocumentDiagnosticReport
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.RelatedFullDocumentDiagnosticReport
import io.lsp4k.protocol.ServerCapabilities
import io.lsp4k.protocol.ShowDocumentResult
import io.lsp4k.protocol.TextDocumentContentResult
import io.lsp4k.protocol.WorkspaceDiagnosticReport
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * G2.2 and G2.3: every request and notification of the vendored LSP 3.18 meta model has an lsp4k API on both sides.
 *
 * A `clientToServer` method needs a server hook and a client call, a `serverToClient` method a server call and a
 * client hook, and a method sent both ways needs all four. [rows] is the explicit table: a meta-model method without a
 * row, a row for a method the meta model does not declare, and a row whose direction differs from `messageDirection`
 * all fail. Hooks are checked by registering them; calls are public functions found by reflection, under their
 * mangled `name-hash` JVM name when a parameter has a value-class type such as `Duration`.
 */
class MethodCoverageTest {
    @Test
    fun `the table has one row per meta model method with its direction`() {
        val directions = metaModelDirections()
        val methods = rows.map { it.method }
        assertEquals(emptyList(), methods.filterIndexed { index, method -> methods.indexOf(method) != index }, "methods with two rows")
        assertEquals(emptySet(), directions.keys - methods.toSet(), "meta-model methods without a row")
        assertEquals(emptySet(), methods.toSet() - directions.keys, "rows for methods the meta model does not declare")
        rows.forEach { assertEquals(directions[it.method], it.direction, "direction of ${it.method}") }
    }

    @Test
    fun `every method has the APIs its direction requires`() {
        rows.forEach { row ->
            val toServer = row.direction != SERVER_TO_CLIENT
            val toClient = row.direction != CLIENT_TO_SERVER
            val byConnection = row.method in HANDLED_BY_CONNECTION
            assertEquals(toServer, row.serverHook != null || byConnection, "server hook for ${row.method}")
            assertEquals(toServer, row.clientCall != null, "client call for ${row.method}")
            assertEquals(toClient, row.serverCall != null, "server call for ${row.method}")
            assertEquals(toClient, row.clientHook != null || byConnection, "client hook for ${row.method}")
        }
    }

    @Test
    fun `every server hook handles exactly its method`() {
        rows.forEach { row ->
            row.serverHook?.let { hook -> assertEquals(setOf(row.method), languageServer(hook).handledMethods(), row.method) }
        }
    }

    @Test
    fun `every client hook handles exactly its method`() {
        rows.forEach { row ->
            row.clientHook?.let { hook ->
                val config = languageClient(hook)
                assertEquals(setOf(row.method), config.requestHandlers.keys + config.notificationHandlers.keys, row.method)
            }
        }
    }

    @Test
    fun `every call is a public function`() {
        val missing = rows.flatMap { listOfNotNull(it.clientCall, it.serverCall) }.filterNot { it.exists() }
        assertEquals(emptyList(), missing.map { it.toString() })
    }

    @Test
    fun `call lookup accepts mangled names and rejects unknown functions`() {
        val workspace = LanguageClient::class.java.getMethod("getWorkspace").returnType
        assertTrue(workspace.methods.none { it.name == "workspaceFolders" }, "workspaceFolders should have a mangled JVM name")
        assertTrue(server("workspace.workspaceFolders").exists())
        assertFalse(server("workspace.noSuchCall").exists())
        assertFalse(server("noSuchGroup.applyEdit").exists())
    }

    @Test
    fun `cancellation is reserved for the jsonrpc4k connection`() {
        assertFailsWith<IllegalArgumentException> { languageServer { onNotification(LspMethods.CANCEL_REQUEST) {} } }
    }

    /** A public function reached from [root] through the property getters named by all but the last [path] segment. */
    private class ApiFunction(
        val root: Class<*>,
        val path: String,
    ) {
        fun exists(): Boolean {
            val segments = path.split('.')
            val owner =
                segments.dropLast(1).fold<String, Class<*>?>(root) { current, property ->
                    current?.methods?.firstOrNull { it.name == "get" + property.replaceFirstChar(Char::uppercaseChar) }?.returnType
                }
            val name = segments.last()
            return owner?.methods.orEmpty().any { !Modifier.isStatic(it.modifiers) && (it.name == name || it.name.startsWith("$name-")) }
        }

        override fun toString(): String = "${root.simpleName}.$path"
    }

    private class Row(
        val method: String,
        val direction: String,
        val serverHook: (LanguageServerBuilder.() -> Unit)? = null,
        val serverCall: ApiFunction? = null,
        val clientCall: ApiFunction? = null,
        val clientHook: (LanguageClientBuilder.() -> Unit)? = null,
    )

    private companion object {
        const val CLIENT_TO_SERVER = "clientToServer"
        const val SERVER_TO_CLIENT = "serverToClient"
        const val BOTH = "both"
        const val META_MODEL = "/lsp/3.18/metaModel.json"

        /** Inbound `$/cancelRequest` cancels the target handler inside the jsonrpc4k connection, on both sides. */
        val HANDLED_BY_CONNECTION = setOf(LspMethods.CANCEL_REQUEST)

        fun metaModelDirections(): Map<String, String> {
            val stream = checkNotNull(MethodCoverageTest::class.java.getResourceAsStream(META_MODEL)) { "Missing $META_MODEL" }
            val root = Json.parseToJsonElement(stream.use { it.readBytes() }.decodeToString()).jsonObject
            return listOf("requests", "notifications")
                .flatMap { root.getValue(it).jsonArray }
                .map { it.jsonObject }
                .associate { it.string("method") to it.string("messageDirection") }
        }

        fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

        fun LanguageServerConfig.handledMethods(): Set<String> =
            requestHandlers.keys + notificationHandlers.keys +
                listOfNotNull(
                    LspMethods.INITIALIZE.takeIf { lifecycle.initialize != null },
                    LspMethods.INITIALIZED.takeIf { lifecycle.initialized != null },
                    LspMethods.SHUTDOWN.takeIf { lifecycle.shutdown != null },
                    LspMethods.EXIT.takeIf { lifecycle.exit != null },
                    LspMethods.SET_TRACE.takeIf { lifecycle.setTrace != null },
                )

        /** A call on the client's [ServerProxy]. */
        fun client(path: String) = ApiFunction(ServerProxy::class.java, path)

        /** A lifecycle call on [LanguageClientSession]. */
        fun session(path: String) = ApiFunction(LanguageClientSession::class.java, path)

        /** A call on the server's [LanguageClient] proxy. */
        fun server(path: String) = ApiFunction(LanguageClient::class.java, path)

        fun toServer(
            method: String,
            hook: LanguageServerBuilder.() -> Unit,
            call: ApiFunction,
        ) = Row(method, CLIENT_TO_SERVER, serverHook = hook, clientCall = call)

        fun toClient(
            method: String,
            call: ApiFunction,
            hook: LanguageClientBuilder.() -> Unit,
        ) = Row(method, SERVER_TO_CLIENT, serverCall = call, clientHook = hook)

        val rows: List<Row> =
            listOf(
                // Lifecycle: the session sends `initialized` after the `initialize` result and `exit` after `shutdown`.
                toServer(LspMethods.INITIALIZE, { onInitialize { InitializeResult(ServerCapabilities()) } }, session("initialize")),
                toServer(LspMethods.INITIALIZED, { onInitialized {} }, session("initialize")),
                toServer(LspMethods.SHUTDOWN, { onShutdown {} }, session("shutdown")),
                toServer(LspMethods.EXIT, { onExit {} }, session("shutdown")),
                toServer(LspMethods.SET_TRACE, { onSetTrace {} }, client("setTrace")),
                Row(LspMethods.CANCEL_REQUEST, BOTH, serverCall = server("cancelRequest"), clientCall = client("cancelRequest")),
                Row(LspMethods.PROGRESS, BOTH, { onProgress {} }, server("progress"), client("progress"), { onProgress {} }),
                toClient(LspMethods.LOG_TRACE, server("logTrace")) { onLogTrace {} },
                toClient(LspMethods.TELEMETRY_EVENT, server("telemetryEvent")) { onTelemetryEvent {} },
                // Text document synchronization, completion, hover, signature help and navigation.
                toServer(LspMethods.TEXT_DOCUMENT_DID_OPEN, { textDocument { didOpen {} } }, client("textDocument.didOpen")),
                toServer(LspMethods.TEXT_DOCUMENT_DID_CHANGE, { textDocument { didChange {} } }, client("textDocument.didChange")),
                toServer(LspMethods.TEXT_DOCUMENT_DID_CLOSE, { textDocument { didClose {} } }, client("textDocument.didClose")),
                toServer(LspMethods.TEXT_DOCUMENT_DID_SAVE, { textDocument { didSave {} } }, client("textDocument.didSave")),
                toServer(LspMethods.TEXT_DOCUMENT_WILL_SAVE, { textDocument { willSave {} } }, client("textDocument.willSave")),
                toServer(
                    LspMethods.TEXT_DOCUMENT_WILL_SAVE_WAIT_UNTIL,
                    { textDocument { willSaveWaitUntil { null } } },
                    client("textDocument.willSaveWaitUntil"),
                ),
                toServer(LspMethods.TEXT_DOCUMENT_COMPLETION, { textDocument { completion { null } } }, client("textDocument.completion")),
                toServer(
                    LspMethods.COMPLETION_ITEM_RESOLVE,
                    { textDocument { completionResolve { it } } },
                    client("textDocument.completionResolve"),
                ),
                toServer(LspMethods.TEXT_DOCUMENT_HOVER, { textDocument { hover { null } } }, client("textDocument.hover")),
                toServer(
                    LspMethods.TEXT_DOCUMENT_SIGNATURE_HELP,
                    { textDocument { signatureHelp { null } } },
                    client("textDocument.signatureHelp"),
                ),
                toServer(
                    LspMethods.TEXT_DOCUMENT_DECLARATION,
                    { textDocument { declaration { null } } },
                    client("textDocument.declaration"),
                ),
                toServer(LspMethods.TEXT_DOCUMENT_DEFINITION, { textDocument { definition { null } } }, client("textDocument.definition")),
                toServer(
                    LspMethods.TEXT_DOCUMENT_TYPE_DEFINITION,
                    { textDocument { typeDefinition { null } } },
                    client("textDocument.typeDefinition"),
                ),
                toServer(
                    LspMethods.TEXT_DOCUMENT_IMPLEMENTATION,
                    { textDocument { implementation { null } } },
                    client("textDocument.implementation"),
                ),
                toServer(LspMethods.TEXT_DOCUMENT_REFERENCES, { textDocument { references { null } } }, client("textDocument.references")),
                toServer(
                    LspMethods.TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT,
                    { textDocument { documentHighlight { null } } },
                    client("textDocument.documentHighlight"),
                ),
                toServer(
                    LspMethods.TEXT_DOCUMENT_DOCUMENT_SYMBOL,
                    { textDocument { documentSymbol { null } } },
                    client("textDocument.documentSymbol"),
                ),
                // Code actions, lenses, links, colors, formatting, rename and ranges.
                toServer(LspMethods.TEXT_DOCUMENT_CODE_ACTION, { editing { codeAction { null } } }, client("editing.codeAction")),
                toServer(LspMethods.CODE_ACTION_RESOLVE, { editing { codeActionResolve { it } } }, client("editing.codeActionResolve")),
                toServer(LspMethods.TEXT_DOCUMENT_CODE_LENS, { editing { codeLens { null } } }, client("editing.codeLens")),
                toServer(LspMethods.CODE_LENS_RESOLVE, { editing { codeLensResolve { it } } }, client("editing.codeLensResolve")),
                toServer(LspMethods.TEXT_DOCUMENT_DOCUMENT_LINK, { editing { documentLink { null } } }, client("editing.documentLink")),
                toServer(
                    LspMethods.DOCUMENT_LINK_RESOLVE,
                    { editing { documentLinkResolve { it } } },
                    client("editing.documentLinkResolve"),
                ),
                toServer(
                    LspMethods.TEXT_DOCUMENT_DOCUMENT_COLOR,
                    { editing { documentColor { emptyList() } } },
                    client("editing.documentColor"),
                ),
                toServer(
                    LspMethods.TEXT_DOCUMENT_COLOR_PRESENTATION,
                    { editing { colorPresentation { emptyList() } } },
                    client("editing.colorPresentation"),
                ),
                toServer(LspMethods.TEXT_DOCUMENT_FORMATTING, { editing { formatting { null } } }, client("editing.formatting")),
                toServer(
                    LspMethods.TEXT_DOCUMENT_RANGE_FORMATTING,
                    { editing { rangeFormatting { null } } },
                    client("editing.rangeFormatting"),
                ),
                toServer(
                    LspMethods.TEXT_DOCUMENT_RANGES_FORMATTING,
                    { editing { rangesFormatting { null } } },
                    client("editing.rangesFormatting"),
                ),
                toServer(
                    LspMethods.TEXT_DOCUMENT_ON_TYPE_FORMATTING,
                    { editing { onTypeFormatting { null } } },
                    client("editing.onTypeFormatting"),
                ),
                toServer(LspMethods.TEXT_DOCUMENT_RENAME, { editing { rename { null } } }, client("editing.rename")),
                toServer(LspMethods.TEXT_DOCUMENT_PREPARE_RENAME, { editing { prepareRename { null } } }, client("editing.prepareRename")),
                toServer(LspMethods.TEXT_DOCUMENT_FOLDING_RANGE, { editing { foldingRange { null } } }, client("editing.foldingRange")),
                toServer(
                    LspMethods.TEXT_DOCUMENT_SELECTION_RANGE,
                    { editing { selectionRange { null } } },
                    client("editing.selectionRange"),
                ),
                toServer(
                    LspMethods.TEXT_DOCUMENT_LINKED_EDITING_RANGE,
                    { editing { linkedEditingRange { null } } },
                    client("editing.linkedEditingRange"),
                ),
                // Semantic tokens, inlay hints, diagnostics, monikers, inline values and inline completion.
                toServer(
                    LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL,
                    { insights { semanticTokensFull { null } } },
                    client("insights.semanticTokensFull"),
                ),
                toServer(
                    LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL_DELTA,
                    { insights { semanticTokensFullDelta { null } } },
                    client("insights.semanticTokensFullDelta"),
                ),
                toServer(
                    LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_RANGE,
                    { insights { semanticTokensRange { null } } },
                    client("insights.semanticTokensRange"),
                ),
                toServer(LspMethods.TEXT_DOCUMENT_INLAY_HINT, { insights { inlayHint { null } } }, client("insights.inlayHint")),
                toServer(LspMethods.INLAY_HINT_RESOLVE, { insights { inlayHintResolve { it } } }, client("insights.inlayHintResolve")),
                toServer(
                    LspMethods.TEXT_DOCUMENT_DIAGNOSTIC,
                    { insights { diagnostic { DocumentDiagnosticReport.Full(RelatedFullDocumentDiagnosticReport(items = emptyList())) } } },
                    client("insights.diagnostic"),
                ),
                toServer(LspMethods.TEXT_DOCUMENT_MONIKER, { insights { moniker { null } } }, client("insights.moniker")),
                toServer(LspMethods.TEXT_DOCUMENT_INLINE_VALUE, { insights { inlineValue { null } } }, client("insights.inlineValue")),
                toServer(
                    LspMethods.TEXT_DOCUMENT_INLINE_COMPLETION,
                    { insights { inlineCompletion { null } } },
                    client("insights.inlineCompletion"),
                ),
                // Call and type hierarchy.
                toServer(
                    LspMethods.TEXT_DOCUMENT_PREPARE_CALL_HIERARCHY,
                    { hierarchy { prepareCallHierarchy { null } } },
                    client("hierarchy.prepareCallHierarchy"),
                ),
                toServer(
                    LspMethods.CALL_HIERARCHY_INCOMING_CALLS,
                    { hierarchy { callHierarchyIncomingCalls { null } } },
                    client("hierarchy.callHierarchyIncomingCalls"),
                ),
                toServer(
                    LspMethods.CALL_HIERARCHY_OUTGOING_CALLS,
                    { hierarchy { callHierarchyOutgoingCalls { null } } },
                    client("hierarchy.callHierarchyOutgoingCalls"),
                ),
                toServer(
                    LspMethods.TEXT_DOCUMENT_PREPARE_TYPE_HIERARCHY,
                    { hierarchy { prepareTypeHierarchy { null } } },
                    client("hierarchy.prepareTypeHierarchy"),
                ),
                toServer(
                    LspMethods.TYPE_HIERARCHY_SUPERTYPES,
                    { hierarchy { typeHierarchySupertypes { null } } },
                    client("hierarchy.typeHierarchySupertypes"),
                ),
                toServer(
                    LspMethods.TYPE_HIERARCHY_SUBTYPES,
                    { hierarchy { typeHierarchySubtypes { null } } },
                    client("hierarchy.typeHierarchySubtypes"),
                ),
                // Workspace.
                toServer(LspMethods.WORKSPACE_SYMBOL, { workspace { symbol { null } } }, client("workspace.symbol")),
                toServer(
                    LspMethods.WORKSPACE_SYMBOL_RESOLVE,
                    { workspace { workspaceSymbolResolve { it } } },
                    client("workspace.workspaceSymbolResolve"),
                ),
                toServer(
                    LspMethods.WORKSPACE_EXECUTE_COMMAND,
                    { workspace { executeCommand { null } } },
                    client("workspace.executeCommand"),
                ),
                toServer(
                    LspMethods.WORKSPACE_DIAGNOSTIC,
                    { workspace { workspaceDiagnostic { WorkspaceDiagnosticReport(items = emptyList()) } } },
                    client("workspace.workspaceDiagnostic"),
                ),
                toServer(
                    LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT,
                    { workspace { textDocumentContent { TextDocumentContentResult("") } } },
                    client("workspace.textDocumentContent"),
                ),
                toServer(
                    LspMethods.WORKSPACE_DID_CHANGE_CONFIGURATION,
                    { workspace { didChangeConfiguration {} } },
                    client("workspace.didChangeConfiguration"),
                ),
                toServer(
                    LspMethods.WORKSPACE_DID_CHANGE_WATCHED_FILES,
                    { workspace { didChangeWatchedFiles {} } },
                    client("workspace.didChangeWatchedFiles"),
                ),
                toServer(
                    LspMethods.WORKSPACE_DID_CHANGE_WORKSPACE_FOLDERS,
                    { workspace { didChangeWorkspaceFolders {} } },
                    client("workspace.didChangeWorkspaceFolders"),
                ),
                toServer(
                    LspMethods.WORKSPACE_WILL_CREATE_FILES,
                    { workspace { willCreateFiles { null } } },
                    client("workspace.willCreateFiles"),
                ),
                toServer(LspMethods.WORKSPACE_DID_CREATE_FILES, { workspace { didCreateFiles {} } }, client("workspace.didCreateFiles")),
                toServer(
                    LspMethods.WORKSPACE_WILL_RENAME_FILES,
                    { workspace { willRenameFiles { null } } },
                    client("workspace.willRenameFiles"),
                ),
                toServer(LspMethods.WORKSPACE_DID_RENAME_FILES, { workspace { didRenameFiles {} } }, client("workspace.didRenameFiles")),
                toServer(
                    LspMethods.WORKSPACE_WILL_DELETE_FILES,
                    { workspace { willDeleteFiles { null } } },
                    client("workspace.willDeleteFiles"),
                ),
                toServer(LspMethods.WORKSPACE_DID_DELETE_FILES, { workspace { didDeleteFiles {} } }, client("workspace.didDeleteFiles")),
                // Notebook document synchronization.
                toServer(LspMethods.NOTEBOOK_DOCUMENT_DID_OPEN, { notebook { didOpen {} } }, client("notebook.didOpen")),
                toServer(LspMethods.NOTEBOOK_DOCUMENT_DID_CHANGE, { notebook { didChange {} } }, client("notebook.didChange")),
                toServer(LspMethods.NOTEBOOK_DOCUMENT_DID_SAVE, { notebook { didSave {} } }, client("notebook.didSave")),
                toServer(LspMethods.NOTEBOOK_DOCUMENT_DID_CLOSE, { notebook { didClose {} } }, client("notebook.didClose")),
                // Window.
                toServer(
                    LspMethods.WINDOW_WORK_DONE_PROGRESS_CANCEL,
                    { window { workDoneProgressCancel {} } },
                    client("window.workDoneProgressCancel"),
                ),
                toClient(LspMethods.WINDOW_SHOW_MESSAGE, server("window.showMessage")) { window { showMessage {} } },
                toClient(LspMethods.WINDOW_LOG_MESSAGE, server("window.logMessage")) { window { logMessage {} } },
                toClient(
                    LspMethods.WINDOW_SHOW_MESSAGE_REQUEST,
                    server("window.showMessageRequest"),
                ) { window { showMessageRequest { null } } },
                toClient(LspMethods.WINDOW_SHOW_DOCUMENT, server("window.showDocument")) {
                    window { showDocument { ShowDocumentResult(success = true) } }
                },
                toClient(LspMethods.WINDOW_WORK_DONE_PROGRESS_CREATE, server("window.createWorkDoneProgress")) {
                    window { workDoneProgressCreate {} }
                },
                // Server-to-client workspace requests.
                toClient(LspMethods.WORKSPACE_APPLY_EDIT, server("workspace.applyEdit")) {
                    workspace { applyEdit { ApplyWorkspaceEditResult(applied = true) } }
                },
                toClient(
                    LspMethods.WORKSPACE_CONFIGURATION,
                    server("workspace.configuration"),
                ) { workspace { configuration { emptyList() } } },
                toClient(
                    LspMethods.WORKSPACE_WORKSPACE_FOLDERS,
                    server("workspace.workspaceFolders"),
                ) { workspace { workspaceFolders { null } } },
                toClient(LspMethods.WORKSPACE_CODE_LENS_REFRESH, server("workspace.codeLensRefresh")) { workspace { codeLensRefresh {} } },
                toClient(LspMethods.WORKSPACE_SEMANTIC_TOKENS_REFRESH, server("workspace.semanticTokensRefresh")) {
                    workspace { semanticTokensRefresh {} }
                },
                toClient(
                    LspMethods.WORKSPACE_INLAY_HINT_REFRESH,
                    server("workspace.inlayHintRefresh"),
                ) { workspace { inlayHintRefresh {} } },
                toClient(
                    LspMethods.WORKSPACE_INLINE_VALUE_REFRESH,
                    server("workspace.inlineValueRefresh"),
                ) { workspace { inlineValueRefresh {} } },
                toClient(
                    LspMethods.WORKSPACE_DIAGNOSTIC_REFRESH,
                    server("workspace.diagnosticRefresh"),
                ) { workspace { diagnosticRefresh {} } },
                toClient(
                    LspMethods.WORKSPACE_FOLDING_RANGE_REFRESH,
                    server("workspace.foldingRangeRefresh"),
                ) { workspace { foldingRangeRefresh {} } },
                toClient(LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT_REFRESH, server("workspace.textDocumentContentRefresh")) {
                    workspace { textDocumentContentRefresh {} }
                },
                toClient(LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS, server("textDocument.publishDiagnostics")) {
                    textDocument { publishDiagnostics {} }
                },
                toClient(
                    LspMethods.CLIENT_REGISTER_CAPABILITY,
                    server("registration.registerCapability"),
                ) { client { registerCapability {} } },
                toClient(
                    LspMethods.CLIENT_UNREGISTER_CAPABILITY,
                    server("registration.unregisterCapability"),
                ) { client { unregisterCapability {} } },
            )
    }
}
