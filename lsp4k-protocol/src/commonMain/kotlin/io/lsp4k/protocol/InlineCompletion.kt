package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable(with = InlineCompletionTriggerKindSerializer::class)
public enum class InlineCompletionTriggerKind(
    public val value: Int,
) {
    Invoked(0),
    Automatic(1),
    ;

    public companion object {
        public fun fromValue(value: Int): InlineCompletionTriggerKind =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown InlineCompletionTriggerKind: $value")
    }
}

public object InlineCompletionTriggerKindSerializer : IntEnumSerializer<InlineCompletionTriggerKind>(
    "InlineCompletionTriggerKind",
    InlineCompletionTriggerKind::fromValue,
    { it.value },
)

@Serializable
public data class SelectedCompletionInfo(
    val range: Range,
    val text: String,
)

@Serializable
public data class InlineCompletionContext(
    val triggerKind: InlineCompletionTriggerKind,
    val selectedCompletionInfo: SelectedCompletionInfo? = null,
)

@Serializable
public data class InlineCompletionParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
    val context: InlineCompletionContext,
)

/**
 * Serializer for InlineCompletionItem.insertText which can be either a plain string or a StringValue.
 */
public object InlineCompletionInsertTextSerializer : KSerializer<Either<String, StringValue>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("InlineCompletionInsertText")

    override fun serialize(
        encoder: Encoder,
        value: Either<String, StringValue>,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is Either.Left -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is Either.Right -> jsonEncoder.encodeSerializableValue(StringValue.serializer(), value.value)
        }
    }

    override fun deserialize(decoder: Decoder): Either<String, StringValue> {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> Either.Left(element.content)
            is JsonObject -> Either.Right(jsonDecoder.json.decodeFromJsonElement(StringValue.serializer(), element))
            else -> throw IllegalArgumentException("insertText must be a string or StringValue object")
        }
    }
}

@Serializable
public data class InlineCompletionItem(
    @Serializable(with = InlineCompletionInsertTextSerializer::class)
    val insertText: Either<String, StringValue>,
    val filterText: String? = null,
    val range: Range? = null,
    val command: Command? = null,
)

@Serializable
public data class InlineCompletionList(
    val items: List<InlineCompletionItem>,
)
