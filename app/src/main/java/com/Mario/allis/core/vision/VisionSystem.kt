package com.Mario.allis.core.vision

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * VisionSystem
 * Análisis continuo de cámara/frames para presencia y gestos.
 * Se integra con CameraX o fuente de frames externa.
 */
class VisionSystem(
    private val gestureInterpreter: GestureInterpreter = GestureInterpreter()
) {

    private val active = AtomicBoolean(false)
    private var loopJob: Job? = null
    private var lastPresence = PresenceEvent(false, 0.0f, 0L)
    private val presenceListeners = mutableListOf<(PresenceEvent) -> Unit>()
    private val gestureListeners = mutableListOf<(GestureEvent) -> Unit>()

    fun start() {
        if (active.get()) return
        active.set(true)
        loopJob = CoroutineScope(Dispatchers.Default).launch {
            while (active.get()) {
                delay(200)
            }
        }
    }

    fun stop() {
        active.set(false)
        loopJob?.cancel()
    }

    fun onPresence(listener: (PresenceEvent) -> Unit) {
        presenceListeners.add(listener)
    }

    fun onGesture(listener: (GestureEvent) -> Unit) {
        gestureListeners.add(listener)
    }

    fun feedFrame(frame: VisionFrame) {
        val presence = detectPresence(frame)
        if (presence.detected != lastPresence.detected || presence.confidence > lastPresence.confidence + 0.2f) {
            lastPresence = presence
            presenceListeners.forEach { it(presence) }
        }
    }

    fun feedHandPose(pose: HandPose) {
        val gesture = gestureInterpreter.classify(pose)
        gestureListeners.forEach { it(gesture) }
    }

    private fun detectPresence(frame: VisionFrame): PresenceEvent {
        val average = frame.luminance.map { it.toInt() and 0xFF }.average().toFloat()
        val detected = average in 20f..220f
        val confidence = (average / 255f).coerceIn(0f, 1f)
        return PresenceEvent(detected, confidence, frame.timestamp)
    }
}
