package com.gpstracker.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.gpstracker.app.databinding.ActivityMainBinding
import com.gpstracker.app.service.GpsTrackingService
import com.gpstracker.app.model.TrackingState
import com.gpstracker.app.utils.EnhancedCrashHandler
import com.gpstracker.app.utils.GpsAccuracyOptimizer
import com.gpstracker.app.utils.BackgroundGpsTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var isTracking = false
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var crashHandler: EnhancedCrashHandler
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startTracking()
        } else {
            Toast.makeText(this, "需要位置权限才能使用GPS跟踪功能", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // 初始化增强版崩溃处理器
            crashHandler = EnhancedCrashHandler.getInstance(this)
            crashHandler.logAppState("MainActivity onCreate")
            
            setupClickListeners()
            updateUI()
            startStatusMonitoring()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "onCreate failed", e)
            crashHandler.logError("MainActivity onCreate failed", e)
            // 如果初始化失败，显示错误信息
            Toast.makeText(this, "应用初始化失败，请重启应用", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupClickListeners() {
        binding.startButton.setOnClickListener {
            if (!isTracking) {
                checkPermissionsAndStartTracking()
            }
        }
        
        binding.stopButton.setOnClickListener {
            if (isTracking) {
                stopTracking()
            }
        }
        
        binding.exportButton.setOnClickListener {
            // 显示GPX文件保存路径和详细信息
            if (isTracking) {
                val serviceIntent = Intent(this, GpsTrackingService::class.java)
                try {
                    val serviceConnection = object : android.content.ServiceConnection {
                        override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                            service?.let {
                                val gpsService = (it as GpsTrackingService.GpsTrackingBinder).getService()
                                val gpxPath = gpsService.getGpxDirectoryPath()
                                val gpsCount = gpsService.getGpsDataCount()
                                
                                // 异步获取文件信息
                                lifecycleScope.launch {
                                    try {
                                        val fileInfo = gpsService.getGpxFileInfo()
                                        val fileCount = fileInfo["fileCount"] as Int
                                        val files = fileInfo["files"] as List<Map<String, Any>>
                                        
                                        val message = if (fileCount > 0) {
                                            val totalSize = files.sumOf { it["size"] as Long }
                                            "GPX文件保存在: $gpxPath\n已记录 $gpsCount 个GPS点\n文件数量: $fileCount\n总大小: ${totalSize} 字节"
                                        } else {
                                            "GPX文件保存在: $gpxPath\n已记录 $gpsCount 个GPS点\n暂无GPX文件"
                                        }
                                        
                                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "GPX文件保存在: $gpxPath\n已记录 $gpsCount 个GPS点", Toast.LENGTH_LONG).show()
                                    }
                                }
                                
                                unbindService(this)
                            }
                        }
                        override fun onServiceDisconnected(name: android.content.ComponentName?) {}
                    }
                    bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                } catch (e: Exception) {
                    Toast.makeText(this, "无法获取GPX文件信息", Toast.LENGTH_SHORT).show()
                }
            } else {
                val intent = Intent(this, ExportActivity::class.java)
                startActivity(intent)
            }
        }
        
        binding.powerSaveButton.setOnClickListener {
            togglePowerSaveMode()
        }
        
        binding.logViewerButton.setOnClickListener {
            val intent = Intent(this, LogViewerActivity::class.java)
            startActivity(intent)
        }
        
        binding.debugButton.setOnClickListener {
            SimpleDebugActivity.start(this)
        }
        
        binding.settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        // 长按设置按钮测试后台GPS功能
        binding.settingsButton.setOnLongClickListener {
            testBackgroundGpsFunctionality()
            true
        }
        
        // MQTT测试功能和GPS精度检查功能已删除
        
    }
    
    private fun checkPermissionsAndStartTracking() {
        val permissions = mutableListOf<String>().apply {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            // Android 13+ 需要通知权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startTracking()
        }
    }
    
    private fun startTracking() {
        try {
            crashHandler.logAppState("Starting GPS tracking")
            
            val intent = Intent(this, GpsTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            isTracking = true
            
            // 自动开始行程
            lifecycleScope.launch {
                delay(2000) // 等待2秒让服务完全启动
                startTrip()
            }
            
            updateUI()
            crashHandler.logAppState("GPS tracking started successfully")
            Toast.makeText(this, "开始GPS跟踪和行程", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            crashHandler.logError("Failed to start GPS tracking", e)
            Toast.makeText(this, "启动GPS跟踪失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopTracking() {
        // 先结束当前行程
        stopTrip()
        
        // 等待一小段时间让行程结束
        lifecycleScope.launch {
            delay(1000)
            val intent = Intent(this@MainActivity, GpsTrackingService::class.java)
            stopService(intent)
            isTracking = false
            updateUI()
        }
        
        Toast.makeText(this, "停止GPS跟踪和行程", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateUI() {
        binding.startButton.isEnabled = !isTracking
        binding.stopButton.isEnabled = isTracking
        
        if (isTracking) {
            binding.statusText.text = "跟踪中..."
        } else {
            binding.statusText.text = "已停止"
        }
    }
    
    private fun startStatusMonitoring() {
        lifecycleScope.launch {
            while (true) {
                updateServiceStatus()
                delay(2000) // 每2秒更新一次状态
            }
        }
    }
    
    private fun updateServiceStatus() {
        if (isTracking) {
            // 检查服务是否在运行
            val isServiceRunning = isServiceRunning()
            if (isServiceRunning) {
                // 获取服务状态信息
                val serviceIntent = Intent(this, GpsTrackingService::class.java)
                try {
                    val serviceConnection = object : android.content.ServiceConnection {
                        override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                            service?.let {
                                val gpsService = (it as GpsTrackingService.GpsTrackingBinder).getService()
                                updateStatusDisplay(
                                    gpsService.getCurrentState(), 
                                    gpsService.isGpsAvailable(),
                                    gpsService.getStepCount(),
                                    gpsService.getLastAcceleration(),
                                    gpsService.isPowerSaveMode(),
                                    gpsService.getCurrentTripId(),
                                    gpsService.isTripActive(),
                                    gpsService.getMqttConnectionInfo()
                                )
                                
                                // 添加调试信息
                                val debugInfo = gpsService.getDebugInfo()
                                android.util.Log.d("MainActivity", "状态调试信息: $debugInfo")
                                unbindService(this)
                            }
                        }
                        override fun onServiceDisconnected(name: android.content.ComponentName?) {}
                    }
                    bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                } catch (e: Exception) {
                    // 服务连接失败，显示默认状态
                    updateStatusDisplay(null, false, 0, 0f, false, null, false, null)
                }
            } else {
                updateStatusDisplay(null, false, 0, 0f, false, null, false, null)
            }
        } else {
            // 服务未运行
            updateStatusDisplay(null, false, 0, 0f, false, null, false, null)
        }
    }
    
    /**
     * 测试后台GPS功能
     */
    private fun testBackgroundGpsFunctionality() {
        if (!isServiceRunning()) {
            Toast.makeText(this, "GPS服务未运行，请先启动跟踪", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "开始测试后台GPS功能...", Toast.LENGTH_SHORT).show()
        
        BackgroundGpsTest.testBackgroundGpsData(this) { result ->
            runOnUiThread {
                val message = if (result.success) {
                    "后台GPS测试成功！\n" +
                    "状态: ${result.currentState}\n" +
                    "GPS可用: ${result.isGpsAvailable}\n" +
                    "数据点数: ${result.gpsDataCount}\n" +
                    "行程数: ${result.allTripIds.size}"
                } else {
                    "后台GPS测试失败: ${result.message}"
                }
                
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                Log.d("MainActivity", "后台GPS测试结果: $result")
            }
        }
    }
    
    private fun isServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        return runningServices.any { it.service.className == GpsTrackingService::class.java.name }
    }
    
    private fun updateStatusDisplay(currentState: TrackingState?, gpsAvailable: Boolean, stepCount: Int, acceleration: Float, isPowerSave: Boolean, currentTripId: String?, isTripActive: Boolean, mqttStatus: String?) {
        // 更新GPS状态
        binding.gpsStatusText.text = if (gpsAvailable) "有信号" else "无信号"
        binding.gpsStatusText.setTextColor(
            ContextCompat.getColor(this, if (gpsAvailable) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )
        
        // 更新状态显示
        val stateText = when (currentState) {
            TrackingState.INDOOR -> "室内模式"
            TrackingState.OUTDOOR -> "室外模式"
            TrackingState.ACTIVE -> "活跃状态"
            TrackingState.DRIVING -> "驾驶模式 (基于速度检测)"
            TrackingState.DEEP_STATIONARY -> "深度静止模式 (省电中)"
            null -> if (isTracking) "跟踪中..." else "已停止"
        }
        
        binding.statusText.text = stateText
        
        // 设置状态颜色
        val stateColor = when (currentState) {
            TrackingState.INDOOR -> android.R.color.holo_orange_dark
            TrackingState.OUTDOOR -> android.R.color.holo_blue_dark
            TrackingState.ACTIVE -> android.R.color.holo_green_dark
            TrackingState.DRIVING -> android.R.color.holo_purple
            TrackingState.DEEP_STATIONARY -> android.R.color.holo_red_dark
            null -> android.R.color.darker_gray
        }
        
        binding.statusText.setTextColor(ContextCompat.getColor(this, stateColor))
        
        // 后台服务状态和步数统计显示已删除
        
        // 更新已保存点数
        updateSavedPointsCount()
        
        // 更新省电模式状态
        updatePowerSaveStatus(isPowerSave)
        
        // 更新最后位置信息
        updateLastLocationInfo()
        
        // 隐藏行程信息显示
        // updateTripInfo(currentTripId, isTripActive)
        
        // 更新MQTT状态
        updateMqttStatus(mqttStatus)
    }
    
    private fun updateLastLocationInfo() {
        if (isTracking) {
            // 从服务获取最后位置信息
            val serviceIntent = Intent(this, GpsTrackingService::class.java)
            try {
                val serviceConnection = object : android.content.ServiceConnection {
                    override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                        service?.let {
                            val gpsService = (it as GpsTrackingService.GpsTrackingBinder).getService()
                            val lastLocation = gpsService.getLastLocation()
                            if (lastLocation != null) {
                                val lat = String.format("%.6f", lastLocation.latitude)
                                val lon = String.format("%.6f", lastLocation.longitude)
                                binding.lastLocationText.text = "纬度: $lat\n经度: $lon"
                            } else {
                                binding.lastLocationText.text = "获取中..."
                            }
                            unbindService(this)
                        }
                    }
                    override fun onServiceDisconnected(name: android.content.ComponentName?) {}
                }
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                binding.lastLocationText.text = "获取中..."
            }
        } else {
            binding.lastLocationText.text = "未知"
        }
    }
    
    private fun togglePowerSaveMode() {
        if (isTracking) {
            val serviceIntent = Intent(this, GpsTrackingService::class.java)
            try {
                val serviceConnection = object : android.content.ServiceConnection {
                    override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                        service?.let {
                            val gpsService = (it as GpsTrackingService.GpsTrackingBinder).getService()
                            gpsService.togglePowerSaveMode()
                            updatePowerSaveStatus(gpsService.isPowerSaveMode())
                            unbindService(this)
                        }
                    }
                    override fun onServiceDisconnected(name: android.content.ComponentName?) {}
                }
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                Toast.makeText(this, "切换省电模式失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "请先开始GPS跟踪", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startTrip() {
        if (isTracking) {
            val serviceIntent = Intent(this, GpsTrackingService::class.java)
            try {
                val serviceConnection = object : android.content.ServiceConnection {
                    override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                        service?.let {
                            val gpsService = (it as GpsTrackingService.GpsTrackingBinder).getService()
                            val success = gpsService.startTrip()
                            if (success) {
                                android.util.Log.d("MainActivity", "行程已开始")
                            } else {
                                android.util.Log.w("MainActivity", "无法开始行程，可能已有活跃行程")
                            }
                            unbindService(this)
                        }
                    }
                    override fun onServiceDisconnected(name: android.content.ComponentName?) {}
                }
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "开始行程失败", e)
            }
        }
    }
    
    private fun stopTrip() {
        if (isTracking) {
            val serviceIntent = Intent(this, GpsTrackingService::class.java)
            try {
                val serviceConnection = object : android.content.ServiceConnection {
                    override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                        service?.let {
                            val gpsService = (it as GpsTrackingService.GpsTrackingBinder).getService()
                            val success = gpsService.stopTrip()
                            if (success) {
                                android.util.Log.d("MainActivity", "行程已结束")
                            } else {
                                android.util.Log.w("MainActivity", "无法结束行程，可能没有活跃行程")
                            }
                            unbindService(this)
                        }
                    }
                    override fun onServiceDisconnected(name: android.content.ComponentName?) {}
                }
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "结束行程失败", e)
            }
        }
    }
    
    private fun updatePowerSaveStatus(isPowerSave: Boolean) {
        binding.powerSaveStatusText.text = if (isPowerSave) "开启" else "关闭"
        binding.powerSaveStatusText.setTextColor(
            ContextCompat.getColor(this, if (isPowerSave) android.R.color.holo_orange_dark else android.R.color.holo_green_dark)
        )
        
        val buttonText = if (isPowerSave) "持续记录" else "省电模式"
        binding.powerSaveButton.text = buttonText
        
        // 移除弹窗提示，静默切换模式
    }
    
    private fun updateSavedPointsCount() {
        if (isTracking) {
            val serviceIntent = Intent(this, GpsTrackingService::class.java)
            try {
                val serviceConnection = object : android.content.ServiceConnection {
                    override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                        service?.let {
                            val gpsService = (it as GpsTrackingService.GpsTrackingBinder).getService()
                            val savedCount = gpsService.getGpsDataCount()
                            binding.savedPointsText.text = savedCount.toString()
                            unbindService(this)
                        }
                    }
                    override fun onServiceDisconnected(name: android.content.ComponentName?) {}
                }
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                binding.savedPointsText.text = "0"
            }
        } else {
            binding.savedPointsText.text = "0"
        }
    }
    
    private fun updateTripInfo(currentTripId: String?, isTripActive: Boolean) {
        // 更新当前行程显示
        if (isTripActive && currentTripId != null) {
            binding.currentTripText.text = currentTripId
            binding.currentTripText.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            )
        } else {
            binding.currentTripText.text = "无"
            binding.currentTripText.setTextColor(
                ContextCompat.getColor(this, android.R.color.darker_gray)
            )
        }
        
        // 总行程数显示已删除
    }
    
    
    // MQTT测试功能已删除
    
    // GPS精度检查功能已删除
    
    
    private fun updateMqttStatus(mqttStatus: String?) {
        val status = mqttStatus ?: "未知"
        binding.mqttStatusText.text = status
        
        val statusColor = when (status) {
            "已连接" -> android.R.color.holo_green_dark
            "连接中..." -> android.R.color.holo_orange_dark
            "未连接" -> android.R.color.holo_red_dark
            else -> android.R.color.darker_gray
        }
        
        binding.mqttStatusText.setTextColor(ContextCompat.getColor(this, statusColor))
    }
    
}
