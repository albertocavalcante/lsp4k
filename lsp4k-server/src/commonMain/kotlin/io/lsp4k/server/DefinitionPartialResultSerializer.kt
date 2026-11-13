package io.lsp4k.server

import io.lsp4k.protocol.DefinitionResult
import io.lsp4k.protocol.DefinitionResultSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
