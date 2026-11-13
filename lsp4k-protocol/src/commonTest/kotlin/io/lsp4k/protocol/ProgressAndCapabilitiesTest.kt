package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Golden-JSON tests for the progress token mixins (`workDoneToken`, `partialResultToken`,
 * `workDoneProgress`), the small client capability additions and the open string kinds.
 * Uses a Json equivalent to jsonrpc4k `JsonRpcCodec.defaultJson`.
 */
class ProgressAndCapabilitiesTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            isLenient = true
            coerceInputValues = true
        }

    private val doc = TextDocumentIdentifier(uri = "file:///a")

    // ==================== progress tokens ====================

    @Test
    fun `document color params carry both tokens`() {
        val params =
            DocumentColorParams(
                textDocument = doc,
                workDoneToken = Either.right("w"),
                partialResultToken = Either.left(7),
            )
        val encoded = json.encodeToString(params)
        encoded shouldBe """{"textDocument":{"uri":"file:///a"},"workDoneToken":"w","partialResultToken":7}"""
        json.decodeFromString<DocumentColorParams>(encoded) shouldBe params
    }

    @Test
    fun `tokens are omitted when absent`() {
        json.encodeToString(DocumentColorParams(textDocument = doc)) shouldBe """{"textDocument":{"uri":"file:///a"}}"""
    }

    @Test
    fun `document highlight params decode both tokens`() {
        val decoded =
            json.decodeFromString<DocumentHighlightParams>(
                """{"textDocument":{"uri":"file:///a"},"position":{"line":1,"character":2},""" +
                    """"workDoneToken":1,"partialResultToken":"p"}""",
            )
        decoded.workDoneToken shouldBe Either.left(1)
        decoded.partialResultToken shouldBe Either.right("p")
    }

    @Test
    fun `execute command params carry only a work done token`() {
        val params = ExecuteCommandParams(command = "c", workDoneToken = Either.right("w"))
        val encoded = json.encodeToString(params)
        encoded shouldBe """{"command":"c","workDoneToken":"w"}"""
        json.decodeFromString<ExecuteCommandParams>(encoded) shouldBe params
    }

    @Test
    fun `initialize params decode a work done token`() {
        val decoded =
            json.decodeFromString<InitializeParams>(
                """{"processId":null,"rootUri":null,"capabilities":{},"workDoneToken":"init"}""",
            )
        decoded.workDoneToken shouldBe Either.right("init")
    }

    @Test
    fun `workspace symbol params decode both tokens`() {
        val decoded =
            json.decodeFromString<WorkspaceSymbolParams>("""{"query":"q","workDoneToken":3,"partialResultToken":4}""")
        decoded.workDoneToken shouldBe Either.left(3)
        decoded.partialResultToken shouldBe Either.left(4)
    }

    @Test
    fun `options carry workDoneProgress`() {
        json.encodeToString(CodeLensOptions(workDoneProgress = true)) shouldBe """{"workDoneProgress":true}"""
        json.decodeFromString<CompletionOptions>("""{"workDoneProgress":true}""").workDoneProgress shouldBe true
        json.decodeFromString<SignatureHelpOptions>("""{"workDoneProgress":false}""").workDoneProgress shouldBe false
        json.decodeFromString<DocumentLinkOptions>("""{"workDoneProgress":true}""").workDoneProgress shouldBe true
        val semantic =
            json.decodeFromString<SemanticTokensOptions>(
                """{"legend":{"tokenTypes":[],"tokenModifiers":[]},"workDoneProgress":true}""",
            )
        semantic.workDoneProgress shouldBe true
    }

    // ==================== client capabilities ====================

    @Test
    fun `inline completion client capabilities round trip`() {
        val caps =
            ClientCapabilities(
                textDocument =
                    TextDocumentClientCapabilities(
                        inlineCompletion = InlineCompletionClientCapabilities(dynamicRegistration = true),
                    ),
            )
        val encoded = json.encodeToString(caps)
        encoded shouldBe """{"textDocument":{"inlineCompletion":{"dynamicRegistration":true}}}"""
        json.decodeFromString<ClientCapabilities>(encoded) shouldBe caps
    }

    @Test
    fun `notebook document client capabilities round trip`() {
        val caps =
            ClientCapabilities(
                notebookDocument =
                    NotebookDocumentClientCapabilities(
                        synchronization =
                            NotebookDocumentSyncClientCapabilities(
                                dynamicRegistration = true,
                                executionSummarySupport = true,
                            ),
                    ),
            )
        val encoded = json.encodeToString(caps)
        encoded shouldBe
            """{"notebookDocument":{"synchronization":{"dynamicRegistration":true,"executionSummarySupport":true}}}"""
        json.decodeFromString<ClientCapabilities>(encoded) shouldBe caps
    }

    @Test
    fun `workspace folding range capabilities round trip`() {
        val caps = FoldingRangeWorkspaceClientCapabilities(refreshSupport = true)
        val encoded = json.encodeToString(WorkspaceClientCapabilities(foldingRange = caps))
        encoded shouldBe """{"foldingRange":{"refreshSupport":true}}"""
        json.decodeFromString<WorkspaceClientCapabilities>(encoded).foldingRange?.refreshSupport shouldBe true
    }

    @Test
    fun `code lens resolve support and no active parameter support decode`() {
        val lens =
            json.decodeFromString<CodeLensClientCapabilities>("""{"resolveSupport":{"properties":["command"]}}""")
        lens.resolveSupport shouldBe ClientCodeLensResolveOptions(properties = listOf("command"))
        json
            .decodeFromString<SignatureInformationCapability>("""{"noActiveParameterSupport":true}""")
            .noActiveParameterSupport shouldBe true
    }

    @Test
    fun `document range formatting options advertise rangesSupport`() {
        val options = DocumentRangeFormattingOptions(rangesSupport = true)
        val encoded = json.encodeToString(options)
        encoded shouldBe """{"rangesSupport":true}"""
        json.decodeFromString<DocumentRangeFormattingOptions>(encoded) shouldBe options
    }

    @Test
    fun `symbol tag support requires valueSet`() {
        val encoded = json.encodeToString(SymbolTagSupport(valueSet = listOf(SymbolTag.Deprecated)))
        encoded shouldBe """{"valueSet":[1]}"""
        json.decodeFromString<SymbolTagSupport>(encoded).valueSet shouldBe listOf(SymbolTag.Deprecated)
    }

    // ==================== open string kinds ====================

    @Test
    fun `folding range kind accepts custom values`() {
        json.encodeToString(FoldingRangeKind.Comment) shouldBe "\"comment\""
        json.decodeFromString<FoldingRangeKind>("\"custom\"") shouldBe FoldingRangeKind("custom")
        json.encodeToString(FoldingRangeKind("custom")) shouldBe "\"custom\""
        json.decodeFromString<FoldingRange>("""{"startLine":0,"endLine":1,"kind":"imports"}""").kind shouldBe
            FoldingRangeKind.Imports
    }

    @Test
    fun `position encoding kind accepts custom values`() {
        json.encodeToString(PositionEncodingKind.UTF8) shouldBe "\"utf-8\""
        json.decodeFromString<PositionEncodingKind>("\"utf-16\"") shouldBe PositionEncodingKind.UTF16
        json.decodeFromString<PositionEncodingKind>("\"utf-64\"") shouldBe PositionEncodingKind("utf-64")
        json
            .decodeFromString<GeneralClientCapabilities>("""{"positionEncodings":["utf-32","x"]}""")
            .positionEncodings shouldBe listOf(PositionEncodingKind.UTF32, PositionEncodingKind("x"))
        json.encodeToString(ServerCapabilities(positionEncoding = PositionEncodingKind.UTF8)) shouldBe
            """{"positionEncoding":"utf-8"}"""
    }

    @Test
    fun `language kind constants and custom values`() {
        LanguageKind.Python.value shouldBe "python"
        LanguageKind.WindowsBat.value shouldBe "bat"
        LanguageKind.Pug.value shouldBe "jade"
        json.encodeToString(LanguageKind.TypeScriptReact) shouldBe "\"typescriptreact\""
        json.decodeFromString<LanguageKind>("\"starlark\"") shouldBe LanguageKind("starlark")
    }
}
