package io.lsp4k.transport

/**
 * Native implementation of TransportFactory.
 *
 * Note: Full native transport implementation requires platform-specific APIs.
 * This is a placeholder that throws UnsupportedOperationException.
 */
public actual object TransportFactory {
    public actual fun stdio(): Transport =
        throw UnsupportedOperationException(
            "Native stdio transport not yet implemented. " +
                "Requires platform-specific stdin/stdout handling.",
        )

    public actual fun socket(
        host: String,
        port: Int,
    ): Transport =
        throw UnsupportedOperationException(
            "Native socket transport not yet implemented. " +
                "Requires platform-specific socket APIs (POSIX or Windows).",
        )
}
