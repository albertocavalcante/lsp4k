package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * Text document specific client capabilities.
 */
@Serializable
public data class TextDocumentClientCapabilities(
    /**
     * Defines which synchronization capabilities the client supports.
     */
    val synchronization: TextDocumentSyncClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/completion` request.
     */
    val completion: CompletionClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/hover` request.
     */
    val hover: HoverClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/signatureHelp` request.
     */
    val signatureHelp: SignatureHelpClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/declaration` request.
     */
    val declaration: GotoCapability? = null,
    /**
     * Capabilities specific to the `textDocument/definition` request.
     */
    val definition: GotoCapability? = null,
    /**
     * Capabilities specific to the `textDocument/typeDefinition` request.
     */
    val typeDefinition: GotoCapability? = null,
    /**
     * Capabilities specific to the `textDocument/implementation` request.
     */
    val implementation: GotoCapability? = null,
    /**
     * Capabilities specific to the `textDocument/references` request.
     */
    val references: ReferenceClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/documentHighlight` request.
     */
    val documentHighlight: DocumentHighlightClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/documentSymbol` request.
     */
    val documentSymbol: DocumentSymbolClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/codeAction` request.
     */
    val codeAction: CodeActionClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/codeLens` request.
     */
    val codeLens: CodeLensClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/documentLink` request.
     */
    val documentLink: DocumentLinkClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/documentColor` request.
     */
    val colorProvider: DocumentColorClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/formatting` request.
     */
    val formatting: DocumentFormattingClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/rangeFormatting` request.
     */
    val rangeFormatting: DocumentRangeFormattingClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/onTypeFormatting` request.
     */
    val onTypeFormatting: DocumentOnTypeFormattingClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/rename` request.
     */
    val rename: RenameClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/publishDiagnostics` notification.
     */
    val publishDiagnostics: PublishDiagnosticsClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/foldingRange` request.
     */
    val foldingRange: FoldingRangeClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/selectionRange` request.
     */
    val selectionRange: SelectionRangeClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/linkedEditingRange` request.
     */
    val linkedEditingRange: LinkedEditingRangeClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/prepareCallHierarchy` request.
     */
    val callHierarchy: CallHierarchyClientCapabilities? = null,
    /**
     * Capabilities specific to the various semantic token requests.
     */
    val semanticTokens: SemanticTokensClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/moniker` request.
     */
    val moniker: MonikerClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/prepareTypeHierarchy` request.
     */
    val typeHierarchy: TypeHierarchyClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/inlineValue` request.
     */
    val inlineValue: InlineValueClientCapabilities? = null,
    /**
     * Capabilities specific to the `textDocument/inlayHint` request.
     */
    val inlayHint: InlayHintClientCapabilities? = null,
    /**
     * Capabilities specific to the diagnostic pull model.
     */
    val diagnostic: DiagnosticClientCapabilities? = null,
)

/**
 * Defines which synchronization capabilities the client supports.
 */
