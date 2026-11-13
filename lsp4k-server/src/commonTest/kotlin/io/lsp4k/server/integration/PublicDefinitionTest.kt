package io.lsp4k.server.integration

import io.jsonrpc4k.transport.Transport
import io.lsp4k.client.languageClient
import io.lsp4k.client.start
import io.lsp4k.protocol.ClientCapabilities
import io.lsp4k.protocol.DefinitionParams
import io.lsp4k.protocol.DefinitionResult
import io.lsp4k.protocol.GotoCapability
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.Location
import io.lsp4k.protocol.LocationLink
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.Range
import io.lsp4k.protocol.ServerCapabilities
import io.lsp4k.protocol.TextDocumentClientCapabilities
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.server.languageServer
import io.lsp4k.server.start
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PublicDefinitionTest {
    private val params = DefinitionParams(TextDocumentIdentifier("file:///source"), Position(1, 2))
    private val range = Range(Position(2, 3), Position(4, 5))
    private val selection = Range(Position(3, 1), Position(3, 7))
    private val location = Location("file:///目标", range)
    private val link = LocationLink(selection, "file:///目标", range, selection)
    private val locationJson =
        """{"uri":"file:///目标","range":{"start":{"line":2,"character":3},""" +
            """"end":{"line":4,"character":5}}}"""
    private val linkJson =
        """{"originSelectionRange":{"start":{"line":3,"character":1},"end":{"line":3,"character":7}},""" +
            """"targetUri":"file:///目标","targetRange":{"start":{"line":2,"character":3},""" +
            """"end":{"line":4,"character":5}},""" +
            """"targetSelectionRange":{"start":{"line":3,"character":1},"end":{"line":3,"character":7}}}"""

    @Test
    fun `client decodes literal definition shapes`() =
        runTest {
            val peer = DefinitionPeer()
            val client = languageClient {}.start(peer, backgroundScope)
            peer.result = locationJson
            assertEquals(DefinitionResult.Single(location), client.server.textDocument.definition(params))
            peer.result = "[$locationJson]"
            assertEquals(DefinitionResult.Locations(listOf(location)), client.server.textDocument.definition(params))
            peer.result = "[$linkJson]"
            assertEquals(DefinitionResult.Links(listOf(link)), client.server.textDocument.definition(params))
            peer.result = "[]"
            assertEquals(DefinitionResult.Locations(emptyList()), client.server.textDocument.definition(params))
            peer.result = "null"
            assertEquals(null, client.server.textDocument.definition(params))
            client.close()
        }

    @Test
    fun `client rejects malformed ambiguous and mixed result shapes`() =
        runTest {
            val peer = DefinitionPeer()
            val client = languageClient {}.start(peer, backgroundScope)
            for (bad in listOf(
                "42",
                "{}",
                "[$locationJson,$linkJson]",
                "[$locationJson,42]",
                linkJson,
                """{"uri":"file:///x","range":{},"targetUri":"file:///y"}""",
                """[{"targetUri":"file:///x","targetRange":{}}]""",
            )) {
                peer.result = bad
                assertFailsWith<SerializationException> { client.server.textDocument.definition(params) }
            }
            client.close()
        }

    @Test
    fun `public initialization capability selects location or link arrays`() =
        runTest {
            for (supportsLinks in listOf(false, true)) {
                val toClient = Channel<ByteArray>(Channel.UNLIMITED)
                val toServer = Channel<ByteArray>(Channel.UNLIMITED)
                val responses = Channel<JsonObject>(Channel.UNLIMITED)
                val serverTransport = DefinitionTransport(toServer, toClient, responses)
                val clientTransport = DefinitionTransport(toClient, toServer, null)
                var negotiated = false
                val server =
                    languageServer {
                        onInitialize {
                            negotiated = it.capabilities.textDocument
                                ?.definition
                                ?.linkSupport == true
                            InitializeResult(ServerCapabilities())
                        }
                        textDocument {
                            definition {
                                if (negotiated) {
                                    DefinitionResult.Links(listOf(link))
                                } else {
                                    DefinitionResult.Locations(listOf(location))
                                }
                            }
                        }
                    }.start(serverTransport, backgroundScope)
                val capabilities =
                    ClientCapabilities(
                        textDocument = TextDocumentClientCapabilities(definition = GotoCapability(linkSupport = supportsLinks)),
                    )
                val client = languageClient { capabilities(capabilities) }.start(clientTransport, backgroundScope)
                client.initialize()
                responses.receive()
                val actual = client.server.textDocument.definition(params)
                val expected =
                    if (supportsLinks) DefinitionResult.Links(listOf(link)) else DefinitionResult.Locations(listOf(location))
                assertEquals(expected, actual)
                val expectedJson = if (supportsLinks) "[$linkJson]" else "[$locationJson]"
                assertEquals(Json.parseToJsonElement(expectedJson), responses.receive()["result"])
                client.close()
                server.close()
            }
        }

    @Test
    fun `server emits single empty and null response shapes`() =
        runTest {
            val toClient = Channel<ByteArray>(Channel.UNLIMITED)
            val toServer = Channel<ByteArray>(Channel.UNLIMITED)
            val responses = Channel<JsonObject>(Channel.UNLIMITED)
            var result: DefinitionResult? = DefinitionResult.Single(location)
            val server =
                languageServer { textDocument { definition { result } } }
                    .start(DefinitionTransport(toServer, toClient, responses), backgroundScope)
            val client = languageClient {}.start(DefinitionTransport(toClient, toServer, null), backgroundScope)
            client.initialize()
            responses.receive()
            assertEquals(result, client.server.textDocument.definition(params))
            assertEquals(Json.parseToJsonElement(locationJson), responses.receive()["result"])
            result = DefinitionResult.Links(emptyList())
            assertEquals(DefinitionResult.Locations(emptyList()), client.server.textDocument.definition(params))
            assertEquals(Json.parseToJsonElement("[]"), responses.receive()["result"])
            result = null
            assertEquals(null, client.server.textDocument.definition(params))
            val response = responses.receive()
            assertTrue("result" in response)
            assertEquals(JsonNull, response["result"])
            client.close()
            server.close()
        }
}

