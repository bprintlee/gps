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
    
    // MQTT配置
    private val serverUri = "tcp://8.153.37.172:1883"
    private val topic = "owntracks/#"
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
        Log.d("MqttManager", "=== 开始MQTT连接流程 ===")
        logManager.saveLog("MqttManager", "DEBUG", "开始MQTT连接流程")
        
        try {
            // Android 15兼容性检查 (API 35)
            if (Build.VERSION.SDK_INT >= 35) {
                Log.w("MqttManager", "检测到Android 15 (API ${Build.VERSION.SDK_INT})，使用兼容性处理")
                connectWithAndroid15Compatibility()
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
                    cause?.let {
                        Log.e("MqttManager", "连接丢失原因: ${it.message}", it)
                        Log.e("MqttManager", "连接丢失堆栈: ${it.stackTraceToString()}")
                    }
                    // 尝试重新连接
                    serviceScope.launch {
                        delay(5000) // 5秒后重试
                        if (!isConnected()) {
                            Log.d("MqttManager", "尝试重新连接MQTT")
                            connect()
                        }
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
                
                // Android 15兼容性处理 - 使用兼容的连接方式
                if (Build.VERSION.SDK_INT >= 35) {
                    Log.w("MqttManager", "Android 15兼容性：使用兼容的MQTT连接方式")
                    publishLocationWithAndroid15Compatibility(gpsData)
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
    
    /**
     * Android 15兼容的位置数据发布方法
     * 使用简化的连接方式避免BroadcastReceiver注册问题
     */
    private fun publishLocationWithAndroid15Compatibility(gpsData: GpsData) {
        try {
            Log.d("MqttManager", "使用Android 15兼容方式发布位置数据")
            
            if (!isNetworkAvailable()) {
                Log.w("MqttManager", "网络不可用，跳过位置数据发送")
                return
            }
            
            // 如果客户端不存在或未连接，创建新的连接
            if (mqttClient == null || !mqttClient!!.isConnected) {
                Log.d("MqttManager", "创建Android 15兼容的MQTT客户端")
                createAndroid15CompatibleClient()
            }
            
            // 等待连接建立
            var retryCount = 0
            val maxRetries = 3
            while (retryCount < maxRetries && (mqttClient == null || !mqttClient!!.isConnected)) {
                Log.d("MqttManager", "等待MQTT连接建立... (尝试 ${retryCount + 1}/$maxRetries)")
                Thread.sleep(1000)
                retryCount++
            }
            
            if (mqttClient?.isConnected == true) {
                val message = createLocationMessage(gpsData)
                Log.d("MqttManager", "准备发送的消息: $message")
                
                val mqttMessage = MqttMessage(message.toByteArray())
                mqttMessage.qos = 1
                
                mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("MqttManager", "✅ Android 15兼容模式：位置数据发送成功")
                        Log.d("MqttManager", "发送到主题: $topic")
                        Log.d("MqttManager", "位置: ${gpsData.latitude}, ${gpsData.longitude}")
                    }
                    
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MqttManager", "❌ Android 15兼容模式：位置数据发送失败", exception)
                        Log.e("MqttManager", "失败原因: ${exception?.message}")
                    }
                })
            } else {
                Log.w("MqttManager", "⚠️ Android 15兼容模式：MQTT连接失败，无法发送位置数据")
            }
            
        } catch (e: Exception) {
            Log.e("MqttManager", "Android 15兼容模式：发送位置数据异常", e)
        }
    }
    
    /**
     * 创建Android 15兼容的MQTT客户端
     */
    private fun createAndroid15CompatibleClient() {
        try {
            Log.d("MqttManager", "创建Android 15兼容的MQTT客户端")
            
            mqttClient = MqttAndroidClient(context, serverUri, clientId)
            
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                isAutomaticReconnect = false // 禁用自动重连避免BroadcastReceiver问题
                connectionTimeout = 10
                keepAliveInterval = 60
                mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
                isHttpsHostnameVerificationEnabled = false
            }
            
            // 简化的回调，不处理连接丢失重连
            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w("MqttManager", "Android 15兼容模式：连接丢失", cause)
                }
                
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d("MqttManager", "Android 15兼容模式：收到消息: $topic -> ${message?.toString()}")
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MqttManager", "Android 15兼容模式：消息发送完成")
                }
            })
            
            // 尝试连接
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MqttManager", "✅ Android 15兼容模式：MQTT连接成功")
                    lastConnectionState = "已连接"
                    lastError = null
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MqttManager", "❌ Android 15兼容模式：MQTT连接失败", exception)
                    lastConnectionState = "连接失败"
                    lastError = exception
                }
            })
            
        } catch (e: Exception) {
            Log.e("MqttManager", "创建Android 15兼容MQTT客户端异常", e)
            lastConnectionState = "创建客户端异常"
            lastError = e
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
            mqttClient?.isConnected == true
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
            when {
                Build.VERSION.SDK_INT >= 35 -> "Android 15兼容模式"
                isConnecting -> "连接中..."
                isConnected() -> "已连接"
                else -> lastConnectionState
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
            state.toString()
        } catch (e: Exception) {
            Log.e("MqttManager", "获取详细状态失败", e)
            "状态获取失败: ${e.message}"
        }
    }
    
    
    /**
     * Android 15兼容性连接方法
     * 使用简化的连接方式避免BroadcastReceiver注册问题
     */
    private fun connectWithAndroid15Compatibility() {
        try {
            Log.d("MqttManager", "使用Android 15兼容性连接方式")
            
            // 检查网络连接
            if (!isNetworkAvailable()) {
                Log.w("MqttManager", "网络不可用，跳过MQTT连接")
                return
            }
            
            // 防止频繁连接
            val currentTime = System.currentTimeMillis()
            if (isConnecting) {
                Log.d("MqttManager", "MQTT连接已在进行中，跳过")
                return
            }
            if ((currentTime - lastConnectAttempt) < minConnectInterval) {
                Log.d("MqttManager", "连接间隔太短，跳过")
                return
            }
            
            isConnecting = true
            lastConnectAttempt = currentTime
            connectionAttempts++
            lastConnectionState = "连接中"
            
            // 创建MQTT客户端
            mqttClient = MqttAndroidClient(context, serverUri, clientId)
            
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                isAutomaticReconnect = false // 禁用自动重连避免BroadcastReceiver问题
                connectionTimeout = 10
                keepAliveInterval = 60
                mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
                isHttpsHostnameVerificationEnabled = false
            }
            
            // 简化的回调，不处理连接丢失重连
            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w("MqttManager", "Android 15兼容模式：连接丢失", cause)
                    isConnecting = false
                    lastConnectionState = "连接丢失"
                }
                
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d("MqttManager", "收到MQTT消息: $topic -> ${message?.toString()}")
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MqttManager", "MQTT消息发送完成")
                }
            })
            
            // 尝试连接
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MqttManager", "Android 15兼容模式：MQTT连接成功")
                    lastConnectionState = "已连接"
                    lastError = null
                    isConnecting = false
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MqttManager", "Android 15兼容模式：MQTT连接失败", exception)
                    lastConnectionState = "连接失败"
                    lastError = exception
                    isConnecting = false
                }
            })
            
        } catch (e: Exception) {
            Log.e("MqttManager", "Android 15兼容性连接异常", e)
            lastConnectionState = "连接异常"
            lastError = e
            isConnecting = false
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        try {
            disconnect()
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e("MqttManager", "清理资源异常", e)
        }
    }
}
