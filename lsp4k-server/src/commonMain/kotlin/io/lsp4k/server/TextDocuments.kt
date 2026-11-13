package io.lsp4k.server

import io.lsp4k.protocol.DidChangeTextDocumentParams
import io.lsp4k.protocol.DidCloseTextDocumentParams
import io.lsp4k.protocol.DidOpenTextDocumentParams
import io.lsp4k.protocol.DocumentUri
import io.lsp4k.protocol.PositionEncodingKind
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Outcome of [TextDocuments.didChange]. */
public enum class ChangeResult {
    /** All content changes were applied and the version was updated. */
    APPLIED,

    /** The document is not open; the notification was ignored. */
    UNKNOWN_DOCUMENT,

    /** The version was not newer than the tracked one; the notification was ignored. */
    STALE_VERSION,
}

/**
 * Tracks the documents a client has open, fed by `textDocument/didOpen`, `didChange` and `didClose`.
 *
 * Full and incremental synchronisation are both handled per change event: an event without a range
 * replaces the whole text, an event with a range replaces that range. Events within one notification
 * apply in order, each against the result of the previous one. Ranges are interpreted with
 * [encoding], which must be the position encoding negotiated during `initialize`
 * (UTF-16 when the client offers nothing else).
 *
 * Version policy: versions must strictly increase between changes. A `didChange` whose version is
 * lower than or equal to the tracked one is dropped as stale ([ChangeResult.STALE_VERSION]) so a
 * reordered or duplicated notification cannot corrupt the text. A `didChange` for a document that is
 * not open is dropped ([ChangeResult.UNKNOWN_DOCUMENT]). A `didOpen` for an already open URI replaces
 * the tracked document, because open is authoritative. `didClose` for an unknown URI is a no-op.
 *
 * Duplicate versions: some clients resend a version (for example after undo) or reuse versions after
 * reopening. Such a `didChange` is dropped, so inspect the returned [ChangeResult] and log or resync on
 * [ChangeResult.STALE_VERSION] if your client behaves that way; closing and reopening resets tracking.
 * The deprecated `rangeLength` of a content change is ignored; only the range is used.
 *
 * Concurrency: every operation holds an internal [Mutex], so notifications dispatched concurrently
 * cannot lose updates or race the version check. Operations are therefore `suspend`. Returned
 * [TextDocument] snapshots are immutable.
 *
 * Wiring: register `didOpen`, `didChange` and `didClose` handlers on the server builder that forward
 * to [didOpen], [didChange] and [didClose].
 */
public class TextDocuments(
    public val encoding: PositionEncodingKind = PositionEncodingKind.UTF16,
) {
    private val lock = Mutex()
    private val documents = LinkedHashMap<DocumentUri, TextDocument>()

    /** Number of open documents. */
    public suspend fun size(): Int = lock.withLock { documents.size }

    /** The open document for [uri], or null. */
    public suspend fun get(uri: DocumentUri): TextDocument? = lock.withLock { documents[uri] }

    /** Snapshots of all open documents in the order they were opened. */
    public suspend fun all(): List<TextDocument> = lock.withLock { documents.values.toList() }

    /** The tracked version of [uri], or null when it is not open. */
    public suspend fun version(uri: DocumentUri): Int? = lock.withLock { documents[uri]?.version }

    /** Starts tracking the opened document, replacing any earlier state for the URI. */
    public suspend fun didOpen(params: DidOpenTextDocumentParams): TextDocument {
        val item = params.textDocument
        val document = TextDocument(item.uri, item.languageId, item.version, item.text, encoding)
        lock.withLock { documents[item.uri] = document }
        return document
    }

    /** Applies the content changes of [params] subject to the version policy. */
    public suspend fun didChange(params: DidChangeTextDocumentParams): ChangeResult = lock.withLock { applyChange(params) }

    private fun applyChange(params: DidChangeTextDocumentParams): ChangeResult {
        val id = params.textDocument
        val current = documents[id.uri] ?: return ChangeResult.UNKNOWN_DOCUMENT
        if (id.version <= current.version) return ChangeResult.STALE_VERSION
        var updated = current.withText(id.version, current.text)
        for (change in params.contentChanges) {
            val range = change.range
            updated =
                if (range == null) {
                    updated.withText(id.version, change.text)
                } else {
                    updated.withEdit(range, change.text)
                }
        }
        documents[id.uri] = updated
        return ChangeResult.APPLIED
    }

    /** Stops tracking the document and returns its last snapshot, or null when it was not open. */
    public suspend fun didClose(params: DidCloseTextDocumentParams): TextDocument? =
        lock.withLock { documents.remove(params.textDocument.uri) }

    /** Forgets every document, for example on shutdown. */
    public suspend fun clear() {
        lock.withLock { documents.clear() }
    }
}
