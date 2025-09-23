package com.gpstracker.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * GPS跟踪设置管理器
 * 负责管理所有GPS跟踪相关的配置参数
 */
object SettingsManager {
    
    private const val TAG = "SettingsManager"
    private const val PREFS_NAME = "gps_tracking_settings"
    
    // 默认值常量
    private const val DEFAULT_GPS_TIMEOUT_MS = 45000L
    private const val DEFAULT_ACTIVE_STATE_TIMEOUT_MS = 300000L
    private const val DEFAULT_ACTIVE_STATE_DISTANCE_THRESHOLD = 200.0f
    private const val DEFAULT_STEP_THRESHOLD = 20
    private const val DEFAULT_ACCELERATION_THRESHOLD = 2.0f
    private const val DEFAULT_DEEP_STATIONARY_TIMEOUT_MS = 300000L
    private const val DEFAULT_DEEP_STATIONARY_STEP_THRESHOLD = 30
    private const val DEFAULT_DEEP_STATIONARY_ACCELERATION_THRESHOLD = 1.5f
    private const val DEFAULT_DRIVING_SPEED_THRESHOLD = 7.0f
    private const val DEFAULT_DRIVING_STATIONARY_TIMEOUT_MS = 300000L
    private const val DEFAULT_DRIVING_STATIONARY_DISTANCE_THRESHOLD = 100.0f
    private const val DEFAULT_ENVIRONMENT_CHECK_INTERVAL = 60000L
    private const val DEFAULT_POWER_SAVE_MODE = true
    private const val DEFAULT_GPS_UPDATE_INTERVAL = 10000L
    private const val DEFAULT_STATE_CHECK_INTERVAL = 15000L
    
    /**
     * 获取SharedPreferences实例
     */
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * GPS超时时间 (毫秒)
     */
    fun getGpsTimeoutMs(context: Context): Long {
        return getSharedPreferences(context).getLong("gps_timeout_ms", DEFAULT_GPS_TIMEOUT_MS)
    }
    
    /**
     * 活跃状态保持时间 (毫秒)
     */
    fun getActiveStateTimeoutMs(context: Context): Long {
        return getSharedPreferences(context).getLong("active_state_timeout_ms", DEFAULT_ACTIVE_STATE_TIMEOUT_MS)
    }
    
    /**
     * 活跃状态距离阈值 (米)
     */
    fun getActiveStateDistanceThreshold(context: Context): Float {
        return getSharedPreferences(context).getFloat("active_state_distance_threshold", DEFAULT_ACTIVE_STATE_DISTANCE_THRESHOLD)
    }
    
    /**
     * 步数阈值
     */
    fun getStepThreshold(context: Context): Int {
        return getSharedPreferences(context).getInt("step_threshold", DEFAULT_STEP_THRESHOLD)
    }
    
    /**
     * 加速度阈值 (m/s²)
     */
    fun getAccelerationThreshold(context: Context): Float {
        return getSharedPreferences(context).getFloat("acceleration_threshold", DEFAULT_ACCELERATION_THRESHOLD)
    }
    
    /**
     * 深度静止超时时间 (毫秒)
     */
    fun getDeepStationaryTimeoutMs(context: Context): Long {
        return getSharedPreferences(context).getLong("deep_stationary_timeout_ms", DEFAULT_DEEP_STATIONARY_TIMEOUT_MS)
    }
    
    /**
     * 深度静止步数阈值
     */
    fun getDeepStationaryStepThreshold(context: Context): Int {
        return getSharedPreferences(context).getInt("deep_stationary_step_threshold", DEFAULT_DEEP_STATIONARY_STEP_THRESHOLD)
    }
    
    /**
     * 深度静止加速度阈值 (m/s²)
     */
    fun getDeepStationaryAccelerationThreshold(context: Context): Float {
        return getSharedPreferences(context).getFloat("deep_stationary_acceleration_threshold", DEFAULT_DEEP_STATIONARY_ACCELERATION_THRESHOLD)
    }
    
    /**
     * 驾驶速度阈值 (km/h)
     */
    fun getDrivingSpeedThreshold(context: Context): Float {
        return getSharedPreferences(context).getFloat("driving_speed_threshold", DEFAULT_DRIVING_SPEED_THRESHOLD)
    }
    
