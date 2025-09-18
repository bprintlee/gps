package com.gpstracker.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gpstracker.app.databinding.ActivityMainBinding
import com.gpstracker.app.service.GpsTrackingService

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
            // TODO: 实现GPX导出功能
            Toast.makeText(this, "导出功能开发中...", Toast.LENGTH_SHORT).show()
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
}
