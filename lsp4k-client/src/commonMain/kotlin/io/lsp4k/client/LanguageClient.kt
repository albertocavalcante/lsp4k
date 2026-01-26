package io.lsp4k.client

import io.lsp4k.jsonrpc.Connection
import io.lsp4k.jsonrpc.LspCodec
import io.lsp4k.jsonrpc.LspMethods
import io.lsp4k.jsonrpc.NotificationHandler
import io.lsp4k.protocol.ClientCapabilities
import io.lsp4k.protocol.ClientInfo
import io.lsp4k.protocol.Diagnostic
import io.lsp4k.protocol.InitializeParams
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.MessageType
import io.lsp4k.protocol.PublishDiagnosticsParams
import io.lsp4k.protocol.ShowMessageParams
import io.lsp4k.transport.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

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
    private val json = LspCodec.defaultJson

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
        notificationHandlers[LspMethods.WINDOW_SHOW_MESSAGE] =
            NotificationHandler { params ->
                val showMessageParams =
                    params?.let {
                        json.decodeFromJsonElement(ShowMessageParams.serializer(), it)
                    }
                if (showMessageParams != null) {
                    handler(showMessageParams.type, showMessageParams.message)
                }
            }
    }

    /**
     * Handle textDocument/publishDiagnostics notifications.
     */
    public fun onPublishDiagnostics(handler: suspend (String, List<Diagnostic>) -> Unit) {
        notificationHandlers[LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS] =
            NotificationHandler { params ->
                val diagnosticsParams =
                    params?.let {
                        json.decodeFromJsonElement(PublishDiagnosticsParams.serializer(), it)
                    }
                if (diagnosticsParams != null) {
                    handler(diagnosticsParams.uri, diagnosticsParams.diagnostics)
                }
            }
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

    internal fun build(): LanguageClientConfig =
        LanguageClientConfig(
            clientInfo = clientInfo,
            capabilities = capabilities,
            rootUri = rootUri,
            notificationHandlers = notificationHandlers.toMap(),
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
    private val json = LspCodec.defaultJson

    @Suppress("unused")
    private var initialized = false

    /**
     * Server proxy for sending requests to the server.
     */
    public val server: ServerProxy = ServerProxy(connection)

    internal fun setup() {
        config.notificationHandlers.forEach { (method, handler) ->
            connection.onNotification(method, handler)
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
        initialized = true

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
 * Proxy for sending requests to the language server.
 */
public class ServerProxy internal constructor(
    private val connection: Connection,
) {
    // Server request methods can be added here as needed
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
