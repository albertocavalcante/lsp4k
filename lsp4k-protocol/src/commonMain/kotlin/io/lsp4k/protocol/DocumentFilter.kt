package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

/**
 * The glob pattern. Either a string pattern or a [RelativePattern].
 *
 * @since 3.17.0
 */
public typealias GlobPattern = Either<String, RelativePattern>

/** Serializer for [GlobPattern]: a JSON string is the pattern, a JSON object is a [RelativePattern]. */
public object GlobPatternSerializer : KSerializer<GlobPattern> by strictUnion(
    "GlobPattern",
    String.serializer(),
    RelativePattern.serializer(),
    JsonElement::isStringLiteral,
    JsonElement::isObject,
)

/** Serializer for `RelativePattern.baseUri`: a [WorkspaceFolder] object or a URI string. */
public object RelativePatternBaseUriSerializer : KSerializer<Either<WorkspaceFolder, URI>> by strictUnion(
    "WorkspaceFolderOrUri",
    WorkspaceFolder.serializer(),
    String.serializer(),
    JsonElement::isObject,
    JsonElement::isStringLiteral,
)

/**
 * A relative pattern is a helper to construct glob patterns that are matched
 * relatively to a base URI. The common value for a `baseUri` is a workspace
 * folder root, but it can be another absolute URI as well.
 *
 * Clients support it when `textDocument.filters.relativePatternSupport` is set.
 *
 * @since 3.17.0
 */
@Serializable
public data class RelativePattern(
    /**
     * A workspace folder or a base URI to which this pattern will be matched against relatively.
     */
    @Serializable(with = RelativePatternBaseUriSerializer::class)
    val baseUri: Either<WorkspaceFolder, URI>,
    /**
     * The actual glob pattern.
     */
    val pattern: String,
)

/**
 * A document filter describes a top level text document or a notebook cell document.
 *
 * On the wire an object with a `notebook` key is a [NotebookCellTextDocumentFilter] and any other
 * object is a [TextDocumentFilter].
 *
 * @since 3.17.0 - support for NotebookCellTextDocumentFilter.
 */
@Serializable(with = DocumentFilterSerializer::class)
public sealed interface DocumentFilter

/**
 * A document filter denotes a document by different properties like the language id, the URI scheme
 * of its resource, or a glob pattern applied to its path. The meta model splits it into the variants
 * `TextDocumentFilterLanguage`, `TextDocumentFilterScheme` and `TextDocumentFilterPattern`, each of
 * which requires one of the properties; this class merges them, so at least one property must be set.
 *
 * @since 3.17.0
 */
@Serializable
public data class TextDocumentFilter(
    /**
     * A language id, like `typescript`.
     */
    val language: String? = null,
    /**
     * A Uri scheme, like `file` or `untitled`.
     */
    val scheme: String? = null,
    /**
     * A glob pattern, like `*.{ts,js}`.
     *
     * @since 3.18.0 - support for relative patterns.
     */
    @Serializable(with = GlobPatternSerializer::class)
    val pattern: GlobPattern? = null,
) : DocumentFilter {
    init {
        require(language != null || scheme != null || pattern != null) {
            "TextDocumentFilter requires at least one of language, scheme or pattern"
        }
    }
}

/**
 * A notebook cell text document filter denotes a cell text document by different properties.
 *
 * @since 3.17.0
 */
@Serializable
public data class NotebookCellTextDocumentFilter(
    /**
     * A filter that matches against the notebook containing the notebook cell. If a string
     * value is provided it matches against the notebook type. '*' matches every notebook.
     */
    @Serializable(with = NotebookSelectorNotebookSerializer::class)
    val notebook: Either<String, NotebookDocumentFilter>,
    /**
     * A language id like `python`. Will be matched against the language id of the notebook
     * cell document. '*' matches every language.
     */
    val language: String? = null,
) : DocumentFilter

/**
 * Client capabilities for text document filters.
 *
 * @since 3.18.0
 */
@Serializable
public data class TextDocumentFilterClientCapabilities(
    /**
     * The client supports Relative Patterns.
     */
    val relativePatternSupport: Boolean? = null,
)

/**
 * Serializer for [DocumentFilter]. Only JSON objects are accepted; the `notebook` key selects
 * [NotebookCellTextDocumentFilter].
 */
public object DocumentFilterSerializer : KSerializer<DocumentFilter> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DocumentFilter")

    override fun serialize(
        encoder: Encoder,
        value: DocumentFilter,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is TextDocumentFilter -> jsonEncoder.encodeSerializableValue(TextDocumentFilter.serializer(), value)
            is NotebookCellTextDocumentFilter ->
                jsonEncoder.encodeSerializableValue(NotebookCellTextDocumentFilter.serializer(), value)
        }
    }

    override fun deserialize(decoder: Decoder): DocumentFilter {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        if (element !is JsonObject) {
            throw SerializationException("Invalid DocumentFilter, expected an object: $element")
        }
        return if (element.containsKey("notebook")) {
            jsonDecoder.json.decodeFromJsonElement(NotebookCellTextDocumentFilter.serializer(), element)
        } else {
            jsonDecoder.json.decodeFromJsonElement(TextDocumentFilter.serializer(), element)
        }
    }
}
