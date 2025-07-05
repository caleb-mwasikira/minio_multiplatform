package org.example.project.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.moveTo
import kotlin.io.path.name

actual object FileOperations {
    actual suspend fun copy(
        files: List<DirEntry>, destination: DirEntry, overwrite: Boolean
    ): List<FileError> = withContext(Dispatchers.IO) {
        if (destination.isFile()) {
            return@withContext listOf(
                FileError(
                    file = destination,
                    exception = IOException("Destination MUST be a directory")
                )
            )
        }

        val fileErrors = mutableListOf<FileError>()
        val destFolder = File(destination.path)

        for (file in files) {
            try {
                val source = File(file.path)
                var dest = File(destFolder.path, source.name)
                if (dest.exists()) {
                    // Change destination filename
                    val filename = source.name.substringBeforeLast('.')
                    val extension =
                        if (source.extension.isNotEmpty()) ".${source.extension}" else ""
                    val numFilesWithSameName = destFolder.listFiles()?.count {
                        it.name.contains(filename)
                    } ?: 0
                    val newFilename = "$filename copy($numFilesWithSameName)$extension"
                    dest = File(destFolder.path, newFilename)
                }

                if (source.isDirectory) {
                    source.copyRecursively(dest, overwrite, onError = { _, err ->
                        fileErrors.add(
                            FileError(file, err)
                        )
                        OnErrorAction.SKIP
                    })
                    continue
                }

                source.copyTo(dest, overwrite)

            } catch (e: Exception) {
                fileErrors.add(
                    FileError(file = file, exception = e)
                )
            }
        }
        return@withContext fileErrors
    }

    actual suspend fun move(
        files: List<DirEntry>, destination: DirEntry, overwrite: Boolean
    ): List<FileError> = withContext(Dispatchers.IO) {
        if (destination.isFile()) {
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

    actual suspend fun rename(file: DirEntry, newFilename: String): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val oldFile = Path(file.path).toFile()
                val newFile = Path(oldFile.parent, newFilename).toFile()
                val ok = oldFile.renameTo(newFile)
                ok

            } catch (e: Exception) {
                println("Error renaming file; ${e.message}")
                false
            }
        }

    actual suspend fun delete(files: List<DirEntry>): List<FileError> =
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
                                exception = IOException("Failed to  file/directory ${file.path}")
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

    actual fun getOrCreateInternalFile(filename: String): File? {
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

    actual suspend fun createExternalFile(
        filename: String,
        targetDir: String,
        isDirectory: Boolean
    ): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val dest = File(targetDir, filename)
                if (dest.exists()) {
                    return@withContext false
                }

                return@withContext if (isDirectory) {
                    dest.mkdir()
                } else {
                    dest.createNewFile()
                }
            } catch (e: Exception) {
                println("Error creating external file; ${e.message}")
                false
            }
        }

    /**
     * Opens a document for viewing within the running platform
     */
    actual suspend fun open(doc: DirEntry) = withContext(Dispatchers.IO) {
        val file = File(doc.path)
        if (!file.exists()) {
            println("File ${doc.path} not found")
            return@withContext
        }

        if (!Desktop.isDesktopSupported()) {
            println("Desktop not supported")
            return@withContext
        }

        val desktop = Desktop.getDesktop()
        try {
            desktop.open(file)
        } catch (e: Exception) {
            println("Error opening file; ${e.message}")
        }
    }
}