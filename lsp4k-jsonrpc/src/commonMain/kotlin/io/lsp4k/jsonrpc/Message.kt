package io.lsp4k.jsonrpc

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * JSON-RPC 2.0 protocol version constant.
 */
public const val JSONRPC_VERSION: String = "2.0"

/**
 * Request ID can be either a number or a string.
 * We use a sealed interface for type safety.
 */
@Serializable(with = RequestIdSerializer::class)
public sealed interface RequestId {
    @Serializable
    public data class NumberId(
        public val value: Long,
    ) : RequestId {
        override fun toString(): String = value.toString()
    }

    @Serializable
    public data class StringId(
        public val value: String,
    ) : RequestId {
        override fun toString(): String = value
    }

    public companion object {
        public fun of(value: Long): RequestId = NumberId(value)

        public fun of(value: String): RequestId = StringId(value)
    }
}

/**
 * Custom serializer for RequestId to handle both number and string IDs.
 * Serializes NumberId as a primitive number and StringId as a primitive string.
 */
public object RequestIdSerializer : kotlinx.serialization.KSerializer<RequestId> {
    override val descriptor: kotlinx.serialization.descriptors.SerialDescriptor =
        kotlinx.serialization.descriptors.buildClassSerialDescriptor("RequestId")

    override fun serialize(
        encoder: kotlinx.serialization.encoding.Encoder,
        value: RequestId,
    ) {
        val jsonEncoder = encoder as kotlinx.serialization.json.JsonEncoder
        when (value) {
            is RequestId.NumberId -> jsonEncoder.encodeJsonElement(kotlinx.serialization.json.JsonPrimitive(value.value))
            is RequestId.StringId -> jsonEncoder.encodeJsonElement(kotlinx.serialization.json.JsonPrimitive(value.value))
        }
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): RequestId {
        val jsonDecoder = decoder as kotlinx.serialization.json.JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element.jsonPrimitive.isString -> RequestId.StringId(element.jsonPrimitive.content)
            else -> RequestId.NumberId(element.jsonPrimitive.content.toLong())
        }
    }
}

/**
 * Base sealed interface for all JSON-RPC 2.0 messages.
 */
@Serializable(with = MessageSerializer::class)
public sealed interface Message {
    public val jsonrpc: String get() = JSONRPC_VERSION
}

/**
 * A JSON-RPC 2.0 request message.
 */
@Serializable
public data class RequestMessage(
    /**
     * The request id.
     */
    val id: RequestId,
    /**
     * The method to be invoked.
     */
    val method: String,
    /**
     * The method's params.
     */
    val params: JsonElement? = null,
) : Message

/**
 * A JSON-RPC 2.0 response message.
 */
@Serializable
public data class ResponseMessage(
    /**
     * The request id.
     */
    val id: RequestId?,
    /**
     * The result of a request. This member is REQUIRED on success.
     * This member MUST NOT exist if there was an error invoking the method.
     */
    val result: JsonElement? = null,
    /**
     * The error object in case a request fails.
     */
    val error: ResponseError? = null,
) : Message {
    init {
        require(result != null || error != null || id == null) {
            "Response must have either result or error (or be a response to a notification)"
        }
    }

    public companion object {
        public fun success(
            id: RequestId,
            result: JsonElement?,
        ): ResponseMessage = ResponseMessage(id = id, result = result)

        public fun error(
            id: RequestId?,
            error: ResponseError,
        ): ResponseMessage = ResponseMessage(id = id, error = error)
    }
}

/**
 * A JSON-RPC 2.0 notification message.
 * A notification is a request without an id - no response is expected.
 */
@Serializable
public data class NotificationMessage(
    /**
     * The method to be invoked.
     */
    val method: String,
    /**
     * The notification's params.
     */
    val params: JsonElement? = null,
) : Message

/**
 * JSON-RPC 2.0 error object.
 */
@Serializable
public data class ResponseError(
    /**
     * A number indicating the error type that occurred.
     */
    val code: Int,
    /**
     * A string providing a short description of the error.
     */
    val message: String,
    /**
     * A primitive or structured value that contains additional information about the error.
     */
    val data: JsonElement? = null,
) {
    public companion object {
        // JSON-RPC 2.0 defined error codes
        public const val PARSE_ERROR: Int = -32700
        public const val INVALID_REQUEST: Int = -32600
        public const val METHOD_NOT_FOUND: Int = -32601
        public const val INVALID_PARAMS: Int = -32602
        public const val INTERNAL_ERROR: Int = -32603

        // LSP defined error codes
        public const val SERVER_NOT_INITIALIZED: Int = -32002
        public const val UNKNOWN_ERROR_CODE: Int = -32001

        // LSP 3.16+ error codes
        public const val REQUEST_FAILED: Int = -32803
        public const val SERVER_CANCELLED: Int = -32802
        public const val CONTENT_MODIFIED: Int = -32801
        public const val REQUEST_CANCELLED: Int = -32800

        public fun parseError(
            message: String,
            data: JsonElement? = null,
        ): ResponseError = ResponseError(PARSE_ERROR, message, data)

        public fun invalidRequest(
            message: String,
            data: JsonElement? = null,
        ): ResponseError = ResponseError(INVALID_REQUEST, message, data)

        public fun methodNotFound(method: String): ResponseError = ResponseError(METHOD_NOT_FOUND, "Method not found: $method")

        public fun invalidParams(
            message: String,
            data: JsonElement? = null,
        ): ResponseError = ResponseError(INVALID_PARAMS, message, data)

        public fun internalError(
            message: String,
            data: JsonElement? = null,
        ): ResponseError = ResponseError(INTERNAL_ERROR, message, data)

        public fun serverNotInitialized(): ResponseError = ResponseError(SERVER_NOT_INITIALIZED, "Server not initialized")

        public fun requestCancelled(): ResponseError = ResponseError(REQUEST_CANCELLED, "Request cancelled")

        public fun contentModified(): ResponseError = ResponseError(CONTENT_MODIFIED, "Content modified")
    }
}

/**
 * Polymorphic serializer for Message that determines the concrete type
 * based on the presence of id/method/result/error fields.
 */
public object MessageSerializer : JsonContentPolymorphicSerializer<Message>(Message::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Message> {
        val jsonObject = element.jsonObject
        return when {
            // Has "id" field
            "id" in jsonObject -> {
                // Has "method" field -> Request
                if ("method" in jsonObject) {
                    RequestMessage.serializer()
                } else {
                    // Has "result" or "error" field -> Response
                    ResponseMessage.serializer()
                }
            }
            // No "id" but has "method" -> Notification
            "method" in jsonObject -> NotificationMessage.serializer()
            // Response with null id (error response to notification)
            "result" in jsonObject || "error" in jsonObject -> ResponseMessage.serializer()
            else -> throw IllegalArgumentException("Cannot determine message type from: $element")
        }
    }
}
