package com.Mario.allis.core.behavior

import com.Mario.allis.brain.MemoryStore

data class Preference(
    val category: String,
    val value: String,
    var score: Float = 0.5f
)

class PreferenceTracker(private val memoryStore: MemoryStore) {

    private val preferences = mutableListOf<Preference>()

    fun updatePreference(category: String, value: String, delta: Float = 0.1f) {
        val existing = preferences.firstOrNull { it.category == category && it.value == value }
        if (existing == null) {
            preferences.add(Preference(category, value, 0.5f + delta))
        } else {
            existing.score = (existing.score + delta).coerceIn(0f, 1f)
        }
        memoryStore.storeUserPreference("$category:$value")
    }

    fun getTopPreferences(category: String, limit: Int = 3): List<Preference> {
        return preferences.filter { it.category == category }
            .sortedByDescending { it.score }
            .take(limit)
    }

    fun summarize(): String {
        if (preferences.isEmpty()) return "Sin preferencias registradas."
        return preferences.groupBy { it.category }.entries.joinToString(" | ") { entry ->
            val top = entry.value.sortedByDescending { it.score }.take(2).joinToString { it.value }
            "${entry.key}: $top"
        }
    }
}
