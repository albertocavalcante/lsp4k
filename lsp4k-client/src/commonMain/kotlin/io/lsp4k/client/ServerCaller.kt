package io.lsp4k.client

import io.jsonrpc4k.core.Connection
import io.jsonrpc4k.core.JsonRpcCodec
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.serializer

/**
 * Typed request and notification sender shared by the namespaced call groups of a [ServerProxy].
 */
internal class ServerCaller(
    private val connection: Connection,
) {
    val json = JsonRpcCodec.defaultJson

    suspend inline fun <reified P, reified R> request(
        method: String,
        params: P,
    ): R? {
        val result = requestRaw(method, params)
        return if (result == null || result is JsonNull) null else json.decodeFromJsonElement(serializer<R>(), result)
    }

    suspend inline fun <reified P> requestRaw(
        method: String,
        params: P,
    ): JsonElement? = send(method, json.encodeToJsonElement(serializer<P>(), params))

    suspend inline fun <reified P> notify(
        method: String,
        params: P,
    ) {
        sendNotification(method, json.encodeToJsonElement(serializer<P>(), params))
    }

    suspend fun send(
        method: String,
        params: JsonElement?,
    ): JsonElement? = connection.request<JsonElement>(method, params)

    suspend fun sendNotification(
        method: String,
        params: JsonElement?,
    ) {
        connection.notify(method, params)
    }
}
