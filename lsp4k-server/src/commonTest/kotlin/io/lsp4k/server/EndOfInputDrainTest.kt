package io.lsp4k.server

import io.jsonrpc4k.core.BoundedDispatchPolicy
import io.jsonrpc4k.core.ResponseError
import io.lsp4k.protocol.ExecuteCommandParams
import io.lsp4k.protocol.LspMethods
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

            assertEquals(JsonPrimitive("done"), response.await().result())
            assertNull(peer.server.awaitTermination())
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
}
