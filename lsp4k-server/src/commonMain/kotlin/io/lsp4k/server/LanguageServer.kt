package io.lsp4k.server

import io.lsp4k.jsonrpc.Connection
import io.lsp4k.jsonrpc.LspCodec
import io.lsp4k.jsonrpc.LspException
import io.lsp4k.jsonrpc.LspMethods
import io.lsp4k.jsonrpc.NotificationHandler
import io.lsp4k.jsonrpc.RequestHandler
import io.lsp4k.jsonrpc.ResponseError
import io.lsp4k.protocol.ApplyWorkspaceEditParams
import io.lsp4k.protocol.ApplyWorkspaceEditResult
import io.lsp4k.protocol.CallHierarchyIncomingCall
import io.lsp4k.protocol.CallHierarchyIncomingCallsParams
import io.lsp4k.protocol.CallHierarchyItem
import io.lsp4k.protocol.CallHierarchyOutgoingCall
import io.lsp4k.protocol.CallHierarchyOutgoingCallsParams
import io.lsp4k.protocol.CallHierarchyPrepareParams
import io.lsp4k.protocol.CodeAction
import io.lsp4k.protocol.CodeActionParams
import io.lsp4k.protocol.CodeLens
import io.lsp4k.protocol.CodeLensOptions
import io.lsp4k.protocol.CodeLensParams
import io.lsp4k.protocol.ColorInformation
import io.lsp4k.protocol.ColorPresentation
import io.lsp4k.protocol.ColorPresentationParams
import io.lsp4k.protocol.CompletionItem
import io.lsp4k.protocol.CompletionList
import io.lsp4k.protocol.CompletionOptions
import io.lsp4k.protocol.CompletionParams
import io.lsp4k.protocol.ConfigurationParams
import io.lsp4k.protocol.CreateFilesParams
import io.lsp4k.protocol.DeclarationParams
import io.lsp4k.protocol.DefinitionParams
import io.lsp4k.protocol.DeleteFilesParams
import io.lsp4k.protocol.DiagnosticOptions
import io.lsp4k.protocol.DidChangeConfigurationParams
import io.lsp4k.protocol.DidChangeNotebookDocumentParams
import io.lsp4k.protocol.DidChangeTextDocumentParams
import io.lsp4k.protocol.DidChangeWatchedFilesParams
import io.lsp4k.protocol.DidChangeWorkspaceFoldersParams
import io.lsp4k.protocol.DidCloseNotebookDocumentParams
import io.lsp4k.protocol.DidCloseTextDocumentParams
import io.lsp4k.protocol.DidOpenNotebookDocumentParams
import io.lsp4k.protocol.DidOpenTextDocumentParams
import io.lsp4k.protocol.DidSaveNotebookDocumentParams
import io.lsp4k.protocol.DidSaveTextDocumentParams
import io.lsp4k.protocol.DocumentColorParams
import io.lsp4k.protocol.DocumentDiagnosticParams
import io.lsp4k.protocol.DocumentDiagnosticReport
import io.lsp4k.protocol.DocumentFormattingParams
import io.lsp4k.protocol.DocumentHighlight
import io.lsp4k.protocol.DocumentHighlightParams
import io.lsp4k.protocol.DocumentLink
import io.lsp4k.protocol.DocumentLinkOptions
import io.lsp4k.protocol.DocumentLinkParams
import io.lsp4k.protocol.DocumentOnTypeFormattingOptions
import io.lsp4k.protocol.DocumentOnTypeFormattingParams
import io.lsp4k.protocol.DocumentRangeFormattingParams
import io.lsp4k.protocol.DocumentRangesFormattingParams
import io.lsp4k.protocol.DocumentSymbol
import io.lsp4k.protocol.DocumentSymbolParams
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.ExecuteCommandOptions
import io.lsp4k.protocol.ExecuteCommandParams
import io.lsp4k.protocol.FoldingRange
import io.lsp4k.protocol.FoldingRangeParams
import io.lsp4k.protocol.Hover
import io.lsp4k.protocol.HoverParams
import io.lsp4k.protocol.ImplementationParams
import io.lsp4k.protocol.InitializeParams
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.InlayHint
import io.lsp4k.protocol.InlayHintParams
import io.lsp4k.protocol.InlineCompletionList
import io.lsp4k.protocol.InlineCompletionParams
import io.lsp4k.protocol.InlineValue
import io.lsp4k.protocol.InlineValueParams
import io.lsp4k.protocol.LinkedEditingRangeParams
import io.lsp4k.protocol.LinkedEditingRanges
import io.lsp4k.protocol.Location
import io.lsp4k.protocol.LocationLink
import io.lsp4k.protocol.LogMessageParams
import io.lsp4k.protocol.LogTraceParams
import io.lsp4k.protocol.MessageActionItem
import io.lsp4k.protocol.MessageType
import io.lsp4k.protocol.Moniker
import io.lsp4k.protocol.MonikerParams
import io.lsp4k.protocol.NotebookDocumentSyncOptions
import io.lsp4k.protocol.PrepareRenameParams
import io.lsp4k.protocol.PrepareRenameResult
import io.lsp4k.protocol.ProgressParams
import io.lsp4k.protocol.PublishDiagnosticsParams
import io.lsp4k.protocol.ReferenceParams
import io.lsp4k.protocol.RegistrationParams
import io.lsp4k.protocol.RenameFilesParams
import io.lsp4k.protocol.RenameParams
import io.lsp4k.protocol.SaveOptions
import io.lsp4k.protocol.SelectionRange
import io.lsp4k.protocol.SelectionRangeParams
import io.lsp4k.protocol.SemanticTokens
import io.lsp4k.protocol.SemanticTokensDelta
import io.lsp4k.protocol.SemanticTokensDeltaParams
import io.lsp4k.protocol.SemanticTokensOptions
import io.lsp4k.protocol.SemanticTokensParams
import io.lsp4k.protocol.SemanticTokensRangeParams
import io.lsp4k.protocol.ServerCapabilities
import io.lsp4k.protocol.ServerInfo
import io.lsp4k.protocol.SetTraceParams
import io.lsp4k.protocol.ShowDocumentParams
import io.lsp4k.protocol.ShowDocumentResult
import io.lsp4k.protocol.ShowMessageParams
import io.lsp4k.protocol.ShowMessageRequestParams
import io.lsp4k.protocol.SignatureHelp
import io.lsp4k.protocol.SignatureHelpOptions
import io.lsp4k.protocol.SignatureHelpParams
import io.lsp4k.protocol.SymbolInformation
import io.lsp4k.protocol.TextDocumentSyncKind
import io.lsp4k.protocol.TextDocumentSyncOptions
import io.lsp4k.protocol.TextEdit
import io.lsp4k.protocol.TraceValue
import io.lsp4k.protocol.TypeDefinitionParams
import io.lsp4k.protocol.TypeHierarchyItem
import io.lsp4k.protocol.TypeHierarchyPrepareParams
import io.lsp4k.protocol.TypeHierarchySubtypesParams
import io.lsp4k.protocol.TypeHierarchySupertypesParams
import io.lsp4k.protocol.UnregistrationParams
import io.lsp4k.protocol.WillSaveTextDocumentParams
import io.lsp4k.protocol.WorkDoneProgressCancelParams
import io.lsp4k.protocol.WorkDoneProgressCreateParams
import io.lsp4k.protocol.WorkspaceDiagnosticParams
import io.lsp4k.protocol.WorkspaceDiagnosticReport
import io.lsp4k.protocol.WorkspaceEdit
import io.lsp4k.protocol.WorkspaceFolder
import io.lsp4k.protocol.WorkspaceSymbol
import io.lsp4k.protocol.WorkspaceSymbolParams
import io.lsp4k.transport.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer
import kotlin.concurrent.Volatile

