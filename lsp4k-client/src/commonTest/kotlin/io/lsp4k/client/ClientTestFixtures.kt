package io.lsp4k.client

import io.jsonrpc4k.core.Connection
import io.jsonrpc4k.core.JsonRpcCodec
import io.jsonrpc4k.core.NotificationHandler
import io.jsonrpc4k.core.RequestHandler
import io.jsonrpc4k.transport.Transport
import io.lsp4k.protocol.ApplyWorkspaceEditResult
import io.lsp4k.protocol.ShowDocumentResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal const val DOC: String = """"textDocument":{"uri":"file:///a.kt"}"""
internal const val POS: String = """"position":{"line":1,"character":2}"""
internal const val LINE_RANGE: String = """{"start":{"line":0,"character":0},"end":{"line":1,"character":0}}"""
internal const val ONE_RANGE: String = """{"start":{"line":0,"character":0},"end":{"line":0,"character":1}}"""
internal const val RNG: String = """"range":$LINE_RANGE"""
internal const val OPTS: String = """"options":{"tabSize":4,"insertSpaces":true}"""
internal const val CHI: String = """{"name":"n","kind":12,"uri":"file:///a.kt",$RNG,"selectionRange":$ONE_RANGE}"""
internal const val NB_ID: String = """"notebookDocument":{"uri":"file:///n.ipynb"}"""

/** A server stand-in: a jsonrpc4k connection wired to the client under test through in-memory frames. */
internal class TestPeer(
    scope: CoroutineScope,
) {
    val connection = Connection(scope = scope)
    private val toClient = Channel<ByteArray>(Channel.UNLIMITED)

    /** The transport handed to the client; frames it sends are delivered to [connection]. */
    val transport: Transport =
        object : Transport {
            override val incoming: Flow<ByteArray> = toClient.receiveAsFlow()
            override val isConnected: Boolean = true

            override suspend fun send(data: ByteArray) {
                connection.receive(data)
            }

            override suspend fun close() = Unit
        }

    init {
        scope.launch { connection.outgoing.collect { toClient.send(it) } }
    }
}

internal data class Received(
    val method: String,
    val params: JsonElement?,
)

/** One client-to-server call: the expected method, the expected wire params and how to make the call. */
internal class CallCase(
    val method: String,
    val params: String,
    val request: Boolean,
    val call: suspend (ServerProxy) -> Unit,
)

internal inline fun <reified P> requestCase(
    method: String,
    params: String,
    noinline call: suspend ServerProxy.(P) -> Any?,
): CallCase = CallCase(method, params, true) { it.call(JsonRpcCodec.defaultJson.decodeFromString(serializer<P>(), params)) }

internal inline fun <reified P> notificationCase(
    method: String,
    params: String,
    noinline call: suspend ServerProxy.(P) -> Any?,
): CallCase = CallCase(method, params, false) { it.call(JsonRpcCodec.defaultJson.decodeFromString(serializer<P>(), params)) }

/** Makes every call against a peer that records what arrives and checks method and params. */
internal suspend fun runCases(
    scope: CoroutineScope,
    cases: List<CallCase>,
) {
    val peer = TestPeer(scope)
    val received = Channel<Received>(Channel.UNLIMITED)
    for (case in cases) {
        if (case.request) {
            peer.connection.onRequest(
                case.method,
                RequestHandler { params ->
                    received.send(Received(case.method, params))
                    null
                },
            )
        } else {
            peer.connection.onNotification(
                case.method,
                NotificationHandler { params -> received.send(Received(case.method, params)) },
            )
        }
    }
    val client = languageClient {}.start(peer.transport, scope)
    for (case in cases) {
        case.call(client.server)
        val got = received.receive()
        assertEquals(case.method, got.method)
        assertContainsAll(Json.parseToJsonElement(case.params), got.params, case.method)
    }
    client.close()
}

/** Asserts that [actual] carries every property of [expected]; extra properties are allowed. */
internal fun assertContainsAll(
    expected: JsonElement,
    actual: JsonElement?,
    path: String,
) {
    if (expected is JsonObject) {
        assertTrue(actual is JsonObject, "$path: expected an object but was $actual")
        for ((key, value) in expected) assertContainsAll(value, actual[key], "$path.$key")
    } else {
        assertEquals(expected, actual, path)
    }
}

internal fun isNullResult(result: JsonElement?): Boolean = result == null || result is JsonNull

/** Registers a no-op hook for every server-to-client method the client DSL supports. */
internal fun LanguageClientBuilder.registerEveryHook() {
    window {
        showMessage { }
        logMessage { }
        showMessageRequest { null }
        showDocument { ShowDocumentResult(success = true) }
        workDoneProgressCreate { }
    }
    workspace {
        applyEdit { ApplyWorkspaceEditResult(applied = true) }
        configuration { emptyList() }
        workspaceFolders { null }
        codeLensRefresh { }
        semanticTokensRefresh { }
        inlayHintRefresh { }
        inlineValueRefresh { }
        diagnosticRefresh { }
        foldingRangeRefresh { }
        textDocumentContentRefresh { }
    }
    textDocument { publishDiagnostics { } }
    client {
        registerCapability { }
        unregisterCapability { }
    }
    onTelemetryEvent { }
    onLogTrace { }
    onProgress { }
}

/** Sends a request from the server stand-in to the client and returns the raw result. */
internal suspend fun TestPeer.call(
    method: String,
    params: String?,
): JsonElement? = connection.request<JsonElement>(method, params?.let { Json.parseToJsonElement(it) })

/** Sends a notification from the server stand-in to the client. */
internal suspend fun TestPeer.send(
    method: String,
    params: String?,
) {
    connection.notify(method, params?.let { Json.parseToJsonElement(it) })
}
