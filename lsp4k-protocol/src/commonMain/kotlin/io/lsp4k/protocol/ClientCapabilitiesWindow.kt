package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * Window specific client capabilities.
 */
@Serializable
public data class WindowClientCapabilities(
    /**
     * It indicates whether the client supports server initiated progress using the `window/workDoneProgress/create` request.
     */
    val workDoneProgress: Boolean? = null,
    /**
     * Capabilities specific to the showMessage request.
     */
    val showMessage: ShowMessageRequestClientCapabilities? = null,
    /**
     * Client capabilities for the show document request.
     */
    val showDocument: ShowDocumentClientCapabilities? = null,
)

/**
 * Show message request client capabilities.
 */
@Serializable
public data class ShowMessageRequestClientCapabilities(
    /**
     * Capabilities specific to the `MessageActionItem` type.
     */
    val messageActionItem: MessageActionItemCapabilities? = null,
)

/**
 * Capabilities for message action items.
 */
@Serializable
public data class MessageActionItemCapabilities(
    /**
     * Whether the client supports additional attributes which are preserved and sent back to the server.
     */
    val additionalPropertiesSupport: Boolean? = null,
)

/**
 * Client capabilities for the show document request.
 */
@Serializable
public data class ShowDocumentClientCapabilities(
    /**
     * The client has support for the show document request.
     */
    val support: Boolean,
)
