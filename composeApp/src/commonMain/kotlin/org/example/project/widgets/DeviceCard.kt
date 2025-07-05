package org.example.project.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import minio_multiplatform.composeapp.generated.resources.Res
import minio_multiplatform.composeapp.generated.resources.delete_24dp
import minio_multiplatform.composeapp.generated.resources.external_hard_drive
import org.example.project.data.Device
import org.example.project.data.SharedViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun DeviceCard(
    device: Device,
    icon: DrawableResource = Res.drawable.external_hard_drive,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val cardContainerColor = if (isHovered) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.background
    }
    val contentColor = if (isHovered) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f)
    }

    Card(
        modifier = Modifier.padding(8.dp)
            .fillMaxWidth()
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { onClick() },
        colors = CardDefaults.cardColors().copy(
            containerColor = cardContainerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.background,
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )

                Text(
                    device.id,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(148.dp)
                )
            }
        }
    }
}

@Composable
fun DeviceCardExpanded(
    device: Device,
    sharedViewModel: SharedViewModel,
    icon: DrawableResource = Res.drawable.external_hard_drive,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val cardContainerColor = if (isHovered) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.background
    }
    val contentColor = if (isHovered) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f)
    }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { onClick() },
        colors = CardDefaults.cardColors().copy(
            containerColor = cardContainerColor,
            contentColor = contentColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Box {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 28.dp)
            ) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            device.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Text(
                            device.id,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(148.dp)
                        )
                    }

                    CircularProgressIndicator(
                        progress = { 0.6f },
                        color = contentColor,
                        trackColor = cardContainerColor,
                    )
                }
            }

            Box(
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                IconButton(
                    onClick = {
                        // Stop syncing device permanently
                        scope.launch {
                            sharedViewModel.removeTrackedDevice(device)
                        }
                    },
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.delete_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}