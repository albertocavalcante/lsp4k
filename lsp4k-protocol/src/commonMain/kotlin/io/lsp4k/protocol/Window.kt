package io.lsp4k.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The message type.
 */
@Serializable
public enum class MessageType {
    /**
     * An error message.
     */
    @SerialName("1")
    Error,

    /**
     * A warning message.
     */
    @SerialName("2")
    Warning,

    /**
     * An information message.
     */
    @SerialName("3")
    Info,

    /**
     * A log message.
     */
    @SerialName("4")
    Log,
}

/**
 * The parameters of a window/showMessage notification.
 */
@Serializable
public data class ShowMessageParams(
    /**
     * The message type.
     */
    val type: MessageType,
    /**
     * The actual message.
     */
    val message: String,
)

/**
 * The parameters of a window/logMessage notification.
 */
@Serializable
public data class LogMessageParams(
    /**
     * The message type.
     */
    val type: MessageType,
    /**
     * The actual message.
     */
    val message: String,
)

/**
 * The parameters of a window/showMessageRequest request.
 */
@Serializable
public data class ShowMessageRequestParams(
    /**
     * The message type.
     */
    val type: MessageType,
    /**
     * The actual message.
     */
    val message: String,
    /**
     * The message action items to present.
     */
    val actions: List<MessageActionItem>? = null,
)

/**
 * A message action item.
 */
@Serializable
public data class MessageActionItem(
    /**
     * A short title like 'Retry', 'Open Log' etc.
     */
    val title: String,
)

/**
 * Work done progress begin parameters.
 */
@Serializable
public data class WorkDoneProgressBegin(
    val kind: String = "begin",
    /**
     * Mandatory title of the progress operation.
     */
    val title: String,
    /**
     * Controls if a cancel button should show to allow the user to cancel the operation.
     */
    val cancellable: Boolean? = null,
    /**
     * Optional, more detailed associated progress message.
     */
    val message: String? = null,
    /**
     * Optional progress percentage to display (value 100 is considered 100%).
     */
    val percentage: Int? = null,
)

/**
 * Work done progress report parameters.
 */
@Serializable
public data class WorkDoneProgressReport(
    val kind: String = "report",
    /**
     * Controls enablement state of a cancel button.
     */
    val cancellable: Boolean? = null,
    /**
     * Optional, more detailed associated progress message.
     */
    val message: String? = null,
    /**
     * Optional progress percentage to display.
     */
    val percentage: Int? = null,
)

/**
 * Work done progress end parameters.
 */
@Serializable
public data class WorkDoneProgressEnd(
    val kind: String = "end",
    /**
     * Optional, a final message indicating completion.
     */
    val message: String? = null,
)

/**
 * Parameters for window/workDoneProgress/create request.
 */
@Serializable
public data class WorkDoneProgressCreateParams(
    /**
     * The token to be used to report progress.
     */
    val token: ProgressToken,
)

/**
 * Progress notification parameters.
 */
@Serializable
public data class ProgressParams<T>(
    /**
     * The progress token provided by the client or server.
     */
    val token: ProgressToken,
    /**
     * The progress data.
     */
    val value: T,
)
