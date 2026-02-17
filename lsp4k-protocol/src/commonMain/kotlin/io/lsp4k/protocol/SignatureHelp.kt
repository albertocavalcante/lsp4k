package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int

/**
 * Parameters for textDocument/signatureHelp request.
 */
@Serializable
public data class SignatureHelpParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The position inside the text document.
     */
    val position: Position,
    /**
     * The signature help context.
     */
    val context: SignatureHelpContext? = null,
    /**
     * An optional token that a server can use to report work done progress.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
)

/**
 * Additional information about the context in which a signature help request
 * was triggered.
 */
@Serializable
public data class SignatureHelpContext(
    /**
     * Action that caused signature help to be triggered.
     */
    val triggerKind: SignatureHelpTriggerKind,
    /**
     * Character that caused signature help to be triggered.
     */
    val triggerCharacter: String? = null,
    /**
     * `true` if signature help was already showing when it was triggered.
     */
    val isRetrigger: Boolean,
    /**
     * The currently active `SignatureHelp`.
     */
    val activeSignatureHelp: SignatureHelp? = null,
)

/**
 * How a signature help was triggered.
 */
@Serializable(with = SignatureHelpTriggerKindSerializer::class)
public enum class SignatureHelpTriggerKind(
    public val value: Int,
) {
    /**
     * Signature help was invoked manually by the user or by a command.
     */
    Invoked(1),

    /**
     * Signature help was triggered by a trigger character.
     */
    TriggerCharacter(2),

    /**
     * Signature help was triggered by the cursor moving or by the document content changing.
     */
    ContentChange(3),

    ;

    public companion object {
        public fun fromValue(value: Int): SignatureHelpTriggerKind =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown SignatureHelpTriggerKind: $value")
    }
}

/**
 * Serializer for SignatureHelpTriggerKind that encodes/decodes as integer.
 */
public object SignatureHelpTriggerKindSerializer : IntEnumSerializer<SignatureHelpTriggerKind>(
    "SignatureHelpTriggerKind",
    SignatureHelpTriggerKind::fromValue,
    { it.value },
)

/**
 * Signature help represents the signature of something callable.
 */
@Serializable
public data class SignatureHelp(
    /**
     * One or more signatures.
     */
    val signatures: List<SignatureInformation>,
    /**
     * The active signature. If omitted or the value lies outside the
     * range of `signatures` the value defaults to zero or is ignored if
     * the `SignatureHelp` has no signatures.
     */
    val activeSignature: Int? = null,
    /**
     * The active parameter of the active signature. If omitted or the value
     * lies outside the range of `signatures[activeSignature].parameters`
     * defaults to 0 if the active signature has parameters. If the active
     * signature has no parameters it is ignored.
     */
    val activeParameter: Int? = null,
)

/**
 * Represents the signature of something callable.
 */
@Serializable
public data class SignatureInformation(
    /**
     * The label of this signature.
     */
    val label: String,
    /**
     * The human-readable doc-comment of this signature.
     * Can be a plain string or a MarkupContent object.
     */
    @Serializable(with = NullableStringOrMarkupContentSerializer::class)
    val documentation: Either<String, MarkupContent>? = null,
    /**
     * The parameters of this signature.
     */
    val parameters: List<ParameterInformation>? = null,
    /**
     * The index of the active parameter.
     */
    val activeParameter: Int? = null,
)

/**
 * Serializer for `Either<String, Pair<Int, Int>>` as used in the LSP spec
 * for `ParameterInformation.label`, which can be either a plain string
 * or a `[uinteger, uinteger]` tuple representing an inclusive start and exclusive end offset.
 *
 * On serialization: if Left (String), writes a JsonPrimitive; if Right (Pair), writes a two-element JsonArray.
 * On deserialization: checks if the element is a JsonPrimitive (string) or a JsonArray (tuple).
 */
public object ParameterLabelSerializer : KSerializer<Either<String, Pair<Int, Int>>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Either<String, Pair<Int, Int>>")

    override fun serialize(
        encoder: Encoder,
        value: Either<String, Pair<Int, Int>>,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is Either.Left -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is Either.Right ->
                jsonEncoder.encodeJsonElement(
                    JsonArray(
                        listOf(
                            JsonPrimitive(value.value.first),
                            JsonPrimitive(value.value.second),
                        ),
                    ),
                )
        }
    }

    override fun deserialize(decoder: Decoder): Either<String, Pair<Int, Int>> {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return if (element is JsonPrimitive && element.isString) {
            Either.Left(element.content)
        } else if (element is JsonArray && element.size == 2) {
            val first = (element[0] as JsonPrimitive).int
            val second = (element[1] as JsonPrimitive).int
            Either.Right(Pair(first, second))
        } else {
            error("ParameterInformation.label must be a string or a [uinteger, uinteger] tuple")
        }
    }
}

/**
 * Represents a parameter of a callable-signature.
 */
@Serializable
public data class ParameterInformation(
    /**
     * The label of this parameter information.
     * Can be a plain string or a [start, end] offset tuple within the
     * containing signature label.
     */
    @Serializable(with = ParameterLabelSerializer::class)
    val label: Either<String, Pair<Int, Int>>,
    /**
     * The human-readable doc-comment of this parameter.
     * Can be a plain string or a MarkupContent object.
     */
    @Serializable(with = NullableStringOrMarkupContentSerializer::class)
    val documentation: Either<String, MarkupContent>? = null,
)
