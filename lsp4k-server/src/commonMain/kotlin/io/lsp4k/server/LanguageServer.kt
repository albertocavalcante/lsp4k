package io.lsp4k.server

import io.lsp4k.jsonrpc.Connection
import io.lsp4k.jsonrpc.LspCodec
import io.lsp4k.jsonrpc.LspException
import io.lsp4k.jsonrpc.LspMethods
import io.lsp4k.jsonrpc.NotificationHandler
import io.lsp4k.jsonrpc.RequestHandler
import io.lsp4k.protocol.CompletionList
import io.lsp4k.protocol.CompletionOptions
import io.lsp4k.protocol.CompletionParams
import io.lsp4k.protocol.DidChangeTextDocumentParams
import io.lsp4k.protocol.DidCloseTextDocumentParams
import io.lsp4k.protocol.DidOpenTextDocumentParams
import io.lsp4k.protocol.Hover
import io.lsp4k.protocol.HoverParams
import io.lsp4k.protocol.InitializeParams
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.LogMessageParams
import io.lsp4k.protocol.MessageType
import io.lsp4k.protocol.PublishDiagnosticsParams
import io.lsp4k.protocol.ServerCapabilities
import io.lsp4k.protocol.ServerInfo
import io.lsp4k.protocol.ShowMessageParams
import io.lsp4k.protocol.TextDocumentSyncKind
import io.lsp4k.protocol.TextDocumentSyncOptions
import io.lsp4k.transport.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Builder DSL for configuring a language server.
 */
@DslMarker
public annotation class LspServerDsl

/**
 * Configuration holder for a language server.
 */
public data class LanguageServerConfig(
    val serverInfo: ServerInfo?,
    val capabilities: ServerCapabilities,
    val requestHandlers: Map<String, RequestHandler>,
    val notificationHandlers: Map<String, NotificationHandler>,
)

/**
 * Configuration builder for a language server.
 */
@LspServerDsl
public class LanguageServerBuilder {
    private var serverInfo: ServerInfo? = null
    private var capabilities = ServerCapabilities()
    private val requestHandlers = mutableMapOf<String, RequestHandler>()
    private val notificationHandlers = mutableMapOf<String, NotificationHandler>()

    /**
     * Set server information.
     */
    public fun serverInfo(
        name: String,
        version: String? = null,
    ) {
        serverInfo = ServerInfo(name, version)
    }

    /**
     * Configure server capabilities.
     */
    public fun capabilities(block: ServerCapabilitiesBuilder.() -> Unit) {
        capabilities = ServerCapabilitiesBuilder().apply(block).build()
    }

    /**
     * Configure text document handlers.
     */
    public fun textDocument(block: TextDocumentHandlersBuilder.() -> Unit) {
        TextDocumentHandlersBuilder(requestHandlers, notificationHandlers).apply(block)
    }

