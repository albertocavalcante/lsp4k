package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private fun JsonElement.asObject(what: String): JsonObject = this as? JsonObject ?: throw SerializationException("$what must be an object")

private fun JsonElement.asArray(what: String): JsonArray = this as? JsonArray ?: throw SerializationException("$what must be an array")

// =============================================================================
// (Command | CodeAction)[]
// =============================================================================

/** One element of a `textDocument/codeAction` result, which is `(Command | CodeAction)[]`. */
@Serializable(with = CommandOrCodeActionSerializer::class)
public sealed interface CommandOrCodeAction {
    /** A bare command, whose `command` member is a string. */
    public data class OfCommand(
        public val command: Command,
    ) : CommandOrCodeAction

    /** A code action, whose optional `command` member is an object. */
    public data class OfCodeAction(
        public val codeAction: CodeAction,
    ) : CommandOrCodeAction
}

/**
 * Decides by the JSON kind of the `command` member: a string is a [Command], while an absent,
 * null or object member is a [CodeAction]. Any other kind is rejected.
 */
public object CommandOrCodeActionSerializer : KSerializer<CommandOrCodeAction> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CommandOrCodeAction")

    override fun serialize(
        encoder: Encoder,
        value: CommandOrCodeAction,
    ) {
        val json = encoder as JsonEncoder
        when (value) {
            is CommandOrCodeAction.OfCommand -> json.encodeSerializableValue(Command.serializer(), value.command)
            is CommandOrCodeAction.OfCodeAction -> json.encodeSerializableValue(CodeAction.serializer(), value.codeAction)
        }
    }

    override fun deserialize(decoder: Decoder): CommandOrCodeAction {
        val json = decoder as JsonDecoder
        val element = json.decodeJsonElement().asObject("Command or code action")
        return when (val command = element["command"]) {
            is JsonPrimitive ->
                if (command.isString) {
                    CommandOrCodeAction.OfCommand(json.json.decodeFromJsonElement(Command.serializer(), element))
                } else if (command is JsonNull) {
                    CommandOrCodeAction.OfCodeAction(json.json.decodeFromJsonElement(CodeAction.serializer(), element))
                } else {
                    throw SerializationException("The command member must be a string or an object")
                }
            null, is JsonObject ->
                CommandOrCodeAction.OfCodeAction(json.json.decodeFromJsonElement(CodeAction.serializer(), element))
            else -> throw SerializationException("The command member must be a string or an object")
        }
    }
}

// =============================================================================
// DocumentSymbol[] | SymbolInformation[]
// =============================================================================

/**
 * A `textDocument/documentSymbol` result preserving its hierarchical or flat shape.
 * Empty arrays have identical JSON; decoding `[]` yields [Hierarchical].
 */
@Serializable(with = DocumentSymbolResultSerializer::class)
public sealed interface DocumentSymbolResult {
    /** Hierarchical [DocumentSymbol] values. */
    public data class Hierarchical(
        public val symbols: List<DocumentSymbol>,
    ) : DocumentSymbolResult

    /** Flat [SymbolInformation] values. */
    public data class Flat(
        public val symbols: List<SymbolInformation>,
    ) : DocumentSymbolResult
}

/**
 * Distinguishes [SymbolInformation] by its `location` key from [DocumentSymbol] by its
 * `selectionRange` key. Objects with both or neither key and mixed arrays are rejected.
 */
public object DocumentSymbolResultSerializer : KSerializer<DocumentSymbolResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DocumentSymbolResult")

    override fun serialize(
        encoder: Encoder,
        value: DocumentSymbolResult,
    ) {
        val json = encoder as JsonEncoder
        when (value) {
            is DocumentSymbolResult.Hierarchical ->
                json.encodeSerializableValue(ListSerializer(DocumentSymbol.serializer()), value.symbols)
            is DocumentSymbolResult.Flat ->
                json.encodeSerializableValue(ListSerializer(SymbolInformation.serializer()), value.symbols)
        }
    }

    override fun deserialize(decoder: Decoder): DocumentSymbolResult {
        val json = decoder as JsonDecoder
        val element = json.decodeJsonElement().asArray("Document symbol result")
        if (element.isEmpty()) return DocumentSymbolResult.Hierarchical(emptyList())
        val flags = element.map { isFlat(it.asObject("Document symbol")) }
        if (flags.any { it != flags.first() }) throw SerializationException("Document symbol array cannot mix symbol shapes")
        return if (flags.first()) {
            DocumentSymbolResult.Flat(json.json.decodeFromJsonElement(ListSerializer(SymbolInformation.serializer()), element))
        } else {
            DocumentSymbolResult.Hierarchical(json.json.decodeFromJsonElement(ListSerializer(DocumentSymbol.serializer()), element))
        }
    }

    private fun isFlat(symbol: JsonObject): Boolean {
        val hasLocation = "location" in symbol
        val hasSelectionRange = "selectionRange" in symbol
        if (hasLocation == hasSelectionRange) throw SerializationException("Document symbol has missing or ambiguous shape keys")
        return hasLocation
    }
}

