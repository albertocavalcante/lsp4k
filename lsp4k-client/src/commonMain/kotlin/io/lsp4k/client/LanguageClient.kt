package io.lsp4k.client

import io.jsonrpc4k.core.Connection
import io.jsonrpc4k.core.JsonRpcCodec
import io.jsonrpc4k.core.JsonRpcException
import io.jsonrpc4k.core.NotificationHandler
import io.jsonrpc4k.core.RequestHandler
import io.jsonrpc4k.transport.Transport
import io.lsp4k.protocol.ApplyWorkspaceEditParams
import io.lsp4k.protocol.ApplyWorkspaceEditResult
import io.lsp4k.protocol.CallHierarchyIncomingCall
import io.lsp4k.protocol.CallHierarchyIncomingCallsParams
import io.lsp4k.protocol.CallHierarchyItem
import io.lsp4k.protocol.CallHierarchyOutgoingCall
import io.lsp4k.protocol.CallHierarchyOutgoingCallsParams
import io.lsp4k.protocol.CallHierarchyPrepareParams
import io.lsp4k.protocol.CancelParams
import io.lsp4k.protocol.ClientCapabilities
import io.lsp4k.protocol.ClientInfo
import io.lsp4k.protocol.CodeAction
import io.lsp4k.protocol.CodeActionParams
import io.lsp4k.protocol.CodeLens
import io.lsp4k.protocol.CodeLensParams
import io.lsp4k.protocol.ColorInformation
import io.lsp4k.protocol.ColorPresentation
import io.lsp4k.protocol.ColorPresentationParams
import io.lsp4k.protocol.CompletionItem
import io.lsp4k.protocol.CompletionList
import io.lsp4k.protocol.CompletionParams
import io.lsp4k.protocol.ConfigurationParams
import io.lsp4k.protocol.DeclarationParams
import io.lsp4k.protocol.DefinitionParams
import io.lsp4k.protocol.Diagnostic
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
import io.lsp4k.protocol.DocumentLinkParams
import io.lsp4k.protocol.DocumentOnTypeFormattingParams
import io.lsp4k.protocol.DocumentRangeFormattingParams
import io.lsp4k.protocol.DocumentRangesFormattingParams
import io.lsp4k.protocol.DocumentSymbol
import io.lsp4k.protocol.DocumentSymbolParams
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
import io.lsp4k.protocol.LogMessageParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.MessageActionItem
import io.lsp4k.protocol.MessageType
import io.lsp4k.protocol.Moniker
import io.lsp4k.protocol.MonikerParams
import io.lsp4k.protocol.PrepareRenameParams
import io.lsp4k.protocol.PrepareRenameResult
import io.lsp4k.protocol.ProgressParams
import io.lsp4k.protocol.PublishDiagnosticsParams
import io.lsp4k.protocol.ReferenceParams
import io.lsp4k.protocol.RenameParams
import io.lsp4k.protocol.SelectionRange
import io.lsp4k.protocol.SelectionRangeParams
import io.lsp4k.protocol.SemanticTokens
import io.lsp4k.protocol.SemanticTokensDelta
import io.lsp4k.protocol.SemanticTokensDeltaParams
import io.lsp4k.protocol.SemanticTokensParams
import io.lsp4k.protocol.SemanticTokensRangeParams
import io.lsp4k.protocol.SetTraceParams
import io.lsp4k.protocol.ShowMessageParams
import io.lsp4k.protocol.ShowMessageRequestParams
import io.lsp4k.protocol.SignatureHelp
import io.lsp4k.protocol.SignatureHelpParams
import io.lsp4k.protocol.TextEdit
import io.lsp4k.protocol.TypeDefinitionParams
import io.lsp4k.protocol.TypeHierarchyItem
import io.lsp4k.protocol.TypeHierarchyPrepareParams
import io.lsp4k.protocol.TypeHierarchySubtypesParams
import io.lsp4k.protocol.TypeHierarchySupertypesParams
import io.lsp4k.protocol.WillSaveTextDocumentParams
import io.lsp4k.protocol.WorkspaceDiagnosticParams
import io.lsp4k.protocol.WorkspaceDiagnosticReport
import io.lsp4k.protocol.WorkspaceEdit
import io.lsp4k.protocol.WorkspaceFolder
import io.lsp4k.protocol.WorkspaceSymbol
import io.lsp4k.protocol.WorkspaceSymbolParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/**
 * Builder for configuring a language client.
 */
