package io.lsp4k.server

import io.lsp4k.protocol.CreateFilesParams
import io.lsp4k.protocol.DeleteFilesParams
import io.lsp4k.protocol.DidChangeConfigurationParams
import io.lsp4k.protocol.DidChangeWatchedFilesParams
import io.lsp4k.protocol.DidChangeWorkspaceFoldersParams
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.ExecuteCommandParams
import io.lsp4k.protocol.FileChangeType
import io.lsp4k.protocol.FileCreate
import io.lsp4k.protocol.FileDelete
import io.lsp4k.protocol.FileEvent
import io.lsp4k.protocol.FileRename
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.RenameFilesParams
import io.lsp4k.protocol.SymbolKind
import io.lsp4k.protocol.TextDocumentContentParams
import io.lsp4k.protocol.TextDocumentContentResult
import io.lsp4k.protocol.WorkspaceDiagnosticParams
import io.lsp4k.protocol.WorkspaceDiagnosticReport
import io.lsp4k.protocol.WorkspaceEdit
import io.lsp4k.protocol.WorkspaceFolder
import io.lsp4k.protocol.WorkspaceFoldersChangeEvent
import io.lsp4k.protocol.WorkspaceSymbol
import io.lsp4k.protocol.WorkspaceSymbolParams
import io.lsp4k.protocol.WorkspaceSymbolResult
import io.lsp4k.protocol.WorkspaceSymbolUriLocation
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkspaceHandlersTest {
    private val uri = "file:///workspace/src/Main.kt"
    private val mainSymbol = WorkspaceSymbol("Main", SymbolKind.Class, location = Either.right(WorkspaceSymbolUriLocation(uri)))
    private val edit = WorkspaceEdit(changes = emptyMap())

    @Test
    fun `workspace requests reach typed handlers and encode their results`() =
        runTest {
            val received = mutableListOf<Any>()
            val symbols: WorkspaceSymbolResult = WorkspaceSymbolResult.Symbols(listOf(mainSymbol))
            val peer =
                startPeer(backgroundScope) {
                    workspace {
                        symbol {
                            received += it
                            symbols
                        }
                        workspaceSymbolResolve { it.copy(containerName = "app") }
                        executeCommand {
                            received += it
                            JsonPrimitive("done")
                        }
                        workspaceDiagnostic { WorkspaceDiagnosticReport(items = emptyList()) }
                        textDocumentContent { TextDocumentContentResult("virtual ${it.uri}") }
                        willCreateFiles { edit }
                        willRenameFiles { edit }
                        willDeleteFiles { null }
                    }
                }
            peer.initialize()

            val query = WorkspaceSymbolParams(query = "Ma")
            peer.expect(LspMethods.WORKSPACE_SYMBOL, query, symbols)
            peer.expect(LspMethods.WORKSPACE_SYMBOL_RESOLVE, mainSymbol, mainSymbol.copy(containerName = "app"))
            val command = ExecuteCommandParams("app.build", listOf(JsonPrimitive(1)))
            peer.expect<ExecuteCommandParams, JsonElement>(LspMethods.WORKSPACE_EXECUTE_COMMAND, command, JsonPrimitive("done"))
            val diagnostics = WorkspaceDiagnosticParams(previousResultIds = emptyList())
            peer.expect(LspMethods.WORKSPACE_DIAGNOSTIC, diagnostics, WorkspaceDiagnosticReport(items = emptyList()))
            val content = TextDocumentContentParams("lsp4k:/virtual")
            peer.expect(LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT, content, TextDocumentContentResult("virtual lsp4k:/virtual"))
            val created = CreateFilesParams(listOf(FileCreate(uri)))
            peer.expect(LspMethods.WORKSPACE_WILL_CREATE_FILES, created, edit)
            val renamed = RenameFilesParams(listOf(FileRename(uri, "$uri.bak")))
            peer.expect(LspMethods.WORKSPACE_WILL_RENAME_FILES, renamed, edit)
            val deleted = DeleteFilesParams(listOf(FileDelete(uri)))
            assertEquals(null, peer.call<DeleteFilesParams, WorkspaceEdit>(LspMethods.WORKSPACE_WILL_DELETE_FILES, deleted))
            assertEquals(listOf<Any>(query, command), received)
        }

    @Test
    fun `workspace notifications reach typed handlers in order`() =
        runTest {
            val received = mutableListOf<Any>()
            val peer =
                startPeer(backgroundScope) {
                    workspace {
                        didChangeConfiguration { received += it }
                        didChangeWatchedFiles { received += it }
                        didChangeWorkspaceFolders { received += it }
                        didCreateFiles { received += it }
                        didRenameFiles { received += it }
                        didDeleteFiles { received += it }
                    }
                }
            peer.initialize()
            val configuration = DidChangeConfigurationParams(settings = JsonPrimitive(true))
            val watched = DidChangeWatchedFilesParams(listOf(FileEvent(uri, FileChangeType.Changed)))
            val folders = DidChangeWorkspaceFoldersParams(WorkspaceFoldersChangeEvent(listOf(WorkspaceFolder(uri, "src")), emptyList()))
            val created = CreateFilesParams(listOf(FileCreate(uri)))
            val renamed = RenameFilesParams(listOf(FileRename(uri, "$uri.bak")))
            val deleted = DeleteFilesParams(listOf(FileDelete("$uri.bak")))

            peer.send(LspMethods.WORKSPACE_DID_CHANGE_CONFIGURATION, configuration)
            peer.send(LspMethods.WORKSPACE_DID_CHANGE_WATCHED_FILES, watched)
            peer.send(LspMethods.WORKSPACE_DID_CHANGE_WORKSPACE_FOLDERS, folders)
            peer.send(LspMethods.WORKSPACE_DID_CREATE_FILES, created)
            peer.send(LspMethods.WORKSPACE_DID_RENAME_FILES, renamed)
            peer.send(LspMethods.WORKSPACE_DID_DELETE_FILES, deleted)
            peer.sync()

            assertEquals(listOf<Any>(configuration, watched, folders, created, renamed, deleted), received)
        }
}
