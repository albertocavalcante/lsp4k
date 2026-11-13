package io.lsp4k.server

import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.PublishDiagnosticsParams

/**
 * Text document notifications sent to the client.
 */
public class TextDocumentCalls internal constructor(
    private val sender: ClientSender,
) {
    /** Sends a `textDocument/publishDiagnostics` notification. */
    public suspend fun publishDiagnostics(params: PublishDiagnosticsParams) {
        sender.notify(LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS, params, PublishDiagnosticsParams.serializer())
    }
}
