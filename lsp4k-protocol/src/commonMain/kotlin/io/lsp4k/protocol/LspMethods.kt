package io.lsp4k.protocol

/**
 * LSP method name constants for all standard Language Server Protocol methods.
 */
public object LspMethods {
    // -- Lifecycle --

    /** `initialize` - Sent from client to server to begin initialization. */
    public const val INITIALIZE: String = "initialize"

    /** `initialized` - Sent from client to server after the client received the initialize result. */
    public const val INITIALIZED: String = "initialized"

    /** `shutdown` - Sent from client to server to ask the server to shut down. */
    public const val SHUTDOWN: String = "shutdown"

    /** `exit` - Notification to ask the server to exit its process. */
    public const val EXIT: String = "exit"

    // -- Text Document Synchronization --

    /** `textDocument/didOpen` - Notification sent when a text document is opened. */
    public const val TEXT_DOCUMENT_DID_OPEN: String = "textDocument/didOpen"

    /** `textDocument/didClose` - Notification sent when a text document is closed. */
    public const val TEXT_DOCUMENT_DID_CLOSE: String = "textDocument/didClose"

    /** `textDocument/didChange` - Notification sent when a text document is changed. */
    public const val TEXT_DOCUMENT_DID_CHANGE: String = "textDocument/didChange"

    /** `textDocument/didSave` - Notification sent when a text document is saved. */
    public const val TEXT_DOCUMENT_DID_SAVE: String = "textDocument/didSave"

    /** `textDocument/willSave` - Notification sent before a document is saved. */
    public const val TEXT_DOCUMENT_WILL_SAVE: String = "textDocument/willSave"

    /** `textDocument/willSaveWaitUntil` - Request sent before a document is saved, allowing edits. */
    public const val TEXT_DOCUMENT_WILL_SAVE_WAIT_UNTIL: String = "textDocument/willSaveWaitUntil"

    // -- Language Features --

    /** `textDocument/completion` - Request for completion items at a given cursor position. */
    public const val TEXT_DOCUMENT_COMPLETION: String = "textDocument/completion"

    /** `completionItem/resolve` - Request to resolve additional information for a completion item. */
    public const val COMPLETION_ITEM_RESOLVE: String = "completionItem/resolve"

    /** `textDocument/hover` - Request for hover information at a given position. */
    public const val TEXT_DOCUMENT_HOVER: String = "textDocument/hover"

    /** `textDocument/signatureHelp` - Request for signature help at a given cursor position. */
    public const val TEXT_DOCUMENT_SIGNATURE_HELP: String = "textDocument/signatureHelp"

    /** `textDocument/declaration` - Request to go to the declaration of a symbol. */
    public const val TEXT_DOCUMENT_DECLARATION: String = "textDocument/declaration"

    /** `textDocument/definition` - Request to go to the definition of a symbol. */
    public const val TEXT_DOCUMENT_DEFINITION: String = "textDocument/definition"

    /** `textDocument/typeDefinition` - Request to go to the type definition of a symbol. */
    public const val TEXT_DOCUMENT_TYPE_DEFINITION: String = "textDocument/typeDefinition"

    /** `textDocument/implementation` - Request to go to the implementation of a symbol. */
    public const val TEXT_DOCUMENT_IMPLEMENTATION: String = "textDocument/implementation"

    /** `textDocument/references` - Request to find all references of a symbol. */
    public const val TEXT_DOCUMENT_REFERENCES: String = "textDocument/references"

    /** `textDocument/documentHighlight` - Request to highlight all occurrences of a symbol in a document. */
    public const val TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT: String = "textDocument/documentHighlight"

    /** `textDocument/documentSymbol` - Request for all symbols in a document. */
    public const val TEXT_DOCUMENT_DOCUMENT_SYMBOL: String = "textDocument/documentSymbol"

    /** `textDocument/codeAction` - Request for code actions at a given range or position. */
    public const val TEXT_DOCUMENT_CODE_ACTION: String = "textDocument/codeAction"

    /** `codeAction/resolve` - Request to resolve additional information for a code action. */
    public const val CODE_ACTION_RESOLVE: String = "codeAction/resolve"

    /** `textDocument/codeLens` - Request for code lenses in a document. */
    public const val TEXT_DOCUMENT_CODE_LENS: String = "textDocument/codeLens"

