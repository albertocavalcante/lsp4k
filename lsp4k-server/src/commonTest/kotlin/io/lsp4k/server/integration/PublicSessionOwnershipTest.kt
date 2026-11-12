package io.lsp4k.server.integration

import io.jsonrpc4k.core.JsonRpcCodec
import io.jsonrpc4k.core.NotificationMessage
import io.jsonrpc4k.transport.Transport
import io.lsp4k.client.languageClient
import io.lsp4k.client.start
import io.lsp4k.server.languageServer
import io.lsp4k.server.start
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class PublicSessionOwnershipTest {
    @Test
    fun `client close stops its collector without cancelling caller or borrowed transport`() =
        runTest {
            val parent = SupervisorJob()
            val sibling = SupervisorJob(parent)
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
            val transport = ObservedTransport()
            try {
                val client = languageClient {}.start(transport, scope)
                withTimeout(1.seconds) { transport.started.await() }
                client.close()
                withTimeout(1.seconds) { transport.stopped.await() }
                assertTrue(parent.isActive)
                assertTrue(sibling.isActive)
                assertFalse(transport.closed)
            } finally {
                parent.cancel()
            }
        }

    @Test
    fun `server exit stops its collector without cancelling caller or borrowed transport`() =
        runTest {
            val parent = SupervisorJob()
            val sibling = SupervisorJob(parent)
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
            val transport = ObservedTransport()
            try {
                languageServer {}.start(transport, scope)
                withTimeout(1.seconds) { transport.started.await() }
                transport.inbound.send(JsonRpcCodec.Default.encode(NotificationMessage("exit")))
                withTimeout(1.seconds) { transport.stopped.await() }
                assertTrue(parent.isActive)
                assertTrue(sibling.isActive)
                assertFalse(transport.closed)
            } finally {
                parent.cancel()
            }
        }

    @Test
    fun `incoming EOF terminates a pending client request`() =
        runTest {
            val parent = SupervisorJob()
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
            val transport = ObservedTransport()
            try {
                val client = languageClient {}.start(transport, scope)
                withTimeout(1.seconds) { transport.started.await() }
                val pending = async { runCatching { client.initialize() } }
                withTimeout(1.seconds) { transport.sent.await() }
                transport.inbound.close()
                assertTrue(withTimeout(1.seconds) { pending.await() }.isFailure)
                assertTrue(parent.isActive)
                assertFalse(transport.closed)
            } finally {
                parent.cancel()
            }
        }
}

private class ObservedTransport : Transport {
    val inbound = Channel<ByteArray>(Channel.UNLIMITED)
    val started = CompletableDeferred<Unit>()
    val stopped = CompletableDeferred<Unit>()
    val sent = CompletableDeferred<Unit>()
    var closed = false

    override val incoming: Flow<ByteArray> =
        flow {
            started.complete(Unit)
            try {
                for (bytes in inbound) emit(bytes)
            } finally {
                stopped.complete(Unit)
            }
        }
    override val isConnected: Boolean get() = !closed

    override suspend fun send(data: ByteArray) {
        sent.complete(Unit)
    }

    override suspend fun close() {
        closed = true
    }
}
