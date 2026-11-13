package io.lsp4k.server

import io.kotest.matchers.shouldBe
import io.lsp4k.protocol.ClientCapabilities
import io.lsp4k.protocol.GeneralClientCapabilities
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.PositionEncodingKind
import io.lsp4k.protocol.ServerCapabilities
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test

private val utf8 = PositionEncodingKind.UTF8
private val utf16 = PositionEncodingKind.UTF16
private val utf32 = PositionEncodingKind.UTF32

class PositionEncodingNegotiationTest {
    @Test
    fun `negotiation table`() {
        val table =
            listOf<Triple<List<PositionEncodingKind>?, List<PositionEncodingKind>, PositionEncodingKind>>(
                Triple(null, listOf(utf8, utf16), utf16),
                Triple(emptyList(), listOf(utf8, utf16), utf16),
                Triple(listOf(utf16), listOf(utf8, utf16), utf16),
                Triple(listOf(utf16, utf8), listOf(utf8, utf16), utf8),
                Triple(listOf(utf8, utf16, utf32), listOf(utf32, utf8), utf32),
                Triple(listOf(utf8, utf16, utf32), listOf(utf16, utf8), utf16),
                Triple(listOf(utf8), listOf(utf32), utf16),
                Triple(listOf(utf32), listOf(utf8, utf16), utf16),
                Triple(listOf(PositionEncodingKind("x"), utf32), listOf(utf8, utf32), utf32),
                Triple(listOf(utf8, utf32), emptyList(), utf16),
            )
        for ((offered, preference, expected) in table) {
            negotiatePositionEncoding(offered, preference) shouldBe expected
        }
    }

    @Test
    fun `the default preference favours utf-8 then utf-32 then utf-16`() {
        negotiatePositionEncoding(listOf(utf16, utf32, utf8)) shouldBe utf8
        negotiatePositionEncoding(listOf(utf16, utf32)) shouldBe utf32
        negotiatePositionEncoding(listOf(utf16)) shouldBe utf16
        negotiatePositionEncoding(null) shouldBe utf16
    }

    @Test
    fun `initialize parameters negotiate from general capabilities`() {
        val offering = initializeParams().copy(capabilities = offered(listOf(utf16, utf8)))
        offering.negotiatePositionEncoding() shouldBe utf8
        offering.negotiatePositionEncoding(listOf(utf16)) shouldBe utf16
        initializeParams().negotiatePositionEncoding() shouldBe utf16
    }

    @Test
    fun `text documents are built from negotiated values`() {
        TextDocuments(ServerCapabilities(positionEncoding = utf8)).encoding shouldBe utf8
        TextDocuments(ServerCapabilities()).encoding shouldBe utf16
        TextDocuments(initializeParams().copy(capabilities = offered(listOf(utf32)))).encoding shouldBe utf32
        TextDocuments(initializeParams().copy(capabilities = offered(listOf(utf32))), listOf(utf8)).encoding shouldBe utf16
    }

    @Test
    fun `the default initialize result advertises the negotiated encoding`() =
        runTest {
            val peer = startPeer(backgroundScope) { negotiatePositionEncoding(listOf(utf8, utf16)) }
            val response = peer.initialize(initializeParams().copy(capabilities = offered(listOf(utf16, utf8))))

            advertised(response.result()) shouldBe utf8
            peer.server.positionEncoding shouldBe utf8
        }

    @Test
    fun `a client that offers nothing gets utf-16 from the builder wiring`() =
        runTest {
            val peer = startPeer(backgroundScope) { negotiatePositionEncoding(listOf(utf8)) }
            val response = peer.initialize()

            advertised(response.result()) shouldBe utf16
            peer.server.positionEncoding shouldBe utf16
        }

    @Test
    fun `without negotiation the configured capability is kept`() =
        runTest {
            val peer = startPeer(backgroundScope) { capabilities { positionEncoding = utf32 } }
            val response = peer.initialize(initializeParams().copy(capabilities = offered(listOf(utf8))))

            advertised(response.result()) shouldBe utf32
            peer.server.positionEncoding shouldBe utf32
        }

    @Test
    fun `a custom initialize handler negotiates for itself`() =
        runTest {
            val preference = listOf(utf32, utf16)
            lateinit var documents: TextDocuments
            val peer =
                startPeer(backgroundScope) {
                    onInitialize { params ->
                        val encoding = params.negotiatePositionEncoding(preference)
                        documents = TextDocuments(encoding)
                        InitializeResult(ServerCapabilities(positionEncoding = encoding))
                    }
                }
            val response = peer.initialize(initializeParams().copy(capabilities = offered(listOf(utf8, utf32))))

            advertised(response.result()) shouldBe utf32
            documents.encoding shouldBe utf32
            peer.server.positionEncoding shouldBe utf32
            peer.request(LspMethods.SHUTDOWN).result()
        }

    private fun offered(kinds: List<PositionEncodingKind>) =
        ClientCapabilities(general = GeneralClientCapabilities(positionEncodings = kinds))

    private fun advertised(result: JsonElement): PositionEncodingKind? =
        testJson.decodeFromJsonElement(InitializeResult.serializer(), result).capabilities.positionEncoding
}
