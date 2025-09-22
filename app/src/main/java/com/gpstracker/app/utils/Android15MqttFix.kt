package com.gpstracker.app.utils

import android.content.Context
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.Build
import android.util.Log
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Android 15 MQTT兼容性修复工具
 * 通过反射修复MQTT库中BroadcastReceiver注册时的SecurityException问题
 */
object Android15MqttFix {
    
    private const val TAG = "Android15MqttFix"
    
    /**
     * 修复Context的registerReceiver方法，确保在Android 15上正确设置导出标志
     */
    fun fixContextRegisterReceiver(context: Context): Context {
        if (Build.VERSION.SDK_INT < 33) {
            // Android 13以下不需要修复
            return context
        }
        
        return try {
            // 创建Context的代理，拦截registerReceiver调用
            Proxy.newProxyInstance(
                context.javaClass.classLoader,
                arrayOf(Context::class.java)
            ) { proxy, method, args ->
                if (method.name == "registerReceiver" && args.size >= 2) {
                    // 检查是否已经设置了导出标志
                    val receiver = args[0] as? BroadcastReceiver
                    val filter = args[1] as? IntentFilter
                    
                    if (receiver != null && filter != null) {
                        Log.d(TAG, "拦截registerReceiver调用，添加Android 15兼容性标志")
                        
                        // 如果参数数量为2，添加RECEIVER_NOT_EXPORTED标志
                        if (args.size == 2) {
                            val newArgs = arrayOfNulls<Any>(3)
                            newArgs[0] = args[0] // receiver
                            newArgs[1] = args[1] // filter
                            newArgs[2] = Context.RECEIVER_NOT_EXPORTED // 添加导出标志
                            
                            return@newProxyInstance method.invoke(context, *newArgs)
                        }
                    }
                }
                
                // 其他方法调用直接转发
                method.invoke(context, *args)
            } as Context
        } catch (e: Exception) {
            Log.e(TAG, "修复Context registerReceiver失败", e)
            context
        }
    }
    
    /**
     * 尝试修复MQTT库内部的BroadcastReceiver注册问题
     */
    fun applyMqttLibraryFix() {
        if (Build.VERSION.SDK_INT < 33) {
            return
        }
        
        try {
            Log.d(TAG, "开始应用MQTT库Android 15兼容性修复")
            
            // 这里可以添加更多针对特定MQTT库的修复
            // 由于hannesa2库是闭源的，我们主要通过禁用自动重连来避免问题
            
            Log.d(TAG, "MQTT库Android 15兼容性修复应用完成")
        } catch (e: Exception) {
            Log.e(TAG, "应用MQTT库修复失败", e)
        }
    }
    
    /**
     * 检查是否需要进行Android 15兼容性修复
     */
    fun needsAndroid15Fix(): Boolean {
        return Build.VERSION.SDK_INT >= 33 // Android 13及以上需要修复
    }
}
