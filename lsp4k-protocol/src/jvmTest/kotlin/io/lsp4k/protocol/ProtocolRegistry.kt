package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import java.lang.reflect.Modifier

/**
 * The explicit table from every LSP 3.18 meta-model item to its lsp4k serializer, checked by
 * [MetaModelConformanceTest]. Add a row when lsp4k gains a type; the checker reports every meta-model item without
 * a row and every row whose name the meta model does not declare.
 */
internal object ProtocolRegistry {
    val registry: ConformanceRegistry by lazy {
        ConformanceRegistry(
            structures = structures(),
            enumerations = enumerations(),
            typeAliases = typeAliases(),
            messages = messages(),
            methodConstants = methodConstants(),
        )
    }

    private fun exact(serializer: KSerializer<*>): StructureBinding = StructureBinding.Exact(serializer)

    private fun merged(serializer: KSerializer<*>): StructureBinding = StructureBinding.Merged(serializer)

    private fun closed(
        serializer: KSerializer<*>,
        entries: List<Any>,
    ): EnumerationBinding = EnumerationBinding.Closed(serializer, entries)

    private fun open(
        serializer: KSerializer<*>,
        holder: Class<*>,
    ): EnumerationBinding = EnumerationBinding.Open(serializer, holder)

    /** The values of the `String` constants of [LspMethods]. */
    private fun methodConstants(): Set<String> =
        LspMethods::class.java.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { it.get(null) as String }
            .toSet()

