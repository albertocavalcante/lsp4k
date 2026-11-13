package io.lsp4k.client

import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.WorkDoneProgressCancelParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal val windowCases: List<CallCase> =
    listOf(
        notificationCase<WorkDoneProgressCancelParams>(
            LspMethods.WINDOW_WORK_DONE_PROGRESS_CANCEL,
            """{"token":"t"}""",
        ) { window.workDoneProgressCancel(it) },
    )

class WindowCallsTest {
    @Test
    fun `every call reaches the server with its method and params`() =
        runTest {
            runCases(backgroundScope, windowCases)
        }
}
