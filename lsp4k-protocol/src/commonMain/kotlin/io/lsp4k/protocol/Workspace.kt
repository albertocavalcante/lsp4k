package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A document change in a workspace edit. Can be a text document edit or a
 * resource operation (create, rename, delete).
 *
 * Per LSP spec, `documentChanges` is `(TextDocumentEdit | CreateFile | RenameFile | DeleteFile)[]`.
 */
@Serializable(with = DocumentChangeSerializer::class)
public sealed interface DocumentChange {
    public data class Edit(
        val edit: TextDocumentEdit,
    ) : DocumentChange

    public data class Create(
        val create: CreateFile,
    ) : DocumentChange

    public data class Rename(
        val rename: RenameFile,
    ) : DocumentChange

    public data class Delete(
        val delete: DeleteFile,
    ) : DocumentChange
}

/**
 * Serializer for DocumentChange that discriminates based on the "kind" field.
 * If "kind" is absent, it's a TextDocumentEdit; otherwise it's a resource operation.
 */
public object DocumentChangeSerializer : KSerializer<DocumentChange> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DocumentChange")

    override fun serialize(
        encoder: Encoder,
        value: DocumentChange,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is DocumentChange.Edit ->
                jsonEncoder.encodeSerializableValue(TextDocumentEdit.serializer(), value.edit)
            is DocumentChange.Create ->
                jsonEncoder.encodeSerializableValue(CreateFile.serializer(), value.create)
            is DocumentChange.Rename ->
                jsonEncoder.encodeSerializableValue(RenameFile.serializer(), value.rename)
            is DocumentChange.Delete ->
                jsonEncoder.encodeSerializableValue(DeleteFile.serializer(), value.delete)
        }
    }

    override fun deserialize(decoder: Decoder): DocumentChange {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        require(element is JsonObject) { "DocumentChange must be a JSON object" }
        val kind = element["kind"]?.jsonPrimitive?.content
        return when (kind) {
            "create" ->
                DocumentChange.Create(
                    jsonDecoder.json.decodeFromJsonElement(CreateFile.serializer(), element),
                )
            "rename" ->
                DocumentChange.Rename(
                    jsonDecoder.json.decodeFromJsonElement(RenameFile.serializer(), element),
                )
            "delete" ->
                DocumentChange.Delete(
                    jsonDecoder.json.decodeFromJsonElement(DeleteFile.serializer(), element),
                )
            null ->
                DocumentChange.Edit(
                    jsonDecoder.json.decodeFromJsonElement(TextDocumentEdit.serializer(), element),
                )
            else -> throw IllegalArgumentException("Unknown DocumentChange kind: $kind")
        }
    }
}

/**
 * A workspace edit represents changes to many resources managed in the workspace.
 */
@Serializable
public data class WorkspaceEdit(
    /**
     * Holds changes to existing resources.
     */
    val changes: Map<DocumentUri, List<TextEdit>>? = null,
    /**
     * Depending on the client capability `workspace.workspaceEdit.resourceOperations`
     * document changes are either an array of `TextDocumentEdit`s to express changes
     * to n different text documents where each text document edit addresses a specific
     * version of a text document. Or it can contain above `TextDocumentEdit`s mixed with
     * create, rename and delete file / folder operations.
     */
    val documentChanges: List<DocumentChange>? = null,
    /**
     * A map of change annotations that can be referenced in
     * `AnnotatedTextEdit`s or create, rename and delete file / folder
     * operations.
     */
    val changeAnnotations: Map<String, ChangeAnnotation>? = null,
)

/**
 * Additional information that describes document changes.
 */
@Serializable
public data class ChangeAnnotation(
    /**
     * A human-readable string describing the actual change.
     */
    val label: String,
    /**
     * A flag which indicates that user confirmation is needed before applying the change.
     */
    val needsConfirmation: Boolean? = null,
    /**
     * A human-readable string which is rendered less prominent in the user interface.
     */
    val description: String? = null,
)

/**
 * Parameters for the workspace/applyEdit request.
 */
