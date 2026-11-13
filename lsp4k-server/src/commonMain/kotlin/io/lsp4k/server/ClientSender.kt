package io.lsp4k.server

import io.jsonrpc4k.core.Connection
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.ProgressParams
import io.lsp4k.protocol.ProgressToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_TIMEOUT_SECONDS = 30

/** How long a request to the client waits for its response unless a call passes another timeout. */
internal val DEFAULT_REQUEST_TIMEOUT: Duration = DEFAULT_TIMEOUT_SECONDS.seconds

/** Encodes and sends the requests and notifications of the [LanguageClient] call groups. */
internal class ClientSender(
    private val connection: Connection,
) {
    private val idMutex = Mutex()
    private var lastId = 0L

    suspend fun notify(
        method: String,
        params: JsonElement?,
    ) {
        connection.notify(method, params)
    }

    suspend fun <P> notify(
        method: String,
        params: P,
        serializer: KSerializer<P>,
    ) {
        connection.notify(method, wireJson.encodeToJsonElement(serializer, params))
    }

    suspend fun <T> progress(
        token: ProgressToken,
        value: T,
        serializer: KSerializer<T>,
    ) {
        notify(LspMethods.PROGRESS, ProgressParams(token, value), ProgressParams.serializer(serializer))
    }

    /** Sends a request and returns its result, mapping JSON `null` to null. */
    suspend fun request(
        method: String,
        params: JsonElement?,
        timeout: Duration,
    ): JsonElement? {
        val result = connection.request<JsonElement>(method, params, timeout)
        return if (result is JsonNull) null else result
    }

    suspend fun <P> request(
        method: String,
        params: P,
        serializer: KSerializer<P>,
        timeout: Duration,
    ): JsonElement? = request(method, wireJson.encodeToJsonElement(serializer, params), timeout)

    fun <R : Any> decodeOrNull(
        result: JsonElement?,
        serializer: KSerializer<R>,
    ): R? = result?.let { wireJson.decodeFromJsonElement(serializer, it) }

    fun <R : Any> decode(
        method: String,
        result: JsonElement?,
        serializer: KSerializer<R>,
    ): R = decodeOrNull(result, serializer) ?: throw SerializationException("The client answered $method without a result")

    suspend fun nextId(prefix: String): String = idMutex.withLock { "$prefix-${++lastId}" }
}