/**
 * Marks DSL classes used for configuring an LSP language server.
 */
@DslMarker
public annotation class LspServerDsl

/**
 * Immutable snapshot of a fully configured language server.
 *
 * Produced by [languageServer] and consumed by [LanguageServerConfig.start].
 *
 * @property serverInfo Optional name/version metadata advertised to the client.
 * @property capabilities The set of LSP capabilities the server declares.
 * @property requestHandlers Handlers keyed by JSON-RPC method name for requests.
 * @property notificationHandlers Handlers keyed by JSON-RPC method name for notifications.
 */
public data class LanguageServerConfig(
    val serverInfo: ServerInfo?,
    val capabilities: ServerCapabilities,
    val requestHandlers: Map<String, RequestHandler>,
    val notificationHandlers: Map<String, NotificationHandler>,
)

/**
 * Top-level builder for assembling a [LanguageServerConfig] via the [languageServer] DSL.
 */
@LspServerDsl
public class LanguageServerBuilder {
    private var serverInfo: ServerInfo? = null
    private var capabilities = ServerCapabilities()
    private val requestHandlers = mutableMapOf<String, RequestHandler>()
    private val notificationHandlers = mutableMapOf<String, NotificationHandler>()

    /**
     * Sets the server name and optional version advertised in the `initialize` response.
     */
    public fun serverInfo(
        name: String,
        version: String? = null,
    ) {
        serverInfo = ServerInfo(name, version)
    }

    /**
     * Configures server capabilities advertised in the `initialize` response.
     */
    public fun capabilities(block: ServerCapabilitiesBuilder.() -> Unit) {
        capabilities = ServerCapabilitiesBuilder().apply(block).build()
    }

    /**
     * Registers handlers for textDocument methods.
     */
    public fun textDocument(block: TextDocumentHandlersBuilder.() -> Unit) {
        TextDocumentHandlersBuilder(requestHandlers, notificationHandlers).apply(block)
    }

    /**
     * Registers handlers for workspace methods.
     */
    public fun workspace(block: WorkspaceHandlersBuilder.() -> Unit) {
        WorkspaceHandlersBuilder(requestHandlers, notificationHandlers).apply(block)
    }

    /**
     * Registers handlers for call hierarchy methods.
     */
    public fun callHierarchy(block: CallHierarchyHandlersBuilder.() -> Unit) {
        CallHierarchyHandlersBuilder(requestHandlers).apply(block)
    }

    /**
     * Registers handlers for type hierarchy methods.
     */
    public fun typeHierarchy(block: TypeHierarchyHandlersBuilder.() -> Unit) {
        TypeHierarchyHandlersBuilder(requestHandlers).apply(block)
    }

    /**
     * Registers handlers for notebookDocument methods.
     */
    public fun notebook(block: NotebookDocumentHandlersBuilder.() -> Unit) {
        NotebookDocumentHandlersBuilder(notificationHandlers).apply(block)
    }

    /**
     * Registers handlers for window methods.
     */
    public fun window(block: WindowHandlersBuilder.() -> Unit) {
        WindowHandlersBuilder(notificationHandlers).apply(block)
    }

    /**
     * Registers a custom request handler for the given JSON-RPC [method].
     */
    public fun onRequest(
        method: String,
        handler: RequestHandler,
    ) {
        requestHandlers[method] = handler
    }

    /**
     * Registers a custom notification handler for the given JSON-RPC [method].
     */
    public fun onNotification(
        method: String,
        handler: NotificationHandler,
    ) {
        notificationHandlers[method] = handler
    }

    internal fun build(): LanguageServerConfig =
        LanguageServerConfig(
            serverInfo = serverInfo,
            capabilities = capabilities,
            requestHandlers = requestHandlers.toMap(),
            notificationHandlers = notificationHandlers.toMap(),
        )
}

// =============================================================================
// ServerCapabilitiesBuilder
// =============================================================================

/**
 * Builder for constructing [ServerCapabilities] advertised during initialization.
 */
@LspServerDsl
public class ServerCapabilitiesBuilder {
    /** Defines how text documents are synced (open/close, change granularity, save). */
    public var textDocumentSync: TextDocumentSyncKind? = null

    /** Defines how notebook documents are synced. */
    public var notebookDocumentSync: NotebookDocumentSyncOptions? = null

    /** Options for the `textDocument/completion` provider. */
    public var completionProvider: CompletionOptions? = null

    /** Whether the server provides `textDocument/hover` support. */
    public var hoverProvider: Boolean = false

    /** Options for the `textDocument/signatureHelp` provider. */
    public var signatureHelpProvider: SignatureHelpOptions? = null

    /** Whether the server provides `textDocument/declaration` support. */
    public var declarationProvider: Boolean = false

    /** Whether the server provides `textDocument/definition` support. */
    public var definitionProvider: Boolean = false

    /** Whether the server provides `textDocument/typeDefinition` support. */
    public var typeDefinitionProvider: Boolean = false

    /** Whether the server provides `textDocument/implementation` support. */
    public var implementationProvider: Boolean = false

