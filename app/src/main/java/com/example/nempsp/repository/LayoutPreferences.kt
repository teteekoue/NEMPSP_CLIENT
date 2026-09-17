package com.example.nempsp.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.nempsp.model.LayoutConfig
import com.example.nempsp.model.PspSkin

class LayoutPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nempsp_layout_prefs", Context.MODE_PRIVATE)

    fun loadConfig(): LayoutConfig {
        val skinName = prefs.getString("skin", PspSkin.PIANO_BLACK.name) ?: PspSkin.PIANO_BLACK.name
        val skin = try {
            PspSkin.valueOf(skinName)
        } catch (_: Exception) {
            PspSkin.PIANO_BLACK
        }

        val hapticProfileName = prefs.getString("hapticProfile", com.example.nempsp.model.HapticProfile.CRISP.name)
        val hapticProfile = try {
            com.example.nempsp.model.HapticProfile.valueOf(hapticProfileName ?: com.example.nempsp.model.HapticProfile.CRISP.name)
        } catch (_: Exception) {
            com.example.nempsp.model.HapticProfile.CRISP
        }

        return LayoutConfig(
            skin = skin,
            dpadScale = prefs.getFloat("dpadScale", 1.0f),
            dpadOffsetX = prefs.getFloat("dpadOffsetX", 0f),
            dpadOffsetY = prefs.getFloat("dpadOffsetY", 0f),
            actionScale = prefs.getFloat("actionScale", 1.0f),
            actionOffsetX = prefs.getFloat("actionOffsetX", 0f),
            actionOffsetY = prefs.getFloat("actionOffsetY", 0f),
            analogScale = prefs.getFloat("analogScale", 1.0f),
            analogOffsetX = prefs.getFloat("analogOffsetX", 0f),
            analogOffsetY = prefs.getFloat("analogOffsetY", 0f),
            triggerScale = prefs.getFloat("triggerScale", 1.0f),
            triggerOffsetX = prefs.getFloat("triggerOffsetX", 0f),
            triggerOffsetY = prefs.getFloat("triggerOffsetY", 0f),
            systemBarScale = prefs.getFloat("systemBarScale", 1.0f),
            stickDeadzone = prefs.getFloat("stickDeadzone", 0.08f),
            stickSensitivity = prefs.getFloat("stickSensitivity", 1.0f),
            hapticFeedback = prefs.getBoolean("hapticFeedback", true),
            hapticProfile = hapticProfile,
            soundFeedback = prefs.getBoolean("soundFeedback", false),
            buttonsOpacity = prefs.getFloat("buttonsOpacity", 0.95f),
            touchAnimationEnabled = prefs.getBoolean("touchAnimationEnabled", true),
            buttonGlowIntensity = prefs.getFloat("buttonGlowIntensity", 0.85f),
            autoSendRateMs = prefs.getLong("autoSendRateMs", 16L)
        )
    }

    fun saveConfig(config: LayoutConfig) {
        prefs.edit()
            .putString("skin", config.skin.name)
            .putFloat("dpadScale", config.dpadScale)
            .putFloat("dpadOffsetX", config.dpadOffsetX)
            .putFloat("dpadOffsetY", config.dpadOffsetY)
            .putFloat("actionScale", config.actionScale)
            .putFloat("actionOffsetX", config.actionOffsetX)
            .putFloat("actionOffsetY", config.actionOffsetY)
            .putFloat("analogScale", config.analogScale)
            .putFloat("analogOffsetX", config.analogOffsetX)
            .putFloat("analogOffsetY", config.analogOffsetY)
            .putFloat("triggerScale", config.triggerScale)
            .putFloat("triggerOffsetX", config.triggerOffsetX)
            .putFloat("triggerOffsetY", config.triggerOffsetY)
            .putFloat("systemBarScale", config.systemBarScale)
            .putFloat("stickDeadzone", config.stickDeadzone)
            .putFloat("stickSensitivity", config.stickSensitivity)
            .putBoolean("hapticFeedback", config.hapticFeedback)
            .putString("hapticProfile", config.hapticProfile.name)
            .putBoolean("soundFeedback", config.soundFeedback)
            .putFloat("buttonsOpacity", config.buttonsOpacity)
            .putBoolean("touchAnimationEnabled", config.touchAnimationEnabled)
            .putFloat("buttonGlowIntensity", config.buttonGlowIntensity)
            .putLong("autoSendRateMs", config.autoSendRateMs)
            .apply()
    }

    fun resetToDefaults(): LayoutConfig {
        val defaultConfig = LayoutConfig()
        saveConfig(defaultConfig)
        return defaultConfig
    }
}
