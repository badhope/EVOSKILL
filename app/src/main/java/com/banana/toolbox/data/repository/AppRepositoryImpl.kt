package com.banana.toolbox.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.banana.toolbox.domain.model.AppItem
import com.banana.toolbox.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用仓库实现
 */
@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppRepository {
    
    override suspend fun getInstalledApps(): Result<List<AppItem>> {
        return try {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            
            val apps = packages.map { packageInfo ->
                val appInfo = packageInfo.applicationInfo
                AppItem(
                    packageName = packageInfo.packageName,
                    appName = appInfo.loadLabel(pm).toString(),
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
                    isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                    apkPath = appInfo.sourceDir
                )
            }.sortedBy { it.appName.lowercase() }
            
            Result.success(apps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUserApps(): Result<List<AppItem>> {
        return getInstalledApps().map { apps ->
            apps.filter { !it.isSystemApp }
        }
    }
    
    override suspend fun getSystemApps(): Result<List<AppItem>> {
        return getInstalledApps().map { apps ->
            apps.filter { it.isSystemApp }
        }
    }
    
    override suspend fun uninstallApp(packageName: String): Result<Unit> {
        return try {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_DELETE,
                android.net.Uri.parse("package:$packageName")
            )
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun backupApp(packageName: String, destPath: String): Result<String> {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val sourceFile = File(appInfo.sourceDir)
            val destFile = File(destPath, "${packageName}.apk")
            
            sourceFile.copyTo(destFile, overwrite = true)
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAppInfo(packageName: String): Result<AppItem> {
        return try {
            val pm = context.packageManager
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            val appInfo = packageInfo.applicationInfo
            
            Result.success(
                AppItem(
                    packageName = packageInfo.packageName,
                    appName = appInfo.loadLabel(pm).toString(),
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
                    isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                    apkPath = appInfo.sourceDir
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
