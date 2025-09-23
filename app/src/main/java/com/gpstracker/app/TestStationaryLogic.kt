package com.gpstracker.app

import android.content.Context
import android.location.Location
import android.util.Log
import com.gpstracker.app.model.TrackingState
import com.gpstracker.app.service.GpsTrackingService
import kotlinx.coroutines.*

/**
 * 测试室内状态切换逻辑的模拟器
 */
class TestStationaryLogic(private val context: Context) {
    
    private val TAG = "TestStationaryLogic"
    
    // 模拟GPS服务的关键参数
    private var currentState = TrackingState.INDOOR
    private var lastGpsTime = 0L
    private var activeStateStartTime = 0L
    private var lastLocation: Location? = null
    private var activeStateStartLocation: Location? = null
    
    // 配置参数 - 与GpsTrackingService保持一致
    private val gpsTimeoutMs = 45000L // 45秒GPS超时
    private val activeStateTimeoutMs = 300000L // 5分钟活跃状态保持时间
    private val activeStateDistanceThreshold = 200.0f // 200米距离阈值
    
    // 位置历史记录
    private val locationHistory = mutableListOf<Pair<Long, Location>>()
    private val maxHistorySize = 100
    
    /**
     * 模拟测试活跃状态切换到室内状态的逻辑
     */
    fun simulateActiveToIndoorTransition() {
        Log.d(TAG, "=== 开始模拟活跃状态切换到室内状态测试 ===")
        
        // 测试场景1：正常切换到活跃状态
        simulateStepToActive()
        
        // 等待一段时间
        Thread.sleep(1000)
        
        // 测试场景2：5分钟后距离条件触发切换到室内
        simulateDistanceBasedTransition()
        
        // 等待一段时间
        Thread.sleep(1000)
        
        // 测试场景3：GPS超时触发切换到室内
        simulateGpsTimeoutTransition()
        
        Log.d(TAG, "=== 模拟测试完成 ===")
    }
    
    /**
     * 模拟步数触发切换到活跃状态
     */
    private fun simulateStepToActive() {
        Log.d(TAG, "--- 测试场景1：步数触发切换到活跃状态 ---")
        
        // 创建模拟位置
        val mockLocation = createMockLocation(39.9042, 116.4074, 10.0f) // 北京天安门
        lastLocation = mockLocation
        activeStateStartLocation = mockLocation
        activeStateStartTime = System.currentTimeMillis()
        lastGpsTime = System.currentTimeMillis()
        
        // 模拟切换到活跃状态
        currentState = TrackingState.ACTIVE
        
        Log.d(TAG, "模拟切换到活跃状态成功")
        Log.d(TAG, "当前状态: $currentState")
        Log.d(TAG, "活跃状态开始时间: $activeStateStartTime")
        Log.d(TAG, "开始位置: ${mockLocation.latitude}, ${mockLocation.longitude}")
    }
    
    /**
     * 模拟基于距离的切换到室内状态
     */
    private fun simulateDistanceBasedTransition() {
        Log.d(TAG, "--- 测试场景2：基于距离的切换到室内状态 ---")
        
        // 模拟5分钟后的情况
        val currentTime = System.currentTimeMillis()
        val timeInActiveState = currentTime - activeStateStartTime
        
        Log.d(TAG, "当前时间: $currentTime")
        Log.d(TAG, "活跃状态持续时间: ${timeInActiveState/1000}秒")
        Log.d(TAG, "是否超过5分钟: ${timeInActiveState > activeStateTimeoutMs}")
        
        if (timeInActiveState > activeStateTimeoutMs) {
            // 创建5分钟前的位置（距离很近，应该触发切换到室内）
            val locationFiveMinutesAgo = createMockLocation(39.9042, 116.4074, 10.0f) // 相同位置
            addLocationToHistory(locationFiveMinutesAgo)
            
            // 创建当前位置（距离很近）
            val currentLocation = createMockLocation(39.9043, 116.4075, 10.0f) // 稍微移动
            lastLocation = currentLocation
            
            val distanceToFiveMinutesAgo = locationFiveMinutesAgo.distanceTo(currentLocation)
            
            Log.d(TAG, "5分钟前位置: ${locationFiveMinutesAgo.latitude}, ${locationFiveMinutesAgo.longitude}")
            Log.d(TAG, "当前位置: ${currentLocation.latitude}, ${currentLocation.longitude}")
            Log.d(TAG, "距离: ${distanceToFiveMinutesAgo}m")
            Log.d(TAG, "距离阈值: ${activeStateDistanceThreshold}m")
            Log.d(TAG, "是否满足距离条件: ${distanceToFiveMinutesAgo <= activeStateDistanceThreshold}")
            
            if (distanceToFiveMinutesAgo <= activeStateDistanceThreshold) {
                currentState = TrackingState.INDOOR
                Log.d(TAG, "✓ 成功切换到室内状态 - 距离条件满足")
            } else {
                Log.d(TAG, "✗ 未切换到室内状态 - 距离条件不满足")
            }
        } else {
            Log.d(TAG, "活跃状态时间不足5分钟，不进行切换检测")
        }
    }
    
