package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Tests for TextDocument type serialization including:
 * - DidOpenTextDocumentParams, DidCloseTextDocumentParams, DidChangeTextDocumentParams
 * - DidSaveTextDocumentParams, WillSaveTextDocumentParams
 * - TextDocumentSaveReason
 * - TextDocumentContentChangeEvent
 */
class TextDocumentSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== TextDocumentSaveReason Tests ====================

    @Test
    fun `TextDocumentSaveReason Manual serialization`() {
        val reason = TextDocumentSaveReason.Manual
        val encoded = json.encodeToString(reason)
        encoded shouldBe "1"
    }

    @Test
    fun `TextDocumentSaveReason AfterDelay serialization`() {
        val reason = TextDocumentSaveReason.AfterDelay
        val encoded = json.encodeToString(reason)
        encoded shouldBe "2"
    }

    @Test
    fun `TextDocumentSaveReason FocusOut serialization`() {
        val reason = TextDocumentSaveReason.FocusOut
        val encoded = json.encodeToString(reason)
        encoded shouldBe "3"
    }

    @Test
    fun `TextDocumentSaveReason deserialization`() {
        json.decodeFromString<TextDocumentSaveReason>("1") shouldBe TextDocumentSaveReason.Manual
        json.decodeFromString<TextDocumentSaveReason>("2") shouldBe TextDocumentSaveReason.AfterDelay
        json.decodeFromString<TextDocumentSaveReason>("3") shouldBe TextDocumentSaveReason.FocusOut
    }

    @Test
    fun `TextDocumentSaveReason roundtrip for all values`() {
        TextDocumentSaveReason.entries.forEach { reason ->
            val encoded = json.encodeToString(reason)
            val decoded = json.decodeFromString<TextDocumentSaveReason>(encoded)
            decoded shouldBe reason
        }
    }

    @Test
    fun `TextDocumentSaveReason fromValue throws on invalid value`() {
        assertFailsWith<IllegalArgumentException> {
            TextDocumentSaveReason.fromValue(999)
        }.message shouldContain "Unknown TextDocumentSaveReason"
    }

    @Test
    fun `TextDocumentSaveReason fromValue throws on zero`() {
        assertFailsWith<IllegalArgumentException> {
            TextDocumentSaveReason.fromValue(0)
        }
    }

    @Test
    fun `TextDocumentSaveReason fromValue throws on negative`() {
        assertFailsWith<IllegalArgumentException> {
            TextDocumentSaveReason.fromValue(-1)
        }
    }

    // ==================== DidOpenTextDocumentParams Tests ====================

    @Test
    fun `DidOpenTextDocumentParams basic serialization`() {
        val params =
            DidOpenTextDocumentParams(
                textDocument =
                    TextDocumentItem(
                        uri = "file:///test.kt",
                        languageId = "kotlin",
                        version = 1,
                        text = "fun main() {}",
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidOpenTextDocumentParams>(encoded)
        decoded shouldBe params
    }

    @Test
    fun `DidOpenTextDocumentParams with empty text`() {
        val params =
            DidOpenTextDocumentParams(
                textDocument =
                    TextDocumentItem(
                        uri = "file:///empty.kt",
                        languageId = "kotlin",
                        version = 1,
                        text = "",
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidOpenTextDocumentParams>(encoded)
        decoded.textDocument.text shouldBe ""
    }

    @Test
    fun `DidOpenTextDocumentParams with large text`() {
        val largeText = "x".repeat(100_000)
        val params =
            DidOpenTextDocumentParams(
                textDocument =
                    TextDocumentItem(
                        uri = "file:///large.kt",
                        languageId = "kotlin",
                        version = 1,
                        text = largeText,
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidOpenTextDocumentParams>(encoded)
        decoded.textDocument.text.length shouldBe 100_000
    }

    @Test
    fun `DidOpenTextDocumentParams with unicode text`() {
        val params =
            DidOpenTextDocumentParams(
                textDocument =
                    TextDocumentItem(
                        uri = "file:///unicode.kt",
                        languageId = "kotlin",
                        version = 1,
                        text = "fun hello() = \"Hello, 世界! 🌍\"",
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidOpenTextDocumentParams>(encoded)
        decoded.textDocument.text shouldContain "世界"
        decoded.textDocument.text shouldContain "🌍"
    }

    @Test
    fun `DidOpenTextDocumentParams with special characters`() {
        val params =
            DidOpenTextDocumentParams(
                textDocument =
                    TextDocumentItem(
                        uri = "file:///special.kt",
                        languageId = "kotlin",
                        version = 1,
                        text = "val text = \"Contains \\\"quotes\\\" and \\\\backslashes\\\\ and\\nnewlines\"",
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidOpenTextDocumentParams>(encoded)
        decoded shouldBe params
    }

    @Test
    fun `DidOpenTextDocumentParams with various language IDs`() {
        val languageIds = listOf("kotlin", "java", "python", "typescript", "rust", "go")
        languageIds.forEach { langId ->
            val params =
                DidOpenTextDocumentParams(
                    textDocument =
                        TextDocumentItem(
                            uri = "file:///test.$langId",
                            languageId = langId,
                            version = 1,
                            text = "// comment",
                        ),
                )
            val encoded = json.encodeToString(params)
            val decoded = json.decodeFromString<DidOpenTextDocumentParams>(encoded)
            decoded.textDocument.languageId shouldBe langId
        }
    }

    // ==================== DidCloseTextDocumentParams Tests ====================

    @Test
    fun `DidCloseTextDocumentParams serialization`() {
        val params =
            DidCloseTextDocumentParams(
                textDocument = TextDocumentIdentifier(uri = "file:///closed.kt"),
            )
        val encoded = json.encodeToString(params)
        encoded shouldBe """{"textDocument":{"uri":"file:///closed.kt"}}"""
    }

    @Test
    fun `DidCloseTextDocumentParams roundtrip`() {
        val params =
            DidCloseTextDocumentParams(
                textDocument = TextDocumentIdentifier(uri = "file:///path/to/File.kt"),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidCloseTextDocumentParams>(encoded)
        decoded shouldBe params
    }

    // ==================== DidChangeTextDocumentParams Tests ====================

    @Test
    fun `DidChangeTextDocumentParams with full document change`() {
        val params =
            DidChangeTextDocumentParams(
                textDocument =
                    VersionedTextDocumentIdentifier(
                        uri = "file:///test.kt",
                        version = 2,
                    ),
                contentChanges =
                    listOf(
                        TextDocumentContentChangeEvent(
                            text = "new content",
                        ),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidChangeTextDocumentParams>(encoded)
        decoded shouldBe params
        decoded.contentChanges.first().range shouldBe null
    }

    @Test
    fun `DidChangeTextDocumentParams with incremental change`() {
        val params =
            DidChangeTextDocumentParams(
                textDocument =
                    VersionedTextDocumentIdentifier(
                        uri = "file:///test.kt",
                        version = 3,
                    ),
                contentChanges =
                    listOf(
                        TextDocumentContentChangeEvent(
                            range = Range(Position(5, 0), Position(5, 10)),
                            text = "replacement",
                        ),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidChangeTextDocumentParams>(encoded)
        decoded.contentChanges.first().range shouldBe Range(Position(5, 0), Position(5, 10))
    }

    @Test
    fun `DidChangeTextDocumentParams with multiple changes`() {
        val params =
            DidChangeTextDocumentParams(
                textDocument =
                    VersionedTextDocumentIdentifier(
                        uri = "file:///test.kt",
                        version = 5,
                    ),
                contentChanges =
                    listOf(
                        TextDocumentContentChangeEvent(
                            range = Range(Position(1, 0), Position(1, 5)),
                            text = "first",
                        ),
                        TextDocumentContentChangeEvent(
                            range = Range(Position(10, 0), Position(10, 20)),
                            text = "second",
                        ),
                        TextDocumentContentChangeEvent(
                            range = Range(Position(20, 5), Position(25, 0)),
                            text = "third",
                        ),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidChangeTextDocumentParams>(encoded)
        decoded.contentChanges.size shouldBe 3
    }

    @Test
    fun `DidChangeTextDocumentParams with rangeLength (deprecated)`() {
        val params =
            DidChangeTextDocumentParams(
                textDocument =
                    VersionedTextDocumentIdentifier(
                        uri = "file:///test.kt",
                        version = 2,
                    ),
                contentChanges =
                    listOf(
                        TextDocumentContentChangeEvent(
                            range = Range(Position(0, 0), Position(0, 10)),
                            rangeLength = 10,
                            text = "new text",
                        ),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidChangeTextDocumentParams>(encoded)
        decoded.contentChanges.first().rangeLength shouldBe 10
    }

    @Test
    fun `DidChangeTextDocumentParams with empty changes list`() {
        val params =
            DidChangeTextDocumentParams(
                textDocument =
                    VersionedTextDocumentIdentifier(
                        uri = "file:///test.kt",
                        version = 1,
                    ),
                contentChanges = emptyList(),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidChangeTextDocumentParams>(encoded)
        decoded.contentChanges shouldBe emptyList()
    }

    // ==================== DidSaveTextDocumentParams Tests ====================

    @Test
    fun `DidSaveTextDocumentParams without text`() {
        val params =
            DidSaveTextDocumentParams(
                textDocument = TextDocumentIdentifier(uri = "file:///saved.kt"),
            )
        val encoded = json.encodeToString(params)
        // Check that there's no "text" key at the top level (not inside textDocument)
        encoded.contains("\"text\":") shouldBe false

        val decoded = json.decodeFromString<DidSaveTextDocumentParams>(encoded)
        decoded.text shouldBe null
    }

    @Test
    fun `DidSaveTextDocumentParams with text`() {
        val params =
            DidSaveTextDocumentParams(
                textDocument = TextDocumentIdentifier(uri = "file:///saved.kt"),
                text = "saved content",
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidSaveTextDocumentParams>(encoded)
        decoded.text shouldBe "saved content"
    }

    @Test
    fun `DidSaveTextDocumentParams with empty text`() {
        val params =
            DidSaveTextDocumentParams(
                textDocument = TextDocumentIdentifier(uri = "file:///empty.kt"),
                text = "",
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidSaveTextDocumentParams>(encoded)
        decoded.text shouldBe ""
    }

    // ==================== WillSaveTextDocumentParams Tests ====================

    @Test
    fun `WillSaveTextDocumentParams with Manual reason`() {
        val params =
            WillSaveTextDocumentParams(
                textDocument = TextDocumentIdentifier(uri = "file:///will-save.kt"),
                reason = TextDocumentSaveReason.Manual,
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"reason\":1"

        val decoded = json.decodeFromString<WillSaveTextDocumentParams>(encoded)
        decoded.reason shouldBe TextDocumentSaveReason.Manual
    }

    @Test
    fun `WillSaveTextDocumentParams with AfterDelay reason`() {
        val params =
            WillSaveTextDocumentParams(
                textDocument = TextDocumentIdentifier(uri = "file:///auto-save.kt"),
                reason = TextDocumentSaveReason.AfterDelay,
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<WillSaveTextDocumentParams>(encoded)
        decoded.reason shouldBe TextDocumentSaveReason.AfterDelay
    }

    @Test
    fun `WillSaveTextDocumentParams with FocusOut reason`() {
        val params =
            WillSaveTextDocumentParams(
                textDocument = TextDocumentIdentifier(uri = "file:///focus-out.kt"),
                reason = TextDocumentSaveReason.FocusOut,
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<WillSaveTextDocumentParams>(encoded)
        decoded.reason shouldBe TextDocumentSaveReason.FocusOut
    }

    @Test
    fun `WillSaveTextDocumentParams roundtrip for all reasons`() {
        TextDocumentSaveReason.entries.forEach { reason ->
            val params =
                WillSaveTextDocumentParams(
                    textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                    reason = reason,
                )
            val encoded = json.encodeToString(params)
            val decoded = json.decodeFromString<WillSaveTextDocumentParams>(encoded)
            decoded.reason shouldBe reason
        }
    }

    // ==================== TextDocumentContentChangeEvent Tests ====================

    @Test
    fun `TextDocumentContentChangeEvent full document change`() {
        val event = TextDocumentContentChangeEvent(text = "full document content")
        val encoded = json.encodeToString(event)
        val decoded = json.decodeFromString<TextDocumentContentChangeEvent>(encoded)
        decoded.text shouldBe "full document content"
        decoded.range shouldBe null
        decoded.rangeLength shouldBe null
    }

    @Test
    fun `TextDocumentContentChangeEvent incremental change`() {
        val event =
            TextDocumentContentChangeEvent(
                range = Range(Position(10, 5), Position(10, 15)),
                text = "replacement",
            )
        val encoded = json.encodeToString(event)
        val decoded = json.decodeFromString<TextDocumentContentChangeEvent>(encoded)
        decoded.range shouldBe Range(Position(10, 5), Position(10, 15))
    }

    @Test
    fun `TextDocumentContentChangeEvent with zero-width range (insertion)`() {
        val event =
            TextDocumentContentChangeEvent(
                range = Range(Position(5, 10), Position(5, 10)),
                text = "inserted text",
            )
        val encoded = json.encodeToString(event)
        val decoded = json.decodeFromString<TextDocumentContentChangeEvent>(encoded)
        decoded.range?.start shouldBe decoded.range?.end
    }

    @Test
    fun `TextDocumentContentChangeEvent deletion (empty replacement)`() {
        val event =
            TextDocumentContentChangeEvent(
                range = Range(Position(0, 0), Position(5, 0)),
                text = "",
            )
        val encoded = json.encodeToString(event)
        val decoded = json.decodeFromString<TextDocumentContentChangeEvent>(encoded)
        decoded.text shouldBe ""
    }

    // ==================== Edge Cases ====================

    @Test
    fun `TextDocument types handle very long URIs`() {
        val longPath = "a".repeat(1000)
        val params =
            DidOpenTextDocumentParams(
                textDocument =
                    TextDocumentItem(
                        uri = "file:///$longPath/file.kt",
                        languageId = "kotlin",
                        version = 1,
                        text = "content",
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidOpenTextDocumentParams>(encoded)
        decoded.textDocument.uri.length shouldBe "file:///$longPath/file.kt".length
    }

    @Test
    fun `TextDocument types handle special URI characters`() {
        val params =
            DidOpenTextDocumentParams(
                textDocument =
                    TextDocumentItem(
                        uri = "file:///path%20with%20spaces/file%2B%2B.kt",
                        languageId = "kotlin",
                        version = 1,
                        text = "content",
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidOpenTextDocumentParams>(encoded)
        decoded.textDocument.uri shouldContain "spaces"
    }

    @Test
    fun `TextDocumentItem with version zero`() {
        val item =
            TextDocumentItem(
                uri = "file:///test.kt",
                languageId = "kotlin",
                version = 0,
                text = "",
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<TextDocumentItem>(encoded)
        decoded.version shouldBe 0
    }

    @Test
    fun `TextDocumentItem with max version`() {
        val item =
            TextDocumentItem(
                uri = "file:///test.kt",
                languageId = "kotlin",
                version = Int.MAX_VALUE,
                text = "",
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<TextDocumentItem>(encoded)
        decoded.version shouldBe Int.MAX_VALUE
    }

    @Test
    fun `VersionedTextDocumentIdentifier serialization`() {
        val doc =
            VersionedTextDocumentIdentifier(
                uri = "file:///versioned.kt",
                version = 42,
            )
        val encoded = json.encodeToString(doc)
        val decoded = json.decodeFromString<VersionedTextDocumentIdentifier>(encoded)
        decoded shouldBe doc
    }
}
