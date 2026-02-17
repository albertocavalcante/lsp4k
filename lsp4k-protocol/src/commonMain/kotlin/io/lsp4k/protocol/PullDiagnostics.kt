package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

/**
 * Parameters for the textDocument/diagnostic request.
 *
 * @since 3.17.0
 */
@Serializable
public data class DocumentDiagnosticParams(
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The additional identifier provided during registration.
     */
    val identifier: String? = null,
    /**
     * The result id of a previous response if provided.
     */
    val previousResultId: String? = null,
)

/**
 * The document diagnostic report kinds.
 *
 * @since 3.17.0
 */
@Serializable
public enum class DocumentDiagnosticReportKind {
    /**
     * A diagnostic report with a full set of problems.
     */
    @SerialName("full")
    Full,

    /**
     * A report indicating that the last returned report is still accurate.
     */
    @SerialName("unchanged")
    Unchanged,
}

/**
 * A diagnostic report with a full set of problems.
 *
 * @since 3.17.0
 */
@Serializable
public data class FullDocumentDiagnosticReport(
    /**
     * A full document diagnostic report.
     */
    val kind: DocumentDiagnosticReportKind = DocumentDiagnosticReportKind.Full,
    /**
     * An optional result id. If provided it will be sent on the next
     * diagnostic request for the same document.
     */
    val resultId: String? = null,
    /**
     * The actual items.
     */
    val items: List<Diagnostic>,
)

/**
 * A diagnostic report indicating that the last returned report is still accurate.
 *
 * @since 3.17.0
 */
@Serializable
public data class UnchangedDocumentDiagnosticReport(
    /**
     * A document diagnostic report indicating no changes to the last result.
     */
    val kind: DocumentDiagnosticReportKind = DocumentDiagnosticReportKind.Unchanged,
    /**
     * A result id which will be sent on the next diagnostic request for the same document.
     */
    val resultId: String,
)

/**
 * A full diagnostic report with a set of related documents.
 *
 * @since 3.17.0
 */
@Serializable
public data class RelatedFullDocumentDiagnosticReport(
    /**
     * A full document diagnostic report.
     */
    val kind: DocumentDiagnosticReportKind = DocumentDiagnosticReportKind.Full,
    /**
     * An optional result id. If provided it will be sent on the next
     * diagnostic request for the same document.
     */
    val resultId: String? = null,
    /**
     * The actual items.
     */
    val items: List<Diagnostic>,
    /**
     * Diagnostics of related documents. This information is useful
     * in programming languages where code in a file A can generate
     * diagnostics in a file B which A depends on. An example of
     * such a language is C/C++ where macro definitions in a file
     * a.cpp can result in errors in a header file b.hpp.
     *
     * @since 3.17.0
     */
    @Serializable(with = RelatedDocumentsSerializer::class)
    val relatedDocuments: Map<DocumentUri, DocumentDiagnosticReport>? = null,
)

/**
 * An unchanged diagnostic report with a set of related documents.
 *
 * @since 3.17.0
 */
@Serializable
public data class RelatedUnchangedDocumentDiagnosticReport(
    /**
     * A document diagnostic report indicating no changes to the last result.
     */
    val kind: DocumentDiagnosticReportKind = DocumentDiagnosticReportKind.Unchanged,
    /**
     * A result id which will be sent on the next diagnostic request for the same document.
     */
    val resultId: String,
    /**
     * Diagnostics of related documents. This information is useful
     * in programming languages where code in a file A can generate
     * diagnostics in a file B which A depends on.
     *
     * @since 3.17.0
     */
    @Serializable(with = RelatedDocumentsSerializer::class)
    val relatedDocuments: Map<DocumentUri, DocumentDiagnosticReport>? = null,
)

/**
 * The result of a document diagnostic pull request.
 * A report can either be a full report containing all diagnostics for the
 * requested document or an unchanged report indicating that nothing has changed
 * in terms of diagnostics in comparison to the last pull request.
 *
 * @since 3.17.0
 */
@Serializable(with = DocumentDiagnosticReportSerializer::class)
public sealed interface DocumentDiagnosticReport {
    /**
     * Wrapper for a full document diagnostic report.
     */
    public data class Full(
        public val report: FullDocumentDiagnosticReport,
    ) : DocumentDiagnosticReport

    /**
     * Wrapper for an unchanged document diagnostic report.
     */
    public data class Unchanged(
        public val report: UnchangedDocumentDiagnosticReport,
    ) : DocumentDiagnosticReport
}

/**
 * Serializer for DocumentDiagnosticReport that discriminates based on the "kind" field.
 */
