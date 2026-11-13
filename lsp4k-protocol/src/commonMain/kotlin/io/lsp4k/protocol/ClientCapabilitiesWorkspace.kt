package io.lsp4k.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Workspace specific client capabilities.
 */
@Serializable
public data class WorkspaceClientCapabilities(
    /**
     * The client supports applying batch edits to the workspace.
     */
    val applyEdit: Boolean? = null,
    /**
     * Capabilities specific to `WorkspaceEdit`s.
     */
    val workspaceEdit: WorkspaceEditClientCapabilities? = null,
    /**
     * Capabilities specific to the `workspace/didChangeConfiguration` notification.
     */
    val didChangeConfiguration: DidChangeConfigurationClientCapabilities? = null,
    /**
     * Capabilities specific to the `workspace/didChangeWatchedFiles` notification.
     */
    val didChangeWatchedFiles: DidChangeWatchedFilesClientCapabilities? = null,
    /**
     * Capabilities specific to the `workspace/symbol` request.
     */
    val symbol: WorkspaceSymbolClientCapabilities? = null,
    /**
     * Capabilities specific to the `workspace/executeCommand` request.
     */
    val executeCommand: ExecuteCommandClientCapabilities? = null,
    /**
     * The client has support for workspace folders.
     */
    val workspaceFolders: Boolean? = null,
    /**
     * The client supports `workspace/configuration` requests.
     */
    val configuration: Boolean? = null,
    /**
     * Capabilities specific to the semantic token requests scoped to the workspace.
     */
    val semanticTokens: SemanticTokensWorkspaceClientCapabilities? = null,
    /**
     * Capabilities specific to the code lens requests scoped to the workspace.
     */
    val codeLens: CodeLensWorkspaceClientCapabilities? = null,
    /**
     * The client has support for file requests/notifications.
     */
    val fileOperations: FileOperationClientCapabilities? = null,
    /**
     * Client workspace capabilities specific to inline values.
     */
    val inlineValue: InlineValueWorkspaceClientCapabilities? = null,
    /**
     * Client workspace capabilities specific to inlay hints.
     */
    val inlayHint: InlayHintWorkspaceClientCapabilities? = null,
    /**
     * Client workspace capabilities specific to diagnostics.
     */
    val diagnostics: DiagnosticWorkspaceClientCapabilities? = null,
)

// ============================================================================
// Workspace Client Capabilities
// ============================================================================

/**
 * Capabilities specific to `WorkspaceEdit`s.
 */
@Serializable
public data class WorkspaceEditClientCapabilities(
    /**
     * The client supports versioned document changes in `WorkspaceEdit`s.
     */
    val documentChanges: Boolean? = null,
    /**
     * The resource operations the client supports.
     */
    val resourceOperations: List<ResourceOperationKind>? = null,
    /**
     * The failure handling strategy of a client if applying the workspace edit fails.
     */
    val failureHandling: FailureHandlingKind? = null,
    /**
     * Whether the client normalizes line endings to the client specific setting.
     */
    val normalizesLineEndings: Boolean? = null,
    /**
     * Whether the client supports change annotations.
     */
    val changeAnnotationSupport: ChangeAnnotationSupport? = null,
)

/**
 * The kind of resource operations supported by the client.
 */
@Serializable
public enum class ResourceOperationKind {
    @SerialName("create")
    Create,

    @SerialName("rename")
    Rename,

    @SerialName("delete")
    Delete,
}

/**
 * The kind of failure handling supported by the client.
 */
@Serializable
public enum class FailureHandlingKind {
    @SerialName("abort")
    Abort,

    @SerialName("transactional")
    Transactional,

    @SerialName("undo")
    Undo,

    @SerialName("textOnlyTransactional")
    TextOnlyTransactional,
}

/**
 * Client support for change annotations.
 */
@Serializable
public data class ChangeAnnotationSupport(
    /**
     * Whether the client groups edits with equal labels into tree nodes.
     */
    val groupsOnLabel: Boolean? = null,
)

/**
 * Capabilities specific to the `workspace/didChangeConfiguration` notification.
 */
