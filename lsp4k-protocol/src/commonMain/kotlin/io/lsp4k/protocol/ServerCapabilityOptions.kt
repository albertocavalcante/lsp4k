package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * Defines how the host (editor) should sync document changes to the language server.
 */
@Serializable(with = TextDocumentSyncKindSerializer::class)
public enum class TextDocumentSyncKind(
    public val value: Int,
) {
    /**
     * Documents should not be synced at all.
     */
    None(0),

    /**
     * Documents are synced by always sending the full content of the document.
     */
    Full(1),

    /**
     * Documents are synced by sending the full content on open.
     * After that only incremental updates to the document are sent.
     */
    Incremental(2),

    ;

    public companion object {
        public fun fromValue(value: Int): TextDocumentSyncKind =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown TextDocumentSyncKind: $value")
    }
}

/**
 * Serializer for TextDocumentSyncKind that encodes/decodes as integer.
 */
public object TextDocumentSyncKindSerializer : IntEnumSerializer<TextDocumentSyncKind>(
    "TextDocumentSyncKind",
    TextDocumentSyncKind::fromValue,
    { it.value },
)

/**
 * Completion options.
 */
@Serializable
public data class CompletionOptions(
    /**
     * The additional characters, beyond the defaults provided by the client, that trigger completion.
     */
    val triggerCharacters: List<String>? = null,
    /**
     * The list of all possible characters that commit a completion.
     */
    val allCommitCharacters: List<String>? = null,
    /**
     * The server provides support to resolve additional information for a completion item.
     */
    val resolveProvider: Boolean? = null,
)

/**
 * Signature help options.
 */
@Serializable
public data class SignatureHelpOptions(
    /**
     * The characters that trigger signature help automatically.
     */
    val triggerCharacters: List<String>? = null,
    /**
     * List of characters that re-trigger signature help.
     */
    val retriggerCharacters: List<String>? = null,
)

/**
 * Server capabilities for a text document sync.
 */
@Serializable
public data class TextDocumentSyncOptions(
    /**
     * Open and close notifications are sent to the server.
     */
    val openClose: Boolean? = null,
    /**
     * Change notifications are sent to the server.
     */
    val change: TextDocumentSyncKind? = null,
    /**
     * Will save notifications are sent to the server.
     */
    val willSave: Boolean? = null,
    /**
     * Will save wait until requests are sent to the server.
     */
    val willSaveWaitUntil: Boolean? = null,
    /**
     * Save notifications are sent to the server.
     */
    @Serializable(with = SaveSer::class)
    val save: Either<Boolean, SaveOptions>? = null,
)

/**
 * Save options.
 */
@Serializable
public data class SaveOptions(
    /**
     * The client is supposed to include the content on save.
     */
    val includeText: Boolean? = null,
)

/**
 * Hover options.
 */
@Serializable
public data class HoverOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Definition options.
 */
@Serializable
public data class DefinitionOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Declaration options.
 */
@Serializable
public data class DeclarationOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Type definition options.
 */
@Serializable
public data class TypeDefinitionOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Implementation options.
 */
@Serializable
public data class ImplementationOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * References options.
 */
@Serializable
public data class ReferencesOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Document highlight options.
 */
@Serializable
public data class DocumentHighlightOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Document symbol options.
 */
