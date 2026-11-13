package io.lsp4k.server

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
import io.lsp4k.protocol.HoverParams
import io.lsp4k.protocol.ImplementationParams
import io.lsp4k.protocol.Location
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.ReferenceParams
import io.lsp4k.protocol.SignatureHelp
import io.lsp4k.protocol.SignatureHelpParams
import io.lsp4k.protocol.TextEdit
import io.lsp4k.protocol.TypeDefinitionParams
import io.lsp4k.protocol.WillSaveTextDocumentParams
import kotlinx.serialization.serializer

/**
 * Handlers for text document synchronization, completion, hover, signature help and navigation.
 *
 * Navigation handlers stream partial results as [DefinitionResult] chunks; a [DefinitionResult.Single] chunk is
 * sent as a one-element location array.
 */
@LspServerDsl
public class TextDocumentHandlersBuilder internal constructor(
    registry: HandlerRegistry,
) : HandlersBuilder(registry) {
    /** Handles `textDocument/didOpen` notifications. */
    public fun didOpen(handler: suspend HandlerContext.(DidOpenTextDocumentParams) -> Unit) {
        notification(LspMethods.TEXT_DOCUMENT_DID_OPEN, handler)
    }

    /** Handles `textDocument/didChange` notifications. */
    public fun didChange(handler: suspend HandlerContext.(DidChangeTextDocumentParams) -> Unit) {
        notification(LspMethods.TEXT_DOCUMENT_DID_CHANGE, handler)
    }

    /** Handles `textDocument/didClose` notifications. */
    public fun didClose(handler: suspend HandlerContext.(DidCloseTextDocumentParams) -> Unit) {
        notification(LspMethods.TEXT_DOCUMENT_DID_CLOSE, handler)
    }

    /** Handles `textDocument/didSave` notifications. */
    public fun didSave(handler: suspend HandlerContext.(DidSaveTextDocumentParams) -> Unit) {
        notification(LspMethods.TEXT_DOCUMENT_DID_SAVE, handler)
    }

    /** Handles `textDocument/willSave` notifications. */
    public fun willSave(handler: suspend HandlerContext.(WillSaveTextDocumentParams) -> Unit) {
        notification(LspMethods.TEXT_DOCUMENT_WILL_SAVE, handler)
    }

    /** Handles `textDocument/willSaveWaitUntil` requests. */
    public fun willSaveWaitUntil(handler: suspend RequestContext.(WillSaveTextDocumentParams) -> List<TextEdit>?) {
        request(LspMethods.TEXT_DOCUMENT_WILL_SAVE_WAIT_UNTIL, handler)
    }

    /** Handles `textDocument/completion` requests; partial results are lists of [CompletionItem]. */
    public fun completion(handler: suspend PartialResultContext<List<CompletionItem>>.(CompletionParams) -> CompletionResult?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_COMPLETION, serializer<List<CompletionItem>>(), handler)
    }

    /** Handles `completionItem/resolve` requests. */
    public fun completionResolve(handler: suspend RequestContext.(CompletionItem) -> CompletionItem) {
        request(LspMethods.COMPLETION_ITEM_RESOLVE, handler)
    }

    /** Handles `textDocument/hover` requests. */
    public fun hover(handler: suspend RequestContext.(HoverParams) -> Hover?) {
        request(LspMethods.TEXT_DOCUMENT_HOVER, handler)
    }

    /** Handles `textDocument/signatureHelp` requests. */
    public fun signatureHelp(handler: suspend RequestContext.(SignatureHelpParams) -> SignatureHelp?) {
        request(LspMethods.TEXT_DOCUMENT_SIGNATURE_HELP, handler)
    }

    /** Handles `textDocument/declaration` requests. */
    public fun declaration(handler: suspend PartialResultContext<DefinitionResult>.(DeclarationParams) -> DefinitionResult?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_DECLARATION, DefinitionPartialResultSerializer, handler)
    }

    /** Handles `textDocument/definition` requests. */
    public fun definition(handler: suspend PartialResultContext<DefinitionResult>.(DefinitionParams) -> DefinitionResult?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_DEFINITION, DefinitionPartialResultSerializer, handler)
    }

    /** Handles `textDocument/typeDefinition` requests. */
    public fun typeDefinition(handler: suspend PartialResultContext<DefinitionResult>.(TypeDefinitionParams) -> DefinitionResult?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_TYPE_DEFINITION, DefinitionPartialResultSerializer, handler)
    }

    /** Handles `textDocument/implementation` requests. */
    public fun implementation(handler: suspend PartialResultContext<DefinitionResult>.(ImplementationParams) -> DefinitionResult?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_IMPLEMENTATION, DefinitionPartialResultSerializer, handler)
    }

    /** Handles `textDocument/references` requests; partial results are lists of [Location]. */
    public fun references(handler: suspend PartialResultContext<List<Location>>.(ReferenceParams) -> List<Location>?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_REFERENCES, serializer<List<Location>>(), handler)
    }

    /** Handles `textDocument/documentHighlight` requests; partial results are lists of [DocumentHighlight]. */
    public fun documentHighlight(
        handler: suspend PartialResultContext<List<DocumentHighlight>>.(DocumentHighlightParams) -> List<DocumentHighlight>?,
    ) {
        partialRequest(LspMethods.TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT, serializer<List<DocumentHighlight>>(), handler)
    }

    /** Handles `textDocument/documentSymbol` requests (hierarchical or flat); partial results use the same shapes. */
    public fun documentSymbol(handler: suspend PartialResultContext<DocumentSymbolResult>.(DocumentSymbolParams) -> DocumentSymbolResult?) {
        partialRequest(LspMethods.TEXT_DOCUMENT_DOCUMENT_SYMBOL, serializer<DocumentSymbolResult>(), handler)
    }
}
