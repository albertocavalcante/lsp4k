package io.lsp4k.protocol

import kotlinx.serialization.Serializable

@Serializable
public data class FileCreate(
    val uri: String,
)

@Serializable
public data class CreateFilesParams(
    val files: List<FileCreate>,
)

@Serializable
public data class FileRename(
    val oldUri: String,
    val newUri: String,
)

@Serializable
public data class RenameFilesParams(
    val files: List<FileRename>,
)

@Serializable
public data class FileDelete(
    val uri: String,
)

@Serializable
public data class DeleteFilesParams(
    val files: List<FileDelete>,
)
