package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * The diagnostic severity.
 */
@Serializable(with = DiagnosticSeveritySerializer::class)
public enum class DiagnosticSeverity(
    public val value: Int,
) {
    Error(1),
    Warning(2),
    Information(3),
    Hint(4),

    ;

    public companion object {
        public fun fromValue(value: Int): DiagnosticSeverity =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown DiagnosticSeverity: $value")
    }
}

/**
 * Serializer for DiagnosticSeverity that encodes/decodes as integer.
 */
public object DiagnosticSeveritySerializer : IntEnumSerializer<DiagnosticSeverity>(
    "DiagnosticSeverity", DiagnosticSeverity::fromValue, { it.value },
)

/**
 * The diagnostic tags.
 */
@Serializable(with = DiagnosticTagSerializer::class)
public enum class DiagnosticTag(
    public val value: Int,
) {
    /**
     * Unused or unnecessary code.
     */
    Unnecessary(1),

    /**
     * Deprecated or obsolete code.
     */
    Deprecated(2),

    ;

    public companion object {
        public fun fromValue(value: Int): DiagnosticTag =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown DiagnosticTag: $value")
    }
}

/**
 * Serializer for DiagnosticTag that encodes/decodes as integer.
 */
public object DiagnosticTagSerializer : IntEnumSerializer<DiagnosticTag>(
    "DiagnosticTag", DiagnosticTag::fromValue, { it.value },
)

/**
 * Structure to capture a description for an error code.
 */
@Serializable
public data class CodeDescription(
    /**
     * An URI to open with more information about the diagnostic error.
     */
    val href: URI,
)

/**
 * Represents a related message and source code location for a diagnostic.
 */
@Serializable
public data class DiagnosticRelatedInformation(
    /**
     * The location of this related diagnostic information.
     */
    val location: Location,
    /**
     * The message of this related diagnostic information.
     */
    val message: String,
)

/**
 * Type alias for diagnostic code which can be either an Int or a String.
 * Per LSP spec, the diagnostic code can be a number or a string.
 */
public typealias DiagnosticCode = Either<Int, String>

/**
 * Serializer for DiagnosticCode (Either<Int, String>).
 * Handles both integer and string diagnostic codes as per LSP spec.
 */
public object DiagnosticCodeSerializer : KSerializer<DiagnosticCode> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DiagnosticCode")

    override fun serialize(
        encoder: Encoder,
        value: DiagnosticCode,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is Either.Left -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is Either.Right -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
        }
    }

    override fun deserialize(decoder: Decoder): DiagnosticCode {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element is JsonPrimitive && element.intOrNull != null -> Either.Left(element.intOrNull!!)
            element is JsonPrimitive -> Either.Right(element.content)
            else -> throw IllegalArgumentException("DiagnosticCode must be an integer or string")
        }
    }
}

/**
 * Represents a diagnostic, such as a compiler error or warning.
 */
@Serializable
public data class Diagnostic(
    /**
     * The range at which the message applies.
     */
    val range: Range,
    /**
     * The diagnostic's message.
     */
    val message: String,
    /**
     * The diagnostic's severity.
     */
    val severity: DiagnosticSeverity? = null,
    /**
     * The diagnostic's code, which might appear in the user interface.
     * Can be either a string or an integer as per LSP spec.
     */
    @Serializable(with = DiagnosticCodeSerializer::class)
    val code: DiagnosticCode? = null,
    /**
     * An optional property to describe the error code.
     */
    val codeDescription: CodeDescription? = null,
    /**
     * A human-readable string describing the source of this diagnostic.
     */
    val source: String? = null,
    /**
     * Additional metadata about the diagnostic.
     */
    val tags: List<DiagnosticTag>? = null,
    /**
     * An array of related diagnostic information.
     */
    val relatedInformation: List<DiagnosticRelatedInformation>? = null,
    /**
     * A data entry field that is preserved between a `textDocument/publishDiagnostics`
     * notification and `textDocument/codeAction` request.
     */
    val data: JsonElement? = null,
)

/**
 * The publish diagnostics notification parameters.
 */
@Serializable
public data class PublishDiagnosticsParams(
    /**
     * The URI for which diagnostic information is reported.
     */
    val uri: DocumentUri,
    /**
     * Optional the version number of the document the diagnostics are published for.
     */
    val version: Int? = null,
    /**
     * An array of diagnostic information items.
     */
    val diagnostics: List<Diagnostic>,
)
