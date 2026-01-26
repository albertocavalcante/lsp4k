package io.lsp4k.jsonrpc

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test

class MessageTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    @Test
    fun `RequestMessage serialization roundtrip`() {
        val request =
            RequestMessage(
                id = RequestId.of(1),
                method = "textDocument/completion",
                params =
                    buildJsonObject {
                        put(
                            "textDocument",
                            buildJsonObject {
                                put("uri", JsonPrimitive("file:///test.kt"))
                            },
                        )
                    },
            )

        val encoded = json.encodeToString(Message.serializer(), request)
        val decoded = json.decodeFromString(Message.serializer(), encoded)

        decoded.shouldBeInstanceOf<RequestMessage>()
        (decoded as RequestMessage).id shouldBe RequestId.of(1)
        decoded.method shouldBe "textDocument/completion"
    }

    @Test
    fun `NotificationMessage serialization roundtrip`() {
        val notification =
            NotificationMessage(
                method = "initialized",
                params = null,
            )

        val encoded = json.encodeToString(Message.serializer(), notification)
        val decoded = json.decodeFromString(Message.serializer(), encoded)

        decoded.shouldBeInstanceOf<NotificationMessage>()
        (decoded as NotificationMessage).method shouldBe "initialized"
    }

    @Test
    fun `ResponseMessage with result`() {
        val response =
            ResponseMessage.success(
                id = RequestId.of(1),
                result = JsonPrimitive("success"),
            )

        val encoded = json.encodeToString(Message.serializer(), response)
        val decoded = json.decodeFromString(Message.serializer(), encoded)

        decoded.shouldBeInstanceOf<ResponseMessage>()
        (decoded as ResponseMessage).id shouldBe RequestId.of(1)
        decoded.result shouldBe JsonPrimitive("success")
        decoded.error shouldBe null
    }

    @Test
    fun `ResponseMessage with error`() {
        val response =
            ResponseMessage.error(
                id = RequestId.of(1),
                error = ResponseError.methodNotFound("unknown/method"),
            )

        val encoded = json.encodeToString(Message.serializer(), response)
        val decoded = json.decodeFromString(Message.serializer(), encoded)

        decoded.shouldBeInstanceOf<ResponseMessage>()
        (decoded as ResponseMessage).error?.code shouldBe ResponseError.METHOD_NOT_FOUND
    }

    @Test
    fun `RequestId with string value`() {
        val id = RequestId.of("uuid-123")
        id.shouldBeInstanceOf<RequestId.StringId>()
        id.toString() shouldBe "uuid-123"
    }

    @Test
    fun `RequestId with number value`() {
        val id = RequestId.of(42)
        id.shouldBeInstanceOf<RequestId.NumberId>()
        id.toString() shouldBe "42"
    }
}
