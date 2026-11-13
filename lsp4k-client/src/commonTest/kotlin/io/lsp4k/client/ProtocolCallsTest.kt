package io.lsp4k.client

import io.lsp4k.protocol.CancelParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.ProgressParams
import io.lsp4k.protocol.SetTraceParams
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test

internal val protocolCases: List<CallCase> =
    listOf(
        notificationCase<SetTraceParams>(LspMethods.SET_TRACE, """{"value":"verbose"}""") { setTrace(it) },
        notificationCase<ProgressParams<JsonElement>>(LspMethods.PROGRESS, """{"token":"t","value":{"n":1}}""") { progress(it) },
        notificationCase<CancelParams>(LspMethods.CANCEL_REQUEST, """{"id":1}""") { cancelRequest(it) },
    )

class ProtocolCallsTest {
    @Test
    fun `trace progress and cancel notifications reach the server`() =
        runTest {
            runCases(backgroundScope, protocolCases)
        }
}
