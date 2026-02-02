package com.Mario.allis.brain

import com.Mario.allis.core.behavior.MoodAnalyzer
import com.Mario.allis.core.behavior.MoodSignal
import com.Mario.allis.core.behavior.PreferenceTracker
import com.Mario.allis.core.finance.CapitalManager
import com.Mario.allis.core.orchestration.OrchestratedTask
import com.Mario.allis.core.orchestration.TaskOrchestrator
import com.Mario.allis.core.orchestration.TaskPriority
import com.Mario.allis.core.productivity.PresentationBuilder
import com.Mario.allis.core.research.ResearchEngine
import com.Mario.allis.core.security.ConfirmationPolicy
import com.Mario.allis.core.vision.VisionSystem

/**
 * AllisBrain v0.5 – Parte 1
 * Cerebro central de ALLIS con:
 * - Estado cognitivo completo
 * - PendingAction / Confirmación
 * - Gestión de sub-intenciones básica
 * - Flujo coherente para CHAT / ACTION / REMINDER / CALL / SEARCH / UNKNOWN
 * - Speaking hook y archivado inicial de sub-intenciones
 * - Hooks para memoria futura y promoción
 */

enum class CognitiveState {
    IDLE,
    LISTENING,
    THINKING,
    WAITING_CONFIRMATION,
    EXECUTING,
    CHATTING,
    INTERRUPTED,
    ERROR
}