@Serializable
public data class ApplyWorkspaceEditParams(
    /**
     * An optional label of the workspace edit.
     */
    val label: String? = null,
    /**
     * The edits to apply.
     */
    val edit: WorkspaceEdit,
)

/**
 * Result of the workspace/applyEdit request.
 */
@Serializable
public data class ApplyWorkspaceEditResult(
    /**
     * Indicates whether the edit was applied or not.
     */
    val applied: Boolean,
    /**
     * An optional textual description for why the edit was not applied.
     */
    val failureReason: String? = null,
    /**
     * Depending on the client's failure handling strategy `failedChange`
     * might contain the index of the change that failed.
     */
    val failedChange: Int? = null,
)

/**
 * Parameters for the workspace/symbol request.
 */
@Serializable
public data class WorkspaceSymbolParams(
    /**
     * A query string to filter symbols by.
     */
    val query: String,
)

/**
 * A special workspace symbol that supports locations without a range.
 */
@Serializable
public data class WorkspaceSymbol(
    /**
     * The name of this symbol.
     */
    val name: String,
    /**
     * The kind of this symbol.
     */
    val kind: SymbolKind,
    /**
     * Tags for this symbol.
     */
    val tags: List<SymbolTag>? = null,
    /**
     * The name of the symbol containing this symbol.
     */
    val containerName: String? = null,
    /**
     * The location of this symbol.
     */
    val location: Location,
    /**
     * A data entry field that is preserved on a workspace symbol between a
     * workspace symbol request and a workspace symbol resolve request.
     */
    val data: JsonElement? = null,
)

/**
 * Parameters for the workspace/didChangeConfiguration notification.
 */
@Serializable
public data class DidChangeConfigurationParams(
    /**
     * The actual changed settings.
     */
    val settings: JsonElement?,
)

/**
 * Parameters for the workspace/didChangeWatchedFiles notification.
 */
@Serializable
public data class DidChangeWatchedFilesParams(
    /**
     * The actual file events.
     */
    val changes: List<FileEvent>,
)

/**
 * An event describing a file change.
 */
@Serializable
public data class FileEvent(
    /**
     * The file's URI.
     */
    val uri: DocumentUri,
    /**
     * The change type.
     */
    val type: FileChangeType,
)

/**
 * The file event type.
 */
@Serializable(with = FileChangeTypeSerializer::class)
public enum class FileChangeType(
    public val value: Int,
) {
    Created(1),
    Changed(2),
    Deleted(3),
    ;

    public companion object {
        public fun fromValue(value: Int): FileChangeType =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown FileChangeType: $value")
    }
}

/**
 * Serializer for FileChangeType that encodes/decodes as integer.
 */
public object FileChangeTypeSerializer : IntEnumSerializer<FileChangeType>(
    "FileChangeType",
    FileChangeType::fromValue,
    { it.value },
)

/**
 * Parameters for the workspace/didChangeWorkspaceFolders notification.
 */
@Serializable
public data class DidChangeWorkspaceFoldersParams(
    /**
     * The actual workspace folder change event.
     */
    val event: WorkspaceFoldersChangeEvent,
)

/**
 * The workspace folder change event.
 */
@Serializable
public data class WorkspaceFoldersChangeEvent(
    /**
     * The array of added workspace folders.
     */
    val added: List<WorkspaceFolder>,
    /**
     * The array of the removed workspace folders.
     */
    val removed: List<WorkspaceFolder>,
)

/**
 * Watch kind flags as a bitmask.
 */
public object WatchKind {
    /** Interested in create events. */
    public const val CREATE: Int = 1

    /** Interested in change events. */
    public const val CHANGE: Int = 2

    /** Interested in delete events. */
    public const val DELETE: Int = 4
}

@Serializable
public data class FileSystemWatcher(
    val globPattern: String,
    val kind: Int? = null,
)

@Serializable
public data class DidChangeWatchedFilesRegistrationOptions(
    val watchers: List<FileSystemWatcher>,
)

@Serializable
public data class RelativePattern(
    val baseUri: String,
    val pattern: String,
)
