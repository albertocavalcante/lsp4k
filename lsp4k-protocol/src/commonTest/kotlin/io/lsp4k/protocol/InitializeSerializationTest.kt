package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test

/**
 * Tests for Initialize types serialization: InitializeParams, InitializeResult, ServerCapabilities.
 */
class InitializeSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== TraceValue Tests ====================

    @Test
    fun `TraceValue Off serialization`() {
        val trace = TraceValue.Off
        val encoded = json.encodeToString(trace)
        encoded shouldBe "\"off\""
    }

    @Test
    fun `TraceValue Messages serialization`() {
        val trace = TraceValue.Messages
        val encoded = json.encodeToString(trace)
        encoded shouldBe "\"messages\""
    }

    @Test
    fun `TraceValue Verbose serialization`() {
        val trace = TraceValue.Verbose
        val encoded = json.encodeToString(trace)
        encoded shouldBe "\"verbose\""
    }

    @Test
    fun `TraceValue roundtrip for all values`() {
        TraceValue.entries.forEach { trace ->
            val encoded = json.encodeToString(trace)
            val decoded = json.decodeFromString<TraceValue>(encoded)
            decoded shouldBe trace
        }
    }

    // ==================== TextDocumentSyncKind Tests ====================

    @Test
    fun `TextDocumentSyncKind None serialization`() {
        val kind = TextDocumentSyncKind.None
        val encoded = json.encodeToString(kind)
        encoded shouldBe "0"
    }

    @Test
    fun `TextDocumentSyncKind Full serialization`() {
        val kind = TextDocumentSyncKind.Full
        val encoded = json.encodeToString(kind)
        encoded shouldBe "1"
    }

    @Test
    fun `TextDocumentSyncKind Incremental serialization`() {
        val kind = TextDocumentSyncKind.Incremental
        val encoded = json.encodeToString(kind)
        encoded shouldBe "2"
    }

    @Test
    fun `TextDocumentSyncKind roundtrip for all values`() {
        TextDocumentSyncKind.entries.forEach { kind ->
            val encoded = json.encodeToString(kind)
            val decoded = json.decodeFromString<TextDocumentSyncKind>(encoded)
            decoded shouldBe kind
        }
    }

    // ==================== ClientInfo Tests ====================

    @Test
    fun `ClientInfo with name only`() {
        val info = ClientInfo(name = "vscode")
        val encoded = json.encodeToString(info)
        encoded shouldBe """{"name":"vscode"}"""

        val decoded = json.decodeFromString<ClientInfo>(encoded)
        decoded.name shouldBe "vscode"
        decoded.version shouldBe null
    }

    @Test
    fun `ClientInfo with name and version`() {
        val info = ClientInfo(name = "vscode", version = "1.85.0")
        val encoded = json.encodeToString(info)
        encoded shouldBe """{"name":"vscode","version":"1.85.0"}"""

        val decoded = json.decodeFromString<ClientInfo>(encoded)
        decoded shouldBe info
    }

    // ==================== ServerInfo Tests ====================

    @Test
    fun `ServerInfo with name only`() {
        val info = ServerInfo(name = "kotlin-language-server")
        val encoded = json.encodeToString(info)
        encoded shouldBe """{"name":"kotlin-language-server"}"""

        val decoded = json.decodeFromString<ServerInfo>(encoded)
        decoded.name shouldBe "kotlin-language-server"
        decoded.version shouldBe null
    }

    @Test
    fun `ServerInfo with name and version`() {
        val info = ServerInfo(name = "kotlin-language-server", version = "1.3.0")
        val encoded = json.encodeToString(info)
        val decoded = json.decodeFromString<ServerInfo>(encoded)
        decoded shouldBe info
    }

    // ==================== WorkspaceFolder Tests ====================

    @Test
    fun `WorkspaceFolder serialization`() {
        val folder =
            WorkspaceFolder(
                uri = "file:///Users/dev/project",
                name = "my-project",
            )
        val encoded = json.encodeToString(folder)
        encoded shouldBe """{"uri":"file:///Users/dev/project","name":"my-project"}"""

        val decoded = json.decodeFromString<WorkspaceFolder>(encoded)
        decoded shouldBe folder
    }

    // ==================== ClientCapabilities Tests ====================

    @Test
    fun `ClientCapabilities empty`() {
        val caps = ClientCapabilities()
        val encoded = json.encodeToString(caps)
        encoded shouldBe "{}"
    }

    @Test
    fun `ClientCapabilities with experimental`() {
        val caps =
            ClientCapabilities(
                experimental =
                    buildJsonObject {
                        put("customFeature", JsonPrimitive(true))
                    },
            )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ClientCapabilities>(encoded)
        decoded.experimental shouldBe caps.experimental
    }

    // ==================== InitializeParams Tests ====================

    @Test
    fun `InitializeParams minimal`() {
        val params =
            InitializeParams(
                processId = 12345,
                rootUri = "file:///project",
                capabilities = ClientCapabilities(),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<InitializeParams>(encoded)
        decoded.processId shouldBe 12345
        decoded.rootUri shouldBe "file:///project"
    }

    @Test
    fun `InitializeParams with null processId`() {
        val params =
            InitializeParams(
                processId = null,
                rootUri = "file:///project",
                capabilities = ClientCapabilities(),
            )
        val encoded = json.encodeToString(params)
        // null processId should be encoded
        encoded.contains("\"processId\":null") shouldBe true

        val decoded = json.decodeFromString<InitializeParams>(encoded)
        decoded.processId shouldBe null
    }

    @Test
    fun `InitializeParams with null rootUri`() {
        val params =
            InitializeParams(
                processId = 1234,
                rootUri = null,
                capabilities = ClientCapabilities(),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<InitializeParams>(encoded)
        decoded.rootUri shouldBe null
    }

    @Test
    fun `InitializeParams with clientInfo`() {
        val params =
            InitializeParams(
                processId = 5678,
                clientInfo = ClientInfo(name = "test-client", version = "1.0.0"),
                rootUri = "file:///workspace",
                capabilities = ClientCapabilities(),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<InitializeParams>(encoded)
        decoded.clientInfo?.name shouldBe "test-client"
        decoded.clientInfo?.version shouldBe "1.0.0"
    }

    @Test
    fun `InitializeParams with locale`() {
        val params =
            InitializeParams(
                processId = 1000,
                locale = "en-US",
                rootUri = "file:///project",
                capabilities = ClientCapabilities(),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<InitializeParams>(encoded)
        decoded.locale shouldBe "en-US"
    }

    @Test
    fun `InitializeParams with rootPath (deprecated)`() {
        val params =
            InitializeParams(
                processId = 1000,
                rootPath = "/legacy/path",
                rootUri = "file:///project",
                capabilities = ClientCapabilities(),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<InitializeParams>(encoded)
        decoded.rootPath shouldBe "/legacy/path"
    }

    @Test
    fun `InitializeParams with initializationOptions`() {
        val params =
            InitializeParams(
                processId = 1000,
                rootUri = "file:///project",
                initializationOptions =
                    buildJsonObject {
                        put("serverMode", JsonPrimitive("full"))
                        put("enableDebug", JsonPrimitive(true))
                    },
                capabilities = ClientCapabilities(),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<InitializeParams>(encoded)
        decoded.initializationOptions shouldBe params.initializationOptions
    }

    @Test
    fun `InitializeParams with trace`() {
        val params =
            InitializeParams(
                processId = 1000,
                rootUri = "file:///project",
                capabilities = ClientCapabilities(),
                trace = TraceValue.Verbose,
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<InitializeParams>(encoded)
        decoded.trace shouldBe TraceValue.Verbose
    }

    @Test
    fun `InitializeParams with workspaceFolders`() {
        val params =
            InitializeParams(
                processId = 1000,
                rootUri = "file:///project",
                capabilities = ClientCapabilities(),
                workspaceFolders =
                    listOf(
                        WorkspaceFolder(uri = "file:///project/main", name = "main"),
                        WorkspaceFolder(uri = "file:///project/test", name = "test"),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<InitializeParams>(encoded)
        decoded.workspaceFolders?.size shouldBe 2
        decoded.workspaceFolders?.first()?.name shouldBe "main"
    }

    @Test
    fun `InitializeParams comprehensive`() {
        val params =
            InitializeParams(
                processId = 9999,
                clientInfo = ClientInfo(name = "my-editor", version = "2.5.0"),
                locale = "de-DE",
                rootPath = "/deprecated/path",
                rootUri = "file:///workspace/project",
                initializationOptions = JsonPrimitive("custom-options"),
                capabilities = ClientCapabilities(),
                trace = TraceValue.Messages,
                workspaceFolders =
                    listOf(
                        WorkspaceFolder(uri = "file:///workspace/project", name = "project"),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<InitializeParams>(encoded)
        decoded shouldBe params
    }

    // ==================== ServerCapabilities Tests ====================

    @Test
    fun `ServerCapabilities empty`() {
        val caps = ServerCapabilities()
        val encoded = json.encodeToString(caps)
        encoded shouldBe "{}"
    }

    @Test
    fun `ServerCapabilities with positionEncoding`() {
        val caps = ServerCapabilities(positionEncoding = "utf-16")
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded.positionEncoding shouldBe "utf-16"
    }

    @Test
    fun `ServerCapabilities with hoverProvider`() {
        val caps = ServerCapabilities(hoverProvider = Either.left(true))
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded.hoverProvider shouldBe Either.left(true)
    }

    @Test
    fun `ServerCapabilities with textDocumentSync`() {
        val caps =
            ServerCapabilities(
                textDocumentSync =
                    TextDocumentSyncOptions(
                        openClose = true,
                        change = TextDocumentSyncKind.Incremental,
                        save = SaveOptions(includeText = true),
                    ),
            )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded.textDocumentSync?.openClose shouldBe true
        decoded.textDocumentSync?.change shouldBe TextDocumentSyncKind.Incremental
        decoded.textDocumentSync?.save?.includeText shouldBe true
    }

    @Test
    fun `ServerCapabilities with completionProvider`() {
        val caps =
            ServerCapabilities(
                completionProvider =
                    CompletionOptions(
                        triggerCharacters = listOf(".", ":", "<"),
                        resolveProvider = true,
                    ),
            )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded.completionProvider?.triggerCharacters shouldBe listOf(".", ":", "<")
        decoded.completionProvider?.resolveProvider shouldBe true
    }

    @Test
    fun `ServerCapabilities with signatureHelpProvider`() {
        val caps =
            ServerCapabilities(
                signatureHelpProvider =
                    SignatureHelpOptions(
                        triggerCharacters = listOf("(", ","),
                        retriggerCharacters = listOf(","),
                    ),
            )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded.signatureHelpProvider?.triggerCharacters shouldBe listOf("(", ",")
    }

    @Test
    fun `ServerCapabilities with navigation providers`() {
        val caps =
            ServerCapabilities(
                declarationProvider = Either.left(true),
                definitionProvider = Either.left(true),
                typeDefinitionProvider = Either.left(true),
                implementationProvider = Either.left(true),
                referencesProvider = Either.left(true),
            )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded.declarationProvider shouldBe Either.left(true)
        decoded.definitionProvider shouldBe Either.left(true)
        decoded.typeDefinitionProvider shouldBe Either.left(true)
        decoded.implementationProvider shouldBe Either.left(true)
        decoded.referencesProvider shouldBe Either.left(true)
    }

    @Test
    fun `ServerCapabilities with document providers`() {
        val caps =
            ServerCapabilities(
                documentHighlightProvider = Either.left(true),
                documentSymbolProvider = Either.left(true),
                codeActionProvider = Either.left(true),
                documentFormattingProvider = Either.left(true),
                documentRangeFormattingProvider = Either.left(true),
            )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded.documentHighlightProvider shouldBe Either.left(true)
        decoded.documentSymbolProvider shouldBe Either.left(true)
        decoded.codeActionProvider shouldBe Either.left(true)
    }

    @Test
    fun `ServerCapabilities with codeLensProvider`() {
        val caps =
            ServerCapabilities(
                codeLensProvider = CodeLensOptions(resolveProvider = true),
            )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded.codeLensProvider?.resolveProvider shouldBe true
    }

    @Test
    fun `ServerCapabilities with documentLinkProvider`() {
        val caps =
            ServerCapabilities(
                documentLinkProvider = DocumentLinkOptions(resolveProvider = true),
            )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded.documentLinkProvider?.resolveProvider shouldBe true
    }

    @Test
    fun `ServerCapabilities with documentOnTypeFormattingProvider`() {
        val caps =
            ServerCapabilities(
                documentOnTypeFormattingProvider =
                    DocumentOnTypeFormattingOptions(
                        firstTriggerCharacter = "}",
                        moreTriggerCharacter = listOf(";", "\n"),
                    ),
            )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded.documentOnTypeFormattingProvider?.firstTriggerCharacter shouldBe "}"
        decoded.documentOnTypeFormattingProvider?.moreTriggerCharacter shouldBe listOf(";", "\n")
    }

    @Test
    fun `ServerCapabilities with executeCommandProvider`() {
        val caps =
            ServerCapabilities(
                executeCommandProvider =
                    ExecuteCommandOptions(
                        commands = listOf("editor.action.quickFix", "editor.action.format"),
                    ),
            )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded.executeCommandProvider?.commands shouldBe listOf("editor.action.quickFix", "editor.action.format")
    }

    @Test
    fun `ServerCapabilities with workspace capabilities`() {
        val caps =
            ServerCapabilities(
                workspaceSymbolProvider = Either.left(true),
                workspace =
                    ServerWorkspaceCapabilities(
                        workspaceFolders =
                            WorkspaceFoldersServerCapabilities(
                                supported = true,
                                changeNotifications = true,
                            ),
                    ),
            )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded.workspaceSymbolProvider shouldBe Either.left(true)
        decoded.workspace?.workspaceFolders?.supported shouldBe true
        decoded.workspace?.workspaceFolders?.changeNotifications shouldBe true
    }

    @Test
    fun `ServerCapabilities with experimental`() {
        val caps =
            ServerCapabilities(
                experimental =
                    buildJsonObject {
                        put("customCapability", JsonPrimitive("value"))
                    },
            )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded.experimental shouldBe caps.experimental
    }

    @Test
    fun `ServerCapabilities comprehensive`() {
        val caps =
            ServerCapabilities(
                positionEncoding = "utf-16",
                textDocumentSync =
                    TextDocumentSyncOptions(
                        openClose = true,
                        change = TextDocumentSyncKind.Full,
                    ),
                hoverProvider = Either.left(true),
                completionProvider =
                    CompletionOptions(
                        triggerCharacters = listOf("."),
                        resolveProvider = true,
                    ),
                definitionProvider = Either.left(true),
                referencesProvider = Either.left(true),
                documentHighlightProvider = Either.left(true),
                documentSymbolProvider = Either.left(true),
                codeActionProvider = Either.left(true),
                documentFormattingProvider = Either.left(true),
                renameProvider = Either.left(true),
                foldingRangeProvider = Either.left(true),
                selectionRangeProvider = Either.left(true),
            )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ServerCapabilities>(encoded)
        decoded shouldBe caps
    }

    // ==================== InitializeResult Tests ====================

    @Test
    fun `InitializeResult minimal`() {
        val result =
            InitializeResult(
                capabilities = ServerCapabilities(),
            )
        val encoded = json.encodeToString(result)
        val decoded = json.decodeFromString<InitializeResult>(encoded)
        decoded.serverInfo shouldBe null
    }

    @Test
    fun `InitializeResult with serverInfo`() {
        val result =
            InitializeResult(
                capabilities = ServerCapabilities(hoverProvider = Either.left(true)),
                serverInfo = ServerInfo(name = "kotlin-ls", version = "1.0.0"),
            )
        val encoded = json.encodeToString(result)
        val decoded = json.decodeFromString<InitializeResult>(encoded)
        decoded.serverInfo?.name shouldBe "kotlin-ls"
        decoded.serverInfo?.version shouldBe "1.0.0"
    }

    @Test
    fun `InitializeResult comprehensive`() {
        val result =
            InitializeResult(
                capabilities =
                    ServerCapabilities(
                        textDocumentSync =
                            TextDocumentSyncOptions(
                                openClose = true,
                                change = TextDocumentSyncKind.Incremental,
                            ),
                        hoverProvider = Either.left(true),
                        completionProvider =
                            CompletionOptions(
                                triggerCharacters = listOf(".", ":"),
                                resolveProvider = true,
                            ),
                        definitionProvider = Either.left(true),
                        referencesProvider = Either.left(true),
                        documentFormattingProvider = Either.left(true),
                    ),
                serverInfo = ServerInfo(name = "lsp4k-server", version = "0.1.0"),
            )
        val encoded = json.encodeToString(result)
        val decoded = json.decodeFromString<InitializeResult>(encoded)
        decoded shouldBe result
    }

    // ==================== InitializeError Tests ====================

    @Test
    fun `InitializeError serialization`() {
        val error = InitializeError(retry = true)
        val encoded = json.encodeToString(error)
        encoded shouldBe """{"retry":true}"""

        val decoded = json.decodeFromString<InitializeError>(encoded)
        decoded.retry shouldBe true
    }

    @Test
    fun `InitializeError retry false`() {
        val error = InitializeError(retry = false)
        val encoded = json.encodeToString(error)
        encoded shouldBe """{"retry":false}"""

        val decoded = json.decodeFromString<InitializeError>(encoded)
        decoded.retry shouldBe false
    }

    // ==================== Additional Option Types Tests ====================

    @Test
    fun `SaveOptions serialization`() {
        val options = SaveOptions(includeText = true)
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<SaveOptions>(encoded)
        decoded.includeText shouldBe true
    }

    @Test
    fun `SaveOptions empty`() {
        val options = SaveOptions()
        val encoded = json.encodeToString(options)
        encoded shouldBe "{}"
    }

    @Test
    fun `TextDocumentSyncOptions comprehensive`() {
        val options =
            TextDocumentSyncOptions(
                openClose = true,
                change = TextDocumentSyncKind.Incremental,
                willSave = true,
                willSaveWaitUntil = true,
                save = SaveOptions(includeText = true),
            )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<TextDocumentSyncOptions>(encoded)
        decoded shouldBe options
    }

    @Test
    fun `CompletionOptions empty`() {
        val options = CompletionOptions()
        val encoded = json.encodeToString(options)
        encoded shouldBe "{}"
    }

    @Test
    fun `CompletionOptions with allCommitCharacters`() {
        val options =
            CompletionOptions(
                triggerCharacters = listOf("."),
                allCommitCharacters = listOf(".", ";", "("),
                resolveProvider = true,
            )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<CompletionOptions>(encoded)
        decoded.allCommitCharacters shouldBe listOf(".", ";", "(")
    }

    @Test
    fun `SignatureHelpOptions roundtrip`() {
        val options =
            SignatureHelpOptions(
                triggerCharacters = listOf("(", ","),
                retriggerCharacters = listOf(",", ")"),
            )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<SignatureHelpOptions>(encoded)
        decoded shouldBe options
    }
}
