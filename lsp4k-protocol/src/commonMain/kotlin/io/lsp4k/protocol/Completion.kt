package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

/**
 * The kind of a completion entry.
 */
@Suppress("MagicNumber")
@Serializable(with = CompletionItemKindSerializer::class)
public enum class CompletionItemKind(
    public val value: Int,
) {
    Text(1),
    Method(2),
    Function(3),
    Constructor(4),
    Field(5),
    Variable(6),
    Class(7),
    Interface(8),
    Module(9),
    Property(10),
    Unit(11),
    Value(12),
    Enum(13),
    Keyword(14),
    Snippet(15),
    Color(16),
    File(17),
    Reference(18),
    Folder(19),
    EnumMember(20),
    Constant(21),
    Struct(22),
    Event(23),
    Operator(24),
    TypeParameter(25),

    ;

    public companion object {
        public fun fromValue(value: Int): CompletionItemKind =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown CompletionItemKind: $value")
    }
}

/**
 * Serializer for CompletionItemKind that encodes/decodes as integer.
 */
public object CompletionItemKindSerializer : IntEnumSerializer<CompletionItemKind>(
    "CompletionItemKind",
    CompletionItemKind::fromValue,
    { it.value },
)

/**
 * Completion item tags are extra annotations that tweak the rendering of a completion item.
 */
@Serializable(with = CompletionItemTagSerializer::class)
public enum class CompletionItemTag(
    public val value: Int,
) {
    /**
     * Render a completion as obsolete, usually using a strike-out.
     */
    Deprecated(1),

    ;

    public companion object {
        public fun fromValue(value: Int): CompletionItemTag =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown CompletionItemTag: $value")
    }
}

/**
 * Serializer for CompletionItemTag that encodes/decodes as integer.
 */
public object CompletionItemTagSerializer : IntEnumSerializer<CompletionItemTag>(
    "CompletionItemTag",
    CompletionItemTag::fromValue,
    { it.value },
)

/**
 * Defines whether the insert text in a completion item should be interpreted as
 * plain text or a snippet.
 */
@Serializable(with = InsertTextFormatSerializer::class)
public enum class InsertTextFormat(
    public val value: Int,
) {
    /**
     * The primary text to be inserted is treated as a plain string.
     */
    PlainText(1),

    /**
     * The primary text to be inserted is treated as a snippet.
     */
    Snippet(2),

    ;

    public companion object {
        public fun fromValue(value: Int): InsertTextFormat =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown InsertTextFormat: $value")
    }
}

/**
 * Serializer for InsertTextFormat that encodes/decodes as integer.
 */
public object InsertTextFormatSerializer : IntEnumSerializer<InsertTextFormat>(
    "InsertTextFormat",
    InsertTextFormat::fromValue,
    { it.value },
)

/**
 * How whitespace and indentation is handled during completion item insertion.
 */
@Serializable(with = InsertTextModeSerializer::class)
public enum class InsertTextMode(
    public val value: Int,
) {
    /**
     * The insertion or replace strings are taken as-is.
     */
    AsIs(1),

    /**
     * The editor adjusts leading whitespace of new lines.
     */
    AdjustIndentation(2),

    ;

    public companion object {
        public fun fromValue(value: Int): InsertTextMode =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown InsertTextMode: $value")
    }
}

/**
 * Serializer for InsertTextMode that encodes/decodes as integer.
 */
public object InsertTextModeSerializer : IntEnumSerializer<InsertTextMode>(
    "InsertTextMode",
    InsertTextMode::fromValue,
    { it.value },
)

/**
 * Type alias for documentation which can be either a String or MarkupContent.
 * Per LSP spec, documentation can be a plain string or structured MarkupContent.
 */
public typealias Documentation = Either<String, MarkupContent>

/**
 * Serializer for Documentation (Either<String, MarkupContent>).
 * Delegates to [StringOrMarkupContentSerializer] from Types.kt which handles
 * the same `string | MarkupContent` union type.
 */
public val DocumentationSerializer: KSerializer<Documentation> = StringOrMarkupContentSerializer

/**
 * A special text edit to provide an insert and a replace operation.
 *
 * @since 3.16.0
 */
@Serializable
public data class InsertReplaceEdit(
    /**
     * The string to be inserted.
     */
    val newText: String,
    /**
     * The range if the insert is requested.
     */
    val insert: Range,
    /**
     * The range if the replace is requested.
     */
    val replace: Range,
)

