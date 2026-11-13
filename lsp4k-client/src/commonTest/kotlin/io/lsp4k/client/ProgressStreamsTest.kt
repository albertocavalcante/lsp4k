package io.lsp4k.client

import io.lsp4k.protocol.Either
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.ProgressToken
import io.lsp4k.protocol.WorkDoneProgressBegin
import io.lsp4k.protocol.WorkDoneProgressEnd
import io.lsp4k.protocol.WorkDoneProgressReport
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProgressStreamsTest {
    private val token: ProgressToken = Either.right("work")

    @Test
    fun `work done flow yields begin report end and then completes`() =
        runTest {
            val peer = TestPeer(backgroundScope)
            val client = languageClient {}.start(peer.transport, backgroundScope)
            val updates = mutableListOf<WorkDoneUpdate>()
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) { client.progress.workDone(token).toList(updates) }

            peer.send(LspMethods.PROGRESS, """{"token":"other","value":{"kind":"begin","title":"ignored"}}""")
            peer.send(LspMethods.PROGRESS, """{"token":"work","value":{"kind":"begin","title":"Index","percentage":0}}""")
            peer.send(LspMethods.PROGRESS, """{"token":"work","value":{"kind":"report","message":"half","percentage":50}}""")
            peer.send(LspMethods.PROGRESS, """{"token":"work","value":{"kind":"end","message":"done"}}""")
            collector.join()

            assertEquals(
                listOf(
                    WorkDoneUpdate.Begin(WorkDoneProgressBegin(title = "Index", percentage = 0)),
                    WorkDoneUpdate.Report(WorkDoneProgressReport(message = "half", percentage = 50)),
                    WorkDoneUpdate.End(WorkDoneProgressEnd(message = "done")),
                ),
                updates,
            )
            client.close()
        }

    @Test
    fun `partial results are decoded and keyed by integer token`() =
        runTest {
            val peer = TestPeer(backgroundScope)
            val client = languageClient {}.start(peer.transport, backgroundScope)
            val intToken: ProgressToken = Either.left(7)
            val batches =
                async(start = CoroutineStart.UNDISPATCHED) {
                    client.progress
                        .partialResults<List<String>>(intToken)
                        .take(2)
                        .toList()
                }

            peer.send(LspMethods.PROGRESS, """{"token":"work","value":["wrong"]}""")
            peer.send(LspMethods.PROGRESS, """{"token":7,"value":["a","b"]}""")
            peer.send(LspMethods.PROGRESS, """{"token":7,"value":["c"]}""")

            assertEquals(listOf(listOf("a", "b"), listOf("c")), batches.await())
            client.close()
        }

    @Test
    fun `all streams every token and values stays raw`() =
        runTest {
            val peer = TestPeer(backgroundScope)
            val client = languageClient {}.start(peer.transport, backgroundScope)
            val first = async(start = CoroutineStart.UNDISPATCHED) { client.progress.all.first() }
            val raw = async(start = CoroutineStart.UNDISPATCHED) { client.progress.values(token).first() }

            peer.send(LspMethods.PROGRESS, """{"token":"work","value":{"x":1}}""")

            assertEquals(token, first.await().token)
            assertEquals(Json.parseToJsonElement("""{"x":1}"""), raw.await())
            client.close()
        }

    @Test
    fun `an unknown work done kind fails the flow`() =
        runTest {
            val peer = TestPeer(backgroundScope)
            val client = languageClient {}.start(peer.transport, backgroundScope)
            val outcome =
                async(start = CoroutineStart.UNDISPATCHED) { runCatching { client.progress.workDone(token).first() } }

            peer.send(LspMethods.PROGRESS, """{"token":"work","value":{"kind":"bogus"}}""")

            assertIs<SerializationException>(outcome.await().exceptionOrNull())
            client.close()
        }
}
