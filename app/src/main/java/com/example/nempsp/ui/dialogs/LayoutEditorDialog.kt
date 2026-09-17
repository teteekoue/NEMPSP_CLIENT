package com.example.nempsp.ui.dialogs

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nempsp.model.HapticProfile
import com.example.nempsp.model.LayoutConfig
import com.example.nempsp.model.PspSkin
import com.example.nempsp.ui.components.NemPspLogoBadge

@Composable
fun LayoutEditorDialog(
    currentConfig: LayoutConfig,
    onSaveConfig: (LayoutConfig) -> Unit,
    onResetDefaults: () -> LayoutConfig,
    onTestVibration: (HapticProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var skin by remember { mutableStateOf(currentConfig.skin) }
    var dpadScale by remember { mutableFloatStateOf(currentConfig.dpadScale) }
    var actionScale by remember { mutableFloatStateOf(currentConfig.actionScale) }
    var analogScale by remember { mutableFloatStateOf(currentConfig.analogScale) }
    var triggerScale by remember { mutableFloatStateOf(currentConfig.triggerScale) }
    var systemBarScale by remember { mutableFloatStateOf(currentConfig.systemBarScale) }
    var opacity by remember { mutableFloatStateOf(currentConfig.buttonsOpacity) }
    var deadzone by remember { mutableFloatStateOf(currentConfig.stickDeadzone) }
    var sensitivity by remember { mutableFloatStateOf(currentConfig.stickSensitivity) }
    var hapticFeedback by remember { mutableStateOf(currentConfig.hapticFeedback) }
    var hapticProfile by remember { mutableStateOf(currentConfig.hapticProfile) }
    var touchAnimationEnabled by remember { mutableStateOf(currentConfig.touchAnimationEnabled) }
    var buttonGlowIntensity by remember { mutableFloatStateOf(currentConfig.buttonGlowIntensity) }
    var soundFeedback by remember { mutableStateOf(currentConfig.soundFeedback) }

    val scrollState = rememberScrollState()

    fun buildUpdatedConfig(): LayoutConfig {
        return currentConfig.copy(
            skin = skin,
            dpadScale = dpadScale,
            actionScale = actionScale,
            analogScale = analogScale,
            triggerScale = triggerScale,
            systemBarScale = systemBarScale,
            buttonsOpacity = opacity,
            stickDeadzone = deadzone,
            stickSensitivity = sensitivity,
            hapticFeedback = hapticFeedback,
            hapticProfile = hapticProfile,
            touchAnimationEnabled = touchAnimationEnabled,
            buttonGlowIntensity = buttonGlowIntensity,
            soundFeedback = soundFeedback
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.94f)
                .testTag("dialog_layout_editor"),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0C0F17),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF252D3F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header with custom NEMPSP logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NemPspLogoBadge(compact = true)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Personnalisation Manette",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Redimensionnez les touches & réglez l'immersion",
                                color = Color(0xFF80D8FF),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Fermer", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }

                // Scrollable options
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: REDIMENSIONNER LES TOUCHES À VOLONTÉ
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF121622))
                            .border(1.dp, Color(0xFF232B3D), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Redimensionnement des Touches à volonté",
                                color = Color(0xFF00E5FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Sliders grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SliderControl(
                                    title = "Croix Directionnelle (D-Pad)",
                                    value = dpadScale,
                                    range = 0.50f..1.80f,
                                    displayValue = "${(dpadScale * 100).toInt()}%",
                                    onValueChange = { dpadScale = it }
                                )

                                SliderControl(
                                    title = "Touches Action (△ ○ ✕ □)",
                                    value = actionScale,
                                    range = 0.50f..1.80f,
                                    displayValue = "${(actionScale * 100).toInt()}%",
                                    onValueChange = { actionScale = it }
                                )

                                SliderControl(
                                    title = "Stick Analogique",
                                    value = analogScale,
                                    range = 0.50f..1.80f,
                                    displayValue = "${(analogScale * 100).toInt()}%",
                                    onValueChange = { analogScale = it }
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SliderControl(
                                    title = "Gâchettes d'Épaule (L & R)",
                                    value = triggerScale,
                                    range = 0.60f..1.80f,
                                    displayValue = "${(triggerScale * 100).toInt()}%",
                                    onValueChange = { triggerScale = it }
                                )

                                SliderControl(
                                    title = "Barre Système (HOME/START)",
                                    value = systemBarScale,
                                    range = 0.60f..1.60f,
                                    displayValue = "${(systemBarScale * 100).toInt()}%",
                                    onValueChange = { systemBarScale = it }
                                )

                                SliderControl(
                                    title = "Opacité globale des boutons",
                                    value = opacity,
                                    range = 0.35f..1.0f,
                                    displayValue = "${(opacity * 100).toInt()}%",
                                    onValueChange = { opacity = it }
                                )
                            }
                        }
                    }

                    // SECTION 2: ANIMATION AU TOUCHER & IMMERSION VISUELLE
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF121622))
                            .border(1.dp, Color(0xFF232B3D), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Animation au Toucher & Réalisme Visuel",
                                color = Color(0xFFFFD54F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Effet Rebond Mécanique & Dépression 3D", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Animation dynamique spring avec enfoncement physique au clic", color = Color(0xFF78909C), fontSize = 11.sp)
                            }
                            Switch(
                                checked = touchAnimationEnabled,
                                onCheckedChange = { touchAnimationEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD54F))
                            )
                        }

                        SliderControl(
                            title = "Intensité du Halo Néon / Lueur Réactive au Toucher",
                            value = buttonGlowIntensity,
                            range = 0.0f..1.0f,
                            displayValue = "${(buttonGlowIntensity * 100).toInt()}%",
                            onValueChange = { buttonGlowIntensity = it }
                        )
                    }

                    // SECTION 3: VIBRATION & RETOUR HAPTIQUE IMMERSIF
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF121622))
                            .border(1.dp, Color(0xFF232B3D), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Vibration, null, tint = Color(0xFF00E676), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Vibration & Retour Haptique Immersif",
                                        color = Color(0xFF00E676),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Sensation physique réaliste pour chaque touche pressée",
                                        color = Color(0xFF78909C),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { onTestVibration(hapticProfile) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B382A)),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Tester Vibration", fontSize = 11.sp, color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = hapticFeedback,
                                    onCheckedChange = { hapticFeedback = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676))
                                )
                            }
                        }

                        Text("Profil de Sensation Tactile :", color = Color(0xFFB0BEC5), fontSize = 12.sp, fontWeight = FontWeight.Medium)

                        // 4 Profiles: SOFT, CRISP, HEAVY, DUAL_PULSE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HapticProfile.entries.forEach { prof ->
                                val selected = prof == hapticProfile
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFF1B3A2C) else Color(0xFF0E121B))
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) Color(0xFF00E676) else Color(0xFF222B3D),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            hapticProfile = prof
                                            onTestVibration(prof)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = prof.label,
                                            color = if (selected) Color.White else Color(0xFF90A4AE),
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = prof.description,
                                            color = if (selected) Color(0xFF69F0AE) else Color(0xFF546E7A),
                                            fontSize = 8.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 4: THÈME / SKIN CONSOLE
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF121622))
                            .border(1.dp, Color(0xFF232B3D), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Skin & Habillage Console PSP",
                            color = Color(0xFFB0BEC5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PspSkin.entries.forEach { item ->
                                val selected = item == skin
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFF1E283D) else Color(0xFF0F121B))
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) Color(item.accentColorHex) else Color(0xFF242C3E),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { skin = item }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(item.accentColorHex))
                                        )
                                        Text(
                                            text = item.title.replace("PSP ", ""),
                                            color = if (selected) Color.White else Color(0xFFB0BEC5),
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Buttons (Reset, Cancel, Save)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val reset = onResetDefaults()
                            skin = reset.skin
                            dpadScale = reset.dpadScale
                            actionScale = reset.actionScale
                            analogScale = reset.analogScale
                            triggerScale = reset.triggerScale
                            systemBarScale = reset.systemBarScale
                            opacity = reset.buttonsOpacity
                            deadzone = reset.stickDeadzone
                            sensitivity = reset.stickSensitivity
                            hapticFeedback = reset.hapticFeedback
                            hapticProfile = reset.hapticProfile
                            touchAnimationEnabled = reset.touchAnimationEnabled
                            buttonGlowIntensity = reset.buttonGlowIntensity
                            soundFeedback = reset.soundFeedback
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80))
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Réinitialiser Défaut", fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Annuler", fontSize = 12.sp, color = Color(0xFFB0BEC5))
                        }

                        Button(
                            onClick = {
                                onSaveConfig(buildUpdatedConfig())
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                        ) {
                            Text("Enregistrer & Appliquer", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderControl(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A0D14))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color(0xFFCFD8DC), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(displayValue, color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00B0FF),
                inactiveTrackColor = Color(0xFF1F2636)
            )
        )
    }
}
