package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement

/**
 * Options specific to a notebook plus its cells to be synced to the server.
 */
@Serializable
public data class NotebookDocumentSyncOptions(
    /**
     * The notebooks to be synced.
     */
    val notebookSelector: List<NotebookSelector>,
    /**
     * Whether save notification should be forwarded to the server.
     */
    val save: Boolean? = null,
)

/**
 * A notebook document filter denotes a notebook document by different properties.
 */
@Serializable
public data class NotebookDocumentFilter(
    /** The type of the enclosing notebook. */
    val notebookType: String? = null,
    /** A Uri scheme, like `file` or `untitled`. */
    val scheme: String? = null,
    /** A glob pattern. */
    @Serializable(with = GlobPatternSerializer::class)
    val pattern: GlobPattern? = null,
) {
    init {
        require(notebookType != null || scheme != null || pattern != null) {
            "NotebookDocumentFilter requires at least one of notebookType, scheme or pattern"
        }
    }
}

/**
 * Serializer for notebook which can be string | NotebookDocumentFilter.
 */
public object NotebookSelectorNotebookSerializer :
    KSerializer<Either<String, NotebookDocumentFilter>> by strictUnion(
        "NotebookSelectorNotebook",
        String.serializer(),
        NotebookDocumentFilter.serializer(),
        JsonElement::isStringLiteral,
        JsonElement::isObject,
    )

/**
 * A notebook selector.
 */
@Serializable
public data class NotebookSelector(
    /**
     * The notebook to be synced. If a string value is provided it matches
     * against the notebook type. '*' matches every notebook.
     */
    @Serializable(with = NotebookSelectorNotebookSerializer::class)
    val notebook: Either<String, NotebookDocumentFilter>? = null,
    /**
     * The cells of the matching notebook to be synced.
     */
    val cells: List<NotebookCellSelector>? = null,
) {
    init {
        require(notebook != null || cells != null) {
            "NotebookSelector requires at least one of notebook or cells"
        }
    }
}

/**
 * A notebook cell selector.
 */
@Serializable
public data class NotebookCellSelector(
    /**
     * The language of the cell.
     */
    val language: String,
)

/**
 * Diagnostic options.
 */
@Serializable
public data class DiagnosticOptions(
    /**
     * An optional identifier under which the diagnostics are managed by the client.
     */
    val identifier: String? = null,
    /**
     * Whether the language has inter file dependencies meaning that editing code
     * in one file can result in a different diagnostic set in another file.
     */
    val interFileDependencies: Boolean,
    /**
     * The server provides support for workspace diagnostics as well.
     */
    val workspaceDiagnostics: Boolean,
    /**
     * Whether the server supports work done progress.
     */
    val workDoneProgress: Boolean? = null,
)
