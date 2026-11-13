package io.lsp4k.server

import io.lsp4k.protocol.DocumentDiagnosticParams
import io.lsp4k.protocol.DocumentDiagnosticReport
import io.lsp4k.protocol.DocumentDiagnosticReportProgress
import io.lsp4k.protocol.DocumentDiagnosticReportProgressSerializer
import io.lsp4k.protocol.InlayHint
import io.lsp4k.protocol.InlayHintParams
import io.lsp4k.protocol.InlineCompletionItem
import io.lsp4k.protocol.InlineCompletionParams
import io.lsp4k.protocol.InlineCompletionResult
import io.lsp4k.protocol.InlineValue
import io.lsp4k.protocol.InlineValueParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.Moniker
import io.lsp4k.protocol.MonikerParams
import io.lsp4k.protocol.SemanticTokens
import io.lsp4k.protocol.SemanticTokensDeltaParams
import io.lsp4k.protocol.SemanticTokensDeltaPartial
import io.lsp4k.protocol.SemanticTokensDeltaPartialSerializer
import io.lsp4k.protocol.SemanticTokensDeltaResult
import io.lsp4k.protocol.SemanticTokensParams
import io.lsp4k.protocol.SemanticTokensPartialResult
import io.lsp4k.protocol.SemanticTokensRangeParams
import kotlinx.serialization.serializer

/**
 * Handlers for semantic tokens, inlay hints, pull diagnostics, monikers, inline values and inline completion.
 */
@LspServerDsl
public class InsightHandlersBuilder internal constructor(
    registry: HandlerRegistry,
) : HandlersBuilder(registry) {
    /** Handles `textDocument/semanticTokens/full` requests. */
    public fun semanticTokensFull(
        handler: suspend PartialResultContext<SemanticTokensPartialResult>.(SemanticTokensParams) -> SemanticTokens?,
    ) {
        partialRequest(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL, SemanticTokensPartialResult.serializer(), handler)
    }

    /**
     * Handles `textDocument/semanticTokens/full/delta` requests. Partial results are either full token chunks
     * (`Either.Left`) or edit chunks (`Either.Right`).
     */
    public fun semanticTokensFullDelta(
        handler: suspend PartialResultContext<SemanticTokensDeltaPartial>.(SemanticTokensDeltaParams) -> SemanticTokensDeltaResult?,
    ) {
        partialRequest(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL_DELTA, SemanticTokensDeltaPartialSerializer, handler)
    }

    /** Handles `textDocument/semanticTokens/range` requests. */
    public fun semanticTokensRange(
        handler: suspend PartialResultContext<SemanticTokensPartialResult>.(SemanticTokensRangeParams) -> SemanticTokens?,
    ) {
        partialRequest(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_RANGE, SemanticTokensPartialResult.serializer(), handler)
    }

    /** Handles `textDocument/inlayHint` requests. */
    public fun inlayHint(handler: suspend PartialResultContext<List<InlayHint>>.(InlayHintParams) -> List<InlayHint>?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_INLAY_HINT, serializer<List<InlayHint>>(), handler)
    }

    /** Handles `inlayHint/resolve` requests. */
    public fun inlayHintResolve(handler: suspend RequestContext.(InlayHint) -> InlayHint) {
        request(LspMethods.INLAY_HINT_RESOLVE, handler)
    }

    /**
     * Handles `textDocument/diagnostic` requests. The first partial result must be a [DocumentDiagnosticReport]
     * (`Either.Left`); later chunks report related documents (`Either.Right`).
     */
    public fun diagnostic(
        handler: suspend PartialResultContext<DocumentDiagnosticReportProgress>.(DocumentDiagnosticParams) -> DocumentDiagnosticReport,
    ) {
        partialRequest(LspMethods.TEXT_DOCUMENT_DIAGNOSTIC, DocumentDiagnosticReportProgressSerializer, handler)
    }

    /** Handles `textDocument/moniker` requests. */
    public fun moniker(handler: suspend PartialResultContext<List<Moniker>>.(MonikerParams) -> List<Moniker>?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_MONIKER, serializer<List<Moniker>>(), handler)
    }

    /** Handles `textDocument/inlineValue` requests. */
    public fun inlineValue(handler: suspend PartialResultContext<List<InlineValue>>.(InlineValueParams) -> List<InlineValue>?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_INLINE_VALUE, serializer<List<InlineValue>>(), handler)
    }

    /** Handles `textDocument/inlineCompletion` requests; partial results are lists of [InlineCompletionItem]. */
    public fun inlineCompletion(
        handler: suspend PartialResultContext<List<InlineCompletionItem>>.(InlineCompletionParams) -> InlineCompletionResult?,
    ) {
        partialRequest(LspMethods.TEXT_DOCUMENT_INLINE_COMPLETION, serializer<List<InlineCompletionItem>>(), handler)
    }
}
