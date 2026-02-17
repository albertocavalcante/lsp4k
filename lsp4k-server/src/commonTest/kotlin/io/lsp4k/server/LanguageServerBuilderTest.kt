package io.lsp4k.server

import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.lsp4k.jsonrpc.LspMethods
import io.lsp4k.protocol.CompletionOptions
import io.lsp4k.protocol.TextDocumentSyncKind
import kotlin.test.Test

/**
 * Tests for the language server builder DSL: LanguageServerBuilder,
 * ServerCapabilitiesBuilder, handler registration builders.
 */
class LanguageServerBuilderTest {

    // ==================== DSL Entry Point Tests ====================

    @Test
    fun `languageServer DSL builds config with default capabilities`() {
        val config = languageServer {}
        config.serverInfo.shouldBeNull()
        config.requestHandlers shouldBe emptyMap()
        config.notificationHandlers shouldBe emptyMap()
    }

    @Test
    fun `languageServer DSL sets serverInfo`() {
        val config = languageServer {
            serverInfo("test-server", "1.0.0")
        }
        val serverInfo = config.serverInfo.shouldNotBeNull()
        serverInfo.name shouldBe "test-server"
        serverInfo.version shouldBe "1.0.0"
    }

    @Test
    fun `languageServer DSL sets serverInfo without version`() {
        val config = languageServer {
            serverInfo("minimal-server")
        }
        val serverInfo = config.serverInfo.shouldNotBeNull()
        serverInfo.name shouldBe "minimal-server"
        serverInfo.version.shouldBeNull()
    }

    // ==================== ServerCapabilitiesBuilder Tests ====================

    @Test
    fun `capabilities builder sets hover provider`() {
        val config = languageServer {
            capabilities {
                hoverProvider = true
            }
        }
        config.capabilities.hoverProvider.shouldNotBeNull()
    }

    @Test
    fun `capabilities builder sets completion provider`() {
        val config = languageServer {
            capabilities {
                completionProvider = CompletionOptions(
                    triggerCharacters = listOf(".", ":"),
                    resolveProvider = true,
                )
            }
        }
        val completionProvider = config.capabilities.completionProvider.shouldNotBeNull()
        completionProvider.triggerCharacters shouldBe listOf(".", ":")
        completionProvider.resolveProvider shouldBe true
    }

    @Test
    fun `capabilities builder sets text document sync`() {
        val config = languageServer {
            capabilities {
                textDocumentSync = TextDocumentSyncKind.Full
            }
        }
        val textDocumentSync = config.capabilities.textDocumentSync.shouldNotBeNull()
        textDocumentSync.change shouldBe TextDocumentSyncKind.Full
        textDocumentSync.openClose shouldBe true
    }

    @Test
    fun `capabilities builder sets multiple providers`() {
        val config = languageServer {
            capabilities {
                hoverProvider = true
                definitionProvider = true
                referencesProvider = true
                documentSymbolProvider = true
            }
        }
        config.capabilities.hoverProvider.shouldNotBeNull()
        config.capabilities.definitionProvider.shouldNotBeNull()
        config.capabilities.referencesProvider.shouldNotBeNull()
        config.capabilities.documentSymbolProvider.shouldNotBeNull()
    }

    @Test
    fun `capabilities builder leaves disabled providers as null`() {
        val config = languageServer {
            capabilities {
                hoverProvider = true
            }
        }
        config.capabilities.completionProvider.shouldBeNull()
        config.capabilities.definitionProvider.shouldBeNull()
        config.capabilities.referencesProvider.shouldBeNull()
    }

    // ==================== TextDocument Handler Registration Tests ====================

    @Test
    fun `textDocument didOpen handler registration`() {
        val config = languageServer {
            textDocument {
                didOpen { _ -> }
            }
        }
        config.notificationHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_DID_OPEN
    }

    @Test
    fun `textDocument didClose handler registration`() {
        val config = languageServer {
            textDocument {
                didClose { _ -> }
            }
        }
        config.notificationHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_DID_CLOSE
    }

