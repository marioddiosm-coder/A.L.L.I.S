package com.Mario.allis.brain

/**
 * MemoryStore
 * Sistema de memoria jerárquico de ALLIS
 */
class MemoryStore {

    // ---------- MODELOS DE MEMORIA ----------
    data class MemoryItem(
        val content: String,
        val type: MemoryType,
        var importance: Double,
        val timestamp: Long = System.currentTimeMillis(),
        var usageCount: Int = 0,
        val relations: MutableList<String> = mutableListOf(),
        val origin: MemoryOrigin = MemoryOrigin.USER
    ) {
        // NUEVO: puntaje de relevancia para priorización avanzada
        val relevanceScore: Double
            get() = importance * 0.7 + timestamp.toDouble() * 0.0000001 + usageCount * 0.05
    }

    enum class MemoryType {
        CONTEXT,
        TASK,
        ACTION,
        SEARCH,
        CONVERSATION,
        KNOWLEDGE,
        USER_PREFERENCE,
        SYSTEM_EVENT,
        EMOTIONAL_TAG
    }

    enum class MemoryLayer {
        FAST,
        SHORT,
        MID,
        LONG,
        PERSISTENT
    }

    enum class MemoryOrigin {
        USER,
        SYSTEM
    }

    // ---------- CAPAS DE MEMORIA ----------
    private val fastMemory = mutableListOf<MemoryItem>()
    private val shortTermMemory = mutableListOf<MemoryItem>()
    private val midTermMemory = mutableListOf<MemoryItem>()
    private val longTermMemory = mutableListOf<MemoryItem>()

    private val maxFast = 20
    private val maxShort = 50
    private val maxMid = 200
    private val maxLong = 1000

    // ---------- MÉTODOS PÚBLICOS ----------
    fun storeContext(input: String, intent: String, confidence: Double) {
        addMemory(
            MemoryItem(
                content = "[$intent] $input",
                type = MemoryType.CONTEXT,
                importance = confidence
            )
        )
    }

    fun storeTask(task: String) {
        addMemory(
            MemoryItem(
                content = task,
                type = MemoryType.TASK,
                importance = 0.8
            )
        )
    }

    fun storePendingAction(type: String, description: String) {
        addMemory(
            MemoryItem(
                content = "$type: $description",
                type = MemoryType.ACTION,
                importance = 0.9
            )
        )
    }

    fun storeSearchQuery(query: String) {
        addMemory(
            MemoryItem(
                content = query,
                type = MemoryType.SEARCH,
                importance = 0.6
            )
        )
    }

    fun storeConversation(text: String) {
        addMemory(
            MemoryItem(
                content = text,
                type = MemoryType.CONVERSATION,
                importance = 0.4
            )
        )
    }

    fun storeKnowledge(text: String) {
        addMemory(
            MemoryItem(
                content = text,
                type = MemoryType.KNOWLEDGE,
                importance = 0.85,
                origin = MemoryOrigin.USER
            )
        )
    }

    fun storeUserPreference(text: String) {
        addMemory(
            MemoryItem(
                content = text,
                type = MemoryType.USER_PREFERENCE,
                importance = 0.7,
                origin = MemoryOrigin.USER
            )
        )
    }

    fun storeSystemEvent(event: String, payload: String) {
        addMemory(
            MemoryItem(
                content = "$event: $payload",
                type = MemoryType.SYSTEM_EVENT,
                importance = 0.9,
                origin = MemoryOrigin.SYSTEM
            )
        )
    }

    // ---------- LÓGICA DE MEMORIA ----------
    private fun addMemory(item: MemoryItem) {
        val adjustedItem = item.copy(
            importance = when(item.type) {
                MemoryType.TASK -> (item.importance + 0.1).coerceIn(0.0,1.0)
                MemoryType.ACTION -> (item.importance + 0.05).coerceIn(0.0,1.0)
                MemoryType.SEARCH -> item.importance
                MemoryType.CONVERSATION -> (item.importance - 0.05).coerceIn(0.0,1.0)
                MemoryType.KNOWLEDGE -> (item.importance + 0.1).coerceIn(0.0,1.0)
                MemoryType.USER_PREFERENCE -> (item.importance + 0.05).coerceIn(0.0,1.0)
                MemoryType.SYSTEM_EVENT -> item.importance
                MemoryType.EMOTIONAL_TAG -> (item.importance - 0.1).coerceIn(0.0,1.0)
                MemoryType.CONTEXT -> item.importance
            }
        )

        fastMemory.add(adjustedItem)
        trim(fastMemory, maxFast)

        when {
            adjustedItem.importance >= 0.8 -> {
                longTermMemory.add(adjustedItem)
                trim(longTermMemory, maxLong)
            }
            adjustedItem.importance >= 0.5 -> {
                midTermMemory.add(adjustedItem)
                trim(midTermMemory, maxMid)
            }
            else -> {
                shortTermMemory.add(adjustedItem)
                trim(shortTermMemory, maxShort)
            }
        }
    }

