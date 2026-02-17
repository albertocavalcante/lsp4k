package io.lsp4k.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Parameters for the $/cancelRequest notification.
 *
 * The id field is a JsonElement because it can be either a string or a number.
 */
@Serializable
public data class CancelParams(
    /**
     * The request id to cancel.
     */
    val id: JsonElement,
)

/**
 * Parameters for the $/setTrace notification.
 */
@Serializable
public data class SetTraceParams(
    /**
     * The new value that should be assigned to the trace setting.
     */
    val value: TraceValue,
)

/**
 * Parameters for the $/logTrace notification.
 */
@Serializable
public data class LogTraceParams(
    /**
     * The message to be logged.
     */
    val message: String,
    /**
     * Additional information that can be computed if the `trace` configuration
     * is set to `'verbose'`.
     */
    val verbose: String? = null,
)
