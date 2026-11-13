package io.lsp4k.server

import io.lsp4k.protocol.DidChangeNotebookDocumentParams
import io.lsp4k.protocol.DidCloseNotebookDocumentParams
import io.lsp4k.protocol.DidOpenNotebookDocumentParams
import io.lsp4k.protocol.DidSaveNotebookDocumentParams
import io.lsp4k.protocol.LspMethods

/**
 * Handlers for notebook document synchronization notifications.
 */
@LspServerDsl
public class NotebookHandlersBuilder internal constructor(
    registry: HandlerRegistry,
) : HandlersBuilder(registry) {
    /** Handles `notebookDocument/didOpen` notifications. */
    public fun didOpen(handler: suspend HandlerContext.(DidOpenNotebookDocumentParams) -> Unit) {
        notification(LspMethods.NOTEBOOK_DOCUMENT_DID_OPEN, handler)
    }

    /** Handles `notebookDocument/didChange` notifications. */
    public fun didChange(handler: suspend HandlerContext.(DidChangeNotebookDocumentParams) -> Unit) {
        notification(LspMethods.NOTEBOOK_DOCUMENT_DID_CHANGE, handler)
    }

    /** Handles `notebookDocument/didSave` notifications. */
    public fun didSave(handler: suspend HandlerContext.(DidSaveNotebookDocumentParams) -> Unit) {
        notification(LspMethods.NOTEBOOK_DOCUMENT_DID_SAVE, handler)
    }

    /** Handles `notebookDocument/didClose` notifications. */
    public fun didClose(handler: suspend HandlerContext.(DidCloseNotebookDocumentParams) -> Unit) {
        notification(LspMethods.NOTEBOOK_DOCUMENT_DID_CLOSE, handler)
    }
}