private class DefinitionPeer : Transport {
    private val inbound = Channel<ByteArray>(Channel.UNLIMITED)
    var result: String = "null"
    override val incoming: Flow<ByteArray> = inbound.receiveAsFlow()
    override val isConnected: Boolean = true

    override suspend fun send(data: ByteArray) {
        val request = readDefinitionFrame(data)
        val body = """{"jsonrpc":"2.0","id":${request["id"]},"result":$result}"""
        val bytes = body.encodeToByteArray()
        inbound.send("Content-Length: ${bytes.size}\r\n\r\n".encodeToByteArray() + bytes)
    }

    override suspend fun close() = Unit
}

private class DefinitionTransport(
    incomingChannel: Channel<ByteArray>,
    private val outgoingChannel: Channel<ByteArray>,
    private val responses: Channel<JsonObject>?,
) : Transport {
    override val incoming: Flow<ByteArray> = incomingChannel.receiveAsFlow()
    override val isConnected: Boolean = true

    override suspend fun send(data: ByteArray) {
        if (responses != null) {
            val message = readDefinitionFrame(data)
            if ("result" in message || "error" in message) responses.send(message)
        }
        outgoingChannel.send(data)
    }

    override suspend fun close() = Unit
}

private fun readDefinitionFrame(bytes: ByteArray): JsonObject {
    val (header, body) = bytes.decodeToString().split("\r\n\r\n", limit = 2)
    assertEquals(header.removePrefix("Content-Length: ").toInt(), body.encodeToByteArray().size)
    return Json.parseToJsonElement(body).jsonObject
}
