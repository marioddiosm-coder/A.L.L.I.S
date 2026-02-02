package com.Mario.allis.core.vision

/**
 * GestureInterpreter
 * Heurísticas simples para gestos con landmarks.
 */
class GestureInterpreter {

    fun classify(pose: HandPose): GestureEvent {
        if (pose.landmarks.size < 5) {
            return GestureEvent(GestureType.UNKNOWN, 0.1f, System.currentTimeMillis())
        }

        val tips = pose.landmarks.take(5)
        val spread = tips.maxOf { it.x } - tips.minOf { it.x }
        val verticalSpread = tips.maxOf { it.y } - tips.minOf { it.y }

        val gesture = when {
            spread > 0.4f && verticalSpread > 0.3f -> GestureType.OPEN_HAND
            spread < 0.15f && verticalSpread < 0.15f -> GestureType.FIST
            spread > 0.35f && verticalSpread < 0.2f -> GestureType.SWIPE_RIGHT
            spread < 0.2f && verticalSpread > 0.35f -> GestureType.SWIPE_LEFT
            else -> GestureType.POINT
        }

        val confidence = pose.confidence.coerceIn(0.2f, 0.95f)
        return GestureEvent(
            gesture = gesture,
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            meta = mapOf("handedness" to pose.handedness.name)
        )
    }
}
