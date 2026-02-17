package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/**
 * Tests for additional types to achieve 90%+ coverage:
 * - TypeHierarchy types
 * - ServerOptions (NotebookDocumentSyncOptions, DiagnosticOptions)
 * - FileOperation types
 * - Various Options types
 */
class AdditionalTypesSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== TypeHierarchyItem Tests ====================

    @Test
    fun `TypeHierarchyItem minimal`() {
        val item = TypeHierarchyItem(
            name = "MyClass",
            kind = SymbolKind.Class,
            uri = "file:///test.kt",
            range = Range(Position(10, 0), Position(50, 0)),
            selectionRange = Range(Position(10, 6), Position(10, 13)),
        )
        val encoded = json.encodeToString(item)
        encoded shouldContain "\"name\":\"MyClass\""
        encoded shouldContain "\"kind\":5"
    }

    @Test
    fun `TypeHierarchyItem full`() {
        val item = TypeHierarchyItem(
            name = "BaseClass",
            kind = SymbolKind.Class,
            tags = listOf(SymbolTag.Deprecated),
            detail = "abstract class BaseClass",
            uri = "file:///src/Base.kt",
            range = Range(Position(5, 0), Position(100, 0)),
            selectionRange = Range(Position(5, 15), Position(5, 24)),
            data = JsonPrimitive("type-data"),
        )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<TypeHierarchyItem>(encoded)
        decoded.tags shouldBe listOf(SymbolTag.Deprecated)
        decoded.detail shouldBe "abstract class BaseClass"
        decoded.data shouldBe JsonPrimitive("type-data")
    }

    @Test
    fun `TypeHierarchyItem round-trip`() {
        val item = TypeHierarchyItem(
            name = "Interface",
            kind = SymbolKind.Interface,
            uri = "file:///api.kt",
            range = Range(Position(0, 0), Position(20, 0)),
            selectionRange = Range(Position(0, 10), Position(0, 19)),
        )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<TypeHierarchyItem>(encoded)
        decoded shouldBe item
    }

    // ==================== TypeHierarchyPrepareParams Tests ====================

    @Test
    fun `TypeHierarchyPrepareParams serialization`() {
        val params = TypeHierarchyPrepareParams(
            textDocument = TextDocumentIdentifier(uri = "file:///types.kt"),
            position = Position(line = 15, character = 8),
        )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///types.kt\""
    }

    @Test
    fun `TypeHierarchyPrepareParams round-trip`() {
        val params = TypeHierarchyPrepareParams(
            textDocument = TextDocumentIdentifier(uri = "file:///src/Model.kt"),
            position = Position(line = 25, character = 12),
        )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<TypeHierarchyPrepareParams>(encoded)
        decoded shouldBe params
    }

    // ==================== TypeHierarchySupertypesParams Tests ====================

    @Test
    fun `TypeHierarchySupertypesParams serialization`() {
        val item = TypeHierarchyItem(
            name = "Child",
            kind = SymbolKind.Class,
            uri = "file:///child.kt",
            range = Range(Position(10, 0), Position(30, 0)),
            selectionRange = Range(Position(10, 6), Position(10, 11)),
        )
        val params = TypeHierarchySupertypesParams(item = item)
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"item\""
    }

    // ==================== TypeHierarchySubtypesParams Tests ====================

    @Test
    fun `TypeHierarchySubtypesParams serialization`() {
        val item = TypeHierarchyItem(
            name = "Parent",
            kind = SymbolKind.Class,
            uri = "file:///parent.kt",
            range = Range(Position(5, 0), Position(50, 0)),
            selectionRange = Range(Position(5, 6), Position(5, 12)),
        )
        val params = TypeHierarchySubtypesParams(item = item)
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"item\""
    }

    // ==================== NotebookDocumentSyncOptions Tests ====================

    @Test
    fun `NotebookDocumentSyncOptions minimal`() {
        val options = NotebookDocumentSyncOptions(
            notebookSelector = listOf(
                NotebookSelector(notebook = Either.Left("jupyter-notebook")),
            ),
        )
        val encoded = json.encodeToString(options)
        encoded shouldContain "\"notebookSelector\""
    }

    @Test
    fun `NotebookDocumentSyncOptions with save`() {
        val options = NotebookDocumentSyncOptions(
            notebookSelector = listOf(
                NotebookSelector(notebook = Either.Left("*")),
            ),
            save = true,
        )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<NotebookDocumentSyncOptions>(encoded)
        decoded.save shouldBe true
    }

    // ==================== NotebookSelector Tests ====================

    @Test
    fun `NotebookSelector minimal`() {
        val selector = NotebookSelector()
        val encoded = json.encodeToString(selector)
        encoded shouldBe "{}"
    }

    @Test
    fun `NotebookSelector with notebook type`() {
        val selector = NotebookSelector(
            notebook = Either.Left("jupyter-notebook"),
        )
        val encoded = json.encodeToString(selector)
        val decoded = json.decodeFromString<NotebookSelector>(encoded)
        decoded.notebook shouldBe Either.Left("jupyter-notebook")
    }

    @Test
    fun `NotebookSelector with cells`() {
        val selector = NotebookSelector(
            notebook = Either.Left("jupyter-notebook"),
            cells = listOf(
                NotebookCellSelector(language = "python"),
                NotebookCellSelector(language = "markdown"),
            ),
        )
        val encoded = json.encodeToString(selector)
        val decoded = json.decodeFromString<NotebookSelector>(encoded)
        decoded.cells?.size shouldBe 2
    }

    // ==================== DiagnosticOptions Tests ====================

    @Test
    fun `DiagnosticOptions minimal`() {
        val options = DiagnosticOptions(
            interFileDependencies = true,
            workspaceDiagnostics = false,
        )
        val encoded = json.encodeToString(options)
        encoded shouldContain "\"interFileDependencies\":true"
        encoded shouldContain "\"workspaceDiagnostics\":false"
    }

    @Test
    fun `DiagnosticOptions with identifier`() {
        val options = DiagnosticOptions(
            identifier = "my-diagnostic-provider",
            interFileDependencies = false,
            workspaceDiagnostics = true,
        )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<DiagnosticOptions>(encoded)
        decoded.identifier shouldBe "my-diagnostic-provider"
    }

    @Test
    fun `DiagnosticOptions round-trip`() {
        val options = DiagnosticOptions(
            identifier = "lsp4k",
            interFileDependencies = true,
            workspaceDiagnostics = true,
        )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<DiagnosticOptions>(encoded)
        decoded shouldBe options
    }

    // ==================== FileOperationOptions Tests ====================

    @Test
    fun `FileOperationOptions minimal`() {
        val options = FileOperationOptions()
        val encoded = json.encodeToString(options)
        encoded shouldBe "{}"
    }

    @Test
    fun `FileOperationOptions with all fields`() {
        val filter = FileOperationFilter(
            pattern = FileOperationPattern(glob = "**/*.kt"),
        )
        val registration = FileOperationRegistrationOptions(filters = listOf(filter))
        val options = FileOperationOptions(
            didCreate = registration,
            willCreate = registration,
            didRename = registration,
            willRename = registration,
            didDelete = registration,
            willDelete = registration,
        )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<FileOperationOptions>(encoded)
        decoded.didCreate shouldBe registration
        decoded.willCreate shouldBe registration
    }

    // ==================== FileOperationFilter Tests ====================

    @Test
    fun `FileOperationFilter minimal`() {
        val filter = FileOperationFilter(
            pattern = FileOperationPattern(glob = "*.kt"),
        )
        val encoded = json.encodeToString(filter)
        encoded shouldContain "\"glob\":\"*.kt\""
    }

    @Test
    fun `FileOperationFilter with scheme`() {
        val filter = FileOperationFilter(
            scheme = "file",
            pattern = FileOperationPattern(glob = "**/*.java"),
        )
        val encoded = json.encodeToString(filter)
        val decoded = json.decodeFromString<FileOperationFilter>(encoded)
        decoded.scheme shouldBe "file"
    }

    // ==================== FileOperationPattern Tests ====================

    @Test
    fun `FileOperationPattern minimal`() {
        val pattern = FileOperationPattern(glob = "**/*.ts")
        val encoded = json.encodeToString(pattern)
        encoded shouldContain "\"glob\":\"**/*.ts\""
    }

    @Test
    fun `FileOperationPattern with matches and options`() {
        val pattern = FileOperationPattern(
            glob = "**/build/**",
            matches = FileOperationPatternKind.Folder,
            options = FileOperationPatternOptions(ignoreCase = true),
        )
        val encoded = json.encodeToString(pattern)
        val decoded = json.decodeFromString<FileOperationPattern>(encoded)
        decoded.matches shouldBe FileOperationPatternKind.Folder
        decoded.options?.ignoreCase shouldBe true
    }

    // ==================== Options Types Tests ====================

    @Test
    fun `CodeActionOptions serialization`() {
        val options = CodeActionOptions(
            codeActionKinds = listOf(CodeActionKind.QuickFix, CodeActionKind.Refactor),
            resolveProvider = true,
            workDoneProgress = true,
        )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<CodeActionOptions>(encoded)
        decoded.codeActionKinds?.size shouldBe 2
        decoded.resolveProvider shouldBe true
    }

    @Test
    fun `RenameOptions serialization`() {
        val options = RenameOptions(
            prepareProvider = true,
            workDoneProgress = true,
        )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<RenameOptions>(encoded)
        decoded.prepareProvider shouldBe true
    }

    @Test
    fun `DocumentSymbolOptions serialization`() {
        val options = DocumentSymbolOptions(
            label = "Document Symbols",
            workDoneProgress = true,
        )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<DocumentSymbolOptions>(encoded)
        decoded.label shouldBe "Document Symbols"
    }

    @Test
    fun `WorkspaceSymbolOptions serialization`() {
        val options = WorkspaceSymbolOptions(
            resolveProvider = true,
            workDoneProgress = true,
        )
        val encoded = json.encodeToString(options)
        val decoded = json.decodeFromString<WorkspaceSymbolOptions>(encoded)
        decoded.resolveProvider shouldBe true
    }

    // ==================== Client Capability Types Tests ====================

    @Test
    fun `DocumentLinkClientCapabilities serialization`() {
        val caps = DocumentLinkClientCapabilities(
            dynamicRegistration = true,
            tooltipSupport = true,
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<DocumentLinkClientCapabilities>(encoded)
        decoded.tooltipSupport shouldBe true
    }

    @Test
    fun `DocumentRangeFormattingClientCapabilities serialization`() {
        val caps = DocumentRangeFormattingClientCapabilities(
            dynamicRegistration = true,
            rangesSupport = true,
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<DocumentRangeFormattingClientCapabilities>(encoded)
        decoded.rangesSupport shouldBe true
    }

    @Test
    fun `CallHierarchyClientCapabilities serialization`() {
        val caps = CallHierarchyClientCapabilities(
            dynamicRegistration = true,
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<CallHierarchyClientCapabilities>(encoded)
        decoded.dynamicRegistration shouldBe true
    }
}
