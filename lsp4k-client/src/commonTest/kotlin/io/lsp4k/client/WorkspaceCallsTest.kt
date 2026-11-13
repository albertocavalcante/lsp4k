package io.lsp4k.client

import io.lsp4k.protocol.CreateFilesParams
import io.lsp4k.protocol.DeleteFilesParams
import io.lsp4k.protocol.DidChangeConfigurationParams
import io.lsp4k.protocol.DidChangeWatchedFilesParams
import io.lsp4k.protocol.DidChangeWorkspaceFoldersParams
import io.lsp4k.protocol.ExecuteCommandParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.RenameFilesParams
import io.lsp4k.protocol.TextDocumentContentParams
import io.lsp4k.protocol.WorkspaceDiagnosticParams
import io.lsp4k.protocol.WorkspaceSymbol
import io.lsp4k.protocol.WorkspaceSymbolParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal val workspaceCases: List<CallCase> =
    listOf(
        requestCase<WorkspaceSymbolParams>(
            LspMethods.WORKSPACE_SYMBOL,
            """{"query":"q"}""",
        ) { workspace.symbol(it) },
        requestCase<WorkspaceSymbol>(
            LspMethods.WORKSPACE_SYMBOL_RESOLVE,
            """{"name":"n","kind":12,"location":{"uri":"file:///a.kt"}}""",
        ) { workspace.workspaceSymbolResolve(it) },
        requestCase<ExecuteCommandParams>(
            LspMethods.WORKSPACE_EXECUTE_COMMAND,
            """{"command":"c"}""",
        ) { workspace.executeCommand(it) },
        requestCase<WorkspaceDiagnosticParams>(
            LspMethods.WORKSPACE_DIAGNOSTIC,
            """{"previousResultIds":[]}""",
        ) { workspace.workspaceDiagnostic(it) },
        requestCase<TextDocumentContentParams>(
            LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT,
            """{"uri":"example:doc"}""",
        ) { workspace.textDocumentContent(it) },
        notificationCase<DidChangeConfigurationParams>(
            LspMethods.WORKSPACE_DID_CHANGE_CONFIGURATION,
            """{"settings":{"a":1}}""",
        ) { workspace.didChangeConfiguration(it) },
        notificationCase<DidChangeWatchedFilesParams>(
            LspMethods.WORKSPACE_DID_CHANGE_WATCHED_FILES,
            """{"changes":[{"uri":"file:///a.kt","type":1}]}""",
        ) { workspace.didChangeWatchedFiles(it) },
        notificationCase<DidChangeWorkspaceFoldersParams>(
            LspMethods.WORKSPACE_DID_CHANGE_WORKSPACE_FOLDERS,
            """{"event":{"added":[{"uri":"file:///w","name":"w"}],"removed":[]}}""",
        ) { workspace.didChangeWorkspaceFolders(it) },
        requestCase<CreateFilesParams>(
            LspMethods.WORKSPACE_WILL_CREATE_FILES,
            """{"files":[{"uri":"file:///n.kt"}]}""",
        ) { workspace.willCreateFiles(it) },
        notificationCase<CreateFilesParams>(
            LspMethods.WORKSPACE_DID_CREATE_FILES,
            """{"files":[{"uri":"file:///n.kt"}]}""",
        ) { workspace.didCreateFiles(it) },
        requestCase<RenameFilesParams>(
            LspMethods.WORKSPACE_WILL_RENAME_FILES,
            """{"files":[{"oldUri":"file:///a.kt","newUri":"file:///b.kt"}]}""",
        ) { workspace.willRenameFiles(it) },
        notificationCase<RenameFilesParams>(
            LspMethods.WORKSPACE_DID_RENAME_FILES,
            """{"files":[{"oldUri":"file:///a.kt","newUri":"file:///b.kt"}]}""",
        ) { workspace.didRenameFiles(it) },
        requestCase<DeleteFilesParams>(
            LspMethods.WORKSPACE_WILL_DELETE_FILES,
            """{"files":[{"uri":"file:///a.kt"}]}""",
        ) { workspace.willDeleteFiles(it) },
        notificationCase<DeleteFilesParams>(
            LspMethods.WORKSPACE_DID_DELETE_FILES,
            """{"files":[{"uri":"file:///a.kt"}]}""",
        ) { workspace.didDeleteFiles(it) },
    )

class WorkspaceCallsTest {
    @Test
    fun `every call reaches the server with its method and params`() =
        runTest {
            runCases(backgroundScope, workspaceCases)
        }
}
