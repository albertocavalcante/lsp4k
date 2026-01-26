package io.lsp4k.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Describes the content type that a client supports in various result literals
 * like `Hover`, `ParameterInfo` or `CompletionItem`.
 */
@Serializable
public enum class MarkupKind {
    /**
     * Plain text is supported as a content format.
     */
    @SerialName("plaintext")
    PlainText,

    /**
     * Markdown is supported as a content format.
     */
    @SerialName("markdown")
    Markdown,
}

/**
 * A `MarkupContent` literal represents a string value which content is interpreted based on its kind flag.
 */
@Serializable
public data class MarkupContent(
    /**
     * The type of the Markup.
     */
    val kind: MarkupKind,
    /**
     * The content itself.
     */
    val value: String,
)

/**
 * The result of a hover request.
 */
@Serializable
public data class Hover(
    /**
     * The hover's content.
     */
    val contents: MarkupContent,
    /**
     * An optional range is a range inside a text document that is used to
     * visualize a hover, e.g. by changing the background color.
     */
    val range: Range? = null,
)

/**
 * Parameters for a hover request.
 */
@Serializable
public data class HoverParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The position inside the text document.
     */
    val position: Position,
)
