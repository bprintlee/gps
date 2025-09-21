package com.gpstracker.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.gpstracker.app.databinding.ActivityMainBinding
import com.gpstracker.app.service.GpsTrackingService
import com.gpstracker.app.model.TrackingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var isTracking = false
    
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
            
            setupClickListeners()
            updateUI()
            startStatusMonitoring()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "onCreate failed", e)
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
        
        binding.startTripButton.setOnClickListener {
            startTrip()
        }
        
        binding.stopTripButton.setOnClickListener {
            stopTrip()
        }
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
        val intent = Intent(this, GpsTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isTracking = true
        updateUI()
        Toast.makeText(this, "开始GPS跟踪", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopTracking() {
        val intent = Intent(this, GpsTrackingService::class.java)
        stopService(intent)
        isTracking = false
        updateUI()
        Toast.makeText(this, "停止GPS跟踪", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateUI() {
        binding.startButton.isEnabled = !isTracking
        binding.stopButton.isEnabled = isTracking
        
        // 更新行程按钮状态
        binding.startTripButton.isEnabled = isTracking
        binding.stopTripButton.isEnabled = isTracking
        
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
                                    gpsService.isTripActive()
                                )
                                unbindService(this)
                            }
                        }
                        override fun onServiceDisconnected(name: android.content.ComponentName?) {}
                    }
                    bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                } catch (e: Exception) {
                    // 服务连接失败，显示默认状态
                    updateStatusDisplay(null, false, 0, 0f, false, null, false)
                }
            } else {
                updateStatusDisplay(null, false, 0, 0f, false, null, false)
            }
        } else {
            // 服务未运行
            updateStatusDisplay(null, false, 0, 0f, false, null, false)
        }
    }
    
    private fun isServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        return runningServices.any { it.service.className == GpsTrackingService::class.java.name }
    }
    
    private fun updateStatusDisplay(currentState: TrackingState?, gpsAvailable: Boolean, stepCount: Int, acceleration: Float, isPowerSave: Boolean, currentTripId: String?, isTripActive: Boolean) {
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
            TrackingState.DRIVING -> "驾驶状态"
            null -> if (isTracking) "跟踪中..." else "已停止"
        }
        
        binding.statusText.text = stateText
        
        // 设置状态颜色
        val stateColor = when (currentState) {
            TrackingState.INDOOR -> android.R.color.holo_orange_dark
            TrackingState.OUTDOOR -> android.R.color.holo_blue_dark
            TrackingState.ACTIVE -> android.R.color.holo_green_dark
            TrackingState.DRIVING -> android.R.color.holo_purple
            null -> android.R.color.darker_gray
        }
        
        binding.statusText.setTextColor(ContextCompat.getColor(this, stateColor))
        
        // 更新后台服务状态
        val serviceStatus = if (isTracking && isServiceRunning()) "运行中" else "未运行"
        binding.serviceStatusText.text = serviceStatus
        binding.serviceStatusText.setTextColor(
            ContextCompat.getColor(this, if (isTracking && isServiceRunning()) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )
        
        // 更新步数统计
        binding.stepCountText.text = stepCount.toString()
        
        // 更新已保存点数
        updateSavedPointsCount()
        
        // 更新省电模式状态
        updatePowerSaveStatus(isPowerSave)
        
        // 更新最后位置信息
        updateLastLocationInfo()
        
        // 更新行程信息
        updateTripInfo(currentTripId, isTripActive)
        
        // 更新行程按钮状态
        updateTripButtonStates(isTripActive)
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
                                Toast.makeText(this@MainActivity, "行程已开始", Toast.LENGTH_SHORT).show()
                                updateTripButtonStates(gpsService.isTripActive())
                            } else {
                                Toast.makeText(this@MainActivity, "无法开始行程，可能已有活跃行程", Toast.LENGTH_SHORT).show()
                            }
                            unbindService(this)
                        }
                    }
                    override fun onServiceDisconnected(name: android.content.ComponentName?) {}
                }
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                Toast.makeText(this, "开始行程失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "请先开始GPS跟踪", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(this@MainActivity, "行程已结束", Toast.LENGTH_SHORT).show()
                                updateTripButtonStates(gpsService.isTripActive())
                            } else {
                                Toast.makeText(this@MainActivity, "无法结束行程，可能没有活跃行程", Toast.LENGTH_SHORT).show()
                            }
                            unbindService(this)
                        }
                    }
                    override fun onServiceDisconnected(name: android.content.ComponentName?) {}
                }
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                Toast.makeText(this, "结束行程失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "请先开始GPS跟踪", Toast.LENGTH_SHORT).show()
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
        
        // 更新总行程数
        if (isTracking) {
            val serviceIntent = Intent(this, GpsTrackingService::class.java)
            try {
                val serviceConnection = object : android.content.ServiceConnection {
                    override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                        service?.let {
                            val gpsService = (it as GpsTrackingService.GpsTrackingBinder).getService()
                            val totalTrips = gpsService.getAllTripIds().size
                            binding.totalTripsText.text = totalTrips.toString()
                            unbindService(this)
                        }
                    }
                    override fun onServiceDisconnected(name: android.content.ComponentName?) {}
                }
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                binding.totalTripsText.text = "0"
            }
        } else {
            binding.totalTripsText.text = "0"
        }
    }
    
    private fun updateTripButtonStates(isTripActive: Boolean) {
        binding.startTripButton.isEnabled = isTracking && !isTripActive
        binding.stopTripButton.isEnabled = isTracking && isTripActive
        
        // 更新按钮文本颜色
        val startButtonColor = if (binding.startTripButton.isEnabled) {
            ContextCompat.getColor(this, android.R.color.holo_green_dark)
        } else {
            ContextCompat.getColor(this, android.R.color.darker_gray)
        }
        
        val stopButtonColor = if (binding.stopTripButton.isEnabled) {
            ContextCompat.getColor(this, android.R.color.holo_red_dark)
        } else {
            ContextCompat.getColor(this, android.R.color.darker_gray)
        }
        
        binding.startTripButton.setTextColor(startButtonColor)
        binding.stopTripButton.setTextColor(stopButtonColor)
    }
    
}
