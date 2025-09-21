package com.gpstracker.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.gpstracker.app.databinding.ActivityLogViewerBinding
import com.gpstracker.app.utils.LogManager
import java.io.File

class LogViewerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLogViewerBinding
    private lateinit var logManager: LogManager
    private lateinit var logFileAdapter: LogFileAdapter
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            exportLogs()
        } else {
            Toast.makeText(this, "需要存储权限才能导出日志", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupLogManager()
        setupRecyclerView()
        loadRecentLogs()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "应用日志"
    }
    
    private fun setupLogManager() {
        logManager = LogManager(this)
    }
    
    private fun setupRecyclerView() {
        logFileAdapter = LogFileAdapter { file ->
            showLogContent(file)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LogViewerActivity)
            adapter = logFileAdapter
        }
        
        loadLogFiles()
    }
    
    private fun loadLogFiles() {
        val logFiles = logManager.getAllLogFiles()
        logFileAdapter.submitList(logFiles)
        
        // 更新统计信息
        binding.logStatsText.text = "日志文件: ${logFiles.size} 个, 总大小: ${logManager.getLogDirSize()}"
    }
    
    private fun loadRecentLogs() {
        val recentLogs = logManager.getRecentLogs(500)
        binding.logContentText.text = recentLogs
        binding.logContentText.movementMethod = ScrollingMovementMethod()
    }
    
    private fun showLogContent(file: File) {
        val content = logManager.getLogContent(file)
        
        AlertDialog.Builder(this)
            .setTitle("日志文件: ${file.name}")
            .setMessage(content)
            .setPositiveButton("确定", null)
            .setNeutralButton("复制", null)
            .show()
            .getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("日志内容", content)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "日志内容已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.log_viewer_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_refresh -> {
                loadLogFiles()
                loadRecentLogs()
                Toast.makeText(this, "日志已刷新", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_export -> {
                checkStoragePermissionAndExport()
                true
            }
            R.id.action_clear -> {
                showClearLogsDialog()
                true
            }
            R.id.action_mqtt_status -> {
                showMqttStatus()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun checkStoragePermissionAndExport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            == PackageManager.PERMISSION_GRANTED) {
            exportLogs()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    
    private fun exportLogs() {
        val exportPath = logManager.exportLogs()
        Toast.makeText(this, "日志已导出到: $exportPath", Toast.LENGTH_LONG).show()
    }
    
    private fun showClearLogsDialog() {
        AlertDialog.Builder(this)
            .setTitle("清理日志")
            .setMessage("确定要清理所有日志文件吗？此操作不可撤销。")
            .setPositiveButton("确定") { _, _ ->
                logManager.clearAllLogs()
                loadLogFiles()
                loadRecentLogs()
                Toast.makeText(this, "日志已清理", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showMqttStatus() {
        try {
            // 尝试获取MQTT状态
            val serviceIntent = Intent(this, com.gpstracker.app.service.GpsTrackingService::class.java)
            val serviceConnection = object : android.content.ServiceConnection {
                override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                    service?.let {
                        val gpsService = (it as com.gpstracker.app.service.GpsTrackingService.GpsTrackingBinder).getService()
                        val mqttInfo = gpsService.getMqttConnectionInfo()
                        
                        AlertDialog.Builder(this@LogViewerActivity)
                            .setTitle("MQTT连接状态")
                            .setMessage(mqttInfo)
                            .setPositiveButton("确定", null)
                            .show()
                        
                        unbindService(this)
                    }
                }
                override fun onServiceDisconnected(name: android.content.ComponentName?) {}
            }
            
            if (bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)) {
                // 如果服务未运行，显示默认状态
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        unbindService(serviceConnection)
                        AlertDialog.Builder(this)
                            .setTitle("MQTT连接状态")
                            .setMessage("GPS服务未运行，无法获取MQTT状态")
                            .setPositiveButton("确定", null)
                            .show()
                    } catch (e: Exception) {
                        // 忽略解绑异常
                    }
                }, 3000)
            }
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("MQTT连接状态")
                .setMessage("获取MQTT状态失败: ${e.message}")
                .setPositiveButton("确定", null)
                .show()
        }
    }
}
