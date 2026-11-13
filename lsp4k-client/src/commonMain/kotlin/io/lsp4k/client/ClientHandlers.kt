package io.lsp4k.client

import io.jsonrpc4k.core.JsonRpcCodec
import io.jsonrpc4k.core.JsonRpcException
import io.jsonrpc4k.core.NotificationHandler
import io.jsonrpc4k.core.RequestHandler
import io.lsp4k.protocol.ApplyWorkspaceEditParams
import io.lsp4k.protocol.ApplyWorkspaceEditResult
import io.lsp4k.protocol.ConfigurationParams
import io.lsp4k.protocol.LogMessageParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.MessageActionItem
import io.lsp4k.protocol.PublishDiagnosticsParams
import io.lsp4k.protocol.RegistrationParams
import io.lsp4k.protocol.ShowDocumentParams
import io.lsp4k.protocol.ShowDocumentResult
import io.lsp4k.protocol.ShowMessageParams
import io.lsp4k.protocol.ShowMessageRequestParams
import io.lsp4k.protocol.TextDocumentContentRefreshParams
import io.lsp4k.protocol.UnregistrationParams
import io.lsp4k.protocol.WorkDoneProgressCreateParams
import io.lsp4k.protocol.WorkspaceFolder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

@PublishedApi
internal fun <H> MutableMap<String, H>.putUniqueHandler(
    kind: String,
    method: String,
    handler: H,
) {
    require(method !in this) {
        "$kind handler already registered for method: $method"
    }
    this[method] = handler
}

/**
 * Base for the namespaced handler builders of a language client. Each handler hook serves one
 * server-to-client request or notification; registering the same method twice fails.
 */
@LspClientDsl
public abstract class ClientHandlersBuilder internal constructor(
    @PublishedApi internal val requestHandlers: MutableMap<String, RequestHandler>,
    @PublishedApi internal val notificationHandlers: MutableMap<String, NotificationHandler>,
) {
    @PublishedApi
    internal val json: Json = JsonRpcCodec.defaultJson

    /** Registers a notification handler that decodes [P]; a notification without params is ignored. */
    @PublishedApi
    internal inline fun <reified P> registerNotification(
        method: String,
        crossinline handler: suspend (P) -> Unit,
    ) {
        val paramSerializer = serializer<P>()
        notificationHandlers.putUniqueHandler(
            "Notification",
            method,
            NotificationHandler { params ->
                val typed = params?.let { json.decodeFromJsonElement(paramSerializer, it) }
                if (typed != null) handler(typed)
            },
        )
    }

    /** Registers a request handler that decodes [P] and encodes a nullable [R]. */
    @PublishedApi
    internal inline fun <reified P, reified R> registerRequest(
        method: String,
        crossinline handler: suspend (P) -> R?,
    ) {
        val paramSerializer = serializer<P>()
        val resultSerializer = serializer<R>()
        requestHandlers.putUniqueHandler(
            "Request",
            method,
            RequestHandler { params ->
                val typed =
                    params?.let { json.decodeFromJsonElement(paramSerializer, it) }
                        ?: throw JsonRpcException.invalidParams("Missing params for $method")
                handler(typed)?.let { json.encodeToJsonElement(resultSerializer, it) }
            },
        )
    }

    /** Registers a request handler that decodes [P] and answers with a `null` result. */
    @PublishedApi
    internal inline fun <reified P> registerUnitRequest(
        method: String,
        crossinline handler: suspend (P) -> Unit,
    ) {
        registerRequest<P, Unit>(method) {
            handler(it)
            null
        }
    }

    /** Registers a request handler without params that answers with a `null` result (refresh requests). */
    @PublishedApi
    internal fun registerRefresh(
        method: String,
        handler: suspend () -> Unit,
    ) {
        requestHandlers.putUniqueHandler(
            "Request",
            method,
            RequestHandler { _ ->
                handler()
                null
            },
        )
    }

    /** Registers a request handler without params whose result is a list. */
    internal fun <T> registerListRequest(
        method: String,
        itemSerializer: KSerializer<T>,
        handler: suspend () -> List<T>?,
    ) {
        requestHandlers.putUniqueHandler(
            "Request",
            method,
            RequestHandler { _ ->
                handler()?.let { json.encodeToJsonElement(ListSerializer(itemSerializer), it) }
            },
        )
    }

    /** Registers a request handler for a raw JSON result. */
    internal inline fun <reified P> registerRawRequest(
        method: String,
        crossinline handler: suspend (P) -> JsonElement?,
    ) {
        val paramSerializer = serializer<P>()
        requestHandlers.putUniqueHandler(
            "Request",
            method,
            RequestHandler { params ->
                val typed =
                    params?.let { json.decodeFromJsonElement(paramSerializer, it) }
                        ?: throw JsonRpcException.invalidParams("Missing params for $method")
                handler(typed)
            },
        )
    }
}

/**
 * Handlers for the `window` namespace requests and notifications sent by the server.
 */
