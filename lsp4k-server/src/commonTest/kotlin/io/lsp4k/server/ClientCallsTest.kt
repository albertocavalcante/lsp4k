package io.lsp4k.server

import io.lsp4k.protocol.ApplyWorkspaceEditParams
import io.lsp4k.protocol.ApplyWorkspaceEditResult
import io.lsp4k.protocol.CancelParams
import io.lsp4k.protocol.ConfigurationItem
import io.lsp4k.protocol.ConfigurationParams
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.ExecuteCommandParams
import io.lsp4k.protocol.LogMessageParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.MessageActionItem
import io.lsp4k.protocol.MessageType
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.ProgressParams
import io.lsp4k.protocol.PublishDiagnosticsParams
import io.lsp4k.protocol.Range
import io.lsp4k.protocol.ShowDocumentParams
import io.lsp4k.protocol.ShowDocumentResult
import io.lsp4k.protocol.ShowMessageRequestParams
import io.lsp4k.protocol.TextDocumentContentRefreshParams
import io.lsp4k.protocol.TextEdit
import io.lsp4k.protocol.TraceValue
import io.lsp4k.protocol.WorkspaceEdit
import io.lsp4k.protocol.WorkspaceFolder
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientCallsTest {
    @Test
    fun `window calls reach the client with spec shapes and typed results`() =
        runTest {
            val peer = startPeer(backgroundScope) {}
            peer.initialize()
            val window = peer.server.client.window

            window.showMessage(MessageType.Warning, "careful")
            assertMessage(peer.notifications.receive(), LspMethods.WINDOW_SHOW_MESSAGE, """{"type":2,"message":"careful"}""")
            window.logMessage(LogMessageParams(MessageType.Log, "logged"))
            assertMessage(peer.notifications.receive(), LspMethods.WINDOW_LOG_MESSAGE, """{"type":4,"message":"logged"}""")

            val question = ShowMessageRequestParams(MessageType.Info, "pick", listOf(MessageActionItem("A"), MessageActionItem("B")))
            val picked =
                peer.answer(this, LspMethods.WINDOW_SHOW_MESSAGE_REQUEST, wire(question), json("""{"title":"B"}""")) {
                    window.showMessageRequest(question)
                }
            assertEquals(MessageActionItem("B"), picked)
            val dismissed = ShowMessageRequestParams(MessageType.Error, "dismiss")
            val none = peer.answer(this, LspMethods.WINDOW_SHOW_MESSAGE_REQUEST, wire(dismissed)) { window.showMessageRequest(dismissed) }
            assertEquals(null, none)

            val document = ShowDocumentParams("https://example.com/docs", external = true)
            val documentJson = json("""{"uri":"https://example.com/docs","external":true}""")
            val shown =
                peer.answer(this, LspMethods.WINDOW_SHOW_DOCUMENT, documentJson, json("""{"success":true}""")) {
                    window.showDocument(document)
                }
            assertEquals(ShowDocumentResult(success = true), shown)
        }

    @Test
    fun `workspace calls reach the client with spec shapes and typed results`() =
        runTest {
            val peer = startPeer(backgroundScope) {}
            peer.initialize()
            val workspace = peer.server.client.workspace

            val edit = WorkspaceEdit(changes = mapOf("file:///a.kt" to listOf(TextEdit(Range(Position(0, 0), Position(0, 1)), "b"))))
            val apply = ApplyWorkspaceEditParams(label = "Rename", edit = edit)
            val applied =
                peer.answer(this, LspMethods.WORKSPACE_APPLY_EDIT, wire(apply), json("""{"applied":false,"failureReason":"read-only"}""")) {
                    workspace.applyEdit(apply)
                }
            assertEquals(ApplyWorkspaceEditResult(applied = false, failureReason = "read-only"), applied)

            val configuration = ConfigurationParams(listOf(ConfigurationItem(section = "notes")))
            val values =
                peer.answer(this, LspMethods.WORKSPACE_CONFIGURATION, wire(configuration), json("""[{"width":80}]""")) {
                    workspace.configuration(configuration)
                }
            assertEquals(listOf(json("""{"width":80}""")), values)

            val folders =
                peer.answer(this, LspMethods.WORKSPACE_WORKSPACE_FOLDERS, null, json("""[{"uri":"file:///ws","name":"ws"}]""")) {
                    workspace.workspaceFolders()
                }
            assertEquals(listOf(WorkspaceFolder("file:///ws", "ws")), folders)
            assertEquals(null, peer.answer(this, LspMethods.WORKSPACE_WORKSPACE_FOLDERS, null) { workspace.workspaceFolders() })

            peer.answer(this, LspMethods.WORKSPACE_CODE_LENS_REFRESH, null) { workspace.codeLensRefresh() }
            peer.answer(this, LspMethods.WORKSPACE_SEMANTIC_TOKENS_REFRESH, null) { workspace.semanticTokensRefresh() }
            peer.answer(this, LspMethods.WORKSPACE_INLAY_HINT_REFRESH, null) { workspace.inlayHintRefresh() }
            peer.answer(this, LspMethods.WORKSPACE_INLINE_VALUE_REFRESH, null) { workspace.inlineValueRefresh() }
            peer.answer(this, LspMethods.WORKSPACE_DIAGNOSTIC_REFRESH, null) { workspace.diagnosticRefresh() }
            peer.answer(this, LspMethods.WORKSPACE_FOLDING_RANGE_REFRESH, null) { workspace.foldingRangeRefresh() }
            val refresh = TextDocumentContentRefreshParams("lsp4k:/virtual")
            peer.answer(this, LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT_REFRESH, json("""{"uri":"lsp4k:/virtual"}""")) {
                workspace.textDocumentContentRefresh(refresh)
            }
        }

    @Test
    fun `notifications to the client use the spec shapes`() =
        runTest {
            val peer = startPeer(backgroundScope) {}
            peer.initialize()
            val client = peer.server.client

            client.textDocument.publishDiagnostics(PublishDiagnosticsParams(uri = "file:///a.kt", version = 3, diagnostics = emptyList()))
            val diagnostics = """{"uri":"file:///a.kt","version":3,"diagnostics":[]}"""
            assertMessage(peer.notifications.receive(), LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS, diagnostics)
            client.telemetryEvent(json("""{"event":"opened"}"""))
            assertMessage(peer.notifications.receive(), LspMethods.TELEMETRY_EVENT, """{"event":"opened"}""")
            client.progress(ProgressParams(Either.left(1), json("""{"kind":"report","percentage":5}""")))
            assertMessage(peer.notifications.receive(), LspMethods.PROGRESS, """{"token":1,"value":{"kind":"report","percentage":5}}""")
            client.cancelRequest(CancelParams(JsonPrimitive(9)))
            assertMessage(peer.notifications.receive(), LspMethods.CANCEL_REQUEST, """{"id":9}""")
        }

    @Test
    fun `a handler may await a client request`() =
        runTest {
            val peer =
                startPeer(backgroundScope) {
                    workspace {
                        executeCommand {
                            val folders = client.workspace.workspaceFolders().orEmpty()
                            JsonPrimitive(folders.joinToString { it.name })
                        }
                    }
                }
            peer.initialize()

            val params = wire(ExecuteCommandParams("app.ask"))
            val command = backgroundScope.async { peer.request(LspMethods.WORKSPACE_EXECUTE_COMMAND, params) }
            val request = peer.requests.receive()
            assertEquals(LspMethods.WORKSPACE_WORKSPACE_FOLDERS, request.method())
            peer.respond(request, json("""[{"uri":"file:///ws","name":"ws"}]"""))
            assertEquals(JsonPrimitive("ws"), command.await().result())
        }

    @Test
    fun `every server to client method has a typed call`() =
        runTest {
            val peer = startPeer(backgroundScope) {}
            peer.initialize(initializeParams(trace = TraceValue.Verbose))
            val seen = mutableSetOf<String>()
            backgroundScope.launch {
                for (request in peer.requests) {
                    seen += request.method()
                    peer.respond(request, cannedResult(request.method()))
                }
            }
            val client = peer.server.client
            val edit = ApplyWorkspaceEditParams(edit = WorkspaceEdit())

            client.window.showMessage(MessageType.Info, "message")
            client.window.logMessage(MessageType.Info, "log")
            client.window.showMessageRequest(ShowMessageRequestParams(MessageType.Info, "question"))
            client.window.showDocument(ShowDocumentParams("file:///a.kt"))
            client.window.createWorkDoneProgress().begin("Work")
            client.workspace.applyEdit(edit)
            client.workspace.configuration(ConfigurationParams(emptyList()))
            client.workspace.workspaceFolders()
            client.workspace.codeLensRefresh()
            client.workspace.semanticTokensRefresh()
            client.workspace.inlayHintRefresh()
            client.workspace.inlineValueRefresh()
            client.workspace.diagnosticRefresh()
            client.workspace.foldingRangeRefresh()
            client.workspace.textDocumentContentRefresh(TextDocumentContentRefreshParams("lsp4k:/virtual"))
            client.registration.register(LspMethods.TEXT_DOCUMENT_HOVER).unregister()
            client.textDocument.publishDiagnostics(PublishDiagnosticsParams(uri = "file:///a.kt", diagnostics = emptyList()))
            client.telemetryEvent(JsonObject(emptyMap()))
            assertTrue(client.logTrace("trace"))
            client.cancelRequest(CancelParams(JsonPrimitive(1)))
            peer.sync()
            while (true) {
                val notification = peer.notifications.tryReceive().getOrNull() ?: break
                seen += notification.method()
            }

            val expected =
                setOf(
                    LspMethods.CLIENT_REGISTER_CAPABILITY,
                    LspMethods.CLIENT_UNREGISTER_CAPABILITY,
                    LspMethods.WINDOW_SHOW_DOCUMENT,
                    LspMethods.WINDOW_SHOW_MESSAGE_REQUEST,
                    LspMethods.WINDOW_WORK_DONE_PROGRESS_CREATE,
                    LspMethods.WORKSPACE_APPLY_EDIT,
                    LspMethods.WORKSPACE_CODE_LENS_REFRESH,
                    LspMethods.WORKSPACE_CONFIGURATION,
                    LspMethods.WORKSPACE_DIAGNOSTIC_REFRESH,
                    LspMethods.WORKSPACE_FOLDING_RANGE_REFRESH,
                    LspMethods.WORKSPACE_INLAY_HINT_REFRESH,
                    LspMethods.WORKSPACE_INLINE_VALUE_REFRESH,
                    LspMethods.WORKSPACE_SEMANTIC_TOKENS_REFRESH,
                    LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT_REFRESH,
                    LspMethods.WORKSPACE_WORKSPACE_FOLDERS,
                    LspMethods.CANCEL_REQUEST,
                    LspMethods.LOG_TRACE,
                    LspMethods.PROGRESS,
                    LspMethods.TELEMETRY_EVENT,
                    LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS,
                    LspMethods.WINDOW_LOG_MESSAGE,
                    LspMethods.WINDOW_SHOW_MESSAGE,
                )
            assertEquals(expected, seen)
        }

    private fun cannedResult(method: String): JsonElement =
        when (method) {
            LspMethods.WINDOW_SHOW_DOCUMENT -> json("""{"success":true}""")
            LspMethods.WORKSPACE_APPLY_EDIT -> json("""{"applied":true}""")
            LspMethods.WORKSPACE_CONFIGURATION -> json("[]")
            else -> json("null")
        }
}
