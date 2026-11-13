package io.lsp4k.server

import io.lsp4k.protocol.CallHierarchyOptions
import io.lsp4k.protocol.CallHierarchyRegistrationOptions
import io.lsp4k.protocol.DiagnosticRegistrationOptions
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.Either3
import io.lsp4k.protocol.InlayHintOptions
import io.lsp4k.protocol.InlayHintRegistrationOptions
import io.lsp4k.protocol.InlineValueOptions
import io.lsp4k.protocol.InlineValueRegistrationOptions
import io.lsp4k.protocol.LinkedEditingRangeOptions
import io.lsp4k.protocol.LinkedEditingRangeRegistrationOptions
import io.lsp4k.protocol.MonikerOptions
import io.lsp4k.protocol.MonikerRegistrationOptions
import io.lsp4k.protocol.NotebookDocumentSyncRegistrationOptions
import io.lsp4k.protocol.SemanticTokensRegistrationOptions
import io.lsp4k.protocol.TypeHierarchyOptions
import io.lsp4k.protocol.TypeHierarchyRegistrationOptions

/** Advertises `call hierarchy` with the given [options] instead of the boolean flag. */
public fun ServerCapabilitiesBuilder.provideCallHierarchy(options: CallHierarchyOptions) {
    forms.callHierarchy = Either3.second(options)
}

/** Advertises `call hierarchy` as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideCallHierarchy(registration: CallHierarchyRegistrationOptions) {
    forms.callHierarchy = Either3.third(registration)
}

/** Advertises `textDocument/linkedEditingRange` with the given [options] instead of the boolean flag. */
public fun ServerCapabilitiesBuilder.provideLinkedEditingRange(options: LinkedEditingRangeOptions) {
    forms.linkedEditingRange = Either3.second(options)
}

/** Advertises `textDocument/linkedEditingRange` as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideLinkedEditingRange(registration: LinkedEditingRangeRegistrationOptions) {
    forms.linkedEditingRange = Either3.third(registration)
}

/** Advertises `textDocument/moniker` with the given [options] instead of the boolean flag. */
public fun ServerCapabilitiesBuilder.provideMoniker(options: MonikerOptions) {
    forms.moniker = Either3.second(options)
}

/** Advertises `textDocument/moniker` as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideMoniker(registration: MonikerRegistrationOptions) {
    forms.moniker = Either3.third(registration)
}

/** Advertises `type hierarchy` with the given [options] instead of the boolean flag. */
public fun ServerCapabilitiesBuilder.provideTypeHierarchy(options: TypeHierarchyOptions) {
    forms.typeHierarchy = Either3.second(options)
}

/** Advertises `type hierarchy` as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideTypeHierarchy(registration: TypeHierarchyRegistrationOptions) {
    forms.typeHierarchy = Either3.third(registration)
}

/** Advertises `textDocument/inlineValue` with the given [options] instead of the boolean flag. */
public fun ServerCapabilitiesBuilder.provideInlineValue(options: InlineValueOptions) {
    forms.inlineValue = Either3.second(options)
}

/** Advertises `textDocument/inlineValue` as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideInlineValue(registration: InlineValueRegistrationOptions) {
    forms.inlineValue = Either3.third(registration)
}

/** Advertises `textDocument/inlayHint` with the given [options] instead of the boolean flag. */
public fun ServerCapabilitiesBuilder.provideInlayHint(options: InlayHintOptions) {
    forms.inlayHint = Either3.second(options)
}

/** Advertises `textDocument/inlayHint` as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideInlayHint(registration: InlayHintRegistrationOptions) {
    forms.inlayHint = Either3.third(registration)
}

/** Advertises semantic tokens as a registration with the given [registration] options (selector, legend and id). */
public fun ServerCapabilitiesBuilder.provideSemanticTokens(registration: SemanticTokensRegistrationOptions) {
    forms.semanticTokens = Either.right(registration)
}

/** Advertises pull diagnostics as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideDiagnostics(registration: DiagnosticRegistrationOptions) {
    forms.diagnostic = Either.right(registration)
}

/** Advertises notebook document sync as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideNotebookDocumentSync(registration: NotebookDocumentSyncRegistrationOptions) {
    forms.notebookDocumentSync = Either.right(registration)
}