@LspClientDsl
public class WindowHandlersBuilder internal constructor(
    requestHandlers: MutableMap<String, RequestHandler>,
    notificationHandlers: MutableMap<String, NotificationHandler>,
) : ClientHandlersBuilder(requestHandlers, notificationHandlers) {
    /** Handles `window/showMessage` notifications. */
    public fun showMessage(handler: suspend (ShowMessageParams) -> Unit) {
        registerNotification(LspMethods.WINDOW_SHOW_MESSAGE, handler)
    }

    /** Handles `window/logMessage` notifications. */
    public fun logMessage(handler: suspend (LogMessageParams) -> Unit) {
        registerNotification(LspMethods.WINDOW_LOG_MESSAGE, handler)
    }

    /** Handles `window/showMessageRequest` requests; return the chosen action or `null` when dismissed. */
    public fun showMessageRequest(handler: suspend (ShowMessageRequestParams) -> MessageActionItem?) {
        registerRequest(LspMethods.WINDOW_SHOW_MESSAGE_REQUEST, handler)
    }

    /** Handles `window/showDocument` requests. */
    public fun showDocument(handler: suspend (ShowDocumentParams) -> ShowDocumentResult) {
        registerRequest(LspMethods.WINDOW_SHOW_DOCUMENT, handler)
    }

    /**
     * Handles `window/workDoneProgress/create` requests. The server may report progress for the
     * token once this handler returns, so subscribe with [ProgressStreams.workDone] inside it.
     */
    public fun workDoneProgressCreate(handler: suspend (WorkDoneProgressCreateParams) -> Unit) {
        registerUnitRequest(LspMethods.WINDOW_WORK_DONE_PROGRESS_CREATE, handler)
    }
}

/**
 * Handlers for the `workspace` namespace requests sent by the server, including every refresh request.
 */
@LspClientDsl
public class WorkspaceHandlersBuilder internal constructor(
    requestHandlers: MutableMap<String, RequestHandler>,
    notificationHandlers: MutableMap<String, NotificationHandler>,
) : ClientHandlersBuilder(requestHandlers, notificationHandlers) {
    /** Handles `workspace/applyEdit` requests. */
    public fun applyEdit(handler: suspend (ApplyWorkspaceEditParams) -> ApplyWorkspaceEditResult) {
        registerRequest(LspMethods.WORKSPACE_APPLY_EDIT, handler)
    }

    /** Handles `workspace/configuration` requests; return one value per requested item. */
    public fun configuration(handler: suspend (ConfigurationParams) -> List<JsonElement>) {
        registerRawRequest<ConfigurationParams>(LspMethods.WORKSPACE_CONFIGURATION) { params ->
            json.encodeToJsonElement(ListSerializer(JsonElement.serializer()), handler(params))
        }
    }

    /** Handles `workspace/workspaceFolders` requests; return `null` when no folder is open. */
    public fun workspaceFolders(handler: suspend () -> List<WorkspaceFolder>?) {
        registerListRequest(LspMethods.WORKSPACE_WORKSPACE_FOLDERS, WorkspaceFolder.serializer(), handler)
    }

    /** Handles `workspace/codeLens/refresh` requests. */
    public fun codeLensRefresh(handler: suspend () -> Unit) {
        registerRefresh(LspMethods.WORKSPACE_CODE_LENS_REFRESH, handler)
    }

    /** Handles `workspace/semanticTokens/refresh` requests. */
    public fun semanticTokensRefresh(handler: suspend () -> Unit) {
        registerRefresh(LspMethods.WORKSPACE_SEMANTIC_TOKENS_REFRESH, handler)
    }

    /** Handles `workspace/inlayHint/refresh` requests. */
    public fun inlayHintRefresh(handler: suspend () -> Unit) {
        registerRefresh(LspMethods.WORKSPACE_INLAY_HINT_REFRESH, handler)
    }

    /** Handles `workspace/inlineValue/refresh` requests. */
    public fun inlineValueRefresh(handler: suspend () -> Unit) {
        registerRefresh(LspMethods.WORKSPACE_INLINE_VALUE_REFRESH, handler)
    }

    /** Handles `workspace/diagnostic/refresh` requests. */
    public fun diagnosticRefresh(handler: suspend () -> Unit) {
        registerRefresh(LspMethods.WORKSPACE_DIAGNOSTIC_REFRESH, handler)
    }

    /** Handles `workspace/foldingRange/refresh` requests. */
    public fun foldingRangeRefresh(handler: suspend () -> Unit) {
        registerRefresh(LspMethods.WORKSPACE_FOLDING_RANGE_REFRESH, handler)
    }

    /** Handles `workspace/textDocumentContent/refresh` requests. */
    public fun textDocumentContentRefresh(handler: suspend (TextDocumentContentRefreshParams) -> Unit) {
        registerUnitRequest(LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT_REFRESH, handler)
    }
}

/**
 * Handlers for the `textDocument` namespace notifications sent by the server.
 */
@LspClientDsl
public class TextDocumentHandlersBuilder internal constructor(
    requestHandlers: MutableMap<String, RequestHandler>,
    notificationHandlers: MutableMap<String, NotificationHandler>,
) : ClientHandlersBuilder(requestHandlers, notificationHandlers) {
    /** Handles `textDocument/publishDiagnostics` notifications. */
    public fun publishDiagnostics(handler: suspend (PublishDiagnosticsParams) -> Unit) {
        registerNotification(LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS, handler)
    }
}

/**
 * Handlers for the `client` namespace dynamic registration requests sent by the server.
 */
@LspClientDsl
public class ClientCapabilityHandlersBuilder internal constructor(
    requestHandlers: MutableMap<String, RequestHandler>,
    notificationHandlers: MutableMap<String, NotificationHandler>,
) : ClientHandlersBuilder(requestHandlers, notificationHandlers) {
    /** Handles `client/registerCapability` requests. */
    public fun registerCapability(handler: suspend (RegistrationParams) -> Unit) {
        registerUnitRequest(LspMethods.CLIENT_REGISTER_CAPABILITY, handler)
    }

    /** Handles `client/unregisterCapability` requests. */
    public fun unregisterCapability(handler: suspend (UnregistrationParams) -> Unit) {
        registerUnitRequest(LspMethods.CLIENT_UNREGISTER_CAPABILITY, handler)
    }
}
