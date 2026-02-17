package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test

/**
 * Tests for Registration type serialization: Registration, RegistrationParams,
 * Unregistration, UnregistrationParams, ConfigurationItem, ConfigurationParams.
 */
class RegistrationSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== Registration Tests ====================

    @Test
    fun `Registration with required fields only`() {
        val original = Registration(
            id = "reg-001",
            method = "textDocument/completion",
        )
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "registerOptions"
        val decoded = json.decodeFromString<Registration>(encoded)
        decoded shouldBe original
        decoded.registerOptions shouldBe null
    }

    @Test
    fun `Registration with registerOptions`() {
        val options = buildJsonObject {
            put("triggerCharacters", Json.encodeToJsonElement(ListSerializer(String.serializer()), listOf(".", ":")))
        }
        val original = Registration(
            id = "reg-002",
            method = "textDocument/completion",
            registerOptions = options,
        )
        val encoded = json.encodeToString(original)
        encoded shouldContain "registerOptions"
        val decoded = json.decodeFromString<Registration>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `Registration with empty registerOptions object`() {
        val original = Registration(
            id = "reg-003",
            method = "textDocument/hover",
            registerOptions = JsonObject(emptyMap()),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Registration>(encoded)
        decoded shouldBe original
    }

    // ==================== RegistrationParams Tests ====================

    @Test
    fun `RegistrationParams with single registration`() {
        val original = RegistrationParams(
            registrations = listOf(
                Registration(id = "1", method = "textDocument/didOpen"),
            ),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<RegistrationParams>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `RegistrationParams with multiple registrations`() {
        val original = RegistrationParams(
            registrations = listOf(
                Registration(id = "1", method = "textDocument/didOpen"),
                Registration(id = "2", method = "textDocument/completion"),
                Registration(id = "3", method = "textDocument/hover"),
            ),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<RegistrationParams>(encoded)
        decoded shouldBe original
        decoded.registrations.size shouldBe 3
    }

    @Test
    fun `RegistrationParams with empty list`() {
        val original = RegistrationParams(registrations = emptyList())
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<RegistrationParams>(encoded)
        decoded.registrations shouldBe emptyList()
    }

    // ==================== Unregistration Tests ====================

    @Test
    fun `Unregistration serialization roundtrip`() {
        val original = Unregistration(
            id = "unreg-001",
            method = "textDocument/completion",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Unregistration>(encoded)
        decoded shouldBe original
    }

    // ==================== UnregistrationParams Tests ====================

    @Test
    fun `UnregistrationParams serialization roundtrip`() {
        val original = UnregistrationParams(
            unregistrations = listOf(
                Unregistration(id = "1", method = "textDocument/didOpen"),
            ),
        )
        val encoded = json.encodeToString(original)
        // The LSP spec has a typo: the JSON field name is "unregisterations"
        encoded shouldContain "unregisterations"
        val decoded = json.decodeFromString<UnregistrationParams>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `UnregistrationParams with multiple unregistrations`() {
        val original = UnregistrationParams(
            unregistrations = listOf(
                Unregistration(id = "1", method = "textDocument/didOpen"),
                Unregistration(id = "2", method = "textDocument/completion"),
            ),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<UnregistrationParams>(encoded)
        decoded shouldBe original
        decoded.unregistrations.size shouldBe 2
    }

    @Test
    fun `UnregistrationParams with empty list`() {
        val original = UnregistrationParams(unregistrations = emptyList())
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<UnregistrationParams>(encoded)
        decoded.unregistrations shouldBe emptyList()
    }

    // ==================== ConfigurationItem Tests ====================

    @Test
    fun `ConfigurationItem with all fields`() {
        val original = ConfigurationItem(
            scopeUri = "file:///workspace",
            section = "editor.fontSize",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<ConfigurationItem>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `ConfigurationItem with scopeUri only`() {
        val original = ConfigurationItem(scopeUri = "file:///project")
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "section"
        val decoded = json.decodeFromString<ConfigurationItem>(encoded)
        decoded.scopeUri shouldBe "file:///project"
        decoded.section shouldBe null
    }

    @Test
    fun `ConfigurationItem with section only`() {
        val original = ConfigurationItem(section = "myExtension.setting")
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "scopeUri"
        val decoded = json.decodeFromString<ConfigurationItem>(encoded)
        decoded.section shouldBe "myExtension.setting"
        decoded.scopeUri shouldBe null
    }

    @Test
    fun `ConfigurationItem with no fields`() {
        val original = ConfigurationItem()
        val encoded = json.encodeToString(original)
        encoded shouldBe "{}"
        val decoded = json.decodeFromString<ConfigurationItem>(encoded)
        decoded.scopeUri shouldBe null
        decoded.section shouldBe null
    }

    // ==================== ConfigurationParams Tests ====================

    @Test
    fun `ConfigurationParams with single item`() {
        val original = ConfigurationParams(
            items = listOf(
                ConfigurationItem(section = "editor"),
            ),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<ConfigurationParams>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `ConfigurationParams with multiple items`() {
        val original = ConfigurationParams(
            items = listOf(
                ConfigurationItem(scopeUri = "file:///a", section = "editor"),
                ConfigurationItem(scopeUri = "file:///b", section = "terminal"),
                ConfigurationItem(section = "myPlugin.config"),
            ),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<ConfigurationParams>(encoded)
        decoded shouldBe original
        decoded.items.size shouldBe 3
    }

    @Test
    fun `ConfigurationParams with empty list`() {
        val original = ConfigurationParams(items = emptyList())
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<ConfigurationParams>(encoded)
        decoded.items shouldBe emptyList()
    }
}
