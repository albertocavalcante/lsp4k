package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class TypesTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    @Test
    fun `Position serialization roundtrip`() {
        val position = Position(line = 10, character = 5)
        val encoded = json.encodeToString(position)
        val decoded = json.decodeFromString<Position>(encoded)
        decoded shouldBe position
    }

    @Test
    fun `Range serialization roundtrip`() {
        val range =
            Range(
                start = Position(0, 0),
                end = Position(10, 20),
            )
        val encoded = json.encodeToString(range)
        val decoded = json.decodeFromString<Range>(encoded)
        decoded shouldBe range
    }

    @Test
    fun `Location serialization roundtrip`() {
        val location =
            Location(
                uri = "file:///path/to/file.kt",
                range = Range(Position(1, 0), Position(1, 10)),
            )
        val encoded = json.encodeToString(location)
        val decoded = json.decodeFromString<Location>(encoded)
        decoded shouldBe location
    }

    @Test
    fun `Position comparison`() {
        val p1 = Position(1, 5)
        val p2 = Position(1, 10)
        val p3 = Position(2, 0)

        (p1 < p2) shouldBe true
        (p2 < p3) shouldBe true
        (p1 < p3) shouldBe true
    }

    @Test
    fun `Range contains position`() {
        val range = Range(Position(1, 0), Position(3, 0))

        range.contains(Position(0, 0)) shouldBe false
        range.contains(Position(1, 0)) shouldBe true
        range.contains(Position(2, 5)) shouldBe true
        range.contains(Position(3, 0)) shouldBe false
    }

    @Test
    fun `TextDocumentIdentifier serialization`() {
        val doc = TextDocumentIdentifier(uri = "file:///test.kt")
        val encoded = json.encodeToString(doc)
        encoded shouldBe """{"uri":"file:///test.kt"}"""
    }

    @Test
    fun `CompletionItem serialization with optional fields`() {
        val item =
            CompletionItem(
                label = "myFunction",
                kind = CompletionItemKind.Function,
                detail = "fun myFunction(): Unit",
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.label shouldBe "myFunction"
        decoded.kind shouldBe CompletionItemKind.Function
        decoded.detail shouldBe "fun myFunction(): Unit"
    }
}
