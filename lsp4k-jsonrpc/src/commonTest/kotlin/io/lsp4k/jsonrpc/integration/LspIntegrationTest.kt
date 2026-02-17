package io.lsp4k.jsonrpc.integration

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.lsp4k.jsonrpc.Connection
import io.lsp4k.jsonrpc.LspCodec
import io.lsp4k.jsonrpc.ResponseError
import io.lsp4k.protocol.ClientCapabilities
import io.lsp4k.protocol.CompletionItem
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.CompletionItemKind
import io.lsp4k.protocol.CompletionList
import io.lsp4k.protocol.CompletionOptions
import io.lsp4k.protocol.CompletionParams
import io.lsp4k.protocol.Diagnostic
import io.lsp4k.protocol.DiagnosticSeverity
import io.lsp4k.protocol.DidChangeTextDocumentParams
import io.lsp4k.protocol.DidCloseTextDocumentParams
import io.lsp4k.protocol.DidOpenTextDocumentParams
import io.lsp4k.protocol.InitializeParams
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.PublishDiagnosticsParams
import io.lsp4k.protocol.Range
import io.lsp4k.protocol.ServerCapabilities
import io.lsp4k.protocol.ServerInfo
import io.lsp4k.protocol.TextDocumentContentChangeEvent
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.protocol.TextDocumentItem
import io.lsp4k.protocol.TextDocumentSyncKind
import io.lsp4k.protocol.TextDocumentSyncOptions
import io.lsp4k.protocol.VersionedTextDocumentIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * LSP Method constants for testing.
 * Using string literals to avoid dependency on specific LspMethods constants naming convention.
 */
private object Methods {
    const val INITIALIZE = "initialize"
    const val INITIALIZED = "initialized"
    const val SHUTDOWN = "shutdown"
    const val EXIT = "exit"
    const val TEXT_DOCUMENT_DID_OPEN = "textDocument/didOpen"
    const val TEXT_DOCUMENT_DID_CLOSE = "textDocument/didClose"
    const val TEXT_DOCUMENT_DID_CHANGE = "textDocument/didChange"
    const val TEXT_DOCUMENT_COMPLETION = "textDocument/completion"
    const val TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS = "textDocument/publishDiagnostics"
    const val WORKSPACE_DID_CHANGE_CONFIGURATION = "workspace/didChangeConfiguration"
}

/**
 * Integration tests for the LSP server implementation.
 *
 * These tests verify the full LSP flow using the JSON-RPC Connection directly,
 * without depending on the LanguageServer DSL:
 * - Initialize handshake
 * - Text document synchronization
 * - Request/response flow
 * - Error handling
 * - Bidirectional communication
 * - Shutdown flow
 */
class LspIntegrationTest {
    private val json = LspCodec.defaultJson

    // ===== Test 1: Full Initialize Handshake =====

