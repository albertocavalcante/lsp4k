import io.jsonrpc4k.core.JsonRpcCodec
import io.jsonrpc4k.transport.Transport
import io.lsp4k.client.languageClient
import io.lsp4k.client.start
import io.lsp4k.protocol.ClientCapabilities
import io.lsp4k.protocol.InitializeParams
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.Maybe
import io.lsp4k.protocol.ServerCapabilities
import io.lsp4k.protocol.WorkspaceFolder
import io.lsp4k.server.languageServer
import io.lsp4k.server.start
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonPrimitive

fun main() =
    runBlocking {
        check(System.getProperty("java.specification.version") == "17")
        check(JsonRpcCodec.Default.parseContentLength("Content-Length: 1") == 1)
        Class.forName("kotlinx.collections.immutable.ExtensionsKt")
        withTimeout(15_000) {
            val toClient = Channel<ByteArray>(Channel.UNLIMITED)
            val toServer = Channel<ByteArray>(Channel.UNLIMITED)
            val clientTransport = FixtureTransport(toClient, toServer)
            val serverTransport = FixtureTransport(toServer, toClient)
            val server =
                languageServer {
                    onInitialize { params ->
                        check(
                            params.workspaceFolders.valueOrNull
                                ?.single()
                                ?.name == "fixture",
                        )
                        check(params.initializationOptions == JsonPrimitive("options"))
                        InitializeResult(ServerCapabilities())
                    }
                }.start(serverTransport, this)
            val client = languageClient {}.start(clientTransport, this)
            try {
                client.initialize(
                    InitializeParams(
                        processId = null,
                        rootUri = "file:///fixture",
                        capabilities = ClientCapabilities(),
                        initializationOptions = JsonPrimitive("options"),
                        workspaceFolders = Maybe.Of(listOf(WorkspaceFolder("file:///fixture", "fixture"))),
                    ),
                )
                client.shutdown()
                check(client.awaitTermination() == null)
                check(server.awaitTermination() == null)
            } finally {
                client.close()
                server.close()
                clientTransport.close()
                serverTransport.close()
            }
        }
    }

private class FixtureTransport(
    private val inbound: Channel<ByteArray>,
    private val outbound: Channel<ByteArray>,
) : Transport {
    override val incoming: Flow<ByteArray> = inbound.receiveAsFlow()
    override val isConnected: Boolean = true

    override suspend fun send(data: ByteArray) {
        outbound.send(data)
    }

    override suspend fun close() {
        inbound.close()
    }
}
