package io.lsp4k.protocol

import kotlinx.serialization.Serializable

/**
 * Parameters for textDocument/documentSymbol request.
 */
@Serializable
public data class DocumentSymbolParams(
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
 * Represents programming constructs like variables, classes, interfaces etc.
 * that appear in a document.
 */
@Serializable
public data class DocumentSymbol(
    /**
     * The name of this symbol.
     */
    val name: String,
    /**
     * More detail for this symbol, e.g. the signature of a function.
     */
    val detail: String? = null,
    /**
     * The kind of this symbol.
     */
    val kind: SymbolKind,
    /**
     * Tags for this symbol.
     */
    val tags: List<SymbolTag>? = null,
    /**
     * Indicates if this symbol is deprecated.
     */
    val deprecated: Boolean? = null,
    /**
     * The range enclosing this symbol not including leading/trailing whitespace
     * but everything else, e.g. comments and code.
     */
    val range: Range,
    /**
     * The range that should be selected and revealed when this symbol is being
     * picked, e.g. the name of a function.
     */
    val selectionRange: Range,
    /**
     * Children of this symbol, e.g. properties of a class.
     */
    val children: List<DocumentSymbol>? = null,
)

/**
 * Represents information about programming constructs like variables, classes,
 * interfaces etc.
 */
@Serializable
public data class SymbolInformation(
    /**
     * The name of this symbol.
     */
    val name: String,
    /**
     * The kind of this symbol.
     */
    val kind: SymbolKind,
    /**
     * Tags for this symbol.
     */
    val tags: List<SymbolTag>? = null,
    /**
     * Indicates if this symbol is deprecated.
     */
    val deprecated: Boolean? = null,
    /**
     * The location of this symbol.
     */
    val location: Location,
    /**
     * The name of the symbol containing this symbol.
     */
    val containerName: String? = null,
)

/**
 * A symbol kind.
 */
@Serializable(with = SymbolKindSerializer::class)
public enum class SymbolKind(
    public val value: Int,
) {
    File(1),
    Module(2),
    Namespace(3),
    Package(4),
    Class(5),
    Method(6),
    Property(7),
    Field(8),
    Constructor(9),
    Enum(10),
    Interface(11),
    Function(12),
    Variable(13),
    Constant(14),
    String(15),
    Number(16),
    Boolean(17),
    Array(18),
    Object(19),
    Key(20),
    Null(21),
    EnumMember(22),
    Struct(23),
    Event(24),
    Operator(25),
    TypeParameter(26),

    ;

    public companion object {
        public fun fromValue(value: Int): SymbolKind =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown SymbolKind: $value")
    }
}

/**
 * Serializer for SymbolKind that encodes/decodes as integer.
 */
public object SymbolKindSerializer : IntEnumSerializer<SymbolKind>(
    "SymbolKind",
    SymbolKind::fromValue,
    { it.value },
)

/**
 * Symbol tags are extra annotations that tweak the rendering of a symbol.
 */
@Serializable(with = SymbolTagSerializer::class)
public enum class SymbolTag(
    public val value: Int,
) {
    /**
     * Render a symbol as obsolete, usually using a strike-out.
     */
    Deprecated(1),

    ;

    public companion object {
        public fun fromValue(value: Int): SymbolTag =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown SymbolTag: $value")
    }
}

/**
 * Serializer for SymbolTag that encodes/decodes as integer.
 */
public object SymbolTagSerializer : IntEnumSerializer<SymbolTag>(
    "SymbolTag",
    SymbolTag::fromValue,
    { it.value },
)
