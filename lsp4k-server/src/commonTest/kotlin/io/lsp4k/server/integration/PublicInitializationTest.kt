package io.lsp4k.server.integration

import io.jsonrpc4k.core.JsonRpcCodec
import io.jsonrpc4k.core.JsonRpcException
import io.jsonrpc4k.core.NotificationMessage
import io.jsonrpc4k.core.RequestId
import io.jsonrpc4k.core.RequestMessage
import io.jsonrpc4k.core.ResponseError
import io.jsonrpc4k.transport.Transport
import io.lsp4k.client.languageClient
import io.lsp4k.client.start
import io.lsp4k.protocol.ClientCapabilities
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.HoverParams
import io.lsp4k.protocol.InitializeParams
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.ServerCapabilities
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.protocol.TraceValue
import io.lsp4k.protocol.WorkspaceFolder
import io.lsp4k.server.languageServer
import io.lsp4k.server.start
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PublicInitializationTest {
    @Test
    fun `explicit initialization reaches server and returns negotiated result`() =
        runTest {
            val (clientTransport, serverTransport) = transports()
            var received: InitializeParams? = null
            val negotiated = InitializeResult(ServerCapabilities(hoverProvider = Either.left(true)))
            val server = languageServer {
                onInitialize { params ->
                    received = params
                    negotiated
                }
            }.start(serverTransport, backgroundScope)
            val client = languageClient {}.start(clientTransport, backgroundScope)
            val params =
                InitializeParams(
                    processId = 42,
                    rootUri = "file:///workspace",
                    initializationOptions = JsonPrimitive("settings"),
                    capabilities = ClientCapabilities(experimental = JsonPrimitive("client-feature")),
                    trace = TraceValue.Verbose,
                    workspaceFolders = listOf(WorkspaceFolder("file:///workspace", "workspace")),
                )

            assertEquals(negotiated, client.initialize(params))
            assertEquals(params, received)
            assertEquals(TraceValue.Verbose, server.traceLevel)
        }

    @Test
    fun `default initialization uses configured immutable capabilities and server response`() =
        runTest {
            val (clientTransport, serverTransport) = transports()
            val capabilities = ClientCapabilities(experimental = JsonPrimitive("feature"))
            var received: InitializeParams? = null
            languageServer {
                onInitialize { params ->
                    received = params
                    InitializeResult(ServerCapabilities())
                }
            }.start(serverTransport, backgroundScope)
            val client = languageClient {
                capabilities(capabilities)
                rootUri("file:///root")
            }.start(clientTransport, backgroundScope)

            assertEquals(InitializeResult(ServerCapabilities()), client.initialize())
            assertEquals(capabilities, received?.capabilities)
            assertEquals("file:///root", received?.rootUri)
        }

    @Test
    fun `server without a handler retains configured response`() =
        runTest {
            val (clientTransport, serverTransport) = transports()
            languageServer {
                serverInfo("example", "1")
                capabilities { hoverProvider = true }
            }.start(serverTransport, backgroundScope)
            val client = languageClient {}.start(clientTransport, backgroundScope)

            val result = client.initialize()
            assertEquals("example", result.serverInfo?.name)
            assertEquals(Either.left(true), result.capabilities.hoverProvider)
        }

    @Test
    fun `failed initialization cannot unlock requests and can be retried once`() =
        runTest {
            val (clientTransport, serverTransport) = transports()
            var attempts = 0
            languageServer {
                onInitialize {
                    attempts++
                    if (attempts == 1) error("startup failed")
                    InitializeResult(ServerCapabilities())
                }
                textDocument { hover { null } }
            }.start(serverTransport, backgroundScope)
            val client = languageClient {}.start(clientTransport, backgroundScope)
            val hover = HoverParams(TextDocumentIdentifier("file:///test"), Position(0, 0))

            clientTransport.send(JsonRpcCodec.Default.encode(NotificationMessage("initialized")))
            assertEquals(
                ResponseError.SERVER_NOT_INITIALIZED,
                assertFailsWith<JsonRpcException> { client.server.hover(hover) }.code,
            )
            assertFailsWith<JsonRpcException> { client.initialize() }
            clientTransport.send(JsonRpcCodec.Default.encode(NotificationMessage("initialized")))
            assertEquals(
                ResponseError.SERVER_NOT_INITIALIZED,
                assertFailsWith<JsonRpcException> { client.server.hover(hover) }.code,
            )
            client.initialize()
            assertEquals(null, client.server.hover(hover))
            assertEquals(ResponseError.INVALID_REQUEST, assertFailsWith<JsonRpcException> { client.initialize() }.code)
            assertEquals(2, attempts)
        }

    @Test
    fun `initialize after shutdown is rejected`() =
        runTest {
            val (clientTransport, serverTransport) = transports()
            languageServer {}.start(serverTransport, backgroundScope)
            val client = languageClient {}.start(clientTransport, backgroundScope)
            client.initialize()
            // Send shutdown without exit so the server stays available for the next request.
            clientTransport.send(JsonRpcCodec.Default.encode(RequestMessage(RequestId.of(99), "shutdown")))
            assertEquals(ResponseError.INVALID_REQUEST, assertFailsWith<JsonRpcException> { client.initialize() }.code)
        }
}

private fun transports(): Pair<Transport, Transport> {
    val toClient = Channel<ByteArray>(Channel.UNLIMITED)
    val toServer = Channel<ByteArray>(Channel.UNLIMITED)
    return TestTransport(toClient, toServer) to TestTransport(toServer, toClient)
}

private class TestTransport(
    incomingChannel: Channel<ByteArray>,
    private val outgoingChannel: Channel<ByteArray>,
) : Transport {
    override val incoming: Flow<ByteArray> = incomingChannel.receiveAsFlow()
    override val isConnected: Boolean = true

    override suspend fun send(data: ByteArray) {
        outgoingChannel.send(data)
    }

    override suspend fun close() {
        outgoingChannel.close()
    }
}
