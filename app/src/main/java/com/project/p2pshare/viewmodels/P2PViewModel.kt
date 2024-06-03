package com.project.p2pshare.viewmodels

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.p2pshare.utils.PORT9876
import com.project.p2pshare.utils.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.math.min

class P2PViewModel : ViewModel() {

    //permission state
    private val _ps = MutableStateFlow<Boolean>(Environment.isExternalStorageManager())
    val permission = _ps
    private val folder = File(Environment.getExternalStorageDirectory(), "/p2pShare")

    //content resolver : get file from storage
    lateinit var contentResolver: ContentResolver

    //file uri : content uri of file that want to share
    private var uri: Uri = Uri.EMPTY

    //your ipv4 address
    private val _ip = MutableStateFlow<String>("refresh")
    val ip = _ip.asStateFlow()


    // server and client fields : both client and server will manage here
    // todo : make a class for server and client for later version !!!
    private var port: Int = PORT9876
    private lateinit var serverSocket: ServerSocket
    private lateinit var endpoint: Socket
    private lateinit var dis: DataInputStream
    private lateinit var dos: DataOutputStream

    //server status : running or not
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning = _isServerRunning.asStateFlow()

    //client status : connected or not
    private val _isConnected = MutableStateFlow(false)
    val isClientConnected = _isConnected.asStateFlow()

    // send file percentage
    private val _progressSend = MutableStateFlow<Long>(0L)
    val progressSend = _progressSend.asStateFlow()

    // receive file percentage
    private val _progressReceive = MutableStateFlow<Long>(0L)
    val progressReceive = _progressReceive.asStateFlow()


    //init viewmodel
    init {
        when (Environment.isExternalStorageManager()) {
            true -> createFolder()
            false -> {
                _ps.value = false
                Log.e(TAG, "viewmodel inti () ->: no permission granted ")
            }
        }
    }


    //region viewmodel functions :

    fun changePort(port : Int){
        this.port = port
    }

    fun startTheServer() {
        viewModelScope.launch {
            startServer(port)
            acceptClient()
        }
    }

    fun connectToServer(host: String) {
        viewModelScope.launch {
            clientConnectToServer(host, port)

        }
    }

    fun stopTheServer() {
        viewModelScope.launch {
            stopServer()
        }
    }

    fun closeConnection(){
        viewModelScope.launch {
            disconnect()
            withContext(Dispatchers.Main){
                ensureActive()
                _isConnected.value = false
            }
        }

    }

    //todo: progressSend
    fun send(contentResolver: ContentResolver = this.contentResolver, uri: Uri = this.uri) {
//        Log.d(TAG, "info : $endpoint")
//        viewModelScope.launch(Dispatchers.IO) {
//            try{
//                val fileInfo = getFileInfo(contentResolver,uri)
//                Log.d(TAG, "send: ${fileInfo.first} \n ${fileInfo.second}")
//                dos.writeUTF(fileInfo.first)
//                dos.writeLong(fileInfo.second)
//
//                var byte = 0
//                val buffer = ByteArray(16 *4096)
//
//                try{
//                    contentResolver.openInputStream(uri).use { input ->
//                        if (input != null) {
//                            while (
//                                input.read(buffer).also { i -> byte = i } != -1
//                            ){
//                                dos.write(buffer,0,byte)
//                                dos.flush()
//                            }
//                        }
//                    }
//                }catch (e : Exception){
//                    Log.e(TAG,"openInputStream - send() -> ${e.cause}")
//                }
//
//
//            }catch (e : Exception){
//                Log.e(TAG, "send() -> :  ${e.cause}", )
//            }
//        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "start sending ...")
                val fileInfo = getFileInfo(contentResolver, uri)


                val fileName = fileInfo.first
                val fileSize = fileInfo.second

                dos.writeUTF(fileName)
                dos.writeLong(fileSize)

                var byte = 0
                val buffer = ByteArray(16 * 4096)
                var sent : Long = 0L

                launch(Dispatchers.Main){
                    while (sent < fileSize){
                        ensureActive()
                        _progressSend.value = ((sent * 100) / fileSize)
                        delay(1000)
//                        Log.d(TAG, "sent: ${_progressSend.value}")
                        Log.d(TAG, "sent: $sent * 100 / $fileSize = ${(sent*100)/fileSize}")
                    }
                }

                try {
                    contentResolver.openInputStream(uri).use { input ->
                        if (input != null) {
                            while (
                                input.read(buffer).also { i -> byte = i } != -1
                            ) {
                                dos.write(buffer, 0, byte)
                                dos.flush()
                                sent += byte.toLong()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "viewmodel send():openInputStream -> ${e.cause}")
                }

                Log.d(TAG, "sending finished !!!")


            } catch (e: Exception) {
                Log.e(TAG, "send() -> :  ${e.cause}")
            }
        }
    }

