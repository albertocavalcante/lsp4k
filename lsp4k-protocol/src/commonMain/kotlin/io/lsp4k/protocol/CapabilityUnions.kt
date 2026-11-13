package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * The `{}` form of `range` in `SemanticTokensOptions` and `SemanticTokensClientCapabilities.requests`.
 * It carries no properties and always encodes as an empty JSON object.
 */
@Serializable
public object SemanticTokensRangeOptions

private fun JsonElement.isBoolean(): Boolean = this is JsonPrimitive && !isString && booleanOrNull != null

private fun JsonElement.isInteger(): Boolean = this is JsonPrimitive && !isString && intOrNull != null

private fun JsonElement.isStringLiteral(): Boolean = this is JsonPrimitive && isString

private fun JsonElement.isObject(): Boolean = this is JsonObject

/**
 * Builds a strict serializer for `L | R`: the JSON kind of the element selects the side,
 * and any element matching neither side is rejected.
 */
private fun <L, R> strictUnion(
    name: String,
    leftSerializer: KSerializer<L>,
    rightSerializer: KSerializer<R>,
    isLeft: (JsonElement) -> Boolean,
    isRight: (JsonElement) -> Boolean,
): KSerializer<Either<L, R>> =
    object : KSerializer<Either<L, R>> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name)

        override fun serialize(
            encoder: Encoder,
            value: Either<L, R>,
        ) {
            val jsonEncoder = encoder as JsonEncoder
            when (value) {
                is Either.Left -> jsonEncoder.encodeSerializableValue(leftSerializer, value.value)
                is Either.Right -> jsonEncoder.encodeSerializableValue(rightSerializer, value.value)
            }
        }

        override fun deserialize(decoder: Decoder): Either<L, R> {
            val jsonDecoder = decoder as JsonDecoder
            val element = jsonDecoder.decodeJsonElement()
            return when {
                isLeft(element) -> Either.Left(jsonDecoder.json.decodeFromJsonElement(leftSerializer, element))
                isRight(element) -> Either.Right(jsonDecoder.json.decodeFromJsonElement(rightSerializer, element))
                else -> throw SerializationException("Invalid $name: $element")
            }
        }
    }

/** `ServerCapabilities.textDocumentSync`: `TextDocumentSyncOptions | TextDocumentSyncKind`. */
internal object TextDocumentSyncSer : KSerializer<Either<TextDocumentSyncKind, TextDocumentSyncOptions>> by strictUnion(
    "TextDocumentSyncKindOrOptions",
    TextDocumentSyncKind.serializer(),
    TextDocumentSyncOptions.serializer(),
    JsonElement::isInteger,
    JsonElement::isObject,
)

/** `TextDocumentSyncOptions.save`: `boolean | SaveOptions`. */
internal object SaveSer : KSerializer<Either<Boolean, SaveOptions>> by strictUnion(
    "BooleanOrSaveOptions",
    Boolean.serializer(),
    SaveOptions.serializer(),
    JsonElement::isBoolean,
    JsonElement::isObject,
)

/** `SemanticTokensOptions.range`: `boolean | {}`. */
internal object SemanticTokensRangeSer : KSerializer<Either<Boolean, SemanticTokensRangeOptions>> by strictUnion(
    "BooleanOrSemanticTokensRange",
    Boolean.serializer(),
    SemanticTokensRangeOptions.serializer(),
    JsonElement::isBoolean,
    JsonElement::isObject,
)

/** `SemanticTokensOptions.full`: `boolean | { delta?: boolean }`. */
internal object SemanticTokensFullSer : KSerializer<Either<Boolean, SemanticTokensFullOptions>> by strictUnion(
    "BooleanOrSemanticTokensFull",
    Boolean.serializer(),
    SemanticTokensFullOptions.serializer(),
    JsonElement::isBoolean,
    JsonElement::isObject,
)

/** `SemanticTokensClientCapabilities.requests.full`: `boolean | { delta?: boolean }`. */
internal object SemanticTokensFullRequestSer :
    KSerializer<Either<Boolean, SemanticTokensFullRequestCapability>> by strictUnion(
        "BooleanOrSemanticTokensFullRequest",
        Boolean.serializer(),
        SemanticTokensFullRequestCapability.serializer(),
        JsonElement::isBoolean,
        JsonElement::isObject,
    )

/** `WorkspaceFoldersServerCapabilities.changeNotifications`: `string | boolean`. */
internal object ChangeNotificationsSer : KSerializer<Either<String, Boolean>> by strictUnion(
    "StringOrBoolean",
    String.serializer(),
    Boolean.serializer(),
    JsonElement::isStringLiteral,
    JsonElement::isBoolean,
)