public object DocumentDiagnosticReportSerializer : KSerializer<DocumentDiagnosticReport> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DocumentDiagnosticReport")

    override fun serialize(
        encoder: Encoder,
        value: DocumentDiagnosticReport,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is DocumentDiagnosticReport.Full ->
                jsonEncoder.encodeSerializableValue(FullDocumentDiagnosticReport.serializer(), value.report)
            is DocumentDiagnosticReport.Unchanged ->
                jsonEncoder.encodeSerializableValue(UnchangedDocumentDiagnosticReport.serializer(), value.report)
        }
    }

    override fun deserialize(decoder: Decoder): DocumentDiagnosticReport {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element is JsonObject && element["kind"]?.jsonPrimitive?.content == "full" -> {
                DocumentDiagnosticReport.Full(
                    jsonDecoder.json.decodeFromJsonElement(FullDocumentDiagnosticReport.serializer(), element),
                )
            }
            element is JsonObject && element["kind"]?.jsonPrimitive?.content == "unchanged" -> {
                DocumentDiagnosticReport.Unchanged(
                    jsonDecoder.json.decodeFromJsonElement(UnchangedDocumentDiagnosticReport.serializer(), element),
                )
            }
            else -> throw IllegalArgumentException("DocumentDiagnosticReport must have kind 'full' or 'unchanged'")
        }
    }
}

/**
 * Serializer for the relatedDocuments map.
 */
public object RelatedDocumentsSerializer : KSerializer<Map<DocumentUri, DocumentDiagnosticReport>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RelatedDocuments")

    override fun serialize(
        encoder: Encoder,
        value: Map<DocumentUri, DocumentDiagnosticReport>,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        val jsonObject =
            buildMap {
                value.forEach { (uri, report) ->
                    put(
                        uri,
                        when (report) {
                            is DocumentDiagnosticReport.Full ->
                                jsonEncoder.json.encodeToJsonElement(FullDocumentDiagnosticReport.serializer(), report.report)
                            is DocumentDiagnosticReport.Unchanged ->
                                jsonEncoder.json.encodeToJsonElement(UnchangedDocumentDiagnosticReport.serializer(), report.report)
                        },
                    )
                }
            }
        jsonEncoder.encodeSerializableValue(
            serializer<Map<String, JsonElement>>(),
            jsonObject,
        )
    }

    override fun deserialize(decoder: Decoder): Map<DocumentUri, DocumentDiagnosticReport> {
        val jsonDecoder = decoder as JsonDecoder
        val jsonObject = jsonDecoder.decodeJsonElement() as JsonObject
        return jsonObject.mapValues { (_, element) ->
            when {
                element is JsonObject && element["kind"]?.jsonPrimitive?.content == "full" -> {
                    DocumentDiagnosticReport.Full(
                        jsonDecoder.json.decodeFromJsonElement(FullDocumentDiagnosticReport.serializer(), element),
                    )
                }
                element is JsonObject && element["kind"]?.jsonPrimitive?.content == "unchanged" -> {
                    DocumentDiagnosticReport.Unchanged(
                        jsonDecoder.json.decodeFromJsonElement(UnchangedDocumentDiagnosticReport.serializer(), element),
                    )
                }
                else -> throw IllegalArgumentException("Related document diagnostic must have kind 'full' or 'unchanged'")
            }
        }
    }
}

/**
 * A partial result for a document diagnostic report.
 *
 * @since 3.17.0
 */
@Serializable
public data class DocumentDiagnosticReportPartialResult(
    /**
     * Partial result of related documents.
     */
    @Serializable(with = RelatedDocumentsSerializer::class)
    val relatedDocuments: Map<DocumentUri, DocumentDiagnosticReport>,
)

/**
 * Parameters for the workspace/diagnostic request.
 *
 * @since 3.17.0
 */
@Serializable
public data class WorkspaceDiagnosticParams(
    /**
     * The additional identifier provided during registration.
     */
    val identifier: String? = null,
    /**
     * The currently known diagnostic reports with their
     * previous result ids.
     */
    val previousResultIds: List<PreviousResultId>,
)

/**
 * A previous result id in a workspace pull request.
 *
 * @since 3.17.0
 */
@Serializable
public data class PreviousResultId(
    /**
     * The URI for which the client knows a result id.
     */
    val uri: DocumentUri,
    /**
     * The value of the previous result id.
     */
    val value: String,
)

/**
 * A full document diagnostic report for a workspace diagnostic result.
 *
 * @since 3.17.0
 */
@Serializable
public data class WorkspaceFullDocumentDiagnosticReport(
    /**
     * A full document diagnostic report.
     */
    val kind: DocumentDiagnosticReportKind = DocumentDiagnosticReportKind.Full,
    /**
     * An optional result id. If provided it will be sent on the next
     * diagnostic request for the same document.
     */
    val resultId: String? = null,
    /**
     * The actual items.
     */
    val items: List<Diagnostic>,
    /**
     * The URI for which diagnostic information is reported.
     */
    val uri: DocumentUri,
    /**
     * The version number for which the diagnostics are reported.
     * If the document is not marked as open `null` can be provided.
     */
    val version: Int? = null,
)

