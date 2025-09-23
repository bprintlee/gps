package com.gpstracker.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.gpstracker.app.R

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var sharedPreferences: SharedPreferences
    
    // GPS超时设置
    private lateinit var gpsTimeoutSeekBar: SeekBar
    private lateinit var gpsTimeoutValue: TextView
    
    // 活跃状态设置
    private lateinit var activeStateTimeoutSeekBar: SeekBar
    private lateinit var activeStateTimeoutValue: TextView
    private lateinit var activeStateDistanceSeekBar: SeekBar
    private lateinit var activeStateDistanceValue: TextView
    
    // 步数阈值设置
    private lateinit var stepThresholdSeekBar: SeekBar
    private lateinit var stepThresholdValue: TextView
    
    // 加速度阈值设置
    private lateinit var accelerationThresholdSeekBar: SeekBar
    private lateinit var accelerationThresholdValue: TextView
    
    // 深度静止设置
    private lateinit var deepStationaryTimeoutSeekBar: SeekBar
    private lateinit var deepStationaryTimeoutValue: TextView
    private lateinit var deepStationaryStepSeekBar: SeekBar
    private lateinit var deepStationaryStepValue: TextView
    private lateinit var deepStationaryAccelerationSeekBar: SeekBar
    private lateinit var deepStationaryAccelerationValue: TextView
    
    // 驾驶模式设置
    private lateinit var drivingSpeedSeekBar: SeekBar
    private lateinit var drivingSpeedValue: TextView
    private lateinit var drivingStationaryTimeoutSeekBar: SeekBar
    private lateinit var drivingStationaryTimeoutValue: TextView
    private lateinit var drivingStationaryDistanceSeekBar: SeekBar
    private lateinit var drivingStationaryDistanceValue: TextView
    
    // 环境检测设置
    private lateinit var environmentCheckSeekBar: SeekBar
    private lateinit var environmentCheckValue: TextView
    
    // 省电模式设置
    private lateinit var powerSaveSwitch: Switch
    private lateinit var gpsUpdateIntervalSeekBar: SeekBar
    private lateinit var gpsUpdateIntervalValue: TextView
    private lateinit var stateCheckIntervalSeekBar: SeekBar
    private lateinit var stateCheckIntervalValue: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GPS跟踪设置"
        
        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences("gps_tracking_settings", Context.MODE_PRIVATE)
        
        // 初始化视图
        initViews()
        
        // 加载当前设置
        loadCurrentSettings()
        
        // 设置监听器
        setupListeners()
    }
    
    private fun initViews() {
        // GPS超时设置
        gpsTimeoutSeekBar = findViewById(R.id.gpsTimeoutSeekBar)
        gpsTimeoutValue = findViewById(R.id.gpsTimeoutValue)
        
        // 活跃状态设置
        activeStateTimeoutSeekBar = findViewById(R.id.activeStateTimeoutSeekBar)
        activeStateTimeoutValue = findViewById(R.id.activeStateTimeoutValue)
        activeStateDistanceSeekBar = findViewById(R.id.activeStateDistanceSeekBar)
        activeStateDistanceValue = findViewById(R.id.activeStateDistanceValue)
        
        // 步数阈值设置
        stepThresholdSeekBar = findViewById(R.id.stepThresholdSeekBar)
        stepThresholdValue = findViewById(R.id.stepThresholdValue)
        
        // 加速度阈值设置
        accelerationThresholdSeekBar = findViewById(R.id.accelerationThresholdSeekBar)
        accelerationThresholdValue = findViewById(R.id.accelerationThresholdValue)
        
        // 深度静止设置
        deepStationaryTimeoutSeekBar = findViewById(R.id.deepStationaryTimeoutSeekBar)
        deepStationaryTimeoutValue = findViewById(R.id.deepStationaryTimeoutValue)
        deepStationaryStepSeekBar = findViewById(R.id.deepStationaryStepSeekBar)
        deepStationaryStepValue = findViewById(R.id.deepStationaryStepValue)
        deepStationaryAccelerationSeekBar = findViewById(R.id.deepStationaryAccelerationSeekBar)
        deepStationaryAccelerationValue = findViewById(R.id.deepStationaryAccelerationValue)
        
        // 驾驶模式设置
        drivingSpeedSeekBar = findViewById(R.id.drivingSpeedSeekBar)
        drivingSpeedValue = findViewById(R.id.drivingSpeedValue)
        drivingStationaryTimeoutSeekBar = findViewById(R.id.drivingStationaryTimeoutSeekBar)
        drivingStationaryTimeoutValue = findViewById(R.id.drivingStationaryTimeoutValue)
        drivingStationaryDistanceSeekBar = findViewById(R.id.drivingStationaryDistanceSeekBar)
        drivingStationaryDistanceValue = findViewById(R.id.drivingStationaryDistanceValue)
        
        // 环境检测设置
        environmentCheckSeekBar = findViewById(R.id.environmentCheckSeekBar)
        environmentCheckValue = findViewById(R.id.environmentCheckValue)
        
        // 省电模式设置
        powerSaveSwitch = findViewById(R.id.powerSaveSwitch)
        gpsUpdateIntervalSeekBar = findViewById(R.id.gpsUpdateIntervalSeekBar)
        gpsUpdateIntervalValue = findViewById(R.id.gpsUpdateIntervalValue)
        stateCheckIntervalSeekBar = findViewById(R.id.stateCheckIntervalSeekBar)
        stateCheckIntervalValue = findViewById(R.id.stateCheckIntervalValue)
    }
    
    private fun loadCurrentSettings() {
        // GPS超时设置 (15-120秒)
        val gpsTimeout = sharedPreferences.getLong("gps_timeout_ms", 45000L)
        gpsTimeoutSeekBar.progress = ((gpsTimeout - 15000) / 1000).toInt()
        gpsTimeoutValue.text = "${gpsTimeout / 1000}秒"
        
        // 活跃状态超时 (1-10分钟)
        val activeStateTimeout = sharedPreferences.getLong("active_state_timeout_ms", 300000L)
        activeStateTimeoutSeekBar.progress = ((activeStateTimeout - 60000) / 60000).toInt()
        activeStateTimeoutValue.text = "${activeStateTimeout / 60000}分钟"
        
        // 活跃状态距离阈值 (50-500米)
        val activeStateDistance = sharedPreferences.getFloat("active_state_distance_threshold", 200.0f)
        activeStateDistanceSeekBar.progress = ((activeStateDistance - 50) / 10).toInt()
        activeStateDistanceValue.text = "${activeStateDistance.toInt()}米"
        
        // 步数阈值 (5-100步)
        val stepThreshold = sharedPreferences.getInt("step_threshold", 20)
        stepThresholdSeekBar.progress = stepThreshold - 5
        stepThresholdValue.text = "${stepThreshold}步"
        
        // 加速度阈值 (0.5-5.0)
        val accelerationThreshold = sharedPreferences.getFloat("acceleration_threshold", 2.0f)
        accelerationThresholdSeekBar.progress = ((accelerationThreshold - 0.5f) * 10).toInt()
        accelerationThresholdValue.text = "${accelerationThreshold}m/s²"
        
        // 深度静止超时 (1-15分钟)
        val deepStationaryTimeout = sharedPreferences.getLong("deep_stationary_timeout_ms", 300000L)
        deepStationaryTimeoutSeekBar.progress = ((deepStationaryTimeout - 60000) / 60000).toInt()
        deepStationaryTimeoutValue.text = "${deepStationaryTimeout / 60000}分钟"
        
        // 深度静止步数阈值 (10-100步)
        val deepStationaryStep = sharedPreferences.getInt("deep_stationary_step_threshold", 30)
        deepStationaryStepSeekBar.progress = deepStationaryStep - 10
        deepStationaryStepValue.text = "${deepStationaryStep}步"
        
        // 深度静止加速度阈值 (0.5-3.0)
        val deepStationaryAcceleration = sharedPreferences.getFloat("deep_stationary_acceleration_threshold", 1.5f)
        deepStationaryAccelerationSeekBar.progress = ((deepStationaryAcceleration - 0.5f) * 10).toInt()
        deepStationaryAccelerationValue.text = "${deepStationaryAcceleration}m/s²"
        
        // 驾驶速度阈值 (3-20 km/h)
        val drivingSpeed = sharedPreferences.getFloat("driving_speed_threshold", 7.0f)
        drivingSpeedSeekBar.progress = (drivingSpeed - 3).toInt()
        drivingSpeedValue.text = "${drivingSpeed.toInt()}km/h"
        
        // 驾驶静止超时 (1-15分钟)
        val drivingStationaryTimeout = sharedPreferences.getLong("driving_stationary_timeout_ms", 300000L)
        drivingStationaryTimeoutSeekBar.progress = ((drivingStationaryTimeout - 60000) / 60000).toInt()
        drivingStationaryTimeoutValue.text = "${drivingStationaryTimeout / 60000}分钟"
        
        // 驾驶静止距离阈值 (50-300米)
        val drivingStationaryDistance = sharedPreferences.getFloat("driving_stationary_distance_threshold", 100.0f)
        drivingStationaryDistanceSeekBar.progress = ((drivingStationaryDistance - 50) / 10).toInt()
        drivingStationaryDistanceValue.text = "${drivingStationaryDistance.toInt()}米"
        
        // 环境检测间隔 (30秒-5分钟)
        val environmentCheck = sharedPreferences.getLong("environment_check_interval", 60000L)
        environmentCheckSeekBar.progress = ((environmentCheck - 30000) / 30000).toInt()
        environmentCheckValue.text = "${environmentCheck / 60000}分钟"
        
        // 省电模式
        val powerSave = sharedPreferences.getBoolean("power_save_mode", true)
        powerSaveSwitch.isChecked = powerSave
        
        // GPS更新间隔 (5-60秒)
        val gpsUpdateInterval = sharedPreferences.getLong("gps_update_interval", 10000L)
        gpsUpdateIntervalSeekBar.progress = ((gpsUpdateInterval - 5000) / 1000).toInt()
        gpsUpdateIntervalValue.text = "${gpsUpdateInterval / 1000}秒"
        
        // 状态检查间隔 (5-60秒)
        val stateCheckInterval = sharedPreferences.getLong("state_check_interval", 15000L)
        stateCheckIntervalSeekBar.progress = ((stateCheckInterval - 5000) / 1000).toInt()
        stateCheckIntervalValue.text = "${stateCheckInterval / 1000}秒"
    }
    
    private fun setupListeners() {
        // GPS超时监听器
        gpsTimeoutSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = (progress + 15) * 1000L
                gpsTimeoutValue.text = "${value / 1000}秒"
                if (fromUser) {
                    sharedPreferences.edit().putLong("gps_timeout_ms", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 活跃状态超时监听器
        activeStateTimeoutSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = (progress + 1) * 60000L
                activeStateTimeoutValue.text = "${value / 60000}分钟"
                if (fromUser) {
                    sharedPreferences.edit().putLong("active_state_timeout_ms", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 活跃状态距离阈值监听器
        activeStateDistanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = (progress + 5) * 10f
                activeStateDistanceValue.text = "${value.toInt()}米"
                if (fromUser) {
                    sharedPreferences.edit().putFloat("active_state_distance_threshold", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 步数阈值监听器
        stepThresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 5
                stepThresholdValue.text = "${value}步"
                if (fromUser) {
                    sharedPreferences.edit().putInt("step_threshold", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 加速度阈值监听器
        accelerationThresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = (progress / 10f) + 0.5f
                accelerationThresholdValue.text = "${value}m/s²"
                if (fromUser) {
                    sharedPreferences.edit().putFloat("acceleration_threshold", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 深度静止超时监听器
        deepStationaryTimeoutSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = (progress + 1) * 60000L
                deepStationaryTimeoutValue.text = "${value / 60000}分钟"
                if (fromUser) {
                    sharedPreferences.edit().putLong("deep_stationary_timeout_ms", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 深度静止步数阈值监听器
        deepStationaryStepSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 10
                deepStationaryStepValue.text = "${value}步"
                if (fromUser) {
                    sharedPreferences.edit().putInt("deep_stationary_step_threshold", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 深度静止加速度阈值监听器
        deepStationaryAccelerationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = (progress / 10f) + 0.5f
                deepStationaryAccelerationValue.text = "${value}m/s²"
                if (fromUser) {
                    sharedPreferences.edit().putFloat("deep_stationary_acceleration_threshold", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 驾驶速度阈值监听器
        drivingSpeedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 3f
                drivingSpeedValue.text = "${value.toInt()}km/h"
                if (fromUser) {
                    sharedPreferences.edit().putFloat("driving_speed_threshold", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 驾驶静止超时监听器
        drivingStationaryTimeoutSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = (progress + 1) * 60000L
                drivingStationaryTimeoutValue.text = "${value / 60000}分钟"
                if (fromUser) {
                    sharedPreferences.edit().putLong("driving_stationary_timeout_ms", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 驾驶静止距离阈值监听器
        drivingStationaryDistanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = (progress + 5) * 10f
                drivingStationaryDistanceValue.text = "${value.toInt()}米"
                if (fromUser) {
                    sharedPreferences.edit().putFloat("driving_stationary_distance_threshold", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 环境检测间隔监听器
        environmentCheckSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = (progress + 1) * 30000L
                environmentCheckValue.text = "${value / 60000}分钟"
                if (fromUser) {
                    sharedPreferences.edit().putLong("environment_check_interval", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 省电模式开关监听器
        powerSaveSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("power_save_mode", isChecked).apply()
        }
        
        // GPS更新间隔监听器
        gpsUpdateIntervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = (progress + 5) * 1000L
                gpsUpdateIntervalValue.text = "${value / 1000}秒"
                if (fromUser) {
                    sharedPreferences.edit().putLong("gps_update_interval", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 状态检查间隔监听器
        stateCheckIntervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = (progress + 5) * 1000L
                stateCheckIntervalValue.text = "${value / 1000}秒"
                if (fromUser) {
                    sharedPreferences.edit().putLong("state_check_interval", value).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
