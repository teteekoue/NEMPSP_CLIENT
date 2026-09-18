package com.example.nempsp.ui.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.nempsp.model.LayoutConfig
import com.example.nempsp.model.PspButton
import com.example.nempsp.network.ConnectionManager
import com.example.nempsp.ui.components.PspActionButtons
import com.example.nempsp.ui.components.PspAnalogStick
import com.example.nempsp.ui.components.PspConsolePanel
import com.example.nempsp.ui.components.PspDPad
import com.example.nempsp.ui.components.PspShoulderTrigger
import com.example.nempsp.ui.components.PspSystemBar
import com.example.nempsp.ui.dialogs.ConnectionSettingsSheet
import com.example.nempsp.ui.dialogs.LayoutEditorDialog
import com.example.nempsp.ui.dialogs.LiveLogsDialog
import com.example.nempsp.ui.dialogs.ServerCompanionDialog
import com.example.nempsp.util.HapticFeedbackHelper
import com.example.nempsp.util.SoundFeedbackHelper

/**
 * Écran manette (mode Client).
 *
 * Deux corrections d'ergonomie demandées :
 * 1. **Mise à l'échelle automatique** : la manette était bâtie sur des tailles fixes
 *    (gâchettes 44 dp + croix 160 dp + stick 130 dp ≈ 342 dp de haut) alors qu'un petit
 *    téléphone en paysage n'offre que ~320-360 dp. Résultat : tout débordait, les éléments se
 *    chevauchaient et les commandes des bords devenaient inatteignables. Un facteur d'échelle
 *    global est maintenant calculé à partir de la place réellement disponible (en tenant compte
 *    des réglages de taille de l'utilisateur), avec un plancher pour rester jouable.
 * 2. **Barre d'outils recentrée** : plus rien n'est collé aux bords de l'écran, tous les réglages
 *    (connexion, personnalisation, journal, guide, rôle, test) sont dans le cadre central NEMPSP.
 */