    @Test
    fun `initialize handshake completes successfully`() =
        runTest {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val clientConnection = Connection(scope = scope)
            val serverConnection = Connection(scope = scope)

            // Configure server capabilities directly
            val serverInfo = ServerInfo("TestServer", "1.0.0")
            val capabilities =
                ServerCapabilities(
                    textDocumentSync =
                        TextDocumentSyncOptions(
                            openClose = true,
                            change = TextDocumentSyncKind.Full,
                        ),
                    hoverProvider = Either.left(true),
                )

            // Register server handlers
            serverConnection.onRequest(Methods.INITIALIZE) { params: JsonElement? ->
                val initParams = params?.let { json.decodeFromJsonElement(InitializeParams.serializer(), it) }
                initParams shouldNotBe null
                initParams!!.processId shouldBe 1234

                val result =
                    InitializeResult(
                        capabilities = capabilities,
                        serverInfo = serverInfo,
                    )
                json.encodeToJsonElement(InitializeResult.serializer(), result)
            }

            var initializedReceived = false
            serverConnection.onNotification(Methods.INITIALIZED) { _: JsonElement? ->
                initializedReceived = true
            }

            // Wire up connections
            launch {
                clientConnection.outgoing.collect { bytes ->
                    serverConnection.receive(bytes)
                }
            }
            launch {
                serverConnection.outgoing.collect { bytes ->
                    clientConnection.receive(bytes)
                }
            }

            // Send initialize request
            val initParams =
                InitializeParams(
                    processId = 1234,
                    rootUri = "file:///test",
                    capabilities = ClientCapabilities(),
                )
            val initResult =
                clientConnection.request<InitializeParams, InitializeResult>(
                    Methods.INITIALIZE,
                    initParams,
                )

            // Verify response
            initResult shouldNotBe null
            initResult!!.serverInfo?.name shouldBe "TestServer"
            initResult.serverInfo?.version shouldBe "1.0.0"
            initResult.capabilities.textDocumentSync?.change shouldBe TextDocumentSyncKind.Full
            initResult.capabilities.hoverProvider shouldBe Either.left(true)

            // Send initialized notification
            clientConnection.notify(Methods.INITIALIZED, null as JsonElement?)

            // Wait for notification processing
            delay(100)
            initializedReceived shouldBe true

            // Cleanup
            clientConnection.close()
            serverConnection.close()
            scope.cancel()
        }

    @Test
    fun `initialize returns server capabilities with completion provider`() =
        runTest {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val clientConnection = Connection(scope = scope)
            val serverConnection = Connection(scope = scope)

            val serverInfo = ServerInfo("CompletionServer", "2.0.0")
            val capabilities =
                ServerCapabilities(
                    completionProvider =
                        CompletionOptions(
                            triggerCharacters = listOf(".", ":"),
                            resolveProvider = true,
                        ),
                )

            serverConnection.onRequest(Methods.INITIALIZE) { _: JsonElement? ->
                val result =
                    InitializeResult(
                        capabilities = capabilities,
                        serverInfo = serverInfo,
                    )
                json.encodeToJsonElement(InitializeResult.serializer(), result)
            }

            launch {
                clientConnection.outgoing.collect { bytes ->
                    serverConnection.receive(bytes)
                }
            }
            launch {
                serverConnection.outgoing.collect { bytes ->
                    clientConnection.receive(bytes)
                }
            }

            val initResult =
                clientConnection.request<InitializeParams, InitializeResult>(
                    Methods.INITIALIZE,
                    InitializeParams(
                        processId = null,
                        rootUri = null,
                        capabilities = ClientCapabilities(),
                    ),
                )

            initResult shouldNotBe null
            initResult!!.capabilities.completionProvider shouldNotBe null
            initResult.capabilities.completionProvider!!.triggerCharacters shouldBe listOf(".", ":")
            initResult.capabilities.completionProvider!!.resolveProvider shouldBe true

            clientConnection.close()
            serverConnection.close()
            scope.cancel()
        }

    // ===== Test 2: Text Document Sync =====