    /**
     * Abstract structures (only used in `extends` and `mixins`) have no row; their properties are checked in every
     * structure that inherits them. A [StructureBinding.Merged] row maps the members of a union alias that lsp4k
     * represents with one class.
     */
    private fun structures(): Map<String, StructureBinding> =
        mapOf(
            "AnnotatedTextEdit" to exact(AnnotatedTextEdit.serializer()),
            "ApplyWorkspaceEditParams" to exact(ApplyWorkspaceEditParams.serializer()),
            "ApplyWorkspaceEditResult" to exact(ApplyWorkspaceEditResult.serializer()),
            "CallHierarchyClientCapabilities" to exact(CallHierarchyClientCapabilities.serializer()),
            "CallHierarchyIncomingCall" to exact(CallHierarchyIncomingCall.serializer()),
            "CallHierarchyIncomingCallsParams" to exact(CallHierarchyIncomingCallsParams.serializer()),
            "CallHierarchyItem" to exact(CallHierarchyItem.serializer()),
            "CallHierarchyOptions" to exact(CallHierarchyOptions.serializer()),
            "CallHierarchyOutgoingCall" to exact(CallHierarchyOutgoingCall.serializer()),
            "CallHierarchyOutgoingCallsParams" to exact(CallHierarchyOutgoingCallsParams.serializer()),
            "CallHierarchyPrepareParams" to exact(CallHierarchyPrepareParams.serializer()),
            "CallHierarchyRegistrationOptions" to exact(CallHierarchyRegistrationOptions.serializer()),
            "CancelParams" to exact(CancelParams.serializer()),
            "ChangeAnnotation" to exact(ChangeAnnotation.serializer()),
            "ChangeAnnotationsSupportOptions" to exact(ChangeAnnotationSupport.serializer()),
            "ClientCapabilities" to exact(ClientCapabilities.serializer()),
            "ClientCodeActionKindOptions" to exact(CodeActionKindCapability.serializer()),
            "ClientCodeActionLiteralOptions" to exact(CodeActionLiteralSupport.serializer()),
            "ClientCodeActionResolveOptions" to exact(CodeActionResolveSupport.serializer()),
            "ClientCodeLensResolveOptions" to exact(ClientCodeLensResolveOptions.serializer()),
            "ClientCompletionItemInsertTextModeOptions" to exact(InsertTextModeSupport.serializer()),
            "ClientCompletionItemOptions" to exact(CompletionItemCapability.serializer()),
            "ClientCompletionItemOptionsKind" to exact(CompletionItemKindCapability.serializer()),
            "ClientCompletionItemResolveOptions" to exact(CompletionItemResolveSupport.serializer()),
            "ClientDiagnosticsTagOptions" to exact(DiagnosticTagSupport.serializer()),
            "ClientFoldingRangeKindOptions" to exact(FoldingRangeKindCapability.serializer()),
            "ClientFoldingRangeOptions" to exact(FoldingRangeCapability.serializer()),
            "ClientInfo" to exact(ClientInfo.serializer()),
            "ClientInlayHintResolveOptions" to exact(InlayHintResolveSupport.serializer()),
            "ClientSemanticTokensRequestFullDelta" to exact(SemanticTokensFullRequestCapability.serializer()),
            "ClientSemanticTokensRequestOptions" to exact(SemanticTokensRequests.serializer()),
            "ClientShowMessageActionItemOptions" to exact(MessageActionItemCapabilities.serializer()),
            "ClientSignatureInformationOptions" to exact(SignatureInformationCapability.serializer()),
            "ClientSignatureParameterInformationOptions" to exact(ParameterInformationCapability.serializer()),
            "ClientSymbolKindOptions" to exact(SymbolKindCapability.serializer()),
            "ClientSymbolResolveOptions" to exact(WorkspaceSymbolResolveSupport.serializer()),
            "ClientSymbolTagOptions" to exact(SymbolTagSupport.serializer()),
            "CodeAction" to exact(CodeAction.serializer()),
            "CodeActionClientCapabilities" to exact(CodeActionClientCapabilities.serializer()),
            "CodeActionContext" to exact(CodeActionContext.serializer()),
            "CodeActionDisabled" to exact(CodeActionDisabled.serializer()),
            "CodeActionKindDocumentation" to exact(CodeActionKindDocumentation.serializer()),
            "CodeActionOptions" to exact(CodeActionOptions.serializer()),
            "CodeActionParams" to exact(CodeActionParams.serializer()),
            "CodeActionRegistrationOptions" to exact(CodeActionRegistrationOptions.serializer()),
            "CodeActionTagOptions" to exact(CodeActionTagSupport.serializer()),
            "CodeDescription" to exact(CodeDescription.serializer()),
            "CodeLens" to exact(CodeLens.serializer()),
            "CodeLensClientCapabilities" to exact(CodeLensClientCapabilities.serializer()),
            "CodeLensOptions" to exact(CodeLensOptions.serializer()),
            "CodeLensParams" to exact(CodeLensParams.serializer()),
            "CodeLensRegistrationOptions" to exact(CodeLensRegistrationOptions.serializer()),
            "CodeLensWorkspaceClientCapabilities" to exact(CodeLensWorkspaceClientCapabilities.serializer()),
            "Color" to exact(Color.serializer()),
            "ColorInformation" to exact(ColorInformation.serializer()),
            "ColorPresentation" to exact(ColorPresentation.serializer()),
            "ColorPresentationParams" to exact(ColorPresentationParams.serializer()),
            "Command" to exact(Command.serializer()),
            "CompletionClientCapabilities" to exact(CompletionClientCapabilities.serializer()),
            "CompletionContext" to exact(CompletionContext.serializer()),
            "CompletionItem" to exact(CompletionItem.serializer()),
            "CompletionItemApplyKinds" to exact(CompletionItemApplyKinds.serializer()),
            "CompletionItemDefaults" to exact(CompletionItemDefaults.serializer()),
            "CompletionItemLabelDetails" to exact(CompletionItemLabelDetails.serializer()),
            "CompletionItemTagOptions" to exact(CompletionItemTagSupport.serializer()),
            "CompletionList" to exact(CompletionList.serializer()),
            "CompletionListCapabilities" to exact(CompletionListCapabilities.serializer()),
            "CompletionOptions" to exact(CompletionOptions.serializer()),
            "CompletionParams" to exact(CompletionParams.serializer()),
            "CompletionRegistrationOptions" to exact(CompletionRegistrationOptions.serializer()),
            "ConfigurationItem" to exact(ConfigurationItem.serializer()),
            "ConfigurationParams" to exact(ConfigurationParams.serializer()),
            "CreateFile" to exact(CreateFile.serializer()),
            "CreateFileOptions" to exact(CreateFileOptions.serializer()),
            "CreateFilesParams" to exact(CreateFilesParams.serializer()),
            "DeclarationClientCapabilities" to exact(GotoCapability.serializer()),
            "DeclarationOptions" to exact(DeclarationOptions.serializer()),
            "DeclarationParams" to exact(DeclarationParams.serializer()),
            "DeclarationRegistrationOptions" to exact(DeclarationRegistrationOptions.serializer()),
            "DefinitionClientCapabilities" to exact(GotoCapability.serializer()),
            "DefinitionOptions" to exact(DefinitionOptions.serializer()),
            "DefinitionParams" to exact(DefinitionParams.serializer()),
            "DefinitionRegistrationOptions" to exact(DefinitionRegistrationOptions.serializer()),
            "DeleteFile" to exact(DeleteFile.serializer()),
            "DeleteFileOptions" to exact(DeleteFileOptions.serializer()),
            "DeleteFilesParams" to exact(DeleteFilesParams.serializer()),
            "Diagnostic" to exact(Diagnostic.serializer()),
            "DiagnosticClientCapabilities" to exact(DiagnosticClientCapabilities.serializer()),
            "DiagnosticOptions" to exact(DiagnosticOptions.serializer()),
            "DiagnosticRegistrationOptions" to exact(DiagnosticRegistrationOptions.serializer()),
            "DiagnosticRelatedInformation" to exact(DiagnosticRelatedInformation.serializer()),
            "DiagnosticServerCancellationData" to exact(DiagnosticServerCancellationData.serializer()),
            "DiagnosticWorkspaceClientCapabilities" to exact(DiagnosticWorkspaceClientCapabilities.serializer()),
            "DidChangeConfigurationClientCapabilities" to exact(DidChangeConfigurationClientCapabilities.serializer()),
            "DidChangeConfigurationParams" to exact(DidChangeConfigurationParams.serializer()),
            "DidChangeConfigurationRegistrationOptions" to exact(DidChangeConfigurationRegistrationOptions.serializer()),
            "DidChangeNotebookDocumentParams" to exact(DidChangeNotebookDocumentParams.serializer()),
            "DidChangeTextDocumentParams" to exact(DidChangeTextDocumentParams.serializer()),
            "DidChangeWatchedFilesClientCapabilities" to exact(DidChangeWatchedFilesClientCapabilities.serializer()),
            "DidChangeWatchedFilesParams" to exact(DidChangeWatchedFilesParams.serializer()),
            "DidChangeWatchedFilesRegistrationOptions" to exact(DidChangeWatchedFilesRegistrationOptions.serializer()),
            "DidChangeWorkspaceFoldersParams" to exact(DidChangeWorkspaceFoldersParams.serializer()),
            "DidCloseNotebookDocumentParams" to exact(DidCloseNotebookDocumentParams.serializer()),
            "DidCloseTextDocumentParams" to exact(DidCloseTextDocumentParams.serializer()),
            "DidOpenNotebookDocumentParams" to exact(DidOpenNotebookDocumentParams.serializer()),
            "DidOpenTextDocumentParams" to exact(DidOpenTextDocumentParams.serializer()),
            "DidSaveNotebookDocumentParams" to exact(DidSaveNotebookDocumentParams.serializer()),
            "DidSaveTextDocumentParams" to exact(DidSaveTextDocumentParams.serializer()),
            "DocumentColorClientCapabilities" to exact(DocumentColorClientCapabilities.serializer()),
            "DocumentColorOptions" to exact(DocumentColorOptions.serializer()),
            "DocumentColorParams" to exact(DocumentColorParams.serializer()),
            "DocumentColorRegistrationOptions" to exact(DocumentColorRegistrationOptions.serializer()),
            "DocumentDiagnosticParams" to exact(DocumentDiagnosticParams.serializer()),
            "DocumentDiagnosticReportPartialResult" to exact(DocumentDiagnosticReportPartialResult.serializer()),
            "DocumentFormattingClientCapabilities" to exact(DocumentFormattingClientCapabilities.serializer()),
            "DocumentFormattingOptions" to exact(DocumentFormattingOptions.serializer()),
            "DocumentFormattingParams" to exact(DocumentFormattingParams.serializer()),
            "DocumentFormattingRegistrationOptions" to exact(DocumentFormattingRegistrationOptions.serializer()),
            "DocumentHighlight" to exact(DocumentHighlight.serializer()),
            "DocumentHighlightClientCapabilities" to exact(DocumentHighlightClientCapabilities.serializer()),
            "DocumentHighlightOptions" to exact(DocumentHighlightOptions.serializer()),
            "DocumentHighlightParams" to exact(DocumentHighlightParams.serializer()),
            "DocumentHighlightRegistrationOptions" to exact(DocumentHighlightRegistrationOptions.serializer()),
            "DocumentLink" to exact(DocumentLink.serializer()),
            "DocumentLinkClientCapabilities" to exact(DocumentLinkClientCapabilities.serializer()),
            "DocumentLinkOptions" to exact(DocumentLinkOptions.serializer()),
            "DocumentLinkParams" to exact(DocumentLinkParams.serializer()),
            "DocumentLinkRegistrationOptions" to exact(DocumentLinkRegistrationOptions.serializer()),
            "DocumentOnTypeFormattingClientCapabilities" to exact(DocumentOnTypeFormattingClientCapabilities.serializer()),
            "DocumentOnTypeFormattingOptions" to exact(DocumentOnTypeFormattingOptions.serializer()),
            "DocumentOnTypeFormattingParams" to exact(DocumentOnTypeFormattingParams.serializer()),
            "DocumentOnTypeFormattingRegistrationOptions" to exact(DocumentOnTypeFormattingRegistrationOptions.serializer()),
            "DocumentRangeFormattingClientCapabilities" to exact(DocumentRangeFormattingClientCapabilities.serializer()),
            "DocumentRangeFormattingOptions" to exact(DocumentRangeFormattingOptions.serializer()),
            "DocumentRangeFormattingParams" to exact(DocumentRangeFormattingParams.serializer()),
            "DocumentRangeFormattingRegistrationOptions" to exact(DocumentRangeFormattingRegistrationOptions.serializer()),
            "DocumentRangesFormattingParams" to exact(DocumentRangesFormattingParams.serializer()),
            "DocumentSymbol" to exact(DocumentSymbol.serializer()),
            "DocumentSymbolClientCapabilities" to exact(DocumentSymbolClientCapabilities.serializer()),
            "DocumentSymbolOptions" to exact(DocumentSymbolOptions.serializer()),
            "DocumentSymbolParams" to exact(DocumentSymbolParams.serializer()),
            "DocumentSymbolRegistrationOptions" to exact(DocumentSymbolRegistrationOptions.serializer()),
            "EditRangeWithInsertReplace" to exact(EditRangeWithInsertReplace.serializer()),
            "ExecuteCommandClientCapabilities" to exact(ExecuteCommandClientCapabilities.serializer()),
            "ExecuteCommandOptions" to exact(ExecuteCommandOptions.serializer()),
            "ExecuteCommandParams" to exact(ExecuteCommandParams.serializer()),
            "ExecuteCommandRegistrationOptions" to exact(ExecuteCommandRegistrationOptions.serializer()),
            "ExecutionSummary" to exact(ExecutionSummary.serializer()),
            "FileCreate" to exact(FileCreate.serializer()),
            "FileDelete" to exact(FileDelete.serializer()),
            "FileEvent" to exact(FileEvent.serializer()),
            "FileOperationClientCapabilities" to exact(FileOperationClientCapabilities.serializer()),
            "FileOperationFilter" to exact(FileOperationFilter.serializer()),
            "FileOperationOptions" to exact(FileOperationOptions.serializer()),
            "FileOperationPattern" to exact(FileOperationPattern.serializer()),
            "FileOperationPatternOptions" to exact(FileOperationPatternOptions.serializer()),
            "FileOperationRegistrationOptions" to exact(FileOperationRegistrationOptions.serializer()),
            "FileRename" to exact(FileRename.serializer()),
            "FileSystemWatcher" to exact(FileSystemWatcher.serializer()),
            "FoldingRange" to exact(FoldingRange.serializer()),
            "FoldingRangeClientCapabilities" to exact(FoldingRangeClientCapabilities.serializer()),
            "FoldingRangeOptions" to exact(FoldingRangeOptions.serializer()),
            "FoldingRangeParams" to exact(FoldingRangeParams.serializer()),
            "FoldingRangeRegistrationOptions" to exact(FoldingRangeRegistrationOptions.serializer()),
            "FoldingRangeWorkspaceClientCapabilities" to exact(FoldingRangeWorkspaceClientCapabilities.serializer()),
            "FormattingOptions" to exact(FormattingOptions.serializer()),
            "FullDocumentDiagnosticReport" to exact(FullDocumentDiagnosticReport.serializer()),
            "GeneralClientCapabilities" to exact(GeneralClientCapabilities.serializer()),
            "Hover" to exact(Hover.serializer()),
            "HoverClientCapabilities" to exact(HoverClientCapabilities.serializer()),
            "HoverOptions" to exact(HoverOptions.serializer()),
            "HoverParams" to exact(HoverParams.serializer()),
            "HoverRegistrationOptions" to exact(HoverRegistrationOptions.serializer()),
            "ImplementationClientCapabilities" to exact(GotoCapability.serializer()),
            "ImplementationOptions" to exact(ImplementationOptions.serializer()),
            "ImplementationParams" to exact(ImplementationParams.serializer()),
            "ImplementationRegistrationOptions" to exact(ImplementationRegistrationOptions.serializer()),
            "InitializeError" to exact(InitializeError.serializer()),
            "InitializeParams" to exact(InitializeParams.serializer()),
            "InitializeResult" to exact(InitializeResult.serializer()),
            "InitializedParams" to exact(InitializedParams.serializer()),
            "InlayHint" to exact(InlayHint.serializer()),
            "InlayHintClientCapabilities" to exact(InlayHintClientCapabilities.serializer()),
            "InlayHintLabelPart" to exact(InlayHintLabelPart.serializer()),
            "InlayHintOptions" to exact(InlayHintOptions.serializer()),
            "InlayHintParams" to exact(InlayHintParams.serializer()),
            "InlayHintRegistrationOptions" to exact(InlayHintRegistrationOptions.serializer()),
            "InlayHintWorkspaceClientCapabilities" to exact(InlayHintWorkspaceClientCapabilities.serializer()),
            "InlineCompletionClientCapabilities" to exact(InlineCompletionClientCapabilities.serializer()),
            "InlineCompletionContext" to exact(InlineCompletionContext.serializer()),
            "InlineCompletionItem" to exact(InlineCompletionItem.serializer()),
            "InlineCompletionList" to exact(InlineCompletionList.serializer()),
            "InlineCompletionOptions" to exact(InlineCompletionOptions.serializer()),
            "InlineCompletionParams" to exact(InlineCompletionParams.serializer()),
            "InlineCompletionRegistrationOptions" to exact(InlineCompletionRegistrationOptions.serializer()),
            "InlineValueClientCapabilities" to exact(InlineValueClientCapabilities.serializer()),
            "InlineValueContext" to exact(InlineValueContext.serializer()),
            "InlineValueEvaluatableExpression" to exact(InlineValueEvaluatableExpression.serializer()),
            "InlineValueOptions" to exact(InlineValueOptions.serializer()),
            "InlineValueParams" to exact(InlineValueParams.serializer()),
            "InlineValueRegistrationOptions" to exact(InlineValueRegistrationOptions.serializer()),
            "InlineValueText" to exact(InlineValueText.serializer()),
            "InlineValueVariableLookup" to exact(InlineValueVariableLookup.serializer()),
            "InlineValueWorkspaceClientCapabilities" to exact(InlineValueWorkspaceClientCapabilities.serializer()),
            "InsertReplaceEdit" to exact(InsertReplaceEdit.serializer()),
            "LinkedEditingRangeClientCapabilities" to exact(LinkedEditingRangeClientCapabilities.serializer()),
            "LinkedEditingRangeOptions" to exact(LinkedEditingRangeOptions.serializer()),
            "LinkedEditingRangeParams" to exact(LinkedEditingRangeParams.serializer()),
            "LinkedEditingRangeRegistrationOptions" to exact(LinkedEditingRangeRegistrationOptions.serializer()),
            "LinkedEditingRanges" to exact(LinkedEditingRanges.serializer()),
            "Location" to exact(Location.serializer()),
            "LocationLink" to exact(LocationLink.serializer()),
            "LocationUriOnly" to exact(WorkspaceSymbolUriLocation.serializer()),
            "LogMessageParams" to exact(LogMessageParams.serializer()),
            "LogTraceParams" to exact(LogTraceParams.serializer()),
            "MarkdownClientCapabilities" to exact(MarkdownClientCapabilities.serializer()),
            "MarkedStringWithLanguage" to exact(MarkedString.serializer()),
            "MarkupContent" to exact(MarkupContent.serializer()),
            "MessageActionItem" to exact(MessageActionItem.serializer()),
            "Moniker" to exact(Moniker.serializer()),
            "MonikerClientCapabilities" to exact(MonikerClientCapabilities.serializer()),
            "MonikerOptions" to exact(MonikerOptions.serializer()),
            "MonikerParams" to exact(MonikerParams.serializer()),
            "MonikerRegistrationOptions" to exact(MonikerRegistrationOptions.serializer()),
            "NotebookCell" to exact(NotebookCell.serializer()),
            "NotebookCellArrayChange" to exact(NotebookCellArrayChange.serializer()),
            "NotebookCellLanguage" to exact(NotebookCellSelector.serializer()),
            "NotebookCellTextDocumentFilter" to exact(NotebookCellTextDocumentFilter.serializer()),
            "NotebookDocument" to exact(NotebookDocument.serializer()),
            "NotebookDocumentCellChangeStructure" to exact(NotebookDocumentCellChangeStructure.serializer()),
            "NotebookDocumentCellChanges" to exact(NotebookDocumentCellChanges.serializer()),
            "NotebookDocumentCellContentChanges" to exact(NotebookDocumentCellContentChanges.serializer()),
            "NotebookDocumentChangeEvent" to exact(NotebookDocumentChangeEvent.serializer()),
            "NotebookDocumentClientCapabilities" to exact(NotebookDocumentClientCapabilities.serializer()),
            "NotebookDocumentFilterNotebookType" to merged(NotebookDocumentFilter.serializer()),
            "NotebookDocumentFilterPattern" to merged(NotebookDocumentFilter.serializer()),
            "NotebookDocumentFilterScheme" to merged(NotebookDocumentFilter.serializer()),
            "NotebookDocumentFilterWithCells" to merged(NotebookSelector.serializer()),
            "NotebookDocumentFilterWithNotebook" to merged(NotebookSelector.serializer()),
            "NotebookDocumentIdentifier" to exact(NotebookDocumentIdentifier.serializer()),
            "NotebookDocumentSyncClientCapabilities" to exact(NotebookDocumentSyncClientCapabilities.serializer()),
            "NotebookDocumentSyncOptions" to exact(NotebookDocumentSyncOptions.serializer()),
            "NotebookDocumentSyncRegistrationOptions" to exact(NotebookDocumentSyncRegistrationOptions.serializer()),
            "OptionalVersionedTextDocumentIdentifier" to exact(OptionalVersionedTextDocumentIdentifier.serializer()),
            "ParameterInformation" to exact(ParameterInformation.serializer()),
            "Position" to exact(Position.serializer()),
            "PrepareRenameDefaultBehavior" to exact(PrepareRenameDefaultBehavior.serializer()),
            "PrepareRenameParams" to exact(PrepareRenameParams.serializer()),
            "PrepareRenamePlaceholder" to exact(PrepareRenamePlaceholder.serializer()),
            "PreviousResultId" to exact(PreviousResultId.serializer()),
            "ProgressParams" to exact(ProgressParams.serializer(JsonElement.serializer())),
            "PublishDiagnosticsClientCapabilities" to exact(PublishDiagnosticsClientCapabilities.serializer()),
            "PublishDiagnosticsParams" to exact(PublishDiagnosticsParams.serializer()),
            "Range" to exact(Range.serializer()),
            "ReferenceClientCapabilities" to exact(ReferenceClientCapabilities.serializer()),
            "ReferenceContext" to exact(ReferenceContext.serializer()),
            "ReferenceOptions" to exact(ReferencesOptions.serializer()),
            "ReferenceParams" to exact(ReferenceParams.serializer()),
            "ReferenceRegistrationOptions" to exact(ReferenceRegistrationOptions.serializer()),
            "Registration" to exact(Registration.serializer()),
            "RegistrationParams" to exact(RegistrationParams.serializer()),
            "RegularExpressionsClientCapabilities" to exact(RegularExpressionsClientCapabilities.serializer()),
            "RelatedFullDocumentDiagnosticReport" to exact(RelatedFullDocumentDiagnosticReport.serializer()),
            "RelatedUnchangedDocumentDiagnosticReport" to exact(RelatedUnchangedDocumentDiagnosticReport.serializer()),
            "RelativePattern" to exact(RelativePattern.serializer()),
            "RenameClientCapabilities" to exact(RenameClientCapabilities.serializer()),
            "RenameFile" to exact(RenameFile.serializer()),
            "RenameFileOptions" to exact(RenameFileOptions.serializer()),
            "RenameFilesParams" to exact(RenameFilesParams.serializer()),
            "RenameOptions" to exact(RenameOptions.serializer()),
            "RenameParams" to exact(RenameParams.serializer()),
            "RenameRegistrationOptions" to exact(RenameRegistrationOptions.serializer()),
            "SaveOptions" to exact(SaveOptions.serializer()),
            "SelectedCompletionInfo" to exact(SelectedCompletionInfo.serializer()),
            "SelectionRange" to exact(SelectionRange.serializer()),
            "SelectionRangeClientCapabilities" to exact(SelectionRangeClientCapabilities.serializer()),
            "SelectionRangeOptions" to exact(SelectionRangeOptions.serializer()),
            "SelectionRangeParams" to exact(SelectionRangeParams.serializer()),
            "SelectionRangeRegistrationOptions" to exact(SelectionRangeRegistrationOptions.serializer()),
            "SemanticTokens" to exact(SemanticTokens.serializer()),
            "SemanticTokensClientCapabilities" to exact(SemanticTokensClientCapabilities.serializer()),
            "SemanticTokensDelta" to exact(SemanticTokensDelta.serializer()),
            "SemanticTokensDeltaParams" to exact(SemanticTokensDeltaParams.serializer()),
            "SemanticTokensDeltaPartialResult" to exact(SemanticTokensDeltaPartialResult.serializer()),
            "SemanticTokensEdit" to exact(SemanticTokensEdit.serializer()),
            "SemanticTokensFullDelta" to exact(SemanticTokensFullOptions.serializer()),
            "SemanticTokensLegend" to exact(SemanticTokensLegend.serializer()),
            "SemanticTokensOptions" to exact(SemanticTokensOptions.serializer()),
            "SemanticTokensParams" to exact(SemanticTokensParams.serializer()),
            "SemanticTokensPartialResult" to exact(SemanticTokensPartialResult.serializer()),
            "SemanticTokensRangeParams" to exact(SemanticTokensRangeParams.serializer()),
            "SemanticTokensRegistrationOptions" to exact(SemanticTokensRegistrationOptions.serializer()),
            "SemanticTokensWorkspaceClientCapabilities" to exact(SemanticTokensWorkspaceClientCapabilities.serializer()),
            "ServerCapabilities" to exact(ServerCapabilities.serializer()),
            "ServerCompletionItemOptions" to exact(ServerCompletionItemOptions.serializer()),
            "ServerInfo" to exact(ServerInfo.serializer()),
            "SetTraceParams" to exact(SetTraceParams.serializer()),
            "ShowDocumentClientCapabilities" to exact(ShowDocumentClientCapabilities.serializer()),
            "ShowDocumentParams" to exact(ShowDocumentParams.serializer()),
            "ShowDocumentResult" to exact(ShowDocumentResult.serializer()),
            "ShowMessageParams" to exact(ShowMessageParams.serializer()),
            "ShowMessageRequestClientCapabilities" to exact(ShowMessageRequestClientCapabilities.serializer()),
            "ShowMessageRequestParams" to exact(ShowMessageRequestParams.serializer()),
            "SignatureHelp" to exact(SignatureHelp.serializer()),
            "SignatureHelpClientCapabilities" to exact(SignatureHelpClientCapabilities.serializer()),
            "SignatureHelpContext" to exact(SignatureHelpContext.serializer()),
            "SignatureHelpOptions" to exact(SignatureHelpOptions.serializer()),
            "SignatureHelpParams" to exact(SignatureHelpParams.serializer()),
            "SignatureHelpRegistrationOptions" to exact(SignatureHelpRegistrationOptions.serializer()),
            "SignatureInformation" to exact(SignatureInformation.serializer()),
            "SnippetTextEdit" to exact(SnippetTextEdit.serializer()),
            "StaleRequestSupportOptions" to exact(StaleRequestSupportCapability.serializer()),
            "StringValue" to exact(StringValue.serializer()),
            "SymbolInformation" to exact(SymbolInformation.serializer()),
            "TextDocumentChangeRegistrationOptions" to exact(TextDocumentChangeRegistrationOptions.serializer()),
            "TextDocumentClientCapabilities" to exact(TextDocumentClientCapabilities.serializer()),
            "TextDocumentContentChangePartial" to merged(TextDocumentContentChangeEvent.serializer()),
            "TextDocumentContentChangeWholeDocument" to merged(TextDocumentContentChangeEvent.serializer()),
            "TextDocumentContentClientCapabilities" to exact(TextDocumentContentClientCapabilities.serializer()),
            "TextDocumentContentOptions" to exact(TextDocumentContentOptions.serializer()),
            "TextDocumentContentParams" to exact(TextDocumentContentParams.serializer()),
            "TextDocumentContentRefreshParams" to exact(TextDocumentContentRefreshParams.serializer()),
            "TextDocumentContentRegistrationOptions" to exact(TextDocumentContentRegistrationOptions.serializer()),
            "TextDocumentContentResult" to exact(TextDocumentContentResult.serializer()),
            "TextDocumentEdit" to exact(TextDocumentEdit.serializer()),
            "TextDocumentFilterClientCapabilities" to exact(TextDocumentFilterClientCapabilities.serializer()),
            "TextDocumentFilterLanguage" to merged(TextDocumentFilter.serializer()),
            "TextDocumentFilterPattern" to merged(TextDocumentFilter.serializer()),
            "TextDocumentFilterScheme" to merged(TextDocumentFilter.serializer()),
            "TextDocumentIdentifier" to exact(TextDocumentIdentifier.serializer()),
            "TextDocumentItem" to exact(TextDocumentItem.serializer()),
            "TextDocumentPositionParams" to exact(TextDocumentPositionParams.serializer()),
            "TextDocumentRegistrationOptions" to exact(TextDocumentRegistrationOptions.serializer()),
            "TextDocumentSaveRegistrationOptions" to exact(TextDocumentSaveRegistrationOptions.serializer()),
            "TextDocumentSyncClientCapabilities" to exact(TextDocumentSyncClientCapabilities.serializer()),
            "TextDocumentSyncOptions" to exact(TextDocumentSyncOptions.serializer()),
            "TextEdit" to exact(TextEdit.serializer()),
            "TypeDefinitionClientCapabilities" to exact(GotoCapability.serializer()),
            "TypeDefinitionOptions" to exact(TypeDefinitionOptions.serializer()),
            "TypeDefinitionParams" to exact(TypeDefinitionParams.serializer()),
            "TypeDefinitionRegistrationOptions" to exact(TypeDefinitionRegistrationOptions.serializer()),
            "TypeHierarchyClientCapabilities" to exact(TypeHierarchyClientCapabilities.serializer()),
            "TypeHierarchyItem" to exact(TypeHierarchyItem.serializer()),
            "TypeHierarchyOptions" to exact(TypeHierarchyOptions.serializer()),
            "TypeHierarchyPrepareParams" to exact(TypeHierarchyPrepareParams.serializer()),
            "TypeHierarchyRegistrationOptions" to exact(TypeHierarchyRegistrationOptions.serializer()),
            "TypeHierarchySubtypesParams" to exact(TypeHierarchySubtypesParams.serializer()),
            "TypeHierarchySupertypesParams" to exact(TypeHierarchySupertypesParams.serializer()),
            "UnchangedDocumentDiagnosticReport" to exact(UnchangedDocumentDiagnosticReport.serializer()),
            "Unregistration" to exact(Unregistration.serializer()),
            "UnregistrationParams" to exact(UnregistrationParams.serializer()),
            "VersionedNotebookDocumentIdentifier" to exact(VersionedNotebookDocumentIdentifier.serializer()),
            "VersionedTextDocumentIdentifier" to exact(VersionedTextDocumentIdentifier.serializer()),
            "WillSaveTextDocumentParams" to exact(WillSaveTextDocumentParams.serializer()),
            "WindowClientCapabilities" to exact(WindowClientCapabilities.serializer()),
            "WorkDoneProgressBegin" to exact(WorkDoneProgressBegin.serializer()),
            "WorkDoneProgressCancelParams" to exact(WorkDoneProgressCancelParams.serializer()),
            "WorkDoneProgressCreateParams" to exact(WorkDoneProgressCreateParams.serializer()),
            "WorkDoneProgressEnd" to exact(WorkDoneProgressEnd.serializer()),
            "WorkDoneProgressReport" to exact(WorkDoneProgressReport.serializer()),
            "WorkspaceClientCapabilities" to exact(WorkspaceClientCapabilities.serializer()),
            "WorkspaceDiagnosticParams" to exact(WorkspaceDiagnosticParams.serializer()),
            "WorkspaceDiagnosticReport" to exact(WorkspaceDiagnosticReport.serializer()),
            "WorkspaceDiagnosticReportPartialResult" to exact(WorkspaceDiagnosticReportPartialResult.serializer()),
            "WorkspaceEdit" to exact(WorkspaceEdit.serializer()),
            "WorkspaceEditClientCapabilities" to exact(WorkspaceEditClientCapabilities.serializer()),
            "WorkspaceEditMetadata" to exact(WorkspaceEditMetadata.serializer()),
            "WorkspaceFolder" to exact(WorkspaceFolder.serializer()),
            "WorkspaceFoldersChangeEvent" to exact(WorkspaceFoldersChangeEvent.serializer()),
            "WorkspaceFoldersServerCapabilities" to exact(WorkspaceFoldersServerCapabilities.serializer()),
            "WorkspaceFullDocumentDiagnosticReport" to exact(WorkspaceFullDocumentDiagnosticReport.serializer()),
            "WorkspaceOptions" to exact(ServerWorkspaceCapabilities.serializer()),
            "WorkspaceSymbol" to exact(WorkspaceSymbol.serializer()),
            "WorkspaceSymbolClientCapabilities" to exact(WorkspaceSymbolClientCapabilities.serializer()),
            "WorkspaceSymbolOptions" to exact(WorkspaceSymbolOptions.serializer()),
            "WorkspaceSymbolParams" to exact(WorkspaceSymbolParams.serializer()),
            "WorkspaceSymbolRegistrationOptions" to exact(WorkspaceSymbolRegistrationOptions.serializer()),
            "WorkspaceUnchangedDocumentDiagnosticReport" to exact(WorkspaceUnchangedDocumentDiagnosticReport.serializer()),
        )

