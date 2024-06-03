package com.project.p2pshare.permission

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.project.p2pshare.utils.TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


@Composable
fun ComposePermission(
    state: MutableStateFlow<Boolean>
) {
    var stateIdk by remember {
        mutableStateOf(state.value)
    }
    val uri = Uri.parse("package:${LocalContext.current.packageName}")
    val lunchPermission = Intent(
        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
        uri
    )
    val permission =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            state.value = Environment.isExternalStorageManager()
            stateIdk = state.value
        }

    // TODO: not a good way to do this  =(
    when (stateIdk) {
        true -> {
            Log.d(TAG, "ComposePermission: Granted")
        }

        false -> {
            AlertDialog(
                onDismissRequest = {

                },
                title = {
                    Text(text = "Permission Required")
                },
                text = {
                    Text(text = "We need your permission to perform this action")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            permission.launch(lunchPermission)
                        }
                    ) {
                        Text("Continue")
                    }
                },
                // TODO: add dismissButton for later 
                dismissButton = {

                }
            )
        }
    }
}