package io.lsp4k.server

import io.lsp4k.protocol.CallHierarchyIncomingCall
import io.lsp4k.protocol.CallHierarchyIncomingCallsParams
import io.lsp4k.protocol.CallHierarchyItem
import io.lsp4k.protocol.CallHierarchyOutgoingCall
import io.lsp4k.protocol.CallHierarchyOutgoingCallsParams
import io.lsp4k.protocol.CallHierarchyPrepareParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.TypeHierarchyItem
import io.lsp4k.protocol.TypeHierarchyPrepareParams
import io.lsp4k.protocol.TypeHierarchySubtypesParams
import io.lsp4k.protocol.TypeHierarchySupertypesParams
import kotlinx.serialization.serializer

/**
 * Handlers for call hierarchy and type hierarchy requests.
 */
@LspServerDsl
public class HierarchyHandlersBuilder internal constructor(
    registry: HandlerRegistry,
) : HandlersBuilder(registry) {
    /** Handles `textDocument/prepareCallHierarchy` requests. */
    public fun prepareCallHierarchy(handler: suspend RequestContext.(CallHierarchyPrepareParams) -> List<CallHierarchyItem>?) {
        request(LspMethods.TEXT_DOCUMENT_PREPARE_CALL_HIERARCHY, handler)
    }

    /** Handles `callHierarchy/incomingCalls` requests. */
    public fun callHierarchyIncomingCalls(
        handler: suspend PartialResultContext<List<CallHierarchyIncomingCall>>.(
            CallHierarchyIncomingCallsParams,
        ) -> List<CallHierarchyIncomingCall>?,
    ) {
        partialRequest(LspMethods.CALL_HIERARCHY_INCOMING_CALLS, serializer<List<CallHierarchyIncomingCall>>(), handler)
    }

    /** Handles `callHierarchy/outgoingCalls` requests. */
    public fun callHierarchyOutgoingCalls(
        handler: suspend PartialResultContext<List<CallHierarchyOutgoingCall>>.(
            CallHierarchyOutgoingCallsParams,
        ) -> List<CallHierarchyOutgoingCall>?,
    ) {
        partialRequest(LspMethods.CALL_HIERARCHY_OUTGOING_CALLS, serializer<List<CallHierarchyOutgoingCall>>(), handler)
    }

    /** Handles `textDocument/prepareTypeHierarchy` requests. */
    public fun prepareTypeHierarchy(handler: suspend RequestContext.(TypeHierarchyPrepareParams) -> List<TypeHierarchyItem>?) {
        request(LspMethods.TEXT_DOCUMENT_PREPARE_TYPE_HIERARCHY, handler)
    }

    /** Handles `typeHierarchy/supertypes` requests. */
    public fun typeHierarchySupertypes(
        handler: suspend PartialResultContext<List<TypeHierarchyItem>>.(TypeHierarchySupertypesParams) -> List<TypeHierarchyItem>?,
    ) {
        partialRequest(LspMethods.TYPE_HIERARCHY_SUPERTYPES, serializer<List<TypeHierarchyItem>>(), handler)
    }

    /** Handles `typeHierarchy/subtypes` requests. */
    public fun typeHierarchySubtypes(
        handler: suspend PartialResultContext<List<TypeHierarchyItem>>.(TypeHierarchySubtypesParams) -> List<TypeHierarchyItem>?,
    ) {
        partialRequest(LspMethods.TYPE_HIERARCHY_SUBTYPES, serializer<List<TypeHierarchyItem>>(), handler)
    }
}
