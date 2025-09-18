package com.gpstracker.app.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.gpstracker.app.model.GpsData
import com.gpstracker.app.model.TrackingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class GpxExporter(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    private fun getGpxDirectory(): File {
        // 使用应用内部存储，确保有写入权限
        val directory = File(context.filesDir, "gpx_tracks")
        if (!directory.exists()) {
            val created = directory.mkdirs()
            Log.d("GpxExporter", "创建GPX目录: $created, 路径: ${directory.absolutePath}")
        }
        
        // 验证目录是否可写
        if (!directory.canWrite()) {
            Log.e("GpxExporter", "GPX目录不可写: ${directory.absolutePath}")
            // 尝试使用缓存目录作为备选
            val cacheDir = File(context.cacheDir, "gpx_tracks")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            Log.d("GpxExporter", "使用缓存目录: ${cacheDir.absolutePath}")
            return cacheDir
        }
        
        return directory
    }
    
    private fun getGpxFileName(): String {
        val today = dateFormat.format(Date())
        return "gps_track_$today.gpx"
    }
    
    suspend fun appendGpsData(dataList: List<GpsData>) = withContext(Dispatchers.IO) {
        try {
            val gpxFile = File(getGpxDirectory(), getGpxFileName())
            Log.d("GpxExporter", "保存GPX数据到: ${gpxFile.absolutePath}")
            
            if (!gpxFile.exists()) {
                createNewGpxFile(gpxFile)
                Log.d("GpxExporter", "创建新的GPX文件")
            }
            
            // 直接写入数据，不使用追加模式
            writeGpsDataToFile(gpxFile, dataList)
            Log.d("GpxExporter", "成功保存 ${dataList.size} 个GPS点")
        } catch (e: Exception) {
            Log.e("GpxExporter", "保存GPX数据失败", e)
        }
    }
    
    private fun createNewGpxFile(gpxFile: File) {
        val writer = FileWriter(gpxFile)
        writer.use {
            it.write("""<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="GPS Tracker App">
  <metadata>
    <name>GPS Track ${dateFormat.format(Date())}</name>
    <desc>GPS位置跟踪数据</desc>
    <time>${timeFormat.format(Date())}</time>
  </metadata>
  <trk>
    <name>GPS Track</name>
    <desc>GPS位置跟踪轨迹</desc>
    <trkseg>
""")
        }
    }
    
    private fun writeGpsDataToFile(gpxFile: File, dataList: List<GpsData>) {
        try {
            // 确保父目录存在
            gpxFile.parentFile?.mkdirs()
            
            Log.d("GpxExporter", "开始写入 ${dataList.size} 个GPS点到文件: ${gpxFile.absolutePath}")
            
            // 如果文件不存在，创建新文件
            if (!gpxFile.exists()) {
                createNewGpxFile(gpxFile)
                Log.d("GpxExporter", "创建新的GPX文件: ${gpxFile.absolutePath}")
            }
            
            // 读取现有文件内容
            val existingContent = gpxFile.readText()
            
            // 在 </trkseg> 之前插入新的GPS点
            val newPoints = StringBuilder()
            dataList.forEach { data ->
                val timeStr = timeFormat.format(Date(data.timestamp))
                val stateStr = getStateString(data.state)
                
                newPoints.append("""      <trkpt lat="${data.latitude}" lon="${data.longitude}">
        <ele>${data.altitude}</ele>
        <time>$timeStr</time>
        <extensions>
          <accuracy>${data.accuracy}</accuracy>
          <state>$stateStr</state>
        </extensions>
      </trkpt>
""")
            }
            
            // 重新写入整个文件
            val updatedContent = existingContent.replace("    </trkseg>", "$newPoints    </trkseg>")
            
            gpxFile.writeText(updatedContent)
            Log.d("GpxExporter", "成功写入 ${dataList.size} 个GPS点到文件: ${gpxFile.absolutePath}")
            
            // 验证文件内容
            val finalContent = gpxFile.readText()
            val pointCount = finalContent.split("<trkpt").size - 1
            Log.d("GpxExporter", "文件验证: 包含 $pointCount 个GPS点")
            
        } catch (e: Exception) {
            Log.e("GpxExporter", "写入GPX文件失败: ${gpxFile.absolutePath}", e)
            throw e
        }
    }
    
    private fun appendToGpxFile(gpxFile: File, dataList: List<GpsData>) {
        val writer = FileWriter(gpxFile, true)
        writer.use {
            dataList.forEach { data ->
                val timeStr = timeFormat.format(Date(data.timestamp))
                val stateStr = getStateString(data.state)
                
                it.write("""      <trkpt lat="${data.latitude}" lon="${data.longitude}">
        <ele>${data.altitude}</ele>
        <time>$timeStr</time>
        <extensions>
          <accuracy>${data.accuracy}</accuracy>
          <state>$stateStr</state>
        </extensions>
      </trkpt>
""")
            }
        }
    }
    
    private fun getStateString(state: TrackingState): String {
        return when (state) {
            TrackingState.INDOOR -> "室内"
            TrackingState.OUTDOOR -> "室外"
            TrackingState.ACTIVE -> "活跃"
            TrackingState.DRIVING -> "驾驶"
        }
    }
    
    suspend fun exportGpxFile(): String? = withContext(Dispatchers.IO) {
        val gpxFile = File(getGpxDirectory(), getGpxFileName())
        
        if (!gpxFile.exists()) {
            return@withContext null
        }
        
        // 完成GPX文件
        val writer = FileWriter(gpxFile, true)
        writer.use {
            it.write("""    </trkseg>
  </trk>
</gpx>
""")
        }
        
        return@withContext gpxFile.absolutePath
    }
    
    suspend fun getAllGpxFiles(): List<File> = withContext(Dispatchers.IO) {
        getGpxDirectory().listFiles()?.filter { it.extension == "gpx" } ?: emptyList()
    }
    
    suspend fun deleteGpxFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(getGpxDirectory(), fileName)
        file.delete()
    }
    
    fun getGpxDirectoryPath(): File = getGpxDirectory()
    
    suspend fun getGpxFileInfo(): Map<String, Any> = withContext(Dispatchers.IO) {
        val directory = getGpxDirectory()
        val files = directory.listFiles()?.filter { it.extension == "gpx" } ?: emptyList()
        
        mapOf(
            "directory" to directory.absolutePath,
            "fileCount" to files.size,
            "files" to files.map { 
                mapOf(
                    "name" to it.name,
                    "size" to it.length(),
                    "lastModified" to it.lastModified()
                )
            }
        )
    }
    
    suspend fun saveAsGpxFile(sourceFile: File, targetPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(targetPath)
            // 确保目标目录存在
            targetFile.parentFile?.mkdirs()
            
            // 复制文件
            sourceFile.copyTo(targetFile, overwrite = true)
            Log.d("GpxExporter", "文件另存为成功: ${targetFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("GpxExporter", "文件另存为失败: $targetPath", e)
            false
        }
    }
    
    suspend fun exportToExternalStorage(fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(getGpxDirectory(), fileName)
            if (!sourceFile.exists()) {
                Log.e("GpxExporter", "源文件不存在: ${sourceFile.absolutePath}")
                return@withContext null
            }
            
            // 使用外部存储的Downloads目录
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val targetFile = File(downloadsDir, fileName)
            
            // 确保目标目录存在
            downloadsDir.mkdirs()
            
            // 复制文件
            sourceFile.copyTo(targetFile, overwrite = true)
            Log.d("GpxExporter", "文件导出到外部存储成功: ${targetFile.absolutePath}")
            targetFile.absolutePath
        } catch (e: Exception) {
            Log.e("GpxExporter", "导出到外部存储失败: $fileName", e)
            null
        }
    }
}