@Serializable
public data class DocumentSymbolOptions(
    /**
     * A human-readable string that is shown when multiple outlines trees
     * are shown for the same document.
     */
    val label: String? = null,
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Code action options.
 */
@Serializable
public data class CodeActionOptions(
    /**
     * CodeActionKinds that this server may return.
     */
    val codeActionKinds: List<String>? = null,
    /**
     * The server provides support to resolve additional information for a code action.
     */
    val resolveProvider: Boolean? = null,
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Rename options.
 */
@Serializable
public data class RenameOptions(
    /**
     * Renames should be checked and tested before being executed.
     */
    val prepareProvider: Boolean? = null,
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Folding range options.
 */
@Serializable
public data class FoldingRangeOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Selection range options.
 */
@Serializable
public data class SelectionRangeOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Document formatting options.
 */
@Serializable
public data class DocumentFormattingOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Document range formatting options.
 */
@Serializable
public data class DocumentRangeFormattingOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Document color options.
 */
@Serializable
public data class DocumentColorOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Linked editing range options.
 */
@Serializable
public data class LinkedEditingRangeOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Call hierarchy options.
 */
@Serializable
public data class CallHierarchyOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Type hierarchy options.
 */
@Serializable
public data class TypeHierarchyOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Inline value options.
 */
@Serializable
public data class InlineValueOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Moniker options.
 */
@Serializable
public data class MonikerOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Inlay hint options.
 */
@Serializable
public data class InlayHintOptions(
    /**
     * The server provides support to resolve additional information for an inlay hint item.
     */
    val resolveProvider: Boolean? = null,
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Inline completion options.
 * @since 3.18.0
 */
@Serializable
public data class InlineCompletionOptions(
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Workspace symbol options.
 */
@Serializable
public data class WorkspaceSymbolOptions(
    /**
     * The server provides support to resolve additional information for a workspace symbol.
     */
    val resolveProvider: Boolean? = null,
    /**
     * Work done progress options.
     */
    val workDoneProgress: Boolean? = null,
)

// ============================================================================
// Concrete BooleanOr serializers for ServerCapabilities fields
// ============================================================================

internal object HoverProviderSer : NullableBooleanOrSerializer<HoverOptions>(
    BooleanOrSerializer(HoverOptions.serializer()),
)

internal object DeclarationProviderSer : NullableBooleanOrSerializer<DeclarationOptions>(
    BooleanOrSerializer(DeclarationOptions.serializer()),
)

internal object DefinitionProviderSer : NullableBooleanOrSerializer<DefinitionOptions>(
    BooleanOrSerializer(DefinitionOptions.serializer()),
)

internal object TypeDefinitionProviderSer : NullableBooleanOrSerializer<TypeDefinitionOptions>(
    BooleanOrSerializer(TypeDefinitionOptions.serializer()),
)

internal object ImplementationProviderSer : NullableBooleanOrSerializer<ImplementationOptions>(
    BooleanOrSerializer(ImplementationOptions.serializer()),
)

internal object ReferencesProviderSer : NullableBooleanOrSerializer<ReferencesOptions>(
    BooleanOrSerializer(ReferencesOptions.serializer()),
)

internal object DocumentHighlightProviderSer : NullableBooleanOrSerializer<DocumentHighlightOptions>(
    BooleanOrSerializer(DocumentHighlightOptions.serializer()),
)

internal object DocumentSymbolProviderSer : NullableBooleanOrSerializer<DocumentSymbolOptions>(
    BooleanOrSerializer(DocumentSymbolOptions.serializer()),
)

internal object CodeActionProviderSer : NullableBooleanOrSerializer<CodeActionOptions>(
    BooleanOrSerializer(CodeActionOptions.serializer()),
)

internal object ColorProviderSer : NullableBooleanOrSerializer<DocumentColorOptions>(
    BooleanOrSerializer(DocumentColorOptions.serializer()),
)

internal object DocumentFormattingProviderSer : NullableBooleanOrSerializer<DocumentFormattingOptions>(
    BooleanOrSerializer(DocumentFormattingOptions.serializer()),
)

internal object DocumentRangeFormattingProviderSer : NullableBooleanOrSerializer<DocumentRangeFormattingOptions>(
    BooleanOrSerializer(DocumentRangeFormattingOptions.serializer()),
)

internal object RenameProviderSer : NullableBooleanOrSerializer<RenameOptions>(
    BooleanOrSerializer(RenameOptions.serializer()),
)

internal object FoldingRangeProviderSer : NullableBooleanOrSerializer<FoldingRangeOptions>(
    BooleanOrSerializer(FoldingRangeOptions.serializer()),
)

internal object SelectionRangeProviderSer : NullableBooleanOrSerializer<SelectionRangeOptions>(
    BooleanOrSerializer(SelectionRangeOptions.serializer()),
)

internal object LinkedEditingRangeProviderSer : NullableBooleanOrSerializer<LinkedEditingRangeOptions>(
    BooleanOrSerializer(LinkedEditingRangeOptions.serializer()),
)

internal object CallHierarchyProviderSer : NullableBooleanOrSerializer<CallHierarchyOptions>(
    BooleanOrSerializer(CallHierarchyOptions.serializer()),
)

internal object MonikerProviderSer : NullableBooleanOrSerializer<MonikerOptions>(
    BooleanOrSerializer(MonikerOptions.serializer()),
)

internal object TypeHierarchyProviderSer : NullableBooleanOrSerializer<TypeHierarchyOptions>(
    BooleanOrSerializer(TypeHierarchyOptions.serializer()),
)

internal object InlineValueProviderSer : NullableBooleanOrSerializer<InlineValueOptions>(
    BooleanOrSerializer(InlineValueOptions.serializer()),
)

internal object InlineCompletionProviderSer : NullableBooleanOrSerializer<InlineCompletionOptions>(
    BooleanOrSerializer(InlineCompletionOptions.serializer()),
)

internal object InlayHintProviderSer : NullableBooleanOrSerializer<InlayHintOptions>(
    BooleanOrSerializer(InlayHintOptions.serializer()),
)

internal object WorkspaceSymbolProviderSer : NullableBooleanOrSerializer<WorkspaceSymbolOptions>(
    BooleanOrSerializer(WorkspaceSymbolOptions.serializer()),
)
