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
        
        // 添加从数据库导出按钮
        binding.exportFromDatabaseButton.setOnClickListener {
            exportFromDatabase()
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
