package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for Formatting-related types including:
 * - FormattingOptions
 * - DocumentFormattingParams
 * - DocumentRangeFormattingParams
 * - DocumentOnTypeFormattingParams
 */
class FormattingSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== FormattingOptions Tests ====================

    @Test
    fun `FormattingOptions minimal`() {
        val options =
            FormattingOptions(
                tabSize = 4,
                insertSpaces = true,
            )
        val encoded = json.encodeToString(options)
        encoded shouldContain "\"tabSize\":4"
        encoded shouldContain "\"insertSpaces\":true"
    }

    @Test
    fun `FormattingOptions with tabs`() {
        val options =
            FormattingOptions(
                tabSize = 2,
                insertSpaces = false,
            )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<FormattingOptions>(encoded)
        decoded.tabSize shouldBe 2
        decoded.insertSpaces shouldBe false
    }

    @Test
    fun `FormattingOptions full`() {
        val options =
            FormattingOptions(
                tabSize = 4,
                insertSpaces = true,
                trimTrailingWhitespace = true,
                insertFinalNewline = true,
                trimFinalNewlines = true,
            )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<FormattingOptions>(encoded)
        decoded.trimTrailingWhitespace shouldBe true
        decoded.insertFinalNewline shouldBe true
        decoded.trimFinalNewlines shouldBe true
    }

    @Test
    fun `FormattingOptions with all false`() {
        val options =
            FormattingOptions(
                tabSize = 8,
                insertSpaces = false,
                trimTrailingWhitespace = false,
                insertFinalNewline = false,
                trimFinalNewlines = false,
            )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<FormattingOptions>(encoded)
        decoded.trimTrailingWhitespace shouldBe false
        decoded.insertFinalNewline shouldBe false
        decoded.trimFinalNewlines shouldBe false
    }

    @Test
    fun `FormattingOptions round-trip`() {
        val options =
            FormattingOptions(
                tabSize = 2,
                insertSpaces = true,
                trimTrailingWhitespace = true,
            )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<FormattingOptions>(encoded)
        decoded shouldBe options
    }

    @Test
    fun `FormattingOptions tab size edge cases`() {
        val options1 = FormattingOptions(tabSize = 1, insertSpaces = true)
        val options8 = FormattingOptions(tabSize = 8, insertSpaces = true)

        json.decodeFromString<FormattingOptions>(json.encodeToString(options1)).tabSize shouldBe 1
        json.decodeFromString<FormattingOptions>(json.encodeToString(options8)).tabSize shouldBe 8
    }

    // ==================== DocumentFormattingParams Tests ====================

    @Test
    fun `DocumentFormattingParams serialization`() {
        val params =
            DocumentFormattingParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                options = FormattingOptions(tabSize = 4, insertSpaces = true),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///test.kt\""
        encoded shouldContain "\"tabSize\":4"
    }

    @Test
    fun `DocumentFormattingParams round-trip`() {
        val params =
            DocumentFormattingParams(
                textDocument = TextDocumentIdentifier(uri = "file:///src/main.kt"),
                options =
                    FormattingOptions(
                        tabSize = 2,
                        insertSpaces = true,
                        trimTrailingWhitespace = true,
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DocumentFormattingParams>(encoded)
        decoded shouldBe params
    }

    @Test
    fun `DocumentFormattingParams with tabs`() {
        val params =
            DocumentFormattingParams(
                textDocument = TextDocumentIdentifier(uri = "file:///Makefile"),
                options = FormattingOptions(tabSize = 8, insertSpaces = false),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DocumentFormattingParams>(encoded)
        decoded.options.insertSpaces shouldBe false
    }

    // ==================== DocumentRangeFormattingParams Tests ====================

    @Test
    fun `DocumentRangeFormattingParams serialization`() {
        val params =
            DocumentRangeFormattingParams(
                textDocument = TextDocumentIdentifier(uri = "file:///code.kt"),
                range =
                    Range(
                        start = Position(line = 10, character = 0),
                        end = Position(line = 20, character = 0),
                    ),
                options = FormattingOptions(tabSize = 4, insertSpaces = true),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///code.kt\""
        encoded shouldContain "\"start\""
        encoded shouldContain "\"end\""
    }

    @Test
    fun `DocumentRangeFormattingParams round-trip`() {
        val params =
            DocumentRangeFormattingParams(
                textDocument = TextDocumentIdentifier(uri = "file:///format.kt"),
                range =
                    Range(
                        start = Position(line = 5, character = 2),
                        end = Position(line = 15, character = 10),
                    ),
                options = FormattingOptions(tabSize = 2, insertSpaces = true),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DocumentRangeFormattingParams>(encoded)
        decoded shouldBe params
    }

    @Test
    fun `DocumentRangeFormattingParams single line range`() {
        val params =
            DocumentRangeFormattingParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                range =
                    Range(
                        start = Position(line = 5, character = 0),
                        end = Position(line = 5, character = 50),
                    ),
                options = FormattingOptions(tabSize = 4, insertSpaces = true),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DocumentRangeFormattingParams>(encoded)
        decoded.range.start.line shouldBe decoded.range.end.line
    }

    @Test
    fun `DocumentRangeFormattingParams empty range`() {
        val params =
            DocumentRangeFormattingParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                range =
                    Range(
                        start = Position(line = 10, character = 5),
                        end = Position(line = 10, character = 5),
                    ),
                options = FormattingOptions(tabSize = 4, insertSpaces = true),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DocumentRangeFormattingParams>(encoded)
        decoded.range.start shouldBe decoded.range.end
    }

    // ==================== DocumentOnTypeFormattingParams Tests ====================

    @Test
    fun `DocumentOnTypeFormattingParams serialization`() {
        val params =
            DocumentOnTypeFormattingParams(
                textDocument = TextDocumentIdentifier(uri = "file:///typing.kt"),
                position = Position(line = 15, character = 25),
                ch = "}",
                options = FormattingOptions(tabSize = 4, insertSpaces = true),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"ch\":\"}\""
    }

    @Test
    fun `DocumentOnTypeFormattingParams round-trip`() {
        val params =
            DocumentOnTypeFormattingParams(
                textDocument = TextDocumentIdentifier(uri = "file:///code.kt"),
                position = Position(line = 20, character = 1),
                ch = "\n",
                options = FormattingOptions(tabSize = 2, insertSpaces = true),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DocumentOnTypeFormattingParams>(encoded)
        decoded shouldBe params
    }

    @Test
    fun `DocumentOnTypeFormattingParams closing brace`() {
        val params =
            DocumentOnTypeFormattingParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                position = Position(line = 100, character = 0),
                ch = "}",
                options = FormattingOptions(tabSize = 4, insertSpaces = true),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DocumentOnTypeFormattingParams>(encoded)
        decoded.ch shouldBe "}"
    }

    @Test
    fun `DocumentOnTypeFormattingParams semicolon`() {
        val params =
            DocumentOnTypeFormattingParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.java"),
                position = Position(line = 50, character = 30),
                ch = ";",
                options = FormattingOptions(tabSize = 4, insertSpaces = true),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DocumentOnTypeFormattingParams>(encoded)
        decoded.ch shouldBe ";"
    }

    @Test
    fun `DocumentOnTypeFormattingParams newline character`() {
        val params =
            DocumentOnTypeFormattingParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.py"),
                position = Position(line = 10, character = 0),
                ch = "\n",
                options = FormattingOptions(tabSize = 4, insertSpaces = true),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DocumentOnTypeFormattingParams>(encoded)
        decoded.ch shouldBe "\n"
    }

    @Test
    fun `DocumentOnTypeFormattingParams colon`() {
        val params =
            DocumentOnTypeFormattingParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.py"),
                position = Position(line = 5, character = 15),
                ch = ":",
                options = FormattingOptions(tabSize = 4, insertSpaces = true),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DocumentOnTypeFormattingParams>(encoded)
        decoded.ch shouldBe ":"
    }
}
