package io.lsp4k.client

import io.lsp4k.protocol.DidChangeNotebookDocumentParams
import io.lsp4k.protocol.DidCloseNotebookDocumentParams
import io.lsp4k.protocol.DidOpenNotebookDocumentParams
import io.lsp4k.protocol.DidSaveNotebookDocumentParams
import io.lsp4k.protocol.LspMethods
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

private const val CELL_URI: String = "file:///n.ipynb#c1"
private const val NB_DOC: String =
    """{"uri":"file:///n.ipynb","notebookType":"jupyter","version":1,"cells":[{"kind":2,"document":"$CELL_URI"}]}"""
private const val CELL_TEXT: String = """{"uri":"$CELL_URI","languageId":"python","version":1,"text":"x"}"""

internal val notebookCases: List<CallCase> =
    listOf(
        notificationCase<DidOpenNotebookDocumentParams>(
            LspMethods.NOTEBOOK_DOCUMENT_DID_OPEN,
            """{"notebookDocument":$NB_DOC,"cellTextDocuments":[$CELL_TEXT]}""",
        ) { notebook.didOpen(it) },
        notificationCase<DidChangeNotebookDocumentParams>(
            LspMethods.NOTEBOOK_DOCUMENT_DID_CHANGE,
            """{"notebookDocument":{"uri":"file:///n.ipynb","version":2},"change":{}}""",
        ) { notebook.didChange(it) },
        notificationCase<DidSaveNotebookDocumentParams>(
            LspMethods.NOTEBOOK_DOCUMENT_DID_SAVE,
            """{$NB_ID}""",
        ) { notebook.didSave(it) },
        notificationCase<DidCloseNotebookDocumentParams>(
            LspMethods.NOTEBOOK_DOCUMENT_DID_CLOSE,
            """{$NB_ID,"cellTextDocuments":[{"uri":"file:///n.ipynb#c1"}]}""",
        ) { notebook.didClose(it) },
    )

class NotebookCallsTest {
    @Test
    fun `every call reaches the server with its method and params`() =
        runTest {
            runCases(backgroundScope, notebookCases)
        }
}