    /** Whether the server provides `textDocument/references` support. */
    public var referencesProvider: Boolean = false

    /** Whether the server provides `textDocument/documentHighlight` support. */
    public var documentHighlightProvider: Boolean = false

    /** Whether the server provides `textDocument/documentSymbol` support. */
    public var documentSymbolProvider: Boolean = false

    /** Whether the server provides `textDocument/codeAction` support. */
    public var codeActionProvider: Boolean = false

    /** Options for the `textDocument/codeLens` provider. */
    public var codeLensProvider: CodeLensOptions? = null

    /** Options for the `textDocument/documentLink` provider. */
    public var documentLinkProvider: DocumentLinkOptions? = null

    /** Whether the server provides `textDocument/documentColor` support. */
    public var colorProvider: Boolean = false

    /** Whether the server provides `textDocument/formatting` support. */
    public var documentFormattingProvider: Boolean = false

    /** Whether the server provides `textDocument/rangeFormatting` support. */
    public var documentRangeFormattingProvider: Boolean = false

    /** Options for the `textDocument/onTypeFormatting` provider. */
    public var documentOnTypeFormattingProvider: DocumentOnTypeFormattingOptions? = null

    /** Whether the server provides `textDocument/rename` support. */
    public var renameProvider: Boolean = false

    /** Whether the server provides `textDocument/foldingRange` support. */
    public var foldingRangeProvider: Boolean = false

    /** Whether the server provides `textDocument/selectionRange` support. */
    public var selectionRangeProvider: Boolean = false

    /** Whether the server provides `textDocument/linkedEditingRange` support. */
    public var linkedEditingRangeProvider: Boolean = false

    /** Whether the server provides call hierarchy support. */
    public var callHierarchyProvider: Boolean = false

    /** Options for the semantic tokens provider. */
    public var semanticTokensProvider: SemanticTokensOptions? = null

    /** Whether the server provides `textDocument/moniker` support. */
    public var monikerProvider: Boolean = false

    /** Whether the server provides type hierarchy support. */
    public var typeHierarchyProvider: Boolean = false

    /** Whether the server provides `textDocument/inlineValue` support. */
    public var inlineValueProvider: Boolean = false

    /** Whether the server provides `textDocument/inlineCompletion` support. */
    public var inlineCompletionProvider: Boolean = false

    /** Whether the server provides `textDocument/inlayHint` support. */
    public var inlayHintProvider: Boolean = false

    /** Options for the `textDocument/diagnostic` provider. */
    public var diagnosticProvider: DiagnosticOptions? = null

    /** Whether the server provides `workspace/symbol` support. */
    public var workspaceSymbolProvider: Boolean = false

    /** Options for the `workspace/executeCommand` provider. */
    public var executeCommandProvider: ExecuteCommandOptions? = null

    private fun Boolean.toEither(): Either<Boolean, Nothing>? = if (this) Either.left(true) else null

    internal fun build(): ServerCapabilities =
        ServerCapabilities(
            textDocumentSync =
                textDocumentSync?.let {
                    TextDocumentSyncOptions(
                        openClose = true,
                        change = it,
                        save = SaveOptions(includeText = false),
                    )
                },
            notebookDocumentSync = notebookDocumentSync,
            completionProvider = completionProvider,
            hoverProvider = hoverProvider.toEither(),
            signatureHelpProvider = signatureHelpProvider,
            declarationProvider = declarationProvider.toEither(),
            definitionProvider = definitionProvider.toEither(),
            typeDefinitionProvider = typeDefinitionProvider.toEither(),
            implementationProvider = implementationProvider.toEither(),
            referencesProvider = referencesProvider.toEither(),
            documentHighlightProvider = documentHighlightProvider.toEither(),
            documentSymbolProvider = documentSymbolProvider.toEither(),
            codeActionProvider = codeActionProvider.toEither(),
            codeLensProvider = codeLensProvider,
            documentLinkProvider = documentLinkProvider,
            colorProvider = colorProvider.toEither(),
            documentFormattingProvider = documentFormattingProvider.toEither(),
            documentRangeFormattingProvider = documentRangeFormattingProvider.toEither(),
            documentOnTypeFormattingProvider = documentOnTypeFormattingProvider,
            renameProvider = renameProvider.toEither(),
            foldingRangeProvider = foldingRangeProvider.toEither(),
            selectionRangeProvider = selectionRangeProvider.toEither(),
            linkedEditingRangeProvider = linkedEditingRangeProvider.toEither(),
            callHierarchyProvider = callHierarchyProvider.toEither(),
            semanticTokensProvider = semanticTokensProvider,
            monikerProvider = monikerProvider.toEither(),
            typeHierarchyProvider = typeHierarchyProvider.toEither(),
            inlineValueProvider = inlineValueProvider.toEither(),
            inlineCompletionProvider = inlineCompletionProvider.toEither(),
            inlayHintProvider = inlayHintProvider.toEither(),
            diagnosticProvider = diagnosticProvider,
            workspaceSymbolProvider = workspaceSymbolProvider.toEither(),
            executeCommandProvider = executeCommandProvider,
        )
}

// =============================================================================
// HandlersBuilder (abstract base)
// =============================================================================

/**
 * Abstract base for handler-builder classes that share request/notification
 * registration logic.
 *
 * Subclasses inherit [registerRequest], [registerNotification], and
 * [registerRequestRaw] to wire up typed handlers with minimal boilerplate.
 */
