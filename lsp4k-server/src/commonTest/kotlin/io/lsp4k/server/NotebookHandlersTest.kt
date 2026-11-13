package io.lsp4k.server

import io.lsp4k.protocol.DidChangeNotebookDocumentParams
import io.lsp4k.protocol.DidCloseNotebookDocumentParams
import io.lsp4k.protocol.DidOpenNotebookDocumentParams
import io.lsp4k.protocol.DidSaveNotebookDocumentParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.NotebookDocument
import io.lsp4k.protocol.NotebookDocumentChangeEvent
import io.lsp4k.protocol.NotebookDocumentIdentifier
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.protocol.TextDocumentItem
import io.lsp4k.protocol.VersionedNotebookDocumentIdentifier
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NotebookHandlersTest {
    @Test
    fun `notebook synchronization notifications reach typed handlers in order`() =
        runTest {
            val received = mutableListOf<Any>()
            val peer =
                startPeer(backgroundScope) {
                    notebook {
                        didOpen { received += it }
                        didChange { received += it }
                        didSave { received += it }
                        didClose { received += it }
                    }
                }
            peer.initialize()
            val uri = "file:///analysis.ipynb"
            val cell = "vscode-notebook-cell:/analysis.ipynb#cell1"
            val opened =
                DidOpenNotebookDocumentParams(
                    NotebookDocument(uri, "jupyter-notebook", 1, cells = emptyList()),
                    listOf(TextDocumentItem(cell, "python", 1, "print(1)")),
                )
            val changed = DidChangeNotebookDocumentParams(VersionedNotebookDocumentIdentifier(uri, 2), NotebookDocumentChangeEvent())
            val saved = DidSaveNotebookDocumentParams(NotebookDocumentIdentifier(uri))
            val closed = DidCloseNotebookDocumentParams(NotebookDocumentIdentifier(uri), listOf(TextDocumentIdentifier(cell)))

            peer.send(LspMethods.NOTEBOOK_DOCUMENT_DID_OPEN, opened)
            peer.send(LspMethods.NOTEBOOK_DOCUMENT_DID_CHANGE, changed)
            peer.send(LspMethods.NOTEBOOK_DOCUMENT_DID_SAVE, saved)
            peer.send(LspMethods.NOTEBOOK_DOCUMENT_DID_CLOSE, closed)
            peer.sync()

            assertEquals(listOf<Any>(opened, changed, saved, closed), received)
        }
}
