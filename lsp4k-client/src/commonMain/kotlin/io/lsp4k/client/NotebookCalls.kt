package io.lsp4k.client

import io.lsp4k.protocol.DidChangeNotebookDocumentParams
import io.lsp4k.protocol.DidCloseNotebookDocumentParams
import io.lsp4k.protocol.DidOpenNotebookDocumentParams
import io.lsp4k.protocol.DidSaveNotebookDocumentParams
import io.lsp4k.protocol.LspMethods

/**
 * Notebook document synchronization notifications (the `notebookDocument` namespace).
 */
public class NotebookCalls internal constructor(
    private val caller: ServerCaller,
) {
    /**
     * Notify the server that a notebook document was opened.
     */
    public suspend fun didOpen(params: DidOpenNotebookDocumentParams): Unit =
        caller.notify(LspMethods.NOTEBOOK_DOCUMENT_DID_OPEN, params)

    /**
     * Notify the server that a notebook document was changed.
     */
    public suspend fun didChange(params: DidChangeNotebookDocumentParams): Unit =
        caller.notify(LspMethods.NOTEBOOK_DOCUMENT_DID_CHANGE, params)

    /**
     * Notify the server that a notebook document was saved.
     */
    public suspend fun didSave(params: DidSaveNotebookDocumentParams): Unit =
        caller.notify(LspMethods.NOTEBOOK_DOCUMENT_DID_SAVE, params)

    /**
     * Notify the server that a notebook document was closed.
     */
    public suspend fun didClose(params: DidCloseNotebookDocumentParams): Unit =
        caller.notify(LspMethods.NOTEBOOK_DOCUMENT_DID_CLOSE, params)
}
