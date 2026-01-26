package io.lsp4k.jsonrpc

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class CodecTest {
    private val codec = LspCodec.Default

    @Test
    fun `encode message with Content-Length header`() {
        val message = NotificationMessage(method = "initialized")
        val encoded = codec.encode(message)
        val decoded = encoded.decodeToString()

        decoded.startsWith("Content-Length:") shouldBe true
        decoded.contains("\r\n\r\n") shouldBe true
        decoded.contains("\"method\":\"initialized\"") shouldBe true
    }

    @Test
    fun `decode single message`() {
        val decoder = LspMessageDecoder(codec)
        val content = """{"jsonrpc":"2.0","method":"initialized"}"""
        val input = "Content-Length: ${content.length}\r\n\r\n$content"

        val messages = decoder.feed(input)
        messages shouldHaveSize 1
        messages[0].shouldBeInstanceOf<NotificationMessage>()
        (messages[0] as NotificationMessage).method shouldBe "initialized"
    }

    @Test
    fun `decode multiple messages in one chunk`() {
        val decoder = LspMessageDecoder(codec)
        val content1 = """{"jsonrpc":"2.0","method":"method1"}"""
        val content2 = """{"jsonrpc":"2.0","method":"method2"}"""
        val input =
            "Content-Length: ${content1.length}\r\n\r\n$content1" +
                "Content-Length: ${content2.length}\r\n\r\n$content2"

        val messages = decoder.feed(input)
        messages shouldHaveSize 2
        (messages[0] as NotificationMessage).method shouldBe "method1"
        (messages[1] as NotificationMessage).method shouldBe "method2"
    }

    @Test
    fun `decode message split across chunks`() {
        val decoder = LspMessageDecoder(codec)
        val content = """{"jsonrpc":"2.0","method":"test"}"""
        val fullMessage = "Content-Length: ${content.length}\r\n\r\n$content"

        // Split message in the middle
        val part1 = fullMessage.substring(0, 20)
        val part2 = fullMessage.substring(20)

        val messages1 = decoder.feed(part1)
        messages1 shouldHaveSize 0

        val messages2 = decoder.feed(part2)
        messages2 shouldHaveSize 1
        (messages2[0] as NotificationMessage).method shouldBe "test"
    }

    @Test
    fun `parseContentLength extracts correct value`() {
        codec.parseContentLength("Content-Length: 123") shouldBe 123
        codec.parseContentLength("content-length: 456") shouldBe 456
        codec.parseContentLength("Content-Type: application/json") shouldBe null
    }
}
