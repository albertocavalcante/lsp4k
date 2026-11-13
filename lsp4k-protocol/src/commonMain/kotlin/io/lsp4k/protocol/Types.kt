package io.lsp4k.protocol

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.jvm.JvmInline

/**
 * Abstract base serializer for integer-backed enum types in LSP.
 * Subclasses only need to provide the serial name and conversion functions.
 */
public abstract class IntEnumSerializer<E>(
    serialName: String,
    private val fromValue: (Int) -> E,
    private val toValue: (E) -> Int,
) : KSerializer<E> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.INT)

    override fun serialize(
        encoder: Encoder,
        value: E,
    ): Unit = encoder.encodeInt(toValue(value))

    override fun deserialize(decoder: Decoder): E = fromValue(decoder.decodeInt())
}

/**
 * Type aliases for LSP primitive types.
 * These provide semantic meaning while remaining compatible with JSON.
 */
public typealias DocumentUri = String
public typealias URI = String
public typealias ProgressToken = Either<Int, String>

/**
 * Serializer for [ProgressToken] (`Either<Int, String>`).
 * Discriminates based on whether the JSON primitive is a string or integer.
 */
public object ProgressTokenSerializer : KSerializer<ProgressToken> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ProgressToken", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ProgressToken,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is Either.Left -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is Either.Right -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
        }
    }

    override fun deserialize(decoder: Decoder): ProgressToken {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement() as JsonPrimitive
        return if (element.isString) {
            Either.right(element.content)
        } else {
            Either.left(element.content.toInt())
        }
    }
}

/**
 * Serializer for nullable [ProgressToken].
 */
public object NullableProgressTokenSerializer : KSerializer<ProgressToken?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ProgressToken?", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ProgressToken?,
    ) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            ProgressTokenSerializer.serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): ProgressToken? =
        if (decoder.decodeNotNullMark()) {
            ProgressTokenSerializer.deserialize(decoder)
        } else {
            decoder.decodeNull()
        }
}

/**
 * Position in a text document expressed as zero-based line and character offset.
 * A position is between two characters like an 'insert' cursor in an editor.
 */
@Serializable
public data class Position(
    /**
     * Line position in a document (zero-based).
     */
    val line: Int,
    /**
     * Character offset on a line in a document (zero-based).
     * The meaning of this offset is determined by the negotiated
     * `PositionEncodingKind`.
     */
    val character: Int,
) : Comparable<Position> {
    override fun compareTo(other: Position): Int {
        val lineCompare = line.compareTo(other.line)
        return if (lineCompare != 0) lineCompare else character.compareTo(other.character)
    }

    public companion object {
        public val ZERO: Position = Position(0, 0)
    }
}

/**
 * A range in a text document expressed as (zero-based) start and end positions.
 * A range is comparable to a selection in an editor.
 */
@Serializable
public data class Range(
    /**
     * The range's start position (inclusive).
     */
    val start: Position,
    /**
     * The range's end position (exclusive).
     */
    val end: Position,
) {
    public fun contains(position: Position): Boolean = position >= start && position < end

    public fun isEmpty(): Boolean = start == end

    public companion object {
        public val ZERO: Range = Range(Position.ZERO, Position.ZERO)
    }
}

/**
 * Represents a location inside a resource, such as a line inside a text file.
 */
@Serializable
public data class Location(
    val uri: DocumentUri,
    val range: Range,
)

/**
 * Represents a link between a source and a target location.
 */
@Serializable
public data class LocationLink(
    /**
     * Span of the origin of this link.
     * Used as the underlined span for mouse interaction.
     */
    val originSelectionRange: Range? = null,
    /**
     * The target resource identifier of this link.
     */
    val targetUri: DocumentUri,
    /**
     * The full target range of this link.
     */
    val targetRange: Range,
    /**
     * The range that should be selected and revealed when this link is being followed.
     */
    val targetSelectionRange: Range,
)

