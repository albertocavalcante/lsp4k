package io.lsp4k.server

import io.jsonrpc4k.core.BoundedDispatchPolicy
import io.jsonrpc4k.core.JsonRpcCodec
import io.jsonrpc4k.transport.Transport
import io.lsp4k.protocol.ClientCapabilities
import io.lsp4k.protocol.InitializeParams
import io.lsp4k.protocol.ProgressToken
import io.lsp4k.protocol.TraceValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import kotlin.test.assertEquals

/** The production wire configuration, used to encode test inputs exactly like a real peer would. */
internal val testJson: Json = JsonRpcCodec.defaultJson

/** A method no server registers; its MethodNotFound answer proves that earlier messages were processed. */
private const val SYNC_METHOD = "lsp4k/test/sync"

/**
 * A scripted LSP client that talks framed JSON-RPC to a server over in-memory channels.
 *
 * Responses complete the matching [request]; server notifications and server requests are queued in
 * [notifications] and [requests].
 */
internal class ClientPeer(
    scope: CoroutineScope,
) {
    private val toServer = Channel<ByteArray>(Channel.UNLIMITED)
    private val toClient = Channel<ByteArray>(Channel.UNLIMITED)
    private val pending = mutableMapOf<Int, CompletableDeferred<JsonObject>>()
    private var lastId = 0

    /** The server side of the in-memory connection. */
    val transport: Transport = ChannelTransport(toServer, toClient)

    /** Notifications sent by the server, in order. */
    val notifications: Channel<JsonObject> = Channel(Channel.UNLIMITED)

    /** Requests sent by the server, in order; answer them with [respond]. */
    val requests: Channel<JsonObject> = Channel(Channel.UNLIMITED)

    /** The server under test, set by [startPeer]. */
    lateinit var server: LanguageServer

    init {
        scope.launch {
            for (bytes in toClient) route(Json.parseToJsonElement(bytes.decodeToString().substringAfter("\r\n\r\n")).jsonObject)
        }
    }

    private fun route(message: JsonObject) {
        val id = message["id"]
        when {
            "method" in message && id == null -> notifications.trySend(message)
            "method" in message -> requests.trySend(message)
            else -> {
                val key = id?.jsonPrimitive?.intOrNull
                if (key != null) pending.remove(key)?.complete(message)
            }
        }
    }

    /** Sends a request with raw [params] and waits for its response. */
    suspend fun request(
        method: String,
        params: JsonElement? = null,
    ): JsonObject = begin(method, params).second.await()

    /** Sends a request with raw [params] without waiting; returns its id and the pending response. */
    suspend fun begin(
        method: String,
        params: JsonElement? = null,
    ): Pair<Int, CompletableDeferred<JsonObject>> {
        val id = ++lastId
        val response = CompletableDeferred<JsonObject>()
        pending[id] = response
        write(
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", id)
                put("method", method)
                if (params != null) put("params", params)
            },
        )
        return id to response
    }

    /** Sends `$/cancelRequest` for the request [id]. */
    suspend fun cancel(id: Int) {
        notify("\$/cancelRequest", buildJsonObject { put("id", id) })
    }

    /** Sends a notification with raw [params]. */
    suspend fun notify(
        method: String,
        params: JsonElement? = null,
    ) {
        write(
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("method", method)
                if (params != null) put("params", params)
            },
        )
    }

    /** Answers a server [request] with [result]. */
    suspend fun respond(
        request: JsonObject,
        result: JsonElement = JsonNull,
    ) {
        write(
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", request.getValue("id"))
                put("result", result)
            },
        )
    }

    /** Answers a server [request] with an error. */
    suspend fun respondError(
        request: JsonObject,
        code: Int,
        message: String,
    ) {
        write(
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", request.getValue("id"))
                put(
                    "error",
                    buildJsonObject {
                        put("code", code)
                        put("message", message)
                    },
                )
            },
        )
    }

    /** Closes the client's output, so the server's transport reports the end of input. */
    fun closeOutput() {
        toServer.close()
    }

    /** Waits until the server processed every message sent before. */
    suspend fun sync() {
        request(SYNC_METHOD)
    }

    /** Sends `initialize` with [params] followed by `initialized`, returning the `initialize` response. */
    suspend fun initialize(params: InitializeParams = initializeParams()): JsonObject {
        val response = request("initialize", testJson.encodeToJsonElement(InitializeParams.serializer(), params))
        notify("initialized", JsonObject(emptyMap()))
        return response
    }

    private suspend fun write(message: JsonObject) {
        val body = message.toString().encodeToByteArray()
        toServer.send("Content-Length: ${body.size}\r\n\r\n".encodeToByteArray() + body)
    }
}

