package io.lsp4k.server

import io.jsonrpc4k.core.JsonRpcCodec
import io.jsonrpc4k.core.JsonRpcException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.serializer

internal val wireJson: Json = JsonRpcCodec.defaultJson

/** Collects the handlers registered while a [LanguageServerBuilder] block runs. */
internal class HandlerRegistry {
    val requests: MutableMap<String, ServerRequestHandler> = mutableMapOf()
    val notifications: MutableMap<String, ServerNotificationHandler> = mutableMapOf()

    fun putRequest(
        method: String,
        handler: ServerRequestHandler,
    ) {
        require(method !in requests) { "Request handler already registered for method: $method" }
        requests[method] = handler
    }

    fun putNotification(
        method: String,
        handler: ServerNotificationHandler,
    ) {
        require(method !in notifications) { "Notification handler already registered for method: $method" }
        notifications[method] = handler
    }
}

/**
 * Base of the namespace builders: adapts typed handlers to [ServerRequestHandler] and [ServerNotificationHandler].
 *
 * Parameters that are missing or do not decode are answered with `InvalidParams`; a null handler result is sent
 * as JSON `null`.
 */
@LspServerDsl
public abstract class HandlersBuilder internal constructor(
    internal val registry: HandlerRegistry,
) {
    internal inline fun <reified P, reified R> request(
        method: String,
        noinline handler: suspend RequestContext.(P) -> R?,
    ) {
        registry.putRequest(method, typedRequest(method, serializer<P>(), serializer<R>(), handler))
    }

    internal inline fun <reified P, reified R, T> partialRequest(
        method: String,
        partials: KSerializer<T>,
        noinline handler: suspend PartialResultContext<T>.(P) -> R?,
    ) {
        registry.putRequest(method, typedPartialRequest(method, serializer<P>(), serializer<R>(), partials, handler))
    }

    internal inline fun <reified P> notification(
        method: String,
        noinline handler: suspend HandlerContext.(P) -> Unit,
    ) {
        registry.putNotification(method, typedNotification(method, serializer<P>(), handler))
    }
}

/** Decodes the params of [method], answering absent or malformed params with `InvalidParams`. */
internal fun <P> decodeParams(
    method: String,
    serializer: KSerializer<P>,
    params: JsonElement?,
): P {
    if (params == null || params is JsonNull) throw JsonRpcException.invalidParams("Missing params for $method")
    return try {
        wireJson.decodeFromJsonElement(serializer, params)
    } catch (expected: IllegalArgumentException) {
        throw JsonRpcException.invalidParams("Invalid params for $method: ${expected.message}")
    }
}

internal fun <P, R> typedRequest(
    method: String,
    params: KSerializer<P>,
    result: KSerializer<R>,
    handler: suspend RequestContext.(P) -> R?,
): ServerRequestHandler =
    ServerRequestHandler { context, raw ->
        val value = context.handler(decodeParams(method, params, raw))
        value?.let { wireJson.encodeToJsonElement(result, it) }
    }

internal fun <P, R, T> typedPartialRequest(
    method: String,
    params: KSerializer<P>,
    result: KSerializer<R>,
    partials: KSerializer<T>,
    handler: suspend PartialResultContext<T>.(P) -> R?,
): ServerRequestHandler =
    ServerRequestHandler { context, raw ->
        val typed = decodeParams(method, params, raw)
        val partial = PartialResultContext(context, progressTokenOf(raw, PARTIAL_RESULT_TOKEN), partials)
        val value =
            try {
                partial.handler(typed)
            } finally {
                partial.close()
            }
        value?.let { wireJson.encodeToJsonElement(result, it) }
    }

internal fun <P> typedNotification(
    method: String,
    params: KSerializer<P>,
    handler: suspend HandlerContext.(P) -> Unit,
): ServerNotificationHandler = ServerNotificationHandler { context, raw -> context.handler(decodeParams(method, params, raw)) }