class AllisBrain(
    private val memoryStore: MemoryStore = MemoryStore(),
    private val actionEngine: ActionEngine = ActionEngine(),
    private val contextResolver: ContextResolver = ContextResolver(memoryStore),
    private val moodAnalyzer: MoodAnalyzer = MoodAnalyzer(),
    private val preferenceTracker: PreferenceTracker = PreferenceTracker(memoryStore),
    private val researchEngine: ResearchEngine = ResearchEngine(com.Mario.allis.core.api.ApiGateway()),
    private val presentationBuilder: PresentationBuilder = PresentationBuilder(),
    private val capitalManager: CapitalManager = CapitalManager(),
    private val taskOrchestrator: TaskOrchestrator = TaskOrchestrator(),
    private val confirmationPolicy: ConfirmationPolicy = ConfirmationPolicy(),
    private val visionSystem: VisionSystem = VisionSystem()
) {

    // -----------------------
    // ESTADO COGNITIVO Y ACCIÓN PENDIENTE
    // -----------------------
    private var cognitiveState: CognitiveState = CognitiveState.IDLE
    private var pendingAction: MemoryStore.MemoryItem? = null
    private var pendingIntent: IntentionEngine.Intent? = null
    private var lastResponseTimestamp: Long = 0
    private var speaking: Boolean = false
    private var lastDecision: CognitiveDecision? = null
    private var lastMood: MoodSignal? = null

    fun setSpeaking(active: Boolean) { speaking = active }
    fun isSpeaking(): Boolean = speaking

    fun getCognitiveState(): CognitiveState {
        return when {
            pendingAction != null -> CognitiveState.WAITING_CONFIRMATION
            cognitiveState == CognitiveState.EXECUTING -> CognitiveState.EXECUTING
            cognitiveState == CognitiveState.CHATTING -> CognitiveState.CHATTING
            else -> CognitiveState.IDLE
        }
    }

    fun hasRecentResponse(thresholdMs: Long = 1500): Boolean {
        return System.currentTimeMillis() - lastResponseTimestamp < thresholdMs
    }

    // -----------------------
    // PROCESO PRINCIPAL
    // -----------------------
    fun processInput(input: String): AllisResponse {
        cognitiveState = CognitiveState.LISTENING

        if (input.isBlank()) {
            return AllisResponse(
                text = "No te he entendido bien, ¿puedes repetirlo?",
                confidence = 0.2
            )
        }

        // 1️⃣ Control de confirmación
        detectControlIntent(input)?.let {
            return handlePendingControl(it)
        }

        // 2️⃣ Analizar intención + contexto
        cognitiveState = CognitiveState.THINKING
        val detectedIntent = IntentionEngine.detectIntent(input)
        val resolvedContext = resolveContextReferences(input)
        val moodSignal = analyzeMood(input)
        lastMood = moodSignal

        // 3️⃣ Guardar contexto en memoria
        memoryStore.storeContext(
            input = input,
            intent = detectedIntent.mainIntent.name,
            confidence = detectedIntent.confidence
        )

        // 4️⃣ Decisión cognitiva
        val decision = decideNextAction(
            input = input,
            detectedIntent = detectedIntent,
            resolvedContext = resolvedContext
        )
        lastDecision = decision

        // 5️⃣ Activar speaking
        startSpeaking()

        // 6️⃣ Manejar intención principal
        val mainResponse = handleMainIntent(detectedIntent, input)

        // 7️⃣ Manejar sub-intenciones
        archiveSubIntentions(detectedIntent.subIntents, input)
        val subResponses = detectedIntent.subIntents.mapNotNull { sub ->
            when(sub) {
                IntentionEngine.Intent.REMINDER -> "He detectado un recordatorio secundario."
                IntentionEngine.Intent.SEARCH -> "Puedo buscar información relacionada si quieres."
                IntentionEngine.Intent.ACTION -> "Listo para realizar otra acción si confirmas."
                else -> null
            }
        }

        val finalText = buildString {
            append(mainResponse)
            if (subResponses.isNotEmpty()) {
                append(" ")
                append(subResponses.joinToString(" "))
            }
        }

        lastResponseTimestamp = System.currentTimeMillis()

        // 8️⃣ Aprendizaje
        learnFromInteraction(
            input = input,
            detectedIntent = detectedIntent,
            responseText = finalText,
            decision = decision
        )

        // 9️⃣ Sync memoria por capas
        syncWithMemoryLayers()

        // 10️⃣ Reset estado si no requiere confirmación
        if (!detectedIntent.requiresConfirmation) {
            cognitiveState = CognitiveState.IDLE
            pendingAction = null
            pendingIntent = null
        }

        stopSpeaking()

        return AllisResponse(
            text = finalText,
            requiresConfirmation = detectedIntent.requiresConfirmation,
            confidence = detectedIntent.confidence
        )
    }

    // -----------------------
    // MANEJO DE INTENCIÓN PRINCIPAL
    // -----------------------
    private fun handleMainIntent(detectedIntent: IntentionEngine.DetectedIntent, input: String): String {
        return when(detectedIntent.mainIntent) {
            IntentionEngine.Intent.REMINDER -> {
                cognitiveState = CognitiveState.WAITING_CONFIRMATION
                pendingAction = MemoryStore.MemoryItem(input, MemoryStore.MemoryType.TASK, 0.8)
                pendingIntent = IntentionEngine.Intent.REMINDER
                pendingAction?.let { actionEngine.registerPending(it) }
                "He detectado un recordatorio importante, señor. ¿Quieres que lo guarde?"
            }
            IntentionEngine.Intent.CALL -> {
                cognitiveState = CognitiveState.WAITING_CONFIRMATION
                pendingAction = MemoryStore.MemoryItem(input, MemoryStore.MemoryType.ACTION, 0.9)
                pendingIntent = IntentionEngine.Intent.CALL
                pendingAction?.let { actionEngine.registerPending(it) }
                "Puedo hacer la llamada, jefe, pero necesito que me confirmes."
            }
            IntentionEngine.Intent.SEARCH -> {
                cognitiveState = CognitiveState.WAITING_CONFIRMATION
                pendingAction = MemoryStore.MemoryItem(input, MemoryStore.MemoryType.SEARCH, 0.6)
                pendingIntent = IntentionEngine.Intent.SEARCH
                pendingAction?.let { actionEngine.registerPending(it) }
                "Puedo buscar esa información en internet. ¿Quieres que lo haga ahora, señor?"
            }
            IntentionEngine.Intent.ACTION -> {
                cognitiveState = CognitiveState.WAITING_CONFIRMATION
                pendingAction = MemoryStore.MemoryItem(input, MemoryStore.MemoryType.ACTION, 0.9)
                pendingIntent = IntentionEngine.Intent.ACTION
                pendingAction?.let { actionEngine.registerPending(it) }
                "He entendido la acción, jefe. Dímelo cuando quieras que la ejecute."
            }
            IntentionEngine.Intent.LEARN -> {
                cognitiveState = CognitiveState.THINKING
                memoryStore.storeKnowledge(input)
                "He aprendido la nueva información, señor. La guardaré para futuras referencias."
            }
            IntentionEngine.Intent.SYSTEM -> {
                cognitiveState = CognitiveState.THINKING
                memoryStore.storeSystemEvent("SYSTEM_INTENT", input)
                "He registrado la instrucción del sistema, jefe. ¿Quieres que confirme alguna acción?"
            }
            IntentionEngine.Intent.CHAT -> {
                cognitiveState = CognitiveState.CHATTING
                handleChat(input)
            }
            else -> {
                cognitiveState = CognitiveState.IDLE
                handleUnknown(input)
            }
        }
    }

    // -----------------------
    // CONTROL DE CONFIRMACIÓN
    // -----------------------
    private enum class ControlIntent { CONFIRM, CANCEL, NONE }

    private fun detectControlIntent(input: String): ControlIntent {
        val t = input.lowercase()
        return when {
            t in listOf("sí", "vale", "ok") -> ControlIntent.CONFIRM
            t in listOf("no", "mejor no", "cancela", "olvídalo") -> ControlIntent.CANCEL
            else -> ControlIntent.NONE
        }
    }

    private fun handlePendingControl(control: ControlIntent): AllisResponse {
        val responseText = when(control) {
            ControlIntent.CONFIRM -> {
                pendingAction?.let {
                    when(it.type) {
                        MemoryStore.MemoryType.TASK -> memoryStore.storeTask(it.content)
                        MemoryStore.MemoryType.ACTION -> memoryStore.storePendingAction(it.type.name, it.content)
                        MemoryStore.MemoryType.SEARCH -> memoryStore.storeSearchQuery(it.content)
                        MemoryStore.MemoryType.CONVERSATION -> memoryStore.storeConversation(it.content)
                        MemoryStore.MemoryType.KNOWLEDGE -> memoryStore.storeKnowledge(it.content)
                        MemoryStore.MemoryType.USER_PREFERENCE -> memoryStore.storeUserPreference(it.content)
                        MemoryStore.MemoryType.SYSTEM_EVENT -> memoryStore.storeSystemEvent("CONFIRM", it.content)
                        else -> {}
                    }
                    val actionType = pendingIntent?.name ?: "acción"
                    actionEngine.confirmPending(it)
                    "He confirmado y guardado la $actionType, señor."
                } ?: "No había ninguna acción pendiente."
            }
            ControlIntent.CANCEL -> {
                pendingAction = null
                pendingIntent = null
                actionEngine.cancelPending()
                "He cancelado la acción pendiente, jefe."
            }
            else -> ""
        }
        cognitiveState = CognitiveState.IDLE
        pendingAction = null
        pendingIntent = null
        return AllisResponse(text = responseText)
    }

    // -----------------------
    // CHAT Y REFERENCIAS
    // -----------------------
    private fun handleChat(input: String): String {
        fun isReference(text: String) = listOf("sí, eso", "lo mismo", "como antes", "eso").any { it in text.lowercase() }

        memoryStore.storeConversation(input)

        val greetings = listOf("hola", "qué tal", "buenos días", "buenas tardes", "hey")
        if (greetings.any { it in input.lowercase() }) {
            return listOf(
                "Hola, señor. ¿Qué tal estás?",
                "¡Hola, jefe! ¿Cómo te va?",
                "Buenas, ¿quieres charlar un rato, señor?"
            ).random()
        }

        if (isReference(input)) {
            val related = memoryStore.findRelated(input)
            related?.let {
                memoryStore.getLastByType(MemoryStore.MemoryType.CONVERSATION)?.let { item ->
                    memoryStore.boostImportance(item, 0.05)
                }
            }
            return related?.let { "Antes hablábamos de esto: \"$it\". ¿Quieres seguir por ahí?" }
                ?: "No estoy segura de a qué te refieres. ¿Puedes explicármelo más?"
        }

        return listOf(
            "Estoy aquí contigo, señor. ¿De qué te apetece hablar?",
            "Cuéntame más, jefe, me interesa lo que dices.",
            "Hablemos de lo que quieras, señor."
        ).random()
    }

    private fun handleUnknown(input: String) =
        "No estoy segura de haberte entendido del todo. ¿Puedes explicármelo de otra forma?"

    // -----------------------
    // HOOKS PARA MEMORIA FUTURA
    // -----------------------
    fun autoPromote() { /* Hook futuro: promover memorias relevantes */ }
    fun pruneMemory() { /* Hook futuro: limpiar shortTermMemory */ }

    fun debugMemory() {
        println("=== ESTADO COGNITIVO DE ALLIS ===")
        println("CognitiveState: $cognitiveState")
        memoryStore.debugPrint()
        println("================================")
    }

    // -----------------------
    // SPEAKING
    // -----------------------
    fun startSpeaking() { setSpeaking(true) }
    fun stopSpeaking() { setSpeaking(false) }

    // -----------------------
    // ARCHIVADO DE SUB-INTENCIONES BÁSICO
    // -----------------------
    fun archiveSubIntentions(subIntents: List<IntentionEngine.Intent>, input: String) {
        subIntents.forEach { sub ->
            when(sub) {
                IntentionEngine.Intent.REMINDER -> memoryStore.storeTask(input)
                IntentionEngine.Intent.ACTION -> memoryStore.storePendingAction(sub.name, input)
                IntentionEngine.Intent.SEARCH -> memoryStore.storeSearchQuery(input)
                IntentionEngine.Intent.CHAT -> memoryStore.storeConversation(input)
                IntentionEngine.Intent.LEARN -> memoryStore.storeKnowledge(input)
                IntentionEngine.Intent.SYSTEM -> memoryStore.storeSystemEvent("SUB_INTENT", input)
                else -> {}
            }
        }
    }

    // -----------------------
    // RESOLUCIÓN DE CONTEXTO
    // -----------------------
    fun resolveContextReferences(input: String): ResolvedContext {
        return contextResolver.resolve(input)
    }

    // -----------------------
    // DECISIÓN COGNITIVA
    // -----------------------
    fun decideNextAction(
        input: String,
        detectedIntent: IntentionEngine.DetectedIntent,
        resolvedContext: ResolvedContext
    ): CognitiveDecision {
        val requiresConfirmation = detectedIntent.requiresConfirmation
        val actionType = detectedIntent.mainIntent.name
        val reason = if (requiresConfirmation) {
            "Acción crítica detectada, requiere confirmación explícita."
        } else {
            "Intención no crítica, se responde directamente."
        }
        return CognitiveDecision(
            actionType = actionType,
            reason = reason,
            requiresConfirmation = requiresConfirmation,
            contextSummary = resolvedContext.summary
        )
    }

    // -----------------------
    // APRENDIZAJE
    // -----------------------
    fun learnFromInteraction(
        input: String,
        detectedIntent: IntentionEngine.DetectedIntent,
        responseText: String,
        decision: CognitiveDecision
    ) {
        memoryStore.storeConversation("INPUT: $input")
        memoryStore.storeConversation("RESPONSE: $responseText")
        memoryStore.storeSystemEvent("DECISION", "${decision.actionType} | ${decision.reason}")
        memoryStore.markUsed(detectedIntent.mainIntent.name)
    }

    // -----------------------
    // SINCRONIZACIÓN DE MEMORIA
    // -----------------------
    fun syncWithMemoryLayers() {
        memoryStore.autoPromote()
        memoryStore.applyDecay()
        memoryStore.refreshFastMemory()
    }

    // -----------------------
    // INICIATIVA PROACTIVA
    // -----------------------
    fun proposeNextQuestion(resolvedContext: ResolvedContext): String? {
        if (resolvedContext.references.isEmpty()) return null
        return "¿Quieres que profundice en lo anterior, señor?"
    }

    // -----------------------
    // SISTEMA EMOCIONAL / INTENCIÓN
    // -----------------------
    fun analyzeMood(input: String): MoodSignal {
        return moodAnalyzer.analyze(input)
    }

    fun registerPreference(category: String, value: String, delta: Float = 0.1f) {
        preferenceTracker.updatePreference(category, value, delta)
    }

    fun preferenceSummary(): String {
        return preferenceTracker.summarize()
    }

    // -----------------------
    // INVESTIGACIÓN Y PRESENTACIONES
    // -----------------------
    fun research(query: String): String {
        val result = researchEngine.research(query)
        memoryStore.storeKnowledge(result.summary)
        return result.summary
    }

    fun buildPresentation(title: String, sections: Map<String, List<String>>): String {
        val presentation = presentationBuilder.build(title, sections)
        return presentationBuilder.exportMarkdown(presentation)
    }

    // -----------------------
    // CAPITAL Y ORQUESTACIÓN
    // -----------------------
    fun addIncome(category: String, amount: Double, description: String) {
        capitalManager.addIncome(category, amount, description)
    }

    fun addExpense(category: String, amount: Double, description: String) {
        capitalManager.addExpense(category, amount, description)
    }

    fun capitalSummary(): String {
        val summary = capitalManager.summarize()
        return "Balance: ${summary.balance} | Ingresos: ${summary.totalIncome} | Gastos: ${summary.totalExpense}"
    }

    fun enqueueTask(description: String, priority: TaskPriority, requiresConfirmation: Boolean) {
        taskOrchestrator.addTask(
            OrchestratedTask(
                description = description,
                priority = priority,
                requiresConfirmation = requiresConfirmation
            )
        )
    }

    fun nextTaskSummary(): String {
        return taskOrchestrator.nextTask()?.description ?: "No hay tareas pendientes."
    }

    // -----------------------
    // POLÍTICA DE CONFIRMACIÓN
    // -----------------------
    fun requiresConfirmation(intent: IntentionEngine.Intent, payload: String): Boolean {
        return confirmationPolicy.requiresConfirmation(intent, payload)
    }

    // -----------------------
    // VISIÓN EN TIEMPO REAL
    // -----------------------
    fun startVision() {
        visionSystem.start()
    }

    fun stopVision() {
        visionSystem.stop()
    }

    fun getMoodSnapshot(): MoodSignal? = lastMood
}

