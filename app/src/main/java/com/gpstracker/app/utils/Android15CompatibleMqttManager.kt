package com.gpstracker.app.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.gpstracker.app.model.GpsData
import kotlinx.coroutines.*
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.*

/**
 * Android 15兼容的MQTT管理器
 * 解决BroadcastReceiver注册时的SecurityException问题
 */
class Android15CompatibleMqttManager(private val context: Context) {

    private var mqttClient: MqttAndroidClient? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logManager = LogManager(context)
    private val mqttReceiverManager = MqttBroadcastReceiverManager(context)
    
    init {
        // 应用Android 15兼容性修复
        Android15MqttFix.applyMqttLibraryFix()
        
        // 注册MQTT相关的BroadcastReceiver
        mqttReceiverManager.registerMqttReceivers()
    }
    
    // MQTT配置
    private val serverUri = "tcp://8.153.37.172:1883"
    private val topic = "owntracks/L"  // 使用具体的主题而不是通配符
    private val clientId = "gps_tracker_${UUID.randomUUID().toString().substring(0, 8)}"
    
    // 连接状态
    private var isConnecting = false
    private var lastConnectAttempt = 0L
    private val minConnectInterval = 5000L // 5秒最小连接间隔
    
    // 状态记录
    private var lastConnectionState = "未连接"
    private var lastError: Throwable? = null
    private var connectionAttempts = 0
    
