package io.lsp4k.client

import io.lsp4k.protocol.CodeAction
import io.lsp4k.protocol.CodeActionParams
import io.lsp4k.protocol.CodeLens
import io.lsp4k.protocol.CodeLensParams
import io.lsp4k.protocol.ColorPresentationParams
import io.lsp4k.protocol.DocumentColorParams
import io.lsp4k.protocol.DocumentFormattingParams
import io.lsp4k.protocol.DocumentLink
import io.lsp4k.protocol.DocumentLinkParams
import io.lsp4k.protocol.DocumentOnTypeFormattingParams
import io.lsp4k.protocol.DocumentRangeFormattingParams
import io.lsp4k.protocol.DocumentRangesFormattingParams
import io.lsp4k.protocol.FoldingRangeParams
import io.lsp4k.protocol.LinkedEditingRangeParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.PrepareRenameParams
import io.lsp4k.protocol.RenameParams
import io.lsp4k.protocol.SelectionRangeParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal val editingCases: List<CallCase> =
    listOf(
        requestCase<CodeActionParams>(
            LspMethods.TEXT_DOCUMENT_CODE_ACTION,
            """{$DOC,$RNG,"context":{"diagnostics":[]}}""",
        ) { editing.codeAction(it) },
        requestCase<CodeAction>(
            LspMethods.CODE_ACTION_RESOLVE,
            """{"title":"t"}""",
        ) { editing.codeActionResolve(it) },
        requestCase<CodeLensParams>(
            LspMethods.TEXT_DOCUMENT_CODE_LENS,
            """{$DOC}""",
        ) { editing.codeLens(it) },
        requestCase<CodeLens>(
            LspMethods.CODE_LENS_RESOLVE,
            """{$RNG}""",
        ) { editing.codeLensResolve(it) },
        requestCase<DocumentLinkParams>(
            LspMethods.TEXT_DOCUMENT_DOCUMENT_LINK,
            """{$DOC}""",
        ) { editing.documentLink(it) },
        requestCase<DocumentLink>(
            LspMethods.DOCUMENT_LINK_RESOLVE,
            """{$RNG}""",
        ) { editing.documentLinkResolve(it) },
        requestCase<DocumentColorParams>(
            LspMethods.TEXT_DOCUMENT_DOCUMENT_COLOR,
            """{$DOC}""",
        ) { editing.documentColor(it) },
        requestCase<ColorPresentationParams>(
            LspMethods.TEXT_DOCUMENT_COLOR_PRESENTATION,
            """{$DOC,"color":{"red":1,"green":0,"blue":0,"alpha":1},$RNG}""",
        ) { editing.colorPresentation(it) },
        requestCase<DocumentFormattingParams>(
            LspMethods.TEXT_DOCUMENT_FORMATTING,
            """{$DOC,$OPTS}""",
        ) { editing.formatting(it) },
        requestCase<DocumentRangeFormattingParams>(
            LspMethods.TEXT_DOCUMENT_RANGE_FORMATTING,
            """{$DOC,$RNG,$OPTS}""",
        ) { editing.rangeFormatting(it) },
        requestCase<DocumentRangesFormattingParams>(
            LspMethods.TEXT_DOCUMENT_RANGES_FORMATTING,
            """{$DOC,"ranges":[$LINE_RANGE],$OPTS}""",
        ) { editing.rangesFormatting(it) },
        requestCase<DocumentOnTypeFormattingParams>(
            LspMethods.TEXT_DOCUMENT_ON_TYPE_FORMATTING,
            """{$DOC,$POS,"ch":"}",$OPTS}""",
        ) { editing.onTypeFormatting(it) },
        requestCase<RenameParams>(
            LspMethods.TEXT_DOCUMENT_RENAME,
            """{$DOC,$POS,"newName":"n"}""",
        ) { editing.rename(it) },
        requestCase<PrepareRenameParams>(
            LspMethods.TEXT_DOCUMENT_PREPARE_RENAME,
            """{$DOC,$POS}""",
        ) { editing.prepareRename(it) },
        requestCase<FoldingRangeParams>(
            LspMethods.TEXT_DOCUMENT_FOLDING_RANGE,
            """{$DOC}""",
        ) { editing.foldingRange(it) },
        requestCase<SelectionRangeParams>(
            LspMethods.TEXT_DOCUMENT_SELECTION_RANGE,
            """{$DOC,"positions":[{"line":1,"character":2}]}""",
        ) { editing.selectionRange(it) },
        requestCase<LinkedEditingRangeParams>(
            LspMethods.TEXT_DOCUMENT_LINKED_EDITING_RANGE,
            """{$DOC,$POS}""",
        ) { editing.linkedEditingRange(it) },
    )

class EditingCallsTest {
    @Test
    fun `every call reaches the server with its method and params`() =
        runTest {
            runCases(backgroundScope, editingCases)
        }
}
