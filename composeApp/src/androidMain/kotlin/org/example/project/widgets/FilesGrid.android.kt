package org.example.project.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import minio_multiplatform.composeapp.generated.resources.Res
import minio_multiplatform.composeapp.generated.resources.content_empty
import org.example.project.data.DirEntry
import org.example.project.data.FileOperations
import org.example.project.data.SharedViewModel
import org.example.project.data.formatTimeMillis
import org.example.project.data.getFileIcon
import org.example.project.data.isDirectory
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun FilesGrid(
    sharedViewModel: SharedViewModel,
) {
    val files by sharedViewModel.files.collectAsState()
    var openContextMenu by remember { mutableStateOf(false) }
    val selectedFiles = remember { mutableStateListOf<DirEntry>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedFiles.size) {
        openContextMenu = selectedFiles.isNotEmpty()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {
                    openContextMenu = false
                },
                onLongClick = {
                    openContextMenu = !openContextMenu
                }
            )
    ) {
        var displayDeleteDialog by remember { mutableStateOf(false) }
        var displayRenameDialog by remember { mutableStateOf(false) }
        val onDismissRequest = {
            displayDeleteDialog = false
            displayRenameDialog = false
            openContextMenu = false
            selectedFiles.clear()
        }

        // Close context menu when rename dialog or delete dialog is open
        LaunchedEffect(displayRenameDialog, displayDeleteDialog) {
            if (displayRenameDialog || displayDeleteDialog) {
                openContextMenu = false
            }
        }

        if (displayDeleteDialog) {
            ConfirmationDialog(
                title = "Are you sure you want to delete this file?",
                subtitle = "This action is irreversible",
                onDismissRequest = onDismissRequest,
                onDecline = onDismissRequest,
                onAccept = {
                    // Copying selected files into its own variable to avoid
                    // the list being cleared by on-dismiss before its values are used
                    val filesToBeDeleted = selectedFiles.toList()
                    scope.launch {
                        sharedViewModel.delete(filesToBeDeleted)
                    }
                    onDismissRequest()
                }
            )
        }

        if (displayRenameDialog && selectedFiles.isNotEmpty()) {
            val file = remember { selectedFiles.first() }

            RenameFileDialog(
                file = file,
                onDismissRequest = {
                    displayRenameDialog = false
                    onDismissRequest()
                },
                onAccept = { newFilename ->
                    scope.launch {
                        sharedViewModel.rename(file, newFilename)
                    }
                    onDismissRequest()
                },
                onDecline = {
                    displayRenameDialog = false
                    onDismissRequest()
                }
            )
        }

        if (files.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(Res.drawable.content_empty),
                    contentDescription = null,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Empty directory",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(count = files.size) { index ->
                    val file = files[index]
                    val isSelected = selectedFiles.contains(file)

                    FileItemCard(
                        file = file,
                        isSelected = isSelected,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (openContextMenu) {
                                    if (isSelected) {
                                        selectedFiles.remove(file)
                                    } else {
                                        selectedFiles.add(file)
                                    }
                                    return@combinedClickable
                                }

                                selectedFiles.clear()
                                if (file.isDirectory()) {
                                    sharedViewModel.changeWorkingDir(file)
                                } else {
                                    scope.launch {
                                        FileOperations.open(file)
                                    }
                                }
                            },
                            onLongClick = {
                                if (isSelected) {
                                    selectedFiles.remove(file)
                                } else {
                                    selectedFiles.add(file)
                                }
                            }
                        )
                    )
                }
            }
        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            ContextMenu(
                expanded = openContextMenu,
                selectedFiles = selectedFiles,
                sharedViewModel = sharedViewModel,
                onDismissRequest = {
                    openContextMenu = false
                },
                onDeleteFiles = {
                    displayDeleteDialog = true
                },
                onRenameFiles = {
                    displayRenameDialog = true
                },
            )
        }
    }
}


@Composable
actual fun FileItemCard(
    file: DirEntry,
    modifier: Modifier,
    isSelected: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors().copy(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.background
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.scrim
            }
        )
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row {
                Image(
                    painter = painterResource(getFileIcon(file.fileType)),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        file.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.scrim
                        },
                    )

                    Text(
                        formatTimeMillis(file.lastModified),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}