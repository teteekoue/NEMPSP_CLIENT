package com.example.nempsp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nempsp.model.ConnectionMode
import com.example.nempsp.model.ConnectionStatus
import com.example.nempsp.model.GamepadState

/**
 * Panneau central « écran de la console » : statut, télémétrie, logo et **toute la barre
 * d'outils**.
 *
 * Pourquoi ce changement : les réglages étaient auparavant alignés sur le bord supérieur de
 * l'écran, collés à la tranche (et donc sous l'encoche / le bord arrondi en paysage). Sur un
 * petit téléphone, la barre débordait et les icônes devenaient quasi impossibles à toucher.
 * Tout est désormais regroupé au centre, dans le cadre NEMPSP, avec des cibles de 38 dp et un
 * passage automatique sur deux lignes quand la place manque.
 */
private class ConsoleAction(
    val tag: String,
    val icon: ImageVector,
    val description: String,
    val tint: Color,
    val active: Boolean,
    val onClick: () -> Unit
)

@Composable
fun PspConsolePanel(
    currentMode: ConnectionMode,
    connectionStatus: ConnectionStatus,
    statusDetail: String,
    gamepadState: GamepadState,
    latencyMs: Long,
    packetsPerSecond: Int,
    totalPackets: Int,
    testMode: Boolean,
    onToggleTestMode: (Boolean) -> Unit,
    onOpenConnectionSheet: () -> Unit,
    onOpenCustomizer: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenServerGuide: () -> Unit,
    onSwitchMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        testMode -> Color(0xFF00E5FF)
        connectionStatus == ConnectionStatus.CONNECTED -> Color(0xFF00E676)
        connectionStatus == ConnectionStatus.CONNECTING || connectionStatus == ConnectionStatus.SCANNING -> Color(0xFFFFD54F)
        else -> Color(0xFFFF5252)
    }

    val statusText = when {
        testMode -> "SIMULATION ACTIVE"
        else -> "${currentMode.displayName.split(" ").first()} · ${connectionStatus.label}"
    }

    val actions = listOf(
        ConsoleAction("btn_connection_settings", Icons.Default.Settings, "Paramètres de connexion", Color(0xFFE0E0E0), false, onOpenConnectionSheet),
        ConsoleAction("btn_customize_layout", Icons.Default.Tune, "Personnaliser la manette", Color(0xFFFFB74D), false, onOpenCustomizer),
        ConsoleAction("btn_live_logs", Icons.Default.List, "Journal en direct", Color(0xFF81C784), false, onOpenLogs),
        ConsoleAction("btn_server_guide", Icons.Default.Computer, "Guide récepteur / Chromebook", Color(0xFF64B5F6), false, onOpenServerGuide),
        ConsoleAction("btn_switch_app_mode", Icons.Default.Dns, "Changer de rôle (manette / récepteur)", Color(0xFF00E676), false, onSwitchMode),
        ConsoleAction("test_mode_toggle", Icons.Default.PlayArrow, "Mode test / simulation locale", Color(0xFF00E5FF), testMode) { onToggleTestMode(!testMode) }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // ---------- Ligne 1 : statut de connexion + télémétrie ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF10131B))
                .border(1.dp, Color(0xFF1F2636), RoundedCornerShape(8.dp))
                .clickable { onOpenConnectionSheet() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("connection_mode_badge"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                text = statusText,
                color = Color(0xFFD4DAE8),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (latencyMs >= 0) "${latencyMs}ms" else "--",
                color = when {
                    latencyMs in 0..10 -> Color(0xFF00E676)
                    latencyMs in 11..40 -> Color(0xFFFFD54F)
                    else -> Color(0xFF90A4AE)
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(10.dp)
                    .background(Color(0xFF2C3448))
            )
            Text("$packetsPerSecond p/s", color = Color(0xFFB0BEC5), fontSize = 10.sp)
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(10.dp)
                    .background(Color(0xFF2C3448))
            )
            Text("#$totalPackets", color = Color(0xFF78909C), fontSize = 9.sp)
        }

        // ---------- Ligne 2 : logo + télémétrie manette ----------
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            NemPspLogoBadge(compact = false)

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF10141D))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "STICK (${String.format(java.util.Locale.US, "%+.2f", gamepadState.analogX)}; ${String.format(java.util.Locale.US, "%+.2f", gamepadState.analogY)})",
                    color = Color(0xFF00E5FF),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Text(
                    text = if (gamepadState.activeButtons.isEmpty()) {
                        "BTNS ---"
                    } else {
                        "BTNS ${gamepadState.activeButtons.joinToString("") { it.symbol }}"
                    },
                    color = Color(0xFF81C784),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ---------- Ligne 3 : diagnostic (uniquement quand quelque chose cloche) ----------
        val showDiagnostic = statusDetail.isNotBlank() &&
            !testMode &&
            connectionStatus != ConnectionStatus.CONNECTED
        if (showDiagnostic) {
            Text(
                text = statusDetail,
                color = if (connectionStatus == ConnectionStatus.ERROR) Color(0xFFFFAB91) else Color(0xFFFFE082),
                fontSize = 9.sp,
                lineHeight = 11.sp,
                maxLines = 3,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x33FF5252).copy(alpha = if (connectionStatus == ConnectionStatus.ERROR) 0.12f else 0.07f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .testTag("status_detail")
            )
        }

        // ---------- Ligne 4 : barre d'outils, centrée ----------
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val perRow = if (maxWidth < 250.dp) 3 else actions.size
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                actions.chunked(perRow).forEach { rowActions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowActions.forEach { action ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (action.active) Color(0xFF00384D) else Color(0xFF161A24))
                                    .border(
                                        1.dp,
                                        if (action.active) Color(0xFF00E5FF) else Color(0xFF262D3D),
                                        RoundedCornerShape(9.dp)
                                    )
                                    .clickable { action.onClick() }
                                    .testTag(action.tag),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.description,
                                    tint = action.tint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
