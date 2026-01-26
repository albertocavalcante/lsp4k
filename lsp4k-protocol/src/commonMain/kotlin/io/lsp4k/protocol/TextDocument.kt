package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * Parameters for textDocument/didOpen notification.
 */
@Serializable
public data class DidOpenTextDocumentParams(
    /**
     * The document that was opened.
     */
    val textDocument: TextDocumentItem,
)

/**
 * Parameters for textDocument/didClose notification.
 */
@Serializable
public data class DidCloseTextDocumentParams(
    /**
     * The document that was closed.
     */
    val textDocument: TextDocumentIdentifier,
)

/**
 * Parameters for textDocument/didChange notification.
 */
@Serializable
public data class DidChangeTextDocumentParams(
    /**
     * The document that did change.
     */
    val textDocument: VersionedTextDocumentIdentifier,
    /**
     * The actual content changes.
     */
    val contentChanges: List<TextDocumentContentChangeEvent>,
)

/**
 * An event describing a change to a text document.
 */
@Serializable
public data class TextDocumentContentChangeEvent(
    /**
     * The range of the document that changed.
     * If null, the whole document changed.
     */
    val range: Range? = null,
    /**
     * The optional length of the range that got replaced.
     * @deprecated Use range instead.
     */
    val rangeLength: Int? = null,
    /**
     * The new text for the provided range, or the new text of the whole document.
     */
    val text: String,
)

/**
 * Parameters for textDocument/didSave notification.
 */
@Serializable
public data class DidSaveTextDocumentParams(
    /**
     * The document that was saved.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * Optional the content when saved. Depends on the includeText value.
     */
    val text: String? = null,
)

/**
 * Parameters for textDocument/willSave notification.
 */
@Serializable
public data class WillSaveTextDocumentParams(
    /**
     * The document that will be saved.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The 'TextDocumentSaveReason'.
     */
    val reason: TextDocumentSaveReason,
)

/**
 * Represents reasons why a text document is saved.
 */
@Serializable
public enum class TextDocumentSaveReason {
    /**
     * Manually triggered, e.g. by the user pressing save.
     */
    Manual,

    /**
     * Automatic after a delay.
     */
    AfterDelay,

    /**
     * When the editor lost focus.
     */
    FocusOut,
}
