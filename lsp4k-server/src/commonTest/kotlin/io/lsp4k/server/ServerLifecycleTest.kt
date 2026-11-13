package io.lsp4k.server

import io.jsonrpc4k.core.ResponseError
import io.lsp4k.protocol.DidOpenTextDocumentParams
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.Hover
import io.lsp4k.protocol.HoverContents
import io.lsp4k.protocol.HoverParams
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.MarkupContent
import io.lsp4k.protocol.MarkupKind
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.ServerCapabilities
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.protocol.TextDocumentItem
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerLifecycleTest {
    private val hoverParams = HoverParams(TextDocumentIdentifier("file:///a.txt"), Position(0, 0))
    private val hoverResult = Hover(HoverContents.Markup(MarkupContent(MarkupKind.PlainText, "hover")))
    private val opened = DidOpenTextDocumentParams(TextDocumentItem("file:///a.txt", "text", 1, "a"))

    @Test
    fun `requests before initialize fail with ServerNotInitialized and notifications are dropped`() =
        runTest {
            val received = mutableListOf<String>()
            val peer =
                startPeer(backgroundScope) {
                    textDocument {
                        didOpen { received += "didOpen" }
                        hover { hoverResult }
                    }
                }

            val early = peer.request(LspMethods.TEXT_DOCUMENT_HOVER, wire(hoverParams))
            assertEquals(ResponseError.SERVER_NOT_INITIALIZED, early.errorCode())
            assertEquals(ResponseError.SERVER_NOT_INITIALIZED, peer.request(LspMethods.SHUTDOWN).errorCode())
            peer.send(LspMethods.TEXT_DOCUMENT_DID_OPEN, opened)
            peer.notify(LspMethods.INITIALIZED, JsonObject(emptyMap()))
            peer.sync()
            assertEquals(ServerState.Uninitialized, peer.server.state.value)

            peer.initialize()
            assertEquals(hoverResult, peer.call<HoverParams, Hover>(LspMethods.TEXT_DOCUMENT_HOVER, hoverParams))
            assertEquals(emptyList(), received)
            assertEquals(ServerState.Initialized, peer.server.state.value)
        }

    @Test
    fun `requests are served once initialize answered even before initialized`() =
        runTest {
            val peer = startPeer(backgroundScope) { textDocument { hover { hoverResult } } }
            peer.request(LspMethods.INITIALIZE, wire(initializeParams())).result()
            assertEquals(hoverResult, peer.call<HoverParams, Hover>(LspMethods.TEXT_DOCUMENT_HOVER, hoverParams))
        }

    @Test
    fun `a second initialize is rejected with InvalidRequest`() =
        runTest {
            var calls = 0
            val peer =
                startPeer(backgroundScope) {
                    onInitialize {
                        calls++
                        InitializeResult(ServerCapabilities())
                    }
                }
            peer.initialize().result()
            val second = peer.request(LspMethods.INITIALIZE, wire(initializeParams()))
            assertEquals(ResponseError.INVALID_REQUEST, second.errorCode())
            assertEquals(1, calls)
            assertEquals(ServerState.Initialized, peer.server.state.value)
        }

    @Test
    fun `initialize without params fails with InvalidParams and can be retried`() =
        runTest {
            val peer = startPeer(backgroundScope) {}
            assertEquals(ResponseError.INVALID_PARAMS, peer.request(LspMethods.INITIALIZE).errorCode())
            assertEquals(ServerState.Uninitialized, peer.server.state.value)
            peer.initialize().result()
            assertEquals(ServerState.Initialized, peer.server.state.value)
            assertEquals(initializeParams(), peer.server.initializeParams)
        }

    @Test
    fun `shutdown rejects later requests and drops notifications until exit with code 0`() =
        runTest {
            val events = mutableListOf<String>()
            val peer =
                startPeer(backgroundScope) {
                    onShutdown { events += "shutdown" }
                    onExit { code -> events += "exit $code" }
                    textDocument {
                        didOpen { events += "didOpen" }
                        hover { hoverResult }
                    }
                }
            peer.initialize()

            assertEquals(JsonNull, peer.request(LspMethods.SHUTDOWN).result())
            assertEquals(listOf("shutdown"), events)
            assertEquals(ServerState.ShutDown, peer.server.state.value)
            val after = peer.request(LspMethods.TEXT_DOCUMENT_HOVER, wire(hoverParams))
            assertEquals(ResponseError.INVALID_REQUEST, after.errorCode())
            assertEquals(ResponseError.INVALID_REQUEST, peer.request(LspMethods.SHUTDOWN).errorCode())
            assertEquals(ResponseError.INVALID_REQUEST, peer.request(LspMethods.INITIALIZE, wire(initializeParams())).errorCode())
            peer.send(LspMethods.TEXT_DOCUMENT_DID_OPEN, opened)
            peer.sync()

            peer.notify(LspMethods.EXIT)
            assertNull(peer.server.awaitTermination())
            assertEquals(0, peer.server.exitCode)
            assertEquals(ServerState.Exited, peer.server.state.value)
            assertEquals(listOf("shutdown", "exit 0"), events)
        }

    @Test
    fun `exit without shutdown reports exit code 1`() =
        runTest {
            var exitCode: Int? = null
            val peer = startPeer(backgroundScope) { onExit { exitCode = it } }
            peer.initialize()
            assertNull(peer.server.exitCode)
            peer.notify(LspMethods.EXIT)
            assertNull(peer.server.awaitTermination())
            assertEquals(1, exitCode)
            assertEquals(1, peer.server.exitCode)
        }

    @Test
    fun `exit before initialize closes the session with code 1`() =
        runTest {
            val peer = startPeer(backgroundScope) {}
            peer.notify(LspMethods.EXIT)
            assertNull(peer.server.awaitTermination())
            assertEquals(1, peer.server.exitCode)
            assertEquals(ServerState.Exited, peer.server.state.value)
        }

    @Test
    fun `initialized hook runs once after a successful initialize`() =
        runTest {
            var initialized = 0
            val peer = startPeer(backgroundScope) { onInitialized { initialized++ } }
            peer.notify(LspMethods.INITIALIZED, JsonObject(emptyMap()))
            peer.initialize()
            peer.notify(LspMethods.INITIALIZED, JsonObject(emptyMap()))
            peer.sync()
            assertEquals(1, initialized)
        }

    @Test
    fun `initialize hook can use the request work done progress`() =
        runTest {
            val peer =
                startPeer(backgroundScope) {
                    onInitialize {
                        workDoneProgress?.begin("Starting")
                        InitializeResult(ServerCapabilities())
                    }
                }
            peer.initialize(initializeParams(workDoneToken = Either.right("init"))).result()
            val begin = peer.notifications.receive()
            val end = peer.notifications.receive()
            assertEquals(LspMethods.PROGRESS, begin.method())
            assertEquals(json("""{"token":"init","value":{"kind":"begin","title":"Starting"}}"""), begin["params"])
            assertEquals(json("""{"token":"init","value":{"kind":"end"}}"""), end["params"])
            assertEquals(ServerState.Initialized, peer.server.state.value)
        }
}
