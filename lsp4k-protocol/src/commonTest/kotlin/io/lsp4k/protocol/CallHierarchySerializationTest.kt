package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/**
 * Tests for CallHierarchy-related types including:
 * - CallHierarchyItem
 * - CallHierarchyPrepareParams
 * - CallHierarchyIncomingCallsParams
 * - CallHierarchyIncomingCall
 * - CallHierarchyOutgoingCallsParams
 * - CallHierarchyOutgoingCall
 */
class CallHierarchySerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== CallHierarchyItem Tests ====================

    @Test
    fun `CallHierarchyItem minimal`() {
        val item = CallHierarchyItem(
            name = "myFunction",
            kind = SymbolKind.Function,
            uri = "file:///test.kt",
            range = Range(Position(10, 0), Position(20, 0)),
            selectionRange = Range(Position(10, 4), Position(10, 14)),
        )
        val encoded = json.encodeToString(item)
        encoded shouldContain "\"name\":\"myFunction\""
        encoded shouldContain "\"kind\":12"
    }

    @Test
    fun `CallHierarchyItem with all fields`() {
        val item = CallHierarchyItem(
            name = "MyClass",
            kind = SymbolKind.Class,
            tags = listOf(SymbolTag.Deprecated),
            detail = "class MyClass : BaseClass",
            uri = "file:///src/MyClass.kt",
            range = Range(Position(5, 0), Position(100, 0)),
            selectionRange = Range(Position(5, 6), Position(5, 13)),
            data = JsonPrimitive("custom-data"),
        )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CallHierarchyItem>(encoded)
        decoded.name shouldBe "MyClass"
        decoded.kind shouldBe SymbolKind.Class
        decoded.tags shouldBe listOf(SymbolTag.Deprecated)
        decoded.detail shouldBe "class MyClass : BaseClass"
        decoded.data shouldBe JsonPrimitive("custom-data")
    }

    @Test
    fun `CallHierarchyItem round-trip`() {
        val item = CallHierarchyItem(
            name = "process",
            kind = SymbolKind.Method,
            uri = "file:///Handler.kt",
            range = Range(Position(50, 4), Position(80, 4)),
            selectionRange = Range(Position(50, 8), Position(50, 15)),
        )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<CallHierarchyItem>(encoded)
        decoded shouldBe item
    }

    @Test
    fun `CallHierarchyItem with different symbol kinds`() {
        val kinds = listOf(
            SymbolKind.Function to 12,
            SymbolKind.Method to 6,
            SymbolKind.Constructor to 9,
            SymbolKind.Class to 5,
        )
        for ((kind, value) in kinds) {
            val item = CallHierarchyItem(
                name = "test",
                kind = kind,
                uri = "file:///test.kt",
                range = Range(Position(0, 0), Position(10, 0)),
                selectionRange = Range(Position(0, 0), Position(0, 4)),
            )
            val encoded = json.encodeToString(item)
            encoded shouldContain "\"kind\":$value"
        }
    }

    // ==================== CallHierarchyPrepareParams Tests ====================

    @Test
    fun `CallHierarchyPrepareParams serialization`() {
        val params = CallHierarchyPrepareParams(
            textDocument = TextDocumentIdentifier(uri = "file:///code.kt"),
            position = Position(line = 25, character = 10),
        )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///code.kt\""
        encoded shouldContain "\"line\":25"
    }

    @Test
    fun `CallHierarchyPrepareParams round-trip`() {
        val params = CallHierarchyPrepareParams(
            textDocument = TextDocumentIdentifier(uri = "file:///src/main.kt"),
            position = Position(line = 100, character = 5),
        )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<CallHierarchyPrepareParams>(encoded)
        decoded shouldBe params
    }

    // ==================== CallHierarchyIncomingCallsParams Tests ====================

    @Test
    fun `CallHierarchyIncomingCallsParams serialization`() {
        val item = CallHierarchyItem(
            name = "targetFunction",
            kind = SymbolKind.Function,
            uri = "file:///target.kt",
            range = Range(Position(10, 0), Position(20, 0)),
            selectionRange = Range(Position(10, 4), Position(10, 18)),
        )
        val params = CallHierarchyIncomingCallsParams(item = item)
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"item\""
        encoded shouldContain "\"targetFunction\""
    }

    @Test
    fun `CallHierarchyIncomingCallsParams round-trip`() {
        val item = CallHierarchyItem(
            name = "callee",
            kind = SymbolKind.Method,
            uri = "file:///service.kt",
            range = Range(Position(50, 0), Position(60, 0)),
            selectionRange = Range(Position(50, 8), Position(50, 14)),
        )
        val params = CallHierarchyIncomingCallsParams(item = item)
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<CallHierarchyIncomingCallsParams>(encoded)
        decoded shouldBe params
    }

    // ==================== CallHierarchyIncomingCall Tests ====================

    @Test
    fun `CallHierarchyIncomingCall serialization`() {
        val caller = CallHierarchyItem(
            name = "callerFunction",
            kind = SymbolKind.Function,
            uri = "file:///caller.kt",
            range = Range(Position(100, 0), Position(150, 0)),
            selectionRange = Range(Position(100, 4), Position(100, 18)),
        )
        val call = CallHierarchyIncomingCall(
            from = caller,
            fromRanges = listOf(
                Range(Position(110, 8), Position(110, 25)),
                Range(Position(130, 12), Position(130, 29)),
            ),
        )
        val encoded = json.encodeToString(call)
        encoded shouldContain "\"from\""
        encoded shouldContain "\"fromRanges\""
    }

    @Test
    fun `CallHierarchyIncomingCall round-trip`() {
        val caller = CallHierarchyItem(
            name = "main",
            kind = SymbolKind.Function,
            uri = "file:///Main.kt",
            range = Range(Position(0, 0), Position(10, 0)),
            selectionRange = Range(Position(0, 4), Position(0, 8)),
        )
        val call = CallHierarchyIncomingCall(
            from = caller,
            fromRanges = listOf(Range(Position(5, 4), Position(5, 20))),
        )
        val encoded = json.encodeToString(call)
        val decoded = json.decodeFromString<CallHierarchyIncomingCall>(encoded)
        decoded shouldBe call
    }

    @Test
    fun `CallHierarchyIncomingCall with multiple ranges`() {
        val caller = CallHierarchyItem(
            name = "test",
            kind = SymbolKind.Function,
            uri = "file:///test.kt",
            range = Range(Position(0, 0), Position(50, 0)),
            selectionRange = Range(Position(0, 4), Position(0, 8)),
        )
        val call = CallHierarchyIncomingCall(
            from = caller,
            fromRanges = listOf(
                Range(Position(10, 0), Position(10, 15)),
                Range(Position(20, 0), Position(20, 15)),
                Range(Position(30, 0), Position(30, 15)),
            ),
        )
        val encoded = json.encodeToString(call)
        val decoded = json.decodeFromString<CallHierarchyIncomingCall>(encoded)
        decoded.fromRanges.size shouldBe 3
    }

    // ==================== CallHierarchyOutgoingCallsParams Tests ====================

    @Test
    fun `CallHierarchyOutgoingCallsParams serialization`() {
        val item = CallHierarchyItem(
            name = "sourceFunction",
            kind = SymbolKind.Function,
            uri = "file:///source.kt",
            range = Range(Position(10, 0), Position(30, 0)),
            selectionRange = Range(Position(10, 4), Position(10, 18)),
        )
        val params = CallHierarchyOutgoingCallsParams(item = item)
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"sourceFunction\""
    }

    @Test
    fun `CallHierarchyOutgoingCallsParams round-trip`() {
        val item = CallHierarchyItem(
            name = "caller",
            kind = SymbolKind.Method,
            uri = "file:///handler.kt",
            range = Range(Position(20, 0), Position(40, 0)),
            selectionRange = Range(Position(20, 8), Position(20, 14)),
        )
        val params = CallHierarchyOutgoingCallsParams(item = item)
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<CallHierarchyOutgoingCallsParams>(encoded)
        decoded shouldBe params
    }

    // ==================== CallHierarchyOutgoingCall Tests ====================

    @Test
    fun `CallHierarchyOutgoingCall serialization`() {
        val callee = CallHierarchyItem(
            name = "calleeFunction",
            kind = SymbolKind.Function,
            uri = "file:///callee.kt",
            range = Range(Position(200, 0), Position(250, 0)),
            selectionRange = Range(Position(200, 4), Position(200, 18)),
        )
        val call = CallHierarchyOutgoingCall(
            to = callee,
            fromRanges = listOf(Range(Position(15, 8), Position(15, 30))),
        )
        val encoded = json.encodeToString(call)
        encoded shouldContain "\"to\""
        encoded shouldContain "\"fromRanges\""
    }

    @Test
    fun `CallHierarchyOutgoingCall round-trip`() {
        val callee = CallHierarchyItem(
            name = "helper",
            kind = SymbolKind.Function,
            uri = "file:///utils.kt",
            range = Range(Position(5, 0), Position(15, 0)),
            selectionRange = Range(Position(5, 4), Position(5, 10)),
        )
        val call = CallHierarchyOutgoingCall(
            to = callee,
            fromRanges = listOf(
                Range(Position(25, 4), Position(25, 20)),
            ),
        )
        val encoded = json.encodeToString(call)
        val decoded = json.decodeFromString<CallHierarchyOutgoingCall>(encoded)
        decoded shouldBe call
    }
}
