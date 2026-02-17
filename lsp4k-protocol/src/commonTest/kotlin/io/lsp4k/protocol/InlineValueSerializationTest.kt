package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for InlineValue type serialization: InlineValueText, InlineValueVariableLookup,
 * InlineValueEvaluatableExpression, InlineValue (sealed), InlineValueContext, InlineValueParams.
 */
class InlineValueSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== InlineValueText Tests ====================

    @Test
    fun `InlineValueText serialization roundtrip`() {
        val original =
            InlineValueText(
                range = Range(Position(10, 0), Position(10, 20)),
                text = "x = 42",
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineValueText>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `InlineValueText with empty text`() {
        val original =
            InlineValueText(
                range = Range(Position(0, 0), Position(0, 0)),
                text = "",
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineValueText>(encoded)
        decoded shouldBe original
    }

    // ==================== InlineValueVariableLookup Tests ====================

    @Test
    fun `InlineValueVariableLookup serialization roundtrip`() {
        val original =
            InlineValueVariableLookup(
                range = Range(Position(5, 4), Position(5, 10)),
                variableName = "counter",
                caseSensitiveLookup = true,
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineValueVariableLookup>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `InlineValueVariableLookup without variableName`() {
        val original =
            InlineValueVariableLookup(
                range = Range(Position(3, 0), Position(3, 5)),
                caseSensitiveLookup = false,
            )
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "variableName"
        val decoded = json.decodeFromString<InlineValueVariableLookup>(encoded)
        decoded shouldBe original
        decoded.variableName shouldBe null
    }

    @Test
    fun `InlineValueVariableLookup case insensitive`() {
        val original =
            InlineValueVariableLookup(
                range = Range(Position(0, 0), Position(0, 3)),
                variableName = "foo",
                caseSensitiveLookup = false,
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineValueVariableLookup>(encoded)
        decoded.caseSensitiveLookup shouldBe false
    }

    // ==================== InlineValueEvaluatableExpression Tests ====================

    @Test
    fun `InlineValueEvaluatableExpression serialization roundtrip`() {
        val original =
            InlineValueEvaluatableExpression(
                range = Range(Position(7, 0), Position(7, 15)),
                expression = "a + b",
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineValueEvaluatableExpression>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `InlineValueEvaluatableExpression without expression`() {
        val original =
            InlineValueEvaluatableExpression(
                range = Range(Position(2, 0), Position(2, 10)),
            )
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "expression"
        val decoded = json.decodeFromString<InlineValueEvaluatableExpression>(encoded)
        decoded shouldBe original
        decoded.expression shouldBe null
    }

    // ==================== InlineValue (sealed interface) Tests ====================

    @Test
    fun `InlineValue serialization roundtrip for InlineValueText`() {
        val original: InlineValue =
            InlineValueText(
                range = Range(Position(1, 0), Position(1, 10)),
                text = "value = 123",
            )
        val encoded = json.encodeToString(original)
        encoded shouldContain "text"
        val decoded = json.decodeFromString<InlineValue>(encoded)
        decoded.shouldBeInstanceOf<InlineValueText>()
        (decoded as InlineValueText).text shouldBe "value = 123"
    }

    @Test
    fun `InlineValue serialization roundtrip for InlineValueVariableLookup`() {
        val original: InlineValue =
            InlineValueVariableLookup(
                range = Range(Position(2, 0), Position(2, 5)),
                variableName = "myVar",
                caseSensitiveLookup = true,
            )
        val encoded = json.encodeToString(original)
        encoded shouldContain "caseSensitiveLookup"
        val decoded = json.decodeFromString<InlineValue>(encoded)
        decoded.shouldBeInstanceOf<InlineValueVariableLookup>()
        (decoded as InlineValueVariableLookup).variableName shouldBe "myVar"
    }

    @Test
    fun `InlineValue serialization roundtrip for InlineValueEvaluatableExpression`() {
        val original: InlineValue =
            InlineValueEvaluatableExpression(
                range = Range(Position(3, 0), Position(3, 8)),
                expression = "x * 2",
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineValue>(encoded)
        decoded.shouldBeInstanceOf<InlineValueEvaluatableExpression>()
        (decoded as InlineValueEvaluatableExpression).expression shouldBe "x * 2"
    }

    @Test
    fun `InlineValue deserializes expression without expression field as EvaluatableExpression`() {
        val original: InlineValue =
            InlineValueEvaluatableExpression(
                range = Range(Position(4, 0), Position(4, 5)),
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineValue>(encoded)
        decoded.shouldBeInstanceOf<InlineValueEvaluatableExpression>()
    }

    // ==================== InlineValueContext Tests ====================

    @Test
    fun `InlineValueContext serialization roundtrip`() {
        val original =
            InlineValueContext(
                frameId = 42,
                stoppedLocation = Range(Position(10, 0), Position(10, 20)),
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineValueContext>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `InlineValueContext with zero frameId`() {
        val original =
            InlineValueContext(
                frameId = 0,
                stoppedLocation = Range(Position(0, 0), Position(0, 0)),
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineValueContext>(encoded)
        decoded shouldBe original
    }

    // ==================== InlineValueParams Tests ====================

    @Test
    fun `InlineValueParams serialization roundtrip`() {
        val original =
            InlineValueParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                range = Range(Position(0, 0), Position(50, 0)),
                context =
                    InlineValueContext(
                        frameId = 1,
                        stoppedLocation = Range(Position(25, 0), Position(25, 30)),
                    ),
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineValueParams>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `InlineValueParams contains expected fields`() {
        val original =
            InlineValueParams(
                textDocument = TextDocumentIdentifier(uri = "file:///app.py"),
                range = Range(Position(10, 0), Position(20, 0)),
                context =
                    InlineValueContext(
                        frameId = 99,
                        stoppedLocation = Range(Position(15, 0), Position(15, 10)),
                    ),
            )
        val encoded = json.encodeToString(original)
        encoded shouldContain "textDocument"
        encoded shouldContain "file:///app.py"
        encoded shouldContain "frameId"
    }
}
