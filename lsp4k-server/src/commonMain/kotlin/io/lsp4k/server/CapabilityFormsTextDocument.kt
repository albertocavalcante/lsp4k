package io.lsp4k.server

import io.lsp4k.protocol.DeclarationOptions
import io.lsp4k.protocol.DeclarationRegistrationOptions
import io.lsp4k.protocol.DocumentColorOptions
import io.lsp4k.protocol.DocumentColorRegistrationOptions
import io.lsp4k.protocol.Either3
import io.lsp4k.protocol.FoldingRangeOptions
import io.lsp4k.protocol.FoldingRangeRegistrationOptions
import io.lsp4k.protocol.ImplementationOptions
import io.lsp4k.protocol.ImplementationRegistrationOptions
import io.lsp4k.protocol.SelectionRangeOptions
import io.lsp4k.protocol.SelectionRangeRegistrationOptions
import io.lsp4k.protocol.TypeDefinitionOptions
import io.lsp4k.protocol.TypeDefinitionRegistrationOptions

/** Advertises `textDocument/declaration` with the given [options] instead of the boolean flag. */
public fun ServerCapabilitiesBuilder.provideDeclaration(options: DeclarationOptions) {
    forms.declaration = Either3.second(options)
}

/** Advertises `textDocument/declaration` as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideDeclaration(registration: DeclarationRegistrationOptions) {
    forms.declaration = Either3.third(registration)
}

/** Advertises `textDocument/typeDefinition` with the given [options] instead of the boolean flag. */
public fun ServerCapabilitiesBuilder.provideTypeDefinition(options: TypeDefinitionOptions) {
    forms.typeDefinition = Either3.second(options)
}

/** Advertises `textDocument/typeDefinition` as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideTypeDefinition(registration: TypeDefinitionRegistrationOptions) {
    forms.typeDefinition = Either3.third(registration)
}

/** Advertises `textDocument/implementation` with the given [options] instead of the boolean flag. */
public fun ServerCapabilitiesBuilder.provideImplementation(options: ImplementationOptions) {
    forms.implementation = Either3.second(options)
}

/** Advertises `textDocument/implementation` as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideImplementation(registration: ImplementationRegistrationOptions) {
    forms.implementation = Either3.third(registration)
}

/** Advertises `textDocument/documentColor` with the given [options] instead of the boolean flag. */
public fun ServerCapabilitiesBuilder.provideDocumentColor(options: DocumentColorOptions) {
    forms.color = Either3.second(options)
}

/** Advertises `textDocument/documentColor` as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideDocumentColor(registration: DocumentColorRegistrationOptions) {
    forms.color = Either3.third(registration)
}

/** Advertises `textDocument/foldingRange` with the given [options] instead of the boolean flag. */
public fun ServerCapabilitiesBuilder.provideFoldingRange(options: FoldingRangeOptions) {
    forms.foldingRange = Either3.second(options)
}

/** Advertises `textDocument/foldingRange` as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideFoldingRange(registration: FoldingRangeRegistrationOptions) {
    forms.foldingRange = Either3.third(registration)
}

/** Advertises `textDocument/selectionRange` with the given [options] instead of the boolean flag. */
public fun ServerCapabilitiesBuilder.provideSelectionRange(options: SelectionRangeOptions) {
    forms.selectionRange = Either3.second(options)
}

/** Advertises `textDocument/selectionRange` as a registration with the given [registration] options (selector and id). */
public fun ServerCapabilitiesBuilder.provideSelectionRange(registration: SelectionRangeRegistrationOptions) {
    forms.selectionRange = Either3.third(registration)
}
