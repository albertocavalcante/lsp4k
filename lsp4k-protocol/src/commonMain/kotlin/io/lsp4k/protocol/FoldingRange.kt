package io.lsp4k.protocol

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

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
    @Serializable(with = ProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
    /**
     * An optional token that a server can use to report partial results.
     */
    @Serializable(with = ProgressTokenSerializer::class)
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
 * A set of predefined range kinds. The set is open: servers may send, and clients must
 * accept, custom values.
 */
@Serializable
@JvmInline
public value class FoldingRangeKind(
    public val value: String,
) {
    public companion object {
        /**
         * Folding range for a comment.
         */
        public val Comment: FoldingRangeKind = FoldingRangeKind("comment")

        /**
         * Folding range for an import or include.
         */
        public val Imports: FoldingRangeKind = FoldingRangeKind("imports")

        /**
         * Folding range for a region (for example `#region`).
         */
        public val Region: FoldingRangeKind = FoldingRangeKind("region")
    }
}

/**
 * Client workspace capabilities specific to folding ranges.
 */
@Serializable
public data class FoldingRangeWorkspaceClientCapabilities(
    /**
     * Whether the client implementation supports a refresh request sent from the server to the
     * client.
     *
     * Note that this event is global and will force the client to refresh all folding ranges
     * currently shown. It should be used with absolute care and is useful for situations where a
     * server, for example, detects a project wide change that requires such a calculation.
     */
    val refreshSupport: Boolean? = null,
)
