package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

/**
 * Parameters for textDocument/rename request.
 */
@Serializable
public data class RenameParams(
    /**
     * The document to rename.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The position at which this request was sent.
     */
    val position: Position,
    /**
     * The new name of the symbol.
     */
    val newName: String,
    /**
     * An optional token that a server can use to report work done progress.
     */
    @Serializable(with = ProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
)

/**
 * Parameters for textDocument/prepareRename request.
 */
@Serializable
public data class PrepareRenameParams(
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
    @Serializable(with = ProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
)

/**
 * The `{ range, placeholder }` form of a prepareRename result.
 */
@Serializable
public data class PrepareRenamePlaceholder(
    /**
     * The range of the string to rename.
     */
    val range: Range,
    /**
     * A placeholder text of the string content to be renamed.
     */
    val placeholder: String,
)

/**
 * The `{ defaultBehavior }` form of a prepareRename result.
 */
@Serializable
public data class PrepareRenameDefaultBehavior(
    /**
     * Whether the client's default behavior should compute the rename range.
     */
    val defaultBehavior: Boolean,
)

/**
 * Result of textDocument/prepareRename request: `Range | { range, placeholder } | { defaultBehavior }`.
 */
@Serializable(with = PrepareRenameResultSerializer::class)
public sealed interface PrepareRenameResult {
    /** A plain range. */
    public data class OfRange(
        public val range: Range,
    ) : PrepareRenameResult

    /** A range with a placeholder text. */
    public data class OfPlaceholder(
        public val placeholder: PrepareRenamePlaceholder,
    ) : PrepareRenameResult

    /** A request to use the client's default rename behavior. */
    public data class OfDefaultBehavior(
        public val defaultBehavior: PrepareRenameDefaultBehavior,
    ) : PrepareRenameResult
}

/**
 * Distinguishes a range by its `start` and `end` keys, a placeholder by `range` and `placeholder`,
 * and the default behavior by `defaultBehavior`. Objects matching none or several forms are rejected.
 */
public object PrepareRenameResultSerializer : KSerializer<PrepareRenameResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PrepareRenameResult")

    override fun serialize(
        encoder: Encoder,
        value: PrepareRenameResult,
    ) {
        val json = encoder as JsonEncoder
        when (value) {
            is PrepareRenameResult.OfRange -> json.encodeSerializableValue(Range.serializer(), value.range)
            is PrepareRenameResult.OfPlaceholder ->
                json.encodeSerializableValue(PrepareRenamePlaceholder.serializer(), value.placeholder)
            is PrepareRenameResult.OfDefaultBehavior ->
                json.encodeSerializableValue(PrepareRenameDefaultBehavior.serializer(), value.defaultBehavior)
        }
    }

    override fun deserialize(decoder: Decoder): PrepareRenameResult {
        val json = decoder as JsonDecoder
        val element = json.decodeJsonElement() as? JsonObject ?: throw SerializationException("Prepare rename result must be an object")
        val isRange = "start" in element && "end" in element
        val isPlaceholder = "range" in element && "placeholder" in element
        val isDefault = "defaultBehavior" in element
        if (listOf(isRange, isPlaceholder, isDefault).count { it } != 1) {
            throw SerializationException("Prepare rename result has missing or ambiguous keys")
        }
        return when {
            isRange -> PrepareRenameResult.OfRange(json.json.decodeFromJsonElement(Range.serializer(), element))
            isPlaceholder ->
                PrepareRenameResult.OfPlaceholder(json.json.decodeFromJsonElement(PrepareRenamePlaceholder.serializer(), element))
            else ->
                PrepareRenameResult.OfDefaultBehavior(
                    json.json.decodeFromJsonElement(PrepareRenameDefaultBehavior.serializer(), element),
                )
        }
    }
}
