package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * Represents a color in RGBA space.
 */
@Serializable
public data class Color(
    /**
     * The red component of this color in the range [0-1].
     */
    val red: Double,
    /**
     * The green component of this color in the range [0-1].
     */
    val green: Double,
    /**
     * The blue component of this color in the range [0-1].
     */
    val blue: Double,
    /**
     * The alpha component of this color in the range [0-1].
     */
    val alpha: Double,
)

/**
 * Parameters for textDocument/documentColor request.
 */
@Serializable
public data class DocumentColorParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
)

/**
 * Represents a color range from a document.
 */
@Serializable
public data class ColorInformation(
    /**
     * The range in the document where this color appears.
     */
    val range: Range,
    /**
     * The actual color value for this color range.
     */
    val color: Color,
)

/**
 * Parameters for textDocument/colorPresentation request.
 */
@Serializable
public data class ColorPresentationParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The color information to request presentations for.
     */
    val color: Color,
    /**
     * The range where the color would be inserted.
     */
    val range: Range,
)

/**
 * Represents a color presentation.
 */
@Serializable
public data class ColorPresentation(
    /**
     * The label of this color presentation.
     */
    val label: String,
    /**
     * An edit which is applied to a document when selecting this presentation
     * for the color.
     */
    val textEdit: TextEdit? = null,
    /**
     * An optional array of additional text edits that are applied when selecting
     * this color presentation.
     */
    val additionalTextEdits: List<TextEdit>? = null,
)
