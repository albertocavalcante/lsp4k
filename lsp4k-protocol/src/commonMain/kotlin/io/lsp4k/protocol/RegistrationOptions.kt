package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * Registration options for the implementation feature.
 */
@Serializable
public data class ImplementationRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Registration options for the type definition feature.
 */
@Serializable
public data class TypeDefinitionRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Registration options for the document color feature.
 */
@Serializable
public data class DocumentColorRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Registration options for the folding range feature.
 */
@Serializable
public data class FoldingRangeRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Registration options for the declaration feature.
 */
@Serializable
public data class DeclarationRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Registration options for the selection range feature.
 */
@Serializable
public data class SelectionRangeRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Call hierarchy options used during static or dynamic registration.
 *
 * @since 3.16.0
 */
@Serializable
public data class CallHierarchyRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Registration options for the semantic tokens feature.
 *
 * @since 3.16.0
 */
@Serializable
public data class SemanticTokensRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * The legend used by the server
     */
    val legend: SemanticTokensLegend,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * Server supports providing semantic tokens for a specific range
     * of a document.
     */
    @Serializable(with = SemanticTokensRangeSer::class)
    val range: Either<Boolean, SemanticTokensRangeOptions>? = null,
    /**
     * Server supports providing semantic tokens for a full document.
     */
    @Serializable(with = SemanticTokensFullSer::class)
    val full: Either<Boolean, SemanticTokensFullOptions>? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Registration options for the linked editing range feature.
 */
@Serializable
public data class LinkedEditingRangeRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Registration options for the moniker feature.
 */
@Serializable
public data class MonikerRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Type hierarchy options used during static or dynamic registration.
 *
 * @since 3.17.0
 */
@Serializable
public data class TypeHierarchyRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Inline value options used during static or dynamic registration.
 *
 * @since 3.17.0
 */
@Serializable
public data class InlineValueRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Inlay hint options used during static or dynamic registration.
 *
 * @since 3.17.0
 */
@Serializable
public data class InlayHintRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * The server provides support to resolve additional
     * information for an inlay hint item.
     */
    val resolveProvider: Boolean? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Registration options specific to a notebook.
 *
 * @since 3.17.0
 */
@Serializable
public data class NotebookDocumentSyncRegistrationOptions(
    /**
     * The notebooks to be synced
     */
    val notebookSelector: List<NotebookSelector>,
    /**
     * Whether save notification should be forwarded to
     * the server. Will only be honored if mode === `notebook`.
     */
    val save: Boolean? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Inline completion options used during static or dynamic registration.
 *
 * @since 3.18.0
 */
@Serializable
public data class InlineCompletionRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)

/**
 * Registration options for the did change configuration feature.
 */
@Serializable
public data class DidChangeConfigurationRegistrationOptions(
    @Serializable(with = ConfigurationSectionSer::class)
    val section: Either<String, List<String>>? = null,
)

/**
 * General text document registration options.
 */
@Serializable
public data class TextDocumentRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
)

/**
 * Describe options to be used when registered for text document change events.
 */
@Serializable
public data class TextDocumentChangeRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * How documents are synced to the server.
     */
    val syncKind: TextDocumentSyncKind,
)

/**
 * Save registration options.
 */
@Serializable
public data class TextDocumentSaveRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * The client is supposed to include the content on save.
     */
    val includeText: Boolean? = null,
)

/**
 * Registration options for a {@link CompletionRequest}.
 */
@Serializable
public data class CompletionRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * Most tools trigger completion request automatically without explicitly requesting
     * it using a keyboard shortcut (e.g. Ctrl+Space). Typically they do so when the user
     * starts to type an identifier. For example if the user types `c` in a JavaScript file
     * code complete will automatically pop up present `console` besides others as a
     * completion item. Characters that make up identifiers don't need to be listed here.
     *
     * If code complete should automatically be trigger on characters not being valid inside
     * an identifier (for example `.` in JavaScript) list them in `triggerCharacters`.
     */
    val triggerCharacters: List<String>? = null,
    /**
     * The list of all possible characters that commit a completion. This field can be used
     * if clients don't support individual commit characters per completion item. See
     * `ClientCapabilities.textDocument.completion.completionItem.commitCharactersSupport`
     *
     * If a server provides both `allCommitCharacters` and commit characters on an individual
     * completion item the ones on the completion item win.
     *
     * @since 3.2.0
     */
    val allCommitCharacters: List<String>? = null,
    /**
     * The server provides support to resolve additional
     * information for a completion item.
     */
    val resolveProvider: Boolean? = null,
    /**
     * The server supports the following `CompletionItem` specific
     * capabilities.
     *
     * @since 3.17.0
     */
    val completionItem: ServerCompletionItemOptions? = null,
)

