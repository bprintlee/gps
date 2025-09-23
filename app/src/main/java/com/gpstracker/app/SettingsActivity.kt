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
                        val newTimeout = progress * 1000L
                        sharedPreferences.edit().putLong("gps_timeout_ms", newTimeout).apply()
                        Log.d("SettingsActivity", "GPS超时设置更新: ${newTimeout}ms")
                    }
                })
            }
            
            // 省电模式设置
            val powerSaveSwitch = findViewById<Switch>(R.id.powerSaveSwitch)
            if (powerSaveSwitch != null) {
                val currentMode = SettingsManager.isPowerSaveMode(this)
                powerSaveSwitch.isChecked = currentMode
                
                powerSaveSwitch.setOnCheckedChangeListener { _, isChecked ->
                    sharedPreferences.edit().putBoolean("power_save_mode", isChecked).apply()
                    Log.d("SettingsActivity", "省电模式设置更新: $isChecked")
                }
            }
            
            Log.d("SettingsActivity", "设置项初始化完成")
            
        } catch (e: Exception) {
            Log.e("SettingsActivity", "设置项初始化失败", e)
            Toast.makeText(this, "设置项初始化失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}