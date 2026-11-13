package io.lsp4k.client

import io.jsonrpc4k.core.JsonRpcCodec
import io.jsonrpc4k.core.NotificationHandler
import io.jsonrpc4k.core.RequestHandler
import io.lsp4k.protocol.ClientCapabilities
import io.lsp4k.protocol.ClientInfo
import io.lsp4k.protocol.LogTraceParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.ProgressParams
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * Marks the language client DSL scope.
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
 *
 * Handlers for server-to-client requests and notifications are registered by namespace:
 * [window], [workspace], [textDocument] and [client], plus the `$/` notifications [onProgress],
 * [onLogTrace] and `telemetry/event` ([onTelemetryEvent]). Methods outside the specification can
 * be added with [onRequest] and [onNotification].
 *
 * `$/cancelRequest` has no hook: the jsonrpc4k dispatcher consumes the inbound notification.
 */
@LspClientDsl
public class LanguageClientBuilder {
    private var clientInfo: ClientInfo? = null
    private var capabilities = ClientCapabilities()
    private var rootUri: String? = null
    private val notificationHandlers = mutableMapOf<String, NotificationHandler>()
    private val requestHandlers = mutableMapOf<String, RequestHandler>()
    private val json = JsonRpcCodec.defaultJson

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

    /** Sets the immutable client capabilities advertised during initialization. */
    public fun capabilities(value: ClientCapabilities) {
        capabilities = value
    }

    /** Registers handlers for the `window` namespace requests and notifications. */
    public fun window(block: WindowHandlersBuilder.() -> Unit) {
        WindowHandlersBuilder(requestHandlers, notificationHandlers).apply(block)
    }

    /** Registers handlers for the `workspace` namespace requests, including every refresh request. */
    public fun workspace(block: WorkspaceHandlersBuilder.() -> Unit) {
        WorkspaceHandlersBuilder(requestHandlers, notificationHandlers).apply(block)
    }

    /** Registers handlers for the `textDocument` namespace notifications. */
    public fun textDocument(block: TextDocumentHandlersBuilder.() -> Unit) {
        TextDocumentHandlersBuilder(requestHandlers, notificationHandlers).apply(block)
    }

    /** Registers handlers for `client/registerCapability` and `client/unregisterCapability`. */
    public fun client(block: ClientCapabilityHandlersBuilder.() -> Unit) {
        ClientCapabilityHandlersBuilder(requestHandlers, notificationHandlers).apply(block)
    }

    /**
     * Handle telemetry/event notifications. A notification without params is delivered as JSON null.
     */
    public fun onTelemetryEvent(handler: suspend (JsonElement) -> Unit) {
        notificationHandlers.putUniqueHandler(
            "Notification",
            LspMethods.TELEMETRY_EVENT,
            NotificationHandler { params -> handler(params ?: JsonNull) },
        )
    }

    /**
     * Handle `$/logTrace` notifications.
     */
    public fun onLogTrace(handler: suspend (LogTraceParams) -> Unit) {
        notificationHandlers.putUniqueHandler(
            "Notification",
            LspMethods.LOG_TRACE,
            NotificationHandler { params ->
                val typed = params?.let { json.decodeFromJsonElement(LogTraceParams.serializer(), it) }
                if (typed != null) handler(typed)
            },
        )
    }

    /**
     * Handle `$/progress` notifications. Use [LanguageClientSession.progress] to consume progress as
     * flows keyed by token; both receive every notification.
     */
    public fun onProgress(handler: suspend (ProgressParams<JsonElement>) -> Unit) {
        notificationHandlers.putUniqueHandler(
            "Notification",
            LspMethods.PROGRESS,
            NotificationHandler { params ->
                val typed = params?.let { decodeProgress(it) }
                if (typed != null) handler(typed)
            },
        )
    }

    /**
     * Add a custom notification handler.
     */
    public fun onNotification(
        method: String,
        handler: NotificationHandler,
    ) {
        notificationHandlers.putUniqueHandler("Notification", method, handler)
    }

    /**
     * Add a custom request handler.
     */
    public fun onRequest(
        method: String,
        handler: RequestHandler,
    ) {
        requestHandlers.putUniqueHandler("Request", method, handler)
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

internal fun decodeProgress(params: JsonElement): ProgressParams<JsonElement> =
    JsonRpcCodec.defaultJson.decodeFromJsonElement(
        ProgressParams.serializer(JsonElement.serializer()),
        params,
    )

/**
 * DSL entry point for creating a language client.
 */
public fun languageClient(block: LanguageClientBuilder.() -> Unit): LanguageClientConfig = LanguageClientBuilder().apply(block).build()
