package com.banana.toolbox.domain.usecase.app

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import com.banana.toolbox.domain.model.AppItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用管理用例
 */
@Singleton
class AppUseCases @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val packageManager: PackageManager = context.packageManager
    
    /**
     * 获取所有已安装应用
     */
    suspend fun getInstalledApps(): Result<List<AppItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
                val apps = packages.mapNotNull { packageInfo ->
                    try {
                        val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                        AppItem(
                            packageName = packageInfo.packageName,
                            appName = appInfo.loadLabel(packageManager).toString(),
                            versionName = packageInfo.versionName ?: "Unknown",
                            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                packageInfo.longVersionCode
                            } else {
                                @Suppress("DEPRECATION")
                                packageInfo.versionCode.toLong()
                            },
                            size = File(appInfo.sourceDir).length(),
                            installTime = packageInfo.firstInstallTime,
                            updateTime = packageInfo.lastUpdateTime,
                            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                            apkPath = appInfo.sourceDir
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedBy { it.appName.lowercase() }
                Result.success(apps)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 获取用户应用
     */
    suspend fun getUserApps(): Result<List<AppItem>> {
        return getInstalledApps().map { apps ->
            apps.filter { !it.isSystemApp }
        }
    }
    
    /**
     * 获取系统应用
     */
    suspend fun getSystemApps(): Result<List<AppItem>> {
        return getInstalledApps().map { apps ->
            apps.filter { it.isSystemApp }
        }
    }
    
    /**
     * 卸载应用
     */
    fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    /**
     * 打开应用设置
     */
    fun openAppSettings(packageName: String) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * 启动应用
     */
    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 备份应用（导出 APK）
     */
    suspend fun backupApp(packageName: String, destDir: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val sourceFile = File(appInfo.sourceDir)
                val destFile = File(destDir, "${getAppName(packageName)}_${packageManager.getPackageInfo(packageName, 0).versionName}.apk")
                
                if (!destFile.parentFile?.exists()!!) {
                    destFile.parentFile?.mkdirs()
                }
                
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                Result.success(destFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 批量备份应用
     */
    suspend fun backupApps(
        packages: List<String>,
        destDir: String,
        onProgress: (Int, Int) -> Unit
    ): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<String>()
            packages.forEachIndexed { index, packageName ->
                backupApp(packageName, destDir)
                    .onSuccess { results.add(it) }
                onProgress(index + 1, packages.size)
            }
            Result.success(results)
        }
    }
    
    /**
     * 获取应用详情
     */
    suspend fun getAppInfo(packageName: String): Result<AppItem> {
        return withContext(Dispatchers.IO) {
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
                val appInfo = packageInfo.applicationInfo ?: return@withContext Result.failure(Exception("应用信息不可用"))
                
                Result.success(
                    AppItem(
                        packageName = packageInfo.packageName,
                        appName = appInfo.loadLabel(packageManager).toString(),
                        versionName = packageInfo.versionName ?: "Unknown",
                        versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            packageInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            packageInfo.versionCode.toLong()
                        },
                        size = File(appInfo.sourceDir).length(),
                        installTime = packageInfo.firstInstallTime,
                        updateTime = packageInfo.lastUpdateTime,
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        apkPath = appInfo.sourceDir
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 获取应用权限列表
     */
    suspend fun getAppPermissions(packageName: String): Result<List<PermissionInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                val permissions = packageInfo.requestedPermissions?.mapIndexed { index, permName ->
                    val granted = packageInfo.requestedPermissionsFlags?.get(index)?.let {
                        (it and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                    } ?: false
                    
                    PermissionInfo(
                        name = permName.substringAfterLast("."),
                        fullName = permName,
                        isDangerous = isDangerousPermission(permName),
                        isGranted = granted
                    )
                } ?: emptyList()
                Result.success(permissions)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }
    }
    
    private fun isDangerousPermission(permission: String): Boolean {
        return permission.startsWith("android.permission.") && permission in DANGEROUS_PERMISSIONS
    }
    
    companion object {
        private val DANGEROUS_PERMISSIONS = setOf(
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.GET_ACCOUNTS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_PHONE_STATE",
            "android.permission.CALL_PHONE",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.CAMERA",
            "android.permission.BODY_SENSORS"
        )
    }
}

data class PermissionInfo(
    val name: String,
    val fullName: String,
    val isDangerous: Boolean,
    val isGranted: Boolean
)
