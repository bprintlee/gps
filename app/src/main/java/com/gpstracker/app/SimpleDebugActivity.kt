package com.gpstracker.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gpstracker.app.databinding.ActivitySimpleDebugBinding
import com.gpstracker.app.utils.EnhancedCrashHandler
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 简化版调试页面 - 用于查看崩溃日志和调试信息
 */
class SimpleDebugActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySimpleDebugBinding
    private lateinit var crashHandler: EnhancedCrashHandler
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SimpleDebugActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivitySimpleDebugBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // 设置返回按钮
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "调试信息"
            
            // 初始化增强版崩溃处理器
            crashHandler = EnhancedCrashHandler.getInstance(this)
            
            // 加载崩溃日志
            loadCrashLogs()
            
            // 设置点击事件
            binding.refreshButton.setOnClickListener {
                loadCrashLogs()
            }
            
            binding.createTestButton.setOnClickListener {
                createTestLog()
            }
            
            binding.copyLogsButton.setOnClickListener {
                copyLogsToClipboard()
            }
            
            binding.viewRuntimeLogsButton.setOnClickListener {
                viewRuntimeLogs()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SimpleDebugActivity", "onCreate failed", e)
            Toast.makeText(this, "调试页面初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun loadCrashLogs() {
        try {
            val crashLogs = crashHandler.getCrashLogs()
            
            if (crashLogs.isEmpty()) {
                binding.logListText.text = "暂无崩溃日志"
                binding.logContentText.text = "没有找到崩溃日志文件\n\n提示：当应用发生崩溃时，会自动生成崩溃日志文件。\n\n您可以点击'创建测试日志'按钮来测试功能。"
            } else {
                // 显示日志文件列表
                val logListText = buildString {
                    appendLine("找到 ${crashLogs.size} 个崩溃日志文件：")
                    appendLine()
                    crashLogs.sortedByDescending { it.lastModified() }.forEachIndexed { index, file ->
                        try {
                            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .format(Date(file.lastModified()))
                            val size = formatFileSize(file.length())
                            appendLine("${index + 1}. ${file.name}")
                            appendLine("   时间: $date")
                            appendLine("   大小: $size")
                            appendLine()
                        } catch (e: Exception) {
                            appendLine("${index + 1}. ${file.name} (信息获取失败)")
                            appendLine()
                        }
                    }
                }
                binding.logListText.text = logListText
                
                // 显示最新日志的内容
                val latestLog = crashLogs.maxByOrNull { it.lastModified() }
                if (latestLog != null) {
                    loadLogContent(latestLog)
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SimpleDebugActivity", "loadCrashLogs failed", e)
            binding.logListText.text = "加载日志列表失败: ${e.message}"
            binding.logContentText.text = "错误: ${e.message}\n\n请检查应用权限或重启应用。"
        }
    }
    
    private fun loadLogContent(logFile: File) {
        try {
            val content = if (logFile.exists() && logFile.canRead()) {
                logFile.readText()
            } else {
                "文件不存在或无法读取"
            }
            binding.logContentText.text = content
            
        } catch (e: Exception) {
            android.util.Log.e("SimpleDebugActivity", "读取日志文件失败", e)
            binding.logContentText.text = "读取文件失败: ${e.message}"
        }
    }
    
    private fun createTestLog() {
        try {
            val logDir = File(filesDir, "crash_logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            val logFile = File(logDir, "crash_${timestamp}.log")
            
            val content = buildString {
                appendLine("=== GPS Tracker 测试崩溃日志 ===")
                appendLine("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))}")
                appendLine("线程: main")
                appendLine("异常类型: TestException")
                appendLine("异常消息: 这是一个测试崩溃日志")
                appendLine()
                appendLine("=== 设备信息 ===")
                appendLine("Android版本: ${android.os.Build.VERSION.RELEASE}")
                appendLine("API级别: ${android.os.Build.VERSION.SDK_INT}")
                appendLine("设备型号: ${android.os.Build.MODEL}")
                appendLine("制造商: ${android.os.Build.MANUFACTURER}")
                appendLine()
                appendLine("=== 异常堆栈 ===")
                appendLine("java.lang.TestException: 这是一个测试崩溃日志")
                appendLine("    at com.gpstracker.app.SimpleDebugActivity.createTestLog(SimpleDebugActivity.kt:123)")
                appendLine("    at com.gpstracker.app.SimpleDebugActivity.onCreate(SimpleDebugActivity.kt:45)")
                appendLine("    at android.app.Activity.performCreate(Activity.java:7136)")
                appendLine("    at android.app.Activity.performCreate(Activity.java:7127)")
                appendLine("    at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1271)")
                appendLine("    at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2893)")
                appendLine("    at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3048)")
                appendLine("    at android.app.servertransaction.LaunchActivityItem.execute(LaunchActivityItem.java:78)")
                appendLine("    at android.app.servertransaction.TransactionExecutor.executeCallbacks(TransactionExecutor.java:108)")
                appendLine("    at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:68)")
                appendLine("    at android.app.ActivityThread.handleMessage(ActivityThread.java:1808)")
                appendLine("    at android.os.Handler.dispatchMessage(Handler.java:106)")
                appendLine("    at android.os.Looper.loop(Looper.java:193)")
                appendLine("    at android.app.ActivityThread.main(ActivityThread.java:6669)")
                appendLine("    at java.lang.reflect.Method.invoke(Native Method)")
                appendLine("    at com.android.internal.os.RuntimeInit.MethodAndArgsCaller.run(RuntimeInit.java:493)")
                appendLine("    at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:858)")
                appendLine()
                appendLine("=== 线程信息 ===")
                appendLine("活动线程数: ${Thread.activeCount()}")
                appendLine("当前线程: ${Thread.currentThread().name}")
                appendLine("线程状态: RUNNABLE")
            }
            
            logFile.writeText(content)
            Toast.makeText(this, "测试日志已创建", Toast.LENGTH_SHORT).show()
            loadCrashLogs()
            
        } catch (e: Exception) {
            android.util.Log.e("SimpleDebugActivity", "创建测试日志失败", e)
            Toast.makeText(this, "创建测试日志失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
    
    private fun copyLogsToClipboard() {
        try {
            crashHandler.copyLogsToClipboard()
        } catch (e: Exception) {
            android.util.Log.e("SimpleDebugActivity", "复制日志失败", e)
            Toast.makeText(this, "复制日志失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun viewRuntimeLogs() {
        try {
            val runtimeLogs = crashHandler.getAllRuntimeLogs()
            if (runtimeLogs.isNotEmpty()) {
                binding.logContentText.text = "=== 运行时日志 ===\n\n$runtimeLogs"
            } else {
                binding.logContentText.text = "暂无运行时日志\n\n运行时日志会记录应用运行过程中的重要事件。"
            }
        } catch (e: Exception) {
            android.util.Log.e("SimpleDebugActivity", "查看运行时日志失败", e)
            binding.logContentText.text = "查看运行时日志失败: ${e.message}"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