/**
 * A literal to identify a text document in the client.
 */
@Serializable
public data class TextDocumentIdentifier(
    /**
     * The text document's URI.
     */
    val uri: DocumentUri,
)

/**
 * An identifier to denote a specific version of a text document.
 */
@Serializable
public data class VersionedTextDocumentIdentifier(
    /**
     * The text document's URI.
     */
    val uri: DocumentUri,
    /**
     * The version number of this document.
     */
    val version: Int,
)

/**
 * An identifier which optionally denotes a specific version of a text document.
 */
@Serializable
public data class OptionalVersionedTextDocumentIdentifier(
    /**
     * The text document's URI.
     */
    val uri: DocumentUri,
    /**
     * The version number of this document. If an optional versioned text document
     * identifier is sent from the server to the client and the file is not open
     * in the editor the server can send `null` to indicate that the version is
     * known and the content on disk is the master.
     */
    val version: Int?,
)

/**
 * An item to transfer a text document from the client to the server.
 */
@Serializable
public data class TextDocumentItem(
    /**
     * The text document's URI.
     */
    val uri: DocumentUri,
    /**
     * The text document's language identifier.
     */
    val languageId: String,
    /**
     * The version number of this document (increases after each change).
     */
    val version: Int,
    /**
     * The content of the opened text document.
     */
    val text: String,
)

/**
 * A parameter literal used in requests to pass a text document and a position inside that document.
 */
@Serializable
public data class TextDocumentPositionParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The position inside the text document.
     */
    val position: Position,
)

/**
 * An entry of `TextDocumentEdit.edits`: a [TextEdit], an [AnnotatedTextEdit] or a [SnippetTextEdit].
 *
 * On the wire the member is chosen by content: `snippet` selects [SnippetTextEdit], `newText` with
 * `annotationId` selects [AnnotatedTextEdit], and `newText` alone selects [TextEdit].
 */
@Serializable(with = DocumentTextEditSerializer::class)
public sealed interface DocumentTextEdit

/**
 * A text edit applicable to a text document.
 */
@Serializable
public data class TextEdit(
    /**
     * The range of the text document to be manipulated.
     */
    val range: Range,
    /**
     * The string to be inserted. For delete operations use an empty string.
     */
    val newText: String,
) : DocumentTextEdit

/**
 * A special text edit with an additional change annotation.
 */
@Serializable
public data class AnnotatedTextEdit(
    val range: Range,
    val newText: String,
    val annotationId: String,
) : DocumentTextEdit

/**
 * An interactive text edit. The snippet can contain placeholders and tab stops
 * following the TextMate snippet syntax. Clients support it when the
 * `workspace.workspaceEdit.snippetEditSupport` capability is set.
 *
 * @since 3.18.0
 */
@Serializable
public data class SnippetTextEdit(
    /**
     * The range of the text document to be manipulated.
     */
    val range: Range,
    /**
     * The snippet to be inserted.
     */
    val snippet: StringValue,
    /**
     * The actual identifier of the snippet edit.
     */
    val annotationId: String? = null,
) : DocumentTextEdit

/**
 * A string value used in snippet text edits.
 */
@Serializable
public data class StringValue(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val kind: String = "snippet",
    val value: String,
)

/**
 * Serializer for [DocumentTextEdit]. The union member is selected by the JSON keys of the object;
 * anything that is not an object, has neither `snippet` nor `newText`, or has both is rejected.
 */
