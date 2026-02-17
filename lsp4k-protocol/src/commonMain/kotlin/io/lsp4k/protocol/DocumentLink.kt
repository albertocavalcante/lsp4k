package io.lsp4k.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Parameters for textDocument/documentLink request.
 */
@Serializable
public data class DocumentLinkParams(
    /**
     * The document to provide document links for.
     */
    val textDocument: TextDocumentIdentifier,
)

/**
 * A document link is a range in a text document that links to an internal or
 * external resource, like another text document or a web site.
 */
@Serializable
public data class DocumentLink(
    /**
     * The range this link applies to.
     */
    val range: Range,
    /**
     * The uri this link points to. If missing a resolve request is sent later.
     */
    val target: DocumentUri? = null,
    /**
     * The tooltip text when you hover over this link.
     */
    val tooltip: String? = null,
    /**
     * A data entry field that is preserved on a document link between a
     * DocumentLinkRequest and a DocumentLinkResolveRequest.
     */
    val data: JsonElement? = null,
)
