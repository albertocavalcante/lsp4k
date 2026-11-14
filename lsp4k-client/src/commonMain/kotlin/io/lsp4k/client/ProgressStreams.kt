package io.lsp4k.client

import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.ProgressParams
import io.lsp4k.protocol.ProgressToken
import io.lsp4k.protocol.WorkDoneProgressBegin
import io.lsp4k.protocol.WorkDoneProgressEnd
import io.lsp4k.protocol.WorkDoneProgressReport
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transform
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer

/**
 * One value of a work done progress stream.
 */
public sealed interface WorkDoneUpdate {
    /** The operation started. */
    public data class Begin(
        val value: WorkDoneProgressBegin,
    ) : WorkDoneUpdate

    /** The operation reported intermediate progress. */
    public data class Report(
        val value: WorkDoneProgressReport,
    ) : WorkDoneUpdate

    /** The operation finished; the stream completes after this value. */
    public data class End(
        val value: WorkDoneProgressEnd,
    ) : WorkDoneUpdate
}

/**
 * Consumes `$/progress` notifications as [Flow]s keyed by progress token.
 *
 * The streams are hot and unbuffered per subscriber: a value is only delivered to flows that are
 * already being collected when the notification arrives. Start collecting before the request that
 * carries the token is sent (for client-initiated work done and partial results), or inside the
 * `window/workDoneProgress/create` handler (for server-initiated work done): the client answers the
 * request only after that handler returns, and a server reports progress for the token only once it
 * has the answer. `$/progress` runs on the control lane ([CLIENT_CONTROL_NOTIFICATIONS]), so it is
 * not held back behind a running request handler, and a server that reports progress without
 * waiting for that answer can reach the stream before the handler subscribed. A subscriber that
 * falls more than [BUFFER_CAPACITY] notifications behind loses the oldest ones.
 */
public class ProgressStreams internal constructor(
    @PublishedApi internal val json: Json,
) {
    private val hub =
        MutableSharedFlow<ProgressParams<JsonElement>>(
            extraBufferCapacity = BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    /** Every `$/progress` notification received, for any token. */
    public val all: Flow<ProgressParams<JsonElement>> = hub.asSharedFlow()

    internal fun publish(params: ProgressParams<JsonElement>) {
        hub.tryEmit(params)
    }

    /** Raw progress values reported for [token]. */
    public fun values(token: ProgressToken): Flow<JsonElement> = hub.filter { it.token == token }.map { it.value }

    /** Partial results for [token], each decoded with [serializer] (for example a list of locations). */
    public fun <T> partialResults(
        token: ProgressToken,
        serializer: KSerializer<T>,
    ): Flow<T> = values(token).map { json.decodeFromJsonElement(serializer, it) }

    /** Partial results for [token] decoded as [T]. */
    public inline fun <reified T> partialResults(token: ProgressToken): Flow<T> = partialResults(token, serializer<T>())

    /**
     * Work done progress for [token]: a begin, any number of reports, then an end, after which the
     * flow completes.
     */
    public fun workDone(token: ProgressToken): Flow<WorkDoneUpdate> =
        values(token)
            .map { decodeWorkDone(it) }
            .transform<WorkDoneUpdate, WorkDoneUpdate?> {
                emit(it)
                if (it is WorkDoneUpdate.End) emit(null)
            }.takeWhile { it != null }
            .filterNotNull()

    private fun decodeWorkDone(value: JsonElement): WorkDoneUpdate {
        val kind = ((value as? JsonObject)?.get("kind") as? JsonPrimitive)?.content
        return when (kind) {
            "begin" -> WorkDoneUpdate.Begin(json.decodeFromJsonElement(WorkDoneProgressBegin.serializer(), value))
            "report" -> WorkDoneUpdate.Report(json.decodeFromJsonElement(WorkDoneProgressReport.serializer(), value))
            "end" -> WorkDoneUpdate.End(json.decodeFromJsonElement(WorkDoneProgressEnd.serializer(), value))
            else -> throw SerializationException("Unknown work done progress kind for ${LspMethods.PROGRESS}: $kind")
        }
    }

    internal companion object {
        const val BUFFER_CAPACITY: Int = 1024
    }
}
