package io.lsp4k.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents an item in the call hierarchy.
 */
@Serializable
public data class CallHierarchyItem(
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
     * picked, e.g. the name of a function.
     */
    val selectionRange: Range,
    /**
     * A data entry field that is preserved between a call hierarchy prepare and
     * incoming calls or outgoing calls requests.
     */
    val data: JsonElement? = null,
)

/**
 * Parameters for textDocument/prepareCallHierarchy request.
 */
@Serializable
public data class CallHierarchyPrepareParams(
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
 * Parameters for callHierarchy/incomingCalls request.
 */
@Serializable
public data class CallHierarchyIncomingCallsParams(
    /**
     * The item to show incoming calls for.
     */
    val item: CallHierarchyItem,
)

/**
 * Represents an incoming call, e.g. a caller of a method or constructor.
 */
@Serializable
public data class CallHierarchyIncomingCall(
    /**
     * The item that makes the call.
     */
    val from: CallHierarchyItem,
    /**
     * The ranges at which the calls appear. This is relative to the caller
     * denoted by `from`.
     */
    val fromRanges: List<Range>,
)

/**
 * Parameters for callHierarchy/outgoingCalls request.
 */
@Serializable
public data class CallHierarchyOutgoingCallsParams(
    /**
     * The item to show outgoing calls for.
     */
    val item: CallHierarchyItem,
)

/**
 * Represents an outgoing call, e.g. calling a getter from a method or a method from a constructor.
 */
@Serializable
public data class CallHierarchyOutgoingCall(
    /**
     * The item that is called.
     */
    val to: CallHierarchyItem,
    /**
     * The range at which this item is called. This is the range relative to
     * the caller, e.g. the item passed to `callHierarchy/outgoingCalls` request.
     */
    val fromRanges: List<Range>,
)
