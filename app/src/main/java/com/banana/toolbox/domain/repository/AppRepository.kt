package com.banana.toolbox.domain.repository

import com.banana.toolbox.domain.model.AppItem

/**
 * 应用仓库接口
 */
interface AppRepository {
    
    /**
     * 获取已安装的应用列表
     */
    suspend fun getInstalledApps(): Result<List<AppItem>>
    
    /**
     * 获取用户应用列表
     */
    suspend fun getUserApps(): Result<List<AppItem>>
    
    /**
     * 获取系统应用列表
     */
    suspend fun getSystemApps(): Result<List<AppItem>>
    
    /**
     * 卸载应用
     */
    suspend fun uninstallApp(packageName: String): Result<Unit>
    
    /**
     * 备份应用（导出 APK）
     */
    suspend fun backupApp(packageName: String, destPath: String): Result<String>
    
    /**
     * 获取应用详情
     */
    suspend fun getAppInfo(packageName: String): Result<AppItem>
}
