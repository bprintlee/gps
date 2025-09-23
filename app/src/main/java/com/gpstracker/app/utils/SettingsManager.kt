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
    
    // 新增状态转换阈值
    private const val DEFAULT_INDOOR_TO_OUTDOOR_STEP_THRESHOLD = 50
    private const val DEFAULT_INDOOR_TO_OUTDOOR_ACCELERATION_THRESHOLD = 3.0f
    private const val DEFAULT_INDOOR_TO_OUTDOOR_DISTANCE_THRESHOLD = 100.0f
    private const val DEFAULT_OUTDOOR_TO_ACTIVE_STEP_THRESHOLD = 100
    private const val DEFAULT_OUTDOOR_TO_ACTIVE_ACCELERATION_THRESHOLD = 4.0f
    private const val DEFAULT_OUTDOOR_TO_ACTIVE_DISTANCE_THRESHOLD = 500.0f
    private const val DEFAULT_OUTDOOR_TO_ACTIVE_TIME_THRESHOLD = 120000L // 2分钟
    private const val DEFAULT_ACTIVE_TO_DRIVING_SPEED_THRESHOLD = 15.0f
    private const val DEFAULT_ACTIVE_TO_DRIVING_TIME_THRESHOLD = 60000L // 1分钟
    private const val DEFAULT_DRIVING_TO_ACTIVE_SPEED_THRESHOLD = 5.0f
    private const val DEFAULT_DRIVING_TO_ACTIVE_TIME_THRESHOLD = 180000L // 3分钟
    private const val DEFAULT_LOCATION_ACCURACY_THRESHOLD = 50.0f
    private const val DEFAULT_MQTT_ACCURACY_THRESHOLD = 60.0f
    private const val DEFAULT_MAX_LOCATION_HISTORY = 100
    private const val DEFAULT_SENSOR_UPDATE_INTERVAL = 1000L
    
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
    
    // ========== 新增状态转换阈值方法 ==========
    
    /**
     * 室内到室外步数阈值
     */
    fun getIndoorToOutdoorStepThreshold(context: Context): Int {
        return getSharedPreferences(context).getInt("indoor_to_outdoor_step_threshold", DEFAULT_INDOOR_TO_OUTDOOR_STEP_THRESHOLD)
    }
    
    /**
     * 室内到室外加速度阈值
     */
    fun getIndoorToOutdoorAccelerationThreshold(context: Context): Float {
        return getSharedPreferences(context).getFloat("indoor_to_outdoor_acceleration_threshold", DEFAULT_INDOOR_TO_OUTDOOR_ACCELERATION_THRESHOLD)
    }
    
    /**
     * 室内到室外距离阈值
     */
    fun getIndoorToOutdoorDistanceThreshold(context: Context): Float {
        return getSharedPreferences(context).getFloat("indoor_to_outdoor_distance_threshold", DEFAULT_INDOOR_TO_OUTDOOR_DISTANCE_THRESHOLD)
    }
    
    /**
     * 室外到活跃步数阈值
     */
    fun getOutdoorToActiveStepThreshold(context: Context): Int {
        return getSharedPreferences(context).getInt("outdoor_to_active_step_threshold", DEFAULT_OUTDOOR_TO_ACTIVE_STEP_THRESHOLD)
    }
    
    /**
     * 室外到活跃加速度阈值
     */
    fun getOutdoorToActiveAccelerationThreshold(context: Context): Float {
        return getSharedPreferences(context).getFloat("outdoor_to_active_acceleration_threshold", DEFAULT_OUTDOOR_TO_ACTIVE_ACCELERATION_THRESHOLD)
    }
    
    /**
     * 室外到活跃距离阈值
     */
    fun getOutdoorToActiveDistanceThreshold(context: Context): Float {
        return getSharedPreferences(context).getFloat("outdoor_to_active_distance_threshold", DEFAULT_OUTDOOR_TO_ACTIVE_DISTANCE_THRESHOLD)
    }
    
    /**
     * 室外到活跃时间阈值
     */
    fun getOutdoorToActiveTimeThreshold(context: Context): Long {
        return getSharedPreferences(context).getLong("outdoor_to_active_time_threshold", DEFAULT_OUTDOOR_TO_ACTIVE_TIME_THRESHOLD)
    }
    
    /**
     * 活跃到驾驶速度阈值
     */
    fun getActiveToDrivingSpeedThreshold(context: Context): Float {
        return getSharedPreferences(context).getFloat("active_to_driving_speed_threshold", DEFAULT_ACTIVE_TO_DRIVING_SPEED_THRESHOLD)
    }
    
    /**
     * 活跃到驾驶时间阈值
     */
    fun getActiveToDrivingTimeThreshold(context: Context): Long {
        return getSharedPreferences(context).getLong("active_to_driving_time_threshold", DEFAULT_ACTIVE_TO_DRIVING_TIME_THRESHOLD)
    }
    
    /**
     * 驾驶到活跃速度阈值
     */
    fun getDrivingToActiveSpeedThreshold(context: Context): Float {
        return getSharedPreferences(context).getFloat("driving_to_active_speed_threshold", DEFAULT_DRIVING_TO_ACTIVE_SPEED_THRESHOLD)
    }
    
    /**
     * 驾驶到活跃时间阈值
     */
    fun getDrivingToActiveTimeThreshold(context: Context): Long {
        return getSharedPreferences(context).getLong("driving_to_active_time_threshold", DEFAULT_DRIVING_TO_ACTIVE_TIME_THRESHOLD)
    }
    
    /**
     * 位置精度阈值
     */
    fun getLocationAccuracyThreshold(context: Context): Float {
        return getSharedPreferences(context).getFloat("location_accuracy_threshold", DEFAULT_LOCATION_ACCURACY_THRESHOLD)
    }
    
    /**
     * MQTT精度阈值
     */
    fun getMqttAccuracyThreshold(context: Context): Float {
        return getSharedPreferences(context).getFloat("mqtt_accuracy_threshold", DEFAULT_MQTT_ACCURACY_THRESHOLD)
    }
    
    /**
     * 最大位置历史数量
     */
    fun getMaxLocationHistory(context: Context): Int {
        return getSharedPreferences(context).getInt("max_location_history", DEFAULT_MAX_LOCATION_HISTORY)
    }
    
    /**
     * 传感器更新间隔
     */
    fun getSensorUpdateInterval(context: Context): Long {
        return getSharedPreferences(context).getLong("sensor_update_interval", DEFAULT_SENSOR_UPDATE_INTERVAL)
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
        
        // 基本GPS设置
        summary.appendLine("【基本GPS设置】")
        summary.appendLine("GPS超时: ${getGpsTimeoutMs(context) / 1000}秒")
        summary.appendLine("GPS更新间隔: ${getGpsUpdateInterval(context) / 1000}秒")
        summary.appendLine("状态检查间隔: ${getStateCheckInterval(context) / 1000}秒")
        summary.appendLine("位置精度阈值: ${getLocationAccuracyThreshold(context)}米")
        summary.appendLine("MQTT精度阈值: ${getMqttAccuracyThreshold(context)}米")
        summary.appendLine("最大位置历史: ${getMaxLocationHistory(context)}个")
        summary.appendLine("传感器更新间隔: ${getSensorUpdateInterval(context)}毫秒")
        
        // 状态转换阈值
        summary.appendLine("\n【状态转换阈值】")
        summary.appendLine("室内→室外步数: ${getIndoorToOutdoorStepThreshold(context)}步")
        summary.appendLine("室内→室外加速度: ${getIndoorToOutdoorAccelerationThreshold(context)}m/s²")
        summary.appendLine("室内→室外距离: ${getIndoorToOutdoorDistanceThreshold(context)}米")
        summary.appendLine("室外→活跃步数: ${getOutdoorToActiveStepThreshold(context)}步")
        summary.appendLine("室外→活跃加速度: ${getOutdoorToActiveAccelerationThreshold(context)}m/s²")
        summary.appendLine("室外→活跃距离: ${getOutdoorToActiveDistanceThreshold(context)}米")
        summary.appendLine("室外→活跃时间: ${getOutdoorToActiveTimeThreshold(context) / 1000}秒")
        summary.appendLine("活跃→驾驶速度: ${getActiveToDrivingSpeedThreshold(context)}km/h")
        summary.appendLine("活跃→驾驶时间: ${getActiveToDrivingTimeThreshold(context) / 1000}秒")
        summary.appendLine("驾驶→活跃速度: ${getDrivingToActiveSpeedThreshold(context)}km/h")
        summary.appendLine("驾驶→活跃时间: ${getDrivingToActiveTimeThreshold(context) / 1000}秒")
        
        // 活跃状态设置
        summary.appendLine("\n【活跃状态设置】")
        summary.appendLine("活跃状态保持: ${getActiveStateTimeoutMs(context) / 60000}分钟")
        summary.appendLine("活跃状态距离阈值: ${getActiveStateDistanceThreshold(context)}米")
        
        // 深度静止设置
        summary.appendLine("\n【深度静止设置】")
        summary.appendLine("深度静止超时: ${getDeepStationaryTimeoutMs(context) / 60000}分钟")
        summary.appendLine("深度静止步数阈值: ${getDeepStationaryStepThreshold(context)}步")
        summary.appendLine("深度静止加速度阈值: ${getDeepStationaryAccelerationThreshold(context)}m/s²")
        
        // 驾驶模式设置
        summary.appendLine("\n【驾驶模式设置】")
        summary.appendLine("驾驶速度阈值: ${getDrivingSpeedThreshold(context)}km/h")
        summary.appendLine("驾驶静止超时: ${getDrivingStationaryTimeoutMs(context) / 60000}分钟")
        summary.appendLine("驾驶静止距离阈值: ${getDrivingStationaryDistanceThreshold(context)}米")
        
        // 其他设置
        summary.appendLine("\n【其他设置】")
        summary.appendLine("步数阈值: ${getStepThreshold(context)}步")
        summary.appendLine("加速度阈值: ${getAccelerationThreshold(context)}m/s²")
        summary.appendLine("环境检测间隔: ${getEnvironmentCheckInterval(context) / 60000}分钟")
        summary.appendLine("省电模式: ${if (isPowerSaveMode(context)) "启用" else "禁用"}")
        
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
