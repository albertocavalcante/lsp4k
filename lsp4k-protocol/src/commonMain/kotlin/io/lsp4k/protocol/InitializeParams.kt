package io.lsp4k.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Workspace folder.
 */
@Serializable
public data class WorkspaceFolder(
    /**
     * The associated URI for this workspace folder.
     */
    val uri: DocumentUri,
    /**
     * The name of the workspace folder.
     */
    val name: String,
)

/**
 * Client info sent during initialization.
 */
@Serializable
public data class ClientInfo(
    /**
     * The name of the client as defined by the client.
     */
    val name: String,
    /**
     * The client's version as defined by the client.
     */
    val version: String? = null,
)

/**
 * Server info sent during initialization.
 */
@Serializable
public data class ServerInfo(
    /**
     * The name of the server as defined by the server.
     */
    val name: String,
    /**
     * The server's version as defined by the server.
     */
    val version: String? = null,
)

/**
 * Initialize request params.
 */
@Serializable
public data class InitializeParams(
    /**
     * The process Id of the parent process that started the server.
     * Is null if the process has not been started by another process.
     */
    val processId: Int?,
    /**
     * Information about the client.
     */
    val clientInfo: ClientInfo? = null,
    /**
     * The locale the client is currently showing the user interface in.
     */
    val locale: String? = null,
    /**
     * The rootPath of the workspace. Is null if no folder is open.
     * @deprecated Use rootUri instead.
     */
    val rootPath: String? = null,
    /**
     * The rootUri of the workspace. Is null if no folder is open.
     */
    val rootUri: DocumentUri?,
    /**
     * User provided initialization options.
     */
    val initializationOptions: JsonElement? = null,
    /**
     * The capabilities provided by the client (editor or tool).
     */
    val capabilities: ClientCapabilities,
    /**
     * The initial trace setting.
     */
    val trace: TraceValue? = null,
    /**
     * The workspace folders configured in the client when the server starts.
     */
    val workspaceFolders: List<WorkspaceFolder>? = null,
)

/**
 * Trace values.
 */
@Serializable
public enum class TraceValue {
    @SerialName("off")
    Off,

    @SerialName("messages")
    Messages,

    @SerialName("verbose")
    Verbose,
}

/**
 * Initialize result.
 */
@Serializable
public data class InitializeResult(
    /**
     * The capabilities the language server provides.
     */
    val capabilities: ServerCapabilities,
    /**
     * Information about the server.
     */
    val serverInfo: ServerInfo? = null,
)

/**
 * Initialize error data.
 */
@Serializable
public data class InitializeError(
    /**
     * Indicates whether the client execute the following retry logic:
     * (1) show the message provided by the ResponseError to the user
     * (2) user selects retry or cancel
     * (3) if user selected retry the initialize method is sent again.
     */
    val retry: Boolean,
)
