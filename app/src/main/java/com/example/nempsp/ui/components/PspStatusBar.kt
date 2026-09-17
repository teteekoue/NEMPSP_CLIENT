package com.example.nempsp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nempsp.model.ConnectionMode
import com.example.nempsp.model.ConnectionStatus

@Composable
fun PspStatusBar(
    currentMode: ConnectionMode,
    connectionStatus: ConnectionStatus,
    latencyMs: Long,
    packetsPerSecond: Int,
    totalPackets: Int,
    testMode: Boolean,
    onToggleTestMode: (Boolean) -> Unit,
    onOpenConnectionSheet: () -> Unit,
    onOpenCustomizer: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenServerGuide: () -> Unit,
    onSwitchMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        testMode -> Color(0xFF00E5FF)
        connectionStatus == ConnectionStatus.CONNECTED -> Color(0xFF00E676)
        connectionStatus == ConnectionStatus.CONNECTING || connectionStatus == ConnectionStatus.SCANNING -> Color(0xFFFFD54F)
        else -> Color(0xFFFF5252)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: App Name + Connection Mode Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "NEMPSP",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            // Mode badge (Clickable to switch mode / settings)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF161A24))
                    .border(1.dp, Color(0xFF2C3448), RoundedCornerShape(8.dp))
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
                    text = if (testMode) "SIMULATION ACTIVE" else "${currentMode.displayName.split(" ").first()} : ${connectionStatus.label}",
                    color = Color(0xFFD4DAE8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Center: Real-time Telemetry (Ping + PPS)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF10131B))
                .border(1.dp, Color(0xFF1F2636), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (latencyMs >= 0) "Ping: ${latencyMs}ms" else "Ping: --",
                color = if (latencyMs in 0..10) Color(0xFF00E676) else if (latencyMs in 11..40) Color(0xFFFFD54F) else Color(0xFF90A4AE),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(12.dp)
                    .background(Color(0xFF2C3448))
            )

            Text(
                text = "$packetsPerSecond PPS",
                color = Color(0xFFB0BEC5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(12.dp)
                    .background(Color(0xFF2C3448))
            )

            Text(
                text = "#$totalPackets",
                color = Color(0xFF78909C),
                fontSize = 10.sp
            )
        }

        // Right: Action buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Test mode toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (testMode) Color(0xFF00384D) else Color(0xFF161A24))
                    .border(1.dp, if (testMode) Color(0xFF00E5FF) else Color(0xFF262D3D), RoundedCornerShape(6.dp))
                    .clickable { onToggleTestMode(!testMode) }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .testTag("test_mode_toggle")
            ) {
                Text(
                    text = "Test",
                    color = if (testMode) Color(0xFF00E5FF) else Color(0xFF90A4AE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Server companion script dialog button
            IconButton(
                onClick = onOpenServerGuide,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_server_guide")
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = "Serveur Chromebook",
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Live logs console button
            IconButton(
                onClick = onOpenLogs,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_live_logs")
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Logs de débogage",
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Customize / Redesign layout button
            IconButton(
                onClick = onOpenCustomizer,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_customize_layout")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Redesigner manette",
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Switch to Server Mode button
            IconButton(
                onClick = onSwitchMode,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_switch_app_mode")
            ) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = "Changer de rôle (Serveur/Manette)",
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Connection settings button
            IconButton(
                onClick = onOpenConnectionSheet,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_connection_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Paramètres de connexion",
                    tint = Color(0xFFE0E0E0),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
