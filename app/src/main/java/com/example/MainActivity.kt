package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.nempsp.model.AppOperationMode
import com.example.nempsp.network.ConnectionManager
import com.example.nempsp.repository.AppModePreferences
import com.example.nempsp.repository.LayoutPreferences
import com.example.nempsp.server.NemPspServerEngine
import com.example.nempsp.ui.controller.PspControllerScreen
import com.example.nempsp.ui.mode.ModeSelectionDialog
import com.example.nempsp.ui.server.PspServerScreen
import com.example.nempsp.util.HapticFeedbackHelper
import com.example.nempsp.util.SoundFeedbackHelper
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var layoutPreferences: LayoutPreferences
    private lateinit var appModePreferences: AppModePreferences
    private lateinit var hapticHelper: HapticFeedbackHelper
    private lateinit var soundHelper: SoundFeedbackHelper
    private lateinit var connectionManager: ConnectionManager
    private lateinit var serverEngine: NemPspServerEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on during gaming
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide system bars for immersive landscape console gaming experience
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        layoutPreferences = LayoutPreferences(this)
        appModePreferences = AppModePreferences(this)
        hapticHelper = HapticFeedbackHelper(this)
        soundHelper = SoundFeedbackHelper()
        connectionManager = ConnectionManager(this, lifecycleScope)
        serverEngine = NemPspServerEngine(this, lifecycleScope)

        val savedMode = appModePreferences.getSavedMode()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                var config by remember { mutableStateOf(layoutPreferences.loadConfig()) }
                // Active mode or null if prompt should be shown on startup
                var activeMode by remember { mutableStateOf<AppOperationMode?>(savedMode) }
                var showModeDialog by remember { mutableStateOf(savedMode == null) }

                // Request runtime permissions for Bluetooth (Connect, Scan, Advertise) and Notifications
                val multiplePermissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { /* permissions handled gracefully */ }
                )

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()

                    // Notification permission for Android 13+ (API 33+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Bluetooth permissions for Android 12+ (API 31+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.BLUETOOTH_SCAN
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
                        }
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.BLUETOOTH_ADVERTISE
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                        }
                    } else {
                        // For older Android versions, location permission is required for Bluetooth scanning/discovery
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }

                    if (permissionsToRequest.isNotEmpty()) {
                        multiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                }

                LaunchedEffect(activeMode) {
                    if (activeMode == AppOperationMode.CHROMEBOOK_SERVER) {
                        // Start foreground server
                        serverEngine.startServer()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0C11)
                ) {
                    AnimatedContent(
                        targetState = activeMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "modeTransition"
                    ) { mode ->
                        when (mode) {
                            AppOperationMode.CHROMEBOOK_SERVER -> {
                                PspServerScreen(
                                    serverEngine = serverEngine,
                                    onSwitchToController = {
                                        showModeDialog = true
                                    }
                                )
                            }
                            else -> {
                                PspControllerScreen(
                                    connectionManager = connectionManager,
                                    hapticHelper = hapticHelper,
                                    soundHelper = soundHelper,
                                    layoutConfig = config,
                                    onSaveConfig = { updated ->
                                        config = updated
                                        layoutPreferences.saveConfig(updated)
                                    },
                                    onResetConfig = {
                                        val reset = layoutPreferences.resetToDefaults()
                                        config = reset
                                        reset
                                    },
                                    onSwitchToModeSelection = {
                                        showModeDialog = true
                                    }
                                )
                            }
                        }
                    }

                    // Mode Selection Prompt on startup or on explicit switch
                    if (showModeDialog) {
                        ModeSelectionDialog(
                            currentMode = activeMode ?: AppOperationMode.CONTROLLER_CLIENT,
                            rememberChoice = appModePreferences.isChoiceRemembered(),
                            onSelectMode = { selected, remember ->
                                appModePreferences.saveMode(selected, remember)
                                activeMode = selected
                                showModeDialog = false

                                // Automatically start server when switching to server mode
                                if (selected == AppOperationMode.CHROMEBOOK_SERVER) {
                                    serverEngine.startServer()
                                } else {
                                    serverEngine.stopServer()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundHelper.release()
        // Ferme les sockets et le canal d'émission si l'activité se termine vraiment.
        // (Le récepteur, lui, doit survivre : il tourne dans son propre service de premier plan.)
        if (isFinishing) {
            connectionManager.release()
        }
        // Note: Le service de premier plan du serveur continue de tourner en arrière-plan
        // tant que l'utilisateur n'a pas explicitement cliqué sur "Arrêter" ou sur la notification.
    }
}
