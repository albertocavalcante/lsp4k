package io.lsp4k.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Defines how the host (editor) should sync document changes to the language server.
 */
@Serializable
public enum class TextDocumentSyncKind {
    /**
     * Documents should not be synced at all.
     */
    @SerialName("0")
    None,

    /**
     * Documents are synced by always sending the full content of the document.
     */
    @SerialName("1")
    Full,

    /**
     * Documents are synced by sending the full content on open.
     * After that only incremental updates to the document are sent.
     */
    @SerialName("2")
    Incremental,
}

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
    val save: SaveOptions? = null,
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
 * Server capabilities.
 */
@Serializable
public data class ServerCapabilities(
    /**
     * The position encoding the server picked from the encodings offered by the client.
     */
    val positionEncoding: String? = null,
    /**
     * Defines how text documents are synced.
     */
    val textDocumentSync: TextDocumentSyncOptions? = null,
    /**
     * The server provides hover support.
     */
    val hoverProvider: Boolean? = null,
    /**
     * The server provides completion support.
     */
    val completionProvider: CompletionOptions? = null,
    /**
     * The server provides signature help support.
     */
    val signatureHelpProvider: SignatureHelpOptions? = null,
    /**
     * The server provides go to declaration support.
     */
    val declarationProvider: Boolean? = null,
    /**
     * The server provides goto definition support.
     */
    val definitionProvider: Boolean? = null,
    /**
     * The server provides goto type definition support.
     */
    val typeDefinitionProvider: Boolean? = null,
    /**
     * The server provides goto implementation support.
     */
    val implementationProvider: Boolean? = null,
    /**
     * The server provides find references support.
     */
    val referencesProvider: Boolean? = null,
    /**
     * The server provides document highlight support.
     */
    val documentHighlightProvider: Boolean? = null,
    /**
     * The server provides document symbol support.
     */
    val documentSymbolProvider: Boolean? = null,
    /**
     * The server provides code actions.
     */
    val codeActionProvider: Boolean? = null,
    /**
     * The server provides code lens.
     */
    val codeLensProvider: CodeLensOptions? = null,
    /**
     * The server provides document link support.
     */
    val documentLinkProvider: DocumentLinkOptions? = null,
    /**
     * The server provides color provider support.
     */
    val colorProvider: Boolean? = null,
    /**
     * The server provides document formatting.
     */
    val documentFormattingProvider: Boolean? = null,
    /**
     * The server provides document range formatting.
     */
    val documentRangeFormattingProvider: Boolean? = null,
    /**
     * The server provides document formatting on typing.
     */
    val documentOnTypeFormattingProvider: DocumentOnTypeFormattingOptions? = null,
    /**
     * The server provides rename support.
     */
    val renameProvider: Boolean? = null,
    /**
     * The server provides folding provider support.
     */
    val foldingRangeProvider: Boolean? = null,
    /**
     * The server provides execute command support.
     */
    val executeCommandProvider: ExecuteCommandOptions? = null,
    /**
     * The server provides selection range support.
     */
    val selectionRangeProvider: Boolean? = null,
    /**
     * The server provides workspace symbol support.
     */
    val workspaceSymbolProvider: Boolean? = null,
    /**
     * Workspace specific server capabilities.
     */
    val workspace: ServerWorkspaceCapabilities? = null,
    /**
     * Experimental server capabilities.
     */
    val experimental: JsonElement? = null,
)

/**
 * Code lens options.
 */
@Serializable
public data class CodeLensOptions(
    /**
     * Code lens has a resolve provider as well.
     */
    val resolveProvider: Boolean? = null,
)

/**
 * Document link options.
 */
@Serializable
public data class DocumentLinkOptions(
    /**
     * Document links have a resolve provider as well.
     */
    val resolveProvider: Boolean? = null,
)

/**
 * Document on type formatting options.
 */
@Serializable
public data class DocumentOnTypeFormattingOptions(
    /**
     * A character on which formatting should be triggered.
     */
    val firstTriggerCharacter: String,
    /**
     * More trigger characters.
     */
    val moreTriggerCharacter: List<String>? = null,
)

/**
 * Execute command options.
 */
