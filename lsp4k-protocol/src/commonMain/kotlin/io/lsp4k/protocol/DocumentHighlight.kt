package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * Parameters for textDocument/documentHighlight request.
 */
@Serializable
public data class DocumentHighlightParams(
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
 * A document highlight kind.
 */
@Serializable(with = DocumentHighlightKindSerializer::class)
public enum class DocumentHighlightKind(
    public val value: Int,
) {
    /**
     * A textual occurrence.
     */
    Text(1),

    /**
     * Read-access of a symbol, like reading a variable.
     */
    Read(2),

    /**
     * Write-access of a symbol, like writing to a variable.
     */
    Write(3),

    ;

    public companion object {
        public fun fromValue(value: Int): DocumentHighlightKind =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown DocumentHighlightKind: $value")
    }
}

/**
 * Serializer for DocumentHighlightKind that encodes/decodes as integer.
 */
public object DocumentHighlightKindSerializer : IntEnumSerializer<DocumentHighlightKind>(
    "DocumentHighlightKind",
    DocumentHighlightKind::fromValue,
    { it.value },
)

/**
 * A document highlight is a range inside a text document which deserves
 * special attention.
 */
@Serializable
public data class DocumentHighlight(
    /**
     * The range this highlight applies to.
     */
    val range: Range,
    /**
     * The highlight kind, default is DocumentHighlightKind.Text.
     */
    val kind: DocumentHighlightKind? = null,
)
