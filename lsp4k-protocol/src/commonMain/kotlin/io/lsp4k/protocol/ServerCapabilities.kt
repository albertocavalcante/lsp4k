package io.lsp4k.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
    @Serializable(with = TextDocumentSyncSer::class)
    val textDocumentSync: Either<TextDocumentSyncKind, TextDocumentSyncOptions>? = null,
    /**
     * Defines how notebook documents are synced.
     */
    val notebookDocumentSync: NotebookDocumentSyncOptions? = null,
    /**
     * The server provides hover support.
     */
    @Serializable(with = HoverProviderSer::class)
    val hoverProvider: Either<Boolean, HoverOptions>? = null,
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
    @Serializable(with = DeclarationProviderSer::class)
    val declarationProvider: Either<Boolean, DeclarationOptions>? = null,
    /**
     * The server provides goto definition support.
     */
    @Serializable(with = DefinitionProviderSer::class)
    val definitionProvider: Either<Boolean, DefinitionOptions>? = null,
    /**
     * The server provides goto type definition support.
     */
    @Serializable(with = TypeDefinitionProviderSer::class)
    val typeDefinitionProvider: Either<Boolean, TypeDefinitionOptions>? = null,
    /**
     * The server provides goto implementation support.
     */
    @Serializable(with = ImplementationProviderSer::class)
    val implementationProvider: Either<Boolean, ImplementationOptions>? = null,
    /**
     * The server provides find references support.
     */
    @Serializable(with = ReferencesProviderSer::class)
    val referencesProvider: Either<Boolean, ReferencesOptions>? = null,
    /**
     * The server provides document highlight support.
     */
    @Serializable(with = DocumentHighlightProviderSer::class)
    val documentHighlightProvider: Either<Boolean, DocumentHighlightOptions>? = null,
    /**
     * The server provides document symbol support.
     */
    @Serializable(with = DocumentSymbolProviderSer::class)
    val documentSymbolProvider: Either<Boolean, DocumentSymbolOptions>? = null,
    /**
     * The server provides code actions.
     */
    @Serializable(with = CodeActionProviderSer::class)
    val codeActionProvider: Either<Boolean, CodeActionOptions>? = null,
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
    @Serializable(with = ColorProviderSer::class)
    val colorProvider: Either<Boolean, DocumentColorOptions>? = null,
    /**
     * The server provides document formatting.
     */
    @Serializable(with = DocumentFormattingProviderSer::class)
    val documentFormattingProvider: Either<Boolean, DocumentFormattingOptions>? = null,
    /**
     * The server provides document range formatting.
     */
    @Serializable(with = DocumentRangeFormattingProviderSer::class)
    val documentRangeFormattingProvider: Either<Boolean, DocumentRangeFormattingOptions>? = null,
    /**
     * The server provides document formatting on typing.
     */
    val documentOnTypeFormattingProvider: DocumentOnTypeFormattingOptions? = null,
    /**
     * The server provides rename support.
     */
    @Serializable(with = RenameProviderSer::class)
    val renameProvider: Either<Boolean, RenameOptions>? = null,
    /**
     * The server provides folding provider support.
     */
    @Serializable(with = FoldingRangeProviderSer::class)
    val foldingRangeProvider: Either<Boolean, FoldingRangeOptions>? = null,
    /**
     * The server provides execute command support.
     */
    val executeCommandProvider: ExecuteCommandOptions? = null,
    /**
     * The server provides selection range support.
     */
    @Serializable(with = SelectionRangeProviderSer::class)
    val selectionRangeProvider: Either<Boolean, SelectionRangeOptions>? = null,
    /**
     * The server provides linked editing range support.
     */
    @Serializable(with = LinkedEditingRangeProviderSer::class)
    val linkedEditingRangeProvider: Either<Boolean, LinkedEditingRangeOptions>? = null,
    /**
     * The server provides call hierarchy support.
     */
    @Serializable(with = CallHierarchyProviderSer::class)
    val callHierarchyProvider: Either<Boolean, CallHierarchyOptions>? = null,
    /**
     * The server provides semantic tokens support.
     */
    val semanticTokensProvider: SemanticTokensOptions? = null,
    /**
     * The server provides moniker support.
     */
    @Serializable(with = MonikerProviderSer::class)
    val monikerProvider: Either<Boolean, MonikerOptions>? = null,
    /**
     * The server provides type hierarchy support.
     */
    @Serializable(with = TypeHierarchyProviderSer::class)
    val typeHierarchyProvider: Either<Boolean, TypeHierarchyOptions>? = null,
    /**
     * The server provides inline value support.
     */
    @Serializable(with = InlineValueProviderSer::class)
    val inlineValueProvider: Either<Boolean, InlineValueOptions>? = null,
    /**
     * The server provides inline completion support.
     * @since 3.18.0
     */
    @Serializable(with = InlineCompletionProviderSer::class)
    val inlineCompletionProvider: Either<Boolean, InlineCompletionOptions>? = null,
    /**
     * The server provides inlay hints support.
     */
    @Serializable(with = InlayHintProviderSer::class)
    val inlayHintProvider: Either<Boolean, InlayHintOptions>? = null,
    /**
     * The server provides diagnostic support.
     */
    val diagnosticProvider: DiagnosticOptions? = null,
    /**
     * The server provides workspace symbol support.
     */
    @Serializable(with = WorkspaceSymbolProviderSer::class)
    val workspaceSymbolProvider: Either<Boolean, WorkspaceSymbolOptions>? = null,
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
    @Serializable(with = ChangeNotificationsSer::class)
    val changeNotifications: Either<String, Boolean>? = null,
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
