package org.example.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import minio_multiplatform.composeapp.generated.resources.Res
import minio_multiplatform.composeapp.generated.resources.add_24dp
import minio_multiplatform.composeapp.generated.resources.chevron_right_24dp
import minio_multiplatform.composeapp.generated.resources.delete_24dp
import minio_multiplatform.composeapp.generated.resources.external_hard_drive
import minio_multiplatform.composeapp.generated.resources.folder
import minio_multiplatform.composeapp.generated.resources.manage_history_24dp
import minio_multiplatform.composeapp.generated.resources.share_24dp
import minio_multiplatform.composeapp.generated.resources.star_24dp
import org.example.project.data.SharedViewModel
import org.example.project.widgets.DeviceCard
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun SidePanel(
    modifier: Modifier,
    sharedViewModel: SharedViewModel,
    onUploadDirectory: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "Connected Devices",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            val trackedDevices by sharedViewModel.trackedDevices.collectAsState()
            trackedDevices.forEach { device ->
                DeviceCard(
                    device = device,
                    icon = Res.drawable.external_hard_drive,
                    onClick = {},
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 64.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MenuItem(
                    icon = Res.drawable.share_24dp,
                    "Shared with me",
                    onClick = {}
                )

                MenuItem(
                    icon = Res.drawable.manage_history_24dp,
                    "Recent",
                    onClick = {}
                )

                MenuItem(
                    icon = Res.drawable.star_24dp,
                    "Starred",
                    onClick = {}
                )

                MenuItem(
                    icon = Res.drawable.delete_24dp,
                    "Trash",
                    onClick = {}
                )
            }
        }

//        PromoSection()

        ElevatedButton(
            onClick = onUploadDirectory,
            colors = ButtonDefaults.elevatedButtonColors().copy(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(8.dp)
                .pointerHoverIcon(PointerIcon.Hand)
                .fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.add_24dp),
                    contentDescription = "Upload New Folder",
                    modifier = Modifier.padding(4.dp)
                        .size(24.dp)
                )

                Text(
                    "Upload New Folder",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
fun MenuItem(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors().copy(
            contentColor = MaterialTheme.colorScheme.scrim
        ),
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            label,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
fun PromoSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .offset(x = 24.dp, y = -48.dp)
                .align(Alignment.TopStart)
        ) {
            Image(
                painter = painterResource(Res.drawable.folder),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(
                    "Upgrade to PRO to get all features",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(4.dp)
                )

                TextButton(
                    onClick = {},
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text(
                        text = "Upgrade Now",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Icon(
                        painter = painterResource(Res.drawable.chevron_right_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}