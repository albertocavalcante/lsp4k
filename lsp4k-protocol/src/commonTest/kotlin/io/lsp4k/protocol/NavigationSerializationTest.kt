package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for Navigation-related types including:
 * - DeclarationParams
 * - DefinitionParams
 * - TypeDefinitionParams
 * - ImplementationParams
 * - ReferenceParams
 * - ReferenceContext
 */
class NavigationSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== DeclarationParams Tests ====================

    @Test
    fun `DeclarationParams serialization`() {
        val params =
            DeclarationParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                position = Position(line = 10, character = 5),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///test.kt\""
        encoded shouldContain "\"line\":10"
        encoded shouldContain "\"character\":5"
    }

    @Test
    fun `DeclarationParams round-trip`() {
        val params =
            DeclarationParams(
                textDocument = TextDocumentIdentifier(uri = "file:///src/main.kt"),
                position = Position(line = 100, character = 25),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DeclarationParams>(encoded)
        decoded shouldBe params
    }

    @Test
    fun `DeclarationParams at file start`() {
        val params =
            DeclarationParams(
                textDocument = TextDocumentIdentifier(uri = "file:///a.kt"),
                position = Position(line = 0, character = 0),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DeclarationParams>(encoded)
        decoded.position.line shouldBe 0
        decoded.position.character shouldBe 0
    }

    // ==================== DefinitionParams Tests ====================

    @Test
    fun `DefinitionParams serialization`() {
        val params =
            DefinitionParams(
                textDocument = TextDocumentIdentifier(uri = "file:///code.kt"),
                position = Position(line = 50, character = 12),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///code.kt\""
    }

    @Test
    fun `DefinitionParams round-trip`() {
        val params =
            DefinitionParams(
                textDocument = TextDocumentIdentifier(uri = "file:///lib/utils.kt"),
                position = Position(line = 200, character = 8),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DefinitionParams>(encoded)
        decoded shouldBe params
    }

    // ==================== TypeDefinitionParams Tests ====================

    @Test
    fun `TypeDefinitionParams serialization`() {
        val params =
            TypeDefinitionParams(
                textDocument = TextDocumentIdentifier(uri = "file:///types.kt"),
                position = Position(line = 30, character = 20),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///types.kt\""
        encoded shouldContain "\"line\":30"
    }

    @Test
    fun `TypeDefinitionParams round-trip`() {
        val params =
            TypeDefinitionParams(
                textDocument = TextDocumentIdentifier(uri = "file:///model/Entity.kt"),
                position = Position(line = 15, character = 4),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<TypeDefinitionParams>(encoded)
        decoded shouldBe params
    }

    // ==================== ImplementationParams Tests ====================

    @Test
    fun `ImplementationParams serialization`() {
        val params =
            ImplementationParams(
                textDocument = TextDocumentIdentifier(uri = "file:///interface.kt"),
                position = Position(line = 5, character = 10),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///interface.kt\""
    }

    @Test
    fun `ImplementationParams round-trip`() {
        val params =
            ImplementationParams(
                textDocument = TextDocumentIdentifier(uri = "file:///api/Service.kt"),
                position = Position(line = 22, character = 6),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<ImplementationParams>(encoded)
        decoded shouldBe params
    }

    // ==================== ReferenceContext Tests ====================

    @Test
    fun `ReferenceContext include declaration true`() {
        val context = ReferenceContext(includeDeclaration = true)
        val encoded = json.encodeToString(context)
        encoded shouldBe """{"includeDeclaration":true}"""
    }

    @Test
    fun `ReferenceContext include declaration false`() {
        val context = ReferenceContext(includeDeclaration = false)
        val encoded = json.encodeToString(context)
        encoded shouldBe """{"includeDeclaration":false}"""
    }

    @Test
    fun `ReferenceContext round-trip`() {
        val context = ReferenceContext(includeDeclaration = true)
        val encoded = json.encodeToString(context)
        val decoded = json.decodeFromString<ReferenceContext>(encoded)
        decoded shouldBe context
    }

    // ==================== ReferenceParams Tests ====================

    @Test
    fun `ReferenceParams with includeDeclaration true`() {
        val params =
            ReferenceParams(
                textDocument = TextDocumentIdentifier(uri = "file:///main.kt"),
                position = Position(line = 42, character = 15),
                context = ReferenceContext(includeDeclaration = true),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"includeDeclaration\":true"
    }

    @Test
    fun `ReferenceParams with includeDeclaration false`() {
        val params =
            ReferenceParams(
                textDocument = TextDocumentIdentifier(uri = "file:///main.kt"),
                position = Position(line = 42, character = 15),
                context = ReferenceContext(includeDeclaration = false),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"includeDeclaration\":false"
    }

    @Test
    fun `ReferenceParams round-trip`() {
        val params =
            ReferenceParams(
                textDocument = TextDocumentIdentifier(uri = "file:///src/utils/Helper.kt"),
                position = Position(line = 100, character = 0),
                context = ReferenceContext(includeDeclaration = true),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<ReferenceParams>(encoded)
        decoded shouldBe params
    }

    @Test
    fun `ReferenceParams complex URI`() {
        val params =
            ReferenceParams(
                textDocument = TextDocumentIdentifier(uri = "file:///C%3A/Users/test/project/src/main.kt"),
                position = Position(line = 10, character = 5),
                context = ReferenceContext(includeDeclaration = false),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<ReferenceParams>(encoded)
        decoded.textDocument.uri shouldBe "file:///C%3A/Users/test/project/src/main.kt"
    }
}
