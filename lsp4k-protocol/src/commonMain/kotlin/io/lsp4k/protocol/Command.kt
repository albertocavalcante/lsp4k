package io.lsp4k.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a reference to a command.
 * Provides a title which will be used to represent a command in the UI.
 */
@Serializable
public data class Command(
    /**
     * Title of the command, like `save`.
     */
    val title: String,
    /**
     * The identifier of the actual command handler.
     */
    val command: String,
    /**
     * Arguments that the command handler should be invoked with.
     */
    val arguments: List<JsonElement>? = null,
)

/**
 * Execute command params.
 */
@Serializable
public data class ExecuteCommandParams(
    /**
     * The identifier of the actual command handler.
     */
    val command: String,
    /**
     * Arguments that the command should be invoked with.
     */
    val arguments: List<JsonElement>? = null,
)
