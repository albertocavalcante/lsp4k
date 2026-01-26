package io.lsp4k.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The diagnostic severity.
 */
@Serializable
public enum class DiagnosticSeverity {
    @SerialName("1")
    Error,

    @SerialName("2")
    Warning,

    @SerialName("3")
    Information,

    @SerialName("4")
    Hint,

    ;

    public companion object {
        public fun fromValue(value: Int): DiagnosticSeverity =
            when (value) {
                1 -> Error
                2 -> Warning
                3 -> Information
                4 -> Hint
                else -> throw IllegalArgumentException("Unknown DiagnosticSeverity: $value")
            }
    }
}

/**
 * The diagnostic tags.
 */
@Serializable
public enum class DiagnosticTag {
    /**
     * Unused or unnecessary code.
     */
    @SerialName("1")
    Unnecessary,

    /**
     * Deprecated or obsolete code.
     */
    @SerialName("2")
    Deprecated,
}

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
     */
    val code: String? = null,
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
    val data: kotlinx.serialization.json.JsonElement? = null,
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
