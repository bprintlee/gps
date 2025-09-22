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
import com.gpstracker.app.utils.GpsAccuracyOptimizer
import com.gpstracker.app.database.GpsDatabase
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

class GpsTrackingService : Service(), LocationListener, SensorEventListener {
    
    private val binder = GpsTrackingBinder()
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private lateinit var gpxExporter: GpxExporter
    private lateinit var mqttManager: MqttManager
    private lateinit var gpsAccuracyOptimizer: GpsAccuracyOptimizer
    private lateinit var gpsDatabase: GpsDatabase
    private lateinit var logManager: com.gpstracker.app.utils.LogManager
    
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
    
    // 深度静止状态配置
    private val deepStationaryTimeoutMs = 300000L // 5分钟无移动进入深度静止
    private val deepStationaryStepThreshold = 30 // 30步阈值退出深度静止
    private val deepStationaryAccelerationThreshold = 1.5f // 深度静止状态加速度阈值
    private var lastMovementTime = 0L // 最后移动时间
    private var initialStepCount = 0 // 进入深度静止时的步数
    private var isInDeepStationary = false // 是否处于深度静止状态
    
    // 深度静止状态统计
    private var deepStationaryEntryTime = 0L // 进入深度静止状态的时间
    private var totalDeepStationaryTime = 0L // 累计深度静止时间
    
    // 驾驶状态检测配置
    private val drivingSpeedThreshold = 7.0f // 7km/h速度阈值进入驾驶模式
    private val drivingStationaryTimeoutMs = 300000L // 5分钟无移动退出驾驶模式
    private val drivingStationaryDistanceThreshold = 100.0f // 100米距离阈值
    private var drivingEntryTime = 0L // 进入驾驶状态的时间
    private var drivingLastLocation: Location? = null // 驾驶状态下的最后位置
    private var isInDrivingMode = false // 是否处于驾驶模式
    
    // 省电模式配置 - 默认开启省电模式
    private var isPowerSaveMode = true
    private var gpsUpdateInterval = 10000L // 默认10秒间隔（省电模式）
    private var sensorUpdateInterval = SensorManager.SENSOR_DELAY_UI // 默认UI延迟
    private var stateCheckInterval = 15000L // 默认15秒检查一次状态（省电模式）
    
    // 环境检测配置
    private var lastEnvironmentCheck = 0L
    private val environmentCheckInterval = 60000L // 1分钟检查一次环境
    private var lastDetectedMode: GpsAccuracyOptimizer.AccuracyMode? = null
    
    // 最后位置信息
    private var lastLocation: Location? = null
    
    inner class GpsTrackingBinder : Binder() {
        fun getService(): GpsTrackingService = this@GpsTrackingService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            android.util.Log.d("GpsTrackingService", "=== 开始创建GPS跟踪服务 ===")
            
            // 初始化深度静止状态相关变量
            lastMovementTime = System.currentTimeMillis()
            isInDeepStationary = false
            initialStepCount = 0
            
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            android.util.Log.d("GpsTrackingService", "位置管理器初始化完成")
            
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            android.util.Log.d("GpsTrackingService", "传感器管理器初始化完成")
            
            gpxExporter = GpxExporter(this)
            android.util.Log.d("GpsTrackingService", "GPX导出器初始化完成")
            
            android.util.Log.d("GpsTrackingService", "开始初始化日志管理器...")
            logManager = com.gpstracker.app.utils.LogManager(this)
            android.util.Log.d("GpsTrackingService", "日志管理器初始化完成")
            
            android.util.Log.d("GpsTrackingService", "开始初始化MQTT管理器...")
            mqttManager = MqttManager(this)
            android.util.Log.d("GpsTrackingService", "MQTT管理器初始化完成")
            
            android.util.Log.d("GpsTrackingService", "开始初始化GPS精度优化器...")
            gpsAccuracyOptimizer = GpsAccuracyOptimizer(this)
            android.util.Log.d("GpsTrackingService", "GPS精度优化器初始化完成")
            
            gpsDatabase = GpsDatabase(this)
            android.util.Log.d("GpsTrackingService", "数据库初始化完成")
            
            // 检测省电模式
            checkPowerSaveMode()
            setupSensors()
            createNotificationChannel()
            
            android.util.Log.d("GpsTrackingService", "=== GPS跟踪服务创建成功 ===")
        } catch (e: Exception) {
            android.util.Log.e("GpsTrackingService", "=== GPS跟踪服务创建失败 ===", e)
            android.util.Log.e("GpsTrackingService", "异常类型: ${e.javaClass.simpleName}")
            android.util.Log.e("GpsTrackingService", "异常消息: ${e.message}")
            android.util.Log.e("GpsTrackingService", "异常堆栈: ${e.stackTraceToString()}")
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
        android.util.Log.d("GpsTrackingService", "=== 开始启动MQTT连接 ===")
        android.util.Log.d("GpsTrackingService", "当前Android版本: ${Build.VERSION.SDK_INT}")
        
        try {
            mqttManager?.let { manager ->
                android.util.Log.d("GpsTrackingService", "MQTT管理器存在，开始连接...")
                manager.connect()
                android.util.Log.d("GpsTrackingService", "MQTT连接请求已发送")
            } ?: run {
                android.util.Log.e("GpsTrackingService", "MQTT管理器为null，无法连接")
            }
        } catch (e: Exception) {
            android.util.Log.e("GpsTrackingService", "=== MQTT连接启动失败 ===", e)
            android.util.Log.e("GpsTrackingService", "MQTT异常类型: ${e.javaClass.simpleName}")
            android.util.Log.e("GpsTrackingService", "MQTT异常消息: ${e.message}")
            android.util.Log.e("GpsTrackingService", "MQTT异常堆栈: ${e.stackTraceToString()}")
            android.util.Log.w("GpsTrackingService", "MQTT连接失败，继续GPS跟踪功能")
        }
        
        return START_STICKY
    }
    
