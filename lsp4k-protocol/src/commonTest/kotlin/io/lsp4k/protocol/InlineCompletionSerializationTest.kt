package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for InlineCompletion type serialization: InlineCompletionTriggerKind,
 * InlineCompletionContext, InlineCompletionParams, InlineCompletionItem,
 * InlineCompletionList, SelectedCompletionInfo.
 */
class InlineCompletionSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== InlineCompletionTriggerKind Tests ====================

    @Test
    fun `InlineCompletionTriggerKind Invoked serialization`() {
        val kind = InlineCompletionTriggerKind.Invoked
        val encoded = json.encodeToString(kind)
        encoded shouldBe "0"
    }

    @Test
    fun `InlineCompletionTriggerKind Automatic serialization`() {
        val kind = InlineCompletionTriggerKind.Automatic
        val encoded = json.encodeToString(kind)
        encoded shouldBe "1"
    }

    @Test
    fun `InlineCompletionTriggerKind roundtrip for all values`() {
        InlineCompletionTriggerKind.entries.forEach { kind ->
            val encoded = json.encodeToString(kind)
            val decoded = json.decodeFromString<InlineCompletionTriggerKind>(encoded)
            decoded shouldBe kind
        }
    }

    // ==================== SelectedCompletionInfo Tests ====================

    @Test
    fun `SelectedCompletionInfo serialization roundtrip`() {
        val original = SelectedCompletionInfo(
            range = Range(Position(1, 0), Position(1, 5)),
            text = "hello",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SelectedCompletionInfo>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `SelectedCompletionInfo with empty text`() {
        val original = SelectedCompletionInfo(
            range = Range(Position(0, 0), Position(0, 0)),
            text = "",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SelectedCompletionInfo>(encoded)
        decoded shouldBe original
    }

    // ==================== InlineCompletionContext Tests ====================

    @Test
    fun `InlineCompletionContext with triggerKind only`() {
        val original = InlineCompletionContext(
            triggerKind = InlineCompletionTriggerKind.Invoked,
        )
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "selectedCompletionInfo"
        val decoded = json.decodeFromString<InlineCompletionContext>(encoded)
        decoded shouldBe original
        decoded.selectedCompletionInfo shouldBe null
    }

    @Test
    fun `InlineCompletionContext with selectedCompletionInfo`() {
        val original = InlineCompletionContext(
            triggerKind = InlineCompletionTriggerKind.Automatic,
            selectedCompletionInfo = SelectedCompletionInfo(
                range = Range(Position(5, 10), Position(5, 15)),
                text = "myVar",
            ),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineCompletionContext>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `InlineCompletionContext roundtrip`() {
        val original = InlineCompletionContext(
            triggerKind = InlineCompletionTriggerKind.Invoked,
            selectedCompletionInfo = null,
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineCompletionContext>(encoded)
        decoded shouldBe original
    }

    // ==================== InlineCompletionParams Tests ====================

    @Test
    fun `InlineCompletionParams serialization roundtrip`() {
        val original = InlineCompletionParams(
            textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
            position = Position(10, 5),
            context = InlineCompletionContext(
                triggerKind = InlineCompletionTriggerKind.Invoked,
            ),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineCompletionParams>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `InlineCompletionParams with full context`() {
        val original = InlineCompletionParams(
            textDocument = TextDocumentIdentifier(uri = "file:///src/main.kt"),
            position = Position(20, 15),
            context = InlineCompletionContext(
                triggerKind = InlineCompletionTriggerKind.Automatic,
                selectedCompletionInfo = SelectedCompletionInfo(
                    range = Range(Position(20, 10), Position(20, 15)),
                    text = "print",
                ),
            ),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineCompletionParams>(encoded)
        decoded shouldBe original
    }

    // ==================== InlineCompletionItem Tests ====================

    @Test
    fun `InlineCompletionItem with plain string insertText`() {
        val original = InlineCompletionItem(
            insertText = Either.Left("hello world"),
        )
        val encoded = json.encodeToString(original)
        encoded shouldContain "\"hello world\""
        val decoded = json.decodeFromString<InlineCompletionItem>(encoded)
        decoded.insertText.left shouldBe "hello world"
    }

    @Test
    fun `InlineCompletionItem with StringValue insertText`() {
        val original = InlineCompletionItem(
            insertText = Either.Right(StringValue(kind = "snippet", value = "println(\$1)")),
        )
        val encoded = json.encodeToString(original)
        encoded shouldContain "println"
        val decoded = json.decodeFromString<InlineCompletionItem>(encoded)
        decoded.insertText.right?.value shouldBe "println(\$1)"
        // kind defaults to "snippet", so it may not appear in JSON when encodeDefaults=false
        decoded.insertText.right?.kind shouldBe "snippet"
    }

    @Test
    fun `InlineCompletionItem with all optional fields`() {
        val original = InlineCompletionItem(
            insertText = Either.Left("completedText"),
            filterText = "comp",
            range = Range(Position(1, 0), Position(1, 4)),
            command = Command(title = "Apply", command = "editor.apply"),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineCompletionItem>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `InlineCompletionItem with null optionals`() {
        val original = InlineCompletionItem(
            insertText = Either.Left("text"),
        )
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "filterText"
        encoded shouldNotContain "range"
        encoded shouldNotContain "command"
        val decoded = json.decodeFromString<InlineCompletionItem>(encoded)
        decoded.filterText shouldBe null
        decoded.range shouldBe null
        decoded.command shouldBe null
    }

    // ==================== InlineCompletionList Tests ====================

    @Test
    fun `InlineCompletionList serialization roundtrip`() {
        val original = InlineCompletionList(
            items = listOf(
                InlineCompletionItem(insertText = Either.Left("item1")),
                InlineCompletionItem(insertText = Either.Left("item2")),
            ),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineCompletionList>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `InlineCompletionList empty items`() {
        val original = InlineCompletionList(items = emptyList())
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineCompletionList>(encoded)
        decoded.items shouldBe emptyList()
    }

    @Test
    fun `InlineCompletionList with mixed item types`() {
        val original = InlineCompletionList(
            items = listOf(
                InlineCompletionItem(
                    insertText = Either.Left("plain text"),
                ),
                InlineCompletionItem(
                    insertText = Either.Right(StringValue(value = "snippet(\$0)")),
                    filterText = "snippet",
                    range = Range(Position(0, 0), Position(0, 7)),
                ),
            ),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<InlineCompletionList>(encoded)
        decoded.items.size shouldBe 2
        decoded.items[0].insertText.isLeft shouldBe true
        decoded.items[1].insertText.isRight shouldBe true
    }
}