@Serializable
public data class DidChangeConfigurationClientCapabilities(
    /**
     * Did change configuration notification supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the `workspace/didChangeWatchedFiles` notification.
 */
@Serializable
public data class DidChangeWatchedFilesClientCapabilities(
    /**
     * Did change watched files notification supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * Whether the client has support for relative patterns.
     */
    val relativePatternSupport: Boolean? = null,
)

/**
 * Capabilities specific to the `workspace/symbol` request.
 */
@Serializable
public data class WorkspaceSymbolClientCapabilities(
    /**
     * Symbol request supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * Specific capabilities for the `SymbolKind` in the `workspace/symbol` request.
     */
    val symbolKind: SymbolKindCapability? = null,
    /**
     * The client supports tags on `SymbolInformation`.
     */
    val tagSupport: SymbolTagSupport? = null,
    /**
     * The client support partial workspace symbols.
     */
    val resolveSupport: WorkspaceSymbolResolveSupport? = null,
)

/**
 * Specific capabilities for the `SymbolKind`.
 */
@Serializable
public data class SymbolKindCapability(
    /**
     * The symbol kind values the client supports.
     */
    val valueSet: List<SymbolKind>? = null,
)

/**
 * Specific capabilities for symbol tags.
 */
@Serializable
public data class SymbolTagSupport(
    /**
     * The tags supported by the client.
     */
    val valueSet: List<SymbolTag>? = null,
)

/**
 * Capabilities for workspace symbol resolve support.
 */
@Serializable
public data class WorkspaceSymbolResolveSupport(
    /**
     * The properties that a client can resolve lazily.
     */
    val properties: List<String>,
)

/**
 * Capabilities specific to the `workspace/executeCommand` request.
 */
@Serializable
public data class ExecuteCommandClientCapabilities(
    /**
     * Execute command supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * Capabilities specific to the semantic token requests scoped to the workspace.
 */
@Serializable
public data class SemanticTokensWorkspaceClientCapabilities(
    /**
     * Whether the client implementation supports a refresh request.
     */
    val refreshSupport: Boolean? = null,
)

/**
 * Capabilities specific to the code lens requests scoped to the workspace.
 */
@Serializable
public data class CodeLensWorkspaceClientCapabilities(
    /**
     * Whether the client implementation supports a refresh request.
     */
    val refreshSupport: Boolean? = null,
)

/**
 * Capabilities specific to file operations.
 */
@Serializable
public data class FileOperationClientCapabilities(
    /**
     * Whether the client supports dynamic registration for file requests/notifications.
     */
    val dynamicRegistration: Boolean? = null,
    /**
     * The client has support for sending didCreateFiles notifications.
     */
    val didCreate: Boolean? = null,
    /**
     * The client has support for sending willCreateFiles requests.
     */
    val willCreate: Boolean? = null,
    /**
     * The client has support for sending didRenameFiles notifications.
     */
    val didRename: Boolean? = null,
    /**
     * The client has support for sending willRenameFiles requests.
     */
    val willRename: Boolean? = null,
    /**
     * The client has support for sending didDeleteFiles notifications.
     */
    val didDelete: Boolean? = null,
    /**
     * The client has support for sending willDeleteFiles requests.
     */
    val willDelete: Boolean? = null,
)

/**
 * Client workspace capabilities specific to inline values.
 */
@Serializable
public data class InlineValueWorkspaceClientCapabilities(
    /**
     * Whether the client implementation supports a refresh request.
     */
    val refreshSupport: Boolean? = null,
)

/**
 * Client workspace capabilities specific to inlay hints.
 */
@Serializable
public data class InlayHintWorkspaceClientCapabilities(
    /**
     * Whether the client implementation supports a refresh request.
     */
    val refreshSupport: Boolean? = null,
)

/**
 * Client workspace capabilities specific to diagnostics.
 */
@Serializable
public data class DiagnosticWorkspaceClientCapabilities(
    /**
     * Whether the client implementation supports a refresh request.
     */
    val refreshSupport: Boolean? = null,
)

// ============================================================================
// Text Document Client Capabilities
// ============================================================================
