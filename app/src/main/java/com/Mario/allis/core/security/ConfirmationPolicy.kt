package com.Mario.allis.core.security

import com.Mario.allis.brain.IntentionEngine

class ConfirmationPolicy {

    private val criticalIntents = setOf(
        IntentionEngine.Intent.CALL,
        IntentionEngine.Intent.ACTION,
        IntentionEngine.Intent.REMINDER
    )

    fun requiresConfirmation(intent: IntentionEngine.Intent, payload: String): Boolean {
        if (intent in criticalIntents) return true
        val criticalKeywords = listOf("compra", "envía", "transfiere", "borra", "elimina")
        return criticalKeywords.any { payload.lowercase().contains(it) }
    }
}
