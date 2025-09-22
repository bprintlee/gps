package com.gpstracker.app.utils

import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 增强版崩溃日志收集器
 * 用于收集和保存应用崩溃信息，支持实时日志记录和复制功能
 */
class EnhancedCrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val maxLogEntries = 1000 // 最多保存1000条日志
    
    companion object {
        private const val TAG = "EnhancedCrashHandler"
        private const val CRASH_LOG_DIR = "crash_logs"
        private const val RUNTIME_LOG_DIR = "runtime_logs"
        
        @Volatile
        private var instance: EnhancedCrashHandler? = null
        
        fun getInstance(context: Context): EnhancedCrashHandler {
            return instance ?: synchronized(this) {
                instance ?: EnhancedCrashHandler(context.applicationContext).also { instance = it }
            }
        }
    }
    
    init {
        // 设置全局异常处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
        
        // 启动日志记录
        startLogRecording()
    }
    
    /**
     * 记录运行时日志
     */
    fun logRuntime(message: String, level: String = "INFO") {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] [$level] $message"
        
        // 添加到内存队列
        logQueue.offer(logEntry)
        
        // 保持队列大小
        while (logQueue.size > maxLogEntries) {
            logQueue.poll()
        }
        
        // 同时写入文件
        saveRuntimeLog(logEntry)
        
        // 输出到系统日志
        when (level) {
            "ERROR" -> Log.e(TAG, message)
            "WARN" -> Log.w(TAG, message)
            "DEBUG" -> Log.d(TAG, message)
            else -> Log.i(TAG, message)
        }
    }
    
    /**
     * 记录错误日志
     */
    fun logError(message: String, throwable: Throwable? = null) {
        logRuntime("ERROR: $message", "ERROR")
        throwable?.let {
            logRuntime("Exception: ${it.javaClass.simpleName}: ${it.message}", "ERROR")
            logRuntime("Stack trace: ${it.stackTraceToString()}", "ERROR")
        }
    }
    
    /**
     * 记录警告日志
     */
    fun logWarning(message: String) {
        logRuntime("WARNING: $message", "WARN")
    }
    
    /**
     * 记录调试信息
     */
    fun logDebug(message: String) {
        logRuntime("DEBUG: $message", "DEBUG")
    }
    
    /**
     * 记录应用状态
     */
    fun logAppState(state: String) {
        logRuntime("APP_STATE: $state", "INFO")
    }
    
    /**
     * 记录GPS状态
     */
    fun logGpsState(latitude: Double?, longitude: Double?, accuracy: Float?) {
        val location = if (latitude != null && longitude != null) {
            "GPS: lat=$latitude, lon=$longitude, accuracy=$accuracy"
        } else {
            "GPS: No location available"
        }
        logRuntime(location, "INFO")
    }
    
    /**
     * 记录MQTT状态
     */
    fun logMqttState(connected: Boolean, message: String = "") {
        val status = if (connected) "CONNECTED" else "DISCONNECTED"
        logRuntime("MQTT: $status - $message", "INFO")
    }
    
    /**
     * 获取所有运行时日志
     */
    fun getAllRuntimeLogs(): String {
        return logQueue.joinToString("\n")
    }
    
    /**
     * 复制日志到剪贴板
     */
    fun copyLogsToClipboard() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val logs = getAllRuntimeLogs()
            val clip = ClipData.newPlainText("GPS Tracker Logs", logs)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "复制日志到剪贴板失败", e)
            Toast.makeText(context, "复制失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 获取崩溃日志文件列表
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
     * 获取运行时日志文件列表
     */
    fun getRuntimeLogs(): List<File> {
        val logDir = File(context.filesDir, RUNTIME_LOG_DIR)
        return if (logDir.exists()) {
            logDir.listFiles { file -> file.name.startsWith("runtime_") }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * 清理旧日志文件
     */
    fun cleanupOldLogs() {
        try {
            val crashLogDir = File(context.filesDir, CRASH_LOG_DIR)
            val runtimeLogDir = File(context.filesDir, RUNTIME_LOG_DIR)
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            
            listOf(crashLogDir, runtimeLogDir).forEach { logDir ->
                if (logDir.exists()) {
                    logDir.listFiles()?.forEach { file ->
                        if (file.lastModified() < sevenDaysAgo) {
                            file.delete()
                            Log.d(TAG, "删除旧日志文件: ${file.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理旧日志时出错", e)
        }
    }
    
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        Log.e(TAG, "=== 应用崩溃检测到 ===", exception)
        
        try {
            // 记录崩溃前的最后状态
            logError("应用崩溃", exception)
            logAppState("CRASH_DETECTED")
            
            // 保存崩溃日志
            saveCrashLog(thread, exception)
            
            // 保存运行时日志
            saveRuntimeLogs()
            
            // 保存MQTT相关状态
            saveMqttState()
            
        } catch (e: Exception) {
            Log.e(TAG, "保存崩溃日志失败", e)
        }
        
        // 调用默认处理器
        defaultHandler?.uncaughtException(thread, exception)
    }
    
    private fun startLogRecording() {
        // 记录应用启动
        logAppState("APP_STARTED")
        logRuntime("EnhancedCrashHandler initialized", "INFO")
    }
    
    private fun saveRuntimeLog(logEntry: String) {
        try {
            val logDir = File(context.filesDir, RUNTIME_LOG_DIR)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val logFile = File(logDir, "runtime_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.log")
            
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(logEntry)
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存运行时日志失败", e)
        }
    }
    
    private fun saveRuntimeLogs() {
        try {
            val logDir = File(context.filesDir, RUNTIME_LOG_DIR)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            val logFile = File(logDir, "runtime_crash_${timestamp}.log")
            
            PrintWriter(FileWriter(logFile)).use { writer ->
                writer.println("=== GPS Tracker 运行时日志 ===")
                writer.println("时间: ${dateFormat.format(Date(timestamp))}")
                writer.println("日志条数: ${logQueue.size}")
                writer.println()
                
                logQueue.forEach { logEntry ->
                    writer.println(logEntry)
                }
                
                writer.flush()
            }
            
            Log.d(TAG, "运行时日志已保存: ${logFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "保存运行时日志失败", e)
        }
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
                writer.println()
                writer.println("=== 运行时日志 (最近${logQueue.size}条) ===")
                logQueue.forEach { logEntry ->
                    writer.println(logEntry)
                }
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
                writer.println("注意: MQTT状态信息需要在MqttManager中实现状态保存功能")
                writer.flush()
            }
            
            Log.d(TAG, "MQTT状态日志已保存: ${mqttLogFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "保存MQTT状态日志时出错", e)
        }
    }
}
