package com.example.nempsp.model

enum class PspButton(
    val mask: Int,
    val label: String,
    val symbol: String,
    val description: String
) {
    // D-Pad
    UP(1 shl 0, "UP", "▲", "D-Pad Up"),
    RIGHT(1 shl 1, "RIGHT", "▶", "D-Pad Right"),
    DOWN(1 shl 2, "DOWN", "▼", "D-Pad Down"),
    LEFT(1 shl 3, "LEFT", "◀", "D-Pad Left"),

    // PSP Action Buttons
    TRIANGLE(1 shl 4, "TRIANGLE", "△", "Triangle (Green)"),
    CIRCLE(1 shl 5, "CIRCLE", "○", "Circle (Red)"),
    CROSS(1 shl 6, "CROSS", "✕", "Cross (Blue)"),
    SQUARE(1 shl 7, "SQUARE", "□", "Square (Pink)"),

    // Shoulders
    L(1 shl 8, "L", "L", "Left Trigger"),
    R(1 shl 9, "R", "R", "Right Trigger"),

    // System Controls
    SELECT(1 shl 10, "SELECT", "SELECT", "Select Button"),
    START(1 shl 11, "START", "START", "Start Button"),
    HOME(1 shl 12, "HOME", "HOME", "PlayStation / Home"),
    VOL_DOWN(1 shl 13, "VOL-", "VOL-", "Volume Down"),
    VOL_UP(1 shl 14, "VOL+", "VOL+", "Volume Up"),
    NOTE(1 shl 15, "NOTE", "♪", "Audio Note");

    companion object {
        fun fromMask(mask: Int): List<PspButton> {
            return entries.filter { (mask and it.mask) != 0 }
        }
    }
}