    @Test
    fun `text document sync lifecycle works correctly`() =
        runTest {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val clientConnection = Connection(scope = scope)
            val serverConnection = Connection(scope = scope)

            // Track document state
            val openDocuments = mutableMapOf<String, String>()
            var lastClosedUri: String? = null

            serverConnection.onNotification(Methods.TEXT_DOCUMENT_DID_OPEN) { params: JsonElement? ->
                val didOpen = params?.let { json.decodeFromJsonElement(DidOpenTextDocumentParams.serializer(), it) }
                didOpen shouldNotBe null
                openDocuments[didOpen!!.textDocument.uri] = didOpen.textDocument.text
            }

            serverConnection.onNotification(Methods.TEXT_DOCUMENT_DID_CHANGE) { params: JsonElement? ->
                val didChange = params?.let { json.decodeFromJsonElement(DidChangeTextDocumentParams.serializer(), it) }
                didChange shouldNotBe null
                val uri = didChange!!.textDocument.uri
                // For full sync, take the last change's text
                val newText = didChange.contentChanges.lastOrNull()?.text
                if (newText != null) {
                    openDocuments[uri] = newText
                }
            }

            serverConnection.onNotification(Methods.TEXT_DOCUMENT_DID_CLOSE) { params: JsonElement? ->
                val didClose = params?.let { json.decodeFromJsonElement(DidCloseTextDocumentParams.serializer(), it) }
                didClose shouldNotBe null
                lastClosedUri = didClose!!.textDocument.uri
                openDocuments.remove(lastClosedUri)
            }

            launch {
                clientConnection.outgoing.collect { bytes ->
                    serverConnection.receive(bytes)
                }
            }
            launch {
                serverConnection.outgoing.collect { bytes ->
                    clientConnection.receive(bytes)
                }
            }

            val docUri = "file:///test/document.txt"

            // Open document
            clientConnection.notify(
                Methods.TEXT_DOCUMENT_DID_OPEN,
                DidOpenTextDocumentParams(
                    textDocument =
                        TextDocumentItem(
                            uri = docUri,
                            languageId = "plaintext",
                            version = 1,
                            text = "Hello, World!",
                        ),
                ),
            )

            delay(100)
            openDocuments[docUri] shouldBe "Hello, World!"

            // Change document
            clientConnection.notify(
                Methods.TEXT_DOCUMENT_DID_CHANGE,
                DidChangeTextDocumentParams(
                    textDocument =
                        VersionedTextDocumentIdentifier(
                            uri = docUri,
                            version = 2,
                        ),
                    contentChanges =
                        listOf(
                            TextDocumentContentChangeEvent(text = "Hello, LSP!"),
                        ),
                ),
            )

            delay(100)
            openDocuments[docUri] shouldBe "Hello, LSP!"

            // Close document
            clientConnection.notify(
                Methods.TEXT_DOCUMENT_DID_CLOSE,
                DidCloseTextDocumentParams(
                    textDocument = TextDocumentIdentifier(uri = docUri),
                ),
            )

            delay(100)
            lastClosedUri shouldBe docUri
            openDocuments.containsKey(docUri) shouldBe false

            clientConnection.close()
            serverConnection.close()
            scope.cancel()
        }

    @Test
    fun `incremental text document changes are tracked`() =
        runTest {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val clientConnection = Connection(scope = scope)
            val serverConnection = Connection(scope = scope)

            val changeHistory = mutableListOf<DidChangeTextDocumentParams>()

            serverConnection.onNotification(Methods.TEXT_DOCUMENT_DID_CHANGE) { params: JsonElement? ->
                val didChange = params?.let { json.decodeFromJsonElement(DidChangeTextDocumentParams.serializer(), it) }
                didChange shouldNotBe null
                changeHistory.add(didChange!!)
            }

            launch {
                clientConnection.outgoing.collect { bytes ->
                    serverConnection.receive(bytes)
                }
            }
            launch {
                serverConnection.outgoing.collect { bytes ->
                    clientConnection.receive(bytes)
                }
            }

            val docUri = "file:///test/incremental.txt"

            // Send multiple changes
            clientConnection.notify(
                Methods.TEXT_DOCUMENT_DID_CHANGE,
                DidChangeTextDocumentParams(
                    textDocument = VersionedTextDocumentIdentifier(uri = docUri, version = 1),
                    contentChanges =
                        listOf(
                            TextDocumentContentChangeEvent(
                                range = Range(Position(0, 0), Position(0, 5)),
                                text = "Hi",
                            ),
                        ),
                ),
            )

            clientConnection.notify(
                Methods.TEXT_DOCUMENT_DID_CHANGE,
                DidChangeTextDocumentParams(
                    textDocument = VersionedTextDocumentIdentifier(uri = docUri, version = 2),
                    contentChanges =
                        listOf(
                            TextDocumentContentChangeEvent(
                                range = Range(Position(0, 2), Position(0, 2)),
                                text = " there",
                            ),
                        ),
                ),
            )

            delay(200)

            changeHistory shouldHaveSize 2
            changeHistory[0].textDocument.version shouldBe 1
            changeHistory[1].textDocument.version shouldBe 2
            changeHistory[0].contentChanges.first().range shouldNotBe null
            changeHistory[1].contentChanges.first().text shouldBe " there"

            clientConnection.close()
            serverConnection.close()
            scope.cancel()
        }

