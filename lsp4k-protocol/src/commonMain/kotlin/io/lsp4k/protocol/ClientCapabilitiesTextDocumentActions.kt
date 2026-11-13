package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * Capabilities specific to the `textDocument/codeAction` request.
 */
@Serializable
public data class CodeActionClientCapabilities(
    /**
     * Whether code action supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * The client supports code action literals as a valid response of the `textDocument/codeAction` request.
     */
    val codeActionLiteralSupport: CodeActionLiteralSupport? = null,
    /**
     * Whether code action supports the `isPreferred` property.
     */
    val isPreferredSupport: Boolean? = null,
    /**
     * Whether code action supports the `disabled` property.
     */
    val disabledSupport: Boolean? = null,
    /**
     * Whether code action supports the `data` property.
     */
    val dataSupport: Boolean? = null,
    /**
     * Whether the client supports resolving additional code action properties via a separate request.
     */
    val resolveSupport: CodeActionResolveSupport? = null,
    /**
     * Whether the client honors the change annotations in text edits and resource operations.
     */
    val honorsChangeAnnotations: Boolean? = null,
)

/**
 * Capabilities for code action literal support.
 */
@Serializable
public data class CodeActionLiteralSupport(
    /**
     * The code action kind is supported with the following value set.
     */
    val codeActionKind: CodeActionKindCapability,
)

/**
 * Capabilities for code action kind.
 */
@Serializable
public data class CodeActionKindCapability(
    /**
     * The code action kind values the client supports.
     */
    val valueSet: List<String>,
)

/**
 * Capabilities for code action resolve support.
 */
@Serializable
public data class CodeActionResolveSupport(
    /**
     * The properties that a client can resolve lazily.
     */
    val properties: List<String>,
)

/**
 * Capabilities specific to the `textDocument/codeLens` request.
 */
