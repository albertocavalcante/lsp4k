package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * Parameters for textDocument/inlayHint request.
 */
@Serializable
public data class InlayHintParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The visible document range for which inlay hints should be computed.
     */
    val range: Range,
    /**
     * An optional token that a server can use to report work done progress.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
)

/**
 * Inlay hint kinds.
 */
@Serializable(with = InlayHintKindSerializer::class)
public enum class InlayHintKind(
    public val value: Int,
) {
    /**
     * An inlay hint that for a type annotation.
     */
    Type(1),

    /**
     * An inlay hint that is for a parameter.
     */
    Parameter(2),

    ;

    public companion object {
        public fun fromValue(value: Int): InlayHintKind =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown InlayHintKind: $value")
    }
}

/**
 * Serializer for InlayHintKind that encodes/decodes as integer.
 */
public object InlayHintKindSerializer : IntEnumSerializer<InlayHintKind>(
    "InlayHintKind",
    InlayHintKind::fromValue,
    { it.value },
)

/**
 * An inlay hint label part allows for interactive and composite labels
 * of inlay hints.
 */
@Serializable
public data class InlayHintLabelPart(
    /**
     * The value of this label part.
     */
    val value: String,
    /**
     * The tooltip text when you hover over this label part.
     * Can be a plain string or a MarkupContent object.
     */
    @Serializable(with = NullableStringOrMarkupContentSerializer::class)
    val tooltip: Either<String, MarkupContent>? = null,
    /**
     * An optional source code location that represents this label part.
     */
    val location: Location? = null,
    /**
     * An optional command for this label part.
     */
    val command: Command? = null,
)

/**
 * Serializer for `Either<String, List<InlayHintLabelPart>>` as used in the LSP spec
 * for `InlayHint.label`, which can be either a plain string or an array of label parts.
 *
 * On serialization: if Left (String), writes a JsonPrimitive; if Right (list), serializes as array.
 * On deserialization: checks if the element is a JsonPrimitive (string) or a JsonArray (list of parts).
 */
public object InlayHintLabelSerializer : KSerializer<Either<String, List<InlayHintLabelPart>>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Either<String, List<InlayHintLabelPart>>")

    override fun serialize(
        encoder: Encoder,
        value: Either<String, List<InlayHintLabelPart>>,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is Either.Left -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is Either.Right ->
                jsonEncoder.encodeSerializableValue(
                    ListSerializer(InlayHintLabelPart.serializer()),
                    value.value,
                )
        }
    }

    override fun deserialize(decoder: Decoder): Either<String, List<InlayHintLabelPart>> {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return if (element is JsonPrimitive && element.isString) {
            Either.Left(element.content)
        } else if (element is JsonArray) {
            Either.Right(
                jsonDecoder.json.decodeFromJsonElement(
                    ListSerializer(InlayHintLabelPart.serializer()),
                    element,
                ),
            )
        } else {
            error("InlayHint.label must be a string or an array of InlayHintLabelPart")
        }
    }
}

/**
 * Inlay hint information.
 */
@Serializable
public data class InlayHint(
    /**
     * The position of this hint.
     */
    val position: Position,
    /**
     * The label of this hint. A human readable string or an array of
     * InlayHintLabelPart label parts.
     */
    @Serializable(with = InlayHintLabelSerializer::class)
    val label: Either<String, List<InlayHintLabelPart>>,
    /**
     * The kind of this hint. Can be omitted in which case the client should
     * fall back to a reasonable default.
     */
    val kind: InlayHintKind? = null,
    /**
     * Optional text edits that are performed when accepting this inlay hint.
     */
    val textEdits: List<TextEdit>? = null,
    /**
     * The tooltip text when you hover over this item.
     * Can be a plain string or a MarkupContent object.
     */
    @Serializable(with = NullableStringOrMarkupContentSerializer::class)
    val tooltip: Either<String, MarkupContent>? = null,
    /**
     * Render padding before the hint.
     */
    val paddingLeft: Boolean? = null,
    /**
     * Render padding after the hint.
     */
    val paddingRight: Boolean? = null,
    /**
     * A data entry field that is preserved on an inlay hint between a
     * `textDocument/inlayHint` and a `inlayHint/resolve` request.
     */
    val data: JsonElement? = null,
)
