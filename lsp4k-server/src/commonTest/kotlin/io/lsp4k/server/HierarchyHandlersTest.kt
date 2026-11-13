package io.lsp4k.server

import io.lsp4k.protocol.CallHierarchyIncomingCall
import io.lsp4k.protocol.CallHierarchyIncomingCallsParams
import io.lsp4k.protocol.CallHierarchyItem
import io.lsp4k.protocol.CallHierarchyOutgoingCall
import io.lsp4k.protocol.CallHierarchyOutgoingCallsParams
import io.lsp4k.protocol.CallHierarchyPrepareParams
import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.Range
import io.lsp4k.protocol.SymbolKind
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.protocol.TypeHierarchyItem
import io.lsp4k.protocol.TypeHierarchyPrepareParams
import io.lsp4k.protocol.TypeHierarchySubtypesParams
import io.lsp4k.protocol.TypeHierarchySupertypesParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HierarchyHandlersTest {
    private val document = TextDocumentIdentifier("file:///tree.kt")
    private val position = Position(7, 3)
    private val range = Range(Position(7, 0), Position(9, 1))
    private val function = CallHierarchyItem("grow", SymbolKind.Function, uri = document.uri, range = range, selectionRange = range)
    private val type = TypeHierarchyItem("Tree", SymbolKind.Class, uri = document.uri, range = range, selectionRange = range)

    @Test
    fun `call hierarchy requests reach typed handlers`() =
        runTest {
            val received = mutableListOf<Any>()
            val incoming = listOf(CallHierarchyIncomingCall(function, listOf(range)))
            val outgoing = listOf(CallHierarchyOutgoingCall(function, listOf(range)))
            val peer =
                startPeer(backgroundScope) {
                    hierarchy {
                        prepareCallHierarchy {
                            received += it
                            listOf(function)
                        }
                        callHierarchyIncomingCalls {
                            received += it
                            incoming
                        }
                        callHierarchyOutgoingCalls {
                            received += it
                            outgoing
                        }
                    }
                }
            peer.initialize()

            val prepare = CallHierarchyPrepareParams(document, position)
            peer.expect(LspMethods.TEXT_DOCUMENT_PREPARE_CALL_HIERARCHY, prepare, listOf(function))
            val incomingParams = CallHierarchyIncomingCallsParams(function)
            peer.expect(LspMethods.CALL_HIERARCHY_INCOMING_CALLS, incomingParams, incoming)
            val outgoingParams = CallHierarchyOutgoingCallsParams(function)
            peer.expect(LspMethods.CALL_HIERARCHY_OUTGOING_CALLS, outgoingParams, outgoing)
            assertEquals(listOf<Any>(prepare, incomingParams, outgoingParams), received)
        }

    @Test
    fun `type hierarchy requests reach typed handlers`() =
        runTest {
            val received = mutableListOf<Any>()
            val peer =
                startPeer(backgroundScope) {
                    hierarchy {
                        prepareTypeHierarchy {
                            received += it
                            listOf(type)
                        }
                        typeHierarchySupertypes {
                            received += it
                            listOf(type)
                        }
                        typeHierarchySubtypes {
                            received += it
                            null
                        }
                    }
                }
            peer.initialize()

            val prepare = TypeHierarchyPrepareParams(document, position)
            peer.expect(LspMethods.TEXT_DOCUMENT_PREPARE_TYPE_HIERARCHY, prepare, listOf(type))
            val supertypes = TypeHierarchySupertypesParams(type)
            peer.expect(LspMethods.TYPE_HIERARCHY_SUPERTYPES, supertypes, listOf(type))
            val subtypes = TypeHierarchySubtypesParams(type)
            val noSubtypes = peer.call<TypeHierarchySubtypesParams, List<TypeHierarchyItem>>(LspMethods.TYPE_HIERARCHY_SUBTYPES, subtypes)
            assertEquals(null, noSubtypes)
            assertEquals(listOf<Any>(prepare, supertypes, subtypes), received)
        }
}