/**
 * Respuesta estructurada de ALLIS
 */
data class AllisResponse(
    val text: String,
    val requiresConfirmation: Boolean = false,
    val confidence: Double = 1.0
)

// -----------------------
// PARTE 2 – FUNCIONES AVANZADAS Y PRIORIDADES
// -----------------------
class AllisBrainContinuation(private val memoryStore: MemoryStore) {

    // Devuelve todas las memorias de un tipo específico
    fun getAllByType(type: MemoryStore.MemoryType): List<MemoryStore.MemoryItem> {
        return memoryStore.getAllItems(type)
    }

    // Devuelve la acción pendiente más importante
    fun getMostImportantPending(): MemoryStore.MemoryItem? {
        val pendingActions = memoryStore.getAllPending()
        return pendingActions.maxWithOrNull(
            compareBy<MemoryStore.MemoryItem> { it.importance }
                .thenBy { it.timestamp }
        )
    }

    // Prioriza todas las acciones pendientes según importancia y antigüedad
    fun prioritizePendingActions(): List<MemoryStore.MemoryItem> {
        val pending = memoryStore.getAllPending()
        val now = System.currentTimeMillis()
        pending.forEach { item ->
            val ageBoost = ((now - item.timestamp) / 1_000_000.0).coerceAtMost(0.5)
            boostImportance(item, ageBoost)
        }
        return pending.sortedWith(compareByDescending<MemoryStore.MemoryItem> { it.importance }
            .thenBy { it.timestamp })
    }

