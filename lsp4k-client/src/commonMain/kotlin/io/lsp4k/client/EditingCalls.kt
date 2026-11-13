package io.lsp4k.client

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

/**
 * Code action, code lens, link, color, formatting, rename and range requests (the `textDocument` namespace and their resolve requests).
 */
public class EditingCalls internal constructor(
    private val caller: ServerCaller,
) {
    /**
     * Request code actions for a given text document and range.
     */
    public suspend fun codeAction(params: CodeActionParams): List<CommandOrCodeAction>? =
        caller.request(LspMethods.TEXT_DOCUMENT_CODE_ACTION, params)

    /**
     * Resolve additional information for a given code action.
     */
    public suspend fun codeActionResolve(codeAction: CodeAction): CodeAction? = caller.request(LspMethods.CODE_ACTION_RESOLVE, codeAction)

    /**
     * Request code lenses for a given text document.
     */
    public suspend fun codeLens(params: CodeLensParams): List<CodeLens>? = caller.request(LspMethods.TEXT_DOCUMENT_CODE_LENS, params)

    /**
     * Resolve additional information for a given code lens.
     */
    public suspend fun codeLensResolve(codeLens: CodeLens): CodeLens? = caller.request(LspMethods.CODE_LENS_RESOLVE, codeLens)

    /**
     * Request document links for a given text document.
     */
    public suspend fun documentLink(params: DocumentLinkParams): List<DocumentLink>? =
        caller.request(LspMethods.TEXT_DOCUMENT_DOCUMENT_LINK, params)

    /**
     * Resolve additional information for a given document link.
     */
    public suspend fun documentLinkResolve(link: DocumentLink): DocumentLink? = caller.request(LspMethods.DOCUMENT_LINK_RESOLVE, link)

    /**
     * Request document colors for a given text document.
     */
    public suspend fun documentColor(params: DocumentColorParams): List<ColorInformation>? =
        caller.request(LspMethods.TEXT_DOCUMENT_DOCUMENT_COLOR, params)

    /**
     * Request color presentations for a given color.
     */
    public suspend fun colorPresentation(params: ColorPresentationParams): List<ColorPresentation>? =
        caller.request(LspMethods.TEXT_DOCUMENT_COLOR_PRESENTATION, params)

    /**
     * Request formatting for a whole document.
     */
    public suspend fun formatting(params: DocumentFormattingParams): List<TextEdit>? =
        caller.request(LspMethods.TEXT_DOCUMENT_FORMATTING, params)

    /**
     * Request formatting for a range in a document.
     */
    public suspend fun rangeFormatting(params: DocumentRangeFormattingParams): List<TextEdit>? =
        caller.request(LspMethods.TEXT_DOCUMENT_RANGE_FORMATTING, params)

    /**
     * Request formatting for multiple ranges in a document.
     * @since 3.18.0
     */
    public suspend fun rangesFormatting(params: DocumentRangesFormattingParams): List<TextEdit>? =
        caller.request(LspMethods.TEXT_DOCUMENT_RANGES_FORMATTING, params)

    /**
     * Request formatting after a character has been typed.
     */
    public suspend fun onTypeFormatting(params: DocumentOnTypeFormattingParams): List<TextEdit>? =
        caller.request(LspMethods.TEXT_DOCUMENT_ON_TYPE_FORMATTING, params)

    /**
     * Request a rename of a symbol.
     */
    public suspend fun rename(params: RenameParams): WorkspaceEdit? = caller.request(LspMethods.TEXT_DOCUMENT_RENAME, params)

    /**
     * Request to prepare a rename.
     */
    public suspend fun prepareRename(params: PrepareRenameParams): PrepareRenameResult? =
        caller.request(LspMethods.TEXT_DOCUMENT_PREPARE_RENAME, params)

    /**
     * Request folding ranges for a given text document.
     */
    public suspend fun foldingRange(params: FoldingRangeParams): List<FoldingRange>? =
        caller.request(LspMethods.TEXT_DOCUMENT_FOLDING_RANGE, params)

    /**
     * Request selection ranges for a given text document.
     */
    public suspend fun selectionRange(params: SelectionRangeParams): List<SelectionRange>? =
        caller.request(LspMethods.TEXT_DOCUMENT_SELECTION_RANGE, params)

    /**
     * Request linked editing ranges for a given text document position.
     */
    public suspend fun linkedEditingRange(params: LinkedEditingRangeParams): LinkedEditingRanges? =
        caller.request(LspMethods.TEXT_DOCUMENT_LINKED_EDITING_RANGE, params)
}
