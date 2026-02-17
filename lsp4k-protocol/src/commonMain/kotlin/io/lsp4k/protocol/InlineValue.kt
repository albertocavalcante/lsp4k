package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject

/**
 * Provide inline value as text.
 *
 * @since 3.17.0
 */
@Serializable
public data class InlineValueText(
    /**
     * The document range for which the inline value applies.
     */
    val range: Range,
    /**
     * The text of the inline value.
     */
    val text: String,
) : InlineValue

/**
 * Provide inline value through a variable lookup.
 *
 * If only a range is specified, the variable name will be extracted from
 * the underlying document.
 *
 * An optional variable name can be used to override the extracted name.
 *
 * @since 3.17.0
 */
@Serializable
public data class InlineValueVariableLookup(
    /**
     * The document range for which the inline value applies.
     * The range is used to extract the variable name from the underlying document.
     */
    val range: Range,
    /**
     * If specified, the name of the variable to look up.
     */
    val variableName: String? = null,
    /**
     * How to perform the lookup.
     */
    val caseSensitiveLookup: Boolean,
) : InlineValue

/**
 * Provide an inline value through an expression evaluation.
 *
 * If only a range is specified, the expression will be extracted from the
 * underlying document.
 *
 * An optional expression can be used to override the extracted expression.
 *
 * @since 3.17.0
 */
@Serializable
public data class InlineValueEvaluatableExpression(
    /**
     * The document range for which the inline value applies.
     * The range is used to extract the evaluatable expression from the
     * underlying document.
     */
    val range: Range,
    /**
     * If specified, the expression overrides the extracted expression.
     */
    val expression: String? = null,
) : InlineValue

/**
 * Inline value information can be provided by different means:
 * - directly as a text value (class InlineValueText).
 * - as a name to use for a variable lookup (class InlineValueVariableLookup)
 * - as an evaluatable expression (class InlineValueEvaluatableExpression)
 *
 * The InlineValue types combines all inline value types into one type.
 *
 * @since 3.17.0
 */
@Serializable(with = InlineValueSerializer::class)
public sealed interface InlineValue

/**
 * Serializer for InlineValue that discriminates based on the presence of specific fields:
 * - "text" field -> InlineValueText
 * - "caseSensitiveLookup" field -> InlineValueVariableLookup
 * - otherwise -> InlineValueEvaluatableExpression
 */
public object InlineValueSerializer : KSerializer<InlineValue> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("InlineValue")

    override fun serialize(
        encoder: Encoder,
        value: InlineValue,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is InlineValueText ->
                jsonEncoder.encodeSerializableValue(InlineValueText.serializer(), value)
            is InlineValueVariableLookup ->
                jsonEncoder.encodeSerializableValue(InlineValueVariableLookup.serializer(), value)
            is InlineValueEvaluatableExpression ->
                jsonEncoder.encodeSerializableValue(InlineValueEvaluatableExpression.serializer(), value)
        }
    }

    override fun deserialize(decoder: Decoder): InlineValue {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        val jsonObject = element.jsonObject

        return when {
            // InlineValueText has a required "text" field
            "text" in jsonObject ->
                jsonDecoder.json.decodeFromJsonElement(InlineValueText.serializer(), element)
            // InlineValueVariableLookup has a required "caseSensitiveLookup" field
            "caseSensitiveLookup" in jsonObject ->
                jsonDecoder.json.decodeFromJsonElement(InlineValueVariableLookup.serializer(), element)
            // Default to InlineValueEvaluatableExpression (has only range and optional expression)
            else ->
                jsonDecoder.json.decodeFromJsonElement(InlineValueEvaluatableExpression.serializer(), element)
        }
    }
}

/**
 * Provide information about the context in which an inline value was requested.
 *
 * @since 3.17.0
 */
@Serializable
public data class InlineValueContext(
    /**
     * The stack frame (as a DAP Id) where the execution has stopped.
     */
    val frameId: Int,
    /**
     * The document range where execution has stopped.
     * Typically the end position of the range denotes the line where the
     * inline values are shown.
     */
    val stoppedLocation: Range,
)

/**
 * A parameter literal used in inline value requests.
 *
 * @since 3.17.0
 */
@Serializable
public data class InlineValueParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The document range for which inline values should be computed.
     */
    val range: Range,
    /**
     * Additional information about the context in which inline values were
     * requested.
     */
    val context: InlineValueContext,
)
