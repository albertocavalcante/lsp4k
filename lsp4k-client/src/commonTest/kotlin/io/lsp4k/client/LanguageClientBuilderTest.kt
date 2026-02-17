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
import io.lsp4k.protocol.MessageActionItem
import io.lsp4k.protocol.WorkspaceFolder
import kotlin.test.Test

/**
 * Tests for the language client builder DSL: LanguageClientBuilder,
 * notification handler registration, request handler registration.
 */
class LanguageClientBuilderTest {
    // ==================== DSL Entry Point Tests ====================

    @Test
    fun `languageClient DSL builds config with defaults`() {
        val config = languageClient {}
        config.clientInfo.shouldBeNull()
        config.rootUri.shouldBeNull()
        config.notificationHandlers shouldBe emptyMap()
        config.requestHandlers shouldBe emptyMap()
    }

    @Test
    fun `languageClient DSL sets clientInfo`() {
        val config =
            languageClient {
                clientInfo("test-client", "1.0.0")
            }
        val clientInfo = config.clientInfo.shouldNotBeNull()
        clientInfo.name shouldBe "test-client"
        clientInfo.version shouldBe "1.0.0"
    }

    @Test
    fun `languageClient DSL sets clientInfo without version`() {
        val config =
            languageClient {
                clientInfo("minimal-client")
            }
        val clientInfo = config.clientInfo.shouldNotBeNull()
        clientInfo.name shouldBe "minimal-client"
        clientInfo.version.shouldBeNull()
    }

    @Test
    fun `languageClient DSL sets rootUri`() {
        val config =
            languageClient {
                rootUri("file:///workspace/project")
            }
        config.rootUri shouldBe "file:///workspace/project"
    }

    // ==================== Notification Handler Registration Tests ====================

    @Test
    fun `onShowMessage handler registration`() {
        val config =
            languageClient {
                onShowMessage { _, _ -> }
            }
        config.notificationHandlers shouldContainKey LspMethods.WINDOW_SHOW_MESSAGE
    }

    @Test
    fun `onPublishDiagnostics handler registration`() {
        val config =
            languageClient {
                onPublishDiagnostics { _, _ -> }
            }
        config.notificationHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS
    }

    @Test
    fun `onLogMessage handler registration`() {
        val config =
            languageClient {
                onLogMessage { _, _ -> }
            }
        config.notificationHandlers shouldContainKey LspMethods.WINDOW_LOG_MESSAGE
    }

    @Test
    fun `onTelemetryEvent handler registration`() {
        val config =
            languageClient {
                onTelemetryEvent { _ -> }
            }
        config.notificationHandlers shouldContainKey LspMethods.TELEMETRY_EVENT
    }

    @Test
    fun `onProgress handler registration`() {
        val config =
            languageClient {
                onProgress { _ -> }
            }
        config.notificationHandlers shouldContainKey LspMethods.PROGRESS
    }

    @Test
    fun `multiple notification handlers can be registered`() {
        val config =
            languageClient {
                onShowMessage { _, _ -> }
                onPublishDiagnostics { _, _ -> }
                onLogMessage { _, _ -> }
                onTelemetryEvent { _ -> }
                onProgress { _ -> }
            }
        config.notificationHandlers shouldContainKey LspMethods.WINDOW_SHOW_MESSAGE
        config.notificationHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS
        config.notificationHandlers shouldContainKey LspMethods.WINDOW_LOG_MESSAGE
        config.notificationHandlers shouldContainKey LspMethods.TELEMETRY_EVENT
        config.notificationHandlers shouldContainKey LspMethods.PROGRESS
        config.notificationHandlers.size shouldBe 5
    }

    // ==================== Request Handler Registration Tests ====================

    @Test
    fun `onShowMessageRequest handler registration`() {
        val config =
            languageClient {
                onShowMessageRequest { _ -> null }
            }
        config.requestHandlers shouldContainKey LspMethods.WINDOW_SHOW_MESSAGE_REQUEST
    }

