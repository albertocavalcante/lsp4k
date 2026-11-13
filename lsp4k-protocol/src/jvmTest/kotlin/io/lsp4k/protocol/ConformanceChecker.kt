package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.lang.reflect.Modifier

/**
 * The `Json` configuration lsp4k servers and clients use on the wire: the flags of jsonrpc4k
 * `JsonRpcCodec.defaultJson` (jsonrpc4k-core `Codec.kt`), copied so the checker states what it relies on.
 * [MetaModelConformanceTest] fails when the copy and jsonrpc4k diverge.
 */
internal val conformanceJson: Json =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        isLenient = true
        coerceInputValues = true
    }

/** How a meta-model structure is represented in Kotlin. */
internal sealed interface StructureBinding {
    val serializer: KSerializer<*>

    /** A Kotlin class for exactly this structure: property names, optionality, nullability and round trips are checked. */
    class Exact(
        override val serializer: KSerializer<*>,
    ) : StructureBinding

    /**
     * One Kotlin class for several structures that are the members of a union alias, carrying the union of their
     * properties. Only round trips are checked, because the class makes a member's required property optional.
     */
    class Merged(
        override val serializer: KSerializer<*>,
    ) : StructureBinding
}

/** How a meta-model enumeration is represented in Kotlin. */
internal sealed interface EnumerationBinding {
    val serializer: KSerializer<*>

    /** The wire values of the Kotlin constants. */
    fun wireValues(): Set<JsonElement>

    /** A Kotlin enum class: every entry is encoded with [serializer]. */
    class Closed(
        override val serializer: KSerializer<*>,
        private val entries: List<Any>,
    ) : EnumerationBinding {
        override fun wireValues(): Set<JsonElement> = entries.map { encode(serializer, it) }.toSet()
    }

    /**
     * An open value set: the `String` or `Int` constants declared by [holder], a constants object or a value class
     * whose companion declares the constants. [serializer] must also accept values outside that set.
     */
    class Open(
        override val serializer: KSerializer<*>,
        private val holder: Class<*>,
    ) : EnumerationBinding {
        override fun wireValues(): Set<JsonElement> =
            holder.declaredFields
                .filter { Modifier.isStatic(it.modifiers) && !it.isSynthetic && it.trySetAccessible() }
                .mapNotNull { constant(it.get(null)) }
                .toSet()

        private fun constant(value: Any?): JsonElement? =
            when (value) {
                is String -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                else -> null
            }
    }
}

/** The direction of a request or notification, as `messageDirection` spells it. */
internal enum class MessageDirection(
    val wire: String,
) {
    ClientToServer("clientToServer"),
    ServerToClient("serverToClient"),
    Both("both"),
}

/**
 * How lsp4k types one request or notification: its direction, the serializers of each payload the meta model
 * declares and the method used to register it dynamically when that differs from the method itself.
 */
internal data class MessageBinding(
    val direction: MessageDirection,
    val params: KSerializer<*>? = null,
    val result: KSerializer<*>? = null,
    val partialResult: KSerializer<*>? = null,
    val registrationOptions: KSerializer<*>? = null,
    val registrationMethod: String? = null,
    val errorData: KSerializer<*>? = null,
) {
    fun payload(key: String): KSerializer<*>? =
        when (key) {
            "params" -> params
            "result" -> result
            "partialResult" -> partialResult
            "registrationOptions" -> registrationOptions
            "errorData" -> errorData
            else -> error("Unknown payload $key")
        }
}

/** The explicit table from meta-model names to lsp4k serializers that [ConformanceChecker] verifies. */
internal data class ConformanceRegistry(
    val structures: Map<String, StructureBinding>,
    val enumerations: Map<String, EnumerationBinding>,
    val typeAliases: Map<String, KSerializer<*>>,
    val messages: Map<String, MessageBinding>,
    val methodConstants: Set<String>,
)

/** A violation key and, for diagnostics, what went wrong. */
internal data class Violation(
    val key: String,
    val detail: String,
)