    //todo: progressReceive
    fun receive() {
        if (endpoint.isClosed) {
            Log.e(TAG, "viewmodel receive() -> : endpoint is closed")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {

                val fileName = dis.readUTF()
                var size = dis.readLong()
                val file = File(folder, fileName)
                val fileOutputStream = file.outputStream()
                val buffer = ByteArray(16 * 4096)
                var byte = 0

                while (
                    size > 0 &&
                    (dis.read(buffer, 0, min(size.toDouble(), buffer.size.toDouble()).toInt())
                        .also { byte = it }) != -1
                ) {
                    fileOutputStream.write(buffer, 0, byte)
                    size -= byte.toLong()
                    ensureActive()
                }
            } catch (e: Exception) {
                Log.e(TAG, "viewmodel receive() -> ${e.cause}")
            }
        }
    }

    fun getIP() {

        val list = mutableListOf<String>()
        val en = NetworkInterface.getNetworkInterfaces()

        try {
            while (en.hasMoreElements()) {
                val init = en.nextElement()
                val enumIpAdd = init.inetAddresses
                while (enumIpAdd.hasMoreElements()) {
                    val address = enumIpAdd.nextElement()
                    if (!address.isLoopbackAddress) {
//                        Log.d(TAG, "ip : ${address.hostAddress}")
                        address.hostAddress?.let {
                            list.add(it)
                            Log.d(TAG, "viewmodel getIP: $it")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "viewmodel getIP() -> : ${e.cause}")
        }
        // TODO: check if works for all phones
        // TODO: check wifi or access point is on or not
        if (list.size > 2) {
            _ip.value = list[list.lastIndex]
        } else {
            _ip.value = "not found"
        }
    }

    //endregion


    //server function: start the server
    private suspend fun startServer(port: Int = PORT9876) {

        if (this.port != port) {
            this.port = port
            Log.d(TAG, "viewmodel startServer() -> : port changed")
        }

        try {
            withContext(Dispatchers.IO) {
                serverSocket = ServerSocket(port)
//                _isServerRunning.value = true
                Log.d(TAG, "Server started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "viewmodel startServer() -> ${e.cause}")
//            _isServerRunning.value = false
            if (this::serverSocket.isInitialized) {
                withContext(Dispatchers.IO) {
                    serverSocket.close()
                }
            }
        }
    }

    //server function: accept new client
    // TODO: cant manage multiple client 
    private suspend fun acceptClient() {

//        if (!_isServerRunning.value) {
//            Log.e(TAG, "viewmodel acceptClient() -> : server is not running")
//            return
//        }

        while (/*_isServerRunning.value*/true) {
            try {
                withContext(Dispatchers.IO) {
                    ensureActive()
                    endpoint = serverSocket.accept()
                    dis = DataInputStream(endpoint.getInputStream())
                    dos = DataOutputStream(endpoint.getOutputStream())
//                    _isConnected.value = true
                    Log.d(TAG, "acceptClient: $endpoint")
                }
            } catch (e: Exception) {
                Log.e(TAG, "viewmodel acceptClient: () -> message : ${e.message}")
                Log.e(TAG, "viewmodel acceptClient: () -> cause   : ${e.cause}")
                if (this::endpoint.isInitialized) {
                    withContext(Dispatchers.IO) {
                        endpoint.close()
                        dis.close()
                        dos.close()
                        _isConnected.value = false
                    }
                }
            }
        }
    }

    //server function: stop the server
    private suspend fun stopServer() {
        try {
            withContext(Dispatchers.IO) {
//                _isServerRunning.value = false
                _isConnected.value = false
                serverSocket.close()
                Log.d(TAG, "Server stopped")
            }
        } catch (e: Exception) {
            Log.e(TAG, "viewmodel stopServer() -> message: ${e.message}")
            Log.e(TAG, "viewmodel stopServer() -> cause  : ${e.cause}")
        }
    }

    //client function: connect to a server
    private suspend fun clientConnectToServer(host: String, port: Int = PORT9876) {
        try {
            withContext(Dispatchers.IO){
                endpoint = Socket(host,port)
                dis =  DataInputStream(endpoint.getInputStream())
                dos =  DataOutputStream(endpoint.getOutputStream())
                Log.d(TAG, "info : $endpoint")
            }
        } catch (e: Exception) {
            Log.e(TAG, "viewmodel clientConnectToServer() -> cause   : ${e.cause}")
            Log.e(TAG, "viewmodel clientConnectToServer() -> message : ${e.message}")
        }
    }

    //close connection
    private suspend fun disconnect() {
        try{
            withContext(Dispatchers.IO){
                endpoint.close()
            }
        }catch (e : Exception){
            Log.e(TAG,"viewmodel disconnect() -> cause   : ${e.cause}")
            Log.e(TAG,"viewmodel disconnect() -> message : ${e.message}")
        }
    }



    // get file info from content uri : fileName, fileSize
    private fun getFileInfo(
        contentResolver: ContentResolver, uri: Uri
    ): Pair<String, Long> {

        // Get the file name
        var fileName = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).let {
                    if (it > -1) {
                        fileName = cursor.getString(it)
                    }
                }
            }
        }
        // Get the file size
        var fileSize = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.SIZE).let {
                    if (it > -1) {
                        fileSize = cursor.getLong(it)
                    }
                }
            }
        }

        return Pair(fileName, fileSize)
    }

    //create folder : p2pShare
    private fun createFolder() {
        if (!folder.exists()) {
            folder.mkdir()
        }
    }
}