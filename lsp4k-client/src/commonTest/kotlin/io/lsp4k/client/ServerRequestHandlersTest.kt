package io.lsp4k.client

import io.lsp4k.protocol.ApplyWorkspaceEditResult
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.MessageActionItem
import io.lsp4k.protocol.ProgressToken
import io.lsp4k.protocol.ShowDocumentResult
import io.lsp4k.protocol.WorkspaceFolder
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerRequestHandlersTest {
    @Test
    fun `window requests are answered by their hooks`() =
        runTest {
            val tokens = mutableListOf<ProgressToken>()
            val peer = TestPeer(backgroundScope)
            languageClient {
                window {
                    showMessageRequest { params -> params.actions?.firstOrNull() ?: MessageActionItem("none") }
                    showDocument { ShowDocumentResult(success = it.uri == "file:///a.kt") }
                    workDoneProgressCreate { tokens += it.token }
                }
            }.start(peer.transport, backgroundScope)

            val chosen =
                peer.call(
                    LspMethods.WINDOW_SHOW_MESSAGE_REQUEST,
                    """{"type":3,"message":"m","actions":[{"title":"A"},{"title":"B"}]}""",
                )
            assertEquals(Json.parseToJsonElement("""{"title":"A"}"""), chosen)
            val shown = peer.call(LspMethods.WINDOW_SHOW_DOCUMENT, """{"uri":"file:///a.kt"}""")
            assertEquals(Json.parseToJsonElement("""{"success":true}"""), shown)
            assertTrue(isNullResult(peer.call(LspMethods.WINDOW_WORK_DONE_PROGRESS_CREATE, """{"token":"t1"}""")))
            assertTrue(isNullResult(peer.call(LspMethods.WINDOW_WORK_DONE_PROGRESS_CREATE, """{"token":2}""")))
            assertEquals(listOf<ProgressToken>(Either.right("t1"), Either.left(2)), tokens)
        }

    @Test
    fun `dismissed message request answers null`() =
        runTest {
            val peer = TestPeer(backgroundScope)
            languageClient { window { showMessageRequest { null } } }.start(peer.transport, backgroundScope)
            assertTrue(isNullResult(peer.call(LspMethods.WINDOW_SHOW_MESSAGE_REQUEST, """{"type":3,"message":"m"}""")))
        }

    @Test
    fun `workspace requests are answered by their hooks`() =
        runTest {
            val peer = TestPeer(backgroundScope)
            languageClient {
                workspace {
                    applyEdit { ApplyWorkspaceEditResult(applied = true) }
                    configuration { params -> params.items.map { JsonPrimitive(it.section) } }
                    workspaceFolders { listOf(WorkspaceFolder("file:///w", "w")) }
                }
            }.start(peer.transport, backgroundScope)

            val applied = peer.call(LspMethods.WORKSPACE_APPLY_EDIT, """{"edit":{}}""")
            assertEquals(Json.parseToJsonElement("""{"applied":true}"""), applied)
            val values =
                peer.call(LspMethods.WORKSPACE_CONFIGURATION, """{"items":[{"section":"a"},{"section":"b"}]}""")
            assertEquals(Json.parseToJsonElement("""["a","b"]"""), values)
            val folders = peer.call(LspMethods.WORKSPACE_WORKSPACE_FOLDERS, null)
            assertEquals(Json.parseToJsonElement("""[{"uri":"file:///w","name":"w"}]"""), folders)
        }

    @Test
    fun `workspace folders answers null when none is open`() =
        runTest {
            val peer = TestPeer(backgroundScope)
            languageClient { workspace { workspaceFolders { null } } }.start(peer.transport, backgroundScope)
            assertTrue(isNullResult(peer.call(LspMethods.WORKSPACE_WORKSPACE_FOLDERS, null)))
        }

    @Test
    fun `every refresh request invokes its hook and answers null`() =
        runTest {
            val refreshed = mutableListOf<String>()
            val peer = TestPeer(backgroundScope)
            languageClient {
                workspace {
                    codeLensRefresh { refreshed += "codeLens" }
                    semanticTokensRefresh { refreshed += "semanticTokens" }
                    inlayHintRefresh { refreshed += "inlayHint" }
                    inlineValueRefresh { refreshed += "inlineValue" }
                    diagnosticRefresh { refreshed += "diagnostic" }
                    foldingRangeRefresh { refreshed += "foldingRange" }
                    textDocumentContentRefresh { refreshed += "content:${it.uri}" }
                }
            }.start(peer.transport, backgroundScope)

            val noParams =
                listOf(
                    LspMethods.WORKSPACE_CODE_LENS_REFRESH,
                    LspMethods.WORKSPACE_SEMANTIC_TOKENS_REFRESH,
                    LspMethods.WORKSPACE_INLAY_HINT_REFRESH,
                    LspMethods.WORKSPACE_INLINE_VALUE_REFRESH,
                    LspMethods.WORKSPACE_DIAGNOSTIC_REFRESH,
                    LspMethods.WORKSPACE_FOLDING_RANGE_REFRESH,
                )
            for (method in noParams) assertTrue(isNullResult(peer.call(method, null)), method)
            assertTrue(isNullResult(peer.call(LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT_REFRESH, """{"uri":"example:doc"}""")))
            assertEquals(
                listOf("codeLens", "semanticTokens", "inlayHint", "inlineValue", "diagnostic", "foldingRange", "content:example:doc"),
                refreshed,
            )
        }

    @Test
    fun `dynamic registration requests reach their hooks`() =
        runTest {
            val registered = mutableListOf<String>()
            val unregistered = mutableListOf<String>()
            val peer = TestPeer(backgroundScope)
            languageClient {
                client {
                    registerCapability { params -> registered += params.registrations.map { it.method } }
                    unregisterCapability { params -> unregistered += params.unregistrations.map { it.id } }
                }
            }.start(peer.transport, backgroundScope)

            val register =
                peer.call(
                    LspMethods.CLIENT_REGISTER_CAPABILITY,
                    """{"registrations":[{"id":"1","method":"textDocument/hover"}]}""",
                )
            assertTrue(isNullResult(register))
            val unregister =
                peer.call(
                    LspMethods.CLIENT_UNREGISTER_CAPABILITY,
                    """{"unregisterations":[{"id":"1","method":"textDocument/hover"}]}""",
                )
            assertTrue(isNullResult(unregister))
            assertEquals(listOf("textDocument/hover"), registered)
            assertEquals(listOf("1"), unregistered)
        }

    @Test
    fun `a request without a hook is answered with an error`() =
        runTest {
            val peer = TestPeer(backgroundScope)
            languageClient {}.start(peer.transport, backgroundScope)
            val failure = runCatching { peer.call(LspMethods.WORKSPACE_APPLY_EDIT, """{"edit":{}}""") }
            assertTrue(failure.isFailure)
        }
}
