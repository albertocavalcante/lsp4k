package io.lsp4k.server

import io.lsp4k.protocol.Either
import io.lsp4k.protocol.LogMessageParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.MessageActionItem
import io.lsp4k.protocol.MessageType
import io.lsp4k.protocol.ProgressToken
import io.lsp4k.protocol.ShowDocumentParams
import io.lsp4k.protocol.ShowDocumentResult
import io.lsp4k.protocol.ShowMessageParams
import io.lsp4k.protocol.ShowMessageRequestParams
import io.lsp4k.protocol.WorkDoneProgressCreateParams
import kotlin.time.Duration

/**
 * Window requests and notifications sent to the client.
 */
public class WindowCalls internal constructor(
    private val sender: ClientSender,
    private val progress: ProgressRegistry,
) {
    /** Sends a `window/showMessage` notification. */
    public suspend fun showMessage(params: ShowMessageParams) {
        sender.notify(LspMethods.WINDOW_SHOW_MESSAGE, params, ShowMessageParams.serializer())
    }

    /** Sends a `window/showMessage` notification with [type] and [message]. */
    public suspend fun showMessage(
        type: MessageType,
        message: String,
    ) {
        showMessage(ShowMessageParams(type, message))
    }

    /** Sends a `window/logMessage` notification. */
    public suspend fun logMessage(params: LogMessageParams) {
        sender.notify(LspMethods.WINDOW_LOG_MESSAGE, params, LogMessageParams.serializer())
    }

    /** Sends a `window/logMessage` notification with [type] and [message]. */
    public suspend fun logMessage(
        type: MessageType,
        message: String,
    ) {
        logMessage(LogMessageParams(type, message))
    }

    /**
     * Sends `window/showMessageRequest` and returns the action the user picked, or null when none was picked.
     * By default it waits as long as the user needs.
     */
    public suspend fun showMessageRequest(
        params: ShowMessageRequestParams,
        timeout: Duration = Duration.INFINITE,
    ): MessageActionItem? {
        val result = sender.request(LspMethods.WINDOW_SHOW_MESSAGE_REQUEST, params, ShowMessageRequestParams.serializer(), timeout)
        return sender.decodeOrNull(result, MessageActionItem.serializer())
    }

    /** Sends `window/showDocument` and returns whether the client showed the document. */
    public suspend fun showDocument(
        params: ShowDocumentParams,
        timeout: Duration = DEFAULT_REQUEST_TIMEOUT,
    ): ShowDocumentResult {
        val result = sender.request(LspMethods.WINDOW_SHOW_DOCUMENT, params, ShowDocumentParams.serializer(), timeout)
        return sender.decode(LspMethods.WINDOW_SHOW_DOCUMENT, result, ShowDocumentResult.serializer())
    }

    /**
     * Sends `window/workDoneProgress/create` and returns the created progress, ready for
     * [WorkDoneProgress.begin]. A unique string token is generated when [token] is null. Clients advertise
     * support with the `window.workDoneProgress` client capability; when the client answers with an error,
     * this throws and the token must not be used.
     */
    public suspend fun createWorkDoneProgress(
        token: ProgressToken? = null,
        timeout: Duration = DEFAULT_REQUEST_TIMEOUT,
    ): WorkDoneProgress {
        val created: ProgressToken = token ?: Either.right(sender.nextId("lsp4k-progress"))
        sender.request(
            LspMethods.WINDOW_WORK_DONE_PROGRESS_CREATE,
            WorkDoneProgressCreateParams(created),
            WorkDoneProgressCreateParams.serializer(),
            timeout,
        )
        return WorkDoneProgress(created, sender, progress)
    }
}
