package com.project.p2pshare.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.project.p2pshare.utils.PORT9876
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ServerORClientCard(
    modifier: Modifier = Modifier,
    isServerRunning : StateFlow<Boolean>,
    startServer : (port : Int)->Unit,
    stopServer : ()->Unit,
    connectToServer : (hostAddress : String, port : Int)->Unit,
){
    val hostAddress = remember {
        mutableStateOf<String?>(null)
    }

    val isOpen = remember {
        mutableStateOf(false)
    }

    if (hostAddress.value != null){
        connectToServer(hostAddress.value!!, PORT9876)
    }

    if (isOpen.value){
        GetHostAddress(host = hostAddress, isOpen = isOpen)
    }

    val context = LocalContext.current

    Card(
        modifier = modifier
            .padding(10.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth(),
        ){
            when(isServerRunning.collectAsState().value){
                true -> {
                    FloatingActionButton(
                        modifier = modifier
                            .padding(10.dp)
                            .weight(1f),
                        onClick = {
                            stopServer()
                            Toast.makeText(context, "Server Stopped", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Text(text = "Stop Server")
                    }
                }
                false -> {
                    FloatingActionButton(
                        modifier = modifier
                            .padding(10.dp)
                            .weight(1f),
                        onClick = {
                            startServer(PORT9876)
                            Toast.makeText(context, "Server Started", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Text(text = "Start Server")

                    }
                }
            }
            FloatingActionButton(
                modifier = modifier
                    .padding(10.dp)
                    .weight(1f),
                onClick = {
                    isOpen.value = true
                }
            ) {
                Text(text = "Connect to Server")
            }
        }
    }
}

@Composable
fun GetHostAddress(
    host : MutableState<String?>,
    isOpen : MutableState<Boolean>
){
    var ip by remember {
        mutableStateOf("")
    }
    AlertDialog(
        title = {
            Text(text = "Server Address : ")
        },
        text = {
            TextField(value = ip, onValueChange = {
                ip = it
            })
        },
        onDismissRequest = {
            isOpen.value = false
            host.value = null
        },
        confirmButton = {
            Button(onClick = {
                host.value = ip
                isOpen.value = false }
            ) {
                Text(text = "OK")
            }
        }
    )
}