@Composable
fun PspControllerScreen(
    connectionManager: ConnectionManager,
    hapticHelper: HapticFeedbackHelper,
    soundHelper: SoundFeedbackHelper,
    layoutConfig: LayoutConfig,
    onSaveConfig: (LayoutConfig) -> Unit,
    onResetConfig: () -> LayoutConfig,
    onSwitchToModeSelection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentState by connectionManager.currentState.collectAsState()
    val currentMode by connectionManager.currentMode.collectAsState()
    val status by connectionManager.connectionStatus.collectAsState()
    val statusDetail by connectionManager.statusDetail.collectAsState()
    val latency by connectionManager.latencyMs.collectAsState()
    val packetsSent by connectionManager.totalPacketsSent.collectAsState()
    val packetsPerSec by connectionManager.packetsPerSecond.collectAsState()
    val testMode by connectionManager.testModeEnabled.collectAsState()

    var showConnectionSheet by remember { mutableStateOf(false) }
    var showLayoutEditor by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showServerGuide by remember { mutableStateOf(false) }

    // La fréquence d'émission configurée est enfin appliquée au gestionnaire de connexion
    LaunchedEffect(layoutConfig.autoSendRateMs) {
        connectionManager.updateSendRate(layoutConfig.autoSendRateMs)
    }

    fun triggerFeedback(isRelease: Boolean = false) {
        if (layoutConfig.hapticFeedback) {
            hapticHelper.vibrate(
                profile = layoutConfig.hapticProfile,
                enabled = true,
                isRelease = isRelease
            )
        }
        if (layoutConfig.soundFeedback && !isRelease) {
            soundHelper.playClick()
        }
    }

    val skin = layoutConfig.skin

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(skin.surfaceColorHex),
                        Color(skin.primaryColorHex)
                    )
                )
            )
            .testTag("psp_controller_root")
    ) {
        // ---- Calcul de l'échelle globale d'après la place réellement disponible ----
        val neededHeight = (44.dp * layoutConfig.triggerScale) +
            (160.dp * layoutConfig.dpadScale) +
            (130.dp * layoutConfig.analogScale) +
            12.dp
        val widestSide = maxOf(
            160.dp * layoutConfig.dpadScale,
            160.dp * layoutConfig.actionScale,
            130.dp * layoutConfig.analogScale,
            110.dp * layoutConfig.triggerScale
        )
        val neededWidth = widestSide * 2 + 190.dp

        val scaleByHeight = maxHeight / neededHeight
        val scaleByWidth = maxWidth / neededWidth
        val uiScale = minOf(1f, scaleByHeight, scaleByWidth).coerceIn(0.45f, 1f)

        val config = layoutConfig.copy(
            dpadScale = layoutConfig.dpadScale * uiScale,
            actionScale = layoutConfig.actionScale * uiScale,
            analogScale = layoutConfig.analogScale * uiScale,
            triggerScale = layoutConfig.triggerScale * uiScale,
            systemBarScale = layoutConfig.systemBarScale * uiScale
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ==========================================
            // CÔTÉ GAUCHE (gâchette L, croix, stick)
            // ==========================================
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PspShoulderTrigger(
                    button = PspButton.L,
                    isPressed = currentState.isPressed(PspButton.L),
                    opacity = config.buttonsOpacity,
                    isLeft = true,
                    scale = config.triggerScale,
                    glowIntensity = config.buttonGlowIntensity,
                    onPressed = { pressed ->
                        connectionManager.onButtonChanged(PspButton.L, pressed)
                        triggerFeedback(!pressed)
                    },
                    modifier = Modifier.align(Alignment.Start)
                )

                Box(
                    modifier = Modifier.offset {
                        IntOffset(config.dpadOffsetX.toInt(), config.dpadOffsetY.toInt())
                    }
                ) {
                    PspDPad(
                        config = config,
                        activeButtons = currentState.activeButtons,
                        onButtonChange = { btn, pressed ->
                            connectionManager.onButtonChanged(btn, pressed)
                        },
                        onFeedback = { isRelease -> triggerFeedback(isRelease) }
                    )
                }

                Box(
                    modifier = Modifier.offset {
                        IntOffset(config.analogOffsetX.toInt(), config.analogOffsetY.toInt())
                    }
                ) {
                    PspAnalogStick(
                        config = config,
                        onAnalogChange = { ax, ay ->
                            connectionManager.onAnalogChanged(ax, ay)
                        }
                    )
                }
            }

            // ==========================================
            // CONSOLE CENTRALE (écran + réglages + barre système)
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Écran de la console : statut, télémétrie et TOUS les réglages
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF07090D))
                        .border(1.5.dp, Color(0xFF1E2638), RoundedCornerShape(12.dp))
                        .testTag("nempsp_console_frame"),
                    contentAlignment = Alignment.Center
                ) {
                    PspConsolePanel(
                        currentMode = currentMode,
                        connectionStatus = status,
                        statusDetail = statusDetail,
                        gamepadState = currentState,
                        latencyMs = latency,
                        packetsPerSecond = packetsPerSec,
                        totalPackets = packetsSent,
                        testMode = testMode,
                        onToggleTestMode = { connectionManager.setTestMode(it) },
                        onOpenConnectionSheet = { showConnectionSheet = true },
                        onOpenCustomizer = { showLayoutEditor = true },
                        onOpenLogs = { showLogsDialog = true },
                        onOpenServerGuide = { showServerGuide = true },
                        onSwitchMode = onSwitchToModeSelection
                    )
                }

                // Barre système PSP (HOME, VOL, NOTE, SELECT, START)
                PspSystemBar(
                    activeButtons = currentState.activeButtons,
                    opacity = config.buttonsOpacity,
                    scale = config.systemBarScale,
                    onButtonChange = { btn, pressed ->
                        connectionManager.onButtonChanged(btn, pressed)
                    },
                    onFeedback = { isRelease -> triggerFeedback(isRelease) },
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // ==========================================
            // CÔTÉ DROIT (gâchette R, touches △ ○ ✕ □)
            // ==========================================
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PspShoulderTrigger(
                    button = PspButton.R,
                    isPressed = currentState.isPressed(PspButton.R),
                    opacity = config.buttonsOpacity,
                    isLeft = false,
                    scale = config.triggerScale,
                    glowIntensity = config.buttonGlowIntensity,
                    onPressed = { pressed ->
                        connectionManager.onButtonChanged(PspButton.R, pressed)
                        triggerFeedback(!pressed)
                    },
                    modifier = Modifier.align(Alignment.End)
                )

                Box(
                    modifier = Modifier.offset {
                        IntOffset(config.actionOffsetX.toInt(), config.actionOffsetY.toInt())
                    }
                ) {
                    PspActionButtons(
                        config = config,
                        activeButtons = currentState.activeButtons,
                        onButtonChange = { btn, pressed ->
                            connectionManager.onButtonChanged(btn, pressed)
                        },
                        onFeedback = { isRelease -> triggerFeedback(isRelease) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // ==========================================
        // Fenêtres modales
        // ==========================================
        if (showConnectionSheet) {
            ConnectionSettingsSheet(
                connectionManager = connectionManager,
                onDismiss = { showConnectionSheet = false }
            )
        }

        if (showLayoutEditor) {
            LayoutEditorDialog(
                currentConfig = layoutConfig,
                onSaveConfig = onSaveConfig,
                onResetDefaults = onResetConfig,
                onTestVibration = { prof -> hapticHelper.vibrate(prof, isRelease = false) },
                onDismiss = { showLayoutEditor = false }
            )
        }

        if (showLogsDialog) {
            LiveLogsDialog(
                currentState = currentState,
                onDismiss = { showLogsDialog = false }
            )
        }

        if (showServerGuide) {
            ServerCompanionDialog(
                onDismiss = { showServerGuide = false }
            )
        }
    }
}
