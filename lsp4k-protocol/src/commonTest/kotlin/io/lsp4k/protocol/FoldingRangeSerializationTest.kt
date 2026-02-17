package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for FoldingRange-related types including:
 * - FoldingRangeKind (string-serialized enum)
 * - FoldingRangeParams
 * - FoldingRange
 */
class FoldingRangeSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== FoldingRangeKind Tests ====================

    @Test
    fun `FoldingRangeKind Comment serializes to comment`() {
        val encoded = json.encodeToString(FoldingRangeKind.Comment)
        encoded shouldBe "\"comment\""
    }

    @Test
    fun `FoldingRangeKind Imports serializes to imports`() {
        val encoded = json.encodeToString(FoldingRangeKind.Imports)
        encoded shouldBe "\"imports\""
    }

    @Test
    fun `FoldingRangeKind Region serializes to region`() {
        val encoded = json.encodeToString(FoldingRangeKind.Region)
        encoded shouldBe "\"region\""
    }

    @Test
    fun `FoldingRangeKind deserializes from strings`() {
        json.decodeFromString<FoldingRangeKind>("\"comment\"") shouldBe FoldingRangeKind.Comment
        json.decodeFromString<FoldingRangeKind>("\"imports\"") shouldBe FoldingRangeKind.Imports
        json.decodeFromString<FoldingRangeKind>("\"region\"") shouldBe FoldingRangeKind.Region
    }

    // ==================== FoldingRangeParams Tests ====================

    @Test
    fun `FoldingRangeParams serialization`() {
        val params = FoldingRangeParams(
            textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
        )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///test.kt\""
    }

    @Test
    fun `FoldingRangeParams round-trip`() {
        val params = FoldingRangeParams(
            textDocument = TextDocumentIdentifier(uri = "file:///src/main.kt"),
        )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<FoldingRangeParams>(encoded)
        decoded shouldBe params
    }

    // ==================== FoldingRange Tests ====================

    @Test
    fun `FoldingRange minimal`() {
        val range = FoldingRange(
            startLine = 10,
            endLine = 20,
        )
        val encoded = json.encodeToString(range)
        val decoded = json.decodeFromString<FoldingRange>(encoded)
        decoded.startLine shouldBe 10
        decoded.endLine shouldBe 20
        decoded.startCharacter shouldBe null
        decoded.endCharacter shouldBe null
        decoded.kind shouldBe null
        decoded.collapsedText shouldBe null
    }

    @Test
    fun `FoldingRange with character offsets`() {
        val range = FoldingRange(
            startLine = 5,
            startCharacter = 10,
            endLine = 15,
            endCharacter = 5,
        )
        val encoded = json.encodeToString(range)
        val decoded = json.decodeFromString<FoldingRange>(encoded)
        decoded.startLine shouldBe 5
        decoded.startCharacter shouldBe 10
        decoded.endLine shouldBe 15
        decoded.endCharacter shouldBe 5
    }

    @Test
    fun `FoldingRange with Comment kind`() {
        val range = FoldingRange(
            startLine = 1,
            endLine = 10,
            kind = FoldingRangeKind.Comment,
        )
        val encoded = json.encodeToString(range)
        encoded shouldContain "\"kind\":\"comment\""
        val decoded = json.decodeFromString<FoldingRange>(encoded)
        decoded.kind shouldBe FoldingRangeKind.Comment
    }

    @Test
    fun `FoldingRange with Imports kind`() {
        val range = FoldingRange(
            startLine = 0,
            endLine = 15,
            kind = FoldingRangeKind.Imports,
        )
        val encoded = json.encodeToString(range)
        encoded shouldContain "\"kind\":\"imports\""
        val decoded = json.decodeFromString<FoldingRange>(encoded)
        decoded.kind shouldBe FoldingRangeKind.Imports
    }

    @Test
    fun `FoldingRange with Region kind`() {
        val range = FoldingRange(
            startLine = 20,
            endLine = 50,
            kind = FoldingRangeKind.Region,
        )
        val encoded = json.encodeToString(range)
        encoded shouldContain "\"kind\":\"region\""
        val decoded = json.decodeFromString<FoldingRange>(encoded)
        decoded.kind shouldBe FoldingRangeKind.Region
    }

    @Test
    fun `FoldingRange with collapsed text`() {
        val range = FoldingRange(
            startLine = 100,
            endLine = 200,
            collapsedText = "...",
        )
        val encoded = json.encodeToString(range)
        encoded shouldContain "\"collapsedText\":\"...\""
        val decoded = json.decodeFromString<FoldingRange>(encoded)
        decoded.collapsedText shouldBe "..."
    }

    @Test
    fun `FoldingRange full`() {
        val range = FoldingRange(
            startLine = 50,
            startCharacter = 0,
            endLine = 100,
            endCharacter = 1,
            kind = FoldingRangeKind.Region,
            collapsedText = "// region: Tests",
        )
        val encoded = json.encodeToString(range)
        val decoded = json.decodeFromString<FoldingRange>(encoded)
        decoded shouldBe range
    }

    @Test
    fun `FoldingRange with zero-based lines`() {
        val range = FoldingRange(
            startLine = 0,
            endLine = 0,
        )
        val encoded = json.encodeToString(range)
        val decoded = json.decodeFromString<FoldingRange>(encoded)
        decoded.startLine shouldBe 0
        decoded.endLine shouldBe 0
    }

    @Test
    fun `FoldingRange list serialization`() {
        val ranges = listOf(
            FoldingRange(startLine = 0, endLine = 5, kind = FoldingRangeKind.Imports),
            FoldingRange(startLine = 7, endLine = 50, kind = FoldingRangeKind.Region),
            FoldingRange(startLine = 10, endLine = 15, kind = FoldingRangeKind.Comment),
        )
        val encoded = json.encodeToString(ranges)
        val decoded = json.decodeFromString<List<FoldingRange>>(encoded)
        decoded.size shouldBe 3
        decoded[0].kind shouldBe FoldingRangeKind.Imports
        decoded[1].kind shouldBe FoldingRangeKind.Region
        decoded[2].kind shouldBe FoldingRangeKind.Comment
    }

    @Test
    fun `FoldingRange empty list`() {
        val ranges = emptyList<FoldingRange>()
        val encoded = json.encodeToString(ranges)
        encoded shouldBe "[]"
        val decoded = json.decodeFromString<List<FoldingRange>>(encoded)
        decoded shouldBe emptyList()
    }

    @Test
    fun `FoldingRange with large line numbers`() {
        val range = FoldingRange(
            startLine = 10000,
            endLine = 20000,
        )
        val encoded = json.encodeToString(range)
        val decoded = json.decodeFromString<FoldingRange>(encoded)
        decoded.startLine shouldBe 10000
        decoded.endLine shouldBe 20000
    }
}
