package io.lsp4k.client

import io.lsp4k.protocol.CompletionItem
import io.lsp4k.protocol.CompletionParams
import io.lsp4k.protocol.CompletionResult
import io.lsp4k.protocol.DeclarationParams
import io.lsp4k.protocol.DeclarationResult
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
import io.lsp4k.protocol.HoverParams
import io.lsp4k.protocol.ImplementationParams
import io.lsp4k.protocol.ImplementationResult
import io.lsp4k.protocol.Location
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.ReferenceParams
import io.lsp4k.protocol.SignatureHelp
import io.lsp4k.protocol.SignatureHelpParams
import io.lsp4k.protocol.TextEdit
import io.lsp4k.protocol.TypeDefinitionParams
import io.lsp4k.protocol.TypeDefinitionResult
import io.lsp4k.protocol.WillSaveTextDocumentParams

/**
 * Text document synchronization, completion, hover and navigation requests (the `textDocument` namespace).
 */
public class TextDocumentCalls internal constructor(
    private val caller: ServerCaller,
) {
    /**
     * Notify the server that a text document was opened.
     */
    public suspend fun didOpen(params: DidOpenTextDocumentParams): Unit =
        caller.notify(LspMethods.TEXT_DOCUMENT_DID_OPEN, params)

    /**
     * Notify the server that a text document was changed.
     */
    public suspend fun didChange(params: DidChangeTextDocumentParams): Unit =
        caller.notify(LspMethods.TEXT_DOCUMENT_DID_CHANGE, params)

    /**
     * Notify the server that a text document was closed.
     */
    public suspend fun didClose(params: DidCloseTextDocumentParams): Unit =
        caller.notify(LspMethods.TEXT_DOCUMENT_DID_CLOSE, params)

    /**
     * Notify the server that a text document was saved.
     */
    public suspend fun didSave(params: DidSaveTextDocumentParams): Unit =
        caller.notify(LspMethods.TEXT_DOCUMENT_DID_SAVE, params)

    /**
     * Notify the server that a text document will be saved.
     */
    public suspend fun willSave(params: WillSaveTextDocumentParams): Unit =
        caller.notify(LspMethods.TEXT_DOCUMENT_WILL_SAVE, params)

    /**
     * Request text edits before a document is saved.
     */
    public suspend fun willSaveWaitUntil(params: WillSaveTextDocumentParams): List<TextEdit>? =
        caller.request(LspMethods.TEXT_DOCUMENT_WILL_SAVE_WAIT_UNTIL, params)

    /**
     * Request completion at a given text document position, preserving the server's array or list shape.
     */
    public suspend fun completion(params: CompletionParams): CompletionResult? =
        caller.request(LspMethods.TEXT_DOCUMENT_COMPLETION, params)

    /**
     * Resolve additional information for a given completion item.
     */
    public suspend fun completionResolve(item: CompletionItem): CompletionItem? =
        caller.request(LspMethods.COMPLETION_ITEM_RESOLVE, item)

    /**
     * Request hover information at a given text document position.
     */
    public suspend fun hover(params: HoverParams): Hover? =
        caller.request(LspMethods.TEXT_DOCUMENT_HOVER, params)

    /**
     * Request signature help at a given text document position.
     */
    public suspend fun signatureHelp(params: SignatureHelpParams): SignatureHelp? =
        caller.request(LspMethods.TEXT_DOCUMENT_SIGNATURE_HELP, params)

    /**
     * Request the declaration of a symbol at a given text document position.
     */
    public suspend fun declaration(params: DeclarationParams): DeclarationResult? =
        caller.request(LspMethods.TEXT_DOCUMENT_DECLARATION, params)

    /**
     * Request the definition of a symbol at a given text document position, preserving single
     * locations and location or link arrays.
     */
    public suspend fun definition(params: DefinitionParams): DefinitionResult? =
        caller.request(LspMethods.TEXT_DOCUMENT_DEFINITION, params)

    /**
     * Request the type definition of a symbol at a given text document position.
     */
    public suspend fun typeDefinition(params: TypeDefinitionParams): TypeDefinitionResult? =
        caller.request(LspMethods.TEXT_DOCUMENT_TYPE_DEFINITION, params)

    /**
     * Request the implementation of a symbol at a given text document position.
     */
    public suspend fun implementation(params: ImplementationParams): ImplementationResult? =
        caller.request(LspMethods.TEXT_DOCUMENT_IMPLEMENTATION, params)

    /**
     * Request references to a symbol at a given text document position.
     */
    public suspend fun references(params: ReferenceParams): List<Location>? =
        caller.request(LspMethods.TEXT_DOCUMENT_REFERENCES, params)

    /**
     * Request document highlights at a given text document position.
     */
    public suspend fun documentHighlight(params: DocumentHighlightParams): List<DocumentHighlight>? =
        caller.request(LspMethods.TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT, params)

    /**
     * Request document symbols for a given text document.
     */
    public suspend fun documentSymbol(params: DocumentSymbolParams): DocumentSymbolResult? =
        caller.request(LspMethods.TEXT_DOCUMENT_DOCUMENT_SYMBOL, params)
}
