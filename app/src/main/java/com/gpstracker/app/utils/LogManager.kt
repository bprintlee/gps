package com.gpstracker.app.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日志管理器
 * 负责收集、保存和管理应用日志
 */
class LogManager(private val context: Context) {
    
    companion object {
        private const val TAG = "LogManager"
        private const val LOG_DIR = "app_logs"
        private const val MAX_LOG_SIZE = 1024 * 1024 // 1MB
        private const val MAX_LOG_FILES = 10
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val logDir = File(context.filesDir, LOG_DIR)
    
    init {
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
    }
    
    /**
     * 保存日志到文件
     */
    fun saveLog(tag: String, level: String, message: String, throwable: Throwable? = null) {
        try {
            val timestamp = dateFormat.format(Date())
            val logEntry = buildLogEntry(timestamp, tag, level, message, throwable)
            
            val logFile = getCurrentLogFile()
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(logEntry)
                writer.flush()
            }
            
            // 检查文件大小，如果太大则轮转
            if (logFile.length() > MAX_LOG_SIZE) {
                rotateLogFiles()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "保存日志失败", e)
        }
    }
    
    /**
     * 构建日志条目
     */
    private fun buildLogEntry(timestamp: String, tag: String, level: String, message: String, throwable: Throwable?): String {
        val entry = StringBuilder()
        entry.append("$timestamp [$level] $tag: $message")
        
        throwable?.let {
            entry.appendLine()
            entry.append("异常: ${it.javaClass.simpleName}")
            entry.appendLine("消息: ${it.message}")
            entry.appendLine("堆栈:")
            it.stackTrace.forEach { element ->
                entry.appendLine("  at $element")
            }
        }
        
        return entry.toString()
    }
    
    /**
     * 获取当前日志文件
     */
    private fun getCurrentLogFile(): File {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return File(logDir, "app_$today.log")
    }
    
    /**
     * 轮转日志文件
     */
    private fun rotateLogFiles() {
        try {
            val files = logDir.listFiles()?.sortedBy { it.lastModified() } ?: return
            
            // 删除最旧的文件
            val filesToDelete = files.take(files.size - MAX_LOG_FILES + 1)
            filesToDelete.forEach { file ->
                file.delete()
            }
            
            // 重命名当前文件
            val currentFile = getCurrentLogFile()
            if (currentFile.exists()) {
                val timestamp = System.currentTimeMillis()
                val newFile = File(logDir, "app_${timestamp}.log")
                currentFile.renameTo(newFile)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "轮转日志文件失败", e)
        }
    }
    
    /**
     * 获取所有日志文件
     */
    fun getAllLogFiles(): List<File> {
        return logDir.listFiles()?.filter { it.name.startsWith("app_") && it.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * 获取指定文件的日志内容
     */
    fun getLogContent(file: File): String {
        return try {
            file.readText()
        } catch (e: Exception) {
            Log.e(TAG, "读取日志文件失败: ${file.name}", e)
            "读取日志文件失败: ${e.message}"
        }
    }
    
    /**
     * 获取最近的日志内容（最近N行）
     */
    fun getRecentLogs(lines: Int = 1000): String {
        return try {
            val currentFile = getCurrentLogFile()
            if (!currentFile.exists()) {
                return "暂无日志文件"
            }
            
            val allLines = currentFile.readLines()
            val recentLines = allLines.takeLast(lines)
            
            if (recentLines.isEmpty()) {
                "日志文件为空"
            } else {
                recentLines.joinToString("\n")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "获取最近日志失败", e)
            "获取日志失败: ${e.message}"
        }
    }
    
    /**
     * 清理所有日志文件
     */
    fun clearAllLogs() {
        try {
            logDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("app_") && file.name.endsWith(".log")) {
                    file.delete()
                }
            }
            Log.d(TAG, "所有日志文件已清理")
        } catch (e: Exception) {
            Log.e(TAG, "清理日志文件失败", e)
        }
    }
    
    /**
     * 获取日志目录大小
     */
    fun getLogDirSize(): String {
        return try {
            val totalSize = logDir.listFiles()?.sumOf { it.length() } ?: 0L
            formatFileSize(totalSize)
        } catch (e: Exception) {
            "计算失败"
        }
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * 导出日志到外部存储
     */
    fun exportLogs(): String {
        return try {
            val exportDir = File(context.getExternalFilesDir(null), "exported_logs")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(exportDir, "gps_tracker_logs_$timestamp.txt")
            
            PrintWriter(FileWriter(exportFile)).use { writer ->
                writer.println("=== GPS Tracker 日志导出 ===")
                writer.println("导出时间: ${dateFormat.format(Date())}")
                writer.println("设备信息: ${android.os.Build.MODEL} (${android.os.Build.VERSION.RELEASE})")
                writer.println("应用版本: ${PackageInfoHelper.getVersionNameSimple(context)}")
                writer.println()
                
                // 导出所有日志文件
                getAllLogFiles().forEach { logFile ->
                    writer.println("=== ${logFile.name} ===")
                    writer.println(getLogContent(logFile))
                    writer.println()
                }
                
                writer.flush()
            }
            
            exportFile.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "导出日志失败", e)
            "导出失败: ${e.message}"
        }
    }
}