/**
 * Registration options for a {@link HoverRequest}.
 */
@Serializable
public data class HoverRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Registration options for a {@link SignatureHelpRequest}.
 */
@Serializable
public data class SignatureHelpRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * List of characters that trigger signature help automatically.
     */
    val triggerCharacters: List<String>? = null,
    /**
     * List of characters that re-trigger signature help.
     *
     * These trigger characters are only active when signature help is already showing. All trigger characters
     * are also counted as re-trigger characters.
     *
     * @since 3.15.0
     */
    val retriggerCharacters: List<String>? = null,
)

/**
 * Registration options for a {@link DefinitionRequest}.
 */
@Serializable
public data class DefinitionRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Registration options for a {@link ReferencesRequest}.
 */
@Serializable
public data class ReferenceRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Registration options for a {@link DocumentHighlightRequest}.
 */
@Serializable
public data class DocumentHighlightRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Registration options for a {@link DocumentSymbolRequest}.
 */
@Serializable
public data class DocumentSymbolRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * A human-readable string that is shown when multiple outlines trees
     * are shown for the same document.
     *
     * @since 3.16.0
     */
    val label: String? = null,
)

/**
 * Registration options for a {@link CodeActionRequest}.
 */
@Serializable
public data class CodeActionRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * CodeActionKinds that this server may return.
     *
     * The list of kinds may be generic, such as `CodeActionKind.Refactor`, or the server
     * may list out every specific kind they provide.
     */
    val codeActionKinds: List<String>? = null,
    /**
     * Static documentation for a class of code actions.
     *
     * Documentation from the provider should be shown in the code actions menu if either:
     *
     * - Code actions of `kind` are requested by the editor. In this case, the editor will show the documentation that
     *   most closely matches the requested code action kind. For example, if a provider has documentation for
     *   both `Refactor` and `RefactorExtract`, when the user requests code actions for `RefactorExtract`,
     *   the editor will use the documentation for `RefactorExtract` instead of the documentation for `Refactor`.
     *
     * - Any code actions of `kind` are returned by the provider.
     *
     * At most one documentation entry should be shown per provider.
     *
     * @since 3.18.0
     */
    val documentation: List<CodeActionKindDocumentation>? = null,
    /**
     * The server provides support to resolve additional
     * information for a code action.
     *
     * @since 3.16.0
     */
    val resolveProvider: Boolean? = null,
)

/**
 * Registration options for a {@link WorkspaceSymbolRequest}.
 */
@Serializable
public data class WorkspaceSymbolRegistrationOptions(
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * The server provides support to resolve additional
     * information for a workspace symbol.
     *
     * @since 3.17.0
     */
    val resolveProvider: Boolean? = null,
)

/**
 * Registration options for a {@link CodeLensRequest}.
 */
@Serializable
public data class CodeLensRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * Code lens has a resolve provider as well.
     */
    val resolveProvider: Boolean? = null,
)

/**
 * Registration options for a {@link DocumentLinkRequest}.
 */
@Serializable
public data class DocumentLinkRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * Document links have a resolve provider as well.
     */
    val resolveProvider: Boolean? = null,
)

/**
 * Registration options for a {@link DocumentFormattingRequest}.
 */
@Serializable
public data class DocumentFormattingRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Registration options for a {@link DocumentRangeFormattingRequest}.
 */
@Serializable
public data class DocumentRangeFormattingRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * Whether the server supports formatting multiple ranges at once.
     *
     * @since 3.18.0
     */
    val rangesSupport: Boolean? = null,
)

/**
 * Registration options for a {@link DocumentOnTypeFormattingRequest}.
 */
@Serializable
public data class DocumentOnTypeFormattingRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * A character on which formatting should be triggered, like `{`.
     */
    val firstTriggerCharacter: String,
    /**
     * More trigger characters.
     */
    val moreTriggerCharacter: List<String>? = null,
)

/**
 * Registration options for a {@link RenameRequest}.
 */
@Serializable
public data class RenameRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration. If set to null
     * the document selector provided on the client side will be used.
     */
    val documentSelector: List<DocumentFilter>?,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * Renames should be checked and tested before being executed.
     *
     * @since version 3.12.0
     */
    val prepareProvider: Boolean? = null,
)

/**
 * Registration options for a {@link ExecuteCommandRequest}.
 */
@Serializable
public data class ExecuteCommandRegistrationOptions(
    /**
     * The commands to be executed on the server
     */
    val commands: List<String>,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
)

/**
 * Static registration options to be returned in the initialize
 * request.
 */
@Serializable
public data class StaticRegistrationOptions(
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again. See also Registration#id.
     */
    val id: String? = null,
)
