package com.example.nearchat.data.chat


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.example.nearchat.domain.chat.BluetoothController
import com.example.nearchat.domain.chat.BluetoothDeviceDomain
import com.example.nearchat.domain.chat.BluetoothMessage
import com.example.nearchat.domain.chat.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context : Context,
) : BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _scannerDevice = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevice: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannerDevice.asStateFlow()

    private val _pairedDevice = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevice: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevice.asStateFlow()

    private val _isconnected = MutableStateFlow(false)
    override val isconnected: StateFlow<Boolean>
        get() = _isconnected.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    override val error: SharedFlow<String>
        get() = _error.asSharedFlow()

    private val foundReciever = FoundDeviceReciever { device ->
        _scannerDevice.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain()
            if (newDevice in devices) devices else devices + newDevice
        }
    }

    private var dataTransferService : BluetoothDataTransferService? = null

    private val bluetoothStatereciver = BluetoothStateReciver { isConnectd, device ->
        if (bluetoothAdapter?.bondedDevices?.contains(device) == true) {
            _isconnected.update {
                isConnectd
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                _error.emit("can't connect to non paired device")
            }
        }
    }

    init {
        updatePairedDevices()
        context.registerReceiver(
            bluetoothStatereciver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
    }

    private var currentServiceServer: BluetoothServerSocket? = null
    private var currentClientSever: BluetoothSocket? = null
    override fun startDiscovery() {
        if (!haspermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        context.registerReceiver(
            foundReciever,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()

    }

    override fun stopDiscovery() {
        if (!haspermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow {
            if (!haspermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("NO BLUETOOTH CONNECTION PERMISSION")
            }
            currentServiceServer = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "chat_server",
                UUID.fromString(SERVICE_UUID)
            )
            var shouldLoop = true
            while (shouldLoop) {
                currentClientSever = try {
                    currentServiceServer?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }
            emit(ConnectionResult.establishedConnection)
            currentClientSever?.let {
                currentServiceServer?.close()
                val service  = BluetoothDataTransferService(it )
                dataTransferService = service
                emitAll(
                    service.listenForIncommingMessages()
                        .map {
                            ConnectionResult.TransferSucceded(it.message)
                        }
                )
            }
        }
    }.onCompletion {
        closeServer()
        }.flowOn(Dispatchers.IO)
}

    override fun connectToDevice(Device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if (!haspermission(Manifest.permission.BLUETOOTH_CONNECT)){
                throw SecurityException("NO BLUETOOTH_CONNECT PERMISSION")
            }
            currentClientSever = bluetoothAdapter
                ?.getRemoteDevice(Device.address)
                ?.createRfcommSocketToServiceRecord(
                    UUID.fromString(SERVICE_UUID)
                )
            stopDiscovery()
            currentClientSever?.let {socket->
                try {
                    socket.connect()
                    emit(ConnectionResult.establishedConnection)
                    val service = BluetoothDataTransferService(socket)
                    dataTransferService = service
                    emitAll(
                        service.listenForIncommingMessages()
                            .map {
                                ConnectionResult.TransferSucceded(it.message)
                            }
                    )
                }catch (e:IOException){
                    socket.close()
                    currentClientSever = null
                    emit(ConnectionResult.error("connection has been interrupted"))
                }
            }
        }.onCompletion {
            closeServer()
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun trySendMessages(message: String): BluetoothMessage? {
        if (!haspermission(Manifest.permission.BLUETOOTH_CONNECT)){
            return null
        }
        if (dataTransferService == null){
            return null
        }
        val bluetoothMessage = BluetoothMessage(
            message = message,
            senderName = bluetoothAdapter?.name ?: "Unknown-User",
            isFromLocalUser = true
        )
        dataTransferService?.sendMessages(message.encodeToByteArray())
        return bluetoothMessage
    }
    override fun closeServer() {
        currentClientSever?.close()
        currentServiceServer?.close()
        currentClientSever = null
        currentServiceServer = null
    }

    override fun release() {
        context.unregisterReceiver(foundReciever)
        context.unregisterReceiver(bluetoothStatereciver)
        closeServer()
    }
    private fun updatePairedDevices(){
        if(!haspermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
            bluetoothAdapter
                ?.bondedDevices
                ?.map {
                    it.toBluetoothDeviceDomain()
                }
                ?.also {
                    _pairedDevice.update { it }
                }
    }

    private fun haspermission(permission: String) : Boolean{
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
    companion object{
         const val SERVICE_UUID = "27b7d1da-08c7-4505-a6d1-2459987e5e2d"
    }
}
