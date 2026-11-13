package io.lsp4k.server

import io.lsp4k.protocol.CodeLensOptions
import io.lsp4k.protocol.CompletionOptions
import io.lsp4k.protocol.DiagnosticOptions
import io.lsp4k.protocol.DocumentLinkOptions
import io.lsp4k.protocol.DocumentOnTypeFormattingOptions
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.Either3
import io.lsp4k.protocol.ExecuteCommandOptions
import io.lsp4k.protocol.NotebookDocumentSyncOptions
import io.lsp4k.protocol.PositionEncodingKind
import io.lsp4k.protocol.SaveOptions
import io.lsp4k.protocol.SemanticTokensOptions
import io.lsp4k.protocol.ServerCapabilities
import io.lsp4k.protocol.ServerWorkspaceCapabilities
import io.lsp4k.protocol.SignatureHelpOptions
import io.lsp4k.protocol.TextDocumentSyncKind
import io.lsp4k.protocol.TextDocumentSyncOptions
import kotlinx.serialization.json.JsonElement

/**
 * Builder for constructing [ServerCapabilities] advertised during initialization.
 */
@LspServerDsl
public class ServerCapabilitiesBuilder internal constructor() {
    /** The position encoding picked from the client's `general.positionEncodings`; UTF-16 when unset. */
    public var positionEncoding: PositionEncodingKind? = null

    /** Defines how text documents are synced (open/close, change granularity, save). */
    public var textDocumentSync: TextDocumentSyncKind? = null

    /** Defines how notebook documents are synced. */
    public var notebookDocumentSync: NotebookDocumentSyncOptions? = null

    /** Options for the `textDocument/completion` provider. */
    public var completionProvider: CompletionOptions? = null

    /** Whether the server provides `textDocument/hover` support. */
    public var hoverProvider: Boolean = false

    /** Options for the `textDocument/signatureHelp` provider. */
    public var signatureHelpProvider: SignatureHelpOptions? = null

    /** Whether the server provides `textDocument/declaration` support. */
    public var declarationProvider: Boolean = false

    /** Whether the server provides `textDocument/definition` support. */
    public var definitionProvider: Boolean = false

    /** Whether the server provides `textDocument/typeDefinition` support. */
    public var typeDefinitionProvider: Boolean = false

    /** Whether the server provides `textDocument/implementation` support. */
    public var implementationProvider: Boolean = false

    /** Whether the server provides `textDocument/references` support. */
    public var referencesProvider: Boolean = false

    /** Whether the server provides `textDocument/documentHighlight` support. */
    public var documentHighlightProvider: Boolean = false

    /** Whether the server provides `textDocument/documentSymbol` support. */
    public var documentSymbolProvider: Boolean = false

    /** Whether the server provides `textDocument/codeAction` support. */
    public var codeActionProvider: Boolean = false

    /** Options for the `textDocument/codeLens` provider. */
    public var codeLensProvider: CodeLensOptions? = null

    /** Options for the `textDocument/documentLink` provider. */
    public var documentLinkProvider: DocumentLinkOptions? = null

    /** Whether the server provides `textDocument/documentColor` support. */
    public var colorProvider: Boolean = false

    /** Whether the server provides `textDocument/formatting` support. */
    public var documentFormattingProvider: Boolean = false

    /** Whether the server provides `textDocument/rangeFormatting` support. */
    public var documentRangeFormattingProvider: Boolean = false

    /** Options for the `textDocument/onTypeFormatting` provider. */
    public var documentOnTypeFormattingProvider: DocumentOnTypeFormattingOptions? = null

    /** Whether the server provides `textDocument/rename` support. */
    public var renameProvider: Boolean = false

    /** Whether the server provides `textDocument/foldingRange` support. */
    public var foldingRangeProvider: Boolean = false

    /** Whether the server provides `textDocument/selectionRange` support. */
    public var selectionRangeProvider: Boolean = false

    /** Whether the server provides `textDocument/linkedEditingRange` support. */
    public var linkedEditingRangeProvider: Boolean = false

    /** Whether the server provides call hierarchy support. */
    public var callHierarchyProvider: Boolean = false

    /** Options for the semantic tokens provider. */
    public var semanticTokensProvider: SemanticTokensOptions? = null

    /** Whether the server provides `textDocument/moniker` support. */
    public var monikerProvider: Boolean = false

    /** Whether the server provides type hierarchy support. */
    public var typeHierarchyProvider: Boolean = false