    /** `codeLens/resolve` - Request to resolve a code lens command. */
    public const val CODE_LENS_RESOLVE: String = "codeLens/resolve"

    /** `textDocument/documentLink` - Request for document links in a document. */
    public const val TEXT_DOCUMENT_DOCUMENT_LINK: String = "textDocument/documentLink"

    /** `documentLink/resolve` - Request to resolve the target of a document link. */
    public const val DOCUMENT_LINK_RESOLVE: String = "documentLink/resolve"

    /** `textDocument/documentColor` - Request for color information in a document. */
    public const val TEXT_DOCUMENT_DOCUMENT_COLOR: String = "textDocument/documentColor"

    /** `textDocument/colorPresentation` - Request for color presentation options. */
    public const val TEXT_DOCUMENT_COLOR_PRESENTATION: String = "textDocument/colorPresentation"

    // -- Formatting --

    /** `textDocument/formatting` - Request to format an entire document. */
    public const val TEXT_DOCUMENT_FORMATTING: String = "textDocument/formatting"

    /** `textDocument/rangeFormatting` - Request to format a given range in a document. */
    public const val TEXT_DOCUMENT_RANGE_FORMATTING: String = "textDocument/rangeFormatting"

    /** `textDocument/rangesFormatting` - Request to format multiple ranges in a document. */
    public const val TEXT_DOCUMENT_RANGES_FORMATTING: String = "textDocument/rangesFormatting"

    /** `textDocument/onTypeFormatting` - Request to format while typing. */
    public const val TEXT_DOCUMENT_ON_TYPE_FORMATTING: String = "textDocument/onTypeFormatting"

    // -- Rename --

    /** `textDocument/rename` - Request to rename a symbol. */
    public const val TEXT_DOCUMENT_RENAME: String = "textDocument/rename"

    /** `textDocument/prepareRename` - Request to check and resolve rename information. */
    public const val TEXT_DOCUMENT_PREPARE_RENAME: String = "textDocument/prepareRename"

    // -- Folding / Selection --

    /** `textDocument/foldingRange` - Request for folding ranges in a document. */
    public const val TEXT_DOCUMENT_FOLDING_RANGE: String = "textDocument/foldingRange"

    /** `textDocument/selectionRange` - Request for selection ranges at given positions. */
    public const val TEXT_DOCUMENT_SELECTION_RANGE: String = "textDocument/selectionRange"

    /** `textDocument/linkedEditingRange` - Request for linked editing ranges. */
    public const val TEXT_DOCUMENT_LINKED_EDITING_RANGE: String = "textDocument/linkedEditingRange"

    // -- Call Hierarchy --

    /** `textDocument/prepareCallHierarchy` - Request to prepare a call hierarchy for a symbol. */
    public const val TEXT_DOCUMENT_PREPARE_CALL_HIERARCHY: String = "textDocument/prepareCallHierarchy"

    /** `callHierarchy/incomingCalls` - Request for incoming calls for a call hierarchy item. */
    public const val CALL_HIERARCHY_INCOMING_CALLS: String = "callHierarchy/incomingCalls"

    /** `callHierarchy/outgoingCalls` - Request for outgoing calls from a call hierarchy item. */
    public const val CALL_HIERARCHY_OUTGOING_CALLS: String = "callHierarchy/outgoingCalls"

    // -- Type Hierarchy --

    /** `textDocument/prepareTypeHierarchy` - Request to prepare a type hierarchy for a symbol. */
    public const val TEXT_DOCUMENT_PREPARE_TYPE_HIERARCHY: String = "textDocument/prepareTypeHierarchy"

    /** `typeHierarchy/supertypes` - Request for supertypes of a type hierarchy item. */
    public const val TYPE_HIERARCHY_SUPERTYPES: String = "typeHierarchy/supertypes"

    /** `typeHierarchy/subtypes` - Request for subtypes of a type hierarchy item. */
    public const val TYPE_HIERARCHY_SUBTYPES: String = "typeHierarchy/subtypes"

    // -- Semantic Tokens --

    /** `textDocument/semanticTokens/full` - Request for full semantic tokens of a document. */
    public const val TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL: String = "textDocument/semanticTokens/full"

    /** `textDocument/semanticTokens/full/delta` - Request for semantic token deltas since a previous result. */
    public const val TEXT_DOCUMENT_SEMANTIC_TOKENS_FULL_DELTA: String = "textDocument/semanticTokens/full/delta"

