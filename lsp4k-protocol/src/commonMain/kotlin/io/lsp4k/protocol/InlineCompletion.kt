package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement

@Serializable(with = InlineCompletionTriggerKindSerializer::class)
public enum class InlineCompletionTriggerKind(
    public val value: Int,
) {
    Invoked(1),
    Automatic(2),
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
    /**
     * An optional token that a server can use to report work done progress.
     */
    @Serializable(with = ProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
    /**
     * An optional token that a server can use to report partial results.
     */
    @Serializable(with = ProgressTokenSerializer::class)
    val partialResultToken: ProgressToken? = null,
)

/**
 * Serializer for InlineCompletionItem.insertText, which is a plain string or a [StringValue] object.
 * Any other JSON value is rejected.
 */
public object InlineCompletionInsertTextSerializer : KSerializer<Either<String, StringValue>> by strictUnion(
    "InlineCompletionInsertText",
    String.serializer(),
    StringValue.serializer(),
    JsonElement::isStringLiteral,
    JsonElement::isObject,
)

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

/**
 * Client capabilities specific to inline completions.
 */
@Serializable
public data class InlineCompletionClientCapabilities(
    /**
     * Whether implementation supports dynamic registration for inline completion providers.
     */
    val dynamicRegistration: Boolean? = null,
)
