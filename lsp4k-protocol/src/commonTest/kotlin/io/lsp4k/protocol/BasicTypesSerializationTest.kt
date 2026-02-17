package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for basic LSP type serialization: Position, Range, Location, TextDocumentIdentifier.
 */
class BasicTypesSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== Position Tests ====================

    @Test
    fun `Position serialization produces correct JSON`() {
        val pos = Position(line = 10, character = 5)
        val encoded = json.encodeToString(pos)
        encoded shouldBe """{"line":10,"character":5}"""
    }

    @Test
    fun `Position deserialization from JSON`() {
        val jsonStr = """{"line":10,"character":5}"""
        val decoded = json.decodeFromString<Position>(jsonStr)
        decoded shouldBe Position(line = 10, character = 5)
    }

    @Test
    fun `Position roundtrip serialization`() {
        val pos = Position(line = 42, character = 17)
        val encoded = json.encodeToString(pos)
        val decoded = json.decodeFromString<Position>(encoded)
        decoded shouldBe pos
    }

    @Test
    fun `Position zero values`() {
        val pos = Position(line = 0, character = 0)
        val encoded = json.encodeToString(pos)
        encoded shouldBe """{"line":0,"character":0}"""

        val decoded = json.decodeFromString<Position>(encoded)
        decoded shouldBe Position.ZERO
    }

    @Test
    fun `Position large values`() {
        val pos = Position(line = 999999, character = 500)
        val encoded = json.encodeToString(pos)
        val decoded = json.decodeFromString<Position>(encoded)
        decoded shouldBe pos
    }

    // ==================== Range Tests ====================

    @Test
    fun `Range serialization produces correct JSON`() {
        val range =
            Range(
                start = Position(0, 0),
                end = Position(10, 20),
            )
        val encoded = json.encodeToString(range)
        encoded shouldBe """{"start":{"line":0,"character":0},"end":{"line":10,"character":20}}"""
    }

    @Test
    fun `Range deserialization from JSON`() {
        val jsonStr = """{"start":{"line":1,"character":5},"end":{"line":2,"character":10}}"""
        val decoded = json.decodeFromString<Range>(jsonStr)
        decoded shouldBe
            Range(
                start = Position(1, 5),
                end = Position(2, 10),
            )
    }

    @Test
    fun `Range roundtrip serialization`() {
        val range =
            Range(
                start = Position(5, 10),
                end = Position(15, 25),
            )
        val encoded = json.encodeToString(range)
        val decoded = json.decodeFromString<Range>(encoded)
        decoded shouldBe range
    }

    @Test
    fun `Range empty (start equals end)`() {
        val range =
            Range(
                start = Position(5, 10),
                end = Position(5, 10),
            )
        val encoded = json.encodeToString(range)
        val decoded = json.decodeFromString<Range>(encoded)
        decoded shouldBe range
        decoded.isEmpty() shouldBe true
    }

    @Test
    fun `Range ZERO constant`() {
        val encoded = json.encodeToString(Range.ZERO)
        val decoded = json.decodeFromString<Range>(encoded)
        decoded shouldBe Range.ZERO
        decoded.start shouldBe Position.ZERO
        decoded.end shouldBe Position.ZERO
    }

    // ==================== Location Tests ====================

    @Test
    fun `Location serialization produces correct JSON`() {
        val location =
            Location(
                uri = "file:///path/to/file.kt",
                range = Range(Position(1, 0), Position(1, 10)),
            )
        val encoded = json.encodeToString(location)
        encoded shouldBe """{"uri":"file:///path/to/file.kt","range":{"start":{"line":1,"character":0},"end":{"line":1,"character":10}}}"""
    }

    @Test
    fun `Location deserialization from JSON`() {
        val jsonStr = """{"uri":"file:///test.kt","range":{"start":{"line":0,"character":0},"end":{"line":5,"character":15}}}"""
        val decoded = json.decodeFromString<Location>(jsonStr)
        decoded shouldBe
            Location(
                uri = "file:///test.kt",
                range = Range(Position(0, 0), Position(5, 15)),
            )
    }

    @Test
    fun `Location roundtrip serialization`() {
        val location =
            Location(
                uri = "file:///Users/test/project/src/Main.kt",
                range = Range(Position(100, 4), Position(150, 0)),
            )
        val encoded = json.encodeToString(location)
        val decoded = json.decodeFromString<Location>(encoded)
        decoded shouldBe location
    }

    @Test
    fun `Location with special characters in URI`() {
        val location =
            Location(
                uri = "file:///path/with%20spaces/file.kt",
                range = Range.ZERO,
            )
        val encoded = json.encodeToString(location)
        val decoded = json.decodeFromString<Location>(encoded)
        decoded shouldBe location
    }

    // ==================== LocationLink Tests ====================

    @Test
    fun `LocationLink serialization with all fields`() {
        val link =
            LocationLink(
                originSelectionRange = Range(Position(1, 0), Position(1, 10)),
                targetUri = "file:///target.kt",
                targetRange = Range(Position(5, 0), Position(10, 0)),
                targetSelectionRange = Range(Position(5, 4), Position(5, 20)),
            )
        val encoded = json.encodeToString(link)
        val decoded = json.decodeFromString<LocationLink>(encoded)
        decoded shouldBe link
    }

    @Test
    fun `LocationLink serialization without optional originSelectionRange`() {
        val link =
            LocationLink(
                targetUri = "file:///target.kt",
                targetRange = Range(Position(5, 0), Position(10, 0)),
                targetSelectionRange = Range(Position(5, 4), Position(5, 20)),
            )
        val encoded = json.encodeToString(link)
        // originSelectionRange should be absent from JSON when null and encodeDefaults = false
        encoded.contains("originSelectionRange") shouldBe false

        val decoded = json.decodeFromString<LocationLink>(encoded)
        decoded shouldBe link
        decoded.originSelectionRange shouldBe null
    }

    // ==================== TextDocumentIdentifier Tests ====================

    @Test
    fun `TextDocumentIdentifier serialization produces correct JSON`() {
        val doc = TextDocumentIdentifier(uri = "file:///test.kt")
        val encoded = json.encodeToString(doc)
        encoded shouldBe """{"uri":"file:///test.kt"}"""
    }

    @Test
    fun `TextDocumentIdentifier deserialization from JSON`() {
        val jsonStr = """{"uri":"file:///path/to/document.kt"}"""
        val decoded = json.decodeFromString<TextDocumentIdentifier>(jsonStr)
        decoded shouldBe TextDocumentIdentifier(uri = "file:///path/to/document.kt")
    }

    @Test
    fun `TextDocumentIdentifier roundtrip serialization`() {
        val doc = TextDocumentIdentifier(uri = "file:///complex/path/with/many/segments/File.kt")
        val encoded = json.encodeToString(doc)
        val decoded = json.decodeFromString<TextDocumentIdentifier>(encoded)
        decoded shouldBe doc
    }

    // ==================== VersionedTextDocumentIdentifier Tests ====================

    @Test
    fun `VersionedTextDocumentIdentifier serialization`() {
        val doc =
            VersionedTextDocumentIdentifier(
                uri = "file:///test.kt",
                version = 5,
            )
        val encoded = json.encodeToString(doc)
        encoded shouldBe """{"uri":"file:///test.kt","version":5}"""
    }

    @Test
    fun `VersionedTextDocumentIdentifier roundtrip`() {
        val doc =
            VersionedTextDocumentIdentifier(
                uri = "file:///document.kt",
                version = 42,
            )
        val encoded = json.encodeToString(doc)
        val decoded = json.decodeFromString<VersionedTextDocumentIdentifier>(encoded)
        decoded shouldBe doc
    }

    // ==================== OptionalVersionedTextDocumentIdentifier Tests ====================

    @Test
    fun `OptionalVersionedTextDocumentIdentifier with version`() {
        val doc =
            OptionalVersionedTextDocumentIdentifier(
                uri = "file:///test.kt",
                version = 10,
            )
        val encoded = json.encodeToString(doc)
        val decoded = json.decodeFromString<OptionalVersionedTextDocumentIdentifier>(encoded)
        decoded shouldBe doc
        decoded.version shouldBe 10
    }

    @Test
    fun `OptionalVersionedTextDocumentIdentifier without version`() {
        val doc =
            OptionalVersionedTextDocumentIdentifier(
                uri = "file:///test.kt",
            )
        val encoded = json.encodeToString(doc)
        // version should be absent from JSON
        encoded.contains("version") shouldBe false

        val decoded = json.decodeFromString<OptionalVersionedTextDocumentIdentifier>(encoded)
        decoded shouldBe doc
        decoded.version shouldBe null
    }

    // ==================== TextDocumentItem Tests ====================

    @Test
    fun `TextDocumentItem serialization`() {
        val item =
            TextDocumentItem(
                uri = "file:///test.kt",
                languageId = "kotlin",
                version = 1,
                text = "fun main() {}",
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<TextDocumentItem>(encoded)
        decoded shouldBe item
    }

    @Test
    fun `TextDocumentItem with multiline text`() {
        val item =
            TextDocumentItem(
                uri = "file:///test.kt",
                languageId = "kotlin",
                version = 1,
                text = "package test\n\nfun main() {\n    println(\"Hello\")\n}",
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<TextDocumentItem>(encoded)
        decoded shouldBe item
        decoded.text.contains("\n") shouldBe true
    }

    // ==================== TextDocumentPositionParams Tests ====================

    @Test
    fun `TextDocumentPositionParams serialization`() {
        val params =
            TextDocumentPositionParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                position = Position(10, 5),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<TextDocumentPositionParams>(encoded)
        decoded shouldBe params
    }

    // ==================== TextEdit Tests ====================

    @Test
    fun `TextEdit serialization`() {
        val edit =
            TextEdit(
                range = Range(Position(5, 0), Position(5, 10)),
                newText = "replacement",
            )
        val encoded = json.encodeToString(edit)
        val decoded = json.decodeFromString<TextEdit>(encoded)
        decoded shouldBe edit
    }

    @Test
    fun `TextEdit for deletion (empty newText)`() {
        val edit =
            TextEdit(
                range = Range(Position(5, 0), Position(5, 10)),
                newText = "",
            )
        val encoded = json.encodeToString(edit)
        val decoded = json.decodeFromString<TextEdit>(encoded)
        decoded shouldBe edit
        decoded.newText shouldBe ""
    }

    @Test
    fun `TextEdit for insertion (empty range)`() {
        val edit =
            TextEdit(
                range = Range(Position(5, 10), Position(5, 10)),
                newText = "inserted text",
            )
        val encoded = json.encodeToString(edit)
        val decoded = json.decodeFromString<TextEdit>(encoded)
        decoded shouldBe edit
    }

    // ==================== TextDocumentEdit Tests ====================

    @Test
    fun `TextDocumentEdit serialization`() {
        val docEdit =
            TextDocumentEdit(
                textDocument =
                    OptionalVersionedTextDocumentIdentifier(
                        uri = "file:///test.kt",
                        version = 5,
                    ),
                edits =
                    listOf(
                        Either.Left(TextEdit(Range(Position(1, 0), Position(1, 5)), "new")),
                        Either.Left(TextEdit(Range(Position(3, 0), Position(3, 10)), "replacement")),
                    ),
            )
        val encoded = json.encodeToString(docEdit)
        val decoded = json.decodeFromString<TextDocumentEdit>(encoded)
        decoded shouldBe docEdit
    }

    @Test
    fun `TextDocumentEdit with empty edits list`() {
        val docEdit =
            TextDocumentEdit(
                textDocument = OptionalVersionedTextDocumentIdentifier(uri = "file:///test.kt"),
                edits = emptyList(),
            )
        val encoded = json.encodeToString(docEdit)
        val decoded = json.decodeFromString<TextDocumentEdit>(encoded)
        decoded shouldBe docEdit
        decoded.edits shouldBe emptyList()
    }
}