    /** `textDocument/semanticTokens/range` - Request for semantic tokens in a range. */
    public const val TEXT_DOCUMENT_SEMANTIC_TOKENS_RANGE: String = "textDocument/semanticTokens/range"

    // -- Inlay Hints --

    /** `textDocument/inlayHint` - Request for inlay hints in a range. */
    public const val TEXT_DOCUMENT_INLAY_HINT: String = "textDocument/inlayHint"

    /** `inlayHint/resolve` - Request to resolve additional information for an inlay hint. */
    public const val INLAY_HINT_RESOLVE: String = "inlayHint/resolve"

    // -- Diagnostics --

    /** `textDocument/publishDiagnostics` - Notification sent from server to client with diagnostics. */
    public const val TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS: String = "textDocument/publishDiagnostics"

    /** `textDocument/diagnostic` - Request to pull diagnostics for a document. */
    public const val TEXT_DOCUMENT_DIAGNOSTIC: String = "textDocument/diagnostic"

    // -- Moniker / Inline --

    /** `textDocument/moniker` - Request for the moniker of a symbol. */
    public const val TEXT_DOCUMENT_MONIKER: String = "textDocument/moniker"

    /** `textDocument/inlineValue` - Request for inline values in a given range. */
    public const val TEXT_DOCUMENT_INLINE_VALUE: String = "textDocument/inlineValue"

    /** `textDocument/inlineCompletion` - Request for inline completion items. */
    public const val TEXT_DOCUMENT_INLINE_COMPLETION: String = "textDocument/inlineCompletion"

    // -- Workspace --

    /** `workspace/didChangeConfiguration` - Notification of configuration changes. */
    public const val WORKSPACE_DID_CHANGE_CONFIGURATION: String = "workspace/didChangeConfiguration"

    /** `workspace/didChangeWatchedFiles` - Notification when watched files change. */
    public const val WORKSPACE_DID_CHANGE_WATCHED_FILES: String = "workspace/didChangeWatchedFiles"

    /** `workspace/symbol` - Request to list project-wide symbols matching a query string. */
    public const val WORKSPACE_SYMBOL: String = "workspace/symbol"

    /** `workspaceSymbol/resolve` - Request to resolve additional info for a workspace symbol. */
    public const val WORKSPACE_SYMBOL_RESOLVE: String = "workspaceSymbol/resolve"

    /** `workspace/executeCommand` - Request to execute a server command. */
    public const val WORKSPACE_EXECUTE_COMMAND: String = "workspace/executeCommand"

    /** `workspace/applyEdit` - Request from server to client to apply a workspace edit. */
    public const val WORKSPACE_APPLY_EDIT: String = "workspace/applyEdit"

    /** `workspace/didChangeWorkspaceFolders` - Notification when workspace folders change. */
    public const val WORKSPACE_DID_CHANGE_WORKSPACE_FOLDERS: String = "workspace/didChangeWorkspaceFolders"

    /** `workspace/configuration` - Request from server to client to fetch configuration. */
    public const val WORKSPACE_CONFIGURATION: String = "workspace/configuration"

    /** `workspace/workspaceFolders` - Request from server to client for workspace folders. */
    public const val WORKSPACE_WORKSPACE_FOLDERS: String = "workspace/workspaceFolders"

    /** `workspace/codeLens/refresh` - Request from server to client to refresh code lenses. */
    public const val WORKSPACE_CODE_LENS_REFRESH: String = "workspace/codeLens/refresh"

    /** `workspace/semanticTokens/refresh` - Request from server to client to refresh semantic tokens. */
    public const val WORKSPACE_SEMANTIC_TOKENS_REFRESH: String = "workspace/semanticTokens/refresh"

    /** `workspace/inlayHint/refresh` - Request from server to client to refresh inlay hints. */
    public const val WORKSPACE_INLAY_HINT_REFRESH: String = "workspace/inlayHint/refresh"

    /** `workspace/inlineValue/refresh` - Request from server to client to refresh inline values. */
    public const val WORKSPACE_INLINE_VALUE_REFRESH: String = "workspace/inlineValue/refresh"

    /** `workspace/diagnostic/refresh` - Request from server to client to refresh diagnostics. */
    public const val WORKSPACE_DIAGNOSTIC_REFRESH: String = "workspace/diagnostic/refresh"