    private fun setupSensors() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }
    
    private fun startLocationUpdates() {
        try {
            // 深度静止状态下完全停止位置更新
            if (isInDeepStationary) {
                android.util.Log.d("GpsTrackingService", "深度静止状态，跳过位置更新")
                return
            }
            
            // 检查是否需要重新检测环境
            val currentTime = System.currentTimeMillis()
            val shouldCheckEnvironment = (currentTime - lastEnvironmentCheck) > environmentCheckInterval
            
            // 使用GPS精度优化器获取最佳配置
            val accuracyMode = if (isPowerSaveMode) {
                android.util.Log.d("GpsTrackingService", "使用省电模式")
                GpsAccuracyOptimizer.AccuracyMode.POWER_SAVE
            } else {
                // 使用智能推荐模式，自动检测室内环境
                android.util.Log.d("GpsTrackingService", "使用智能推荐模式，自动检测环境")
                val detectedMode = gpsAccuracyOptimizer.getSmartRecommendedMode()
                
                // 如果环境发生变化，记录并重新启动位置更新
                if (shouldCheckEnvironment && lastDetectedMode != null && lastDetectedMode != detectedMode) {
                    android.util.Log.d("GpsTrackingService", "环境变化检测: ${lastDetectedMode} -> $detectedMode")
                    android.util.Log.d("GpsTrackingService", "环境变化，重新启动位置更新")
                    
                    // 更新检测时间
                    lastEnvironmentCheck = currentTime
                    lastDetectedMode = detectedMode
                    
                    // 重新启动位置更新
                    stopLocationUpdates()
                    startLocationUpdates()
                    return
                }
                
                // 更新检测时间
                if (shouldCheckEnvironment) {
                    lastEnvironmentCheck = currentTime
                    lastDetectedMode = detectedMode
                }
                
                detectedMode
            }
            
            val config = gpsAccuracyOptimizer.getAccuracyConfig(accuracyMode)
            val bestProvider = gpsAccuracyOptimizer.getLocationProviderForMode(accuracyMode)
            
            android.util.Log.d("GpsTrackingService", "使用精度模式: $accuracyMode")
            android.util.Log.d("GpsTrackingService", "位置提供者: $bestProvider")
            android.util.Log.d("GpsTrackingService", "更新间隔: ${config.interval}ms")
            android.util.Log.d("GpsTrackingService", "最小位移: ${config.smallestDisplacement}m")
            
            // 室内模式特殊提示
            if (accuracyMode == GpsAccuracyOptimizer.AccuracyMode.INDOOR_NAVIGATION) {
                android.util.Log.d("GpsTrackingService", "室内模式：禁用GPS，仅使用网络定位以节省电量")
            }
            
            locationManager.requestLocationUpdates(
                bestProvider,
                config.interval,
                config.smallestDisplacement,
                this,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            android.util.Log.e("GpsTrackingService", "启动位置更新失败", e)
        }
    }
    
    private fun startSensorUpdates() {
        // 根据省电模式和深度静止状态调整传感器更新频率
        val sensorDelay = when {
            isInDeepStationary -> SensorManager.SENSOR_DELAY_NORMAL // 深度静止状态使用正常延迟
            isPowerSaveMode -> SensorManager.SENSOR_DELAY_UI // 省电模式使用UI延迟
            else -> SensorManager.SENSOR_DELAY_NORMAL // 正常模式使用正常延迟
        }
        
        android.util.Log.d("GpsTrackingService", "启动传感器更新 - 延迟: $sensorDelay, 深度静止: $isInDeepStationary")
        
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
                
                // 根据深度静止状态调整检查频率
                val checkInterval = if (isInDeepStationary) {
                    stateCheckInterval * 2 // 深度静止状态下减少检查频率
                } else {
                    stateCheckInterval
                }
                
                delay(checkInterval)
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
        
        // 检查是否应该进入深度静止状态
        val shouldEnterDeepStationary = !isInDeepStationary && 
                                       currentState == TrackingState.INDOOR && 
                                       (currentTime - lastMovementTime) > deepStationaryTimeoutMs
        
        // 检查是否应该退出深度静止状态
        val shouldExitDeepStationary = isInDeepStationary && (
            (stepCount - initialStepCount) >= deepStationaryStepThreshold ||
            lastAcceleration > deepStationaryAccelerationThreshold
        )
        
        when {
            // 进入深度静止状态
            shouldEnterDeepStationary -> {
                currentState = TrackingState.DEEP_STATIONARY
                isInDeepStationary = true
                initialStepCount = stepCount
                deepStationaryEntryTime = currentTime
                android.util.Log.d("GpsTrackingService", "进入深度静止状态 - 步数: $stepCount, 加速度: $lastAcceleration")
                
                // 深度静止状态下停止GPS更新以节省电量
                stopLocationUpdates()
                
                // 重新启动传感器更新以应用深度静止状态的优化设置
                stopSensorUpdates()
                startSensorUpdates()
            }
            
            // 退出深度静止状态
            shouldExitDeepStationary -> {
                currentState = TrackingState.INDOOR
                isInDeepStationary = false
                lastMovementTime = currentTime
                
                // 计算本次深度静止状态持续时间
                val sessionTime = currentTime - deepStationaryEntryTime
                totalDeepStationaryTime += sessionTime
                
                android.util.Log.d("GpsTrackingService", "退出深度静止状态 - 步数增加: ${stepCount - initialStepCount}, 加速度: $lastAcceleration, 本次持续时间: ${sessionTime/1000}秒")
                
                // 重新启动GPS更新并进行环境检测
                startLocationUpdates()
                
                // 重新启动传感器更新以恢复正常模式
                stopSensorUpdates()
                startSensorUpdates()
            }
            
            // 深度静止状态下的监测
            isInDeepStationary -> {
                // 保持深度静止状态，仅监测步数和加速度
                currentState = TrackingState.DEEP_STATIONARY
                android.util.Log.v("GpsTrackingService", "深度静止状态监测 - 步数: $stepCount, 加速度: $lastAcceleration")
            }
            
            // 正常状态转换逻辑
            gpsTimeout -> {
                currentState = TrackingState.INDOOR
                isGpsAvailable = false
                lastMovementTime = currentTime
                // 室内状态时降低GPS更新频率
                if (!isPowerSaveMode) {
                    stopLocationUpdates()
                    startLocationUpdates()
                }
            }
            stepCount >= stepThreshold -> {
                currentState = TrackingState.ACTIVE
                isGpsAvailable = true
                lastMovementTime = currentTime
            }
            // 检查是否应该进入驾驶状态（基于速度）
            shouldEnterDrivingMode() -> {
                currentState = TrackingState.DRIVING
                isGpsAvailable = true
                lastMovementTime = currentTime
                isInDrivingMode = true
                drivingEntryTime = currentTime
                drivingLastLocation = lastLocation
                android.util.Log.d("GpsTrackingService", "进入驾驶状态 - 速度: ${lastLocation?.speed?.times(3.6f)} km/h")
            }
            
            // 检查是否应该退出驾驶状态（基于距离和时间）
            shouldExitDrivingMode() -> {
                currentState = TrackingState.INDOOR
                isInDrivingMode = false
                lastMovementTime = currentTime
                android.util.Log.d("GpsTrackingService", "退出驾驶状态 - 进行环境检测")
                // 重新启动GPS更新并进行环境检测
                startLocationUpdates()
            }
            isGpsAvailable -> {
                currentState = TrackingState.OUTDOOR
                lastMovementTime = currentTime
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
    
    /**
     * 检查是否应该进入驾驶模式
     */
    private fun shouldEnterDrivingMode(): Boolean {
        return !isInDrivingMode && 
               lastLocation != null && 
               lastLocation!!.hasSpeed() && 
               lastLocation!!.speed * 3.6f >= drivingSpeedThreshold // 转换为km/h
    }
    
    /**
     * 检查是否应该退出驾驶模式
     */
    private fun shouldExitDrivingMode(): Boolean {
        if (!isInDrivingMode) return false
        
        val currentTime = System.currentTimeMillis()
        val timeInDriving = currentTime - drivingEntryTime
        
        // 如果驾驶时间超过5分钟
        if (timeInDriving > drivingStationaryTimeoutMs) {
            // 检查移动距离
            val distance = if (drivingLastLocation != null && lastLocation != null) {
                drivingLastLocation!!.distanceTo(lastLocation!!)
            } else {
                Float.MAX_VALUE
            }
            
            // 如果移动距离小于100米，退出驾驶模式
            if (distance < drivingStationaryDistanceThreshold) {
                android.util.Log.d("GpsTrackingService", "驾驶模式超时且移动距离不足 - 距离: ${distance}m, 时间: ${timeInDriving/1000}秒")
                return true
            } else {
                // 更新驾驶状态下的最后位置
                drivingLastLocation = lastLocation
                drivingEntryTime = currentTime
            }
        }
        
        return false
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
        
        // 使用精度优化器验证位置精度
        val accuracy = gpsAccuracyOptimizer.validateLocationAccuracy(location)
        android.util.Log.d("GpsTrackingService", "位置精度: $accuracy, 精度值: ${location.accuracy}m")
        
        // 如果精度太差，记录但不处理
        if (accuracy == com.gpstracker.app.utils.LocationAccuracy.UNRELIABLE) {
            android.util.Log.w("GpsTrackingService", "位置精度不可靠，跳过处理: ${location.accuracy}m")
            return
        }
        
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
        // 只有精度小于60米的位置才发送MQTT消息
        if (location.accuracy < 60.0f) {
            try {
                mqttManager.publishLocation(gpsData)
                android.util.Log.d("GpsTrackingService", "MQTT位置发送成功 - 精度: ${location.accuracy}m")
            } catch (e: Exception) {
                android.util.Log.w("GpsTrackingService", "MQTT发送失败，继续GPS跟踪功能", e)
            }
        } else {
            android.util.Log.d("GpsTrackingService", "位置精度不足，跳过MQTT发送 - 精度: ${location.accuracy}m")
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
                val currentAcceleration = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                
                // 使用平滑算法减少噪声
                lastAcceleration = lastAcceleration * 0.8f + currentAcceleration * 0.2f
                
                // 在深度静止状态下，记录明显的加速度变化
                if (isInDeepStationary && currentAcceleration > deepStationaryAccelerationThreshold) {
                    android.util.Log.d("GpsTrackingService", "深度静止状态检测到明显加速度: $currentAcceleration")
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val newStepCount = event.values[0].toInt()
                
                // 检查步数是否增加
                if (newStepCount > stepCount) {
                    val stepIncrease = newStepCount - stepCount
                    stepCount = newStepCount
                    
                    // 在深度静止状态下，记录步数增加
                    if (isInDeepStationary) {
                        val totalStepIncrease = stepCount - initialStepCount
                        android.util.Log.d("GpsTrackingService", "深度静止状态检测到步数增加: +$stepIncrease, 总计: $totalStepIncrease")
                    }
                }
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
            TrackingState.DEEP_STATIONARY -> "深度静止模式"
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
    
    // 深度静止状态相关方法
    fun isInDeepStationary(): Boolean = isInDeepStationary
    fun getInitialStepCount(): Int = initialStepCount
    fun getStepIncreaseSinceDeepStationary(): Int = if (isInDeepStationary) stepCount - initialStepCount else 0
    fun getLastMovementTime(): Long = lastMovementTime
    fun getTimeInDeepStationary(): Long = if (isInDeepStationary) System.currentTimeMillis() - deepStationaryEntryTime else 0
    fun getTotalDeepStationaryTime(): Long = totalDeepStationaryTime + if (isInDeepStationary) getTimeInDeepStationary() else 0
    fun getDeepStationaryConfig(): Map<String, Any> = mapOf(
        "timeoutMs" to deepStationaryTimeoutMs,
        "stepThreshold" to deepStationaryStepThreshold,
        "accelerationThreshold" to deepStationaryAccelerationThreshold
    )
    
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
    
    // MQTT测试功能已删除
        
    
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
