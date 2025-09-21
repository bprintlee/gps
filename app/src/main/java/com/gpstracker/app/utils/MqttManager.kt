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
    
    // MQTT配置
    private val serverUri = "tcp://8.153.37.172:1883"
    private val topic = "owntracks/L"
    private val clientId = "gps_tracker_${UUID.randomUUID().toString().substring(0, 8)}"
    
    // 连接状态
    private var isConnecting = false
    private var lastConnectAttempt = 0L
    private val minConnectInterval = 5000L // 5秒最小连接间隔
    
    fun connect() {
        // 检查网络连接
        if (!isNetworkAvailable()) {
            Log.w("MqttManager", "网络不可用，跳过MQTT连接")
            return
        }
        
        // 防止频繁连接
        val currentTime = System.currentTimeMillis()
        if (isConnecting || (currentTime - lastConnectAttempt) < minConnectInterval) {
            Log.d("MqttManager", "MQTT连接已在进行中或间隔太短，跳过")
            return
        }
        
        isConnecting = true
        lastConnectAttempt = currentTime
        
        try {
            mqttClient = MqttAndroidClient(context, serverUri, clientId)
            
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                isAutomaticReconnect = true
                connectionTimeout = 10
                keepAliveInterval = 60
                // Android 14+ 兼容性设置
                mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
                isHttpsHostnameVerificationEnabled = false
            }
            
            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w("MqttManager", "MQTT连接丢失", cause)
                    isConnecting = false
                    // 尝试重新连接
                    serviceScope.launch {
                        delay(5000) // 5秒后重试
                        if (!isConnected()) {
                            connect()
                        }
                    }
                }
                
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d("MqttManager", "收到消息: $topic - ${message?.toString()}")
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MqttManager", "消息发送完成")
                }
            })
            
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MqttManager", "MQTT连接成功")
                    isConnecting = false
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MqttManager", "MQTT连接失败", exception)
                    isConnecting = false
                    // 连接失败时不要抛出异常，避免应用崩溃
                }
            })
            
        } catch (e: Exception) {
            Log.e("MqttManager", "MQTT连接异常", e)
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
                if (!isNetworkAvailable()) {
                    Log.w("MqttManager", "网络不可用，跳过位置数据发送")
                    return@launch
                }
                
                if (mqttClient?.isConnected == true) {
                    val message = createLocationMessage(gpsData)
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttMessage.qos = 1
                    
                    mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d("MqttManager", "位置数据发送成功: ${gpsData.latitude}, ${gpsData.longitude}")
                        }
                        
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e("MqttManager", "位置数据发送失败", exception)
                            // 发送失败时尝试重新连接
                            if (!isConnected()) {
                                connect()
                            }
                        }
                    })
                } else {
                    Log.w("MqttManager", "MQTT未连接，尝试重新连接")
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
                isConnecting -> "连接中..."
                isConnected() -> "已连接"
                else -> "未连接"
            }
        } catch (e: Exception) {
            "状态未知"
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
