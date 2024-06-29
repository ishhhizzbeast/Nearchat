package com.example.nearchat.domain.chat

sealed interface ConnectionResult {
    object establishedConnection : ConnectionResult
    data class TransferSucceded(val messages: String):ConnectionResult
    data class error(val messege: String) :ConnectionResult
}