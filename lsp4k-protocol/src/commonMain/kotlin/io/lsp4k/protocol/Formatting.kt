package io.lsp4k.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Value-object describing what options formatting should use.
 */
@Serializable
public data class FormattingOptions(
    /**
     * Size of a tab in spaces.
     */
    val tabSize: Int,
    /**
     * Prefer spaces over tabs.
     */
    val insertSpaces: Boolean,
    /**
     * Trim trailing whitespace on a line.
     */
    val trimTrailingWhitespace: Boolean? = null,
    /**
     * Insert a newline character at the end of the file if one does not exist.
     */
    val insertFinalNewline: Boolean? = null,
    /**
     * Trim all newlines after the final newline at the end of the file.
     */
    val trimFinalNewlines: Boolean? = null,
)

/**
 * Parameters for textDocument/formatting request.
 */
@Serializable
public data class DocumentFormattingParams(
    /**
     * The document to format.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The format options.
     */
    val options: FormattingOptions,
    /**
     * An optional token that a server can use to report work done progress.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
)

/**
 * Parameters for textDocument/rangeFormatting request.
 */
@Serializable
public data class DocumentRangeFormattingParams(
    /**
     * The document to format.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The range to format.
     */
    val range: Range,
    /**
     * The format options.
     */
    val options: FormattingOptions,
    /**
     * An optional token that a server can use to report work done progress.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
)

/**
 * Parameters for textDocument/onTypeFormatting request.
 */
@Serializable
public data class DocumentOnTypeFormattingParams(
    /**
     * The document to format.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The position around which the on type formatting should happen.
     */
    val position: Position,
    /**
     * The character that has been typed that triggered the formatting on type request.
     */
    val ch: String,
    /**
     * The format options.
     */
    val options: FormattingOptions,
)

/**
 * Parameters for textDocument/rangesFormatting request.
 */
@Serializable
public data class DocumentRangesFormattingParams(
    val textDocument: TextDocumentIdentifier,
    val ranges: List<Range>,
    val options: FormattingOptions,
)
