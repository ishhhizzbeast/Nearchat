package com.example.nearchat.presentation

import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nearchat.domain.chat.BluetoothController
import com.example.nearchat.domain.chat.BluetoothDeviceDomain
import com.example.nearchat.domain.chat.BluetoothMessage
import com.example.nearchat.domain.chat.ConnectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothViewmodel @Inject constructor( private val bluetoothcontroller : BluetoothController)
    : ViewModel() {
        private val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothcontroller.scannedDevice,
        bluetoothcontroller.pairedDevice,
        _state
    ){ scannedDevice , pairedDevice , state ->
        state.copy(
            scannedDevices = scannedDevice,
            pairedDevices = pairedDevice,
            messages = if (state.isConnected)state.messages else emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),_state.value)
    init {
        bluetoothcontroller.isconnected.onEach { isConnected->
            _state.update {
                it.copy(
                    isConnected = isConnected
                )
            }
        }.launchIn(viewModelScope)

        bluetoothcontroller.error.onEach {error->
            _state.update {
                it.copy(
                    errorMessage = error
                )
            }
        }.launchIn(viewModelScope)
    }
    fun startScan(){
        bluetoothcontroller.startDiscovery()
    }
    fun stopScan(){
        bluetoothcontroller.stopDiscovery()
    }
    private var deviceConnectionJob:Job? = null
    fun connectToDevice(device:BluetoothDeviceDomain){
        _state.update {
            it.copy(
                isConnecting = true
            )
        }
        deviceConnectionJob= bluetoothcontroller
            .connectToDevice(device)
            .listen()
    }
    fun disconnectfromdevice(){
        deviceConnectionJob?.cancel()
        bluetoothcontroller.closeServer()
        _state.update {
            it.copy(
                isConnecting = false,
                isConnected = false,
            )
        }
    }
    fun sendMessage(message: String){
        viewModelScope.launch {
            val bluetoothMessage = bluetoothcontroller.trySendMessages(message)
            if (bluetoothMessage != null){
                _state.update {
                    it.copy(
                        messages = it.messages + bluetoothMessage
                    )
                }
            }
        }
    }
    fun waitforIncommingConnection(){
        _state.update {
            it.copy(
                isConnecting = true
            )
        }
        deviceConnectionJob = bluetoothcontroller
            .startBluetoothServer()
            .listen()
    }
    private fun Flow<ConnectionResult>.listen():Job{
        return onEach { result ->
            when (result) {

                ConnectionResult.establishedConnection -> {
                    _state.update {
                        it.copy(
                            isConnected = true,
                            isConnecting = false,
                            errorMessage = null
                        )
                    }
                }
                is ConnectionResult.error -> {
                    _state.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false,
                            errorMessage = result.messege
                        )
                    }
                }

                is ConnectionResult.TransferSucceded -> {
                    _state.update {
                        it.copy(
                            messages = it.messages + convertMessageStringToList( result.messages)
                        )
                    }
                }

                else -> {}
            }
        }
            .catch {throwable->
                bluetoothcontroller.closeServer()
                _state.update {
                    it.copy(
                        isConnected = false,
                        isConnecting = false
                    )
                }
            }
            .launchIn(viewModelScope)


    }

    override fun onCleared() {
        super.onCleared()
        bluetoothcontroller.release()
    }
}
fun convertMessageStringToList(messageString: String): List<BluetoothMessage> {
    val messageList = mutableListOf<BluetoothMessage>()
    messageString.split(",").forEach { message ->
        messageList.add(BluetoothMessage(message, "", false)) // Assuming sender and isFromLocalUser are empty
    }
    return messageList
}