@Serializable
public data class TextDocumentSyncClientCapabilities(
    /**
     * Whether text document synchronization supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * The client supports sending will save notifications.
     */
    val willSave: Boolean? = null,
    /**
     * The client supports sending a will save request and waits for a response.
     */
    val willSaveWaitUntil: Boolean? = null,
    /**
     * The client supports did save notifications.
     */
    val didSave: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/completion` request.
 */
@Serializable
public data class CompletionClientCapabilities(
    /**
     * Whether completion supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * The client supports the following `CompletionItem` specific capabilities.
     */
    val completionItem: CompletionItemCapability? = null,
    /**
     * Specific capabilities for the `CompletionItemKind`.
     */
    val completionItemKind: CompletionItemKindCapability? = null,
    /**
     * The client supports sending additional context information for a completion request.
     */
    val contextSupport: Boolean? = null,
    /**
     * The client's default when the completion item doesn't provide an `insertTextMode` property.
     */
    val insertTextMode: InsertTextMode? = null,
    /**
     * The client supports the following `CompletionList` specific capabilities.
     */
    val completionList: CompletionListCapabilities? = null,
)

/**
 * Capabilities specific to `CompletionItem`.
 */
@Serializable
public data class CompletionItemCapability(
    /**
     * Client supports snippets as insert text.
     */
    val snippetSupport: Boolean? = null,
    /**
     * Client supports commit characters on a completion item.
     */
    val commitCharactersSupport: Boolean? = null,
    /**
     * Client supports the following content formats for the documentation property.
     */
    val documentationFormat: List<MarkupKind>? = null,
    /**
     * Client supports the deprecated property on a completion item.
     */
    val deprecatedSupport: Boolean? = null,
    /**
     * Client supports the preselect property on a completion item.
     */
    val preselectSupport: Boolean? = null,
    /**
     * Client supports the tag property on a completion item.
     */
    val tagSupport: CompletionItemTagSupport? = null,
    /**
     * Client supports insert replace edit to control different behavior if a
     * completion item is inserted in the text or should replace text.
     */
    val insertReplaceSupport: Boolean? = null,
    /**
     * Indicates which properties a client can resolve lazily on a completion item.
     */
    val resolveSupport: CompletionItemResolveSupport? = null,
    /**
     * The client supports the `insertTextMode` property on a completion item.
     */
    val insertTextModeSupport: InsertTextModeSupport? = null,
    /**
     * The client has support for completion item label details.
     */
    val labelDetailsSupport: Boolean? = null,
)

/**
 * Capabilities specific to completion item tags.
 */
@Serializable
public data class CompletionItemTagSupport(
    /**
     * The tags supported by the client.
     */
    val valueSet: List<CompletionItemTag>,
)

/**
 * Capabilities for completion item resolve support.
 */
@Serializable
public data class CompletionItemResolveSupport(
    /**
     * The properties that a client can resolve lazily.
     */
    val properties: List<String>,
)

/**
 * Capabilities for insert text mode support.
 */
@Serializable
public data class InsertTextModeSupport(
    /**
     * The insert text modes supported by the client.
     */
    val valueSet: List<InsertTextMode>,
)

/**
 * Capabilities specific to `CompletionItemKind`.
 */
@Serializable
public data class CompletionItemKindCapability(
    /**
     * The completion item kind values the client supports.
     */
    val valueSet: List<CompletionItemKind>? = null,
)

/**
 * Capabilities specific to `CompletionList`.
 */
@Serializable
public data class CompletionListCapabilities(
    /**
     * The client supports the following itemDefaults on a completion list.
     */
    val itemDefaults: List<String>? = null,
)

/**
 * Capabilities specific to the `textDocument/hover` request.
 */
@Serializable
public data class HoverClientCapabilities(
    /**
     * Whether hover supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * Client supports the following content formats if the content property refers to a `literal of type MarkupContent`.
     */
    val contentFormat: List<MarkupKind>? = null,
)

/**
 * Capabilities specific to the `textDocument/signatureHelp` request.
 */
@Serializable
public data class SignatureHelpClientCapabilities(
    /**
     * Whether signature help supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * The client supports the following `SignatureInformation` specific properties.
     */
    val signatureInformation: SignatureInformationCapability? = null,
    /**
     * The client supports sending additional context information for a signature help request.
     */
    val contextSupport: Boolean? = null,
)

/**
 * Capabilities specific to `SignatureInformation`.
 */
@Serializable
public data class SignatureInformationCapability(
    /**
     * Client supports the following content formats for the documentation property.
     */
    val documentationFormat: List<MarkupKind>? = null,
    /**
     * Client capabilities specific to parameter information.
     */
    val parameterInformation: ParameterInformationCapability? = null,
    /**
     * The client supports the `activeParameter` property on `SignatureInformation`.
     */
    val activeParameterSupport: Boolean? = null,
)

/**
 * Capabilities specific to parameter information.
 */
@Serializable
public data class ParameterInformationCapability(
    /**
     * The client supports processing label offsets instead of a simple label string.
     */
    val labelOffsetSupport: Boolean? = null,
)

/**
 * Generic goto capability for declaration, definition, type definition, and implementation.
 */
@Serializable
public data class GotoCapability(
    /**
     * Whether the request supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * The client supports additional metadata in the form of definition links.
     */
    val linkSupport: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/references` request.
 */
@Serializable
public data class ReferenceClientCapabilities(
    /**
     * Whether references supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/documentHighlight` request.
 */
@Serializable
public data class DocumentHighlightClientCapabilities(
    /**
     * Whether document highlight supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/documentSymbol` request.
 */
@Serializable
public data class DocumentSymbolClientCapabilities(
    /**
     * Whether document symbol supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * Specific capabilities for the `SymbolKind` in the `textDocument/documentSymbol` request.
     */
    val symbolKind: SymbolKindCapability? = null,
    /**
     * The client supports hierarchical document symbols.
     */
    val hierarchicalDocumentSymbolSupport: Boolean? = null,
    /**
     * The client supports tags on `SymbolInformation`.
     */
    val tagSupport: SymbolTagSupport? = null,
    /**
     * The client supports an additional label presented in the UI when registering a document symbol provider.
     */
    val labelSupport: Boolean? = null,
)
