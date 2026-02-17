package io.lsp4k.example

import io.lsp4k.protocol.CompletionItem
import io.lsp4k.protocol.CompletionItemKind
import io.lsp4k.protocol.CompletionList
import io.lsp4k.protocol.CompletionOptions
import io.lsp4k.protocol.Diagnostic
import io.lsp4k.protocol.DiagnosticSeverity
import io.lsp4k.protocol.DocumentSymbol
import io.lsp4k.protocol.Hover
import io.lsp4k.protocol.HoverContents
import io.lsp4k.protocol.MarkupContent
import io.lsp4k.protocol.MarkupKind
import io.lsp4k.protocol.MessageType
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.PublishDiagnosticsParams
import io.lsp4k.protocol.Range
import io.lsp4k.protocol.SymbolKind
import io.lsp4k.protocol.TextDocumentSyncKind
import io.lsp4k.protocol.TextEdit
import io.lsp4k.server.LanguageServer
import io.lsp4k.server.languageServer
import io.lsp4k.server.start
import io.lsp4k.transport.TransportFactory
import kotlinx.coroutines.runBlocking

/**
 * A simple "Note" language server demonstrating lsp4k features.
 *
 * Supports `.note` files with the following syntax:
 * ```
 * # Section Title
 * TODO: something to do
 * NOTE: something to remember
 * FIXME: something to fix
 * Regular text content
 * ```
 *
 * Features demonstrated:
 * - Text document synchronization (open/close/change)
 * - Completion (keywords, snippets)
 * - Hover (keyword documentation)
 * - Diagnostics (publish on change)
 * - Document symbols (sections)
 * - Formatting
 */

private val documents = mutableMapOf<String, String>()

private val keywords = listOf("TODO", "NOTE", "FIXME", "IMPORTANT", "DONE")

fun createNoteServer() =
    languageServer {
        serverInfo("note-language-server", "0.1.0")

        capabilities {
            textDocumentSync = TextDocumentSyncKind.Full
            completionProvider =
                CompletionOptions(
                    triggerCharacters = listOf(":", "#"),
                )
            hoverProvider = true
            documentSymbolProvider = true
            documentFormattingProvider = true
        }

        // =========================================================================
        // Text Document Handlers
        // =========================================================================
        textDocument {
            // -- Synchronization --------------------------------------------------

            didOpen { params ->
                val uri = params.textDocument.uri
                val text = params.textDocument.text
                documents[uri] = text
                publishDiagnosticsFor(uri, text)
            }

            didChange { params ->
                val uri = params.textDocument.uri
                val text = params.contentChanges.lastOrNull()?.text ?: return@didChange
                documents[uri] = text
                publishDiagnosticsFor(uri, text)
            }

            didClose { params ->
                documents.remove(params.textDocument.uri)
            }

            // -- Completion -------------------------------------------------------

            completion { params ->
                val uri = params.textDocument.uri
                val text = documents[uri] ?: return@completion null
                val line = text.lines().getOrNull(params.position.line) ?: ""
                val prefix = line.substring(0, minOf(params.position.character, line.length))

                val items =
                    buildList {
                        // Keyword completions at start of line
                        if (prefix.isBlank() || keywords.any { it.startsWith(prefix.trimStart(), ignoreCase = true) }) {
                            keywords.forEach { keyword ->
                                add(
                                    CompletionItem(
                                        label = "$keyword:",
                                        kind = CompletionItemKind.Keyword,
                                        detail = "$keyword annotation",
                                        insertText = "$keyword: ",
                                    ),
                                )
                            }
                        }

                        // Section header snippet
                        if (prefix.isBlank() || prefix.trimStart().startsWith("#")) {
                            add(
                                CompletionItem(
                                    label = "# Section",
                                    kind = CompletionItemKind.Snippet,
                                    detail = "Insert a new section header",
                                    insertText = "# ",
                                ),
                            )
                        }
                    }

                CompletionList(isIncomplete = false, items = items)
            }

            // -- Hover ------------------------------------------------------------

            hover { params ->
                val uri = params.textDocument.uri
                val text = documents[uri] ?: return@hover null
                val line = text.lines().getOrNull(params.position.line) ?: return@hover null
                val trimmed = line.trimStart()

                val keyword = keywords.firstOrNull { trimmed.startsWith("$it:") }
                if (keyword != null) {
                    val description =
                        when (keyword) {
                            "TODO" -> "**TODO** - A task that needs to be completed."
                            "NOTE" -> "**NOTE** - An important piece of information to remember."
                            "FIXME" -> "**FIXME** - A known issue that needs to be fixed."
                            "IMPORTANT" -> "**IMPORTANT** - Critical information that must not be overlooked."
                            "DONE" -> "**DONE** - A completed task."
                            else -> "**$keyword** annotation"
                        }
                    Hover(
                        contents =
                            HoverContents.Markup(
                                MarkupContent(
                                    kind = MarkupKind.Markdown,
                                    value = description,
                                ),
                            ),
                    )
                } else if (trimmed.startsWith("#")) {
                    Hover(
                        contents =
                            HoverContents.Markup(
                                MarkupContent(
                                    kind = MarkupKind.Markdown,
                                    value = "**Section Header** - Defines a section in the note.",
                                ),
                            ),
                    )
                } else {
                    null
                }
            }

            // -- Document Symbols -------------------------------------------------

            documentSymbol { params ->
                val uri = params.textDocument.uri
                val text = documents[uri] ?: return@documentSymbol null

                text.lines().mapIndexedNotNull { index, line ->
                    val trimmed = line.trimStart()
                    when {
                        trimmed.startsWith("# ") ->
                            DocumentSymbol(
                                name = trimmed.removePrefix("# ").trim(),
                                kind = SymbolKind.Namespace,
                                range = Range(Position(index, 0), Position(index, line.length)),
                                selectionRange = Range(Position(index, 0), Position(index, line.length)),
                            )
                        keywords.any { trimmed.startsWith("$it:") } -> {
                            val keyword = keywords.first { trimmed.startsWith("$it:") }
                            val symbolKind =
                                when (keyword) {
                                    "TODO", "FIXME" -> SymbolKind.Event
                                    "DONE" -> SymbolKind.Constant
                                    else -> SymbolKind.Property
                                }
                            DocumentSymbol(
                                name = trimmed,
                                kind = symbolKind,
                                range = Range(Position(index, 0), Position(index, line.length)),
                                selectionRange = Range(Position(index, 0), Position(index, line.length)),
                            )
                        }
                        else -> null
                    }
                }
            }

            // -- Formatting -------------------------------------------------------

            formatting { params ->
                val uri = params.textDocument.uri
                val text = documents[uri] ?: return@formatting null

                val lines = text.lines()
                val formatted =
                    lines.mapIndexed { index, line ->
                        var result = line.trimEnd()
                        // Normalize keyword casing
                        keywords.forEach { keyword ->
                            val regex = Regex("^(\\s*)${Regex.escape(keyword)}:", RegexOption.IGNORE_CASE)
                            result = result.replace(regex, "$1$keyword:")
                        }
                        // Ensure space after keyword colon
                        keywords.forEach { keyword ->
                            if (result.trimStart().startsWith("$keyword:") &&
                                !result.trimStart().startsWith("$keyword: ")
                            ) {
                                result = result.replace("$keyword:", "$keyword: ")
                            }
                        }
                        result
                    }

                val formattedText = formatted.joinToString("\n")
                if (formattedText != text) {
                    listOf(
                        TextEdit(
                            range =
                                Range(
                                    Position(0, 0),
                                    Position(lines.size, 0),
                                ),
                            newText = formattedText,
                        ),
                    )
                } else {
                    null
                }
            }
        }
    }

