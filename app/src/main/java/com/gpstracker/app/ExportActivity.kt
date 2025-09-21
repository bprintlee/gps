package com.gpstracker.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gpstracker.app.databinding.ActivityExportBinding
import com.gpstracker.app.utils.GpxExporter
import com.gpstracker.app.service.GpsTrackingService
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExportActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityExportBinding
    private lateinit var gpxExporter: GpxExporter
    private lateinit var adapter: GpxFileAdapter
    private var gpxFiles = mutableListOf<File>()
    
    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                saveFileToSelectedLocation(uri)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        gpxExporter = GpxExporter(this)
        
        setupRecyclerView()
        loadGpxFiles()
        setupClickListeners()
    }
    
    private fun setupRecyclerView() {
        adapter = GpxFileAdapter(gpxFiles) { file ->
            onFileClick(file)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupClickListeners() {
        binding.exportAllButton.setOnClickListener {
            exportAllFiles()
        }
        
        // 按行程导出按钮
        binding.exportFromDatabaseButton.setOnClickListener {
            exportFromDatabase()
        }
        
        // 查看行程按钮
        binding.exportTripsButton.setOnClickListener {
            showTripsDialog()
        }
        
        binding.refreshButton.setOnClickListener {
            loadGpxFiles()
        }
        
        binding.backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun loadGpxFiles() {
        lifecycleScope.launch {
            try {
                gpxFiles.clear()
                gpxFiles.addAll(gpxExporter.getAllGpxFiles())
                adapter.notifyDataSetChanged()
                
                binding.emptyTextView.visibility = if (gpxFiles.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (gpxFiles.isEmpty()) View.GONE else View.VISIBLE
                
                binding.exportAllButton.isEnabled = gpxFiles.isNotEmpty()
                
            } catch (e: Exception) {
                Toast.makeText(this@ExportActivity, "加载文件失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun onFileClick(file: File) {
        // 显示文件操作选项
        val options = arrayOf("另存为...", "导出到下载目录", "分享文件", "删除文件", "取消")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> saveAsFile(file)
                    1 -> exportToDownloads(file)
                    2 -> shareFile(file)
                    3 -> deleteFile(file)
                }
            }
            .show()
    }
    
    private var selectedFile: File? = null
    
    private fun saveAsFile(file: File) {
        selectedFile = file
        // 使用系统文件选择器
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_TITLE, file.name)
        }
        filePickerLauncher.launch(intent)
    }
    
    private fun saveFileToSelectedLocation(uri: Uri) {
        lifecycleScope.launch {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    selectedFile?.let { sourceFile ->
                        if (sourceFile.exists()) {
                            sourceFile.inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                            Toast.makeText(this@ExportActivity, "文件保存成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ExportActivity, "源文件不存在", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Toast.makeText(this@ExportActivity, "未选择源文件", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExportActivity, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportToDownloads(file: File) {
        lifecycleScope.launch {
            try {
                val result = gpxExporter.exportToExternalStorage(file.name)
                if (result != null) {
                    Toast.makeText(this@ExportActivity, "文件已导出到下载目录：$result", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ExportActivity, "导出失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExportActivity, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun shareFile(file: File) {
        try {
            val uri = Uri.fromFile(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/gpx+xml"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "GPS轨迹文件")
            }
            startActivity(Intent.createChooser(intent, "分享GPX文件"))
        } catch (e: Exception) {
            Toast.makeText(this@ExportActivity, "分享失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun exportSingleFile(file: File) {
        lifecycleScope.launch {
            try {
                val completed = gpxExporter.exportGpxFile()
                if (completed != null) {
                    // 使用系统分享功能
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/gpx+xml"
                        putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                        putExtra(Intent.EXTRA_SUBJECT, "GPS轨迹文件")
                    }
                    startActivity(Intent.createChooser(intent, "分享GPX文件"))
                } else {
                    Toast.makeText(this@ExportActivity, "导出失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExportActivity, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun deleteFile(file: File) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除文件")
            .setMessage("确定要删除 ${file.name} 吗？")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val deleted = gpxExporter.deleteGpxFile(file.name)
                        if (deleted) {
                            loadGpxFiles()
                            Toast.makeText(this@ExportActivity, "文件已删除", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ExportActivity, "删除失败", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@ExportActivity, "删除失败：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun exportAllFiles() {
        lifecycleScope.launch {
            try {
                var exportedCount = 0
                for (file in gpxFiles) {
                    val completed = gpxExporter.exportGpxFile()
                    if (completed != null) {
                        exportedCount++
                    }
                }
                
                if (exportedCount > 0) {
                    Toast.makeText(this@ExportActivity, "成功导出 $exportedCount 个文件", Toast.LENGTH_LONG).show()
                    loadGpxFiles()
                } else {
                    Toast.makeText(this@ExportActivity, "导出失败", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@ExportActivity, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportFromDatabase() {
        val serviceIntent = Intent(this, GpsTrackingService::class.java)
        try {
            val serviceConnection = object : android.content.ServiceConnection {
                override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                    service?.let {
                        val gpsService = (it as GpsTrackingService.GpsTrackingBinder).getService()
                        
                        // 异步导出
                        lifecycleScope.launch {
                            try {
                                val result = gpsService.exportGpxFromDatabase()
                                if (result != null) {
                                    Toast.makeText(this@ExportActivity, "从数据库导出GPX文件成功：$result", Toast.LENGTH_LONG).show()
                                    loadGpxFiles() // 刷新文件列表
                                } else {
                                    Toast.makeText(this@ExportActivity, "从数据库导出GPX文件失败", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@ExportActivity, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        unbindService(this)
                    }
                }
                override fun onServiceDisconnected(name: android.content.ComponentName?) {}
            }
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Toast.makeText(this, "连接服务失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showTripsDialog() {
        val serviceIntent = Intent(this, GpsTrackingService::class.java)
        try {
            val serviceConnection = object : android.content.ServiceConnection {
                override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                    service?.let {
                        val gpsService = (it as GpsTrackingService.GpsTrackingBinder).getService()
                        
                        // 添加调试信息
                        val totalGpsCount = gpsService.getGpsDataCount()
                        val tripIds = gpsService.getAllTripIds()
                        
                        android.util.Log.d("ExportActivity", "总GPS数据点: $totalGpsCount")
                        android.util.Log.d("ExportActivity", "查询到行程数量: ${tripIds.size}")
                        android.util.Log.d("ExportActivity", "行程列表: $tripIds")
                        
                        if (tripIds.isEmpty()) {
                            if (totalGpsCount == 0) {
                                Toast.makeText(this@ExportActivity, "暂无GPS数据，请先开始跟踪", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@ExportActivity, "暂无行程数据，GPS数据点: $totalGpsCount", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            showTripSelectionDialog(tripIds, gpsService)
                        }
                        unbindService(this)
                    }
                }
                override fun onServiceDisconnected(name: android.content.ComponentName?) {}
            }
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            android.util.Log.e("ExportActivity", "连接服务失败", e)
            Toast.makeText(this, "连接服务失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showTripSelectionDialog(tripIds: List<String>, gpsService: GpsTrackingService) {
        android.util.Log.d("ExportActivity", "显示行程选择对话框，行程数量: ${tripIds.size}")
        
        val tripItems = tripIds.mapIndexed { index, tripId ->
            val gpsData = gpsService.getGpsDataByTripId(tripId)
            val pointCount = gpsData.size
            val startTime = if (gpsData.isNotEmpty()) {
                SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(gpsData.first().timestamp))
            } else "未知"
            val endTime = if (gpsData.isNotEmpty()) {
                SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(gpsData.last().timestamp))
            } else "未知"
            
            android.util.Log.d("ExportActivity", "行程 $index: $tripId, 点数: $pointCount, 开始: $startTime, 结束: $endTime")
            
            "行程 ${index + 1}: $tripId\n开始: $startTime, 结束: $endTime, 点数: $pointCount"
        }.toTypedArray()
        
        if (tripItems.isEmpty()) {
            Toast.makeText(this, "没有可导出的行程数据", Toast.LENGTH_SHORT).show()
            return
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择要导出的行程 (共${tripIds.size}个)")
            .setItems(tripItems) { _, which ->
                if (which >= 0 && which < tripIds.size) {
                    val selectedTripId = tripIds[which]
                    android.util.Log.d("ExportActivity", "用户选择行程: $selectedTripId")
                    exportSingleTrip(selectedTripId, gpsService)
                } else {
                    android.util.Log.e("ExportActivity", "无效的选择索引: $which")
                    Toast.makeText(this, "选择无效", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .setPositiveButton("导出全部") { _, _ ->
                android.util.Log.d("ExportActivity", "用户选择导出全部行程")
                exportAllTrips(tripIds, gpsService)
            }
            .show()
    }
    
    private fun exportSingleTrip(tripId: String, gpsService: GpsTrackingService) {
        lifecycleScope.launch {
            try {
                val result = gpsService.exportTripGpx(tripId)
                if (result != null) {
                    Toast.makeText(this@ExportActivity, "行程 $tripId 导出成功：$result", Toast.LENGTH_LONG).show()
                    loadGpxFiles() // 刷新文件列表
                } else {
                    Toast.makeText(this@ExportActivity, "行程 $tripId 导出失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExportActivity, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportAllTrips(tripIds: List<String>, gpsService: GpsTrackingService) {
        lifecycleScope.launch {
            try {
                var successCount = 0
                var totalCount = tripIds.size
                
                Toast.makeText(this@ExportActivity, "开始导出 $totalCount 个行程...", Toast.LENGTH_SHORT).show()
                
                for (tripId in tripIds) {
                    try {
                        val result = gpsService.exportTripGpx(tripId)
                        if (result != null) {
                            successCount++
                            android.util.Log.d("ExportActivity", "成功导出行程: $tripId")
                        } else {
                            android.util.Log.w("ExportActivity", "导出行程失败: $tripId")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ExportActivity", "导出行程异常: $tripId", e)
                    }
                }
                
                val message = if (successCount == totalCount) {
                    "成功导出所有 $totalCount 个行程"
                } else {
                    "导出完成: $successCount/$totalCount 个行程成功"
                }
                
                Toast.makeText(this@ExportActivity, message, Toast.LENGTH_LONG).show()
                loadGpxFiles() // 刷新文件列表
                
            } catch (e: Exception) {
                android.util.Log.e("ExportActivity", "批量导出失败", e)
                Toast.makeText(this@ExportActivity, "批量导出失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class GpxFileAdapter(
    private val files: List<File>,
    private val onFileClick: (File) -> Unit
) : RecyclerView.Adapter<GpxFileAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(android.R.id.text1)
        val fileInfo: TextView = view.findViewById(android.R.id.text2)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        holder.fileName.text = file.name
        
        val size = formatFileSize(file.length())
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
        holder.fileInfo.text = "大小: $size, 修改时间: $date"
        
        holder.itemView.setOnClickListener {
            onFileClick(file)
        }
    }
    
    override fun getItemCount() = files.size
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
