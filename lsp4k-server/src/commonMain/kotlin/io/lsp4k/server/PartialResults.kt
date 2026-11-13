package io.lsp4k.server

import io.lsp4k.protocol.DefinitionResult
import io.lsp4k.protocol.DefinitionResultSerializer
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.SemanticTokensDeltaPartialResult
import io.lsp4k.protocol.SemanticTokensPartialResult
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject

/**
 * A partial result of `textDocument/semanticTokens/full/delta`: a full token chunk (`Either.Left`) or an edit
 * chunk (`Either.Right`).
 */
public typealias SemanticTokensDeltaPartial = Either<SemanticTokensPartialResult, SemanticTokensDeltaPartialResult>

/**
 * Encodes navigation partial results (`Location[] | LocationLink[]`). A [DefinitionResult.Single] chunk is sent
 * as a one-element location array, because a partial result always appends array items.
 */
internal object DefinitionPartialResultSerializer : KSerializer<DefinitionResult> {
    override val descriptor: SerialDescriptor = DefinitionResultSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: DefinitionResult,
    ) {
        val chunk = if (value is DefinitionResult.Single) DefinitionResult.Locations(listOf(value.location)) else value
        DefinitionResultSerializer.serialize(encoder, chunk)
    }

    override fun deserialize(decoder: Decoder): DefinitionResult = DefinitionResultSerializer.deserialize(decoder)
}

/** Encodes [SemanticTokensDeltaPartial] by its `data` or `edits` key. */
internal object SemanticTokensPartialSerializer : KSerializer<SemanticTokensDeltaPartial> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SemanticTokensDeltaPartial")

    override fun serialize(
        encoder: Encoder,
        value: SemanticTokensDeltaPartial,
    ) {
        when (value) {
            is Either.Left -> encoder.encodeSerializableValue(SemanticTokensPartialResult.serializer(), value.value)
            is Either.Right -> encoder.encodeSerializableValue(SemanticTokensDeltaPartialResult.serializer(), value.value)
        }
    }

    override fun deserialize(decoder: Decoder): SemanticTokensDeltaPartial {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Semantic token partial results require JSON")
        val element = input.decodeJsonElement()
        val keys = (element as? JsonObject)?.keys.orEmpty()
        val hasData = "data" in keys
        val hasEdits = "edits" in keys
        if (hasData == hasEdits) throw SerializationException("A semantic token partial result needs exactly one of data or edits")
        return if (hasData) {
            Either.left(input.json.decodeFromJsonElement(SemanticTokensPartialResult.serializer(), element))
        } else {
            Either.right(input.json.decodeFromJsonElement(SemanticTokensDeltaPartialResult.serializer(), element))
        }
    }
}
