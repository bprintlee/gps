package com.gpstracker.app.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.gpstracker.app.model.GpsData
import kotlinx.coroutines.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.*

class MqttManager(private val context: Context) {
    
    private var mqttClient: MqttAndroidClient? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logManager = LogManager(context)
    
    // Android 15兼容的MQTT管理器
    private val android15CompatibleManager = if (Build.VERSION.SDK_INT >= 35) {
        Android15CompatibleMqttManager(context)
    } else null
    
    // MQTT配置
    private val serverUri = "tcp://8.153.37.172:1883"
    private val topic = "owntracks/user/bprint"
    private val clientId = "gps_tracker_${UUID.randomUUID().toString().substring(0, 8)}"
    
    // 连接状态
    private var isConnecting = false
    private var lastConnectAttempt = 0L
    private val minConnectInterval = 5000L // 5秒最小连接间隔
    
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
        Log.d("MqttManager", "=== 开始MQTT连接流程 ===")
        logManager.saveLog("MqttManager", "DEBUG", "开始MQTT连接流程")
        
        try {
            // Android 15兼容性处理
            if (Build.VERSION.SDK_INT >= 35) {
                Log.w("MqttManager", "检测到Android 15，使用兼容的MQTT管理器")
                android15CompatibleManager?.connect()
                return
            }
            
            // 检查网络连接
            if (!isNetworkAvailable()) {
                Log.w("MqttManager", "网络不可用，跳过MQTT连接")
                logManager.saveLog("MqttManager", "WARN", "网络不可用，跳过MQTT连接")
                return
            }
            Log.d("MqttManager", "网络状态检查通过")
            logManager.saveLog("MqttManager", "DEBUG", "网络状态检查通过")
            
            // 防止频繁连接
            val currentTime = System.currentTimeMillis()
            if (isConnecting) {
                Log.d("MqttManager", "MQTT连接已在进行中，跳过")
                return
            }
            if ((currentTime - lastConnectAttempt) < minConnectInterval) {
                Log.d("MqttManager", "连接间隔太短，跳过。距离上次连接: ${currentTime - lastConnectAttempt}ms")
                return
            }
            
            isConnecting = true
            lastConnectAttempt = currentTime
            connectionAttempts++
            lastConnectionState = "连接中"
            Log.d("MqttManager", "设置连接状态: isConnecting=true, 尝试次数: $connectionAttempts")
            
            Log.d("MqttManager", "开始创建MQTT客户端")
            Log.d("MqttManager", "服务器URI: $serverUri")
            Log.d("MqttManager", "客户端ID: $clientId")
            
            mqttClient = MqttAndroidClient(context, serverUri, clientId)
            Log.d("MqttManager", "MQTT客户端创建成功")
            
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                isAutomaticReconnect = true
                connectionTimeout = 10
                keepAliveInterval = 60
                // Android 14+ 兼容性设置
                mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
                isHttpsHostnameVerificationEnabled = false
            }
            Log.d("MqttManager", "MQTT连接选项配置完成")
            
            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w("MqttManager", "=== MQTT连接丢失 ===", cause)
                    isConnecting = false
                    lastConnectionState = "连接丢失"
                    lastError = cause
                    
                    cause?.let {
                        Log.e("MqttManager", "连接丢失原因: ${it.message}", it)
                        Log.e("MqttManager", "连接丢失堆栈: ${it.stackTraceToString()}")
                    }
                    
                    logManager.saveLog("MqttManager", "WARN", "MQTT连接丢失: ${cause?.message}")
                    
