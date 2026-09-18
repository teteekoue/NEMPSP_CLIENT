package com.example.nempsp.ui.dialogs

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.nempsp.model.ConnectionMode
import com.example.nempsp.model.ConnectionStatus
import com.example.nempsp.network.ConnectionManager
import com.example.nempsp.network.WifiUdpClient
import com.example.nempsp.repository.ConnectionPreferences
import com.example.nempsp.ui.components.NemPspLogoBadge

/**
 * Panneau de connexion (WiFi / Bluetooth / USB).
 *
 * Corrections d'ergonomie :
 * - **Contenu enfin déroulant** : les onglets utilisaient `fillMaxSize()` sans défilement, donc
 *   sur un petit écran en paysage le bouton « Connecter » passait sous la zone visible et
 *   devenait inaccessible. Tout le contenu défile maintenant.
 * - **Clavier géré** : `decorFitsSystemWindows = false` + `imePadding()` — auparavant le clavier
 *   recouvrait les champs IP/Port (barres système masquées, `adjustPan`, aucun inset IME).
 * - **IP, port et mode mémorisés** entre les sessions ([ConnectionPreferences]).
 * - **Diagnostic visible** : le détail d'état du transport (ex. « aucun récepteur n'écoute sur
 *   cette IP ») est affiché en haut du panneau, plus seulement dans le journal.
 * - Libellés d'onglets courts + validation du format d'IP.
 */
