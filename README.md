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

Inbound messages are dispatched concurrently with bounded limits (jsonrpc4k `BoundedDispatchPolicy`; the
default allows 16 running request handlers and 1024 queued messages). Pass `policy = ...` and optionally
`inboundLimits = ...` to `start` to change them; `BoundedDispatchPolicy(1, 1024)` runs one request at a time.
The same holds for `languageClient {}.start(...)`.

- Requests run concurrently. A request that arrives while the queue is full is answered with a server busy error.
- Notifications are barriers: every earlier request finishes before a notification runs, and later messages
  wait for it. `didOpen`, `didChange` and `didClose` therefore run in wire order, and a request sees the effect
  of every notification sent before it.
- `$/cancelRequest` cancels the handler of the request it names, which is answered with `RequestCancelled`
  (-32800). Cancelling a request to the client, or letting it time out, sends `$/cancelRequest` to the peer.
- A handler may await client requests such as `client.workspace.configuration`: responses are read outside the
  dispatch queue. A chain of nested waits can still use up the concurrency limit.
- Because notifications wait for earlier requests, a running request handler does not see
  `window/workDoneProgress/cancel`: its `workDoneProgress.isCancelled` flips only after the handler returned.
  Work that must react to cancellation should use server initiated progress, as above.

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

## Client

`lsp4k-client` covers every LSP 3.18 method in both directions. Calls to the server are grouped by
namespace on `session.server`; handlers for server requests and notifications are registered by
namespace in the `languageClient` DSL.

```kotlin
val config = languageClient {
    clientInfo("my-editor", "1.0.0")

    window {
        showMessage { params -> println(params.message) }
        showMessageRequest { params -> params.actions?.firstOrNull() }
        workDoneProgressCreate { params -> /* subscribe with session.progress.workDone(params.token) */ }
    }
    workspace {
        applyEdit { ApplyWorkspaceEditResult(applied = true) }
        configuration { params -> params.items.map { JsonNull } }
        semanticTokensRefresh { /* re-request tokens */ }
    }
    textDocument { publishDiagnostics { params -> render(params.uri, params.diagnostics) } }
}

val session = config.start(transport)
session.initialize()
session.server.textDocument.didOpen(DidOpenTextDocumentParams(item))
val hover = session.server.textDocument.hover(HoverParams(document, position))
val tokens = session.server.insights.semanticTokensFull(SemanticTokensParams(document))
session.shutdown()
```

Progress arrives as flows keyed by token. Start collecting before the request that carries the token:

```kotlin
val locations = async(start = CoroutineStart.UNDISPATCHED) {
    session.progress.partialResults<List<Location>>(token).toList()
}
session.server.textDocument.references(ReferenceParams(document, position, context, partialResultToken = token))
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

`lsp4k-server` and `lsp4k-client` depend on `jsonrpc4k`, and the `lsp4k-protocol` JVM tests use it to
check the wire `Json` configuration. Local development uses
a sibling `../jsonrpc4k` checkout automatically when present. Use
`-Plsp4k.jsonrpc4kBuild=/path/to/jsonrpc4k` to point at another checkout, or
`-Plsp4k.useLocalJsonrpc4k=false` to force resolution from published artifacts.
For GitHub Packages, provide `gpr.user`/`gpr.key` Gradle properties or
`GITHUB_ACTOR`/`GITHUB_TOKEN` environment variables.

### Specification conformance

`check` runs two JVM tests against the LSP 3.18 meta model vendored, and pinned by SHA-256, at
`lsp4k-protocol/src/jvmTest/resources/lsp/3.18/metaModel.json`:

- `MetaModelConformanceTest` (`lsp4k-protocol`) maps every structure, enumeration, type alias, request and
  notification to its lsp4k serializer through an explicit table, then checks JSON property names, optionality,
  nullability, enumeration values, method directions and wire round trips under the production `Json`
  configuration. Known gaps belong in `conformance-allowlist.txt` next to the meta model; the test fails for a
  violation that is not listed and for a listed one that no longer occurs.
- `MethodCoverageTest` (`lsp4k-server`) checks that every method has a server hook and a client call, or a server
  call and a client hook, as its direction requires.

## License

Apache License 2.0
