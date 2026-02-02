package com.Mario.allis.core.interface

/**
 * AllisInterface
 * Entrada/salida desacoplada para texto, voz, UI y eventos.
 */
class AllisInterface {

    private val eventLog = mutableListOf<InterfaceEvent>()

    fun emit(event: InterfaceEvent) {
        eventLog.add(event)
    }

    fun getRecentEvents(limit: Int = 20): List<InterfaceEvent> {
        return eventLog.takeLast(limit)
    }
}

sealed class InterfaceEvent {
    data class TextInput(val text: String) : InterfaceEvent()
    data class TextOutput(val text: String) : InterfaceEvent()
    data class VoiceInput(val transcript: String) : InterfaceEvent()
    data class VoiceOutput(val text: String) : InterfaceEvent()
    data class UiState(val state: String) : InterfaceEvent()
}
