package io.lsp4k.client

import io.lsp4k.protocol.CompletionItem
import io.lsp4k.protocol.CompletionParams
import io.lsp4k.protocol.DeclarationParams
import io.lsp4k.protocol.DefinitionParams
import io.lsp4k.protocol.DidChangeTextDocumentParams
import io.lsp4k.protocol.DidCloseTextDocumentParams
import io.lsp4k.protocol.DidOpenTextDocumentParams
import io.lsp4k.protocol.DidSaveTextDocumentParams
import io.lsp4k.protocol.DocumentHighlightParams
import io.lsp4k.protocol.DocumentSymbolParams
import io.lsp4k.protocol.HoverParams
import io.lsp4k.protocol.ImplementationParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.ReferenceParams
import io.lsp4k.protocol.SignatureHelpParams
import io.lsp4k.protocol.TypeDefinitionParams
import io.lsp4k.protocol.WillSaveTextDocumentParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal val textDocumentCases: List<CallCase> =
    listOf(
        notificationCase<DidOpenTextDocumentParams>(
            LspMethods.TEXT_DOCUMENT_DID_OPEN,
            """{"textDocument":{"uri":"file:///a.kt","languageId":"kotlin","version":1,"text":"x"}}""",
        ) { textDocument.didOpen(it) },
        notificationCase<DidChangeTextDocumentParams>(
            LspMethods.TEXT_DOCUMENT_DID_CHANGE,
            """{"textDocument":{"uri":"file:///a.kt","version":2},"contentChanges":[{"text":"y"}]}""",
        ) { textDocument.didChange(it) },
        notificationCase<DidCloseTextDocumentParams>(
            LspMethods.TEXT_DOCUMENT_DID_CLOSE,
            """{$DOC}""",
        ) { textDocument.didClose(it) },
        notificationCase<DidSaveTextDocumentParams>(
            LspMethods.TEXT_DOCUMENT_DID_SAVE,
            """{$DOC}""",
        ) { textDocument.didSave(it) },
        notificationCase<WillSaveTextDocumentParams>(
            LspMethods.TEXT_DOCUMENT_WILL_SAVE,
            """{$DOC,"reason":1}""",
        ) { textDocument.willSave(it) },
        requestCase<WillSaveTextDocumentParams>(
            LspMethods.TEXT_DOCUMENT_WILL_SAVE_WAIT_UNTIL,
            """{$DOC,"reason":1}""",
        ) { textDocument.willSaveWaitUntil(it) },
        requestCase<CompletionParams>(
            LspMethods.TEXT_DOCUMENT_COMPLETION,
            """{$DOC,$POS}""",
        ) { textDocument.completion(it) },
        requestCase<CompletionItem>(
            LspMethods.COMPLETION_ITEM_RESOLVE,
            """{"label":"x"}""",
        ) { textDocument.completionResolve(it) },
        requestCase<HoverParams>(
            LspMethods.TEXT_DOCUMENT_HOVER,
            """{$DOC,$POS}""",
        ) { textDocument.hover(it) },
        requestCase<SignatureHelpParams>(
            LspMethods.TEXT_DOCUMENT_SIGNATURE_HELP,
            """{$DOC,$POS}""",
        ) { textDocument.signatureHelp(it) },
        requestCase<DeclarationParams>(
            LspMethods.TEXT_DOCUMENT_DECLARATION,
            """{$DOC,$POS}""",
        ) { textDocument.declaration(it) },
        requestCase<DefinitionParams>(
            LspMethods.TEXT_DOCUMENT_DEFINITION,
            """{$DOC,$POS}""",
        ) { textDocument.definition(it) },
        requestCase<TypeDefinitionParams>(
            LspMethods.TEXT_DOCUMENT_TYPE_DEFINITION,
            """{$DOC,$POS}""",
        ) { textDocument.typeDefinition(it) },
        requestCase<ImplementationParams>(
            LspMethods.TEXT_DOCUMENT_IMPLEMENTATION,
            """{$DOC,$POS}""",
        ) { textDocument.implementation(it) },
        requestCase<ReferenceParams>(
            LspMethods.TEXT_DOCUMENT_REFERENCES,
            """{$DOC,$POS,"context":{"includeDeclaration":true}}""",
        ) { textDocument.references(it) },
        requestCase<DocumentHighlightParams>(
            LspMethods.TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT,
            """{$DOC,$POS}""",
        ) { textDocument.documentHighlight(it) },
        requestCase<DocumentSymbolParams>(
            LspMethods.TEXT_DOCUMENT_DOCUMENT_SYMBOL,
            """{$DOC}""",
        ) { textDocument.documentSymbol(it) },
    )

class TextDocumentCallsTest {
    @Test
    fun `every call reaches the server with its method and params`() =
        runTest {
            runCases(backgroundScope, textDocumentCases)
        }
}
