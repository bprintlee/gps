package com.gpstracker.app

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.gpstracker.app.model.TrackingState
import com.gpstracker.app.service.GpsTrackingService
import kotlinx.coroutines.*

/**
 * 测试室内状态切换逻辑的活动
 */
class TestStationaryActivity : Activity() {
    
    private val TAG = "TestStationaryActivity"
    private var gpsService: GpsTrackingService? = null
    private var isServiceBound = false
    
    private lateinit var statusText: TextView
    private lateinit var debugText: TextView
    private lateinit var testButton: Button
    private lateinit var simulateButton: Button
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as GpsTrackingService.GpsTrackingBinder
            gpsService = binder.getService()
            isServiceBound = true
            Log.d(TAG, "GPS服务已连接")
            updateStatus()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            gpsService = null
            isServiceBound = false
            Log.d(TAG, "GPS服务已断开")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_stationary)
        
        initViews()
        bindGpsService()
        startStatusUpdate()
    }
    
    private fun initViews() {
        statusText = findViewById(R.id.status_text)
        debugText = findViewById(R.id.debug_text)
        testButton = findViewById(R.id.test_button)
        simulateButton = findViewById(R.id.simulate_button)
        
        testButton.setOnClickListener {
            runStationaryLogicTest()
        }
        
        simulateButton.setOnClickListener {
            simulateActiveToIndoorTransition()
        }
    }
    
    private fun bindGpsService() {
        val intent = Intent(this, GpsTrackingService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun startStatusUpdate() {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope.launch {
            while (isActive) {
                updateStatus()
                delay(2000) // 每2秒更新一次
            }
        }
    }
    
    private fun updateStatus() {
        if (!isServiceBound || gpsService == null) {
            statusText.text = "GPS服务未连接"
            return
        }
        
        val service = gpsService!!
        val currentState = service.getCurrentState()
        val isGpsAvailable = service.isGpsAvailable()
        val stepCount = service.getStepCount()
        val lastLocation = service.getLastLocation()
        
        val statusInfo = """
            当前状态: ${getStateText(currentState)}
            GPS可用: $isGpsAvailable
            步数: $stepCount
            最后位置: ${lastLocation?.let { "${it.latitude}, ${it.longitude}" } ?: "无"}
        """.trimIndent()
        
        statusText.text = statusInfo
        
        // 获取调试信息
        val debugInfo = service.getDebugInfo()
        val debugTextContent = debugInfo.entries.joinToString("\n") { (key, value) ->
            "$key: $value"
        }
        debugText.text = debugTextContent
    }
    
    private fun getStateText(state: TrackingState): String {
        return when (state) {
            TrackingState.INDOOR -> "室内模式"
            TrackingState.OUTDOOR -> "室外模式"
            TrackingState.ACTIVE -> "活跃状态"
            TrackingState.DRIVING -> "驾驶状态"
            TrackingState.DEEP_STATIONARY -> "深度静止模式"
        }
    }
    
    private fun runStationaryLogicTest() {
        Log.d(TAG, "开始运行室内状态切换逻辑测试")
        
        if (!isServiceBound || gpsService == null) {
            Log.e(TAG, "GPS服务未连接，无法运行测试")
            return
        }
        
        val service = gpsService!!
        val testLogic = TestStationaryLogic(this)
        
        // 在后台线程运行测试
        CoroutineScope(Dispatchers.IO).launch {
            try {
                testLogic.runFullTestSuite()
                
                // 在主线程更新UI
                withContext(Dispatchers.Main) {
                    debugText.text = "测试完成，请查看日志输出"
                }
            } catch (e: Exception) {
                Log.e(TAG, "测试运行失败", e)
                withContext(Dispatchers.Main) {
                    debugText.text = "测试失败: ${e.message}"
                }
            }
        }
    }
    
    private fun simulateActiveToIndoorTransition() {
        Log.d(TAG, "开始模拟活跃状态到室内状态的切换")
        
        if (!isServiceBound || gpsService == null) {
            Log.e(TAG, "GPS服务未连接，无法模拟")
            return
        }
        
        val service = gpsService!!
        
        // 模拟步数触发切换到活跃状态
        Log.d(TAG, "模拟步数触发切换到活跃状态")
        
        // 创建模拟位置
        val mockLocation = createMockLocation(39.9042, 116.4074, 10.0f)
        
        // 模拟GPS位置更新
        service.onLocationChanged(mockLocation)
        
        // 等待一段时间让状态更新
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            updateStatus()
            
            // 模拟5分钟后的情况
            delay(2000)
            Log.d(TAG, "模拟5分钟后的距离检测")
            
            // 创建相同位置（应该触发切换到室内）
            val sameLocation = createMockLocation(39.9042, 116.4074, 10.0f)
            service.onLocationChanged(sameLocation)
            
            delay(1000)
            updateStatus()
        }
    }
    
    private fun createMockLocation(latitude: Double, longitude: Double, accuracy: Float): Location {
        val location = Location("mock")
        location.latitude = latitude
        location.longitude = longitude
        location.accuracy = accuracy
        location.time = System.currentTimeMillis()
        return location
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}
