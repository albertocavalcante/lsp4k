package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Generic serializer for LSP capability fields that can be either a boolean or an options object.
 * Per LSP spec, many ServerCapabilities fields accept `boolean | XxxOptions`.
 * - `true` or options object means the capability is supported
 * - `false` or absent means not supported
 */
public open class BooleanOrSerializer<T>(
    private val optionsSerializer: KSerializer<T>,
) : KSerializer<Either<Boolean, T>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
        "BooleanOr<${optionsSerializer.descriptor.serialName}>",
    )

    override fun serialize(encoder: Encoder, value: Either<Boolean, T>) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is Either.Left -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is Either.Right -> jsonEncoder.encodeSerializableValue(optionsSerializer, value.value)
        }
    }

    override fun deserialize(decoder: Decoder): Either<Boolean, T> {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element is JsonPrimitive && element.booleanOrNull != null ->
                Either.Left(element.booleanOrNull!!)
            else ->
                Either.Right(jsonDecoder.json.decodeFromJsonElement(optionsSerializer, element))
        }
    }
}

/**
 * Nullable variant of [BooleanOrSerializer].
 */
public open class NullableBooleanOrSerializer<T>(
    private val inner: BooleanOrSerializer<T>,
) : KSerializer<Either<Boolean, T>?> {
    override val descriptor: SerialDescriptor = inner.descriptor

    override fun serialize(encoder: Encoder, value: Either<Boolean, T>?) {
        if (value == null) encoder.encodeNull()
        else inner.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): Either<Boolean, T>? {
        return if (decoder.decodeNotNullMark()) inner.deserialize(decoder)
        else decoder.decodeNull()
    }
}

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
    "TextDocumentSyncKind", TextDocumentSyncKind::fromValue, { it.value },
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
    "PrepareSupportDefaultBehavior", PrepareSupportDefaultBehavior::fromValue, { it.value },
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
    val range: Boolean? = null,
    /**
     * The client will send the `textDocument/semanticTokens/full` request if the server provides a corresponding handler.
     */
    val full: SemanticTokensFullRequestCapability? = null,
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

/**
 * Window specific client capabilities.
 */
@Serializable
public data class WindowClientCapabilities(
    /**
     * It indicates whether the client supports server initiated progress using the `window/workDoneProgress/create` request.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * Capabilities specific to the showMessage request.
     */
    val showMessage: ShowMessageRequestClientCapabilities? = null,
    /**
     * Client capabilities for the show document request.
     */
    val showDocument: ShowDocumentClientCapabilities? = null,
)

/**
 * Show message request client capabilities.
 */
@Serializable
public data class ShowMessageRequestClientCapabilities(
    /**
     * Capabilities specific to the `MessageActionItem` type.
     */
    val messageActionItem: MessageActionItemCapabilities? = null,
)

/**
 * Capabilities for message action items.
 */
@Serializable
public data class MessageActionItemCapabilities(
    /**
     * Whether the client supports additional attributes which are preserved and sent back to the server.
     */
    val additionalPropertiesSupport: Boolean? = null,
)

/**
 * Client capabilities for the show document request.
 */
@Serializable
public data class ShowDocumentClientCapabilities(
    /**
     * The client has support for the show document request.
     */
    val support: Boolean,
)

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
