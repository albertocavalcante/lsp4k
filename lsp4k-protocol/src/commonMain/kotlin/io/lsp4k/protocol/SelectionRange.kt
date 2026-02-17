package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * Parameters for textDocument/selectionRange request.
 */
@Serializable
public data class SelectionRangeParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The positions inside the text document.
     */
    val positions: List<Position>,
    /**
     * An optional token that a server can use to report work done progress.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
    /**
     * An optional token that a server can use to report partial results.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val partialResultToken: ProgressToken? = null,
)

/**
 * Represents a selection range.
 */
@Serializable
public data class SelectionRange(
    /**
     * The range of this selection range.
     */
    val range: Range,
    /**
     * The parent selection range containing this range. Therefore
     * `parent.range` must contain `this.range`.
     */
    val parent: SelectionRange? = null,
)
