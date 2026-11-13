package io.lsp4k.server

import io.kotest.matchers.shouldBe
import io.lsp4k.protocol.CallHierarchyRegistrationOptions
import io.lsp4k.protocol.DiagnosticOptions
import io.lsp4k.protocol.DiagnosticRegistrationOptions
import io.lsp4k.protocol.Either
import io.lsp4k.protocol.Either3
import io.lsp4k.protocol.FoldingRangeOptions
import io.lsp4k.protocol.FoldingRangeRegistrationOptions
import io.lsp4k.protocol.NotebookDocumentSyncOptions
import io.lsp4k.protocol.NotebookDocumentSyncRegistrationOptions
import io.lsp4k.protocol.SemanticTokensLegend
import io.lsp4k.protocol.SemanticTokensOptions
import io.lsp4k.protocol.SemanticTokensRegistrationOptions
import io.lsp4k.protocol.TextDocumentFilter
import kotlin.test.Test

/** The boolean flags, options forms and registration forms of [ServerCapabilitiesBuilder]. */
class CapabilityFormsTest {
    private val selector = listOf(TextDocumentFilter(language = "kotlin"))
    private val legend = SemanticTokensLegend(tokenTypes = listOf("class"), tokenModifiers = emptyList())

    @Test
    fun `boolean flag advertises the boolean form`() {
        val config = languageServer { capabilities { foldingRangeProvider = true } }
        config.capabilities.foldingRangeProvider shouldBe Either3.first(true)
        config.capabilities.selectionRangeProvider shouldBe null
    }

    @Test
    fun `options form replaces the boolean flag`() {
        val options = FoldingRangeOptions(workDoneProgress = true)
        val config =
            languageServer {
                capabilities {
                    foldingRangeProvider = true
                    provideFoldingRange(options)
                }
            }
        config.capabilities.foldingRangeProvider shouldBe Either3.second(options)
    }

    @Test
    fun `registration form carries selector and id`() {
        val config =
            languageServer {
                capabilities {
                    provideFoldingRange(FoldingRangeRegistrationOptions(documentSelector = selector, id = "fold"))
                    provideCallHierarchy(CallHierarchyRegistrationOptions(documentSelector = null))
                }
            }
        config.capabilities.foldingRangeProvider shouldBe
            Either3.third(FoldingRangeRegistrationOptions(documentSelector = selector, id = "fold"))
        config.capabilities.callHierarchyProvider shouldBe Either3.third(CallHierarchyRegistrationOptions(documentSelector = null))
    }

    @Test
    fun `semantic tokens diagnostics and notebook sync support both forms`() {
        val plain =
            languageServer {
                capabilities {
                    semanticTokensProvider = SemanticTokensOptions(legend = legend)
                    diagnosticProvider = DiagnosticOptions(interFileDependencies = false, workspaceDiagnostics = false)
                    notebookDocumentSync = NotebookDocumentSyncOptions(notebookSelector = emptyList())
                }
            }
        plain.capabilities.semanticTokensProvider shouldBe Either.left(SemanticTokensOptions(legend = legend))
        plain.capabilities.diagnosticProvider shouldBe
            Either.left(DiagnosticOptions(interFileDependencies = false, workspaceDiagnostics = false))
        plain.capabilities.notebookDocumentSync shouldBe Either.left(NotebookDocumentSyncOptions(notebookSelector = emptyList()))

        val registered =
            languageServer {
                capabilities {
                    provideSemanticTokens(SemanticTokensRegistrationOptions(documentSelector = null, legend = legend))
                    provideDiagnostics(
                        DiagnosticRegistrationOptions(documentSelector = null, interFileDependencies = true, workspaceDiagnostics = true),
                    )
                    provideNotebookDocumentSync(NotebookDocumentSyncRegistrationOptions(notebookSelector = emptyList(), id = "nb"))
                }
            }
        registered.capabilities.semanticTokensProvider shouldBe
            Either.right(SemanticTokensRegistrationOptions(documentSelector = null, legend = legend))
        registered.capabilities.diagnosticProvider shouldBe
            Either.right(DiagnosticRegistrationOptions(documentSelector = null, interFileDependencies = true, workspaceDiagnostics = true))
        registered.capabilities.notebookDocumentSync shouldBe
            Either.right(NotebookDocumentSyncRegistrationOptions(notebookSelector = emptyList(), id = "nb"))
    }
}
