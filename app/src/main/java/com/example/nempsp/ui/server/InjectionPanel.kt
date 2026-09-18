package com.example.nempsp.ui.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nempsp.injection.PpssppTouchMap
import com.example.nempsp.model.AnalogInjectionMode
import com.example.nempsp.model.PspButton
import com.example.nempsp.repository.InjectionPreferences
import com.example.nempsp.server.NemPspServerEngine

/**
 * Carte « Injection dans PPSSPP » de l'écran Récepteur.
 *
 * Elle rend visible ce qui était invisible : l'état réel du service d'accessibilité, la présence
 * de PPSSPP au premier plan, le nombre de gestes effectivement injectés, et elle donne accès à
 * l'activation, au mode analogique, à la calibration et à un appui de test.
 */
@Composable
fun InjectionPanel(
    serverEngine: NemPspServerEngine,
    modifier: Modifier = Modifier
) {
    val prefs = remember { serverEngine.injectionPreferences() }
    val serviceRunning by serverEngine.injectionRunning.collectAsState()
    val gameFocused by serverEngine.injectionGameFocused.collectAsState()
    val message by serverEngine.injectionMessage.collectAsState()
    val gestures by serverEngine.injectedGestures.collectAsState()

    var enabled by remember { mutableStateOf(prefs.enabled) }
    var analogMode by remember { mutableStateOf(prefs.analogMode) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var showCalibration by remember { mutableStateOf(false) }
    var calibratedCount by remember { mutableIntStateOf(prefs.calibratedCount()) }

    val stateColor = when {
        serviceRunning && gameFocused -> Color(0xFF69F0AE)
        serviceRunning -> Color(0xFFFFD54F)
        else -> Color(0xFFFF8A80)
    }
    val stateLabel = when {
        serviceRunning && gameFocused -> "ACTIVE"
        serviceRunning -> "PRÊTE"
        else -> "INACTIVE"
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF131722))
            .border(1.dp, Color(0xFF2A2440), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TouchApp, null, tint = Color(0xFFB388FF), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Injection dans PPSSPP",
                    color = Color(0xFFB388FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(stateColor.copy(alpha = 0.15f))
                    .border(1.dp, stateColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(stateLabel, color = stateColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = "Les touches reçues deviennent de vrais appuis tactiles sur les commandes de PPSSPP : " +
                "aucun réglage à faire dans l'émulateur.",
            color = Color(0xFFB0BEC5),
            fontSize = 11.sp,
            lineHeight = 14.sp
        )

        // ---------------- Service d'accessibilité ----------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (serviceRunning) "Service d'accessibilité connecté" else "Service d'accessibilité désactivé",
                    color = if (serviceRunning) Color(0xFFE8F5E9) else Color(0xFFFFCDD2),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        !serviceRunning -> "À activer une seule fois : Réglages → Accessibilité → NEMPSP — Manette pour PPSSPP"
                        gameFocused -> "PPSSPP au premier plan : les appuis sont injectés"
                        else -> "PPSSPP en arrière-plan : injection en pause, tous les doigts sont relâchés"
                    },
                    color = Color(0xFF90A4AE),
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
            if (!serviceRunning) {
                Button(
                    onClick = { serverEngine.openAccessibilitySettings() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_open_accessibility")
                ) {
                    Text("Activer", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ---------------- Interrupteur ----------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Injecter les touches", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Coupez pour utiliser NEMPSP comme simple moniteur",
                    color = Color(0xFF78909C),
                    fontSize = 10.sp
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    prefs.enabled = it
                },
                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF00E676)),
                modifier = Modifier.testTag("switch_injection_enabled")
            )
        }

        // ---------------- Mode analogique ----------------
        Text("Stick analogique", color = Color(0xFFCFD8DC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnalogInjectionMode.entries.forEach { mode ->
                val selected = analogMode == mode
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Color(0xFF1B2A44) else Color(0xFF0F131C))
                        .border(
                            1.dp,
                            if (selected) Color(0xFF00E5FF) else Color(0xFF253046),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            analogMode = mode
                            prefs.analogMode = mode
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = mode.label,
                        color = if (selected) Color(0xFF80D8FF) else Color(0xFFB0BEC5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            text = analogMode.description,
            color = Color(0xFF78909C),
            fontSize = 10.sp,
            lineHeight = 13.sp
        )

        // ---------------- Calibration ----------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Position des boutons", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (calibratedCount == 0) {
                        "Disposition tactile d'usine de PPSSPP"
                    } else {
                        "$calibratedCount position(s) calibrée(s)"
                    },
                    color = Color(0xFF78909C),
                    fontSize = 10.sp
                )
            }
            OutlinedButton(
                onClick = { showCalibration = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("btn_open_calibration")
            ) {
                Text("Calibrer", fontSize = 11.sp)
            }
        }

        // ---------------- Test ----------------
        Button(
            onClick = { testResult = serverEngine.runInjectionSelfTest() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .testTag("btn_injection_selftest")
        ) {
            Text("Tester : un appui sur ✕ dans PPSSPP", color = Color(0xFF80D8FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        testResult?.let {
            Text(it, color = Color(0xFFFFE082), fontSize = 10.sp, lineHeight = 13.sp)
        }

        Text(
            text = "$gestures geste(s) injecté(s) · $message",
            color = Color(0xFF607D8B),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 2
        )
    }

    if (showCalibration) {
        InjectionCalibrationDialog(
            prefs = prefs,
            onDismiss = {
                showCalibration = false
                calibratedCount = prefs.calibratedCount()
            }
        )
    }
}

/**
 * Calibration des cibles tactiles sur un schéma de la fenêtre de jeu.
 *
 * Les positions sont enregistrées en **proportions de la fenêtre PPSSPP**, donc elles restent
 * valables quand la fenêtre est déplacée ou redimensionnée (cas courant sur Chromebook). Sur un
 * Chromebook, gardez la fenêtre de PPSSPP à côté de celle de NEMPSP et reproduisez sa disposition.
 */
@Composable
fun InjectionCalibrationDialog(
    prefs: InjectionPreferences,
    onDismiss: () -> Unit
) {
    // « label to bouton » ; bouton == null désigne le centre du stick analogique tactile.
    val targets: List<Pair<String, PspButton?>> = remember {
        PpssppTouchMap.CALIBRABLE_BUTTONS.map { "${'$'}{it.symbol} ${'$'}{it.label}" to it } +
            ("◎ STICK" to null)
    }
    var currentIndex by remember { mutableIntStateOf(0) }
    // Les préférences ne sont pas observables : ce compteur force la relecture après chaque appui.
    var revision by remember { mutableIntStateOf(0) }

    val safeIndex = currentIndex.coerceIn(0, targets.lastIndex)
    val (targetLabel, targetButton) = targets[safeIndex]

    fun record(x: Float, y: Float) {
        if (targetButton == null) {
            prefs.calibrateAnalogCenter(x, y)
        } else {
            prefs.calibrate(targetButton, x, y)
        }
        revision++
        if (safeIndex < targets.lastIndex) currentIndex = safeIndex + 1
    }

    fun resetCurrent() {
        if (targetButton == null) {
            prefs.calibrateAnalogCenter(
                PpssppTouchMap.DEFAULT_ANALOG_CENTER.x,
                PpssppTouchMap.DEFAULT_ANALOG_CENTER.y
            )
        } else {
            prefs.clearCalibration(targetButton)
        }
        revision++
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .testTag("dialog_injection_calibration"),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F1219),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF252D3F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calibration — $targetLabel",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, "Fermer", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Text(
                    text = "Touchez le schéma à l'endroit où PPSSPP affiche « $targetLabel ». " +
                        "Les positions sont mémorisées en proportions de la fenêtre de jeu.",
                    color = Color(0xFFB0BEC5),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Liste des cibles
                    Column(
                        modifier = Modifier
                            .width(116.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // `key(revision)` force la relecture des préférences après chaque appui :
                        // InjectionPreferences n'est pas observable.
                        key(revision) {
                        targets.forEachIndexed { index, (label, button) ->
                            val selected = index == safeIndex
                            val done = if (button == null) {
                                prefs.analogCenter() != PpssppTouchMap.DEFAULT_ANALOG_CENTER
                            } else {
                                prefs.isCalibrated(button)
                            }
                            val marker = if (done) "●" else "○"
                            Text(
                                text = "$marker $label",
                                color = when {
                                    selected -> Color(0xFF00E5FF)
                                    done -> Color(0xFF69F0AE)
                                    else -> Color(0xFF90A4AE)
                                },
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) Color(0xFF16283A) else Color.Transparent)
                                    .clickable { currentIndex = index }
                                    .padding(horizontal = 6.dp, vertical = 5.dp)
                            )
                        }
                        }
                    }

                    // Schéma de la fenêtre de jeu (16:9)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF05070C))
                                .border(1.dp, Color(0xFF2E3A52), RoundedCornerShape(10.dp))
                                .pointerInput(targetLabel) {
                                    detectTapGestures { offset ->
                                        record(
                                            (offset.x / size.width.toFloat()).coerceIn(0f, 1f),
                                            (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                                        )
                                    }
                                },
                            contentAlignment = Alignment.TopStart
                        ) {
                            val areaWidth = maxWidth
                            val areaHeight = maxHeight
                            // Repères discrets : bords de la fenêtre de jeu
                            Text(
                                text = "fenêtre PPSSPP",
                                color = Color(0xFF37474F),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp)
                            )

                            key(revision) {
                            PpssppTouchMap.CALIBRABLE_BUTTONS.forEach { button ->
                                val point = prefs.positionOf(button) ?: return@forEach
                                val isTarget = button == targetButton
                                val calibrated = prefs.isCalibrated(button)
                                Box(
                                    modifier = Modifier
                                        .offset(x = areaWidth * point.x - 13.dp, y = areaHeight * point.y - 13.dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isTarget -> Color(0x4400E5FF)
                                                calibrated -> Color(0x3369F0AE)
                                                else -> Color(0x22FFFFFF)
                                            }
                                        )
                                        .border(
                                            1.5.dp,
                                            when {
                                                isTarget -> Color(0xFF00E5FF)
                                                calibrated -> Color(0xFF69F0AE)
                                                else -> Color(0x55FFFFFF)
                                            },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = button.symbol,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            }

                            // Centre du stick analogique tactile
                            run {
                                val center = prefs.analogCenter()
                                val radius = prefs.analogRadiusFraction()
                                val isTarget = targetButton == null
                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = areaWidth * center.x - areaWidth * radius,
                                            y = areaHeight * center.y - areaWidth * radius
                                        )
                                        .size(areaWidth * radius * 2f)
                                        .clip(CircleShape)
                                        .background(if (isTarget) Color(0x3300E5FF) else Color(0x14B388FF))
                                        .border(
                                            1.5.dp,
                                            if (isTarget) Color(0xFF00E5FF) else Color(0x66B388FF),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("STICK", color = Color(0xFFB388FF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { resetCurrent() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFAB91)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Réinitialiser", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            prefs.clearAllCalibration()
                            revision++
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Tout réinitialiser", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Text("Terminé", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
