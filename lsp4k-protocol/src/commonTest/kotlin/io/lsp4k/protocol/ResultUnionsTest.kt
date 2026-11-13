package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Golden JSON and decode tests for the result unions, using a Json equivalent to jsonrpc4k
 * `JsonRpcCodec.defaultJson`.
 */
class ResultUnionsTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            isLenient = true
            coerceInputValues = true
        }

    private val range = """{"start":{"line":1,"character":2},"end":{"line":1,"character":5}}"""
    private val location = """{"uri":"file:///a","range":$range}"""
    private val symbolInformation = """{"name":"s","kind":13,"location":$location}"""
    private val documentSymbol = """{"name":"d","kind":13,"range":$range,"selectionRange":$range}"""

    private fun <T> KSerializer<T>.roundTrip(text: String): T {
        val decoded = json.decodeFromString(this, text)
        json.encodeToString(this, decoded) shouldBe text
        return decoded
    }

    private fun <T> KSerializer<T>.rejects(vararg texts: String) {
        texts.forEach { text -> assertFailsWith<SerializationException>(text) { json.decodeFromString(this, text) } }
    }

    // ==================== PrepareRenameResult ====================

    @Test
    fun `prepare rename decodes range placeholder and default behavior`() {
        val serializer = PrepareRenameResult.serializer()
        serializer.roundTrip(range).shouldBeInstanceOf<PrepareRenameResult.OfRange>()
        val placeholder = serializer.roundTrip("""{"range":$range,"placeholder":"name"}""")
        placeholder.shouldBeInstanceOf<PrepareRenameResult.OfPlaceholder>().placeholder.placeholder shouldBe "name"
        val default = serializer.roundTrip("""{"defaultBehavior":true}""")
        default.shouldBeInstanceOf<PrepareRenameResult.OfDefaultBehavior>().defaultBehavior.defaultBehavior shouldBe true
    }

    @Test
    fun `prepare rename rejects malformed and ambiguous shapes`() {
        PrepareRenameResult.serializer().rejects(
            "42",
            "[]",
            "{}",
            """{"range":$range}""",
            """{"defaultBehavior":true,"range":$range,"placeholder":"x"}""",
        )
    }

    // ==================== Definition-shaped results ====================

    @Test
    fun `declaration type definition and implementation share the definition shape`() {
        val link =
            """{"targetUri":"file:///a","targetRange":$range,"targetSelectionRange":$range}"""
        val declaration: KSerializer<DeclarationResult> = DefinitionResult.serializer()
        val typeDefinition: KSerializer<TypeDefinitionResult> = DefinitionResult.serializer()
        val implementation: KSerializer<ImplementationResult> = DefinitionResult.serializer()
        declaration.roundTrip(location).shouldBeInstanceOf<DefinitionResult.Single>()
        typeDefinition.roundTrip("[$location]").shouldBeInstanceOf<DefinitionResult.Locations>()
        implementation.roundTrip("[$link]").shouldBeInstanceOf<DefinitionResult.Links>()
        implementation.rejects("[$location,$link]")
    }

    // ==================== (Command | CodeAction)[] ====================

    @Test
    fun `code action result decodes commands and code actions`() {
        val serializer = ListSerializer(CommandOrCodeAction.serializer())
        val command = """{"title":"Run","command":"run"}"""
        val action = """{"title":"Fix","kind":"quickfix","command":{"title":"Run","command":"run"}}"""
        val bare = """{"title":"Fix"}"""
        val decoded = serializer.roundTrip("[$command,$action,$bare]")
        decoded[0].shouldBeInstanceOf<CommandOrCodeAction.OfCommand>().command.command shouldBe "run"
        decoded[1]
            .shouldBeInstanceOf<CommandOrCodeAction.OfCodeAction>()
            .codeAction.command
            ?.command shouldBe "run"
        decoded[2].shouldBeInstanceOf<CommandOrCodeAction.OfCodeAction>()
        serializer.roundTrip("[]") shouldBe emptyList()
    }

    @Test
    fun `code action result rejects invalid elements`() {
        val serializer = ListSerializer(CommandOrCodeAction.serializer())
        serializer.rejects("""[42]""", """[{"title":"x","command":5}]""", """[{"command":"run"}]""", """["x"]""")
    }

    // ==================== DocumentSymbol[] | SymbolInformation[] ====================

    @Test
    fun `document symbol result preserves hierarchical and flat shapes`() {
        val serializer = DocumentSymbolResult.serializer()
        serializer.roundTrip("[$documentSymbol]").shouldBeInstanceOf<DocumentSymbolResult.Hierarchical>()
        serializer.roundTrip("[$symbolInformation]").shouldBeInstanceOf<DocumentSymbolResult.Flat>()
        serializer.roundTrip("[]") shouldBe DocumentSymbolResult.Hierarchical(emptyList())
    }

    @Test
    fun `document symbol result rejects mixed and malformed arrays`() {
        DocumentSymbolResult.serializer().rejects(
            "[$documentSymbol,$symbolInformation]",
            """[{"name":"x","kind":13}]""",
            documentSymbol,
            "[1]",
        )
    }

    // ==================== SymbolInformation[] | WorkspaceSymbol[] ====================

    @Test
    fun `workspace symbol result preserves both shapes`() {
        val serializer = WorkspaceSymbolResult.serializer()
        val deprecated = """{"name":"s","kind":13,"deprecated":true,"location":$location}"""
        serializer.roundTrip("[$deprecated]").shouldBeInstanceOf<WorkspaceSymbolResult.Information>()
        val uriOnly = """{"name":"s","kind":13,"location":{"uri":"file:///a"}}"""
        serializer.roundTrip("[$uriOnly]").shouldBeInstanceOf<WorkspaceSymbolResult.Symbols>()
        val withData = """{"name":"s","kind":13,"location":$location,"data":1}"""
        serializer.roundTrip("[$withData]").shouldBeInstanceOf<WorkspaceSymbolResult.Symbols>()
        serializer.roundTrip("[$symbolInformation]").shouldBeInstanceOf<WorkspaceSymbolResult.Symbols>()
        serializer.roundTrip("[]") shouldBe WorkspaceSymbolResult.Symbols(emptyList())
    }

    @Test
    fun `workspace symbol result rejects mixed and conflicting arrays`() {
        val deprecated = """{"name":"s","kind":13,"deprecated":true,"location":$location}"""
        val uriOnly = """{"name":"s","kind":13,"location":{"uri":"file:///a"}}"""
        WorkspaceSymbolResult.serializer().rejects(
            "[$deprecated,$uriOnly]",
            """[{"name":"s","kind":13,"deprecated":true,"location":{"uri":"file:///a"}}]""",
            "{}",
            """[{"name":"s","kind":13}]""",
        )
    }

    // ==================== InlineCompletionItem[] | InlineCompletionList ====================

    @Test
    fun `inline completion result preserves array and list shapes`() {
        val serializer = InlineCompletionResult.serializer()
        serializer.roundTrip("""[{"insertText":"a"}]""").shouldBeInstanceOf<InlineCompletionResult.Items>()
        serializer.roundTrip("""{"items":[{"insertText":"a"}]}""").shouldBeInstanceOf<InlineCompletionResult.ListResult>()
        serializer.roundTrip("[]") shouldBe InlineCompletionResult.Items(emptyList())
        serializer.rejects("42", "\"x\"")
    }

    // ==================== SemanticTokens | SemanticTokensDelta ====================

    @Test
    fun `semantic tokens delta result decodes full tokens and deltas`() {
        val serializer = SemanticTokensDeltaResult.serializer()
        serializer.roundTrip("""{"resultId":"1","data":[0,1,2,3,4]}""").shouldBeInstanceOf<SemanticTokensDeltaResult.Tokens>()
        val delta = serializer.roundTrip("""{"resultId":"2","edits":[{"start":1,"deleteCount":2,"data":[5]}]}""")
        delta
            .shouldBeInstanceOf<SemanticTokensDeltaResult.Delta>()
            .delta.edits
            .single()
            .start shouldBe 1
        serializer.rejects("{}", """{"data":[],"edits":[]}""", "[]")
    }

    @Test
    fun `semantic tokens partial results encode their chunk`() {
        json.encodeToString(SemanticTokensPartialResult.serializer(), SemanticTokensPartialResult(listOf(1, 2))) shouldBe
            """{"data":[1,2]}"""
        json.encodeToString(
            SemanticTokensDeltaPartialResult.serializer(),
            SemanticTokensDeltaPartialResult(listOf(SemanticTokensEdit(start = 0, deleteCount = 1))),
        ) shouldBe """{"edits":[{"start":0,"deleteCount":1}]}"""
    }

    // ==================== InitializedParams ====================

    @Test
    fun `initialized params always encode as an empty object`() {
        json.encodeToString(InitializedParams.serializer(), InitializedParams) shouldBe "{}"
        json.decodeFromString(InitializedParams.serializer(), "{}") shouldBe InitializedParams
    }
}
