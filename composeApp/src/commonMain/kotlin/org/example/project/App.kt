package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import minio_multiplatform.composeapp.generated.resources.Res
import minio_multiplatform.composeapp.generated.resources.add_24dp
import org.example.project.data.SharedViewModel
import org.example.project.theme.AppTheme
import org.example.project.widgets.AddNewDeviceDialog
import org.example.project.widgets.WindowSizeClass
import org.jetbrains.compose.resources.painterResource

@Composable
fun MobileScreen(
    sharedViewModel: SharedViewModel,
    onUploadDirectory: () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(400.dp)
            ) {
                SidePanel(
                    sharedViewModel = sharedViewModel,
                    modifier = Modifier.fillMaxHeight()
                        .padding(8.dp),
                    onUploadDirectory = {
                        onUploadDirectory()
                        scope.launch {
                            drawerState.close()
                        }
                    },
                )
            }
        },
    ) {
        var addNewDevice by remember { mutableStateOf(false) }
        if (addNewDevice) {
            AddNewDeviceDialog(
                sharedViewModel = sharedViewModel,
                onDismissRequest = {
                    addNewDevice = false
                },
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        addNewDevice = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.add_24dp),
                        contentDescription = "Connect New Device",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        ) { innerPadding ->
            MainPanel(
                modifier = Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(12.dp),
                sharedViewModel = sharedViewModel,
                onOpenDrawer = {
                    scope.launch {
                        if (drawerState.isOpen) drawerState.close() else drawerState.open()
                    }
                },
            )
        }
    }
}

@Composable
fun DesktopScreen(
    sharedViewModel: SharedViewModel,
    onUploadDirectory: () -> Unit,
) {
    var addNewDevice by remember { mutableStateOf(false) }
    if (addNewDevice) {
        AddNewDeviceDialog(
            sharedViewModel = sharedViewModel,
            onDismissRequest = {
                addNewDevice = false
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    addNewDevice = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.add_24dp),
                    contentDescription = "Connect New Device",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding)
        ) {
            SidePanel(
                sharedViewModel = sharedViewModel,
                modifier = Modifier.fillMaxHeight()
                    .width(400.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                onUploadDirectory = onUploadDirectory,
            )

            MainPanel(
                modifier = Modifier.fillMaxSize()
                    .weight(2f)
                    .padding(12.dp),
                onOpenDrawer = null,
                sharedViewModel = sharedViewModel,
            )
        }
    }
}

@Composable
fun App(
    windowSizeClass: WindowSizeClass,
    sharedViewModel: SharedViewModel,
    onUploadDirectory: () -> Unit,
) {
    AppTheme {
        when (windowSizeClass) {
            WindowSizeClass.Compact, WindowSizeClass.Medium -> {
                MobileScreen(
                    sharedViewModel = sharedViewModel,
                    onUploadDirectory = onUploadDirectory,
                )
            }

            WindowSizeClass.Expanded -> {
                DesktopScreen(
                    sharedViewModel = sharedViewModel,
                    onUploadDirectory = onUploadDirectory,
                )
            }
        }
    }
}