                    // 启动自动重连
                    if (isAutoReconnectEnabled) {
                        startAutoReconnect()
                    }
                }
                
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d("MqttManager", "收到MQTT消息: $topic -> ${message?.toString()}")
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MqttManager", "MQTT消息发送完成")
                }
            })
            Log.d("MqttManager", "MQTT回调设置完成")
            
            Log.d("MqttManager", "开始MQTT连接...")
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MqttManager", "=== MQTT连接成功 ===")
                    Log.d("MqttManager", "连接令牌: $asyncActionToken")
                    lastConnectionState = "已连接"
                    lastError = null
                    isConnecting = false
                    
                    // 连接成功，重置重连计数器
                    resetReconnectAttempts()
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MqttManager", "=== MQTT连接失败 ===", exception)
                    Log.e("MqttManager", "失败令牌: $asyncActionToken")
                    lastConnectionState = "连接失败"
                    lastError = exception
                    exception?.let {
                        Log.e("MqttManager", "失败原因: ${it.message}", it)
                        Log.e("MqttManager", "失败堆栈: ${it.stackTraceToString()}")
                        Log.e("MqttManager", "失败类型: ${it.javaClass.simpleName}")
                    }
                    isConnecting = false
                    // 连接失败时不要抛出异常，避免应用崩溃
                }
            })
            Log.d("MqttManager", "MQTT连接请求已发送")
            
        } catch (e: Exception) {
            Log.e("MqttManager", "=== MQTT连接异常 ===", e)
            Log.e("MqttManager", "异常类型: ${e.javaClass.simpleName}")
            Log.e("MqttManager", "异常消息: ${e.message}")
            Log.e("MqttManager", "异常堆栈: ${e.stackTraceToString()}")
            logManager.saveLog("MqttManager", "ERROR", "MQTT连接异常: ${e.message}", e)
            lastConnectionState = "连接异常"
            lastError = e
            isConnecting = false
            // 捕获所有异常，避免应用崩溃
        }
    }
    
    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    fun disconnect() {
        try {
            isConnecting = false
            mqttClient?.disconnect()
            mqttClient?.close()
            mqttClient = null
            Log.d("MqttManager", "MQTT连接已断开")
        } catch (e: Exception) {
            Log.e("MqttManager", "MQTT断开连接异常", e)
        }
    }
    
    fun publishLocation(gpsData: GpsData) {
        serviceScope.launch {
            try {
                Log.d("MqttManager", "=== 开始发布位置数据 ===")
                Log.d("MqttManager", "Android版本: ${Build.VERSION.SDK_INT}")
                Log.d("MqttManager", "位置数据: lat=${gpsData.latitude}, lon=${gpsData.longitude}, acc=${gpsData.accuracy}")
                
                // Android 15兼容性处理
                if (Build.VERSION.SDK_INT >= 35) {
                    Log.w("MqttManager", "检测到Android 15，使用兼容的MQTT管理器发布位置")
                    android15CompatibleManager?.publishLocation(gpsData)
                    return@launch
                }
                
                if (!isNetworkAvailable()) {
                    Log.w("MqttManager", "网络不可用，跳过位置数据发送")
                    return@launch
                }
                
                Log.d("MqttManager", "MQTT客户端状态: ${mqttClient?.isConnected}")
                Log.d("MqttManager", "服务器URI: $serverUri")
                Log.d("MqttManager", "主题: $topic")
                
                if (mqttClient?.isConnected == true) {
                    val message = createLocationMessage(gpsData)
                    Log.d("MqttManager", "准备发送的消息: $message")
                    
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttMessage.qos = 1
                    
                    mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d("MqttManager", "✅ 位置数据发送成功: ${gpsData.latitude}, ${gpsData.longitude}")
                            Log.d("MqttManager", "发送到主题: $topic")
                        }
                        
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e("MqttManager", "❌ 位置数据发送失败", exception)
                            Log.e("MqttManager", "失败原因: ${exception?.message}")
                            // 发送失败时尝试重新连接
                            if (!isConnected()) {
                                Log.w("MqttManager", "尝试重新连接MQTT")
                                connect()
                            }
                        }
                    })
                } else {
                    Log.w("MqttManager", "⚠️ MQTT未连接，尝试重新连接")
                    Log.w("MqttManager", "连接状态: ${getConnectionInfo()}")
                    connect()
                }
            } catch (e: Exception) {
                Log.e("MqttManager", "发送位置数据异常", e)
            }
        }
    }
    
    private fun createLocationMessage(gpsData: GpsData): String {
        val timestamp = gpsData.timestamp / 1000 // 转换为秒
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
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            batteryLevel
        } catch (e: Exception) {
            50 // 默认值
        }
    }
    
    fun isConnected(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= 35) {
                android15CompatibleManager?.isConnected() == true
            } else {
                mqttClient?.isConnected == true
            }
        } catch (e: Exception) {
            Log.e("MqttManager", "检查连接状态异常", e)
            false
        }
    }
    
    /**
     * 获取连接状态信息
     */
    fun getConnectionInfo(): String {
        return try {
            if (Build.VERSION.SDK_INT >= 35) {
                android15CompatibleManager?.getConnectionInfo() ?: "Android 15兼容模式"
            } else {
                when {
                    isConnecting -> "连接中..."
                    isConnected() -> "已连接"
                    else -> lastConnectionState
                }
            }
        } catch (e: Exception) {
            "状态未知"
        }
    }
    
    /**
     * 获取详细的MQTT状态信息（用于崩溃日志）
     */
    fun getDetailedState(): String {
        return try {
            if (Build.VERSION.SDK_INT >= 35) {
                android15CompatibleManager?.getDetailedState() ?: "Android 15兼容模式状态获取失败"
            } else {
                val state = StringBuilder()
                state.appendLine("=== MQTT详细状态 ===")
                state.appendLine("Android版本: ${Build.VERSION.SDK_INT}")
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
                state.toString()
            }
        } catch (e: Exception) {
            Log.e("MqttManager", "获取详细状态失败", e)
            "状态获取失败: ${e.message}"
        }
    }
    
    
    /**
     * 清理资源
     */
    // MQTT测试功能已删除
    
    fun cleanup() {
        try {
            // 停止自动重连
            stopAutoReconnect()
            
            if (Build.VERSION.SDK_INT >= 35) {
                android15CompatibleManager?.cleanup()
            } else {
                disconnect()
            }
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e("MqttManager", "清理资源异常", e)
        }
    }
    
    /**
     * 启动自动重连
     */
    private fun startAutoReconnect() {
        if (reconnectJob?.isActive == true) {
            Log.d("MqttManager", "自动重连已在进行中，跳过")
            return
        }
        
        if (reconnectAttempts >= maxReconnectAttempts) {
            Log.w("MqttManager", "已达到最大重连次数 ($maxReconnectAttempts)，停止自动重连")
            logManager.saveLog("MqttManager", "WARN", "已达到最大重连次数，停止自动重连")
            return
        }
        
        reconnectAttempts++
        Log.d("MqttManager", "启动自动重连 (第 $reconnectAttempts/$maxReconnectAttempts 次)")
        logManager.saveLog("MqttManager", "INFO", "启动自动重连 (第 $reconnectAttempts/$maxReconnectAttempts 次)")
        
        reconnectJob = serviceScope.launch {
            try {
                delay(reconnectDelayMs)
                
                if (isNetworkAvailable()) {
                    Log.d("MqttManager", "网络可用，尝试重连...")
                    connect()
                } else {
                    Log.w("MqttManager", "网络不可用，延迟重连")
                    // 网络不可用时，延长重连间隔
                    delay(reconnectDelayMs * 2)
                    startAutoReconnect()
                }
            } catch (e: Exception) {
                Log.e("MqttManager", "自动重连异常", e)
                logManager.saveLog("MqttManager", "ERROR", "自动重连异常: ${e.message}")
                
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
        Log.d("MqttManager", "已停止自动重连")
    }
    
    /**
     * 重置重连计数器（连接成功时调用）
     */
    private fun resetReconnectAttempts() {
        reconnectAttempts = 0
        reconnectJob?.cancel()
        reconnectJob = null
        Log.d("MqttManager", "连接成功，重置重连计数器")
    }
    
    /**
     * 设置自动重连开关
     */
    fun setAutoReconnectEnabled(enabled: Boolean) {
        isAutoReconnectEnabled = enabled
        Log.d("MqttManager", "自动重连已${if (enabled) "启用" else "禁用"}")
        
        if (!enabled) {
            stopAutoReconnect()
        }
    }
}