@Serializable
public data class ExecuteCommandOptions(
    /**
     * The commands to be executed on the server.
     */
    val commands: List<String>,
)

/**
 * Workspace specific server capabilities.
 */
@Serializable
public data class ServerWorkspaceCapabilities(
    /**
     * The server supports workspace folder.
     */
    val workspaceFolders: WorkspaceFoldersServerCapabilities? = null,
    /**
     * The server is interested in file notifications/requests.
     */
    val fileOperations: FileOperationOptions? = null,
)

/**
 * Workspace folders server capabilities.
 */
@Serializable
public data class WorkspaceFoldersServerCapabilities(
    /**
     * The server has support for workspace folders.
     */
    val supported: Boolean? = null,
    /**
     * Whether the server wants to receive workspace folder change notifications.
     */
    val changeNotifications: Boolean? = null,
)

/**
 * File operation options.
 */
@Serializable
public data class FileOperationOptions(
    val didCreate: FileOperationRegistrationOptions? = null,
    val willCreate: FileOperationRegistrationOptions? = null,
    val didRename: FileOperationRegistrationOptions? = null,
    val willRename: FileOperationRegistrationOptions? = null,
    val didDelete: FileOperationRegistrationOptions? = null,
    val willDelete: FileOperationRegistrationOptions? = null,
)

/**
 * File operation registration options.
 */
@Serializable
public data class FileOperationRegistrationOptions(
    val filters: List<FileOperationFilter>,
)

/**
 * A filter to describe in which file operation requests or notifications
 * the server is interested in.
 */
@Serializable
public data class FileOperationFilter(
    /**
     * A Uri like `file` or `untitled`.
     */
    val scheme: String? = null,
    /**
     * The actual file operation pattern.
     */
    val pattern: FileOperationPattern,
)

/**
 * A pattern to describe in which file operation requests or notifications
 * the server is interested in.
 */
@Serializable
public data class FileOperationPattern(
    /**
     * The glob pattern to match.
     */
    val glob: String,
    /**
     * Whether to match files or folders.
     */
    val matches: FileOperationPatternKind? = null,
    /**
     * Additional options used during matching.
     */
    val options: FileOperationPatternOptions? = null,
)

/**
 * A pattern kind describing if a glob pattern matches a file a folder or both.
 */
@Serializable
public enum class FileOperationPatternKind {
    @SerialName("file")
    File,

    @SerialName("folder")
    Folder,
}

/**
 * Matching options for the file operation pattern.
 */
@Serializable
public data class FileOperationPatternOptions(
    /**
     * The pattern should be matched ignoring casing.
     */
    val ignoreCase: Boolean? = null,
)

/**
 * General client capabilities.
 */
@Serializable
public data class GeneralClientCapabilities(
    /**
     * Client capability that signals how the client handles stale requests.
     */
    val staleRequestSupport: StaleRequestSupportCapability? = null,
    /**
     * Client capabilities specific to regular expressions.
     */
    val regularExpressions: RegularExpressionsClientCapabilities? = null,
    /**
     * Client capabilities specific to the client's markdown parser.
     */
    val markdown: MarkdownClientCapabilities? = null,
    /**
     * The position encodings supported by the client.
     */
    val positionEncodings: List<String>? = null,
)

/**
 * Stale request support capability.
 */
@Serializable
public data class StaleRequestSupportCapability(
    /**
     * The client will actively cancel the request.
     */
    val cancel: Boolean,
    /**
     * The list of requests for which the client will retry the request
     * if it receives a response with error code `ContentModified`.
     */
    val retryOnContentModified: List<String>,
)

/**
 * Client capabilities specific to regular expressions.
 */
@Serializable
public data class RegularExpressionsClientCapabilities(
    /**
     * The engine's name.
     */
    val engine: String,
    /**
     * The engine's version.
     */
    val version: String? = null,
)

/**
 * Client capabilities specific to the used markdown parser.
 */
@Serializable
public data class MarkdownClientCapabilities(
    /**
     * The name of the parser.
     */
    val parser: String,
    /**
     * The version of the parser.
     */
    val version: String? = null,
    /**
     * A list of HTML tags that the client allows in Markdown.
     */
    val allowedTags: List<String>? = null,
)

