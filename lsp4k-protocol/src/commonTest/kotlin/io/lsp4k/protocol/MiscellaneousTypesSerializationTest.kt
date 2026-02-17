package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Tests for miscellaneous types that haven't been covered in other test files:
 * - Rename types (RenameParams, PrepareRenameParams, PrepareRenameResult)
 * - DocumentHighlight types (DocumentHighlightKind, DocumentHighlightParams, DocumentHighlight)
 * - SelectionRange types
 * - LinkedEditingRange types
 * - DocumentLink types
 * - CodeLens types
 * - ExecuteCommand types
 */
class MiscellaneousTypesSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== RenameParams Tests ====================

    @Test
    fun `RenameParams serialization`() {
        val params = RenameParams(
            textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
            position = Position(line = 10, character = 5),
            newName = "newVariableName",
        )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"newName\":\"newVariableName\""
    }

    @Test
    fun `RenameParams round-trip`() {
        val params = RenameParams(
            textDocument = TextDocumentIdentifier(uri = "file:///code.kt"),
            position = Position(line = 25, character = 8),
            newName = "renamedFunction",
        )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<RenameParams>(encoded)
        decoded shouldBe params
    }

    @Test
    fun `RenameParams with special characters in newName`() {
        val params = RenameParams(
            textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
            position = Position(line = 5, character = 0),
            newName = "my_variable_name_123",
        )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<RenameParams>(encoded)
        decoded.newName shouldBe "my_variable_name_123"
    }

    // ==================== PrepareRenameParams Tests ====================

    @Test
    fun `PrepareRenameParams serialization`() {
        val params = PrepareRenameParams(
            textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
            position = Position(line = 15, character = 10),
        )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///test.kt\""
    }

    @Test
    fun `PrepareRenameParams round-trip`() {
        val params = PrepareRenameParams(
            textDocument = TextDocumentIdentifier(uri = "file:///src/Main.kt"),
            position = Position(line = 100, character = 20),
        )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<PrepareRenameParams>(encoded)
        decoded shouldBe params
    }

    // ==================== PrepareRenameResult Tests ====================

    @Test
    fun `PrepareRenameResult serialization`() {
        val result = PrepareRenameResult(
            range = Range(Position(10, 4), Position(10, 15)),
            placeholder = "oldName",
        )
        val encoded = json.encodeToString(result)
        encoded shouldContain "\"placeholder\":\"oldName\""
    }

    @Test
    fun `PrepareRenameResult round-trip`() {
        val result = PrepareRenameResult(
            range = Range(Position(50, 8), Position(50, 20)),
            placeholder = "functionToRename",
        )
        val encoded = json.encodeToString(result)
        val decoded = json.decodeFromString<PrepareRenameResult>(encoded)
        decoded shouldBe result
    }

    // ==================== DocumentHighlightKind Tests ====================

    @Test
    fun `DocumentHighlightKind Text serializes to 1`() {
        val encoded = json.encodeToString(DocumentHighlightKind.Text)
        encoded shouldBe "1"
    }

    @Test
    fun `DocumentHighlightKind Read serializes to 2`() {
        val encoded = json.encodeToString(DocumentHighlightKind.Read)
        encoded shouldBe "2"
    }

    @Test
    fun `DocumentHighlightKind Write serializes to 3`() {
        val encoded = json.encodeToString(DocumentHighlightKind.Write)
        encoded shouldBe "3"
    }

    @Test
    fun `DocumentHighlightKind deserializes from integers`() {
        json.decodeFromString<DocumentHighlightKind>("1") shouldBe DocumentHighlightKind.Text
        json.decodeFromString<DocumentHighlightKind>("2") shouldBe DocumentHighlightKind.Read
        json.decodeFromString<DocumentHighlightKind>("3") shouldBe DocumentHighlightKind.Write
    }

    @Test
    fun `DocumentHighlightKind fromValue throws for unknown value`() {
        assertFailsWith<IllegalArgumentException> {
            DocumentHighlightKind.fromValue(99)
        }
    }

    // ==================== DocumentHighlightParams Tests ====================

    @Test
    fun `DocumentHighlightParams serialization`() {
        val params = DocumentHighlightParams(
            textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
            position = Position(line = 20, character = 15),
        )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///test.kt\""
    }

    @Test
    fun `DocumentHighlightParams round-trip`() {
        val params = DocumentHighlightParams(
            textDocument = TextDocumentIdentifier(uri = "file:///code.kt"),
            position = Position(line = 5, character = 8),
        )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DocumentHighlightParams>(encoded)
        decoded shouldBe params
    }

    // ==================== DocumentHighlight Tests ====================

    @Test
    fun `DocumentHighlight minimal`() {
        val highlight = DocumentHighlight(
            range = Range(Position(10, 4), Position(10, 12)),
        )
        val encoded = json.encodeToString(highlight)
        encoded shouldContain "\"range\""
        val decoded = json.decodeFromString<DocumentHighlight>(encoded)
        decoded.kind shouldBe null
    }

    @Test
    fun `DocumentHighlight with Text kind`() {
        val highlight = DocumentHighlight(
            range = Range(Position(15, 0), Position(15, 10)),
            kind = DocumentHighlightKind.Text,
        )
        val encoded = json.encodeToString(highlight)
        encoded shouldContain "\"kind\":1"
    }

    @Test
    fun `DocumentHighlight with Read kind`() {
        val highlight = DocumentHighlight(
            range = Range(Position(20, 8), Position(20, 15)),
            kind = DocumentHighlightKind.Read,
        )
        val encoded = json.encodeToString(highlight)
        val decoded = json.decodeFromString<DocumentHighlight>(encoded)
        decoded.kind shouldBe DocumentHighlightKind.Read
    }

    @Test
    fun `DocumentHighlight with Write kind`() {
        val highlight = DocumentHighlight(
            range = Range(Position(25, 4), Position(25, 12)),
            kind = DocumentHighlightKind.Write,
        )
        val encoded = json.encodeToString(highlight)
        val decoded = json.decodeFromString<DocumentHighlight>(encoded)
        decoded.kind shouldBe DocumentHighlightKind.Write
    }

    @Test
    fun `DocumentHighlight list`() {
        val highlights = listOf(
            DocumentHighlight(Range(Position(5, 0), Position(5, 8)), DocumentHighlightKind.Write),
            DocumentHighlight(Range(Position(10, 4), Position(10, 12)), DocumentHighlightKind.Read),
            DocumentHighlight(Range(Position(15, 8), Position(15, 16)), DocumentHighlightKind.Read),
        )
        val encoded = json.encodeToString(highlights)
        val decoded = json.decodeFromString<List<DocumentHighlight>>(encoded)
        decoded.size shouldBe 3
    }

    // ==================== SelectionRange Tests ====================

    @Test
    fun `SelectionRange minimal`() {
        val range = SelectionRange(
            range = Range(Position(10, 5), Position(10, 15)),
        )
        val encoded = json.encodeToString(range)
        encoded shouldContain "\"range\""
        val decoded = json.decodeFromString<SelectionRange>(encoded)
        decoded.parent shouldBe null
    }

    @Test
    fun `SelectionRange with parent`() {
        val parent = SelectionRange(
            range = Range(Position(5, 0), Position(20, 0)),
        )
        val child = SelectionRange(
            range = Range(Position(10, 4), Position(10, 20)),
            parent = parent,
        )
        val encoded = json.encodeToString(child)
        val decoded = json.decodeFromString<SelectionRange>(encoded)
        decoded.parent?.range?.start?.line shouldBe 5
    }

    @Test
    fun `SelectionRangeParams serialization`() {
        val params = SelectionRangeParams(
            textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
            positions = listOf(
                Position(10, 5),
                Position(20, 8),
            ),
        )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<SelectionRangeParams>(encoded)
        decoded.positions.size shouldBe 2
    }

    // ==================== LinkedEditingRange Tests ====================

    @Test
    fun `LinkedEditingRangeParams serialization`() {
        val params = LinkedEditingRangeParams(
            textDocument = TextDocumentIdentifier(uri = "file:///test.html"),
            position = Position(line = 10, character = 5),
        )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///test.html\""
    }

    @Test
    fun `LinkedEditingRanges serialization`() {
        val ranges = LinkedEditingRanges(
            ranges = listOf(
                Range(Position(5, 1), Position(5, 5)),
                Range(Position(10, 2), Position(10, 6)),
            ),
        )
        val encoded = json.encodeToString(ranges)
        val decoded = json.decodeFromString<LinkedEditingRanges>(encoded)
        decoded.ranges.size shouldBe 2
    }

    @Test
    fun `LinkedEditingRanges with wordPattern`() {
        val ranges = LinkedEditingRanges(
            ranges = listOf(
                Range(Position(5, 0), Position(5, 10)),
            ),
            wordPattern = "[a-zA-Z_][a-zA-Z0-9_]*",
        )
        val encoded = json.encodeToString(ranges)
        val decoded = json.decodeFromString<LinkedEditingRanges>(encoded)
        decoded.wordPattern shouldBe "[a-zA-Z_][a-zA-Z0-9_]*"
    }

    // ==================== DocumentLink Tests ====================

    @Test
    fun `DocumentLink minimal`() {
        val link = DocumentLink(
            range = Range(Position(5, 10), Position(5, 30)),
        )
        val encoded = json.encodeToString(link)
        encoded shouldContain "\"range\""
        val decoded = json.decodeFromString<DocumentLink>(encoded)
        decoded.target shouldBe null
        decoded.tooltip shouldBe null
    }

    @Test
    fun `DocumentLink with target`() {
        val link = DocumentLink(
            range = Range(Position(10, 5), Position(10, 25)),
            target = "https://kotlinlang.org/docs/",
        )
        val encoded = json.encodeToString(link)
        val decoded = json.decodeFromString<DocumentLink>(encoded)
        decoded.target shouldBe "https://kotlinlang.org/docs/"
    }

    @Test
    fun `DocumentLink full`() {
        val link = DocumentLink(
            range = Range(Position(15, 0), Position(15, 40)),
            target = "file:///docs/README.md",
            tooltip = "Open documentation",
            data = JsonPrimitive("link-data"),
        )
        val encoded = json.encodeToString(link)
        val decoded = json.decodeFromString<DocumentLink>(encoded)
        decoded.tooltip shouldBe "Open documentation"
        decoded.data shouldBe JsonPrimitive("link-data")
    }

    // ==================== CodeLens Tests ====================

    @Test
    fun `CodeLens minimal`() {
        val lens = CodeLens(
            range = Range(Position(10, 0), Position(10, 20)),
        )
        val encoded = json.encodeToString(lens)
        encoded shouldContain "\"range\""
        val decoded = json.decodeFromString<CodeLens>(encoded)
        decoded.command shouldBe null
        decoded.data shouldBe null
    }

    @Test
    fun `CodeLens with command`() {
        val lens = CodeLens(
            range = Range(Position(5, 0), Position(5, 15)),
            command = Command(
                title = "Run Tests",
                command = "test.run",
            ),
        )
        val encoded = json.encodeToString(lens)
        val decoded = json.decodeFromString<CodeLens>(encoded)
        decoded.command?.title shouldBe "Run Tests"
    }

    @Test
    fun `CodeLens full`() {
        val lens = CodeLens(
            range = Range(Position(20, 4), Position(20, 25)),
            command = Command(
                title = "5 references",
                command = "editor.action.showReferences",
                arguments = listOf(JsonPrimitive("file:///test.kt"), JsonPrimitive(20)),
            ),
            data = JsonPrimitive("lens-resolve-data"),
        )
        val encoded = json.encodeToString(lens)
        val decoded = json.decodeFromString<CodeLens>(encoded)
        decoded shouldBe lens
    }

    // ==================== ExecuteCommandParams Tests ====================

    @Test
    fun `ExecuteCommandParams minimal`() {
        val params = ExecuteCommandParams(
            command = "myCommand",
        )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"command\":\"myCommand\""
    }

    @Test
    fun `ExecuteCommandParams with arguments`() {
        val params = ExecuteCommandParams(
            command = "refactor.extract",
            arguments = listOf(
                JsonPrimitive("file:///test.kt"),
                JsonPrimitive(10),
                JsonPrimitive(20),
            ),
        )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<ExecuteCommandParams>(encoded)
        decoded.command shouldBe "refactor.extract"
        decoded.arguments?.size shouldBe 3
    }
}