    @Test
    fun `textDocument didChange handler registration`() {
        val config = languageServer {
            textDocument {
                didChange { _ -> }
            }
        }
        config.notificationHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_DID_CHANGE
    }

    @Test
    fun `textDocument didSave handler registration`() {
        val config = languageServer {
            textDocument {
                didSave { _ -> }
            }
        }
        config.notificationHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_DID_SAVE
    }

    @Test
    fun `textDocument completion handler registration`() {
        val config = languageServer {
            textDocument {
                completion { _ -> null }
            }
        }
        config.requestHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_COMPLETION
    }

    @Test
    fun `textDocument hover handler registration`() {
        val config = languageServer {
            textDocument {
                hover { _ -> null }
            }
        }
        config.requestHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_HOVER
    }

    @Test
    fun `textDocument definition handler registration`() {
        val config = languageServer {
            textDocument {
                definition { _ -> null }
            }
        }
        config.requestHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_DEFINITION
    }

    @Test
    fun `textDocument references handler registration`() {
        val config = languageServer {
            textDocument {
                references { _ -> null }
            }
        }
        config.requestHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_REFERENCES
    }

    @Test
    fun `multiple textDocument handlers can be registered`() {
        val config = languageServer {
            textDocument {
                didOpen { _ -> }
                didClose { _ -> }
                didChange { _ -> }
                completion { _ -> null }
                hover { _ -> null }
                definition { _ -> null }
            }
        }
        config.notificationHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_DID_OPEN
        config.notificationHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_DID_CLOSE
        config.notificationHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_DID_CHANGE
        config.requestHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_COMPLETION
        config.requestHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_HOVER
        config.requestHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_DEFINITION
    }

    // ==================== Workspace Handler Registration Tests ====================

    @Test
    fun `workspace didChangeConfiguration handler registration`() {
        val config = languageServer {
            workspace {
                didChangeConfiguration { _ -> }
            }
        }
        config.notificationHandlers shouldContainKey LspMethods.WORKSPACE_DID_CHANGE_CONFIGURATION
    }

    @Test
    fun `workspace didChangeWatchedFiles handler registration`() {
        val config = languageServer {
            workspace {
                didChangeWatchedFiles { _ -> }
            }
        }
        config.notificationHandlers shouldContainKey LspMethods.WORKSPACE_DID_CHANGE_WATCHED_FILES
    }

    @Test
    fun `workspace symbol handler registration`() {
        val config = languageServer {
            workspace {
                symbol { _ -> null }
            }
        }
        config.requestHandlers shouldContainKey LspMethods.WORKSPACE_SYMBOL
    }

    @Test
    fun `workspace executeCommand handler registration`() {
        val config = languageServer {
            workspace {
                executeCommand { _ -> null }
            }
        }
        config.requestHandlers shouldContainKey LspMethods.WORKSPACE_EXECUTE_COMMAND
    }

    // ==================== Call Hierarchy Handler Registration Tests ====================

    @Test
    fun `callHierarchy prepare handler registration`() {
        val config = languageServer {
            callHierarchy {
                prepare { _ -> null }
            }
        }
        config.requestHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_PREPARE_CALL_HIERARCHY
    }

    @Test
    fun `callHierarchy incomingCalls handler registration`() {
        val config = languageServer {
            callHierarchy {
                incomingCalls { _ -> null }
            }
        }
        config.requestHandlers shouldContainKey LspMethods.CALL_HIERARCHY_INCOMING_CALLS
    }

    @Test
    fun `callHierarchy outgoingCalls handler registration`() {
        val config = languageServer {
            callHierarchy {
                outgoingCalls { _ -> null }
            }
        }
        config.requestHandlers shouldContainKey LspMethods.CALL_HIERARCHY_OUTGOING_CALLS
    }

    // ==================== Type Hierarchy Handler Registration Tests ====================

    @Test
    fun `typeHierarchy prepare handler registration`() {
        val config = languageServer {
            typeHierarchy {
                prepare { _ -> null }
            }
        }
        config.requestHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_PREPARE_TYPE_HIERARCHY
    }

