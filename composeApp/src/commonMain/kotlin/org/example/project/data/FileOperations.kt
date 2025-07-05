package org.example.project.data

import java.io.File

enum class ClipboardAction {
    Copy, Cut
}

data class FileError(
    val file: DirEntry? = null,
    val exception: Throwable?
)

expect object FileOperations {
    suspend fun copy(
        files: List<DirEntry>, destination: DirEntry, overwrite: Boolean
    ): List<FileError>

    suspend fun move(
        files: List<DirEntry>, destination: DirEntry, overwrite: Boolean
    ): List<FileError>

    suspend fun rename(
        file: DirEntry, newFilename: String,
    ): Boolean

    suspend fun delete(files: List<DirEntry>): List<FileError>

    fun getOrCreateInternalFile(filename: String): File?

    suspend fun createExternalFile(
        filename: String,
        targetDir: String,
        isDirectory: Boolean
    ): Boolean

    /**
     * Opens a document for viewing within the running platform
     */
    suspend fun open(doc: DirEntry)
}
