package com.gpstracker.app.utils

import android.content.Context
import android.os.Environment
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
        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "gpx_tracks")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }
    
    private fun getGpxFileName(): String {
        val today = dateFormat.format(Date())
        return "gps_track_$today.gpx"
    }
    
    suspend fun appendGpsData(dataList: List<GpsData>) = withContext(Dispatchers.IO) {
        val gpxFile = File(getGpxDirectory(), getGpxFileName())
        
        if (!gpxFile.exists()) {
            createNewGpxFile(gpxFile)
        }
        
        appendToGpxFile(gpxFile, dataList)
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
}
