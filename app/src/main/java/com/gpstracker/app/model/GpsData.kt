package com.gpstracker.app.model

data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val state: TrackingState
)

enum class TrackingState {
    INDOOR,    // 室内状态
    OUTDOOR,   // 室外状态
    ACTIVE,    // 活跃状态
    DRIVING    // 驾驶状态
}