    /**
     * 驾驶静止超时时间 (毫秒)
     */
    fun getDrivingStationaryTimeoutMs(context: Context): Long {
        return getSharedPreferences(context).getLong("driving_stationary_timeout_ms", DEFAULT_DRIVING_STATIONARY_TIMEOUT_MS)
    }
    
    /**
     * 驾驶静止距离阈值 (米)
     */
    fun getDrivingStationaryDistanceThreshold(context: Context): Float {
        return getSharedPreferences(context).getFloat("driving_stationary_distance_threshold", DEFAULT_DRIVING_STATIONARY_DISTANCE_THRESHOLD)
    }
    
    /**
     * 环境检测间隔 (毫秒)
     */
    fun getEnvironmentCheckInterval(context: Context): Long {
        return getSharedPreferences(context).getLong("environment_check_interval", DEFAULT_ENVIRONMENT_CHECK_INTERVAL)
    }
    
    /**
     * 是否启用省电模式
     */
    fun isPowerSaveMode(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean("power_save_mode", DEFAULT_POWER_SAVE_MODE)
    }
    
    /**
     * GPS更新间隔 (毫秒)
     */
    fun getGpsUpdateInterval(context: Context): Long {
        return getSharedPreferences(context).getLong("gps_update_interval", DEFAULT_GPS_UPDATE_INTERVAL)
    }
    
    /**
     * 状态检查间隔 (毫秒)
     */
    fun getStateCheckInterval(context: Context): Long {
        return getSharedPreferences(context).getLong("state_check_interval", DEFAULT_STATE_CHECK_INTERVAL)
    }
    
    /**
     * 重置所有设置为默认值
     */
    fun resetToDefaults(context: Context) {
        val editor = getSharedPreferences(context).edit()
        editor.clear()
        editor.apply()
        Log.d(TAG, "设置已重置为默认值")
    }
    
    /**
     * 获取所有设置的摘要信息
     */
    fun getSettingsSummary(context: Context): String {
        val summary = StringBuilder()
        summary.appendLine("=== GPS跟踪设置摘要 ===")
        summary.appendLine("GPS超时: ${getGpsTimeoutMs(context) / 1000}秒")
        summary.appendLine("活跃状态保持: ${getActiveStateTimeoutMs(context) / 60000}分钟")
        summary.appendLine("活跃状态距离阈值: ${getActiveStateDistanceThreshold(context)}米")
        summary.appendLine("步数阈值: ${getStepThreshold(context)}步")
        summary.appendLine("加速度阈值: ${getAccelerationThreshold(context)}m/s²")
        summary.appendLine("深度静止超时: ${getDeepStationaryTimeoutMs(context) / 60000}分钟")
        summary.appendLine("深度静止步数阈值: ${getDeepStationaryStepThreshold(context)}步")
        summary.appendLine("深度静止加速度阈值: ${getDeepStationaryAccelerationThreshold(context)}m/s²")
        summary.appendLine("驾驶速度阈值: ${getDrivingSpeedThreshold(context)}km/h")
        summary.appendLine("驾驶静止超时: ${getDrivingStationaryTimeoutMs(context) / 60000}分钟")
        summary.appendLine("驾驶静止距离阈值: ${getDrivingStationaryDistanceThreshold(context)}米")
        summary.appendLine("环境检测间隔: ${getEnvironmentCheckInterval(context) / 60000}分钟")
        summary.appendLine("省电模式: ${if (isPowerSaveMode(context)) "启用" else "禁用"}")
        summary.appendLine("GPS更新间隔: ${getGpsUpdateInterval(context) / 1000}秒")
        summary.appendLine("状态检查间隔: ${getStateCheckInterval(context) / 1000}秒")
        return summary.toString()
    }
    
    /**
     * 设置变更监听器
     */
    interface OnSettingsChangeListener {
        fun onSettingsChanged()
    }
    
    /**
     * 注册设置变更监听器
     */
    fun registerSettingsChangeListener(context: Context, listener: OnSettingsChangeListener) {
        val prefs = getSharedPreferences(context)
        prefs.registerOnSharedPreferenceChangeListener { _, _ ->
            listener.onSettingsChanged()
        }
    }
}
