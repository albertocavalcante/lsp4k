package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/**
 * Tests for Diagnostic type serialization: Diagnostic, DiagnosticSeverity, DiagnosticTag.
 */
class DiagnosticSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== DiagnosticSeverity Tests ====================

    @Test
    fun `DiagnosticSeverity Error serialization`() {
        val severity = DiagnosticSeverity.Error
        val encoded = json.encodeToString(severity)
        encoded shouldBe "1"
    }

    @Test
    fun `DiagnosticSeverity Warning serialization`() {
        val severity = DiagnosticSeverity.Warning
        val encoded = json.encodeToString(severity)
        encoded shouldBe "2"
    }

    @Test
    fun `DiagnosticSeverity Information serialization`() {
        val severity = DiagnosticSeverity.Information
        val encoded = json.encodeToString(severity)
        encoded shouldBe "3"
    }

    @Test
    fun `DiagnosticSeverity Hint serialization`() {
        val severity = DiagnosticSeverity.Hint
        val encoded = json.encodeToString(severity)
        encoded shouldBe "4"
    }

    @Test
    fun `DiagnosticSeverity deserialization`() {
        json.decodeFromString<DiagnosticSeverity>("1") shouldBe DiagnosticSeverity.Error
        json.decodeFromString<DiagnosticSeverity>("2") shouldBe DiagnosticSeverity.Warning
        json.decodeFromString<DiagnosticSeverity>("3") shouldBe DiagnosticSeverity.Information
        json.decodeFromString<DiagnosticSeverity>("4") shouldBe DiagnosticSeverity.Hint
    }

    @Test
    fun `DiagnosticSeverity roundtrip for all values`() {
        DiagnosticSeverity.entries.forEach { severity ->
            val encoded = json.encodeToString(severity)
            val decoded = json.decodeFromString<DiagnosticSeverity>(encoded)
            decoded shouldBe severity
        }
    }

    // ==================== DiagnosticTag Tests ====================

    @Test
    fun `DiagnosticTag Unnecessary serialization`() {
        val tag = DiagnosticTag.Unnecessary
        val encoded = json.encodeToString(tag)
        encoded shouldBe "1"
    }

    @Test
    fun `DiagnosticTag Deprecated serialization`() {
        val tag = DiagnosticTag.Deprecated
        val encoded = json.encodeToString(tag)
        encoded shouldBe "2"
    }

    @Test
    fun `DiagnosticTag roundtrip for all values`() {
        DiagnosticTag.entries.forEach { tag ->
            val encoded = json.encodeToString(tag)
            val decoded = json.decodeFromString<DiagnosticTag>(encoded)
            decoded shouldBe tag
        }
    }

    // ==================== CodeDescription Tests ====================

    @Test
    fun `CodeDescription serialization`() {
        val codeDesc = CodeDescription(href = "https://example.com/docs/error-123")
        val encoded = json.encodeToString(codeDesc)
        encoded shouldBe """{"href":"https://example.com/docs/error-123"}"""
    }

    @Test
    fun `CodeDescription roundtrip`() {
        val codeDesc = CodeDescription(href = "https://kotlinlang.org/docs/reference/exceptions.html")
        val encoded = json.encodeToString(codeDesc)
        val decoded = json.decodeFromString<CodeDescription>(encoded)
        decoded shouldBe codeDesc
    }

    // ==================== DiagnosticRelatedInformation Tests ====================

    @Test
    fun `DiagnosticRelatedInformation serialization`() {
        val related =
            DiagnosticRelatedInformation(
                location =
                    Location(
                        uri = "file:///related.kt",
                        range = Range(Position(5, 0), Position(5, 10)),
                    ),
                message = "Related issue here",
            )
        val encoded = json.encodeToString(related)
        val decoded = json.decodeFromString<DiagnosticRelatedInformation>(encoded)
        decoded shouldBe related
    }

    // ==================== Diagnostic Tests ====================

    @Test
    fun `Diagnostic minimal serialization (required fields only)`() {
        val diagnostic =
            Diagnostic(
                range = Range(Position(10, 0), Position(10, 20)),
                message = "Syntax error",
            )
        val encoded = json.encodeToString(diagnostic)
        val decoded = json.decodeFromString<Diagnostic>(encoded)
        decoded shouldBe diagnostic
        decoded.severity shouldBe null
        decoded.code shouldBe null
        decoded.source shouldBe null
    }

    @Test
    fun `Diagnostic with severity`() {
        val diagnostic =
            Diagnostic(
                range = Range(Position(10, 0), Position(10, 20)),
                message = "Unused variable",
                severity = DiagnosticSeverity.Warning,
            )
        val encoded = json.encodeToString(diagnostic)
        encoded.contains("\"severity\":2") shouldBe true

        val decoded = json.decodeFromString<Diagnostic>(encoded)
        decoded shouldBe diagnostic
        decoded.severity shouldBe DiagnosticSeverity.Warning
    }

    @Test
    fun `Diagnostic with all optional fields`() {
        val diagnostic =
            Diagnostic(
                range = Range(Position(10, 0), Position(10, 20)),
                message = "Variable is deprecated",
                severity = DiagnosticSeverity.Warning,
                code = Either.right("DEPRECATION"),
                codeDescription = CodeDescription(href = "https://kotlinlang.org/docs/deprecation.html"),
                source = "kotlin-compiler",
                tags = listOf(DiagnosticTag.Deprecated),
                relatedInformation =
                    listOf(
                        DiagnosticRelatedInformation(
                            location =
                                Location(
                                    uri = "file:///other.kt",
                                    range = Range(Position(1, 0), Position(1, 10)),
                                ),
                            message = "Declared here",
                        ),
                    ),
                data = JsonPrimitive("additional-data"),
            )
        val encoded = json.encodeToString(diagnostic)
        val decoded = json.decodeFromString<Diagnostic>(encoded)
        decoded shouldBe diagnostic
    }

    @Test
    fun `Diagnostic with multiple tags`() {
        val diagnostic =
            Diagnostic(
                range = Range(Position(5, 0), Position(5, 15)),
                message = "Deprecated and unused",
                tags = listOf(DiagnosticTag.Deprecated, DiagnosticTag.Unnecessary),
            )
        val encoded = json.encodeToString(diagnostic)
        val decoded = json.decodeFromString<Diagnostic>(encoded)
        decoded.tags shouldBe listOf(DiagnosticTag.Deprecated, DiagnosticTag.Unnecessary)
    }

    @Test
    fun `Diagnostic with multiple related information`() {
        val diagnostic =
            Diagnostic(
                range = Range(Position(20, 0), Position(20, 30)),
                message = "Type mismatch",
                severity = DiagnosticSeverity.Error,
                relatedInformation =
                    listOf(
                        DiagnosticRelatedInformation(
                            location = Location("file:///a.kt", Range(Position(1, 0), Position(1, 10))),
                            message = "Expected type declared here",
                        ),
                        DiagnosticRelatedInformation(
                            location = Location("file:///b.kt", Range(Position(5, 0), Position(5, 10))),
                            message = "Actual type defined here",
                        ),
                    ),
            )
        val encoded = json.encodeToString(diagnostic)
        val decoded = json.decodeFromString<Diagnostic>(encoded)
        decoded.relatedInformation?.size shouldBe 2
    }

    @Test
    fun `Diagnostic deserialization with extra unknown fields`() {
        // JSON with an unknown field that should be ignored
        val jsonStr =
            """
            {
                "range": {"start": {"line": 1, "character": 0}, "end": {"line": 1, "character": 10}},
                "message": "Test message",
                "unknownField": "should be ignored"
            }
            """.trimIndent()
        val decoded = json.decodeFromString<Diagnostic>(jsonStr)
        decoded.message shouldBe "Test message"
    }

    // ==================== PublishDiagnosticsParams Tests ====================

    @Test
    fun `PublishDiagnosticsParams without version`() {
        val params =
            PublishDiagnosticsParams(
                uri = "file:///test.kt",
                diagnostics =
                    listOf(
                        Diagnostic(
                            range = Range(Position(0, 0), Position(0, 10)),
                            message = "Error message",
                        ),
                    ),
            )
        val encoded = json.encodeToString(params)
        // version should be absent
        encoded.contains("version") shouldBe false

        val decoded = json.decodeFromString<PublishDiagnosticsParams>(encoded)
        decoded shouldBe params
        decoded.version shouldBe null
    }

    @Test
    fun `PublishDiagnosticsParams with version`() {
        val params =
            PublishDiagnosticsParams(
                uri = "file:///test.kt",
                version = 5,
                diagnostics =
                    listOf(
                        Diagnostic(
                            range = Range(Position(0, 0), Position(0, 10)),
                            message = "Error message",
                            severity = DiagnosticSeverity.Error,
                        ),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<PublishDiagnosticsParams>(encoded)
        decoded shouldBe params
        decoded.version shouldBe 5
    }

    @Test
    fun `PublishDiagnosticsParams with empty diagnostics list`() {
        val params =
            PublishDiagnosticsParams(
                uri = "file:///clean.kt",
                diagnostics = emptyList(),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<PublishDiagnosticsParams>(encoded)
        decoded shouldBe params
        decoded.diagnostics shouldBe emptyList()
    }

    @Test
    fun `PublishDiagnosticsParams with multiple diagnostics`() {
        val params =
            PublishDiagnosticsParams(
                uri = "file:///test.kt",
                version = 3,
                diagnostics =
                    listOf(
                        Diagnostic(
                            range = Range(Position(1, 0), Position(1, 10)),
                            message = "First error",
                            severity = DiagnosticSeverity.Error,
                        ),
                        Diagnostic(
                            range = Range(Position(5, 0), Position(5, 20)),
                            message = "Warning here",
                            severity = DiagnosticSeverity.Warning,
                        ),
                        Diagnostic(
                            range = Range(Position(10, 0), Position(10, 15)),
                            message = "Just a hint",
                            severity = DiagnosticSeverity.Hint,
                        ),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<PublishDiagnosticsParams>(encoded)
        decoded.diagnostics.size shouldBe 3
    }
}
