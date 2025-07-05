package org.example.project

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.example.project.data.SharedViewModel
import org.example.project.data.SyncServer
import org.example.project.widgets.getWindowSizeClass

class MainActivity : ComponentActivity() {
    companion object {
        const val TAG: String = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ContextProvider.init(this)
        val sharedViewModel = SharedViewModel()

        val selectDirectoryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val parentUri = result.data?.data
                parentUri?.let { uri ->
                    // Persist permission across reboots
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    sharedViewModel.trackNewDir(uri.toString())
                }
            }
        }

        lifecycleScope.launch {
            SyncServer.start()
        }

        setContent {
            App(
                windowSizeClass = getWindowSizeClass(),
                sharedViewModel = sharedViewModel,
                onUploadDirectory = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                        )
                    }
                    selectDirectoryLauncher.launch(intent)
                }
            )
        }
    }

}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        windowSizeClass = getWindowSizeClass(),
        sharedViewModel = SharedViewModel(),
        onUploadDirectory = {},
    )
}