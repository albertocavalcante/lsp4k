package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Wire-shape tests for the LSP 3.18 diagnostics, code action and completion additions, using a Json
 * equivalent to jsonrpc4k `JsonRpcCodec.defaultJson`.
 */
class Lsp318DiagnosticsCodeActionCompletionTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            isLenient = true
            coerceInputValues = true
        }

    private val range = Range(Position(0, 0), Position(0, 1))
    private val rangeJson = """{"start":{"line":0,"character":0},"end":{"line":0,"character":1}}"""

    // ==================== Diagnostic.message ====================

    @Test
    fun `diagnostic message accepts a string`() {
        val diagnostic = Diagnostic(range = range, message = Either.left("m"))
        json.encodeToString(diagnostic) shouldBe """{"range":$rangeJson,"message":"m"}"""
        json.decodeFromString<Diagnostic>("""{"range":$rangeJson,"message":"m"}""") shouldBe diagnostic
    }

    @Test
    fun `diagnostic message accepts markup content`() {
        val diagnostic =
            Diagnostic(
                range = range,
                message = Either.right(MarkupContent(kind = MarkupKind.Markdown, value = "**m**")),
            )
        val wire = """{"range":$rangeJson,"message":{"kind":"markdown","value":"**m**"}}"""
        json.encodeToString(diagnostic) shouldBe wire
        json.decodeFromString<Diagnostic>(wire) shouldBe diagnostic
    }

    @Test
    fun `diagnostic message rejects other forms`() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<Diagnostic>("""{"range":$rangeJson,"message":1}""")
        }
        assertFailsWith<SerializationException> {
            json.decodeFromString<Diagnostic>("""{"range":$rangeJson,"message":[]}""")
        }
    }

    // ==================== Diagnostic capabilities ====================

    @Test
    fun `diagnostic client capabilities carry the shared fields and markupMessageSupport`() {
        val capabilities =
            DiagnosticClientCapabilities(
                relatedInformation = true,
                tagSupport = DiagnosticTagSupport(valueSet = listOf(DiagnosticTag.Unnecessary)),
                codeDescriptionSupport = true,
                dataSupport = true,
                markupMessageSupport = true,
            )
        val wire =
            """{"relatedInformation":true,"tagSupport":{"valueSet":[1]},""" +
                """"codeDescriptionSupport":true,"dataSupport":true,"markupMessageSupport":true}"""
        json.encodeToString(capabilities) shouldBe wire
        json.decodeFromString<DiagnosticClientCapabilities>(wire) shouldBe capabilities
    }

    // ==================== Pull diagnostics ====================

    @Test
    fun `document diagnostic report keeps related documents in both forms`() {
        val report: DocumentDiagnosticReport =
            DocumentDiagnosticReport.Full(
                RelatedFullDocumentDiagnosticReport(
                    items = emptyList(),
                    relatedDocuments =
                        mapOf<DocumentUri, RelatedDocumentReport>(
                            "file:///b" to Either.left(FullDocumentDiagnosticReport(items = emptyList())),
                            "file:///c" to Either.right(UnchangedDocumentDiagnosticReport(resultId = "r")),
                        ),
                ),
            )
        val wire =
            """{"kind":"full","items":[],"relatedDocuments":{"file:///b":{"kind":"full","items":[]},""" +
                """"file:///c":{"kind":"unchanged","resultId":"r"}}}"""
        json.encodeToString(report) shouldBe wire
        json.decodeFromString<DocumentDiagnosticReport>(wire) shouldBe report
    }

    @Test
    fun `unchanged document diagnostic report keeps related documents`() {
        val report: DocumentDiagnosticReport =
            DocumentDiagnosticReport.Unchanged(
                RelatedUnchangedDocumentDiagnosticReport(
                    resultId = "r",
                    relatedDocuments =
                        mapOf<DocumentUri, RelatedDocumentReport>(
                            "file:///b" to Either.left(FullDocumentDiagnosticReport(items = emptyList())),
                        ),
                ),
            )
        val wire = """{"kind":"unchanged","resultId":"r","relatedDocuments":{"file:///b":{"kind":"full","items":[]}}}"""
        json.encodeToString(report) shouldBe wire
        json.decodeFromString<DocumentDiagnosticReport>(wire) shouldBe report
    }

    @Test
    fun `related document entries without a known kind are rejected`() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<DocumentDiagnosticReport>(
                """{"kind":"full","items":[],"relatedDocuments":{"file:///b":{"items":[]}}}""",
            )
        }
    }

    @Test
    fun `document diagnostic report progress accepts a report and a partial result`() {
        val serializer = DocumentDiagnosticReportProgressSerializer
        val first: DocumentDiagnosticReportProgress =
            Either.left(DocumentDiagnosticReport.Unchanged(RelatedUnchangedDocumentDiagnosticReport(resultId = "r")))
        val firstWire = """{"kind":"unchanged","resultId":"r"}"""
        json.encodeToString(serializer, first) shouldBe firstWire
        json.decodeFromString(serializer, firstWire) shouldBe first

        val partial: DocumentDiagnosticReportProgress =
            Either.right(
                DocumentDiagnosticReportPartialResult(
                    relatedDocuments =
                        mapOf<DocumentUri, RelatedDocumentReport>(
                            "file:///b" to Either.left(FullDocumentDiagnosticReport(items = emptyList())),
                        ),
                ),
            )
        val partialWire = """{"relatedDocuments":{"file:///b":{"kind":"full","items":[]}}}"""
        json.encodeToString(serializer, partial) shouldBe partialWire
        json.decodeFromString(serializer, partialWire) shouldBe partial

        assertFailsWith<SerializationException> { json.decodeFromString(serializer, "1") }
    }

    @Test
    fun `diagnostic server cancellation data`() {
        json.encodeToString(DiagnosticServerCancellationData(retriggerRequest = true)) shouldBe
            """{"retriggerRequest":true}"""
        json.decodeFromString<DiagnosticServerCancellationData>("""{"retriggerRequest":false}""") shouldBe
            DiagnosticServerCancellationData(retriggerRequest = false)
    }

    // ==================== Code action and command ====================

    @Test
    fun `code action tags encode as integers`() {
        val action = CodeAction(title = "t", tags = listOf(CodeActionTag.LlmGenerated))
        json.encodeToString(action) shouldBe """{"title":"t","tags":[1]}"""
        json.decodeFromString<CodeAction>("""{"title":"t","tags":[1]}""") shouldBe action
    }

    @Test
    fun `code action kinds include move and notebook`() {
        CodeActionKind.REFACTOR_MOVE shouldBe "refactor.move"
        CodeActionKind.NOTEBOOK shouldBe "notebook"
    }

    @Test
    fun `code action options carry kind documentation`() {
        val options =
            CodeActionOptions(
                documentation =
                    listOf(
                        CodeActionKindDocumentation(
                            kind = CodeActionKind.REFACTOR_MOVE,
                            command = Command(title = "Docs", command = "docs.open"),
                        ),
                    ),
            )
        val wire = """{"documentation":[{"kind":"refactor.move","command":{"title":"Docs","command":"docs.open"}}]}"""
        json.encodeToString(options) shouldBe wire
        json.decodeFromString<CodeActionOptions>(wire) shouldBe options
    }

    @Test
    fun `code action client capabilities carry documentationSupport and tagSupport`() {
        val capabilities =
            CodeActionClientCapabilities(
                documentationSupport = true,
                tagSupport = CodeActionTagSupport(valueSet = listOf(CodeActionTag.LlmGenerated)),
            )
        val wire = """{"documentationSupport":true,"tagSupport":{"valueSet":[1]}}"""
        json.encodeToString(capabilities) shouldBe wire
        json.decodeFromString<CodeActionClientCapabilities>(wire) shouldBe capabilities
    }

    @Test
    fun `command tooltip is optional`() {
        json.encodeToString(Command(title = "t", command = "c")) shouldBe """{"title":"t","command":"c"}"""
        val command = Command(title = "t", command = "c", tooltip = "tip")
        json.encodeToString(command) shouldBe """{"title":"t","command":"c","tooltip":"tip"}"""
        json.decodeFromString<Command>("""{"title":"t","command":"c","tooltip":"tip"}""") shouldBe command
    }

    // ==================== Completion ====================

    @Test
    fun `completion list applyKind encodes integers`() {
        val list =
            CompletionList(
                isIncomplete = false,
                items = emptyList(),
                applyKind = CompletionItemApplyKinds(commitCharacters = ApplyKind.Merge, data = ApplyKind.Replace),
            )
        val wire = """{"isIncomplete":false,"items":[],"applyKind":{"commitCharacters":2,"data":1}}"""
        json.encodeToString(list) shouldBe wire
        json.decodeFromString<CompletionList>(wire) shouldBe list
    }

    @Test
    fun `completion options carry server completion item options`() {
        val options = CompletionOptions(completionItem = ServerCompletionItemOptions(labelDetailsSupport = true))
        val wire = """{"completionItem":{"labelDetailsSupport":true}}"""
        json.encodeToString(options) shouldBe wire
        json.decodeFromString<CompletionOptions>(wire) shouldBe options
    }

    @Test
    fun `completion list capabilities carry applyKindSupport`() {
        val capabilities = CompletionListCapabilities(applyKindSupport = true)
        json.encodeToString(capabilities) shouldBe """{"applyKindSupport":true}"""
        json.decodeFromString<CompletionListCapabilities>("""{"applyKindSupport":true}""") shouldBe capabilities
    }
}
