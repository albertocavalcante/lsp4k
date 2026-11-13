package io.lsp4k.client

import io.lsp4k.protocol.Either
import io.lsp4k.protocol.LogMessageParams
import io.lsp4k.protocol.LogTraceParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.MessageType
import io.lsp4k.protocol.ProgressParams
import io.lsp4k.protocol.PublishDiagnosticsParams
import io.lsp4k.protocol.ShowMessageParams
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerNotificationHandlersTest {
    @Test
    fun `window notifications reach their hooks`() =
        runTest {
            val shown = Channel<ShowMessageParams>(Channel.UNLIMITED)
            val logged = Channel<LogMessageParams>(Channel.UNLIMITED)
            val peer = TestPeer(backgroundScope)
            languageClient {
                window {
                    showMessage { shown.send(it) }
                    logMessage { logged.send(it) }
                }
            }.start(peer.transport, backgroundScope)

            peer.send(LspMethods.WINDOW_SHOW_MESSAGE, """{"type":2,"message":"careful"}""")
            peer.send(LspMethods.WINDOW_LOG_MESSAGE, """{"type":4,"message":"detail"}""")
            assertEquals(ShowMessageParams(type = MessageType.Warning, message = "careful"), shown.receive())
            assertEquals(LogMessageParams(type = MessageType.Log, message = "detail"), logged.receive())
        }

    @Test
    fun `diagnostics trace and telemetry reach their hooks`() =
        runTest {
            val diagnostics = Channel<PublishDiagnosticsParams>(Channel.UNLIMITED)
            val traces = Channel<LogTraceParams>(Channel.UNLIMITED)
            val telemetry = Channel<JsonElement>(Channel.UNLIMITED)
            val peer = TestPeer(backgroundScope)
            languageClient {
                textDocument { publishDiagnostics { diagnostics.send(it) } }
                onLogTrace { traces.send(it) }
                onTelemetryEvent { telemetry.send(it) }
            }.start(peer.transport, backgroundScope)

            peer.send(LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS, """{"uri":"file:///a.kt","diagnostics":[]}""")
            peer.send(LspMethods.LOG_TRACE, """{"message":"m","verbose":"v"}""")
            peer.send(LspMethods.TELEMETRY_EVENT, """{"event":"started"}""")
            peer.send(LspMethods.TELEMETRY_EVENT, null)
            assertEquals(PublishDiagnosticsParams(uri = "file:///a.kt", diagnostics = emptyList()), diagnostics.receive())
            assertEquals(LogTraceParams(message = "m", verbose = "v"), traces.receive())
            assertEquals(Json.parseToJsonElement("""{"event":"started"}"""), telemetry.receive())
            assertEquals(JsonNull, telemetry.receive())
        }

    @Test
    fun `progress reaches the hook and the progress streams`() =
        runTest {
            val hook = Channel<ProgressParams<JsonElement>>(Channel.UNLIMITED)
            val peer = TestPeer(backgroundScope)
            val client =
                languageClient { onProgress { hook.send(it) } }.start(peer.transport, backgroundScope)

            peer.send(LspMethods.PROGRESS, """{"token":"t","value":{"n":1}}""")
            val seen = hook.receive()
            assertEquals(Either.right("t"), seen.token)
            assertEquals(Json.parseToJsonElement("""{"n":1}"""), seen.value)
            client.close()
        }
}
