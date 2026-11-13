package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Wire-shape tests that use a Json equivalent to jsonrpc4k `JsonRpcCodec.defaultJson`
 * (`encodeDefaults = false`, `explicitNulls` left at its default of true).
 */
class WireCorrectnessTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            isLenient = true
            coerceInputValues = true
        }

    // ==================== LP-03: literal discriminators ====================

    @Test
    fun `work done progress kinds are always encoded`() {
        json.encodeToString(WorkDoneProgressBegin(title = "t")) shouldBe """{"kind":"begin","title":"t"}"""
        json.encodeToString(WorkDoneProgressReport()) shouldBe """{"kind":"report"}"""
        json.encodeToString(WorkDoneProgressEnd()) shouldBe """{"kind":"end"}"""
    }

    @Test
    fun `resource operation kinds are always encoded`() {
        json.encodeToString(CreateFile(uri = "file:///a")) shouldBe """{"kind":"create","uri":"file:///a"}"""
        json.encodeToString(RenameFile(oldUri = "file:///a", newUri = "file:///b")) shouldBe
            """{"kind":"rename","oldUri":"file:///a","newUri":"file:///b"}"""
        json.encodeToString(DeleteFile(uri = "file:///a")) shouldBe """{"kind":"delete","uri":"file:///a"}"""
    }

    @Test
    fun `string value kind is always encoded`() {
        json.encodeToString(StringValue(value = "x")) shouldBe """{"kind":"snippet","value":"x"}"""
    }

    @Test
    fun `diagnostic report kinds are always encoded`() {
        json.encodeToString(FullDocumentDiagnosticReport(items = emptyList())) shouldBe
            """{"kind":"full","items":[]}"""
        json.encodeToString(UnchangedDocumentDiagnosticReport(resultId = "r")) shouldBe
            """{"kind":"unchanged","resultId":"r"}"""
        json.encodeToString(RelatedFullDocumentDiagnosticReport(items = emptyList())) shouldBe
            """{"kind":"full","items":[]}"""
        json.encodeToString(RelatedUnchangedDocumentDiagnosticReport(resultId = "r")) shouldBe
            """{"kind":"unchanged","resultId":"r"}"""
        json.encodeToString(WorkspaceFullDocumentDiagnosticReport(items = emptyList(), uri = "file:///a", version = 1)) shouldBe
            """{"kind":"full","items":[],"uri":"file:///a","version":1}"""
        json.encodeToString(WorkspaceUnchangedDocumentDiagnosticReport(resultId = "r", uri = "file:///a", version = 1)) shouldBe
            """{"kind":"unchanged","resultId":"r","uri":"file:///a","version":1}"""
    }

    // ==================== LP-05: required `T | null` ====================

    @Test
    fun `required nullable properties are encoded as null`() {
        json.encodeToString(OptionalVersionedTextDocumentIdentifier(uri = "file:///a", version = null)) shouldBe
            """{"uri":"file:///a","version":null}"""
        json.encodeToString(WorkspaceUnchangedDocumentDiagnosticReport(resultId = "r", uri = "file:///a", version = null)) shouldBe
            """{"kind":"unchanged","resultId":"r","uri":"file:///a","version":null}"""
        json.encodeToString(WorkspaceFullDocumentDiagnosticReport(items = emptyList(), uri = "file:///a", version = null)) shouldBe
            """{"kind":"full","items":[],"uri":"file:///a","version":null}"""
        json.encodeToString(
            DiagnosticRegistrationOptions(
                documentSelector = null,
                interFileDependencies = false,
                workspaceDiagnostics = false,
            ),
        ) shouldBe """{"documentSelector":null,"interFileDependencies":false,"workspaceDiagnostics":false}"""
    }

    // ==================== LP-04, LP-29, LP-41: enums and constants ====================

    @Test
    fun `inline completion trigger kind uses 1 and 2`() {
        json.encodeToString(InlineCompletionTriggerKind.Invoked) shouldBe "1"
        json.encodeToString(InlineCompletionTriggerKind.Automatic) shouldBe "2"
        json.decodeFromString<InlineCompletionTriggerKind>("2") shouldBe InlineCompletionTriggerKind.Automatic
        assertFailsWith<IllegalArgumentException> { json.decodeFromString<InlineCompletionTriggerKind>("0") }
    }

    @Test
    fun `message type has debug`() {
        json.encodeToString(MessageType.Debug) shouldBe "5"
        json.decodeFromString<MessageType>("5") shouldBe MessageType.Debug
    }

    @Test
    fun `error code constants match the meta model`() {
        ErrorCodes.PARSE_ERROR shouldBe -32700
        ErrorCodes.INVALID_REQUEST shouldBe -32600
        ErrorCodes.METHOD_NOT_FOUND shouldBe -32601
        ErrorCodes.INVALID_PARAMS shouldBe -32602
        ErrorCodes.INTERNAL_ERROR shouldBe -32603
        ErrorCodes.SERVER_NOT_INITIALIZED shouldBe -32002
        ErrorCodes.UNKNOWN_ERROR_CODE shouldBe -32001
        LSPErrorCodes.REQUEST_FAILED shouldBe -32803
        LSPErrorCodes.SERVER_CANCELLED shouldBe -32802
        LSPErrorCodes.CONTENT_MODIFIED shouldBe -32801
        LSPErrorCodes.REQUEST_CANCELLED shouldBe -32800
    }

    // ==================== LP-07..LP-10: capability unions ====================

    @Test
    fun `textDocumentSync accepts number and options`() {
        val number = ServerCapabilities(textDocumentSync = Either.left(TextDocumentSyncKind.Incremental))
        json.encodeToString(number) shouldBe """{"textDocumentSync":2}"""
        json.decodeFromString<ServerCapabilities>("""{"textDocumentSync":2}""") shouldBe number

        val options =
            ServerCapabilities(
                textDocumentSync = Either.right(TextDocumentSyncOptions(openClose = true)),
            )
        json.encodeToString(options) shouldBe """{"textDocumentSync":{"openClose":true}}"""
        json.decodeFromString<ServerCapabilities>("""{"textDocumentSync":{"openClose":true}}""") shouldBe options

        assertFailsWith<SerializationException> {
            json.decodeFromString<ServerCapabilities>("""{"textDocumentSync":"full"}""")
        }
    }

    @Test
    fun `save accepts boolean and options`() {
        val boolean = TextDocumentSyncOptions(save = Either.left(true))
        json.encodeToString(boolean) shouldBe """{"save":true}"""
        json.decodeFromString<TextDocumentSyncOptions>("""{"save":true}""") shouldBe boolean

        val options = TextDocumentSyncOptions(save = Either.right(SaveOptions(includeText = true)))
        json.encodeToString(options) shouldBe """{"save":{"includeText":true}}"""
        json.decodeFromString<TextDocumentSyncOptions>("""{"save":{"includeText":true}}""") shouldBe options

        assertFailsWith<SerializationException> { json.decodeFromString<TextDocumentSyncOptions>("""{"save":"yes"}""") }
    }

    @Test
    fun `semantic tokens server options accept boolean and object forms`() {
        val legend = SemanticTokensLegend(tokenTypes = emptyList(), tokenModifiers = emptyList())
        val legendJson = """"legend":{"tokenTypes":[],"tokenModifiers":[]}"""

        val booleans = SemanticTokensOptions(legend = legend, range = Either.left(true), full = Either.left(true))
        json.encodeToString(booleans) shouldBe """{$legendJson,"range":true,"full":true}"""
        json.decodeFromString<SemanticTokensOptions>("""{$legendJson,"range":true,"full":true}""") shouldBe booleans

        val objects =
            SemanticTokensOptions(
                legend = legend,
                range = Either.right(SemanticTokensRangeOptions),
                full = Either.right(SemanticTokensFullOptions(delta = true)),
            )
        json.encodeToString(objects) shouldBe """{$legendJson,"range":{},"full":{"delta":true}}"""
        json.decodeFromString<SemanticTokensOptions>("""{$legendJson,"range":{},"full":{"delta":true}}""") shouldBe objects

        assertFailsWith<SerializationException> {
            json.decodeFromString<SemanticTokensOptions>("""{$legendJson,"full":1}""")
        }
    }

    @Test
    fun `semantic tokens client requests accept boolean and object forms`() {
        val booleans = SemanticTokensRequests(range = Either.left(true), full = Either.left(false))
        json.encodeToString(booleans) shouldBe """{"range":true,"full":false}"""
        json.decodeFromString<SemanticTokensRequests>("""{"range":true,"full":false}""") shouldBe booleans

        val objects =
            SemanticTokensRequests(
                range = Either.right(SemanticTokensRangeOptions),
                full = Either.right(SemanticTokensFullRequestCapability(delta = true)),
            )
        json.encodeToString(objects) shouldBe """{"range":{},"full":{"delta":true}}"""
        json.decodeFromString<SemanticTokensRequests>("""{"range":{},"full":{"delta":true}}""") shouldBe objects
    }

    @Test
    fun `changeNotifications accepts string and boolean`() {
        val string = WorkspaceFoldersServerCapabilities(supported = true, changeNotifications = Either.left("id"))
        json.encodeToString(string) shouldBe """{"supported":true,"changeNotifications":"id"}"""
        json.decodeFromString<WorkspaceFoldersServerCapabilities>("""{"supported":true,"changeNotifications":"id"}""") shouldBe string

        val boolean = WorkspaceFoldersServerCapabilities(changeNotifications = Either.right(true))
        json.encodeToString(boolean) shouldBe """{"changeNotifications":true}"""
        json.decodeFromString<WorkspaceFoldersServerCapabilities>("""{"changeNotifications":true}""") shouldBe boolean

        assertFailsWith<SerializationException> {
            json.decodeFromString<WorkspaceFoldersServerCapabilities>("""{"changeNotifications":1}""")
        }
    }
}
