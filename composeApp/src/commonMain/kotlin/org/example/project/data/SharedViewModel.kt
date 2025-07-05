package org.example.project.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.data.http.HttpClient
import org.example.project.data.http.isFail
import kotlin.io.path.Path

sealed class Msg {
    abstract val message: String

    data class Error(override val message: String) : Msg()
    data class Info(override val message: String) : Msg()
    data class Warn(override val message: String) : Msg()
}

class SharedViewModel {
    private val previousDirs = Stack<DirEntry>()
    private val nextDirs = Stack<DirEntry>()

    val hidingHiddenFiles = MutableStateFlow(true)
    val selectedFileFilter = MutableStateFlow<FileType?>(null)

    private var _trackedDirs = MutableStateFlow(LocalStore.getTrackedDirs())
    private var _workingDir = MutableStateFlow<DirEntry?>(null)
    private val _currentFiles = MutableStateFlow<List<DirEntry>>(emptyList())
    private val _filteredFiles = MutableStateFlow<List<DirEntry>>(emptyList())
    val files: StateFlow<List<DirEntry>>
        get() = _filteredFiles.asStateFlow()

    private var _trackedDevices = MutableStateFlow(LocalStore.getTrackedDevices())
    val trackedDevices = _trackedDevices.asStateFlow()
    private var _onlineDevices = MutableStateFlow<List<Device>>(emptyList())
    val onlineDevices = _onlineDevices.asStateFlow()

    private var _clipboardFiles = mutableStateListOf<DirEntry>()
    private val _clipboardAction = MutableStateFlow<ClipboardAction?>(null)
    private val _isOkayToPaste = MutableStateFlow(false)
    val isOkayToPaste = _isOkayToPaste.asStateFlow()

