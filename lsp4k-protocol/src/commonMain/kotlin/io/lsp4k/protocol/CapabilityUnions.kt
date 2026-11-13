package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * The `{}` form of `range` in `SemanticTokensOptions` and `SemanticTokensClientCapabilities.requests`.
 * It carries no properties and always encodes as an empty JSON object.
 */
@Serializable
public object SemanticTokensRangeOptions

private fun JsonElement.isBoolean(): Boolean = this is JsonPrimitive && !isString && booleanOrNull != null

internal fun JsonElement.isInteger(): Boolean = this is JsonPrimitive && !isString && intOrNull != null

internal fun JsonElement.isStringLiteral(): Boolean = this is JsonPrimitive && isString

internal fun JsonElement.isObject(): Boolean = this is JsonObject

private fun JsonElement.isArray(): Boolean = this is JsonArray

private fun JsonElement.hasKind(kind: String): Boolean =
    this is JsonObject && (this["kind"] as? JsonPrimitive)?.takeIf { it.isString }?.content == kind

private fun JsonElement.isObjectWithKind(): Boolean = this is JsonObject && "kind" in this

private fun JsonElement.isObjectWithoutKind(): Boolean = this is JsonObject && "kind" !in this

private fun JsonElement.isObjectWith(key: String): Boolean = this is JsonObject && key in this

private fun JsonElement.isObjectWithout(key: String): Boolean = this is JsonObject && key !in this

/**
 * Builds a strict serializer for `L | R`: the JSON kind of the element selects the side,
 * and any element matching neither side is rejected.
 */
internal fun <L, R> strictUnion(
    name: String,
    leftSerializer: KSerializer<L>,
    rightSerializer: KSerializer<R>,
    isLeft: (JsonElement) -> Boolean,
    isRight: (JsonElement) -> Boolean,
): KSerializer<Either<L, R>> =
    object : KSerializer<Either<L, R>> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name)

        override fun serialize(
            encoder: Encoder,
            value: Either<L, R>,
        ) {
            val jsonEncoder = encoder as JsonEncoder
            when (value) {
                is Either.Left -> jsonEncoder.encodeSerializableValue(leftSerializer, value.value)
                is Either.Right -> jsonEncoder.encodeSerializableValue(rightSerializer, value.value)
            }
        }

        override fun deserialize(decoder: Decoder): Either<L, R> {
            val jsonDecoder = decoder as JsonDecoder
            val element = jsonDecoder.decodeJsonElement()
            return when {
                isLeft(element) -> Either.Left(jsonDecoder.json.decodeFromJsonElement(leftSerializer, element))
                isRight(element) -> Either.Right(jsonDecoder.json.decodeFromJsonElement(rightSerializer, element))
                else -> throw SerializationException("Invalid $name: $element")
            }
        }
    }

/**
 * Builds a strict serializer for `A | B | C`: the JSON kind of the element selects the member,
 * and any element matching none of them is rejected.
 */
internal fun <A, B, C> strictUnion3(
    name: String,
    firstSerializer: KSerializer<A>,
    secondSerializer: KSerializer<B>,
    thirdSerializer: KSerializer<C>,
    isFirst: (JsonElement) -> Boolean,
    isSecond: (JsonElement) -> Boolean,
    isThird: (JsonElement) -> Boolean,
): KSerializer<Either3<A, B, C>> =
    object : KSerializer<Either3<A, B, C>> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name)

        override fun serialize(
            encoder: Encoder,
            value: Either3<A, B, C>,
        ) {
            val jsonEncoder = encoder as JsonEncoder
            when (value) {
                is Either3.First -> jsonEncoder.encodeSerializableValue(firstSerializer, value.value)
                is Either3.Second -> jsonEncoder.encodeSerializableValue(secondSerializer, value.value)
                is Either3.Third -> jsonEncoder.encodeSerializableValue(thirdSerializer, value.value)
            }
        }

        override fun deserialize(decoder: Decoder): Either3<A, B, C> {
            val jsonDecoder = decoder as JsonDecoder
            val element = jsonDecoder.decodeJsonElement()
            return when {
                isFirst(element) -> Either3.First(jsonDecoder.json.decodeFromJsonElement(firstSerializer, element))
                isSecond(element) -> Either3.Second(jsonDecoder.json.decodeFromJsonElement(secondSerializer, element))
                isThird(element) -> Either3.Third(jsonDecoder.json.decodeFromJsonElement(thirdSerializer, element))
                else -> throw SerializationException("Invalid $name: $element")
            }
        }
    }

/** A `boolean | XOptions` capability: a JSON boolean or an options object; anything else is rejected. */
internal fun <O> booleanOrOptions(
    name: String,
    options: KSerializer<O>,
): KSerializer<Either<Boolean, O>> = strictUnion(name, Boolean.serializer(), options, JsonElement::isBoolean, JsonElement::isObject)

