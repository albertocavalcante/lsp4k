package io.lsp4k.client

import io.lsp4k.protocol.DocumentDiagnosticParams
import io.lsp4k.protocol.InlayHint
import io.lsp4k.protocol.InlayHintParams
import io.lsp4k.protocol.InlineCompletionParams
import io.lsp4k.protocol.InlineValueParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.MonikerParams
import io.lsp4k.protocol.SemanticTokensDeltaParams
import io.lsp4k.protocol.SemanticTokensParams
import io.lsp4k.protocol.SemanticTokensRangeParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal val insightCases: List<CallCase> =
    listOf(
        requestCase<SemanticTokensParams>(
            LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL,
            """{$DOC}""",
        ) { insights.semanticTokensFull(it) },
        requestCase<SemanticTokensDeltaParams>(
            LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL_DELTA,
            """{$DOC,"previousResultId":"1"}""",
        ) { insights.semanticTokensFullDelta(it) },
        requestCase<SemanticTokensRangeParams>(
            LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_RANGE,
            """{$DOC,$RNG}""",
        ) { insights.semanticTokensRange(it) },
        requestCase<InlayHintParams>(
            LspMethods.TEXT_DOCUMENT_INLAY_HINT,
            """{$DOC,$RNG}""",
        ) { insights.inlayHint(it) },
        requestCase<InlayHint>(
            LspMethods.INLAY_HINT_RESOLVE,
            """{$POS,"label":"x"}""",
        ) { insights.inlayHintResolve(it) },
        requestCase<DocumentDiagnosticParams>(
            LspMethods.TEXT_DOCUMENT_DIAGNOSTIC,
            """{$DOC}""",
        ) { insights.diagnostic(it) },
        requestCase<MonikerParams>(
            LspMethods.TEXT_DOCUMENT_MONIKER,
            """{$DOC,$POS}""",
        ) { insights.moniker(it) },
        requestCase<InlineValueParams>(
            LspMethods.TEXT_DOCUMENT_INLINE_VALUE,
            """{$DOC,$RNG,"context":{"frameId":1,"stoppedLocation":$ONE_RANGE}}""",
        ) { insights.inlineValue(it) },
        requestCase<InlineCompletionParams>(
            LspMethods.TEXT_DOCUMENT_INLINE_COMPLETION,
            """{$DOC,$POS,"context":{"triggerKind":1}}""",
        ) { insights.inlineCompletion(it) },
    )

class InsightCallsTest {
    @Test
    fun `every call reaches the server with its method and params`() =
        runTest {
            runCases(backgroundScope, insightCases)
        }
}