// =============================================================================
// SymbolInformation[] | WorkspaceSymbol[]
// =============================================================================

/**
 * A `workspace/symbol` result preserving its [SymbolInformation] or [WorkspaceSymbol] shape.
 * A [WorkspaceSymbol] whose location has a range and which carries no `data` is wire-identical to a
 * [SymbolInformation] without `deprecated`; such arrays, including `[]`, decode as [Symbols].
 */
@Serializable(with = WorkspaceSymbolResultSerializer::class)
public sealed interface WorkspaceSymbolResult {
    /** Legacy [SymbolInformation] values. */
    public data class Information(
        public val symbols: List<SymbolInformation>,
    ) : WorkspaceSymbolResult

    /** [WorkspaceSymbol] values, which may have URI-only locations and resolve data. */
    public data class Symbols(
        public val symbols: List<WorkspaceSymbol>,
    ) : WorkspaceSymbolResult
}

/**
 * Decides per element: a `deprecated` key means [SymbolInformation], while a URI-only `location` or a `data`
 * key means [WorkspaceSymbol]. Elements with conflicting evidence and arrays mixing both shapes are rejected.
 */
public object WorkspaceSymbolResultSerializer : KSerializer<WorkspaceSymbolResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("WorkspaceSymbolResult")

    override fun serialize(
        encoder: Encoder,
        value: WorkspaceSymbolResult,
    ) {
        val json = encoder as JsonEncoder
        when (value) {
            is WorkspaceSymbolResult.Information ->
                json.encodeSerializableValue(ListSerializer(SymbolInformation.serializer()), value.symbols)
            is WorkspaceSymbolResult.Symbols ->
                json.encodeSerializableValue(ListSerializer(WorkspaceSymbol.serializer()), value.symbols)
        }
    }

    override fun deserialize(decoder: Decoder): WorkspaceSymbolResult {
        val json = decoder as JsonDecoder
        val element = json.decodeJsonElement().asArray("Workspace symbol result")
        val evidence = element.mapNotNull { evidence(it.asObject("Workspace symbol")) }.toSet()
        if (evidence.size > 1) throw SerializationException("Workspace symbol array cannot mix SymbolInformation and WorkspaceSymbol")
        return if (evidence.singleOrNull() == true) {
            WorkspaceSymbolResult.Information(json.json.decodeFromJsonElement(ListSerializer(SymbolInformation.serializer()), element))
        } else {
            WorkspaceSymbolResult.Symbols(json.json.decodeFromJsonElement(ListSerializer(WorkspaceSymbol.serializer()), element))
        }
    }

    /** Returns true for legacy evidence, false for workspace-symbol evidence and null when ambiguous. */
    private fun evidence(symbol: JsonObject): Boolean? {
        val location = symbol["location"]?.asObject("Workspace symbol location")
        val legacy = "deprecated" in symbol
        val modern = (location != null && "range" !in location) || "data" in symbol
        return when {
            legacy && modern -> throw SerializationException("Workspace symbol has conflicting shape keys")
            legacy -> true
            modern -> false
            else -> null
        }
    }
}

// =============================================================================
// InlineCompletionItem[] | InlineCompletionList
// =============================================================================

/** A `textDocument/inlineCompletion` result preserving whether the server sent an array or a list object. */
@Serializable(with = InlineCompletionResultSerializer::class)
public sealed interface InlineCompletionResult {
    /** A complete array of inline completion items. */
    public data class Items(
        public val items: List<InlineCompletionItem>,
    ) : InlineCompletionResult

    /** An inline completion list object. */
    public data class ListResult(
        public val list: InlineCompletionList,
    ) : InlineCompletionResult
}

