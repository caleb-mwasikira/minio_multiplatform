package org.example.project.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import minio_multiplatform.composeapp.generated.resources.Res
import minio_multiplatform.composeapp.generated.resources.audio_file
import minio_multiplatform.composeapp.generated.resources.doc_file
import minio_multiplatform.composeapp.generated.resources.excel_file
import minio_multiplatform.composeapp.generated.resources.folder
import minio_multiplatform.composeapp.generated.resources.image_file
import minio_multiplatform.composeapp.generated.resources.pdf_file
import minio_multiplatform.composeapp.generated.resources.ppt_file
import minio_multiplatform.composeapp.generated.resources.text_file
import minio_multiplatform.composeapp.generated.resources.unknown_file
import minio_multiplatform.composeapp.generated.resources.video_file
import minio_multiplatform.composeapp.generated.resources.zip_folder
import org.jetbrains.compose.resources.DrawableResource
import java.io.File

data class DirEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val isFile: Boolean = !isDirectory,
    val size: Long = 0L,                    // File size in bytes
    val lastModified: Long = 0L,            // Epoch millis
    val permissions: FilePermissions = FilePermissions(),
    val fileType: FileType,
    val mime: String
)

data class FilePermissions(
    val readable: Boolean = true,
    val writable: Boolean = true,
    val executable: Boolean = false
)

/**
 * Captures the path and lastModified fields of a DirEntry
 */
fun DirEntry.takeSnapshot(): FileSnapshot {
    return FileSnapshot(
        path = this.path,
        lastModified = this.lastModified,
    )
}

fun File.toDirEntry(): DirEntry {
    return DirEntry(
        name = this.name,
        path = this.path,
        isDirectory = this.isDirectory,
        size = this.length(),
        lastModified = this.lastModified(),
        permissions = FilePermissions(
            readable = this.canRead(),
            writable = this.canWrite(),
            executable = this.canExecute()
        ),
        fileType = getFileType(isDirectory, this.extension),
        mime = this.extension
    )
}

fun String.isHiddenFile(): Boolean {
    return this.firstOrNull()?.equals('.') ?: false
}

enum class FileType(val icon: DrawableResource) {
    IMAGE(Res.drawable.image_file),
    VIDEO(Res.drawable.video_file),
    AUDIO(Res.drawable.audio_file),
    PDF(Res.drawable.pdf_file),
    DOCUMENT(Res.drawable.doc_file),
    POWERPOINT(Res.drawable.ppt_file),
    EXCEL(Res.drawable.excel_file),
    TEXT(Res.drawable.text_file),
    FOLDER(Res.drawable.folder),
    ZIP(Res.drawable.zip_folder),
    UNKNOWN(Res.drawable.unknown_file)
}

fun getFileType(isDirectory: Boolean, extension: String): FileType {
    if (isDirectory) return FileType.FOLDER

    return when (extension.lowercase()) {
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif", "svg", "ico", "heic" -> FileType.IMAGE
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp" -> FileType.VIDEO
        "mp3", "wav", "aac", "flac", "ogg", "m4a", "mpeg" -> FileType.AUDIO
        "pdf" -> FileType.PDF
        "doc", "docx" -> FileType.DOCUMENT
        "xls", "xlsx" -> FileType.EXCEL
        "ppt", "pptx" -> FileType.POWERPOINT
        "txt", "csv", "rtf", "odt" -> FileType.TEXT
        "zip", "rar", "7z", "tar", "gz" -> FileType.ZIP
        else -> FileType.UNKNOWN
    }
}

/**
 * Captures the names and lastModified timestamps of all files
 * within a directory
 */
suspend fun takeDirSnapshot(path: String): List<FileSnapshot> = withContext(Dispatchers.IO) {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val fileChannel = Channel<DirEntry>()

    // Coroutine to generate dir entry files
    scope.launch {
        val stack = Stack<DirEntry>()
        var children = listDirEntries(path)
        stack.pushMany(*children.toTypedArray())

        while (stack.isNotEmpty()) {
            val child =
                stack.pop() ?: throw IllegalStateException("Stack isNotEmpty() function is broken")
            if (child.isDirectory) {
                children = listDirEntries(child.path)
                stack.pushMany(*children.toTypedArray())
                continue
            }
            fileChannel.send(child)
        }
        fileChannel.close()
    }

    // Consume dir entry files from channel
    val resultsDeferred = scope.async {
        val results = mutableListOf<FileSnapshot>()

        for (file in fileChannel) {
            val snapshot = file.takeSnapshot()
            results.add(snapshot)
        }
        results
    }
    resultsDeferred.await()
}

expect fun getDirEntry(path: String): DirEntry?

/**
 * List all files and folders in a given path
 */
expect fun listDirEntries(path: String, ignoreHiddenFiles: Boolean = true): List<DirEntry>

fun listDirEntriesRecursive(path: String, ignoreHiddenFiles: Boolean): List<DirEntry> {
    val allFiles = mutableListOf<DirEntry>()

    val stack = Stack<DirEntry>()
    var children = listDirEntries(path)
    allFiles.addAll(children.filter { it.isFile })
    stack.pushMany(*children.filter { it.isDirectory }.toTypedArray())

    while (stack.isNotEmpty()) {
        val child =
            stack.pop() ?: throw IllegalStateException("Stack isNotEmpty() function is broken")
        if (child.isDirectory) {
            children = listDirEntries(child.path, ignoreHiddenFiles)
            allFiles.addAll(children.filter { it.isFile })
            stack.pushMany(*children.filter { it.isDirectory }.toTypedArray())
            continue
        }
        allFiles.add(child)
    }
    return allFiles
}