    /** `workspace/foldingRange/refresh` - Request from server to client to refresh folding ranges. */
    public const val WORKSPACE_FOLDING_RANGE_REFRESH: String = "workspace/foldingRange/refresh"

    /** `workspace/willCreateFiles` - Request sent before files are created. */
    public const val WORKSPACE_WILL_CREATE_FILES: String = "workspace/willCreateFiles"

    /** `workspace/didCreateFiles` - Notification sent after files are created. */
    public const val WORKSPACE_DID_CREATE_FILES: String = "workspace/didCreateFiles"

    /** `workspace/willRenameFiles` - Request sent before files are renamed. */
    public const val WORKSPACE_WILL_RENAME_FILES: String = "workspace/willRenameFiles"

    /** `workspace/didRenameFiles` - Notification sent after files are renamed. */
    public const val WORKSPACE_DID_RENAME_FILES: String = "workspace/didRenameFiles"

    /** `workspace/willDeleteFiles` - Request sent before files are deleted. */
    public const val WORKSPACE_WILL_DELETE_FILES: String = "workspace/willDeleteFiles"

    /** `workspace/didDeleteFiles` - Notification sent after files are deleted. */
    public const val WORKSPACE_DID_DELETE_FILES: String = "workspace/didDeleteFiles"

    /** `workspace/diagnostic` - Request to pull workspace-level diagnostics. */
    public const val WORKSPACE_DIAGNOSTIC: String = "workspace/diagnostic"

    // -- Window --

    /** `window/showMessage` - Notification from server to client to show a message. */
    public const val WINDOW_SHOW_MESSAGE: String = "window/showMessage"

    /** `window/showMessageRequest` - Request from server to client to show a message with actions. */
    public const val WINDOW_SHOW_MESSAGE_REQUEST: String = "window/showMessageRequest"

    /** `window/logMessage` - Notification from server to client to log a message. */
    public const val WINDOW_LOG_MESSAGE: String = "window/logMessage"

    /** `window/workDoneProgress/create` - Request from server to client to create a work done progress. */
    public const val WINDOW_WORK_DONE_PROGRESS_CREATE: String = "window/workDoneProgress/create"

    /** `window/showDocument` - Request from server to client to show a document. */
    public const val WINDOW_SHOW_DOCUMENT: String = "window/showDocument"

    /** `window/workDoneProgress/cancel` - Notification from client to cancel a work done progress. */
    public const val WINDOW_WORK_DONE_PROGRESS_CANCEL: String = "window/workDoneProgress/cancel"

    // -- Client --

    /** `client/registerCapability` - Request from server to client to register new capabilities. */
    public const val CLIENT_REGISTER_CAPABILITY: String = "client/registerCapability"

    /** `client/unregisterCapability` - Request from server to client to unregister capabilities. */
    public const val CLIENT_UNREGISTER_CAPABILITY: String = "client/unregisterCapability"

    // -- Cancellation and Progress --

    /** `$/cancelRequest` - Notification to cancel a pending request. */
    public const val CANCEL_REQUEST: String = "$/cancelRequest"

    /** `$/progress` - Notification for reporting progress. */
    public const val PROGRESS: String = "$/progress"

    // -- Notebook Document --

    /** `notebookDocument/didOpen` - Notification sent when a notebook document is opened. */
    public const val NOTEBOOK_DOCUMENT_DID_OPEN: String = "notebookDocument/didOpen"

    /** `notebookDocument/didChange` - Notification sent when a notebook document is changed. */
    public const val NOTEBOOK_DOCUMENT_DID_CHANGE: String = "notebookDocument/didChange"

    /** `notebookDocument/didSave` - Notification sent when a notebook document is saved. */
    public const val NOTEBOOK_DOCUMENT_DID_SAVE: String = "notebookDocument/didSave"

    /** `notebookDocument/didClose` - Notification sent when a notebook document is closed. */
    public const val NOTEBOOK_DOCUMENT_DID_CLOSE: String = "notebookDocument/didClose"

    // -- Telemetry --

    /** `telemetry/event` - Notification from server to client for telemetry data. */
    public const val TELEMETRY_EVENT: String = "telemetry/event"

    // -- Trace --

    /** `$/setTrace` - Notification to set the trace level. */
    public const val SET_TRACE: String = "$/setTrace"

    /** `$/logTrace` - Notification to log a trace message. */
    public const val LOG_TRACE: String = "$/logTrace"
}
