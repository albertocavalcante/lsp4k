package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for Moniker type serialization: MonikerParams, Moniker, MonikerKind, UniquenessLevel.
 */
class MonikerSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== UniquenessLevel Tests ====================

    @Test
    fun `UniquenessLevel Document serialization`() {
        val level = UniquenessLevel.Document
        val encoded = json.encodeToString(level)
        encoded shouldBe "\"document\""
    }

    @Test
    fun `UniquenessLevel Project serialization`() {
        val level = UniquenessLevel.Project
        val encoded = json.encodeToString(level)
        encoded shouldBe "\"project\""
    }

    @Test
    fun `UniquenessLevel Group serialization`() {
        val level = UniquenessLevel.Group
        val encoded = json.encodeToString(level)
        encoded shouldBe "\"group\""
    }

    @Test
    fun `UniquenessLevel Scheme serialization`() {
        val level = UniquenessLevel.Scheme
        val encoded = json.encodeToString(level)
        encoded shouldBe "\"scheme\""
    }

    @Test
    fun `UniquenessLevel Global serialization`() {
        val level = UniquenessLevel.Global
        val encoded = json.encodeToString(level)
        encoded shouldBe "\"global\""
    }

    @Test
    fun `UniquenessLevel roundtrip for all values`() {
        UniquenessLevel.entries.forEach { level ->
            val encoded = json.encodeToString(level)
            val decoded = json.decodeFromString<UniquenessLevel>(encoded)
            decoded shouldBe level
        }
    }

    // ==================== MonikerKind Tests ====================

    @Test
    fun `MonikerKind Import serialization`() {
        val kind = MonikerKind.Import
        val encoded = json.encodeToString(kind)
        encoded shouldBe "\"import\""
    }

    @Test
    fun `MonikerKind Export serialization`() {
        val kind = MonikerKind.Export
        val encoded = json.encodeToString(kind)
        encoded shouldBe "\"export\""
    }

    @Test
    fun `MonikerKind Local serialization`() {
        val kind = MonikerKind.Local
        val encoded = json.encodeToString(kind)
        encoded shouldBe "\"local\""
    }

    @Test
    fun `MonikerKind roundtrip for all values`() {
        MonikerKind.entries.forEach { kind ->
            val encoded = json.encodeToString(kind)
            val decoded = json.decodeFromString<MonikerKind>(encoded)
            decoded shouldBe kind
        }
    }

    // ==================== MonikerParams Tests ====================

    @Test
    fun `MonikerParams serialization roundtrip`() {
        val original =
            MonikerParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                position = Position(10, 5),
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<MonikerParams>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `MonikerParams with position at start of document`() {
        val original =
            MonikerParams(
                textDocument = TextDocumentIdentifier(uri = "file:///start.kt"),
                position = Position(0, 0),
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<MonikerParams>(encoded)
        decoded shouldBe original
    }

    // ==================== Moniker Tests ====================

    @Test
    fun `Moniker with all required fields`() {
        val original =
            Moniker(
                scheme = "tsc",
                identifier = "com.example.MyClass#myMethod",
                unique = UniquenessLevel.Global,
            )
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "kind"
        val decoded = json.decodeFromString<Moniker>(encoded)
        decoded shouldBe original
        decoded.kind shouldBe null
    }

    @Test
    fun `Moniker with kind`() {
        val original =
            Moniker(
                scheme = "dotnet",
                identifier = "MyNamespace.MyClass",
                unique = UniquenessLevel.Scheme,
                kind = MonikerKind.Export,
            )
        val encoded = json.encodeToString(original)
        encoded shouldContain "\"export\""
        val decoded = json.decodeFromString<Moniker>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `Moniker with Import kind and Document uniqueness`() {
        val original =
            Moniker(
                scheme = "npm",
                identifier = "lodash.debounce",
                unique = UniquenessLevel.Document,
                kind = MonikerKind.Import,
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Moniker>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `Moniker with Local kind and Project uniqueness`() {
        val original =
            Moniker(
                scheme = "local",
                identifier = "localFunction",
                unique = UniquenessLevel.Project,
                kind = MonikerKind.Local,
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Moniker>(encoded)
        decoded.kind shouldBe MonikerKind.Local
        decoded.unique shouldBe UniquenessLevel.Project
    }

    @Test
    fun `Moniker with empty identifier`() {
        val original =
            Moniker(
                scheme = "test",
                identifier = "",
                unique = UniquenessLevel.Global,
            )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Moniker>(encoded)
        decoded.identifier shouldBe ""
    }
}
