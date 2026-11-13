package io.lsp4k.server

import io.jsonrpc4k.core.JsonRpcException
import io.jsonrpc4k.core.ResponseError
import io.lsp4k.protocol.InitializeParams
import io.lsp4k.protocol.LspMethods
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * Lifecycle state of a [LanguageServer], following the LSP lifecycle rules.
 */
public enum class ServerState {
    /** Waiting for `initialize`: requests fail with `ServerNotInitialized`, notifications except `exit` are dropped. */
    Uninitialized,

    /** The `initialize` request is being handled. */
    Initializing,

    /** `initialize` succeeded, so every handler is served; another `initialize` fails with `InvalidRequest`. */
    Initialized,

    /** `shutdown` was received: requests fail with `InvalidRequest`, notifications except `exit` are dropped. */
    ShutDown,

    /** `exit` was received and the session is closing. */
    Exited,
}

/** The LSP lifecycle state machine: which inbound messages a server serves in each [ServerState]. */
internal class ServerLifecycle {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(ServerState.Uninitialized)
    private var initializedNotified = false

    val state: StateFlow<ServerState> = mutableState.asStateFlow()

    /** The parameters of the successful `initialize` request. */
    @Volatile
    var initializeParams: InitializeParams? = null

    @Volatile
    var exitCode: Int? = null
        private set

    /** Whether ordinary requests and notifications are served. */
    val isServing: Boolean get() = mutableState.value == ServerState.Initialized

    /**
     * Runs [block] as the `initialize` request. Only an uninitialized server accepts it; when [block] fails the
     * server becomes uninitialized again, so the client may retry as `InitializeError.retry` allows.
     */
    suspend fun <T> initialize(block: suspend () -> T): T =
        mutex.withLock {
            val current = mutableState.value
            if (current != ServerState.Uninitialized) {
                throw JsonRpcException(ResponseError.INVALID_REQUEST, "Cannot initialize a server that is $current")
            }
            mutableState.value = ServerState.Initializing
            var succeeded = false
            try {
                val result = block()
                succeeded = true
                result
            } finally {
                val next = if (succeeded) ServerState.Initialized else ServerState.Uninitialized
                mutableState.compareAndSet(ServerState.Initializing, next)
            }
        }

    /** Throws the error the specification requires for a request that the current state does not serve. */
    fun requireServing(method: String) {
        when (mutableState.value) {
            ServerState.Initialized -> Unit
            ServerState.Uninitialized, ServerState.Initializing -> throw notInitialized(method)
            ServerState.ShutDown, ServerState.Exited -> throw shuttingDown(method)
        }
    }

    /** Returns true for the first `initialized` notification after a successful `initialize`. */
    suspend fun markInitialized(): Boolean =
        mutex.withLock {
            val first = isServing && !initializedNotified
            if (first) initializedNotified = true
            first
        }

    /** Accepts `shutdown`: only an initialized server may shut down, and only once. */
    suspend fun shutdown() {
        mutex.withLock {
            requireServing(LspMethods.SHUTDOWN)
            mutableState.value = ServerState.ShutDown
        }
    }

    /** Accepts `exit` in every state and returns the exit code: 0 after `shutdown`, otherwise 1. */
    fun exit(): Int {
        val code = if (mutableState.value == ServerState.ShutDown) 0 else 1
        exitCode = code
        mutableState.value = ServerState.Exited
        return code
    }
}

private fun notInitialized(method: String): JsonRpcException =
    JsonRpcException(ResponseError.SERVER_NOT_INITIALIZED, "Server is not initialized; cannot handle $method")

private fun shuttingDown(method: String): JsonRpcException =
    JsonRpcException(ResponseError.INVALID_REQUEST, "Server is shutting down; cannot handle $method")