    @Test
    fun `typeHierarchy supertypes handler registration`() {
        val config = languageServer {
            typeHierarchy {
                supertypes { _ -> null }
            }
        }
        config.requestHandlers shouldContainKey LspMethods.TYPE_HIERARCHY_SUPERTYPES
    }

    @Test
    fun `typeHierarchy subtypes handler registration`() {
        val config = languageServer {
            typeHierarchy {
                subtypes { _ -> null }
            }
        }
        config.requestHandlers shouldContainKey LspMethods.TYPE_HIERARCHY_SUBTYPES
    }

    // ==================== Custom Handler Registration Tests ====================

    @Test
    fun `custom request handler registration`() {
        val config = languageServer {
            onRequest("custom/myMethod") { _ -> null }
        }
        config.requestHandlers shouldContainKey "custom/myMethod"
    }

    @Test
    fun `custom notification handler registration`() {
        val config = languageServer {
            onNotification("custom/myNotification") { _ -> }
        }
        config.notificationHandlers shouldContainKey "custom/myNotification"
    }

    @Test
    fun `custom handlers do not conflict with standard handlers`() {
        val config = languageServer {
            textDocument {
                hover { _ -> null }
            }
            onRequest("custom/myMethod") { _ -> null }
            onNotification("custom/myNotification") { _ -> }
        }
        config.requestHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_HOVER
        config.requestHandlers shouldContainKey "custom/myMethod"
        config.notificationHandlers shouldContainKey "custom/myNotification"
    }

    // ==================== Handler Isolation Tests ====================

    @Test
    fun `handlers from different builder blocks are accumulated`() {
        val config = languageServer {
            textDocument {
                didOpen { _ -> }
                completion { _ -> null }
            }
            workspace {
                didChangeConfiguration { _ -> }
                symbol { _ -> null }
            }
        }
        config.notificationHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_DID_OPEN
        config.notificationHandlers shouldContainKey LspMethods.WORKSPACE_DID_CHANGE_CONFIGURATION
        config.requestHandlers shouldContainKey LspMethods.TEXT_DOCUMENT_COMPLETION
        config.requestHandlers shouldContainKey LspMethods.WORKSPACE_SYMBOL
    }

    @Test
    fun `no handlers registered for unregistered methods`() {
        val config = languageServer {
            textDocument {
                hover { _ -> null }
            }
        }
        config.requestHandlers shouldNotContainKey LspMethods.TEXT_DOCUMENT_COMPLETION
        config.requestHandlers shouldNotContainKey LspMethods.TEXT_DOCUMENT_DEFINITION
        config.notificationHandlers shouldNotContainKey LspMethods.TEXT_DOCUMENT_DID_OPEN
    }

    // ==================== Full Configuration Tests ====================

    @Test
    fun `full server configuration builds correctly`() {
        val config = languageServer {
            serverInfo("my-lsp", "2.0.0")
            capabilities {
                hoverProvider = true
                completionProvider = CompletionOptions(
                    triggerCharacters = listOf("."),
                )
                textDocumentSync = TextDocumentSyncKind.Incremental
                definitionProvider = true
                referencesProvider = true
            }
            textDocument {
                didOpen { _ -> }
                didClose { _ -> }
                didChange { _ -> }
                hover { _ -> null }
                completion { _ -> null }
                definition { _ -> null }
                references { _ -> null }
            }
            workspace {
                didChangeConfiguration { _ -> }
            }
        }

        val serverInfo = config.serverInfo.shouldNotBeNull()
        serverInfo.name shouldBe "my-lsp"
        serverInfo.version shouldBe "2.0.0"

        config.capabilities.hoverProvider.shouldNotBeNull()
        config.capabilities.completionProvider.shouldNotBeNull()
        config.capabilities.textDocumentSync.shouldNotBeNull()
        config.capabilities.definitionProvider.shouldNotBeNull()
        config.capabilities.referencesProvider.shouldNotBeNull()

        config.requestHandlers.size shouldBe 4 // hover, completion, definition, references
        config.notificationHandlers.size shouldBe 4 // didOpen, didClose, didChange, didChangeConfig
    }
}
