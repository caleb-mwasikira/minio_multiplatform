package org.example.project.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import kotlin.system.measureTimeMillis

@Serializable
data class Store(
    val trackedDirectories: MutableMap<String, List<Snapshot>> = mutableMapOf(),
    val trackedDevices: MutableSet<Device> = mutableSetOf(),
)

// Use this to save user preferences and settings persistently across sessions
object LocalStore {
    private val json = Json { ignoreUnknownKeys = true }
    private var _file: File? = null
    val file: File
        get() {
            if (_file != null) {
                return _file!!
            }

            val newFile = FileOperations.getOrCreateInternalFile("synced_files.json")
                ?: throw IOException("Error creating internal file")
            _file = newFile
            return newFile
        }

    private var _store: Store? = null
    val store: Store
        get() {
            if (_store != null) {
                return _store!!
            }

            val data = file.readText()
            if (data.isEmpty()) {
                _store = Store()
                save()
                return _store!!
            }

            _store = json.decodeFromString<Store>(data)
            return _store!!
        }

    private fun save(): Boolean {
        return try {
            file.writeText(json.encodeToString(store))
            true
        } catch (e: Exception) {
            println("Error saving tracked dirs; ${e.message}")
            false
        }
    }

    fun getTrackedDirs(): Set<String> {
        return store.trackedDirectories.keys.also {
            println("Loaded tracked dirs from store; $it")
        }
    }

    suspend fun trackNewDir(dir: String): Boolean = withContext(Dispatchers.IO) {
        val alreadyExists = store.trackedDirectories.containsKey(dir)
        if (alreadyExists) {
            return@withContext false
        }

        return@withContext try {
            println("Taking dir snapshot $dir...")
            val elapsed = measureTimeMillis {
                val snapshots = getSnapshotsOfAllFilesIn(dir)
                store.trackedDirectories[dir] = snapshots
            }
            println("Done taking dir snapshot in $elapsed ms")
            return@withContext save()

        } catch (e: Exception) {
            println("Error taking dir snapshot; ${e.message}")
            false
        }
    }

    fun getTrackedDevices(): Set<Device> {
        return store.trackedDevices
    }

    fun trackNewDevice(device: Device): Boolean {
        val trackedDevices = store.trackedDevices
        if (trackedDevices.contains(device)) return false
        store.trackedDevices.add(device)
        return save()
    }

    fun removeTrackedDevice(device: Device): Boolean {
        store.trackedDevices.remove(device)
        return save()
    }
}
