package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for Color-related types including:
 * - Color
 * - DocumentColorParams
 * - ColorInformation
 * - ColorPresentationParams
 * - ColorPresentation
 */
class ColorSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== Color Tests ====================

    @Test
    fun `Color red`() {
        val color = Color(red = 1.0, green = 0.0, blue = 0.0, alpha = 1.0)
        val encoded = json.encodeToString(color)
        val decoded = json.decodeFromString<Color>(encoded)
        decoded.red shouldBe 1.0
        decoded.green shouldBe 0.0
        decoded.blue shouldBe 0.0
        decoded.alpha shouldBe 1.0
    }

    @Test
    fun `Color green`() {
        val color = Color(red = 0.0, green = 1.0, blue = 0.0, alpha = 1.0)
        val encoded = json.encodeToString(color)
        val decoded = json.decodeFromString<Color>(encoded)
        decoded shouldBe color
    }

    @Test
    fun `Color blue`() {
        val color = Color(red = 0.0, green = 0.0, blue = 1.0, alpha = 1.0)
        val encoded = json.encodeToString(color)
        val decoded = json.decodeFromString<Color>(encoded)
        decoded shouldBe color
    }

    @Test
    fun `Color with alpha`() {
        val color = Color(red = 0.5, green = 0.5, blue = 0.5, alpha = 0.5)
        val encoded = json.encodeToString(color)
        val decoded = json.decodeFromString<Color>(encoded)
        decoded.alpha shouldBe 0.5
    }

    @Test
    fun `Color transparent`() {
        val color = Color(red = 1.0, green = 1.0, blue = 1.0, alpha = 0.0)
        val encoded = json.encodeToString(color)
        val decoded = json.decodeFromString<Color>(encoded)
        decoded.alpha shouldBe 0.0
    }

    @Test
    fun `Color edge values`() {
        val color = Color(red = 0.0, green = 0.0, blue = 0.0, alpha = 0.0)
        val encoded = json.encodeToString(color)
        val decoded = json.decodeFromString<Color>(encoded)
        decoded shouldBe color
    }

    @Test
    fun `Color fractional values`() {
        val color = Color(red = 0.2, green = 0.4, blue = 0.6, alpha = 0.8)
        val encoded = json.encodeToString(color)
        val decoded = json.decodeFromString<Color>(encoded)
        decoded.red shouldBe 0.2
        decoded.green shouldBe 0.4
        decoded.blue shouldBe 0.6
        decoded.alpha shouldBe 0.8
    }

    // ==================== DocumentColorParams Tests ====================

    @Test
    fun `DocumentColorParams serialization`() {
        val params =
            DocumentColorParams(
                textDocument = TextDocumentIdentifier(uri = "file:///style.css"),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///style.css\""
    }

    @Test
    fun `DocumentColorParams round-trip`() {
        val params =
            DocumentColorParams(
                textDocument = TextDocumentIdentifier(uri = "file:///colors.kt"),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DocumentColorParams>(encoded)
        decoded shouldBe params
    }

    // ==================== ColorInformation Tests ====================

    @Test
    fun `ColorInformation serialization`() {
        val info =
            ColorInformation(
                range = Range(Position(10, 15), Position(10, 22)),
                color = Color(red = 1.0, green = 0.0, blue = 0.0, alpha = 1.0),
            )
        val encoded = json.encodeToString(info)
        encoded shouldContain "\"range\""
        encoded shouldContain "\"color\""
    }

    @Test
    fun `ColorInformation round-trip`() {
        val info =
            ColorInformation(
                range = Range(Position(5, 8), Position(5, 25)),
                color = Color(red = 0.0, green = 0.5, blue = 1.0, alpha = 0.9),
            )
        val encoded = json.encodeToString(info)
        val decoded = json.decodeFromString<ColorInformation>(encoded)
        decoded shouldBe info
    }

    @Test
    fun `ColorInformation list`() {
        val colors =
            listOf(
                ColorInformation(
                    range = Range(Position(10, 0), Position(10, 7)),
                    color = Color(1.0, 0.0, 0.0, 1.0),
                ),
                ColorInformation(
                    range = Range(Position(20, 0), Position(20, 7)),
                    color = Color(0.0, 1.0, 0.0, 1.0),
                ),
                ColorInformation(
                    range = Range(Position(30, 0), Position(30, 7)),
                    color = Color(0.0, 0.0, 1.0, 1.0),
                ),
            )
        val encoded = json.encodeToString(colors)
        val decoded = json.decodeFromString<List<ColorInformation>>(encoded)
        decoded.size shouldBe 3
    }

    // ==================== ColorPresentationParams Tests ====================

    @Test
    fun `ColorPresentationParams serialization`() {
        val params =
            ColorPresentationParams(
                textDocument = TextDocumentIdentifier(uri = "file:///colors.kt"),
                color = Color(red = 1.0, green = 0.5, blue = 0.0, alpha = 1.0),
                range = Range(Position(15, 10), Position(15, 25)),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"textDocument\""
        encoded shouldContain "\"color\""
        encoded shouldContain "\"range\""
    }

    @Test
    fun `ColorPresentationParams round-trip`() {
        val params =
            ColorPresentationParams(
                textDocument = TextDocumentIdentifier(uri = "file:///theme.css"),
                color = Color(red = 0.2, green = 0.4, blue = 0.6, alpha = 1.0),
                range = Range(Position(100, 5), Position(100, 20)),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<ColorPresentationParams>(encoded)
        decoded shouldBe params
    }

    // ==================== ColorPresentation Tests ====================

    @Test
    fun `ColorPresentation minimal`() {
        val presentation = ColorPresentation(label = "#FF0000")
        val encoded = json.encodeToString(presentation)
        encoded shouldBe """{"label":"#FF0000"}"""
    }

    @Test
    fun `ColorPresentation with textEdit`() {
        val presentation =
            ColorPresentation(
                label = "rgb(255, 0, 0)",
                textEdit =
                    TextEdit(
                        range = Range(Position(10, 5), Position(10, 12)),
                        newText = "rgb(255, 0, 0)",
                    ),
            )
        val encoded = json.encodeToString(presentation)
        val decoded = json.decodeFromString<ColorPresentation>(encoded)
        decoded.textEdit?.newText shouldBe "rgb(255, 0, 0)"
    }

    @Test
    fun `ColorPresentation with additionalTextEdits`() {
        val presentation =
            ColorPresentation(
                label = "#00FF00",
                additionalTextEdits =
                    listOf(
                        TextEdit(
                            range = Range(Position(1, 0), Position(1, 0)),
                            newText = "import Color\n",
                        ),
                    ),
            )
        val encoded = json.encodeToString(presentation)
        val decoded = json.decodeFromString<ColorPresentation>(encoded)
        decoded.additionalTextEdits?.size shouldBe 1
    }

    @Test
    fun `ColorPresentation full`() {
        val presentation =
            ColorPresentation(
                label = "Color(0.0, 0.0, 1.0, 1.0)",
                textEdit =
                    TextEdit(
                        range = Range(Position(20, 8), Position(20, 15)),
                        newText = "Color(0.0, 0.0, 1.0, 1.0)",
                    ),
                additionalTextEdits =
                    listOf(
                        TextEdit(
                            range = Range(Position(0, 0), Position(0, 0)),
                            newText = "import io.lsp4k.protocol.Color\n",
                        ),
                    ),
            )
        val encoded = json.encodeToString(presentation)
        val decoded = json.decodeFromString<ColorPresentation>(encoded)
        decoded shouldBe presentation
    }

    @Test
    fun `ColorPresentation list`() {
        val presentations =
            listOf(
                ColorPresentation(label = "#FF0000"),
                ColorPresentation(label = "rgb(255, 0, 0)"),
                ColorPresentation(label = "hsl(0, 100%, 50%)"),
                ColorPresentation(label = "Color.RED"),
            )
        val encoded = json.encodeToString(presentations)
        val decoded = json.decodeFromString<List<ColorPresentation>>(encoded)
        decoded.size shouldBe 4
        decoded[0].label shouldBe "#FF0000"
        decoded[3].label shouldBe "Color.RED"
    }
}
