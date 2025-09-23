package com.gpstracker.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.gpstracker.app.utils.SettingsManager

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var sharedPreferences: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_settings)
            
            // 初始化SharedPreferences
            sharedPreferences = getSharedPreferences("gps_tracking_settings", Context.MODE_PRIVATE)
            
            // 设置Toolbar
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "设置"
            
            // 初始化设置项
            initializeSettings()
            
            Log.d("SettingsActivity", "设置页面初始化成功")
            
        } catch (e: Exception) {
            Log.e("SettingsActivity", "设置页面初始化失败", e)
            Toast.makeText(this, "设置页面初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun initializeSettings() {
        try {
            // GPS超时设置
            initializeGpsTimeoutSetting()
            
            // 省电模式设置
            initializePowerSaveSetting()
            
            // 活跃状态设置
            initializeActiveStateSettings()
            
            // 室内状态设置
            initializeIndoorStateSettings()
            
            // 驾驶状态设置
            initializeDrivingStateSettings()
            
            // 深度静止状态设置
            initializeDeepStationarySettings()
            
            // 环境检测设置
            initializeEnvironmentSettings()
            
            Log.d("SettingsActivity", "所有设置项初始化完成")
            
        } catch (e: Exception) {
            Log.e("SettingsActivity", "设置项初始化失败", e)
            Toast.makeText(this, "设置项初始化失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun initializeGpsTimeoutSetting() {
        val gpsTimeoutSeekBar = findViewById<SeekBar>(R.id.gpsTimeoutSeekBar)
        val gpsTimeoutValue = findViewById<TextView>(R.id.gpsTimeoutValue)
        
        if (gpsTimeoutSeekBar != null && gpsTimeoutValue != null) {
            val currentTimeout = SettingsManager.getGpsTimeoutMs(this)
            gpsTimeoutSeekBar.progress = (currentTimeout / 1000).toInt()
            gpsTimeoutValue.text = "${currentTimeout / 1000}秒"
            
            gpsTimeoutSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    gpsTimeoutValue.text = "${progress}秒"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val currentProgress = seekBar?.progress ?: 30
                    val newTimeout = currentProgress * 1000L
                    sharedPreferences.edit().putLong("gps_timeout_ms", newTimeout).apply()
                    Log.d("SettingsActivity", "GPS超时设置更新: ${newTimeout}ms")
                }
            })
        }
    }
    
    private fun initializePowerSaveSetting() {
        val powerSaveSwitch = findViewById<Switch>(R.id.powerSaveSwitch)
        if (powerSaveSwitch != null) {
            val currentMode = SettingsManager.isPowerSaveMode(this)
            powerSaveSwitch.isChecked = currentMode
            
            powerSaveSwitch.setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit().putBoolean("power_save_mode", isChecked).apply()
                Log.d("SettingsActivity", "省电模式设置更新: $isChecked")
            }
        }
    }
    
    private fun initializeActiveStateSettings() {
        // 活跃状态超时时间
        val activeTimeoutSeekBar = findViewById<SeekBar>(R.id.activeTimeoutSeekBar)
        val activeTimeoutValue = findViewById<TextView>(R.id.activeTimeoutValue)
        
        if (activeTimeoutSeekBar != null && activeTimeoutValue != null) {
            val currentTimeout = SettingsManager.getActiveStateTimeoutMs(this)
            activeTimeoutSeekBar.progress = (currentTimeout / 1000).toInt()
            activeTimeoutValue.text = "${currentTimeout / 1000}秒"
            
            activeTimeoutSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    activeTimeoutValue.text = "${progress}秒"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val currentProgress = seekBar?.progress ?: 30
                    val newTimeout = currentProgress * 1000L
                    sharedPreferences.edit().putLong("active_state_timeout_ms", newTimeout).apply()
                    Log.d("SettingsActivity", "活跃状态超时设置更新: ${newTimeout}ms")
                }
            })
        }
        
        // 活跃状态距离阈值
        val activeDistanceSeekBar = findViewById<SeekBar>(R.id.activeDistanceSeekBar)
        val activeDistanceValue = findViewById<TextView>(R.id.activeDistanceValue)
        
        if (activeDistanceSeekBar != null && activeDistanceValue != null) {
            val currentDistance = SettingsManager.getActiveStateDistanceThreshold(this)
            activeDistanceSeekBar.progress = currentDistance.toInt()
            activeDistanceValue.text = "${currentDistance}米"
            
            activeDistanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    activeDistanceValue.text = "${progress}米"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val currentProgress = seekBar?.progress ?: 50
                    sharedPreferences.edit().putFloat("active_state_distance_threshold", currentProgress.toFloat()).apply()
                    Log.d("SettingsActivity", "活跃状态距离阈值设置更新: ${currentProgress}米")
                }
            })
        }
    }
    
    private fun initializeIndoorStateSettings() {
        // 室内到室外步数阈值
        val indoorStepSeekBar = findViewById<SeekBar>(R.id.indoorStepSeekBar)
        val indoorStepValue = findViewById<TextView>(R.id.indoorStepValue)
        
        if (indoorStepSeekBar != null && indoorStepValue != null) {
            val currentStep = SettingsManager.getIndoorToOutdoorStepThreshold(this)
            indoorStepSeekBar.progress = currentStep
            indoorStepValue.text = "${currentStep}步"
            
            indoorStepSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    indoorStepValue.text = "${progress}步"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val currentProgress = seekBar?.progress ?: 10
                    sharedPreferences.edit().putInt("indoor_to_outdoor_step_threshold", currentProgress).apply()
                    Log.d("SettingsActivity", "室内到室外步数阈值设置更新: ${currentProgress}步")
                }
            })
        }
    }
    
    private fun initializeDrivingStateSettings() {
        // 活跃到驾驶速度阈值
        val drivingSpeedSeekBar = findViewById<SeekBar>(R.id.drivingSpeedSeekBar)
        val drivingSpeedValue = findViewById<TextView>(R.id.drivingSpeedValue)
        
        if (drivingSpeedSeekBar != null && drivingSpeedValue != null) {
            val currentSpeed = SettingsManager.getActiveToDrivingSpeedThreshold(this)
            drivingSpeedSeekBar.progress = (currentSpeed * 10).toInt()
            drivingSpeedValue.text = "${currentSpeed}km/h"
            
            drivingSpeedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val speed = progress / 10.0f
                    drivingSpeedValue.text = "${speed}km/h"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val currentProgress = seekBar?.progress ?: 50
                    val speed = currentProgress / 10.0f
                    sharedPreferences.edit().putFloat("active_to_driving_speed_threshold", speed).apply()
                    Log.d("SettingsActivity", "活跃到驾驶速度阈值设置更新: ${speed}km/h")
                }
            })
        }
    }
    
    private fun initializeDeepStationarySettings() {
        // 深度静止加速度阈值
        val deepAccelerationSeekBar = findViewById<SeekBar>(R.id.deepAccelerationSeekBar)
        val deepAccelerationValue = findViewById<TextView>(R.id.deepAccelerationValue)
        
        if (deepAccelerationSeekBar != null && deepAccelerationValue != null) {
            val currentAcceleration = SettingsManager.getDeepStationaryAccelerationThreshold(this)
            deepAccelerationSeekBar.progress = (currentAcceleration * 10).toInt()
            deepAccelerationValue.text = "${currentAcceleration}m/s²"
            
            deepAccelerationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val acceleration = progress / 10.0f
                    deepAccelerationValue.text = "${acceleration}m/s²"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val currentProgress = seekBar?.progress ?: 5
                    val acceleration = currentProgress / 10.0f
                    sharedPreferences.edit().putFloat("deep_stationary_acceleration_threshold", acceleration).apply()
                    Log.d("SettingsActivity", "深度静止加速度阈值设置更新: ${acceleration}m/s²")
                }
            })
        }
    }
    
    private fun initializeEnvironmentSettings() {
        // 环境检测间隔
        val envIntervalSeekBar = findViewById<SeekBar>(R.id.envIntervalSeekBar)
        val envIntervalValue = findViewById<TextView>(R.id.envIntervalValue)
        
        if (envIntervalSeekBar != null && envIntervalValue != null) {
            val currentInterval = SettingsManager.getEnvironmentCheckIntervalMs(this)
            envIntervalSeekBar.progress = (currentInterval / 1000).toInt()
            envIntervalValue.text = "${currentInterval / 1000}秒"
            
            envIntervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    envIntervalValue.text = "${progress}秒"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val currentProgress = seekBar?.progress ?: 30
                    val newInterval = currentProgress * 1000L
                    sharedPreferences.edit().putLong("environment_check_interval_ms", newInterval).apply()
                    Log.d("SettingsActivity", "环境检测间隔设置更新: ${newInterval}ms")
                }
            })
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}