/**
 * Type alias for completion item text edit which can be either a TextEdit or InsertReplaceEdit.
 * Per LSP spec, the textEdit field can be either type.
 */
public typealias CompletionItemTextEdit = Either<TextEdit, InsertReplaceEdit>

/**
 * Serializer for CompletionItemTextEdit (Either<TextEdit, InsertReplaceEdit>).
 * Distinguishes based on presence of 'insert' field (InsertReplaceEdit) vs 'range' field (TextEdit).
 */
public object CompletionItemTextEditSerializer : KSerializer<CompletionItemTextEdit> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CompletionItemTextEdit")

    override fun serialize(
        encoder: Encoder,
        value: CompletionItemTextEdit,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is Either.Left -> jsonEncoder.encodeSerializableValue(TextEdit.serializer(), value.value)
            is Either.Right -> jsonEncoder.encodeSerializableValue(InsertReplaceEdit.serializer(), value.value)
        }
    }

    override fun deserialize(decoder: Decoder): CompletionItemTextEdit {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element is JsonObject && element.containsKey("insert") -> {
                Either.Right(jsonDecoder.json.decodeFromJsonElement(InsertReplaceEdit.serializer(), element))
            }
            element is JsonObject -> {
                Either.Left(jsonDecoder.json.decodeFromJsonElement(TextEdit.serializer(), element))
            }
            else -> throw IllegalArgumentException("CompletionItemTextEdit must be a TextEdit or InsertReplaceEdit object")
        }
    }
}

/**
 * Edit range for completion item defaults.
 * Per LSP spec this is either a Range or an object with insert and replace Range fields.
 */
@Serializable
public data class EditRangeWithInsertReplace(
    val insert: Range,
    val replace: Range,
)

public typealias CompletionEditRange = Either<Range, EditRangeWithInsertReplace>

/**
 * Serializer for CompletionEditRange (Either<Range, EditRangeWithInsertReplace>).
 * Discriminates by presence of 'insert' field.
 */
public object CompletionEditRangeSerializer : KSerializer<CompletionEditRange> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CompletionEditRange")

    override fun serialize(
        encoder: Encoder,
        value: CompletionEditRange,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is Either.Left -> jsonEncoder.encodeSerializableValue(Range.serializer(), value.value)
            is Either.Right -> jsonEncoder.encodeSerializableValue(EditRangeWithInsertReplace.serializer(), value.value)
        }
    }

    override fun deserialize(decoder: Decoder): CompletionEditRange {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element is JsonObject && element.containsKey("insert") -> {
                Either.Right(jsonDecoder.json.decodeFromJsonElement(EditRangeWithInsertReplace.serializer(), element))
            }
            element is JsonObject -> {
                Either.Left(jsonDecoder.json.decodeFromJsonElement(Range.serializer(), element))
            }
            else -> throw IllegalArgumentException("CompletionEditRange must be a Range or {insert, replace} object")
        }
    }
}

/**
 * Additional details for a completion item label.
 */
@Serializable
public data class CompletionItemLabelDetails(
    /**
     * An optional string which is rendered less prominently directly after label.
     */
    val detail: String? = null,
    /**
     * An optional string which is rendered less prominently after detail.
     */
    val description: String? = null,
)

/**
 * A completion item represents a text snippet that is proposed to complete text that is being typed.
 */
