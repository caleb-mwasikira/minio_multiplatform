package org.example.project.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.launch
import minio_multiplatform.composeapp.generated.resources.Res
import minio_multiplatform.composeapp.generated.resources.content_copy_24dp
import minio_multiplatform.composeapp.generated.resources.content_cut_24dp
import minio_multiplatform.composeapp.generated.resources.content_paste_24dp
import minio_multiplatform.composeapp.generated.resources.delete_24dp
import org.example.project.data.ClipboardAction
import org.example.project.data.DirEntry
import org.example.project.data.SharedViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun ContextMenu(
    popupPositionProvider: PopupPositionProvider?,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDeleteRequest: () -> Unit,
    selectedFiles: List<DirEntry>,
    sharedViewModel: SharedViewModel,
) {
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = expanded,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(72.dp),
            colors = CardDefaults.cardColors().copy(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ContextMenuItem(
                    title = "Copy",
                    resource = Res.drawable.content_copy_24dp,
                    onClick = {
                        sharedViewModel.addPasteBin(selectedFiles, ClipboardAction.Copy)
                        onDismissRequest()
                    },
                )
                ContextMenuItem(
                    title = "Cut",
                    resource = Res.drawable.content_cut_24dp,
                    onClick = {
                        sharedViewModel.addPasteBin(selectedFiles, ClipboardAction.Cut)
                        onDismissRequest()
                    },
                )
                ContextMenuItem(
                    title = "Paste",
                    resource = Res.drawable.content_paste_24dp,
                    onClick = {
                        scope.launch {
                            sharedViewModel.paste()
                        }
                        onDismissRequest()
                    }
                )
                ContextMenuItem(
                    title = "Delete",
                    resource = Res.drawable.delete_24dp,
                    onClick = onDeleteRequest,
                )
            }
        }
    }
}

@Composable
fun ContextMenuItem(
    title: String,
    resource: DrawableResource,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onClick,
        ) {
            Icon(
                painter = painterResource(resource),
                contentDescription = title,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
