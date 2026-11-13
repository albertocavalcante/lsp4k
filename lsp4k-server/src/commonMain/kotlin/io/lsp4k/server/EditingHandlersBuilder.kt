package io.lsp4k.server

import io.lsp4k.protocol.CodeAction
import io.lsp4k.protocol.CodeActionParams
import io.lsp4k.protocol.CodeLens
import io.lsp4k.protocol.CodeLensParams
import io.lsp4k.protocol.ColorInformation
import io.lsp4k.protocol.ColorPresentation
import io.lsp4k.protocol.ColorPresentationParams
import io.lsp4k.protocol.CommandOrCodeAction
import io.lsp4k.protocol.DocumentColorParams
import io.lsp4k.protocol.DocumentFormattingParams
import io.lsp4k.protocol.DocumentLink
import io.lsp4k.protocol.DocumentLinkParams
import io.lsp4k.protocol.DocumentOnTypeFormattingParams
import io.lsp4k.protocol.DocumentRangeFormattingParams
import io.lsp4k.protocol.DocumentRangesFormattingParams
import io.lsp4k.protocol.FoldingRange
import io.lsp4k.protocol.FoldingRangeParams
import io.lsp4k.protocol.LinkedEditingRangeParams
import io.lsp4k.protocol.LinkedEditingRanges
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.PrepareRenameParams
import io.lsp4k.protocol.PrepareRenameResult
import io.lsp4k.protocol.RenameParams
import io.lsp4k.protocol.SelectionRange
import io.lsp4k.protocol.SelectionRangeParams
import io.lsp4k.protocol.TextEdit
import io.lsp4k.protocol.WorkspaceEdit
import kotlinx.serialization.serializer

/**
 * Handlers for code actions, code lenses, document links, colors, formatting, rename and ranges.
 *
 * Handlers of requests that support partial results receive a [PartialResultContext] whose chunks have the
 * element type of the result array.
 */
@LspServerDsl
public class EditingHandlersBuilder internal constructor(
    registry: HandlerRegistry,
) : HandlersBuilder(registry) {
    /** Handles `textDocument/codeAction` requests. */
    public fun codeAction(
        handler: suspend PartialResultContext<List<CommandOrCodeAction>>.(CodeActionParams) -> List<CommandOrCodeAction>?,
    ) {
        partialRequest(LspMethods.TEXT_DOCUMENT_CODE_ACTION, serializer<List<CommandOrCodeAction>>(), handler)
    }

    /** Handles `codeAction/resolve` requests. */
    public fun codeActionResolve(handler: suspend RequestContext.(CodeAction) -> CodeAction) {
        request(LspMethods.CODE_ACTION_RESOLVE, handler)
    }

    /** Handles `textDocument/codeLens` requests. */
    public fun codeLens(handler: suspend PartialResultContext<List<CodeLens>>.(CodeLensParams) -> List<CodeLens>?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_CODE_LENS, serializer<List<CodeLens>>(), handler)
    }

    /** Handles `codeLens/resolve` requests. */
    public fun codeLensResolve(handler: suspend RequestContext.(CodeLens) -> CodeLens) {
        request(LspMethods.CODE_LENS_RESOLVE, handler)
    }

    /** Handles `textDocument/documentLink` requests. */
    public fun documentLink(handler: suspend PartialResultContext<List<DocumentLink>>.(DocumentLinkParams) -> List<DocumentLink>?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_DOCUMENT_LINK, serializer<List<DocumentLink>>(), handler)
    }

    /** Handles `documentLink/resolve` requests. */
    public fun documentLinkResolve(handler: suspend RequestContext.(DocumentLink) -> DocumentLink) {
        request(LspMethods.DOCUMENT_LINK_RESOLVE, handler)
    }

    /** Handles `textDocument/documentColor` requests. */
    public fun documentColor(
        handler: suspend PartialResultContext<List<ColorInformation>>.(DocumentColorParams) -> List<ColorInformation>,
    ) {
        partialRequest(LspMethods.TEXT_DOCUMENT_DOCUMENT_COLOR, serializer<List<ColorInformation>>(), handler)
    }

    /** Handles `textDocument/colorPresentation` requests. */
    public fun colorPresentation(
        handler: suspend PartialResultContext<List<ColorPresentation>>.(ColorPresentationParams) -> List<ColorPresentation>,
    ) {
        partialRequest(LspMethods.TEXT_DOCUMENT_COLOR_PRESENTATION, serializer<List<ColorPresentation>>(), handler)
    }

    /** Handles `textDocument/formatting` requests. */
    public fun formatting(handler: suspend RequestContext.(DocumentFormattingParams) -> List<TextEdit>?) {
        request(LspMethods.TEXT_DOCUMENT_FORMATTING, handler)
    }

    /** Handles `textDocument/rangeFormatting` requests. */
    public fun rangeFormatting(handler: suspend RequestContext.(DocumentRangeFormattingParams) -> List<TextEdit>?) {
        request(LspMethods.TEXT_DOCUMENT_RANGE_FORMATTING, handler)
    }

    /** Handles `textDocument/rangesFormatting` requests. */
    public fun rangesFormatting(handler: suspend RequestContext.(DocumentRangesFormattingParams) -> List<TextEdit>?) {
        request(LspMethods.TEXT_DOCUMENT_RANGES_FORMATTING, handler)
    }

    /** Handles `textDocument/onTypeFormatting` requests. */
    public fun onTypeFormatting(handler: suspend RequestContext.(DocumentOnTypeFormattingParams) -> List<TextEdit>?) {
        request(LspMethods.TEXT_DOCUMENT_ON_TYPE_FORMATTING, handler)
    }

    /** Handles `textDocument/rename` requests. */
    public fun rename(handler: suspend RequestContext.(RenameParams) -> WorkspaceEdit?) {
        request(LspMethods.TEXT_DOCUMENT_RENAME, handler)
    }

    /** Handles `textDocument/prepareRename` requests. */
    public fun prepareRename(handler: suspend RequestContext.(PrepareRenameParams) -> PrepareRenameResult?) {
        request(LspMethods.TEXT_DOCUMENT_PREPARE_RENAME, handler)
    }

    /** Handles `textDocument/foldingRange` requests. */
    public fun foldingRange(handler: suspend PartialResultContext<List<FoldingRange>>.(FoldingRangeParams) -> List<FoldingRange>?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_FOLDING_RANGE, serializer<List<FoldingRange>>(), handler)
    }

    /** Handles `textDocument/selectionRange` requests. */
    public fun selectionRange(handler: suspend PartialResultContext<List<SelectionRange>>.(SelectionRangeParams) -> List<SelectionRange>?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_SELECTION_RANGE, serializer<List<SelectionRange>>(), handler)
    }

    /** Handles `textDocument/linkedEditingRange` requests. */
    public fun linkedEditingRange(handler: suspend RequestContext.(LinkedEditingRangeParams) -> LinkedEditingRanges?) {
        request(LspMethods.TEXT_DOCUMENT_LINKED_EDITING_RANGE, handler)
    }
}
