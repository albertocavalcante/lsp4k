package io.lsp4k.client

import io.lsp4k.protocol.CallHierarchyIncomingCallsParams
import io.lsp4k.protocol.CallHierarchyOutgoingCallsParams
import io.lsp4k.protocol.CallHierarchyPrepareParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.TypeHierarchyPrepareParams
import io.lsp4k.protocol.TypeHierarchySubtypesParams
import io.lsp4k.protocol.TypeHierarchySupertypesParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal val hierarchyCases: List<CallCase> =
    listOf(
        requestCase<CallHierarchyPrepareParams>(
            LspMethods.TEXT_DOCUMENT_PREPARE_CALL_HIERARCHY,
            """{$DOC,$POS}""",
        ) { hierarchy.prepareCallHierarchy(it) },
        requestCase<CallHierarchyIncomingCallsParams>(
            LspMethods.CALL_HIERARCHY_INCOMING_CALLS,
            """{"item":$CHI}""",
        ) { hierarchy.callHierarchyIncomingCalls(it) },
        requestCase<CallHierarchyOutgoingCallsParams>(
            LspMethods.CALL_HIERARCHY_OUTGOING_CALLS,
            """{"item":$CHI}""",
        ) { hierarchy.callHierarchyOutgoingCalls(it) },
        requestCase<TypeHierarchyPrepareParams>(
            LspMethods.TEXT_DOCUMENT_PREPARE_TYPE_HIERARCHY,
            """{$DOC,$POS}""",
        ) { hierarchy.prepareTypeHierarchy(it) },
        requestCase<TypeHierarchySupertypesParams>(
            LspMethods.TYPE_HIERARCHY_SUPERTYPES,
            """{"item":$CHI}""",
        ) { hierarchy.typeHierarchySupertypes(it) },
        requestCase<TypeHierarchySubtypesParams>(
            LspMethods.TYPE_HIERARCHY_SUBTYPES,
            """{"item":$CHI}""",
        ) { hierarchy.typeHierarchySubtypes(it) },
    )

class HierarchyCallsTest {
    @Test
    fun `every call reaches the server with its method and params`() =
        runTest {
            runCases(backgroundScope, hierarchyCases)
        }
}
