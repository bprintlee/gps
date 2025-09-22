package com.gpstracker.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

/**
 * MQTT BroadcastReceiver管理器
 * 专门处理MQTT相关的广播接收器注册，确保Android 15兼容性
 */
class MqttBroadcastReceiverManager(private val context: Context) {
    
    private val TAG = "MqttBroadcastReceiverManager"
    private val registeredReceivers = mutableListOf<BroadcastReceiver>()
    
    /**
     * 注册MQTT相关的BroadcastReceiver
     * 自动处理Android 15的导出标志要求
     */
    fun registerMqttReceivers() {
        try {
            Log.d(TAG, "开始注册MQTT相关BroadcastReceiver")
            
            // 注册MQTT连接状态接收器
            registerMqttConnectionReceiver()
            
            // 注册MQTT消息接收器
            registerMqttMessageReceiver()
            
            // 注册网络状态接收器
            registerNetworkStateReceiver()
            
            Log.d(TAG, "MQTT BroadcastReceiver注册完成")
        } catch (e: Exception) {
            Log.e(TAG, "注册MQTT BroadcastReceiver失败", e)
        }
    }
    
    /**
     * 注册MQTT连接状态接收器
     */
    private fun registerMqttConnectionReceiver() {
        val connectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "收到MQTT连接状态广播: ${intent?.action}")
                // 处理MQTT连接状态变化
                when (intent?.action) {
                    "org.eclipse.paho.android.service.MQTT_CONNECTED" -> {
                        Log.d(TAG, "MQTT已连接")
                    }
                    "org.eclipse.paho.android.service.MQTT_DISCONNECTED" -> {
                        Log.d(TAG, "MQTT已断开")
                    }
                    "org.eclipse.paho.android.service.MQTT_CONNECTING" -> {
                        Log.d(TAG, "MQTT连接中")
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction("org.eclipse.paho.android.service.MQTT_CONNECTED")
            addAction("org.eclipse.paho.android.service.MQTT_DISCONNECTED")
            addAction("org.eclipse.paho.android.service.MQTT_CONNECTING")
        }
        
        registerReceiverWithCompatibility(connectionReceiver, filter)
        registeredReceivers.add(connectionReceiver)
    }
    
    /**
     * 注册MQTT消息接收器
     */
    private fun registerMqttMessageReceiver() {
        val messageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "收到MQTT消息广播: ${intent?.action}")
                // 处理MQTT消息
                when (intent?.action) {
                    "org.eclipse.paho.android.service.MQTT_MESSAGE_ARRIVED" -> {
                        Log.d(TAG, "收到MQTT消息")
                    }
                    "org.eclipse.paho.android.service.MQTT_MESSAGE_DELIVERED" -> {
                        Log.d(TAG, "MQTT消息已送达")
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction("org.eclipse.paho.android.service.MQTT_MESSAGE_ARRIVED")
            addAction("org.eclipse.paho.android.service.MQTT_MESSAGE_DELIVERED")
        }
        
        registerReceiverWithCompatibility(messageReceiver, filter)
        registeredReceivers.add(messageReceiver)
    }
    
    /**
     * 注册网络状态接收器
     */
    private fun registerNetworkStateReceiver() {
        val networkReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "收到网络状态广播: ${intent?.action}")
                // 处理网络状态变化
                when (intent?.action) {
                    android.net.ConnectivityManager.CONNECTIVITY_ACTION -> {
                        Log.d(TAG, "网络连接状态变化")
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION)
        }
        
        registerReceiverWithCompatibility(networkReceiver, filter)
        registeredReceivers.add(networkReceiver)
    }
    
    /**
     * 兼容Android 15的registerReceiver方法
     */
    private fun registerReceiverWithCompatibility(
        receiver: BroadcastReceiver,
        filter: IntentFilter
    ) {
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                // Android 13+ 需要指定导出标志
                Log.d(TAG, "使用Android 13+兼容的registerReceiver")
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                // Android 13以下使用传统方法
                Log.d(TAG, "使用传统registerReceiver")
                context.registerReceiver(receiver, filter)
            }
            Log.d(TAG, "BroadcastReceiver注册成功")
        } catch (e: Exception) {
            Log.e(TAG, "注册BroadcastReceiver失败", e)
        }
    }
    
    /**
     * 注销所有注册的BroadcastReceiver
     */
    fun unregisterAllReceivers() {
        try {
            Log.d(TAG, "开始注销所有MQTT BroadcastReceiver")
            registeredReceivers.forEach { receiver ->
                try {
                    context.unregisterReceiver(receiver)
                    Log.d(TAG, "BroadcastReceiver注销成功")
                } catch (e: Exception) {
                    Log.e(TAG, "注销BroadcastReceiver失败", e)
                }
            }
            registeredReceivers.clear()
            Log.d(TAG, "所有MQTT BroadcastReceiver注销完成")
        } catch (e: Exception) {
            Log.e(TAG, "注销BroadcastReceiver异常", e)
        }
    }
    
    /**
     * 检查是否需要Android 15兼容性处理
     */
    fun needsAndroid15Compatibility(): Boolean {
        return Build.VERSION.SDK_INT >= 33
    }
}
