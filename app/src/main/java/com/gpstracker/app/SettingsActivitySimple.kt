package com.gpstracker.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SettingsActivitySimple : AppCompatActivity() {
    
    private lateinit var sharedPreferences: SharedPreferences
    
    // 简化的设置项
    private lateinit var gpsTimeoutSeekBar: SeekBar
    private lateinit var gpsTimeoutValue: TextView
    private lateinit var powerSaveSwitch: Switch
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_settings_simple)
            
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
            
            Toast.makeText(this, "设置页面加载成功", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivitySimple", "设置页面初始化失败", e)
            Toast.makeText(this, "设置页面加载失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun initViews() {
        gpsTimeoutSeekBar = findViewById(R.id.gpsTimeoutSeekBar)
        gpsTimeoutValue = findViewById(R.id.gpsTimeoutValue)
        powerSaveSwitch = findViewById(R.id.powerSaveSwitch)
    }
    
    private fun loadCurrentSettings() {
        // GPS超时设置 (15-120秒)
        val gpsTimeout = sharedPreferences.getLong("gps_timeout_ms", 45000L)
        gpsTimeoutSeekBar.progress = ((gpsTimeout - 15000) / 1000).toInt()
        gpsTimeoutValue.text = "${gpsTimeout / 1000}秒"
        
        // 省电模式
        val powerSave = sharedPreferences.getBoolean("power_save_mode", true)
        powerSaveSwitch.isChecked = powerSave
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
        
        // 省电模式开关监听器
        powerSaveSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("power_save_mode", isChecked).apply()
        }
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
