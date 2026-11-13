package io.lsp4k.protocol

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Options for creating a file.
 */
@Serializable
public data class CreateFileOptions(
    val overwrite: Boolean? = null,
    val ignoreIfExists: Boolean? = null,
)

/**
 * Create file operation.
 */
@Serializable
@SerialName("create")
public data class CreateFile(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val kind: String = "create",
    val uri: DocumentUri,
    val options: CreateFileOptions? = null,
    val annotationId: String? = null,
)

/**
 * Options for renaming a file.
 */
@Serializable
public data class RenameFileOptions(
    val overwrite: Boolean? = null,
    val ignoreIfExists: Boolean? = null,
)

/**
 * Rename file operation.
 */
@Serializable
@SerialName("rename")
public data class RenameFile(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val kind: String = "rename",
    val oldUri: DocumentUri,
    val newUri: DocumentUri,
    val options: RenameFileOptions? = null,
    val annotationId: String? = null,
)

/**
 * Options for deleting a file.
 */
@Serializable
public data class DeleteFileOptions(
    val recursive: Boolean? = null,
    val ignoreIfNotExists: Boolean? = null,
)

/**
 * Delete file operation.
 */
@Serializable
@SerialName("delete")
public data class DeleteFile(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val kind: String = "delete",
    val uri: DocumentUri,
    val options: DeleteFileOptions? = null,
    val annotationId: String? = null,
)
