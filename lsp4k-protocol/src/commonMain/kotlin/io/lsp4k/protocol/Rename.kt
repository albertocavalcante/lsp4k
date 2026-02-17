package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * Parameters for textDocument/rename request.
 */
@Serializable
public data class RenameParams(
    /**
     * The document to rename.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The position at which this request was sent.
     */
    val position: Position,
    /**
     * The new name of the symbol.
     */
    val newName: String,
    /**
     * An optional token that a server can use to report work done progress.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
)

/**
 * Parameters for textDocument/prepareRename request.
 */
@Serializable
public data class PrepareRenameParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The position inside the text document.
     */
    val position: Position,
    /**
     * An optional token that a server can use to report work done progress.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
)

/**
 * Result of textDocument/prepareRename request.
 */
@Serializable
public data class PrepareRenameResult(
    /**
     * The range of the string to rename.
     */
    val range: Range,
    /**
     * A placeholder text of the string content to be renamed.
     */
    val placeholder: String,
)
