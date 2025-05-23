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
import kotlin.io.path.Path

sealed class UIMessages {
    abstract val message: String

    data class Error(override val message: String) : UIMessages()
    data class Info(override val message: String) : UIMessages()
    data class Warn(override val message: String) : UIMessages()
}

class SharedViewModel {
    private val previousDirs = Stack<DirEntry>()
    private val nextDirs = Stack<DirEntry>()

    val hidingHiddenFiles = MutableStateFlow(true)
    val selectedFileFilter = MutableStateFlow<FileType?>(null)

    private var _trackedDirs = MutableStateFlow<Set<String>>(emptySet())
    private var _workingDir = MutableStateFlow<DirEntry?>(null)
    private val _currentFiles = MutableStateFlow<List<DirEntry>>(emptyList())
    private val _filteredFiles = MutableStateFlow<List<DirEntry>>(emptyList())
    val files: StateFlow<List<DirEntry>>
        get() = _filteredFiles

    private var _clipboardFiles = mutableStateListOf<DirEntry>()
    private val _clipboardAction = MutableStateFlow<ClipboardAction?>(null)
    private val _isOkayToPaste = MutableStateFlow(false)
    val isOkayToPaste = _isOkayToPaste.asStateFlow()

    private val _uiMessages =
        MutableSharedFlow<UIMessages>() // Error, warning or info messages to be sent to the UI
    val uiMessages = _uiMessages.asSharedFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.Default + Job())

    init {
        _trackedDirs.value = CustomPreferences.getTrackedDirs()

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
        println("Creating root directory...")
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
            CustomPreferences.trackNewDir(dir)
            refreshCurrentDir()
        }
    }

    fun changeWorkingDir(dir: DirEntry) {
        val newRoot = Path(dir.path).root
        val oldRoot = Path(_workingDir.value?.path ?: "").root

        previousDirs.push(_workingDir.value)
        if (oldRoot != newRoot) {
            nextDirs.clear()
        }

        _workingDir.value = dir
        println("Changed working dir to $dir")
    }

    fun gotoPreviousDir() {
        val previousDir = previousDirs.pop()
        nextDirs.push(_workingDir.value)
        _workingDir.value = previousDir
        refreshCurrentDir()
    }

    fun gotoNextDir() {
        val nextDir = nextDirs.pop()
        nextDir?.let {
            previousDirs.push(_workingDir.value)
            _workingDir.value = it
            println("Moved to next dir; $it")
        }
    }

    fun addPasteBin(newFiles: List<DirEntry>, action: ClipboardAction) {
        if (newFiles.isEmpty()) return
        viewModelScope.launch {
            val message = if (action == ClipboardAction.Copy) {
                "Copied files into clipboard"
            } else {
                "Cut files into clipboard"
            }
            _uiMessages.emit(
                UIMessages.Info(message)
            )
        }

        _clipboardFiles.clear()
        _clipboardFiles.addAll(newFiles)
        _clipboardAction.value = action
    }

    fun refreshCurrentDir() {
        _trackedDirs.value = CustomPreferences.getTrackedDirs()
        _currentFiles.update {
            _workingDir.value?.let {
                listDirEntries(it.path)
            } ?: createRootDir(_trackedDirs.value).toList()
        }
        println("Refreshing current dir; ${_currentFiles.value}")
    }

    suspend fun paste() {
        if (!_isOkayToPaste.value) {
            _uiMessages.emit(
                UIMessages.Error("Paste action currently not permitted")
            )
            return
        }

        val errors = when (_clipboardAction.value!!) {
            ClipboardAction.Copy -> {
                // Setting overwrite == true is dangerous; we might end up overwriting user's files
                // TODO: fixme - add prompt asking user if they wish to overwrite a file
                copyFiles(_clipboardFiles, _workingDir.value!!, true)
            }

            ClipboardAction.Cut -> {
                moveFiles(_clipboardFiles, _workingDir.value!!, true)
            }
        }

        _clipboardFiles.clear()
        refreshCurrentDir()

        errors.forEach {
            val errMessage = it.exception?.message ?: return@forEach
            _uiMessages.emit(
                UIMessages.Error(errMessage)
            )
        }
    }

    suspend fun delete(files: List<DirEntry>) {
        deleteFiles(files)
        refreshCurrentDir()
        viewModelScope.launch {
            _uiMessages.emit(
                UIMessages.Info("Deleted ${files.size} files")
            )
        }
    }

    /**
     * Searches for tracked files or directories matching the given filename
     */
    suspend fun searchFilesWithName(filename: String, ignoreHiddenFiles: Boolean = true) =
        withContext(Dispatchers.IO) {
            _uiMessages.emit(
                UIMessages.Info("Searching files...")
            )

            val trackedDirs = CustomPreferences.getTrackedDirs()
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            val regex = Regex(".*$filename.*", RegexOption.IGNORE_CASE)

            val results = trackedDirs.map {
                scope.async {
                    val dirEntries = listDirEntriesRecursive(it, ignoreHiddenFiles)
                    dirEntries.filter { dirEntry -> regex.containsMatchIn(dirEntry.name) }
                }
            }
            val foundFiles = results.awaitAll().flatten()
            if (foundFiles.isEmpty()) {
                _uiMessages.emit(
                    UIMessages.Error("File with name '$filename' not found")
                )
            }
            _currentFiles.value = foundFiles
        }
}