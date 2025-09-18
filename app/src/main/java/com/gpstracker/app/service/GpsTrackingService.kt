package com.gpstracker.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
import com.gpstracker.app.MainActivity
import com.gpstracker.app.R
import com.gpstracker.app.model.GpsData
import com.gpstracker.app.model.TrackingState
import com.gpstracker.app.utils.GpxExporter
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

class GpsTrackingService : Service(), LocationListener, SensorEventListener {
    
    private val binder = GpsTrackingBinder()
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private lateinit var gpxExporter: GpxExporter
    
    // 传感器
    private var accelerometer: Sensor? = null
    private var stepCounter: Sensor? = null
    
    // 状态管理
    private var currentState = TrackingState.INDOOR
    private var isGpsAvailable = false
    private var lastGpsTime = 0L
    private var stepCount = 0
    private var lastAcceleration = 0f
    
    // 数据收集
    private val gpsDataQueue = ConcurrentLinkedQueue<GpsData>()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 配置参数
    private val gpsTimeoutMs = 30000L // 30秒GPS超时
    private val stepThreshold = 20 // 20步阈值
    private val accelerationThreshold = 2.0f // 加速度阈值
    
    inner class GpsTrackingBinder : Binder() {
        fun getService(): GpsTrackingService = this@GpsTrackingService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gpxExporter = GpxExporter(this)
        
        setupSensors()
        createNotificationChannel()
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
        
        return START_STICKY
    }
    
    private fun setupSensors() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }
    
    private fun startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // 1秒更新间隔
                1f, // 1米精度
                this,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    private fun startSensorUpdates() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepCounter?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    
    private fun startStateMonitoring() {
        serviceScope.launch {
            while (isActive) {
                updateTrackingState()
                delay(5000) // 每5秒检查一次状态
            }
        }
    }
    
    private fun updateTrackingState() {
        val currentTime = System.currentTimeMillis()
        val gpsTimeout = (currentTime - lastGpsTime) > gpsTimeoutMs
        
        when {
            gpsTimeout -> {
                currentState = TrackingState.INDOOR
                isGpsAvailable = false
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
        
        updateNotification()
    }
    
    override fun onLocationChanged(location: Location) {
        lastGpsTime = System.currentTimeMillis()
        isGpsAvailable = true
        
        val gpsData = GpsData(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy,
            timestamp = System.currentTimeMillis(),
            state = currentState
        )
        
        gpsDataQueue.offer(gpsData)
        
        // 定期保存数据
        if (gpsDataQueue.size >= 10) {
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
        val dataList = mutableListOf<GpsData>()
        repeat(10) {
            gpsDataQueue.poll()?.let { dataList.add(it) }
        }
        
        if (dataList.isNotEmpty()) {
            gpxExporter.appendGpsData(dataList)
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
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun getStateText(): String {
        return when (currentState) {
            TrackingState.INDOOR -> "室内模式"
            TrackingState.OUTDOOR -> "室外模式"
            TrackingState.ACTIVE -> "活跃状态"
            TrackingState.DRIVING -> "驾驶状态"
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        
        // 保存剩余数据
        serviceScope.launch {
            saveGpsData()
        }
    }
    
    fun getCurrentState(): TrackingState = currentState
    fun isGpsAvailable(): Boolean = isGpsAvailable
    fun getStepCount(): Int = stepCount
    fun getLastAcceleration(): Float = lastAcceleration
    
    companion object {
        private const val CHANNEL_ID = "gps_tracking_channel"
        private const val NOTIFICATION_ID = 1
    }
}
