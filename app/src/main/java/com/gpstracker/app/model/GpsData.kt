package com.gpstracker.app.model

data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val state: TrackingState,
    val tripId: String? = null  // 行程ID，用于分组GPS数据
)

enum class TrackingState {
    INDOOR,           // 室内状态
    OUTDOOR,          // 室外状态
    ACTIVE,           // 活跃状态
    DRIVING,          // 驾驶状态
    DEEP_STATIONARY   // 深度静止状态 - 长时间无移动，仅监测步数和加速度
}
