package io.lsp4k.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * General client capabilities.
 */
@Serializable
public data class GeneralClientCapabilities(
    /**
     * Client capability that signals how the client handles stale requests.
     */
    val staleRequestSupport: StaleRequestSupportCapability? = null,
    /**
     * Client capabilities specific to regular expressions.
     */
    val regularExpressions: RegularExpressionsClientCapabilities? = null,
    /**
     * Client capabilities specific to the client's markdown parser.
     */
    val markdown: MarkdownClientCapabilities? = null,
    /**
     * The position encodings supported by the client.
     */
    val positionEncodings: List<String>? = null,
)

/**
 * Stale request support capability.
 */
@Serializable
public data class StaleRequestSupportCapability(
    /**
     * The client will actively cancel the request.
     */
    val cancel: Boolean,
    /**
     * The list of requests for which the client will retry the request
     * if it receives a response with error code `ContentModified`.
     */
    val retryOnContentModified: List<String>,
)

/**
 * Client capabilities specific to regular expressions.
 */
@Serializable
public data class RegularExpressionsClientCapabilities(
    /**
     * The engine's name.
     */
    val engine: String,
    /**
     * The engine's version.
     */
    val version: String? = null,
)

/**
 * Client capabilities specific to the used markdown parser.
 */
@Serializable
public data class MarkdownClientCapabilities(
    /**
     * The name of the parser.
     */
    val parser: String,
    /**
     * The version of the parser.
     */
    val version: String? = null,
    /**
     * A list of HTML tags that the client allows in Markdown.
     */
    val allowedTags: List<String>? = null,
)

/**
 * Client capabilities.
 */
@Serializable
public data class ClientCapabilities(
    /**
     * Workspace specific client capabilities.
     */
    val workspace: WorkspaceClientCapabilities? = null,
    /**
     * Text document specific client capabilities.
     */
    val textDocument: TextDocumentClientCapabilities? = null,
    /**
     * Window specific client capabilities.
     */
    val window: WindowClientCapabilities? = null,
    /**
     * General client capabilities.
     */
    val general: GeneralClientCapabilities? = null,
    /**
     * Experimental client capabilities.
     */
    val experimental: JsonElement? = null,
)
