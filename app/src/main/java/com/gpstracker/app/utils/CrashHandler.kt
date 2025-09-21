package com.gpstracker.app.utils

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 崩溃日志收集器
 * 用于收集和保存应用崩溃信息，帮助调试MQTT连接等问题
 */
class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    companion object {
        private const val TAG = "CrashHandler"
        private const val CRASH_LOG_DIR = "crash_logs"
    }
    
    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
    
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        Log.e(TAG, "=== 应用崩溃检测到 ===", exception)
        
        try {
            // 保存崩溃日志
            saveCrashLog(thread, exception)
            
            // 保存MQTT相关状态
            saveMqttState()
            
        } catch (e: Exception) {
            Log.e(TAG, "保存崩溃日志失败", e)
        }
        
        // 调用默认处理器
        defaultHandler?.uncaughtException(thread, exception)
    }
    
    private fun saveCrashLog(thread: Thread, exception: Throwable) {
        try {
            val logDir = File(context.filesDir, CRASH_LOG_DIR)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            val logFile = File(logDir, "crash_${timestamp}.log")
            
            PrintWriter(FileWriter(logFile)).use { writer ->
                writer.println("=== GPS Tracker 崩溃日志 ===")
                writer.println("时间: ${dateFormat.format(Date(timestamp))}")
                writer.println("线程: ${thread.name}")
                writer.println("异常类型: ${exception.javaClass.simpleName}")
                writer.println("异常消息: ${exception.message}")
                writer.println()
                writer.println("=== 设备信息 ===")
                writer.println("Android版本: ${Build.VERSION.RELEASE}")
                writer.println("API级别: ${Build.VERSION.SDK_INT}")
                writer.println("设备型号: ${Build.MODEL}")
                writer.println("制造商: ${Build.MANUFACTURER}")
                writer.println()
                writer.println("=== 异常堆栈 ===")
                exception.printStackTrace(writer)
                writer.println()
                writer.println("=== 线程信息 ===")
                writer.println("活动线程数: ${Thread.activeCount()}")
                writer.println("当前线程: ${Thread.currentThread().name}")
                writer.println("线程状态: ${thread.state}")
                writer.flush()
            }
            
            Log.d(TAG, "崩溃日志已保存: ${logFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "保存崩溃日志时出错", e)
        }
    }
    
    private fun saveMqttState() {
        try {
            val logDir = File(context.filesDir, CRASH_LOG_DIR)
            val mqttLogFile = File(logDir, "mqtt_state_${System.currentTimeMillis()}.log")
            
            PrintWriter(FileWriter(mqttLogFile)).use { writer ->
                writer.println("=== MQTT状态日志 ===")
                writer.println("时间: ${dateFormat.format(Date())}")
                writer.println()
                
                // 这里可以添加更多MQTT状态信息
                // 比如连接状态、最后发送的消息等
                writer.println("注意: MQTT状态信息需要在MqttManager中实现状态保存功能")
                writer.flush()
            }
            
            Log.d(TAG, "MQTT状态日志已保存: ${mqttLogFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "保存MQTT状态日志时出错", e)
        }
    }
    
    /**
     * 获取所有崩溃日志文件
     */
    fun getCrashLogs(): List<File> {
        val logDir = File(context.filesDir, CRASH_LOG_DIR)
        return if (logDir.exists()) {
            logDir.listFiles { file -> file.name.startsWith("crash_") }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * 清理旧的崩溃日志（保留最近7天）
     */
    fun cleanupOldLogs() {
        try {
            val logDir = File(context.filesDir, CRASH_LOG_DIR)
            if (!logDir.exists()) return
            
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            
            logDir.listFiles()?.forEach { file ->
                if (file.lastModified() < sevenDaysAgo) {
                    file.delete()
                    Log.d(TAG, "删除旧日志文件: ${file.name}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "清理旧日志时出错", e)
        }
    }
    
    /**
     * 创建测试崩溃日志（用于调试页面测试）
     */
    fun createTestCrashLog() {
        try {
            val logDir = File(context.filesDir, CRASH_LOG_DIR)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            val logFile = File(logDir, "crash_${timestamp}.log")
            
            PrintWriter(FileWriter(logFile)).use { writer ->
                writer.println("=== GPS Tracker 测试崩溃日志 ===")
                writer.println("时间: ${dateFormat.format(Date(timestamp))}")
                writer.println("线程: main")
                writer.println("异常类型: TestException")
                writer.println("异常消息: 这是一个测试崩溃日志")
                writer.println()
                writer.println("=== 设备信息 ===")
                writer.println("Android版本: ${Build.VERSION.RELEASE}")
                writer.println("API级别: ${Build.VERSION.SDK_INT}")
                writer.println("设备型号: ${Build.MODEL}")
                writer.println("制造商: ${Build.MANUFACTURER}")
                writer.println()
                writer.println("=== 异常堆栈 ===")
                writer.println("java.lang.TestException: 这是一个测试崩溃日志")
                writer.println("    at com.gpstracker.app.DebugActivity.createTestCrash(DebugActivity.kt:123)")
                writer.println("    at com.gpstracker.app.DebugActivity.onCreate(DebugActivity.kt:45)")
                writer.println("    at android.app.Activity.performCreate(Activity.java:7136)")
                writer.println("    at android.app.Activity.performCreate(Activity.java:7127)")
                writer.println("    at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1271)")
                writer.println("    at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2893)")
                writer.println("    at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3048)")
                writer.println("    at android.app.servertransaction.LaunchActivityItem.execute(LaunchActivityItem.java:78)")
                writer.println("    at android.app.servertransaction.TransactionExecutor.executeCallbacks(TransactionExecutor.java:108)")
                writer.println("    at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:68)")
                writer.println("    at android.app.ActivityThread.handleMessage(ActivityThread.java:1808)")
                writer.println("    at android.os.Handler.dispatchMessage(Handler.java:106)")
                writer.println("    at android.os.Looper.loop(Looper.java:193)")
                writer.println("    at android.app.ActivityThread.main(ActivityThread.java:6669)")
                writer.println("    at java.lang.reflect.Method.invoke(Native Method)")
                writer.println("    at com.android.internal.os.RuntimeInit.MethodAndArgsCaller.run(RuntimeInit.java:493)")
                writer.println("    at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:858)")
                writer.println()
                writer.println("=== 线程信息 ===")
                writer.println("活动线程数: ${Thread.activeCount()}")
                writer.println("当前线程: ${Thread.currentThread().name}")
                writer.println("线程状态: RUNNABLE")
                writer.flush()
            }
            
            Log.d(TAG, "测试崩溃日志已创建: ${logFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "创建测试崩溃日志时出错", e)
        }
    }
}
