package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Tests for InlayHint-related types including:
 * - InlayHintKind (integer-serialized enum)
 * - InlayHintParams
 * - InlayHintLabelPart
 * - InlayHint
 */
class InlayHintSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== InlayHintKind Tests ====================

    @Test
    fun `InlayHintKind Type serializes to 1`() {
        val encoded = json.encodeToString(InlayHintKind.Type)
        encoded shouldBe "1"
    }

    @Test
    fun `InlayHintKind Parameter serializes to 2`() {
        val encoded = json.encodeToString(InlayHintKind.Parameter)
        encoded shouldBe "2"
    }

    @Test
    fun `InlayHintKind deserializes from integers`() {
        json.decodeFromString<InlayHintKind>("1") shouldBe InlayHintKind.Type
        json.decodeFromString<InlayHintKind>("2") shouldBe InlayHintKind.Parameter
    }

    @Test
    fun `InlayHintKind fromValue throws for unknown value`() {
        assertFailsWith<IllegalArgumentException> {
            InlayHintKind.fromValue(99)
        }
    }

    // ==================== InlayHintParams Tests ====================

    @Test
    fun `InlayHintParams serialization`() {
        val params =
            InlayHintParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                range =
                    Range(
                        start = Position(line = 0, character = 0),
                        end = Position(line = 100, character = 0),
                    ),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///test.kt\""
        encoded shouldContain "\"range\""
    }

    @Test
    fun `InlayHintParams round-trip`() {
        val params =
            InlayHintParams(
                textDocument = TextDocumentIdentifier(uri = "file:///code.kt"),
                range =
                    Range(
                        start = Position(line = 10, character = 0),
                        end = Position(line = 50, character = 80),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<InlayHintParams>(encoded)
        decoded shouldBe params
    }

    // ==================== InlayHintLabelPart Tests ====================

    @Test
    fun `InlayHintLabelPart minimal`() {
        val part = InlayHintLabelPart(value = "String")
        val encoded = json.encodeToString(part)
        encoded shouldContain "\"value\":\"String\""
    }

    @Test
    fun `InlayHintLabelPart with tooltip`() {
        val part =
            InlayHintLabelPart(
                value = "Int",
                tooltip = Either.left("Inferred type: kotlin.Int"),
            )
        val encoded = json.encodeToString(part)
        val decoded = json.decodeFromString<InlayHintLabelPart>(encoded)
        decoded.tooltip shouldBe Either.left("Inferred type: kotlin.Int")
    }

    @Test
    fun `InlayHintLabelPart with location`() {
        val part =
            InlayHintLabelPart(
                value = "MyClass",
                location =
                    Location(
                        uri = "file:///types.kt",
                        range = Range(Position(10, 0), Position(50, 0)),
                    ),
            )
        val encoded = json.encodeToString(part)
        val decoded = json.decodeFromString<InlayHintLabelPart>(encoded)
        decoded.location?.uri shouldBe "file:///types.kt"
    }

    @Test
    fun `InlayHintLabelPart with command`() {
        val part =
            InlayHintLabelPart(
                value = "List<String>",
                command =
                    Command(
                        title = "Go to Definition",
                        command = "editor.action.goToDeclaration",
                    ),
            )
        val encoded = json.encodeToString(part)
        val decoded = json.decodeFromString<InlayHintLabelPart>(encoded)
        decoded.command?.command shouldBe "editor.action.goToDeclaration"
    }

    @Test
    fun `InlayHintLabelPart full`() {
        val part =
            InlayHintLabelPart(
                value = "Map<String, Int>",
                tooltip = Either.left("Click to go to definition"),
                location =
                    Location(
                        uri = "file:///stdlib.kt",
                        range = Range(Position(100, 0), Position(200, 0)),
                    ),
                command =
                    Command(
                        title = "Navigate",
                        command = "navigate",
                        arguments = listOf(JsonPrimitive("arg1")),
                    ),
            )
        val encoded = json.encodeToString(part)
        val decoded = json.decodeFromString<InlayHintLabelPart>(encoded)
        decoded shouldBe part
    }

    // ==================== InlayHint Tests ====================

    @Test
    fun `InlayHint minimal`() {
        val hint =
            InlayHint(
                position = Position(line = 10, character = 20),
                label = Either.left(": String"),
            )
        val encoded = json.encodeToString(hint)
        encoded shouldContain "\"label\":\": String\""
        encoded shouldContain "\"position\""
    }

    @Test
    fun `InlayHint with Type kind`() {
        val hint =
            InlayHint(
                position = Position(line = 5, character = 15),
                label = Either.left(": Int"),
                kind = InlayHintKind.Type,
            )
        val encoded = json.encodeToString(hint)
        encoded shouldContain "\"kind\":1"
        val decoded = json.decodeFromString<InlayHint>(encoded)
        decoded.kind shouldBe InlayHintKind.Type
    }

    @Test
    fun `InlayHint with Parameter kind`() {
        val hint =
            InlayHint(
                position = Position(line = 20, character = 10),
                label = Either.left("name:"),
                kind = InlayHintKind.Parameter,
            )
        val encoded = json.encodeToString(hint)
        encoded shouldContain "\"kind\":2"
        val decoded = json.decodeFromString<InlayHint>(encoded)
        decoded.kind shouldBe InlayHintKind.Parameter
    }

    @Test
    fun `InlayHint with text edits`() {
        val hint =
            InlayHint(
                position = Position(line = 15, character = 8),
                label = Either.left(": List<String>"),
                kind = InlayHintKind.Type,
                textEdits =
                    listOf(
                        TextEdit(
                            range = Range(Position(15, 8), Position(15, 8)),
                            newText = ": List<String>",
                        ),
                    ),
            )
        val encoded = json.encodeToString(hint)
        val decoded = json.decodeFromString<InlayHint>(encoded)
        decoded.textEdits?.size shouldBe 1
        decoded.textEdits?.first()?.newText shouldBe ": List<String>"
    }

    @Test
    fun `InlayHint with tooltip`() {
        val hint =
            InlayHint(
                position = Position(line = 30, character = 5),
                label = Either.left("value:"),
                kind = InlayHintKind.Parameter,
                tooltip = Either.left("Parameter name hint"),
            )
        val encoded = json.encodeToString(hint)
        val decoded = json.decodeFromString<InlayHint>(encoded)
        decoded.tooltip shouldBe Either.left("Parameter name hint")
    }

    @Test
    fun `InlayHint with padding`() {
        val hint =
            InlayHint(
                position = Position(line = 25, character = 12),
                label = Either.left(": Boolean"),
                paddingLeft = true,
                paddingRight = false,
            )
        val encoded = json.encodeToString(hint)
        val decoded = json.decodeFromString<InlayHint>(encoded)
        decoded.paddingLeft shouldBe true
        decoded.paddingRight shouldBe false
    }

    @Test
    fun `InlayHint with data`() {
        val hint =
            InlayHint(
                position = Position(line = 40, character = 0),
                label = Either.left("timeout:"),
                data = JsonPrimitive("resolve-data-123"),
            )
        val encoded = json.encodeToString(hint)
        val decoded = json.decodeFromString<InlayHint>(encoded)
        decoded.data shouldBe JsonPrimitive("resolve-data-123")
    }

    @Test
    fun `InlayHint full`() {
        val hint =
            InlayHint(
                position = Position(line = 50, character = 15),
                label = Either.left(": Map<String, Any>"),
                kind = InlayHintKind.Type,
                textEdits =
                    listOf(
                        TextEdit(
                            range = Range(Position(50, 15), Position(50, 15)),
                            newText = ": Map<String, Any>",
                        ),
                    ),
                tooltip = Either.left("Inferred return type"),
                paddingLeft = true,
                paddingRight = false,
                data = JsonPrimitive("hint-data"),
            )
        val encoded = json.encodeToString(hint)
        val decoded = json.decodeFromString<InlayHint>(encoded)
        decoded shouldBe hint
    }

    @Test
    fun `InlayHint list serialization`() {
        val hints =
            listOf(
                InlayHint(position = Position(5, 10), label = Either.left(": Int"), kind = InlayHintKind.Type),
                InlayHint(position = Position(10, 8), label = Either.left("name:"), kind = InlayHintKind.Parameter),
                InlayHint(position = Position(15, 12), label = Either.left("count:"), kind = InlayHintKind.Parameter),
            )
        val encoded = json.encodeToString(hints)
        val decoded = json.decodeFromString<List<InlayHint>>(encoded)
        decoded.size shouldBe 3
    }
}
