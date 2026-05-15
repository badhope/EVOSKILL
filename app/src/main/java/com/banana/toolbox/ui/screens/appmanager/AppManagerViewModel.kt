package com.banana.toolbox.ui.screens.appmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banana.toolbox.domain.model.AppItem
import com.banana.toolbox.domain.usecase.app.AppUseCases
import com.banana.toolbox.domain.usecase.app.PermissionInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 应用管理 ViewModel
 */
@HiltViewModel
class AppManagerViewModel @Inject constructor(
    private val appUseCases: AppUseCases
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AppManagerUiState())
    val uiState: StateFlow<AppManagerUiState> = _uiState.asStateFlow()
    
    init {
        loadApps()
    }
    
    /**
     * 加载应用列表
     */
    fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            appUseCases.getInstalledApps()
                .onSuccess { apps ->
                    _uiState.update { 
                        it.copy(
                            allApps = apps,
                            filteredApps = filterApps(apps, it.tabIndex, it.searchQuery),
                            isLoading = false,
                            error = null
                        )
                    }
                    updateStatistics(apps)
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "加载失败"
                        )
                    }
                }
        }
    }
    
    /**
     * 切换 Tab
     */
    fun setTab(tabIndex: Int) {
        _uiState.update { state ->
            state.copy(
                tabIndex = tabIndex,
                filteredApps = filterApps(state.allApps, tabIndex, state.searchQuery)
            )
        }
    }
    
    /**
     * 搜索应用
     */
    fun search(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredApps = filterApps(state.allApps, state.tabIndex, query)
            )
        }
    }
    
    /**
     * 筛选应用
     */
    private fun filterApps(apps: List<AppItem>, tabIndex: Int, query: String): List<AppItem> {
        val filtered = when (tabIndex) {
            1 -> apps.filter { !it.isSystemApp }
            2 -> apps.filter { it.isSystemApp }
            else -> apps
        }
        
        return if (query.isBlank()) filtered
        else filtered.filter { it.appName.contains(query, ignoreCase = true) }
    }
    
    /**
     * 更新统计信息
     */
    private fun updateStatistics(apps: List<AppItem>) {
        val userApps = apps.filter { !it.isSystemApp }
        val systemApps = apps.filter { it.isSystemApp }
        val totalSize = apps.sumOf { it.size }
        
        _uiState.update { 
            it.copy(
                statistics = AppStatistics(
                    totalCount = apps.size,
                    userAppCount = userApps.size,
                    systemAppCount = systemApps.size,
                    totalSize = totalSize
                )
            )
        }
    }
    
    /**
     * 选择应用
     */
    fun toggleSelection(packageName: String) {
        _uiState.update { state ->
            val newSelection = if (packageName in state.selectedApps) {
                state.selectedApps - packageName
            } else {
                state.selectedApps + packageName
            }
            state.copy(
                selectedApps = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }
    
    /**
     * 全选
     */
    fun selectAll() {
        _uiState.update { state ->
            state.copy(
                selectedApps = state.filteredApps.map { it.packageName }.toSet(),
                isSelectionMode = true
            )
        }
    }
    
    /**
     * 取消选择
     */
    fun clearSelection() {
        _uiState.update { 
            it.copy(selectedApps = emptySet(), isSelectionMode = false) 
        }
    }
    
    /**
     * 卸载应用
     */
    fun uninstallApp(packageName: String) {
        appUseCases.uninstallApp(packageName)
    }
    
    /**
     * 批量卸载
     */
    fun uninstallSelected() {
        _uiState.value.selectedApps.forEach { packageName ->
            appUseCases.uninstallApp(packageName)
        }
        clearSelection()
        loadApps()
    }
    
    /**
     * 备份应用
     */
    fun backupApp(packageName: String, destPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true) }
            
            appUseCases.backupApp(packageName, destPath)
                .onSuccess { path ->
                    _uiState.update { 
                        it.copy(
                            isBackingUp = false,
                            lastBackupPath = path
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isBackingUp = false,
                            error = "备份失败: ${error.message}"
                        )
                    }
                }
        }
    }
    
    /**
     * 批量备份
     */
    fun backupSelected(destPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, backupProgress = 0) }
            
            appUseCases.backupApps(
                packages = _uiState.value.selectedApps.toList(),
                destDir = destPath,
                onProgress = { current, total ->
                    _uiState.update { 
                        it.copy(backupProgress = (current * 100) / total) 
                    }
                }
            ).onSuccess { paths ->
                _uiState.update { 
                    it.copy(
                        isBackingUp = false,
                        lastBackupPath = paths.firstOrNull()
                    )
                }
                clearSelection()
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isBackingUp = false,
                        error = "备份失败: ${error.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 打开应用
     */
    fun launchApp(packageName: String) {
        if (!appUseCases.launchApp(packageName)) {
            _uiState.update { it.copy(error = "无法启动应用") }
        }
    }
    
    /**
     * 打开应用设置
     */
    fun openAppSettings(packageName: String) {
        appUseCases.openAppSettings(packageName)
    }
    
    /**
     * 加载应用详情
     */
    fun loadAppDetails(packageName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetails = true) }
            
            appUseCases.getAppInfo(packageName)
                .onSuccess { app ->
                    _uiState.update { it.copy(selectedApp = app, isLoadingDetails = false) }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoadingDetails = false,
                            error = error.message
                        )
                    }
                }
        }
    }
    
    /**
     * 加载应用权限
     */
    fun loadAppPermissions(packageName: String) {
        viewModelScope.launch {
            appUseCases.getAppPermissions(packageName)
                .onSuccess { permissions ->
                    _uiState.update { it.copy(permissions = permissions) }
                }
        }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * 清除选择的应用
     */
    fun clearSelectedApp() {
        _uiState.update { it.copy(selectedApp = null, permissions = emptyList()) }
    }
}

/**
 * UI 状态
 */
data class AppManagerUiState(
    val allApps: List<AppItem> = emptyList(),
    val filteredApps: List<AppItem> = emptyList(),
    val selectedApps: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingDetails: Boolean = false,
    val isBackingUp: Boolean = false,
    val backupProgress: Int = 0,
    val tabIndex: Int = 0,
    val searchQuery: String = "",
    val statistics: AppStatistics = AppStatistics(),
    val selectedApp: AppItem? = null,
    val permissions: List<PermissionInfo> = emptyList(),
    val lastBackupPath: String? = null,
    val error: String? = null
)

data class AppStatistics(
    val totalCount: Int = 0,
    val userAppCount: Int = 0,
    val systemAppCount: Int = 0,
    val totalSize: Long = 0L
) {
    fun formatSize(): String {
        return when {
            totalSize >= 1099511627776 -> "%.1f TB".format(totalSize / 1099511627776.0)
            totalSize >= 1073741824 -> "%.1f GB".format(totalSize / 1073741824.0)
            totalSize >= 1048576 -> "%.1f MB".format(totalSize / 1048576.0)
            else -> "%.1f KB".format(totalSize / 1024.0)
        }
    }
}
