package io.lsp4k.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Parameters for the textDocument/moniker request.
 */
@Serializable
public data class MonikerParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The position inside the text document.
     */
    val position: Position,
)

/**
 * Uniqueness level to identify a moniker.
 */
@Serializable
public enum class UniquenessLevel {
    /**
     * The moniker is only unique inside a document.
     */
    @SerialName("document")
    Document,

    /**
     * The moniker is unique inside a project for which a dump got created.
     */
    @SerialName("project")
    Project,

    /**
     * The moniker is unique inside the group to which a project belongs.
     */
    @SerialName("group")
    Group,

    /**
     * The moniker is unique inside the moniker scheme.
     */
    @SerialName("scheme")
    Scheme,

    /**
     * The moniker is globally unique.
     */
    @SerialName("global")
    Global,
}

/**
 * The moniker kind.
 */
@Serializable
public enum class MonikerKind {
    /**
     * The moniker represents a symbol that is imported into a project.
     */
    @SerialName("import")
    Import,

    /**
     * The moniker represents a symbol that is exported from a project.
     */
    @SerialName("export")
    Export,

    /**
     * The moniker represents a symbol that is local to a project.
     */
    @SerialName("local")
    Local,
}

/**
 * Moniker definition to match LSIF 0.5 moniker definition.
 */
@Serializable
public data class Moniker(
    /**
     * The scheme of the moniker. For example tsc or .Net.
     */
    val scheme: String,
    /**
     * The identifier of the moniker. The value is opaque in LSIF however
     * schema://identifier should be unique inside an LSIF dump.
     */
    val identifier: String,
    /**
     * The scope in which the moniker is unique.
     */
    val unique: UniquenessLevel,
    /**
     * The moniker kind if known.
     */
    val kind: MonikerKind? = null,
)
