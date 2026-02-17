package io.lsp4k.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents an item in the type hierarchy.
 */
@Serializable
public data class TypeHierarchyItem(
    /**
     * The name of this item.
     */
    val name: String,
    /**
     * The kind of this item.
     */
    val kind: SymbolKind,
    /**
     * Tags for this item.
     */
    val tags: List<SymbolTag>? = null,
    /**
     * More detail for this item, e.g. the signature of a function.
     */
    val detail: String? = null,
    /**
     * The resource identifier of this item.
     */
    val uri: DocumentUri,
    /**
     * The range enclosing this symbol not including leading/trailing whitespace
     * but everything else, e.g. comments and code.
     */
    val range: Range,
    /**
     * The range that should be selected and revealed when this symbol is being
     * picked, e.g. the name of a class.
     */
    val selectionRange: Range,
    /**
     * A data entry field that is preserved between a type hierarchy prepare and
     * supertypes or subtypes requests.
     */
    val data: JsonElement? = null,
)

/**
 * Parameters for textDocument/prepareTypeHierarchy request.
 */
@Serializable
public data class TypeHierarchyPrepareParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The position inside the text document.
     */
    val position: Position,
    /**
     * An optional token that a server can use to report work done progress.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
)

/**
 * Parameters for typeHierarchy/supertypes request.
 */
@Serializable
public data class TypeHierarchySupertypesParams(
    /**
     * The item for which to return supertypes.
     */
    val item: TypeHierarchyItem,
)

/**
 * Parameters for typeHierarchy/subtypes request.
 */
@Serializable
public data class TypeHierarchySubtypesParams(
    /**
     * The item for which to return subtypes.
     */
    val item: TypeHierarchyItem,
)
