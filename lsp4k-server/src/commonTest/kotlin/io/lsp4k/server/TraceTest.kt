package io.lsp4k.server

import io.lsp4k.protocol.LogTraceParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.SetTraceParams
import io.lsp4k.protocol.TraceValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TraceTest {
    @Test
    fun `logTrace follows the trace level from initialize and setTrace`() =
        runTest {
            val levels = mutableListOf<TraceValue>()
            val peer = startPeer(backgroundScope) { onSetTrace { levels += it.value } }
            peer.initialize(initializeParams(trace = TraceValue.Off))
            val client = peer.server.client
            assertFalse(client.logTrace("hidden"))
            assertFalse(client.logTrace(LogTraceParams("hidden", "detail")))

            peer.send(LspMethods.SET_TRACE, SetTraceParams(TraceValue.Messages))
            peer.sync()
            assertEquals(TraceValue.Messages, peer.server.traceLevel)
            var evaluated = false
            val sent =
                client.logTrace("message") {
                    evaluated = true
                    "detail"
                }
            assertTrue(sent)
            assertFalse(evaluated)
            assertMessage(peer.notifications.receive(), LspMethods.LOG_TRACE, """{"message":"message"}""")
            assertTrue(client.logTrace(LogTraceParams("params", "detail")))
            assertMessage(peer.notifications.receive(), LspMethods.LOG_TRACE, """{"message":"params"}""")

            peer.send(LspMethods.SET_TRACE, SetTraceParams(TraceValue.Verbose))
            peer.sync()
            assertTrue(client.logTrace("verbose") { "detail" })
            assertMessage(peer.notifications.receive(), LspMethods.LOG_TRACE, """{"message":"verbose","verbose":"detail"}""")
            assertTrue(client.logTrace(LogTraceParams("params", "detail")))
            assertMessage(peer.notifications.receive(), LspMethods.LOG_TRACE, """{"message":"params","verbose":"detail"}""")
            assertEquals(listOf(TraceValue.Messages, TraceValue.Verbose), levels)
        }

    @Test
    fun `setTrace before initialize is dropped and initialize sets the level`() =
        runTest {
            val peer = startPeer(backgroundScope) {}
            peer.send(LspMethods.SET_TRACE, SetTraceParams(TraceValue.Verbose))
            peer.sync()
            assertEquals(TraceValue.Off, peer.server.traceLevel)
            peer.initialize(initializeParams(trace = TraceValue.Messages))
            assertEquals(TraceValue.Messages, peer.server.traceLevel)
        }
}
