package com.Mario.allis.core.behavior

data class MoodSignal(
    val mood: MoodType,
    val intensity: Float,
    val rationale: String
)

enum class MoodType {
    CALM,
    CURIOUS,
    ALERT,
    EMPATHIC,
    EUPHORIC,
    RESERVED
}

class MoodAnalyzer {

    private val calmWords = setOf("tranquilo", "calma", "relax", "sereno")
    private val alertWords = setOf("urgente", "rápido", "ahora", "peligro")
    private val empathicWords = setOf("triste", "cansado", "mal", "agotado")
    private val euphoricWords = setOf("genial", "increíble", "perfecto", "brutal")

    fun analyze(text: String): MoodSignal {
        val lower = text.lowercase()
        val score = mutableMapOf<MoodType, Float>().apply {
            put(MoodType.CALM, scoreWords(lower, calmWords))
            put(MoodType.ALERT, scoreWords(lower, alertWords))
            put(MoodType.EMPATHIC, scoreWords(lower, empathicWords))
            put(MoodType.EUPHORIC, scoreWords(lower, euphoricWords))
            put(MoodType.CURIOUS, 0.3f)
            put(MoodType.RESERVED, 0.1f)
        }
        val selected = score.maxByOrNull { it.value } ?: (MoodType.CALM to 0.2f)
        return MoodSignal(
            mood = selected.key,
            intensity = selected.value.coerceIn(0f, 1f),
            rationale = "Detección basada en vocabulario y contexto."
        )
    }

    private fun scoreWords(text: String, words: Set<String>): Float {
        val matches = words.count { text.contains(it) }
        return (matches / 3f).coerceIn(0f, 1f)
    }
}
