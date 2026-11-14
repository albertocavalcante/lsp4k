package io.lsp4k.server

import io.jsonrpc4k.core.BoundedDispatchPolicy
import io.jsonrpc4k.core.ResponseError
import io.jsonrpc4k.transport.Transport
import io.lsp4k.protocol.ExecuteCommandParams
import io.lsp4k.protocol.LspMethods
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class EndOfInputDrainTest {
    private val command = wire(ExecuteCommandParams("app.slow"))

    @Test
    fun `a request followed at once by the end of input is still answered`() =
        runTest {
            val peer =
                startPeer(backgroundScope) {
                    workspace {
                        executeCommand {
                            delay(1.seconds)
                            JsonPrimitive("done")
                        }
                    }
                }
            peer.initialize()

            val (_, response) = peer.begin(LspMethods.WORKSPACE_EXECUTE_COMMAND, command)
            peer.closeOutput()

            assertEquals(JsonPrimitive("done"), withTimeout(10.seconds) { response.await() }.result())
            assertNull(withTimeout(10.seconds) { peer.server.awaitTermination() })
        }

    @Test
    fun `the drain timeout answers a request still running at the end of input with RequestCancelled`() =
        runTest {
            val entered = CompletableDeferred<Unit>()
            val policy = BoundedDispatchPolicy(maxConcurrentRequests = 4, maxQueuedMessages = 64, drainTimeout = 2.seconds)
            val peer =
                startPeer(backgroundScope, policy) {
                    workspace {
                        executeCommand {
                            entered.complete(Unit)
                            awaitCancellation()
                        }
                    }
                }
            peer.initialize()

            val (_, response) = peer.begin(LspMethods.WORKSPACE_EXECUTE_COMMAND, command)
            entered.await()
            val closedAt = testTimeSource.markNow()
            peer.closeOutput()

            assertEquals(ResponseError.REQUEST_CANCELLED, response.await().errorCode())
            assertNull(peer.server.awaitTermination())
            val drained = closedAt.elapsedNow()
            assertTrue(drained >= 2.seconds && drained <= 4.seconds, "drain took $drained of virtual time")
        }

    @Test
    fun `a transport whose send blocks after the end of input does not keep the session alive`() =
        runTest {
            val body = """{"jsonrpc":"2.0","id":1,"method":"${LspMethods.WORKSPACE_EXECUTE_COMMAND}","params":$command}"""
            val sending = CompletableDeferred<Unit>()
            val stalled =
                object : Transport {
                    override val incoming: Flow<ByteArray> =
                        flowOf("Content-Length: ${body.encodeToByteArray().size}\r\n\r\n$body".encodeToByteArray())
                    override val isConnected: Boolean = true

                    override suspend fun send(data: ByteArray) {
                        sending.complete(Unit)
                        awaitCancellation()
                    }

                    override suspend fun close() = Unit
                }
            val policy = BoundedDispatchPolicy(maxConcurrentRequests = 4, maxQueuedMessages = 64, drainTimeout = 2.seconds)
            val server = languageServer {}.start(stalled, backgroundScope, policy)

            assertNull(withTimeout(10.seconds) { server.awaitTermination() })
            assertTrue(sending.isCompleted, "the response reached the transport's send")
        }

    @Test
    fun `a zero drain timeout still writes the response queued before the end of input`() =
        runTest {
            val body = """{"jsonrpc":"2.0","id":1,"method":"${LspMethods.WORKSPACE_EXECUTE_COMMAND}","params":$command}"""
            val sending = CompletableDeferred<Unit>()
            val written = CompletableDeferred<String>()
            val slow =
                object : Transport {
                    override val incoming: Flow<ByteArray> =
                        flow {
                            emit("Content-Length: ${body.encodeToByteArray().size}\r\n\r\n$body".encodeToByteArray())
                            sending.await()
                        }
                    override val isConnected: Boolean = true

                    override suspend fun send(data: ByteArray) {
                        sending.complete(Unit)
                        delay(500.milliseconds)
                        written.complete(data.decodeToString())
                    }

                    override suspend fun close() = Unit
                }
            val policy = BoundedDispatchPolicy(maxConcurrentRequests = 4, maxQueuedMessages = 64, drainTimeout = Duration.ZERO)
            // Before initialize the server answers with ServerNotInitialized; any response proves the flush.
            val server = languageServer {}.start(slow, backgroundScope, policy)

            assertNull(withTimeout(10.seconds) { server.awaitTermination() })
            assertTrue(written.isCompleted, "the response was written before the session ended")
            assertTrue("\"id\":1" in written.await(), "unexpected frame: ${written.await()}")
        }
}
