package com.example.nempsp.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.nempsp.model.HapticProfile

class HapticFeedbackHelper(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun vibrate(profile: HapticProfile = HapticProfile.CRISP, enabled: Boolean = true, isRelease: Boolean = false) {
        if (!enabled || vibrator == null || !vibrator.hasVibrator()) return

        try {
            if (isRelease) {
                // Subtle release feedback only if DUAL_PULSE profile is chosen
                if (profile == HapticProfile.DUAL_PULSE) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(8, 70))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(8)
                    }
                }
                return
            }

            // Button Press feedback
            when (profile) {
                HapticProfile.SOFT -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(10, 90))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(10)
                    }
                }
                HapticProfile.CRISP -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(18, 180))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(18)
                    }
                }
                HapticProfile.HEAVY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(38, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(38)
                    }
                }
                HapticProfile.DUAL_PULSE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(16, 200))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(16)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore if device permission or hardware vibration is muted
        }
    }

    fun vibrateClick(enabled: Boolean = true) {
        vibrate(HapticProfile.CRISP, enabled, isRelease = false)
    }

    fun vibrateHeavy(enabled: Boolean = true) {
        vibrate(HapticProfile.HEAVY, enabled, isRelease = false)
    }
}