@Serializable
public data class CodeLensClientCapabilities(
    /**
     * Whether code lens supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/documentLink` request.
 */
@Serializable
public data class DocumentLinkClientCapabilities(
    /**
     * Whether document link supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * Whether the client supports the `tooltip` property on `DocumentLink`.
     */
    val tooltipSupport: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/documentColor` request.
 */
@Serializable
public data class DocumentColorClientCapabilities(
    /**
     * Whether document color supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/formatting` request.
 */
@Serializable
public data class DocumentFormattingClientCapabilities(
    /**
     * Whether formatting supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/rangeFormatting` request.
 */
@Serializable
public data class DocumentRangeFormattingClientCapabilities(
    /**
     * Whether range formatting supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * Whether the client supports formatting multiple ranges at once.
     */
    val rangesSupport: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/onTypeFormatting` request.
 */
@Serializable
public data class DocumentOnTypeFormattingClientCapabilities(
    /**
     * Whether on type formatting supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/rename` request.
 */
@Serializable
public data class RenameClientCapabilities(
    /**
     * Whether rename supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * Client supports testing for validity of rename operations before execution.
     */
    val prepareSupport: Boolean? = null,
    /**
     * Client supports the default behavior result.
     */
    val prepareSupportDefaultBehavior: PrepareSupportDefaultBehavior? = null,
    /**
     * Whether the client honors the change annotations in text edits and resource operations.
     */
    val honorsChangeAnnotations: Boolean? = null,
)

/**
 * Prepare support default behavior.
 */
@Serializable(with = PrepareSupportDefaultBehaviorSerializer::class)
public enum class PrepareSupportDefaultBehavior(
    public val value: Int,
) {
    /**
     * The client's default behavior is to select the identifier according to the language's syntax rule.
     */
    Identifier(1),
    ;

    public companion object {
        public fun fromValue(value: Int): PrepareSupportDefaultBehavior =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown PrepareSupportDefaultBehavior: $value")
    }
}

/**
 * Serializer for PrepareSupportDefaultBehavior that encodes/decodes as integer.
 */
public object PrepareSupportDefaultBehaviorSerializer : IntEnumSerializer<PrepareSupportDefaultBehavior>(
    "PrepareSupportDefaultBehavior",
    PrepareSupportDefaultBehavior::fromValue,
    { it.value },
)

/**
 * Capabilities specific to the `textDocument/publishDiagnostics` notification.
 */
@Serializable
public data class PublishDiagnosticsClientCapabilities(
    /**
     * Whether the client accepts diagnostics with related information.
     */
    val relatedInformation: Boolean? = null,
    /**
     * Client supports the tag property to provide meta data about a diagnostic.
     */
    val tagSupport: DiagnosticTagSupport? = null,
    /**
     * Whether the client interprets the version property of the `textDocument/publishDiagnostics` notification.
     */
    val versionSupport: Boolean? = null,
    /**
     * Client supports a codeDescription property.
     */
    val codeDescriptionSupport: Boolean? = null,
    /**
     * Whether code action supports the `data` property.
     */
    val dataSupport: Boolean? = null,
)

/**
 * Capabilities for diagnostic tag support.
 */
@Serializable
public data class DiagnosticTagSupport(
    /**
     * The tags supported by the client.
     */
    val valueSet: List<DiagnosticTag>,
)

/**
 * Capabilities specific to the `textDocument/foldingRange` request.
 */
@Serializable
public data class FoldingRangeClientCapabilities(
    /**
     * Whether implementation supports dynamic registration for folding range providers.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * The maximum number of folding ranges that the client prefers to receive per document.
     */
    val rangeLimit: Int? = null,
    /**
     * If set, the client signals that it only supports folding complete lines.
     */
    val lineFoldingOnly: Boolean? = null,
    /**
     * Specific options for the folding range kind.
     */
    val foldingRangeKind: FoldingRangeKindCapability? = null,
    /**
     * Specific options for the folding range.
     */
    val foldingRange: FoldingRangeCapability? = null,
)

/**
 * Capabilities for folding range kind.
 */
@Serializable
public data class FoldingRangeKindCapability(
    /**
     * The folding range kind values the client supports.
     */
    val valueSet: List<FoldingRangeKind>? = null,
)

/**
 * Capabilities for folding range.
 */
@Serializable
public data class FoldingRangeCapability(
    /**
     * If set, the client signals that it supports setting collapsedText on folding ranges.
     */
    val collapsedText: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/selectionRange` request.
 */
@Serializable
public data class SelectionRangeClientCapabilities(
    /**
     * Whether implementation supports dynamic registration for selection range providers.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/linkedEditingRange` request.
 */
@Serializable
public data class LinkedEditingRangeClientCapabilities(
    /**
     * Whether implementation supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/prepareCallHierarchy` request.
 */
@Serializable
public data class CallHierarchyClientCapabilities(
    /**
     * Whether implementation supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the various semantic token requests.
 */
@Serializable
public data class SemanticTokensClientCapabilities(
    /**
     * Whether implementation supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * Which requests the client supports and might send to the server.
     */
    val requests: SemanticTokensRequests,
    /**
     * The token types that the client supports.
     */
    val tokenTypes: List<String>,
    /**
     * The token modifiers that the client supports.
     */
    val tokenModifiers: List<String>,
    /**
     * The formats the clients supports.
     */
    val formats: List<TokenFormat>,
    /**
     * Whether the client supports tokens that can overlap each other.
     */
    val overlappingTokenSupport: Boolean? = null,
    /**
     * Whether the client supports tokens that can span multiple lines.
     */
    val multilineTokenSupport: Boolean? = null,
    /**
     * Whether the client allows the server to actively cancel a semantic token request.
     */
    val serverCancelSupport: Boolean? = null,
    /**
     * Whether the client uses semantic tokens to augment existing syntax tokens.
     */
    val augmentsSyntaxTokens: Boolean? = null,
)

/**
 * Which requests the client supports for semantic tokens.
 */
@Serializable
public data class SemanticTokensRequests(
    /**
     * The client will send the `textDocument/semanticTokens/range` request if the server provides a corresponding handler.
     */
    @Serializable(with = SemanticTokensRangeSer::class)
    val range: Either<Boolean, SemanticTokensRangeOptions>? = null,
    /**
     * The client will send the `textDocument/semanticTokens/full` request if the server provides a corresponding handler.
     */
    @Serializable(with = SemanticTokensFullRequestSer::class)
    val full: Either<Boolean, SemanticTokensFullRequestCapability>? = null,
)

/**
 * Capabilities for the full semantic tokens request.
 */
@Serializable
public data class SemanticTokensFullRequestCapability(
    /**
     * The client will send the `textDocument/semanticTokens/full/delta` request if the server provides a corresponding handler.
     */
    val delta: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/moniker` request.
 */
@Serializable
public data class MonikerClientCapabilities(
    /**
     * Whether implementation supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/prepareTypeHierarchy` request.
 */
@Serializable
public data class TypeHierarchyClientCapabilities(
    /**
     * Whether implementation supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/inlineValue` request.
 */
@Serializable
public data class InlineValueClientCapabilities(
    /**
     * Whether implementation supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the `textDocument/inlayHint` request.
 */
@Serializable
public data class InlayHintClientCapabilities(
    /**
     * Whether inlay hints support dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * Indicates which properties a client can resolve lazily on an inlay hint.
     */
    val resolveSupport: InlayHintResolveSupport? = null,
)

/**
 * Capabilities for inlay hint resolve support.
 */
@Serializable
public data class InlayHintResolveSupport(
    /**
     * The properties that a client can resolve lazily.
     */
    val properties: List<String>,
)

/**
 * Capabilities specific to the diagnostic pull model.
 */
@Serializable
public data class DiagnosticClientCapabilities(
    /**
     * Whether implementation supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * Whether the clients supports related documents for document diagnostic pulls.
     */
    val relatedDocumentSupport: Boolean? = null,
)

// ============================================================================
// Window Client Capabilities
// ============================================================================
