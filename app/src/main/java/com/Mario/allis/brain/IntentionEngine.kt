package com.Mario.allis.brain

object IntentionEngine {

    // ------------------------
    // INTENCIONES POSIBLES
    // ------------------------
    enum class Intent {
        REMINDER,
        CALL,
        SEARCH,
        ACTION,
        CHAT,
        LEARN,
        SYSTEM,
        UNKNOWN
    }

    // ------------------------
    // RESULTADO DE DETECCIÓN
    // ------------------------
    data class DetectedIntent(
        val mainIntent: Intent,
        val subIntents: List<Intent> = emptyList(),
        val requiresConfirmation: Boolean = false,
        val confidence: Double = 1.0,
        val contextScore: Double = 0.0
    )

    // ------------------------
    // PATRONES
    // ------------------------
    private val invocationPatterns = listOf(
        "allis", "oye", "por favor", "me ayudas", "necesito que", "quiero que"
    )

    private val reminderPatterns = listOf(
        "recuerda", "no te olvides", "acuérdate", "apunta", "mañana tengo", "tengo que"
    )

    private val callPatterns = listOf(
        "llama", "haz una llamada", "marca a", "contacta a"
    )

    private val searchPatterns = listOf(
        "busca", "qué es", "quién es", "explica", "investiga", "información sobre"
    )

    private val actionPatterns = listOf(
        "pon", "abre", "añade", "enciende", "apaga", "elimina", "borra"
    )

    private val chatPatterns = listOf(
        "hola", "qué tal", "buenos días", "buenas tardes", "cuéntame", "hablamos"
    )

    private val learnPatterns = listOf(
        "aprende", "recuerda esto", "guarda esto", "quiero que aprendas", "toma nota"
    )

    private val systemPatterns = listOf(
        "sistema", "configura", "ajustes", "modo", "prioridad", "permiso"
    )

    private val subIntentConnectors = listOf(
        "y luego", "y después", "además", "después", "también", "después haz"
    )

    // ------------------------
    // HISTORIAL DE CONTEXTO
    // ------------------------
    private val recentContext = mutableListOf<String>()
    private const val CONTEXT_MEMORY = 5

    // ------------------------
    // MÉTODO PRINCIPAL
    // ------------------------
    fun detectIntent(text: String): DetectedIntent {
        val t = text.lowercase().trim()

        // Guardar contexto
        if (t.isNotBlank()) {
            recentContext.add(t)
            if (recentContext.size > CONTEXT_MEMORY) recentContext.removeAt(0)
        }

        val directedToMe = invocationPatterns.any { it in t } ||
                reminderPatterns.any { it in t } ||
                callPatterns.any { it in t } ||
                searchPatterns.any { it in t } ||
                actionPatterns.any { it in t } ||
                chatPatterns.any { it in t } ||
                learnPatterns.any { it in t } ||
                systemPatterns.any { it in t }

        if (!directedToMe) {
            return DetectedIntent(
                mainIntent = Intent.UNKNOWN,
                confidence = 0.2
            )
        }

        // 1️⃣ Puntuar TODAS las intenciones
        val scores = scoreIntents(t)

        // 2️⃣ Ajustar puntuación según contexto previo
        val contextBoosts = recentContextScore(t)
        contextBoosts.forEach { (intent, boost) ->
            scores[intent] = (scores[intent] ?: 0.0) + boost
        }

        // 3️⃣ Elegir intención principal
        val mainIntent = scores
            .maxByOrNull { it.value }
            ?.key ?: Intent.UNKNOWN

        // 4️⃣ Sub-intenciones (mejoradas)
        val subIntents = detectSubIntents(t, mainIntent)

        // 5️⃣ Confirmación
        val requiresConfirmation = mainIntent in listOf(
            Intent.REMINDER,
            Intent.CALL,
            Intent.ACTION
        )

        // 6️⃣ Confianza final
        val confidence = (
            calculateConfidence(t, mainIntent) +
                (scores[mainIntent] ?: 0.0) / 5.0
        ).coerceIn(0.2, 1.0)

        return DetectedIntent(
            mainIntent = mainIntent,
            subIntents = subIntents,
            requiresConfirmation = requiresConfirmation,
            confidence = confidence,
            contextScore = contextBoosts[mainIntent] ?: 0.0
        )
    }

    // ------------------------
    // SOPORTE
    // ------------------------
    private fun scoreIntents(text: String): MutableMap<Intent, Double> {
        val scores = mutableMapOf<Intent, Double>()
        fun score(patterns: List<String>, intent: Intent) {
            patterns.forEach {
                if (it in text) scores[intent] = (scores[intent] ?: 0.0) + 1.0
            }
        }
        score(reminderPatterns, Intent.REMINDER)
        score(callPatterns, Intent.CALL)
        score(searchPatterns, Intent.SEARCH)
        score(actionPatterns, Intent.ACTION)
        score(chatPatterns, Intent.CHAT)
        score(learnPatterns, Intent.LEARN)
        score(systemPatterns, Intent.SYSTEM)
        return scores
    }

    private fun recentContextScore(text: String): Map<Intent, Double> {
        val boostMap = mutableMapOf<Intent, Double>()
        recentContext.forEach { ctx ->
            Intent.values().forEach { intent ->
                if (intentPatterns(intent).any { it in ctx } && intentPatterns(intent).any { it in text }) {
                    boostMap[intent] = (boostMap[intent] ?: 0.0) + 0.3
                }
            }
        }
        return boostMap
    }

    private fun intentPatterns(intent: Intent): List<String> = when (intent) {
        Intent.REMINDER -> reminderPatterns
        Intent.CALL -> callPatterns
        Intent.SEARCH -> searchPatterns
        Intent.ACTION -> actionPatterns
        Intent.CHAT -> chatPatterns
        Intent.LEARN -> learnPatterns
        Intent.SYSTEM -> systemPatterns
        Intent.UNKNOWN -> emptyList()
    }

    private fun detectSubIntents(text: String, mainIntent: Intent): List<Intent> {
        val subs = mutableListOf<Intent>()

        // Conectores semánticos
        subIntentConnectors.forEach { connector ->
            if (text.contains(connector)) {
                text.split(connector)
                    .drop(1)
                    .forEach { part ->
                        val detected = detectSimpleIntent(part)
                        if (detected != Intent.UNKNOWN && detected != mainIntent) subs.add(detected)
                    }
            }
        }

        // Detección secundaria en todo el texto
        val secondaryIntents = Intent.values().filter { it != mainIntent && it != Intent.UNKNOWN }
        secondaryIntents.forEach { intent ->
            if (intentPatterns(intent).any { it in text }) subs.add(intent)
        }

        return subs.distinct()
    }

    private fun detectSimpleIntent(text: String): Intent =
        when {
            reminderPatterns.any { it in text } -> Intent.REMINDER
            callPatterns.any { it in text } -> Intent.CALL
            searchPatterns.any { it in text } -> Intent.SEARCH
            actionPatterns.any { it in text } -> Intent.ACTION
            chatPatterns.any { it in text } -> Intent.CHAT
            learnPatterns.any { it in text } -> Intent.LEARN
            systemPatterns.any { it in text } -> Intent.SYSTEM
            else -> Intent.UNKNOWN
        }

    private fun calculateConfidence(text: String, intent: Intent): Double {
        val words = text.split(" ")
        var score = 0.0
        intentPatterns(intent).forEach {
            if (it in text) score += 1.0
        }
        return (score / maxOf(words.size, 1)).coerceIn(0.1, 1.0)
    }
}
