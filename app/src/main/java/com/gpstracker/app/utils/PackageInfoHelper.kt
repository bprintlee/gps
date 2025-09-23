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
            Log.d(TAG, "开始获取PackageInfo - 包名: ${context.packageName}, flags: $flags")
            
            // 检查上下文是否有效
            if (context == null) {
                Log.e(TAG, "Context为null")
                return null
            }
            
            // 检查包管理器是否可用
            val packageManager = context.packageManager
            if (packageManager == null) {
                Log.e(TAG, "PackageManager为null")
                return null
            }
            
            // 检查包名是否有效
            val packageName = context.packageName
            if (packageName.isNullOrEmpty()) {
                Log.e(TAG, "包名为空或null")
                return null
            }
            
            Log.d(TAG, "尝试获取PackageInfo - 包名: $packageName, flags: $flags")
            
            // 尝试获取PackageInfo
            val packageInfo = packageManager.getPackageInfo(packageName, flags)
            
            if (packageInfo != null) {
                Log.d(TAG, "成功获取PackageInfo: 版本=${packageInfo.versionName}, 包名=${packageInfo.packageName}")
                packageInfo
            } else {
                Log.w(TAG, "PackageInfo为null - 包名: $packageName")
                // 尝试不同的flags
                return tryAlternativeFlags(context, packageManager, packageName)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "包名未找到: ${context.packageName}", e)
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "权限不足，无法获取PackageInfo: ${context.packageName}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "获取PackageInfo失败: ${e.javaClass.simpleName} - ${e.message}", e)
            null
        }
    }
    
    /**
     * 尝试使用不同的flags获取PackageInfo
     */
    private fun tryAlternativeFlags(context: Context, packageManager: PackageManager, packageName: String): PackageInfo? {
        val alternativeFlags = listOf(
            PackageManager.GET_ACTIVITIES,
            PackageManager.GET_META_DATA,
            PackageManager.GET_SERVICES,
            PackageManager.GET_RECEIVERS,
            PackageManager.GET_PROVIDERS,
            PackageManager.GET_INSTRUMENTATION,
            PackageManager.GET_SIGNATURES,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
        
        for (flags in alternativeFlags) {
            try {
                Log.d(TAG, "尝试使用flags: $flags")
                val packageInfo = packageManager.getPackageInfo(packageName, flags)
                if (packageInfo != null) {
                    Log.d(TAG, "使用flags $flags 成功获取PackageInfo")
                    return packageInfo
                }
            } catch (e: Exception) {
                Log.d(TAG, "使用flags $flags 失败: ${e.message}")
            }
        }
        
        Log.e(TAG, "所有flags都失败，无法获取PackageInfo")
        return null
    }
    
    /**
     * 获取应用版本名称
     * @param context 上下文
     * @return 版本名称或"未知"
     */
    fun getVersionName(context: Context): String {
        // 首先尝试标准方法
        val packageInfo = getPackageInfo(context)
        if (packageInfo != null) {
            return packageInfo.versionName ?: "未知"
        }
        
        // 如果标准方法失败，尝试使用不同的flags
        Log.w(TAG, "标准方法获取PackageInfo失败，尝试替代方法")
        return getVersionNameAlternative(context)
    }
    
    /**
     * 替代方法获取版本名称
     * @param context 上下文
     * @return 版本名称或"未知"
     */
    private fun getVersionNameAlternative(context: Context): String {
        return try {
            // 尝试使用GET_ACTIVITIES标志
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
            if (packageInfo != null && packageInfo.versionName != null) {
                Log.d(TAG, "替代方法成功获取版本名称: ${packageInfo.versionName}")
                packageInfo.versionName!!
            } else {
                Log.w(TAG, "替代方法也返回null")
                "未知"
            }
        } catch (e: Exception) {
            Log.e(TAG, "替代方法获取版本名称失败: ${e.message}")
            "未知"
        }
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
     * 获取应用版本名称（简化版本，作为最后回退）
     * @param context 上下文
     * @return 版本名称或"1.0.0"
     */
    fun getVersionNameSimple(context: Context): String {
        return try {
            val versionName = getVersionName(context)
            if (versionName == "未知") {
                Log.w(TAG, "无法获取版本名称，使用默认值")
                "1.0.0"
            } else {
                versionName
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取版本名称失败，使用默认值", e)
            "1.0.0"
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
    
    /**
     * 诊断PackageInfo问题
     * @param context 上下文
     * @return 诊断信息字符串
     */
    fun diagnosePackageInfo(context: Context): String {
        val diagnostics = StringBuilder()
        diagnostics.appendLine("=== PackageInfo诊断信息 ===")
        
        try {
            // 检查上下文
            diagnostics.appendLine("上下文: ${context.javaClass.simpleName}")
            diagnostics.appendLine("包名: ${context.packageName}")
            
            // 检查包管理器
            val packageManager = context.packageManager
            diagnostics.appendLine("包管理器: ${if (packageManager != null) "可用" else "null"}")
            
            if (packageManager != null) {
                // 尝试不同的方法获取PackageInfo
                val testFlags = listOf(
                    0 to "标准方法(flags=0)",
                    PackageManager.GET_ACTIVITIES to "GET_ACTIVITIES",
                    PackageManager.GET_META_DATA to "GET_META_DATA",
                    PackageManager.GET_SERVICES to "GET_SERVICES",
                    PackageManager.GET_RECEIVERS to "GET_RECEIVERS",
                    PackageManager.GET_PROVIDERS to "GET_PROVIDERS",
                    PackageManager.GET_INSTRUMENTATION to "GET_INSTRUMENTATION",
                    PackageManager.GET_SIGNATURES to "GET_SIGNATURES",
                    PackageManager.GET_SIGNING_CERTIFICATES to "GET_SIGNING_CERTIFICATES"
                )
                
                for ((flags, description) in testFlags) {
                    try {
                        val packageInfo = packageManager.getPackageInfo(context.packageName, flags)
                        diagnostics.appendLine("$description: ${if (packageInfo != null) "成功" else "返回null"}")
                        if (packageInfo != null) {
                            diagnostics.appendLine("  - 版本名: ${packageInfo.versionName}")
                            diagnostics.appendLine("  - 包名: ${packageInfo.packageName}")
                            diagnostics.appendLine("  - 版本代码: ${packageInfo.versionCode}")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                diagnostics.appendLine("  - 长版本代码: ${packageInfo.longVersionCode}")
                            }
                        }
                    } catch (e: Exception) {
                        diagnostics.appendLine("$description: 失败 - ${e.javaClass.simpleName}: ${e.message}")
                    }
                }
                
                // 检查应用是否已安装
                diagnostics.appendLine()
                diagnostics.appendLine("=== 应用安装状态检查 ===")
                try {
                    val isInstalled = isAppInstalled(context)
                    diagnostics.appendLine("应用安装状态: ${if (isInstalled) "已安装" else "未安装"}")
                } catch (e: Exception) {
                    diagnostics.appendLine("应用安装状态检查失败: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            diagnostics.appendLine("诊断过程中发生异常: ${e.javaClass.simpleName}: ${e.message}")
        }
        
        val result = diagnostics.toString()
        Log.d(TAG, result)
        return result
    }
}
