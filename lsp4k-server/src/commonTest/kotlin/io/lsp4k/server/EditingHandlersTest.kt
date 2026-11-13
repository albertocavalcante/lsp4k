package io.lsp4k.server

import io.lsp4k.protocol.CodeAction
import io.lsp4k.protocol.CodeActionContext
import io.lsp4k.protocol.CodeActionParams
import io.lsp4k.protocol.CodeLens
import io.lsp4k.protocol.CodeLensParams
import io.lsp4k.protocol.Color
import io.lsp4k.protocol.ColorInformation
import io.lsp4k.protocol.ColorPresentation
import io.lsp4k.protocol.ColorPresentationParams
import io.lsp4k.protocol.Command
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
import io.lsp4k.protocol.FormattingOptions
import io.lsp4k.protocol.LinkedEditingRangeParams
import io.lsp4k.protocol.LinkedEditingRanges
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.PrepareRenameParams
import io.lsp4k.protocol.PrepareRenamePlaceholder
import io.lsp4k.protocol.PrepareRenameResult
import io.lsp4k.protocol.Range
import io.lsp4k.protocol.RenameParams
import io.lsp4k.protocol.SelectionRange
import io.lsp4k.protocol.SelectionRangeParams
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.protocol.TextEdit
import io.lsp4k.protocol.WorkspaceEdit
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EditingHandlersTest {
    private val document = TextDocumentIdentifier("file:///edit.kt")
    private val position = Position(3, 1)
    private val range = Range(Position(3, 0), Position(3, 6))
    private val options = FormattingOptions(tabSize = 4, insertSpaces = true)
    private val edits = listOf(TextEdit(range, "edited"))
    private val red = Color(1.0, 0.0, 0.0, 1.0)

    @Test
    fun `editing requests reach typed handlers and encode their results`() =
        runTest {
            val received = mutableListOf<Any>()
            val actions =
                listOf(
                    CommandOrCodeAction.OfCodeAction(CodeAction(title = "fix")),
                    CommandOrCodeAction.OfCommand(Command("Run", "edit.run")),
                )
            val peer =
                startPeer(backgroundScope) {
                    editing {
                        codeAction {
                            received += it
                            actions
                        }
                        codeActionResolve { it.copy(isPreferred = true) }
                        codeLens { listOf(CodeLens(range)) }
                        codeLensResolve { it.copy(command = Command("Show", "edit.show")) }
                        documentLink { listOf(DocumentLink(range)) }
                        documentLinkResolve { it.copy(target = "file:///target.kt") }
                        documentColor { listOf(ColorInformation(range, red)) }
                        colorPresentation { listOf(ColorPresentation(label = "red")) }
                        formatting { edits }
                        rangeFormatting { edits }
                        rangesFormatting {
                            received += it
                            edits
                        }
                        onTypeFormatting { edits }
                        rename { WorkspaceEdit(changes = mapOf(document.uri to edits)) }
                        prepareRename { PrepareRenameResult.OfPlaceholder(PrepareRenamePlaceholder(range, "name")) }
                        foldingRange { listOf(FoldingRange(startLine = 0, endLine = 3)) }
                        selectionRange { listOf(SelectionRange(range)) }
                        linkedEditingRange { LinkedEditingRanges(listOf(range)) }
                    }
                }
            peer.initialize()

            val codeActionParams = CodeActionParams(document, range, CodeActionContext(diagnostics = emptyList()))
            val codeActions = peer.call<CodeActionParams, List<CommandOrCodeAction>>(LspMethods.TEXT_DOCUMENT_CODE_ACTION, codeActionParams)
            assertEquals(actions, codeActions)
            val action = CodeAction(title = "fix")
            assertEquals(action.copy(isPreferred = true), peer.call<CodeAction, CodeAction>(LspMethods.CODE_ACTION_RESOLVE, action))
            val lenses = peer.call<CodeLensParams, List<CodeLens>>(LspMethods.TEXT_DOCUMENT_CODE_LENS, CodeLensParams(document))
            assertEquals(listOf(CodeLens(range)), lenses)
            val lens = peer.call<CodeLens, CodeLens>(LspMethods.CODE_LENS_RESOLVE, CodeLens(range))
            assertEquals(CodeLens(range, Command("Show", "edit.show")), lens)
            val linkParams = DocumentLinkParams(document)
            val links = peer.call<DocumentLinkParams, List<DocumentLink>>(LspMethods.TEXT_DOCUMENT_DOCUMENT_LINK, linkParams)
            assertEquals(listOf(DocumentLink(range)), links)
            val link = peer.call<DocumentLink, DocumentLink>(LspMethods.DOCUMENT_LINK_RESOLVE, DocumentLink(range))
            assertEquals(DocumentLink(range, target = "file:///target.kt"), link)
            val colorParams = DocumentColorParams(document)
            val colors = peer.call<DocumentColorParams, List<ColorInformation>>(LspMethods.TEXT_DOCUMENT_DOCUMENT_COLOR, colorParams)
            assertEquals(listOf(ColorInformation(range, red)), colors)
            val presentationParams = ColorPresentationParams(document, red, range)
            val presentationMethod = LspMethods.TEXT_DOCUMENT_COLOR_PRESENTATION
            val presentations = peer.call<ColorPresentationParams, List<ColorPresentation>>(presentationMethod, presentationParams)
            assertEquals(listOf(ColorPresentation(label = "red")), presentations)
            val formatting = DocumentFormattingParams(document, options)
            assertEquals(edits, peer.call<DocumentFormattingParams, List<TextEdit>>(LspMethods.TEXT_DOCUMENT_FORMATTING, formatting))
            val rangeFormatting = DocumentRangeFormattingParams(document, range, options)
            val rangeMethod = LspMethods.TEXT_DOCUMENT_RANGE_FORMATTING
            assertEquals(edits, peer.call<DocumentRangeFormattingParams, List<TextEdit>>(rangeMethod, rangeFormatting))
            val rangesFormatting = DocumentRangesFormattingParams(document, listOf(range), options)
            val rangesMethod = LspMethods.TEXT_DOCUMENT_RANGES_FORMATTING
            assertEquals(edits, peer.call<DocumentRangesFormattingParams, List<TextEdit>>(rangesMethod, rangesFormatting))
            val onType = DocumentOnTypeFormattingParams(document, position, ";", options)
            val onTypeMethod = LspMethods.TEXT_DOCUMENT_ON_TYPE_FORMATTING
            assertEquals(edits, peer.call<DocumentOnTypeFormattingParams, List<TextEdit>>(onTypeMethod, onType))
            val rename = RenameParams(document, position, "renamed")
            val renamed = peer.call<RenameParams, WorkspaceEdit>(LspMethods.TEXT_DOCUMENT_RENAME, rename)
            assertEquals(WorkspaceEdit(changes = mapOf(document.uri to edits)), renamed)
            val prepare = PrepareRenameParams(document, position)
            val prepared = peer.call<PrepareRenameParams, PrepareRenameResult>(LspMethods.TEXT_DOCUMENT_PREPARE_RENAME, prepare)
            assertEquals(PrepareRenameResult.OfPlaceholder(PrepareRenamePlaceholder(range, "name")), prepared)
            val foldingParams = FoldingRangeParams(document)
            val folding = peer.call<FoldingRangeParams, List<FoldingRange>>(LspMethods.TEXT_DOCUMENT_FOLDING_RANGE, foldingParams)
            assertEquals(listOf(FoldingRange(startLine = 0, endLine = 3)), folding)
            val selection = SelectionRangeParams(document, listOf(position))
            val selected = peer.call<SelectionRangeParams, List<SelectionRange>>(LspMethods.TEXT_DOCUMENT_SELECTION_RANGE, selection)
            assertEquals(listOf(SelectionRange(range)), selected)
            val linked = LinkedEditingRangeParams(document, position)
            val ranges = peer.call<LinkedEditingRangeParams, LinkedEditingRanges>(LspMethods.TEXT_DOCUMENT_LINKED_EDITING_RANGE, linked)
            assertEquals(LinkedEditingRanges(listOf(range)), ranges)
            assertEquals(listOf<Any>(codeActionParams, rangesFormatting), received)
        }
}
