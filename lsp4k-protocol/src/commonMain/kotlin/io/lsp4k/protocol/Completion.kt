package io.lsp4k.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The kind of a completion entry.
 */
@Serializable
public enum class CompletionItemKind {
    @SerialName("1")
    Text,

    @SerialName("2")
    Method,

    @SerialName("3")
    Function,

    @SerialName("4")
    Constructor,

    @SerialName("5")
    Field,

    @SerialName("6")
    Variable,

    @SerialName("7")
    Class,

    @SerialName("8")
    Interface,

    @SerialName("9")
    Module,

    @SerialName("10")
    Property,

    @SerialName("11")
    Unit,

    @SerialName("12")
    Value,

    @SerialName("13")
    Enum,

    @SerialName("14")
    Keyword,

    @SerialName("15")
    Snippet,

    @SerialName("16")
    Color,

    @SerialName("17")
    File,

    @SerialName("18")
    Reference,

    @SerialName("19")
    Folder,

    @SerialName("20")
    EnumMember,

    @SerialName("21")
    Constant,

    @SerialName("22")
    Struct,

    @SerialName("23")
    Event,

    @SerialName("24")
    Operator,

    @SerialName("25")
    TypeParameter,
}

/**
 * Completion item tags are extra annotations that tweak the rendering of a completion item.
 */
@Serializable
public enum class CompletionItemTag {
    /**
     * Render a completion as obsolete, usually using a strike-out.
     */
    @SerialName("1")
    Deprecated,
}

/**
 * Defines whether the insert text in a completion item should be interpreted as
 * plain text or a snippet.
 */
@Serializable
public enum class InsertTextFormat {
    /**
     * The primary text to be inserted is treated as a plain string.
     */
    @SerialName("1")
    PlainText,

    /**
     * The primary text to be inserted is treated as a snippet.
     */
    @SerialName("2")
    Snippet,
}

/**
 * How whitespace and indentation is handled during completion item insertion.
 */
@Serializable
public enum class InsertTextMode {
    /**
     * The insertion or replace strings are taken as-is.
     */
    @SerialName("1")
    AsIs,

    /**
     * The editor adjusts leading whitespace of new lines.
     */
    @SerialName("2")
    AdjustIndentation,
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
     */
    val documentation: String? = null,
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
     */
    val textEdit: TextEdit? = null,
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
     */
    val editRange: Range? = null,
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
)

/**
 * How a completion was triggered.
 */
@Serializable
public enum class CompletionTriggerKind {
    /**
     * Completion was triggered by typing an identifier, manual invocation, or API.
     */
    @SerialName("1")
    Invoked,

    /**
     * Completion was triggered by a trigger character.
     */
    @SerialName("2")
    TriggerCharacter,

    /**
     * Completion was re-triggered as the current completion list is incomplete.
     */
    @SerialName("3")
    TriggerForIncompleteCompletions,
}

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