/** Serializes inline completion results as a JSON array or an `InlineCompletionList` object. */
public object InlineCompletionResultSerializer : KSerializer<InlineCompletionResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("InlineCompletionResult")

    override fun serialize(
        encoder: Encoder,
        value: InlineCompletionResult,
    ) {
        val json = encoder as JsonEncoder
        when (value) {
            is InlineCompletionResult.Items ->
                json.encodeSerializableValue(ListSerializer(InlineCompletionItem.serializer()), value.items)
            is InlineCompletionResult.ListResult -> json.encodeSerializableValue(InlineCompletionList.serializer(), value.list)
        }
    }

    override fun deserialize(decoder: Decoder): InlineCompletionResult {
        val json = decoder as JsonDecoder
        return when (val element = json.decodeJsonElement()) {
            is JsonArray ->
                InlineCompletionResult.Items(json.json.decodeFromJsonElement(ListSerializer(InlineCompletionItem.serializer()), element))
            is JsonObject ->
                InlineCompletionResult.ListResult(json.json.decodeFromJsonElement(InlineCompletionList.serializer(), element))
            else -> throw SerializationException("Inline completion result must be an array or an inline completion list object")
        }
    }
}

// =============================================================================
// SemanticTokens | SemanticTokensDelta
// =============================================================================

/** A `textDocument/semanticTokens/full/delta` result: full [SemanticTokens] or a [SemanticTokensDelta]. */
@Serializable(with = SemanticTokensDeltaResultSerializer::class)
public sealed interface SemanticTokensDeltaResult {
    /** A full set of tokens, which replaces the previous result. */
    public data class Tokens(
        public val tokens: SemanticTokens,
    ) : SemanticTokensDeltaResult

    /** Edits that transform the previous result into a new one. */
    public data class Delta(
        public val delta: SemanticTokensDelta,
    ) : SemanticTokensDeltaResult
}

/** Distinguishes full tokens by the `data` key from a delta by the `edits` key; both or neither is rejected. */
public object SemanticTokensDeltaResultSerializer : KSerializer<SemanticTokensDeltaResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SemanticTokensDeltaResult")

    override fun serialize(
        encoder: Encoder,
        value: SemanticTokensDeltaResult,
    ) {
        val json = encoder as JsonEncoder
        when (value) {
            is SemanticTokensDeltaResult.Tokens -> json.encodeSerializableValue(SemanticTokens.serializer(), value.tokens)
            is SemanticTokensDeltaResult.Delta -> json.encodeSerializableValue(SemanticTokensDelta.serializer(), value.delta)
        }
    }

    override fun deserialize(decoder: Decoder): SemanticTokensDeltaResult {
        val json = decoder as JsonDecoder
        val element = json.decodeJsonElement().asObject("Semantic tokens delta result")
        val hasData = "data" in element
        val hasEdits = "edits" in element
        if (hasData == hasEdits) throw SerializationException("Semantic tokens result must have exactly one of data or edits")
        return if (hasData) {
            SemanticTokensDeltaResult.Tokens(json.json.decodeFromJsonElement(SemanticTokens.serializer(), element))
        } else {
            SemanticTokensDeltaResult.Delta(json.json.decodeFromJsonElement(SemanticTokensDelta.serializer(), element))
        }
    }
}

// =============================================================================
// SemanticTokensPartialResult | SemanticTokensDeltaPartialResult
// =============================================================================

/**
 * A partial result of `textDocument/semanticTokens/full/delta`: a token chunk (`Either.Left`) or an edit chunk
 * (`Either.Right`).
 */
public typealias SemanticTokensDeltaPartial = Either<SemanticTokensPartialResult, SemanticTokensDeltaPartialResult>

/** Distinguishes a token chunk by the `data` key from an edit chunk by the `edits` key; both or neither is rejected. */
public object SemanticTokensDeltaPartialSerializer : KSerializer<SemanticTokensDeltaPartial> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SemanticTokensDeltaPartial")

    override fun serialize(
        encoder: Encoder,
        value: SemanticTokensDeltaPartial,
    ) {
        val json = encoder as JsonEncoder
        when (value) {
            is Either.Left -> json.encodeSerializableValue(SemanticTokensPartialResult.serializer(), value.value)
            is Either.Right -> json.encodeSerializableValue(SemanticTokensDeltaPartialResult.serializer(), value.value)
        }
    }

    override fun deserialize(decoder: Decoder): SemanticTokensDeltaPartial {
        val json = decoder as JsonDecoder
        val element = json.decodeJsonElement().asObject("Semantic tokens partial result")
        val hasData = "data" in element
        val hasEdits = "edits" in element
        if (hasData == hasEdits) throw SerializationException("Semantic tokens partial result must have exactly one of data or edits")
        return if (hasData) {
            Either.Left(json.json.decodeFromJsonElement(SemanticTokensPartialResult.serializer(), element))
        } else {
            Either.Right(json.json.decodeFromJsonElement(SemanticTokensDeltaPartialResult.serializer(), element))
        }
    }
}