/**
 * A `boolean | XOptions | XRegistrationOptions` capability. An object is the registration form when it has a
 * `documentSelector` key (required by every registration options structure, `null` included).
 */
internal fun <O, R> booleanOrOptionsOrRegistration(
    name: String,
    options: KSerializer<O>,
    registration: KSerializer<R>,
): KSerializer<Either3<Boolean, O, R>> =
    strictUnion3(
        name,
        Boolean.serializer(),
        options,
        registration,
        JsonElement::isBoolean,
        { it.isObjectWithout(DOCUMENT_SELECTOR) },
        { it.isObjectWith(DOCUMENT_SELECTOR) },
    )

/**
 * An `XOptions | XRegistrationOptions` capability. An object is the registration form when it has the [key]
 * property, so a registration without that property decodes as the options form; both carry the same
 * information on the wire.
 */
internal fun <O, R> optionsOrRegistration(
    name: String,
    options: KSerializer<O>,
    registration: KSerializer<R>,
    key: String = DOCUMENT_SELECTOR,
): KSerializer<Either<O, R>> =
    strictUnion(name, options, registration, { it.isObjectWithout(key) }, { it.isObjectWith(key) })

private const val DOCUMENT_SELECTOR: String = "documentSelector"

/** `ServerCapabilities.textDocumentSync`: `TextDocumentSyncOptions | TextDocumentSyncKind`. */
internal object TextDocumentSyncSer : KSerializer<Either<TextDocumentSyncKind, TextDocumentSyncOptions>> by strictUnion(
    "TextDocumentSyncKindOrOptions",
    TextDocumentSyncKind.serializer(),
    TextDocumentSyncOptions.serializer(),
    JsonElement::isInteger,
    JsonElement::isObject,
)

/** `TextDocumentSyncOptions.save`: `boolean | SaveOptions`. */
internal object SaveSer : KSerializer<Either<Boolean, SaveOptions>> by strictUnion(
    "BooleanOrSaveOptions",
    Boolean.serializer(),
    SaveOptions.serializer(),
    JsonElement::isBoolean,
    JsonElement::isObject,
)

/** `SemanticTokensOptions.range`: `boolean | {}`. */
internal object SemanticTokensRangeSer : KSerializer<Either<Boolean, SemanticTokensRangeOptions>> by strictUnion(
    "BooleanOrSemanticTokensRange",
    Boolean.serializer(),
    SemanticTokensRangeOptions.serializer(),
    JsonElement::isBoolean,
    JsonElement::isObject,
)

/** `SemanticTokensOptions.full`: `boolean | { delta?: boolean }`. */
internal object SemanticTokensFullSer : KSerializer<Either<Boolean, SemanticTokensFullOptions>> by strictUnion(
    "BooleanOrSemanticTokensFull",
    Boolean.serializer(),
    SemanticTokensFullOptions.serializer(),
    JsonElement::isBoolean,
    JsonElement::isObject,
)

/** `SemanticTokensClientCapabilities.requests.full`: `boolean | { delta?: boolean }`. */
internal object SemanticTokensFullRequestSer :
    KSerializer<Either<Boolean, SemanticTokensFullRequestCapability>> by strictUnion(
        "BooleanOrSemanticTokensFullRequest",
        Boolean.serializer(),
        SemanticTokensFullRequestCapability.serializer(),
        JsonElement::isBoolean,
        JsonElement::isObject,
    )

/** `WorkspaceFoldersServerCapabilities.changeNotifications`: `string | boolean`. */
internal object ChangeNotificationsSer : KSerializer<Either<String, Boolean>> by strictUnion(
    "StringOrBoolean",
    String.serializer(),
    Boolean.serializer(),
    JsonElement::isStringLiteral,
    JsonElement::isBoolean,
)

/** `Diagnostic.message`: `string | MarkupContent`. */
internal object DiagnosticMessageSer : KSerializer<Either<String, MarkupContent>> by strictUnion(
    "StringOrMarkupContent",
    String.serializer(),
    MarkupContent.serializer(),
    JsonElement::isStringLiteral,
    JsonElement::isObject,
)

/** A `relatedDocuments` value: `FullDocumentDiagnosticReport | UnchangedDocumentDiagnosticReport`, selected by `kind`. */
public object RelatedDocumentReportSerializer : KSerializer<RelatedDocumentReport> by strictUnion(
    "RelatedDocumentReport",
    FullDocumentDiagnosticReport.serializer(),
    UnchangedDocumentDiagnosticReport.serializer(),
    { it.hasKind("full") },
    { it.hasKind("unchanged") },
)

