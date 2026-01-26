package io.lsp4k.jsonrpc

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A handler for JSON-RPC requests.
 */
public fun interface RequestHandler {
    /**
     * Handle a request and return a result.
     * @param params The request parameters (may be null)
     * @return The result to send back (may be null for void methods)
     * @throws Exception If an error occurs, it will be converted to a ResponseError
     */
    public suspend fun handle(params: JsonElement?): JsonElement?
}

/**
 * A handler for JSON-RPC notifications.
 */
public fun interface NotificationHandler {
    /**
     * Handle a notification.
     * @param params The notification parameters (may be null)
     */
    public suspend fun handle(params: JsonElement?)
}

/**
 * Dispatches incoming JSON-RPC messages to registered handlers.
 */
public class Dispatcher(
    @PublishedApi internal val json: Json = LspCodec.defaultJson,
) {
    private val requestHandlers = mutableMapOf<String, RequestHandler>()
    private val notificationHandlers = mutableMapOf<String, NotificationHandler>()
    private val pendingRequests = mutableMapOf<RequestId, CompletableDeferred<JsonElement?>>()

    /**
     * Register a request handler for a method.
     */
    public fun onRequest(
        method: String,
        handler: RequestHandler,
    ) {
        requestHandlers[method] = handler
    }

    /**
     * Register a notification handler for a method.
     */
    public fun onNotification(
        method: String,
        handler: NotificationHandler,
    ) {
        notificationHandlers[method] = handler
    }

    /**
     * Register a typed request handler.
     */
    public inline fun <reified P, reified R> onRequest(
        method: String,
        crossinline handler: suspend (P) -> R,
    ) {
        onRequest(method) { params ->
            val typedParams: P =
                if (params != null) {
                    json.decodeFromJsonElement(kotlinx.serialization.serializer(), params)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    null as P
                }
            val result = handler(typedParams)
            if (result != null) {
                json.encodeToJsonElement(kotlinx.serialization.serializer<R>(), result)
            } else {
                null
            }
        }
    }

    /**
     * Register a typed notification handler.
     */
    public inline fun <reified P> onNotification(
        method: String,
        crossinline handler: suspend (P) -> Unit,
    ) {
        onNotification(method) { params ->
            val typedParams: P =
                if (params != null) {
                    json.decodeFromJsonElement(kotlinx.serialization.serializer(), params)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    null as P
                }
            handler(typedParams)
        }
    }

    /**
     * Dispatch an incoming message.
     * Returns a response message if the input was a request, null otherwise.
     */
    public suspend fun dispatch(message: Message): ResponseMessage? =
        when (message) {
            is RequestMessage -> handleRequest(message)
            is NotificationMessage -> {
                handleNotification(message)
                null
            }
            is ResponseMessage -> {
                handleResponse(message)
                null
            }
        }

    /**
     * Register a pending request that expects a response.
     * Returns a Deferred that will complete when the response arrives.
     */
    public fun registerPendingRequest(id: RequestId): CompletableDeferred<JsonElement?> {
        val deferred = CompletableDeferred<JsonElement?>()
        pendingRequests[id] = deferred
        return deferred
    }

    /**
     * Cancel a pending request.
     */
    public fun cancelPendingRequest(id: RequestId) {
        pendingRequests.remove(id)?.cancel()
    }

    private suspend fun handleRequest(request: RequestMessage): ResponseMessage {
        val handler =
            requestHandlers[request.method]
                ?: return ResponseMessage.error(request.id, ResponseError.methodNotFound(request.method))

        return try {
            val result = handler.handle(request.params)
            ResponseMessage.success(request.id, result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: LspException) {
            ResponseMessage.error(request.id, e.toResponseError())
        } catch (e: Exception) {
            ResponseMessage.error(
                request.id,
                ResponseError.internalError(e.message ?: "Unknown error"),
            )
        }
    }

    private suspend fun handleNotification(notification: NotificationMessage) {
        val handler = notificationHandlers[notification.method] ?: return
        try {
            handler.handle(notification.params)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Notifications don't return errors, but we might want to log
        }
    }

    private fun handleResponse(response: ResponseMessage) {
        val id = response.id ?: return
        val deferred = pendingRequests.remove(id) ?: return

        if (response.error != null) {
            deferred.completeExceptionally(
                LspException(
                    response.error.code,
                    response.error.message,
                    response.error.data,
                ),
            )
        } else {
            deferred.complete(response.result)
        }
    }
}

/**
 * Exception representing an LSP error.
 */
public class LspException(
    public val code: Int,
    override val message: String,
    public val data: JsonElement? = null,
) : Exception(message) {
    public fun toResponseError(): ResponseError = ResponseError(code, message, data)

    public companion object {
        public fun methodNotFound(method: String): LspException = LspException(ResponseError.METHOD_NOT_FOUND, "Method not found: $method")

        public fun invalidParams(message: String): LspException = LspException(ResponseError.INVALID_PARAMS, message)

        public fun internalError(message: String): LspException = LspException(ResponseError.INTERNAL_ERROR, message)
    }
}

/**
 * Utility to await a response with timeout.
 */
public suspend fun CompletableDeferred<JsonElement?>.awaitWithTimeout(timeout: Duration = 30.seconds): JsonElement? =
    withTimeout(timeout) {
        await()
    }
