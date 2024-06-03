package com.project.p2pshare.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.project.p2pshare.viewmodels.P2PViewModel

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: P2PViewModel
){
    val context = LocalContext.current



    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            MyTopBar(
                modifier = modifier,
                ip = viewModel.ip.collectAsState(),
                refresh = { viewModel.getIP() }
            )
        },
        bottomBar = {
            MyBottomBar(
                send = { uri : Uri ->
                    viewModel.send(viewModel.contentResolver, uri)
                },
                receive = {
                    viewModel.receive()
                }
            )
        }
    ) { paddingValues ->
        //content
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ServerORClientCard(
                isServerRunning = viewModel.isServerRunning,
                startServer = { port : Int ->
                    viewModel.startTheServer()
                },
                stopServer = {
                    viewModel.stopTheServer()
                },
                connectToServer = { hostAddress: String, port: Int ->
                    viewModel.connectToServer(host = hostAddress)
                },
            )
            if (viewModel.isClientConnected.collectAsState().value){
                Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show()
//                FloatingActionButton(
//                    onClick = {
//                        viewModel.closeConnection()
//                    }
//                ) {
//                    Text(text = "Close Connection")
//                }
            }
        }
    }
}


