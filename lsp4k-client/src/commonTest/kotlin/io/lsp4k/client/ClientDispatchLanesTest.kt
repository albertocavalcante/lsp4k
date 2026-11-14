package io.lsp4k.client

import io.jsonrpc4k.core.BoundedDispatchPolicy
import io.jsonrpc4k.core.NotificationHandler
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.MessageActionItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.seconds

class ClientDispatchLanesTest {
    private val messageRequest = """{"type":3,"message":"Reload?","actions":[{"title":"Yes"}]}"""

    @Test
    fun `progress logs and diagnostics are not held back by a request waiting for the user`() =
        runTest { assertControlLane(DEFAULT_DISPATCH_POLICY) }

    @Test
    fun `the control lane applies to a policy that lists no control notifications`() =
        runTest { assertControlLane(BoundedDispatchPolicy(maxConcurrentRequests = 4, maxQueuedMessages = 64)) }

    private suspend fun TestScope.assertControlLane(policy: BoundedDispatchPolicy) {
        val entered = CompletableDeferred<Unit>()
        val answer = CompletableDeferred<MessageActionItem>()
        val seen = Channel<String>(Channel.UNLIMITED)
        val peer = TestPeer(backgroundScope)
        languageClient {
            window {
                showMessageRequest {
                    entered.complete(Unit)
                    answer.await()
                }
                logMessage { seen.send("log:${it.message}") }
                showMessage { seen.send("show:${it.message}") }
            }
            textDocument { publishDiagnostics { seen.send("diagnostics:${it.uri}") } }
            onProgress { seen.send("progress:${it.value}") }
            onTelemetryEvent { seen.send("telemetry") }
            onLogTrace { seen.send("trace:${it.message}") }
        }.start(peer.transport, backgroundScope, policy)

        val chosen = async { peer.call(LspMethods.WINDOW_SHOW_MESSAGE_REQUEST, messageRequest) }
        entered.await()
        peer.send(LspMethods.PROGRESS, """{"token":"t","value":1}""")
        peer.send(LspMethods.WINDOW_LOG_MESSAGE, """{"type":4,"message":"a"}""")
        peer.send(LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS, """{"uri":"file:///a.kt","diagnostics":[]}""")
        peer.send(LspMethods.WINDOW_SHOW_MESSAGE, """{"type":3,"message":"b"}""")
        peer.send(LspMethods.TELEMETRY_EVENT, """{"event":"e"}""")
        peer.send(LspMethods.LOG_TRACE, """{"message":"c"}""")
        peer.send(LspMethods.PROGRESS, """{"token":"t","value":2}""")

        // Without the control lane these wait behind the unanswered request, so the receive times out.
        val order = withTimeout(10.seconds) { List(7) { seen.receive() } }
        assertEquals(
            listOf(
                "progress:1",
                "log:a",
                "diagnostics:file:///a.kt",
                "show:b",
                "telemetry",
                "trace:c",
                "progress:2",
            ),
            order,
        )
        assertFalse(chosen.isCompleted)

        answer.complete(MessageActionItem("Yes"))
        assertEquals(Json.parseToJsonElement("""{"title":"Yes"}"""), chosen.await())
    }

    @Test
    fun `other notifications stay barriers behind a running request`() =
        runTest {
            val entered = CompletableDeferred<Unit>()
            val answer = CompletableDeferred<MessageActionItem?>()
            val seen = Channel<String>(Channel.UNLIMITED)
            val peer = TestPeer(backgroundScope)
            languageClient {
                window {
                    showMessageRequest {
                        entered.complete(Unit)
                        answer.await()
                    }
                    logMessage { seen.send("log") }
                }
                onNotification("app/custom", NotificationHandler { seen.send("custom") })
            }.start(peer.transport, backgroundScope)

            val chosen = async { peer.call(LspMethods.WINDOW_SHOW_MESSAGE_REQUEST, messageRequest) }
            entered.await()
            peer.send("app/custom", null)
            peer.send(LspMethods.WINDOW_LOG_MESSAGE, """{"type":4,"message":"a"}""")

            assertEquals("log", withTimeout(10.seconds) { seen.receive() })
            assertEquals(null, seen.tryReceive().getOrNull())
            answer.complete(null)
            chosen.await()
            assertEquals("custom", withTimeout(10.seconds) { seen.receive() })
        }

    @Test
    fun `a server request answered after the server closed its output still gets the response`() =
        runTest {
            val entered = CompletableDeferred<Unit>()
            val peer = TestPeer(backgroundScope)
            val client =
                languageClient {
                    window {
                        showMessageRequest {
                            entered.complete(Unit)
                            delay(1.seconds)
                            MessageActionItem("Yes")
                        }
                    }
                }.start(peer.transport, backgroundScope)

            val chosen = async { peer.call(LspMethods.WINDOW_SHOW_MESSAGE_REQUEST, messageRequest) }
            entered.await()
            peer.closeOutput()

            assertEquals(Json.parseToJsonElement("""{"title":"Yes"}"""), chosen.await())
            assertEquals(null, client.awaitTermination())
        }
}
