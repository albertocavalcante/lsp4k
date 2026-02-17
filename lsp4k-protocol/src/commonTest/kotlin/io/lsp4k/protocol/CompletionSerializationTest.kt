package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/**
 * Tests for Completion type serialization: CompletionItem, CompletionList, CompletionItemKind, etc.
 */
class CompletionSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== CompletionItemKind Tests ====================

    @Test
    fun `CompletionItemKind Text serialization`() {
        val kind = CompletionItemKind.Text
        val encoded = json.encodeToString(kind)
        encoded shouldBe "1"
    }

    @Test
    fun `CompletionItemKind Method serialization`() {
        val kind = CompletionItemKind.Method
        val encoded = json.encodeToString(kind)
        encoded shouldBe "2"
    }

    @Test
    fun `CompletionItemKind Function serialization`() {
        val kind = CompletionItemKind.Function
        val encoded = json.encodeToString(kind)
        encoded shouldBe "3"
    }

    @Test
    fun `CompletionItemKind Class serialization`() {
        val kind = CompletionItemKind.Class
        val encoded = json.encodeToString(kind)
        encoded shouldBe "7"
    }

    @Test
    fun `CompletionItemKind roundtrip for all values`() {
        CompletionItemKind.entries.forEach { kind ->
            val encoded = json.encodeToString(kind)
            val decoded = json.decodeFromString<CompletionItemKind>(encoded)
            decoded shouldBe kind
        }
    }

    // ==================== CompletionItemTag Tests ====================

    @Test
    fun `CompletionItemTag Deprecated serialization`() {
        val tag = CompletionItemTag.Deprecated
        val encoded = json.encodeToString(tag)
        encoded shouldBe "1"
    }

    // ==================== InsertTextFormat Tests ====================

    @Test
    fun `InsertTextFormat PlainText serialization`() {
        val format = InsertTextFormat.PlainText
        val encoded = json.encodeToString(format)
        encoded shouldBe "1"
    }

    @Test
    fun `InsertTextFormat Snippet serialization`() {
        val format = InsertTextFormat.Snippet
        val encoded = json.encodeToString(format)
        encoded shouldBe "2"
    }

    @Test
    fun `InsertTextFormat roundtrip`() {
        InsertTextFormat.entries.forEach { format ->
            val encoded = json.encodeToString(format)
            val decoded = json.decodeFromString<InsertTextFormat>(encoded)
            decoded shouldBe format
        }
    }

    // ==================== InsertTextMode Tests ====================

    @Test
    fun `InsertTextMode AsIs serialization`() {
        val mode = InsertTextMode.AsIs
        val encoded = json.encodeToString(mode)
        encoded shouldBe "1"
    }

    @Test
    fun `InsertTextMode AdjustIndentation serialization`() {
        val mode = InsertTextMode.AdjustIndentation
        val encoded = json.encodeToString(mode)
        encoded shouldBe "2"
    }

    // ==================== CompletionItemLabelDetails Tests ====================

    @Test
    fun `CompletionItemLabelDetails with all fields`() {
        val details =
            CompletionItemLabelDetails(
                detail = "(x: Int, y: Int)",
                description = "Point",
            )
        val encoded = json.encodeToString(details)
        val decoded = json.decodeFromString<CompletionItemLabelDetails>(encoded)
        decoded shouldBe details
    }

    @Test
    fun `CompletionItemLabelDetails with only detail`() {
        val details = CompletionItemLabelDetails(detail = "()")
        val encoded = json.encodeToString(details)
        encoded.contains("description") shouldBe false

        val decoded = json.decodeFromString<CompletionItemLabelDetails>(encoded)
        decoded.detail shouldBe "()"
        decoded.description shouldBe null
    }

    @Test
    fun `CompletionItemLabelDetails empty (no optional fields)`() {
        val details = CompletionItemLabelDetails()
        val encoded = json.encodeToString(details)
        encoded shouldBe "{}"
    }

    // ==================== CompletionItem Tests ====================

    @Test
    fun `CompletionItem minimal (label only)`() {
        val item = CompletionItem(label = "myFunction")
        val encoded = json.encodeToString(item)
        encoded shouldBe """{"label":"myFunction"}"""

        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded shouldBe item
    }

    @Test
    fun `CompletionItem with kind`() {
        val item =
            CompletionItem(
                label = "MyClass",
                kind = CompletionItemKind.Class,
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.label shouldBe "MyClass"
        decoded.kind shouldBe CompletionItemKind.Class
    }

    @Test
    fun `CompletionItem with label details`() {
        val item =
            CompletionItem(
                label = "calculate",
                labelDetails =
                    CompletionItemLabelDetails(
                        detail = "(x: Int): Int",
                        description = "Calculates result",
                    ),
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.labelDetails?.detail shouldBe "(x: Int): Int"
        decoded.labelDetails?.description shouldBe "Calculates result"
    }

    @Test
    fun `CompletionItem with common fields`() {
        val item =
            CompletionItem(
                label = "myFunction",
                kind = CompletionItemKind.Function,
                detail = "fun myFunction(): Unit",
                documentation = Either.left("This function does something useful"),
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.label shouldBe "myFunction"
        decoded.kind shouldBe CompletionItemKind.Function
        decoded.detail shouldBe "fun myFunction(): Unit"
        decoded.documentation?.left shouldBe "This function does something useful"
    }

    @Test
    fun `CompletionItem with deprecated flag`() {
        val item =
            CompletionItem(
                label = "oldMethod",
                kind = CompletionItemKind.Method,
                deprecated = true,
                tags = listOf(CompletionItemTag.Deprecated),
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.deprecated shouldBe true
        decoded.tags shouldBe listOf(CompletionItemTag.Deprecated)
    }

    @Test
    fun `CompletionItem with preselect`() {
        val item =
            CompletionItem(
                label = "mostLikely",
                preselect = true,
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.preselect shouldBe true
    }

    @Test
    fun `CompletionItem with sortText and filterText`() {
        val item =
            CompletionItem(
                label = "toString",
                sortText = "0000_toString",
                filterText = "tostring",
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.sortText shouldBe "0000_toString"
        decoded.filterText shouldBe "tostring"
    }

    @Test
    fun `CompletionItem with insertText`() {
        val item =
            CompletionItem(
                label = "forLoop",
                insertText = "for (i in 0 until count) {\n    \n}",
                insertTextFormat = InsertTextFormat.PlainText,
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.insertText shouldBe "for (i in 0 until count) {\n    \n}"
        decoded.insertTextFormat shouldBe InsertTextFormat.PlainText
    }

    @Test
    fun `CompletionItem with snippet`() {
        val item =
            CompletionItem(
                label = "if",
                insertText = "if (\${1:condition}) {\n\t\$0\n}",
                insertTextFormat = InsertTextFormat.Snippet,
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.insertTextFormat shouldBe InsertTextFormat.Snippet
    }

    @Test
    fun `CompletionItem with textEdit`() {
        val item =
            CompletionItem(
                label = "completedText",
                textEdit =
                    Either.left(
                        TextEdit(
                            range = Range(Position(10, 5), Position(10, 10)),
                            newText = "completedText",
                        ),
                    ),
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.textEdit?.left?.newText shouldBe "completedText"
    }

    @Test
    fun `CompletionItem with additionalTextEdits`() {
        val item =
            CompletionItem(
                label = "MyClass",
                additionalTextEdits =
                    listOf(
                        TextEdit(
                            range = Range(Position(0, 0), Position(0, 0)),
                            newText = "import com.example.MyClass\n",
                        ),
                    ),
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.additionalTextEdits?.size shouldBe 1
        decoded.additionalTextEdits?.first()?.newText shouldBe "import com.example.MyClass\n"
    }

    @Test
    fun `CompletionItem with commitCharacters`() {
        val item =
            CompletionItem(
                label = "method",
                commitCharacters = listOf(".", "(", ";"),
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.commitCharacters shouldBe listOf(".", "(", ";")
    }

    @Test
    fun `CompletionItem with command`() {
        val item =
            CompletionItem(
                label = "triggerAction",
                command =
                    Command(
                        title = "Trigger Action",
                        command = "editor.action.triggerAction",
                        arguments = listOf(JsonPrimitive("arg1")),
                    ),
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.command?.title shouldBe "Trigger Action"
        decoded.command?.command shouldBe "editor.action.triggerAction"
    }

    @Test
    fun `CompletionItem with data`() {
        val item =
            CompletionItem(
                label = "itemWithData",
                data = JsonPrimitive("resolve-data-123"),
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded.data shouldBe JsonPrimitive("resolve-data-123")
    }

    @Test
    fun `CompletionItem with all optional fields`() {
        val item =
            CompletionItem(
                label = "fullItem",
                labelDetails = CompletionItemLabelDetails(detail = "()", description = "Full"),
                kind = CompletionItemKind.Function,
                tags = listOf(CompletionItemTag.Deprecated),
                detail = "Full function details",
                documentation = Either.left("Full documentation"),
                deprecated = true,
                preselect = true,
                sortText = "0001_fullItem",
                filterText = "fullitem",
                insertText = "fullItem()",
                insertTextFormat = InsertTextFormat.PlainText,
                insertTextMode = InsertTextMode.AdjustIndentation,
                textEdit = Either.left(TextEdit(Range(Position(0, 0), Position(0, 5)), "fullItem()")),
                textEditText = "fullItem()",
                additionalTextEdits = listOf(TextEdit(Range(Position(1, 0), Position(1, 0)), "import")),
                commitCharacters = listOf("."),
                command = Command(title = "Cmd", command = "cmd.run"),
                data = JsonPrimitive(42),
            )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CompletionItem>(encoded)
        decoded shouldBe item
    }

    // ==================== CompletionList Tests ====================

    @Test
    fun `CompletionList with items`() {
        val list =
            CompletionList(
                isIncomplete = false,
                items =
                    listOf(
                        CompletionItem(label = "item1", kind = CompletionItemKind.Function),
                        CompletionItem(label = "item2", kind = CompletionItemKind.Variable),
                    ),
            )
        val encoded = json.encodeToString(list)
        val decoded = json.decodeFromString<CompletionList>(encoded)
        decoded shouldBe list
    }

    @Test
    fun `CompletionList incomplete`() {
        val list =
            CompletionList(
                isIncomplete = true,
                items = listOf(CompletionItem(label = "partial")),
            )
        val encoded = json.encodeToString(list)
        val decoded = json.decodeFromString<CompletionList>(encoded)
        decoded.isIncomplete shouldBe true
    }

    @Test
    fun `CompletionList empty items`() {
        val list =
            CompletionList(
                isIncomplete = false,
                items = emptyList(),
            )
        val encoded = json.encodeToString(list)
        val decoded = json.decodeFromString<CompletionList>(encoded)
        decoded.items shouldBe emptyList()
    }

    @Test
    fun `CompletionList with itemDefaults`() {
        val list =
            CompletionList(
                isIncomplete = false,
                items =
                    listOf(
                        CompletionItem(label = "item1"),
                        CompletionItem(label = "item2"),
                    ),
                itemDefaults =
                    CompletionItemDefaults(
                        commitCharacters = listOf("."),
                        editRange = Either.left(Range(Position(10, 0), Position(10, 5))),
                        insertTextFormat = InsertTextFormat.PlainText,
                        insertTextMode = InsertTextMode.AsIs,
                    ),
            )
        val encoded = json.encodeToString(list)
        val decoded = json.decodeFromString<CompletionList>(encoded)
        decoded.itemDefaults?.commitCharacters shouldBe listOf(".")
        decoded.itemDefaults?.editRange shouldBe Either.left(Range(Position(10, 0), Position(10, 5)))
    }

    // ==================== CompletionItemDefaults Tests ====================

    @Test
    fun `CompletionItemDefaults empty`() {
        val defaults = CompletionItemDefaults()
        val encoded = json.encodeToString(defaults)
        encoded shouldBe "{}"
    }

    @Test
    fun `CompletionItemDefaults with data`() {
        val defaults =
            CompletionItemDefaults(
                data = JsonPrimitive("shared-data"),
            )
        val encoded = json.encodeToString(defaults)
        val decoded = json.decodeFromString<CompletionItemDefaults>(encoded)
        decoded.data shouldBe JsonPrimitive("shared-data")
    }

    // ==================== CompletionParams Tests ====================

    @Test
    fun `CompletionParams minimal`() {
        val params =
            CompletionParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                position = Position(10, 5),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<CompletionParams>(encoded)
        decoded shouldBe params
    }

    @Test
    fun `CompletionParams with context`() {
        val params =
            CompletionParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                position = Position(10, 5),
                context =
                    CompletionContext(
                        triggerKind = CompletionTriggerKind.TriggerCharacter,
                        triggerCharacter = ".",
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<CompletionParams>(encoded)
        decoded.context?.triggerKind shouldBe CompletionTriggerKind.TriggerCharacter
        decoded.context?.triggerCharacter shouldBe "."
    }

    // ==================== CompletionTriggerKind Tests ====================

    @Test
    fun `CompletionTriggerKind Invoked serialization`() {
        val kind = CompletionTriggerKind.Invoked
        val encoded = json.encodeToString(kind)
        encoded shouldBe "1"
    }

    @Test
    fun `CompletionTriggerKind TriggerCharacter serialization`() {
        val kind = CompletionTriggerKind.TriggerCharacter
        val encoded = json.encodeToString(kind)
        encoded shouldBe "2"
    }

    @Test
    fun `CompletionTriggerKind TriggerForIncompleteCompletions serialization`() {
        val kind = CompletionTriggerKind.TriggerForIncompleteCompletions
        val encoded = json.encodeToString(kind)
        encoded shouldBe "3"
    }

    @Test
    fun `CompletionTriggerKind roundtrip for all values`() {
        CompletionTriggerKind.entries.forEach { kind ->
            val encoded = json.encodeToString(kind)
            val decoded = json.decodeFromString<CompletionTriggerKind>(encoded)
            decoded shouldBe kind
        }
    }

    // ==================== CompletionContext Tests ====================

    @Test
    fun `CompletionContext with trigger kind only`() {
        val context = CompletionContext(triggerKind = CompletionTriggerKind.Invoked)
        val encoded = json.encodeToString(context)
        encoded.contains("triggerCharacter") shouldBe false

        val decoded = json.decodeFromString<CompletionContext>(encoded)
        decoded.triggerKind shouldBe CompletionTriggerKind.Invoked
        decoded.triggerCharacter shouldBe null
    }

    @Test
    fun `CompletionContext with trigger character`() {
        val context =
            CompletionContext(
                triggerKind = CompletionTriggerKind.TriggerCharacter,
                triggerCharacter = ":",
            )
        val encoded = json.encodeToString(context)
        val decoded = json.decodeFromString<CompletionContext>(encoded)
        decoded.triggerCharacter shouldBe ":"
    }
}
