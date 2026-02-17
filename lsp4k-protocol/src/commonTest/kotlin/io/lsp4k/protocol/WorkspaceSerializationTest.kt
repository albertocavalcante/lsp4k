package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test

/**
 * Tests for Workspace type serialization including:
 * - WorkspaceEdit, ChangeAnnotation, ApplyWorkspaceEditParams/Result
 * - WorkspaceSymbolParams, WorkspaceSymbol
 * - DidChangeConfigurationParams, DidChangeWatchedFilesParams
 * - FileEvent, FileChangeType
 * - DidChangeWorkspaceFoldersParams, WorkspaceFoldersChangeEvent
 */
class WorkspaceSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== WorkspaceEdit Tests ====================

    @Test
    fun `WorkspaceEdit empty`() {
        val edit = WorkspaceEdit()
        val encoded = json.encodeToString(edit)
        encoded shouldBe "{}"
    }

    @Test
    fun `WorkspaceEdit with changes`() {
        val edit =
            WorkspaceEdit(
                changes =
                    mapOf(
                        "file:///test.kt" to
                            listOf(
                                TextEdit(
                                    range = Range(Position(0, 0), Position(0, 5)),
                                    newText = "hello",
                                ),
                            ),
                    ),
            )
        val encoded = json.encodeToString(edit)
        val decoded = json.decodeFromString<WorkspaceEdit>(encoded)
        decoded.changes?.get("file:///test.kt")?.size shouldBe 1
    }

    @Test
    fun `WorkspaceEdit with multiple files`() {
        val edit =
            WorkspaceEdit(
                changes =
                    mapOf(
                        "file:///a.kt" to
                            listOf(
                                TextEdit(Range(Position(0, 0), Position(0, 10)), "new a"),
                            ),
                        "file:///b.kt" to
                            listOf(
                                TextEdit(Range(Position(5, 0), Position(5, 10)), "new b"),
                            ),
                        "file:///c.kt" to
                            listOf(
                                TextEdit(Range(Position(10, 0), Position(10, 10)), "new c"),
                            ),
                    ),
            )
        val encoded = json.encodeToString(edit)
        val decoded = json.decodeFromString<WorkspaceEdit>(encoded)
        decoded.changes?.size shouldBe 3
    }

    @Test
    fun `WorkspaceEdit with multiple edits per file`() {
        val edit =
            WorkspaceEdit(
                changes =
                    mapOf(
                        "file:///test.kt" to
                            listOf(
                                TextEdit(Range(Position(0, 0), Position(0, 5)), "first"),
                                TextEdit(Range(Position(10, 0), Position(10, 5)), "second"),
                                TextEdit(Range(Position(20, 0), Position(20, 5)), "third"),
                            ),
                    ),
            )
        val encoded = json.encodeToString(edit)
        val decoded = json.decodeFromString<WorkspaceEdit>(encoded)
        decoded.changes?.get("file:///test.kt")?.size shouldBe 3
    }

    @Test
    fun `WorkspaceEdit with documentChanges`() {
        val edit =
            WorkspaceEdit(
                documentChanges =
                    listOf(
                        DocumentChange.Edit(
                            TextDocumentEdit(
                                textDocument =
                                    OptionalVersionedTextDocumentIdentifier(
                                        uri = "file:///test.kt",
                                        version = 1,
                                    ),
                                edits =
                                    listOf(
                                        Either.Left(TextEdit(Range(Position(0, 0), Position(0, 5)), "edited")),
                                    ),
                            ),
                        ),
                    ),
            )
        val encoded = json.encodeToString(edit)
        val decoded = json.decodeFromString<WorkspaceEdit>(encoded)
        decoded.documentChanges?.size shouldBe 1
    }

    @Test
    fun `WorkspaceEdit with changeAnnotations`() {
        val edit =
            WorkspaceEdit(
                changeAnnotations =
                    mapOf(
                        "annotation1" to
                            ChangeAnnotation(
                                label = "Rename variable",
                                needsConfirmation = true,
                                description = "This will rename the variable across all usages",
                            ),
                    ),
            )
        val encoded = json.encodeToString(edit)
        val decoded = json.decodeFromString<WorkspaceEdit>(encoded)
        decoded.changeAnnotations?.get("annotation1")?.label shouldBe "Rename variable"
        decoded.changeAnnotations?.get("annotation1")?.needsConfirmation shouldBe true
    }

    // ==================== ChangeAnnotation Tests ====================

    @Test
    fun `ChangeAnnotation minimal`() {
        val annotation = ChangeAnnotation(label = "Quick fix")
        val encoded = json.encodeToString(annotation)
        val decoded = json.decodeFromString<ChangeAnnotation>(encoded)
        decoded.label shouldBe "Quick fix"
        decoded.needsConfirmation shouldBe null
        decoded.description shouldBe null
    }

    @Test
    fun `ChangeAnnotation with all fields`() {
        val annotation =
            ChangeAnnotation(
                label = "Refactor",
                needsConfirmation = true,
                description = "This refactoring is complex and requires confirmation",
            )
        val encoded = json.encodeToString(annotation)
        val decoded = json.decodeFromString<ChangeAnnotation>(encoded)
        decoded shouldBe annotation
    }

    @Test
    fun `ChangeAnnotation without confirmation`() {
        val annotation =
            ChangeAnnotation(
                label = "Auto-import",
                needsConfirmation = false,
            )
        val encoded = json.encodeToString(annotation)
        val decoded = json.decodeFromString<ChangeAnnotation>(encoded)
        decoded.needsConfirmation shouldBe false
    }

    // ==================== ApplyWorkspaceEditParams Tests ====================

    @Test
    fun `ApplyWorkspaceEditParams minimal`() {
        val params =
            ApplyWorkspaceEditParams(
                edit = WorkspaceEdit(),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<ApplyWorkspaceEditParams>(encoded)
        decoded.label shouldBe null
    }

    @Test
    fun `ApplyWorkspaceEditParams with label`() {
        val params =
            ApplyWorkspaceEditParams(
                label = "Rename 'oldName' to 'newName'",
                edit =
                    WorkspaceEdit(
                        changes =
                            mapOf(
                                "file:///test.kt" to
                                    listOf(
                                        TextEdit(Range(Position(5, 10), Position(5, 17)), "newName"),
                                    ),
                            ),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<ApplyWorkspaceEditParams>(encoded)
        decoded.label shouldBe "Rename 'oldName' to 'newName'"
    }

    // ==================== ApplyWorkspaceEditResult Tests ====================

    @Test
    fun `ApplyWorkspaceEditResult success`() {
        val result = ApplyWorkspaceEditResult(applied = true)
        val encoded = json.encodeToString(result)
        val decoded = json.decodeFromString<ApplyWorkspaceEditResult>(encoded)
        decoded.applied shouldBe true
        decoded.failureReason shouldBe null
    }

    @Test
    fun `ApplyWorkspaceEditResult failure with reason`() {
        val result =
            ApplyWorkspaceEditResult(
                applied = false,
                failureReason = "File was modified by another process",
            )
        val encoded = json.encodeToString(result)
        val decoded = json.decodeFromString<ApplyWorkspaceEditResult>(encoded)
        decoded.applied shouldBe false
        decoded.failureReason shouldBe "File was modified by another process"
    }

    @Test
    fun `ApplyWorkspaceEditResult failure with failed change index`() {
        val result =
            ApplyWorkspaceEditResult(
                applied = false,
                failureReason = "Edit conflict",
                failedChange = 2,
            )
        val encoded = json.encodeToString(result)
        val decoded = json.decodeFromString<ApplyWorkspaceEditResult>(encoded)
        decoded.failedChange shouldBe 2
    }

    // ==================== WorkspaceSymbolParams Tests ====================

    @Test
    fun `WorkspaceSymbolParams serialization`() {
        val params = WorkspaceSymbolParams(query = "MyClass")
        val encoded = json.encodeToString(params)
        encoded shouldBe """{"query":"MyClass"}"""
    }

    @Test
    fun `WorkspaceSymbolParams with empty query`() {
        val params = WorkspaceSymbolParams(query = "")
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<WorkspaceSymbolParams>(encoded)
        decoded.query shouldBe ""
    }

    @Test
    fun `WorkspaceSymbolParams with special characters`() {
        val params = WorkspaceSymbolParams(query = "fun<T>")
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<WorkspaceSymbolParams>(encoded)
        decoded.query shouldBe "fun<T>"
    }

    // ==================== WorkspaceSymbol Tests ====================

    @Test
    fun `WorkspaceSymbol minimal`() {
        val symbol =
            WorkspaceSymbol(
                name = "MyClass",
                kind = SymbolKind.Class,
                location =
                    Location(
                        uri = "file:///test.kt",
                        range = Range(Position(10, 0), Position(50, 1)),
                    ),
            )
        val encoded = json.encodeToString(symbol)
        val decoded = json.decodeFromString<WorkspaceSymbol>(encoded)
        decoded.name shouldBe "MyClass"
        decoded.kind shouldBe SymbolKind.Class
    }

    @Test
    fun `WorkspaceSymbol with all fields`() {
        val symbol =
            WorkspaceSymbol(
                name = "deprecatedMethod",
                kind = SymbolKind.Method,
                tags = listOf(SymbolTag.Deprecated),
                containerName = "MyClass",
                location =
                    Location(
                        uri = "file:///test.kt",
                        range = Range(Position(20, 4), Position(25, 5)),
                    ),
                data = JsonPrimitive("custom-data"),
            )
        val encoded = json.encodeToString(symbol)
        val decoded = json.decodeFromString<WorkspaceSymbol>(encoded)
        decoded.tags shouldBe listOf(SymbolTag.Deprecated)
        decoded.containerName shouldBe "MyClass"
        decoded.data shouldBe JsonPrimitive("custom-data")
    }

    // ==================== DidChangeConfigurationParams Tests ====================

    @Test
    fun `DidChangeConfigurationParams with null settings`() {
        val params = DidChangeConfigurationParams(settings = null)
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidChangeConfigurationParams>(encoded)
        decoded.settings shouldBe null
    }

    @Test
    fun `DidChangeConfigurationParams with json settings`() {
        val params =
            DidChangeConfigurationParams(
                settings =
                    buildJsonObject {
                        put("serverEnabled", JsonPrimitive(true))
                        put("maxCompletions", JsonPrimitive(100))
                    },
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidChangeConfigurationParams>(encoded)
        decoded.settings?.toString() shouldContain "serverEnabled"
    }

    // ==================== FileChangeType Tests ====================

    @Test
    fun `FileChangeType Created serialization`() {
        // FileChangeType serializes as integer per LSP spec
        val jsonStr = """{"uri":"file:///test.kt","type":1}"""
        val decoded = json.decodeFromString<FileEvent>(jsonStr)
        decoded.type shouldBe FileChangeType.Created
    }

    @Test
    fun `FileChangeType Changed serialization`() {
        val jsonStr = """{"uri":"file:///test.kt","type":2}"""
        val decoded = json.decodeFromString<FileEvent>(jsonStr)
        decoded.type shouldBe FileChangeType.Changed
    }

    @Test
    fun `FileChangeType Deleted serialization`() {
        val jsonStr = """{"uri":"file:///test.kt","type":3}"""
        val decoded = json.decodeFromString<FileEvent>(jsonStr)
        decoded.type shouldBe FileChangeType.Deleted
    }

    // ==================== FileEvent Tests ====================

    @Test
    fun `FileEvent serialization roundtrip`() {
        val event =
            FileEvent(
                uri = "file:///changed.kt",
                type = FileChangeType.Changed,
            )
        val encoded = json.encodeToString(event)
        val decoded = json.decodeFromString<FileEvent>(encoded)
        decoded.uri shouldBe "file:///changed.kt"
        decoded.type shouldBe FileChangeType.Changed
    }

    // ==================== DidChangeWatchedFilesParams Tests ====================

    @Test
    fun `DidChangeWatchedFilesParams with single change`() {
        val params =
            DidChangeWatchedFilesParams(
                changes =
                    listOf(
                        FileEvent(uri = "file:///created.kt", type = FileChangeType.Created),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidChangeWatchedFilesParams>(encoded)
        decoded.changes.size shouldBe 1
    }

    @Test
    fun `DidChangeWatchedFilesParams with multiple changes`() {
        val params =
            DidChangeWatchedFilesParams(
                changes =
                    listOf(
                        FileEvent(uri = "file:///a.kt", type = FileChangeType.Created),
                        FileEvent(uri = "file:///b.kt", type = FileChangeType.Changed),
                        FileEvent(uri = "file:///c.kt", type = FileChangeType.Deleted),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidChangeWatchedFilesParams>(encoded)
        decoded.changes.size shouldBe 3
    }

    @Test
    fun `DidChangeWatchedFilesParams with empty changes`() {
        val params = DidChangeWatchedFilesParams(changes = emptyList())
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidChangeWatchedFilesParams>(encoded)
        decoded.changes shouldBe emptyList()
    }

    // ==================== DidChangeWorkspaceFoldersParams Tests ====================

    @Test
    fun `DidChangeWorkspaceFoldersParams with added folders`() {
        val params =
            DidChangeWorkspaceFoldersParams(
                event =
                    WorkspaceFoldersChangeEvent(
                        added =
                            listOf(
                                WorkspaceFolder(uri = "file:///new-project", name = "new-project"),
                            ),
                        removed = emptyList(),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidChangeWorkspaceFoldersParams>(encoded)
        decoded.event.added.size shouldBe 1
        decoded.event.removed shouldBe emptyList()
    }

    @Test
    fun `DidChangeWorkspaceFoldersParams with removed folders`() {
        val params =
            DidChangeWorkspaceFoldersParams(
                event =
                    WorkspaceFoldersChangeEvent(
                        added = emptyList(),
                        removed =
                            listOf(
                                WorkspaceFolder(uri = "file:///old-project", name = "old-project"),
                            ),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidChangeWorkspaceFoldersParams>(encoded)
        decoded.event.added shouldBe emptyList()
        decoded.event.removed.size shouldBe 1
    }

    @Test
    fun `DidChangeWorkspaceFoldersParams with both added and removed`() {
        val params =
            DidChangeWorkspaceFoldersParams(
                event =
                    WorkspaceFoldersChangeEvent(
                        added =
                            listOf(
                                WorkspaceFolder(uri = "file:///project-a", name = "project-a"),
                                WorkspaceFolder(uri = "file:///project-b", name = "project-b"),
                            ),
                        removed =
                            listOf(
                                WorkspaceFolder(uri = "file:///old-project", name = "old-project"),
                            ),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DidChangeWorkspaceFoldersParams>(encoded)
        decoded.event.added.size shouldBe 2
        decoded.event.removed.size shouldBe 1
    }

    // ==================== WorkspaceFoldersChangeEvent Tests ====================

    @Test
    fun `WorkspaceFoldersChangeEvent empty`() {
        val event =
            WorkspaceFoldersChangeEvent(
                added = emptyList(),
                removed = emptyList(),
            )
        val encoded = json.encodeToString(event)
        val decoded = json.decodeFromString<WorkspaceFoldersChangeEvent>(encoded)
        decoded.added shouldBe emptyList()
        decoded.removed shouldBe emptyList()
    }
}