    private fun trim(list: MutableList<MemoryItem>, max: Int) {
        if (list.size > max) {
            list.removeAt(0)
        }
    }

    // ---------- BÚSQUEDA DE MEMORIA RELACIONADA ----------
    private val stopWords = setOf(
        "que", "y", "de", "la", "el", "un", "una", "en", "si", "es", "lo", "por",
        "me", "te", "se", "con", "para", "mi", "tu", "su"
    )

    fun findRelated(input: String, avoidRecent: Int = 3): MemoryItem? {
        val keywords = input
            .lowercase()
            .split(" ")
            .filter { it.length > 3 && it !in stopWords }

        val allMemories = shortTermMemory + midTermMemory + longTermMemory

        val sortedMemories = allMemories
            .sortedWith(compareByDescending<MemoryItem> { it.importance }
                .thenByDescending { it.timestamp })

        val recentMemories = allMemories.takeLast(avoidRecent)

        return sortedMemories.firstOrNull { memory ->
            memory !in recentMemories &&
                keywords.any { memory.content.lowercase().contains(it) }
        }
    }

    // ---------- CONSULTA DE MEMORIA ----------
    fun findRelated(input: String): String? {
        val words = input.lowercase().split(" ")

        val allMemory = (shortTermMemory + midTermMemory + longTermMemory)
            .sortedByDescending { it.timestamp }

        return allMemory.firstOrNull { mem ->
            words.any { it.length > 3 && mem.content.lowercase().contains(it) }
        }?.content
    }

    fun getLastByType(type: MemoryType): MemoryItem? {
        return (shortTermMemory + midTermMemory + longTermMemory)
            .filter { it.type == type }
            .maxByOrNull { it.timestamp }
    }

    fun getAllItems(type: MemoryType): List<MemoryItem> {
        return (shortTermMemory + midTermMemory + longTermMemory)
            .filter { it.type == type }
            .sortedByDescending { it.importance }
    }

    fun getAllItems(): List<MemoryItem> {
        return fastMemory + shortTermMemory + midTermMemory + longTermMemory
    }

    fun getAllPending(): List<MemoryItem> {
        return getAllItems().filter { it.type == MemoryType.ACTION || it.type == MemoryType.TASK }
            .sortedByDescending { it.importance }
    }

    fun prioritizePendingActions() {
        val pending = getAllPending()
        val now = System.currentTimeMillis()
        pending.forEach { item ->
            val ageBoost = ((now - item.timestamp) / 1_000_000.0).coerceAtMost(0.5)
            boostImportance(item, amount = ageBoost)
        }
    }

    fun relevanceScore(item: MemoryItem, input: String): Double {
        val keywords = input.lowercase().split(" ").filter { it.length > 3 }
        val matches = keywords.count { item.content.lowercase().contains(it) }
        return (matches.toDouble() / maxOf(keywords.size, 1)) * item.importance
    }

    fun findMostRelevant(input: String): MemoryItem? {
        return getAllItems().maxByOrNull { relevanceScore(it, input) }
    }

    fun boostImportance(item: MemoryItem, amount: Double = 0.1) {
        item.importance = (item.importance + amount).coerceIn(0.0, 1.0)
    }

    fun markUsed(tag: String) {
        val items = getAllItems().filter { it.content.contains(tag, ignoreCase = true) }
        items.forEach { item ->
            item.usageCount += 1
            boostImportance(item, 0.02)
        }
    }

    fun autoPromote() {
        val candidates = shortTermMemory.filter { it.importance >= 0.6 }
        candidates.forEach {
            midTermMemory.add(it)
        }
        trim(midTermMemory, maxMid)
    }

    fun applyDecay() {
        shortTermMemory.forEach { item ->
            item.importance = (item.importance - 0.01).coerceAtLeast(0.0)
        }
    }

    fun refreshFastMemory() {
        val recent = (shortTermMemory + midTermMemory)
            .sortedByDescending { it.timestamp }
            .take(maxFast)
        fastMemory.clear()
        fastMemory.addAll(recent)
    }

    // ---------- DEBUG ----------
    fun debugPrint() {
        println("FAST MEMORY: ${fastMemory.size}")
        println("SHORT TERM: ${shortTermMemory.size}")
        println("MID TERM: ${midTermMemory.size}")
        println("LONG TERM: ${longTermMemory.size}")
    }
}