    private fun enumerations(): Map<String, EnumerationBinding> =
        mapOf(
            "ApplyKind" to closed(ApplyKind.serializer(), ApplyKind.entries),
            "CodeActionKind" to open(String.serializer(), CodeActionKind::class.java),
            "CodeActionTag" to closed(CodeActionTag.serializer(), CodeActionTag.entries),
            "CodeActionTriggerKind" to closed(CodeActionTriggerKind.serializer(), CodeActionTriggerKind.entries),
            "CompletionItemKind" to closed(CompletionItemKind.serializer(), CompletionItemKind.entries),
            "CompletionItemTag" to closed(CompletionItemTag.serializer(), CompletionItemTag.entries),
            "CompletionTriggerKind" to closed(CompletionTriggerKind.serializer(), CompletionTriggerKind.entries),
            "DiagnosticSeverity" to closed(DiagnosticSeverity.serializer(), DiagnosticSeverity.entries),
            "DiagnosticTag" to closed(DiagnosticTag.serializer(), DiagnosticTag.entries),
            "DocumentDiagnosticReportKind" to closed(DocumentDiagnosticReportKind.serializer(), DocumentDiagnosticReportKind.entries),
            "DocumentHighlightKind" to closed(DocumentHighlightKind.serializer(), DocumentHighlightKind.entries),
            "ErrorCodes" to open(Int.serializer(), ErrorCodes::class.java),
            "FailureHandlingKind" to closed(FailureHandlingKind.serializer(), FailureHandlingKind.entries),
            "FileChangeType" to closed(FileChangeType.serializer(), FileChangeType.entries),
            "FileOperationPatternKind" to closed(FileOperationPatternKind.serializer(), FileOperationPatternKind.entries),
            "FoldingRangeKind" to open(FoldingRangeKind.serializer(), FoldingRangeKind::class.java),
            "InlayHintKind" to closed(InlayHintKind.serializer(), InlayHintKind.entries),
            "InlineCompletionTriggerKind" to closed(InlineCompletionTriggerKind.serializer(), InlineCompletionTriggerKind.entries),
            "InsertTextFormat" to closed(InsertTextFormat.serializer(), InsertTextFormat.entries),
            "InsertTextMode" to closed(InsertTextMode.serializer(), InsertTextMode.entries),
            "LSPErrorCodes" to open(Int.serializer(), LSPErrorCodes::class.java),
            "LanguageKind" to open(LanguageKind.serializer(), LanguageKind::class.java),
            "MarkupKind" to closed(MarkupKind.serializer(), MarkupKind.entries),
            "MessageType" to closed(MessageType.serializer(), MessageType.entries),
            "MonikerKind" to closed(MonikerKind.serializer(), MonikerKind.entries),
            "NotebookCellKind" to closed(NotebookCellKind.serializer(), NotebookCellKind.entries),
            "PositionEncodingKind" to open(PositionEncodingKind.serializer(), PositionEncodingKind::class.java),
            "PrepareSupportDefaultBehavior" to closed(PrepareSupportDefaultBehavior.serializer(), PrepareSupportDefaultBehavior.entries),
            "ResourceOperationKind" to closed(ResourceOperationKind.serializer(), ResourceOperationKind.entries),
            "SemanticTokenModifiers" to open(String.serializer(), SemanticTokenModifiers::class.java),
            "SemanticTokenTypes" to open(String.serializer(), SemanticTokenTypes::class.java),
            "SignatureHelpTriggerKind" to closed(SignatureHelpTriggerKind.serializer(), SignatureHelpTriggerKind.entries),
            "SymbolKind" to closed(SymbolKind.serializer(), SymbolKind.entries),
            "SymbolTag" to closed(SymbolTag.serializer(), SymbolTag.entries),
            "TextDocumentSaveReason" to closed(TextDocumentSaveReason.serializer(), TextDocumentSaveReason.entries),
            "TextDocumentSyncKind" to closed(TextDocumentSyncKind.serializer(), TextDocumentSyncKind.entries),
            "TokenFormat" to closed(TokenFormat.serializer(), TokenFormat.entries),
            "TraceValue" to closed(TraceValue.serializer(), TraceValue.entries),
            "UniquenessLevel" to closed(UniquenessLevel.serializer(), UniquenessLevel.entries),
            "WatchKind" to open(Int.serializer(), WatchKind::class.java),
        )

