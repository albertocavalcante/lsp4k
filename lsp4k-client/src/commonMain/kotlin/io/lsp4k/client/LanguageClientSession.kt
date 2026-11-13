package io.lsp4k.client

import io.jsonrpc4k.core.BoundedDispatchPolicy
import io.jsonrpc4k.core.Connection
import io.jsonrpc4k.core.InboundResourceLimits
import io.jsonrpc4k.core.JsonRpcCodec
import io.jsonrpc4k.core.NotificationHandler
import io.jsonrpc4k.transport.Transport
import io.lsp4k.client.transport.SessionLifecycle
import io.lsp4k.protocol.InitializeParams
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.InitializedParams
import io.lsp4k.protocol.LspMethods
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonElement

/**
 * A running language client instance.
 */
public class LanguageClientSession internal constructor(
    private val config: LanguageClientConfig,
    private val connection: Connection,
    private val lifecycle: SessionLifecycle,
) {
    private val json = JsonRpcCodec.defaultJson
    private val shutdownStarted = MutableStateFlow(false)

    /**
     * Server proxy for sending requests to the server.
     */
    public val server: ServerProxy = ServerProxy(connection)

    /**
     * Work done and partial result progress received from the server, as flows keyed by token.
     */
    public val progress: ProgressStreams = ProgressStreams(json)

    internal suspend fun setup() {
        val userProgress = config.notificationHandlers[LspMethods.PROGRESS]
        config.notificationHandlers.forEach { (method, handler) ->
            if (method != LspMethods.PROGRESS) connection.onNotification(method, handler)
        }
        connection.onNotification(
            LspMethods.PROGRESS,
            NotificationHandler { params ->
                params?.let { progress.publish(decodeProgress(it)) }
                userProgress?.handle(params)
            },
        )
        config.requestHandlers.forEach { (method, handler) ->
            connection.onRequest(method, handler)
        }
    }

    /**
     * Initialize the connection with the server.
     */
    public suspend fun initialize(): InitializeResult {
        val params =
            InitializeParams(
                processId = null,
                rootUri = config.rootUri,
                capabilities = config.capabilities,
                clientInfo = config.clientInfo,
            )

        return initialize(params)
    }

    /** Initialize with explicit protocol parameters, including folders and initialization options. */
    public suspend fun initialize(params: InitializeParams): InitializeResult {
        val result =
            connection.request<InitializeResult>(
                LspMethods.INITIALIZE,
                json.encodeToJsonElement(InitializeParams.serializer(), params),
            )

        val decoded =
            result?.let {
                json.decodeFromJsonElement(InitializeResult.serializer(), it)
            } ?: error("Initialize returned null")
        connection.notify(LspMethods.INITIALIZED, json.encodeToJsonElement(InitializedParams.serializer(), InitializedParams))
        return decoded
    }

    /**
     * Requests shutdown and waits for the exit frame to be sent to the transport.
     * This does not guarantee peer receipt. The caller may bound the wait with a timeout;
     * cancellation aborts the session without closing the borrowed transport.
     */
    public suspend fun shutdown() {
        check(shutdownStarted.compareAndSet(false, true)) { "Shutdown already started" }
        try {
            connection.request<JsonElement?>(LspMethods.SHUTDOWN, null)
            connection.notify(LspMethods.EXIT, null)
            try {
                connection.flushOutgoing()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                lifecycle.fail(failure.cause ?: failure)
                throw failure
            }
        } finally {
            lifecycle.close()
        }
    }

    /**
     * Stops immediately, canceling pending work and discarding unsent frames.
     * Use [shutdown] for orderly exit. The transport remains caller-owned.
     */
    public fun close() {
        lifecycle.close()
    }

    /**
     * Waits for session jobs to stop. Returns the first read or write failure, or null on EOF,
     * explicit close, graceful exit, or parent cancellation. The transport remains caller-owned
     * and its suspending operations must cooperate with coroutine cancellation. If a borrowed
     * transport blocks in a non-interruptible read, its owner may need to close it to unblock.
     */
    public suspend fun awaitTermination(): Throwable? = lifecycle.awaitTermination()
}

/** Default for [start]: 16 concurrent request handlers and 1024 queued messages, the most jsonrpc4k allows. */
public val DEFAULT_DISPATCH_POLICY: BoundedDispatchPolicy = BoundedDispatchPolicy(maxConcurrentRequests = 16, maxQueuedMessages = 1024)

/**
 * Start a language client session with the given configuration and transport.
 * The supplied scope and transport remain caller-owned; closing a session does not close the transport.
 *
 * Inbound messages from the server are dispatched concurrently and bounded by [policy] (jsonrpc4k bounded
 * dispatch): request handlers run concurrently, notifications are barriers that run in wire order after every
 * earlier request finished, and `$/cancelRequest` cancels the handler of the request it names, which is then
 * answered with `RequestCancelled` (-32800). Handlers may await requests to the server.
 *
 * @param policy Concurrency and queue limits; defaults to [DEFAULT_DISPATCH_POLICY].
 * @param inboundLimits Optional framed input and application byte limits; null leaves input unbounded.
 */
public suspend fun LanguageClientConfig.start(
    transport: Transport,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    policy: BoundedDispatchPolicy = DEFAULT_DISPATCH_POLICY,
    inboundLimits: InboundResourceLimits? = null,
): LanguageClientSession {
    val lifecycle = SessionLifecycle(scope, policy, inboundLimits)
    val session = LanguageClientSession(this, lifecycle.connection, lifecycle)
    var started = false
    try {
        session.setup()
        lifecycle.start(transport)
        started = true
    } finally {
        if (!started) lifecycle.close()
    }

    return session
}
