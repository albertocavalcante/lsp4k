package io.lsp4k.jsonrpc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A bidirectional JSON-RPC connection.
 *
 * This class manages the full lifecycle of a JSON-RPC connection, including:
 * - Sending requests and awaiting responses
 * - Sending notifications (fire-and-forget)
 * - Receiving and dispatching incoming messages
 * - Request ID generation
 */
public class Connection(
    private val codec: LspCodec = LspCodec.Default,
    @PublishedApi internal val json: Json = LspCodec.defaultJson,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) {
    @PublishedApi internal val dispatcher: Dispatcher = Dispatcher(json)
    private var requestIdValue = 0L
    private val requestIdMutex = Mutex()
    private val outgoingMessages = Channel<Message>(Channel.BUFFERED)
    private val decoder = LspMessageDecoder(codec)

    /**
     * Flow of outgoing messages that need to be sent over the transport.
     */
    public val outgoing: Flow<ByteArray> =
        outgoingMessages
            .receiveAsFlow()
            .map { codec.encode(it) }

    /**
     * Register a request handler for a method.
     */
    public fun onRequest(
        method: String,
        handler: RequestHandler,
    ) {
        dispatcher.onRequest(method, handler)
    }

    /**
     * Register a notification handler for a method.
     */
    public fun onNotification(
        method: String,
        handler: NotificationHandler,
    ) {
        dispatcher.onNotification(method, handler)
    }

    /**
     * Register a typed request handler.
     */
    public inline fun <reified P, reified R> onRequest(
        method: String,
        crossinline handler: suspend (P) -> R,
    ) {
        dispatcher.onRequest(method, handler)
    }

    /**
     * Register a typed notification handler.
     */
    public inline fun <reified P> onNotification(
        method: String,
        crossinline handler: suspend (P) -> Unit,
    ) {
        dispatcher.onNotification(method, handler)
    }

    /**
     * Send a request and await the response.
     */
    private suspend fun nextRequestId(): Long = requestIdMutex.withLock { ++requestIdValue }

    public suspend fun <R> request(
        method: String,
        params: JsonElement?,
        timeout: Duration = 30.seconds,
    ): JsonElement? {
        val id = RequestId.of(nextRequestId())
        val request = RequestMessage(id = id, method = method, params = params)

        val deferred = dispatcher.registerPendingRequest(id)
        outgoingMessages.send(request)

        return try {
            deferred.awaitWithTimeout(timeout)
        } catch (e: TimeoutCancellationException) {
            dispatcher.cancelPendingRequest(id)
            throw LspException(ResponseError.REQUEST_CANCELLED, "Request timed out: $method")
        }
    }

    /**
     * Send a typed request and await the response.
     */
    public suspend inline fun <reified P, reified R> request(
        method: String,
        params: P,
        timeout: Duration = 30.seconds,
    ): R? {
        val jsonParams =
            if (params != null) {
                json.encodeToJsonElement(kotlinx.serialization.serializer<P>(), params)
            } else {
                null
            }
        val result = request<R>(method, jsonParams, timeout)
        return if (result != null) {
            json.decodeFromJsonElement(kotlinx.serialization.serializer<R>(), result)
        } else {
            null
        }
    }

    /**
     * Send a notification (no response expected).
     */
    public suspend fun notify(
        method: String,
        params: JsonElement?,
    ) {
        val notification = NotificationMessage(method = method, params = params)
        outgoingMessages.send(notification)
    }

    /**
     * Send a typed notification.
     */
    public suspend inline fun <reified P> notify(
        method: String,
        params: P,
    ) {
        val jsonParams =
            if (params != null) {
                json.encodeToJsonElement(kotlinx.serialization.serializer<P>(), params)
            } else {
                null
            }
        notify(method, jsonParams)
    }

    /**
     * Feed incoming data from the transport.
     * This will decode messages and dispatch them to handlers.
     */
    public suspend fun receive(data: ByteArray) {
        val messages = decoder.feed(data)
        for (message in messages) {
            val response = dispatcher.dispatch(message)
            if (response != null) {
                outgoingMessages.send(response)
            }
        }
    }

    /**
     * Feed incoming data from the transport.
     */
    public suspend fun receive(data: String) {
        receive(data.encodeToByteArray())
    }

    /**
     * Close the connection and cancel all pending operations.
     */
    public fun close() {
        outgoingMessages.close()
        scope.cancel()
    }
}

/**
 * LSP-specific method names.
 */
public object LspMethods {
    // Lifecycle
    public const val INITIALIZE: String = "initialize"
    public const val INITIALIZED: String = "initialized"
    public const val SHUTDOWN: String = "shutdown"
    public const val EXIT: String = "exit"

    // Text Document
    public const val TEXT_DOCUMENT_DID_OPEN: String = "textDocument/didOpen"
    public const val TEXT_DOCUMENT_DID_CLOSE: String = "textDocument/didClose"
    public const val TEXT_DOCUMENT_DID_CHANGE: String = "textDocument/didChange"
    public const val TEXT_DOCUMENT_DID_SAVE: String = "textDocument/didSave"
    public const val TEXT_DOCUMENT_COMPLETION: String = "textDocument/completion"
    public const val TEXT_DOCUMENT_HOVER: String = "textDocument/hover"
    public const val TEXT_DOCUMENT_DEFINITION: String = "textDocument/definition"
    public const val TEXT_DOCUMENT_REFERENCES: String = "textDocument/references"
    public const val TEXT_DOCUMENT_DOCUMENT_SYMBOL: String = "textDocument/documentSymbol"
    public const val TEXT_DOCUMENT_FORMATTING: String = "textDocument/formatting"
    public const val TEXT_DOCUMENT_RANGE_FORMATTING: String = "textDocument/rangeFormatting"
    public const val TEXT_DOCUMENT_CODE_ACTION: String = "textDocument/codeAction"
    public const val TEXT_DOCUMENT_CODE_LENS: String = "textDocument/codeLens"
    public const val TEXT_DOCUMENT_RENAME: String = "textDocument/rename"
    public const val TEXT_DOCUMENT_SIGNATURE_HELP: String = "textDocument/signatureHelp"
    public const val TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS: String = "textDocument/publishDiagnostics"

    // Workspace
    public const val WORKSPACE_DID_CHANGE_CONFIGURATION: String = "workspace/didChangeConfiguration"
    public const val WORKSPACE_DID_CHANGE_WATCHED_FILES: String = "workspace/didChangeWatchedFiles"
    public const val WORKSPACE_SYMBOL: String = "workspace/symbol"
    public const val WORKSPACE_EXECUTE_COMMAND: String = "workspace/executeCommand"
    public const val WORKSPACE_APPLY_EDIT: String = "workspace/applyEdit"

    // Window
    public const val WINDOW_SHOW_MESSAGE: String = "window/showMessage"
    public const val WINDOW_SHOW_MESSAGE_REQUEST: String = "window/showMessageRequest"
    public const val WINDOW_LOG_MESSAGE: String = "window/logMessage"
    public const val WINDOW_WORK_DONE_PROGRESS_CREATE: String = "window/workDoneProgress/create"

    // Client
    public const val CLIENT_REGISTER_CAPABILITY: String = "client/registerCapability"
    public const val CLIENT_UNREGISTER_CAPABILITY: String = "client/unregisterCapability"

    // Cancellation
    public const val CANCEL_REQUEST: String = "$/cancelRequest"
    public const val PROGRESS: String = "$/progress"
}