    private val _uiMessages = MutableSharedFlow<Msg>()
    val uiMessages = _uiMessages.asSharedFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.Default + Job())

    init {
        viewModelScope.launch {
            getOnlineDevices()
        }

        viewModelScope.launch {
            // the combine function runs every time any of the input flows
            // emits a new value.
            // when _workingDir or trackedDirs flows change recompute _currentFiles
            combine(
                _workingDir,
                _trackedDirs,
            ) { workingDir, trackedDirs ->
                val currentFiles = if (workingDir == null) {
                    createRootDir(trackedDirs)
                } else {
                    listDirEntries(workingDir.path)
                }
                currentFiles.toList()
            }.collect {
                _currentFiles.value = it
            }
        }

        viewModelScope.launch {
            // when _currentFiles, hidingHiddenFiles or selectedFileFilter flows
            // changes, recompute _filteredFiles
            combine(
                _currentFiles,
                hidingHiddenFiles,
                selectedFileFilter,
            ) { files, hidingHiddenFiles, selectedFileFilter ->
                var filtered = files
                if (hidingHiddenFiles) {
                    filtered = filtered.filter { file -> !file.name.isHiddenFile() }
                }

                selectedFileFilter?.let {
                    filtered = filtered.filter { file -> file.fileType == it }
                }
                filtered
            }.collect {
                _filteredFiles.value = it
            }
        }

        viewModelScope.launch {
            // Track if paste action is allowed
            combine(
                _workingDir,
                _clipboardAction,
                snapshotFlow { _clipboardFiles },
            ) { workingDir, clipboardAction, clipboardFiles ->
                val okayToPaste =
                    workingDir != null && clipboardAction != null && clipboardFiles.isNotEmpty()
                okayToPaste
            }.collect {
                _isOkayToPaste.value = it
            }
        }
    }

    private fun createRootDir(dirs: Set<String>): Set<DirEntry> {
        return dirs.mapNotNull {
            getDirEntry(it)
        }.toSet()
    }

    fun selectFileFilter(fileType: FileType?) {
        selectedFileFilter.value = fileType
    }

    fun toggleHiddenFiles() {
        hidingHiddenFiles.value = !hidingHiddenFiles.value
    }

    fun trackNewDir(dir: String) {
        _trackedDirs.update { oldList ->
            oldList + dir
        }
        viewModelScope.launch {
            LocalStore.trackNewDir(dir)
        }
    }

    fun refreshCurrentDir() {
        _currentFiles.value = if (_workingDir.value == null) {
            createRootDir(_trackedDirs.value).toList()
        } else {
            listDirEntries(_workingDir.value!!.path)
        }
    }

    fun changeWorkingDir(dir: DirEntry) {
        val newRoot = Path(dir.path).root
        val oldRoot = Path(_workingDir.value?.path ?: "").root

        previousDirs.push(_workingDir.value)
        if (oldRoot != newRoot) {
            nextDirs.clear()
        }

        println("Changing working directory to ${dir.path}")
        _workingDir.value = dir
    }

    fun gotoPreviousDir() {
        val previousDir = previousDirs.pop()
        previousDir?.let {
            println("Moving to previous directory; ${it.path}")
        }
        nextDirs.push(_workingDir.value)
        _workingDir.value = previousDir
    }

    fun gotoNextDir() {
        val nextDir = nextDirs.pop()
        nextDir?.let {
            println("Moving to next directory; ${it.path}")
            previousDirs.push(_workingDir.value)
            _workingDir.value = it
        }
    }

    fun copyOrCut(newFiles: List<DirEntry>, action: ClipboardAction) {
        if (newFiles.isEmpty()) return
        viewModelScope.launch {
            val message = if (action == ClipboardAction.Copy) {
                "Copied files into clipboard"
            } else {
                "Cut files into clipboard"
            }
            _uiMessages.emit(Msg.Info(message))
        }

        _clipboardFiles.clear()
        _clipboardFiles.addAll(newFiles)
        _clipboardAction.value = action
    }

    suspend fun pasteFiles() {
        if (!_isOkayToPaste.value) {
            _uiMessages.emit(
                Msg.Error("Paste action currently not permitted")
            )
            return
        }

        val clipboardErrors = when (_clipboardAction.value!!) {
            ClipboardAction.Copy -> {
                // TODO: add prompt asking user if they wish to overwrite a file
                FileOperations.copy(_clipboardFiles, _workingDir.value!!, true)
            }

            ClipboardAction.Cut -> {
                FileOperations.move(_clipboardFiles, _workingDir.value!!, true)
            }
        }
        clipboardErrors.forEach {
            val errMessage = it.exception?.message ?: return@forEach
            _uiMessages.emit(Msg.Error(errMessage))
        }
        _clipboardFiles.clear()
        _clipboardAction.value = null
        refreshCurrentDir()
    }

    suspend fun delete(files: List<DirEntry>) {
        if (files.isEmpty()) return
        val deleteErrors = FileOperations.delete(files)
        deleteErrors.forEach { deleteError ->
            deleteError.exception?.message?.let {
                _uiMessages.emit(Msg.Error(it))
            }
        }
        refreshCurrentDir()
    }

    /**
     * Searches for tracked files or directories matching the given filename
     */
    suspend fun search(filename: String, ignoreHiddenFiles: Boolean = true) =
        withContext(Dispatchers.IO) {
            _uiMessages.emit(Msg.Info("Searching files..."))

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            val regex = Regex(".*$filename.*", RegexOption.IGNORE_CASE)
            val results = _trackedDirs.value.map {
                scope.async {
                    val dirEntries = listDirEntriesRecursive(it, ignoreHiddenFiles)
                    dirEntries.filter { dirEntry -> regex.containsMatchIn(dirEntry.name) }
                }
            }
            val foundFiles = results.awaitAll().flatten()
            if (foundFiles.isEmpty()) {
                _uiMessages.emit(
                    Msg.Error("File with name '$filename' not found")
                )
            }
            _currentFiles.value = foundFiles
        }

    suspend fun trackNewDevice(device: Device): Unit = withContext(Dispatchers.IO) {
        if (device.ip.isEmpty()) {
            _uiMessages.emit(
                Msg.Error("Error syncing device; Missing IP address")
            )
            return@withContext
        }
        val myDevice = getDevice().value ?: run {
            _uiMessages.emit(
                Msg.Error("Unexpected error syncing device")
            )
            return@withContext
        }

        try {
            val port = 8080
            HttpClient(device.ip, port).use { client ->
                val response = client.post("/track-device", myDevice)
                if (response.isFail() || response.body == null) {
                    _uiMessages.emit(
                        Msg.Error("Error syncing remote device")
                    )
                    return@use
                }
            }

            val success = LocalStore.trackNewDevice(device)
            if (!success) {
                _uiMessages.emit(
                    Msg.Error("Error syncing remote device")
                )
                return@withContext
            }

            println("Tracking new device; $device")
            _uiMessages.emit(
                Msg.Info("Tracking new device; ${device.name}")
            )
            _trackedDevices.value = LocalStore.getTrackedDevices()

        } catch (e: Exception) {
            _uiMessages.emit(
                Msg.Error("Error syncing device")
            )
        }
    }

    suspend fun removeTrackedDevice(device: Device) = withContext(Dispatchers.IO) {
        val success = LocalStore.removeTrackedDevice(device)
        if (!success) {
            _uiMessages.emit(
                Msg.Error("Error tracking device")
            )
            return@withContext
        }
        _uiMessages.emit(
            Msg.Info("Removed tracked device")
        )
        _trackedDevices.value = LocalStore.getTrackedDevices()
    }

    suspend fun getOnlineDevices() = withContext(Dispatchers.IO) {
        _uiMessages.emit(Msg.Info("Searching online devices..."))
        Network.getOnlineDevices()
            .collect { device ->
                println("device=$device")

                _onlineDevices.update { old ->
                    val currentOnlineDevices: MutableList<Device> = old.toMutableList()
                    val found = currentOnlineDevices.any { it.id == device.id }
                    if (!found) {
                        currentOnlineDevices += device
                    }
                    currentOnlineDevices
                }
            }
    }

    suspend fun createNewFile(isDirectory: Boolean) = withContext(Dispatchers.IO) {
        if (_workingDir.value == null) {
            _uiMessages.emit(
                Msg.Error("Cannot create new file at root directory")
            )
            return@withContext
        }

        val filename = if (isDirectory) "New Folder" else "New File"
        val ok = FileOperations.createExternalFile(filename, _workingDir.value!!.path, isDirectory)
        if (ok) {
            refreshCurrentDir()
        } else {
            _uiMessages.emit(Msg.Error("Error creating new file"))
        }
    }

    suspend fun rename(file: DirEntry, newFilename: String) = withContext(Dispatchers.IO) {
        if (newFilename.isEmpty()) {
            _uiMessages.emit(Msg.Error("Filename cannot be empty"))
            return@withContext
        }

        val ok = FileOperations.rename(file, newFilename)
        if (!ok) {
            _uiMessages.emit(Msg.Error("Error renaming file"))
            return@withContext
        }
        refreshCurrentDir()
    }
}