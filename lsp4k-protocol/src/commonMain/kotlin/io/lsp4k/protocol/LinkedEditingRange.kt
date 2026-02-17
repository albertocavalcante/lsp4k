package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * Parameters for textDocument/linkedEditingRange request.
 */
@Serializable
public data class LinkedEditingRangeParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The position inside the text document.
     */
    val position: Position,
)

/**
 * The result of a linked editing range request.
 */
@Serializable
public data class LinkedEditingRanges(
    /**
     * A list of ranges that can be renamed together. The ranges must have
     * identical length and contain identical text content.
     */
    val ranges: List<Range>,
    /**
     * An optional word pattern (regular expression) that describes valid contents
     * for the given ranges.
     */
    val wordPattern: String? = null,
)