    /**
     * 模拟GPS超时触发切换到室内状态
     */
    private fun simulateGpsTimeoutTransition() {
        Log.d(TAG, "--- 测试场景3：GPS超时触发切换到室内状态 ---")
        
        // 模拟GPS超时（45秒没有GPS信号）
        val currentTime = System.currentTimeMillis()
        lastGpsTime = currentTime - 50000L // 50秒前，超过45秒阈值
        
        val gpsTimeout = (currentTime - lastGpsTime) > gpsTimeoutMs
        
        Log.d(TAG, "当前时间: $currentTime")
        Log.d(TAG, "最后GPS时间: $lastGpsTime")
        Log.d(TAG, "GPS超时时间: ${(currentTime - lastGpsTime)/1000}秒")
        Log.d(TAG, "GPS超时阈值: ${gpsTimeoutMs/1000}秒")
        Log.d(TAG, "是否GPS超时: $gpsTimeout")
        
        if (gpsTimeout) {
            currentState = TrackingState.INDOOR
            Log.d(TAG, "✓ 成功切换到室内状态 - GPS超时")
        } else {
            Log.d(TAG, "✗ 未切换到室内状态 - GPS未超时")
        }
    }
    
    /**
     * 创建模拟位置
     */
    private fun createMockLocation(latitude: Double, longitude: Double, accuracy: Float): Location {
        val location = Location("mock")
        location.latitude = latitude
        location.longitude = longitude
        location.accuracy = accuracy
        location.time = System.currentTimeMillis()
        return location
    }
    
    /**
     * 添加位置到历史记录
     */
    private fun addLocationToHistory(location: Location) {
        val currentTime = System.currentTimeMillis()
        locationHistory.add(Pair(currentTime, location))
        
        // 保持历史记录大小在限制范围内
        if (locationHistory.size > maxHistorySize) {
            locationHistory.removeAt(0)
        }
    }
    
    /**
     * 测试边界条件
     */
    fun testBoundaryConditions() {
        Log.d(TAG, "=== 开始测试边界条件 ===")
        
        // 测试距离阈值边界
        testDistanceThreshold()
        
        // 测试时间阈值边界
        testTimeThreshold()
        
        Log.d(TAG, "=== 边界条件测试完成 ===")
    }
    
    /**
     * 测试距离阈值边界
     */
    private fun testDistanceThreshold() {
        Log.d(TAG, "--- 测试距离阈值边界 ---")
        
        val baseLocation = createMockLocation(39.9042, 116.4074, 10.0f)
        
        // 测试刚好在阈值边界的情况
        val testDistances = listOf(199.0f, 200.0f, 201.0f)
        
        for (distance in testDistances) {
            val testLocation = createLocationAtDistance(baseLocation, distance)
            val actualDistance = baseLocation.distanceTo(testLocation)
            
            Log.d(TAG, "测试距离: ${distance}m, 实际距离: ${actualDistance}m")
            Log.d(TAG, "是否触发切换: ${actualDistance <= activeStateDistanceThreshold}")
        }
    }
    
    /**
     * 测试时间阈值边界
     */
    private fun testTimeThreshold() {
        Log.d(TAG, "--- 测试时间阈值边界 ---")
        
        val currentTime = System.currentTimeMillis()
        val testTimes = listOf(299000L, 300000L, 301000L) // 接近5分钟边界
        
        for (timeDiff in testTimes) {
            val testStartTime = currentTime - timeDiff
            val timeInActiveState = currentTime - testStartTime
            
            Log.d(TAG, "测试时间差: ${timeDiff/1000}秒, 活跃时间: ${timeInActiveState/1000}秒")
            Log.d(TAG, "是否超过阈值: ${timeInActiveState > activeStateTimeoutMs}")
        }
    }
    
    /**
     * 在指定距离处创建位置
     */
    private fun createLocationAtDistance(baseLocation: Location, distanceMeters: Float): Location {
        // 简单的距离计算，向北移动
        val earthRadius = 6371000.0 // 地球半径（米）
        val lat1 = Math.toRadians(baseLocation.latitude)
        val lat2 = lat1 + (distanceMeters / earthRadius)
        val lon2 = baseLocation.longitude // 经度不变
        
        val location = Location("mock")
        location.latitude = Math.toDegrees(lat2)
        location.longitude = lon2
        location.accuracy = 10.0f
        location.time = System.currentTimeMillis()
        return location
    }
    
    /**
     * 运行完整的测试套件
     */
    fun runFullTestSuite() {
        Log.d(TAG, "=== 开始运行完整测试套件 ===")
        
        try {
            // 基本功能测试
            simulateActiveToIndoorTransition()
            
            // 边界条件测试
            testBoundaryConditions()
            
            // 性能测试
            testPerformance()
            
            Log.d(TAG, "=== 完整测试套件运行完成 ===")
        } catch (e: Exception) {
            Log.e(TAG, "测试套件运行失败", e)
        }
    }
    
    /**
     * 性能测试
     */
    private fun testPerformance() {
        Log.d(TAG, "--- 性能测试 ---")
        
        val startTime = System.currentTimeMillis()
        
        // 模拟大量位置计算
        repeat(1000) {
            val location1 = createMockLocation(39.9042, 116.4074, 10.0f)
            val location2 = createMockLocation(39.9043, 116.4075, 10.0f)
            val distance = location1.distanceTo(location2)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        Log.d(TAG, "1000次距离计算耗时: ${duration}ms")
        Log.d(TAG, "平均每次计算耗时: ${duration/1000.0}ms")
    }
}
