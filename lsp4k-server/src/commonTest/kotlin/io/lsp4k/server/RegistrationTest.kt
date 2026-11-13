package io.lsp4k.server

import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.Registration
import io.lsp4k.protocol.RegistrationParams
import io.lsp4k.protocol.TextDocumentContentRegistrationOptions
import io.lsp4k.protocol.Unregistration
import io.lsp4k.protocol.UnregistrationParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegistrationTest {
    @Test
    fun `register returns a handle that unregisters exactly once`() =
        runTest {
            val peer = startPeer(backgroundScope) {}
            peer.initialize()
            val registration = peer.server.client.registration
            val options = TextDocumentContentRegistrationOptions(schemes = listOf("lsp4k"))
            val method = LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT

            val registered =
                json("""{"registrations":[{"id":"lsp4k-registration-1","method":"$method","registerOptions":{"schemes":["lsp4k"]}}]}""")
            val handle =
                peer.answer(this, LspMethods.CLIENT_REGISTER_CAPABILITY, registered) {
                    registration.register(method, options, TextDocumentContentRegistrationOptions.serializer())
                }
            assertEquals("lsp4k-registration-1", handle.id)
            assertEquals(method, handle.method)
            assertTrue(handle.isRegistered)

            val unregistered = json("""{"unregisterations":[{"id":"lsp4k-registration-1","method":"$method"}]}""")
            peer.answer(this, LspMethods.CLIENT_UNREGISTER_CAPABILITY, unregistered) { handle.unregister() }
            assertFalse(handle.isRegistered)
            handle.unregister()
            peer.sync()
            assertTrue(peer.requests.tryReceive().isFailure)
        }

    @Test
    fun `raw registration calls and explicit ids keep the spec shapes`() =
        runTest {
            val peer = startPeer(backgroundScope) {}
            peer.initialize()
            val registration = peer.server.client.registration
            val method = LspMethods.WORKSPACE_DID_CHANGE_WATCHED_FILES
            val watchers = json("""{"watchers":[{"globPattern":"**/*.kt"}]}""")

            val raw = RegistrationParams(listOf(Registration("watch", method, watchers)))
            peer.answer(this, LspMethods.CLIENT_REGISTER_CAPABILITY, wire(raw)) { registration.registerCapability(raw) }
            val removal = UnregistrationParams(listOf(Unregistration("watch", method)))
            val removalJson = json("""{"unregisterations":[{"id":"watch","method":"$method"}]}""")
            peer.answer(this, LspMethods.CLIENT_UNREGISTER_CAPABILITY, removalJson) { registration.unregisterCapability(removal) }

            val explicit = json("""{"registrations":[{"id":"mine","method":"$method"}]}""")
            val handle = peer.answer(this, LspMethods.CLIENT_REGISTER_CAPABILITY, explicit) { registration.register(method, id = "mine") }
            assertEquals("mine", handle.id)
        }
}