    // ===== Test 3: Request/Response Flow =====

    @Test
    fun `completion request returns items from handler`() =
        runTest {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val clientConnection = Connection(scope = scope)
            val serverConnection = Connection(scope = scope)

            serverConnection.onRequest(Methods.TEXT_DOCUMENT_COMPLETION) { params: JsonElement? ->
                val completionParams = params?.let { json.decodeFromJsonElement(CompletionParams.serializer(), it) }
                completionParams shouldNotBe null

                val completionList =
                    CompletionList(
                        isIncomplete = false,
                        items =
                            listOf(
                                CompletionItem(
                                    label = "println",
                                    kind = CompletionItemKind.Function,
                                    detail = "Print a line",
                                ),
                                CompletionItem(
                                    label = "print",
                                    kind = CompletionItemKind.Function,
                                    detail = "Print without newline",
                                ),
                            ),
                    )
                json.encodeToJsonElement(CompletionList.serializer(), completionList)
            }

            launch {
                clientConnection.outgoing.collect { bytes ->
                    serverConnection.receive(bytes)
                }
            }
            launch {
                serverConnection.outgoing.collect { bytes ->
                    clientConnection.receive(bytes)
                }
            }

            val result =
                clientConnection.request<CompletionParams, CompletionList>(
                    Methods.TEXT_DOCUMENT_COMPLETION,
                    CompletionParams(
                        textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                        position = Position(line = 10, character = 5),
                    ),
                )

            result shouldNotBe null
            result!!.isIncomplete shouldBe false
            result.items shouldHaveSize 2
            result.items[0].label shouldBe "println"
            result.items[0].kind shouldBe CompletionItemKind.Function
            result.items[1].label shouldBe "print"

            clientConnection.close()
            serverConnection.close()
            scope.cancel()
        }

    @Test
    fun `completion request with null result is handled`() =
        runTest {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val clientConnection = Connection(scope = scope)
            val serverConnection = Connection(scope = scope)

            serverConnection.onRequest(Methods.TEXT_DOCUMENT_COMPLETION) { _: JsonElement? ->
                null // No completions available
            }

            launch {
                clientConnection.outgoing.collect { bytes ->
                    serverConnection.receive(bytes)
                }
            }
            launch {
                serverConnection.outgoing.collect { bytes ->
                    clientConnection.receive(bytes)
                }
            }

            val result =
                clientConnection.request<CompletionParams, CompletionList>(
                    Methods.TEXT_DOCUMENT_COMPLETION,
                    CompletionParams(
                        textDocument = TextDocumentIdentifier(uri = "file:///empty.kt"),
                        position = Position(line = 0, character = 0),
                    ),
                )

            result shouldBe null

            clientConnection.close()
            serverConnection.close()
            scope.cancel()
        }

    // ===== Test 4: Error Handling =====

    @Test
    fun `request to unregistered method returns method not found error`() =
        runTest {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val clientConnection = Connection(scope = scope)
            val serverConnection = Connection(scope = scope)

            // Server has no handlers registered

            launch {
                clientConnection.outgoing.collect { bytes ->
                    serverConnection.receive(bytes)
                }
            }
            launch {
                serverConnection.outgoing.collect { bytes ->
                    clientConnection.receive(bytes)
                }
            }

            val exception =
                assertFailsWith<io.lsp4k.jsonrpc.LspException> {
                    clientConnection.request<CompletionParams, CompletionList>(
                        "textDocument/unknownMethod",
                        CompletionParams(
                            textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                            position = Position(line = 0, character = 0),
                        ),
                    )
                }

            exception.code shouldBe ResponseError.METHOD_NOT_FOUND

            clientConnection.close()
            serverConnection.close()
            scope.cancel()
        }

