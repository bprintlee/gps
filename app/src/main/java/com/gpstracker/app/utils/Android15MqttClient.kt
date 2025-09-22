package com.gpstracker.app.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

/**
 * Android 15兼容的MQTT客户端包装器
 * 解决BroadcastReceiver注册时的RECEIVER_EXPORTED/RECEIVER_NOT_EXPORTED问题
 */
class Android15MqttClient(
    context: Context,
    serverUri: String,
    clientId: String
) : MqttAndroidClient(context, serverUri, clientId) {
    
    companion object {
        private const val TAG = "Android15MqttClient"
    }
    
    private val appContext = context.applicationContext
    
    override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: IntentFilter?): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 需要指定RECEIVER_EXPORTED或RECEIVER_NOT_EXPORTED
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                // Android 12及以下使用传统方式
                appContext.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "注册BroadcastReceiver失败", e)
            null
        }
    }
    
    override fun unregisterReceiver(receiver: android.content.BroadcastReceiver?) {
        try {
            appContext.unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.e(TAG, "注销BroadcastReceiver失败", e)
        }
    }
}
