package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Tests for DocumentSymbol type serialization including:
 * - DocumentSymbol, SymbolInformation
 * - SymbolKind, SymbolTag
 * - DocumentSymbolParams
 */
class DocumentSymbolSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== SymbolKind Tests ====================

    @Test
    fun `SymbolKind File serialization`() {
        val kind = SymbolKind.File
        val encoded = json.encodeToString(kind)
        encoded shouldBe "1"
    }

    @Test
    fun `SymbolKind Class serialization`() {
        val kind = SymbolKind.Class
        val encoded = json.encodeToString(kind)
        encoded shouldBe "5"
    }

    @Test
    fun `SymbolKind Method serialization`() {
        val kind = SymbolKind.Method
        val encoded = json.encodeToString(kind)
        encoded shouldBe "6"
    }

    @Test
    fun `SymbolKind Function serialization`() {
        val kind = SymbolKind.Function
        val encoded = json.encodeToString(kind)
        encoded shouldBe "12"
    }

    @Test
    fun `SymbolKind Variable serialization`() {
        val kind = SymbolKind.Variable
        val encoded = json.encodeToString(kind)
        encoded shouldBe "13"
    }

    @Test
    fun `SymbolKind TypeParameter serialization`() {
        val kind = SymbolKind.TypeParameter
        val encoded = json.encodeToString(kind)
        encoded shouldBe "26"
    }

    @Test
    fun `SymbolKind deserialization`() {
        json.decodeFromString<SymbolKind>("1") shouldBe SymbolKind.File
        json.decodeFromString<SymbolKind>("5") shouldBe SymbolKind.Class
        json.decodeFromString<SymbolKind>("6") shouldBe SymbolKind.Method
        json.decodeFromString<SymbolKind>("12") shouldBe SymbolKind.Function
        json.decodeFromString<SymbolKind>("26") shouldBe SymbolKind.TypeParameter
    }

    @Test
    fun `SymbolKind roundtrip for all values`() {
        SymbolKind.entries.forEach { kind ->
            val encoded = json.encodeToString(kind)
            val decoded = json.decodeFromString<SymbolKind>(encoded)
            decoded shouldBe kind
        }
    }

    @Test
    fun `SymbolKind fromValue throws on invalid value`() {
        assertFailsWith<IllegalArgumentException> {
            SymbolKind.fromValue(999)
        }.message shouldContain "Unknown SymbolKind"
    }

    @Test
    fun `SymbolKind fromValue throws on zero`() {
        assertFailsWith<IllegalArgumentException> {
            SymbolKind.fromValue(0)
        }
    }

    @Test
    fun `SymbolKind fromValue throws on negative`() {
        assertFailsWith<IllegalArgumentException> {
            SymbolKind.fromValue(-1)
        }
    }

    // ==================== SymbolTag Tests ====================

    @Test
    fun `SymbolTag Deprecated serialization`() {
        val tag = SymbolTag.Deprecated
        val encoded = json.encodeToString(tag)
        encoded shouldBe "1"
    }

    @Test
    fun `SymbolTag roundtrip for all values`() {
        SymbolTag.entries.forEach { tag ->
            val encoded = json.encodeToString(tag)
            val decoded = json.decodeFromString<SymbolTag>(encoded)
            decoded shouldBe tag
        }
    }

    @Test
    fun `SymbolTag fromValue throws on invalid value`() {
        assertFailsWith<IllegalArgumentException> {
            SymbolTag.fromValue(999)
        }.message shouldContain "Unknown SymbolTag"
    }

    // ==================== DocumentSymbolParams Tests ====================

    @Test
    fun `DocumentSymbolParams serialization`() {
        val params =
            DocumentSymbolParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
            )
        val encoded = json.encodeToString(params)
        encoded shouldBe """{"textDocument":{"uri":"file:///test.kt"}}"""
    }

    @Test
    fun `DocumentSymbolParams roundtrip`() {
        val params =
            DocumentSymbolParams(
                textDocument = TextDocumentIdentifier(uri = "file:///path/to/File.kt"),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<DocumentSymbolParams>(encoded)
        decoded shouldBe params
    }

    // ==================== DocumentSymbol Tests ====================

    @Test
    fun `DocumentSymbol minimal`() {
        val symbol =
            DocumentSymbol(
                name = "MyClass",
                kind = SymbolKind.Class,
                range = Range(Position(10, 0), Position(50, 1)),
                selectionRange = Range(Position(10, 6), Position(10, 13)),
            )
        val encoded = json.encodeToString(symbol)
        val decoded = json.decodeFromString<DocumentSymbol>(encoded)
        decoded.name shouldBe "MyClass"
        decoded.kind shouldBe SymbolKind.Class
        decoded.detail shouldBe null
        decoded.tags shouldBe null
        decoded.children shouldBe null
    }

    @Test
    fun `DocumentSymbol with detail`() {
        val symbol =
            DocumentSymbol(
                name = "calculate",
                detail = "(x: Int, y: Int): Int",
                kind = SymbolKind.Function,
                range = Range(Position(20, 0), Position(25, 1)),
                selectionRange = Range(Position(20, 4), Position(20, 13)),
            )
        val encoded = json.encodeToString(symbol)
        val decoded = json.decodeFromString<DocumentSymbol>(encoded)
        decoded.detail shouldBe "(x: Int, y: Int): Int"
    }

    @Test
    fun `DocumentSymbol with tags`() {
        val symbol =
            DocumentSymbol(
                name = "oldMethod",
                kind = SymbolKind.Method,
                tags = listOf(SymbolTag.Deprecated),
                range = Range(Position(30, 4), Position(35, 5)),
                selectionRange = Range(Position(30, 8), Position(30, 17)),
            )
        val encoded = json.encodeToString(symbol)
        val decoded = json.decodeFromString<DocumentSymbol>(encoded)
        decoded.tags shouldBe listOf(SymbolTag.Deprecated)
    }

    @Test
    fun `DocumentSymbol with deprecated flag`() {
        val symbol =
            DocumentSymbol(
                name = "legacyFunction",
                kind = SymbolKind.Function,
                deprecated = true,
                range = Range(Position(40, 0), Position(45, 1)),
                selectionRange = Range(Position(40, 4), Position(40, 18)),
            )
        val encoded = json.encodeToString(symbol)
        val decoded = json.decodeFromString<DocumentSymbol>(encoded)
        decoded.deprecated shouldBe true
    }

    @Test
    fun `DocumentSymbol with children`() {
        val symbol =
            DocumentSymbol(
                name = "MyClass",
                kind = SymbolKind.Class,
                range = Range(Position(10, 0), Position(100, 1)),
                selectionRange = Range(Position(10, 6), Position(10, 13)),
                children =
                    listOf(
                        DocumentSymbol(
                            name = "field1",
                            kind = SymbolKind.Field,
                            range = Range(Position(15, 4), Position(15, 20)),
                            selectionRange = Range(Position(15, 8), Position(15, 14)),
                        ),
                        DocumentSymbol(
                            name = "method1",
                            kind = SymbolKind.Method,
                            range = Range(Position(20, 4), Position(30, 5)),
                            selectionRange = Range(Position(20, 8), Position(20, 15)),
                        ),
                    ),
            )
        val encoded = json.encodeToString(symbol)
        val decoded = json.decodeFromString<DocumentSymbol>(encoded)
        decoded.children?.size shouldBe 2
        decoded.children?.first()?.name shouldBe "field1"
        decoded.children?.last()?.name shouldBe "method1"
    }

    @Test
    fun `DocumentSymbol with nested children`() {
        val symbol =
            DocumentSymbol(
                name = "OuterClass",
                kind = SymbolKind.Class,
                range = Range(Position(0, 0), Position(200, 1)),
                selectionRange = Range(Position(0, 6), Position(0, 16)),
                children =
                    listOf(
                        DocumentSymbol(
                            name = "InnerClass",
                            kind = SymbolKind.Class,
                            range = Range(Position(10, 4), Position(100, 5)),
                            selectionRange = Range(Position(10, 10), Position(10, 20)),
                            children =
                                listOf(
                                    DocumentSymbol(
                                        name = "innerMethod",
                                        kind = SymbolKind.Method,
                                        range = Range(Position(15, 8), Position(20, 9)),
                                        selectionRange = Range(Position(15, 12), Position(15, 23)),
                                    ),
                                ),
                        ),
                    ),
            )
        val encoded = json.encodeToString(symbol)
        val decoded = json.decodeFromString<DocumentSymbol>(encoded)
        decoded.children
            ?.first()
            ?.children
            ?.first()
            ?.name shouldBe "innerMethod"
    }

    @Test
    fun `DocumentSymbol comprehensive`() {
        val symbol =
            DocumentSymbol(
                name = "MyClass",
                detail = "class MyClass",
                kind = SymbolKind.Class,
                tags = listOf(SymbolTag.Deprecated),
                deprecated = true,
                range = Range(Position(10, 0), Position(100, 1)),
                selectionRange = Range(Position(10, 6), Position(10, 13)),
                children =
                    listOf(
                        DocumentSymbol(
                            name = "CONSTANT",
                            kind = SymbolKind.Constant,
                            range = Range(Position(12, 4), Position(12, 30)),
                            selectionRange = Range(Position(12, 8), Position(12, 16)),
                        ),
                        DocumentSymbol(
                            name = "property",
                            kind = SymbolKind.Property,
                            range = Range(Position(14, 4), Position(14, 25)),
                            selectionRange = Range(Position(14, 8), Position(14, 16)),
                        ),
                        DocumentSymbol(
                            name = "constructor",
                            kind = SymbolKind.Constructor,
                            range = Range(Position(16, 4), Position(20, 5)),
                            selectionRange = Range(Position(16, 4), Position(16, 15)),
                        ),
                    ),
            )
        val encoded = json.encodeToString(symbol)
        val decoded = json.decodeFromString<DocumentSymbol>(encoded)
        decoded shouldBe symbol
    }

    // ==================== SymbolInformation Tests ====================

    @Test
    fun `SymbolInformation minimal`() {
        val info =
            SymbolInformation(
                name = "myFunction",
                kind = SymbolKind.Function,
                location =
                    Location(
                        uri = "file:///test.kt",
                        range = Range(Position(10, 0), Position(20, 1)),
                    ),
            )
        val encoded = json.encodeToString(info)
        val decoded = json.decodeFromString<SymbolInformation>(encoded)
        decoded.name shouldBe "myFunction"
        decoded.kind shouldBe SymbolKind.Function
        decoded.containerName shouldBe null
    }

    @Test
    fun `SymbolInformation with containerName`() {
        val info =
            SymbolInformation(
                name = "method",
                kind = SymbolKind.Method,
                location =
                    Location(
                        uri = "file:///test.kt",
                        range = Range(Position(20, 4), Position(30, 5)),
                    ),
                containerName = "MyClass",
            )
        val encoded = json.encodeToString(info)
        val decoded = json.decodeFromString<SymbolInformation>(encoded)
        decoded.containerName shouldBe "MyClass"
    }

    @Test
    fun `SymbolInformation with tags`() {
        val info =
            SymbolInformation(
                name = "deprecatedMethod",
                kind = SymbolKind.Method,
                tags = listOf(SymbolTag.Deprecated),
                location =
                    Location(
                        uri = "file:///test.kt",
                        range = Range(Position(25, 4), Position(35, 5)),
                    ),
            )
        val encoded = json.encodeToString(info)
        val decoded = json.decodeFromString<SymbolInformation>(encoded)
        decoded.tags shouldBe listOf(SymbolTag.Deprecated)
    }

    @Test
    fun `SymbolInformation with deprecated flag`() {
        val info =
            SymbolInformation(
                name = "oldFunction",
                kind = SymbolKind.Function,
                deprecated = true,
                location =
                    Location(
                        uri = "file:///test.kt",
                        range = Range(Position(40, 0), Position(50, 1)),
                    ),
            )
        val encoded = json.encodeToString(info)
        val decoded = json.decodeFromString<SymbolInformation>(encoded)
        decoded.deprecated shouldBe true
    }

    @Test
    fun `SymbolInformation comprehensive`() {
        val info =
            SymbolInformation(
                name = "MyClass",
                kind = SymbolKind.Class,
                tags = listOf(SymbolTag.Deprecated),
                deprecated = true,
                location =
                    Location(
                        uri = "file:///project/src/MyClass.kt",
                        range = Range(Position(10, 0), Position(100, 1)),
                    ),
                containerName = "com.example",
            )
        val encoded = json.encodeToString(info)
        val decoded = json.decodeFromString<SymbolInformation>(encoded)
        decoded shouldBe info
    }

    // ==================== Various SymbolKind Tests ====================

    @Test
    fun `all SymbolKind values have correct integer values`() {
        SymbolKind.File.value shouldBe 1
        SymbolKind.Module.value shouldBe 2
        SymbolKind.Namespace.value shouldBe 3
        SymbolKind.Package.value shouldBe 4
        SymbolKind.Class.value shouldBe 5
        SymbolKind.Method.value shouldBe 6
        SymbolKind.Property.value shouldBe 7
        SymbolKind.Field.value shouldBe 8
        SymbolKind.Constructor.value shouldBe 9
        SymbolKind.Enum.value shouldBe 10
        SymbolKind.Interface.value shouldBe 11
        SymbolKind.Function.value shouldBe 12
        SymbolKind.Variable.value shouldBe 13
        SymbolKind.Constant.value shouldBe 14
        SymbolKind.String.value shouldBe 15
        SymbolKind.Number.value shouldBe 16
        SymbolKind.Boolean.value shouldBe 17
        SymbolKind.Array.value shouldBe 18
        SymbolKind.Object.value shouldBe 19
        SymbolKind.Key.value shouldBe 20
        SymbolKind.Null.value shouldBe 21
        SymbolKind.EnumMember.value shouldBe 22
        SymbolKind.Struct.value shouldBe 23
        SymbolKind.Event.value shouldBe 24
        SymbolKind.Operator.value shouldBe 25
        SymbolKind.TypeParameter.value shouldBe 26
    }

    @Test
    fun `DocumentSymbol with each SymbolKind`() {
        SymbolKind.entries.forEach { kind ->
            val symbol =
                DocumentSymbol(
                    name = "test_$kind",
                    kind = kind,
                    range = Range(Position(0, 0), Position(10, 1)),
                    selectionRange = Range(Position(0, 0), Position(0, 10)),
                )
            val encoded = json.encodeToString(symbol)
            val decoded = json.decodeFromString<DocumentSymbol>(encoded)
            decoded.kind shouldBe kind
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `DocumentSymbol with empty name`() {
        val symbol =
            DocumentSymbol(
                name = "",
                kind = SymbolKind.Variable,
                range = Range(Position(0, 0), Position(0, 5)),
                selectionRange = Range(Position(0, 0), Position(0, 5)),
            )
        val encoded = json.encodeToString(symbol)
        val decoded = json.decodeFromString<DocumentSymbol>(encoded)
        decoded.name shouldBe ""
    }

    @Test
    fun `DocumentSymbol with unicode name`() {
        val symbol =
            DocumentSymbol(
                name = "函数名 🎉",
                kind = SymbolKind.Function,
                range = Range(Position(0, 0), Position(10, 1)),
                selectionRange = Range(Position(0, 4), Position(0, 12)),
            )
        val encoded = json.encodeToString(symbol)
        val decoded = json.decodeFromString<DocumentSymbol>(encoded)
        decoded.name shouldBe "函数名 🎉"
    }

    @Test
    fun `DocumentSymbol with very long name`() {
        val longName = "a".repeat(1000)
        val symbol =
            DocumentSymbol(
                name = longName,
                kind = SymbolKind.Variable,
                range = Range(Position(0, 0), Position(0, 1000)),
                selectionRange = Range(Position(0, 4), Position(0, 1004)),
            )
        val encoded = json.encodeToString(symbol)
        val decoded = json.decodeFromString<DocumentSymbol>(encoded)
        decoded.name.length shouldBe 1000
    }

    @Test
    fun `DocumentSymbol with empty children list`() {
        val symbol =
            DocumentSymbol(
                name = "EmptyClass",
                kind = SymbolKind.Class,
                range = Range(Position(0, 0), Position(2, 1)),
                selectionRange = Range(Position(0, 6), Position(0, 16)),
                children = emptyList(),
            )
        val encoded = json.encodeToString(symbol)
        val decoded = json.decodeFromString<DocumentSymbol>(encoded)
        decoded.children shouldBe emptyList()
    }
}