@LspServerDsl
public abstract class HandlersBuilder internal constructor(
    @PublishedApi internal val json: Json,
    @PublishedApi internal val requestHandlers: MutableMap<String, RequestHandler>,
    @PublishedApi internal val notificationHandlers: MutableMap<String, NotificationHandler>,
) {
    /**
     * Registers a typed request handler that decodes [P] from JSON, invokes
     * [handler], and encodes the nullable result [R] back to JSON.
     */
    @PublishedApi
    internal inline fun <reified P, reified R> registerRequest(
        method: String,
        crossinline handler: suspend (P) -> R?,
    ) {
        val paramSerializer = serializer<P>()
        val resultSerializer = serializer<R>()
        requestHandlers[method] =
            RequestHandler { params ->
                val typedParams: P =
                    params?.let { json.decodeFromJsonElement(paramSerializer, it) }
                        ?: throw LspException.invalidParams("Missing params for $method")
                val result = handler(typedParams)
                result?.let { json.encodeToJsonElement(resultSerializer, it) }
            }
    }

    /**
     * Registers a request handler whose [handler] returns a raw [JsonElement]
     * (used when the result type cannot be expressed via reified generics, e.g.
     * `workspace/executeCommand` which returns `any | null`).
     */
    @PublishedApi
    internal inline fun <reified P> registerRequestRaw(
        method: String,
        crossinline handler: suspend (P) -> JsonElement?,
    ) {
        val paramSerializer = serializer<P>()
        requestHandlers[method] =
            RequestHandler { params ->
                val typedParams: P =
                    params?.let { json.decodeFromJsonElement(paramSerializer, it) }
                        ?: throw LspException.invalidParams("Missing params for $method")
                handler(typedParams)
            }
    }

    /**
     * Registers a typed notification handler that decodes [P] from JSON and
     * invokes [handler].
     */
    @PublishedApi
    internal inline fun <reified P> registerNotification(
        method: String,
        crossinline handler: suspend (P) -> Unit,
    ) {
        val paramSerializer = serializer<P>()
        notificationHandlers[method] =
            NotificationHandler { params ->
                val typedParams: P =
                    params?.let { json.decodeFromJsonElement(paramSerializer, it) }
                        ?: throw LspException.invalidParams("Missing params for $method")
                handler(typedParams)
            }
    }
}

// =============================================================================
// TextDocumentHandlersBuilder
// =============================================================================

/**
 * Builder for textDocument request and notification handlers.
 *
 * This class intentionally has many functions to cover all LSP text document
 * methods.
 */
