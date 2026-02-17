package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for PullDiagnostics type serialization: DocumentDiagnosticParams,
 * FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport,
 * DocumentDiagnosticReport (sealed), WorkspaceDiagnosticParams,
 * PreviousResultId, WorkspaceDiagnosticReport.
 */
class PullDiagnosticsSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    private val jsonWithDefaults =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    // ==================== DocumentDiagnosticReportKind Tests ====================

    @Test
    fun `DocumentDiagnosticReportKind Full serialization`() {
        val kind = DocumentDiagnosticReportKind.Full
        val encoded = json.encodeToString(kind)
        encoded shouldBe "\"full\""
    }

    @Test
    fun `DocumentDiagnosticReportKind Unchanged serialization`() {
        val kind = DocumentDiagnosticReportKind.Unchanged
        val encoded = json.encodeToString(kind)
        encoded shouldBe "\"unchanged\""
    }

    @Test
    fun `DocumentDiagnosticReportKind roundtrip for all values`() {
        DocumentDiagnosticReportKind.entries.forEach { kind ->
            val encoded = json.encodeToString(kind)
            val decoded = json.decodeFromString<DocumentDiagnosticReportKind>(encoded)
            decoded shouldBe kind
        }
    }

    // ==================== DocumentDiagnosticParams Tests ====================

    @Test
    fun `DocumentDiagnosticParams with required fields only`() {
        val original = DocumentDiagnosticParams(
            textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
        )
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "identifier"
        encoded shouldNotContain "previousResultId"
        val decoded = json.decodeFromString<DocumentDiagnosticParams>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `DocumentDiagnosticParams with all fields`() {
        val original = DocumentDiagnosticParams(
            textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
            identifier = "diag-provider-1",
            previousResultId = "result-abc-123",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<DocumentDiagnosticParams>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `DocumentDiagnosticParams with null optionals`() {
        val original = DocumentDiagnosticParams(
            textDocument = TextDocumentIdentifier(uri = "file:///src/main.kt"),
            identifier = null,
            previousResultId = null,
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<DocumentDiagnosticParams>(encoded)
        decoded.identifier shouldBe null
        decoded.previousResultId shouldBe null
    }

    // ==================== FullDocumentDiagnosticReport Tests ====================

    @Test
    fun `FullDocumentDiagnosticReport with diagnostics`() {
        val original = FullDocumentDiagnosticReport(
            items = listOf(
                Diagnostic(
                    range = Range(Position(10, 0), Position(10, 20)),
                    message = "Unused variable",
                ),
            ),
        )
        val encoded = jsonWithDefaults.encodeToString(original)
        encoded shouldContain "\"full\""
        val decoded = jsonWithDefaults.decodeFromString<FullDocumentDiagnosticReport>(encoded)
        decoded.kind shouldBe DocumentDiagnosticReportKind.Full
        decoded.items.size shouldBe 1
    }

    @Test
    fun `FullDocumentDiagnosticReport with resultId`() {
        val original = FullDocumentDiagnosticReport(
            resultId = "result-42",
            items = listOf(
                Diagnostic(
                    range = Range(Position(0, 0), Position(0, 5)),
                    message = "Error",
                ),
            ),
        )
        val encoded = jsonWithDefaults.encodeToString(original)
        val decoded = jsonWithDefaults.decodeFromString<FullDocumentDiagnosticReport>(encoded)
        decoded.resultId shouldBe "result-42"
    }

    @Test
    fun `FullDocumentDiagnosticReport with empty items`() {
        val original = FullDocumentDiagnosticReport(items = emptyList())
        val encoded = jsonWithDefaults.encodeToString(original)
        val decoded = jsonWithDefaults.decodeFromString<FullDocumentDiagnosticReport>(encoded)
        decoded.items shouldBe emptyList()
    }

    // ==================== UnchangedDocumentDiagnosticReport Tests ====================

    @Test
    fun `UnchangedDocumentDiagnosticReport serialization roundtrip`() {
        val original = UnchangedDocumentDiagnosticReport(
            resultId = "result-abc",
        )
        val encoded = jsonWithDefaults.encodeToString(original)
        encoded shouldContain "\"unchanged\""
        val decoded = jsonWithDefaults.decodeFromString<UnchangedDocumentDiagnosticReport>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `UnchangedDocumentDiagnosticReport has correct kind`() {
        val original = UnchangedDocumentDiagnosticReport(resultId = "prev-result")
        original.kind shouldBe DocumentDiagnosticReportKind.Unchanged
    }

    // ==================== DocumentDiagnosticReport (sealed interface) Tests ====================

    @Test
    fun `DocumentDiagnosticReport Full variant serialization roundtrip`() {
        val report = FullDocumentDiagnosticReport(
            resultId = "full-result-1",
            items = listOf(
                Diagnostic(
                    range = Range(Position(5, 0), Position(5, 10)),
                    message = "Warning",
                ),
            ),
        )
        val original: DocumentDiagnosticReport = DocumentDiagnosticReport.Full(report)
        val encoded = jsonWithDefaults.encodeToString(original)
        encoded shouldContain "\"full\""
        val decoded = jsonWithDefaults.decodeFromString<DocumentDiagnosticReport>(encoded)
        decoded.shouldBeInstanceOf<DocumentDiagnosticReport.Full>()
        (decoded as DocumentDiagnosticReport.Full).report.resultId shouldBe "full-result-1"
        decoded.report.items.size shouldBe 1
    }

    @Test
    fun `DocumentDiagnosticReport Unchanged variant serialization roundtrip`() {
        val report = UnchangedDocumentDiagnosticReport(resultId = "unchanged-result-1")
        val original: DocumentDiagnosticReport = DocumentDiagnosticReport.Unchanged(report)
        val encoded = jsonWithDefaults.encodeToString(original)
        encoded shouldContain "\"unchanged\""
        val decoded = jsonWithDefaults.decodeFromString<DocumentDiagnosticReport>(encoded)
        decoded.shouldBeInstanceOf<DocumentDiagnosticReport.Unchanged>()
        (decoded as DocumentDiagnosticReport.Unchanged).report.resultId shouldBe "unchanged-result-1"
    }

    @Test
    fun `DocumentDiagnosticReport Full with empty diagnostics`() {
        val report = FullDocumentDiagnosticReport(items = emptyList())
        val original: DocumentDiagnosticReport = DocumentDiagnosticReport.Full(report)
        val encoded = jsonWithDefaults.encodeToString(original)
        val decoded = jsonWithDefaults.decodeFromString<DocumentDiagnosticReport>(encoded)
        decoded.shouldBeInstanceOf<DocumentDiagnosticReport.Full>()
        (decoded as DocumentDiagnosticReport.Full).report.items shouldBe emptyList()
    }

    // ==================== PreviousResultId Tests ====================

    @Test
    fun `PreviousResultId serialization roundtrip`() {
        val original = PreviousResultId(
            uri = "file:///test.kt",
            value = "result-123",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<PreviousResultId>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `PreviousResultId with empty value`() {
        val original = PreviousResultId(
            uri = "file:///empty.kt",
            value = "",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<PreviousResultId>(encoded)
        decoded.value shouldBe ""
    }

    // ==================== WorkspaceDiagnosticParams Tests ====================

    @Test
    fun `WorkspaceDiagnosticParams with empty previousResultIds`() {
        val original = WorkspaceDiagnosticParams(
            previousResultIds = emptyList(),
        )
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "identifier"
        val decoded = json.decodeFromString<WorkspaceDiagnosticParams>(encoded)
        decoded shouldBe original
        decoded.identifier shouldBe null
    }

    @Test
    fun `WorkspaceDiagnosticParams with identifier and previousResultIds`() {
        val original = WorkspaceDiagnosticParams(
            identifier = "diag-provider",
            previousResultIds = listOf(
                PreviousResultId(uri = "file:///a.kt", value = "r1"),
                PreviousResultId(uri = "file:///b.kt", value = "r2"),
            ),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<WorkspaceDiagnosticParams>(encoded)
        decoded shouldBe original
        decoded.previousResultIds.size shouldBe 2
    }

    @Test
    fun `WorkspaceDiagnosticParams with null identifier`() {
        val original = WorkspaceDiagnosticParams(
            identifier = null,
            previousResultIds = listOf(
                PreviousResultId(uri = "file:///test.kt", value = "prev"),
            ),
        )
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "identifier"
        val decoded = json.decodeFromString<WorkspaceDiagnosticParams>(encoded)
        decoded.identifier shouldBe null
    }

    // ==================== WorkspaceDiagnosticReport Tests ====================

    @Test
    fun `WorkspaceDiagnosticReport with full document report`() {
        val fullReport = WorkspaceFullDocumentDiagnosticReport(
            uri = "file:///test.kt",
            items = listOf(
                Diagnostic(
                    range = Range(Position(1, 0), Position(1, 10)),
                    message = "Error in workspace",
                ),
            ),
        )
        val original = WorkspaceDiagnosticReport(
            items = listOf(
                WorkspaceDocumentDiagnosticReport.Full(fullReport),
            ),
        )
        val encoded = jsonWithDefaults.encodeToString(original)
        val decoded = jsonWithDefaults.decodeFromString<WorkspaceDiagnosticReport>(encoded)
        decoded.items.size shouldBe 1
        decoded.items[0].shouldBeInstanceOf<WorkspaceDocumentDiagnosticReport.Full>()
    }

    @Test
    fun `WorkspaceDiagnosticReport with unchanged document report`() {
        val unchangedReport = WorkspaceUnchangedDocumentDiagnosticReport(
            uri = "file:///unchanged.kt",
            resultId = "prev-result",
        )
        val original = WorkspaceDiagnosticReport(
            items = listOf(
                WorkspaceDocumentDiagnosticReport.Unchanged(unchangedReport),
            ),
        )
        val encoded = jsonWithDefaults.encodeToString(original)
        val decoded = jsonWithDefaults.decodeFromString<WorkspaceDiagnosticReport>(encoded)
        decoded.items.size shouldBe 1
        decoded.items[0].shouldBeInstanceOf<WorkspaceDocumentDiagnosticReport.Unchanged>()
    }

    @Test
    fun `WorkspaceDiagnosticReport with mixed report types`() {
        val original = WorkspaceDiagnosticReport(
            items = listOf(
                WorkspaceDocumentDiagnosticReport.Full(
                    WorkspaceFullDocumentDiagnosticReport(
                        uri = "file:///a.kt",
                        items = emptyList(),
                        resultId = "r1",
                    ),
                ),
                WorkspaceDocumentDiagnosticReport.Unchanged(
                    WorkspaceUnchangedDocumentDiagnosticReport(
                        uri = "file:///b.kt",
                        resultId = "r2",
                    ),
                ),
            ),
        )
        val encoded = jsonWithDefaults.encodeToString(original)
        val decoded = jsonWithDefaults.decodeFromString<WorkspaceDiagnosticReport>(encoded)
        decoded.items.size shouldBe 2
        decoded.items[0].shouldBeInstanceOf<WorkspaceDocumentDiagnosticReport.Full>()
        decoded.items[1].shouldBeInstanceOf<WorkspaceDocumentDiagnosticReport.Unchanged>()
    }

    @Test
    fun `WorkspaceDiagnosticReport with empty items`() {
        val original = WorkspaceDiagnosticReport(items = emptyList())
        val encoded = jsonWithDefaults.encodeToString(original)
        val decoded = jsonWithDefaults.decodeFromString<WorkspaceDiagnosticReport>(encoded)
        decoded.items shouldBe emptyList()
    }

    // ==================== WorkspaceFullDocumentDiagnosticReport Tests ====================

    @Test
    fun `WorkspaceFullDocumentDiagnosticReport serialization roundtrip`() {
        val original = WorkspaceFullDocumentDiagnosticReport(
            uri = "file:///workspace/test.kt",
            items = listOf(
                Diagnostic(
                    range = Range(Position(0, 0), Position(0, 10)),
                    message = "Syntax error",
                ),
            ),
            resultId = "ws-result-1",
            version = 5,
        )
        val encoded = jsonWithDefaults.encodeToString(original)
        val decoded = jsonWithDefaults.decodeFromString<WorkspaceFullDocumentDiagnosticReport>(encoded)
        decoded.uri shouldBe "file:///workspace/test.kt"
        decoded.resultId shouldBe "ws-result-1"
        decoded.version shouldBe 5
        decoded.items.size shouldBe 1
    }

    @Test
    fun `WorkspaceFullDocumentDiagnosticReport with null version`() {
        val original = WorkspaceFullDocumentDiagnosticReport(
            uri = "file:///test.kt",
            items = emptyList(),
        )
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "version"
        val decoded = json.decodeFromString<WorkspaceFullDocumentDiagnosticReport>(encoded)
        decoded.version shouldBe null
    }

    // ==================== WorkspaceUnchangedDocumentDiagnosticReport Tests ====================

    @Test
    fun `WorkspaceUnchangedDocumentDiagnosticReport serialization roundtrip`() {
        val original = WorkspaceUnchangedDocumentDiagnosticReport(
            uri = "file:///unchanged.kt",
            resultId = "prev-42",
            version = 3,
        )
        val encoded = jsonWithDefaults.encodeToString(original)
        val decoded = jsonWithDefaults.decodeFromString<WorkspaceUnchangedDocumentDiagnosticReport>(encoded)
        decoded.uri shouldBe "file:///unchanged.kt"
        decoded.resultId shouldBe "prev-42"
        decoded.version shouldBe 3
    }

    @Test
    fun `WorkspaceUnchangedDocumentDiagnosticReport with null version`() {
        val original = WorkspaceUnchangedDocumentDiagnosticReport(
            uri = "file:///test.kt",
            resultId = "r1",
        )
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "version"
        val decoded = json.decodeFromString<WorkspaceUnchangedDocumentDiagnosticReport>(encoded)
        decoded.version shouldBe null
    }

    // ==================== DocumentFilter Tests ====================

    @Test
    fun `DocumentFilter with all fields`() {
        val original = DocumentFilter(
            language = "kotlin",
            scheme = "file",
            pattern = "*.kt",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<DocumentFilter>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `DocumentFilter with language only`() {
        val original = DocumentFilter(language = "typescript")
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "scheme"
        encoded shouldNotContain "pattern"
        val decoded = json.decodeFromString<DocumentFilter>(encoded)
        decoded.language shouldBe "typescript"
        decoded.scheme shouldBe null
        decoded.pattern shouldBe null
    }

    @Test
    fun `DocumentFilter with no fields`() {
        val original = DocumentFilter()
        val encoded = json.encodeToString(original)
        encoded shouldBe "{}"
    }
}
