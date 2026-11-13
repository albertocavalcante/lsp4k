package io.lsp4k.protocol

import io.jsonrpc4k.core.JsonRpcCodec
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * G2.1: checks every structure, enumeration, type alias, request and notification of the vendored LSP 3.18 meta
 * model against the lsp4k serializers named by [ProtocolRegistry], under the production wire configuration.
 *
 * Known gaps live in the ratcheting allow-list resource: the test fails for a violation that is not listed and for
 * a listed key that no longer occurs, so fixing a gap requires deleting its entry.
 */
class MetaModelConformanceTest {
    @Test
    fun `vendored meta model matches the pinned checksum`() {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes(META_MODEL))
        assertEquals(PINNED_SHA256, digest.toHexString())
    }

    @Test
    fun `vendored meta model is version 3_18_0`() {
        assertEquals("3.18.0", MetaModel.parse(resource(META_MODEL)).version)
    }

    @Test
    fun `conformance Json has the jsonrpc4k wire configuration`() {
        assertEquals(JsonRpcCodec.defaultJson.configuration.toString(), conformanceJson.configuration.toString())
    }

    @Test
    fun `every meta model item conforms or is allow-listed`() {
        val violations = ConformanceChecker(MetaModel.parse(resource(META_MODEL)), ProtocolRegistry.registry).check()
        val allowList = ConformanceAllowList.parse(resource(ALLOW_LIST))
        val result = ratchet(violations.map { it.key }.toSet(), allowList.map { it.key }.toSet())
        val unexpected = violations.filter { it.key in result.unexpected }.joinToString("\n") { "  ${it.key}\n      ${it.detail}" }
        val stale = result.stale.joinToString("\n") { "  $it" }
        val problems =
            listOfNotNull(
                unexpected.takeIf { it.isNotEmpty() }?.let { "Fix these violations or allow-list them under their gap id:\n$it" },
                stale.takeIf { it.isNotEmpty() }?.let { "Delete these stale allow-list entries:\n$it" },
            )
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    fun `allow-list is well formed`() {
        val entries = ConformanceAllowList.parse(resource(ALLOW_LIST))
        assertTrue(entries.all { it.reason.isNotBlank() })
    }

    private fun bytes(path: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream(path)) { "Missing test resource $path" }.use { it.readBytes() }

    private fun resource(path: String): String = bytes(path).decodeToString()

    private companion object {
        const val META_MODEL = "/lsp/3.18/metaModel.json"
        const val ALLOW_LIST = "/lsp/3.18/conformance-allowlist.txt"
        const val PINNED_SHA256 = "ce9e95e3c2a1ef333f5b7d64be7c8d31138af736a3d128cd5d99165e9dd26a5d"
    }
}
