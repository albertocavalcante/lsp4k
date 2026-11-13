package io.lsp4k.server

import io.jsonrpc4k.core.ResponseError
import io.lsp4k.protocol.CompletionItem
import io.lsp4k.protocol.CompletionParams
import io.lsp4k.protocol.CompletionResult
import io.lsp4k.protocol.DeclarationParams
import io.lsp4k.protocol.DefinitionParams
import io.lsp4k.protocol.DefinitionResult
import io.lsp4k.protocol.DidChangeTextDocumentParams
import io.lsp4k.protocol.DidCloseTextDocumentParams
import io.lsp4k.protocol.DidOpenTextDocumentParams
import io.lsp4k.protocol.DidSaveTextDocumentParams
import io.lsp4k.protocol.DocumentHighlight
import io.lsp4k.protocol.DocumentHighlightParams
import io.lsp4k.protocol.DocumentSymbolParams
import io.lsp4k.protocol.DocumentSymbolResult
import io.lsp4k.protocol.Hover
import io.lsp4k.protocol.HoverContents
import io.lsp4k.protocol.HoverParams
import io.lsp4k.protocol.ImplementationParams
import io.lsp4k.protocol.Location
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.MarkupContent
import io.lsp4k.protocol.MarkupKind
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.Range
import io.lsp4k.protocol.ReferenceContext
import io.lsp4k.protocol.ReferenceParams
import io.lsp4k.protocol.SignatureHelp
import io.lsp4k.protocol.SignatureHelpParams
import io.lsp4k.protocol.TextDocumentContentChangeEvent
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.protocol.TextDocumentItem
import io.lsp4k.protocol.TextDocumentSaveReason
import io.lsp4k.protocol.TextEdit
import io.lsp4k.protocol.TypeDefinitionParams
import io.lsp4k.protocol.VersionedTextDocumentIdentifier
import io.lsp4k.protocol.WillSaveTextDocumentParams
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.assertEquals

class TextDocumentHandlersTest {
    private val document = TextDocumentIdentifier("file:///notes.txt")
    private val position = Position(2, 4)
    private val range = Range(Position(1, 0), Position(1, 5))
    private val location = Location("file:///notes.txt", range)
    private val edits = listOf(TextEdit(range, "fixed"))
    private val item = CompletionItem(label = "note")
    private val hoverResult = Hover(HoverContents.Markup(MarkupContent(MarkupKind.Markdown, "**note**")))

    @Test
    fun `text document requests reach typed handlers and encode their results`() =
        runTest {
            val received = mutableListOf<Any>()
            val peer =
                startPeer(backgroundScope) {
                    textDocument {
                        willSaveWaitUntil {
                            received += it
                            edits
                        }
                        completion {
                            received += it
                            CompletionResult.Items(listOf(item))
                        }
                        completionResolve { it.copy(detail = "resolved") }
                        hover {
                            received += it
                            hoverResult
                        }
                        signatureHelp { SignatureHelp(signatures = emptyList(), activeSignature = 0) }
                        declaration { DefinitionResult.Single(location) }
                        definition { DefinitionResult.Locations(listOf(location)) }
                        typeDefinition { DefinitionResult.Links(emptyList()) }
                        implementation { null }
                        references {
                            received += it
                            listOf(location)
                        }
                        documentHighlight { listOf(DocumentHighlight(range)) }
                        documentSymbol { DocumentSymbolResult.Flat(emptyList()) }
                    }
                }
            peer.initialize()

            val willSave = WillSaveTextDocumentParams(document, TextDocumentSaveReason.Manual)
            val saveEdits = peer.call<WillSaveTextDocumentParams, List<TextEdit>>(LspMethods.TEXT_DOCUMENT_WILL_SAVE_WAIT_UNTIL, willSave)
            assertEquals(edits, saveEdits)
            val completion = CompletionParams(document, position)
            val items = peer.call<CompletionParams, CompletionResult>(LspMethods.TEXT_DOCUMENT_COMPLETION, completion)
            assertEquals(CompletionResult.Items(listOf(item)), items)
            val resolved = peer.call<CompletionItem, CompletionItem>(LspMethods.COMPLETION_ITEM_RESOLVE, item)
            assertEquals(item.copy(detail = "resolved"), resolved)
            val hoverParams = HoverParams(document, position)
            assertEquals(hoverResult, peer.call<HoverParams, Hover>(LspMethods.TEXT_DOCUMENT_HOVER, hoverParams))
            val signature = SignatureHelpParams(document, position)
            val help = peer.call<SignatureHelpParams, SignatureHelp>(LspMethods.TEXT_DOCUMENT_SIGNATURE_HELP, signature)
            assertEquals(SignatureHelp(signatures = emptyList(), activeSignature = 0), help)
            val declaration = DeclarationParams(document, position)
            val declared = peer.call<DeclarationParams, DefinitionResult>(LspMethods.TEXT_DOCUMENT_DECLARATION, declaration)
            assertEquals(DefinitionResult.Single(location), declared)
            val definition = DefinitionParams(document, position)
            val defined = peer.call<DefinitionParams, DefinitionResult>(LspMethods.TEXT_DOCUMENT_DEFINITION, definition)
            assertEquals(DefinitionResult.Locations(listOf(location)), defined)
            val typeDefinition = TypeDefinitionParams(document, position)
            val typeDefined = peer.call<TypeDefinitionParams, DefinitionResult>(LspMethods.TEXT_DOCUMENT_TYPE_DEFINITION, typeDefinition)
            assertEquals(DefinitionResult.Locations(emptyList()), typeDefined)
            val implementation = ImplementationParams(document, position)
            assertEquals(null, peer.call<ImplementationParams, DefinitionResult>(LspMethods.TEXT_DOCUMENT_IMPLEMENTATION, implementation))
            val references = ReferenceParams(document, position, ReferenceContext(includeDeclaration = true))
            assertEquals(listOf(location), peer.call<ReferenceParams, List<Location>>(LspMethods.TEXT_DOCUMENT_REFERENCES, references))
            val highlight = DocumentHighlightParams(document, position)
            val highlightMethod = LspMethods.TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT
            val highlights = peer.call<DocumentHighlightParams, List<DocumentHighlight>>(highlightMethod, highlight)
            assertEquals(listOf(DocumentHighlight(range)), highlights)
            val symbols = DocumentSymbolParams(document)
            val symbolResult = peer.call<DocumentSymbolParams, DocumentSymbolResult>(LspMethods.TEXT_DOCUMENT_DOCUMENT_SYMBOL, symbols)
            assertEquals(DocumentSymbolResult.Hierarchical(emptyList()), symbolResult)
            assertEquals(listOf<Any>(willSave, completion, hoverParams, references), received)
        }

