package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/**
 * Tests for CodeAction type serialization including:
 * - CodeAction, CodeActionParams, CodeActionContext
 * - CodeActionTriggerKind, CodeActionKind constants
 * - CodeActionDisabled
 */
class CodeActionSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== CodeActionKind Constants Tests ====================

    @Test
    fun `CodeActionKind constants have correct values`() {
        CodeActionKind.EMPTY shouldBe ""
        CodeActionKind.QUICK_FIX shouldBe "quickfix"
        CodeActionKind.REFACTOR shouldBe "refactor"
        CodeActionKind.REFACTOR_EXTRACT shouldBe "refactor.extract"
        CodeActionKind.REFACTOR_INLINE shouldBe "refactor.inline"
        CodeActionKind.REFACTOR_REWRITE shouldBe "refactor.rewrite"
        CodeActionKind.SOURCE shouldBe "source"
        CodeActionKind.SOURCE_ORGANIZE_IMPORTS shouldBe "source.organizeImports"
        CodeActionKind.SOURCE_FIX_ALL shouldBe "source.fixAll"
    }

    // ==================== CodeActionTriggerKind Tests ====================

    @Test
    fun `CodeActionTriggerKind Invoked serialization`() {
        val kind = CodeActionTriggerKind.Invoked
        val encoded = json.encodeToString(kind)
        encoded shouldBe "1"
    }

    @Test
    fun `CodeActionTriggerKind Automatic serialization`() {
        val kind = CodeActionTriggerKind.Automatic
        val encoded = json.encodeToString(kind)
        encoded shouldBe "2"
    }

    @Test
    fun `CodeActionTriggerKind roundtrip for all values`() {
        CodeActionTriggerKind.entries.forEach { kind ->
            val encoded = json.encodeToString(kind)
            val decoded = json.decodeFromString<CodeActionTriggerKind>(encoded)
            decoded shouldBe kind
        }
    }

    // ==================== CodeActionDisabled Tests ====================

    @Test
    fun `CodeActionDisabled serialization`() {
        val disabled = CodeActionDisabled(reason = "Cannot extract - no selection")
        val encoded = json.encodeToString(disabled)
        encoded shouldBe """{"reason":"Cannot extract - no selection"}"""
    }

    @Test
    fun `CodeActionDisabled roundtrip`() {
        val disabled =
            CodeActionDisabled(
                reason = "This refactoring is not available in this context",
            )
        val encoded = json.encodeToString(disabled)
        val decoded = json.decodeFromString<CodeActionDisabled>(encoded)
        decoded shouldBe disabled
    }

    // ==================== CodeActionContext Tests ====================

    @Test
    fun `CodeActionContext minimal`() {
        val context = CodeActionContext(diagnostics = emptyList())
        val encoded = json.encodeToString(context)
        val decoded = json.decodeFromString<CodeActionContext>(encoded)
        decoded.diagnostics shouldBe emptyList()
        decoded.only shouldBe null
        decoded.triggerKind shouldBe null
    }

    @Test
    fun `CodeActionContext with diagnostics`() {
        val context =
            CodeActionContext(
                diagnostics =
                    listOf(
                        Diagnostic(
                            range = Range(Position(10, 0), Position(10, 20)),
                            message = "Unused variable 'x'",
                        ),
                        Diagnostic(
                            range = Range(Position(15, 0), Position(15, 10)),
                            message = "Type mismatch",
                        ),
                    ),
            )
        val encoded = json.encodeToString(context)
        val decoded = json.decodeFromString<CodeActionContext>(encoded)
        decoded.diagnostics.size shouldBe 2
    }

    @Test
    fun `CodeActionContext with only filter`() {
        val context =
            CodeActionContext(
                diagnostics = emptyList(),
                only = listOf(CodeActionKind.QUICK_FIX, CodeActionKind.REFACTOR),
            )
        val encoded = json.encodeToString(context)
        val decoded = json.decodeFromString<CodeActionContext>(encoded)
        decoded.only shouldBe listOf("quickfix", "refactor")
    }

    @Test
    fun `CodeActionContext with trigger kind`() {
        val context =
            CodeActionContext(
                diagnostics = emptyList(),
                triggerKind = CodeActionTriggerKind.Automatic,
            )
        val encoded = json.encodeToString(context)
        val decoded = json.decodeFromString<CodeActionContext>(encoded)
        decoded.triggerKind shouldBe CodeActionTriggerKind.Automatic
    }

    @Test
    fun `CodeActionContext comprehensive`() {
        val context =
            CodeActionContext(
                diagnostics =
                    listOf(
                        Diagnostic(
                            range = Range(Position(5, 0), Position(5, 10)),
                            message = "Error",
                            severity = DiagnosticSeverity.Error,
                        ),
                    ),
                only = listOf(CodeActionKind.QUICK_FIX),
                triggerKind = CodeActionTriggerKind.Invoked,
            )
        val encoded = json.encodeToString(context)
        val decoded = json.decodeFromString<CodeActionContext>(encoded)
        decoded shouldBe context
    }

    // ==================== CodeActionParams Tests ====================

    @Test
    fun `CodeActionParams serialization`() {
        val params =
            CodeActionParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                range = Range(Position(10, 0), Position(10, 20)),
                context = CodeActionContext(diagnostics = emptyList()),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<CodeActionParams>(encoded)
        decoded.textDocument.uri shouldBe "file:///test.kt"
        decoded.range shouldBe Range(Position(10, 0), Position(10, 20))
    }

    @Test
    fun `CodeActionParams with diagnostics in context`() {
        val params =
            CodeActionParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                range = Range(Position(5, 0), Position(5, 15)),
                context =
                    CodeActionContext(
                        diagnostics =
                            listOf(
                                Diagnostic(
                                    range = Range(Position(5, 0), Position(5, 15)),
                                    message = "Unused import",
                                    severity = DiagnosticSeverity.Warning,
                                    tags = listOf(DiagnosticTag.Unnecessary),
                                ),
                            ),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<CodeActionParams>(encoded)
        decoded.context.diagnostics.size shouldBe 1
    }

    // ==================== CodeAction Tests ====================

    @Test
    fun `CodeAction minimal`() {
        val action = CodeAction(title = "Quick Fix")
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CodeAction>(encoded)
        decoded.title shouldBe "Quick Fix"
        decoded.kind shouldBe null
        decoded.edit shouldBe null
        decoded.command shouldBe null
    }

    @Test
    fun `CodeAction with kind`() {
        val action =
            CodeAction(
                title = "Extract to variable",
                kind = CodeActionKind.REFACTOR_EXTRACT,
            )
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CodeAction>(encoded)
        decoded.kind shouldBe "refactor.extract"
    }

    @Test
    fun `CodeAction with diagnostics`() {
        val diagnostic =
            Diagnostic(
                range = Range(Position(10, 0), Position(10, 10)),
                message = "Unused variable",
            )
        val action =
            CodeAction(
                title = "Remove unused variable",
                kind = CodeActionKind.QUICK_FIX,
                diagnostics = listOf(diagnostic),
            )
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CodeAction>(encoded)
        decoded.diagnostics?.size shouldBe 1
    }

    @Test
    fun `CodeAction with isPreferred`() {
        val action =
            CodeAction(
                title = "Import 'List' from 'kotlin.collections'",
                kind = CodeActionKind.QUICK_FIX,
                isPreferred = true,
            )
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CodeAction>(encoded)
        decoded.isPreferred shouldBe true
    }

    @Test
    fun `CodeAction with disabled`() {
        val action =
            CodeAction(
                title = "Extract to method",
                kind = CodeActionKind.REFACTOR_EXTRACT,
                disabled = CodeActionDisabled(reason = "Selection is not a valid expression"),
            )
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CodeAction>(encoded)
        decoded.disabled?.reason shouldBe "Selection is not a valid expression"
    }

    @Test
    fun `CodeAction with edit`() {
        val action =
            CodeAction(
                title = "Add missing import",
                kind = CodeActionKind.QUICK_FIX,
                edit =
                    WorkspaceEdit(
                        changes =
                            mapOf(
                                "file:///test.kt" to
                                    listOf(
                                        TextEdit(
                                            range = Range(Position(0, 0), Position(0, 0)),
                                            newText = "import kotlin.collections.List\n",
                                        ),
                                    ),
                            ),
                    ),
            )
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CodeAction>(encoded)
        decoded.edit
            ?.changes
            ?.get("file:///test.kt")
            ?.first()
            ?.newText shouldBe "import kotlin.collections.List\n"
    }

    @Test
    fun `CodeAction with command`() {
        val action =
            CodeAction(
                title = "Apply and show documentation",
                kind = CodeActionKind.QUICK_FIX,
                command =
                    Command(
                        title = "Show Documentation",
                        command = "editor.action.showHover",
                    ),
            )
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CodeAction>(encoded)
        decoded.command?.command shouldBe "editor.action.showHover"
    }

    @Test
    fun `CodeAction with data`() {
        val action =
            CodeAction(
                title = "Lazy code action",
                kind = CodeActionKind.REFACTOR,
                data = JsonPrimitive("resolve-data-123"),
            )
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CodeAction>(encoded)
        decoded.data shouldBe JsonPrimitive("resolve-data-123")
    }

    @Test
    fun `CodeAction comprehensive`() {
        val action =
            CodeAction(
                title = "Rename 'foo' to 'bar'",
                kind = CodeActionKind.REFACTOR_REWRITE,
                diagnostics =
                    listOf(
                        Diagnostic(
                            range = Range(Position(5, 4), Position(5, 7)),
                            message = "Variable 'foo' should be renamed",
                        ),
                    ),
                isPreferred = true,
                edit =
                    WorkspaceEdit(
                        changes =
                            mapOf(
                                "file:///test.kt" to
                                    listOf(
                                        TextEdit(
                                            range = Range(Position(5, 4), Position(5, 7)),
                                            newText = "bar",
                                        ),
                                        TextEdit(
                                            range = Range(Position(10, 10), Position(10, 13)),
                                            newText = "bar",
                                        ),
                                    ),
                            ),
                    ),
                command =
                    Command(
                        title = "Format document",
                        command = "editor.action.formatDocument",
                    ),
                data = JsonPrimitive(42),
            )
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CodeAction>(encoded)
        decoded shouldBe action
    }

    // ==================== Edge Cases ====================

    @Test
    fun `CodeAction with empty title`() {
        val action = CodeAction(title = "")
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CodeAction>(encoded)
        decoded.title shouldBe ""
    }

    @Test
    fun `CodeAction with unicode title`() {
        val action = CodeAction(title = "修复代码 🔧")
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CodeAction>(encoded)
        decoded.title shouldBe "修复代码 🔧"
    }

    @Test
    fun `CodeAction with multiple code action kinds`() {
        // Test that various kinds work
        val kinds =
            listOf(
                CodeActionKind.QUICK_FIX,
                CodeActionKind.REFACTOR,
                CodeActionKind.REFACTOR_EXTRACT,
                CodeActionKind.REFACTOR_INLINE,
                CodeActionKind.REFACTOR_REWRITE,
                CodeActionKind.SOURCE,
                CodeActionKind.SOURCE_ORGANIZE_IMPORTS,
                CodeActionKind.SOURCE_FIX_ALL,
            )

        kinds.forEach { kind ->
            val action = CodeAction(title = "Action", kind = kind)
            val encoded = json.encodeToString(action)
            val decoded = json.decodeFromString<CodeAction>(encoded)
            decoded.kind shouldBe kind
        }
    }
}
