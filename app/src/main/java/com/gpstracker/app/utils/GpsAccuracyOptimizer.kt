package com.gpstracker.app.utils

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * GPS精度优化器
 * 提供多种策略来提高GPS定位精度
 */
class GpsAccuracyOptimizer(private val context: Context) {
    
    private val TAG = "GpsAccuracyOptimizer"
    private val fusedLocationClient: FusedLocationProviderClient
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    // 精度配置
    data class AccuracyConfig(
        val priority: Int = LocationRequest.PRIORITY_HIGH_ACCURACY,
        val interval: Long = 5000L, // 5秒
        val fastestInterval: Long = 2000L, // 2秒
        val smallestDisplacement: Float = 1f, // 1米
        val maxWaitTime: Long = 10000L, // 10秒最大等待时间
        val enableBatching: Boolean = true,
        val enableBackgroundLocation: Boolean = true
    )
    
    // 不同场景的精度配置
    enum class AccuracyMode {
        HIGH_ACCURACY,      // 高精度模式 - 用于精确跟踪
        BALANCED,           // 平衡模式 - 精度和电量平衡
        POWER_SAVE,         // 省电模式 - 降低精度节省电量
        OUTDOOR_ACTIVITY,   // 户外活动模式 - 运动时使用
        INDOOR_NAVIGATION   // 室内导航模式 - 室内使用
    }
    
    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }
    
    /**
     * 获取指定模式的精度配置
     */
    fun getAccuracyConfig(mode: AccuracyMode): AccuracyConfig {
        return when (mode) {
            AccuracyMode.HIGH_ACCURACY -> AccuracyConfig(
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY,
                interval = 2000L, // 2秒
                fastestInterval = 1000L, // 1秒
                smallestDisplacement = 0.5f, // 0.5米
                maxWaitTime = 5000L, // 5秒
                enableBatching = false,
                enableBackgroundLocation = true
            )
            
            AccuracyMode.BALANCED -> AccuracyConfig(
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY,
                interval = 5000L, // 5秒
                fastestInterval = 2000L, // 2秒
                smallestDisplacement = 2f, // 2米
                maxWaitTime = 10000L, // 10秒
                enableBatching = true,
                enableBackgroundLocation = true
            )
            
            AccuracyMode.POWER_SAVE -> AccuracyConfig(
                priority = LocationRequest.PRIORITY_LOW_POWER,
                interval = 15000L, // 15秒
                fastestInterval = 10000L, // 10秒
                smallestDisplacement = 10f, // 10米
                maxWaitTime = 30000L, // 30秒
                enableBatching = true,
                enableBackgroundLocation = false
            )
            
            AccuracyMode.OUTDOOR_ACTIVITY -> AccuracyConfig(
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY,
                interval = 3000L, // 3秒
                fastestInterval = 1500L, // 1.5秒
                smallestDisplacement = 1f, // 1米
                maxWaitTime = 8000L, // 8秒
                enableBatching = false,
                enableBackgroundLocation = true
            )
            
            AccuracyMode.INDOOR_NAVIGATION -> AccuracyConfig(
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY,
                interval = 8000L, // 8秒
                fastestInterval = 4000L, // 4秒
                smallestDisplacement = 3f, // 3米
                maxWaitTime = 15000L, // 15秒
                enableBatching = true,
                enableBackgroundLocation = false
            )
        }
    }
    
    /**
     * 创建高精度位置请求
     */
    fun createHighAccuracyLocationRequest(config: AccuracyConfig): LocationRequest {
        return LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            config.interval
        ).apply {
            setMinUpdateIntervalMillis(config.fastestInterval)
            setMinUpdateDistanceMeters(config.smallestDisplacement)
            setMaxUpdateDelayMillis(config.maxWaitTime)
            setWaitForAccurateLocation(true) // 等待精确位置
            
            // Android 12+ 支持的新特性
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setMaxUpdateDelayMillis(config.maxWaitTime)
            }
        }.build()
    }
    
    /**
     * 检查GPS精度状态
     */
    fun checkGpsAccuracy(): GpsAccuracyStatus {
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val isPassiveEnabled = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)
        
        return GpsAccuracyStatus(
            isGpsEnabled = isGpsEnabled,
            isNetworkEnabled = isNetworkEnabled,
            isPassiveEnabled = isPassiveEnabled,
            hasHighAccuracy = isGpsEnabled && isNetworkEnabled,
            recommendedMode = when {
                isGpsEnabled && isNetworkEnabled -> AccuracyMode.HIGH_ACCURACY
                isGpsEnabled -> AccuracyMode.BALANCED
                isNetworkEnabled -> AccuracyMode.POWER_SAVE
                else -> AccuracyMode.POWER_SAVE
            }
        )
    }
    
    /**
     * 获取最佳位置提供者
     */
    fun getBestLocationProvider(): String {
        val criteria = android.location.Criteria().apply {
            accuracy = android.location.Criteria.ACCURACY_FINE
            powerRequirement = android.location.Criteria.POWER_HIGH
            isAltitudeRequired = true
            isSpeedRequired = true
            isBearingRequired = true
            isCostAllowed = true
        }
        
        return locationManager.getBestProvider(criteria, true) ?: LocationManager.GPS_PROVIDER
    }
    
    /**
     * 验证位置精度
     */
    fun validateLocationAccuracy(location: Location): LocationAccuracy {
        val accuracy = location.accuracy
        val age = System.currentTimeMillis() - location.time
        
        return when {
            accuracy <= 5f && age <= 5000L -> LocationAccuracy.EXCELLENT
            accuracy <= 10f && age <= 10000L -> LocationAccuracy.GOOD
            accuracy <= 20f && age <= 30000L -> LocationAccuracy.FAIR
            accuracy <= 50f && age <= 60000L -> LocationAccuracy.POOR
            else -> LocationAccuracy.UNRELIABLE
        }
    }
    
    /**
     * 优化位置数据
     */
    fun optimizeLocationData(locations: List<Location>): Location? {
        if (locations.isEmpty()) return null
        
        // 过滤掉精度太差的位置
        val validLocations = locations.filter { location ->
            validateLocationAccuracy(location) != LocationAccuracy.UNRELIABLE
        }
        
        if (validLocations.isEmpty()) return locations.firstOrNull()
        
        // 选择最新且精度最好的位置
        return validLocations.maxByOrNull { location ->
            val accuracyScore = 100f - location.accuracy // 精度越高分数越高
            val freshnessScore = (30000L - (System.currentTimeMillis() - location.time)) / 1000f // 越新分数越高
            accuracyScore + freshnessScore
        }
    }
    
    /**
     * 获取精度优化建议
     */
    fun getAccuracyOptimizationTips(): List<String> {
        val tips = mutableListOf<String>()
        val status = checkGpsAccuracy()
        
        if (!status.isGpsEnabled) {
            tips.add("启用GPS定位服务以提高精度")
        }
        
        if (!status.isNetworkEnabled) {
            tips.add("启用网络定位服务以提供辅助定位")
        }
        
        if (!status.hasHighAccuracy) {
            tips.add("同时启用GPS和网络定位可获得最佳精度")
        }
        
        tips.add("在开阔地带使用GPS可获得更高精度")
        tips.add("避免在高楼密集区域使用GPS")
        tips.add("确保设备有良好的天空视野")
        tips.add("定期校准GPS以提高精度")
        
        return tips
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        // 清理FusedLocationProviderClient
        try {
            fusedLocationClient.removeLocationUpdates { }
        } catch (e: Exception) {
            Log.e(TAG, "清理位置更新失败", e)
        }
    }
}

/**
 * GPS精度状态
 */
data class GpsAccuracyStatus(
    val isGpsEnabled: Boolean,
    val isNetworkEnabled: Boolean,
    val isPassiveEnabled: Boolean,
    val hasHighAccuracy: Boolean,
    val recommendedMode: GpsAccuracyOptimizer.AccuracyMode
)

/**
 * 位置精度等级
 */
enum class LocationAccuracy {
    EXCELLENT,  // 优秀 (≤5米, ≤5秒)
    GOOD,       // 良好 (≤10米, ≤10秒)
    FAIR,       // 一般 (≤20米, ≤30秒)
    POOR,       // 较差 (≤50米, ≤60秒)
    UNRELIABLE  // 不可靠 (>50米 或 >60秒)
}