    @Test
    fun `text document synchronization notifications reach typed handlers in order`() =
        runTest {
            val received = mutableListOf<Any>()
            val peer =
                startPeer(backgroundScope) {
                    textDocument {
                        didOpen { received += it }
                        didChange { received += it }
                        willSave { received += it }
                        didSave { received += it }
                        didClose { received += it }
                    }
                }
            peer.initialize()
            val opened = DidOpenTextDocumentParams(TextDocumentItem(document.uri, "plaintext", 1, "abc"))
            val changed =
                DidChangeTextDocumentParams(
                    VersionedTextDocumentIdentifier(document.uri, 2),
                    listOf(TextDocumentContentChangeEvent(range = range, text = "x")),
                )
            val willSave = WillSaveTextDocumentParams(document, TextDocumentSaveReason.AfterDelay)
            val saved = DidSaveTextDocumentParams(document, text = "xbc")
            val closed = DidCloseTextDocumentParams(document)

            peer.send(LspMethods.TEXT_DOCUMENT_DID_OPEN, opened)
            peer.send(LspMethods.TEXT_DOCUMENT_DID_CHANGE, changed)
            peer.send(LspMethods.TEXT_DOCUMENT_WILL_SAVE, willSave)
            peer.send(LspMethods.TEXT_DOCUMENT_DID_SAVE, saved)
            peer.send(LspMethods.TEXT_DOCUMENT_DID_CLOSE, closed)
            peer.sync()

            assertEquals(listOf<Any>(opened, changed, willSave, saved, closed), received)
        }

    @Test
    fun `missing and malformed params are answered with InvalidParams`() =
        runTest {
            val peer = startPeer(backgroundScope) { textDocument { hover { hoverResult } } }
            peer.initialize()
            assertEquals(ResponseError.INVALID_PARAMS, peer.request(LspMethods.TEXT_DOCUMENT_HOVER).errorCode())
            assertEquals(ResponseError.INVALID_PARAMS, peer.request(LspMethods.TEXT_DOCUMENT_HOVER, JsonNull).errorCode())
            val malformed = json("""{"textDocument":{"uri":"file:///notes.txt"},"position":{"line":"two"}}""")
            assertEquals(ResponseError.INVALID_PARAMS, peer.request(LspMethods.TEXT_DOCUMENT_HOVER, malformed).errorCode())
            assertEquals(hoverResult, peer.call<HoverParams, Hover>(LspMethods.TEXT_DOCUMENT_HOVER, HoverParams(document, position)))
        }
}
