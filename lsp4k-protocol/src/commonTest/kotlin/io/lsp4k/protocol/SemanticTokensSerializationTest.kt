package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for SemanticTokens-related types including:
 * - SemanticTokensOptions
 * - SemanticTokensLegend
 * - SemanticTokensParams, SemanticTokensDeltaParams, SemanticTokensRangeParams
 * - SemanticTokens, SemanticTokensDelta, SemanticTokensEdit
 */
class SemanticTokensSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== SemanticTokensLegend Tests ====================

    @Test
    fun `SemanticTokensLegend minimal`() {
        val legend =
            SemanticTokensLegend(
                tokenTypes = emptyList(),
                tokenModifiers = emptyList(),
            )
        val encoded = json.encodeToString(legend)
        val decoded = json.decodeFromString<SemanticTokensLegend>(encoded)
        decoded.tokenTypes shouldBe emptyList()
        decoded.tokenModifiers shouldBe emptyList()
    }

    @Test
    fun `SemanticTokensLegend with types and modifiers`() {
        val legend =
            SemanticTokensLegend(
                tokenTypes = listOf("namespace", "type", "class", "enum", "interface", "struct", "typeParameter", "function"),
                tokenModifiers = listOf("declaration", "definition", "readonly", "static", "deprecated", "abstract"),
            )
        val encoded = json.encodeToString(legend)
        val decoded = json.decodeFromString<SemanticTokensLegend>(encoded)
        decoded.tokenTypes.size shouldBe 8
        decoded.tokenModifiers.size shouldBe 6
    }

    @Test
    fun `SemanticTokensLegend round-trip`() {
        val legend =
            SemanticTokensLegend(
                tokenTypes = listOf("variable", "property", "parameter", "function"),
                tokenModifiers = listOf("modification", "documentation"),
            )
        val encoded = json.encodeToString(legend)
        val decoded = json.decodeFromString<SemanticTokensLegend>(encoded)
        decoded shouldBe legend
    }

    // ==================== SemanticTokensOptions Tests ====================

    @Test
    fun `SemanticTokensOptions minimal`() {
        val options =
            SemanticTokensOptions(
                legend =
                    SemanticTokensLegend(
                        tokenTypes = listOf("type"),
                        tokenModifiers = emptyList(),
                    ),
            )
        val encoded = json.encodeToString(options)
        encoded shouldContain "\"legend\""
        encoded shouldContain "\"tokenTypes\""
    }

    @Test
    fun `SemanticTokensOptions with range`() {
        val options =
            SemanticTokensOptions(
                legend =
                    SemanticTokensLegend(
                        tokenTypes = listOf("class"),
                        tokenModifiers = listOf("declaration"),
                    ),
                range = true,
            )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<SemanticTokensOptions>(encoded)
        decoded.range shouldBe true
    }

    @Test
    fun `SemanticTokensOptions with full and delta`() {
        val options =
            SemanticTokensOptions(
                legend =
                    SemanticTokensLegend(
                        tokenTypes = listOf("function", "variable"),
                        tokenModifiers = listOf("readonly"),
                    ),
                range = true,
                full = SemanticTokensFullOptions(delta = true),
            )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<SemanticTokensOptions>(encoded)
        decoded.full?.delta shouldBe true
    }

    @Test
    fun `SemanticTokensOptions round-trip`() {
        val options =
            SemanticTokensOptions(
                legend =
                    SemanticTokensLegend(
                        tokenTypes = listOf("namespace", "type", "class"),
                        tokenModifiers = listOf("definition", "static"),
                    ),
                range = true,
                full = SemanticTokensFullOptions(delta = true),
            )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<SemanticTokensOptions>(encoded)
        decoded shouldBe options
    }

    // ==================== SemanticTokensParams Tests ====================

    @Test
    fun `SemanticTokensParams serialization`() {
        val params =
            SemanticTokensParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///test.kt\""
    }

    @Test
    fun `SemanticTokensParams round-trip`() {
        val params =
            SemanticTokensParams(
                textDocument = TextDocumentIdentifier(uri = "file:///src/main.kt"),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<SemanticTokensParams>(encoded)
        decoded shouldBe params
    }

    // ==================== SemanticTokensDeltaParams Tests ====================

    @Test
    fun `SemanticTokensDeltaParams serialization`() {
        val params =
            SemanticTokensDeltaParams(
                textDocument = TextDocumentIdentifier(uri = "file:///delta.kt"),
                previousResultId = "result-123",
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"previousResultId\":\"result-123\""
    }

    @Test
    fun `SemanticTokensDeltaParams round-trip`() {
        val params =
            SemanticTokensDeltaParams(
                textDocument = TextDocumentIdentifier(uri = "file:///code.kt"),
                previousResultId = "abc-def-ghi",
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<SemanticTokensDeltaParams>(encoded)
        decoded shouldBe params
    }

    // ==================== SemanticTokensRangeParams Tests ====================

    @Test
    fun `SemanticTokensRangeParams serialization`() {
        val params =
            SemanticTokensRangeParams(
                textDocument = TextDocumentIdentifier(uri = "file:///range.kt"),
                range =
                    Range(
                        start = Position(line = 10, character = 0),
                        end = Position(line = 50, character = 0),
                    ),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"range\""
        encoded shouldContain "\"start\""
        encoded shouldContain "\"end\""
    }

    @Test
    fun `SemanticTokensRangeParams round-trip`() {
        val params =
            SemanticTokensRangeParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                range =
                    Range(
                        start = Position(line = 0, character = 0),
                        end = Position(line = 100, character = 50),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<SemanticTokensRangeParams>(encoded)
        decoded shouldBe params
    }

    // ==================== SemanticTokens Tests ====================

    @Test
    fun `SemanticTokens empty data`() {
        val tokens =
            SemanticTokens(
                data = emptyList(),
            )
        val encoded = json.encodeToString(tokens)
        val decoded = json.decodeFromString<SemanticTokens>(encoded)
        decoded.data shouldBe emptyList()
        decoded.resultId shouldBe null
    }

    @Test
    fun `SemanticTokens with data`() {
        // Semantic tokens are encoded as: [deltaLine, deltaStartChar, length, tokenType, tokenModifiers]
        val tokens =
            SemanticTokens(
                data = listOf(0, 0, 3, 1, 0, 0, 4, 4, 2, 0, 1, 0, 5, 3, 1),
            )
        val encoded = json.encodeToString(tokens)
        val decoded = json.decodeFromString<SemanticTokens>(encoded)
        decoded.data.size shouldBe 15
    }

    @Test
    fun `SemanticTokens with result id`() {
        val tokens =
            SemanticTokens(
                resultId = "result-456",
                data = listOf(0, 5, 10, 1, 0),
            )
        val encoded = json.encodeToString(tokens)
        val decoded = json.decodeFromString<SemanticTokens>(encoded)
        decoded.resultId shouldBe "result-456"
    }

    @Test
    fun `SemanticTokens round-trip`() {
        val tokens =
            SemanticTokens(
                resultId = "token-id-abc",
                data = listOf(0, 0, 8, 0, 3, 1, 0, 12, 1, 0),
            )
        val encoded = json.encodeToString(tokens)
        val decoded = json.decodeFromString<SemanticTokens>(encoded)
        decoded shouldBe tokens
    }

    // ==================== SemanticTokensEdit Tests ====================

    @Test
    fun `SemanticTokensEdit delete only`() {
        val edit =
            SemanticTokensEdit(
                start = 10,
                deleteCount = 5,
            )
        val encoded = json.encodeToString(edit)
        val decoded = json.decodeFromString<SemanticTokensEdit>(encoded)
        decoded.start shouldBe 10
        decoded.deleteCount shouldBe 5
        decoded.data shouldBe null
    }

    @Test
    fun `SemanticTokensEdit insert only`() {
        val edit =
            SemanticTokensEdit(
                start = 5,
                deleteCount = 0,
                data = listOf(0, 0, 4, 1, 0),
            )
        val encoded = json.encodeToString(edit)
        val decoded = json.decodeFromString<SemanticTokensEdit>(encoded)
        decoded.deleteCount shouldBe 0
        decoded.data shouldBe listOf(0, 0, 4, 1, 0)
    }

    @Test
    fun `SemanticTokensEdit replace`() {
        val edit =
            SemanticTokensEdit(
                start = 15,
                deleteCount = 5,
                data = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
            )
        val encoded = json.encodeToString(edit)
        val decoded = json.decodeFromString<SemanticTokensEdit>(encoded)
        decoded.start shouldBe 15
        decoded.deleteCount shouldBe 5
        decoded.data?.size shouldBe 10
    }

    @Test
    fun `SemanticTokensEdit round-trip`() {
        val edit =
            SemanticTokensEdit(
                start = 0,
                deleteCount = 10,
                data = listOf(0, 0, 5, 2, 1),
            )
        val encoded = json.encodeToString(edit)
        val decoded = json.decodeFromString<SemanticTokensEdit>(encoded)
        decoded shouldBe edit
    }

    // ==================== SemanticTokensDelta Tests ====================

    @Test
    fun `SemanticTokensDelta empty edits`() {
        val delta =
            SemanticTokensDelta(
                edits = emptyList(),
            )
        val encoded = json.encodeToString(delta)
        val decoded = json.decodeFromString<SemanticTokensDelta>(encoded)
        decoded.edits shouldBe emptyList()
        decoded.resultId shouldBe null
    }

    @Test
    fun `SemanticTokensDelta with edits`() {
        val delta =
            SemanticTokensDelta(
                resultId = "delta-result-1",
                edits =
                    listOf(
                        SemanticTokensEdit(start = 0, deleteCount = 5),
                        SemanticTokensEdit(start = 10, deleteCount = 0, data = listOf(1, 2, 3, 4, 5)),
                    ),
            )
        val encoded = json.encodeToString(delta)
        val decoded = json.decodeFromString<SemanticTokensDelta>(encoded)
        decoded.resultId shouldBe "delta-result-1"
        decoded.edits.size shouldBe 2
    }

    @Test
    fun `SemanticTokensDelta round-trip`() {
        val delta =
            SemanticTokensDelta(
                resultId = "delta-abc",
                edits =
                    listOf(
                        SemanticTokensEdit(start = 5, deleteCount = 10, data = listOf(0, 1, 2, 3, 4)),
                    ),
            )
        val encoded = json.encodeToString(delta)
        val decoded = json.decodeFromString<SemanticTokensDelta>(encoded)
        decoded shouldBe delta
    }
}
