package com.example.nempsp.ui.server

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nempsp.model.PspButton
import com.example.nempsp.server.NemPspServerEngine
import com.example.nempsp.server.ServerStatus
import com.example.nempsp.ui.components.NemPspLogoBadge

/**
 * Écran « Mode Récepteur ».
 *
 * Corrections apportées :
 * - **Plus de statut inventé** : l'en-tête affichait « EN LIGNE (8989) » dès que `isRunning`
 *   était vrai, même quand aucun socket n'avait pu s'ouvrir (port occupé, Bluetooth refusé…).
 *   C'est précisément ce qui masquait le « Erreur envoi paquet UDP » côté manette. On affiche
 *   maintenant [ServerStatus.readyForClient], l'état de CHAQUE transport et `startupError`.
 * - **Toutes les adresses IPv4** de l'appareil sont listées et copiables (un Chromebook expose
 *   souvent arc0/vnic0 en plus de wlan0) ; le port utilisé suit la configuration réelle.
 * - **Petits écrans** : au-delà de 640 dp de large on garde les deux colonnes, en dessous tout
 *   s'empile dans une colonne déroulante, et la barre de boutons passe en FlowRow.
 */
@OptIn(ExperimentalLayoutApi::class)
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

    fun copy(text: String, note: String) {
        clipboardManager.setText(AnnotatedString(text))
        copiedNote = note
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
            .testTag("psp_server_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ---------------- Barre supérieure ----------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF101420))
                    .border(1.dp, Color(0xFF222B3D), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "RÉCEPTEUR",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ServerStateBadge(status)
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            if (status.isRunning) serverEngine.stopServer() else serverEngine.startServer()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status.isRunning) Color(0xFFD32F2F) else Color(0xFF00E676)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_server_toggle")
                    ) {
                        Icon(
                            imageVector = if (status.isRunning) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (status.isRunning) "Arrêter" else "Démarrer",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            if (!status.isRunning) serverEngine.startServer()
                            val ppssppIntent = context.packageManager.getLaunchIntentForPackage("org.ppsspp.ppsspp")
                                ?: context.packageManager.getLaunchIntentForPackage("org.ppsspp.ppssppgold")
                            if (ppssppIntent != null) {
                                context.startActivity(ppssppIntent)
                            } else {
                                context.startActivity(
                                    Intent(Intent.ACTION_MAIN).apply {
                                        addCategory(Intent.CATEGORY_HOME)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Launch, null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("PPSSPP", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onSwitchToController,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                    ) {
                        Icon(Icons.Default.SportsEsports, null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Mode Manette", fontSize = 12.sp)
                    }
                }

                // Erreur de démarrage : à ne jamais masquer, c'est LA cause des envois UDP en échec
                status.startupError?.let { error ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22FF5252))
                            .border(1.dp, Color(0x66FF5252), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFF8A80), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = error,
                            color = Color(0xFFFFCDD2),
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.testTag("server_startup_error")
                        )
                    }
                }
            }

            // ---------------- Contenu ----------------
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (maxWidth < 640.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ServerInfoColumn(status, serverEngine, Modifier.fillMaxWidth(), ::copy, copiedNote)
                        ServerMonitorColumn(status, serverEngine, Modifier.fillMaxWidth())
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ServerInfoColumn(
                            status,
                            serverEngine,
                            Modifier
                                .weight(0.48f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            ::copy,
                            copiedNote
                        )
                        ServerMonitorColumn(
                            status,
                            serverEngine,
                            Modifier
                                .weight(0.52f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================================
// En-tête : badge d'état réel
// =====================================================================================

@Composable
private fun ServerStateBadge(status: ServerStatus) {
    val (background, text, label) = when {
        status.readyForClient -> Triple(Color(0xFF1B5E20), Color(0xFF69F0AE), "EN LIGNE · ${status.udpPort}")
        status.isRunning -> Triple(Color(0xFF4E342E), Color(0xFFFFAB91), "INCOMPLET")
        else -> Triple(Color(0xFF37474F), Color(0xFFB0BEC5), "ARRÊTÉ")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

// =====================================================================================
// Colonne gauche : état réel des transports, adresses IP, guide de connexion
// =====================================================================================

@Composable
private fun ServerInfoColumn(
    status: ServerStatus,
    serverEngine: NemPspServerEngine,
    modifier: Modifier = Modifier,
    onCopy: (String, String) -> Unit,
    copiedNote: String? = null
) {
    val adbPort = status.udpPort
    val adbCmd = "adb reverse tcp:$adbPort tcp:$adbPort"
    val mainIp = status.allIpAddresses.firstOrNull() ?: status.localIpAddress

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0E121B))
            .border(1.dp, Color(0xFF1E2536), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "État des transports",
            color = Color(0xFF00E676),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TransportChip("UDP", status.udpListening, ":${status.udpPort}", Modifier.weight(1f))
            TransportChip("TCP", status.tcpListening, ":${status.tcpPort}", Modifier.weight(1f))
            TransportChip("Bluetooth", status.btListening, "SPP", Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatCard("Paquets reçus", "${status.packetsReceived}", Modifier.weight(1f))
            StatCard("Fréquence", "${status.packetsPerSecond} Hz", Modifier.weight(1f))
            StatCard("Clients", "${status.activeConnectionsCount}", Modifier.weight(1f))
        }

        StatCard("Dernière source client", status.lastClientSource, Modifier.fillMaxWidth())

        // ---------------- Adresses IP ----------------
        Text(
            text = "Adresses IPv4 de cet appareil (touchez pour copier)",
            color = Color(0xFFFFD54F),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        if (status.allIpAddresses.isEmpty()) {
            Text(
                text = if (status.isRunning) {
                    "Aucune adresse WiFi exploitable détectée. Vérifiez que cet appareil est bien " +
                        "connecté au même réseau que la manette."
                } else {
                    "Démarrez le récepteur pour détecter les adresses disponibles."
                },
                color = Color(0xFF90A4AE),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                status.allIpAddresses.forEachIndexed { index, ip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (index == 0) Color(0xFF14241B) else Color(0xFF131722))
                            .border(
                                1.dp,
                                if (index == 0) Color(0xFF2E7D32) else Color(0xFF222A3B),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onCopy(ip, "IP $ip copiée") }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$ip:$adbPort",
                            color = if (index == 0) Color(0xFFA5D6A7) else Color(0xFFCFD8DC),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (index == 0) {
                                Text("à utiliser", color = Color(0xFF69F0AE), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Icon(
                                Icons.Default.ContentCopy,
                                null,
                                tint = Color(0xFF80D8FF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // ---------------- Guide ----------------
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
                text = "Comment connecter la manette :",
                color = Color(0xFFFFD54F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            GuideRow(Icons.Default.Wifi, Color(0xFF00E5FF)) {
                "1. WiFi : dans l'app manette, saisissez $mainIp (port $adbPort) ou lancez le scan automatique."
            }
            GuideRow(Icons.Default.Usb, Color(0xFF69F0AE)) {
                "2. USB : branchez le téléphone puis lancez sur cet appareil :"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0A0D14))
                    .border(1.dp, Color(0xFF1B2332), RoundedCornerShape(6.dp))
                    .clickable { onCopy(adbCmd, "Commande ADB copiée") }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(adbCmd, color = Color(0xFF69F0AE), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF80D8FF), modifier = Modifier.size(14.dp))
            }

            GuideRow(Icons.Default.NotificationsActive, Color(0xFFB388FF)) {
                "3. Bluetooth : appairez le téléphone, puis choisissez-le dans la liste du panneau de connexion."
            }
            GuideRow(Icons.Default.SportsEsports, Color(0xFF80D8FF)) {
                "4. Une notification permanente maintient le récepteur actif pendant que PPSSPP est au premier plan."
            }

            copiedNote?.let {
                Text(it, color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ---------------- Injection dans PPSSPP ----------------
        InjectionPanel(serverEngine = serverEngine, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun GuideRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    text: () -> String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text(), color = Color(0xFFCFD8DC), fontSize = 11.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun TransportChip(
    name: String,
    listening: Boolean,
    detail: String,
    modifier: Modifier = Modifier
) {
    val color = if (listening) Color(0xFF69F0AE) else Color(0xFF78909C)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (listening) Color(0xFF14241B) else Color(0xFF141926))
            .border(1.dp, if (listening) Color(0xFF2E7D32) else Color(0xFF222C40), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(name, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text(
            text = if (listening) "écoute $detail" else "inactif",
            color = Color(0xFF90A4AE),
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

// =====================================================================================
// Colonne droite : moniteur des touches reçues
// =====================================================================================

@Composable
private fun ServerMonitorColumn(
    status: ServerStatus,
    serverEngine: NemPspServerEngine,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
                text = "Touches reçues en direct",
                color = Color(0xFF00E5FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
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

        // Grille fixe (pas de LazyVerticalGrid : évite les défilements imbriqués en mode compact)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PspButton.entries.chunked(4).forEach { rowButtons ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowButtons.forEach { btn ->
                        val isPressed = status.activeButtons.contains(btn)
                        VirtualServerButton(
                            button = btn,
                            isPressed = isPressed,
                            onSimulateClick = { serverEngine.injectSimulatedInput(btn, !isPressed) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF111624))
                .padding(8.dp)
        ) {
            Text(
                text = if (status.readyForClient) {
                    "Récepteur prêt : une touche pressée sur la manette doit s'allumer ici. " +
                        "Touchez une touche pour simuler une injection."
                } else {
                    "Aucun transport à l'écoute : démarrez le récepteur et corrigez l'erreur affichée en haut."
                },
                color = Color(0xFF90A4AE),
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
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
        Text(title, color = Color(0xFF78909C), fontSize = 10.sp, maxLines = 1)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun VirtualServerButton(
    button: PspButton,
    isPressed: Boolean,
    onSimulateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) Color(0xFF00E676) else Color(0xFF141A28))
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
                color = if (isPressed) Color.Black else Color(0xFFB0BEC5),
                fontSize = 11.sp,
                fontWeight = if (isPressed) FontWeight.Black else FontWeight.Bold,
                maxLines = 1
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
