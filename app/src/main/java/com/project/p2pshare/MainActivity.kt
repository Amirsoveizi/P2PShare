package com.project.p2pshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.p2pshare.permission.ComposePermission
import com.project.p2pshare.ui.screens.MainScreen
import com.project.p2pshare.ui.theme.P2PShareTheme
import com.project.p2pshare.viewmodels.P2PViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            //vm
            val viewModel: P2PViewModel= viewModel()
            viewModel.contentResolver = applicationContext.contentResolver

            // call permission screen
            val permissionState by viewModel.permission.collectAsState()
            if (!permissionState){
                ComposePermission(state = viewModel.permission)
            }

            //ui
            P2PShareTheme {
                MainScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}