    // Calcula un score de relevancia entre un input y un MemoryItem
    fun relevanceScore(item: MemoryStore.MemoryItem, input: String): Double {
        val keywords = input.lowercase().split(" ").filter { it.length > 3 }
        val matches = keywords.count { item.content.lowercase().contains(it) }
        return (matches.toDouble() / maxOf(keywords.size, 1)) * item.importance
    }

    // Encuentra la memoria más relevante respecto a un input
    fun findMostRelevant(input: String): MemoryStore.MemoryItem? {
        return memoryStore.getAllItems()
            .maxByOrNull { relevanceScore(it, input) }
    }

    // Aumenta la importancia de un MemoryItem
    fun boostImportance(item: MemoryStore.MemoryItem, amount: Double = 0.1) {
        item.importance = (item.importance + amount).coerceIn(0.0, 1.0)
    }

    // Hooks de mantenimiento cognitivo / memoria
    fun autoPromote() {
        // Hook: promoción automática de memorias importantes a mid/long-term
    }

    fun pruneMemory() {
        // Hook: archivar shortTermMemory sin borrar, mantenimiento de memoria
    }

    fun debugMemory() {
        println("=== ESTADO COGNITIVO DE ALLIS – CONTINUACIÓN ===")
        memoryStore.debugPrint()
        println("==============================================")
    }
}

// -----------------------
// CONTEXTO Y DECISIÓN
// -----------------------
data class ResolvedContext(
    val summary: String,
    val references: List<String> = emptyList()
)

data class CognitiveDecision(
    val actionType: String,
    val reason: String,
    val requiresConfirmation: Boolean,
    val contextSummary: String
)

class ContextResolver(private val memoryStore: MemoryStore) {
    fun resolve(input: String): ResolvedContext {
        val related = memoryStore.findRelated(input)
        val summary = related?.let { "Relacionado: $it" } ?: "Sin referencias previas."
        return ResolvedContext(summary = summary, references = related?.let { listOf(it) } ?: emptyList())
    }
}
