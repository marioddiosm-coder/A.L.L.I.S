package com.Mario.allis.voice

/**
 * VoiceEngine
 * Control de voz y estado cognitivo vocal.
 */
class VoiceEngine {

    val keyId = "8907a340581b871500e07693a4f0646b4456522277626571e33e3c55e743f7e7"
    val voiceId = "QP13qjAnJsxC9HbAEHoG"

    private var speaking = false

    fun startSpeaking() {
        speaking = true
    }

    fun stopSpeaking() {
        speaking = false
    }

    fun isSpeaking(): Boolean = speaking
}
