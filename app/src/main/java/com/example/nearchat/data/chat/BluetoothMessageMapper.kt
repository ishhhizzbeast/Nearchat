package com.example.nearchat.data.chat

import com.example.nearchat.domain.chat.BluetoothMessage


fun BluetoothMessage.tobytearray():ByteArray{
    return "$senderName#$message".encodeToByteArray()
}
fun String.toBlueToothMessage(isFromLocalUser : Boolean):BluetoothMessage{
    val name = substringBefore("#")
    val message = substringAfter("#")
    return BluetoothMessage(
        message = message,
        senderName = name,
        isFromLocalUser = isFromLocalUser
    )
}

