package org.example.project.data

import java.io.File

actual fun listDirEntries(path: String, ignoreHiddenFiles: Boolean): List<DirEntry> {
    println("Listing dir entries in $path")

    val file = File(path)
    if (!file.exists()) {
        println("File $path does not exist")
        return emptyList()
    }

    if (ignoreHiddenFiles && file.name.isHiddenFile()) {
        println("Ignored hidden file $path")
        return emptyList()
    }

    if (file.isFile) {
        return listOf(file.toDirEntry())
    }

    val children = file.listFiles()?.toList() ?: emptyList()
    return children.map { child -> child.toDirEntry() }
}

actual fun getDirEntry(path: String): DirEntry? {
    return try {
        val file = File(path)
        if (!file.exists()) {
            return null
        }
        file.toDirEntry()

    } catch (e: Exception) {
        println("Error getting file; ${e.message}")
        null
    }
}