/**
 * Client capabilities.
 */
@Serializable
public data class ClientCapabilities(
    /**
     * Workspace specific client capabilities.
     */
    val workspace: WorkspaceClientCapabilities? = null,
    /**
     * Text document specific client capabilities.
     */
    val textDocument: TextDocumentClientCapabilities? = null,
    /**
     * Window specific client capabilities.
     */
    val window: WindowClientCapabilities? = null,
    /**
     * General client capabilities.
     */
    val general: GeneralClientCapabilities? = null,
    /**
     * Experimental client capabilities.
     */
    val experimental: JsonElement? = null,
)

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

// Placeholder types for brevity - these would be fully defined in a complete implementation
@Serializable
public class WorkspaceEditClientCapabilities

@Serializable
public class DidChangeConfigurationClientCapabilities

@Serializable
public class DidChangeWatchedFilesClientCapabilities

@Serializable
public class WorkspaceSymbolClientCapabilities

@Serializable
public class ExecuteCommandClientCapabilities

@Serializable
public class SemanticTokensWorkspaceClientCapabilities

@Serializable
public class CodeLensWorkspaceClientCapabilities

@Serializable
public class FileOperationClientCapabilities

@Serializable
public class InlineValueWorkspaceClientCapabilities

@Serializable
public class InlayHintWorkspaceClientCapabilities

@Serializable
public class DiagnosticWorkspaceClientCapabilities

@Serializable
public class TextDocumentClientCapabilities

@Serializable
public class WindowClientCapabilities

/**
 * Workspace folder.
 */
@Serializable
public data class WorkspaceFolder(
    /**
     * The associated URI for this workspace folder.
     */
    val uri: DocumentUri,
    /**
     * The name of the workspace folder.
     */
    val name: String,
)

/**
 * Client info sent during initialization.
 */
@Serializable
public data class ClientInfo(
    /**
     * The name of the client as defined by the client.
     */
    val name: String,
    /**
     * The client's version as defined by the client.
     */
    val version: String? = null,
)

/**
 * Server info sent during initialization.
 */
@Serializable
public data class ServerInfo(
    /**
     * The name of the server as defined by the server.
     */
    val name: String,
    /**
     * The server's version as defined by the server.
     */
    val version: String? = null,
)

/**
 * Initialize request params.
 */
@Serializable
public data class InitializeParams(
    /**
     * The process Id of the parent process that started the server.
     * Is null if the process has not been started by another process.
     */
    val processId: Int?,
    /**
     * Information about the client.
     */
    val clientInfo: ClientInfo? = null,
    /**
     * The locale the client is currently showing the user interface in.
     */
    val locale: String? = null,
    /**
     * The rootPath of the workspace. Is null if no folder is open.
     * @deprecated Use rootUri instead.
     */
    val rootPath: String? = null,
    /**
     * The rootUri of the workspace. Is null if no folder is open.
     */
    val rootUri: DocumentUri?,
    /**
     * User provided initialization options.
     */
    val initializationOptions: JsonElement? = null,
    /**
     * The capabilities provided by the client (editor or tool).
     */
    val capabilities: ClientCapabilities,
    /**
     * The initial trace setting.
     */
    val trace: TraceValue? = null,
    /**
     * The workspace folders configured in the client when the server starts.
     */
    val workspaceFolders: List<WorkspaceFolder>? = null,
)

/**
 * Trace values.
 */
@Serializable
public enum class TraceValue {
    @SerialName("off")
    Off,

    @SerialName("messages")
    Messages,

    @SerialName("verbose")
    Verbose,
}

/**
 * Initialize result.
 */
@Serializable
public data class InitializeResult(
    /**
     * The capabilities the language server provides.
     */
    val capabilities: ServerCapabilities,
    /**
     * Information about the server.
     */
    val serverInfo: ServerInfo? = null,
)

/**
 * Initialize error data.
 */
@Serializable
public data class InitializeError(
    /**
     * Indicates whether the client execute the following retry logic:
     * (1) show the message provided by the ResponseError to the user
     * (2) user selects retry or cancel
     * (3) if user selected retry the initialize method is sent again.
     */
    val retry: Boolean,
)
