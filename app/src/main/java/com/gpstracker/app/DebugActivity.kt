package com.gpstracker.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gpstracker.app.databinding.ActivityDebugBinding
import com.gpstracker.app.utils.CrashHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 调试页面 - 用于查看崩溃日志和调试信息
 */
class DebugActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDebugBinding
    private lateinit var crashHandler: CrashHandler
    private var currentLogFile: File? = null
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DebugActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityDebugBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // 设置工具栏
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "调试信息"
            
            // 初始化崩溃处理器
            crashHandler = CrashHandler(this)
            
            // 设置滚动
            binding.logContentText.movementMethod = ScrollingMovementMethod()
            
            // 加载崩溃日志列表
            loadCrashLogs()
            
            // 设置点击事件
            setupClickListeners()
            
        } catch (e: Exception) {
            android.util.Log.e("DebugActivity", "onCreate failed", e)
            Toast.makeText(this, "调试页面初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupClickListeners() {
        binding.refreshButton.setOnClickListener {
            loadCrashLogs()
        }
        
        binding.clearLogsButton.setOnClickListener {
            clearAllLogs()
        }
        
        binding.exportLogButton.setOnClickListener {
            exportCurrentLog()
        }
        
        binding.createTestLogButton.setOnClickListener {
            createTestLog()
        }
    }
    
    private fun loadCrashLogs() {
        lifecycleScope.launch {
            try {
                val crashLogs = withContext(Dispatchers.IO) {
                    try {
                        crashHandler.getCrashLogs().sortedByDescending { it.lastModified() }
                    } catch (e: Exception) {
                        android.util.Log.e("DebugActivity", "获取崩溃日志失败", e)
                        emptyList<File>()
                    }
                }
                
                if (crashLogs.isEmpty()) {
                    binding.logListText.text = "暂无崩溃日志"
                    binding.logContentText.text = "没有找到崩溃日志文件\n\n提示：当应用发生崩溃时，会自动生成崩溃日志文件。"
                    currentLogFile = null
                } else {
                    // 显示日志文件列表
                    val logListText = buildString {
                        appendLine("找到 ${crashLogs.size} 个崩溃日志文件：")
                        appendLine()
                        crashLogs.forEachIndexed { index, file ->
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
                    
                    // 默认显示最新的日志
                    currentLogFile = crashLogs.first()
                    loadLogContent(currentLogFile!!)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("DebugActivity", "loadCrashLogs failed", e)
                binding.logListText.text = "加载日志列表失败: ${e.message}"
                binding.logContentText.text = "错误: ${e.message}\n\n请检查应用权限或重启应用。"
            }
        }
    }
    
    private fun loadLogContent(logFile: File) {
        lifecycleScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    try {
                        if (logFile.exists() && logFile.canRead()) {
                            logFile.readText()
                        } else {
                            "文件不存在或无法读取"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DebugActivity", "读取日志文件失败", e)
                        "读取文件失败: ${e.message}"
                    }
                }
                binding.logContentText.text = content
                
                // 更新标题显示当前文件
                supportActionBar?.subtitle = logFile.name
                
            } catch (e: Exception) {
                android.util.Log.e("DebugActivity", "loadLogContent failed", e)
                binding.logContentText.text = "读取日志文件失败: ${e.message}"
            }
        }
    }
    
    private fun clearAllLogs() {
        lifecycleScope.launch {
            try {
                val deletedCount = withContext(Dispatchers.IO) {
                    val crashLogs = crashHandler.getCrashLogs()
                    var count = 0
                    crashLogs.forEach { file ->
                        if (file.delete()) {
                            count++
                        }
                    }
                    count
                }
                
                Toast.makeText(this@DebugActivity, "已删除 $deletedCount 个日志文件", Toast.LENGTH_SHORT).show()
                loadCrashLogs()
                
            } catch (e: Exception) {
                Toast.makeText(this@DebugActivity, "删除日志失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportCurrentLog() {
        currentLogFile?.let { logFile ->
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(logFile))
                    putExtra(Intent.EXTRA_SUBJECT, "崩溃日志: ${logFile.name}")
                    putExtra(Intent.EXTRA_TEXT, "GPS Tracker 崩溃日志文件")
                }
                startActivity(Intent.createChooser(intent, "导出崩溃日志"))
                
            } catch (e: Exception) {
                Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "没有可导出的日志", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createTestLog() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    crashHandler.createTestCrashLog()
                }
                Toast.makeText(this@DebugActivity, "测试日志已创建", Toast.LENGTH_SHORT).show()
                loadCrashLogs()
                
            } catch (e: Exception) {
                Toast.makeText(this@DebugActivity, "创建测试日志失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.debug_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_refresh -> {
                loadCrashLogs()
                true
            }
            R.id.action_clear_logs -> {
                clearAllLogs()
                true
            }
            R.id.action_export_log -> {
                exportCurrentLog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
