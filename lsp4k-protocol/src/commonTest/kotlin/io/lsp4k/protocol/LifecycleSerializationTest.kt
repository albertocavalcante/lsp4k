package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/**
 * Tests for Lifecycle type serialization: SetTraceParams, LogTraceParams, TraceValue, CancelParams.
 */
class LifecycleSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== TraceValue Tests ====================

    @Test
    fun `TraceValue Off serialization`() {
        val value = TraceValue.Off
        val encoded = json.encodeToString(value)
        encoded shouldBe "\"off\""
    }

    @Test
    fun `TraceValue Messages serialization`() {
        val value = TraceValue.Messages
        val encoded = json.encodeToString(value)
        encoded shouldBe "\"messages\""
    }

    @Test
    fun `TraceValue Verbose serialization`() {
        val value = TraceValue.Verbose
        val encoded = json.encodeToString(value)
        encoded shouldBe "\"verbose\""
    }

    @Test
    fun `TraceValue roundtrip for all values`() {
        TraceValue.entries.forEach { value ->
            val encoded = json.encodeToString(value)
            val decoded = json.decodeFromString<TraceValue>(encoded)
            decoded shouldBe value
        }
    }

    // ==================== SetTraceParams Tests ====================

    @Test
    fun `SetTraceParams serialization roundtrip with Off`() {
        val original = SetTraceParams(value = TraceValue.Off)
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SetTraceParams>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `SetTraceParams serialization roundtrip with Verbose`() {
        val original = SetTraceParams(value = TraceValue.Verbose)
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SetTraceParams>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `SetTraceParams serialization roundtrip with Messages`() {
        val original = SetTraceParams(value = TraceValue.Messages)
        val encoded = json.encodeToString(original)
        encoded shouldContain "\"messages\""
        val decoded = json.decodeFromString<SetTraceParams>(encoded)
        decoded.value shouldBe TraceValue.Messages
    }

    // ==================== LogTraceParams Tests ====================

    @Test
    fun `LogTraceParams with message only`() {
        val original = LogTraceParams(message = "Processing request")
        val encoded = json.encodeToString(original)
        encoded shouldNotContain "verbose"
        val decoded = json.decodeFromString<LogTraceParams>(encoded)
        decoded shouldBe original
        decoded.verbose shouldBe null
    }

    @Test
    fun `LogTraceParams with message and verbose`() {
        val original = LogTraceParams(
            message = "Processing request",
            verbose = "Detailed trace info: request id=42, method=textDocument/completion",
        )
        val encoded = json.encodeToString(original)
        encoded shouldContain "verbose"
        val decoded = json.decodeFromString<LogTraceParams>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `LogTraceParams with empty message`() {
        val original = LogTraceParams(message = "")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<LogTraceParams>(encoded)
        decoded.message shouldBe ""
    }

    @Test
    fun `LogTraceParams with empty verbose`() {
        val original = LogTraceParams(message = "msg", verbose = "")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<LogTraceParams>(encoded)
        decoded.verbose shouldBe ""
    }

    // ==================== CancelParams Tests ====================

    @Test
    fun `CancelParams with integer id`() {
        val original = CancelParams(id = JsonPrimitive(42))
        val encoded = json.encodeToString(original)
        encoded shouldContain "42"
        val decoded = json.decodeFromString<CancelParams>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `CancelParams with string id`() {
        val original = CancelParams(id = JsonPrimitive("request-abc-123"))
        val encoded = json.encodeToString(original)
        encoded shouldContain "request-abc-123"
        val decoded = json.decodeFromString<CancelParams>(encoded)
        decoded shouldBe original
    }

    @Test
    fun `CancelParams roundtrip with numeric id`() {
        val original = CancelParams(id = JsonPrimitive(0))
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CancelParams>(encoded)
        decoded.id shouldBe JsonPrimitive(0)
    }

    @Test
    fun `CancelParams roundtrip with empty string id`() {
        val original = CancelParams(id = JsonPrimitive(""))
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CancelParams>(encoded)
        decoded.id shouldBe JsonPrimitive("")
    }
}
