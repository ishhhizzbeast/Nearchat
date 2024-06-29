package com.example.nearchat.data.chat

import android.bluetooth.BluetoothSocket
import com.example.nearchat.domain.chat.BluetoothMessage
import com.example.nearchat.domain.chat.ConnectionResult
import com.example.nearchat.domain.chat.TransferFailedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException

class BluetoothDataTransferService(
    private val socket:BluetoothSocket
) {
    fun listenForIncommingMessages(): Flow<BluetoothMessage>
    {
        return flow {
            if (!socket.isConnected){
                return@flow
            }
            val buffer = ByteArray(1024)
            while (true){
                val bytecount = try {
                    socket.inputStream.read(buffer)
                }catch (e:IOException){
                    throw TransferFailedException()
                }
                emit(
                    buffer.decodeToString(
                        endIndex = bytecount
                    ).toBlueToothMessage(isFromLocalUser = false)
                )
            }
        }.flowOn(Dispatchers.IO)
    }
    suspend fun sendMessages(bytes:ByteArray):Boolean{
        return withContext(Dispatchers.IO){
             try {
                 socket.outputStream.write(bytes)
             }catch (e:IOException){
                 e.printStackTrace()
                 return@withContext false
             }
            true
        }
    }
}