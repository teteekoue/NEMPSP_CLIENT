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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nempsp.model.LayoutConfig
import com.example.nempsp.model.PspButton
import com.example.nempsp.network.ConnectionManager
import com.example.nempsp.ui.components.NemPspLogoBadge
import com.example.nempsp.ui.components.PspActionButtons
import com.example.nempsp.ui.components.PspAnalogStick
import com.example.nempsp.ui.components.PspDPad
import com.example.nempsp.ui.components.PspShoulderTrigger
import com.example.nempsp.ui.components.PspStatusBar
import com.example.nempsp.ui.components.PspSystemBar
import com.example.nempsp.ui.dialogs.ConnectionSettingsSheet
import com.example.nempsp.ui.dialogs.LayoutEditorDialog
import com.example.nempsp.ui.dialogs.LiveLogsDialog
import com.example.nempsp.ui.dialogs.ServerCompanionDialog
import com.example.nempsp.util.HapticFeedbackHelper
import com.example.nempsp.util.SoundFeedbackHelper

@Composable
fun PspControllerScreen(
    connectionManager: ConnectionManager,
    hapticHelper: HapticFeedbackHelper,
    soundHelper: SoundFeedbackHelper,
    layoutConfig: LayoutConfig,
    onSaveConfig: (LayoutConfig) -> Unit,
    onResetConfig: () -> LayoutConfig,
    modifier: Modifier = Modifier
) {
    val currentState by connectionManager.currentState.collectAsState()
    val currentMode by connectionManager.currentMode.collectAsState()
    val status by connectionManager.connectionStatus.collectAsState()
    val latency by connectionManager.latencyMs.collectAsState()
    val packetsSent by connectionManager.totalPacketsSent.collectAsState()
    val packetsPerSec by connectionManager.packetsPerSecond.collectAsState()
    val testMode by connectionManager.testModeEnabled.collectAsState()

    var showConnectionSheet by remember { mutableStateOf(false) }
    var showLayoutEditor by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showServerGuide by remember { mutableStateOf(false) }

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
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Status Bar
            PspStatusBar(
                currentMode = currentMode,
                connectionStatus = status,
                latencyMs = latency,
                packetsPerSecond = packetsPerSec,
                totalPackets = packetsSent,
                testMode = testMode,
                onToggleTestMode = { connectionManager.setTestMode(it) },
                onOpenConnectionSheet = { showConnectionSheet = true },
                onOpenCustomizer = { showLayoutEditor = true },
                onOpenLogs = { showLogsDialog = true },
                onOpenServerGuide = { showServerGuide = true }
            )

            // Main Play Surface
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ==========================================
                // LEFT SIDE (L Trigger, D-Pad, Analog Stick)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(180.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // L Shoulder Trigger
                    PspShoulderTrigger(
                        button = PspButton.L,
                        isPressed = currentState.isPressed(PspButton.L),
                        opacity = layoutConfig.buttonsOpacity,
                        isLeft = true,
                        scale = layoutConfig.triggerScale,
                        glowIntensity = layoutConfig.buttonGlowIntensity,
                        onPressed = { pressed ->
                            connectionManager.onButtonChanged(PspButton.L, pressed)
                            triggerFeedback(!pressed)
                        },
                        modifier = Modifier.align(Alignment.Start)
                    )

                    // PSP D-Pad
                    Box(
                        modifier = Modifier.offset {
                            IntOffset(layoutConfig.dpadOffsetX.toInt(), layoutConfig.dpadOffsetY.toInt())
                        }
                    ) {
                        PspDPad(
                            config = layoutConfig,
                            activeButtons = currentState.activeButtons,
                            onButtonChange = { btn, pressed ->
                                connectionManager.onButtonChanged(btn, pressed)
                            },
                            onFeedback = { isRelease -> triggerFeedback(isRelease) }
                        )
                    }

                    // PSP Analog Stick
                    Box(
                        modifier = Modifier.offset {
                            IntOffset(layoutConfig.analogOffsetX.toInt(), layoutConfig.analogOffsetY.toInt())
                        }
                    ) {
                        PspAnalogStick(
                            config = layoutConfig,
                            onAnalogChange = { ax, ay ->
                                connectionManager.onAnalogChanged(ax, ay)
                            }
                        )
                    }
                }

                // ==========================================
                // CENTER CONSOLE (Branding screen & System bar)
                // ==========================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(2.dp))

                    // Decorative PSP Screen Display / Monitor Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .weight(1f)
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF07090D))
                            .border(1.5.dp, Color(0xFF1E2638), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Official NEMPSP Logo Badge
                            NemPspLogoBadge(compact = false)

                            // Telemetry snippet on center screen
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10141D))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "STICK: (${String.format(java.util.Locale.US, "%+.2f", currentState.analogX)}, ${String.format(java.util.Locale.US, "%+.2f", currentState.analogY)})",
                                    color = Color(0xFF00E5FF),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )

                                Text(
                                    text = "BTNS: ${if (currentState.activeButtons.isEmpty()) "---" else currentState.activeButtons.joinToString("") { it.symbol }}",
                                    color = Color(0xFF81C784),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Bottom PSP System Bar (HOME, SELECT, START, etc.)
                    PspSystemBar(
                        activeButtons = currentState.activeButtons,
                        opacity = layoutConfig.buttonsOpacity,
                        scale = layoutConfig.systemBarScale,
                        onButtonChange = { btn, pressed ->
                            connectionManager.onButtonChanged(btn, pressed)
                        },
                        onFeedback = { isRelease -> triggerFeedback(isRelease) },
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // ==========================================
                // RIGHT SIDE (R Trigger, Action Buttons △ ○ ✕ □)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(180.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // R Shoulder Trigger
                    PspShoulderTrigger(
                        button = PspButton.R,
                        isPressed = currentState.isPressed(PspButton.R),
                        opacity = layoutConfig.buttonsOpacity,
                        isLeft = false,
                        scale = layoutConfig.triggerScale,
                        glowIntensity = layoutConfig.buttonGlowIntensity,
                        onPressed = { pressed ->
                            connectionManager.onButtonChanged(PspButton.R, pressed)
                            triggerFeedback(!pressed)
                        },
                        modifier = Modifier.align(Alignment.End)
                    )

                    // PSP Action Buttons (Triangle, Circle, Cross, Square)
                    Box(
                        modifier = Modifier.offset {
                            IntOffset(layoutConfig.actionOffsetX.toInt(), layoutConfig.actionOffsetY.toInt())
                        }
                    ) {
                        PspActionButtons(
                            config = layoutConfig,
                            activeButtons = currentState.activeButtons,
                            onButtonChange = { btn, pressed ->
                                connectionManager.onButtonChanged(btn, pressed)
                            },
                            onFeedback = { isRelease -> triggerFeedback(isRelease) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // ==========================================
        // Modal Dialogs
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
