package io.lsp4k.server

import io.lsp4k.protocol.DocumentDiagnosticParams
import io.lsp4k.protocol.DocumentDiagnosticReport
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.InlayHint
import io.lsp4k.protocol.InlayHintParams
import io.lsp4k.protocol.InlineCompletionContext
import io.lsp4k.protocol.InlineCompletionItem
import io.lsp4k.protocol.InlineCompletionParams
import io.lsp4k.protocol.InlineCompletionResult
import io.lsp4k.protocol.InlineCompletionTriggerKind
import io.lsp4k.protocol.InlineValue
import io.lsp4k.protocol.InlineValueContext
import io.lsp4k.protocol.InlineValueParams
import io.lsp4k.protocol.InlineValueText
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.Moniker
import io.lsp4k.protocol.MonikerParams
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.Range
import io.lsp4k.protocol.RelatedFullDocumentDiagnosticReport
import io.lsp4k.protocol.SemanticTokens
import io.lsp4k.protocol.SemanticTokensDelta
import io.lsp4k.protocol.SemanticTokensDeltaParams
import io.lsp4k.protocol.SemanticTokensDeltaResult
import io.lsp4k.protocol.SemanticTokensEdit
import io.lsp4k.protocol.SemanticTokensParams
import io.lsp4k.protocol.SemanticTokensRangeParams
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.protocol.UniquenessLevel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InsightHandlersTest {
    private val document = TextDocumentIdentifier("file:///insight.kt")
    private val position = Position(5, 2)
    private val range = Range(Position(5, 0), Position(6, 0))

    @Test
    fun `insight requests reach typed handlers and encode their results`() =
        runTest {
            val received = mutableListOf<Any>()
            val hint = InlayHint(position, Either.left("hint"))
            val delta = SemanticTokensDeltaResult.Delta(SemanticTokensDelta(edits = listOf(SemanticTokensEdit(0, 1))))
            val report = DocumentDiagnosticReport.Full(RelatedFullDocumentDiagnosticReport(items = emptyList()))
            val monikerValue = Moniker(scheme = "kotlin", identifier = "insight.value", unique = UniquenessLevel.Document)
            val inlineResult = InlineCompletionResult.Items(listOf(InlineCompletionItem(Either.left("value"))))
            val peer =
                startPeer(backgroundScope) {
                    insights {
                        semanticTokensFull { SemanticTokens(resultId = "1", data = listOf(0, 1, 2, 3, 4)) }
                        semanticTokensFullDelta {
                            received += it
                            delta
                        }
                        semanticTokensRange { SemanticTokens(data = emptyList()) }
                        inlayHint { listOf(hint) }
                        inlayHintResolve { it.copy(paddingLeft = true) }
                        diagnostic {
                            received += it
                            report
                        }
                        moniker { listOf(monikerValue) }
                        inlineValue { listOf(InlineValueText(range, "42")) }
                        inlineCompletion {
                            received += it
                            inlineResult
                        }
                    }
                }
            peer.initialize()

            val fullParams = SemanticTokensParams(document)
            val full = peer.call<SemanticTokensParams, SemanticTokens>(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL, fullParams)
            assertEquals(SemanticTokens(resultId = "1", data = listOf(0, 1, 2, 3, 4)), full)
            val deltaParams = SemanticTokensDeltaParams(document, previousResultId = "1")
            val deltaMethod = LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL_DELTA
            assertEquals(delta, peer.call<SemanticTokensDeltaParams, SemanticTokensDeltaResult>(deltaMethod, deltaParams))
            val rangeParams = SemanticTokensRangeParams(document, range)
            val rangeMethod = LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_RANGE
            val rangeTokens = peer.call<SemanticTokensRangeParams, SemanticTokens>(rangeMethod, rangeParams)
            assertEquals(SemanticTokens(data = emptyList()), rangeTokens)
            val hints = peer.call<InlayHintParams, List<InlayHint>>(LspMethods.TEXT_DOCUMENT_INLAY_HINT, InlayHintParams(document, range))
            assertEquals(listOf(hint), hints)
            assertEquals(hint.copy(paddingLeft = true), peer.call<InlayHint, InlayHint>(LspMethods.INLAY_HINT_RESOLVE, hint))
            val diagnosticParams = DocumentDiagnosticParams(document)
            val diagnosticMethod = LspMethods.TEXT_DOCUMENT_DIAGNOSTIC
            val diagnosed = peer.call<DocumentDiagnosticParams, DocumentDiagnosticReport>(diagnosticMethod, diagnosticParams)
            assertEquals(report, diagnosed)
            val monikerParams = MonikerParams(document, position)
            assertEquals(listOf(monikerValue), peer.call<MonikerParams, List<Moniker>>(LspMethods.TEXT_DOCUMENT_MONIKER, monikerParams))
            val inlineValueParams = InlineValueParams(document, range, InlineValueContext(frameId = 1, stoppedLocation = range))
            val values = peer.call<InlineValueParams, List<InlineValue>>(LspMethods.TEXT_DOCUMENT_INLINE_VALUE, inlineValueParams)
            assertEquals(listOf<InlineValue>(InlineValueText(range, "42")), values)
            val completionParams = InlineCompletionParams(document, position, InlineCompletionContext(InlineCompletionTriggerKind.Invoked))
            val completionMethod = LspMethods.TEXT_DOCUMENT_INLINE_COMPLETION
            assertEquals(inlineResult, peer.call<InlineCompletionParams, InlineCompletionResult>(completionMethod, completionParams))
            assertEquals(listOf<Any>(deltaParams, diagnosticParams, completionParams), received)
        }
}
