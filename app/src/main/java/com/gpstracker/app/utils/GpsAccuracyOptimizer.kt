package com.gpstracker.app.utils

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
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
                interval = 30000L, // 30秒 - 进一步降低更新频率
                fastestInterval = 20000L, // 20秒
                smallestDisplacement = 10f, // 10米
                maxWaitTime = 60000L, // 60秒 - 增加等待时间
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
                priority = LocationRequest.PRIORITY_LOW_POWER, // 室内模式使用低功耗
                interval = 60000L, // 60秒 - 与环境检测周期同步，大幅降低更新频率
                fastestInterval = 30000L, // 30秒
                smallestDisplacement = 10f, // 10米 - 降低精度要求
                maxWaitTime = 120000L, // 120秒 - 增加等待时间
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
     * 检测是否为室内环境
     */
    fun detectIndoorEnvironment(): Boolean {
        return try {
            // 检查位置权限
            val hasFineLocation = ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (!hasFineLocation && !hasCoarseLocation) {
                Log.w(TAG, "没有位置权限，无法检测室内环境")
                return false
            }
            
            // 获取最后已知位置
            val lastKnownLocation = if (hasFineLocation) {
                try {
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } catch (e: SecurityException) {
                    Log.w(TAG, "获取GPS位置失败", e)
                    null
                }
            } else null
            
            val lastNetworkLocation = if (hasCoarseLocation || hasFineLocation) {
                try {
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } catch (e: SecurityException) {
                    Log.w(TAG, "获取网络位置失败", e)
                    null
                }
            } else null
            
            val currentTime = System.currentTimeMillis()
            
            // 检查GPS状态
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isGpsRecent = lastKnownLocation != null && 
                             (currentTime - lastKnownLocation.time) < 300000L // 5分钟内
            val isGpsAccurate = lastKnownLocation?.accuracy ?: Float.MAX_VALUE <= 20f // 精度20米内
            
            // 检查网络定位状态
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            val isNetworkRecent = lastNetworkLocation != null && 
                                 (currentTime - lastNetworkLocation.time) < 300000L // 5分钟内
            val isNetworkAccurate = lastNetworkLocation?.accuracy ?: Float.MAX_VALUE <= 100f // 精度100米内
            
            // 室内环境判断逻辑
            val isIndoor = when {
                // GPS完全不可用，但网络定位可用 - 很可能是室内
                !isGpsEnabled && isNetworkEnabled && isNetworkRecent && isNetworkAccurate -> {
                    Log.d(TAG, "GPS完全禁用，网络定位可用 - 判断为室内")
                    true
                }
                // GPS启用但信号很差，网络定位可用 - 可能是室内
                isGpsEnabled && !isGpsRecent && isNetworkEnabled && isNetworkRecent && isNetworkAccurate -> {
                    Log.d(TAG, "GPS信号差，网络定位可用 - 判断为室内")
                    true
                }
                // GPS启用但精度很差，网络定位可用 - 可能是室内
                isGpsEnabled && isGpsRecent && !isGpsAccurate && isNetworkEnabled && isNetworkRecent && isNetworkAccurate -> {
                    Log.d(TAG, "GPS精度差，网络定位可用 - 判断为室内")
                    true
                }
                // 其他情况判断为室外
                else -> {
                    Log.d(TAG, "GPS状态良好或网络定位不可用 - 判断为室外")
                    false
                }
            }
            
            Log.d(TAG, "室内环境检测详情:")
            Log.d(TAG, "  GPS启用: $isGpsEnabled, 最近: $isGpsRecent, 精确: $isGpsAccurate")
            Log.d(TAG, "  网络启用: $isNetworkEnabled, 最近: $isNetworkRecent, 精确: $isNetworkAccurate")
            Log.d(TAG, "  最终判断: ${if (isIndoor) "室内" else "室外"}")
            
            isIndoor
        } catch (e: Exception) {
            Log.e(TAG, "检测室内环境失败", e)
            false
        }
    }
    
    /**
     * 获取智能推荐的精度模式
     */
    fun getSmartRecommendedMode(): AccuracyMode {
        val isIndoor = detectIndoorEnvironment()
        val status = checkGpsAccuracy()
        
        return when {
            isIndoor -> {
                Log.d(TAG, "检测到室内环境，推荐使用室内导航模式")
                AccuracyMode.INDOOR_NAVIGATION
            }
            status.isGpsEnabled && status.isNetworkEnabled -> AccuracyMode.HIGH_ACCURACY
            status.isGpsEnabled -> AccuracyMode.BALANCED
            status.isNetworkEnabled -> AccuracyMode.POWER_SAVE
            else -> AccuracyMode.POWER_SAVE
        }
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
     * 根据精度模式获取位置提供者
     */
    fun getLocationProviderForMode(mode: AccuracyMode): String {
        return when (mode) {
            AccuracyMode.INDOOR_NAVIGATION -> {
                // 室内模式：只使用网络定位，不使用GPS
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    LocationManager.NETWORK_PROVIDER
                } else if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                    LocationManager.PASSIVE_PROVIDER
                } else {
                    LocationManager.GPS_PROVIDER // 回退到GPS
                }
            }
            AccuracyMode.POWER_SAVE -> {
                // 省电模式：优先使用网络定位
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    LocationManager.NETWORK_PROVIDER
                } else {
                    LocationManager.GPS_PROVIDER
                }
            }
            else -> {
                // 其他模式：使用最佳提供者
                getBestLocationProvider()
            }
        }
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
        tips.add("室内环境建议使用网络定位以节省电量")
        tips.add("室内模式下GPS信号弱且耗电，建议禁用")
        
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
