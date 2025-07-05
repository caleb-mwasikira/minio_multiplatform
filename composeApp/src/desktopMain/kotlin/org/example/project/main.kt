package org.example.project

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import org.example.project.data.SharedViewModel
import org.example.project.data.SyncServer
import org.example.project.widgets.getWindowSizeClass
import javax.swing.JFileChooser
import javax.swing.JFrame

fun selectDirectory(): String? {
    return try {
        val frame = JFrame()
        frame.isAlwaysOnTop = true

        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Select Folder"
        }

        val result = chooser.showOpenDialog(frame)
        return if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.path
        } else {
            null
        }
    } catch (e: Exception) {
        println("Error selecting dir; ${e.message}")
        null
    }
}

fun main() = application {
    val coroutineScope = rememberCoroutineScope()

    coroutineScope.launch {
        SyncServer.start()
    }

    val windowState = rememberWindowState(
        size = DpSize(1200.dp, 896.dp),
        isMinimized = false,
    )

    Window(
        state = windowState,
        onCloseRequest = ::exitApplication,
        title = "MinIo",
    ) {
        val sharedViewModel = remember { SharedViewModel() }

        App(
            windowSizeClass = getWindowSizeClass(
                widthPx = windowState.size.width.value
            ),
            sharedViewModel = sharedViewModel,
            onUploadDirectory = {
                val selected = selectDirectory()
                selected?.let {
                    sharedViewModel.trackNewDir(it)
                }
            }
        )
    }
}