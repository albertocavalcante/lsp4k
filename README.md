# lsp4k

Idiomatic Kotlin Multiplatform library for the [Language Server Protocol (LSP)](https://microsoft.github.io/language-server-protocol/).

## Features

- **Pure Kotlin** - No Java dependencies, designed for Kotlin from the ground up
- **Multiplatform** - JVM, JS (Node.js and browser) and Kotlin/Native targets; see [Targets](#targets) for which ones CI runs
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

## Specification, targets and transports

### Specification version

lsp4k implements [LSP 3.18](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/),
including its proposed (still unfinalized) items such as inline completion, text document content and
`rangesFormatting`. Proposed items follow the published meta model and can change when the specification does.

Conformance is checked against the specification's machine-readable `metaModel.json`: the server and client API
coverage tests compare the handler hooks and typed calls with the method list taken from the 3.18 meta model
(`MetaModelMethods` in the `lsp4k-server` tests), so a method missing from either side fails `./gradlew check`.
The meta model's structures are not yet diffed against the `lsp4k-protocol` types automatically.

### Targets

| Target | What CI does |
|--------|--------------|
| JVM (toolchain 17, published for Java 17) | Runs all tests, including the end-to-end TCP session test (`TcpEndToEndTest`), on Linux; a separate job builds a Java 17 consumer |
| JS (Node.js, browser) | Part of `./gradlew check` on the Linux job |
| macOS native (`macosArm64`, `macosX64`) | `./gradlew check` on a macOS runner with Apple targets enabled |
| `linuxX64` | Tests run on the Linux job |
| `linuxArm64` | Compile-only: there is no Linux arm64 job, so its tests never execute |
| `mingwX64` (Windows) | Compile-only: lsp4k has no Windows job, so its tests never execute |

Compile-only targets are built by `check` but their behaviour is unverified here. The end-to-end session test
is JVM only.

### Transports

lsp4k does not frame bytes itself; `start(transport)` takes a `jsonrpc4k` `Transport` and speaks `Content-Length`
framed JSON-RPC over it. jsonrpc4k provides stdio (`TransportFactory.stdio()`), TCP client and listener
(`TcpSocket`, `TcpListener`), Unix sockets, named pipes (Node.js on Windows), Node IPC, a child process
(`ProcessTransport`) and an in-memory pair (`InMemoryTransport.pair()`). Which of them each platform supports is
tabulated in the [jsonrpc4k README](https://github.com/albertocavalcante/jsonrpc4k#transports-by-platform); an
unsupported combination throws `UnsupportedTransportException`.

```kotlin
val listener = TcpListener.bind("127.0.0.1", 0)           // server process
val server = config.start(listener.accept())

val session = clientConfig.start(TcpSocket.connect("127.0.0.1", port)) // client process
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

### Position encoding negotiation

Positions count UTF-16 code units unless the client and server agree on `utf-8` or `utf-32`. A client lists
the encodings it supports in `general.positionEncodings`; the server answers with `positionEncoding`. Use
`negotiatePositionEncoding` for the decision: the first entry of the server's preference that the client
offers wins, and UTF-16 is the result when the client lists nothing or nothing matches.

```kotlin
negotiatePositionEncoding(listOf(PositionEncodingKind.UTF16, PositionEncodingKind.UTF8), listOf(UTF8, UTF16)) // UTF8
negotiatePositionEncoding(null, listOf(UTF8, UTF16)) // UTF16: the client offered nothing
```

The builder wires it into the default `initialize` result, and the outcome is `server.positionEncoding` once
`initialize` was answered, so `TextDocuments` can be created in the `initialized` hook:

```kotlin
val documents = CompletableDeferred<TextDocuments>()

languageServer {
    negotiatePositionEncoding(PositionEncodingKind.UTF8, PositionEncodingKind.UTF16)
    onInitialized { documents.complete(TextDocuments(server.positionEncoding)) }
    textDocument { didOpen { documents.await().didOpen(it) } }
}
```

A server that builds its own result in `onInitialize` calls `params.negotiatePositionEncoding(preference)` and
passes the value to `TextDocuments(encoding)`; `TextDocuments(params, preference)` and
`TextDocuments(serverCapabilities)` are shorthands for the same.

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

Nothing is published yet, so build from source. Clone `lsp4k` next to a `jsonrpc4k` checkout (the build uses
the sibling `../jsonrpc4k` automatically). Then either include lsp4k in your build as a composite build, in
your `settings.gradle.kts`:

```kotlin
includeBuild("../lsp4k")
```

```kotlin
dependencies {
    implementation("io.lsp4k:lsp4k-server") // resolved from the included build
}
```

or publish to your local Maven repository with `./gradlew publishToMavenLocal`, add `mavenLocal()` to your
repositories and depend on `io.lsp4k:lsp4k-server:0.1.0-SNAPSHOT`. No remote repository serves these artifacts.

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
