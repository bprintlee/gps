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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
        updateUI()
        startStatusMonitoring()
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
            val intent = Intent(this, ExportActivity::class.java)
            startActivity(intent)
        }
        
        binding.powerSaveButton.setOnClickListener {
            togglePowerSaveMode()
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
                                    gpsService.isPowerSaveMode()
                                )
                                unbindService(this)
                            }
                        }
                        override fun onServiceDisconnected(name: android.content.ComponentName?) {}
                    }
                    bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                } catch (e: Exception) {
                    // 服务连接失败，显示默认状态
                    updateStatusDisplay(null, false, 0, 0f, false)
                }
            } else {
                updateStatusDisplay(null, false, 0, 0f, false)
            }
        } else {
            // 服务未运行
            updateStatusDisplay(null, false, 0, 0f, false)
        }
    }
    
    private fun isServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        return runningServices.any { it.service.className == GpsTrackingService::class.java.name }
    }
    
    private fun updateStatusDisplay(currentState: TrackingState?, gpsAvailable: Boolean, stepCount: Int, acceleration: Float, isPowerSave: Boolean) {
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
        
        // 更新省电模式状态
        updatePowerSaveStatus(isPowerSave)
        
        // 更新最后位置信息
        updateLastLocationInfo()
    }
    
    private fun updateLastLocationInfo() {
        // 这里可以添加获取最后位置信息的逻辑
        // 暂时显示默认信息
        binding.lastLocationText.text = if (isTracking) "获取中..." else "未知"
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
    
    private fun updatePowerSaveStatus(isPowerSave: Boolean) {
        binding.powerSaveStatusText.text = if (isPowerSave) "开启" else "关闭"
        binding.powerSaveStatusText.setTextColor(
            ContextCompat.getColor(this, if (isPowerSave) android.R.color.holo_orange_dark else android.R.color.holo_green_dark)
        )
        
        val buttonText = if (isPowerSave) "正常模式" else "省电模式"
        binding.powerSaveButton.text = buttonText
        
        Toast.makeText(this, if (isPowerSave) "已开启省电模式" else "已关闭省电模式", Toast.LENGTH_SHORT).show()
    }
    
}
