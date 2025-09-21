package com.gpstracker.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gpstracker.app.MainActivity
import com.gpstracker.app.R
import com.gpstracker.app.model.GpsData
import com.gpstracker.app.model.TrackingState
import com.gpstracker.app.utils.GpxExporter
import com.gpstracker.app.utils.MqttManager
import com.gpstracker.app.database.GpsDatabase
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

class GpsTrackingService : Service(), LocationListener, SensorEventListener {
    
    private val binder = GpsTrackingBinder()
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private lateinit var gpxExporter: GpxExporter
    private lateinit var mqttManager: MqttManager
    private lateinit var gpsDatabase: GpsDatabase
    
    // 传感器
    private var accelerometer: Sensor? = null
    private var stepCounter: Sensor? = null
    
    // 状态管理
    private var currentState = TrackingState.INDOOR
    private var isGpsAvailable = false
    private var lastGpsTime = 0L
    private var stepCount = 0
    private var lastAcceleration = 0f
    
    // 行程管理
    private var currentTripId: String? = null
    private var isTripActive = false
    
    // 数据收集
    private val gpsDataQueue = ConcurrentLinkedQueue<GpsData>()
    private val allGpsData = mutableListOf<GpsData>() // 累积所有GPS数据
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 配置参数 - 优化省电
    private val gpsTimeoutMs = 30000L // 30秒GPS超时
    private val stepThreshold = 20 // 20步阈值
    private val accelerationThreshold = 2.0f // 加速度阈值
    
    // 省电模式配置 - 默认开启省电模式
    private var isPowerSaveMode = true
    private var gpsUpdateInterval = 10000L // 默认10秒间隔（省电模式）
    private var sensorUpdateInterval = SensorManager.SENSOR_DELAY_UI // 默认UI延迟
    private var stateCheckInterval = 15000L // 默认15秒检查一次状态（省电模式）
    
    // 最后位置信息
    private var lastLocation: Location? = null
    
