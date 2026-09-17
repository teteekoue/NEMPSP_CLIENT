package com.example.nempsp.ui.server

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import com.example.nempsp.model.PspButton
import com.example.nempsp.server.NemPspServerEngine
import com.example.nempsp.ui.components.NemPspLogoBadge

@Composable
fun PspServerScreen(
    serverEngine: NemPspServerEngine,
    onSwitchToController: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status by serverEngine.status.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copiedNote by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
            .testTag("psp_server_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF101420))
                    .border(1.dp, Color(0xFF222B3D), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NemPspLogoBadge(compact = true)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NEMPSP SERVEUR RÉCEPTEUR",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (status.isRunning) Color(0xFF1B5E20) else Color(0xFF37474F))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (status.isRunning) "EN LIGNE (8989)" else "ARRÊTÉ",
                                    color = if (status.isRunning) Color(0xFF69F0AE) else Color(0xFFB0BEC5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "IP: ${status.localIpAddress} | Port: ${status.udpPort} (WiFi/UDP & USB/TCP)",
                            color = Color(0xFF80D8FF),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (status.isRunning) serverEngine.stopServer()
                            else serverEngine.startServer()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status.isRunning) Color(0xFFD32F2F) else Color(0xFF00E676)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = if (status.isRunning) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (status.isRunning) "Arrêter" else "Démarrer Serveur",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Bouton pour basculer vers PPSSPP ou passer en arrière-plan
                    Button(
                        onClick = {
                            // Si le serveur n'est pas lancé, on le démarre d'abord
                            if (!status.isRunning) {
                                serverEngine.startServer()
                            }
                            // Tenter d'ouvrir directement PPSSPP s'il est installé sur l'appareil
                            val ppssppIntent = context.packageManager.getLaunchIntentForPackage("org.ppsspp.ppsspp")
                                ?: context.packageManager.getLaunchIntentForPackage("org.ppsspp.ppssppgold")

                            if (ppssppIntent != null) {
                                context.startActivity(ppssppIntent)
                            } else {
                                // Retour à l'écran d'accueil pour laisser l'utilisateur ouvrir son jeu, tout en gardant le serveur actif en arrière-plan
                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(homeIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Launch, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aller sur PPSSPP", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onSwitchToController,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                    ) {
                        Icon(Icons.Default.SportsEsports, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mode Manette", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // MAIN CONTENT SPLIT (Left: Telemetry & Config / Right: Virtual Monitor & Test inputs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // LEFT COLUMN: Informations & Connection Guide
                Column(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0E121B))
                        .border(1.dp, Color(0xFF1E2536), RoundedCornerShape(14.dp))
                        .verticalScroll(scrollState)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "État des Connexions & Télémétrie",
                        color = Color(0xFF00E676),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatCard("Paquets reçus", "${status.packetsReceived}", Modifier.weight(1f))
                        StatCard("Fréquence (PPS)", "${status.packetsPerSecond} Hz", Modifier.weight(1f))
                        StatCard("Clients actifs", "${status.activeConnectionsCount}", Modifier.weight(1f))
                    }

                    StatCard(
                        "Dernière source client",
                        status.lastClientSource,
                        Modifier.fillMaxWidth()
                    )

                    // Instructions to connect
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF131722))
                            .border(1.dp, Color(0xFF222A3B), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Comment connecter NEMPSP Client :",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "1. WiFi UDP : Entrez l'IP ${status.localIpAddress} dans l'app manette ou utilisez le scan auto.",
                                color = Color(0xFFCFD8DC),
                                fontSize = 11.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Usb, null, tint = Color(0xFF69F0AE), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "2. USB Filaire : Branchez le smartphone et lancez sur votre Chromebook/PC :",
                                color = Color(0xFFCFD8DC),
                                fontSize = 11.sp
                            )
                        }

                        // Copyable ADB command
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0A0D14))
                                .border(1.dp, Color(0xFF1B2332), RoundedCornerShape(6.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString("adb reverse tcp:8989 tcp:8989"))
                                    copiedNote = "Commande ADB copiée !"
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "adb reverse tcp:8989 tcp:8989",
                                color = Color(0xFF69F0AE),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                            Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF80D8FF), modifier = Modifier.size(14.dp))
                        }

                        copiedNote?.let {
                            Text(it, color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFFB388FF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "3. Bluetooth RFCOMM : Appairez le smartphone dans les paramètres Bluetooth du Chromebook.",
                                color = Color(0xFFCFD8DC),
                                fontSize = 11.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsEsports, null, tint = Color(0xFFFF8A80), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "4. Arrière-plan & PPSSPP : Une notification permanente maintient le serveur actif dès que vous ouvrez PPSSPP !",
                                color = Color(0xFF80D8FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // RIGHT COLUMN: Live Inputs Monitor & Virtual Button Tester
                Column(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0C1018))
                        .border(1.dp, Color(0xFF1E2536), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Moniteur des Touches Reçues en Direct",
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Analog Stick status pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF182030))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Stick: X=%.2f Y=%.2f".format(status.analogX, status.analogY),
                                color = Color(0xFF80D8FF),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Grid of 16 PSP Buttons with real-time illumination
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(PspButton.entries.toTypedArray()) { btn ->
                            val isPressed = status.activeButtons.contains(btn)
                            VirtualServerButton(
                                button = btn,
                                isPressed = isPressed,
                                onSimulateClick = {
                                    serverEngine.injectSimulatedInput(btn, !isPressed)
                                }
                            )
                        }
                    }

                    // Bottom info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111624))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Astuce : Cliquez sur une touche ci-dessus pour simuler et vérifier l'injection serveur !",
                            color = Color(0xFF90A4AE),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141926))
            .border(1.dp, Color(0xFF222C40), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(title, color = Color(0xFF78909C), fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun VirtualServerButton(
    button: PspButton,
    isPressed: Boolean,
    onSimulateClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isPressed) Color(0xFF00E676) else Color(0xFF141A28),
        label = "btnBg"
    )
    val textColor by animateColorAsState(
        if (isPressed) Color.Black else Color(0xFFB0BEC5),
        label = "btnText"
    )
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1.0f, label = "btnScale")

    Box(
        modifier = Modifier
            .scale(scale)
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                1.dp,
                if (isPressed) Color(0xFF69F0AE) else Color(0xFF253046),
                RoundedCornerShape(8.dp)
            )
            .clickable { onSimulateClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = button.label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isPressed) FontWeight.Black else FontWeight.Bold
            )
            Text(
                text = if (isPressed) "ON" else "OFF",
                color = if (isPressed) Color(0xFF004D40) else Color(0xFF546E7A),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