    /** Whether the server provides `textDocument/inlineValue` support. */
    public var inlineValueProvider: Boolean = false

    /** Whether the server provides `textDocument/inlineCompletion` support. */
    public var inlineCompletionProvider: Boolean = false

    /** Whether the server provides `textDocument/inlayHint` support. */
    public var inlayHintProvider: Boolean = false

    /** Options for the `textDocument/diagnostic` provider. */
    public var diagnosticProvider: DiagnosticOptions? = null

    /** Whether the server provides `workspace/symbol` support. */
    public var workspaceSymbolProvider: Boolean = false

    /** Options for the `workspace/executeCommand` provider. */
    public var executeCommandProvider: ExecuteCommandOptions? = null

    /** Workspace folder, file operation and `workspace/textDocumentContent` capabilities. */
    public var workspace: ServerWorkspaceCapabilities? = null

    /** Experimental server capabilities. */
    public var experimental: JsonElement? = null

    /** Option and registration forms set through the `provide...` functions; they win over the flags above. */
    internal val forms: ProviderForms = ProviderForms()

    private fun Boolean.toEither(): Either<Boolean, Nothing>? = if (this) Either.left(true) else null

    private fun Boolean.toEither3(): Either3<Boolean, Nothing, Nothing>? = if (this) Either3.first(true) else null

    internal fun build(): ServerCapabilities = baseCapabilities().withNavigationForms().withLanguageForms()

    private fun baseCapabilities(): ServerCapabilities =
        ServerCapabilities(
            positionEncoding = positionEncoding,
            textDocumentSync =
                textDocumentSync?.let {
                    Either.right(
                        TextDocumentSyncOptions(
                            openClose = true,
                            change = it,
                            save = Either.right(SaveOptions(includeText = false)),
                        ),
                    )
                },
            notebookDocumentSync = forms.notebookDocumentSync ?: notebookDocumentSync?.let { Either.left(it) },
            completionProvider = completionProvider,
            hoverProvider = hoverProvider.toEither(),
            signatureHelpProvider = signatureHelpProvider,
            definitionProvider = definitionProvider.toEither(),
            referencesProvider = referencesProvider.toEither(),
            documentHighlightProvider = documentHighlightProvider.toEither(),
            documentSymbolProvider = documentSymbolProvider.toEither(),
            codeActionProvider = codeActionProvider.toEither(),
            codeLensProvider = codeLensProvider,
            documentLinkProvider = documentLinkProvider,
            documentFormattingProvider = documentFormattingProvider.toEither(),
            documentRangeFormattingProvider = documentRangeFormattingProvider.toEither(),
            documentOnTypeFormattingProvider = documentOnTypeFormattingProvider,
            renameProvider = renameProvider.toEither(),
            semanticTokensProvider = forms.semanticTokens ?: semanticTokensProvider?.let { Either.left(it) },
            inlineCompletionProvider = inlineCompletionProvider.toEither(),
            diagnosticProvider = forms.diagnostic ?: diagnosticProvider?.let { Either.left(it) },
            workspaceSymbolProvider = workspaceSymbolProvider.toEither(),
            executeCommandProvider = executeCommandProvider,
            workspace = workspace,
            experimental = experimental,
        )

    private fun ServerCapabilities.withNavigationForms(): ServerCapabilities =
        copy(
            declarationProvider = forms.declaration ?: declarationProvider.toEither3(),
            typeDefinitionProvider = forms.typeDefinition ?: typeDefinitionProvider.toEither3(),
            implementationProvider = forms.implementation ?: implementationProvider.toEither3(),
            colorProvider = forms.color ?: colorProvider.toEither3(),
            foldingRangeProvider = forms.foldingRange ?: foldingRangeProvider.toEither3(),
            selectionRangeProvider = forms.selectionRange ?: selectionRangeProvider.toEither3(),
            linkedEditingRangeProvider = forms.linkedEditingRange ?: linkedEditingRangeProvider.toEither3(),
            callHierarchyProvider = forms.callHierarchy ?: callHierarchyProvider.toEither3(),
        )

    private fun ServerCapabilities.withLanguageForms(): ServerCapabilities =
        copy(
            monikerProvider = forms.moniker ?: monikerProvider.toEither3(),
            typeHierarchyProvider = forms.typeHierarchy ?: typeHierarchyProvider.toEither3(),
            inlineValueProvider = forms.inlineValue ?: inlineValueProvider.toEither3(),
            inlayHintProvider = forms.inlayHint ?: inlayHintProvider.toEither3(),
        )
}