@LspServerDsl
public class TextDocumentHandlersBuilder(
    requestHandlers: MutableMap<String, RequestHandler>,
    notificationHandlers: MutableMap<String, NotificationHandler>,
) : HandlersBuilder(LspCodec.defaultJson, requestHandlers, notificationHandlers) {
    // ===== Document Synchronization =====

    /** Registers a handler for `textDocument/didOpen` notifications. */
    public fun didOpen(handler: suspend (DidOpenTextDocumentParams) -> Unit) {
        registerNotification(LspMethods.TEXT_DOCUMENT_DID_OPEN, handler)
    }

    /** Registers a handler for `textDocument/didClose` notifications. */
    public fun didClose(handler: suspend (DidCloseTextDocumentParams) -> Unit) {
        registerNotification(LspMethods.TEXT_DOCUMENT_DID_CLOSE, handler)
    }

    /** Registers a handler for `textDocument/didChange` notifications. */
    public fun didChange(handler: suspend (DidChangeTextDocumentParams) -> Unit) {
        registerNotification(LspMethods.TEXT_DOCUMENT_DID_CHANGE, handler)
    }

    /** Registers a handler for `textDocument/didSave` notifications. */
    public fun didSave(handler: suspend (DidSaveTextDocumentParams) -> Unit) {
        registerNotification(LspMethods.TEXT_DOCUMENT_DID_SAVE, handler)
    }

    /** Registers a handler for `textDocument/willSave` notifications. */
    public fun willSave(handler: suspend (WillSaveTextDocumentParams) -> Unit) {
        registerNotification(LspMethods.TEXT_DOCUMENT_WILL_SAVE, handler)
    }

    /** Registers a handler for `textDocument/willSaveWaitUntil` requests. */
    public fun willSaveWaitUntil(handler: suspend (WillSaveTextDocumentParams) -> List<TextEdit>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_WILL_SAVE_WAIT_UNTIL, handler)
    }

    // ===== Language Features =====

    /** Registers a handler for `textDocument/completion` requests. */
    public fun completion(handler: suspend (CompletionParams) -> CompletionList?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_COMPLETION, handler)
    }

    /** Registers a handler for `completionItem/resolve` requests. */
    public fun completionResolve(handler: suspend (CompletionItem) -> CompletionItem) {
        registerRequest(LspMethods.COMPLETION_ITEM_RESOLVE, handler)
    }

    /** Registers a handler for `textDocument/hover` requests. */
    public fun hover(handler: suspend (HoverParams) -> Hover?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_HOVER, handler)
    }

    /** Registers a handler for `textDocument/signatureHelp` requests. */
    public fun signatureHelp(handler: suspend (SignatureHelpParams) -> SignatureHelp?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_SIGNATURE_HELP, handler)
    }

    /** Registers a handler for `textDocument/declaration` requests. */
    public fun declaration(handler: suspend (DeclarationParams) -> List<Location>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_DECLARATION, handler)
    }

    /** Registers a handler for `textDocument/definition` requests (returns [Location] list). */
    public fun definition(handler: suspend (DefinitionParams) -> List<Location>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_DEFINITION, handler)
    }

    /** Registers a handler for `textDocument/definition` requests (returns [LocationLink] list). */
    public fun definitionWithLinks(handler: suspend (DefinitionParams) -> List<LocationLink>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_DEFINITION, handler)
    }

    /** Registers a handler for `textDocument/typeDefinition` requests. */
    public fun typeDefinition(handler: suspend (TypeDefinitionParams) -> List<Location>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_TYPE_DEFINITION, handler)
    }

    /** Registers a handler for `textDocument/implementation` requests. */
    public fun implementation(handler: suspend (ImplementationParams) -> List<Location>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_IMPLEMENTATION, handler)
    }

    /** Registers a handler for `textDocument/references` requests. */
    public fun references(handler: suspend (ReferenceParams) -> List<Location>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_REFERENCES, handler)
    }

    /** Registers a handler for `textDocument/documentHighlight` requests. */
    public fun documentHighlight(handler: suspend (DocumentHighlightParams) -> List<DocumentHighlight>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT, handler)
    }

    /** Registers a handler for `textDocument/documentSymbol` requests (hierarchical). */
    public fun documentSymbol(handler: suspend (DocumentSymbolParams) -> List<DocumentSymbol>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_DOCUMENT_SYMBOL, handler)
    }

    /** Registers a handler for `textDocument/documentSymbol` requests (flat [SymbolInformation] list). */
    public fun documentSymbolFlat(handler: suspend (DocumentSymbolParams) -> List<SymbolInformation>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_DOCUMENT_SYMBOL, handler)
    }

    /** Registers a handler for `textDocument/codeAction` requests. */
    public fun codeAction(handler: suspend (CodeActionParams) -> List<CodeAction>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_CODE_ACTION, handler)
    }

    /** Registers a handler for `codeAction/resolve` requests. */
    public fun codeActionResolve(handler: suspend (CodeAction) -> CodeAction) {
        registerRequest(LspMethods.CODE_ACTION_RESOLVE, handler)
    }

    /** Registers a handler for `textDocument/codeLens` requests. */
    public fun codeLens(handler: suspend (CodeLensParams) -> List<CodeLens>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_CODE_LENS, handler)
    }

    /** Registers a handler for `codeLens/resolve` requests. */
    public fun codeLensResolve(handler: suspend (CodeLens) -> CodeLens) {
        registerRequest(LspMethods.CODE_LENS_RESOLVE, handler)
    }

    /** Registers a handler for `textDocument/documentLink` requests. */
    public fun documentLink(handler: suspend (DocumentLinkParams) -> List<DocumentLink>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_DOCUMENT_LINK, handler)
    }

    /** Registers a handler for `documentLink/resolve` requests. */
    public fun documentLinkResolve(handler: suspend (DocumentLink) -> DocumentLink) {
        registerRequest(LspMethods.DOCUMENT_LINK_RESOLVE, handler)
    }

    /** Registers a handler for `textDocument/documentColor` requests. */
    public fun documentColor(handler: suspend (DocumentColorParams) -> List<ColorInformation>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_DOCUMENT_COLOR, handler)
    }

    /** Registers a handler for `textDocument/colorPresentation` requests. */
    public fun colorPresentation(handler: suspend (ColorPresentationParams) -> List<ColorPresentation>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_COLOR_PRESENTATION, handler)
    }

    /** Registers a handler for `textDocument/formatting` requests. */
    public fun formatting(handler: suspend (DocumentFormattingParams) -> List<TextEdit>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_FORMATTING, handler)
    }

    /** Registers a handler for `textDocument/rangeFormatting` requests. */
    public fun rangeFormatting(handler: suspend (DocumentRangeFormattingParams) -> List<TextEdit>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_RANGE_FORMATTING, handler)
    }

    /** Registers a handler for `textDocument/onTypeFormatting` requests. */
    public fun onTypeFormatting(handler: suspend (DocumentOnTypeFormattingParams) -> List<TextEdit>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_ON_TYPE_FORMATTING, handler)
    }

    /** Registers a handler for `textDocument/rename` requests. */
    public fun rename(handler: suspend (RenameParams) -> WorkspaceEdit?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_RENAME, handler)
    }

    /** Registers a handler for `textDocument/prepareRename` requests. */
    public fun prepareRename(handler: suspend (PrepareRenameParams) -> PrepareRenameResult?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_PREPARE_RENAME, handler)
    }

    /** Registers a handler for `textDocument/foldingRange` requests. */
    public fun foldingRange(handler: suspend (FoldingRangeParams) -> List<FoldingRange>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_FOLDING_RANGE, handler)
    }

    /** Registers a handler for `textDocument/selectionRange` requests. */
    public fun selectionRange(handler: suspend (SelectionRangeParams) -> List<SelectionRange>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_SELECTION_RANGE, handler)
    }

    /** Registers a handler for `textDocument/linkedEditingRange` requests. */
    public fun linkedEditingRange(handler: suspend (LinkedEditingRangeParams) -> LinkedEditingRanges?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_LINKED_EDITING_RANGE, handler)
    }

    /** Registers a handler for `textDocument/semanticTokens/full` requests. */
    public fun semanticTokensFull(handler: suspend (SemanticTokensParams) -> SemanticTokens?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL, handler)
    }

    /** Registers a handler for `textDocument/semanticTokens/full/delta` requests. */
    public fun semanticTokensFullDelta(handler: suspend (SemanticTokensDeltaParams) -> SemanticTokensDelta?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL_DELTA, handler)
    }

    /** Registers a handler for `textDocument/semanticTokens/range` requests. */
    public fun semanticTokensRange(handler: suspend (SemanticTokensRangeParams) -> SemanticTokens?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_RANGE, handler)
    }

    /** Registers a handler for `textDocument/inlayHint` requests. */
    public fun inlayHint(handler: suspend (InlayHintParams) -> List<InlayHint>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_INLAY_HINT, handler)
    }

    /** Registers a handler for `inlayHint/resolve` requests. */
    public fun inlayHintResolve(handler: suspend (InlayHint) -> InlayHint) {
        registerRequest(LspMethods.INLAY_HINT_RESOLVE, handler)
    }

    /** Registers a handler for `textDocument/diagnostic` requests. */
    public fun diagnostic(handler: suspend (DocumentDiagnosticParams) -> DocumentDiagnosticReport?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_DIAGNOSTIC, handler)
    }

    /** Registers a handler for `textDocument/moniker` requests. */
    public fun moniker(handler: suspend (MonikerParams) -> List<Moniker>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_MONIKER, handler)
    }

    /** Registers a handler for `textDocument/inlineValue` requests. */
    public fun inlineValue(handler: suspend (InlineValueParams) -> List<InlineValue>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_INLINE_VALUE, handler)
    }

    /** Registers a handler for `textDocument/inlineCompletion` requests. */
    public fun inlineCompletion(handler: suspend (InlineCompletionParams) -> InlineCompletionList?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_INLINE_COMPLETION, handler)
    }

    /** Registers a handler for `textDocument/rangesFormatting` requests. */
    public fun rangesFormatting(handler: suspend (DocumentRangesFormattingParams) -> List<TextEdit>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_RANGES_FORMATTING, handler)
    }
}

// =============================================================================
// WorkspaceHandlersBuilder
// =============================================================================

/**
 * Builder for workspace request and notification handlers.
 */
