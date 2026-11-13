package io.lsp4k.client

import io.jsonrpc4k.core.NotificationHandler
import io.jsonrpc4k.core.RequestHandler
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.lsp4k.protocol.ApplyWorkspaceEditResult
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.ShowDocumentResult
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Tests for the language client builder DSL: namespace registration, custom handlers and
 * duplicate detection.
 */
class LanguageClientBuilderTest {
    @Test
    fun `defaults are empty`() {
        val config = languageClient {}
        config.clientInfo.shouldBeNull()
        config.rootUri.shouldBeNull()
        config.notificationHandlers shouldBe emptyMap()
        config.requestHandlers shouldBe emptyMap()
    }

    @Test
    fun `client info and root uri are recorded`() {
        val config =
            languageClient {
                clientInfo("editor", "1.0.0")
                rootUri("file:///workspace")
            }
        val info = config.clientInfo.shouldNotBeNull()
        info.name shouldBe "editor"
        info.version shouldBe "1.0.0"
        config.rootUri shouldBe "file:///workspace"
    }

    @Test
    fun `capabilities block configures capabilities`() {
        languageClient { capabilities { } }.capabilities.shouldNotBeNull()
    }

    @Test
    fun `window hooks register their methods`() {
        val config =
            languageClient {
                window {
                    showMessage { }
                    logMessage { }
                    showMessageRequest { null }
                    showDocument { ShowDocumentResult(success = true) }
                    workDoneProgressCreate { }
                }
            }
        config.notificationHandlers.keys shouldBe setOf(LspMethods.WINDOW_SHOW_MESSAGE, LspMethods.WINDOW_LOG_MESSAGE)
        config.requestHandlers.keys shouldBe
            setOf(
                LspMethods.WINDOW_SHOW_MESSAGE_REQUEST,
                LspMethods.WINDOW_SHOW_DOCUMENT,
                LspMethods.WINDOW_WORK_DONE_PROGRESS_CREATE,
            )
    }

    @Test
    fun `workspace hooks register requests including every refresh`() {
        val config = languageClient { registerEveryHook() }
        config.requestHandlers shouldContainKey LspMethods.WORKSPACE_APPLY_EDIT
        config.requestHandlers shouldContainKey LspMethods.WORKSPACE_CONFIGURATION
        config.requestHandlers shouldContainKey LspMethods.WORKSPACE_WORKSPACE_FOLDERS
        config.requestHandlers shouldContainKey LspMethods.WORKSPACE_CODE_LENS_REFRESH
        config.requestHandlers shouldContainKey LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT_REFRESH
        config.requestHandlers shouldContainKey LspMethods.CLIENT_REGISTER_CAPABILITY
        config.requestHandlers shouldContainKey LspMethods.CLIENT_UNREGISTER_CAPABILITY
        config.notificationHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS
        config.notificationHandlers shouldContainKey LspMethods.TELEMETRY_EVENT
        config.notificationHandlers shouldContainKey LspMethods.LOG_TRACE
        config.notificationHandlers shouldContainKey LspMethods.PROGRESS
    }

    @Test
    fun `unregistered methods have no handler`() {
        val config = languageClient { window { showMessage { } } }
        config.notificationHandlers shouldContainKey LspMethods.WINDOW_SHOW_MESSAGE
        config.notificationHandlers shouldNotContainKey LspMethods.WINDOW_LOG_MESSAGE
        config.requestHandlers shouldBe emptyMap()
    }

    @Test
    fun `custom handlers sit beside standard ones`() {
        val config =
            languageClient {
                window { showMessage { } }
                onNotification("custom/notification", NotificationHandler { })
                onRequest("custom/request", RequestHandler { null })
            }
        config.notificationHandlers shouldContainKey "custom/notification"
        config.requestHandlers shouldContainKey "custom/request"
        config.notificationHandlers shouldContainKey LspMethods.WINDOW_SHOW_MESSAGE
    }

    @Test
    fun `duplicate notification handlers are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            languageClient {
                window { showMessage { } }
                onNotification(LspMethods.WINDOW_SHOW_MESSAGE, NotificationHandler { })
            }
        }
        assertFailsWith<IllegalArgumentException> {
            languageClient {
                onProgress { }
                onProgress { }
            }
        }
    }

    @Test
    fun `duplicate request handlers are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            languageClient {
                workspace { applyEdit { ApplyWorkspaceEditResult(applied = true) } }
                onRequest(LspMethods.WORKSPACE_APPLY_EDIT, RequestHandler { null })
            }
        }
        assertFailsWith<IllegalArgumentException> {
            languageClient {
                workspace { codeLensRefresh { } }
                workspace { codeLensRefresh { } }
            }
        }
    }
}