/**
 * Compares the meta model with the lsp4k serializers of a [ConformanceRegistry] and reports stable violation keys.
 *
 * - `structure:S`, `enum:E`, `alias:A`, `message:m`: the meta model declares an item the registry does not map.
 *   Abstract structures (only used in `extends` and `mixins`) need no mapping.
 * - `registry:K:name`: a registry row names an item the meta model does not declare.
 * - `property:S.p:missing|extra|optionality|nullability`: an [StructureBinding.Exact] descriptor differs from the
 *   flattened properties of `S`. A required property must have no Kotlin default (a string literal may have one
 *   when it is always encoded under [conformanceJson]); an optional property must have one. A property whose type
 *   admits `null` must be nullable, and a required property that does not must not be.
 * - `enumValue:E=v`, `enumExtra:E=v`, `enumCustom:E`: a declared value has no Kotlin constant, a Kotlin constant
 *   is not declared, or an enumeration that supports custom values rejects one.
 * - `method:m`, `direction:m`, `registrationMethod:m`: no `LspMethods` constant, a different direction, or a
 *   different registration method (or one without an `LspMethods` constant).
 * - `P:m`, `P:m:extra`, where `P` is `params`, `result`, `partialResult`, `registrationOptions` or `errorData`: a
 *   declared payload without a serializer, or a serializer for a payload that is not declared.
 * - `roundtrip:<scope><variant>`: a sample did not decode and encode back to itself under [conformanceJson]; scopes
 *   are `structure:S`, `alias:A` and `P:m`, and variants are named by [MetaModelSamples].
 */