/** The `relatedDocuments` map of a diagnostic report, keyed by document URI. */
public object RelatedDocumentsSerializer :
    KSerializer<Map<DocumentUri, RelatedDocumentReport>> by MapSerializer(
        String.serializer(),
        RelatedDocumentReportSerializer,
    )

/**
 * A partial-result chunk of `textDocument/diagnostic`: a [DocumentDiagnosticReport] (an object with a `kind`)
 * or a [DocumentDiagnosticReportPartialResult] (an object without one).
 */
public object DocumentDiagnosticReportProgressSerializer :
    KSerializer<DocumentDiagnosticReportProgress> by strictUnion(
        "DocumentDiagnosticReportProgress",
        DocumentDiagnosticReportSerializer,
        DocumentDiagnosticReportPartialResult.serializer(),
        JsonElement::isObjectWithKind,
        JsonElement::isObjectWithoutKind,
    )

// Provider unions of ServerCapabilities: strict by JSON kind, never lenient.

internal object DeclarationProviderSer :
    KSerializer<Either3<Boolean, DeclarationOptions, DeclarationRegistrationOptions>> by booleanOrOptionsOrRegistration(
        "BooleanOrDeclarationOptionsOrDeclarationRegistrationOptions",
        DeclarationOptions.serializer(),
        DeclarationRegistrationOptions.serializer(),
    )

internal object TypeDefinitionProviderSer :
    KSerializer<Either3<Boolean, TypeDefinitionOptions, TypeDefinitionRegistrationOptions>> by booleanOrOptionsOrRegistration(
        "BooleanOrTypeDefinitionOptionsOrTypeDefinitionRegistrationOptions",
        TypeDefinitionOptions.serializer(),
        TypeDefinitionRegistrationOptions.serializer(),
    )

internal object ImplementationProviderSer :
    KSerializer<Either3<Boolean, ImplementationOptions, ImplementationRegistrationOptions>> by booleanOrOptionsOrRegistration(
        "BooleanOrImplementationOptionsOrImplementationRegistrationOptions",
        ImplementationOptions.serializer(),
        ImplementationRegistrationOptions.serializer(),
    )

internal object ColorProviderSer :
    KSerializer<Either3<Boolean, DocumentColorOptions, DocumentColorRegistrationOptions>> by booleanOrOptionsOrRegistration(
        "BooleanOrDocumentColorOptionsOrDocumentColorRegistrationOptions",
        DocumentColorOptions.serializer(),
        DocumentColorRegistrationOptions.serializer(),
    )

internal object FoldingRangeProviderSer :
    KSerializer<Either3<Boolean, FoldingRangeOptions, FoldingRangeRegistrationOptions>> by booleanOrOptionsOrRegistration(
        "BooleanOrFoldingRangeOptionsOrFoldingRangeRegistrationOptions",
        FoldingRangeOptions.serializer(),
        FoldingRangeRegistrationOptions.serializer(),
    )

internal object SelectionRangeProviderSer :
    KSerializer<Either3<Boolean, SelectionRangeOptions, SelectionRangeRegistrationOptions>> by booleanOrOptionsOrRegistration(
        "BooleanOrSelectionRangeOptionsOrSelectionRangeRegistrationOptions",
        SelectionRangeOptions.serializer(),
        SelectionRangeRegistrationOptions.serializer(),
    )

internal object CallHierarchyProviderSer :
    KSerializer<Either3<Boolean, CallHierarchyOptions, CallHierarchyRegistrationOptions>> by booleanOrOptionsOrRegistration(
        "BooleanOrCallHierarchyOptionsOrCallHierarchyRegistrationOptions",
        CallHierarchyOptions.serializer(),
        CallHierarchyRegistrationOptions.serializer(),
    )

internal object LinkedEditingRangeProviderSer :
    KSerializer<Either3<Boolean, LinkedEditingRangeOptions, LinkedEditingRangeRegistrationOptions>> by booleanOrOptionsOrRegistration(
        "BooleanOrLinkedEditingRangeOptionsOrLinkedEditingRangeRegistrationOptions",
        LinkedEditingRangeOptions.serializer(),
        LinkedEditingRangeRegistrationOptions.serializer(),
    )

internal object MonikerProviderSer :
    KSerializer<Either3<Boolean, MonikerOptions, MonikerRegistrationOptions>> by booleanOrOptionsOrRegistration(
        "BooleanOrMonikerOptionsOrMonikerRegistrationOptions",
        MonikerOptions.serializer(),
        MonikerRegistrationOptions.serializer(),
    )

internal object TypeHierarchyProviderSer :
    KSerializer<Either3<Boolean, TypeHierarchyOptions, TypeHierarchyRegistrationOptions>> by booleanOrOptionsOrRegistration(
        "BooleanOrTypeHierarchyOptionsOrTypeHierarchyRegistrationOptions",
        TypeHierarchyOptions.serializer(),
        TypeHierarchyRegistrationOptions.serializer(),
    )

