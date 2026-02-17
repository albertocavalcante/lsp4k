package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Tests for ClientCapabilities and related types including:
 * - TextDocumentClientCapabilities and nested capabilities
 * - WorkspaceClientCapabilities and nested capabilities
 * - WindowClientCapabilities
 * - GeneralClientCapabilities
 * - Various capability enums
 */
class ClientCapabilitiesSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== ResourceOperationKind Tests ====================

    @Test
    fun `ResourceOperationKind serialization`() {
        json.encodeToString(ResourceOperationKind.Create) shouldBe "\"create\""
        json.encodeToString(ResourceOperationKind.Rename) shouldBe "\"rename\""
        json.encodeToString(ResourceOperationKind.Delete) shouldBe "\"delete\""
    }

    @Test
    fun `ResourceOperationKind deserialization`() {
        json.decodeFromString<ResourceOperationKind>("\"create\"") shouldBe ResourceOperationKind.Create
        json.decodeFromString<ResourceOperationKind>("\"rename\"") shouldBe ResourceOperationKind.Rename
        json.decodeFromString<ResourceOperationKind>("\"delete\"") shouldBe ResourceOperationKind.Delete
    }

    // ==================== FailureHandlingKind Tests ====================

    @Test
    fun `FailureHandlingKind serialization`() {
        json.encodeToString(FailureHandlingKind.Abort) shouldBe "\"abort\""
        json.encodeToString(FailureHandlingKind.Transactional) shouldBe "\"transactional\""
        json.encodeToString(FailureHandlingKind.Undo) shouldBe "\"undo\""
        json.encodeToString(FailureHandlingKind.TextOnlyTransactional) shouldBe "\"textOnlyTransactional\""
    }

    @Test
    fun `FailureHandlingKind deserialization`() {
        json.decodeFromString<FailureHandlingKind>("\"abort\"") shouldBe FailureHandlingKind.Abort
        json.decodeFromString<FailureHandlingKind>("\"transactional\"") shouldBe FailureHandlingKind.Transactional
        json.decodeFromString<FailureHandlingKind>("\"undo\"") shouldBe FailureHandlingKind.Undo
        json.decodeFromString<FailureHandlingKind>("\"textOnlyTransactional\"") shouldBe FailureHandlingKind.TextOnlyTransactional
    }

    // ==================== PrepareSupportDefaultBehavior Tests ====================

    @Test
    fun `PrepareSupportDefaultBehavior serializes as integer`() {
        val encoded = json.encodeToString(PrepareSupportDefaultBehavior.Identifier)
        encoded shouldBe "1"
    }

    @Test
    fun `PrepareSupportDefaultBehavior deserializes from integer`() {
        json.decodeFromString<PrepareSupportDefaultBehavior>("1") shouldBe PrepareSupportDefaultBehavior.Identifier
    }

    @Test
    fun `PrepareSupportDefaultBehavior fromValue throws for unknown`() {
        assertFailsWith<IllegalArgumentException> {
            PrepareSupportDefaultBehavior.fromValue(99)
        }
    }

    // ==================== TokenFormat Tests ====================

    @Test
    fun `TokenFormat serialization`() {
        json.encodeToString(TokenFormat.Relative) shouldBe "\"relative\""
    }

    @Test
    fun `TokenFormat deserialization`() {
        json.decodeFromString<TokenFormat>("\"relative\"") shouldBe TokenFormat.Relative
    }

    // ==================== WorkspaceEditClientCapabilities Tests ====================

    @Test
    fun `WorkspaceEditClientCapabilities minimal`() {
        val caps = WorkspaceEditClientCapabilities()
        val encoded = json.encodeToString(caps)
        encoded shouldBe "{}"
    }

    @Test
    fun `WorkspaceEditClientCapabilities full`() {
        val caps = WorkspaceEditClientCapabilities(
            documentChanges = true,
            resourceOperations = listOf(
                ResourceOperationKind.Create,
                ResourceOperationKind.Rename,
                ResourceOperationKind.Delete,
            ),
            failureHandling = FailureHandlingKind.Transactional,
            normalizesLineEndings = true,
            changeAnnotationSupport = ChangeAnnotationSupport(groupsOnLabel = true),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<WorkspaceEditClientCapabilities>(encoded)
        decoded.documentChanges shouldBe true
        decoded.resourceOperations?.size shouldBe 3
        decoded.failureHandling shouldBe FailureHandlingKind.Transactional
        decoded.normalizesLineEndings shouldBe true
        decoded.changeAnnotationSupport?.groupsOnLabel shouldBe true
    }

    // ==================== ChangeAnnotationSupport Tests ====================

    @Test
    fun `ChangeAnnotationSupport serialization`() {
        val support = ChangeAnnotationSupport(groupsOnLabel = true)
        val encoded = json.encodeToString(support)
        encoded shouldContain "\"groupsOnLabel\":true"
    }

    // ==================== WorkspaceClientCapabilities Tests ====================

    @Test
    fun `WorkspaceClientCapabilities minimal`() {
        val caps = WorkspaceClientCapabilities()
        val encoded = json.encodeToString(caps)
        encoded shouldBe "{}"
    }

    @Test
    fun `WorkspaceClientCapabilities full`() {
        val caps = WorkspaceClientCapabilities(
            applyEdit = true,
            workspaceEdit = WorkspaceEditClientCapabilities(documentChanges = true),
            didChangeConfiguration = DidChangeConfigurationClientCapabilities(dynamicRegistration = true),
            didChangeWatchedFiles = DidChangeWatchedFilesClientCapabilities(
                dynamicRegistration = true,
                relativePatternSupport = true,
            ),
            symbol = WorkspaceSymbolClientCapabilities(
                dynamicRegistration = true,
                symbolKind = SymbolKindCapability(
                    valueSet = listOf(SymbolKind.File, SymbolKind.Class, SymbolKind.Function),
                ),
            ),
            executeCommand = ExecuteCommandClientCapabilities(dynamicRegistration = true),
            workspaceFolders = true,
            configuration = true,
            semanticTokens = SemanticTokensWorkspaceClientCapabilities(refreshSupport = true),
            codeLens = CodeLensWorkspaceClientCapabilities(refreshSupport = true),
            fileOperations = FileOperationClientCapabilities(
                dynamicRegistration = true,
                didCreate = true,
                willCreate = true,
                didRename = true,
                willRename = true,
                didDelete = true,
                willDelete = true,
            ),
            inlineValue = InlineValueWorkspaceClientCapabilities(refreshSupport = true),
            inlayHint = InlayHintWorkspaceClientCapabilities(refreshSupport = true),
            diagnostics = DiagnosticWorkspaceClientCapabilities(refreshSupport = true),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<WorkspaceClientCapabilities>(encoded)
        decoded.applyEdit shouldBe true
        decoded.workspaceFolders shouldBe true
        decoded.configuration shouldBe true
        decoded.semanticTokens?.refreshSupport shouldBe true
    }

    // ==================== TextDocumentClientCapabilities Tests ====================

    @Test
    fun `TextDocumentClientCapabilities minimal`() {
        val caps = TextDocumentClientCapabilities()
        val encoded = json.encodeToString(caps)
        encoded shouldBe "{}"
    }

    @Test
    fun `TextDocumentClientCapabilities with synchronization`() {
        val caps = TextDocumentClientCapabilities(
            synchronization = TextDocumentSyncClientCapabilities(
                dynamicRegistration = true,
                willSave = true,
                willSaveWaitUntil = true,
                didSave = true,
            ),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<TextDocumentClientCapabilities>(encoded)
        decoded.synchronization?.dynamicRegistration shouldBe true
        decoded.synchronization?.willSave shouldBe true
        decoded.synchronization?.willSaveWaitUntil shouldBe true
        decoded.synchronization?.didSave shouldBe true
    }

    @Test
    fun `TextDocumentClientCapabilities with completion`() {
        val caps = TextDocumentClientCapabilities(
            completion = CompletionClientCapabilities(
                dynamicRegistration = true,
                completionItem = CompletionItemCapability(
                    snippetSupport = true,
                    commitCharactersSupport = true,
                    documentationFormat = listOf(MarkupKind.Markdown, MarkupKind.PlainText),
                    deprecatedSupport = true,
                    preselectSupport = true,
                    tagSupport = CompletionItemTagSupport(valueSet = listOf(CompletionItemTag.Deprecated)),
                    insertReplaceSupport = true,
                    resolveSupport = CompletionItemResolveSupport(properties = listOf("documentation", "detail")),
                    insertTextModeSupport = InsertTextModeSupport(valueSet = listOf(InsertTextMode.AsIs)),
                    labelDetailsSupport = true,
                ),
                completionItemKind = CompletionItemKindCapability(
                    valueSet = listOf(CompletionItemKind.Text, CompletionItemKind.Method),
                ),
                contextSupport = true,
                insertTextMode = InsertTextMode.AdjustIndentation,
                completionList = CompletionListCapabilities(itemDefaults = listOf("commitCharacters")),
            ),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<TextDocumentClientCapabilities>(encoded)
        decoded.completion?.completionItem?.snippetSupport shouldBe true
        decoded.completion?.contextSupport shouldBe true
    }

    @Test
    fun `TextDocumentClientCapabilities with hover`() {
        val caps = TextDocumentClientCapabilities(
            hover = HoverClientCapabilities(
                dynamicRegistration = true,
                contentFormat = listOf(MarkupKind.Markdown, MarkupKind.PlainText),
            ),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<TextDocumentClientCapabilities>(encoded)
        decoded.hover?.dynamicRegistration shouldBe true
        decoded.hover?.contentFormat shouldBe listOf(MarkupKind.Markdown, MarkupKind.PlainText)
    }

    @Test
    fun `TextDocumentClientCapabilities with signature help`() {
        val caps = TextDocumentClientCapabilities(
            signatureHelp = SignatureHelpClientCapabilities(
                dynamicRegistration = true,
                signatureInformation = SignatureInformationCapability(
                    documentationFormat = listOf(MarkupKind.Markdown),
                    parameterInformation = ParameterInformationCapability(labelOffsetSupport = true),
                    activeParameterSupport = true,
                ),
                contextSupport = true,
            ),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<TextDocumentClientCapabilities>(encoded)
        decoded.signatureHelp?.contextSupport shouldBe true
        decoded.signatureHelp?.signatureInformation?.activeParameterSupport shouldBe true
    }

    @Test
    fun `TextDocumentClientCapabilities with goto capabilities`() {
        val caps = TextDocumentClientCapabilities(
            declaration = GotoCapability(dynamicRegistration = true, linkSupport = true),
            definition = GotoCapability(dynamicRegistration = true, linkSupport = true),
            typeDefinition = GotoCapability(dynamicRegistration = true),
            implementation = GotoCapability(dynamicRegistration = true, linkSupport = false),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<TextDocumentClientCapabilities>(encoded)
        decoded.declaration?.linkSupport shouldBe true
        decoded.definition?.linkSupport shouldBe true
        decoded.typeDefinition?.dynamicRegistration shouldBe true
        decoded.implementation?.linkSupport shouldBe false
    }

    @Test
    fun `TextDocumentClientCapabilities with document symbol`() {
        val caps = TextDocumentClientCapabilities(
            documentSymbol = DocumentSymbolClientCapabilities(
                dynamicRegistration = true,
                symbolKind = SymbolKindCapability(
                    valueSet = SymbolKind.entries,
                ),
                hierarchicalDocumentSymbolSupport = true,
                tagSupport = SymbolTagSupport(valueSet = listOf(SymbolTag.Deprecated)),
                labelSupport = true,
            ),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<TextDocumentClientCapabilities>(encoded)
        decoded.documentSymbol?.hierarchicalDocumentSymbolSupport shouldBe true
        decoded.documentSymbol?.symbolKind?.valueSet?.size shouldBe 26
    }

    @Test
    fun `TextDocumentClientCapabilities with code action`() {
        val caps = TextDocumentClientCapabilities(
            codeAction = CodeActionClientCapabilities(
                dynamicRegistration = true,
                codeActionLiteralSupport = CodeActionLiteralSupport(
                    codeActionKind = CodeActionKindCapability(
                        valueSet = listOf(CodeActionKind.QuickFix, CodeActionKind.Refactor),
                    ),
                ),
                isPreferredSupport = true,
                disabledSupport = true,
                dataSupport = true,
                resolveSupport = CodeActionResolveSupport(properties = listOf("edit")),
                honorsChangeAnnotations = true,
            ),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<TextDocumentClientCapabilities>(encoded)
        decoded.codeAction?.isPreferredSupport shouldBe true
        decoded.codeAction?.disabledSupport shouldBe true
    }

    @Test
    fun `TextDocumentClientCapabilities with publish diagnostics`() {
        val caps = TextDocumentClientCapabilities(
            publishDiagnostics = PublishDiagnosticsClientCapabilities(
                relatedInformation = true,
                tagSupport = DiagnosticTagSupport(
                    valueSet = listOf(DiagnosticTag.Unnecessary, DiagnosticTag.Deprecated),
                ),
                versionSupport = true,
                codeDescriptionSupport = true,
                dataSupport = true,
            ),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<TextDocumentClientCapabilities>(encoded)
        decoded.publishDiagnostics?.relatedInformation shouldBe true
        decoded.publishDiagnostics?.tagSupport?.valueSet?.size shouldBe 2
    }

    @Test
    fun `TextDocumentClientCapabilities with folding range`() {
        val caps = TextDocumentClientCapabilities(
            foldingRange = FoldingRangeClientCapabilities(
                dynamicRegistration = true,
                rangeLimit = 5000,
                lineFoldingOnly = true,
                foldingRangeKind = FoldingRangeKindCapability(
                    valueSet = listOf(FoldingRangeKind.Comment, FoldingRangeKind.Imports, FoldingRangeKind.Region),
                ),
                foldingRange = FoldingRangeCapability(collapsedText = true),
            ),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<TextDocumentClientCapabilities>(encoded)
        decoded.foldingRange?.rangeLimit shouldBe 5000
        decoded.foldingRange?.lineFoldingOnly shouldBe true
        decoded.foldingRange?.foldingRange?.collapsedText shouldBe true
    }

    @Test
    fun `TextDocumentClientCapabilities with rename`() {
        val caps = TextDocumentClientCapabilities(
            rename = RenameClientCapabilities(
                dynamicRegistration = true,
                prepareSupport = true,
                prepareSupportDefaultBehavior = PrepareSupportDefaultBehavior.Identifier,
                honorsChangeAnnotations = true,
            ),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<TextDocumentClientCapabilities>(encoded)
        decoded.rename?.prepareSupport shouldBe true
        decoded.rename?.prepareSupportDefaultBehavior shouldBe PrepareSupportDefaultBehavior.Identifier
    }

    @Test
    fun `TextDocumentClientCapabilities with semantic tokens`() {
        val caps = TextDocumentClientCapabilities(
            semanticTokens = SemanticTokensClientCapabilities(
                dynamicRegistration = true,
                requests = SemanticTokensRequests(
                    range = true,
                    full = SemanticTokensFullRequestCapability(delta = true),
                ),
                tokenTypes = listOf("namespace", "type", "class", "enum", "function"),
                tokenModifiers = listOf("declaration", "definition", "readonly"),
                formats = listOf(TokenFormat.Relative),
                overlappingTokenSupport = true,
                multilineTokenSupport = true,
                serverCancelSupport = true,
                augmentsSyntaxTokens = true,
            ),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<TextDocumentClientCapabilities>(encoded)
        decoded.semanticTokens?.requests?.range shouldBe true
        decoded.semanticTokens?.requests?.full?.delta shouldBe true
        decoded.semanticTokens?.tokenTypes?.size shouldBe 5
    }

    @Test
    fun `TextDocumentClientCapabilities with inlay hints`() {
        val caps = TextDocumentClientCapabilities(
            inlayHint = InlayHintClientCapabilities(
                dynamicRegistration = true,
                resolveSupport = InlayHintResolveSupport(properties = listOf("tooltip", "label")),
            ),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<TextDocumentClientCapabilities>(encoded)
        decoded.inlayHint?.resolveSupport?.properties shouldBe listOf("tooltip", "label")
    }

    @Test
    fun `TextDocumentClientCapabilities with diagnostic`() {
        val caps = TextDocumentClientCapabilities(
            diagnostic = DiagnosticClientCapabilities(
                dynamicRegistration = true,
                relatedDocumentSupport = true,
            ),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<TextDocumentClientCapabilities>(encoded)
        decoded.diagnostic?.relatedDocumentSupport shouldBe true
    }

    // ==================== WindowClientCapabilities Tests ====================

    @Test
    fun `WindowClientCapabilities minimal`() {
        val caps = WindowClientCapabilities()
        val encoded = json.encodeToString(caps)
        encoded shouldBe "{}"
    }

    @Test
    fun `WindowClientCapabilities full`() {
        val caps = WindowClientCapabilities(
            workDoneProgress = true,
            showMessage = ShowMessageRequestClientCapabilities(
                messageActionItem = MessageActionItemCapabilities(additionalPropertiesSupport = true),
            ),
            showDocument = ShowDocumentClientCapabilities(support = true),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<WindowClientCapabilities>(encoded)
        decoded.workDoneProgress shouldBe true
        decoded.showMessage?.messageActionItem?.additionalPropertiesSupport shouldBe true
        decoded.showDocument?.support shouldBe true
    }

    // ==================== GeneralClientCapabilities Tests ====================

    @Test
    fun `GeneralClientCapabilities minimal`() {
        val caps = GeneralClientCapabilities()
        val encoded = json.encodeToString(caps)
        encoded shouldBe "{}"
    }

    @Test
    fun `GeneralClientCapabilities full`() {
        val caps = GeneralClientCapabilities(
            staleRequestSupport = StaleRequestSupportCapability(
                cancel = true,
                retryOnContentModified = listOf("textDocument/completion", "textDocument/hover"),
            ),
            regularExpressions = RegularExpressionsClientCapabilities(
                engine = "ECMAScript",
                version = "ES2020",
            ),
            markdown = MarkdownClientCapabilities(
                parser = "marked",
                version = "1.0.0",
                allowedTags = listOf("a", "code", "pre"),
            ),
            positionEncodings = listOf("utf-16", "utf-32"),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<GeneralClientCapabilities>(encoded)
        decoded.staleRequestSupport?.cancel shouldBe true
        decoded.regularExpressions?.engine shouldBe "ECMAScript"
        decoded.markdown?.parser shouldBe "marked"
        decoded.positionEncodings shouldBe listOf("utf-16", "utf-32")
    }

    // ==================== Full ClientCapabilities Tests ====================

    @Test
    fun `ClientCapabilities minimal`() {
        val caps = ClientCapabilities()
        val encoded = json.encodeToString(caps)
        encoded shouldBe "{}"
    }

    @Test
    fun `ClientCapabilities full round-trip`() {
        val caps = ClientCapabilities(
            workspace = WorkspaceClientCapabilities(
                applyEdit = true,
                workspaceFolders = true,
            ),
            textDocument = TextDocumentClientCapabilities(
                hover = HoverClientCapabilities(dynamicRegistration = true),
            ),
            window = WindowClientCapabilities(workDoneProgress = true),
            general = GeneralClientCapabilities(
                positionEncodings = listOf("utf-16"),
            ),
        )
        val encoded = json.encodeToString(caps)
        val decoded = json.decodeFromString<ClientCapabilities>(encoded)
        decoded.workspace?.applyEdit shouldBe true
        decoded.textDocument?.hover?.dynamicRegistration shouldBe true
        decoded.window?.workDoneProgress shouldBe true
        decoded.general?.positionEncodings shouldBe listOf("utf-16")
    }
}
