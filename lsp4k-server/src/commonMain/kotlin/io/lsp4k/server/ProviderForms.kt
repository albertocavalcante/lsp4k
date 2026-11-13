package io.lsp4k.server

import io.lsp4k.protocol.CallHierarchyOptions
import io.lsp4k.protocol.CallHierarchyRegistrationOptions
import io.lsp4k.protocol.DeclarationOptions
import io.lsp4k.protocol.DeclarationRegistrationOptions
import io.lsp4k.protocol.DiagnosticOptions
import io.lsp4k.protocol.DiagnosticRegistrationOptions
import io.lsp4k.protocol.DocumentColorOptions
import io.lsp4k.protocol.DocumentColorRegistrationOptions
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.Either3
import io.lsp4k.protocol.FoldingRangeOptions
import io.lsp4k.protocol.FoldingRangeRegistrationOptions
import io.lsp4k.protocol.ImplementationOptions
import io.lsp4k.protocol.ImplementationRegistrationOptions
import io.lsp4k.protocol.InlayHintOptions
import io.lsp4k.protocol.InlayHintRegistrationOptions
import io.lsp4k.protocol.InlineValueOptions
import io.lsp4k.protocol.InlineValueRegistrationOptions
import io.lsp4k.protocol.LinkedEditingRangeOptions
import io.lsp4k.protocol.LinkedEditingRangeRegistrationOptions
import io.lsp4k.protocol.MonikerOptions
import io.lsp4k.protocol.MonikerRegistrationOptions
import io.lsp4k.protocol.NotebookDocumentSyncOptions
import io.lsp4k.protocol.NotebookDocumentSyncRegistrationOptions
import io.lsp4k.protocol.SelectionRangeOptions
import io.lsp4k.protocol.SelectionRangeRegistrationOptions
import io.lsp4k.protocol.SemanticTokensOptions
import io.lsp4k.protocol.SemanticTokensRegistrationOptions
import io.lsp4k.protocol.TypeDefinitionOptions
import io.lsp4k.protocol.TypeDefinitionRegistrationOptions
import io.lsp4k.protocol.TypeHierarchyOptions
import io.lsp4k.protocol.TypeHierarchyRegistrationOptions

/**
 * Option and registration forms set through the `provide...` functions of [ServerCapabilitiesBuilder].
 * A form wins over the boolean flag or the options property of the same capability.
 */
internal class ProviderForms {
    var declaration: Either3<Boolean, DeclarationOptions, DeclarationRegistrationOptions>? = null
    var typeDefinition: Either3<Boolean, TypeDefinitionOptions, TypeDefinitionRegistrationOptions>? = null
    var implementation: Either3<Boolean, ImplementationOptions, ImplementationRegistrationOptions>? = null
    var color: Either3<Boolean, DocumentColorOptions, DocumentColorRegistrationOptions>? = null
    var foldingRange: Either3<Boolean, FoldingRangeOptions, FoldingRangeRegistrationOptions>? = null
    var selectionRange: Either3<Boolean, SelectionRangeOptions, SelectionRangeRegistrationOptions>? = null
    var callHierarchy: Either3<Boolean, CallHierarchyOptions, CallHierarchyRegistrationOptions>? = null
    var linkedEditingRange: Either3<Boolean, LinkedEditingRangeOptions, LinkedEditingRangeRegistrationOptions>? = null
    var moniker: Either3<Boolean, MonikerOptions, MonikerRegistrationOptions>? = null
    var typeHierarchy: Either3<Boolean, TypeHierarchyOptions, TypeHierarchyRegistrationOptions>? = null
    var inlineValue: Either3<Boolean, InlineValueOptions, InlineValueRegistrationOptions>? = null
    var inlayHint: Either3<Boolean, InlayHintOptions, InlayHintRegistrationOptions>? = null
    var semanticTokens: Either<SemanticTokensOptions, SemanticTokensRegistrationOptions>? = null
    var diagnostic: Either<DiagnosticOptions, DiagnosticRegistrationOptions>? = null
    var notebookDocumentSync: Either<NotebookDocumentSyncOptions, NotebookDocumentSyncRegistrationOptions>? = null
}
