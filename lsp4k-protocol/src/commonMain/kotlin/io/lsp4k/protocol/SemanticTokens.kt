package io.lsp4k.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Semantic tokens options.
 */
@Serializable
public data class SemanticTokensOptions(
    /**
     * The legend used by the server.
     */
    val legend: SemanticTokensLegend,
    /**
     * Server supports providing semantic tokens for a specific range of a document.
     */
    val range: Boolean? = null,
    /**
     * Server supports providing semantic tokens for a full document.
     */
    val full: SemanticTokensFullOptions? = null,
)

/**
 * Semantic tokens full options.
 */
@Serializable
public data class SemanticTokensFullOptions(
    /**
     * The server supports deltas for full documents.
     */
    val delta: Boolean? = null,
)

/**
 * Semantic tokens legend.
 */
@Serializable
public data class SemanticTokensLegend(
    /**
     * The token types a server uses.
     */
    val tokenTypes: List<String>,
    /**
     * The token modifiers a server uses.
     */
    val tokenModifiers: List<String>,
)

/**
 * Parameters for textDocument/semanticTokens/full request.
 */
@Serializable
public data class SemanticTokensParams(
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
 * Parameters for textDocument/semanticTokens/full/delta request.
 */
@Serializable
public data class SemanticTokensDeltaParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The result id of a previous response. The result Id can either point to
     * a full response or a delta response depending on what was received last.
     */
    val previousResultId: String,
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
 * Parameters for textDocument/semanticTokens/range request.
 */
@Serializable
public data class SemanticTokensRangeParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The range the semantic tokens are requested for.
     */
    val range: Range,
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
 * Semantic tokens response.
 */
@Serializable
public data class SemanticTokens(
    /**
     * An optional result id. If provided and clients support delta updating
     * the client will include the result id in the next semantic token request.
     */
    val resultId: String? = null,
    /**
     * The actual tokens.
     */
    val data: List<Int>,
)

/**
 * Semantic tokens delta response.
 */
@Serializable
public data class SemanticTokensDelta(
    /**
     * The result id.
     */
    val resultId: String? = null,
    /**
     * The semantic token edits to transform a previous result into a new result.
     */
    val edits: List<SemanticTokensEdit>,
)

/**
 * Semantic tokens edit.
 */
@Serializable
public data class SemanticTokensEdit(
    /**
     * The start offset of the edit.
     */
    val start: Int,
    /**
     * The count of elements to remove.
     */
    val deleteCount: Int,
    /**
     * The elements to insert.
     */
    val data: List<Int>? = null,
)

/**
 * The token format for semantic tokens.
 */
@Serializable
public enum class TokenFormat {
    @SerialName("relative") Relative,
}

/**
 * Predefined semantic token types as defined by the LSP specification.
 */
public object SemanticTokenTypes {
    public const val Namespace: String = "namespace"
    public const val Type: String = "type"
    public const val Class: String = "class"
    public const val Enum: String = "enum"
    public const val Interface: String = "interface"
    public const val Struct: String = "struct"
    public const val TypeParameter: String = "typeParameter"
    public const val Parameter: String = "parameter"
    public const val Variable: String = "variable"
    public const val Property: String = "property"
    public const val EnumMember: String = "enumMember"
    public const val Event: String = "event"
    public const val Function: String = "function"
    public const val Method: String = "method"
    public const val Macro: String = "macro"
    public const val Keyword: String = "keyword"
    public const val Modifier: String = "modifier"
    public const val Comment: String = "comment"
    public const val String: String = "string"
    public const val Number: String = "number"
    public const val Regexp: String = "regexp"
    public const val Operator: String = "operator"
    public const val Decorator: String = "decorator"
    /**
     * @since 3.18.0
     */
    public const val Label: String = "label"
}

/**
 * Predefined semantic token modifiers as defined by the LSP specification.
 */
public object SemanticTokenModifiers {
    public const val Declaration: String = "declaration"
    public const val Definition: String = "definition"
    public const val Readonly: String = "readonly"
    public const val Static: String = "static"
    public const val Deprecated: String = "deprecated"
    public const val Abstract: String = "abstract"
    public const val Async: String = "async"
    public const val Modification: String = "modification"
    public const val Documentation: String = "documentation"
    public const val DefaultLibrary: String = "defaultLibrary"
}
