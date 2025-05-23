package org.example.project.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.launch
import minio_multiplatform.composeapp.generated.resources.Res
import minio_multiplatform.composeapp.generated.resources.content_empty
import org.example.project.SelectionMode
import org.example.project.data.DirEntry
import org.example.project.data.SharedViewModel
import org.example.project.data.openDocument
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun FilesGrid(
    sharedViewModel: SharedViewModel,
) {
    val files by sharedViewModel.files.collectAsState()
    val selectedFiles = remember { mutableStateListOf<DirEntry>() }
    var selectionMode by remember { mutableStateOf<SelectionMode?>(null) }
    var inSelectMode by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Runs every time files changes
    LaunchedEffect(files) {
        focusRequester.requestFocus()
        selectedFiles.clear()
    }

    var mouseOffset by remember { mutableStateOf(IntOffset.Zero) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        var widgetPosition by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = Modifier.fillMaxSize()
                .onGloballyPositioned { coords ->
                    widgetPosition = coords.positionInWindow()
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                                val localPos = event.changes.first().position
                                mouseOffset = IntOffset(
                                    (widgetPosition.x + localPos.x).toInt(),
                                    (widgetPosition.y + localPos.y).toInt()
                                )
                                inSelectMode = true
                            }
                        }
                    }
                }
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

            LazyVerticalGrid(
                columns = GridCells.Adaptive(144.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
                    .focusable()
                    .focusRequester(focusRequester)
                    .onKeyEvent {
                        val ctrlPressed = it.isCtrlPressed && it.type == KeyEventType.KeyDown
                        val shiftPressed = it.isShiftPressed && it.type == KeyEventType.KeyDown
                        val keyAPressed = it.key == Key.A && it.type == KeyEventType.KeyDown

                        selectionMode = when {
                            ctrlPressed && keyAPressed -> SelectionMode.All
                            ctrlPressed -> SelectionMode.Single
                            shiftPressed -> SelectionMode.Range
                            else -> null
                        }
                        selectionMode != null
                    }
            ) {
                items(count = files.size) { index ->
                    val file = files[index]
                    val isSelected = selectedFiles.contains(file)

                    FileItemCard(
                        file = file,
                        isSelected = isSelected,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {
                                    when (selectionMode) {
                                        SelectionMode.Single -> {
                                            if (isSelected) {
                                                selectedFiles.remove(file)
                                            } else {
                                                selectedFiles.add(file)
                                            }
                                        }

                                        SelectionMode.Range -> {
                                            if (selectedFiles.isEmpty()) {
                                                selectedFiles.add(file)
                                                return@combinedClickable
                                            }

                                            val index1 = selectedFiles.lastIndex
                                            val index2 = files.indexOf(file)
                                            val start = if (index1 < index2) index1 else index2
                                            val stop = if (index1 > index2) index1 else index2

                                            selectedFiles.clear()
                                            for (i in start until stop + 1) {
                                                selectedFiles.add(files[i])
                                            }
                                        }

                                        SelectionMode.All -> {
                                            selectedFiles.addAll(files)
                                        }

                                        null -> {
                                            selectedFiles.clear()
                                            if (file.isDirectory) {
                                                sharedViewModel.changeWorkingDir(file)
                                            } else {
                                                openDocument(file)
                                            }
                                        }
                                    }
                                }
                            )
                            .onClick(
                                // Right click to display context menu
                                matcher = PointerMatcher.mouse(PointerButton.Secondary),
                                onClick = {
                                    selectedFiles.add(file)
                                    inSelectMode = true
                                }
                            ),
                    )
                }
            }
        }

        var inDeleteMode by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val onDismissRequest = {
            inDeleteMode = false
            inSelectMode = false
        }

        if (inDeleteMode) {
            ConfirmationDialog(
                title = "Are you sure you want to delete this file?",
                subtitle = "This action is irreversible",
                onDismissRequest = onDismissRequest,
                onDecline = onDismissRequest,
                onAccept = {
                    scope.launch {
                        sharedViewModel.delete(selectedFiles)
                    }
                    onDismissRequest()
                }
            )
        }

        ContextMenu(
            popupPositionProvider = object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    return mouseOffset
                }
            },
            expanded = inSelectMode,
            onDismissRequest = {
                inSelectMode = false
                selectedFiles.clear()
            },
            onDeleteRequest = {
                inDeleteMode = true
            },
            selectedFiles = selectedFiles,
            sharedViewModel = sharedViewModel,
        )
    }
}

@Composable
actual fun FileItemCard(
    file: DirEntry,
    modifier: Modifier,
    isSelected: Boolean
) {
    val localInteractionSource = remember { MutableInteractionSource() }
    val isHovered by localInteractionSource.collectIsHoveredAsState()
    val containerColor =
        if (isHovered) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.background

    Card(
        colors = CardDefaults.cardColors().copy(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.scrim,
        ),
        modifier = modifier.hoverable(localInteractionSource)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
                .padding(12.dp),
        ) {
            Image(
                painter = painterResource(file.fileType.icon),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )

            Text(
                file.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.scrim
                },
                modifier = Modifier
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            containerColor
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            )
        }
    }
}