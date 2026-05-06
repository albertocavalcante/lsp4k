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
        completionProvider = CompletionOptions(
            triggerCharacters = listOf(".", ":")
        )
        hoverProvider = true
    }

    textDocument {
        completion { params ->
            CompletionList(
                isIncomplete = false,
                items = listOf(
                    CompletionItem(label = "hello", kind = CompletionItemKind.Text)
                )
            )
        }

        hover { params ->
            Hover(
                contents = MarkupContent(
                    kind = MarkupKind.Markdown,
                    value = "**Hello** from lsp4k!"
                )
            )
        }

        didOpen { params ->
            // Handle document open
        }
    }
}

// Start with stdio transport
val transport = TransportFactory.stdio()
val server = config.start(transport)
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
