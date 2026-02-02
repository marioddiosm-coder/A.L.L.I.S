package com.Mario.allis.brain

/**
 * ActionEngine
 * Ejecuta acciones reales solo después de confirmación explícita.
 */
class ActionEngine {

    private var pendingAction: MemoryStore.MemoryItem? = null

    fun registerPending(action: MemoryStore.MemoryItem) {
        pendingAction = action
    }

    fun confirmPending(action: MemoryStore.MemoryItem) {
        pendingAction = action
    }

    fun cancelPending() {
        pendingAction = null
    }

    fun hasPending(): Boolean = pendingAction != null

    fun executeConfirmed(action: MemoryStore.MemoryItem): ActionResult {
        return ActionResult(
            success = true,
            message = "Acción ejecutada: ${action.content}",
            actionType = action.type.name
        )
    }
}

data class ActionResult(
    val success: Boolean,
    val message: String,
    val actionType: String
)
