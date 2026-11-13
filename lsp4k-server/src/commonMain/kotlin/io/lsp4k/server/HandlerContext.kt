package io.lsp4k.server

import io.lsp4k.protocol.Either
import io.lsp4k.protocol.ProgressToken
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlin.concurrent.Volatile

internal const val WORK_DONE_TOKEN: String = "workDoneToken"

internal const val PARTIAL_RESULT_TOKEN: String = "partialResultToken"

/**
 * Receiver of every handler registered through the server DSL.
 *
 * Requests run concurrently and notifications run in order, each after the earlier requests finished (see
 * [LanguageServerConfig.start]). A handler may send notifications through [client] and await client requests: responses are read
 * outside the dispatch queue.
 *
 * @property server The running server that dispatched the message.
 */
public open class HandlerContext internal constructor(
    public val server: LanguageServer,
) {
    /** Proxy for requests and notifications sent to the client. */
    public val client: LanguageClient get() = server.client
}

/**
 * Receiver of request handlers.
 *
 * @property method The JSON-RPC method being handled.
 * @property workDoneProgress Work done progress bound to the request's `workDoneToken`, or null when the client
 *   sent none. Progress that is still running when the handler returns is ended automatically.
 */
public open class RequestContext internal constructor(
    server: LanguageServer,
    public val method: String,
    public val workDoneProgress: WorkDoneProgress?,
) : HandlerContext(server)

/**
 * Receiver of request handlers whose results can be streamed as partial results.
 *
 * When the client sent a [partialResultToken], [sendPartialResult] reports each chunk with `$/progress`. Once a
 * chunk was sent, the specification requires the whole result to be reported that way and the final response
 * to be empty (an empty array, or the empty value of the method's result type).
 *
 * @property partialResultToken The request's `partialResultToken`, or null when the client sent none.
 */
public class PartialResultContext<T> internal constructor(
    base: RequestContext,
    public val partialResultToken: ProgressToken?,
    private val serializer: KSerializer<T>,
) : RequestContext(base.server, base.method, base.workDoneProgress) {
    @Volatile
    private var open = true

    /** Whether at least one partial result was sent. */
    @Volatile
    public var partialResultsSent: Boolean = false
        private set

    /**
     * Sends [value] as the next partial result and returns true, or returns false without sending anything when
     * the client provided no [partialResultToken].
     *
     * @throws IllegalStateException when called after the handler returned.
     */
    public suspend fun sendPartialResult(value: T): Boolean {
        val token = partialResultToken ?: return false
        check(open) { "Partial results of $method must be sent before its response" }
        client.progress(token, value, serializer)
        partialResultsSent = true
        return true
    }

    internal fun close() {
        open = false
    }
}

/** Reads a progress token member of raw request params, ignoring values that are not a string or an integer. */
internal fun progressTokenOf(
    params: JsonElement?,
    key: String,
): ProgressToken? {
    val element = (params as? JsonObject)?.get(key) as? JsonPrimitive ?: return null
    return if (element.isString) Either.right(element.content) else element.intOrNull?.let { Either.left(it) }
}
