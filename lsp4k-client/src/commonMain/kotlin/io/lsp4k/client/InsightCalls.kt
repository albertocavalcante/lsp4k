package io.lsp4k.client

import io.lsp4k.protocol.DocumentDiagnosticParams
import io.lsp4k.protocol.DocumentDiagnosticReport
import io.lsp4k.protocol.InlayHint
import io.lsp4k.protocol.InlayHintParams
import io.lsp4k.protocol.InlineCompletionParams
import io.lsp4k.protocol.InlineCompletionResult
import io.lsp4k.protocol.InlineValue
import io.lsp4k.protocol.InlineValueParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.Moniker
import io.lsp4k.protocol.MonikerParams
import io.lsp4k.protocol.SemanticTokens
import io.lsp4k.protocol.SemanticTokensDeltaParams
import io.lsp4k.protocol.SemanticTokensDeltaResult
import io.lsp4k.protocol.SemanticTokensParams
import io.lsp4k.protocol.SemanticTokensRangeParams

/**
 * Semantic tokens, inlay hints, diagnostics, monikers, inline values and inline completion requests.
 */
public class InsightCalls internal constructor(
    private val caller: ServerCaller,
) {
    /**
     * Request semantic tokens for a full document.
     */
    public suspend fun semanticTokensFull(params: SemanticTokensParams): SemanticTokens? =
        caller.request(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL, params)

    /**
     * Request semantic tokens delta for a full document.
     */
    public suspend fun semanticTokensFullDelta(params: SemanticTokensDeltaParams): SemanticTokensDeltaResult? =
        caller.request(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL_DELTA, params)

    /**
     * Request semantic tokens for a range.
     */
    public suspend fun semanticTokensRange(params: SemanticTokensRangeParams): SemanticTokens? =
        caller.request(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_RANGE, params)

    /**
     * Request inlay hints for a given text document range.
     */
    public suspend fun inlayHint(params: InlayHintParams): List<InlayHint>? = caller.request(LspMethods.TEXT_DOCUMENT_INLAY_HINT, params)

    /**
     * Resolve additional information for a given inlay hint.
     */
    public suspend fun inlayHintResolve(hint: InlayHint): InlayHint? = caller.request(LspMethods.INLAY_HINT_RESOLVE, hint)

    /**
     * Request pull diagnostics for a given text document.
     * @since 3.17.0
     */
    public suspend fun diagnostic(params: DocumentDiagnosticParams): DocumentDiagnosticReport? =
        caller.request(LspMethods.TEXT_DOCUMENT_DIAGNOSTIC, params)

    /**
     * Request monikers for a given text document position.
     */
    public suspend fun moniker(params: MonikerParams): List<Moniker>? = caller.request(LspMethods.TEXT_DOCUMENT_MONIKER, params)

    /**
     * Request inline values for a given text document.
     * @since 3.17.0
     */
    public suspend fun inlineValue(params: InlineValueParams): List<InlineValue>? =
        caller.request(LspMethods.TEXT_DOCUMENT_INLINE_VALUE, params)

    /**
     * Request inline completions for a given text document position.
     * @since 3.18.0
     */
    public suspend fun inlineCompletion(params: InlineCompletionParams): InlineCompletionResult? =
        caller.request(LspMethods.TEXT_DOCUMENT_INLINE_COMPLETION, params)
}
