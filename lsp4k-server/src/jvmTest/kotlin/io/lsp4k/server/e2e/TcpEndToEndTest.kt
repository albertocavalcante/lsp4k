package io.lsp4k.server.e2e

import io.jsonrpc4k.transport.TcpListener
import io.jsonrpc4k.transport.TcpSocket
import io.lsp4k.client.LanguageClientConfig
import io.lsp4k.client.LanguageClientSession
import io.lsp4k.client.languageClient
import io.lsp4k.client.start
import io.lsp4k.protocol.ClientCapabilities
import io.lsp4k.protocol.CompletionItem
import io.lsp4k.protocol.CompletionParams
import io.lsp4k.protocol.CompletionResult
import io.lsp4k.protocol.ConfigurationItem
import io.lsp4k.protocol.ConfigurationParams
import io.lsp4k.protocol.DidOpenTextDocumentParams
import io.lsp4k.protocol.DocumentSymbolParams
import io.lsp4k.protocol.GeneralClientCapabilities
import io.lsp4k.protocol.Hover
import io.lsp4k.protocol.HoverContents
import io.lsp4k.protocol.HoverParams
import io.lsp4k.protocol.MarkupContent
import io.lsp4k.protocol.MarkupKind
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.PositionEncodingKind
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.protocol.TextDocumentItem
import io.lsp4k.server.LanguageServer
import io.lsp4k.server.ServerState
import io.lsp4k.server.TextDocuments
import io.lsp4k.server.languageServer
import io.lsp4k.server.start
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

private const val URI = "file:///e2e.txt"

// The e-acute takes two UTF-8 bytes, so position 2 is the equals sign in UTF-8 but the digit in UTF-16.
private const val SOURCE = "é=1"

private const val SECTION = "lsp4k"

/** What the server under test records, so the test can observe handler side effects. */
private class ServerProbe {
    val documents = CompletableDeferred<TextDocuments>()
    val slowStarted = CompletableDeferred<Unit>()
    val slowCancelled = CompletableDeferred<Unit>()

    val config =
        languageServer {
            serverInfo("e2e-server", "1.0.0")
            negotiatePositionEncoding(PositionEncodingKind.UTF8, PositionEncodingKind.UTF16)
            capabilities {
                hoverProvider = true
                documentSymbolProvider = true
            }
            onInitialized { documents.complete(TextDocuments(server.positionEncoding)) }
            textDocument {
                didOpen { documents.await().didOpen(it) }
                hover { params -> hoverAt(params) }
                completion { _ ->
                    val values = client.workspace.configuration(ConfigurationParams(listOf(ConfigurationItem(section = SECTION))))
                    CompletionResult.Items(listOf(CompletionItem(label = values.single().jsonPrimitive.content)))
                }
                documentSymbol {
                    slowStarted.complete(Unit)
                    try {
                        awaitCancellation()
                    } catch (cancelled: CancellationException) {
                        slowCancelled.complete(Unit)
                        throw cancelled
                    }
                }
            }
        }

    private suspend fun hoverAt(params: HoverParams): Hover? {
        val document = documents.await().get(params.textDocument.uri) ?: return null
        val character = document.text[document.offsetAt(params.position)]
        return Hover(HoverContents.Markup(MarkupContent(MarkupKind.PlainText, "char=$character")))
    }
}

private fun clientConfig(): LanguageClientConfig =
    languageClient {
        clientInfo("e2e-client", "1.0.0")
        capabilities(
            ClientCapabilities(
                general = GeneralClientCapabilities(positionEncodings = listOf(PositionEncodingKind.UTF16, PositionEncodingKind.UTF8)),
            ),
        )
        workspace {
            configuration { params -> params.items.map { JsonPrimitive("configured-${it.section}") } }
        }
    }

class TcpEndToEndTest {
    @Test
    fun `a full session runs over a loopback tcp transport`() =
        runBlocking(Dispatchers.Default) {
            withTimeout(30.seconds) {
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                val listener = TcpListener.bind("127.0.0.1", 0)
                val accepted = async { listener.accept() }
                val clientTransport = TcpSocket.connect("127.0.0.1", listener.port)
                val serverTransport = accepted.await()
                try {
                    val probe = ServerProbe()
                    val server = probe.config.start(serverTransport, scope)
                    val session = clientConfig().start(clientTransport, scope)
                    exercise(probe, server, session)
                } finally {
                    scope.cancel()
                    clientTransport.close()
                    serverTransport.close()
                    listener.close()
                }
            }
        }

    private suspend fun exercise(
        probe: ServerProbe,
        server: LanguageServer,
        session: LanguageClientSession,
    ) {
        coroutineScope {
            val initialized = session.initialize()
            assertEquals(PositionEncodingKind.UTF8, initialized.capabilities.positionEncoding)
            assertEquals("e2e-server", initialized.serverInfo?.name)

            session.server.textDocument.didOpen(DidOpenTextDocumentParams(TextDocumentItem(URI, "text", 1, SOURCE)))
            val hover = session.server.textDocument.hover(HoverParams(TextDocumentIdentifier(URI), Position(0, 2)))
            assertEquals(Hover(HoverContents.Markup(MarkupContent(MarkupKind.PlainText, "char=="))), hover)

            // The completion handler asks the client for workspace/configuration while the client awaits the completion.
            val completion = session.server.textDocument.completion(CompletionParams(TextDocumentIdentifier(URI), Position(0, 0)))
            assertEquals(CompletionResult.Items(listOf(CompletionItem(label = "configured-$SECTION"))), completion)

            // Cancelling the awaiting call sends $/cancelRequest, which cancels the server handler.
            val slow = async { session.server.textDocument.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(URI))) }
            probe.slowStarted.await()
            slow.cancelAndJoin()
            probe.slowCancelled.await()

            // The session keeps working after the cancellation.
            assertNotNull(session.server.textDocument.hover(HoverParams(TextDocumentIdentifier(URI), Position(0, 0))))

            session.shutdown()
            server.awaitTermination()
            assertEquals(0, server.exitCode)
            assertEquals(ServerState.Exited, server.state.value)
        }
    }
}