@DslMarker
public annotation class LspClientDsl

/**
 * Configuration for a language client.
 */
public data class LanguageClientConfig(
    val clientInfo: ClientInfo?,
    val capabilities: ClientCapabilities,
    val rootUri: String?,
    val notificationHandlers: Map<String, NotificationHandler>,
    val requestHandlers: Map<String, RequestHandler>,
)

/**
 * Builder for language client configuration.
 */
@LspClientDsl
public class LanguageClientBuilder {
    private var clientInfo: ClientInfo? = null
    private var capabilities = ClientCapabilities()
    private var rootUri: String? = null
    private val notificationHandlers = mutableMapOf<String, NotificationHandler>()
    private val requestHandlers = mutableMapOf<String, RequestHandler>()
    private val json = JsonRpcCodec.defaultJson

    private inline fun <reified P> handleNotification(
        method: String,
        crossinline extract: suspend (P) -> Unit,
    ) {
        notificationHandlers[method] =
            NotificationHandler { params ->
                val typed = params?.let { json.decodeFromJsonElement(serializer<P>(), it) }
                if (typed != null) extract(typed)
            }
    }

    private inline fun <reified P, reified R> handleRequest(
        method: String,
        crossinline handler: suspend (P) -> R?,
    ) {
        val paramSer = serializer<P>()
        val resultSer = serializer<R>()
        requestHandlers[method] =
            RequestHandler { params ->
                val typed =
                    params?.let { json.decodeFromJsonElement(paramSer, it) }
                        ?: throw JsonRpcException.invalidParams("Missing params for $method")
                val result = handler(typed)
                result?.let { json.encodeToJsonElement(resultSer, it) }
            }
    }

    /**
     * Set client information.
     */
    public fun clientInfo(
        name: String,
        version: String? = null,
    ) {
        clientInfo = ClientInfo(name, version)
    }

    /**
     * Set the root URI for the workspace.
     */
    public fun rootUri(uri: String) {
        rootUri = uri
    }

    /**
     * Configure client capabilities.
     */
    public fun capabilities(block: ClientCapabilities.() -> Unit) {
        capabilities = ClientCapabilities().apply(block)
    }

    /**
     * Handle window/showMessage notifications.
     */
    public fun onShowMessage(handler: suspend (MessageType, String) -> Unit) {
        handleNotification<ShowMessageParams>(LspMethods.WINDOW_SHOW_MESSAGE) {
            handler(it.type, it.message)
        }
    }

    /**
     * Handle textDocument/publishDiagnostics notifications.
     */
    public fun onPublishDiagnostics(handler: suspend (String, List<Diagnostic>) -> Unit) {
        handleNotification<PublishDiagnosticsParams>(LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS) {
            handler(it.uri, it.diagnostics)
        }
    }

    /**
     * Handle window/logMessage notifications.
     */
    public fun onLogMessage(handler: suspend (MessageType, String) -> Unit) {
        handleNotification<LogMessageParams>(LspMethods.WINDOW_LOG_MESSAGE) {
            handler(it.type, it.message)
        }
    }

    /**
     * Handle telemetry/event notifications.
     */
    public fun onTelemetryEvent(handler: suspend (JsonElement) -> Unit) {
        notificationHandlers[LspMethods.TELEMETRY_EVENT] =
            NotificationHandler { params ->
                if (params != null) handler(params)
            }
    }

    /**
     * Handle $/progress notifications.
     */
    public fun onProgress(handler: suspend (ProgressParams<JsonElement>) -> Unit) {
        notificationHandlers[LspMethods.PROGRESS] =
            NotificationHandler { params ->
                val progressParams =
                    params?.let {
                        json.decodeFromJsonElement(
                            ProgressParams.serializer(JsonElement.serializer()),
                            it,
                        )
                    }
                if (progressParams != null) handler(progressParams)
            }
    }

