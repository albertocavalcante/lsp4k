package io.lsp4k.server

import io.jsonrpc4k.core.JsonRpcException
import io.jsonrpc4k.core.ResponseError
import io.lsp4k.protocol.DefinitionParams
import io.lsp4k.protocol.DefinitionResult
import io.lsp4k.protocol.DocumentDiagnosticParams
import io.lsp4k.protocol.DocumentDiagnosticReport
import io.lsp4k.protocol.DocumentDiagnosticReportPartialResult
import io.lsp4k.protocol.DocumentDiagnosticReportProgressSerializer
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.FullDocumentDiagnosticReport
import io.lsp4k.protocol.HoverParams
import io.lsp4k.protocol.Location
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.ProgressToken
import io.lsp4k.protocol.Range
import io.lsp4k.protocol.ReferenceContext
import io.lsp4k.protocol.ReferenceParams
import io.lsp4k.protocol.RelatedFullDocumentDiagnosticReport
import io.lsp4k.protocol.SemanticTokensDelta
import io.lsp4k.protocol.SemanticTokensDeltaParams
import io.lsp4k.protocol.SemanticTokensDeltaPartialResult
import io.lsp4k.protocol.SemanticTokensDeltaResult
import io.lsp4k.protocol.SemanticTokensEdit
import io.lsp4k.protocol.SemanticTokensPartialResult
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.protocol.WorkDoneProgressCancelParams
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressTest {
    private val document = TextDocumentIdentifier("file:///progress.kt")
    private val position = Position(0, 0)
    private val range = Range(position, Position(0, 4))
    private val first = Location(document.uri, range)
    private val second = Location("file:///other.kt", range)

    @Test
    fun `request work done progress uses the client token and is ended when the handler returns`() =
        runTest {
            var definitionProgress: WorkDoneProgress? = null
            val peer =
                startPeer(backgroundScope) {
                    textDocument {
                        hover {
                            val progress = checkNotNull(workDoneProgress)
                            progress.begin("Hovering", cancellable = false, percentage = 0)
                            progress.report(message = "half", percentage = 50)
                            null
                        }
                        definition {
                            definitionProgress = workDoneProgress
                            null
                        }
                    }
                }
            peer.initialize()

            val hover = HoverParams(document, position, workDoneToken = Either.left(7))
            assertEquals(JsonNull, peer.request(LspMethods.TEXT_DOCUMENT_HOVER, wire(hover)).result())
            val begin = """{"token":7,"value":{"kind":"begin","title":"Hovering","cancellable":false,"percentage":0}}"""
            assertMessage(peer.notifications.receive(), LspMethods.PROGRESS, begin)
            val report = """{"token":7,"value":{"kind":"report","message":"half","percentage":50}}"""
            assertMessage(peer.notifications.receive(), LspMethods.PROGRESS, report)
            assertMessage(peer.notifications.receive(), LspMethods.PROGRESS, """{"token":7,"value":{"kind":"end"}}""")

            val definition = DefinitionParams(document, position)
            assertNull(peer.call<DefinitionParams, DefinitionResult>(LspMethods.TEXT_DOCUMENT_DEFINITION, definition))
            assertNull(definitionProgress)
            assertTrue(peer.notifications.tryReceive().isFailure)
        }

    @Test
    fun `partial results stream through progress only when the client sent a token`() =
        runTest {
            val sent = mutableListOf<Boolean>()
            var saved: PartialResultContext<List<Location>>? = null
            val peer =
                startPeer(backgroundScope) {
                    textDocument {
                        references {
                            saved = this
                            sent += sendPartialResult(listOf(first))
                            sent += sendPartialResult(listOf(second))
                            if (partialResultsSent) emptyList() else listOf(first, second)
                        }
                    }
                }
            peer.initialize()

            val streamed = ReferenceParams(document, position, ReferenceContext(false), partialResultToken = Either.right("refs"))
            assertEquals(JsonArray(emptyList()), peer.request(LspMethods.TEXT_DOCUMENT_REFERENCES, wire(streamed)).result())
            for (expected in listOf(first, second)) {
                val chunk = peer.notifications.receive()
                assertEquals(LspMethods.PROGRESS, chunk.method())
                assertEquals(JsonPrimitive("refs"), chunk.params()["token"])
                assertEquals(wire(listOf(expected)), chunk.params()["value"])
            }
            val late = checkNotNull(saved)
            assertFailsWith<IllegalStateException> { late.sendPartialResult(listOf(first)) }

            val plain = ReferenceParams(document, position, ReferenceContext(false))
            val all = peer.call<ReferenceParams, List<Location>>(LspMethods.TEXT_DOCUMENT_REFERENCES, plain)
            assertEquals(listOf(first, second), all)
            assertEquals(listOf(true, true, false, false), sent)
            assertTrue(peer.notifications.tryReceive().isFailure)
        }

    @Test
    fun `union partial results use the spec chunk shapes`() =
        runTest {
            val report = DocumentDiagnosticReport.Full(RelatedFullDocumentDiagnosticReport(items = emptyList()))
            val relatedReport = FullDocumentDiagnosticReport(items = emptyList())
            val related = DocumentDiagnosticReportPartialResult(mapOf(second.uri to Either.left(relatedReport)))
            val edits = SemanticTokensDeltaPartialResult(edits = listOf(SemanticTokensEdit(start = 0, deleteCount = 1)))
            val peer =
                startPeer(backgroundScope) {
                    textDocument {
                        definition {
                            sendPartialResult(DefinitionResult.Single(first))
                            DefinitionResult.Locations(emptyList())
                        }
                    }
                    insights {
                        semanticTokensFullDelta {
                            sendPartialResult(Either.left(SemanticTokensPartialResult(data = listOf(1, 2))))
                            sendPartialResult(Either.right(edits))
                            SemanticTokensDeltaResult.Delta(SemanticTokensDelta(edits = emptyList()))
                        }
                        diagnostic {
                            sendPartialResult(Either.left(report))
                            sendPartialResult(Either.right(related))
                            report
                        }
                    }
                }
            peer.initialize()
            val token = Either.right("chunks")

            val definitionParams = DefinitionParams(document, position, partialResultToken = token)
            peer.request(LspMethods.TEXT_DOCUMENT_DEFINITION, wire(definitionParams)).result()
            assertEquals(wire(listOf(first)), peer.notifications.receive().params()["value"])

            val deltaParams = SemanticTokensDeltaParams(document, previousResultId = "1", partialResultToken = token)
            peer.request(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL_DELTA, wire(deltaParams)).result()
            assertEquals(json("""{"data":[1,2]}"""), peer.notifications.receive().params()["value"])
            assertEquals(json("""{"edits":[{"start":0,"deleteCount":1}]}"""), peer.notifications.receive().params()["value"])

            val diagnosticParams = DocumentDiagnosticParams(document, partialResultToken = token)
            peer.request(LspMethods.TEXT_DOCUMENT_DIAGNOSTIC, wire(diagnosticParams)).result()
            val reportChunk = testJson.encodeToJsonElement(DocumentDiagnosticReportProgressSerializer, Either.left(report))
            assertEquals(reportChunk, peer.notifications.receive().params()["value"])
            val relatedChunk = json("""{"relatedDocuments":{"file:///other.kt":{"kind":"full","items":[]}}}""")
            assertEquals(relatedChunk, peer.notifications.receive().params()["value"])
        }

    @Test
    fun `server initiated progress is created then cancelled by the client and ended`() =
        runTest {
            val cancelled = CompletableDeferred<ProgressToken>()
            val peer = startPeer(backgroundScope) { window { workDoneProgressCancel { cancelled.complete(it.token) } } }
            peer.initialize()
            val window = peer.server.client.window

            val created = json("""{"token":"lsp4k-progress-1"}""")
            val progress = peer.answer(this, LspMethods.WINDOW_WORK_DONE_PROGRESS_CREATE, created) { window.createWorkDoneProgress() }
            assertEquals(Either.right("lsp4k-progress-1"), progress.token)
            assertFailsWith<IllegalStateException> { progress.report(percentage = 1) }
            progress.begin("Indexing", cancellable = true)
            val begin = """{"token":"lsp4k-progress-1","value":{"kind":"begin","title":"Indexing","cancellable":true}}"""
            assertMessage(peer.notifications.receive(), LspMethods.PROGRESS, begin)
            assertFailsWith<IllegalStateException> { progress.begin("Indexing again") }
            assertFailsWith<IllegalArgumentException> { progress.report(percentage = 101) }
            assertFalse(progress.isCancelled)

            peer.send(LspMethods.WINDOW_WORK_DONE_PROGRESS_CANCEL, WorkDoneProgressCancelParams(progress.token))
            progress.awaitCancelled()
            assertTrue(progress.isCancelled)
            assertEquals(progress.token, cancelled.await())

            progress.end("stopped")
            val end = """{"token":"lsp4k-progress-1","value":{"kind":"end","message":"stopped"}}"""
            assertMessage(peer.notifications.receive(), LspMethods.PROGRESS, end)
            assertFailsWith<IllegalStateException> { progress.end() }
        }

    @Test
    fun `a rejected progress creation throws and keeps the token unused`() =
        runTest {
            val peer = startPeer(backgroundScope) {}
            peer.initialize()
            val window = peer.server.client.window
            val creation = async { runCatching { window.createWorkDoneProgress(Either.left(5)) } }
            val request = peer.requests.receive()
            assertEquals(json("""{"token":5}"""), request["params"])
            peer.respondError(request, ResponseError.INTERNAL_ERROR, "no progress support")
            val failure = creation.await().exceptionOrNull()
            assertTrue(failure is JsonRpcException, "unexpected $failure")
            assertTrue(peer.notifications.tryReceive().isFailure)
        }
}
