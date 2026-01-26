# lsp4k

Idiomatic Kotlin Multiplatform library for the [Language Server Protocol (LSP)](https://microsoft.github.io/language-server-protocol/).

## Features

- **Pure Kotlin** - No Java dependencies, designed for Kotlin from the ground up
- **Multiplatform** - JVM, Native (macOS, Linux, Windows), and JS targets
- **kotlinx-serialization** - Type-safe JSON serialization with `@Serializable` data classes
- **Coroutines** - Async-first API with `suspend` functions and `Flow`
- **DSL-based** - Intuitive builder DSL for server configuration

## Modules

| Module | Description |
|--------|-------------|
| `lsp4k-protocol` | LSP type definitions (`Position`, `Range`, `Diagnostic`, etc.) |
| `lsp4k-jsonrpc` | JSON-RPC 2.0 implementation with LSP framing |
| `lsp4k-transport` | Transport abstractions (stdio, socket) |
| `lsp4k-server` | Server-side abstractions and DSL |
| `lsp4k-client` | Client-side abstractions (for testing, editors) |

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

```bash
./gradlew build
```

## License

Apache License 2.0
