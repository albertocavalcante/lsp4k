package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement

/**
 * Serializer for a JSON-RPC request id carried as a [JsonElement]: an integer number or a string.
 * Any other JSON value, including `null`, a boolean, a fractional number, an array or an object, is rejected.
 */
public object RequestIdSerializer : KSerializer<JsonElement> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: JsonElement,
    ) {
        JsonElement.serializer().serialize(encoder, requireRequestId(value))
    }

    override fun deserialize(decoder: Decoder): JsonElement = requireRequestId(JsonElement.serializer().deserialize(decoder))

    private fun requireRequestId(element: JsonElement): JsonElement =
        if (element.isInteger() || element.isStringLiteral()) {
            element
        } else {
            throw SerializationException("Invalid request id: $element")
        }
}

/**
 * Parameters for the $/cancelRequest notification.
 *
 * The id field is a JsonElement because it can be either a string or a number.
 */
@Serializable
public data class CancelParams(
    /**
     * The request id to cancel.
     */
    @Serializable(with = RequestIdSerializer::class)
    val id: JsonElement,
)

/**
 * Parameters for the $/setTrace notification.
 */
@Serializable
public data class SetTraceParams(
    /**
     * The new value that should be assigned to the trace setting.
     */
    val value: TraceValue,
)

/**
 * Parameters for the $/logTrace notification.
 */
@Serializable
public data class LogTraceParams(
    /**
     * The message to be logged.
     */
    val message: String,
    /**
     * Additional information that can be computed if the `trace` configuration
     * is set to `'verbose'`.
     */
    val verbose: String? = null,
)

/**
 * Parameters for the `initialized` notification. They carry no properties and always encode
 * as an empty JSON object, never as `null` or an omitted `params`.
 */
@Serializable
public object InitializedParams
