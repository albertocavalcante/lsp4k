package io.lsp4k.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Parameters for textDocument/foldingRange request.
 */
@Serializable
public data class FoldingRangeParams(
    /**
     * The text document.
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
 * Represents a folding range.
 */
@Serializable
public data class FoldingRange(
    /**
     * The zero-based start line of the range to fold. The folded area starts after the
     * line's last character.
     */
    val startLine: Int,
    /**
     * The zero-based character offset from where the folded range starts. If not defined,
     * defaults to the length of the start line.
     */
    val startCharacter: Int? = null,
    /**
     * The zero-based end line of the range to fold. The folded area ends with the
     * line's last character.
     */
    val endLine: Int,
    /**
     * The zero-based character offset before the folded range ends. If not defined,
     * defaults to the length of the end line.
     */
    val endCharacter: Int? = null,
    /**
     * Describes the kind of the folding range such as `comment` or `region`.
     */
    val kind: FoldingRangeKind? = null,
    /**
     * The text that the client should show when the specified range is collapsed.
     */
    val collapsedText: String? = null,
)

/**
 * Enum of known range kinds.
 */
@Serializable
public enum class FoldingRangeKind {
    @SerialName("comment") Comment,
    @SerialName("imports") Imports,
    @SerialName("region") Region,
}