internal object InlineValueProviderSer :
    KSerializer<Either3<Boolean, InlineValueOptions, InlineValueRegistrationOptions>> by booleanOrOptionsOrRegistration(
        "BooleanOrInlineValueOptionsOrInlineValueRegistrationOptions",
        InlineValueOptions.serializer(),
        InlineValueRegistrationOptions.serializer(),
    )

internal object InlayHintProviderSer :
    KSerializer<Either3<Boolean, InlayHintOptions, InlayHintRegistrationOptions>> by booleanOrOptionsOrRegistration(
        "BooleanOrInlayHintOptionsOrInlayHintRegistrationOptions",
        InlayHintOptions.serializer(),
        InlayHintRegistrationOptions.serializer(),
    )

internal object HoverProviderSer :
    KSerializer<Either<Boolean, HoverOptions>> by booleanOrOptions("BooleanOrHoverOptions", HoverOptions.serializer())

internal object DefinitionProviderSer :
    KSerializer<Either<Boolean, DefinitionOptions>> by booleanOrOptions("BooleanOrDefinitionOptions", DefinitionOptions.serializer())

internal object ReferencesProviderSer :
    KSerializer<Either<Boolean, ReferencesOptions>> by booleanOrOptions("BooleanOrReferencesOptions", ReferencesOptions.serializer())

internal object DocumentHighlightProviderSer :
    KSerializer<Either<Boolean, DocumentHighlightOptions>> by booleanOrOptions(
        "BooleanOrDocumentHighlightOptions",
        DocumentHighlightOptions.serializer(),
    )

internal object DocumentSymbolProviderSer :
    KSerializer<Either<Boolean, DocumentSymbolOptions>> by booleanOrOptions(
        "BooleanOrDocumentSymbolOptions",
        DocumentSymbolOptions.serializer(),
    )

internal object CodeActionProviderSer :
    KSerializer<Either<Boolean, CodeActionOptions>> by booleanOrOptions("BooleanOrCodeActionOptions", CodeActionOptions.serializer())

internal object DocumentFormattingProviderSer :
    KSerializer<Either<Boolean, DocumentFormattingOptions>> by booleanOrOptions(
        "BooleanOrDocumentFormattingOptions",
        DocumentFormattingOptions.serializer(),
    )

internal object DocumentRangeFormattingProviderSer :
    KSerializer<Either<Boolean, DocumentRangeFormattingOptions>> by booleanOrOptions(
        "BooleanOrDocumentRangeFormattingOptions",
        DocumentRangeFormattingOptions.serializer(),
    )

internal object RenameProviderSer :
    KSerializer<Either<Boolean, RenameOptions>> by booleanOrOptions("BooleanOrRenameOptions", RenameOptions.serializer())

internal object InlineCompletionProviderSer :
    KSerializer<Either<Boolean, InlineCompletionOptions>> by booleanOrOptions(
        "BooleanOrInlineCompletionOptions",
        InlineCompletionOptions.serializer(),
    )

internal object WorkspaceSymbolProviderSer :
    KSerializer<Either<Boolean, WorkspaceSymbolOptions>> by booleanOrOptions(
        "BooleanOrWorkspaceSymbolOptions",
        WorkspaceSymbolOptions.serializer(),
    )

internal object SemanticTokensProviderSer :
    KSerializer<Either<SemanticTokensOptions, SemanticTokensRegistrationOptions>> by optionsOrRegistration(
        "SemanticTokensOptionsOrRegistrationOptions",
        SemanticTokensOptions.serializer(),
        SemanticTokensRegistrationOptions.serializer(),
    )

internal object DiagnosticProviderSer :
    KSerializer<Either<DiagnosticOptions, DiagnosticRegistrationOptions>> by optionsOrRegistration(
        "DiagnosticOptionsOrRegistrationOptions",
        DiagnosticOptions.serializer(),
        DiagnosticRegistrationOptions.serializer(),
    )

internal object NotebookDocumentSyncProviderSer :
    KSerializer<Either<NotebookDocumentSyncOptions, NotebookDocumentSyncRegistrationOptions>> by optionsOrRegistration(
        "NotebookDocumentSyncOptionsOrRegistrationOptions",
        NotebookDocumentSyncOptions.serializer(),
        NotebookDocumentSyncRegistrationOptions.serializer(),
        "id",
    )

/** `DidChangeConfigurationRegistrationOptions.section`: `string | string[]`. */
internal object ConfigurationSectionSer : KSerializer<Either<String, List<String>>> by strictUnion(
    "StringOrStringArray",
    String.serializer(),
    ListSerializer(String.serializer()),
    JsonElement::isStringLiteral,
    JsonElement::isArray,
)
