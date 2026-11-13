package io.lsp4k.protocol

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Builds deterministic wire samples from meta-model types: the smallest valid value of a type, and named variants
 * that put every optional property and every union member on the wire once.
 */
internal class MetaModelSamples(
    private val meta: MetaModel,
) {
    /** The smallest valid value of [type]: required properties only, the first union member, one array element. */
    fun minimal(
        type: JsonObject,
        depth: Int = 0,
    ): JsonElement {
        check(depth <= MAX_DEPTH) { "Sample nesting exceeds $MAX_DEPTH levels at $type" }
        return when (type.metaKind) {
            "base" -> base(type.metaName)
            "reference" -> reference(type.metaName, depth)
            "array" -> JsonArray(if (depth >= MAX_ARRAY_DEPTH) emptyList() else listOf(minimal(type.metaChild("element"), depth + 1)))
            "map" -> JsonObject(mapOf(URI to minimal(type.metaChild("value"), depth + 1)))
            "or" -> minimal(type.metaChildren("items").first { !it.isNullType() }, depth)
            "tuple" -> JsonArray(type.metaChildren("items").map { minimal(it, depth + 1) })
            "literal" -> requiredObject(type.metaChild("value").metaChildren("properties").associateBy { it.metaName }, depth)
            "stringLiteral" -> type.getValue("value")
            else -> error("Unsupported meta type kind ${type.metaKind}")
        }
    }

    /** Samples of a payload or type alias: one per union member (`#member`), otherwise only the minimal value. */
    fun variants(type: JsonObject): Map<String, JsonElement> =
        meta.unionMembers(type)?.associate { "#${describe(it)}" to minimal(it) } ?: mapOf("" to minimal(type))

    /** The structure [name] with its required properties only. */
    fun required(name: String): JsonObject = requiredObject(meta.properties(name), 0)

    /**
     * Samples of the structure [name]: the required properties alone, each optional property on its own
     * (`+property`), each member of a union-typed property (`.property=member`), each declared value of an
     * enumeration-typed property (`.property=value`) and, when that enumeration supports custom values, a value
     * outside the declared set (`.property=custom`).
     */
    fun structureVariants(name: String): Map<String, JsonElement> {
        val required = required(name)
        val result = linkedMapOf<String, JsonElement>("" to required)
        for ((property, spec) in meta.properties(name)) {
            val type = spec.metaChild("type")
            val members = meta.unionMembers(type)
            if (members == null && spec.isMetaOptional) result["+$property"] = JsonObject(required + (property to minimal(type, 1)))
            members?.forEach { result[".$property=${describe(it)}"] = JsonObject(required + (property to minimal(it, 1))) }
            declaredValues(type).forEach { result[".$property=$it"] = JsonObject(required + (property to it)) }
            customValue(type)?.let { result[".$property=custom"] = JsonObject(required + (property to it)) }
        }
        return result
    }

    /** A value of [enumeration]'s base type that is not one of its declared values. */
    fun customEnumValue(enumeration: JsonObject): JsonPrimitive =
        if (enumeration.metaChild("type").metaName == "string") JsonPrimitive(CUSTOM_STRING) else JsonPrimitive(CUSTOM_INTEGER)

    /** A short, stable name for [type], used in violation keys. */
    fun describe(type: JsonObject): String =
        when (type.metaKind) {
            "base", "reference" -> type.metaName
            "array" -> describe(type.metaChild("element")) + "[]"
            "map" -> "{" + describe(type.metaChild("value")) + "}"
            "tuple" -> type.metaChildren("items").joinToString(",", "[", "]", transform = ::describe)
            "stringLiteral" -> type.getValue("value").toString()
            else -> type.metaKind
        }

    /** Every declared value of the enumeration typing [type] (or its array elements), so each one is used once. */
    private fun declaredValues(type: JsonObject): List<JsonElement> =
        when (type.metaKind) {
            "reference" -> meta.enumerations[type.metaName]?.let(::valuesOf).orEmpty()
            "array" -> declaredValues(type.metaChild("element")).map { JsonArray(listOf(it)) }
            else -> emptyList()
        }

    private fun valuesOf(enumeration: JsonObject): List<JsonElement> = enumeration.metaChildren("values").map { it.getValue("value") }

    private fun customValue(type: JsonObject): JsonElement? =
        when (type.metaKind) {
            "reference" -> meta.enumerations[type.metaName]?.takeIf { it.supportsCustomValues }?.let(::customEnumValue)
            "array" -> customValue(type.metaChild("element"))?.let { JsonArray(listOf(it)) }
            else -> null
        }

    private fun requiredObject(
        properties: Map<String, JsonObject>,
        depth: Int,
    ): JsonObject {
        val required = properties.filterValues { !it.isMetaOptional }
        return JsonObject(required.mapValues { (_, spec) -> minimal(spec.metaChild("type"), depth + 1) })
    }

    private fun reference(
        name: String,
        depth: Int,
    ): JsonElement =
        when (name) {
            in meta.structures -> requiredObject(meta.properties(name), depth)
            in meta.enumerations ->
                meta.enumerations
                    .getValue(name)
                    .metaChildren("values")
                    .first()
                    .getValue("value")
            "LSPAny", "LSPObject" -> LSP_OBJECT
            "LSPArray" -> LSP_ARRAY
            in meta.typeAliases -> minimal(meta.typeAliases.getValue(name).metaChild("type"), depth)
            else -> error("Unknown reference $name")
        }

    private fun base(name: String): JsonElement =
        when (name) {
            "string" -> JsonPrimitive("s")
            "integer", "uinteger" -> JsonPrimitive(1)
            "decimal" -> JsonPrimitive(DECIMAL)
            "boolean" -> JsonPrimitive(true)
            "null" -> JsonNull
            "DocumentUri", "URI" -> JsonPrimitive(URI)
            else -> error("Unknown base type $name")
        }

    private companion object {
        const val MAX_DEPTH = 32
        const val MAX_ARRAY_DEPTH = 3
        const val URI = "file:///a"
        const val DECIMAL = 0.5
        const val CUSTOM_STRING = "lsp4k-custom"
        const val CUSTOM_INTEGER = 4242
        val LSP_ARRAY = JsonArray(listOf(JsonPrimitive(1), JsonPrimitive("v"), JsonNull, JsonPrimitive(false)))
        val LSP_OBJECT = JsonObject(mapOf("k" to LSP_ARRAY, "o" to JsonObject(mapOf("n" to JsonPrimitive(DECIMAL)))))
    }
}