    inner class GpsTrackingBinder : Binder() {
        fun getService(): GpsTrackingService = this@GpsTrackingService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            gpxExporter = GpxExporter(this)
            mqttManager = MqttManager(this)
            gpsDatabase = GpsDatabase(this)
            
            // 检测省电模式
            checkPowerSaveMode()
            setupSensors()
            createNotificationChannel()
            
            android.util.Log.d("GpsTrackingService", "Service created successfully")
        } catch (e: Exception) {
            android.util.Log.e("GpsTrackingService", "Service creation failed", e)
            throw e
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, createNotification())
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, createNotification())
        }
        startLocationUpdates()
        startSensorUpdates()
        startStateMonitoring()
        
        // 启用MQTT功能
        try {
            mqttManager.connect()
            android.util.Log.d("GpsTrackingService", "MQTT连接已启动")
        } catch (e: Exception) {
            android.util.Log.w("GpsTrackingService", "MQTT连接失败，继续GPS跟踪功能", e)
        }
        
        return START_STICKY
    }
    
    private fun setupSensors() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }
    
    private fun startLocationUpdates() {
        try {
            // 根据省电模式调整GPS更新频率
            val interval = if (isPowerSaveMode) gpsUpdateInterval * 2 else gpsUpdateInterval
            val distance = if (isPowerSaveMode) 5f else 1f // 省电模式降低精度要求
            
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                interval,
                distance,
                this,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    private fun startSensorUpdates() {
        // 根据省电模式调整传感器更新频率
        val sensorDelay = if (isPowerSaveMode) SensorManager.SENSOR_DELAY_UI else SensorManager.SENSOR_DELAY_NORMAL
        
        accelerometer?.let {
            sensorManager.registerListener(this, it, sensorDelay)
        }
        stepCounter?.let {
            sensorManager.registerListener(this, it, sensorDelay)
        }
    }
    
    private fun startStateMonitoring() {
        serviceScope.launch {
            while (isActive) {
                updateTrackingState()
                delay(stateCheckInterval) // 根据省电模式调整检查频率
            }
        }
        
        // 启动定时保存任务
        serviceScope.launch {
            while (isActive) {
                delay(30000) // 每30秒保存一次
                if (synchronized(allGpsData) { allGpsData.isNotEmpty() }) {
                    saveGpsData()
                }
            }
        }
    }
    
    private fun checkPowerSaveMode() {
        // 不再检查系统省电模式，使用应用内省电模式设置
        // 根据省电模式调整参数
        if (isPowerSaveMode) {
            gpsUpdateInterval = 5000L // 省电模式：5秒间隔（减少间隔以提高响应速度）
            stateCheckInterval = 5000L // 省电模式：5秒检查一次（减少间隔）
        } else {
            gpsUpdateInterval = 3000L // 持续记录模式：3秒间隔
            stateCheckInterval = 3000L // 持续记录模式：3秒检查一次
        }
    }
    
    private fun updateTrackingState() {
        val currentTime = System.currentTimeMillis()
        val gpsTimeout = (currentTime - lastGpsTime) > gpsTimeoutMs
        val previousState = currentState
        
        when {
            gpsTimeout -> {
                currentState = TrackingState.INDOOR
                isGpsAvailable = false
                // 室内状态时降低GPS更新频率
                if (!isPowerSaveMode) {
                    stopLocationUpdates()
                    startLocationUpdates()
                }
            }
            stepCount >= stepThreshold -> {
                currentState = TrackingState.ACTIVE
                isGpsAvailable = true
            }
            lastAcceleration > accelerationThreshold -> {
                currentState = TrackingState.DRIVING
                isGpsAvailable = true
            }
            isGpsAvailable -> {
                currentState = TrackingState.OUTDOOR
            }
            else -> {
                currentState = TrackingState.INDOOR
            }
        }
        
        // 行程管理逻辑
        manageTripState(previousState, currentState)
        
        updateNotification()
    }
    
    private fun manageTripState(previousState: TrackingState, newState: TrackingState) {
        // 移除自动行程管理逻辑，改为手动控制
        // 行程的开始和结束完全由用户控制
        android.util.Log.d("GpsTrackingService", "状态变化: $previousState -> $newState, 当前行程: $currentTripId, 活跃: $isTripActive")
    }
    
    private fun startNewTrip() {
        if (!isTripActive) {
            currentTripId = generateTripId()
            isTripActive = true
            android.util.Log.d("GpsTrackingService", "开始新行程: $currentTripId")
        } else {
            android.util.Log.d("GpsTrackingService", "行程已活跃，跳过开始新行程: $currentTripId")
        }
    }
    
    private fun endCurrentTrip() {
        if (isTripActive && currentTripId != null) {
            android.util.Log.d("GpsTrackingService", "结束行程: $currentTripId")
            isTripActive = false
            currentTripId = null
        } else {
            android.util.Log.d("GpsTrackingService", "没有活跃的行程需要结束")
        }
    }
    
    private fun generateTripId(): String {
        val timestamp = System.currentTimeMillis()
        val dateFormat = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
        return "trip_${dateFormat.format(java.util.Date(timestamp))}"
    }
    
    override fun onLocationChanged(location: Location) {
        lastGpsTime = System.currentTimeMillis()
        isGpsAvailable = true
        lastLocation = location // 保存最后位置
        
        val gpsData = GpsData(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy,
            timestamp = System.currentTimeMillis(),
            state = currentState,
            tripId = currentTripId
        )
        
        // 添加调试信息
        android.util.Log.d("GpsTrackingService", "创建GPS数据: tripId=$currentTripId, state=$currentState, isTripActive=$isTripActive, lat=${location.latitude}, lon=${location.longitude}")
        
        // 添加到队列和累积列表
        gpsDataQueue.offer(gpsData)
        synchronized(allGpsData) {
            allGpsData.add(gpsData)
        }
        
        // 保存到SQLite数据库
        try {
            gpsDatabase.insertGpsData(gpsData)
            android.util.Log.d("GpsTrackingService", "GPS数据已保存到数据库")
        } catch (e: Exception) {
            android.util.Log.e("GpsTrackingService", "保存GPS数据到数据库失败", e)
        }
        
        // 尝试发送到MQTT服务器（失败不影响GPS跟踪功能）
        try {
            mqttManager.publishLocation(gpsData)
        } catch (e: Exception) {
            android.util.Log.w("GpsTrackingService", "MQTT发送失败，继续GPS跟踪功能", e)
        }
        
        // 根据模式调整保存频率
        val saveThreshold = if (isPowerSaveMode) 5 else 3 // 省电模式5个点保存一次，持续记录模式3个点保存一次
        if (gpsDataQueue.size >= saveThreshold) {
            serviceScope.launch {
                saveGpsData()
            }
        }
        
        // 确保至少每30秒保存一次数据
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGpsTime > 30000) {
            serviceScope.launch {
                saveGpsData()
            }
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                lastAcceleration = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            }
            Sensor.TYPE_STEP_COUNTER -> {
                stepCount = event.values[0].toInt()
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    
    private suspend fun saveGpsData() {
        try {
            // 获取累积的所有数据的副本
            val allDataCopy = synchronized(allGpsData) {
                allGpsData.toList()
            }
            
            if (allDataCopy.isNotEmpty()) {
                // 在同步块外调用挂起函数
                gpxExporter.appendGpsData(allDataCopy)
                android.util.Log.d("GpsTrackingService", "成功保存 ${allDataCopy.size} 个GPS点到GPX文件")
            } else {
                android.util.Log.w("GpsTrackingService", "没有GPS数据需要保存")
            }
        } catch (e: Exception) {
            android.util.Log.e("GpsTrackingService", "保存GPX数据失败", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS跟踪服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "后台GPS位置跟踪服务"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS跟踪服务")
            .setContentText("状态: ${getStateText()}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        // 检查通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
    
    private fun getStateText(): String {
        return when (currentState) {
            TrackingState.INDOOR -> "室内模式"
            TrackingState.OUTDOOR -> "室外模式"
            TrackingState.ACTIVE -> "活跃状态"
            TrackingState.DRIVING -> "驾驶状态"
        }
    }
    
    private fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(this)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    private fun stopSensorUpdates() {
        sensorManager.unregisterListener(this)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        stopSensorUpdates()
        
        // 断开MQTT连接
        mqttManager.cleanup()
        
        // 保存所有剩余数据
        try {
            val allDataCopy = synchronized(allGpsData) {
                allGpsData.toList()
            }
            
            if (allDataCopy.isNotEmpty()) {
                // 使用 runBlocking 在非协程上下文中调用挂起函数
                runBlocking {
                    gpxExporter.appendGpsData(allDataCopy)
                }
                android.util.Log.d("GpsTrackingService", "服务停止时保存了 ${allDataCopy.size} 个GPS点")
            }
        } catch (e: Exception) {
            android.util.Log.e("GpsTrackingService", "服务停止时保存数据失败", e)
        }
        
        serviceScope.cancel()
    }
    
    fun getCurrentState(): TrackingState = currentState
    fun isGpsAvailable(): Boolean = isGpsAvailable
    fun getStepCount(): Int = stepCount
    fun getLastAcceleration(): Float = lastAcceleration
    fun isPowerSaveMode(): Boolean = isPowerSaveMode
    fun getLastLocation(): Location? = lastLocation
    fun getGpxDirectoryPath(): String = gpxExporter.getGpxDirectoryPath().absolutePath
    
    // 行程管理相关方法
    fun getCurrentTripId(): String? = currentTripId
    fun isTripActive(): Boolean = isTripActive
    
    // 手动开始行程
    fun startTrip(): Boolean {
        return if (!isTripActive) {
            startNewTrip()
            android.util.Log.d("GpsTrackingService", "用户手动开始行程: $currentTripId")
            true
        } else {
            android.util.Log.w("GpsTrackingService", "行程已活跃，无法开始新行程")
            false
        }
    }
    
    // 手动结束行程
    fun stopTrip(): Boolean {
        return if (isTripActive && currentTripId != null) {
            endCurrentTrip()
            android.util.Log.d("GpsTrackingService", "用户手动结束行程")
            true
        } else {
            android.util.Log.w("GpsTrackingService", "没有活跃的行程需要结束")
            false
        }
    }
    
    fun getAllTripIds(): List<String> = gpsDatabase.getAllTripIds()
    fun getGpsDataByTripId(tripId: String): List<GpsData> = gpsDatabase.getGpsDataByTripId(tripId)
    fun getGpsDataCount(): Int {
        return try {
            gpsDatabase.getGpsDataCount()
        } catch (e: Exception) {
            android.util.Log.e("GpsTrackingService", "获取GPS数据数量失败", e)
            synchronized(allGpsData) { allGpsData.size }
        }
    }
    
    suspend fun getGpxFileInfo(): Map<String, Any> = gpxExporter.getGpxFileInfo()
    
    // 从数据库导出GPX文件（按行程分组）
    suspend fun exportGpxFromDatabase(fileName: String? = null): String? {
        return try {
            gpxExporter.exportFromDatabase(gpsDatabase, fileName)
        } catch (e: Exception) {
            android.util.Log.e("GpsTrackingService", "从数据库导出GPX文件失败", e)
            null
        }
    }
    
    // 导出特定行程的GPX文件
    suspend fun exportTripGpx(tripId: String, fileName: String? = null): String? {
        return try {
            gpxExporter.exportTripGpx(gpsDatabase, tripId, fileName)
        } catch (e: Exception) {
            android.util.Log.e("GpsTrackingService", "导出行程 $tripId 的GPX文件失败", e)
            null
        }
    }
    
    // 从数据库导出指定时间范围的GPX文件
    suspend fun exportGpxFromDatabaseByDateRange(startTime: Long, endTime: Long, fileName: String? = null): String? {
        return try {
            gpxExporter.exportFromDatabaseByDateRange(gpsDatabase, startTime, endTime, fileName)
        } catch (e: Exception) {
            android.util.Log.e("GpsTrackingService", "从数据库导出时间范围GPX文件失败", e)
            null
        }
    }
    
    // 清空数据库
    fun clearDatabase() {
        try {
            gpsDatabase.clearAllData()
            android.util.Log.d("GpsTrackingService", "数据库已清空")
        } catch (e: Exception) {
            android.util.Log.e("GpsTrackingService", "清空数据库失败", e)
        }
    }
    
    // 获取MQTT连接状态
    fun getMqttConnectionInfo(): String {
        return try {
            mqttManager.getConnectionInfo()
        } catch (e: Exception) {
            android.util.Log.e("GpsTrackingService", "获取MQTT状态失败", e)
            "状态未知"
        }
    }
    
    // 手动切换省电模式
    fun togglePowerSaveMode() {
        isPowerSaveMode = !isPowerSaveMode
        checkPowerSaveMode()
        
        // 重新启动位置更新以应用新设置
        stopLocationUpdates()
        startLocationUpdates()
        
        // 重新启动传感器更新以应用新设置
        stopSensorUpdates()
        startSensorUpdates()
    }
    
    companion object {
        private const val CHANNEL_ID = "gps_tracking_channel"
        private const val NOTIFICATION_ID = 1
    }
}