// =============================================================================
// Diagnostics
// =============================================================================

/**
 * Reference to the running server instance, used for publishing diagnostics.
 *
 * Set after [LanguageServerConfig.start] returns.
 */
private var serverInstance: LanguageServer? = null

private suspend fun publishDiagnosticsFor(
    uri: String,
    text: String,
) {
    val diagnostics =
        buildList {
            text.lines().forEachIndexed { index, line ->
                val trimmed = line.trimStart()

                // Warn on TODO items
                if (trimmed.startsWith("TODO:")) {
                    add(
                        Diagnostic(
                            range = Range(Position(index, 0), Position(index, line.length)),
                            severity = DiagnosticSeverity.Information,
                            source = "note-ls",
                            message = "Pending TODO item",
                        ),
                    )
                }

                // Warn on FIXME items
                if (trimmed.startsWith("FIXME:")) {
                    add(
                        Diagnostic(
                            range = Range(Position(index, 0), Position(index, line.length)),
                            severity = DiagnosticSeverity.Warning,
                            source = "note-ls",
                            message = "FIXME: needs attention",
                        ),
                    )
                }

                // Error on empty section headers
                if (trimmed == "#" || trimmed == "# ") {
                    add(
                        Diagnostic(
                            range = Range(Position(index, 0), Position(index, line.length)),
                            severity = DiagnosticSeverity.Error,
                            source = "note-ls",
                            message = "Section header must have a title",
                        ),
                    )
                }
            }
        }

    serverInstance?.client?.publishDiagnostics(
        PublishDiagnosticsParams(uri = uri, diagnostics = diagnostics),
    )
}

// =============================================================================
// Entry Point
// =============================================================================

fun main(): Unit =
    runBlocking {
        val config = createNoteServer()
        val transport = TransportFactory.stdio()
        serverInstance = config.start(transport)
        serverInstance?.client?.showMessage(MessageType.Info, "Note Language Server started")
    }