    @Test
    fun `onApplyEdit handler registration`() {
        val config =
            languageClient {
                onApplyEdit { _ -> ApplyWorkspaceEditResult(applied = true) }
            }
        config.requestHandlers shouldContainKey LspMethods.WORKSPACE_APPLY_EDIT
    }

    @Test
    fun `onWorkspaceFolders handler registration`() {
        val config =
            languageClient {
                onWorkspaceFolders { null }
            }
        config.requestHandlers shouldContainKey LspMethods.WORKSPACE_WORKSPACE_FOLDERS
    }

    @Test
    fun `onConfiguration handler registration`() {
        val config =
            languageClient {
                onConfiguration { _ -> emptyList() }
            }
        config.requestHandlers shouldContainKey LspMethods.WORKSPACE_CONFIGURATION
    }

    @Test
    fun `multiple request handlers can be registered`() {
        val config =
            languageClient {
                onShowMessageRequest { _ -> null }
                onApplyEdit { _ -> ApplyWorkspaceEditResult(applied = true) }
                onWorkspaceFolders { null }
                onConfiguration { _ -> emptyList() }
            }
        config.requestHandlers.size shouldBe 4
    }

    // ==================== Custom Handler Registration Tests ====================

    @Test
    fun `custom notification handler registration`() {
        val config =
            languageClient {
                onNotification("custom/serverNotification", NotificationHandler { _ -> })
            }
        config.notificationHandlers shouldContainKey "custom/serverNotification"
    }

    @Test
    fun `custom request handler registration`() {
        val config =
            languageClient {
                onRequest("custom/serverRequest", RequestHandler { _ -> null })
            }
        config.requestHandlers shouldContainKey "custom/serverRequest"
    }

    @Test
    fun `custom handlers do not conflict with standard handlers`() {
        val config =
            languageClient {
                onShowMessage { _, _ -> }
                onNotification("custom/myNotification", NotificationHandler { _ -> })
                onRequest("custom/myRequest", RequestHandler { _ -> null })
            }
        config.notificationHandlers shouldContainKey LspMethods.WINDOW_SHOW_MESSAGE
        config.notificationHandlers shouldContainKey "custom/myNotification"
        config.requestHandlers shouldContainKey "custom/myRequest"
    }

    // ==================== Handler Isolation Tests ====================

    @Test
    fun `no handlers registered for unregistered methods`() {
        val config =
            languageClient {
                onShowMessage { _, _ -> }
            }
        config.notificationHandlers shouldContainKey LspMethods.WINDOW_SHOW_MESSAGE
        config.notificationHandlers shouldNotContainKey LspMethods.WINDOW_LOG_MESSAGE
        config.notificationHandlers shouldNotContainKey LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS
        config.requestHandlers shouldBe emptyMap()
    }

    // ==================== Full Configuration Tests ====================

    @Test
    fun `full client configuration builds correctly`() {
        val config =
            languageClient {
                clientInfo("my-editor", "3.0.0")
                rootUri("file:///home/user/project")

                onShowMessage { _, _ -> }
                onPublishDiagnostics { _, _ -> }
                onLogMessage { _, _ -> }
                onProgress { _ -> }

                onShowMessageRequest { _ ->
                    MessageActionItem(title = "OK")
                }
                onApplyEdit { _ ->
                    ApplyWorkspaceEditResult(applied = true)
                }
                onWorkspaceFolders {
                    listOf(
                        WorkspaceFolder(
                            uri = "file:///home/user/project",
                            name = "project",
                        ),
                    )
                }
                onConfiguration { _ -> emptyList() }
            }

        val clientInfo = config.clientInfo.shouldNotBeNull()
        clientInfo.name shouldBe "my-editor"
        clientInfo.version shouldBe "3.0.0"
        config.rootUri shouldBe "file:///home/user/project"

        config.notificationHandlers.size shouldBe 4
        config.requestHandlers.size shouldBe 4
    }

    // ==================== Capabilities Tests ====================

    @Test
    fun `capabilities block configures client capabilities`() {
        val config =
            languageClient {
                capabilities {
                    // ClientCapabilities is a data class, verify block executes
                }
            }
        config.capabilities.shouldNotBeNull()
    }
}
