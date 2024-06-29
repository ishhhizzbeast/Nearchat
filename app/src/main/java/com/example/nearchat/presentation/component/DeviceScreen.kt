package com.example.nearchat.presentation.component


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nearchat.domain.chat.BluetoothDeviceDomain
import com.example.nearchat.presentation.BluetoothUiState

@Composable
fun DeviceScreen(
    state : BluetoothUiState,
    onStart:()->Unit,
    onStop:()->Unit,
    onClick: (BluetoothDeviceDomain) -> Unit,
    onStartServer:()->Unit
) {
    Column(
        modifier  =  Modifier.fillMaxSize()

    ) {
        BlutoothDeviceList(pairedDevice = state.pairedDevices,
            scannedDevice = state.scannedDevices,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = {
                onStart()
            }) {
                Text(text = "start scan")
            }
            Button(onClick = {
                onStop()
            }) {
                Text(text = "stop scan")
            }
            Button(onClick = { onStartServer()}) {
                Text(text = "start server")
            }
        }
    }
}

@Composable
fun BlutoothDeviceList(
    pairedDevice : List<BluetoothDeviceDomain>,
    scannedDevice: List<BluetoothDeviceDomain>,
    onClick: (BluetoothDeviceDomain)-> Unit,
    modifier : Modifier = Modifier
) {
    LazyColumn {
        item {
            Text(
                text = "Paired Device",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = modifier.padding()
            )
        }
        items(pairedDevice){device->
            Text(text = device.name ?: "no-name",
                modifier = modifier
                    .fillMaxWidth()
                    .clickable { onClick(device) }
                    .padding(16.dp)
                )
        }
    }
    LazyColumn {
        item {
            Text(
                text = "Scanned Device",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = modifier.padding()
            )
        }
        items(scannedDevice){device->
            Text(text = device.name ?: "no-name",
                modifier = modifier
                    .fillMaxWidth()
                    .clickable { onClick(device) }
                    .padding(16.dp)
            )
        }
    }
}