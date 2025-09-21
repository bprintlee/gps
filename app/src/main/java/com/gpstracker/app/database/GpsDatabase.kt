package com.gpstracker.app.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.gpstracker.app.model.GpsData
import com.gpstracker.app.model.TrackingState

class GpsDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    companion object {
        private const val DATABASE_NAME = "gps_tracking.db"
        private const val DATABASE_VERSION = 2  // 增加版本号以支持数据库升级
        
        // 表名和列名
        const val TABLE_GPS_DATA = "gps_data"
        const val COLUMN_ID = "id"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_ALTITUDE = "altitude"
        const val COLUMN_ACCURACY = "accuracy"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_STATE = "state"
        const val COLUMN_TRIP_ID = "trip_id"  // 新增行程ID字段
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_GPS_DATA (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_ALTITUDE REAL NOT NULL,
                $COLUMN_ACCURACY REAL NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_STATE TEXT NOT NULL,
                $COLUMN_TRIP_ID TEXT
            )
        """.trimIndent()
        
        db.execSQL(createTable)
        Log.d("GpsDatabase", "数据库表创建成功")
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        when (oldVersion) {
            1 -> {
                // 从版本1升级到版本2：添加trip_id字段
                db.execSQL("ALTER TABLE $TABLE_GPS_DATA ADD COLUMN $COLUMN_TRIP_ID TEXT")
                Log.d("GpsDatabase", "数据库升级：添加trip_id字段")
            }
            else -> {
                // 其他版本升级，重建表
                db.execSQL("DROP TABLE IF EXISTS $TABLE_GPS_DATA")
                onCreate(db)
            }
        }
    }
    
    fun insertGpsData(gpsData: GpsData): Long {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put(COLUMN_LATITUDE, gpsData.latitude)
            put(COLUMN_LONGITUDE, gpsData.longitude)
            put(COLUMN_ALTITUDE, gpsData.altitude)
            put(COLUMN_ACCURACY, gpsData.accuracy)
            put(COLUMN_TIMESTAMP, gpsData.timestamp)
            put(COLUMN_STATE, gpsData.state.name)
            put(COLUMN_TRIP_ID, gpsData.tripId)
        }
        
        val result = db.insert(TABLE_GPS_DATA, null, values)
        Log.d("GpsDatabase", "插入GPS数据: $result")
        return result
    }
    
    fun insertGpsDataList(gpsDataList: List<GpsData>): Int {
        val db = writableDatabase
        var successCount = 0
        
        db.beginTransaction()
        try {
            for (gpsData in gpsDataList) {
                val values = android.content.ContentValues().apply {
                    put(COLUMN_LATITUDE, gpsData.latitude)
                    put(COLUMN_LONGITUDE, gpsData.longitude)
                    put(COLUMN_ALTITUDE, gpsData.altitude)
                    put(COLUMN_ACCURACY, gpsData.accuracy)
                    put(COLUMN_TIMESTAMP, gpsData.timestamp)
                    put(COLUMN_STATE, gpsData.state.name)
                    put(COLUMN_TRIP_ID, gpsData.tripId)
                }
                
                val result = db.insert(TABLE_GPS_DATA, null, values)
                if (result != -1L) {
                    successCount++
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("GpsDatabase", "批量插入GPS数据失败", e)
        } finally {
            db.endTransaction()
        }
        
        Log.d("GpsDatabase", "批量插入GPS数据: $successCount/${gpsDataList.size}")
        return successCount
    }
    
    fun getAllGpsData(): List<GpsData> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_GPS_DATA,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP ASC"
        )
        
        val gpsDataList = mutableListOf<GpsData>()
        
        cursor.use {
            while (it.moveToNext()) {
                val gpsData = GpsData(
                    latitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                    longitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                    altitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_ALTITUDE)),
                    accuracy = it.getFloat(it.getColumnIndexOrThrow(COLUMN_ACCURACY)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    state = TrackingState.valueOf(it.getString(it.getColumnIndexOrThrow(COLUMN_STATE))),
                    tripId = it.getString(it.getColumnIndexOrThrow(COLUMN_TRIP_ID))
                )
                gpsDataList.add(gpsData)
            }
        }
        
        Log.d("GpsDatabase", "查询到 ${gpsDataList.size} 条GPS数据")
        return gpsDataList
    }
    
    fun getGpsDataCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_GPS_DATA", null)
        
        var count = 0
        cursor.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }
        
        return count
    }
    
    fun clearAllData() {
        val db = writableDatabase
        val deletedRows = db.delete(TABLE_GPS_DATA, null, null)
        Log.d("GpsDatabase", "清空数据库: 删除了 $deletedRows 条记录")
    }
    
    fun getGpsDataByDateRange(startTime: Long, endTime: Long): List<GpsData> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_GPS_DATA,
            null,
            "$COLUMN_TIMESTAMP BETWEEN ? AND ?",
            arrayOf(startTime.toString(), endTime.toString()),
            null,
            null,
            "$COLUMN_TIMESTAMP ASC"
        )
        
        val gpsDataList = mutableListOf<GpsData>()
        
        cursor.use {
            while (it.moveToNext()) {
                val gpsData = GpsData(
                    latitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                    longitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                    altitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_ALTITUDE)),
                    accuracy = it.getFloat(it.getColumnIndexOrThrow(COLUMN_ACCURACY)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    state = TrackingState.valueOf(it.getString(it.getColumnIndexOrThrow(COLUMN_STATE))),
                    tripId = it.getString(it.getColumnIndexOrThrow(COLUMN_TRIP_ID))
                )
                gpsDataList.add(gpsData)
            }
        }
        
        return gpsDataList
    }
    
    // 按行程ID查询GPS数据
    fun getGpsDataByTripId(tripId: String): List<GpsData> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_GPS_DATA,
            null,
            "$COLUMN_TRIP_ID = ?",
            arrayOf(tripId),
            null,
            null,
            "$COLUMN_TIMESTAMP ASC"
        )
        
        val gpsDataList = mutableListOf<GpsData>()
        
        cursor.use {
            while (it.moveToNext()) {
                val gpsData = GpsData(
                    latitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                    longitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                    altitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_ALTITUDE)),
                    accuracy = it.getFloat(it.getColumnIndexOrThrow(COLUMN_ACCURACY)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    state = TrackingState.valueOf(it.getString(it.getColumnIndexOrThrow(COLUMN_STATE))),
                    tripId = it.getString(it.getColumnIndexOrThrow(COLUMN_TRIP_ID))
                )
                gpsDataList.add(gpsData)
            }
        }
        
        Log.d("GpsDatabase", "查询到行程 $tripId 的 ${gpsDataList.size} 条GPS数据")
        return gpsDataList
    }
    
    // 获取所有行程ID列表
    fun getAllTripIds(): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT DISTINCT $COLUMN_TRIP_ID FROM $TABLE_GPS_DATA WHERE $COLUMN_TRIP_ID IS NOT NULL ORDER BY $COLUMN_TIMESTAMP ASC",
            null
        )
        
        val tripIds = mutableListOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                val tripId = it.getString(0)
                if (tripId != null) {
                    tripIds.add(tripId)
                }
            }
        }
        
        Log.d("GpsDatabase", "查询到 ${tripIds.size} 个行程")
        return tripIds
    }
}
