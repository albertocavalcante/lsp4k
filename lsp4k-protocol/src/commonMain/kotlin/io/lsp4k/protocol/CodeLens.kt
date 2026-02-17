package io.lsp4k.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Parameters for textDocument/codeLens request.
 */
@Serializable
public data class CodeLensParams(
    /**
     * The document to request code lens for.
     */
    val textDocument: TextDocumentIdentifier,
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
 * A code lens represents a command that should be shown along with source text.
 */
@Serializable
public data class CodeLens(
    /**
     * The range in which this code lens is valid. Should only span a single line.
     */
    val range: Range,
    /**
     * The command this code lens represents.
     */
    val command: Command? = null,
    /**
     * A data entry field that is preserved on a code lens item between
     * a code lens and a code lens resolve request.
     */
    val data: JsonElement? = null,
)