    private fun typeAliases(): Map<String, KSerializer<*>> =
        mapOf(
            "ChangeAnnotationIdentifier" to String.serializer(),
            "Declaration" to DefinitionResultSerializer,
            "DeclarationLink" to LocationLink.serializer(),
            "Definition" to DefinitionResultSerializer,
            "DefinitionLink" to LocationLink.serializer(),
            "DocumentDiagnosticReport" to DocumentDiagnosticReportSerializer,
            "DocumentDiagnosticReportProgress" to DocumentDiagnosticReportProgressSerializer,
            "DocumentFilter" to DocumentFilterSerializer,
            "DocumentSelector" to ListSerializer(DocumentFilterSerializer),
            "GlobPattern" to GlobPatternSerializer,
            "InlineValue" to InlineValueSerializer,
            "LSPAny" to JsonElement.serializer(),
            "LSPArray" to ListSerializer(JsonElement.serializer()),
            "LSPObject" to JsonElement.serializer(),
            // The deprecated MarkedString only types Hover.contents, where HoverContents.SingleMarked carries it.
            "MarkedString" to HoverContentsSerializer,
            "NotebookDocumentFilter" to NotebookDocumentFilter.serializer(),
            "Pattern" to String.serializer(),
            "PrepareRenameResult" to PrepareRenameResultSerializer,
            "ProgressToken" to ProgressTokenSerializer,
            "RegularExpressionEngineKind" to String.serializer(),
            "TextDocumentContentChangeEvent" to TextDocumentContentChangeEvent.serializer(),
            "TextDocumentFilter" to TextDocumentFilter.serializer(),
            "WorkspaceDocumentDiagnosticReport" to WorkspaceDocumentDiagnosticReportSerializer,
        )

