package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Options specific to a notebook plus its cells to be synced to the server.
 */
@Serializable
public data class NotebookDocumentSyncOptions(
    /**
     * The notebooks to be synced.
     */
    val notebookSelector: List<NotebookSelector>,
    /**
     * Whether save notification should be forwarded to the server.
     */
    val save: Boolean? = null,
)

/**
 * A notebook document filter denotes a notebook document by different properties.
 */
@Serializable
public data class NotebookDocumentFilter(
    /** The type of the enclosing notebook. */
    val notebookType: String? = null,
    /** A Uri scheme, like `file` or `untitled`. */
    val scheme: String? = null,
    /** A glob pattern. */
    val pattern: String? = null,
)

/**
 * Serializer for notebook which can be string | NotebookDocumentFilter.
 */
public object NotebookSelectorNotebookSerializer : KSerializer<Either<String, NotebookDocumentFilter>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("NotebookSelectorNotebook")

    override fun serialize(
        encoder: Encoder,
        value: Either<String, NotebookDocumentFilter>,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is Either.Left -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is Either.Right -> jsonEncoder.encodeSerializableValue(NotebookDocumentFilter.serializer(), value.value)
        }
    }

    override fun deserialize(decoder: Decoder): Either<String, NotebookDocumentFilter> {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> Either.Left(element.content)
            is JsonObject -> Either.Right(jsonDecoder.json.decodeFromJsonElement(NotebookDocumentFilter.serializer(), element))
            else -> throw IllegalArgumentException("notebook must be a string or NotebookDocumentFilter object")
        }
    }
}

/**
 * A notebook selector.
 */
@Serializable
public data class NotebookSelector(
    /**
     * The notebook to be synced. If a string value is provided it matches
     * against the notebook type. '*' matches every notebook.
     */
    @Serializable(with = NotebookSelectorNotebookSerializer::class)
    val notebook: Either<String, NotebookDocumentFilter>? = null,
    /**
     * The cells of the matching notebook to be synced.
     */
    val cells: List<NotebookCellSelector>? = null,
)

/**
 * A notebook cell selector.
 */
@Serializable
public data class NotebookCellSelector(
    /**
     * The language of the cell.
     */
    val language: String,
)

/**
 * Diagnostic options.
 */
@Serializable
public data class DiagnosticOptions(
    /**
     * An optional identifier under which the diagnostics are managed by the client.
     */
    val identifier: String? = null,
    /**
     * Whether the language has inter file dependencies meaning that editing code
     * in one file can result in a different diagnostic set in another file.
     */
    val interFileDependencies: Boolean,
    /**
     * The server provides support for workspace diagnostics as well.
     */
    val workspaceDiagnostics: Boolean,
)