    /**
     * Handle window/showMessageRequest requests from the server.
     */
    public fun onShowMessageRequest(handler: suspend (ShowMessageRequestParams) -> MessageActionItem?) {
        handleRequest(LspMethods.WINDOW_SHOW_MESSAGE_REQUEST, handler)
    }

    /**
     * Handle workspace/applyEdit requests from the server.
     */
    public fun onApplyEdit(handler: suspend (ApplyWorkspaceEditParams) -> ApplyWorkspaceEditResult) {
        handleRequest(LspMethods.WORKSPACE_APPLY_EDIT, handler)
    }

    /**
     * Handle workspace/workspaceFolders requests from the server.
     */
    public fun onWorkspaceFolders(handler: suspend () -> List<WorkspaceFolder>?) {
        requestHandlers[LspMethods.WORKSPACE_WORKSPACE_FOLDERS] =
            RequestHandler { _ ->
                val result = handler()
                result?.let { json.encodeToJsonElement(ListSerializer(WorkspaceFolder.serializer()), it) }
            }
    }

    /**
     * Handle workspace/configuration requests from the server.
     */
    public fun onConfiguration(handler: suspend (ConfigurationParams) -> List<JsonElement>) {
        handleRequest(LspMethods.WORKSPACE_CONFIGURATION, handler)
    }

    /**
     * Add a custom notification handler.
     */
    public fun onNotification(
        method: String,
        handler: NotificationHandler,
    ) {
        notificationHandlers[method] = handler
    }

    /**
     * Add a custom request handler.
     */
    public fun onRequest(
        method: String,
        handler: RequestHandler,
    ) {
        requestHandlers[method] = handler
    }

    internal fun build(): LanguageClientConfig =
        LanguageClientConfig(
            clientInfo = clientInfo,
            capabilities = capabilities,
            rootUri = rootUri,
            notificationHandlers = notificationHandlers.toMap(),
            requestHandlers = requestHandlers.toMap(),
        )
}

/**
 * A running language client instance.
 */
public class LanguageClientSession internal constructor(
    private val config: LanguageClientConfig,
    private val connection: Connection,
    private val scope: CoroutineScope,
) {
    private val json = JsonRpcCodec.defaultJson

    /**
     * Server proxy for sending requests to the server.
     */
    public val server: ServerProxy = ServerProxy(connection)

    internal suspend fun setup() {
        config.notificationHandlers.forEach { (method, handler) ->
            connection.onNotification(method, handler)
        }
        config.requestHandlers.forEach { (method, handler) ->
            connection.onRequest(method, handler)
        }
    }

    /**
     * Initialize the connection with the server.
     */
    public suspend fun initialize(): InitializeResult {
        val params =
            InitializeParams(
                processId = null,
                rootUri = config.rootUri,
                capabilities = config.capabilities,
                clientInfo = config.clientInfo,
            )

        val result =
            connection.request<InitializeResult>(
                LspMethods.INITIALIZE,
                json.encodeToJsonElement(InitializeParams.serializer(), params),
            )

        // Send initialized notification
        connection.notify(LspMethods.INITIALIZED, null)

        return result?.let {
            json.decodeFromJsonElement(InitializeResult.serializer(), it)
        } ?: error("Initialize returned null")
    }

    /**
     * Shutdown the connection.
     */
    public suspend fun shutdown() {
        connection.request<JsonElement?>(LspMethods.SHUTDOWN, null)
        connection.notify(LspMethods.EXIT, null)
    }

    /**
     * Close the client connection.
     */
    public fun close() {
        connection.close()
    }
}

/**
 * Proxy for sending requests and notifications to the language server.
 */
