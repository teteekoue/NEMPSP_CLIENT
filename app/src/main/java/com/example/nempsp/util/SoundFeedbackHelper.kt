package com.example.nempsp.util

import android.media.AudioManager
import android.media.ToneGenerator

class SoundFeedbackHelper {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 40)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    fun playClick(enabled: Boolean = true) {
        if (!enabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 20)
        } catch (_: Exception) {
            // Ignore sound pool/tone error
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {
            // Ignore
        }
    }
}