@LspServerDsl
public class WorkspaceHandlersBuilder(
    requestHandlers: MutableMap<String, RequestHandler>,
    notificationHandlers: MutableMap<String, NotificationHandler>,
) : HandlersBuilder(LspCodec.defaultJson, requestHandlers, notificationHandlers) {
    /** Registers a handler for `workspace/didChangeConfiguration` notifications. */
    public fun didChangeConfiguration(handler: suspend (DidChangeConfigurationParams) -> Unit) {
        registerNotification(LspMethods.WORKSPACE_DID_CHANGE_CONFIGURATION, handler)
    }

    /** Registers a handler for `workspace/didChangeWatchedFiles` notifications. */
    public fun didChangeWatchedFiles(handler: suspend (DidChangeWatchedFilesParams) -> Unit) {
        registerNotification(LspMethods.WORKSPACE_DID_CHANGE_WATCHED_FILES, handler)
    }

    /** Registers a handler for `workspace/didChangeWorkspaceFolders` notifications. */
    public fun didChangeWorkspaceFolders(handler: suspend (DidChangeWorkspaceFoldersParams) -> Unit) {
        registerNotification(LspMethods.WORKSPACE_DID_CHANGE_WORKSPACE_FOLDERS, handler)
    }

    /** Registers a handler for `workspace/symbol` requests. */
    public fun symbol(handler: suspend (WorkspaceSymbolParams) -> List<WorkspaceSymbol>?) {
        registerRequest(LspMethods.WORKSPACE_SYMBOL, handler)
    }

    /** Registers a handler for `workspaceSymbol/resolve` requests. */
    public fun symbolResolve(handler: suspend (WorkspaceSymbol) -> WorkspaceSymbol) {
        registerRequest(LspMethods.WORKSPACE_SYMBOL_RESOLVE, handler)
    }

    /** Registers a handler for `workspace/executeCommand` requests. */
    public fun executeCommand(handler: suspend (ExecuteCommandParams) -> JsonElement?) {
        registerRequestRaw(LspMethods.WORKSPACE_EXECUTE_COMMAND, handler)
    }

    /** Registers a handler for `workspace/diagnostic` requests. */
    public fun workspaceDiagnostic(handler: suspend (WorkspaceDiagnosticParams) -> WorkspaceDiagnosticReport?) {
        registerRequest(LspMethods.WORKSPACE_DIAGNOSTIC, handler)
    }

    /** Registers a handler for `workspace/willCreateFiles` requests. */
    public fun willCreateFiles(handler: suspend (CreateFilesParams) -> WorkspaceEdit?) {
        registerRequest(LspMethods.WORKSPACE_WILL_CREATE_FILES, handler)
    }

    /** Registers a handler for `workspace/didCreateFiles` notifications. */
    public fun didCreateFiles(handler: suspend (CreateFilesParams) -> Unit) {
        registerNotification(LspMethods.WORKSPACE_DID_CREATE_FILES, handler)
    }

    /** Registers a handler for `workspace/willRenameFiles` requests. */
    public fun willRenameFiles(handler: suspend (RenameFilesParams) -> WorkspaceEdit?) {
        registerRequest(LspMethods.WORKSPACE_WILL_RENAME_FILES, handler)
    }

    /** Registers a handler for `workspace/didRenameFiles` notifications. */
    public fun didRenameFiles(handler: suspend (RenameFilesParams) -> Unit) {
        registerNotification(LspMethods.WORKSPACE_DID_RENAME_FILES, handler)
    }

    /** Registers a handler for `workspace/willDeleteFiles` requests. */
    public fun willDeleteFiles(handler: suspend (DeleteFilesParams) -> WorkspaceEdit?) {
        registerRequest(LspMethods.WORKSPACE_WILL_DELETE_FILES, handler)
    }

    /** Registers a handler for `workspace/didDeleteFiles` notifications. */
    public fun didDeleteFiles(handler: suspend (DeleteFilesParams) -> Unit) {
        registerNotification(LspMethods.WORKSPACE_DID_DELETE_FILES, handler)
    }
}

// =============================================================================
// CallHierarchyHandlersBuilder
// =============================================================================

/**
 * Builder for call hierarchy request handlers.
 */
@LspServerDsl
public class CallHierarchyHandlersBuilder(
    requestHandlers: MutableMap<String, RequestHandler>,
) : HandlersBuilder(LspCodec.defaultJson, requestHandlers, mutableMapOf()) {
    /** Registers a handler for `textDocument/prepareCallHierarchy` requests. */
    public fun prepare(handler: suspend (CallHierarchyPrepareParams) -> List<CallHierarchyItem>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_PREPARE_CALL_HIERARCHY, handler)
    }

    /** Registers a handler for `callHierarchy/incomingCalls` requests. */
    public fun incomingCalls(handler: suspend (CallHierarchyIncomingCallsParams) -> List<CallHierarchyIncomingCall>?) {
        registerRequest(LspMethods.CALL_HIERARCHY_INCOMING_CALLS, handler)
    }

    /** Registers a handler for `callHierarchy/outgoingCalls` requests. */
    public fun outgoingCalls(handler: suspend (CallHierarchyOutgoingCallsParams) -> List<CallHierarchyOutgoingCall>?) {
        registerRequest(LspMethods.CALL_HIERARCHY_OUTGOING_CALLS, handler)
    }
}

// =============================================================================
// TypeHierarchyHandlersBuilder
// =============================================================================

/**
 * Builder for type hierarchy request handlers.
 */
@LspServerDsl
public class TypeHierarchyHandlersBuilder(
    requestHandlers: MutableMap<String, RequestHandler>,
) : HandlersBuilder(LspCodec.defaultJson, requestHandlers, mutableMapOf()) {
    /** Registers a handler for `textDocument/prepareTypeHierarchy` requests. */
    public fun prepare(handler: suspend (TypeHierarchyPrepareParams) -> List<TypeHierarchyItem>?) {
        registerRequest(LspMethods.TEXT_DOCUMENT_PREPARE_TYPE_HIERARCHY, handler)
    }

    /** Registers a handler for `typeHierarchy/supertypes` requests. */
    public fun supertypes(handler: suspend (TypeHierarchySupertypesParams) -> List<TypeHierarchyItem>?) {
        registerRequest(LspMethods.TYPE_HIERARCHY_SUPERTYPES, handler)
    }

    /** Registers a handler for `typeHierarchy/subtypes` requests. */
    public fun subtypes(handler: suspend (TypeHierarchySubtypesParams) -> List<TypeHierarchyItem>?) {
        registerRequest(LspMethods.TYPE_HIERARCHY_SUBTYPES, handler)
    }
}

// =============================================================================
// NotebookDocumentHandlersBuilder
// =============================================================================

/**
 * Builder for notebookDocument notification handlers.
 */