internal class ConformanceChecker(
    private val meta: MetaModel,
    private val registry: ConformanceRegistry,
) {
    private val samples = MetaModelSamples(meta)
    private val violations = sortedMapOf<String, String>()

    fun check(): List<Violation> {
        violations.clear()
        meta.structures.keys.forEach(::checkStructure)
        meta.enumerations.keys.forEach(::checkEnumeration)
        meta.typeAliases.keys.forEach(::checkAlias)
        meta.messages.values.forEach(::checkMessage)
        checkRegistryRows()
        return violations.map { (key, detail) -> Violation(key, detail) }
    }

    private fun report(
        key: String,
        detail: String,
    ) {
        violations.putIfAbsent(key, detail)
    }

    private fun checkRegistryRows() {
        (registry.structures.keys - meta.structures.keys).forEach { report("registry:structure:$it", "not a meta-model structure") }
        (registry.enumerations.keys - meta.enumerations.keys).forEach { report("registry:enum:$it", "not a meta-model enumeration") }
        (registry.typeAliases.keys - meta.typeAliases.keys).forEach { report("registry:alias:$it", "not a meta-model type alias") }
        (registry.messages.keys - meta.messages.keys).forEach { report("registry:message:$it", "not a meta-model method") }
    }

    private fun checkStructure(name: String) {
        when (val binding = registry.structures[name]) {
            null -> if (name !in meta.abstractStructures) report("structure:$name", "no registry row")
            is StructureBinding.Exact -> {
                checkProperties(name, binding.serializer)
                checkRoundTrips("structure:$name", binding.serializer, samples.structureVariants(name))
            }
            is StructureBinding.Merged -> checkRoundTrips("structure:$name", binding.serializer, samples.structureVariants(name))
        }
    }

    private fun checkProperties(
        name: String,
        serializer: KSerializer<*>,
    ) {
        val descriptor = serializer.descriptor
        val elements = (0 until descriptor.elementsCount).associateBy(descriptor::getElementName)
        val properties = meta.properties(name)
        val encodedRequired by lazy { runCatching { roundTrip(serializer, samples.required(name)) as? JsonObject }.getOrNull() }
        for ((property, spec) in properties) {
            val index = elements[property]
            val problem =
                if (index == null) {
                    "missing"
                } else {
                    propertyProblem(spec, descriptor.getElementDescriptor(index), descriptor.isElementOptional(index)) {
                        encodedRequired?.containsKey(property) == true
                    }
                }
            problem?.let { report("property:$name.$property:$it", "${descriptor.serialName}.$property: $it") }
        }
        for (extra in elements.keys - properties.keys) {
            report("property:$name.$extra:extra", "${descriptor.serialName}.$extra is not in the meta model")
        }
    }

    private fun propertyProblem(
        spec: JsonObject,
        element: SerialDescriptor,
        hasDefault: Boolean,
        alwaysEncoded: () -> Boolean,
    ): String? {
        val type = spec.metaChild("type")
        val required = !spec.isMetaOptional
        val admitsNull = meta.admitsNull(type)
        val alwaysEncodedLiteral = type.metaKind == "stringLiteral" && alwaysEncoded()
        return when {
            !required && !hasDefault -> "optionality"
            required && hasDefault && !alwaysEncodedLiteral -> "optionality"
            admitsNull && !element.canHoldNull() -> "nullability"
            required && !admitsNull && element.isNullable -> "nullability"
            else -> null
        }
    }

    private fun checkEnumeration(name: String) {
        val enumeration = meta.enumerations.getValue(name)
        val binding = registry.enumerations[name]
        if (binding == null) {
            report("enum:$name", "no registry row")
            return
        }
        val declared = enumeration.metaChildren("values").map { it.getValue("value") }.toSet()
        val constants = binding.wireValues()
        (declared - constants).forEach { report("enumValue:$name=$it", "no Kotlin constant encodes $it") }
        (constants - declared).forEach { report("enumExtra:$name=$it", "a Kotlin constant encodes $it, which $name does not declare") }
        val custom = samples.customEnumValue(enumeration)
        if (enumeration.supportsCustomValues && !survives(binding.serializer, custom)) {
            report("enumCustom:$name", "${binding.serializer.descriptor.serialName} rejects $custom")
        }
    }

    private fun checkAlias(name: String) {
        val serializer = registry.typeAliases[name]
        if (serializer == null) {
            report("alias:$name", "no registry row")
            return
        }
        checkRoundTrips("alias:$name", serializer, samples.variants(meta.typeAliases.getValue(name).metaChild("type")))
    }

    private fun checkMessage(message: JsonObject) {
        val method = message.metaMethod
        if (method !in registry.methodConstants) report("method:$method", "no LspMethods constant")
        val binding = registry.messages[method]
        if (binding == null) {
            report("message:$method", "no registry row")
            return
        }
        val direction = message.getValue("messageDirection").jsonPrimitive.content
        if (binding.direction.wire != direction) {
            report("direction:$method", "registry says ${binding.direction.wire}, meta model says $direction")
        }
        val registrationMethod = message["registrationMethod"]?.jsonPrimitive?.content
        val knownConstant = registrationMethod == null || registrationMethod in registry.methodConstants
        if (binding.registrationMethod != registrationMethod || !knownConstant) {
            report("registrationMethod:$method", "registry says ${binding.registrationMethod}, meta model says $registrationMethod")
        }
        MetaModel.PAYLOADS.forEach { key -> checkPayload(method, key, message[key] as? JsonObject, binding.payload(key)) }
    }

    private fun checkPayload(
        method: String,
        key: String,
        type: JsonObject?,
        serializer: KSerializer<*>?,
    ) {
        when {
            type == null && serializer != null -> report("$key:$method:extra", "the meta model declares no $key")
            type == null -> Unit
            serializer == null -> report("$key:$method", "no serializer for ${samples.describe(type)}")
            else -> checkRoundTrips("$key:$method", serializer, samples.variants(type))
        }
    }

    private fun checkRoundTrips(
        scope: String,
        serializer: KSerializer<*>,
        variants: Map<String, JsonElement>,
    ) {
        for ((variant, sample) in variants) {
            val outcome = runCatching { roundTrip(serializer, sample) }
            if (outcome.getOrNull() != sample) {
                val actual = outcome.fold({ "encoded $it" }, { "failed with ${it::class.simpleName}: ${it.message}" })
                report("roundtrip:$scope$variant", "decoded $sample and $actual")
            }
        }
    }

    private fun survives(
        serializer: KSerializer<*>,
        sample: JsonElement,
    ): Boolean = runCatching { roundTrip(serializer, sample) }.getOrNull() == sample

    private fun SerialDescriptor.canHoldNull(): Boolean = isNullable || serialName == JSON_ELEMENT

    private companion object {
        val JSON_ELEMENT: String = JsonElement.serializer().descriptor.serialName
    }
}

/** Decodes [sample] with [serializer] under [conformanceJson] and encodes the result again. */
internal fun roundTrip(
    serializer: KSerializer<*>,
    sample: JsonElement,
): JsonElement {
    @Suppress("UNCHECKED_CAST")
    val typed = serializer as KSerializer<Any?>
    return conformanceJson.encodeToJsonElement(typed, conformanceJson.decodeFromJsonElement(typed, sample))
}

private fun encode(
    serializer: KSerializer<*>,
    value: Any,
): JsonElement {
    @Suppress("UNCHECKED_CAST")
    val typed = serializer as KSerializer<Any?>
    return conformanceJson.encodeToJsonElement(typed, value)
}