/**
 * An unchanged document diagnostic report for a workspace diagnostic result.
 *
 * @since 3.17.0
 */
@Serializable
public data class WorkspaceUnchangedDocumentDiagnosticReport(
    /**
     * A document diagnostic report indicating no changes to the last result.
     */
    val kind: DocumentDiagnosticReportKind = DocumentDiagnosticReportKind.Unchanged,
    /**
     * A result id which will be sent on the next diagnostic request for the same document.
     */
    val resultId: String,
    /**
     * The URI for which diagnostic information is reported.
     */
    val uri: DocumentUri,
    /**
     * The version number for which the diagnostics are reported.
     * If the document is not marked as open `null` can be provided.
     */
    val version: Int? = null,
)

/**
 * A workspace diagnostic document report.
 *
 * @since 3.17.0
 */
@Serializable(with = WorkspaceDocumentDiagnosticReportSerializer::class)
public sealed interface WorkspaceDocumentDiagnosticReport {
    /**
     * Wrapper for a full workspace document diagnostic report.
     */
    public data class Full(
        public val report: WorkspaceFullDocumentDiagnosticReport,
    ) : WorkspaceDocumentDiagnosticReport

    /**
     * Wrapper for an unchanged workspace document diagnostic report.
     */
    public data class Unchanged(
        public val report: WorkspaceUnchangedDocumentDiagnosticReport,
    ) : WorkspaceDocumentDiagnosticReport
}

/**
 * Serializer for WorkspaceDocumentDiagnosticReport that discriminates based on the "kind" field.
 */
public object WorkspaceDocumentDiagnosticReportSerializer : KSerializer<WorkspaceDocumentDiagnosticReport> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("WorkspaceDocumentDiagnosticReport")

    override fun serialize(
        encoder: Encoder,
        value: WorkspaceDocumentDiagnosticReport,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is WorkspaceDocumentDiagnosticReport.Full ->
                jsonEncoder.encodeSerializableValue(WorkspaceFullDocumentDiagnosticReport.serializer(), value.report)
            is WorkspaceDocumentDiagnosticReport.Unchanged ->
                jsonEncoder.encodeSerializableValue(WorkspaceUnchangedDocumentDiagnosticReport.serializer(), value.report)
        }
    }

    override fun deserialize(decoder: Decoder): WorkspaceDocumentDiagnosticReport {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element is JsonObject && element["kind"]?.jsonPrimitive?.content == "full" -> {
                WorkspaceDocumentDiagnosticReport.Full(
                    jsonDecoder.json.decodeFromJsonElement(WorkspaceFullDocumentDiagnosticReport.serializer(), element),
                )
            }
            element is JsonObject && element["kind"]?.jsonPrimitive?.content == "unchanged" -> {
                WorkspaceDocumentDiagnosticReport.Unchanged(
                    jsonDecoder.json.decodeFromJsonElement(WorkspaceUnchangedDocumentDiagnosticReport.serializer(), element),
                )
            }
            else -> throw IllegalArgumentException("WorkspaceDocumentDiagnosticReport must have kind 'full' or 'unchanged'")
        }
    }
}

/**
 * A workspace diagnostic report.
 *
 * @since 3.17.0
 */
@Serializable
public data class WorkspaceDiagnosticReport(
    /**
     * The items of the workspace diagnostic report.
     */
    val items: List<WorkspaceDocumentDiagnosticReport>,
)

/**
 * A partial result for a workspace diagnostic report.
 *
 * @since 3.17.0
 */
@Serializable
public data class WorkspaceDiagnosticReportPartialResult(
    /**
     * The items of the workspace diagnostic report.
     */
    val items: List<WorkspaceDocumentDiagnosticReport>,
)

/**
 * Diagnostic registration options.
 *
 * @since 3.17.0
 */
@Serializable
public data class DiagnosticRegistrationOptions(
    /**
     * A document selector to identify the scope of the registration.
     */
    val documentSelector: List<DocumentFilter>? = null,
    /**
     * An optional identifier under which the diagnostics are managed by the client.
     */
    val identifier: String? = null,
    /**
     * Whether the language has inter file dependencies meaning that
     * editing code in one file can result in a different diagnostic
     * set in another file.
     */
    val interFileDependencies: Boolean,
    /**
     * The server provides support for workspace diagnostics as well.
     */
    val workspaceDiagnostics: Boolean,
    /**
     * The id used to register the request.
     */
    val id: String? = null,
)

/**
 * A document filter denotes a document through properties like language, scheme, or pattern.
 *
 * @since 3.17.0
 */
@Serializable
public data class DocumentFilter(
    /**
     * A language id, like `typescript`.
     */
    val language: String? = null,
    /**
     * A Uri scheme, like `file` or `untitled`.
     */
    val scheme: String? = null,
    /**
     * A glob pattern, like `*.{ts,js}`.
     */
    val pattern: String? = null,
)
