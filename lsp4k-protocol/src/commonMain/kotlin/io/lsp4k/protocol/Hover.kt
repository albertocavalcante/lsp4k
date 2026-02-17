package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Describes the content type that a client supports in various result literals
 * like `Hover`, `ParameterInfo` or `CompletionItem`.
 */
@Serializable
public enum class MarkupKind {
    /**
     * Plain text is supported as a content format.
     */
    @SerialName("plaintext")
    PlainText,

    /**
     * Markdown is supported as a content format.
     */
    @SerialName("markdown")
    Markdown,
}

/**
 * A `MarkupContent` literal represents a string value which content is interpreted based on its kind flag.
 */
@Serializable
public data class MarkupContent(
    /**
     * The type of the Markup.
     */
    val kind: MarkupKind,
    /**
     * The content itself.
     */
    val value: String,
)

/**
 * MarkedString can be used to render human-readable text.
 * It is either a markdown string or a code-block that provides a language and a code snippet.
 * @deprecated use MarkupContent instead.
 */
@Serializable
public data class MarkedString(
    val language: String,
    val value: String,
)

/**
 * The type of Hover contents. Per LSP spec this is:
 * MarkedString | MarkedString[] | MarkupContent
 *
 * We model this as a sealed interface to handle all forms.
 */
@Serializable(with = HoverContentsSerializer::class)
public sealed interface HoverContents {
    /** A single MarkupContent (the modern, preferred form). */
    public data class Markup(val content: MarkupContent) : HoverContents

    /** A single MarkedString (deprecated). */
    public data class SingleMarked(val value: Either<String, MarkedString>) : HoverContents

    /** An array of MarkedStrings (deprecated). */
    public data class MultiMarked(val values: List<Either<String, MarkedString>>) : HoverContents
}

/**
 * Custom serializer for [HoverContents] that handles the LSP union type:
 * MarkedString | MarkedString[] | MarkupContent
 *
 * Deserialization logic:
 * - JsonObject with "kind" field -> MarkupContent (Markup)
 * - JsonPrimitive (string) -> plain string MarkedString (SingleMarked)
 * - JsonObject without "kind" -> MarkedString {language, value} (SingleMarked)
 * - JsonArray -> array of MarkedStrings (MultiMarked)
 */
public object HoverContentsSerializer : KSerializer<HoverContents> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("HoverContents")

    override fun serialize(encoder: Encoder, value: HoverContents) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is HoverContents.Markup ->
                jsonEncoder.encodeSerializableValue(MarkupContent.serializer(), value.content)
            is HoverContents.SingleMarked -> when (val v = value.value) {
                is Either.Left -> jsonEncoder.encodeJsonElement(JsonPrimitive(v.value))
                is Either.Right -> jsonEncoder.encodeSerializableValue(MarkedString.serializer(), v.value)
            }
            is HoverContents.MultiMarked -> {
                val array = value.values.map { item ->
                    when (item) {
                        is Either.Left -> JsonPrimitive(item.value)
                        is Either.Right -> jsonEncoder.json.encodeToJsonElement(MarkedString.serializer(), item.value)
                    }
                }
                jsonEncoder.encodeJsonElement(JsonArray(array))
            }
        }
    }

    override fun deserialize(decoder: Decoder): HoverContents {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element is JsonArray -> {
                val items = element.map { item ->
                    when (item) {
                        is JsonPrimitive -> Either.Left<String>(item.content)
                        is JsonObject -> Either.Right<MarkedString>(
                            jsonDecoder.json.decodeFromJsonElement(MarkedString.serializer(), item),
                        )
                        else -> throw IllegalArgumentException("MarkedString must be a string or object")
                    }
                }
                HoverContents.MultiMarked(items)
            }
            element is JsonObject && element.containsKey("kind") -> {
                HoverContents.Markup(
                    jsonDecoder.json.decodeFromJsonElement(MarkupContent.serializer(), element),
                )
            }
            element is JsonObject -> {
                HoverContents.SingleMarked(
                    Either.Right(jsonDecoder.json.decodeFromJsonElement(MarkedString.serializer(), element)),
                )
            }
            element is JsonPrimitive -> {
                HoverContents.SingleMarked(Either.Left(element.content))
            }
            else -> throw IllegalArgumentException("Hover contents must be a MarkedString, MarkedString array, or MarkupContent")
        }
    }
}

/**
 * The result of a hover request.
 */
@Serializable
public data class Hover(
    /**
     * The hover's content.
     */
    val contents: HoverContents,
    /**
     * An optional range is a range inside a text document that is used to
     * visualize a hover, e.g. by changing the background color.
     */
    val range: Range? = null,
)

/**
 * Parameters for a hover request.
 */
@Serializable
public data class HoverParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The position inside the text document.
     */
    val position: Position,
    /**
     * An optional token that a server can use to report work done progress.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
)
