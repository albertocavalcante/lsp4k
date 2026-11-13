package io.lsp4k.server.integration

import io.jsonrpc4k.transport.Transport
import io.lsp4k.client.languageClient
import io.lsp4k.client.start
import io.lsp4k.protocol.CompletionItem
import io.lsp4k.protocol.CompletionList
import io.lsp4k.protocol.CompletionParams
import io.lsp4k.protocol.CompletionResult
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.server.languageServer
import io.lsp4k.server.start
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PublicCompletionTest {
    private val params = CompletionParams(TextDocumentIdentifier("file:///test"), Position(0, 0))

    @Test
    fun `client reads literal array object and null responses`() =
        runTest {
            val transport = CompletionPeer()
            val client = languageClient {}.start(transport, backgroundScope)
            transport.result = """[{"label":"one","data":{"key":"value"}}]"""
            val items = client.server.completion(params) as CompletionResult.Items
            assertEquals("one", items.items.single().label)
            assertEquals(Json.parseToJsonElement("""{"key":"value"}"""), items.items.single().data)
            transport.result = "[]"
            assertEquals(CompletionResult.Items(emptyList()), client.server.completion(params))
            transport.result = """{"isIncomplete":true,"items":[],"itemDefaults":{"commitCharacters":["."],"data":{"shared":true}}}"""
            val list = (client.server.completion(params) as CompletionResult.ListResult).list
            assertEquals(true, list.isIncomplete)
            assertEquals(listOf("."), list.itemDefaults?.commitCharacters)
            assertEquals(Json.parseToJsonElement("""{"shared":true}"""), list.itemDefaults?.data)
            transport.result = "null"
            assertEquals(null, client.server.completion(params))
            transport.result = "42"
            assertFailsWith<IllegalArgumentException> { client.server.completion(params) }
            transport.result = "{}"
            assertFailsWith<IllegalArgumentException> { client.server.completion(params) }
            client.close()
        }

    @Test
    fun `server emits array list and null through public handlers`() =
        runTest {
            val toClient = Channel<ByteArray>(Channel.UNLIMITED)
            val toServer = Channel<ByteArray>(Channel.UNLIMITED)
            val responses = Channel<JsonObject>(Channel.UNLIMITED)
            val serverTransport = RecordingTransport(toServer, toClient, responses)
            val clientTransport = RecordingTransport(toClient, toServer, null)
            var result: CompletionResult? = CompletionResult.Items(listOf(CompletionItem(label = "one")))
            val server = languageServer { textDocument { completion { result } } }.start(serverTransport, backgroundScope)
            val client = languageClient {}.start(clientTransport, backgroundScope)
            client.initialize()
            responses.receive() // initialize
            client.server.completion(params)
            assertEquals(Json.parseToJsonElement("""[{"label":"one"}]"""), responses.receive()["result"])
            result = CompletionResult.ListResult(CompletionList(true, emptyList()))
            client.server.completion(params)
            assertEquals(Json.parseToJsonElement("""{"isIncomplete":true,"items":[]}"""), responses.receive()["result"])
            result = null
            assertEquals(null, client.server.completion(params))
            val nullResponse = responses.receive()
            assertTrue("result" in nullResponse)
            assertEquals(JsonNull, nullResponse["result"])
            client.close()
            server.close()
        }
}

private class CompletionPeer : Transport {
    private val inbound = Channel<ByteArray>(Channel.UNLIMITED)
    var result: String = "null"
    override val incoming: Flow<ByteArray> = inbound.receiveAsFlow()
    override val isConnected: Boolean = true

    override suspend fun send(data: ByteArray) {
        val request = readFrame(data)
        val body = """{"jsonrpc":"2.0","id":${request["id"]},"result":$result}"""
        val bytes = body.encodeToByteArray()
        inbound.send("Content-Length: ${bytes.size}\r\n\r\n".encodeToByteArray() + bytes)
    }

    override suspend fun close() = Unit
}

private class RecordingTransport(
    incomingChannel: Channel<ByteArray>,
    private val outgoingChannel: Channel<ByteArray>,
    private val responses: Channel<JsonObject>?,
) : Transport {
    override val incoming: Flow<ByteArray> = incomingChannel.receiveAsFlow()
    override val isConnected: Boolean = true

    override suspend fun send(data: ByteArray) {
        if (responses != null) {
            val message = readFrame(data)
            if ("result" in message || "error" in message) responses.send(message)
        }
        outgoingChannel.send(data)
    }

    override suspend fun close() = Unit
}

private fun readFrame(bytes: ByteArray): JsonObject {
    val text = bytes.decodeToString()
    val (header, body) = text.split("\r\n\r\n", limit = 2)
    val length = header.removePrefix("Content-Length: ").toInt()
    assertEquals(length, body.encodeToByteArray().size)
    return Json.parseToJsonElement(body).jsonObject
}
