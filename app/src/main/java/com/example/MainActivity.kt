package com.example

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.nempsp.network.ConnectionManager
import com.example.nempsp.repository.LayoutPreferences
import com.example.nempsp.ui.controller.PspControllerScreen
import com.example.nempsp.util.HapticFeedbackHelper
import com.example.nempsp.util.SoundFeedbackHelper
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var layoutPreferences: LayoutPreferences
    private lateinit var hapticHelper: HapticFeedbackHelper
    private lateinit var soundHelper: SoundFeedbackHelper
    private lateinit var connectionManager: ConnectionManager

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
        hapticHelper = HapticFeedbackHelper(this)
        soundHelper = SoundFeedbackHelper()
        connectionManager = ConnectionManager(this, lifecycleScope)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                var config by remember { mutableStateOf(layoutPreferences.loadConfig()) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0C11)
                ) {
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
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundHelper.release()
    }
}

