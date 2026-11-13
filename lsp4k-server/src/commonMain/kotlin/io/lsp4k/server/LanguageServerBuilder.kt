package io.lsp4k.server

import io.lsp4k.protocol.InitializeParams
import io.lsp4k.protocol.InitializeResult
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.PositionEncodingKind
import io.lsp4k.protocol.ProgressParams
import io.lsp4k.protocol.ServerCapabilities
import io.lsp4k.protocol.ServerInfo
import io.lsp4k.protocol.SetTraceParams
import kotlinx.serialization.json.JsonElement

/**
 * Top-level builder of the [languageServer] DSL.
 *
 * Every client-to-server method of LSP 3.18 has a hook, grouped like the client's call API:
 *
 * | Block | Methods |
 * |---|---|
 * | [textDocument] | document synchronization, completion, hover, signature help, navigation, highlights, symbols |
 * | [editing] | code actions, code lenses, links, colors, formatting, rename, folding, selection and linked ranges |
 * | [insights] | semantic tokens, inlay hints, pull diagnostics, monikers, inline values, inline completion |
 * | [hierarchy] | call hierarchy and type hierarchy |
 * | [workspace] | symbols, commands, workspace diagnostics, text document content, configuration, files, folders |
 * | [notebook] | notebook document synchronization |
 * | [window] | `window/workDoneProgress/cancel` |
 *
 * The lifecycle methods and `$/setTrace` have `on*` hooks here, and inbound `$/progress` has [onProgress].
 * `$/cancelRequest` is consumed by the jsonrpc4k connection and cannot be hooked.
 */
@LspServerDsl
public class LanguageServerBuilder internal constructor() {
    internal val registry: HandlerRegistry = HandlerRegistry()
    internal var serverInfo: ServerInfo? = null
    internal var capabilities: ServerCapabilities = ServerCapabilities()
    internal var lifecycle: LifecycleHandlers = LifecycleHandlers()
    internal var positionEncodingPreference: List<PositionEncodingKind>? = null

    /**
     * Sets the server name and optional version advertised in the default `initialize` result.
     */
    public fun serverInfo(
        name: String,
        version: String? = null,
    ) {
        serverInfo = ServerInfo(name, version)
    }

    /**
     * Configures the capabilities advertised in the default `initialize` result.
     */
    public fun capabilities(block: ServerCapabilitiesBuilder.() -> Unit) {
        capabilities = ServerCapabilitiesBuilder().apply(block).build()
    }

    /**
     * Handles `initialize` and chooses the result from the client's parameters, replacing the default result
     * built from [serverInfo] and [capabilities]. Throwing fails the request and lets the client retry.
     */
    public fun onInitialize(handler: suspend RequestContext.(InitializeParams) -> InitializeResult) {
        require(lifecycle.initialize == null) { "An initialize handler is already registered" }
        lifecycle = lifecycle.copy(initialize = handler)
    }

    /** Runs once when the client confirms initialization with the `initialized` notification. */
    public fun onInitialized(handler: suspend HandlerContext.() -> Unit) {
        require(lifecycle.initialized == null) { "An initialized handler is already registered" }
        lifecycle = lifecycle.copy(initialized = handler)
    }

    /** Runs when `shutdown` is accepted, before the server answers it; release resources here. */
    public fun onShutdown(handler: suspend RequestContext.() -> Unit) {
        require(lifecycle.shutdown == null) { "A shutdown handler is already registered" }
        lifecycle = lifecycle.copy(shutdown = handler)
    }

    /**
     * Receives the exit code (0 when `shutdown` came first, otherwise 1) when `exit` arrives, after pending
     * output was flushed and before the session closes. The same code is available as [LanguageServer.exitCode].
     */
    public fun onExit(handler: (exitCode: Int) -> Unit) {
        require(lifecycle.exit == null) { "An exit handler is already registered" }
        lifecycle = lifecycle.copy(exit = handler)
    }

    /** Runs after `$/setTrace` updated [LanguageServer.traceLevel]. */
    public fun onSetTrace(handler: suspend HandlerContext.(SetTraceParams) -> Unit) {
        require(lifecycle.setTrace == null) { "A setTrace handler is already registered" }
        lifecycle = lifecycle.copy(setTrace = handler)
    }

    /** Handles `$/progress` notifications sent by the client, with raw values. */
    public fun onProgress(handler: suspend HandlerContext.(ProgressParams<JsonElement>) -> Unit) {
        val serializer = ProgressParams.serializer(JsonElement.serializer())
        registry.putNotification(LspMethods.PROGRESS, typedNotification(LspMethods.PROGRESS, serializer, handler))
    }

    /** Registers text document synchronization, completion, hover, signature help and navigation handlers. */
    public fun textDocument(block: TextDocumentHandlersBuilder.() -> Unit) {
        TextDocumentHandlersBuilder(registry).apply(block)
    }

    /** Registers code action, code lens, link, color, formatting, rename and range handlers. */
    public fun editing(block: EditingHandlersBuilder.() -> Unit) {
        EditingHandlersBuilder(registry).apply(block)
    }

    /** Registers semantic token, inlay hint, diagnostic, moniker, inline value and inline completion handlers. */
    public fun insights(block: InsightHandlersBuilder.() -> Unit) {
        InsightHandlersBuilder(registry).apply(block)
    }

    /** Registers call hierarchy and type hierarchy handlers. */
    public fun hierarchy(block: HierarchyHandlersBuilder.() -> Unit) {
        HierarchyHandlersBuilder(registry).apply(block)
    }

    /** Registers workspace handlers. */
    public fun workspace(block: WorkspaceHandlersBuilder.() -> Unit) {
        WorkspaceHandlersBuilder(registry).apply(block)
    }

    /** Registers notebook document synchronization handlers. */
    public fun notebook(block: NotebookHandlersBuilder.() -> Unit) {
        NotebookHandlersBuilder(registry).apply(block)
    }

    /** Registers window notification handlers. */
    public fun window(block: WindowHandlersBuilder.() -> Unit) {
        WindowHandlersBuilder(registry).apply(block)
    }

    /**
     * Registers a request handler for a custom JSON-RPC [method]; the handler receives the raw params and
     * returns the raw result. Lifecycle methods are reserved.
     */
    public fun onRequest(
        method: String,
        handler: suspend RequestContext.(JsonElement?) -> JsonElement?,
    ) {
        requireNotReservedRequest(method)
        registry.putRequest(method, ServerRequestHandler { context, params -> context.handler(params) })
    }

    /**
     * Registers a notification handler for a custom JSON-RPC [method]; the handler receives the raw params.
     * Lifecycle methods, `$/setTrace` and `$/cancelRequest` are reserved.
     */
    public fun onNotification(
        method: String,
        handler: suspend HandlerContext.(JsonElement?) -> Unit,
    ) {
        requireNotReservedNotification(method)
        registry.putNotification(method, ServerNotificationHandler { context, params -> context.handler(params) })
    }
}

internal fun LanguageServerBuilder.toConfig(): LanguageServerConfig =
    LanguageServerConfig(
        serverInfo = serverInfo,
        capabilities = capabilities,
        requestHandlers = registry.requests.toMap(),
        notificationHandlers = registry.notifications.toMap(),
        lifecycle = lifecycle,
        positionEncodingPreference = positionEncodingPreference,
    )
