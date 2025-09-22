package com.gpstracker.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.gpstracker.app.utils.CrashHandler
import com.gpstracker.app.utils.EnhancedCrashHandler

class GPSTrackerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("GPSTrackerApp", "=== Application onCreate ===")
        
        try {
            // 初始化增强版崩溃处理器
            EnhancedCrashHandler.getInstance(this)
            Log.d("GPSTrackerApp", "增强版崩溃处理器初始化完成")
            
            // 清理旧的崩溃日志
            CrashHandler(this).cleanupOldLogs()
            Log.d("GPSTrackerApp", "旧崩溃日志清理完成")
            
            // 确保应用上下文正确初始化
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            Log.d("GPSTrackerApp", "包信息加载成功: ${packageInfo.versionName}")
            
            // Android 15兼容性处理已移至MqttManager
            
            Log.d("GPSTrackerApp", "=== Application初始化完成 ===")
        } catch (e: Exception) {
            Log.e("GPSTrackerApp", "=== Application初始化失败 ===", e)
            Log.e("GPSTrackerApp", "异常类型: ${e.javaClass.simpleName}")
            Log.e("GPSTrackerApp", "异常消息: ${e.message}")
            Log.e("GPSTrackerApp", "异常堆栈: ${e.stackTraceToString()}")
        }
    }
    
}
