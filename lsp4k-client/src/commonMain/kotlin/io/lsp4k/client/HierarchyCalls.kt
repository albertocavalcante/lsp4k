package io.lsp4k.client

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

/**
 * Call hierarchy and type hierarchy requests.
 */
public class HierarchyCalls internal constructor(
    private val caller: ServerCaller,
) {
    /**
     * Request to prepare a call hierarchy at a given text document position.
     */
    public suspend fun prepareCallHierarchy(params: CallHierarchyPrepareParams): List<CallHierarchyItem>? =
        caller.request(LspMethods.TEXT_DOCUMENT_PREPARE_CALL_HIERARCHY, params)

    /**
     * Request incoming calls for a given call hierarchy item.
     */
    public suspend fun callHierarchyIncomingCalls(params: CallHierarchyIncomingCallsParams): List<CallHierarchyIncomingCall>? =
        caller.request(LspMethods.CALL_HIERARCHY_INCOMING_CALLS, params)

    /**
     * Request outgoing calls for a given call hierarchy item.
     */
    public suspend fun callHierarchyOutgoingCalls(params: CallHierarchyOutgoingCallsParams): List<CallHierarchyOutgoingCall>? =
        caller.request(LspMethods.CALL_HIERARCHY_OUTGOING_CALLS, params)

    /**
     * Request to prepare a type hierarchy at a given text document position.
     */
    public suspend fun prepareTypeHierarchy(params: TypeHierarchyPrepareParams): List<TypeHierarchyItem>? =
        caller.request(LspMethods.TEXT_DOCUMENT_PREPARE_TYPE_HIERARCHY, params)

    /**
     * Request supertypes for a given type hierarchy item.
     */
    public suspend fun typeHierarchySupertypes(params: TypeHierarchySupertypesParams): List<TypeHierarchyItem>? =
        caller.request(LspMethods.TYPE_HIERARCHY_SUPERTYPES, params)

    /**
     * Request subtypes for a given type hierarchy item.
     */
    public suspend fun typeHierarchySubtypes(params: TypeHierarchySubtypesParams): List<TypeHierarchyItem>? =
        caller.request(LspMethods.TYPE_HIERARCHY_SUBTYPES, params)
}
