package com.example.nempsp.repository

import android.content.Context
import android.graphics.PointF
import com.example.nempsp.injection.PpssppTouchMap
import com.example.nempsp.model.AnalogInjectionMode
import com.example.nempsp.model.PspButton

/**
 * Réglages de l'injection dans PPSSPP, persistés entre les sessions.
 *
 * Deux niveaux :
 * - des **positions par défaut** issues de [PpssppTouchMap] (disposition tactile d'usine de
 *   PPSSPP) pour que ça fonctionne immédiatement, sans calibration ;
 * - une **calibration** optionnelle par bouton, qui remplace la valeur par défaut. Elle est
 *   stockée en coordonnées normalisées (fractions de la fenêtre de jeu), donc elle reste valable
 *   si la fenêtre PPSSPP est redimensionnée ou déplacée.
 */
class InjectionPreferences(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("nempsp_injection", Context.MODE_PRIVATE)

    /** Interrupteur général de l'injection tactile. */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    var analogMode: AnalogInjectionMode
        get() {
            val name = prefs.getString(KEY_ANALOG_MODE, AnalogInjectionMode.DPAD.name)
            return AnalogInjectionMode.entries.firstOrNull { it.name == name } ?: AnalogInjectionMode.DPAD
        }
        set(value) {
            prefs.edit().putString(KEY_ANALOG_MODE, value.name).apply()
        }

    /**
     * Période de renouvellement des appuis, en ms. Chaque geste posé est prolongé toutes les
     * [holdMs] : trop long et un relâchement met du temps à être pris en compte, trop court et
     * l'on sature le canal d'injection.
     */
    var holdMs: Int
        get() = prefs.getInt(KEY_HOLD_MS, DEFAULT_HOLD_MS)
        set(value) {
            prefs.edit().putInt(KEY_HOLD_MS, value).apply()
        }

    /** Position effective d'un bouton : calibration si elle existe, sinon valeur par défaut. */
    fun positionOf(button: PspButton): PointF? = calibrated(button) ?: PpssppTouchMap.DEFAULT_POSITIONS[button]

    fun calibrated(button: PspButton): PointF? {
        val key = KEY_CALIB_X + button.name
        if (!prefs.contains(key)) return null
        return PointF(prefs.getFloat(key, 0f), prefs.getFloat(KEY_CALIB_Y + button.name, 0f))
    }

    fun isCalibrated(button: PspButton): Boolean = prefs.contains(KEY_CALIB_X + button.name)

    fun calibrate(button: PspButton, x: Float, y: Float) {
        prefs.edit()
            .putFloat(KEY_CALIB_X + button.name, x.coerceIn(0f, 1f))
            .putFloat(KEY_CALIB_Y + button.name, y.coerceIn(0f, 1f))
            .apply()
    }

    fun clearCalibration(button: PspButton) {
        prefs.edit()
            .remove(KEY_CALIB_X + button.name)
            .remove(KEY_CALIB_Y + button.name)
            .apply()
    }

    fun clearAllCalibration() {
        val editor = prefs.edit()
        PspButton.entries.forEach { button ->
            editor.remove(KEY_CALIB_X + button.name).remove(KEY_CALIB_Y + button.name)
        }
        editor.remove(KEY_ANALOG_X).remove(KEY_ANALOG_Y)
        editor.apply()
    }

    /** Bouton activé individuellement. HOME et NOTE sont exclus par défaut (pas de cible fiable). */
    fun isButtonEnabled(button: PspButton): Boolean =
        prefs.getBoolean(KEY_BTN_ENABLED + button.name, PpssppTouchMap.isTouchable(button))

    fun setButtonEnabled(button: PspButton, value: Boolean) {
        prefs.edit().putBoolean(KEY_BTN_ENABLED + button.name, value).apply()
    }

    fun analogCenter(): PointF = PointF(
        prefs.getFloat(KEY_ANALOG_X, PpssppTouchMap.DEFAULT_ANALOG_CENTER.x),
        prefs.getFloat(KEY_ANALOG_Y, PpssppTouchMap.DEFAULT_ANALOG_CENTER.y)
    )

    fun calibrateAnalogCenter(x: Float, y: Float) {
        prefs.edit()
            .putFloat(KEY_ANALOG_X, x.coerceIn(0f, 1f))
            .putFloat(KEY_ANALOG_Y, y.coerceIn(0f, 1f))
            .apply()
    }

    fun analogRadiusFraction(): Float =
        prefs.getFloat(KEY_ANALOG_RADIUS, PpssppTouchMap.DEFAULT_ANALOG_RADIUS_FRACTION)
            .coerceIn(0.01f, 0.25f)

    fun setAnalogRadiusFraction(fraction: Float) {
        prefs.edit().putFloat(KEY_ANALOG_RADIUS, fraction.coerceIn(0.01f, 0.25f)).apply()
    }

    /** Nombre de boutons calibrés, pour l'affichage d'état. */
    fun calibratedCount(): Int = PspButton.entries.count { isCalibrated(it) }

    companion object {
        const val DEFAULT_HOLD_MS = 90

        private const val KEY_ENABLED = "injection_enabled"
        private const val KEY_ANALOG_MODE = "analog_mode"
        private const val KEY_HOLD_MS = "hold_ms"
        private const val KEY_CALIB_X = "calib_x_"
        private const val KEY_CALIB_Y = "calib_y_"
        private const val KEY_BTN_ENABLED = "btn_enabled_"
        private const val KEY_ANALOG_X = "analog_x"
        private const val KEY_ANALOG_Y = "analog_y"
        private const val KEY_ANALOG_RADIUS = "analog_radius"
    }
}
