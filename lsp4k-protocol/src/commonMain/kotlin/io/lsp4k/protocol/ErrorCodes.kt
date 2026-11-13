package io.lsp4k.protocol

/**
 * Predefined JSON-RPC error codes used by LSP (meta model enumeration `ErrorCodes`).
 *
 * The set is open: servers and clients may use any other integer code.
 */
public object ErrorCodes {
    /** Invalid JSON was received by the server. */
    public const val PARSE_ERROR: Int = -32700

    /** The JSON sent is not a valid request object. */
    public const val INVALID_REQUEST: Int = -32600

    /** The method does not exist or is not available. */
    public const val METHOD_NOT_FOUND: Int = -32601

    /** Invalid method parameters. */
    public const val INVALID_PARAMS: Int = -32602

    /** Internal JSON-RPC error. */
    public const val INTERNAL_ERROR: Int = -32603

    /** Error code indicating that a server received a request before the `initialize` request. */
    public const val SERVER_NOT_INITIALIZED: Int = -32002

    /** Unknown error code. */
    public const val UNKNOWN_ERROR_CODE: Int = -32001
}

/**
 * Error codes defined by LSP itself (meta model enumeration `LSPErrorCodes`).
 *
 * The set is open: servers and clients may use any other integer code.
 */
public object LSPErrorCodes {
    /** A request failed but it was syntactically correct and the server was able to process it. */
    public const val REQUEST_FAILED: Int = -32803

    /** The server cancelled the request. */
    public const val SERVER_CANCELLED: Int = -32802

    /** The server detected that the content of a document got modified outside normal conditions. */
    public const val CONTENT_MODIFIED: Int = -32801

    /** The client has canceled a request and a server has detected the cancel. */
    public const val REQUEST_CANCELLED: Int = -32800
}
