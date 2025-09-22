package com.gpstracker.app.utils

import android.content.Context
import android.os.Build
import android.util.Log
import com.gpstracker.app.model.GpsData
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.*

/**
 * Android 15 MQTT 解决方案
 * 由于官方Eclipse Paho库也没有适配Android 15，我们使用纯Java MQTT客户端
 * 避免使用Android特定的MqttAndroidClient
 */
class Android15MqttWorkaround(private val context: Context) {
    
    private val TAG = "Android15MqttWorkaround"
    private var mqttClient: MqttClient? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logManager = LogManager(context)
    
    // MQTT配置
    private val serverUri = "tcp://8.153.37.172:1883"
    private val topic = "owntracks/user/bprint"
    private val clientId = "gps_tracker_${UUID.randomUUID().toString().substring(0, 8)}"
    
    // 连接状态
    private var isConnecting = false
    private var lastConnectAttempt = 0L
    private val minConnectInterval = 5000L
    
    // 状态记录
    private var lastConnectionState = "未连接"
    private var lastError: Throwable? = null
    private var connectionAttempts = 0
    
    // 自动重连配置
    private var isAutoReconnectEnabled = true
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 10
    private val reconnectDelayMs = 5000L // 5秒重连延迟
    private var reconnectJob: Job? = null
    
    fun connect() {
        Log.d(TAG, "=== 开始Android 15兼容的MQTT连接 ===")
        Log.d(TAG, "服务器URI: $serverUri")
        Log.d(TAG, "客户端ID: $clientId")
        Log.d(TAG, "主题: $topic")
        logManager.saveLog(TAG, "DEBUG", "开始Android 15兼容的MQTT连接")
        
        try {
            // 检查网络连接
            if (!isNetworkAvailable()) {
                Log.w(TAG, "网络不可用，跳过MQTT连接")
                logManager.saveLog(TAG, "WARN", "网络不可用，跳过MQTT连接")
                return
            }
            
            // 防止频繁连接
            val currentTime = System.currentTimeMillis()
            if (isConnecting) {
                Log.d(TAG, "MQTT连接已在进行中，跳过")
                return
            }
            if ((currentTime - lastConnectAttempt) < minConnectInterval) {
                Log.d(TAG, "连接间隔太短，跳过")
                return
            }
            
            isConnecting = true
            lastConnectAttempt = currentTime
            connectionAttempts++
            lastConnectionState = "连接中"
            
            // 使用纯Java MQTT客户端，避免Android特定的BroadcastReceiver问题
            mqttClient = MqttClient(serverUri, clientId, MemoryPersistence())
            
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                // 禁用自动重连，避免BroadcastReceiver问题
                isAutomaticReconnect = false
                connectionTimeout = 10
                keepAliveInterval = 60
                mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
                isHttpsHostnameVerificationEnabled = false
            }
            
            // 设置回调
            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "连接丢失", cause)
                    isConnecting = false
                    lastConnectionState = "连接丢失"
                    lastError = cause
                    logManager.saveLog(TAG, "WARN", "MQTT连接丢失: ${cause?.message}")
                    