    /**
     * Configure workspace handlers.
     */
    public fun workspace(block: WorkspaceHandlersBuilder.() -> Unit) {
        WorkspaceHandlersBuilder(requestHandlers, notificationHandlers).apply(block)
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

    /**
     * Add a custom notification handler.
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

/**
 * Builder for server capabilities.
 */
@LspServerDsl
public class ServerCapabilitiesBuilder {
    public var textDocumentSync: TextDocumentSyncKind? = null
    public var completionProvider: CompletionOptions? = null
    public var hoverProvider: Boolean = false
    public var definitionProvider: Boolean = false
    public var referencesProvider: Boolean = false
    public var documentSymbolProvider: Boolean = false
    public var workspaceSymbolProvider: Boolean = false
    public var codeActionProvider: Boolean = false
    public var documentFormattingProvider: Boolean = false
    public var renameProvider: Boolean = false

    internal fun build(): ServerCapabilities =
        ServerCapabilities(
            textDocumentSync =
                textDocumentSync?.let {
                    TextDocumentSyncOptions(
                        openClose = true,
                        change = it,
                    )
                },
            completionProvider = completionProvider,
            hoverProvider = hoverProvider,
            definitionProvider = definitionProvider,
            referencesProvider = referencesProvider,
            documentSymbolProvider = documentSymbolProvider,
            workspaceSymbolProvider = workspaceSymbolProvider,
            codeActionProvider = codeActionProvider,
            documentFormattingProvider = documentFormattingProvider,
            renameProvider = renameProvider,
        )
}

/**
 * Builder for textDocument method handlers.
 */
@LspServerDsl
public class TextDocumentHandlersBuilder(
    private val requestHandlers: MutableMap<String, RequestHandler>,
    private val notificationHandlers: MutableMap<String, NotificationHandler>,
) {
    private val json = LspCodec.defaultJson

    public fun completion(handler: suspend (CompletionParams) -> CompletionList?) {
        requestHandlers[LspMethods.TEXT_DOCUMENT_COMPLETION] =
            RequestHandler { params ->
                val typedParams =
                    params?.let { json.decodeFromJsonElement(CompletionParams.serializer(), it) }
                        ?: throw LspException.invalidParams("Missing completion params")
                val result = handler(typedParams)
                result?.let { json.encodeToJsonElement(CompletionList.serializer(), it) }
            }
    }

    public fun hover(handler: suspend (HoverParams) -> Hover?) {
        requestHandlers[LspMethods.TEXT_DOCUMENT_HOVER] =
            RequestHandler { params ->
                val typedParams =
                    params?.let { json.decodeFromJsonElement(HoverParams.serializer(), it) }
                        ?: throw LspException.invalidParams("Missing hover params")
                val result = handler(typedParams)
                result?.let { json.encodeToJsonElement(Hover.serializer(), it) }
            }
    }

    public fun didOpen(handler: suspend (DidOpenTextDocumentParams) -> Unit) {
        notificationHandlers[LspMethods.TEXT_DOCUMENT_DID_OPEN] =
            NotificationHandler { params ->
                val typedParams =
                    params?.let { json.decodeFromJsonElement(DidOpenTextDocumentParams.serializer(), it) }
                        ?: throw LspException.invalidParams("Missing didOpen params")
                handler(typedParams)
            }
    }

    public fun didClose(handler: suspend (DidCloseTextDocumentParams) -> Unit) {
        notificationHandlers[LspMethods.TEXT_DOCUMENT_DID_CLOSE] =
            NotificationHandler { params ->
                val typedParams =
                    params?.let { json.decodeFromJsonElement(DidCloseTextDocumentParams.serializer(), it) }
                        ?: throw LspException.invalidParams("Missing didClose params")
                handler(typedParams)
            }
    }

    public fun didChange(handler: suspend (DidChangeTextDocumentParams) -> Unit) {
        notificationHandlers[LspMethods.TEXT_DOCUMENT_DID_CHANGE] =
            NotificationHandler { params ->
                val typedParams =
                    params?.let { json.decodeFromJsonElement(DidChangeTextDocumentParams.serializer(), it) }
                        ?: throw LspException.invalidParams("Missing didChange params")
                handler(typedParams)
            }
    }
}

/**
 * Builder for workspace method handlers.
 */
@LspServerDsl
public class WorkspaceHandlersBuilder(
    private val requestHandlers: MutableMap<String, RequestHandler>,
    private val notificationHandlers: MutableMap<String, NotificationHandler>,
) {
    // Add workspace handlers as needed
}

/**
 * A running language server instance.
 */
public class LanguageServer internal constructor(
    private val config: LanguageServerConfig,
    private val connection: Connection,
    private val scope: CoroutineScope,
) {
    @Suppress("unused")
    private var initialized = false

    @Suppress("unused")
    private var shutdownRequested = false

    /**
     * Client proxy for sending requests/notifications to the client.
     */
    public val client: LanguageClient = LanguageClient(connection)

    internal fun setup() {
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

        config.requestHandlers.forEach { (method, handler) ->
            connection.onRequest(method, handler)
        }

        config.notificationHandlers.forEach { (method, handler) ->
            connection.onNotification(method, handler)
        }
    }

    private fun handleInitialize(params: JsonElement?): JsonElement {
        val json = LspCodec.defaultJson

        @Suppress("UNUSED_VARIABLE")
        val initParams = params?.let { json.decodeFromJsonElement(InitializeParams.serializer(), it) }

        val result =
            InitializeResult(
                capabilities = config.capabilities,
                serverInfo = config.serverInfo,
            )

        return json.encodeToJsonElement(InitializeResult.serializer(), result)
    }
}

/**
 * Client proxy for sending requests/notifications to the LSP client.
 */
public class LanguageClient internal constructor(
    private val connection: Connection,
) {
    private val json = LspCodec.defaultJson

    /**
     * Publish diagnostics to the client.
     */
    public suspend fun publishDiagnostics(params: PublishDiagnosticsParams) {
        connection.notify(
            LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS,
            json.encodeToJsonElement(PublishDiagnosticsParams.serializer(), params),
        )
    }

    /**
     * Show a message to the user.
     */
    public suspend fun showMessage(
        type: MessageType,
        message: String,
    ) {
        connection.notify(
            LspMethods.WINDOW_SHOW_MESSAGE,
            json.encodeToJsonElement(ShowMessageParams.serializer(), ShowMessageParams(type, message)),
        )
    }

    /**
     * Log a message.
     */
    public suspend fun logMessage(
        type: MessageType,
        message: String,
    ) {
        connection.notify(
            LspMethods.WINDOW_LOG_MESSAGE,
            json.encodeToJsonElement(LogMessageParams.serializer(), LogMessageParams(type, message)),
        )
    }
}

/**
 * DSL entry point for creating a language server.
 */
public fun languageServer(block: LanguageServerBuilder.() -> Unit): LanguageServerConfig = LanguageServerBuilder().apply(block).build()

/**
 * Start a language server with the given configuration and transport.
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