/** Starts a server configured by [block] that talks to a new [ClientPeer]. */
internal suspend fun startPeer(
    scope: CoroutineScope,
    policy: BoundedDispatchPolicy = DEFAULT_DISPATCH_POLICY,
    block: LanguageServerBuilder.() -> Unit,
): ClientPeer {
    val peer = ClientPeer(scope)
    peer.server = languageServer(block).start(peer.transport, scope, policy)
    return peer
}

/** Minimal `initialize` parameters. */
internal fun initializeParams(
    trace: TraceValue? = null,
    workDoneToken: ProgressToken? = null,
): InitializeParams =
    InitializeParams(
        processId = null,
        rootUri = null,
        capabilities = ClientCapabilities(),
        trace = trace,
        workDoneToken = workDoneToken,
    )

/** Sends a typed request, asserts success and decodes its result; JSON `null` decodes to null. */
internal suspend inline fun <reified P, reified R> ClientPeer.call(
    method: String,
    params: P,
): R? {
    val response = request(method, testJson.encodeToJsonElement(serializer<P>(), params))
    val result = response.result()
    return if (result is JsonNull) null else testJson.decodeFromJsonElement(serializer<R>(), result)
}

/**
 * Sends a typed request and asserts that its decoded result equals [expected]. [R] is inferred from [expected], so
 * give it the declared result type (not a subtype of a sealed result).
 */
internal suspend inline fun <reified P, reified R> ClientPeer.expect(
    method: String,
    params: P,
    expected: R,
) {
    assertEquals(expected, call<P, R>(method, params), method)
}

/**
 * Runs [call] in [scope], expects it to send the request [method] with [params], answers it with [result] and
 * returns the value of [call].
 */
internal suspend fun <T> ClientPeer.answer(
    scope: CoroutineScope,
    method: String,
    params: JsonElement?,
    result: JsonElement = JsonNull,
    call: suspend () -> T,
): T {
    val pending = scope.async { call() }
    val request = requests.receive()
    assertEquals(method, request.method())
    assertEquals(params, request["params"], method)
    respond(request, result)
    return pending.await()
}

/** Asserts that [message] has [method] and the literal JSON [params]. */
internal fun assertMessage(
    message: JsonObject,
    method: String,
    params: String,
) {
    assertEquals(method, message.method())
    assertEquals(json(params), message["params"], method)
}

/** Sends a typed notification. */
internal suspend inline fun <reified P> ClientPeer.send(
    method: String,
    params: P,
) {
    notify(method, testJson.encodeToJsonElement(serializer<P>(), params))
}

/** Encodes [value] with the production configuration. */
internal inline fun <reified T> wire(value: T): JsonElement = testJson.encodeToJsonElement(serializer<T>(), value)

/** Parses literal JSON. */
internal fun json(text: String): JsonElement = Json.parseToJsonElement(text)

/** The `result` member of a successful response. */
internal fun JsonObject.result(): JsonElement {
    assertEquals(null, this["error"], "unexpected error response")
    return checkNotNull(this["result"]) { "response without result: $this" }
}

/** The `error.code` member of an error response, or null. */
internal fun JsonObject.errorCode(): Int? = (this["error"] as? JsonObject)?.get("code")?.jsonPrimitive?.int

/** The `method` member of a message. */
internal fun JsonObject.method(): String = getValue("method").jsonPrimitive.content

/** The `params` member of a message as an object. */
internal fun JsonObject.params(): JsonObject = getValue("params").jsonObject

private class ChannelTransport(
    private val inbound: Channel<ByteArray>,
    private val outbound: Channel<ByteArray>,
) : Transport {
    override val incoming: Flow<ByteArray> = inbound.receiveAsFlow()
    override val isConnected: Boolean = true

    override suspend fun send(data: ByteArray) {
        outbound.send(data)
    }

    override suspend fun close() = Unit
}
