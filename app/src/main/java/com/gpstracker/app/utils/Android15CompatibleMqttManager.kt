package com.gpstracker.app.utils

import android.content.Context
import android.util.Log
import com.gpstracker.app.model.GpsData

/**
 * Android 15兼容的MQTT管理器
 * 使用纯Java MQTT客户端避免BroadcastReceiver注册问题
 */
class Android15CompatibleMqttManager(private val context: Context) {

    // 使用纯Java MQTT客户端避免BroadcastReceiver问题
    private val mqttWorkaround = Android15MqttWorkaround(context)
    
    init {
        Log.d("Android15CompatibleMqttManager", "使用Android 15 MQTT解决方案")
    }
    
    fun connect() {
        Log.d("Android15CompatibleMqttManager", "=== 开始Android 15兼容的MQTT连接 ===")
        mqttWorkaround.connect()
    }
    
    fun publishLocation(gpsData: GpsData) {
        Log.d("Android15CompatibleMqttManager", "发布位置数据到Android 15兼容的MQTT客户端")
        mqttWorkaround.publishLocation(gpsData)
    }
    
    fun isConnected(): Boolean {
        return mqttWorkaround.isConnected()
    }
    
    fun getConnectionInfo(): String {
        return mqttWorkaround.getConnectionInfo()
    }
    
    fun getDetailedState(): String {
        return mqttWorkaround.getDetailedState()
    }
    
    // MQTT测试功能已删除
    
    fun cleanup() {
        Log.d("Android15CompatibleMqttManager", "清理Android 15兼容的MQTT资源")
        mqttWorkaround.cleanup()
    }
}