@LspServerDsl
public class NotebookDocumentHandlersBuilder(
    notificationHandlers: MutableMap<String, NotificationHandler>,
) : HandlersBuilder(LspCodec.defaultJson, mutableMapOf(), notificationHandlers) {
    /** Registers a handler for `notebookDocument/didOpen` notifications. */
    public fun didOpen(handler: suspend (DidOpenNotebookDocumentParams) -> Unit) {
        registerNotification(LspMethods.NOTEBOOK_DOCUMENT_DID_OPEN, handler)
    }

    /** Registers a handler for `notebookDocument/didChange` notifications. */
    public fun didChange(handler: suspend (DidChangeNotebookDocumentParams) -> Unit) {
        registerNotification(LspMethods.NOTEBOOK_DOCUMENT_DID_CHANGE, handler)
    }

    /** Registers a handler for `notebookDocument/didSave` notifications. */
    public fun didSave(handler: suspend (DidSaveNotebookDocumentParams) -> Unit) {
        registerNotification(LspMethods.NOTEBOOK_DOCUMENT_DID_SAVE, handler)
    }

    /** Registers a handler for `notebookDocument/didClose` notifications. */
    public fun didClose(handler: suspend (DidCloseNotebookDocumentParams) -> Unit) {
        registerNotification(LspMethods.NOTEBOOK_DOCUMENT_DID_CLOSE, handler)
    }
}

// =============================================================================
// WindowHandlersBuilder
// =============================================================================

/**
 * Builder for window notification handlers.
 */
@LspServerDsl
public class WindowHandlersBuilder(
    notificationHandlers: MutableMap<String, NotificationHandler>,
) : HandlersBuilder(LspCodec.defaultJson, mutableMapOf(), notificationHandlers) {
    /** Registers a handler for `window/workDoneProgress/cancel` notifications. */
    public fun workDoneProgressCancel(handler: suspend (WorkDoneProgressCancelParams) -> Unit) {
        registerNotification(LspMethods.WINDOW_WORK_DONE_PROGRESS_CANCEL, handler)
    }
}

// =============================================================================
// LanguageServer
// =============================================================================

/**
 * A running language server instance.
 *
 * Created via [LanguageServerConfig.start]. Manages the LSP lifecycle
 * (initialize, initialized, shutdown, exit) and delegates registered
 * handlers to the underlying [Connection].
 *
 * @property client A proxy for sending requests and notifications to the
 *   connected LSP client.
 * @property traceLevel The current trace level set by the client via `$/setTrace`.
 */
public class LanguageServer internal constructor(
    private val config: LanguageServerConfig,
    private val connection: Connection,
    private val scope: CoroutineScope,
) {
    @Volatile
    private var initialized = false

    @Volatile
    private var shutdownRequested = false

    /**
     * The current trace level set by the client via `$/setTrace`.
     */
    @Volatile
    public var traceLevel: TraceValue = TraceValue.Off
        private set

    /**
     * Client proxy for sending requests/notifications to the client.
     */
    public val client: LanguageClient = LanguageClient(connection)

    /**
     * Validates that the server is initialized and not shutting down.
     *
     * @throws LspException with [ResponseError.INVALID_REQUEST] if shutdown has
     *   been requested, or [ResponseError.SERVER_NOT_INITIALIZED] if the server
     *   has not yet received `initialized`.
     */
    private fun checkServerState(method: String) {
        if (shutdownRequested) {
            throw LspException(
                ResponseError.INVALID_REQUEST,
                "Server is shutting down, cannot process request: $method",
            )
        }
        if (!initialized) {
            throw LspException(
                ResponseError.SERVER_NOT_INITIALIZED,
                "Server not initialized",
            )
        }
    }

    internal suspend fun setup() {
        connection.onRequest(LspMethods.INITIALIZE) { params ->
            handleInitialize(params)
        }

        connection.onNotification(LspMethods.INITIALIZED) { _ ->
            initialized = true
        }

        connection.onRequest(LspMethods.SHUTDOWN) { _ ->
            shutdownRequested = true
            null
        }

        connection.onNotification(LspMethods.EXIT) { _ ->
            scope.cancel()
        }

        connection.onNotification(LspMethods.SET_TRACE) { params ->
            val setTraceParams =
                params?.let {
                    LspCodec.defaultJson.decodeFromJsonElement(SetTraceParams.serializer(), it)
                }
            if (setTraceParams != null) {
                traceLevel = setTraceParams.value
            }
        }

        // Register user-provided request handlers with state checks
        config.requestHandlers.forEach { (method, handler) ->
            connection.onRequest(method) { params ->
                checkServerState(method)
                handler.handle(params)
            }
        }

        // Register user-provided notification handlers with state checks
        config.notificationHandlers.forEach { (method, handler) ->
            connection.onNotification(method) { params ->
                checkServerState(method)
                handler.handle(params)
            }
        }
    }

    private fun handleInitialize(params: JsonElement?): JsonElement {
        val json = LspCodec.defaultJson
        val initParams = params?.let { json.decodeFromJsonElement(InitializeParams.serializer(), it) }

        // Set initial trace level from initialize params
        initParams?.trace?.let { traceLevel = it }

        val result =
            InitializeResult(
                capabilities = config.capabilities,
                serverInfo = config.serverInfo,
            )

        return json.encodeToJsonElement(InitializeResult.serializer(), result)
    }
}

// =============================================================================
// LanguageClient
// =============================================================================

/**
 * Client proxy for sending requests and notifications to the LSP client.
 *
 * An instance is available via [LanguageServer.client] after the server is
 * started.
 */