@Serializable
public data class CompletionItem(
    /**
     * The label of this completion item.
     */
    val label: String,
    /**
     * Additional details for the label.
     */
    val labelDetails: CompletionItemLabelDetails? = null,
    /**
     * The kind of this completion item.
     */
    val kind: CompletionItemKind? = null,
    /**
     * Tags for this completion item.
     */
    val tags: List<CompletionItemTag>? = null,
    /**
     * A human-readable string with additional information about this item.
     */
    val detail: String? = null,
    /**
     * A human-readable string that represents a doc-comment.
     * Can be either a plain string or MarkupContent as per LSP spec.
     */
    @Serializable(with = NullableStringOrMarkupContentSerializer::class)
    val documentation: Documentation? = null,
    /**
     * Indicates if this item is deprecated.
     */
    val deprecated: Boolean? = null,
    /**
     * Select this item when showing.
     */
    val preselect: Boolean? = null,
    /**
     * A string that should be used when comparing this item with other items.
     */
    val sortText: String? = null,
    /**
     * A string that should be used when filtering a set of completion items.
     */
    val filterText: String? = null,
    /**
     * A string that should be inserted into a document when selecting this completion.
     */
    val insertText: String? = null,
    /**
     * The format of the insert text.
     */
    val insertTextFormat: InsertTextFormat? = null,
    /**
     * How whitespace and indentation is handled during completion item insertion.
     */
    val insertTextMode: InsertTextMode? = null,
    /**
     * An edit which is applied to a document when selecting this completion.
     * Can be either a TextEdit or InsertReplaceEdit as per LSP spec.
     */
    @Serializable(with = CompletionItemTextEditSerializer::class)
    val textEdit: CompletionItemTextEdit? = null,
    /**
     * The edit text used if the completion item is part of a CompletionList
     * and CompletionList defines an item default for the text edit range.
     */
    val textEditText: String? = null,
    /**
     * An optional array of additional text edits that are applied when selecting this completion.
     */
    val additionalTextEdits: List<TextEdit>? = null,
    /**
     * An optional set of characters that when pressed while this completion is active
     * will accept it first and then type that character.
     */
    val commitCharacters: List<String>? = null,
    /**
     * An optional command that is executed after inserting this completion.
     */
    val command: Command? = null,
    /**
     * A data entry field that is preserved on a completion item between a completion
     * and a completion resolve request.
     */
    val data: JsonElement? = null,
)

/**
 * Represents a collection of completion items to be presented in the editor.
 */
@Serializable
public data class CompletionList(
    /**
     * This list is not complete. Further typing should result in recomputing this list.
     */
    val isIncomplete: Boolean,
    /**
     * The completion items.
     */
    val items: List<CompletionItem>,
    /**
     * In many cases the items of an actual completion result share the same
     * value for properties like `commitCharacters` or the range of a text edit.
     * A completion list can therefore define item defaults.
     */
    val itemDefaults: CompletionItemDefaults? = null,
)

/**
 * Default values for completion items.
 */
@Serializable
public data class CompletionItemDefaults(
    /**
     * A default commit character set.
     */
    val commitCharacters: List<String>? = null,
    /**
     * A default edit range.
     * Per LSP spec this is either a Range or an object with insert and replace Range fields.
     */
    @Serializable(with = CompletionEditRangeSerializer::class)
    val editRange: CompletionEditRange? = null,
    /**
     * A default insert text format.
     */
    val insertTextFormat: InsertTextFormat? = null,
    /**
     * A default insert text mode.
     */
    val insertTextMode: InsertTextMode? = null,
    /**
     * A default data value.
     */
    val data: JsonElement? = null,
)

/**
 * Completion parameters.
 */
@Serializable
public data class CompletionParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The position inside the text document.
     */
    val position: Position,
    /**
     * The completion context.
     */
    val context: CompletionContext? = null,
    /**
     * An optional token that a server can use to report work done progress.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
    /**
     * An optional token that a server can use to report partial results.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val partialResultToken: ProgressToken? = null,
)

/**
 * How a completion was triggered.
 */
@Serializable(with = CompletionTriggerKindSerializer::class)
public enum class CompletionTriggerKind(
    public val value: Int,
) {
    /**
     * Completion was triggered by typing an identifier, manual invocation, or API.
     */
    Invoked(1),

    /**
     * Completion was triggered by a trigger character.
     */
    TriggerCharacter(2),

    /**
     * Completion was re-triggered as the current completion list is incomplete.
     */
    TriggerForIncompleteCompletions(3),

    ;

    public companion object {
        public fun fromValue(value: Int): CompletionTriggerKind =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown CompletionTriggerKind: $value")
    }
}

/**
 * Serializer for CompletionTriggerKind that encodes/decodes as integer.
 */
public object CompletionTriggerKindSerializer : IntEnumSerializer<CompletionTriggerKind>(
    "CompletionTriggerKind",
    CompletionTriggerKind::fromValue,
    { it.value },
)

/**
 * Contains additional information about the context in which a completion request is triggered.
 */
@Serializable
public data class CompletionContext(
    /**
     * How the completion was triggered.
     */
    val triggerKind: CompletionTriggerKind,
    /**
     * The trigger character (single character) that has trigger code complete.
     */
    val triggerCharacter: String? = null,
)
