package io.lsp4k.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable(with = NotebookCellKindSerializer::class)
public enum class NotebookCellKind(
    public val value: Int,
) {
    Markup(1),
    Code(2),
    ;

    public companion object {
        public fun fromValue(value: Int): NotebookCellKind =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown NotebookCellKind: $value")
    }
}

public object NotebookCellKindSerializer : IntEnumSerializer<NotebookCellKind>(
    "NotebookCellKind",
    NotebookCellKind::fromValue,
    { it.value },
)

@Serializable
public data class ExecutionSummary(
    val executionOrder: Int,
    val success: Boolean? = null,
)

@Serializable
public data class NotebookCell(
    val kind: NotebookCellKind,
    val document: DocumentUri,
    val metadata: JsonElement? = null,
    val executionSummary: ExecutionSummary? = null,
)

@Serializable
public data class NotebookDocument(
    val uri: DocumentUri,
    val notebookType: String,
    val version: Int,
    val metadata: JsonElement? = null,
    val cells: List<NotebookCell>,
)

@Serializable
public data class NotebookDocumentIdentifier(
    val uri: DocumentUri,
)

@Serializable
public data class DidOpenNotebookDocumentParams(
    val notebookDocument: NotebookDocument,
    val cellTextDocuments: List<TextDocumentItem>,
)

@Serializable
public data class NotebookCellArrayChange(
    val start: Int,
    val deleteCount: Int,
    val cells: List<NotebookCell>? = null,
)

@Serializable
public data class NotebookDocumentCellChangeStructure(
    val array: NotebookCellArrayChange,
    val didOpen: List<TextDocumentItem>? = null,
    val didClose: List<TextDocumentIdentifier>? = null,
)

@Serializable
public data class NotebookDocumentCellContentChanges(
    val document: VersionedTextDocumentIdentifier,
    val changes: List<TextDocumentContentChangeEvent>,
)

@Serializable
public data class NotebookDocumentChangeEvent(
    val metadata: JsonElement? = null,
    val cells: NotebookDocumentCellChanges? = null,
)

@Serializable
public data class NotebookDocumentCellChanges(
    val structure: NotebookDocumentCellChangeStructure? = null,
    val data: List<NotebookCell>? = null,
    val textContent: List<NotebookDocumentCellContentChanges>? = null,
)

@Serializable
public data class DidChangeNotebookDocumentParams(
    val notebookDocument: VersionedNotebookDocumentIdentifier,
    val change: NotebookDocumentChangeEvent,
)

@Serializable
public data class VersionedNotebookDocumentIdentifier(
    val uri: DocumentUri,
    val version: Int,
)

@Serializable
public data class DidSaveNotebookDocumentParams(
    val notebookDocument: NotebookDocumentIdentifier,
)

@Serializable
public data class DidCloseNotebookDocumentParams(
    val notebookDocument: NotebookDocumentIdentifier,
    val cellTextDocuments: List<TextDocumentIdentifier>,
)