    fun connect() {
        Log.d("Android15CompatibleMqttManager", "=== 开始MQTT连接流程 ===")
        Log.d("Android15CompatibleMqttManager", "服务器URI: $serverUri")
        Log.d("Android15CompatibleMqttManager", "客户端ID: $clientId")
        Log.d("Android15CompatibleMqttManager", "主题: $topic")
        logManager.saveLog("Android15CompatibleMqttManager", "DEBUG", "开始MQTT连接流程")
        
        try {
            // 检查网络连接
            if (!isNetworkAvailable()) {
                Log.w("Android15CompatibleMqttManager", "网络不可用，跳过MQTT连接")
                logManager.saveLog("Android15CompatibleMqttManager", "WARN", "网络不可用，跳过MQTT连接")
                return
            }
            
            // 防止频繁连接
            val currentTime = System.currentTimeMillis()
            if (isConnecting) {
                Log.d("Android15CompatibleMqttManager", "MQTT连接已在进行中，跳过")
                return
            }
            if ((currentTime - lastConnectAttempt) < minConnectInterval) {
                Log.d("Android15CompatibleMqttManager", "连接间隔太短，跳过")
                return
            }
            
            isConnecting = true
            lastConnectAttempt = currentTime
            connectionAttempts++
            lastConnectionState = "连接中"
            
            Log.d("Android15CompatibleMqttManager", "服务器URI: $serverUri")
            Log.d("Android15CompatibleMqttManager", "客户端ID: $clientId")
            
            // 创建MQTT客户端 - 使用兼容Android 15的版本
            mqttClient = MqttAndroidClient(context, serverUri, clientId, info.mqtt.android.service.Ack.AUTO_ACK)
            
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                // Android 15兼容性：禁用自动重连避免BroadcastReceiver问题
                isAutomaticReconnect = false
                connectionTimeout = 10
                keepAliveInterval = 60
                mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
                isHttpsHostnameVerificationEnabled = false
            }
            
            // 设置回调
            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w("Android15CompatibleMqttManager", "连接丢失", cause)
                    isConnecting = false
                    lastConnectionState = "连接丢失"
                }
                
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d("Android15CompatibleMqttManager", "收到MQTT消息: $topic -> ${message?.toString()}")
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("Android15CompatibleMqttManager", "消息发送完成")
                }
            })
            
            // 尝试连接
            Log.d("Android15CompatibleMqttManager", "开始尝试MQTT连接...")
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("Android15CompatibleMqttManager", "✅ MQTT连接成功！")
                    Log.d("Android15CompatibleMqttManager", "连接令牌: ${asyncActionToken?.messageId}")
                    lastConnectionState = "已连接"
                    lastError = null
                    isConnecting = false
                    logManager.saveLog("Android15CompatibleMqttManager", "INFO", "MQTT连接成功")
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("Android15CompatibleMqttManager", "❌ MQTT连接失败", exception)
                    Log.e("Android15CompatibleMqttManager", "失败原因: ${exception?.message}")
                    Log.e("Android15CompatibleMqttManager", "异常类型: ${exception?.javaClass?.simpleName}")
                    lastConnectionState = "连接失败"
                    lastError = exception
                    isConnecting = false
                    logManager.saveLog("Android15CompatibleMqttManager", "ERROR", "MQTT连接失败: ${exception?.message}")
                }
            })
            
        } catch (e: Exception) {
            Log.e("Android15CompatibleMqttManager", "MQTT连接异常", e)
            lastConnectionState = "连接异常"
            lastError = e
            isConnecting = false
        }
    }
    
    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient?.close()
            mqttClient = null
            lastConnectionState = "已断开"
            Log.d("Android15CompatibleMqttManager", "MQTT断开连接")
        } catch (e: Exception) {
            Log.e("Android15CompatibleMqttManager", "MQTT断开连接异常", e)
        }
    }
    
    fun publishLocation(gpsData: GpsData) {
        serviceScope.launch {
            try {
                Log.d("Android15CompatibleMqttManager", "=== 开始发布位置数据 ===")
                Log.d("Android15CompatibleMqttManager", "Android版本: ${Build.VERSION.SDK_INT}")
                Log.d("Android15CompatibleMqttManager", "位置数据: lat=${gpsData.latitude}, lon=${gpsData.longitude}, acc=${gpsData.accuracy}")
                
                if (!isNetworkAvailable()) {
                    Log.w("Android15CompatibleMqttManager", "网络不可用，跳过位置数据发送")
                    return@launch
                }
                
                Log.d("Android15CompatibleMqttManager", "MQTT客户端状态: ${mqttClient?.isConnected}")
                Log.d("Android15CompatibleMqttManager", "服务器URI: $serverUri")
                Log.d("Android15CompatibleMqttManager", "主题: $topic")
                
                if (mqttClient?.isConnected == true) {
                    val message = createLocationMessage(gpsData)
                    Log.d("Android15CompatibleMqttManager", "准备发送的消息: $message")
                    
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttMessage.qos = 1
                    
                    mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d("Android15CompatibleMqttManager", "✅ 位置数据发送成功: ${gpsData.latitude}, ${gpsData.longitude}")
                            Log.d("Android15CompatibleMqttManager", "发送到主题: $topic")
                        }
                        
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e("Android15CompatibleMqttManager", "❌ 位置数据发送失败", exception)
                            Log.e("Android15CompatibleMqttManager", "失败原因: ${exception?.message}")
                        }
                    })
                } else {
                    Log.w("Android15CompatibleMqttManager", "⚠️ MQTT未连接，尝试重新连接")
                    Log.w("Android15CompatibleMqttManager", "连接状态: ${getConnectionInfo()}")
                    connect()
                }
            } catch (e: Exception) {
                Log.e("Android15CompatibleMqttManager", "发送位置数据异常", e)
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
            batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            Log.w("Android15CompatibleMqttManager", "获取电池电量失败", e)
            100
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            Log.e("Android15CompatibleMqttManager", "检查网络状态失败", e)
            false
        }
    }
    
    fun isConnected(): Boolean {
        return mqttClient?.isConnected == true
    }
    
    fun getConnectionInfo(): String {
        return try {
            when {
                isConnecting -> "连接中..."
                isConnected() -> "已连接"
                else -> lastConnectionState
            }
        } catch (e: Exception) {
            "状态未知"
        }
    }
    
    fun getDetailedState(): String {
        return try {
            val state = StringBuilder()
            state.appendLine("=== Android 15兼容MQTT详细状态 ===")
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
            state.appendLine("兼容性: 禁用自动重连避免BroadcastReceiver问题")
            state.toString()
        } catch (e: Exception) {
            Log.e("Android15CompatibleMqttManager", "获取详细状态失败", e)
            "状态获取失败: ${e.message}"
        }
    }
    
    /**
     * 测试MQTT连接并发送测试消息
     */
    fun testConnection() {
        serviceScope.launch {
            try {
                Log.d("Android15CompatibleMqttManager", "=== 开始测试MQTT连接 ===")
                
                if (!isNetworkAvailable()) {
                    Log.w("Android15CompatibleMqttManager", "网络不可用，无法测试连接")
                    return@launch
                }
                
                // 如果未连接，先连接
                if (!isConnected()) {
                    Log.d("Android15CompatibleMqttManager", "未连接，先建立连接...")
                    connect()
                    
                    // 等待连接建立
                    var retryCount = 0
                    val maxRetries = 10
                    while (retryCount < maxRetries && !isConnected()) {
                        Log.d("Android15CompatibleMqttManager", "等待连接建立... (${retryCount + 1}/$maxRetries)")
                        delay(1000)
                        retryCount++
                    }
                }
                
                if (isConnected()) {
                    Log.d("Android15CompatibleMqttManager", "连接已建立，发送测试消息...")
                    
                    val testMessage = """
                    {
                      "_type": "test",
                      "message": "MQTT连接测试",
                      "timestamp": ${System.currentTimeMillis() / 1000},
                      "clientId": "$clientId"
                    }
                    """.trimIndent()
                    
                    val mqttMessage = MqttMessage(testMessage.toByteArray())
                    mqttMessage.qos = 1
                    
                    mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d("Android15CompatibleMqttManager", "✅ 测试消息发送成功！")
                            Log.d("Android15CompatibleMqttManager", "消息ID: ${asyncActionToken?.messageId}")
                        }
                        
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e("Android15CompatibleMqttManager", "❌ 测试消息发送失败", exception)
                            Log.e("Android15CompatibleMqttManager", "失败原因: ${exception?.message}")
                        }
                    })
                } else {
                    Log.w("Android15CompatibleMqttManager", "⚠️ 连接建立失败，无法发送测试消息")
                }
                
            } catch (e: Exception) {
                Log.e("Android15CompatibleMqttManager", "测试连接异常", e)
            }
        }
    }
    
    fun cleanup() {
        try {
            disconnect()
            mqttReceiverManager.unregisterAllReceivers()
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e("Android15CompatibleMqttManager", "清理资源异常", e)
        }
    }
}
