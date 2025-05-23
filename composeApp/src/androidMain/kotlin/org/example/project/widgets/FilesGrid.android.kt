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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import minio_multiplatform.composeapp.generated.resources.Res
import minio_multiplatform.composeapp.generated.resources.content_empty
import minio_multiplatform.composeapp.generated.resources.more_horiz_24dp
import org.example.project.data.DirEntry
import org.example.project.data.SharedViewModel
import org.example.project.data.formatLastModified
import org.example.project.data.openDocument
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun FilesGrid(
    sharedViewModel: SharedViewModel,
) {
    val files by sharedViewModel.files.collectAsState()
    var inSelectMode by remember { mutableStateOf(false) }
    val selectedFiles = remember { mutableStateListOf<DirEntry>() }

    LaunchedEffect(selectedFiles.size) {
        inSelectMode = selectedFiles.isNotEmpty()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {
                    inSelectMode = false
                },
                onLongClick = {
                    inSelectMode = !inSelectMode
                }
            )
    ) {
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
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(count = files.size) { index ->
                val file = files[index]
                val alreadySelected = selectedFiles.contains(file)

                FileItemCard(
                    file = file,
                    isSelected = selectedFiles.contains(file),
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (inSelectMode) {
                                if (alreadySelected) {
                                    selectedFiles.remove(file)
                                } else {
                                    selectedFiles.add(file)
                                }
                                return@combinedClickable
                            }

                            selectedFiles.clear()
                            if (file.isDirectory) {
                                sharedViewModel.changeWorkingDir(file)
                            } else {
                                openDocument(file)
                            }
                        },
                        onLongClick = {
                            if (alreadySelected) {
                                selectedFiles.remove(file)
                            } else {
                                selectedFiles.add(file)
                            }
                        }
                    )
                )
            }
        }

        var inDeleteMode by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        if (inDeleteMode) {
            ConfirmationDialog(
                title = "Are you sure you want to delete this file?",
                subtitle = "This action is irreversible",
                onDismissRequest = {
                    inDeleteMode = false
                    selectedFiles.clear()
                },
                onDecline = {
                    inDeleteMode = false
                    selectedFiles.clear()
                },
                onAccept = {
                    scope.launch {
                        sharedViewModel.delete(selectedFiles)
                        selectedFiles.clear()
                    }
                    inDeleteMode = false
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            ContextMenu(
                expanded = inSelectMode,
                onDismissRequest = {
                    inSelectMode = false
                },
                onDeleteRequest = {
                    inDeleteMode = true
                },
                selectedFiles = selectedFiles,
                sharedViewModel = sharedViewModel,
            )
        }
    }
}


@Composable
actual fun FileItemCard(
    file: DirEntry,
    modifier: Modifier,
    isSelected: Boolean
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
                    painter = painterResource(file.fileType.icon),
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.scrim
                        },
                    )
                    Text(
                        formatLastModified(file.lastModified),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            IconButton(
                onClick = {}
            ) {
                Icon(
                    painter = painterResource(Res.drawable.more_horiz_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}