public class ServerProxy internal constructor(
    private val connection: Connection,
) {
    private val json = JsonRpcCodec.defaultJson

    // ===== Private helpers =====

    private suspend inline fun <reified P, reified R> request(
        method: String,
        params: P,
    ): R? {
        val jsonParams = json.encodeToJsonElement(serializer<P>(), params)
        val result = connection.request<JsonElement>(method, jsonParams)
        return result?.let { json.decodeFromJsonElement(serializer<R>(), it) }
    }

    private suspend inline fun <reified P> notify(
        method: String,
        params: P,
    ) {
        connection.notify(method, json.encodeToJsonElement(serializer<P>(), params))
    }

    // ===== Text Document Requests =====

    /**
     * Request completion at a given text document position.
     */
    public suspend fun completion(params: CompletionParams): CompletionList? = request(LspMethods.TEXT_DOCUMENT_COMPLETION, params)

    /**
     * Resolve additional information for a given completion item.
     */
    public suspend fun completionResolve(item: CompletionItem): CompletionItem? = request(LspMethods.COMPLETION_ITEM_RESOLVE, item)

    /**
     * Request hover information at a given text document position.
     */
    public suspend fun hover(params: HoverParams): Hover? = request(LspMethods.TEXT_DOCUMENT_HOVER, params)

    /**
     * Request signature help at a given text document position.
     */
    public suspend fun signatureHelp(params: SignatureHelpParams): SignatureHelp? = request(LspMethods.TEXT_DOCUMENT_SIGNATURE_HELP, params)

    /**
     * Request the declaration of a symbol at a given text document position.
     */
    public suspend fun declaration(params: DeclarationParams): List<Location>? = request(LspMethods.TEXT_DOCUMENT_DECLARATION, params)

    /**
     * Request the definition of a symbol at a given text document position.
     */
    public suspend fun definition(params: DefinitionParams): List<Location>? = request(LspMethods.TEXT_DOCUMENT_DEFINITION, params)

    /**
     * Request the type definition of a symbol at a given text document position.
     */
    public suspend fun typeDefinition(params: TypeDefinitionParams): List<Location>? =
        request(LspMethods.TEXT_DOCUMENT_TYPE_DEFINITION, params)

    /**
     * Request the implementation of a symbol at a given text document position.
     */
    public suspend fun implementation(params: ImplementationParams): List<Location>? =
        request(LspMethods.TEXT_DOCUMENT_IMPLEMENTATION, params)

    /**
     * Request references to a symbol at a given text document position.
     */
    public suspend fun references(params: ReferenceParams): List<Location>? = request(LspMethods.TEXT_DOCUMENT_REFERENCES, params)

    /**
     * Request document highlights at a given text document position.
     */
    public suspend fun documentHighlight(params: DocumentHighlightParams): List<DocumentHighlight>? =
        request(LspMethods.TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT, params)

    /**
     * Request document symbols for a given text document.
     */
    public suspend fun documentSymbol(params: DocumentSymbolParams): List<DocumentSymbol>? =
        request(LspMethods.TEXT_DOCUMENT_DOCUMENT_SYMBOL, params)

    /**
     * Request code actions for a given text document and range.
     */
    public suspend fun codeAction(params: CodeActionParams): List<CodeAction>? = request(LspMethods.TEXT_DOCUMENT_CODE_ACTION, params)

    /**
     * Resolve additional information for a given code action.
     */
    public suspend fun codeActionResolve(codeAction: CodeAction): CodeAction? = request(LspMethods.CODE_ACTION_RESOLVE, codeAction)

    /**
     * Request code lenses for a given text document.
     */
    public suspend fun codeLens(params: CodeLensParams): List<CodeLens>? = request(LspMethods.TEXT_DOCUMENT_CODE_LENS, params)

    /**
     * Resolve additional information for a given code lens.
     */
    public suspend fun codeLensResolve(codeLens: CodeLens): CodeLens? = request(LspMethods.CODE_LENS_RESOLVE, codeLens)

    /**
     * Request document links for a given text document.
     */
    public suspend fun documentLink(params: DocumentLinkParams): List<DocumentLink>? =
        request(LspMethods.TEXT_DOCUMENT_DOCUMENT_LINK, params)

    /**
     * Resolve additional information for a given document link.
     */
    public suspend fun documentLinkResolve(link: DocumentLink): DocumentLink? = request(LspMethods.DOCUMENT_LINK_RESOLVE, link)

    /**
     * Request document colors for a given text document.
     */
    public suspend fun documentColor(params: DocumentColorParams): List<ColorInformation>? =
        request(LspMethods.TEXT_DOCUMENT_DOCUMENT_COLOR, params)

    /**
     * Request color presentations for a given color.
     */
    public suspend fun colorPresentation(params: ColorPresentationParams): List<ColorPresentation>? =
        request(LspMethods.TEXT_DOCUMENT_COLOR_PRESENTATION, params)

    /**
     * Request formatting for a whole document.
     */
    public suspend fun formatting(params: DocumentFormattingParams): List<TextEdit>? = request(LspMethods.TEXT_DOCUMENT_FORMATTING, params)

    /**
     * Request formatting for a range in a document.
     */
    public suspend fun rangeFormatting(params: DocumentRangeFormattingParams): List<TextEdit>? =
        request(LspMethods.TEXT_DOCUMENT_RANGE_FORMATTING, params)

    /**
     * Request formatting after a character has been typed.
     */
    public suspend fun onTypeFormatting(params: DocumentOnTypeFormattingParams): List<TextEdit>? =
        request(LspMethods.TEXT_DOCUMENT_ON_TYPE_FORMATTING, params)

    /**
     * Request a rename of a symbol.
     */
    public suspend fun rename(params: RenameParams): WorkspaceEdit? = request(LspMethods.TEXT_DOCUMENT_RENAME, params)

    /**
     * Request to prepare a rename.
     */
    public suspend fun prepareRename(params: PrepareRenameParams): PrepareRenameResult? =
        request(LspMethods.TEXT_DOCUMENT_PREPARE_RENAME, params)

    /**
     * Request folding ranges for a given text document.
     */
    public suspend fun foldingRange(params: FoldingRangeParams): List<FoldingRange>? =
        request(LspMethods.TEXT_DOCUMENT_FOLDING_RANGE, params)

    /**
     * Request selection ranges for a given text document.
     */
    public suspend fun selectionRange(params: SelectionRangeParams): List<SelectionRange>? =
        request(LspMethods.TEXT_DOCUMENT_SELECTION_RANGE, params)

    /**
     * Request linked editing ranges for a given text document position.
     */
    public suspend fun linkedEditingRange(params: LinkedEditingRangeParams): LinkedEditingRanges? =
        request(LspMethods.TEXT_DOCUMENT_LINKED_EDITING_RANGE, params)

    /**
     * Request semantic tokens for a full document.
     */
    public suspend fun semanticTokensFull(params: SemanticTokensParams): SemanticTokens? =
        request(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL, params)

    /**
     * Request semantic tokens delta for a full document.
     */
    public suspend fun semanticTokensFullDelta(params: SemanticTokensDeltaParams): SemanticTokensDelta? =
        request(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL_DELTA, params)

    /**
     * Request semantic tokens for a range.
     */
    public suspend fun semanticTokensRange(params: SemanticTokensRangeParams): SemanticTokens? =
        request(LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_RANGE, params)

    /**
     * Request inlay hints for a given text document range.
     */
    public suspend fun inlayHint(params: InlayHintParams): List<InlayHint>? = request(LspMethods.TEXT_DOCUMENT_INLAY_HINT, params)

    /**
     * Resolve additional information for a given inlay hint.
     */
    public suspend fun inlayHintResolve(hint: InlayHint): InlayHint? = request(LspMethods.INLAY_HINT_RESOLVE, hint)

    // ===== Call Hierarchy =====

    /**
     * Request to prepare a call hierarchy at a given text document position.
     */
    public suspend fun prepareCallHierarchy(params: CallHierarchyPrepareParams): List<CallHierarchyItem>? =
        request(LspMethods.TEXT_DOCUMENT_PREPARE_CALL_HIERARCHY, params)

    /**
     * Request incoming calls for a given call hierarchy item.
     */
    public suspend fun callHierarchyIncomingCalls(params: CallHierarchyIncomingCallsParams): List<CallHierarchyIncomingCall>? =
        request(LspMethods.CALL_HIERARCHY_INCOMING_CALLS, params)

    /**
     * Request outgoing calls for a given call hierarchy item.
     */
    public suspend fun callHierarchyOutgoingCalls(params: CallHierarchyOutgoingCallsParams): List<CallHierarchyOutgoingCall>? =
        request(LspMethods.CALL_HIERARCHY_OUTGOING_CALLS, params)

    // ===== Type Hierarchy =====

    /**
     * Request to prepare a type hierarchy at a given text document position.
     */
    public suspend fun prepareTypeHierarchy(params: TypeHierarchyPrepareParams): List<TypeHierarchyItem>? =
        request(LspMethods.TEXT_DOCUMENT_PREPARE_TYPE_HIERARCHY, params)

    /**
     * Request supertypes for a given type hierarchy item.
     */
    public suspend fun typeHierarchySupertypes(params: TypeHierarchySupertypesParams): List<TypeHierarchyItem>? =
        request(LspMethods.TYPE_HIERARCHY_SUPERTYPES, params)

    /**
     * Request subtypes for a given type hierarchy item.
     */
    public suspend fun typeHierarchySubtypes(params: TypeHierarchySubtypesParams): List<TypeHierarchyItem>? =
        request(LspMethods.TYPE_HIERARCHY_SUBTYPES, params)

    // ===== Text Document (LSP 3.17+) =====

    /**
     * Request pull diagnostics for a given text document.
     * @since 3.17.0
     */
    public suspend fun diagnostic(params: DocumentDiagnosticParams): DocumentDiagnosticReport? =
        request(LspMethods.TEXT_DOCUMENT_DIAGNOSTIC, params)

    /**
     * Request monikers for a given text document position.
     */
    public suspend fun moniker(params: MonikerParams): List<Moniker>? = request(LspMethods.TEXT_DOCUMENT_MONIKER, params)

    /**
     * Request inline values for a given text document.
     * @since 3.17.0
     */
    public suspend fun inlineValue(params: InlineValueParams): List<InlineValue>? = request(LspMethods.TEXT_DOCUMENT_INLINE_VALUE, params)

    /**
     * Request inline completions for a given text document position.
     * @since 3.18.0
     */
    public suspend fun inlineCompletion(params: InlineCompletionParams): InlineCompletionList? =
        request(LspMethods.TEXT_DOCUMENT_INLINE_COMPLETION, params)

    /**
     * Request formatting for multiple ranges in a document.
     * @since 3.18.0
     */
    public suspend fun rangesFormatting(params: DocumentRangesFormattingParams): List<TextEdit>? =
        request(LspMethods.TEXT_DOCUMENT_RANGES_FORMATTING, params)

    /**
     * Request text edits before a document is saved.
     */
    public suspend fun willSaveWaitUntil(params: WillSaveTextDocumentParams): List<TextEdit>? =
        request(LspMethods.TEXT_DOCUMENT_WILL_SAVE_WAIT_UNTIL, params)

    // ===== Workspace =====

    /**
     * Request workspace symbols matching a query.
     */
    public suspend fun symbol(params: WorkspaceSymbolParams): List<WorkspaceSymbol>? = request(LspMethods.WORKSPACE_SYMBOL, params)

    /**
     * Resolve additional information for a workspace symbol.
     */
    public suspend fun workspaceSymbolResolve(symbol: WorkspaceSymbol): WorkspaceSymbol? =
        request(LspMethods.WORKSPACE_SYMBOL_RESOLVE, symbol)

    /**
     * Request the server to execute a command.
     */
    public suspend fun executeCommand(params: ExecuteCommandParams): JsonElement? {
        val jsonParams = json.encodeToJsonElement(ExecuteCommandParams.serializer(), params)
        return connection.request<JsonElement>(LspMethods.WORKSPACE_EXECUTE_COMMAND, jsonParams)
    }

    /**
     * Request workspace diagnostics.
     * @since 3.17.0
     */
    public suspend fun workspaceDiagnostic(params: WorkspaceDiagnosticParams): WorkspaceDiagnosticReport? =
        request(LspMethods.WORKSPACE_DIAGNOSTIC, params)

    // ===== Notifications (client -> server) =====

    /**
     * Notify the server that a text document was opened.
     */
    public suspend fun didOpen(params: DidOpenTextDocumentParams): Unit = notify(LspMethods.TEXT_DOCUMENT_DID_OPEN, params)

    /**
     * Notify the server that a text document was closed.
     */
    public suspend fun didClose(params: DidCloseTextDocumentParams): Unit = notify(LspMethods.TEXT_DOCUMENT_DID_CLOSE, params)

    /**
     * Notify the server that a text document was changed.
     */
    public suspend fun didChange(params: DidChangeTextDocumentParams): Unit = notify(LspMethods.TEXT_DOCUMENT_DID_CHANGE, params)

    /**
     * Notify the server that a text document was saved.
     */
    public suspend fun didSave(params: DidSaveTextDocumentParams): Unit = notify(LspMethods.TEXT_DOCUMENT_DID_SAVE, params)

    /**
     * Notify the server that a text document will be saved.
     */
    public suspend fun willSave(params: WillSaveTextDocumentParams): Unit = notify(LspMethods.TEXT_DOCUMENT_WILL_SAVE, params)

    /**
     * Notify the server that the configuration has changed.
     */
    public suspend fun didChangeConfiguration(params: DidChangeConfigurationParams): Unit =
        notify(LspMethods.WORKSPACE_DID_CHANGE_CONFIGURATION, params)

    /**
     * Notify the server that watched files have changed.
     */
    public suspend fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit =
        notify(LspMethods.WORKSPACE_DID_CHANGE_WATCHED_FILES, params)

    /**
     * Notify the server that workspace folders have changed.
     */
    public suspend fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams): Unit =
        notify(LspMethods.WORKSPACE_DID_CHANGE_WORKSPACE_FOLDERS, params)

    // ===== Notebook Document Notifications =====

    /**
     * Notify the server that a notebook document was opened.
     */
    public suspend fun notebookDidOpen(params: DidOpenNotebookDocumentParams): Unit = notify(LspMethods.NOTEBOOK_DOCUMENT_DID_OPEN, params)

    /**
     * Notify the server that a notebook document was changed.
     */
    public suspend fun notebookDidChange(params: DidChangeNotebookDocumentParams): Unit =
        notify(LspMethods.NOTEBOOK_DOCUMENT_DID_CHANGE, params)

    /**
     * Notify the server that a notebook document was saved.
     */
    public suspend fun notebookDidSave(params: DidSaveNotebookDocumentParams): Unit = notify(LspMethods.NOTEBOOK_DOCUMENT_DID_SAVE, params)

    /**
     * Notify the server that a notebook document was closed.
     */
    public suspend fun notebookDidClose(params: DidCloseNotebookDocumentParams): Unit =
        notify(LspMethods.NOTEBOOK_DOCUMENT_DID_CLOSE, params)

    // ===== Protocol Notifications =====

    /**
     * Set the trace level on the server.
     */
    public suspend fun setTrace(params: SetTraceParams): Unit = notify(LspMethods.SET_TRACE, params)

    /**
     * Cancel a pending request.
     */
    public suspend fun cancelRequest(params: CancelParams): Unit = notify(LspMethods.CANCEL_REQUEST, params)
}

/**
 * DSL entry point for creating a language client.
 */
public fun languageClient(block: LanguageClientBuilder.() -> Unit): LanguageClientConfig = LanguageClientBuilder().apply(block).build()

/**
 * Start a language client session with the given configuration and transport.
 */
public suspend fun LanguageClientConfig.start(
    transport: Transport,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
): LanguageClientSession {
    val connection = Connection(scope = scope)
    val session = LanguageClientSession(this, connection, scope)
    session.setup()

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

    return session
}
