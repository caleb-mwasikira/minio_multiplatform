package org.example.project.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import minio_multiplatform.composeapp.generated.resources.Res
import minio_multiplatform.composeapp.generated.resources.external_hard_drive
import org.example.project.data.SharedViewModel

@Composable
fun AddNewDeviceDialog(
    onDismissRequest: () -> Unit,
    sharedViewModel: SharedViewModel,
) {
    val onlineDevices by sharedViewModel.onlineDevices.collectAsState()
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier.width(420.dp)
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (onlineDevices.isEmpty()) {
                Text(
                    "No devices found within your local network",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    "Network Devices",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                )

                onlineDevices.forEach { device ->
                    DeviceCard(
                        device = device,
                        icon = Res.drawable.external_hard_drive,
                        onClick = {
                            scope.launch {
                                sharedViewModel.trackNewDevice(device)
                            }
                            onDismissRequest()
                        },
                    )
                }
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        sharedViewModel.getOnlineDevices()
                    }
                },
                colors = ButtonDefaults.elevatedButtonColors().copy(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.padding(vertical = 12.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    "Search For Devices",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}