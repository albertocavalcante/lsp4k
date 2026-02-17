package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

    override fun serialize(encoder: Encoder, value: E): Unit = encoder.encodeInt(toValue(value))

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

    override fun serialize(encoder: Encoder, value: ProgressToken) {
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

    override fun serialize(encoder: Encoder, value: ProgressToken?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            ProgressTokenSerializer.serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): ProgressToken? {
        return if (decoder.decodeNotNullMark()) {
            ProgressTokenSerializer.deserialize(decoder)
        } else {
            decoder.decodeNull()
        }
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
    val version: Int? = null,
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
)

/**
 * A special text edit with an additional change annotation.
 */
@Serializable
public data class AnnotatedTextEdit(
    val range: Range,
    val newText: String,
    val annotationId: String,
)

/**
 * A snippet text edit. The snippet can contain placeholders and tab stops
 * following the TextMate snippet syntax.
 *
 * @since 3.18.0 (proposed)
 */
@Serializable
public data class SnippetTextEdit(
    val range: Range,
    val snippet: StringValue,
    val annotationId: String? = null,
)

/**
 * A string value used in snippet text edits.
 */
@Serializable
public data class StringValue(
    val kind: String = "snippet",
    val value: String,
)

/**
 * A text edit that is either a plain [TextEdit] or an [AnnotatedTextEdit].
 * Per LSP spec, `TextDocumentEdit.edits` can contain both types.
 */
public typealias TextOrAnnotatedEdit = Either<TextEdit, AnnotatedTextEdit>

/**
 * Serializer for [TextOrAnnotatedEdit] that discriminates based on the
 * presence of the `annotationId` field: if present, deserializes as
 * [AnnotatedTextEdit] (Right); otherwise as [TextEdit] (Left).
 */
public object TextOrAnnotatedEditSerializer : KSerializer<TextOrAnnotatedEdit> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("TextOrAnnotatedEdit")

    override fun serialize(encoder: Encoder, value: TextOrAnnotatedEdit) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is Either.Left -> jsonEncoder.encodeSerializableValue(TextEdit.serializer(), value.value)
            is Either.Right -> jsonEncoder.encodeSerializableValue(AnnotatedTextEdit.serializer(), value.value)
        }
    }

    override fun deserialize(decoder: Decoder): TextOrAnnotatedEdit {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return if (element is JsonObject && element.containsKey("annotationId")) {
            Either.Right(jsonDecoder.json.decodeFromJsonElement(AnnotatedTextEdit.serializer(), element))
        } else {
            Either.Left(jsonDecoder.json.decodeFromJsonElement(TextEdit.serializer(), element))
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
     * The edits to be applied.
     * Per LSP spec, each edit can be either a [TextEdit] or an [AnnotatedTextEdit].
     */
    val edits: List<@Serializable(with = TextOrAnnotatedEditSerializer::class) TextOrAnnotatedEdit>,
)

/**
 * A set of predefined position encoding kinds.
 */
@Serializable
public enum class PositionEncodingKind {
    @SerialName("utf-32") UTF32,
    @SerialName("utf-16") UTF16,
    @SerialName("utf-8") UTF8,
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

    override fun deserialize(decoder: Decoder): Either<String, MarkupContent>? {
        return if (decoder.decodeNotNullMark()) {
            StringOrMarkupContentSerializer.deserialize(decoder)
        } else {
            decoder.decodeNull()
        }
    }
}
