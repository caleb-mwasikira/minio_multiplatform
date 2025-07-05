package org.example.project.data

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.ContextProvider
import org.example.project.MainActivity.Companion.TAG
import java.io.File
import java.io.IOException

actual object FileOperations {
    actual suspend fun copy(
        files: List<DirEntry>, destination: DirEntry, overwrite: Boolean
    ): List<FileError> {
        if (destination.isFile()) {
            return listOf(
                FileError(
                    file = destination,
                    exception = IOException("Copy destination must be a directory")
                )
            )
        }

        val destUri = getDocumentUri(destination.path) ?: run {
            return listOf(
                FileError(
                    file = destination,
                    exception = IOException("Error acquiring destination uri")
                )
            )
        }

        val fileErrors = mutableListOf<FileError>()
        val stack = Stack<DirEntry>()
        stack.pushMany(*files.toTypedArray())

        val contentResolver = ContextProvider.get().contentResolver

        while (stack.isNotEmpty()) {
            val file =
                stack.pop() ?: throw IllegalStateException("Stack isNotEmpty() function is broken")

            try {
                if (file.isDirectory()) {
                    val children = listDirEntries(file.path)
                    stack.pushMany(*children.toTypedArray())
                    continue
                }
                val srcUri: Uri = getDocumentUri(file.path)
                    ?: throw IOException("Error acquiring source file")

                contentResolver.openInputStream(srcUri)?.use { inputStream ->
                    val newFileUri = DocumentsContract.createDocument(
                        contentResolver, destUri, file.fileType.mime, file.name
                    ) ?: return@use

                    contentResolver.openOutputStream(newFileUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

            } catch (e: Exception) {
                fileErrors.add(
                    FileError(file = file, exception = e)
                )
            }
        }
        return fileErrors
    }

    actual suspend fun move(
        files: List<DirEntry>, destination: DirEntry, overwrite: Boolean
    ): List<FileError> {
        if (destination.isFile()) {
            return listOf(
                FileError(
                    file = destination,
                    exception = IOException("Destination MUST be a directory")
                )
            )
        }

        val destUri = getDocumentUri(destination.path) ?: run {
            return listOf(
                FileError(
                    file = destination,
                    exception = IOException("Error acquiring destination URI")
                )
            )
        }

        val fileErrors = mutableListOf<FileError>()
        val stack = Stack<DirEntry>()
        stack.pushMany(*files.toTypedArray())

        val contentResolver = ContextProvider.get().contentResolver

        while (stack.isNotEmpty()) {
            val file =
                stack.pop() ?: throw IllegalStateException("Stack isNotEmpty() function is broken")

            try {
                if (file.isDirectory()) {
                    val children = listDirEntries(file.path)
                    stack.pushMany(*children.toTypedArray())
                }
                val srcUri: Uri = getDocumentUri(file.path) ?: continue
                val srcParentUri: Uri = getParentUri(srcUri) ?: continue
                DocumentsContract.moveDocument(
                    contentResolver, srcUri, srcParentUri, destUri
                )

            } catch (e: Exception) {
                fileErrors.add(
                    FileError(file = file, exception = e)
                )
            }
        }
        return fileErrors
    }

    actual suspend fun rename(file: DirEntry, newFilename: String): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val context = ContextProvider.get()
                val documentUri = getDocumentUri(file.path) ?: return@withContext false

                DocumentsContract.renameDocument(
                    context.contentResolver,
                    documentUri,
                    newFilename
                ) ?: return@withContext false
                true

            } catch (e: Exception) {
                Log.e(TAG, "Error renaming file; ${e.message}")
                false
            }
        }

    actual suspend fun delete(files: List<DirEntry>): List<FileError> {
        val contentResolver = ContextProvider.get().contentResolver
        val fileErrors = mutableListOf<FileError>()

        for (file in files) {
            val uri = getDocumentUri(file.path)
            if (uri == null) {
                fileErrors.add(
                    FileError(
                        file = file,
                        exception = IOException("Error acquiring source file")
                    )
                )
                continue
            }
            val result = DocumentsContract.deleteDocument(contentResolver, uri)
            Log.d(TAG, "File deleted successfully? $result")
        }
        return fileErrors
    }

    actual fun getOrCreateInternalFile(filename: String): File? {
        return try {
            val context = ContextProvider.get()
            val file = File(context.filesDir, filename)
            if (!file.exists()) {
                file.createNewFile()
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error creating internal file; ${e.message}")
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
                val context = ContextProvider.get()
                val targetUri = getDocumentUri(targetDir) ?: return@withContext false

                DocumentsContract.createDocument(
                    context.contentResolver,
                    targetUri,
                    if (isDirectory) DocumentsContract.Document.MIME_TYPE_DIR else "*/*",
                    filename
                )
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error creating external file; ${e.message}")
                false
            }
        }

    /**
     * Opens a document for viewing within the running platform
     */
    actual suspend fun open(doc: DirEntry): Unit = withContext(Dispatchers.IO) {
        val context = ContextProvider.get()
        val uri = Uri.parse(doc.path)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndTypeAndNormalize(uri, doc.fileType.mime)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK // Calling startActivity() from outside an Activity requires this flag
            )
        }

        Log.d(TAG, "Opening file ${doc.path}. MIME type; ${doc.fileType.mime}")
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file; ${e.message}")
        }
    }
}