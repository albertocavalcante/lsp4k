package io.lsp4k.server

import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.WorkDoneProgressCancelParams

/**
 * Handlers for window notifications sent by the client.
 */
@LspServerDsl
public class WindowHandlersBuilder internal constructor(
    registry: HandlerRegistry,
) : HandlersBuilder(registry) {
    /**
     * Handles `window/workDoneProgress/cancel` notifications. The server first marks the matching
     * [WorkDoneProgress] as cancelled, then runs this handler.
     */
    public fun workDoneProgressCancel(handler: suspend HandlerContext.(WorkDoneProgressCancelParams) -> Unit) {
        notification(LspMethods.WINDOW_WORK_DONE_PROGRESS_CANCEL, handler)
    }
}
