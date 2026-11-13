# lsp4k

Idiomatic Kotlin Multiplatform library for the [Language Server Protocol (LSP)](https://microsoft.github.io/language-server-protocol/).

## Features

- **Pure Kotlin** - No Java dependencies, designed for Kotlin from the ground up
- **Multiplatform** - JVM, Native (macOS, Linux, Windows), and JS targets
- **kotlinx-serialization** - Type-safe JSON serialization with `@Serializable` data classes
- **Coroutines** - Async-first API with `suspend` functions and `Flow`
- **DSL-based** - Intuitive builder DSL for server configuration
- **JSON-RPC transport integration** - Uses `jsonrpc4k` for LSP message framing and transports

## Modules

| Module | Description |
|--------|-------------|
| `lsp4k-protocol` | LSP type definitions (`Position`, `Range`, `Diagnostic`, etc.) |
| `lsp4k-server` | Server-side abstractions and DSL |
| `lsp4k-client` | Client-side abstractions (for testing, editors) |
| `example` | Example language server application |

## Quick Start

```kotlin
val config = languageServer {
    serverInfo("my-server", "1.0.0")

    capabilities {
        textDocumentSync = TextDocumentSyncKind.Incremental
        completionProvider = CompletionOptions(triggerCharacters = listOf(".", ":"))
        hoverProvider = true
    }

    onInitialized {
        client.window.logMessage(MessageType.Info, "ready")
    }

    textDocument {
        didOpen { params ->
            client.textDocument.publishDiagnostics(
                PublishDiagnosticsParams(uri = params.textDocument.uri, diagnostics = emptyList()),
            )
        }

        completion { params ->
            CompletionResult.ListResult(
                CompletionList(
                    isIncomplete = false,
                    items = listOf(CompletionItem(label = "hello", kind = CompletionItemKind.Text)),
                ),
            )
        }

        hover { params ->
            Hover(HoverContents.Markup(MarkupContent(kind = MarkupKind.Markdown, value = "**Hello** from lsp4k!")))
        }
    }
}

fun main() {
    val exitCode = runBlocking {
        val server = config.start(TransportFactory.stdio())
        server.awaitTermination()
        server.exitCode ?: 1 // 0 when the client sent shutdown before exit
    }
    exitProcess(exitCode)
}
```

## Server API

`lsp4k-server` has a handler hook for every client-to-server request and notification of LSP 3.18 and a
typed call for every server-to-client one.

| Handler block | Methods |
|---------------|---------|
| `textDocument { }` | `didOpen`, `didChange`, `didClose`, `didSave`, `willSave`, `willSaveWaitUntil`, `completion`, `completionResolve`, `hover`, `signatureHelp`, `declaration`, `definition`, `typeDefinition`, `implementation`, `references`, `documentHighlight`, `documentSymbol` |
| `editing { }` | `codeAction`, `codeActionResolve`, `codeLens`, `codeLensResolve`, `documentLink`, `documentLinkResolve`, `documentColor`, `colorPresentation`, `formatting`, `rangeFormatting`, `rangesFormatting`, `onTypeFormatting`, `rename`, `prepareRename`, `foldingRange`, `selectionRange`, `linkedEditingRange` |
| `insights { }` | `semanticTokensFull`, `semanticTokensFullDelta`, `semanticTokensRange`, `inlayHint`, `inlayHintResolve`, `diagnostic`, `moniker`, `inlineValue`, `inlineCompletion` |
| `hierarchy { }` | `prepareCallHierarchy`, `callHierarchyIncomingCalls`, `callHierarchyOutgoingCalls`, `prepareTypeHierarchy`, `typeHierarchySupertypes`, `typeHierarchySubtypes` |
| `workspace { }` | `symbol`, `workspaceSymbolResolve`, `executeCommand`, `workspaceDiagnostic`, `textDocumentContent`, `didChangeConfiguration`, `didChangeWatchedFiles`, `didChangeWorkspaceFolders`, `willCreateFiles`, `didCreateFiles`, `willRenameFiles`, `didRenameFiles`, `willDeleteFiles`, `didDeleteFiles` |
| `notebook { }` | `didOpen`, `didChange`, `didSave`, `didClose` |
| `window { }` | `workDoneProgressCancel` |
| top level | `onInitialize`, `onInitialized`, `onShutdown`, `onExit`, `onSetTrace`, `onProgress`, `onRequest`, `onNotification` |

Handlers run with a receiver that exposes `client` and `server`. Request handlers also get the request's
work-done progress, and handlers of requests that support partial results get `sendPartialResult`:

```kotlin
textDocument {
    references { params ->
        workDoneProgress?.begin("Searching references")
        for (chunk in findReferences(params)) {
            if (!sendPartialResult(chunk)) return@references findAllReferences(params)
        }
        emptyList() // the final result is empty once partial results were sent
    }
}
```

Calls to the client are grouped the same way: `client.window` (`showMessage`, `showMessageRequest`,
`logMessage`, `showDocument`, `createWorkDoneProgress`), `client.workspace` (`applyEdit`, `configuration`,
`workspaceFolders`, every refresh request), `client.textDocument.publishDiagnostics`, `client.registration`
(`registerCapability`, `unregisterCapability`, and a `register` helper returning a handle that unregisters),
plus `telemetryEvent`, `logTrace` (filtered by the trace level from `initialize` and `$/setTrace`), `progress`
and `cancelRequest`.

```kotlin
appScope.launch {
    val progress = server.client.window.createWorkDoneProgress()
    progress.begin("Indexing", cancellable = true)
    for ((index, file) in files.withIndex()) {
        if (progress.isCancelled) break
        index(file)
        progress.report(percentage = index * 100 / files.size)
    }
    progress.end()
}
```

The receive loop dispatches one message at a time, so awaiting a client request inside a handler would never
see its response; such calls fail at once with `IllegalStateException`. Launch them in your own scope instead.
The one exception is the `onInitialized` hook, which runs outside the loop and may call
`client.registration.register`, `client.workspace.configuration` and the like. For the same reason a request
handler does not see `window/workDoneProgress/cancel` while it runs: its `workDoneProgress.isCancelled` flips only
after the handler returned. Work that must react to cancellation should use server initiated progress, as above.

The server enforces the lifecycle: requests before `initialize` fail with `ServerNotInitialized`, a second
`initialize` and every request after `shutdown` fail with `InvalidRequest`, notifications other than `exit`
are dropped in those states, and `exit` records `exitCode` (0 after `shutdown`, otherwise 1) for the
application to act on.

### Tracking open documents

`TextDocuments` keeps the text of open documents in sync, including incremental edits in the
negotiated position encoding (UTF-16, UTF-8 or UTF-32). All operations are `suspend` and guarded by an
internal mutex, so concurrently dispatched notifications are safe. Changes whose version is not newer
than the tracked one are dropped and reported as `ChangeResult.STALE_VERSION`.

```kotlin
// Use the encoding your initialize result advertises (InitializeResult.capabilities.positionEncoding);
// UTF-16 is the protocol default when none is negotiated.
val negotiated: PositionEncodingKind? = initializeResult.capabilities.positionEncoding
val documents = TextDocuments(negotiated ?: PositionEncodingKind.UTF16)

languageServer {
    textDocument {
        didOpen { documents.didOpen(it) }
        didChange { documents.didChange(it) }
        didClose { documents.didClose(it) }
    }
}

val doc = documents.get(uri) // suspend; returns an immutable snapshot
val offset = doc?.offsetAt(Position(line = 3, character = 5))
```

## Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.lsp4k:lsp4k-server:0.1.0")
}
```

## Building

```sh
./gradlew check
```

On macOS, Apple native targets are enabled when a full Xcode is selectable by
`xcrun xcodebuild -version`. If Xcode is installed outside `/Applications`,
select it with `xcode-select` or launch Gradle with `DEVELOPER_DIR`:

```sh
DEVELOPER_DIR=/path/to/Xcode.app/Contents/Developer ./gradlew check
```

Use `-Plsp4k.enableAppleTargets=true` in CI when Apple native targets must be
present; the build fails during configuration if Xcode is not available.

`lsp4k-server` and `lsp4k-client` depend on `jsonrpc4k`. Local development uses
a sibling `../jsonrpc4k` checkout automatically when present. Use
`-Plsp4k.jsonrpc4kBuild=/path/to/jsonrpc4k` to point at another checkout, or
`-Plsp4k.useLocalJsonrpc4k=false` to force resolution from published artifacts.
For GitHub Packages, provide `gpr.user`/`gpr.key` Gradle properties or
`GITHUB_ACTOR`/`GITHUB_TOKEN` environment variables.

## License

Apache License 2.0
