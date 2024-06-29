package com.example.nearchat.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nearchat.domain.chat.BluetoothMessage

@Composable
fun ChatMessage(
    message: BluetoothMessage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = if (message.isFromLocalUser) 15.dp else 0.dp,
                    topEnd = 15.dp,
                    bottomStart = 15.dp,
                    bottomEnd = if (message.isFromLocalUser) 0.dp else 15.dp
                )
            )
            .background(if (message.isFromLocalUser) Color.Cyan else Color.Blue)
            .padding(16.dp)
    ) {
       Text(text = message.senderName,
           fontSize = 10.sp)
        Text(text = message.message, color = Color.Black,
            modifier = modifier.widthIn(100.dp)
        )

    }

}

@Preview
@Composable
private fun ChatMessage() {
    ChatMessage(
        message = BluetoothMessage(
            message = "hellow ",
            senderName = "ik",
            isFromLocalUser = true
        )
    )
}