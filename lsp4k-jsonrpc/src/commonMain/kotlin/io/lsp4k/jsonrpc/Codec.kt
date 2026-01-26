package io.lsp4k.jsonrpc

import kotlinx.serialization.json.Json

/**
 * LSP message codec for encoding/decoding messages with Content-Length headers.
 *
 * LSP messages are framed with HTTP-like headers:
 * ```
 * Content-Length: <length>\r\n
 * \r\n
 * <JSON content>
 * ```
 */
public class LspCodec(
    private val json: Json = defaultJson,
) {
    /**
     * Encode a message to LSP wire format with Content-Length header.
     */
    public fun encode(message: Message): ByteArray {
        val content = json.encodeToString(Message.serializer(), message)
        val contentBytes = content.encodeToByteArray()
        val header = "Content-Length: ${contentBytes.size}\r\n\r\n"
        return header.encodeToByteArray() + contentBytes
    }

    /**
     * Encode a message to JSON string (without headers).
     */
    public fun encodeToJson(message: Message): String = json.encodeToString(Message.serializer(), message)

    /**
     * Decode a JSON string to a Message.
     */
    public fun decodeFromJson(jsonString: String): Message = json.decodeFromString(Message.serializer(), jsonString)

    /**
     * Parse Content-Length header from a header line.
     * Returns null if the line is not a Content-Length header.
     */
    public fun parseContentLength(headerLine: String): Int? {
        val trimmed = headerLine.trim()
        if (!trimmed.startsWith(CONTENT_LENGTH_HEADER, ignoreCase = true)) {
            return null
        }
        val value = trimmed.substringAfter(':').trim()
        return value.toIntOrNull()
    }

    public companion object {
        public const val CONTENT_LENGTH_HEADER: String = "Content-Length:"
        public const val HEADER_DELIMITER: String = "\r\n\r\n"
        public const val LINE_DELIMITER: String = "\r\n"

        /**
         * Default JSON configuration for LSP.
         */
        public val defaultJson: Json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
                isLenient = true
                coerceInputValues = true
            }

        /**
         * Shared instance with default configuration.
         */
        public val Default: LspCodec = LspCodec()
    }
}

/**
 * A streaming decoder for LSP messages.
 * Handles partial reads and buffering.
 */
public class LspMessageDecoder(
    private val codec: LspCodec = LspCodec.Default,
) {
    private var buffer = ""
    private var expectedContentLength: Int? = null
    private var headersParsed = false

    /**
     * Feed data into the decoder.
     * Returns a list of complete messages that were decoded.
     */
    public fun feed(data: String): List<Message> {
        buffer += data
        return drainMessages()
    }

    /**
     * Feed data into the decoder.
     * Returns a list of complete messages that were decoded.
     */
    public fun feed(data: ByteArray): List<Message> = feed(data.decodeToString())

    /**
     * Reset the decoder state.
     */
    public fun reset() {
        buffer = ""
        expectedContentLength = null
        headersParsed = false
    }

    private fun drainMessages(): List<Message> {
        val messages = mutableListOf<Message>()

        while (true) {
            val message = tryDecodeOne() ?: break
            messages.add(message)
        }

        return messages
    }

    private fun tryDecodeOne(): Message? {
        // If we haven't parsed headers yet, look for the header delimiter
        if (!headersParsed) {
            val headerEndIndex = buffer.indexOf(LspCodec.HEADER_DELIMITER)
            if (headerEndIndex < 0) {
                return null // Need more data
            }

            val headerSection = buffer.substring(0, headerEndIndex)
            val lines = headerSection.split(LspCodec.LINE_DELIMITER)

            for (line in lines) {
                val contentLength = codec.parseContentLength(line)
                if (contentLength != null) {
                    expectedContentLength = contentLength
                    break
                }
            }

            if (expectedContentLength == null) {
                throw LspProtocolException("Missing Content-Length header")
            }

            // Remove headers from buffer
            buffer = buffer.substring(headerEndIndex + LspCodec.HEADER_DELIMITER.length)
            headersParsed = true
        }

        // Check if we have enough content
        val contentLength = expectedContentLength ?: return null
        if (buffer.length < contentLength) {
            return null // Need more data
        }

        // Extract and decode the message
        val jsonContent = buffer.substring(0, contentLength)
        buffer = buffer.substring(contentLength)

        // Reset state for next message
        expectedContentLength = null
        headersParsed = false

        return codec.decodeFromJson(jsonContent)
    }
}

/**
 * Exception thrown when LSP protocol is violated.
 */
public class LspProtocolException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
