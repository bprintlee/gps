package com.gpstracker.app

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.gpstracker.app.utils.SettingsManager

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    
    // 基本GPS设置
    private lateinit var gpsTimeoutSeekBar: SeekBar
    private lateinit var gpsTimeoutValue: android.widget.TextView
    private lateinit var gpsUpdateIntervalSeekBar: SeekBar
    private lateinit var gpsUpdateIntervalValue: android.widget.TextView
    private lateinit var locationAccuracySeekBar: SeekBar
    private lateinit var locationAccuracyValue: android.widget.TextView
    private lateinit var mqttAccuracySeekBar: SeekBar
    private lateinit var mqttAccuracyValue: android.widget.TextView
    
    // 状态转换阈值
    private lateinit var indoorToOutdoorStepSeekBar: SeekBar
    private lateinit var indoorToOutdoorStepValue: android.widget.TextView
    private lateinit var indoorToOutdoorDistanceSeekBar: SeekBar
    private lateinit var indoorToOutdoorDistanceValue: android.widget.TextView
    private lateinit var outdoorToActiveStepSeekBar: SeekBar
    private lateinit var outdoorToActiveStepValue: android.widget.TextView
    private lateinit var outdoorToActiveDistanceSeekBar: SeekBar
    private lateinit var outdoorToActiveDistanceValue: android.widget.TextView
    private lateinit var activeToDrivingSpeedSeekBar: SeekBar
    private lateinit var activeToDrivingSpeedValue: android.widget.TextView
    
    // 深度静止设置
    private lateinit var deepStationaryTimeoutSeekBar: SeekBar
    private lateinit var deepStationaryTimeoutValue: android.widget.TextView
    private lateinit var deepStationaryStepSeekBar: SeekBar
    private lateinit var deepStationaryStepValue: android.widget.TextView
    private lateinit var deepStationaryAccelerationSeekBar: SeekBar
    private lateinit var deepStationaryAccelerationValue: android.widget.TextView
    
    // 驾驶模式设置
    private lateinit var drivingSpeedSeekBar: SeekBar
    private lateinit var drivingSpeedValue: android.widget.TextView
    private lateinit var drivingStationaryTimeoutSeekBar: SeekBar
    private lateinit var drivingStationaryTimeoutValue: android.widget.TextView
    private lateinit var drivingStationaryDistanceSeekBar: SeekBar
    private lateinit var drivingStationaryDistanceValue: android.widget.TextView
    
    // 其他设置
    private lateinit var powerSaveModeSwitch: Switch
    private lateinit var environmentCheckIntervalSeekBar: SeekBar
    private lateinit var environmentCheckIntervalValue: android.widget.TextView
    private lateinit var maxLocationHistorySeekBar: SeekBar
    private lateinit var maxLocationHistoryValue: android.widget.TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_settings)
            
            // 设置Toolbar
            toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "GPS设置"
            
            // 初始化视图
            initializeViews()
            
            // 加载当前设置
            loadCurrentSettings()
            
            // 设置监听器
            setupListeners()
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "初始化失败", e)
            Toast.makeText(this, "设置页面初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun initializeViews() {
        // 基本GPS设置
        gpsTimeoutSeekBar = findViewById(R.id.gpsTimeoutSeekBar)
        gpsTimeoutValue = findViewById(R.id.gpsTimeoutValue)
        gpsUpdateIntervalSeekBar = findViewById(R.id.gpsUpdateIntervalSeekBar)
        gpsUpdateIntervalValue = findViewById(R.id.gpsUpdateIntervalValue)
        locationAccuracySeekBar = findViewById(R.id.locationAccuracySeekBar)
        locationAccuracyValue = findViewById(R.id.locationAccuracyValue)
        mqttAccuracySeekBar = findViewById(R.id.mqttAccuracySeekBar)
        mqttAccuracyValue = findViewById(R.id.mqttAccuracyValue)
        
        // 状态转换阈值
        indoorToOutdoorStepSeekBar = findViewById(R.id.indoorToOutdoorStepSeekBar)
        indoorToOutdoorStepValue = findViewById(R.id.indoorToOutdoorStepValue)
        indoorToOutdoorDistanceSeekBar = findViewById(R.id.indoorToOutdoorDistanceSeekBar)
        indoorToOutdoorDistanceValue = findViewById(R.id.indoorToOutdoorDistanceValue)
        outdoorToActiveStepSeekBar = findViewById(R.id.outdoorToActiveStepSeekBar)
        outdoorToActiveStepValue = findViewById(R.id.outdoorToActiveStepValue)
        outdoorToActiveDistanceSeekBar = findViewById(R.id.outdoorToActiveDistanceSeekBar)
        outdoorToActiveDistanceValue = findViewById(R.id.outdoorToActiveDistanceValue)
        activeToDrivingSpeedSeekBar = findViewById(R.id.activeToDrivingSpeedSeekBar)
        activeToDrivingSpeedValue = findViewById(R.id.activeToDrivingSpeedValue)
        
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
        
        // 其他设置
        powerSaveModeSwitch = findViewById(R.id.powerSaveModeSwitch)
        environmentCheckIntervalSeekBar = findViewById(R.id.environmentCheckIntervalSeekBar)
        environmentCheckIntervalValue = findViewById(R.id.environmentCheckIntervalValue)
        maxLocationHistorySeekBar = findViewById(R.id.maxLocationHistorySeekBar)
        maxLocationHistoryValue = findViewById(R.id.maxLocationHistoryValue)
    }
    
    private fun loadCurrentSettings() {
        // 基本GPS设置
        gpsTimeoutSeekBar.progress = (SettingsManager.getGpsTimeoutMs(this) / 1000).toInt()
        gpsUpdateIntervalSeekBar.progress = (SettingsManager.getGpsUpdateInterval(this) / 1000).toInt()
        locationAccuracySeekBar.progress = SettingsManager.getLocationAccuracyThreshold(this).toInt()
        mqttAccuracySeekBar.progress = SettingsManager.getMqttAccuracyThreshold(this).toInt()
        
        // 状态转换阈值
        indoorToOutdoorStepSeekBar.progress = SettingsManager.getIndoorToOutdoorStepThreshold(this)
        indoorToOutdoorDistanceSeekBar.progress = SettingsManager.getIndoorToOutdoorDistanceThreshold(this).toInt()
        outdoorToActiveStepSeekBar.progress = SettingsManager.getOutdoorToActiveStepThreshold(this)
        outdoorToActiveDistanceSeekBar.progress = SettingsManager.getOutdoorToActiveDistanceThreshold(this).toInt()
        activeToDrivingSpeedSeekBar.progress = SettingsManager.getActiveToDrivingSpeedThreshold(this).toInt()
        
        // 深度静止设置
        deepStationaryTimeoutSeekBar.progress = (SettingsManager.getDeepStationaryTimeoutMs(this) / 60000).toInt()
        deepStationaryStepSeekBar.progress = SettingsManager.getDeepStationaryStepThreshold(this)
        deepStationaryAccelerationSeekBar.progress = (SettingsManager.getDeepStationaryAccelerationThreshold(this) * 10).toInt()
        
        // 驾驶模式设置
        drivingSpeedSeekBar.progress = SettingsManager.getDrivingSpeedThreshold(this).toInt()
        drivingStationaryTimeoutSeekBar.progress = (SettingsManager.getDrivingStationaryTimeoutMs(this) / 60000).toInt()
        drivingStationaryDistanceSeekBar.progress = SettingsManager.getDrivingStationaryDistanceThreshold(this).toInt()
        
        // 其他设置
        powerSaveModeSwitch.isChecked = SettingsManager.isPowerSaveMode(this)
        environmentCheckIntervalSeekBar.progress = (SettingsManager.getEnvironmentCheckInterval(this) / 60000).toInt()
        maxLocationHistorySeekBar.progress = SettingsManager.getMaxLocationHistory(this)
        
        // 更新显示值
        updateAllDisplayValues()
    }
    
    private fun setupListeners() {
        // 基本GPS设置监听器
        gpsTimeoutSeekBar.setOnSeekBarChangeListener(createSeekBarListener(gpsTimeoutValue, "秒"))
        gpsUpdateIntervalSeekBar.setOnSeekBarChangeListener(createSeekBarListener(gpsUpdateIntervalValue, "秒"))
        locationAccuracySeekBar.setOnSeekBarChangeListener(createSeekBarListener(locationAccuracyValue, "米"))
        mqttAccuracySeekBar.setOnSeekBarChangeListener(createSeekBarListener(mqttAccuracyValue, "米"))
        
        // 状态转换阈值监听器
        indoorToOutdoorStepSeekBar.setOnSeekBarChangeListener(createSeekBarListener(indoorToOutdoorStepValue, "步"))
        indoorToOutdoorDistanceSeekBar.setOnSeekBarChangeListener(createSeekBarListener(indoorToOutdoorDistanceValue, "米"))
        outdoorToActiveStepSeekBar.setOnSeekBarChangeListener(createSeekBarListener(outdoorToActiveStepValue, "步"))
        outdoorToActiveDistanceSeekBar.setOnSeekBarChangeListener(createSeekBarListener(outdoorToActiveDistanceValue, "米"))
        activeToDrivingSpeedSeekBar.setOnSeekBarChangeListener(createSeekBarListener(activeToDrivingSpeedValue, "km/h"))
        
        // 深度静止设置监听器
        deepStationaryTimeoutSeekBar.setOnSeekBarChangeListener(createSeekBarListener(deepStationaryTimeoutValue, "分钟"))
        deepStationaryStepSeekBar.setOnSeekBarChangeListener(createSeekBarListener(deepStationaryStepValue, "步"))
        deepStationaryAccelerationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                deepStationaryAccelerationValue.text = "${(progress / 10.0f)}m/s²"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 驾驶模式设置监听器
        drivingSpeedSeekBar.setOnSeekBarChangeListener(createSeekBarListener(drivingSpeedValue, "km/h"))
        drivingStationaryTimeoutSeekBar.setOnSeekBarChangeListener(createSeekBarListener(drivingStationaryTimeoutValue, "分钟"))
        drivingStationaryDistanceSeekBar.setOnSeekBarChangeListener(createSeekBarListener(drivingStationaryDistanceValue, "米"))
        
        // 其他设置监听器
        environmentCheckIntervalSeekBar.setOnSeekBarChangeListener(createSeekBarListener(environmentCheckIntervalValue, "分钟"))
        maxLocationHistorySeekBar.setOnSeekBarChangeListener(createSeekBarListener(maxLocationHistoryValue, "个"))
        
        // 按钮监听器
        findViewById<com.google.android.material.button.MaterialButton>(R.id.resetButton).setOnClickListener {
            resetToDefaults()
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.saveButton).setOnClickListener {
            saveSettings()
        }
    }
    
    private fun createSeekBarListener(textView: android.widget.TextView, unit: String): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textView.text = "$progress$unit"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
    }
    
    private fun updateAllDisplayValues() {
        // 基本GPS设置
        gpsTimeoutValue.text = "${gpsTimeoutSeekBar.progress}秒"
        gpsUpdateIntervalValue.text = "${gpsUpdateIntervalSeekBar.progress}秒"
        locationAccuracyValue.text = "${locationAccuracySeekBar.progress}米"
        mqttAccuracyValue.text = "${mqttAccuracySeekBar.progress}米"
        
        // 状态转换阈值
        indoorToOutdoorStepValue.text = "${indoorToOutdoorStepSeekBar.progress}步"
        indoorToOutdoorDistanceValue.text = "${indoorToOutdoorDistanceSeekBar.progress}米"
        outdoorToActiveStepValue.text = "${outdoorToActiveStepSeekBar.progress}步"
        outdoorToActiveDistanceValue.text = "${outdoorToActiveDistanceSeekBar.progress}米"
        activeToDrivingSpeedValue.text = "${activeToDrivingSpeedSeekBar.progress}km/h"
        
        // 深度静止设置
        deepStationaryTimeoutValue.text = "${deepStationaryTimeoutSeekBar.progress}分钟"
        deepStationaryStepValue.text = "${deepStationaryStepSeekBar.progress}步"
        deepStationaryAccelerationValue.text = "${(deepStationaryAccelerationSeekBar.progress / 10.0f)}m/s²"
        
        // 驾驶模式设置
        drivingSpeedValue.text = "${drivingSpeedSeekBar.progress}km/h"
        drivingStationaryTimeoutValue.text = "${drivingStationaryTimeoutSeekBar.progress}分钟"
        drivingStationaryDistanceValue.text = "${drivingStationaryDistanceSeekBar.progress}米"
        
        // 其他设置
        environmentCheckIntervalValue.text = "${environmentCheckIntervalSeekBar.progress}分钟"
        maxLocationHistoryValue.text = "${maxLocationHistorySeekBar.progress}个"
    }
    
    private fun resetToDefaults() {
        try {
            SettingsManager.resetToDefaults(this)
            loadCurrentSettings()
            Toast.makeText(this, "设置已重置为默认值", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "重置设置失败", e)
            Toast.makeText(this, "重置设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveSettings() {
        try {
            val prefs = getSharedPreferences("gps_tracking_settings", MODE_PRIVATE)
            val editor = prefs.edit()
            
            // 基本GPS设置
            editor.putLong("gps_timeout_ms", gpsTimeoutSeekBar.progress * 1000L)
            editor.putLong("gps_update_interval", gpsUpdateIntervalSeekBar.progress * 1000L)
            editor.putFloat("location_accuracy_threshold", locationAccuracySeekBar.progress.toFloat())
            editor.putFloat("mqtt_accuracy_threshold", mqttAccuracySeekBar.progress.toFloat())
            
            // 状态转换阈值
            editor.putInt("indoor_to_outdoor_step_threshold", indoorToOutdoorStepSeekBar.progress)
            editor.putFloat("indoor_to_outdoor_distance_threshold", indoorToOutdoorDistanceSeekBar.progress.toFloat())
            editor.putInt("outdoor_to_active_step_threshold", outdoorToActiveStepSeekBar.progress)
            editor.putFloat("outdoor_to_active_distance_threshold", outdoorToActiveDistanceSeekBar.progress.toFloat())
            editor.putFloat("active_to_driving_speed_threshold", activeToDrivingSpeedSeekBar.progress.toFloat())
            
            // 深度静止设置
            editor.putLong("deep_stationary_timeout_ms", deepStationaryTimeoutSeekBar.progress * 60000L)
            editor.putInt("deep_stationary_step_threshold", deepStationaryStepSeekBar.progress)
            editor.putFloat("deep_stationary_acceleration_threshold", deepStationaryAccelerationSeekBar.progress / 10.0f)
            
            // 驾驶模式设置
            editor.putFloat("driving_speed_threshold", drivingSpeedSeekBar.progress.toFloat())
            editor.putLong("driving_stationary_timeout_ms", drivingStationaryTimeoutSeekBar.progress * 60000L)
            editor.putFloat("driving_stationary_distance_threshold", drivingStationaryDistanceSeekBar.progress.toFloat())
            
            // 其他设置
            editor.putBoolean("power_save_mode", powerSaveModeSwitch.isChecked)
            editor.putLong("environment_check_interval", environmentCheckIntervalSeekBar.progress * 60000L)
            editor.putInt("max_location_history", maxLocationHistorySeekBar.progress)
            
            editor.apply()
            
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "保存设置失败", e)
            Toast.makeText(this, "保存设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}