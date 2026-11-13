package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

/**
 * A definition response preserving its single-object or array wire shape.
 * Empty location and link arrays have identical JSON; decoding `[]` yields [Locations].
 * Servers should return links only when the client advertises definition link support.
 */
@Serializable(with = DefinitionResultSerializer::class)
public sealed interface DefinitionResult {
    /** One location encoded as a JSON object. */
    public data class Single(
        public val location: Location,
    ) : DefinitionResult

    /** Locations encoded as a JSON array. */
    public data class Locations(
        public val locations: List<Location>,
    ) : DefinitionResult

    /** Location links encoded as a JSON array. */
    public data class Links(
        public val links: List<LocationLink>,
    ) : DefinitionResult
}

/**
 * Distinguishes locations by required `uri` and `range` keys and links by required
 * `targetUri`, `targetRange` and `targetSelectionRange` keys. Objects containing
 * keys from both families and mixed or malformed arrays are rejected.
 */
public object DefinitionResultSerializer : KSerializer<DefinitionResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DefinitionResult")

    override fun serialize(
        encoder: Encoder,
        value: DefinitionResult,
    ) {
        val json = encoder as JsonEncoder
        when (value) {
            is DefinitionResult.Single -> json.encodeSerializableValue(Location.serializer(), value.location)
            is DefinitionResult.Locations -> json.encodeSerializableValue(ListSerializer(Location.serializer()), value.locations)
            is DefinitionResult.Links -> json.encodeSerializableValue(ListSerializer(LocationLink.serializer()), value.links)
        }
    }

    override fun deserialize(decoder: Decoder): DefinitionResult {
        val json = decoder as JsonDecoder
        return when (val element = json.decodeJsonElement()) {
            is JsonObject -> {
                if (family(element) != Family.LOCATION) throw SerializationException("A single definition must be a Location")
                DefinitionResult.Single(json.json.decodeFromJsonElement(Location.serializer(), element))
            }
            is JsonArray -> {
                if (element.isEmpty()) return DefinitionResult.Locations(emptyList())
                val families =
                    element.map { value ->
                        family(value as? JsonObject ?: throw SerializationException("Definition array elements must be objects"))
                    }
                if (families.any { it != families.first() }) throw SerializationException("Definition array cannot mix locations and links")
                when (families.first()) {
                    Family.LOCATION ->
                        DefinitionResult.Locations(
                            json.json.decodeFromJsonElement(ListSerializer(Location.serializer()), element),
                        )
                    Family.LINK ->
                        DefinitionResult.Links(
                            json.json.decodeFromJsonElement(ListSerializer(LocationLink.serializer()), element),
                        )
                }
            }
            else -> throw SerializationException("Definition result must be a Location object or an array")
        }
    }

    private fun family(value: JsonObject): Family {
        val locationKeys = setOf("uri", "range")
        val linkKeys = setOf("targetUri", "targetRange", "targetSelectionRange", "originSelectionRange")
        val hasLocation = locationKeys.any(value::containsKey)
        val hasLink = linkKeys.any(value::containsKey)
        return when {
            hasLocation && !hasLink && locationKeys.all(value::containsKey) -> Family.LOCATION
            hasLink && !hasLocation && listOf("targetUri", "targetRange", "targetSelectionRange").all(value::containsKey) -> Family.LINK
            else -> throw SerializationException("Definition object has missing or ambiguous location keys")
        }
    }

    private enum class Family { LOCATION, LINK }
}

/** `textDocument/declaration` result: `Location | Location[] | LocationLink[] | null`, shaped like [DefinitionResult]. */
public typealias DeclarationResult = DefinitionResult

/** `textDocument/typeDefinition` result: `Location | Location[] | LocationLink[] | null`, shaped like [DefinitionResult]. */
public typealias TypeDefinitionResult = DefinitionResult

/** `textDocument/implementation` result: `Location | Location[] | LocationLink[] | null`, shaped like [DefinitionResult]. */
public typealias ImplementationResult = DefinitionResult
