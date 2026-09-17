package com.example.nempsp.ui.mode

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.LaptopChromebook
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nempsp.model.AppOperationMode
import com.example.nempsp.ui.components.NemPspLogoBadge

@Composable
fun ModeSelectionDialog(
    currentMode: AppOperationMode,
    rememberChoice: Boolean,
    onSelectMode: (AppOperationMode, Boolean) -> Unit
) {
    var selected by remember { mutableStateOf(currentMode) }
    var rememberSelection by remember { mutableStateOf(rememberChoice) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF005070C))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .testTag("mode_selection_dialog")
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, Color(0xFF28344D), RoundedCornerShape(20.dp)),
            color = Color(0xFF0C1018),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NemPspLogoBadge(compact = true)

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CHOISIR LE MODE DE FONCTIONNEMENT",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Sélectionnez le rôle de cet appareil au démarrage",
                            color = Color(0xFF80D8FF),
                            fontSize = 11.sp
                        )
                    }
                }

                // Middle: 2 Mode Cards side-by-side (Landscape optimized)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Option 1: CLIENT (Manette Tactile)
                    LandscapeModeCard(
                        title = "Mode Manette (Client)",
                        subtitle = "Ce smartphone sert de manette PSP tactile haute fidélité avec vibration, retour physique et touches redimensionnables.",
                        badge = "POUR SMARTPHONE / MANETTE",
                        accentColor = Color(0xFF00E5FF),
                        icon = Icons.Default.Gamepad,
                        deviceIcon = Icons.Default.PhoneAndroid,
                        isSelected = selected == AppOperationMode.CONTROLLER_CLIENT,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            selected = AppOperationMode.CONTROLLER_CLIENT
                        },
                        onDoubleClickOrDirect = {
                            // Clic direct pour valider immédiatement !
                            onSelectMode(AppOperationMode.CONTROLLER_CLIENT, rememberSelection)
                        }
                    )

                    // Option 2: SERVEUR (Chromebook / Host)
                    LandscapeModeCard(
                        title = "Mode Récepteur (Serveur)",
                        subtitle = "Cet appareil (Chromebook/PC/TV) reçoit les touches via WiFi (UDP), USB (ADB) ou Bluetooth pour les injecter dans PPSSPP.",
                        badge = "POUR CHROMEBOOK / PC / TV",
                        accentColor = Color(0xFF00E676),
                        icon = Icons.Default.Dns,
                        deviceIcon = Icons.Default.LaptopChromebook,
                        isSelected = selected == AppOperationMode.CHROMEBOOK_SERVER,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            selected = AppOperationMode.CHROMEBOOK_SERVER
                        },
                        onDoubleClickOrDirect = {
                            // Clic direct pour valider immédiatement !
                            onSelectMode(AppOperationMode.CHROMEBOOK_SERVER, rememberSelection)
                        }
                    )
                }

                // Bottom Controls Bar: Checkbox + Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF101522))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Remember Choice Checkbox
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { rememberSelection = !rememberSelection }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberSelection,
                            onCheckedChange = { rememberSelection = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF00E5FF),
                                uncheckedColor = Color(0xFF546E7A)
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mémoriser mon choix au démarrage (modifiable à tout moment dans le menu)",
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp
                        )
                    }

                    // Main Launch Button (prominent & clickable)
                    Button(
                        onClick = {
                            onSelectMode(selected, rememberSelection)
                        },
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("btn_launch_selected_mode"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected == AppOperationMode.CONTROLLER_CLIENT) Color(0xFF00E5FF) else Color(0xFF00E676)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (selected == AppOperationMode.CONTROLLER_CLIENT) "Lancer la Manette PSP" else "Démarrer le Serveur PSP",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeModeCard(
    title: String,
    subtitle: String,
    badge: String,
    accentColor: Color,
    icon: ImageVector,
    deviceIcon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDoubleClickOrDirect: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.01f else 1.0f, label = "cardScale")

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) {
                    Brush.verticalGradient(
                        listOf(Color(0xFF131D2D), Color(0xFF0F1522))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFF0E121B), Color(0xFF080A0E))
                    )
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else Color(0xFF222B3D),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable {
                if (isSelected) {
                    onDoubleClickOrDirect()
                } else {
                    onClick()
                }
            }
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row of Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = if (isSelected) 0.25f else 0.12f))
                            .border(1.dp, accentColor.copy(alpha = if (isSelected) 0.8f else 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = deviceIcon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = badge,
                                color = accentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Check indicator
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) accentColor else Color.Transparent)
                        .border(1.5.dp, if (isSelected) accentColor else Color(0xFF455A64), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Subtitle Description
            Text(
                text = subtitle,
                color = Color(0xFF90A4AE),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // Direct Click Hint Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF141926))
                    .border(1.dp, if (isSelected) accentColor.copy(alpha = 0.5f) else Color(0xFF222B3D), RoundedCornerShape(8.dp))
                    .clickable { onDoubleClickOrDirect() }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSelected) "▶ Cliquez ici pour entrer directement" else "Sélectionner ce mode",
                    color = if (isSelected) accentColor else Color(0xFF78909C),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
