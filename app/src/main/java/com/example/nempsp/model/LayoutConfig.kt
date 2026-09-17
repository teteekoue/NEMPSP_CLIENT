package com.example.nempsp.model

enum class PspSkin(
    val title: String,
    val primaryColorHex: Long,
    val surfaceColorHex: Long,
    val accentColorHex: Long,
    val textColorHex: Long
) {
    PIANO_BLACK("PSP Piano Black", 0xFF0D0F14, 0xFF171A21, 0xFF3DDC84, 0xFFE0E0E0),
    PEARL_WHITE("PSP Pearl White", 0xFFE3E5EB, 0xFFD0D4DE, 0xFF2979FF, 0xFF12141A),
    MYSTIC_SILVER("PSP Mystic Silver", 0xFF2A2E39, 0xFF3D4352, 0xFF00E5FF, 0xFFF0F0F0),
    RADIANT_RED("PSP Radiant Red", 0xFF350A10, 0xFF54121B, 0xFFFF3366, 0xFFFFE5EC),
    CYBER_NEON("Cyberpunk Neon", 0xFF080712, 0xFF130E26, 0xFF00F0FF, 0xFF00FFCC)
}

enum class HapticProfile(val label: String, val description: String) {
    SOFT("Doux & Subtil", "Micro-impulsion discrète (8ms)"),
    CRISP("Réaliste / Switch", "Sensation de clic tactile précis"),
    HEAVY("Impact Mécanique", "Pression affirmée avec retour physique lourd"),
    DUAL_PULSE("Double Détente", "Vibration à l'appui et au relâchement")
}

data class LayoutConfig(
    val skin: PspSkin = PspSkin.PIANO_BLACK,
    val dpadScale: Float = 1.0f,
    val dpadOffsetX: Float = 0f,
    val dpadOffsetY: Float = 0f,
    val actionScale: Float = 1.0f,
    val actionOffsetX: Float = 0f,
    val actionOffsetY: Float = 0f,
    val analogScale: Float = 1.0f,
    val analogOffsetX: Float = 0f,
    val analogOffsetY: Float = 0f,
    val triggerScale: Float = 1.0f,
    val triggerOffsetX: Float = 0f,
    val triggerOffsetY: Float = 0f,
    val systemBarScale: Float = 1.0f,
    val stickDeadzone: Float = 0.08f,
    val stickSensitivity: Float = 1.0f,
    val hapticFeedback: Boolean = true,
    val hapticProfile: HapticProfile = HapticProfile.CRISP,
    val soundFeedback: Boolean = false,
    val buttonsOpacity: Float = 0.95f,
    val touchAnimationEnabled: Boolean = true,
    val buttonGlowIntensity: Float = 0.85f,
    val autoSendRateMs: Long = 16L // ~60 Hz packet transmission
)
