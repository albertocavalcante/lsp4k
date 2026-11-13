package io.lsp4k.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The `kind` of a meta-model type expression: `base`, `reference`, `array`, `map`, `or`, `tuple`, `literal`, `stringLiteral`. */
internal val JsonObject.metaKind: String get() = getValue("kind").jsonPrimitive.content

/** The `name` of a meta-model declaration, property or base/reference type. */
internal val JsonObject.metaName: String get() = getValue("name").jsonPrimitive.content

/** Whether a meta-model property is declared `optional`. */
internal val JsonObject.isMetaOptional: Boolean get() = this["optional"]?.jsonPrimitive?.booleanOrNull == true

/** Whether a meta-model enumeration accepts values outside its declared set. */
internal val JsonObject.supportsCustomValues: Boolean get() = this["supportsCustomValues"]?.jsonPrimitive?.booleanOrNull == true

internal fun JsonObject.metaChild(key: String): JsonObject = getValue(key).jsonObject

internal fun JsonObject.metaChildren(key: String): List<JsonObject> = this[key]?.jsonArray?.map { it.jsonObject }.orEmpty()

internal fun JsonObject.isNullType(): Boolean = metaKind == "base" && metaName == "null"

/** The `method` of a meta-model request or notification. */
internal val JsonObject.metaMethod: String get() = getValue("method").jsonPrimitive.content

/** Builds the meta-model type `element[]`, used for the members of an array whose element type is a union. */
internal fun metaArrayOf(element: JsonObject): JsonObject = JsonObject(mapOf("kind" to JsonPrimitive("array"), "element" to element))

/** Builds the meta-model type `{ key: value }`, used for the members of a map whose value type is a union. */
internal fun metaMapOf(
    key: JsonObject,
    value: JsonObject,
): JsonObject = JsonObject(mapOf("kind" to JsonPrimitive("map"), "key" to key, "value" to value))

/**
 * Read-only view of the vendored LSP meta model (`metaModel.json`), parsed with kotlinx-serialization-json and
 * no generated classes.
 */
internal class MetaModel(
    root: JsonObject,
) {
    val version: String =
        root
            .metaChild("metaData")
            .getValue("version")
            .jsonPrimitive.content
    val structures: Map<String, JsonObject> = root.metaChildren("structures").associateBy { it.metaName }
    val enumerations: Map<String, JsonObject> = root.metaChildren("enumerations").associateBy { it.metaName }
    val typeAliases: Map<String, JsonObject> = root.metaChildren("typeAliases").associateBy { it.metaName }
    val requests: Map<String, JsonObject> = root.metaChildren("requests").associateBy { it.metaMethod }
    val notifications: Map<String, JsonObject> = root.metaChildren("notifications").associateBy { it.metaMethod }
    val messages: Map<String, JsonObject> = requests + notifications

    /**
     * Structures that only appear in `extends` or `mixins` and are never used as a type. Their properties are
     * flattened into the structures that inherit them, so they need no Kotlin class of their own.
     */
    val abstractStructures: Set<String> =
        structures.values
            .flatMap { it.metaChildren("extends") + it.metaChildren("mixins") }
            .map { it.metaName }
            .toSet() - referencedNames()

    /** Own, inherited and mixed-in properties of [structure]; an own property wins over an inherited one. */
    fun properties(structure: String): Map<String, JsonObject> {
        val node = structures.getValue(structure)
        val result = LinkedHashMap<String, JsonObject>()
        (node.metaChildren("extends") + node.metaChildren("mixins")).forEach { result.putAll(properties(it.metaName)) }
        node.metaChildren("properties").forEach { result[it.metaName] = it }
        return result
    }

    /**
     * The flattened members of a union type, or null when [type] is not a union. Unions nested through type
     * aliases are flattened, arrays and maps of a union become one array or map type per member, and the JSON
     * aliases `LSPAny`, `LSPObject` and `LSPArray` are kept whole.
     */
    fun unionMembers(type: JsonObject): List<JsonObject>? =
        when (type.metaKind) {
            "or" -> type.metaChildren("items").flatMap { unionMembers(it) ?: listOf(it) }
            "reference" -> unionAliasType(type.metaName)?.let(::unionMembers)
            "array" -> unionMembers(type.metaChild("element"))?.map(::metaArrayOf)
            "map" -> unionMembers(type.metaChild("value"))?.map { metaMapOf(type.metaChild("key"), it) }
            else -> null
        }

    /** Whether [type] accepts JSON `null`, directly or through a type alias such as `LSPAny`. */
    fun admitsNull(type: JsonObject): Boolean =
        when (type.metaKind) {
            "or" -> type.metaChildren("items").any(::admitsNull)
            "reference" -> typeAliases[type.metaName]?.let { admitsNull(it.metaChild("type")) } == true
            else -> type.isNullType()
        }

    /** The type of the alias [name], unless it is not an alias or is one of the [JSON_ALIASES]. */
    private fun unionAliasType(name: String): JsonObject? = typeAliases[name]?.takeUnless { name in JSON_ALIASES }?.metaChild("type")

    private fun referencedNames(): Set<String> {
        val names = HashSet<String>()

        fun walk(type: JsonObject) {
            when (type.metaKind) {
                "reference" -> names += type.metaName
                "array" -> walk(type.metaChild("element"))
                "map" -> walk(type.metaChild("value"))
                "or", "and", "tuple" -> type.metaChildren("items").forEach(::walk)
                "literal" -> type.metaChild("value").metaChildren("properties").forEach { walk(it.metaChild("type")) }
            }
        }
        structures.values.forEach { structure -> structure.metaChildren("properties").forEach { walk(it.metaChild("type")) } }
        typeAliases.values.forEach { walk(it.metaChild("type")) }
        messages.values.forEach { message -> PAYLOADS.forEach { key -> (message[key] as? JsonObject)?.let(::walk) } }
        return names
    }

    companion object {
        /** Payload keys of a request or notification, in the order the checker reports them. */
        val PAYLOADS: List<String> = listOf("params", "result", "partialResult", "registrationOptions", "errorData")

        /** Type aliases for arbitrary JSON; samples treat them as opaque values. */
        val JSON_ALIASES: Set<String> = setOf("LSPAny", "LSPObject", "LSPArray")

        fun parse(text: String): MetaModel = MetaModel(Json.parseToJsonElement(text).jsonObject)
    }
}