                    // 启动自动重连
                    if (isAutoReconnectEnabled) {
                        startAutoReconnect()
                    }
                }
                
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d(TAG, "收到MQTT消息: $topic -> ${message?.toString()}")
                    logManager.saveLog(TAG, "INFO", "收到MQTT消息: $topic -> ${message?.toString()}")
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "消息发送完成")
                    logManager.saveLog(TAG, "INFO", "MQTT消息发送完成")
                }
            })
            
            // 尝试连接
            Log.d(TAG, "开始尝试MQTT连接...")
            mqttClient?.connect(options)
            
            Log.d(TAG, "✅ MQTT连接成功！")
            lastConnectionState = "已连接"
            lastError = null
            isConnecting = false
            
            // 连接成功，重置重连计数器
            resetReconnectAttempts()
            
            logManager.saveLog(TAG, "INFO", "MQTT连接成功")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ MQTT连接失败", e)
            Log.e(TAG, "失败原因: ${e.message}")
            Log.e(TAG, "异常类型: ${e.javaClass.simpleName}")
            lastConnectionState = "连接失败"
            lastError = e
            isConnecting = false
            logManager.saveLog(TAG, "ERROR", "MQTT连接失败: ${e.message}")
        }
    }
    
    fun publishLocation(gpsData: GpsData) {
        serviceScope.launch {
            try {
                Log.d(TAG, "=== 开始发布位置数据 ===")
                Log.d(TAG, "位置数据: lat=${gpsData.latitude}, lon=${gpsData.longitude}, acc=${gpsData.accuracy}")
                
                if (!isNetworkAvailable()) {
                    Log.w(TAG, "网络不可用，跳过位置数据发送")
                    return@launch
                }
                
                Log.d(TAG, "MQTT客户端状态: ${mqttClient?.isConnected}")
                Log.d(TAG, "服务器URI: $serverUri")
                Log.d(TAG, "主题: $topic")
                
                if (mqttClient?.isConnected == true) {
                    val message = createLocationMessage(gpsData)
                    Log.d(TAG, "准备发送的消息: $message")
                    
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttMessage.qos = 1
                    
                    mqttClient?.publish(topic, mqttMessage)
                    
                    Log.d(TAG, "✅ 位置数据发送成功: ${gpsData.latitude}, ${gpsData.longitude}")
                    Log.d(TAG, "发送到主题: $topic")
                    logManager.saveLog(TAG, "INFO", "位置数据发送成功: ${gpsData.latitude}, ${gpsData.longitude}")
                } else {
                    Log.w(TAG, "⚠️ MQTT未连接，尝试重新连接")
                    connect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "发送位置数据异常", e)
                logManager.saveLog(TAG, "ERROR", "发送位置数据异常: ${e.message}")
            }
        }
    }
    
    fun isConnected(): Boolean {
        return try {
            mqttClient?.isConnected == true
        } catch (e: Exception) {
            Log.e(TAG, "检查连接状态异常", e)
            false
        }
    }
    
    fun getConnectionInfo(): String {
        return when {
            isConnecting -> "连接中..."
            isConnected() -> "已连接"
            else -> lastConnectionState
        }
    }
    
    fun getDetailedState(): String {
        return try {
            val state = StringBuilder()
            state.appendLine("=== MQTT详细状态 (Android 15纯Java模式) ===")
            state.appendLine("连接状态: ${getConnectionInfo()}")
            state.appendLine("是否连接中: $isConnecting")
            state.appendLine("连接尝试次数: $connectionAttempts")
            state.appendLine("最后连接尝试: ${if (lastConnectAttempt > 0) java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(Date(lastConnectAttempt)) else "无"}")
            state.appendLine("服务器URI: $serverUri")
            state.appendLine("客户端ID: $clientId")
            state.appendLine("主题: $topic")
            state.appendLine("最后错误: ${lastError?.message ?: "无"}")
            state.appendLine("最后错误类型: ${lastError?.javaClass?.simpleName ?: "无"}")
            state.appendLine("网络可用: ${isNetworkAvailable()}")
            state.appendLine("自动重连: ${if (isAutoReconnectEnabled) "已启用" else "已禁用"}")
            state.appendLine("重连次数: $reconnectAttempts/$maxReconnectAttempts")
            state.appendLine("兼容性: 使用纯Java MQTT客户端避免BroadcastReceiver问题")
            state.toString()
        } catch (e: Exception) {
            Log.e(TAG, "获取详细状态失败", e)
            "状态获取失败: ${e.message}"
        }
    }
    
    // MQTT测试功能已删除
    
    fun cleanup() {
        try {
            disconnect()
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "清理资源异常", e)
        }
    }
    
    private     fun disconnect() {
        try {
            // 停止自动重连
            stopAutoReconnect()
            
            mqttClient?.disconnect()
            Log.d(TAG, "MQTT已断开连接")
            lastConnectionState = "已断开"
        } catch (e: Exception) {
            Log.e(TAG, "MQTT断开连接异常", e)
        }
    }
    
    /**
     * 启动自动重连
     */
    private fun startAutoReconnect() {
        if (reconnectJob?.isActive == true) {
            Log.d(TAG, "自动重连已在进行中，跳过")
            return
        }
        
        if (reconnectAttempts >= maxReconnectAttempts) {
            Log.w(TAG, "已达到最大重连次数 ($maxReconnectAttempts)，停止自动重连")
            logManager.saveLog(TAG, "WARN", "已达到最大重连次数，停止自动重连")
            return
        }
        
        reconnectAttempts++
        Log.d(TAG, "启动自动重连 (第 $reconnectAttempts/$maxReconnectAttempts 次)")
        logManager.saveLog(TAG, "INFO", "启动自动重连 (第 $reconnectAttempts/$maxReconnectAttempts 次)")
        
        reconnectJob = serviceScope.launch {
            try {
                delay(reconnectDelayMs)
                
                if (isNetworkAvailable()) {
                    Log.d(TAG, "网络可用，尝试重连...")
                    connect()
                } else {
                    Log.w(TAG, "网络不可用，延迟重连")
                    // 网络不可用时，延长重连间隔
                    delay(reconnectDelayMs * 2)
                    startAutoReconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "自动重连异常", e)
                logManager.saveLog(TAG, "ERROR", "自动重连异常: ${e.message}")
                
                // 重连失败，继续尝试
                if (reconnectAttempts < maxReconnectAttempts) {
                    delay(reconnectDelayMs * 2)
                    startAutoReconnect()
                }
            }
        }
    }
    
    /**
     * 停止自动重连
     */
    private fun stopAutoReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempts = 0
        Log.d(TAG, "已停止自动重连")
    }
    
    /**
     * 重置重连计数器（连接成功时调用）
     */
    private fun resetReconnectAttempts() {
        reconnectAttempts = 0
        reconnectJob?.cancel()
        reconnectJob = null
        Log.d(TAG, "连接成功，重置重连计数器")
    }
    
    /**
     * 设置自动重连开关
     */
    fun setAutoReconnectEnabled(enabled: Boolean) {
        isAutoReconnectEnabled = enabled
        Log.d(TAG, "自动重连已${if (enabled) "启用" else "禁用"}")
        
        if (!enabled) {
            stopAutoReconnect()
        }
    }
    
    private fun createLocationMessage(gpsData: GpsData): String {
        val timestamp = gpsData.timestamp / 1000
        val batteryLevel = getBatteryLevel()
        
        return """
        {
          "_type": "location",
          "BSSID": "02:00:00:00:00:00",
          "SSID": "<unknown ssid>",
          "_id": "${UUID.randomUUID().toString().substring(0, 8)}",
          "acc": ${gpsData.accuracy.toInt()},
          "alt": ${gpsData.altitude.toInt()},
          "batt": $batteryLevel,
          "bs": 1,
          "conn": "w",
          "created_at": $timestamp,
          "inregions": [
            "home"
          ],
          "lat": ${gpsData.latitude},
          "lon": ${gpsData.longitude},
          "m": 1,
          "t": "p",
          "tid": "L",
          "tst": $timestamp,
          "vac": ${gpsData.accuracy.toInt()},
          "vel": 0
        }
        """.trimIndent()
    }
    
    private fun getBatteryLevel(): Int {
        return 50 // 模拟电池电量
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}
