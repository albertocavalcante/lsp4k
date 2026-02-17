package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * The message type.
 */
@Serializable(with = MessageTypeSerializer::class)
public enum class MessageType(
    public val value: Int,
) {
    /**
     * An error message.
     */
    Error(1),

    /**
     * A warning message.
     */
    Warning(2),

    /**
     * An information message.
     */
    Info(3),

    /**
     * A log message.
     */
    Log(4),

    ;

    public companion object {
        public fun fromValue(value: Int): MessageType =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown MessageType: $value")
    }
}

/**
 * Serializer for MessageType that encodes/decodes as integer.
 */
public object MessageTypeSerializer : IntEnumSerializer<MessageType>(
    "MessageType", MessageType::fromValue, { it.value },
)

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
    @Serializable(with = ProgressTokenSerializer::class)
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
    @Serializable(with = ProgressTokenSerializer::class)
    val token: ProgressToken,
    /**
     * The progress data.
     */
    val value: T,
)

/**
 * Parameters for the window/workDoneProgress/cancel notification.
 */
@Serializable
public data class WorkDoneProgressCancelParams(
    /**
     * The token to be used to report progress.
     */
    @Serializable(with = ProgressTokenSerializer::class)
    val token: ProgressToken,
)

/**
 * Parameters for the window/showDocument request.
 */
@Serializable
public data class ShowDocumentParams(
    /**
     * The document URI to show.
     */
    val uri: String,
    /**
     * Indicates to show the resource in an external program.
     * To show for example `https://code.visualstudio.com/`
     * in the default WEB browser set `external` to `true`.
     */
    val external: Boolean? = null,
    /**
     * An optional property to indicate whether the editor showing the document
     * should take focus or not. Clients might ignore this property if an
     * external program is started.
     */
    val takeFocus: Boolean? = null,
    /**
     * An optional selection range if the document is a text document.
     * Clients might ignore the property if an external program is started
     * or the file is not a text file.
     */
    val selection: Range? = null,
)

/**
 * The result of a show document request.
 */
@Serializable
public data class ShowDocumentResult(
    /**
     * A boolean indicating if the show was successful.
     */
    val success: Boolean,
)
