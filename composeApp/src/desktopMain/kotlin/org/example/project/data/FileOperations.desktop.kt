package org.example.project.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.moveTo
import kotlin.io.path.name

actual suspend fun copyFiles(
    files: List<DirEntry>, destination: DirEntry, overwrite: Boolean
): List<FileError> = withContext(Dispatchers.IO) {
    if (destination.isFile) {
        return@withContext listOf(
            FileError(
                file = destination,
                exception = IOException("Destination MUST be a directory")
            )
        )
    }

    val fileErrors = mutableListOf<FileError>()
    val dest = File(destination.path)

    for (file in files) {
        try {
            val source = File(file.path)
            if (source.isDirectory) {
                val destFolder = File(dest.path, source.name)
                if (!destFolder.exists()) {
                    destFolder.mkdir()
                }

                source.copyRecursively(destFolder, overwrite, onError = { _, err ->
                    fileErrors.add(
                        FileError(file, err)
                    )
                    OnErrorAction.SKIP
                })
                continue
            }

            // Create a new file in destination with same name as source
            val newFile = File(dest.path, source.name)
            if (!newFile.exists() || (newFile.exists() && overwrite)) {
                newFile.createNewFile()
                source.copyTo(newFile, overwrite)
            }
        } catch (e: Exception) {
            fileErrors.add(
                FileError(file = file, exception = e)
            )
        }
    }
    return@withContext fileErrors
}

actual suspend fun moveFiles(
    files: List<DirEntry>, destination: DirEntry, overwrite: Boolean
): List<FileError> = withContext(Dispatchers.IO) {
    if (destination.isFile) {
        return@withContext listOf(
            FileError(
                file = destination,
                exception = IOException("Destination MUST be a directory")
            )
        )
    }

    val fileErrors = mutableListOf<FileError>()

    for (file in files) {
        try {
            val sourcePath = Path(file.path)

            // Create a new file in destination with same name as source
            val newPath = Path(destination.path, sourcePath.name)
            sourcePath.moveTo(newPath, overwrite)

        } catch (e: Exception) {
            fileErrors.add(
                FileError(file = file, exception = e)
            )
        }
    }
    return@withContext fileErrors
}

actual suspend fun deleteFiles(files: List<DirEntry>): List<FileError> =
    withContext(Dispatchers.IO) {
        val fileErrors = mutableListOf<FileError>()

        for (file in files) {
            try {
                val source = File(file.path)
                val result = if (source.isDirectory) {
                    source.deleteRecursively()
                } else {
                    source.delete()
                }
                if (!result) {
                    fileErrors.add(
                        FileError(
                            file = file,
                            exception = IOException("Failed to delete file/directory ${file.path}")
                        )
                    )
                }

            } catch (e: Exception) {
                fileErrors.add(
                    FileError(file = file, exception = e)
                )
            }
        }
        return@withContext fileErrors
    }

actual fun openDocument(doc: DirEntry) {
    val file = File(doc.path)
    if (!file.exists()) {
        println("File ${doc.path} not found")
        return
    }

    if (!Desktop.isDesktopSupported()) {
        println("Desktop not supported")
        return
    }

    val desktop = Desktop.getDesktop()
    try {
        desktop.open(file)
    } catch (e: Exception) {
        println("Error opening file; ${e.message}")
    }
}

actual fun createNewFile(filename: String): File? {
    return try {
        val path = Path(System.getProperty("user.home"), ".minio", filename)
        val file = path.toFile()
        if (!file.exists()) {
            file.createNewFile()
        }
        file
    } catch (e: Exception) {
        println("Error creating new internal file; ${e.message}")
        null
    }
}