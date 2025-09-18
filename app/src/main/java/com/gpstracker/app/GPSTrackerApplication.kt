package com.gpstracker.app

import android.app.Application
import android.util.Log

class GPSTrackerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("GPSTrackerApp", "Application onCreate")
        
        try {
            // 确保应用上下文正确初始化
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            Log.d("GPSTrackerApp", "Package info loaded successfully: ${packageInfo.versionName}")
        } catch (e: Exception) {
            Log.e("GPSTrackerApp", "Failed to get package info", e)
        }
    }
}
