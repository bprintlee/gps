package com.gpstracker.app.utils

import android.content.Context
import android.util.Log
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
    
    fun connect() {
        try {
            mqttClient = MqttAndroidClient(context, serverUri, clientId)
            
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                isAutomaticReconnect = true
                connectionTimeout = 10
                keepAliveInterval = 60
            }
            
            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w("MqttManager", "MQTT连接丢失", cause)
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
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MqttManager", "MQTT连接失败", exception)
                }
            })
            
        } catch (e: Exception) {
            Log.e("MqttManager", "MQTT连接异常", e)
        }
    }
    
    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient?.close()
            Log.d("MqttManager", "MQTT连接已断开")
        } catch (e: Exception) {
            Log.e("MqttManager", "MQTT断开连接异常", e)
        }
    }
    
    fun publishLocation(gpsData: GpsData) {
        serviceScope.launch {
            try {
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
                        }
                    })
                } else {
                    Log.w("MqttManager", "MQTT未连接，无法发送位置数据")
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
        return mqttClient?.isConnected == true
    }
}
