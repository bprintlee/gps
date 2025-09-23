package com.gpstracker.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.gpstracker.app.utils.CrashHandler
import com.gpstracker.app.utils.EnhancedCrashHandler
import com.gpstracker.app.utils.GlobalBroadcastReceiverFix
import com.gpstracker.app.utils.PackageInfoHelper

class GPSTrackerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("GPSTrackerApp", "=== Application onCreate ===")
        
        try {
            // 应用全局BroadcastReceiver修复（Android 15兼容性）
            GlobalBroadcastReceiverFix.applyGlobalFix(this)
            Log.d("GPSTrackerApp", "全局BroadcastReceiver修复应用完成")
            
            // 初始化增强版崩溃处理器
            EnhancedCrashHandler.getInstance(this)
            Log.d("GPSTrackerApp", "增强版崩溃处理器初始化完成")
            
            // 清理旧的崩溃日志
            CrashHandler(this).cleanupOldLogs()
            Log.d("GPSTrackerApp", "旧崩溃日志清理完成")
            
            // 确保应用上下文正确初始化
            val appInfo = PackageInfoHelper.getAppInfoSummary(this)
            Log.d("GPSTrackerApp", "应用信息: $appInfo")
            
            // 如果PackageInfo获取失败，运行诊断
            if (appInfo.contains("获取失败")) {
                Log.w("GPSTrackerApp", "PackageInfo获取失败，运行诊断...")
                val diagnostics = PackageInfoHelper.diagnosePackageInfo(this)
                Log.w("GPSTrackerApp", diagnostics)
            }
            
            Log.d("GPSTrackerApp", "=== Application初始化完成 ===")
        } catch (e: Exception) {
            Log.e("GPSTrackerApp", "=== Application初始化失败 ===", e)
            Log.e("GPSTrackerApp", "异常类型: ${e.javaClass.simpleName}")
            Log.e("GPSTrackerApp", "异常消息: ${e.message}")
            Log.e("GPSTrackerApp", "异常堆栈: ${e.stackTraceToString()}")
        }
    }
    
}
