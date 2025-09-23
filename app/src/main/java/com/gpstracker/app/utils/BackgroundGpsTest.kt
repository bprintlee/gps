package com.gpstracker.app.utils

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.util.Log
import com.gpstracker.app.model.GpsData
import com.gpstracker.app.model.TrackingState
import com.gpstracker.app.service.GpsTrackingService

/**
 * 后台GPS功能测试工具
 * 用于验证GPS服务在后台运行时的数据返回能力
 */
object BackgroundGpsTest {
    
    private const val TAG = "BackgroundGpsTest"
    
    /**
     * 测试后台GPS数据获取
     * @param context 上下文
     * @param callback 测试结果回调
     */
    fun testBackgroundGpsData(context: Context, callback: (TestResult) -> Unit) {
        Log.d(TAG, "开始测试后台GPS数据获取...")
        
        val serviceIntent = Intent(context, GpsTrackingService::class.java)
        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                try {
                    service?.let { binder ->
                        val gpsService = (binder as GpsTrackingService.GpsTrackingBinder).getService()
                        
                        // 测试各种数据获取方法
                        val testResult = TestResult().apply {
                            // 基本状态信息
                            currentState = gpsService.getCurrentState()
                            isGpsAvailable = gpsService.isGpsAvailable()
                            lastLocation = gpsService.getLastLocation()
                            stepCount = gpsService.getStepCount()
                            lastAcceleration = gpsService.getLastAcceleration()
                            isPowerSaveMode = gpsService.isPowerSaveMode()
                            
                            // 行程信息
                            currentTripId = gpsService.getCurrentTripId()
                            isTripActive = gpsService.isTripActive()
                            
                            // GPS数据统计
                            gpsDataCount = gpsService.getGpsDataCount()
                            allTripIds = gpsService.getAllTripIds()
                            
                            // 调试信息
                            debugInfo = gpsService.getDebugInfo()
                            
                            // MQTT连接信息
                            mqttConnectionInfo = mapOf("connection_info" to gpsService.getMqttConnectionInfo())
                            
                            success = true
                            message = "后台GPS数据获取测试成功"
                        }
                        
                        Log.d(TAG, "后台GPS测试结果: $testResult")
                        callback(testResult)
                        
                    } ?: run {
                        val errorResult = TestResult(
                            success = false,
                            message = "服务绑定失败：Binder为null"
                        )
                        Log.e(TAG, errorResult.message)
                        callback(errorResult)
                    }
                } catch (e: Exception) {
                    val errorResult = TestResult(
                        success = false,
                        message = "后台GPS测试失败: ${e.message}",
                        exception = e
                    )
                    Log.e(TAG, "后台GPS测试异常", e)
                    callback(errorResult)
                } finally {
                    try {
                        context.unbindService(this)
                    } catch (e: Exception) {
                        Log.w(TAG, "解绑服务失败", e)
                    }
                }
            }
            