    @Test
    fun `handler exception returns proper error response`() =
        runTest {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val clientConnection = Connection(scope = scope)
            val serverConnection = Connection(scope = scope)

            serverConnection.onRequest("test/fail") { _: JsonElement? ->
                throw io.lsp4k.jsonrpc.LspException(
                    ResponseError.INVALID_PARAMS,
                    "Invalid parameters provided",
                )
            }

            launch {
                clientConnection.outgoing.collect { bytes ->
                    serverConnection.receive(bytes)
                }
            }
            launch {
                serverConnection.outgoing.collect { bytes ->
                    clientConnection.receive(bytes)
                }
            }

            val exception =
                assertFailsWith<io.lsp4k.jsonrpc.LspException> {
                    clientConnection.request<JsonElement?, String>("test/fail", null)
                }

            exception.code shouldBe ResponseError.INVALID_PARAMS
            exception.message shouldContain "Invalid parameters"

            clientConnection.close()
            serverConnection.close()
            scope.cancel()
        }

    // ===== Test 5: Bidirectional Communication =====

    @Test
    fun `server can send publishDiagnostics notification to client`() =
        runTest {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val clientConnection = Connection(scope = scope)
            val serverConnection = Connection(scope = scope)

            var receivedDiagnostics: PublishDiagnosticsParams? = null

            clientConnection.onNotification(Methods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS) { params: JsonElement? ->
                receivedDiagnostics =
                    params?.let {
                        json.decodeFromJsonElement(PublishDiagnosticsParams.serializer(), it)
                    }
            }

            launch {
                clientConnection.outgoing.collect { bytes ->
                    serverConnection.receive(bytes)
                }
            }
            launch {
                serverConnection.outgoing.collect { bytes ->
                    clientConnection.receive(bytes)
                }
            }

            // Server sends diagnostics
            val diagnostics =
                PublishDiagnosticsParams(
                    uri = "file:///test/error.kt",
                    version = 1,
                    diagnostics =
                        listOf(
                            Diagnostic(
                                range = Range(Position(5, 0), Position(5, 10)),
                                message = "Unresolved reference: foobar",
                                severity = DiagnosticSeverity.Error,
                                source = "kotlin",
                            ),
                            Diagnostic(
                                range = Range(Position(10, 4), Position(10, 20)),
                                message = "Unused variable 'x'",
                                severity = DiagnosticSeverity.Warning,
                                source = "kotlin",
                            ),
                        ),
                )

            serverConnection.notify(
                Methods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS,
                json.encodeToJsonElement(PublishDiagnosticsParams.serializer(), diagnostics),
            )

            delay(200)

            receivedDiagnostics shouldNotBe null
            receivedDiagnostics!!.uri shouldBe "file:///test/error.kt"
            receivedDiagnostics!!.version shouldBe 1
            receivedDiagnostics!!.diagnostics shouldHaveSize 2
            receivedDiagnostics!!.diagnostics[0].message shouldBe "Unresolved reference: foobar"
            receivedDiagnostics!!.diagnostics[0].severity shouldBe DiagnosticSeverity.Error
            receivedDiagnostics!!.diagnostics[1].severity shouldBe DiagnosticSeverity.Warning

            clientConnection.close()
            serverConnection.close()
            scope.cancel()
        }

