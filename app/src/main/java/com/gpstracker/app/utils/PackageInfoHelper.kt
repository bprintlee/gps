package com.gpstracker.app.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

/**
 * PackageInfo获取辅助类
 * 提供安全的PackageInfo获取方法，避免null异常
 */
object PackageInfoHelper {
    
    private const val TAG = "PackageInfoHelper"
    
    /**
     * 安全获取PackageInfo
     * @param context 上下文
     * @param flags 标志位，默认为0
     * @return PackageInfo或null
     */
    fun getPackageInfo(context: Context, flags: Int = 0): PackageInfo? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, flags)
            if (packageInfo != null) {
                Log.d(TAG, "成功获取PackageInfo: ${packageInfo.versionName}")
                packageInfo
            } else {
                Log.w(TAG, "PackageInfo为null")
                null
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "包名未找到: ${context.packageName}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "获取PackageInfo失败", e)
            null
        }
    }
    
    /**
     * 获取应用版本名称
     * @param context 上下文
     * @return 版本名称或"未知"
     */
    fun getVersionName(context: Context): String {
        return getPackageInfo(context)?.versionName ?: "未知"
    }
    
    /**
     * 获取应用版本代码
     * @param context 上下文
     * @return 版本代码或0
     */
    fun getVersionCode(context: Context): Long {
        return try {
            val packageInfo = getPackageInfo(context)
            if (packageInfo != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // API 28+ 使用 longVersionCode
                    packageInfo.longVersionCode
                } else {
                    // API 24-27 使用 versionCode (int)
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取版本代码失败", e)
            0L
        }
    }
    
    /**
     * 获取应用包名
     * @param context 上下文
     * @return 包名或"未知"
     */
    fun getPackageName(context: Context): String {
        return try {
            context.packageName
        } catch (e: Exception) {
            Log.e(TAG, "获取包名失败", e)
            "未知"
        }
    }
    
    /**
     * 检查应用是否已安装
     * @param context 上下文
     * @return 是否已安装
     */
    fun isAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0) != null
        } catch (e: Exception) {
            Log.e(TAG, "检查应用安装状态失败", e)
            false
        }
    }
    
    /**
     * 获取应用信息摘要
     * @param context 上下文
     * @return 应用信息字符串
     */
    fun getAppInfoSummary(context: Context): String {
        return try {
            val packageInfo = getPackageInfo(context)
            if (packageInfo != null) {
                val versionCode = getVersionCode(context)
                "包名: ${packageInfo.packageName}, 版本: ${packageInfo.versionName} ($versionCode)"
            } else {
                "包名: ${getPackageName(context)}, 版本: 获取失败"
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取应用信息摘要失败", e)
            "应用信息获取失败"
        }
    }
}
