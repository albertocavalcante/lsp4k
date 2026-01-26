package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * Type aliases for LSP primitive types.
 * These provide semantic meaning while remaining compatible with JSON.
 */
public typealias DocumentUri = String
public typealias URI = String
public typealias ProgressToken = String // Can be Int or String, we use String for simplicity

/**
 * Position in a text document expressed as zero-based line and character offset.
 * A position is between two characters like an 'insert' cursor in an editor.
 */
@Serializable
public data class Position(
    /**
     * Line position in a document (zero-based).
     */
    val line: Int,
    /**
     * Character offset on a line in a document (zero-based).
     * The meaning of this offset is determined by the negotiated
     * `PositionEncodingKind`.
     */
    val character: Int,
) : Comparable<Position> {
    override fun compareTo(other: Position): Int {
        val lineCompare = line.compareTo(other.line)
        return if (lineCompare != 0) lineCompare else character.compareTo(other.character)
    }

    public companion object {
        public val ZERO: Position = Position(0, 0)
    }
}

/**
 * A range in a text document expressed as (zero-based) start and end positions.
 * A range is comparable to a selection in an editor.
 */
@Serializable
public data class Range(
    /**
     * The range's start position (inclusive).
     */
    val start: Position,
    /**
     * The range's end position (exclusive).
     */
    val end: Position,
) {
    public fun contains(position: Position): Boolean = position >= start && position < end

    public fun isEmpty(): Boolean = start == end

    public companion object {
        public val ZERO: Range = Range(Position.ZERO, Position.ZERO)
    }
}

/**
 * Represents a location inside a resource, such as a line inside a text file.
 */
@Serializable
public data class Location(
    val uri: DocumentUri,
    val range: Range,
)

/**
 * Represents a link between a source and a target location.
 */
@Serializable
public data class LocationLink(
    /**
     * Span of the origin of this link.
     * Used as the underlined span for mouse interaction.
     */
    val originSelectionRange: Range? = null,
    /**
     * The target resource identifier of this link.
     */
    val targetUri: DocumentUri,
    /**
     * The full target range of this link.
     */
    val targetRange: Range,
    /**
     * The range that should be selected and revealed when this link is being followed.
     */
    val targetSelectionRange: Range,
)

/**
 * A literal to identify a text document in the client.
 */
@Serializable
public data class TextDocumentIdentifier(
    /**
     * The text document's URI.
     */
    val uri: DocumentUri,
)

/**
 * An identifier to denote a specific version of a text document.
 */
@Serializable
public data class VersionedTextDocumentIdentifier(
    /**
     * The text document's URI.
     */
    val uri: DocumentUri,
    /**
     * The version number of this document.
     */
    val version: Int,
)

/**
 * An identifier which optionally denotes a specific version of a text document.
 */
@Serializable
public data class OptionalVersionedTextDocumentIdentifier(
    /**
     * The text document's URI.
     */
    val uri: DocumentUri,
    /**
     * The version number of this document. If an optional versioned text document
     * identifier is sent from the server to the client and the file is not open
     * in the editor the server can send `null` to indicate that the version is
     * known and the content on disk is the master.
     */
    val version: Int? = null,
)

/**
 * An item to transfer a text document from the client to the server.
 */
@Serializable
public data class TextDocumentItem(
    /**
     * The text document's URI.
     */
    val uri: DocumentUri,
    /**
     * The text document's language identifier.
     */
    val languageId: String,
    /**
     * The version number of this document (increases after each change).
     */
    val version: Int,
    /**
     * The content of the opened text document.
     */
    val text: String,
)

/**
 * A parameter literal used in requests to pass a text document and a position inside that document.
 */
@Serializable
public data class TextDocumentPositionParams(
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
 * A text edit applicable to a text document.
 */
@Serializable
public data class TextEdit(
    /**
     * The range of the text document to be manipulated.
     */
    val range: Range,
    /**
     * The string to be inserted. For delete operations use an empty string.
     */
    val newText: String,
)

/**
 * Describes textual changes on a single text document.
 */
@Serializable
public data class TextDocumentEdit(
    /**
     * The text document to change.
     */
    val textDocument: OptionalVersionedTextDocumentIdentifier,
    /**
     * The edits to be applied.
     */
    val edits: List<TextEdit>,
)
