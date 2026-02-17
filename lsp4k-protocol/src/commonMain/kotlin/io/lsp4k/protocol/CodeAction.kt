package io.lsp4k.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A set of predefined code action kinds.
 */
public object CodeActionKind {
    /**
     * Empty kind.
     */
    public const val EMPTY: String = ""

    /**
     * Base kind for quickfix actions: 'quickfix'.
     */
    public const val QUICK_FIX: String = "quickfix"

    /**
     * Base kind for refactoring actions: 'refactor'.
     */
    public const val REFACTOR: String = "refactor"

    /**
     * Base kind for refactoring extraction actions: 'refactor.extract'.
     */
    public const val REFACTOR_EXTRACT: String = "refactor.extract"

    /**
     * Base kind for refactoring inline actions: 'refactor.inline'.
     */
    public const val REFACTOR_INLINE: String = "refactor.inline"

    /**
     * Base kind for refactoring rewrite actions: 'refactor.rewrite'.
     */
    public const val REFACTOR_REWRITE: String = "refactor.rewrite"

    /**
     * Base kind for source actions: 'source'.
     */
    public const val SOURCE: String = "source"

    /**
     * Base kind for an organize imports source action: 'source.organizeImports'.
     */
    public const val SOURCE_ORGANIZE_IMPORTS: String = "source.organizeImports"

    /**
     * Base kind for a fix all source action: 'source.fixAll'.
     */
    public const val SOURCE_FIX_ALL: String = "source.fixAll"

    // Deprecated aliases for backwards compatibility
    @Deprecated("Use EMPTY", ReplaceWith("EMPTY"))
    public const val Empty: String = EMPTY

    @Deprecated("Use QUICK_FIX", ReplaceWith("QUICK_FIX"))
    public const val QuickFix: String = QUICK_FIX

    @Deprecated("Use REFACTOR", ReplaceWith("REFACTOR"))
    public const val Refactor: String = REFACTOR

    @Deprecated("Use REFACTOR_EXTRACT", ReplaceWith("REFACTOR_EXTRACT"))
    public const val RefactorExtract: String = REFACTOR_EXTRACT

    @Deprecated("Use REFACTOR_INLINE", ReplaceWith("REFACTOR_INLINE"))
    public const val RefactorInline: String = REFACTOR_INLINE

    @Deprecated("Use REFACTOR_REWRITE", ReplaceWith("REFACTOR_REWRITE"))
    public const val RefactorRewrite: String = REFACTOR_REWRITE

    @Deprecated("Use SOURCE", ReplaceWith("SOURCE"))
    public const val Source: String = SOURCE

    @Deprecated("Use SOURCE_ORGANIZE_IMPORTS", ReplaceWith("SOURCE_ORGANIZE_IMPORTS"))
    public const val SourceOrganizeImports: String = SOURCE_ORGANIZE_IMPORTS

    @Deprecated("Use SOURCE_FIX_ALL", ReplaceWith("SOURCE_FIX_ALL"))
    public const val SourceFixAll: String = SOURCE_FIX_ALL
}

/**
 * Parameters for a textDocument/codeAction request.
 */
@Serializable
public data class CodeActionParams(
    /**
     * The document in which the command was invoked.
     */
    val textDocument: TextDocumentIdentifier,
    /**
     * The range for which the command was invoked.
     */
    val range: Range,
    /**
     * Context carrying additional information.
     */
    val context: CodeActionContext,
    /**
     * An optional token that a server can use to report work done progress.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val workDoneToken: ProgressToken? = null,
    /**
     * An optional token that a server can use to report partial results.
     */
    @Serializable(with = NullableProgressTokenSerializer::class)
    val partialResultToken: ProgressToken? = null,
)

/**
 * Contains additional diagnostic information about the context in which
 * a code action is run.
 */
@Serializable
public data class CodeActionContext(
    /**
     * An array of diagnostics known on the client side overlapping the range provided
     * to the `textDocument/codeAction` request.
     */
    val diagnostics: List<Diagnostic>,
    /**
     * Requested kind of actions to return.
     */
    val only: List<String>? = null,
    /**
     * The reason why code actions were requested.
     */
    val triggerKind: CodeActionTriggerKind? = null,
)

/**
 * The reason why code actions were requested.
 */
@Serializable(with = CodeActionTriggerKindSerializer::class)
public enum class CodeActionTriggerKind(
    public val value: Int,
) {
    /**
     * Code actions were explicitly requested by the user or by an extension.
     */
    Invoked(1),

    /**
     * Code actions were requested automatically.
     */
    Automatic(2),
    ;

    public companion object {
        public fun fromValue(value: Int): CodeActionTriggerKind =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown CodeActionTriggerKind: $value")
    }
}

/**
 * Serializer for CodeActionTriggerKind that encodes/decodes as integer.
 */
public object CodeActionTriggerKindSerializer : IntEnumSerializer<CodeActionTriggerKind>(
    "CodeActionTriggerKind", CodeActionTriggerKind::fromValue, { it.value },
)

/**
 * A code action represents a change that can be performed in code.
 */
@Serializable
public data class CodeAction(
    /**
     * A short, human-readable, title for this code action.
     */
    val title: String,
    /**
     * The kind of the code action. Used to filter code actions.
     */
    val kind: String? = null,
    /**
     * The diagnostics that this code action resolves.
     */
    val diagnostics: List<Diagnostic>? = null,
    /**
     * Marks this as a preferred action. Preferred actions are used by the
     * `auto fix` command and can be targeted by keybindings.
     */
    val isPreferred: Boolean? = null,
    /**
     * Marks that the code action cannot currently be applied.
     */
    val disabled: CodeActionDisabled? = null,
    /**
     * The workspace edit this code action performs.
     */
    val edit: WorkspaceEdit? = null,
    /**
     * A command this code action executes. If a code action provides an edit
     * and a command, first the edit is executed and then the command.
     */
    val command: Command? = null,
    /**
     * A data entry field that is preserved on a code action between
     * a `textDocument/codeAction` and a `codeAction/resolve` request.
     */
    val data: JsonElement? = null,
)

/**
 * Represents the reason why a code action is disabled.
 */
@Serializable
public data class CodeActionDisabled(
    /**
     * Human readable description of why the code action is currently disabled.
     */
    val reason: String,
)