    @Test
    fun `bidirectional request response flow works`() =
        runTest {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val clientConnection = Connection(scope = scope)
            val serverConnection = Connection(scope = scope)

            // Server can request workspace configuration from client
            clientConnection.onRequest("workspace/configuration") { _: JsonElement? ->
                json.encodeToJsonElement(
                    kotlinx.serialization.serializer<Map<String, String>>(),
                    mapOf("tabSize" to "4", "insertSpaces" to "true"),
                )
            }

            launch {
                clientConnection.outgoing.collect { bytes ->
                    serverConnection.receive(bytes)
                }
            }
            launch {
                serverConnection.outgoing.collect { bytes ->
                    clientConnection.receive(bytes)
                }
            }

            // Server requests configuration from client
            val config =
                serverConnection.request<JsonElement?, Map<String, String>>(
                    "workspace/configuration",
                    null,
                )

            config shouldNotBe null
            config!!["tabSize"] shouldBe "4"
            config["insertSpaces"] shouldBe "true"

            clientConnection.close()
            serverConnection.close()
            scope.cancel()
        }

    // ===== Test 6: Shutdown Flow =====

    @Test
    fun `shutdown and exit sequence completes cleanly`() =
        runTest {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val clientConnection = Connection(scope = scope)
            val serverConnection = Connection(scope = scope)

            var shutdownReceived = false
            var exitReceived = false

            serverConnection.onRequest(Methods.SHUTDOWN) { _: JsonElement? ->
                shutdownReceived = true
                null // Shutdown returns null
            }

            serverConnection.onNotification(Methods.EXIT) { _: JsonElement? ->
                exitReceived = true
            }

            launch {
                clientConnection.outgoing.collect { bytes ->
                    serverConnection.receive(bytes)
                }
            }
            launch {
                serverConnection.outgoing.collect { bytes ->
                    clientConnection.receive(bytes)
                }
            }

            // Send shutdown request
            val shutdownResult =
                clientConnection.request<JsonElement?, JsonElement?>(
                    Methods.SHUTDOWN,
                    null,
                )

            shutdownResult shouldBe null
            shutdownReceived shouldBe true

            // Send exit notification
            clientConnection.notify(Methods.EXIT, null as JsonElement?)

            delay(100)
            exitReceived shouldBe true

            clientConnection.close()
            serverConnection.close()
            scope.cancel()
        }

    @Test
    fun `server rejects requests after shutdown`() =
        runTest {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val clientConnection = Connection(scope = scope)
            val serverConnection = Connection(scope = scope)

            var serverShutdown = false

            serverConnection.onRequest(Methods.SHUTDOWN) { _: JsonElement? ->
                serverShutdown = true
                null
            }

            serverConnection.onRequest(Methods.TEXT_DOCUMENT_COMPLETION) { _: JsonElement? ->
                if (serverShutdown) {
                    throw io.lsp4k.jsonrpc.LspException(
                        ResponseError.INVALID_REQUEST,
                        "Server is shutting down",
                    )
                }
                json.encodeToJsonElement(
                    CompletionList.serializer(),
                    CompletionList(isIncomplete = false, items = emptyList()),
                )
            }

            launch {
                clientConnection.outgoing.collect { bytes ->
                    serverConnection.receive(bytes)
                }
            }
            launch {
                serverConnection.outgoing.collect { bytes ->
                    clientConnection.receive(bytes)
                }
            }

            // Shutdown
            clientConnection.request<JsonElement?, JsonElement?>(Methods.SHUTDOWN, null)

            // Try to make a request after shutdown
            val exception =
                assertFailsWith<io.lsp4k.jsonrpc.LspException> {
                    clientConnection.request<CompletionParams, CompletionList>(
                        Methods.TEXT_DOCUMENT_COMPLETION,
                        CompletionParams(
                            textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                            position = Position(line = 0, character = 0),
                        ),
                    )
                }

            exception.code shouldBe ResponseError.INVALID_REQUEST
            exception.message shouldContain "shutting down"

            clientConnection.close()
            serverConnection.close()
            scope.cancel()
        }
}
