package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Registration options, the `boolean | Options | RegistrationOptions` capability unions and the strict
 * decoders. The `Json` mirrors jsonrpc4k `JsonRpcCodec.defaultJson`.
 */
class RegistrationOptionsSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            isLenient = true
            coerceInputValues = true
        }

    private val kotlinSelector = listOf(TextDocumentFilter(language = "kotlin"))

    private inline fun <reified T> rejects(text: String) {
        assertFailsWith<SerializationException> { json.decodeFromString<T>(text) }
    }

    // ==================== registration options structures ====================

    @Test
    fun `null document selector is always encoded`() {
        json.encodeToString(HoverRegistrationOptions(documentSelector = null)) shouldBe """{"documentSelector":null}"""
        json.encodeToString(TextDocumentRegistrationOptions(documentSelector = null)) shouldBe """{"documentSelector":null}"""
    }

    @Test
    fun `hover registration options golden`() {
        val options = HoverRegistrationOptions(documentSelector = kotlinSelector, workDoneProgress = true)
        val text = """{"documentSelector":[{"language":"kotlin"}],"workDoneProgress":true}"""
        json.encodeToString(options) shouldBe text
        json.decodeFromString<HoverRegistrationOptions>(text) shouldBe options
    }

    @Test
    fun `static and id carrying registration options golden`() {
        json.encodeToString(StaticRegistrationOptions(id = "s1")) shouldBe """{"id":"s1"}"""
        json.encodeToString(StaticRegistrationOptions()) shouldBe "{}"
        json.encodeToString(FoldingRangeRegistrationOptions(documentSelector = null, id = "f1")) shouldBe
            """{"documentSelector":null,"id":"f1"}"""
        json.encodeToString(MonikerRegistrationOptions(documentSelector = null)) shouldBe """{"documentSelector":null}"""
    }

    @Test
    fun `completion registration options flatten the options fields`() {
        val options =
            CompletionRegistrationOptions(
                documentSelector = kotlinSelector,
                triggerCharacters = listOf("."),
                allCommitCharacters = listOf(";"),
                resolveProvider = true,
                completionItem = ServerCompletionItemOptions(labelDetailsSupport = true),
            )
        val text =
            """{"documentSelector":[{"language":"kotlin"}],"triggerCharacters":["."],"allCommitCharacters":[";"],""" +
                """"resolveProvider":true,"completionItem":{"labelDetailsSupport":true}}"""
        json.encodeToString(options) shouldBe text
        json.decodeFromString<CompletionRegistrationOptions>(text) shouldBe options
    }

    @Test
    fun `semantic tokens registration options golden`() {
        val options =
            SemanticTokensRegistrationOptions(
                documentSelector = null,
                legend = SemanticTokensLegend(tokenTypes = listOf("class"), tokenModifiers = emptyList()),
                range = Either.left(true),
                full = Either.right(SemanticTokensFullOptions(delta = true)),
                id = "st",
            )
        val text =
            """{"documentSelector":null,"legend":{"tokenTypes":["class"],"tokenModifiers":[]},""" +
                """"range":true,"full":{"delta":true},"id":"st"}"""
        json.encodeToString(options) shouldBe text
        json.decodeFromString<SemanticTokensRegistrationOptions>(text) shouldBe options
    }

    @Test
    fun `text document change and save registration options golden`() {
        val change = TextDocumentChangeRegistrationOptions(documentSelector = null, syncKind = TextDocumentSyncKind.Incremental)
        json.encodeToString(change) shouldBe """{"documentSelector":null,"syncKind":2}"""
        json.decodeFromString<TextDocumentChangeRegistrationOptions>("""{"documentSelector":null,"syncKind":2}""") shouldBe change
        val save = TextDocumentSaveRegistrationOptions(documentSelector = kotlinSelector, includeText = true)
        json.encodeToString(save) shouldBe """{"documentSelector":[{"language":"kotlin"}],"includeText":true}"""
    }

    @Test
    fun `did change configuration section accepts a string or an array`() {
        val single = DidChangeConfigurationRegistrationOptions(section = Either.left("kotlin"))
        json.encodeToString(single) shouldBe """{"section":"kotlin"}"""
        json.decodeFromString<DidChangeConfigurationRegistrationOptions>("""{"section":"kotlin"}""") shouldBe single
        val many = DidChangeConfigurationRegistrationOptions(section = Either.right(listOf("a", "b")))
        json.encodeToString(many) shouldBe """{"section":["a","b"]}"""
        json.decodeFromString<DidChangeConfigurationRegistrationOptions>("""{"section":["a","b"]}""") shouldBe many
        rejects<DidChangeConfigurationRegistrationOptions>("""{"section":1}""")
        rejects<DidChangeConfigurationRegistrationOptions>("""{"section":{}}""")
    }

    @Test
    fun `remaining registration options golden`() {
        json.encodeToString(DocumentSymbolRegistrationOptions(documentSelector = null, label = "l")) shouldBe
            """{"documentSelector":null,"label":"l"}"""
        json.encodeToString(
            CodeActionRegistrationOptions(documentSelector = null, codeActionKinds = listOf("quickfix"), resolveProvider = true),
        ) shouldBe """{"documentSelector":null,"codeActionKinds":["quickfix"],"resolveProvider":true}"""
        json.encodeToString(
            DocumentOnTypeFormattingRegistrationOptions(
                documentSelector = null,
                firstTriggerCharacter = "}",
                moreTriggerCharacter = listOf(";"),
            ),
        ) shouldBe """{"documentSelector":null,"firstTriggerCharacter":"}","moreTriggerCharacter":[";"]}"""
        json.encodeToString(RenameRegistrationOptions(documentSelector = null, prepareProvider = true)) shouldBe
            """{"documentSelector":null,"prepareProvider":true}"""
        json.encodeToString(ExecuteCommandRegistrationOptions(commands = listOf("a.b"), workDoneProgress = true)) shouldBe
            """{"commands":["a.b"],"workDoneProgress":true}"""
        json.encodeToString(WorkspaceSymbolRegistrationOptions(workDoneProgress = true, resolveProvider = true)) shouldBe
            """{"workDoneProgress":true,"resolveProvider":true}"""
        json.encodeToString(DocumentRangeFormattingRegistrationOptions(documentSelector = null, rangesSupport = true)) shouldBe
            """{"documentSelector":null,"rangesSupport":true}"""
        json.encodeToString(
            SignatureHelpRegistrationOptions(documentSelector = null, triggerCharacters = listOf("("), retriggerCharacters = listOf(",")),
        ) shouldBe """{"documentSelector":null,"triggerCharacters":["("],"retriggerCharacters":[","]}"""
        json.encodeToString(InlayHintRegistrationOptions(documentSelector = null, resolveProvider = true, id = "ih")) shouldBe
            """{"documentSelector":null,"resolveProvider":true,"id":"ih"}"""
        json.encodeToString(NotebookDocumentSyncRegistrationOptions(notebookSelector = emptyList(), save = true, id = "n")) shouldBe
            """{"notebookSelector":[],"save":true,"id":"n"}"""
    }

    // ==================== ServerCapabilities three-way unions ====================

    @Test
    fun `provider forms encode as boolean options or registration`() {
        val caps =
            ServerCapabilities(
                foldingRangeProvider = Either3.first(true),
                selectionRangeProvider = Either3.second(SelectionRangeOptions(workDoneProgress = true)),
                callHierarchyProvider = Either3.third(CallHierarchyRegistrationOptions(documentSelector = null, id = "ch")),
            )
        val text =
            """{"foldingRangeProvider":true,"selectionRangeProvider":{"workDoneProgress":true},""" +
                """"callHierarchyProvider":{"documentSelector":null,"id":"ch"}}"""
        json.encodeToString(caps) shouldBe text
        json.decodeFromString<ServerCapabilities>(text) shouldBe caps
    }

    @Test
    fun `every three way provider keeps its registration form`() {
        val names =
            listOf(
                "declaration",
                "typeDefinition",
                "implementation",
                "color",
                "foldingRange",
                "selectionRange",
                "linkedEditingRange",
                "callHierarchy",
                "moniker",
                "typeHierarchy",
                "inlineValue",
                "inlayHint",
            )
        val form = """{"documentSelector":[{"language":"kotlin"}],"id":"x"}"""
        val text = names.joinToString(prefix = "{", postfix = "}", separator = ",") { """"${it}Provider":$form""" }
        val decoded = json.decodeFromString<ServerCapabilities>(text)
        decoded.declarationProvider shouldBe Either3.third(DeclarationRegistrationOptions(kotlinSelector, id = "x"))
        decoded.typeDefinitionProvider shouldBe Either3.third(TypeDefinitionRegistrationOptions(kotlinSelector, id = "x"))
        decoded.implementationProvider shouldBe Either3.third(ImplementationRegistrationOptions(kotlinSelector, id = "x"))
        decoded.colorProvider shouldBe Either3.third(DocumentColorRegistrationOptions(kotlinSelector, id = "x"))
        decoded.foldingRangeProvider shouldBe Either3.third(FoldingRangeRegistrationOptions(kotlinSelector, id = "x"))
        decoded.selectionRangeProvider shouldBe Either3.third(SelectionRangeRegistrationOptions(kotlinSelector, id = "x"))
        decoded.linkedEditingRangeProvider shouldBe Either3.third(LinkedEditingRangeRegistrationOptions(kotlinSelector, id = "x"))
        decoded.callHierarchyProvider shouldBe Either3.third(CallHierarchyRegistrationOptions(kotlinSelector, id = "x"))
        decoded.monikerProvider shouldBe Either3.third(MonikerRegistrationOptions(kotlinSelector))
        decoded.typeHierarchyProvider shouldBe Either3.third(TypeHierarchyRegistrationOptions(kotlinSelector, id = "x"))
        decoded.inlineValueProvider shouldBe Either3.third(InlineValueRegistrationOptions(kotlinSelector, id = "x"))
        decoded.inlayHintProvider shouldBe Either3.third(InlayHintRegistrationOptions(kotlinSelector, id = "x"))
        Json.parseToJsonElement(json.encodeToString(decoded)) shouldBe Json.parseToJsonElement(text).withoutMonikerId()
    }

    private fun JsonElement.withoutMonikerId(): JsonElement =
        JsonObject(
            (this as JsonObject).mapValues { (key, value) ->
                if (key == "monikerProvider") JsonObject((value as JsonObject) - "id") else value
            },
        )

    @Test
    fun `provider unions reject wrong json kinds`() {
        rejects<ServerCapabilities>("""{"foldingRangeProvider":"yes"}""")
        rejects<ServerCapabilities>("""{"foldingRangeProvider":1}""")
        rejects<ServerCapabilities>("""{"foldingRangeProvider":[]}""")
        rejects<ServerCapabilities>("""{"hoverProvider":"yes"}""")
        rejects<ServerCapabilities>("""{"hoverProvider":2}""")
        rejects<ServerCapabilities>("""{"semanticTokensProvider":true}""")
        rejects<ServerCapabilities>("""{"diagnosticProvider":false}""")
        rejects<ServerCapabilities>("""{"notebookDocumentSync":[]}""")
    }

    @Test
    fun `provider objects decode by the document selector key`() {
        val options = json.decodeFromString<ServerCapabilities>("""{"monikerProvider":{"workDoneProgress":true}}""")
        options.monikerProvider shouldBe Either3.second(MonikerOptions(workDoneProgress = true))
        val registration = json.decodeFromString<ServerCapabilities>("""{"monikerProvider":{"documentSelector":null}}""")
        registration.monikerProvider shouldBe Either3.third(MonikerRegistrationOptions(documentSelector = null))
    }

    @Test
    fun `semantic tokens diagnostic and notebook provider forms`() {
        val legend = SemanticTokensLegend(tokenTypes = listOf("class"), tokenModifiers = emptyList())
        val caps =
            ServerCapabilities(
                notebookDocumentSync =
                    Either.right(
                        NotebookDocumentSyncRegistrationOptions(
                            notebookSelector = listOf(NotebookSelector(notebook = Either.left("jupyter"))),
                            id = "nb",
                        ),
                    ),
                semanticTokensProvider =
                    Either.right(SemanticTokensRegistrationOptions(documentSelector = null, legend = legend)),
                diagnosticProvider =
                    Either.right(
                        DiagnosticRegistrationOptions(
                            documentSelector = null,
                            interFileDependencies = false,
                            workspaceDiagnostics = false,
                        ),
                    ),
            )
        val decoded = json.decodeFromString<ServerCapabilities>(json.encodeToString(caps))
        decoded shouldBe caps
        val options =
            ServerCapabilities(
                semanticTokensProvider = Either.left(SemanticTokensOptions(legend = legend)),
                diagnosticProvider =
                    Either.left(DiagnosticOptions(interFileDependencies = true, workspaceDiagnostics = false)),
            )
        json.decodeFromString<ServerCapabilities>(json.encodeToString(options)) shouldBe options
    }

    @Test
    fun `notebook sync registration without id decodes as options`() {
        val selector = listOf(NotebookSelector(notebook = Either.left("jupyter")))
        val caps =
            ServerCapabilities(
                notebookDocumentSync = Either.right(NotebookDocumentSyncRegistrationOptions(notebookSelector = selector)),
            )
        val decoded = json.decodeFromString<ServerCapabilities>(json.encodeToString(caps))
        decoded.notebookDocumentSync shouldBe Either.left(NotebookDocumentSyncOptions(notebookSelector = selector))
    }

    // ==================== method constants ====================

    @Test
    fun `registration only method constants`() {
        LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS shouldBe "textDocument/semanticTokens"
        LspMethods.NOTEBOOK_DOCUMENT_SYNC shouldBe "notebookDocument/sync"
    }

    // ==================== strict discriminators ====================

    @Test
    fun `progress token accepts integers and strings only`() {
        json.decodeFromString<WorkDoneProgressCancelParams>("""{"token":7}""").token shouldBe Either.left(7)
        json.decodeFromString<WorkDoneProgressCancelParams>("""{"token":"t"}""").token shouldBe Either.right("t")
        rejects<WorkDoneProgressCancelParams>("""{"token":1.5}""")
        rejects<WorkDoneProgressCancelParams>("""{"token":true}""")
        rejects<WorkDoneProgressCancelParams>("""{"token":{}}""")
    }

    @Test
    fun `inline completion insert text accepts a string or an object only`() {
        json.decodeFromString<InlineCompletionItem>("""{"insertText":"x"}""").insertText shouldBe Either.left("x")
        rejects<InlineCompletionItem>("""{"insertText":5}""")
        rejects<InlineCompletionItem>("""{"insertText":true}""")
        rejects<InlineCompletionItem>("""{"insertText":[]}""")
    }

    @Test
    fun `cancel request id accepts integers and strings only`() {
        json.decodeFromString<CancelParams>("""{"id":7}""").id shouldBe JsonPrimitive(7)
        json.decodeFromString<CancelParams>("""{"id":"a"}""").id shouldBe JsonPrimitive("a")
        json.encodeToString(CancelParams(id = JsonPrimitive(7))) shouldBe """{"id":7}"""
        rejects<CancelParams>("""{"id":true}""")
        rejects<CancelParams>("""{"id":1.5}""")
        rejects<CancelParams>("""{"id":{}}""")
        assertFailsWith<SerializationException> { json.encodeToString(CancelParams(id = JsonPrimitive(true))) }
    }

    @Test
    fun `string or markup content rejects other kinds`() {
        rejects<SignatureInformation>("""{"label":"f","documentation":5}""")
        rejects<SignatureInformation>("""{"label":"f","documentation":[]}""")
        json.decodeFromString<SignatureInformation>("""{"label":"f","documentation":"d"}""").documentation shouldBe Either.left("d")
        json.decodeFromString<SignatureInformation>("""{"label":"f","documentation":null}""").documentation shouldBe null
    }

    // ==================== tri-state optional nullable properties ====================

    @Test
    fun `signature help active parameter tells absent null and value apart`() {
        json.encodeToString(SignatureHelp(signatures = emptyList())) shouldBe """{"signatures":[]}"""
        json.encodeToString(SignatureHelp(signatures = emptyList(), activeParameter = Maybe.Null)) shouldBe
            """{"signatures":[],"activeParameter":null}"""
        json.encodeToString(SignatureHelp(signatures = emptyList(), activeParameter = Maybe.Of(2))) shouldBe
            """{"signatures":[],"activeParameter":2}"""
        json.decodeFromString<SignatureHelp>("""{"signatures":[]}""").activeParameter shouldBe Maybe.Absent
        json.decodeFromString<SignatureHelp>("""{"signatures":[],"activeParameter":null}""").activeParameter shouldBe Maybe.Null
        json.decodeFromString<SignatureHelp>("""{"signatures":[],"activeParameter":2}""").activeParameter shouldBe Maybe.Of(2)
    }

    @Test
    fun `absent is not encoded even when defaults are encoded`() {
        val withDefaults = Json { encodeDefaults = true }
        withDefaults.encodeToString(SignatureInformation(label = "f")).contains("activeParameter") shouldBe false
    }

    @Test
    fun `signature information active parameter null is kept`() {
        val info = json.decodeFromString<SignatureInformation>("""{"label":"f","activeParameter":null}""")
        info.activeParameter shouldBe Maybe.Null
        json.encodeToString(info) shouldBe """{"label":"f","activeParameter":null}"""
    }

    @Test
    fun `initialize params root path and workspace folders keep null`() {
        val text = """{"processId":null,"rootPath":null,"rootUri":null,"capabilities":{},"workspaceFolders":null}"""
        val params = json.decodeFromString<InitializeParams>(text)
        params.rootPath shouldBe Maybe.Null
        params.workspaceFolders shouldBe Maybe.Null
        json.encodeToString(params) shouldBe text
        val absent = json.decodeFromString<InitializeParams>("""{"processId":null,"rootUri":null,"capabilities":{}}""")
        absent.rootPath shouldBe Maybe.Absent
        absent.workspaceFolders shouldBe Maybe.Absent
        json.encodeToString(absent) shouldBe """{"processId":null,"rootUri":null,"capabilities":{}}"""
    }
}