    private fun messages(): Map<String, MessageBinding> =
        mapOf(
            LspMethods.CANCEL_REQUEST to
                MessageBinding(
                    MessageDirection.Both,
                    params = CancelParams.serializer(),
                ),
            LspMethods.LOG_TRACE to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    params = LogTraceParams.serializer(),
                ),
            LspMethods.PROGRESS to
                MessageBinding(
                    MessageDirection.Both,
                    params = ProgressParams.serializer(JsonElement.serializer()),
                ),
            LspMethods.SET_TRACE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = SetTraceParams.serializer(),
                ),
            LspMethods.CALL_HIERARCHY_INCOMING_CALLS to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = CallHierarchyIncomingCallsParams.serializer(),
                    result = ListSerializer(CallHierarchyIncomingCall.serializer()).nullable,
                    partialResult = ListSerializer(CallHierarchyIncomingCall.serializer()),
                ),
            LspMethods.CALL_HIERARCHY_OUTGOING_CALLS to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = CallHierarchyOutgoingCallsParams.serializer(),
                    result = ListSerializer(CallHierarchyOutgoingCall.serializer()).nullable,
                    partialResult = ListSerializer(CallHierarchyOutgoingCall.serializer()),
                ),
            LspMethods.CLIENT_REGISTER_CAPABILITY to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    params = RegistrationParams.serializer(),
                    result = JsonNull.serializer(),
                ),
            LspMethods.CLIENT_UNREGISTER_CAPABILITY to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    params = UnregistrationParams.serializer(),
                    result = JsonNull.serializer(),
                ),
            LspMethods.CODE_ACTION_RESOLVE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = CodeAction.serializer(),
                    result = CodeAction.serializer(),
                ),
            LspMethods.CODE_LENS_RESOLVE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = CodeLens.serializer(),
                    result = CodeLens.serializer(),
                ),
            LspMethods.COMPLETION_ITEM_RESOLVE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = CompletionItem.serializer(),
                    result = CompletionItem.serializer(),
                ),
            LspMethods.DOCUMENT_LINK_RESOLVE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DocumentLink.serializer(),
                    result = DocumentLink.serializer(),
                ),
            LspMethods.EXIT to
                MessageBinding(
                    MessageDirection.ClientToServer,
                ),
            LspMethods.INITIALIZE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = InitializeParams.serializer(),
                    result = InitializeResult.serializer(),
                    errorData = InitializeError.serializer(),
                ),
            LspMethods.INITIALIZED to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = InitializedParams.serializer(),
                ),
            LspMethods.INLAY_HINT_RESOLVE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = InlayHint.serializer(),
                    result = InlayHint.serializer(),
                ),
            LspMethods.NOTEBOOK_DOCUMENT_DID_CHANGE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DidChangeNotebookDocumentParams.serializer(),
                    registrationOptions = NotebookDocumentSyncRegistrationOptions.serializer(),
                    registrationMethod = LspMethods.NOTEBOOK_DOCUMENT_SYNC,
                ),
            LspMethods.NOTEBOOK_DOCUMENT_DID_CLOSE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DidCloseNotebookDocumentParams.serializer(),
                    registrationOptions = NotebookDocumentSyncRegistrationOptions.serializer(),
                    registrationMethod = LspMethods.NOTEBOOK_DOCUMENT_SYNC,
                ),
            LspMethods.NOTEBOOK_DOCUMENT_DID_OPEN to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DidOpenNotebookDocumentParams.serializer(),
                    registrationOptions = NotebookDocumentSyncRegistrationOptions.serializer(),
                    registrationMethod = LspMethods.NOTEBOOK_DOCUMENT_SYNC,
                ),
            LspMethods.NOTEBOOK_DOCUMENT_DID_SAVE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DidSaveNotebookDocumentParams.serializer(),
                    registrationOptions = NotebookDocumentSyncRegistrationOptions.serializer(),
                    registrationMethod = LspMethods.NOTEBOOK_DOCUMENT_SYNC,
                ),
            LspMethods.SHUTDOWN to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    result = JsonNull.serializer(),
                ),
            LspMethods.TELEMETRY_EVENT to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    params = JsonElement.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_CODE_ACTION to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = CodeActionParams.serializer(),
                    result = ListSerializer(CommandOrCodeActionSerializer).nullable,
                    partialResult = ListSerializer(CommandOrCodeActionSerializer),
                    registrationOptions = CodeActionRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_CODE_LENS to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = CodeLensParams.serializer(),
                    result = ListSerializer(CodeLens.serializer()).nullable,
                    partialResult = ListSerializer(CodeLens.serializer()),
                    registrationOptions = CodeLensRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_COLOR_PRESENTATION to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = ColorPresentationParams.serializer(),
                    result = ListSerializer(ColorPresentation.serializer()),
                    partialResult = ListSerializer(ColorPresentation.serializer()),
                    registrationOptions = DocumentColorRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_COMPLETION to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = CompletionParams.serializer(),
                    result = CompletionResultSerializer.nullable,
                    partialResult = ListSerializer(CompletionItem.serializer()),
                    registrationOptions = CompletionRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_DECLARATION to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DeclarationParams.serializer(),
                    result = DefinitionResultSerializer.nullable,
                    partialResult = DefinitionResultSerializer,
                    registrationOptions = DeclarationRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_DEFINITION to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DefinitionParams.serializer(),
                    result = DefinitionResultSerializer.nullable,
                    partialResult = DefinitionResultSerializer,
                    registrationOptions = DefinitionRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_DIAGNOSTIC to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DocumentDiagnosticParams.serializer(),
                    result = DocumentDiagnosticReportSerializer,
                    partialResult = DocumentDiagnosticReportProgressSerializer,
                    registrationOptions = DiagnosticRegistrationOptions.serializer(),
                    errorData = DiagnosticServerCancellationData.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_DID_CHANGE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DidChangeTextDocumentParams.serializer(),
                    registrationOptions = TextDocumentChangeRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_DID_CLOSE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DidCloseTextDocumentParams.serializer(),
                    registrationOptions = TextDocumentRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_DID_OPEN to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DidOpenTextDocumentParams.serializer(),
                    registrationOptions = TextDocumentRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_DID_SAVE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DidSaveTextDocumentParams.serializer(),
                    registrationOptions = TextDocumentSaveRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_DOCUMENT_COLOR to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DocumentColorParams.serializer(),
                    result = ListSerializer(ColorInformation.serializer()),
                    partialResult = ListSerializer(ColorInformation.serializer()),
                    registrationOptions = DocumentColorRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DocumentHighlightParams.serializer(),
                    result = ListSerializer(DocumentHighlight.serializer()).nullable,
                    partialResult = ListSerializer(DocumentHighlight.serializer()),
                    registrationOptions = DocumentHighlightRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_DOCUMENT_LINK to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DocumentLinkParams.serializer(),
                    result = ListSerializer(DocumentLink.serializer()).nullable,
                    partialResult = ListSerializer(DocumentLink.serializer()),
                    registrationOptions = DocumentLinkRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_DOCUMENT_SYMBOL to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DocumentSymbolParams.serializer(),
                    result = DocumentSymbolResultSerializer.nullable,
                    partialResult = DocumentSymbolResultSerializer,
                    registrationOptions = DocumentSymbolRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_FOLDING_RANGE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = FoldingRangeParams.serializer(),
                    result = ListSerializer(FoldingRange.serializer()).nullable,
                    partialResult = ListSerializer(FoldingRange.serializer()),
                    registrationOptions = FoldingRangeRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_FORMATTING to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DocumentFormattingParams.serializer(),
                    result = ListSerializer(TextEdit.serializer()).nullable,
                    registrationOptions = DocumentFormattingRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_HOVER to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = HoverParams.serializer(),
                    result = Hover.serializer().nullable,
                    registrationOptions = HoverRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_IMPLEMENTATION to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = ImplementationParams.serializer(),
                    result = DefinitionResultSerializer.nullable,
                    partialResult = DefinitionResultSerializer,
                    registrationOptions = ImplementationRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_INLAY_HINT to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = InlayHintParams.serializer(),
                    result = ListSerializer(InlayHint.serializer()).nullable,
                    partialResult = ListSerializer(InlayHint.serializer()),
                    registrationOptions = InlayHintRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_INLINE_COMPLETION to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = InlineCompletionParams.serializer(),
                    result = InlineCompletionResultSerializer.nullable,
                    partialResult = ListSerializer(InlineCompletionItem.serializer()),
                    registrationOptions = InlineCompletionRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_INLINE_VALUE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = InlineValueParams.serializer(),
                    result = ListSerializer(InlineValueSerializer).nullable,
                    partialResult = ListSerializer(InlineValueSerializer),
                    registrationOptions = InlineValueRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_LINKED_EDITING_RANGE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = LinkedEditingRangeParams.serializer(),
                    result = LinkedEditingRanges.serializer().nullable,
                    registrationOptions = LinkedEditingRangeRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_MONIKER to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = MonikerParams.serializer(),
                    result = ListSerializer(Moniker.serializer()).nullable,
                    partialResult = ListSerializer(Moniker.serializer()),
                    registrationOptions = MonikerRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_ON_TYPE_FORMATTING to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DocumentOnTypeFormattingParams.serializer(),
                    result = ListSerializer(TextEdit.serializer()).nullable,
                    registrationOptions = DocumentOnTypeFormattingRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_PREPARE_CALL_HIERARCHY to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = CallHierarchyPrepareParams.serializer(),
                    result = ListSerializer(CallHierarchyItem.serializer()).nullable,
                    registrationOptions = CallHierarchyRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_PREPARE_RENAME to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = PrepareRenameParams.serializer(),
                    result = PrepareRenameResultSerializer.nullable,
                ),
            LspMethods.TEXT_DOCUMENT_PREPARE_TYPE_HIERARCHY to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = TypeHierarchyPrepareParams.serializer(),
                    result = ListSerializer(TypeHierarchyItem.serializer()).nullable,
                    registrationOptions = TypeHierarchyRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    params = PublishDiagnosticsParams.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_RANGE_FORMATTING to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DocumentRangeFormattingParams.serializer(),
                    result = ListSerializer(TextEdit.serializer()).nullable,
                    registrationOptions = DocumentRangeFormattingRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_RANGES_FORMATTING to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DocumentRangesFormattingParams.serializer(),
                    result = ListSerializer(TextEdit.serializer()).nullable,
                    registrationOptions = DocumentRangeFormattingRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_REFERENCES to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = ReferenceParams.serializer(),
                    result = ListSerializer(Location.serializer()).nullable,
                    partialResult = ListSerializer(Location.serializer()),
                    registrationOptions = ReferenceRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_RENAME to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = RenameParams.serializer(),
                    result = WorkspaceEdit.serializer().nullable,
                    registrationOptions = RenameRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_SELECTION_RANGE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = SelectionRangeParams.serializer(),
                    result = ListSerializer(SelectionRange.serializer()).nullable,
                    partialResult = ListSerializer(SelectionRange.serializer()),
                    registrationOptions = SelectionRangeRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = SemanticTokensParams.serializer(),
                    result = SemanticTokens.serializer().nullable,
                    partialResult = SemanticTokensPartialResult.serializer(),
                    registrationOptions = SemanticTokensRegistrationOptions.serializer(),
                    registrationMethod = LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS,
                ),
            LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL_DELTA to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = SemanticTokensDeltaParams.serializer(),
                    result = SemanticTokensDeltaResultSerializer.nullable,
                    partialResult = SemanticTokensDeltaPartialSerializer,
                    registrationOptions = SemanticTokensRegistrationOptions.serializer(),
                    registrationMethod = LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS,
                ),
            LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS_RANGE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = SemanticTokensRangeParams.serializer(),
                    result = SemanticTokens.serializer().nullable,
                    partialResult = SemanticTokensPartialResult.serializer(),
                    registrationMethod = LspMethods.TEXT_DOCUMENT_SEMANTIC_TOKENS,
                ),
            LspMethods.TEXT_DOCUMENT_SIGNATURE_HELP to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = SignatureHelpParams.serializer(),
                    result = SignatureHelp.serializer().nullable,
                    registrationOptions = SignatureHelpRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_TYPE_DEFINITION to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = TypeDefinitionParams.serializer(),
                    result = DefinitionResultSerializer.nullable,
                    partialResult = DefinitionResultSerializer,
                    registrationOptions = TypeDefinitionRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_WILL_SAVE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = WillSaveTextDocumentParams.serializer(),
                    registrationOptions = TextDocumentRegistrationOptions.serializer(),
                ),
            LspMethods.TEXT_DOCUMENT_WILL_SAVE_WAIT_UNTIL to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = WillSaveTextDocumentParams.serializer(),
                    result = ListSerializer(TextEdit.serializer()).nullable,
                    registrationOptions = TextDocumentRegistrationOptions.serializer(),
                ),
            LspMethods.TYPE_HIERARCHY_SUBTYPES to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = TypeHierarchySubtypesParams.serializer(),
                    result = ListSerializer(TypeHierarchyItem.serializer()).nullable,
                    partialResult = ListSerializer(TypeHierarchyItem.serializer()),
                ),
            LspMethods.TYPE_HIERARCHY_SUPERTYPES to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = TypeHierarchySupertypesParams.serializer(),
                    result = ListSerializer(TypeHierarchyItem.serializer()).nullable,
                    partialResult = ListSerializer(TypeHierarchyItem.serializer()),
                ),
            LspMethods.WINDOW_LOG_MESSAGE to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    params = LogMessageParams.serializer(),
                ),
            LspMethods.WINDOW_SHOW_DOCUMENT to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    params = ShowDocumentParams.serializer(),
                    result = ShowDocumentResult.serializer(),
                ),
            LspMethods.WINDOW_SHOW_MESSAGE to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    params = ShowMessageParams.serializer(),
                ),
            LspMethods.WINDOW_SHOW_MESSAGE_REQUEST to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    params = ShowMessageRequestParams.serializer(),
                    result = MessageActionItem.serializer().nullable,
                ),
            LspMethods.WINDOW_WORK_DONE_PROGRESS_CANCEL to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = WorkDoneProgressCancelParams.serializer(),
                ),
            LspMethods.WINDOW_WORK_DONE_PROGRESS_CREATE to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    params = WorkDoneProgressCreateParams.serializer(),
                    result = JsonNull.serializer(),
                ),
            LspMethods.WORKSPACE_APPLY_EDIT to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    params = ApplyWorkspaceEditParams.serializer(),
                    result = ApplyWorkspaceEditResult.serializer(),
                ),
            LspMethods.WORKSPACE_CODE_LENS_REFRESH to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    result = JsonNull.serializer(),
                ),
            LspMethods.WORKSPACE_CONFIGURATION to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    params = ConfigurationParams.serializer(),
                    result = ListSerializer(JsonElement.serializer()),
                ),
            LspMethods.WORKSPACE_DIAGNOSTIC to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = WorkspaceDiagnosticParams.serializer(),
                    result = WorkspaceDiagnosticReport.serializer(),
                    partialResult = WorkspaceDiagnosticReportPartialResult.serializer(),
                    errorData = DiagnosticServerCancellationData.serializer(),
                ),
            LspMethods.WORKSPACE_DIAGNOSTIC_REFRESH to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    result = JsonNull.serializer(),
                ),
            LspMethods.WORKSPACE_DID_CHANGE_CONFIGURATION to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DidChangeConfigurationParams.serializer(),
                    registrationOptions = DidChangeConfigurationRegistrationOptions.serializer(),
                ),
            LspMethods.WORKSPACE_DID_CHANGE_WATCHED_FILES to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DidChangeWatchedFilesParams.serializer(),
                    registrationOptions = DidChangeWatchedFilesRegistrationOptions.serializer(),
                ),
            LspMethods.WORKSPACE_DID_CHANGE_WORKSPACE_FOLDERS to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DidChangeWorkspaceFoldersParams.serializer(),
                ),
            LspMethods.WORKSPACE_DID_CREATE_FILES to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = CreateFilesParams.serializer(),
                    registrationOptions = FileOperationRegistrationOptions.serializer(),
                ),
            LspMethods.WORKSPACE_DID_DELETE_FILES to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DeleteFilesParams.serializer(),
                    registrationOptions = FileOperationRegistrationOptions.serializer(),
                ),
            LspMethods.WORKSPACE_DID_RENAME_FILES to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = RenameFilesParams.serializer(),
                    registrationOptions = FileOperationRegistrationOptions.serializer(),
                ),
            LspMethods.WORKSPACE_EXECUTE_COMMAND to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = ExecuteCommandParams.serializer(),
                    result = JsonElement.serializer().nullable,
                    registrationOptions = ExecuteCommandRegistrationOptions.serializer(),
                ),
            LspMethods.WORKSPACE_FOLDING_RANGE_REFRESH to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    result = JsonNull.serializer(),
                ),
            LspMethods.WORKSPACE_INLAY_HINT_REFRESH to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    result = JsonNull.serializer(),
                ),
            LspMethods.WORKSPACE_INLINE_VALUE_REFRESH to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    result = JsonNull.serializer(),
                ),
            LspMethods.WORKSPACE_SEMANTIC_TOKENS_REFRESH to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    result = JsonNull.serializer(),
                ),
            LspMethods.WORKSPACE_SYMBOL to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = WorkspaceSymbolParams.serializer(),
                    result = WorkspaceSymbolResultSerializer.nullable,
                    partialResult = WorkspaceSymbolResultSerializer,
                    registrationOptions = WorkspaceSymbolRegistrationOptions.serializer(),
                ),
            LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = TextDocumentContentParams.serializer(),
                    result = TextDocumentContentResult.serializer(),
                    registrationOptions = TextDocumentContentRegistrationOptions.serializer(),
                ),
            LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT_REFRESH to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    params = TextDocumentContentRefreshParams.serializer(),
                    result = JsonNull.serializer(),
                ),
            LspMethods.WORKSPACE_WILL_CREATE_FILES to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = CreateFilesParams.serializer(),
                    result = WorkspaceEdit.serializer().nullable,
                    registrationOptions = FileOperationRegistrationOptions.serializer(),
                ),
            LspMethods.WORKSPACE_WILL_DELETE_FILES to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = DeleteFilesParams.serializer(),
                    result = WorkspaceEdit.serializer().nullable,
                    registrationOptions = FileOperationRegistrationOptions.serializer(),
                ),
            LspMethods.WORKSPACE_WILL_RENAME_FILES to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = RenameFilesParams.serializer(),
                    result = WorkspaceEdit.serializer().nullable,
                    registrationOptions = FileOperationRegistrationOptions.serializer(),
                ),
            LspMethods.WORKSPACE_WORKSPACE_FOLDERS to
                MessageBinding(
                    MessageDirection.ServerToClient,
                    result = ListSerializer(WorkspaceFolder.serializer()).nullable,
                ),
            LspMethods.WORKSPACE_SYMBOL_RESOLVE to
                MessageBinding(
                    MessageDirection.ClientToServer,
                    params = WorkspaceSymbol.serializer(),
                    result = WorkspaceSymbol.serializer(),
                ),
        )
}
