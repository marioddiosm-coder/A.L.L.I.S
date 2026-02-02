package com.Mario.allis.core.vision

data class VisionFrame(
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val luminance: ByteArray
)

data class HandLandmark(
    val x: Float,
    val y: Float,
    val confidence: Float
)

data class HandPose(
    val landmarks: List<HandLandmark>,
    val handedness: Handedness,
    val confidence: Float
)

enum class Handedness {
    LEFT,
    RIGHT,
    UNKNOWN
}

enum class GestureType {
    OPEN_HAND,
    FIST,
    PINCH,
    POINT,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    UNKNOWN
}

data class GestureEvent(
    val gesture: GestureType,
    val confidence: Float,
    val timestamp: Long,
    val meta: Map<String, String> = emptyMap()
)

data class PresenceEvent(
    val detected: Boolean,
    val confidence: Float,
    val timestamp: Long
)
