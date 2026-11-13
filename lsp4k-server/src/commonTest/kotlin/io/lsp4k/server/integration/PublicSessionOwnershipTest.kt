package io.lsp4k.server.integration

import io.jsonrpc4k.core.JsonRpcCodec
import io.jsonrpc4k.core.JsonRpcException
import io.jsonrpc4k.core.JsonRpcMessageDecoder
import io.jsonrpc4k.core.Message
import io.jsonrpc4k.core.NotificationMessage
import io.jsonrpc4k.core.Payload
import io.jsonrpc4k.core.RequestId
import io.jsonrpc4k.core.RequestMessage
import io.jsonrpc4k.core.ResponseError
import io.jsonrpc4k.core.ResponseMessage
import io.jsonrpc4k.transport.Transport
import io.lsp4k.client.languageClient
import io.lsp4k.client.start
import io.lsp4k.server.languageServer
import io.lsp4k.server.start
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    @Test
    fun `send failure terminates pending request and remains observable`() =
        runTest {
            val parent = SupervisorJob()
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
            val failure = CancellationException("transport send failed")
            val transport = ObservedTransport(failure)
            try {
                val client = languageClient {}.start(transport, scope)
                val pending = async { runCatching { client.initialize() } }
                assertTrue(withTimeout(1.seconds) { pending.await() }.isFailure)
                val result = withTimeout(1.seconds) { client.awaitTermination() }
                assertTrue(generateSequence(result) { it.cause }.take(8).any { it === failure })
                assertTrue(parent.isActive)
                assertFalse(transport.closed)
            } finally {
                parent.cancel()
            }
        }

    @Test
    fun `fatal transport failure is reported after session cleanup`() =
        runTest {
            val parent = SupervisorJob()
            val reported = CompletableDeferred<Throwable>()
            val handler = CoroutineExceptionHandler { _, failure -> reported.complete(failure) }
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler) + handler)
            val failure = Error("fatal transport failure")
            val transport = ObservedTransport(failure)
            try {
                val client = languageClient {}.start(transport, scope)
                val pending = async { runCatching { client.initialize() } }
                assertTrue(withTimeout(1.seconds) { pending.await() }.isFailure)
                val terminal = withTimeout(1.seconds) { client.awaitTermination() }
                assertTrue(generateSequence(terminal) { it.cause }.take(8).any { it === failure })
                assertTrue(generateSequence(withTimeout(1.seconds) { reported.await() }) { it.cause }.take(8).any { it === failure })
                assertTrue(parent.isActive)
                assertFalse(transport.closed)
            } finally {
                parent.cancel()
            }
        }

    @Test
    fun `already cancelled parent returns terminated sessions without closing transports`() =
        runTest {
            val parent = SupervisorJob()
            parent.cancel()
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
            val clientTransport = ObservedTransport()
            val serverTransport = ObservedTransport()
            val client = languageClient {}.start(clientTransport, scope)
            val server = languageServer {}.start(serverTransport, scope)
            assertEquals(null, withTimeout(1.seconds) { client.awaitTermination() })
            assertEquals(null, withTimeout(1.seconds) { server.awaitTermination() })
            assertFalse(clientTransport.closed)
            assertFalse(serverTransport.closed)
        }

    @Test
    fun `server read cancellation is reported without cancelling caller`() =
        runTest {
            val parent = SupervisorJob()
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
            val failure = CancellationException("transport read failed")
            val transport =
                object : Transport {
                    override val incoming: Flow<ByteArray> = flow { throw failure }
                    override val isConnected: Boolean = true

                    override suspend fun send(data: ByteArray) = Unit

                    override suspend fun close() = error("borrowed transport must not be closed")
                }
            try {
                val server = languageServer {}.start(transport, scope)
                val result = withTimeout(1.seconds) { server.awaitTermination() }
                assertTrue(generateSequence(result) { it.cause }.take(8).any { it === failure })
                assertTrue(parent.isActive)
            } finally {
                parent.cancel()
            }
        }

    @Test
    fun `parent cancellation stops both sessions without closing transports`() =
        runTest {
            val parent = SupervisorJob()
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
            val clientTransport = ObservedTransport()
            val serverTransport = ObservedTransport()
            val client = languageClient {}.start(clientTransport, scope)
            val server = languageServer {}.start(serverTransport, scope)
            withTimeout(1.seconds) { clientTransport.started.await() }
            withTimeout(1.seconds) { serverTransport.started.await() }
            parent.cancel()
            assertEquals(null, withTimeout(1.seconds) { client.awaitTermination() })
            assertEquals(null, withTimeout(1.seconds) { server.awaitTermination() })
            assertTrue(clientTransport.stopped.isCompleted)
            assertTrue(serverTransport.stopped.isCompleted)
            assertFalse(clientTransport.closed)
            assertFalse(serverTransport.closed)
        }

    @Test
    fun `explicit server close stops reader without cancelling caller`() =
        runTest {
            val parent = SupervisorJob()
            val sibling = SupervisorJob(parent)
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
            val transport = ObservedTransport()
            try {
                val server = languageServer {}.start(transport, scope)
                withTimeout(1.seconds) { transport.started.await() }
                server.close()
                assertEquals(null, withTimeout(1.seconds) { server.awaitTermination() })
                assertTrue(transport.stopped.isCompleted)
                assertTrue(parent.isActive)
                assertTrue(sibling.isActive)
                assertFalse(transport.closed)
            } finally {
                parent.cancel()
            }
        }

    @Test
    fun `cancelling a termination waiter does not cancel the session`() =
        runTest {
            val parent = SupervisorJob()
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
            try {
                val client = languageClient {}.start(ObservedTransport(), scope)
                val waiter = async { client.awaitTermination() }
                runCurrent()
                waiter.cancel()
                client.close()
                assertEquals(null, withTimeout(1.seconds) { client.awaitTermination() })
                assertTrue(parent.isActive)
            } finally {
                parent.cancel()
            }
        }

    @Test
    fun `client shutdown waits for exit write and keeps borrowed resources`() =
        runTest {
            val parent = SupervisorJob()
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
            val transport = ShutdownTransport()
            try {
                val client = languageClient {}.start(transport, scope)
                val shutdown = async { client.shutdown() }
                withTimeout(1.seconds) { transport.exitStarted.await() }
                assertFalse(shutdown.isCompleted)
                assertFailsWith<IllegalStateException> { client.shutdown() }
                transport.releaseExit.complete(Unit)
                withTimeout(1.seconds) { shutdown.await() }
                assertEquals(null, withTimeout(1.seconds) { client.awaitTermination() })
                assertTrue(transport.exitSent)
                assertTrue(parent.isActive)
                assertFalse(transport.closed)
            } finally {
                parent.cancel()
            }
        }

    @Test
    fun `cancelling blocked shutdown stops session but preserves borrowed resources`() =
        runTest {
            val parent = SupervisorJob()
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
            val transport = ShutdownTransport()
            try {
                val client = languageClient {}.start(transport, scope)
                val shutdown = async { client.shutdown() }
                withTimeout(1.seconds) { transport.exitStarted.await() }
                shutdown.cancelAndJoin()
                assertEquals(null, withTimeout(1.seconds) { client.awaitTermination() })
                assertTrue(parent.isActive)
                assertFalse(transport.closed)
            } finally {
                parent.cancel()
            }
        }

    @Test
    fun `failed exit write remains observable during shutdown`() =
        runTest {
            val parent = SupervisorJob()
            val scope = CoroutineScope(parent + UnconfinedTestDispatcher(testScheduler))
            val failure = IllegalStateException("exit write failed")
            val transport = ShutdownTransport(failure)
            try {
                val client = languageClient {}.start(transport, scope)
                val shutdown = async(UnconfinedTestDispatcher(testScheduler)) { runCatching { client.shutdown() } }
                withTimeout(1.seconds) { transport.exitStarted.await() }
                transport.releaseExit.complete(Unit)
                assertTrue(withTimeout(1.seconds) { shutdown.await() }.isFailure)
                val result = withTimeout(1.seconds) { client.awaitTermination() }
                assertTrue(generateSequence(result) { it.cause }.take(8).any { it === failure })
                assertTrue(parent.isActive)
                assertFalse(transport.closed)
            } finally {
                parent.cancel()
            }
        }

    @Test
    fun `shutdown request error reaches caller and closes session normally`() =
        runTest {
            val parent = SupervisorJob()
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
            val transport = ShutdownTransport(shutdownError = ResponseError(-32001, "shutdown rejected"))
            try {
                val client = languageClient {}.start(transport, scope)
                val failure = assertFailsWith<JsonRpcException> { withTimeout(1.seconds) { client.shutdown() } }
                assertEquals(-32001, failure.code)
                assertEquals(null, withTimeout(1.seconds) { client.awaitTermination() })
                assertFalse(transport.exitStarted.isCompleted)
                assertTrue(parent.isActive)
                assertFalse(transport.closed)
            } finally {
                parent.cancel()
            }
        }

    @Test
    fun `pipelined server exit waits for the uninitialized shutdown error write`() =
        runTest {
            val parent = SupervisorJob()
            val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
            val transport = BlockingResponseTransport()
            try {
                val server = languageServer {}.start(transport, scope)
                val shutdown = JsonRpcCodec.Default.encode(RequestMessage(RequestId.of(7), "shutdown"))
                val exit = JsonRpcCodec.Default.encode(NotificationMessage("exit"))
                transport.inbound.send(shutdown + exit)
                withTimeout(1.seconds) { transport.sendStarted.await() }
                val termination = async { server.awaitTermination() }
                runCurrent()
                assertFalse(termination.isCompleted)
                transport.releaseSend.complete(Unit)
                assertEquals(null, withTimeout(1.seconds) { termination.await() })
                val response = decodeFrame(transport.sent.single()) as ResponseMessage
                assertEquals(RequestId.of(7), response.id)
                assertEquals(ResponseError.SERVER_NOT_INITIALIZED, response.error?.code)
                assertEquals(1, server.exitCode)
                assertTrue(parent.isActive)
                assertFalse(transport.closed)
            } finally {
                parent.cancel()
            }
        }

    @Test
    fun `failed shutdown response write remains observable during server exit`() =
        runTest {
            val parent = SupervisorJob()
            val scope = CoroutineScope(parent + UnconfinedTestDispatcher(testScheduler))
            val failure = IllegalStateException("response write failed")
            val transport = BlockingResponseTransport(failure)
            try {
                val server = languageServer {}.start(transport, scope)
                val shutdown = JsonRpcCodec.Default.encode(RequestMessage(RequestId.of(7), "shutdown"))
                val exit = JsonRpcCodec.Default.encode(NotificationMessage("exit"))
                transport.inbound.send(shutdown + exit)
                withTimeout(1.seconds) { transport.sendStarted.await() }
                runCurrent()
                transport.releaseSend.complete(Unit)
                val result = withTimeout(1.seconds) { server.awaitTermination() }
                assertTrue(generateSequence(result) { it.cause }.take(8).any { it === failure })
                assertTrue(parent.isActive)
                assertFalse(transport.closed)
            } finally {
                parent.cancel()
            }
        }
}

