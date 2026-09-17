package com.example.nempsp.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.nempsp.model.AppOperationMode

class AppModePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nempsp_app_mode_prefs", Context.MODE_PRIVATE)

    fun getSavedMode(): AppOperationMode? {
        val remember = prefs.getBoolean("remember_mode_choice", false)
        if (!remember) return null
        val modeName = prefs.getString("saved_operation_mode", null) ?: return null
        return try {
            AppOperationMode.valueOf(modeName)
        } catch (_: Exception) {
            null
        }
    }

    fun saveMode(mode: AppOperationMode, rememberChoice: Boolean) {
        prefs.edit()
            .putBoolean("remember_mode_choice", rememberChoice)
            .putString("saved_operation_mode", mode.name)
            .apply()
    }

    fun isChoiceRemembered(): Boolean {
        return prefs.getBoolean("remember_mode_choice", false)
    }

    fun clearRememberedChoice() {
        prefs.edit().putBoolean("remember_mode_choice", false).apply()
    }
}