public object DocumentTextEditSerializer : KSerializer<DocumentTextEdit> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DocumentTextEdit")

    override fun serialize(
        encoder: Encoder,
        value: DocumentTextEdit,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is TextEdit -> jsonEncoder.encodeSerializableValue(TextEdit.serializer(), value)
            is AnnotatedTextEdit -> jsonEncoder.encodeSerializableValue(AnnotatedTextEdit.serializer(), value)
            is SnippetTextEdit -> jsonEncoder.encodeSerializableValue(SnippetTextEdit.serializer(), value)
        }
    }

    override fun deserialize(decoder: Decoder): DocumentTextEdit {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        if (element !is JsonObject) {
            throw SerializationException("Invalid DocumentTextEdit, expected an object: $element")
        }
        val hasSnippet = element.containsKey("snippet")
        val hasNewText = element.containsKey("newText")
        return when {
            hasSnippet && !hasNewText ->
                jsonDecoder.json.decodeFromJsonElement(SnippetTextEdit.serializer(), element)
            hasNewText && !hasSnippet && element.containsKey("annotationId") ->
                jsonDecoder.json.decodeFromJsonElement(AnnotatedTextEdit.serializer(), element)
            hasNewText && !hasSnippet ->
                jsonDecoder.json.decodeFromJsonElement(TextEdit.serializer(), element)
            else -> throw SerializationException("Invalid DocumentTextEdit, expected newText or snippet: $element")
        }
    }
}

/**
 * Describes textual changes on a single text document.
 */
@Serializable
public data class TextDocumentEdit(
    /**
     * The text document to change.
     */
    val textDocument: OptionalVersionedTextDocumentIdentifier,
    /**
     * The edits to be applied. [AnnotatedTextEdit] is guarded by the client capability
     * `changeAnnotationSupport` and [SnippetTextEdit] by `snippetEditSupport`.
     */
    val edits: List<DocumentTextEdit>,
)

/**
 * A set of predefined position encoding kinds.
 */
@Serializable
@JvmInline
public value class PositionEncodingKind(
    public val value: String,
) {
    public companion object {
        /**
         * Character offsets count UTF-8 code units (bytes).
         */
        public val UTF8: PositionEncodingKind = PositionEncodingKind("utf-8")

        /**
         * Character offsets count UTF-16 code units. This is the default and must always be supported.
         */
        public val UTF16: PositionEncodingKind = PositionEncodingKind("utf-16")

        /**
         * Character offsets count UTF-32 code units (Unicode code points).
         */
        public val UTF32: PositionEncodingKind = PositionEncodingKind("utf-32")
    }
}

/**
 * Serializer for `Either<String, MarkupContent>` as used in LSP spec for fields
 * like `tooltip` and `documentation` that can be either a plain string or a MarkupContent object.
 *
 * On serialization: if Left (String), writes a JsonPrimitive; if Right (MarkupContent), serializes as object.
 * On deserialization: checks if the element is a JsonPrimitive (string) or a JsonObject (MarkupContent).
 */
public object StringOrMarkupContentSerializer : KSerializer<Either<String, MarkupContent>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Either<String, MarkupContent>")

    override fun serialize(
        encoder: Encoder,
        value: Either<String, MarkupContent>,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is Either.Left -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is Either.Right -> jsonEncoder.encodeSerializableValue(MarkupContent.serializer(), value.value)
        }
    }

    override fun deserialize(decoder: Decoder): Either<String, MarkupContent> {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return if (element is JsonPrimitive && element.isString) {
            Either.Left(element.content)
        } else {
            Either.Right(jsonDecoder.json.decodeFromJsonElement(MarkupContent.serializer(), element))
        }
    }
}

/**
 * Serializer for nullable `Either<String, MarkupContent>?`.
 * Handles null, plain string, or MarkupContent object.
 */
public object NullableStringOrMarkupContentSerializer : KSerializer<Either<String, MarkupContent>?> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Either<String, MarkupContent>?")

    override fun serialize(
        encoder: Encoder,
        value: Either<String, MarkupContent>?,
    ) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            StringOrMarkupContentSerializer.serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): Either<String, MarkupContent>? =
        if (decoder.decodeNotNullMark()) {
            StringOrMarkupContentSerializer.deserialize(decoder)
        } else {
            decoder.decodeNull()
        }
}