@Composable
fun ConnectionSettingsSheet(
    connectionManager: ConnectionManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { ConnectionPreferences(context) }

    val currentMode by connectionManager.currentMode.collectAsState()
    val status by connectionManager.connectionStatus.collectAsState()
    val statusDetail by connectionManager.statusDetail.collectAsState()
    val latency by connectionManager.latencyMs.collectAsState()

    var selectedTab by remember {
        mutableIntStateOf(
            when (prefs.connectionMode) {
                ConnectionMode.WIFI_UDP -> 0
                ConnectionMode.BLUETOOTH -> 1
                ConnectionMode.USB_ADB -> 2
            }
        )
    }

    // Le mode choisi est conservé pour la prochaine ouverture du panneau
    LaunchedEffect(currentMode) { prefs.connectionMode = currentMode }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(6.dp)
                .testTag("dialog_connection_settings"),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F1219),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF252D3F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                // ---------------- En-tête ----------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        NemPspLogoBadge(compact = true)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connexion",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val (chipColor, chipText) = when (status) {
                            ConnectionStatus.CONNECTED -> Color(0xFF00E676) to "Connecté (${latency}ms)"
                            ConnectionStatus.CONNECTING, ConnectionStatus.SCANNING -> Color(0xFFFFD54F) to status.label
                            ConnectionStatus.ERROR -> Color(0xFFFF5252) to "Échec"
                            ConnectionStatus.DISCONNECTED -> Color(0xFFFF5252) to "Déconnecté"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(chipColor.copy(alpha = 0.15f))
                                .border(1.dp, chipColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(chipText, color = chipColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, "Fermer", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }

                // ---------------- Diagnostic ----------------
                if (statusDetail.isNotBlank() && status != ConnectionStatus.CONNECTED) {
                    Text(
                        text = statusDetail,
                        color = if (status == ConnectionStatus.ERROR) Color(0xFFFFAB91) else Color(0xFFFFE082),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1AFF5252))
                            .border(1.dp, Color(0x55FF5252), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("connection_status_detail")
                    )
                }

                // ---------------- Onglets ----------------
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
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            connectionManager.setMode(ConnectionMode.WIFI_UDP)
                        },
                        modifier = Modifier.height(44.dp),
                        text = { TabLabel(Icons.Default.Wifi, "WiFi") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            connectionManager.setMode(ConnectionMode.BLUETOOTH)
                        },
                        modifier = Modifier.height(44.dp),
                        text = { TabLabel(Icons.Default.Bluetooth, "Bluetooth") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            connectionManager.setMode(ConnectionMode.USB_ADB)
                        },
                        modifier = Modifier.height(44.dp),
                        text = { TabLabel(Icons.Default.Usb, "USB") }
                    )
                }

                // ---------------- Contenu déroulant ----------------
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0A0C12))
                        .border(1.dp, Color(0xFF1B202E), RoundedCornerShape(10.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (selectedTab) {
                            0 -> WifiSettingsTab(connectionManager, prefs)
                            1 -> BluetoothSettingsTab(connectionManager, context)
                            2 -> UsbSettingsTab(connectionManager, prefs, context)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun SectionTitle(text: String, color: Color = Color(0xFF00E5FF)) {
    Text(text = text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SectionHint(text: String) {
    Text(text = text, color = Color(0xFF90A4AE), fontSize = 11.sp, lineHeight = 14.sp)
}

/** `OutlinedTextFieldDefaults.colors()` est @Composable : il faut une fonction, pas une constante. */
@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF00E5FF),
    unfocusedBorderColor = Color(0xFF2E384D),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color(0xFF80D8FF),
    unfocusedLabelColor = Color(0xFF78909C)
)

private fun isValidIpv4(ip: String): Boolean {
    val parts = ip.trim().split(".")
    if (parts.size != 4) return false
    return parts.all { part ->
        val value = part.toIntOrNull()
        value != null && value in 0..255 && part.length <= 3
    }
}

// =====================================================================================
// ONGLET WiFi
// =====================================================================================

@Composable
private fun WifiSettingsTab(
    connectionManager: ConnectionManager,
    prefs: ConnectionPreferences
) {
    var ipInput by remember { mutableStateOf(prefs.serverIp) }
    var portInput by remember { mutableStateOf(prefs.serverPort.toString()) }
    val discoveredServers by connectionManager.wifiClient.discoveredServers.collectAsState()
    val wifiStatus by connectionManager.wifiClient.status.collectAsState()

    val ipValid = isValidIpv4(ipInput)
    val portValid = (portInput.toIntOrNull() ?: -1) in 1..65535

    fun connectNow() {
        val port = portInput.toIntOrNull() ?: WifiUdpClient.DEFAULT_PORT
        prefs.serverIp = ipInput
        prefs.serverPort = port
        connectionManager.wifiClient.connect(ipInput, port)
    }

    SectionTitle("WiFi local (UDP) — la voie la plus simple")
    SectionHint(
        "Les deux appareils doivent être sur le même réseau WiFi. Lancez le Mode Récepteur sur " +
            "l'appareil de jeu, puis saisissez l'IP affichée sur son écran (ou utilisez le scan)."
    )

    OutlinedTextField(
        value = ipInput,
        onValueChange = { ipInput = it },
        label = { Text("Adresse IP du récepteur") },
        singleLine = true,
        isError = !ipValid,
        supportingText = {
            if (!ipValid) Text("Format attendu : 192.168.1.45", color = Color(0xFFFF8A80), fontSize = 10.sp)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = darkFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("field_wifi_ip")
    )

    OutlinedTextField(
        value = portInput,
        onValueChange = { portInput = it.filter { c -> c.isDigit() }.take(5) },
        label = { Text("Port") },
        singleLine = true,
        isError = !portValid,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = darkFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("field_wifi_port")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { connectNow() },
            enabled = ipValid && portValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (wifiStatus == ConnectionStatus.CONNECTED) Color(0xFF2E7D32) else Color(0xFF00E676)
            ),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("btn_wifi_connect")
        ) {
            Text(
                text = if (wifiStatus == ConnectionStatus.CONNECTED) "Reconnecter" else "Connecter",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        OutlinedButton(
            onClick = { connectionManager.wifiClient.disconnect() },
            enabled = wifiStatus != ConnectionStatus.DISCONNECTED,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
            modifier = Modifier.height(44.dp)
        ) {
            Text("Couper", fontSize = 12.sp)
        }
    }

    Button(
        onClick = { connectionManager.wifiClient.startAutoDiscovery() },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("btn_wifi_scan")
    ) {
        Icon(Icons.Default.Search, null, tint = Color(0xFF80D8FF), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Scanner le réseau à la recherche du récepteur", color = Color(0xFF80D8FF), fontSize = 12.sp)
    }

    if (discoveredServers.isNotEmpty()) {
        SectionTitle("Récepteurs détectés", Color(0xFF81C784))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            discoveredServers.forEach { serverIp ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF161D2A))
                        .border(1.dp, Color(0xFF28364F), RoundedCornerShape(6.dp))
                        .clickable {
                            ipInput = serverIp
                            connectNow()
                        }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(serverIp, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text("Connecter ➔", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    InfoBox(
        "Si le scan ne trouve rien : vérifiez que le Mode Récepteur est bien démarré sur l'appareil " +
            "de jeu (son écran doit afficher « Écoute UDP 0.0.0.0:8989 ACTIVE »), que les deux " +
            "appareils sont sur le même WiFi, et que la box n'a pas activé l'« isolation client »."
    )
}

// =====================================================================================
// ONGLET Bluetooth
// =====================================================================================

@Composable
private fun BluetoothSettingsTab(
    connectionManager: ConnectionManager,
    context: Context
) {
    val pairedDevices by connectionManager.btClient.pairedDevices.collectAsState()
    val connectedDeviceName by connectionManager.btClient.connectedDeviceName.collectAsState()
    val btStatus by connectionManager.btClient.status.collectAsState()
    val prefs = remember { ConnectionPreferences(context) }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { connectionManager.btClient.refreshPairedDevices() }
    )

    fun requestBtAndRefresh() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val needed = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (needed.isNotEmpty()) {
                btPermissionLauncher.launch(needed.toTypedArray())
                return
            }
        }
        connectionManager.btClient.refreshPairedDevices()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SectionTitle("Bluetooth (RFCOMM / SPP)")
            SectionHint("Appairez d'abord les deux appareils dans les paramètres Bluetooth Android.")
        }
        Button(
            onClick = { requestBtAndRefresh() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
            modifier = Modifier.height(38.dp)
        ) {
            Icon(Icons.Default.Refresh, null, tint = Color(0xFF80D8FF), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Actualiser", color = Color(0xFF80D8FF), fontSize = 11.sp)
        }
    }

    if (connectedDeviceName != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1B3D2B))
                .border(1.dp, Color(0xFF00E676), RoundedCornerShape(8.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Connecté à : $connectedDeviceName", color = Color(0xFFE8F5E9), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Button(
                onClick = { connectionManager.btClient.disconnect() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Couper", fontSize = 11.sp, color = Color.Black)
            }
        }
    }

    SectionTitle("Appareils appairés", Color(0xFFCFD8DC))

    if (pairedDevices.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF131722))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aucun appareil appairé trouvé.\nActivez le Bluetooth, associez l'appareil de jeu, puis touchez « Actualiser ».",
                color = Color(0xFF78909C),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            pairedDevices.forEach { (name, address) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF141A26))
                        .border(1.dp, Color(0xFF253047), RoundedCornerShape(6.dp))
                        .clickable {
                            prefs.bluetoothAddress = address
                            connectionManager.btClient.connectToDevice(address)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        Text(address, color = Color(0xFF78909C), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            prefs.bluetoothAddress = address
                            connectionManager.btClient.connectToDevice(address)
                        },
                        enabled = btStatus != ConnectionStatus.CONNECTED,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Connecter", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    InfoBox(
        "Le récepteur doit être en Mode Récepteur avec le Bluetooth activé : il ouvre alors une " +
            "écoute RFCOMM « NEMPSP_GAMEPAD_SERVER ». La latence affichée est mesurée réellement " +
            "(aller-retour), plus estimée."
    )
}

// =====================================================================================
// ONGLET USB
// =====================================================================================

@Composable
private fun UsbSettingsTab(
    connectionManager: ConnectionManager,
    prefs: ConnectionPreferences,
    context: Context
) {
    var usbPort by remember { mutableStateOf(prefs.usbPort.toString()) }
    val usbStatus by connectionManager.usbClient.status.collectAsState()
    val latency by connectionManager.usbClient.latencyMs.collectAsState()
    val portValid = (usbPort.toIntOrNull() ?: -1) in 1..65535

    SectionTitle("Câble USB (adb reverse) — latence la plus faible")
    SectionHint(
        "Branchez le téléphone à l'appareil de jeu, activez le débogage USB (options développeur), " +
            "puis lancez cette commande dans un terminal sur l'appareil de jeu :"
    )

    val adbPort = usbPort.toIntOrNull() ?: WifiUdpClient.DEFAULT_PORT
    val adbCmd = "adb reverse tcp:$adbPort tcp:$adbPort"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF05070B))
            .border(1.dp, Color(0xFF2E3A52), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(adbCmd, color = Color(0xFF00E676), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Button(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Commande ADB", adbCmd))
                Toast.makeText(context, "Commande copiée !", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283D)),
            modifier = Modifier.height(30.dp)
        ) {
            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(13.dp), tint = Color(0xFF80D8FF))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copier", fontSize = 10.sp, color = Color(0xFF80D8FF))
        }
    }

    OutlinedTextField(
        value = usbPort,
        onValueChange = { usbPort = it.filter { c -> c.isDigit() }.take(5) },
        label = { Text("Port du tunnel (127.0.0.1)") },
        singleLine = true,
        isError = !portValid,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = darkFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                val port = usbPort.toIntOrNull() ?: WifiUdpClient.DEFAULT_PORT
                prefs.usbPort = port
                connectionManager.usbClient.connect(port)
            },
            enabled = portValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (usbStatus == ConnectionStatus.CONNECTED) Color(0xFF2E7D32) else Color(0xFF00E676)
            ),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
        ) {
            Text(
                text = if (usbStatus == ConnectionStatus.CONNECTED) "Connecté ($latency ms)" else "Connecter USB",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        OutlinedButton(
            onClick = { connectionManager.usbClient.disconnect() },
            enabled = usbStatus != ConnectionStatus.DISCONNECTED,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
            modifier = Modifier.height(44.dp)
        ) {
            Text("Couper", fontSize = 12.sp)
        }
    }

    InfoBox(
        "Avantages du filaire : latence minimale et stable, aucune interférence radio, et le " +
            "téléphone se recharge pendant la partie. Le port doit être identique des deux côtés."
    )
}

// =====================================================================================
// Élément partagé
// =====================================================================================

@Composable
private fun InfoBox(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF10141F))
            .border(1.dp, Color(0xFF1E2536), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFD54F))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color(0xFFB0BEC5), fontSize = 11.sp, lineHeight = 15.sp)
    }
}
