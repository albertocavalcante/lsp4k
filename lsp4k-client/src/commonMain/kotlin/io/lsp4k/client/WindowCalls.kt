package io.lsp4k.client

import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.WorkDoneProgressCancelParams

/**
 * Window notifications sent to the server (the `window` namespace).
 */
public class WindowCalls internal constructor(
    private val caller: ServerCaller,
) {
    /**
     * Ask the server to cancel a server-initiated work done progress.
     */
    public suspend fun workDoneProgressCancel(params: WorkDoneProgressCancelParams): Unit =
        caller.notify(LspMethods.WINDOW_WORK_DONE_PROGRESS_CANCEL, params)
}
