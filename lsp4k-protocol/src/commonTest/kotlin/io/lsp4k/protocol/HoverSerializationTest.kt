package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for Hover type serialization: Hover, MarkupContent, MarkupKind,
 * MarkedString, HoverContents.
 */
class HoverSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== MarkupKind Tests ====================

    @Test
    fun `MarkupKind PlainText serialization`() {
        val kind = MarkupKind.PlainText
        val encoded = json.encodeToString(kind)
        encoded shouldBe "\"plaintext\""
    }

    @Test
    fun `MarkupKind Markdown serialization`() {
        val kind = MarkupKind.Markdown
        val encoded = json.encodeToString(kind)
        encoded shouldBe "\"markdown\""
    }

    @Test
    fun `MarkupKind deserialization`() {
        json.decodeFromString<MarkupKind>("\"plaintext\"") shouldBe MarkupKind.PlainText
        json.decodeFromString<MarkupKind>("\"markdown\"") shouldBe MarkupKind.Markdown
    }

    @Test
    fun `MarkupKind roundtrip for all values`() {
        MarkupKind.entries.forEach { kind ->
            val encoded = json.encodeToString(kind)
            val decoded = json.decodeFromString<MarkupKind>(encoded)
            decoded shouldBe kind
        }
    }

    // ==================== MarkupContent Tests ====================

    @Test
    fun `MarkupContent plaintext serialization`() {
        val content =
            MarkupContent(
                kind = MarkupKind.PlainText,
                value = "Simple text content",
            )
        val encoded = json.encodeToString(content)
        encoded shouldBe """{"kind":"plaintext","value":"Simple text content"}"""
    }

    @Test
    fun `MarkupContent markdown serialization`() {
        val content =
            MarkupContent(
                kind = MarkupKind.Markdown,
                value = "# Header\n\nSome **bold** text",
            )
        val encoded = json.encodeToString(content)
        val decoded = json.decodeFromString<MarkupContent>(encoded)
        decoded shouldBe content
    }

    @Test
    fun `MarkupContent roundtrip`() {
        val content =
            MarkupContent(
                kind = MarkupKind.Markdown,
                value =
                    """
                    ## Function Documentation

                    ```kotlin
                    fun example(): String
                    ```

                    Returns a sample string.
                    """.trimIndent(),
            )
        val encoded = json.encodeToString(content)
        val decoded = json.decodeFromString<MarkupContent>(encoded)
        decoded shouldBe content
    }

    @Test
    fun `MarkupContent with empty value`() {
        val content =
            MarkupContent(
                kind = MarkupKind.PlainText,
                value = "",
            )
        val encoded = json.encodeToString(content)
        val decoded = json.decodeFromString<MarkupContent>(encoded)
        decoded.value shouldBe ""
    }

    @Test
    fun `MarkupContent with special characters`() {
        val content =
            MarkupContent(
                kind = MarkupKind.PlainText,
                value = "Contains \"quotes\" and \\backslashes\\ and\nnewlines",
            )
        val encoded = json.encodeToString(content)
        val decoded = json.decodeFromString<MarkupContent>(encoded)
        decoded shouldBe content
    }

    @Test
    fun `MarkupContent with code block`() {
        val content =
            MarkupContent(
                kind = MarkupKind.Markdown,
                value =
                    """
                    ```kotlin
                    fun hello() {
                        println("Hello, World!")
                    }
                    ```
                    """.trimIndent(),
            )
        val encoded = json.encodeToString(content)
        val decoded = json.decodeFromString<MarkupContent>(encoded)
        decoded.value.contains("```kotlin") shouldBe true
    }

    // ==================== Hover Tests (MarkupContent / Markup form) ====================

    @Test
    fun `Hover minimal (contents only)`() {
        val hover =
            Hover(
                contents =
                    HoverContents.Markup(
                        MarkupContent(
                            kind = MarkupKind.PlainText,
                            value = "Hover content",
                        ),
                    ),
            )
        val encoded = json.encodeToString(hover)
        // range should not be present when null and encodeDefaults = false
        encoded.contains("range") shouldBe false

        val decoded = json.decodeFromString<Hover>(encoded)
        decoded shouldBe hover
        decoded.range shouldBe null
    }

    @Test
    fun `Hover with range`() {
        val hover =
            Hover(
                contents =
                    HoverContents.Markup(
                        MarkupContent(
                            kind = MarkupKind.Markdown,
                            value = "**Important** content",
                        ),
                    ),
                range = Range(Position(10, 5), Position(10, 15)),
            )
        val encoded = json.encodeToString(hover)
        val decoded = json.decodeFromString<Hover>(encoded)
        decoded shouldBe hover
        decoded.range shouldBe Range(Position(10, 5), Position(10, 15))
    }

    @Test
    fun `Hover roundtrip with markdown content`() {
        val hover =
            Hover(
                contents =
                    HoverContents.Markup(
                        MarkupContent(
                            kind = MarkupKind.Markdown,
                            value =
                                """
                                ## `myFunction`

                                ```kotlin
                                fun myFunction(param: String): Int
                                ```

                                This function processes the given parameter.

                                **Parameters:**
                                - `param` - the input string

                                **Returns:** the length of the string
                                """.trimIndent(),
                        ),
                    ),
                range = Range(Position(5, 0), Position(5, 10)),
            )
        val encoded = json.encodeToString(hover)
        val decoded = json.decodeFromString<Hover>(encoded)
        decoded shouldBe hover
    }

    @Test
    fun `Hover deserialization with extra fields`() {
        val jsonStr =
            """
            {
                "contents": {"kind": "plaintext", "value": "Test"},
                "unknownField": "ignored"
            }
            """.trimIndent()
        val decoded = json.decodeFromString<Hover>(jsonStr)
        val contents = decoded.contents.shouldBeInstanceOf<HoverContents.Markup>()
        contents.content.value shouldBe "Test"
    }

    @Test
    fun `Hover with plaintext content`() {
        val hover =
            Hover(
                contents =
                    HoverContents.Markup(
                        MarkupContent(
                            kind = MarkupKind.PlainText,
                            value = "fun example(): Unit",
                        ),
                    ),
            )
        val encoded = json.encodeToString(hover)
        val decoded = json.decodeFromString<Hover>(encoded)
        val contents = decoded.contents.shouldBeInstanceOf<HoverContents.Markup>()
        contents.content.kind shouldBe MarkupKind.PlainText
    }

    // ==================== Hover Tests (MarkedString forms) ====================

    @Test
    fun `Hover with plain string MarkedString`() {
        val jsonStr =
            """
            {
                "contents": "Hello, world!"
            }
            """.trimIndent()
        val decoded = json.decodeFromString<Hover>(jsonStr)
        val contents = decoded.contents.shouldBeInstanceOf<HoverContents.SingleMarked>()
        contents.value.shouldBeInstanceOf<Either.Left<String>>()
        contents.value.left shouldBe "Hello, world!"
    }

    @Test
    fun `Hover with plain string MarkedString roundtrip`() {
        val hover =
            Hover(
                contents = HoverContents.SingleMarked(Either.Left("some markdown text")),
            )
        val encoded = json.encodeToString(hover)
        val decoded = json.decodeFromString<Hover>(encoded)
        decoded shouldBe hover
    }

    @Test
    fun `Hover with MarkedString object (language and value)`() {
        val jsonStr =
            """
            {
                "contents": {"language": "kotlin", "value": "fun hello()"}
            }
            """.trimIndent()
        val decoded = json.decodeFromString<Hover>(jsonStr)
        val contents = decoded.contents.shouldBeInstanceOf<HoverContents.SingleMarked>()
        contents.value.shouldBeInstanceOf<Either.Right<MarkedString>>()
        val markedString = contents.value.value
        markedString.language shouldBe "kotlin"
        markedString.value shouldBe "fun hello()"
    }

    @Test
    fun `Hover with MarkedString object roundtrip`() {
        val hover =
            Hover(
                contents =
                    HoverContents.SingleMarked(
                        Either.Right(MarkedString(language = "typescript", value = "const x = 1")),
                    ),
            )
        val encoded = json.encodeToString(hover)
        val decoded = json.decodeFromString<Hover>(encoded)
        decoded shouldBe hover
    }

    @Test
    fun `Hover with MarkedString array`() {
        val jsonStr =
            """
            {
                "contents": [
                    "Some documentation",
                    {"language": "kotlin", "value": "fun example(): String"}
                ]
            }
            """.trimIndent()
        val decoded = json.decodeFromString<Hover>(jsonStr)
        val contents = decoded.contents.shouldBeInstanceOf<HoverContents.MultiMarked>()
        contents.values.size shouldBe 2
        contents.values[0].shouldBeInstanceOf<Either.Left<String>>()
        contents.values[0].left shouldBe "Some documentation"
        contents.values[1].shouldBeInstanceOf<Either.Right<MarkedString>>()
        (contents.values[1] as Either.Right<MarkedString>).value.language shouldBe "kotlin"
    }

    @Test
    fun `Hover with MarkedString array roundtrip`() {
        val hover =
            Hover(
                contents =
                    HoverContents.MultiMarked(
                        listOf(
                            Either.Left("First paragraph"),
                            Either.Right(MarkedString(language = "python", value = "def foo(): pass")),
                            Either.Left("Second paragraph"),
                        ),
                    ),
            )
        val encoded = json.encodeToString(hover)
        val decoded = json.decodeFromString<Hover>(encoded)
        decoded shouldBe hover
    }

    @Test
    fun `Hover with empty MarkedString array`() {
        val jsonStr =
            """
            {
                "contents": []
            }
            """.trimIndent()
        val decoded = json.decodeFromString<Hover>(jsonStr)
        val contents = decoded.contents.shouldBeInstanceOf<HoverContents.MultiMarked>()
        contents.values.size shouldBe 0
    }

    // ==================== HoverParams Tests ====================

    @Test
    fun `HoverParams serialization`() {
        val params =
            HoverParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                position = Position(15, 10),
            )
        val encoded = json.encodeToString(params)
        encoded shouldBe """{"textDocument":{"uri":"file:///test.kt"},"position":{"line":15,"character":10}}"""
    }

    @Test
    fun `HoverParams roundtrip`() {
        val params =
            HoverParams(
                textDocument = TextDocumentIdentifier(uri = "file:///path/to/File.kt"),
                position = Position(100, 25),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<HoverParams>(encoded)
        decoded shouldBe params
    }

    @Test
    fun `HoverParams deserialization from JSON`() {
        val jsonStr = """{"textDocument":{"uri":"file:///example.kt"},"position":{"line":5,"character":8}}"""
        val decoded = json.decodeFromString<HoverParams>(jsonStr)
        decoded.textDocument.uri shouldBe "file:///example.kt"
        decoded.position.line shouldBe 5
        decoded.position.character shouldBe 8
    }
}
