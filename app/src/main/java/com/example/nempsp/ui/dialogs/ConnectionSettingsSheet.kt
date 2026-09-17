package com.example.nempsp.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nempsp.model.ConnectionMode
import com.example.nempsp.model.ConnectionStatus
import com.example.nempsp.network.ConnectionManager
import com.example.nempsp.ui.components.NemPspLogoBadge

@Composable
fun ConnectionSettingsSheet(
    connectionManager: ConnectionManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentMode by connectionManager.currentMode.collectAsState()
    val status by connectionManager.connectionStatus.collectAsState()
    val latency by connectionManager.latencyMs.collectAsState()

    var selectedTab by remember {
        mutableIntStateOf(
            when (currentMode) {
                ConnectionMode.WIFI_UDP -> 0
                ConnectionMode.BLUETOOTH -> 1
                ConnectionMode.USB_ADB -> 2
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .testTag("dialog_connection_settings"),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F1219),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF252D3F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NemPspLogoBadge(compact = true)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Connexion Chromebook",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        // Status chip
                        val (chipColor, chipText) = when (status) {
                            ConnectionStatus.CONNECTED -> Color(0xFF00E676) to "Connecté (${latency}ms)"
                            ConnectionStatus.CONNECTING, ConnectionStatus.SCANNING -> Color(0xFFFFD54F) to status.label
                            else -> Color(0xFFFF5252) to "Déconnecté"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(chipColor.copy(alpha = 0.15f))
                                .border(1.dp, chipColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(chipText, color = chipColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Fermer", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }

                // Tabs: WiFi, Bluetooth, USB
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF131722),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF00E5FF)
                        )
                    },
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            connectionManager.setMode(ConnectionMode.WIFI_UDP)
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WiFi Réseau Local", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            connectionManager.setMode(ConnectionMode.BLUETOOTH)
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bluetooth, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bluetooth Sans-fil", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            connectionManager.setMode(ConnectionMode.USB_ADB)
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Usb, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Câble USB (ADB)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0A0C12))
                        .border(1.dp, Color(0xFF1B202E), RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> WifiSettingsTab(connectionManager, context)
                        1 -> BluetoothSettingsTab(connectionManager, context)
                        2 -> UsbSettingsTab(connectionManager, context)
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiSettingsTab(
    connectionManager: ConnectionManager,
    context: Context
) {
    var ipInput by remember { mutableStateOf("192.168.1.100") }
    var portInput by remember { mutableStateOf("8989") }
    val discoveredServers by connectionManager.wifiClient.discoveredServers.collectAsState()
    val wifiStatus by connectionManager.wifiClient.status.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Connexion WiFi UDP Ultra-Basse Latence",
            color = Color(0xFF00E5FF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Connectez votre téléphone et le Chromebook au même réseau WiFi local pour jouer avec un ping minimal (1 à 4ms).",
            color = Color(0xFF90A4AE),
            fontSize = 12.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = ipInput,
                onValueChange = { ipInput = it },
                label = { Text("Adresse IP du Chromebook") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF2E384D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(2f)
            )

            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text("Port") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF2E384D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val port = portInput.toIntOrNull() ?: 8989
                    connectionManager.wifiClient.connect(ipInput, port)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (wifiStatus == ConnectionStatus.CONNECTED) Color(0xFF2E7D32) else Color(0xFF00E676)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (wifiStatus == ConnectionStatus.CONNECTED) "Reconnecter WiFi" else "Connecter WiFi",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { connectionManager.wifiClient.disconnect() },
                enabled = wifiStatus == ConnectionStatus.CONNECTED,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
            ) {
                Text("Déconnecter")
            }

            Button(
                onClick = { connectionManager.wifiClient.startAutoDiscovery() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638))
            ) {
                Icon(Icons.Default.Search, null, tint = Color(0xFF80D8FF), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scanner le réseau", color = Color(0xFF80D8FF), fontSize = 12.sp)
            }
        }

        // Discovered servers section
        if (discoveredServers.isNotEmpty()) {
            Text("Serveurs Chromebook détectés automatiquement :", color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(discoveredServers) { srvIp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF161D2A))
                            .border(1.dp, Color(0xFF28364F), RoundedCornerShape(6.dp))
                            .clickable {
                                ipInput = srvIp
                                connectionManager.wifiClient.connect(srvIp, portInput.toIntOrNull() ?: 8989)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(srvIp, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        Text("Connecter ➔", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BluetoothSettingsTab(
    connectionManager: ConnectionManager,
    context: Context
) {
    val pairedDevices by connectionManager.btClient.pairedDevices.collectAsState()
    val connectedDeviceName by connectionManager.btClient.connectedDeviceName.collectAsState()
    val btStatus by connectionManager.btClient.status.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Connexion Bluetooth Sans-fil (RFCOMM / SPP)",
                    color = Color(0xFF00E5FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Appairez votre téléphone avec votre Chromebook via les paramètres Bluetooth Android.",
                    color = Color(0xFF90A4AE),
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = { connectionManager.btClient.refreshPairedDevices() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638))
            ) {
                Icon(Icons.Default.Refresh, null, tint = Color(0xFF80D8FF), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Actualiser", color = Color(0xFF80D8FF), fontSize = 11.sp)
            }
        }

        if (connectedDeviceName != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1B3D2B))
                    .border(1.dp, Color(0xFF00E676), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Connecté à : $connectedDeviceName", color = Color(0xFFE8F5E9), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Button(
                        onClick = { connectionManager.btClient.disconnect() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Déconnecter", fontSize = 11.sp)
                    }
                }
            }
        }

        Text("Appareils Bluetooth appairés :", color = Color(0xFFCFD8DC), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

        if (pairedDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun appareil Bluetooth appairé trouvé.\nVeuillez activer le Bluetooth et associer votre Chromebook.",
                    color = Color(0xFF78909C),
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(pairedDevices) { (name, address) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF141A26))
                            .border(1.dp, Color(0xFF253047), RoundedCornerShape(6.dp))
                            .clickable { connectionManager.btClient.connectToDevice(address) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(address, color = Color(0xFF78909C), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { connectionManager.btClient.connectToDevice(address) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Connecter", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsbSettingsTab(
    connectionManager: ConnectionManager,
    context: Context
) {
    var usbPort by remember { mutableStateOf("8989") }
    val usbStatus by connectionManager.usbClient.status.collectAsState()
    val latency by connectionManager.usbClient.latencyMs.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Connexion Câble USB Ultra-Basse Latence (< 1ms)",
            color = Color(0xFF00E5FF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Branchez votre téléphone en USB au Chromebook. Activez le débogage USB dans les options développeur Android, puis entrez cette commande dans le terminal Linux Chromebook :",
            color = Color(0xFF90A4AE),
            fontSize = 12.sp
        )

        // Command box with one-click copy
        val adbCmd = "adb reverse tcp:8989 tcp:8989"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF05070B))
                .border(1.dp, Color(0xFF2E3A52), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(adbCmd, color = Color(0xFF00E676), fontFamily = FontFamily.Monospace, fontSize = 13.sp)

            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", adbCmd))
                    Toast.makeText(context, "Commande copiée !", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283D)),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = Color(0xFF80D8FF))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copier", fontSize = 11.sp, color = Color(0xFF80D8FF))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = usbPort,
                onValueChange = { usbPort = it },
                label = { Text("Port USB Local (127.0.0.1)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF2E384D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    val port = usbPort.toIntOrNull() ?: 8989
                    connectionManager.usbClient.connect(port)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (usbStatus == ConnectionStatus.CONNECTED) Color(0xFF2E7D32) else Color(0xFF00E676)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (usbStatus == ConnectionStatus.CONNECTED) "Connecté USB ($latency ms)" else "Connecter USB",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { connectionManager.usbClient.disconnect() },
                enabled = usbStatus == ConnectionStatus.CONNECTED,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
            ) {
                Text("Déconnecter")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF10141F))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("💡 Avantages du mode USB :", color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("• Latence zéro (inférieure à 1 ms), idéal pour les jeux de combat et de rythme PSP.", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                Text("• Aucune interférence radio WiFi / micro-ondes.", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                Text("• Le Chromebook recharge automatiquement la batterie de votre téléphone pendant que vous jouez !", color = Color(0xFFB0BEC5), fontSize = 11.sp)
            }
        }
    }
}
