package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Generic serializer for LSP capability fields that can be either a boolean or an options object.
 * Per LSP spec, many ServerCapabilities fields accept `boolean | XxxOptions`.
 * - `true` or options object means the capability is supported
 * - `false` or absent means not supported
 */
public open class BooleanOrSerializer<T>(
    private val optionsSerializer: KSerializer<T>,
) : KSerializer<Either<Boolean, T>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(
            "BooleanOr<${optionsSerializer.descriptor.serialName}>",
        )

    override fun serialize(
        encoder: Encoder,
        value: Either<Boolean, T>,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is Either.Left -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is Either.Right -> jsonEncoder.encodeSerializableValue(optionsSerializer, value.value)
        }
    }

    override fun deserialize(decoder: Decoder): Either<Boolean, T> {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element is JsonPrimitive && element.booleanOrNull != null ->
                Either.Left(element.booleanOrNull!!)
            else ->
                Either.Right(jsonDecoder.json.decodeFromJsonElement(optionsSerializer, element))
        }
    }
}

/**
 * Nullable variant of [BooleanOrSerializer].
 */
public open class NullableBooleanOrSerializer<T>(
    private val inner: BooleanOrSerializer<T>,
) : KSerializer<Either<Boolean, T>?> {
    override val descriptor: SerialDescriptor = inner.descriptor

    override fun serialize(
        encoder: Encoder,
        value: Either<Boolean, T>?,
    ) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            inner.serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): Either<Boolean, T>? =
        if (decoder.decodeNotNullMark()) {
            inner.deserialize(decoder)
        } else {
            decoder.decodeNull()
        }
}
