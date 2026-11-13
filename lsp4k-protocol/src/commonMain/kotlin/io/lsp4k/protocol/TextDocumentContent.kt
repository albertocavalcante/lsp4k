package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Parameters for the `workspace/textDocumentContent` request.
 *
 * @since 3.18.0
 */
@Serializable
public data class TextDocumentContentParams(
    /**
     * The uri of the text document.
     */
    val uri: DocumentUri,
)

/**
 * Result of the `workspace/textDocumentContent` request.
 *
 * @since 3.18.0
 */
@Serializable
public data class TextDocumentContentResult(
    /**
     * The text content of the text document. The content of any subsequent open notifications
     * for the text document might differ from the returned content due to whitespace and line
     * ending normalizations done on the client.
     */
    val text: String,
)

/**
 * Text document content provider options.
 *
 * @since 3.18.0
 */
@Serializable
public data class TextDocumentContentOptions(
    /**
     * The schemes for which the server provides content.
     */
    val schemes: List<String>,
)

/**
 * Text document content provider registration options.
 *
 * @since 3.18.0
 */
@Serializable
public data class TextDocumentContentRegistrationOptions(
    /**
     * The schemes for which the server provides content.
     */
    val schemes: List<String>,
    /**
     * The id used to register the request. The id can be used to deregister the request again.
     */
    val id: String? = null,
)

/**
 * Parameters for the `workspace/textDocumentContent/refresh` request.
 *
 * @since 3.18.0
 */
@Serializable
public data class TextDocumentContentRefreshParams(
    /**
     * The uri of the text document to refresh.
     */
    val uri: DocumentUri,
)

/**
 * Client capabilities for a text document content provider.
 *
 * @since 3.18.0
 */
@Serializable
public data class TextDocumentContentClientCapabilities(
    /**
     * Text document content provider supports dynamic registration.
     */
    val dynamicRegistration: Boolean? = null,
)

/**
 * `ServerWorkspaceCapabilities.textDocumentContent`: `TextDocumentContentOptions |
 * TextDocumentContentRegistrationOptions`. The registration form is chosen when the object has
 * an `id` key, so a registration without an id decodes as [TextDocumentContentOptions]; both
 * forms carry the same information on the wire.
 */
internal object TextDocumentContentProviderSer :
    KSerializer<Either<TextDocumentContentOptions, TextDocumentContentRegistrationOptions>> by strictUnion(
        "TextDocumentContentOptionsOrRegistrationOptions",
        TextDocumentContentOptions.serializer(),
        TextDocumentContentRegistrationOptions.serializer(),
        { it is JsonObject && !it.containsKey("id") },
        { it is JsonObject && it.containsKey("id") },
    )