private class ObservedTransport(
    private val failure: Throwable? = null,
) : Transport {
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
        failure?.let { throw it }
        sent.complete(Unit)
    }

    override suspend fun close() {
        closed = true
    }
}

private class ShutdownTransport(
    private val exitFailure: Throwable? = null,
    private val shutdownError: ResponseError? = null,
) : Transport {
    val inbound = Channel<ByteArray>(Channel.UNLIMITED)
    val exitStarted = CompletableDeferred<Unit>()
    val releaseExit = CompletableDeferred<Unit>()
    var exitSent = false
    var closed = false
    override val incoming: Flow<ByteArray> = inbound.receiveAsFlow()
    override val isConnected: Boolean get() = !closed

    override suspend fun send(data: ByteArray) {
        when (val message = decodeFrame(data)) {
            is RequestMessage -> {
                val response =
                    shutdownError?.let { ResponseMessage.error(message.id, it) }
                        ?: ResponseMessage.success(message.id, null)
                inbound.send(JsonRpcCodec.Default.encode(response))
            }
            is NotificationMessage -> {
                assertEquals("exit", message.method)
                exitStarted.complete(Unit)
                releaseExit.await()
                exitFailure?.let { throw it }
                exitSent = true
            }
            else -> error("Unexpected outbound message: $message")
        }
    }

    override suspend fun close() {
        closed = true
    }
}

private class BlockingResponseTransport(
    private val failure: Throwable? = null,
) : Transport {
    val inbound = Channel<ByteArray>(Channel.UNLIMITED)
    val sendStarted = CompletableDeferred<Unit>()
    val releaseSend = CompletableDeferred<Unit>()
    val sent = mutableListOf<ByteArray>()
    var closed = false
    override val incoming: Flow<ByteArray> = inbound.receiveAsFlow()
    override val isConnected: Boolean get() = !closed

    override suspend fun send(data: ByteArray) {
        sendStarted.complete(Unit)
        releaseSend.await()
        failure?.let { throw it }
        sent.add(data)
    }

    override suspend fun close() {
        closed = true
    }
}

/** Decodes a frame that holds exactly one message. */
private fun decodeFrame(data: ByteArray): Message = (JsonRpcMessageDecoder().feed(data).single() as Payload.Single).message
