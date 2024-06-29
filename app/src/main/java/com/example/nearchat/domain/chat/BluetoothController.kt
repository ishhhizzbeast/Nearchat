package com.example.nearchat.domain.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val isconnected : StateFlow<Boolean>
    val error: SharedFlow<String>
    val scannedDevice : StateFlow<List<BluetoothDevice>>
    val pairedDevice : StateFlow<List<BluetoothDevice>>

    fun startDiscovery()
    fun stopDiscovery()
    fun startBluetoothServer() : Flow<ConnectionResult>

    fun connectToDevice(Device : BluetoothDeviceDomain) : Flow<ConnectionResult>

    fun closeServer()

    suspend fun trySendMessages(message:String): BluetoothMessage?

    fun release()
}