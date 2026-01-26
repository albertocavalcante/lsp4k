package io.lsp4k.transport

/**
 * JS implementation of TransportFactory.
 *
 * Note: Full JS transport implementation requires Node.js or browser-specific APIs.
 * This is a placeholder that throws UnsupportedOperationException.
 */
public actual object TransportFactory {
    public actual fun stdio(): Transport =
        throw UnsupportedOperationException(
            "JS stdio transport not yet implemented. " +
                "Use Node.js process.stdin/stdout or browser-specific APIs.",
        )

    public actual fun socket(
        host: String,
        port: Int,
    ): Transport =
        throw UnsupportedOperationException(
            "JS socket transport not yet implemented. " +
                "Use Node.js net module or browser WebSocket.",
        )
}