public class LanguageClient internal constructor(
    @PublishedApi internal val connection: Connection,
) {
    @PublishedApi
    internal val json: Json = LspCodec.defaultJson

    @PublishedApi
    internal suspend inline fun <reified P> sendNotification(
        method: String,
        params: P,
    ) {
        connection.notify(method, json.encodeToJsonElement(serializer<P>(), params))
    }

    @PublishedApi
    internal suspend inline fun <reified P, reified R> sendRequest(
        method: String,
        params: P,
    ): R? {
        val jsonParams = json.encodeToJsonElement(serializer<P>(), params)
        val result = connection.request<R>(method, jsonParams)
        return result?.let { json.decodeFromJsonElement(serializer<R>(), it) }
    }

    /**
     * Publishes diagnostics for a document to the client.
     */
    public suspend fun publishDiagnostics(params: PublishDiagnosticsParams) {
        sendNotification(LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS, params)
    }

    /**
     * Shows a message to the user via `window/showMessage`.
     */
    public suspend fun showMessage(
        type: MessageType,
        message: String,
    ) {
        sendNotification(LspMethods.WINDOW_SHOW_MESSAGE, ShowMessageParams(type, message))
    }

    /**
     * Logs a message via `window/logMessage`.
     */
    public suspend fun logMessage(
        type: MessageType,
        message: String,
    ) {
        sendNotification(LspMethods.WINDOW_LOG_MESSAGE, LogMessageParams(type, message))
    }

    /**
     * Requests the client to apply a workspace edit via `workspace/applyEdit`.
     */
    public suspend fun applyEdit(params: ApplyWorkspaceEditParams): ApplyWorkspaceEditResult? =
        sendRequest(LspMethods.WORKSPACE_APPLY_EDIT, params)

    /**
     * Requests a code lens refresh via `workspace/codeLens/refresh`.
     */
    public suspend fun codeLensRefresh() {
        connection.request<Unit>(LspMethods.WORKSPACE_CODE_LENS_REFRESH, null)
    }

    /**
     * Requests a semantic tokens refresh via `workspace/semanticTokens/refresh`.
     */
    public suspend fun semanticTokensRefresh() {
        connection.request<Unit>(LspMethods.WORKSPACE_SEMANTIC_TOKENS_REFRESH, null)
    }

    /**
     * Requests an inlay hint refresh via `workspace/inlayHint/refresh`.
     */
    public suspend fun inlayHintRefresh() {
        connection.request<Unit>(LspMethods.WORKSPACE_INLAY_HINT_REFRESH, null)
    }

    /**
     * Shows a message request to the user and waits for a response.
     */
    public suspend fun showMessageRequest(params: ShowMessageRequestParams): MessageActionItem? =
        sendRequest(LspMethods.WINDOW_SHOW_MESSAGE_REQUEST, params)

    /**
     * Requests the client to show a document via `window/showDocument`.
     */
    public suspend fun showDocument(params: ShowDocumentParams): ShowDocumentResult? = sendRequest(LspMethods.WINDOW_SHOW_DOCUMENT, params)

    /**
     * Sends a telemetry event to the client via `telemetry/event`.
     */
    public suspend fun telemetryEvent(data: JsonElement) {
        connection.notify(LspMethods.TELEMETRY_EVENT, data)
    }

    /**
     * Sends a log trace notification via `$/logTrace`.
     */
    public suspend fun logTrace(params: LogTraceParams) {
        sendNotification(LspMethods.LOG_TRACE, params)
    }

    /**
     * Registers a capability with the client via `client/registerCapability`.
     */
    public suspend fun registerCapability(params: RegistrationParams) {
        connection.request<JsonElement?>(
            LspMethods.CLIENT_REGISTER_CAPABILITY,
            json.encodeToJsonElement(RegistrationParams.serializer(), params),
        )
    }

    /**
     * Unregisters a capability with the client via `client/unregisterCapability`.
     */
    public suspend fun unregisterCapability(params: UnregistrationParams) {
        connection.request<JsonElement?>(
            LspMethods.CLIENT_UNREGISTER_CAPABILITY,
            json.encodeToJsonElement(UnregistrationParams.serializer(), params),
        )
    }

    /**
     * Requests workspace folders from the client.
     */
    public suspend fun workspaceFolders(): List<WorkspaceFolder>? {
        val result = connection.request<JsonElement?>(LspMethods.WORKSPACE_WORKSPACE_FOLDERS, null)
        return result?.let { json.decodeFromJsonElement(ListSerializer(WorkspaceFolder.serializer()), it) }
    }

    /**
     * Requests configuration from the client via `workspace/configuration`.
     */
    public suspend fun configuration(params: ConfigurationParams): List<JsonElement> {
        val result =
            connection.request<JsonElement?>(
                LspMethods.WORKSPACE_CONFIGURATION,
                json.encodeToJsonElement(ConfigurationParams.serializer(), params),
            )
        return result?.let { json.decodeFromJsonElement(ListSerializer(JsonElement.serializer()), it) } ?: emptyList()
    }

    /**
     * Requests an inline value refresh via `workspace/inlineValue/refresh`.
     */
    public suspend fun inlineValueRefresh() {
        connection.request<Unit>(LspMethods.WORKSPACE_INLINE_VALUE_REFRESH, null)
    }

    /**
     * Requests a diagnostic refresh via `workspace/diagnostic/refresh`.
     */
    public suspend fun diagnosticRefresh() {
        connection.request<Unit>(LspMethods.WORKSPACE_DIAGNOSTIC_REFRESH, null)
    }

    /**
     * Requests a folding range refresh via `workspace/foldingRange/refresh`.
     */
    public suspend fun foldingRangeRefresh() {
        connection.request<Unit>(LspMethods.WORKSPACE_FOLDING_RANGE_REFRESH, null)
    }

    /**
     * Creates a work done progress token via `window/workDoneProgress/create`.
     */
    public suspend fun createWorkDoneProgress(params: WorkDoneProgressCreateParams) {
        connection.request<Unit>(
            LspMethods.WINDOW_WORK_DONE_PROGRESS_CREATE,
            json.encodeToJsonElement(WorkDoneProgressCreateParams.serializer(), params),
        )
    }

    /**
     * Sends a progress notification via `$/progress`.
     */
    public suspend fun progress(params: ProgressParams<JsonElement>) {
        connection.notify(
            LspMethods.PROGRESS,
            json.encodeToJsonElement(ProgressParams.serializer(JsonElement.serializer()), params),
        )
    }
}

// =============================================================================
// DSL entry point & start
// =============================================================================

/**
 * DSL entry point for creating a [LanguageServerConfig].
 *
 * Example:
 * ```kotlin
 * val config = languageServer {
 *     serverInfo("my-server", "1.0.0")
 *     capabilities {
 *         hoverProvider = true
 *         completionProvider = CompletionOptions(triggerCharacters = listOf("."))
 *     }
 *     textDocument {
 *         hover { params -> Hover(MarkupContent(MarkupKind.PlainText, "Hello!")) }
 *     }
 * }
 * ```
 */
public fun languageServer(block: LanguageServerBuilder.() -> Unit): LanguageServerConfig = LanguageServerBuilder().apply(block).build()

/**
 * Starts a language server with the given [transport] and optional [scope].
 *
 * The returned [LanguageServer] is fully wired: incoming transport data is
 * decoded and dispatched, and outgoing messages are forwarded to the transport.
 */
public suspend fun LanguageServerConfig.start(
    transport: Transport,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
): LanguageServer {
    val connection = Connection(scope = scope)
    val server = LanguageServer(this, connection, scope)
    server.setup()

    scope.launch {
        transport.incoming.collect { data ->
            connection.receive(data)
        }
    }

    scope.launch {
        connection.outgoing.collect { data ->
            transport.send(data)
        }
    }

    return server
}
