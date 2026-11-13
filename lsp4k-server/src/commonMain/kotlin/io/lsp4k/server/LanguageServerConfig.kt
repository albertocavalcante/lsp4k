package io.lsp4k.server

import io.jsonrpc4k.core.BoundedDispatchPolicy
import io.jsonrpc4k.core.InboundResourceLimits
import io.jsonrpc4k.transport.Transport
import io.lsp4k.protocol.InitializeParams
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.PositionEncodingKind
import io.lsp4k.protocol.ServerCapabilities
import io.lsp4k.protocol.ServerInfo
import io.lsp4k.protocol.SetTraceParams
import io.lsp4k.server.transport.SessionLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.JsonElement

/**
 * Marks DSL classes used for configuring an LSP language server.
 */
@DslMarker
public annotation class LspServerDsl

/**
 * Handles one inbound request from its raw JSON parameters.
 *
 * The builder DSL adapts every typed handler to this interface; [LanguageServerBuilder.onRequest] registers
 * custom methods directly.
 */
public fun interface ServerRequestHandler {
    /**
     * Handles the request described by [context]. [params] is the raw `params` member, or null when it was
     * omitted. The returned element becomes the `result` member; null encodes as JSON `null`.
     */
    public suspend fun handle(
        context: RequestContext,
        params: JsonElement?,
    ): JsonElement?
}

/**
 * Handles one inbound notification from its raw JSON parameters.
 */
public fun interface ServerNotificationHandler {
    /** Handles the notification; [params] is the raw `params` member, or null when it was omitted. */
    public suspend fun handle(
        context: HandlerContext,
        params: JsonElement?,
    )
}

/**
 * Hooks for the lifecycle methods. The server runs the lifecycle state machine itself, so these methods
 * never appear in [LanguageServerConfig.requestHandlers] or [LanguageServerConfig.notificationHandlers].
 *
 * @property initialize Computes the `initialize` result. When null, the server answers with the configured
 *   capabilities and server info.
 * @property initialized Runs once, for the first `initialized` notification after a successful `initialize`.
 * @property shutdown Runs when `shutdown` is accepted, before the server answers it.
 * @property exit Receives the exit code (0 after `shutdown`, otherwise 1) when `exit` arrives, after pending
 *   output was flushed and before the session closes.
 * @property setTrace Runs after `$/setTrace` updated [LanguageServer.traceLevel].
 */
public data class LifecycleHandlers(
    val initialize: (suspend RequestContext.(InitializeParams) -> InitializeResult)? = null,
    val initialized: (suspend HandlerContext.() -> Unit)? = null,
    val shutdown: (suspend RequestContext.() -> Unit)? = null,
    val exit: ((exitCode: Int) -> Unit)? = null,
    val setTrace: (suspend HandlerContext.(SetTraceParams) -> Unit)? = null,
)

/**
 * Immutable snapshot of a fully configured language server, produced by [languageServer] and run by [start].
 *
 * @property serverInfo Optional name and version metadata advertised in the default `initialize` result.
 * @property capabilities The capabilities advertised in the default `initialize` result.
 * @property requestHandlers Request handlers keyed by JSON-RPC method name.
 * @property notificationHandlers Notification handlers keyed by JSON-RPC method name.
 * @property lifecycle Hooks for `initialize`, `initialized`, `shutdown`, `exit` and `$/setTrace`.
 * @property positionEncodingPreference When set, the default `initialize` result advertises the encoding negotiated
 *   from this server preference and the client's offer (see [negotiatePositionEncoding]); null leaves
 *   [capabilities] as configured. A custom initialize hook is not affected.
 */
public data class LanguageServerConfig(
    val serverInfo: ServerInfo?,
    val capabilities: ServerCapabilities,
    val requestHandlers: Map<String, ServerRequestHandler>,
    val notificationHandlers: Map<String, ServerNotificationHandler>,
    val lifecycle: LifecycleHandlers = LifecycleHandlers(),
    val positionEncodingPreference: List<PositionEncodingKind>? = null,
)

private val reservedRequestMethods: Set<String> = setOf(LspMethods.INITIALIZE, LspMethods.SHUTDOWN)

private val reservedNotificationMethods: Set<String> =
    setOf(
        LspMethods.INITIALIZED,
        LspMethods.EXIT,
        LspMethods.SET_TRACE,
        LspMethods.CANCEL_REQUEST,
    )

internal fun requireNotReservedRequest(method: String) {
    require(method !in reservedRequestMethods) {
        "Cannot register a custom request handler for reserved server method: $method"
    }
}

internal fun requireNotReservedNotification(method: String) {
    require(method !in reservedNotificationMethods) {
        "Cannot register a custom notification handler for reserved server method: $method"
    }
}

/** Default for [start]: 16 concurrent request handlers and 1024 queued messages, the most jsonrpc4k allows. */
public val DEFAULT_DISPATCH_POLICY: BoundedDispatchPolicy = BoundedDispatchPolicy(maxConcurrentRequests = 16, maxQueuedMessages = 1024)

/**
 * DSL entry point for creating a [LanguageServerConfig].
 *
 * Example:
 * ```kotlin
 * val config = languageServer {
 *     serverInfo("my-server", "1.0.0")
 *     capabilities {
 *         hoverProvider = true
 *     }
 *     textDocument {
 *         hover { params -> Hover(HoverContents.Markup(MarkupContent(MarkupKind.PlainText, "Hello!"))) }
 *     }
 * }
 * ```
 */
public fun languageServer(block: LanguageServerBuilder.() -> Unit): LanguageServerConfig = LanguageServerBuilder().apply(block).toConfig()

/**
 * Starts a language server with the given [transport] and optional [scope].
 *
 * The returned [LanguageServer] is fully wired: incoming transport data is decoded and dispatched, and outgoing
 * messages are forwarded to the transport. The supplied scope and transport remain caller-owned; closing a
 * session does not close the transport.
 *
 * Dispatch is concurrent and bounded by [policy] (jsonrpc4k bounded dispatch). Request handlers run
 * concurrently, up to [BoundedDispatchPolicy.maxConcurrentRequests]; a request that arrives while
 * [BoundedDispatchPolicy.maxQueuedMessages] are queued is answered with a server busy error. Notifications are
 * barriers: every earlier request finishes before a notification runs, and later messages wait for it, so
 * `didOpen`, `didChange` and `didClose` run in wire order and a request sees the effect of every notification
 * that preceded it. Responses and `$/cancelRequest` bypass the queue: a cancelled request is answered with
 * `RequestCancelled` (-32800) and its handler is cancelled. Pass `BoundedDispatchPolicy(1, ...)` to run requests
 * one at a time. Handlers may await requests to the client; nested chains of such waits can still use up the
 * concurrency limit.
 *
 * @param policy Concurrency and queue limits; defaults to [DEFAULT_DISPATCH_POLICY].
 * @param inboundLimits Optional framed input and application byte limits; null leaves input unbounded.
 */
public suspend fun LanguageServerConfig.start(
    transport: Transport,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    policy: BoundedDispatchPolicy = DEFAULT_DISPATCH_POLICY,
    inboundLimits: InboundResourceLimits? = null,
): LanguageServer {
    requestHandlers.keys.forEach(::requireNotReservedRequest)
    notificationHandlers.keys.forEach(::requireNotReservedNotification)
    val session = SessionLifecycle(scope, policy, inboundLimits)
    val server = LanguageServer(this, session.connection, session)
    var started = false
    try {
        server.setup()
        session.start(transport)
        started = true
    } finally {
        if (!started) session.close()
    }
    return server
}
