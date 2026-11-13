package io.lsp4k.server

import io.jsonrpc4k.core.ResponseError
import io.lsp4k.protocol.DidChangeTextDocumentParams
import io.lsp4k.protocol.DidCloseTextDocumentParams
import io.lsp4k.protocol.DidOpenTextDocumentParams
import io.lsp4k.protocol.ExecuteCommandParams
import io.lsp4k.protocol.Hover
import io.lsp4k.protocol.HoverContents
import io.lsp4k.protocol.HoverParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.MarkupContent
import io.lsp4k.protocol.MarkupKind
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.TextDocumentContentChangeEvent
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.protocol.TextDocumentItem
import io.lsp4k.protocol.VersionedTextDocumentIdentifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcurrentDispatchTest {
    private val uri = "file:///a.txt"
    private val hoverParams = HoverParams(TextDocumentIdentifier(uri), Position(0, 0))

    @Test
    fun `a cancelled long running request is answered RequestCancelled while another completes`() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val handlerCancelled = CompletableDeferred<Unit>()
            val peer =
                startPeer(backgroundScope) {
                    textDocument {
                        hover {
                            started.complete(Unit)
                            try {
                                awaitCancellation()
                            } catch (cancelled: CancellationException) {
                                handlerCancelled.complete(Unit)
                                throw cancelled
                            }
                        }
                    }
                    workspace { executeCommand { JsonPrimitive("done") } }
                }
            peer.initialize()

            val (slowId, slow) = peer.begin(LspMethods.TEXT_DOCUMENT_HOVER, wire(hoverParams))
            started.await()
            val fast = peer.request(LspMethods.WORKSPACE_EXECUTE_COMMAND, wire(ExecuteCommandParams("app.fast")))
            assertEquals(JsonPrimitive("done"), fast.result())
            assertEquals(false, slow.isCompleted)

            peer.cancel(slowId)
            assertEquals(ResponseError.REQUEST_CANCELLED, slow.await().errorCode())
            handlerCancelled.await()
        }

    @Test
    fun `notifications run in order and before a request that follows them`() =
        runTest {
            val log = mutableListOf<String>()
            val peer =
                startPeer(backgroundScope) {
                    textDocument {
                        didOpen {
                            delay(100)
                            log += "open"
                        }
                        didChange { log += "change" }
                        didClose { log += "close" }
                        hover {
                            log += "hover"
                            Hover(HoverContents.Markup(MarkupContent(MarkupKind.PlainText, log.joinToString(","))))
                        }
                    }
                }
            peer.initialize()

            peer.send(LspMethods.TEXT_DOCUMENT_DID_OPEN, DidOpenTextDocumentParams(TextDocumentItem(uri, "text", 1, "a")))
            val edit = TextDocumentContentChangeEvent(text = "b")
            val change = DidChangeTextDocumentParams(VersionedTextDocumentIdentifier(uri, 2), listOf(edit))
            peer.send(LspMethods.TEXT_DOCUMENT_DID_CHANGE, change)
            peer.send(LspMethods.TEXT_DOCUMENT_DID_CLOSE, DidCloseTextDocumentParams(TextDocumentIdentifier(uri)))
            val hover = peer.call<HoverParams, Hover>(LspMethods.TEXT_DOCUMENT_HOVER, hoverParams)

            val seen = (hover?.contents as HoverContents.Markup).content.value
            assertEquals("open,change,close,hover", seen)
            assertEquals(listOf("open", "change", "close", "hover"), log)
        }
}
