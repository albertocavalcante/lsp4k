package io.lsp4k.client

import io.jsonrpc4k.core.NotificationHandler
import io.jsonrpc4k.core.RequestHandler
import io.lsp4k.protocol.LspMethods
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionLifecycleCallsTest {
    @Test
    fun `initialize initialized shutdown and exit are sent in order`() =
        runTest {
            val order = Channel<String>(Channel.UNLIMITED)
            val peer = TestPeer(backgroundScope)
            peer.connection.onRequest(
                LspMethods.INITIALIZE,
                RequestHandler {
                    order.send(LspMethods.INITIALIZE)
                    Json.parseToJsonElement("""{"capabilities":{}}""")
                },
            )
            peer.connection.onNotification(LspMethods.INITIALIZED, NotificationHandler { order.send(LspMethods.INITIALIZED) })
            peer.connection.onRequest(
                LspMethods.SHUTDOWN,
                RequestHandler {
                    order.send(LspMethods.SHUTDOWN)
                    null
                },
            )
            peer.connection.onNotification(LspMethods.EXIT, NotificationHandler { order.send(LspMethods.EXIT) })

            val client = languageClient { clientInfo("test") }.start(peer.transport, backgroundScope)
            client.initialize()
            client.shutdown()

            val seen = List(4) { order.receive() }
            assertEquals(
                listOf(LspMethods.INITIALIZE, LspMethods.INITIALIZED, LspMethods.SHUTDOWN, LspMethods.EXIT),
                seen,
            )
        }
}
