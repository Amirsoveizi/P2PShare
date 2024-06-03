package com.project.p2pshare.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.project.p2pshare.utils.ALL_FILES

@Composable
fun MyBottomBar(
    modifier: Modifier = Modifier,
    send : (uri : Uri) -> Unit,
    receive : () -> Unit
){

    val context = LocalContext.current

    var uri by remember {
        mutableStateOf<Uri?>(null)
    }
    val result = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()) {
        uri = it
    }
    if (uri!=null){
        send(uri!!)
    }

    BottomAppBar {
        Row(
            modifier = modifier
                .padding(10.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FloatingActionButton(
                modifier = modifier
                    .weight(1f),
                onClick = {
                    result.launch(ALL_FILES)
                }
            ) {
                Text(text = "Send")
            }
            
            Spacer(modifier = modifier.width(10.dp))
            
            FloatingActionButton(
                modifier = modifier
                    .weight(1f),
                onClick = {
                    receive()
                    Toast.makeText(context, "start receiving ...", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text(text = "Receiving")
            }
        }
    }
}