            override fun onServiceDisconnected(name: android.content.ComponentName?) {
                Log.w(TAG, "服务连接断开")
            }
        }
        
        try {
            val bindResult = context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (!bindResult) {
                val errorResult = TestResult(
                    success = false,
                    message = "无法绑定GPS服务"
                )
                Log.e(TAG, errorResult.message)
                callback(errorResult)
            }
        } catch (e: Exception) {
            val errorResult = TestResult(
                success = false,
                message = "绑定GPS服务失败: ${e.message}",
                exception = e
            )
            Log.e(TAG, "绑定GPS服务异常", e)
            callback(errorResult)
        }
    }
    
    /**
     * 测试特定行程的GPS数据获取
     * @param context 上下文
     * @param tripId 行程ID
     * @param callback 测试结果回调
     */
    fun testTripGpsData(context: Context, tripId: String, callback: (TripTestResult) -> Unit) {
        Log.d(TAG, "开始测试行程GPS数据获取: $tripId")
        
        val serviceIntent = Intent(context, GpsTrackingService::class.java)
        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                try {
                    service?.let { binder ->
                        val gpsService = (binder as GpsTrackingService.GpsTrackingBinder).getService()
                        
                        // 获取指定行程的GPS数据
                        val gpsDataList = gpsService.getGpsDataByTripId(tripId)
                        
                        val testResult = TripTestResult(
                            tripId = tripId,
                            gpsDataCount = gpsDataList.size,
                            gpsDataList = gpsDataList,
                            success = true,
                            message = "行程GPS数据获取成功，共${gpsDataList.size}个点"
                        )
                        
                        Log.d(TAG, "行程GPS测试结果: $testResult")
                        callback(testResult)
                        
                    } ?: run {
                        val errorResult = TripTestResult(
                            tripId = tripId,
                            success = false,
                            message = "服务绑定失败：Binder为null"
                        )
                        Log.e(TAG, errorResult.message)
                        callback(errorResult)
                    }
                } catch (e: Exception) {
                    val errorResult = TripTestResult(
                        tripId = tripId,
                        success = false,
                        message = "行程GPS测试失败: ${e.message}",
                        exception = e
                    )
                    Log.e(TAG, "行程GPS测试异常", e)
                    callback(errorResult)
                } finally {
                    try {
                        context.unbindService(this)
                    } catch (e: Exception) {
                        Log.w(TAG, "解绑服务失败", e)
                    }
                }
            }
            
            override fun onServiceDisconnected(name: android.content.ComponentName?) {
                Log.w(TAG, "服务连接断开")
            }
        }
        
        try {
            val bindResult = context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (!bindResult) {
                val errorResult = TripTestResult(
                    tripId = tripId,
                    success = false,
                    message = "无法绑定GPS服务"
                )
                Log.e(TAG, errorResult.message)
                callback(errorResult)
            }
        } catch (e: Exception) {
            val errorResult = TripTestResult(
                tripId = tripId,
                success = false,
                message = "绑定GPS服务失败: ${e.message}",
                exception = e
            )
            Log.e(TAG, "绑定GPS服务异常", e)
            callback(errorResult)
        }
    }
    
    /**
     * 测试结果数据类
     */
    data class TestResult(
        var success: Boolean = false,
        var message: String = "",
        var exception: Exception? = null,
        var currentState: TrackingState? = null,
        var isGpsAvailable: Boolean = false,
        var lastLocation: Location? = null,
        var stepCount: Int = 0,
        var lastAcceleration: Float = 0f,
        var isPowerSaveMode: Boolean = false,
        var currentTripId: String? = null,
        var isTripActive: Boolean = false,
        var gpsDataCount: Int = 0,
        var allTripIds: List<String> = emptyList(),
        var debugInfo: Map<String, Any> = emptyMap(),
        var mqttConnectionInfo: Map<String, Any>? = null
    ) {
        override fun toString(): String {
            return buildString {
                appendLine("=== 后台GPS测试结果 ===")
                appendLine("成功: $success")
                appendLine("消息: $message")
                if (exception != null) {
                    appendLine("异常: ${exception?.message}")
                }
                appendLine("当前状态: $currentState")
                appendLine("GPS可用: $isGpsAvailable")
                appendLine("最后位置: ${lastLocation?.let { "${it.latitude}, ${it.longitude} (精度: ${it.accuracy}m)" } ?: "无"}")
                appendLine("步数: $stepCount")
                appendLine("加速度: ${lastAcceleration}m/s²")
                appendLine("省电模式: $isPowerSaveMode")
                appendLine("当前行程ID: $currentTripId")
                appendLine("行程活跃: $isTripActive")
                appendLine("GPS数据数量: $gpsDataCount")
                appendLine("所有行程ID: $allTripIds")
                appendLine("MQTT连接信息: $mqttConnectionInfo")
                appendLine("调试信息: $debugInfo")
            }
        }
    }
    
    /**
     * 行程测试结果数据类
     */
    data class TripTestResult(
        var tripId: String = "",
        var success: Boolean = false,
        var message: String = "",
        var exception: Exception? = null,
        var gpsDataCount: Int = 0,
        var gpsDataList: List<GpsData> = emptyList()
    ) {
        override fun toString(): String {
            return buildString {
                appendLine("=== 行程GPS测试结果 ===")
                appendLine("行程ID: $tripId")
                appendLine("成功: $success")
                appendLine("消息: $message")
                if (exception != null) {
                    appendLine("异常: ${exception?.message}")
                }
                appendLine("GPS数据数量: $gpsDataCount")
                if (gpsDataList.isNotEmpty()) {
                    appendLine("第一个GPS点: ${gpsDataList.first()}")
                    appendLine("最后一个GPS点: ${gpsDataList.last()}")
                }
            }
        }
    }
}
