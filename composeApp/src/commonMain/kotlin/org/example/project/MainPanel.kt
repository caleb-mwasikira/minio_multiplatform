package org.example.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import minio_multiplatform.composeapp.generated.resources.Res
import minio_multiplatform.composeapp.generated.resources.chevron_left_24dp
import minio_multiplatform.composeapp.generated.resources.chevron_right_24dp
import minio_multiplatform.composeapp.generated.resources.error_24dp
import minio_multiplatform.composeapp.generated.resources.external_hard_drive
import minio_multiplatform.composeapp.generated.resources.info_24dp
import minio_multiplatform.composeapp.generated.resources.menu_24dp
import minio_multiplatform.composeapp.generated.resources.notifications_24dp
import minio_multiplatform.composeapp.generated.resources.search_24dp
import minio_multiplatform.composeapp.generated.resources.send_24dp
import minio_multiplatform.composeapp.generated.resources.visibility_24dp
import minio_multiplatform.composeapp.generated.resources.visibility_off_24dp
import minio_multiplatform.composeapp.generated.resources.warning_24dp
import org.example.project.data.FileType
import org.example.project.data.Msg
import org.example.project.data.SharedViewModel
import org.example.project.data.getFileIcon
import org.example.project.widgets.DeviceCardExpanded
import org.example.project.widgets.FilesGrid
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import java.util.Locale

@Composable
fun MainPanel(
    modifier: Modifier,
    sharedViewModel: SharedViewModel,
    onOpenDrawer: (() -> Unit)?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = modifier.padding(8.dp),
    ) {
        TopBar(
            sharedViewModel = sharedViewModel,
            onOpenDrawer = onOpenDrawer,
        )

        val trackedDevices by sharedViewModel.trackedDevices.collectAsState()
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp)
        ) {
            trackedDevices.forEach { device ->
                DeviceCardExpanded(
                    device = device,
                    sharedViewModel = sharedViewModel,
                    icon = Res.drawable.external_hard_drive,
                    onClick = {},
                )
            }
        }

        val selectedFileFilter by sharedViewModel.selectedFileFilter.collectAsState()
        QuickAccess(
            selectedFileFilter = selectedFileFilter,
            selectFileFilter = { filter ->
                sharedViewModel.selectFileFilter(filter)
            }
        )

        Box {
            // MyFiles
            Box {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    FilesActionBar(
                        sharedViewModel = sharedViewModel
                    )

                    FilesGrid(
                        sharedViewModel = sharedViewModel
                    )
                }
            }

            Box(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .fillMaxWidth()
            ) {
                var message by remember { mutableStateOf<Msg?>(null) }

                LaunchedEffect(Unit) {
                    sharedViewModel.uiMessages.collectLatest {
                        message = it
                        delay(3000)
                        message = null
                    }
                }

                message?.let { msg ->
                    val containerColor = when (msg) {
                        is Msg.Info -> MaterialTheme.colorScheme.primary
                        is Msg.Warn -> MaterialTheme.colorScheme.scrim
                        is Msg.Error -> MaterialTheme.colorScheme.error
                    }
                    val drawableIcon = when (msg) {
                        is Msg.Info -> Res.drawable.info_24dp
                        is Msg.Warn -> Res.drawable.warning_24dp
                        is Msg.Error -> Res.drawable.error_24dp
                    }

                    Column {
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically { it },
                            exit = slideOutVertically { it },
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(12.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = containerColor,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(
                                        horizontal = 32.dp,
                                        vertical = 24.dp
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(drawableIcon),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        msg.message,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopBar(
    sharedViewModel: SharedViewModel,
    onOpenDrawer: (() -> Unit)?,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            onOpenDrawer?.let {
                IconButton(
                    onClick = onOpenDrawer,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.menu_24dp),
                        contentDescription = "Menu",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            var text by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()

            LaunchedEffect(text) {
                if (text.isEmpty()) {
                    sharedViewModel.refreshCurrentDir()
                }
            }

            TextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.search_24dp),
                        contentDescription = "Search Bar",
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (text.isNotEmpty()) {
                                scope.launch {
                                    sharedViewModel.search(text)
                                }
                            }
                        },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.send_24dp),
                            contentDescription = "Search",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp),
                placeholder = {
                    Text(
                        "Search for files",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors().copy(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent, // removes underline when focused
                    unfocusedIndicatorColor = Color.Transparent, // removes underline when not focused
                    disabledIndicatorColor = Color.Transparent // removes underline when disabled
                ),
                textStyle = MaterialTheme.typography.titleLarge,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.notifications_24dp),
                    contentDescription = "Notifications",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun QuickAccess(
    selectedFileFilter: FileType?,
    selectFileFilter: (FileType?) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Quick Access",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
        )

        val ignoredFileTypes = listOf(FileType.UNKNOWN, FileType.FOLDER)
        val items = FileType.entries
            .filter { fileType -> fileType !in ignoredFileTypes }
            .toList()

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            items.forEach { fileType ->
                val isSelected = selectedFileFilter == fileType

                FilterItem(
                    label = fileType.name,
                    icon = getFileIcon(fileType),
                    isSelected = isSelected,
                    onClick = {
                        // If user clicks on an already selected item, we deselect it
                        if (isSelected) {
                            selectFileFilter(null)
                            return@FilterItem
                        }

                        selectFileFilter(fileType)
                    },
                )
            }
        }
    }
}

@Composable
fun FilesActionBar(
    sharedViewModel: SharedViewModel,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .padding(8.dp),
    ) {
        Text(
            "My Files",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    sharedViewModel.gotoPreviousDir()
                },
            ) {
                Icon(
                    painter = painterResource(Res.drawable.chevron_left_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = {
                    sharedViewModel.gotoNextDir()
                },
            ) {
                Icon(
                    painter = painterResource(Res.drawable.chevron_right_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = {
                    sharedViewModel.toggleHiddenFiles()
                },
            ) {
                val hidingHiddenFiles by sharedViewModel.hidingHiddenFiles.collectAsState()
                val resource = if (hidingHiddenFiles) {
                    Res.drawable.visibility_24dp
                } else {
                    Res.drawable.visibility_off_24dp
                }
                Icon(
                    painter = painterResource(resource),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun FilterItem(
    label: String,
    icon: DrawableResource,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val localInteractionSource = remember { MutableInteractionSource() }
    val isHovered by localInteractionSource.collectIsHoveredAsState()
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else if (isHovered) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (isSelected || isHovered) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.scrim
    }
    val elevation = if (isSelected) 10.dp else 0.dp

    Card(
        modifier = Modifier.size(78.dp)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
            .hoverable(localInteractionSource)
            .pointerHoverIcon(PointerIcon.Hand),
        colors = CardDefaults.cardColors().copy(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation,
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
