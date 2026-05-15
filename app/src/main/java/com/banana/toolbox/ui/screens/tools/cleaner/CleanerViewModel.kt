package com.banana.toolbox.ui.screens.tools.cleaner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banana.toolbox.domain.usecase.tools.CleanerUseCases
import com.banana.toolbox.domain.usecase.tools.JunkCategory
import com.banana.toolbox.domain.usecase.tools.JunkItem
import com.banana.toolbox.domain.usecase.tools.JunkScanResult
import com.banana.toolbox.domain.usecase.tools.SearchResult
import com.banana.toolbox.domain.usecase.tools.SimilarGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 垃圾清理 ViewModel
 *
 * 封装 [CleanerUseCases]，管理垃圾扫描、文件搜索、大文件查找、重复文件查找的 UI 状态。
 */
@HiltViewModel
class CleanerViewModel @Inject constructor(
    private val cleanerUseCases: CleanerUseCases,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CleanerUiState())
    val uiState: StateFlow<CleanerUiState> = _uiState.asStateFlow()

    // ==================== Tab 切换 ====================

    fun setActiveTab(tab: CleanerTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // ==================== 垃圾扫描 ====================

    /**
     * 扫描垃圾文件
     */
    fun scanJunk() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    scanError = null,
                    scanResult = null,
                    selectedCategories = emptySet()
                )
            }

            try {
                val result = cleanerUseCases.scanJunkFiles(appContext)
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanResult = result,
                        selectedCategories = result.categories.keys
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanError = e.message ?: "扫描失败"
                    )
                }
            }
        }
    }

    /**
     * 切换分类选中状态
     */
    fun toggleCategory(category: JunkCategory) {
        _uiState.update { state ->
            val updated = if (category in state.selectedCategories) {
                state.selectedCategories - category
            } else {
                state.selectedCategories + category
            }
            state.copy(selectedCategories = updated)
        }
    }

    /**
     * 全选/取消全选分类
     */
    fun toggleSelectAll() {
        _uiState.update { state ->
            val allCategories = state.scanResult?.categories?.keys ?: emptySet()
            val updated = if (state.selectedCategories.size == allCategories.size) {
                emptySet()
            } else {
                allCategories
            }
            state.copy(selectedCategories = updated)
        }
    }

    /**
     * 获取选中分类对应的垃圾项
     */
    private fun getSelectedJunkItems(): List<JunkItem> {
        val state = _uiState.value
        val result = state.scanResult ?: return emptyList()
        return result.items.filter { it.category in state.selectedCategories }
    }

    /**
     * 计算选中分类的总大小
     */
    fun getSelectedSize(): Long {
        val state = _uiState.value
        val result = state.scanResult ?: return 0L
        return state.selectedCategories.sumOf { category ->
            result.categories[category] ?: 0L
        }
    }

    /**
     * 一键清理选中的垃圾文件
     */
    fun cleanSelected() {
        val items = getSelectedJunkItems()
        if (items.isEmpty()) {
            _uiState.update { it.copy(scanError = "请至少选择一个分类") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCleaning = true, cleanResult = null, scanError = null) }

            try {
                val result = cleanerUseCases.cleanItems(items)
                _uiState.update { state ->
                    state.copy(
                        isCleaning = false,
                        cleanResult = result,
                        // 清理后重新扫描以更新数据
                    )
                }
                // 清理完成后重新扫描
                scanJunk()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCleaning = false,
                        scanError = e.message ?: "清理失败"
                    )
                }
            }
        }
    }

    // ==================== 文件搜索 ====================

    /**
     * 设置搜索关键词
     */
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * 设置文件类型过滤
     */
    fun setFileTypeFilter(filter: FileTypeFilter) {
        _uiState.update { it.copy(fileTypeFilter = filter) }
    }

    /**
     * 设置排序方式
     */
    fun setSortMode(sortMode: FileSortMode) {
        _uiState.update { state ->
            val sortedResults = sortSearchResults(state.searchResults, sortMode)
            state.copy(sortMode = sortMode, searchResults = sortedResults)
        }
    }

    /**
     * 执行文件搜索
     */
    fun searchFiles() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) {
            _uiState.update { it.copy(searchError = "请输入搜索关键词") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }

            try {
                val results = cleanerUseCases.searchFiles(query)
                val filtered = filterByType(results, _uiState.value.fileTypeFilter)
                val sorted = sortSearchResults(filtered, _uiState.value.sortMode)
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = sorted
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchError = e.message ?: "搜索失败"
                    )
                }
            }
        }
    }

    /**
     * 按文件类型过滤搜索结果
     */
    private fun filterByType(
        results: List<SearchResult>,
        filter: FileTypeFilter
    ): List<SearchResult> {
        if (filter == FileTypeFilter.ALL) return results
        val extensions = when (filter) {
            FileTypeFilter.IMAGE -> listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg")
            FileTypeFilter.VIDEO -> listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "3gp")
            FileTypeFilter.AUDIO -> listOf("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a")
            FileTypeFilter.DOC -> listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv")
            FileTypeFilter.ARCHIVE -> listOf("zip", "rar", "7z", "tar", "gz", "bz2")
            FileTypeFilter.ALL -> emptyList()
        }
        return results.filter { result ->
            val ext = result.name.substringAfterLast('.', "").lowercase()
            ext in extensions
        }
    }

    /**
     * 排序搜索结果
     */
    private fun sortSearchResults(
        results: List<SearchResult>,
        sortMode: FileSortMode
    ): List<SearchResult> {
        return when (sortMode) {
            FileSortMode.NAME -> results.sortedBy { it.name.lowercase() }
            FileSortMode.SIZE_DESC -> results.sortedByDescending { it.size }
            FileSortMode.SIZE_ASC -> results.sortedBy { it.size }
            FileSortMode.DATE_DESC -> results.sortedByDescending { it.lastModified }
            FileSortMode.DATE_ASC -> results.sortedBy { it.lastModified }
        }
    }

    // ==================== 大文件管理 ====================

    /**
     * 设置最小文件大小 (MB)
     */
    fun setMinFileSize(mb: Float) {
        _uiState.update { it.copy(minFileSizeMB = mb) }
    }

    /**
     * 扫描大文件
     */
    fun scanLargeFiles() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanningLargeFiles = true,
                    largeFilesError = null
                )
            }

            try {
                val minSizeBytes = (_uiState.value.minFileSizeMB * 1024 * 1024).toLong()
                val results = cleanerUseCases.findLargeFiles(minSizeBytes)
                _uiState.update {
                    it.copy(
                        isScanningLargeFiles = false,
                        largeFiles = results,
                        selectedLargeFiles = emptySet()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanningLargeFiles = false,
                        largeFilesError = e.message ?: "扫描大文件失败"
                    )
                }
            }
        }
    }

    /**
     * 切换大文件选中状态
     */
    fun toggleLargeFileSelection(path: String) {
        _uiState.update { state ->
            val updated = if (path in state.selectedLargeFiles) {
                state.selectedLargeFiles - path
            } else {
                state.selectedLargeFiles + path
            }
            state.copy(selectedLargeFiles = updated)
        }
    }

    /**
     * 删除选中的大文件
     */
    fun deleteSelectedLargeFiles() {
        val state = _uiState.value
        val itemsToDelete = state.largeFiles
            .filter { it.path in state.selectedLargeFiles }
            .map { result ->
                JunkItem(
                    path = result.path,
                    name = result.name,
                    size = result.size,
                    category = JunkCategory.LARGE_FILES
                )
            }

        if (itemsToDelete.isEmpty()) {
            _uiState.update { it.copy(largeFilesError = "请至少选择一个文件") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingLargeFiles = true, largeFilesError = null) }

            try {
                val result = cleanerUseCases.cleanItems(itemsToDelete)
                _uiState.update { state ->
                    state.copy(
                        isDeletingLargeFiles = false,
                        largeFilesCleanResult = result,
                        // 从列表中移除已删除的文件
                        largeFiles = state.largeFiles.filter { it.path !in state.selectedLargeFiles },
                        selectedLargeFiles = emptySet()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeletingLargeFiles = false,
                        largeFilesError = e.message ?: "删除失败"
                    )
                }
            }
        }
    }

    // ==================== 重复文件 ====================

    /**
     * 扫描相似图片
     */
    fun scanSimilarImages() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanningDuplicates = true,
                    duplicatesError = null
                )
            }

            try {
                val groups = cleanerUseCases.findSimilarImages()
                _uiState.update {
                    it.copy(
                        isScanningDuplicates = false,
                        similarGroups = groups,
                        selectedDuplicateFiles = emptySet()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanningDuplicates = false,
                        duplicatesError = e.message ?: "扫描重复文件失败"
                    )
                }
            }
        }
    }

    /**
     * 切换重复文件选中状态
     */
    fun toggleDuplicateFileSelection(path: String) {
        _uiState.update { state ->
            val updated = if (path in state.selectedDuplicateFiles) {
                state.selectedDuplicateFiles - path
            } else {
                state.selectedDuplicateFiles + path
            }
            state.copy(selectedDuplicateFiles = updated)
        }
    }

    /**
     * 选中组内除第一个以外的所有文件（保留最新的，删除其余）
     */
    fun selectGroupExceptFirst(groupIndex: Int) {
        val group = _uiState.value.similarGroups.getOrNull(groupIndex) ?: return
        _uiState.update { state ->
            val toAdd = group.files.drop(1).map { it.path }.toSet()
            state.copy(selectedDuplicateFiles = state.selectedDuplicateFiles + toAdd)
        }
    }

    /**
     * 删除选中的重复文件
     */
    fun deleteSelectedDuplicates() {
        val state = _uiState.value
        val itemsToDelete = state.similarGroups
            .flatMap { group -> group.files }
            .filter { it.path in state.selectedDuplicateFiles }
            .map { result ->
                JunkItem(
                    path = result.path,
                    name = result.name,
                    size = result.size,
                    category = JunkCategory.DUPLICATE_FILES
                )
            }

        if (itemsToDelete.isEmpty()) {
            _uiState.update { it.copy(duplicatesError = "请至少选择一个文件") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingDuplicates = true, duplicatesError = null) }

            try {
                val result = cleanerUseCases.cleanItems(itemsToDelete)
                _uiState.update { state ->
                    // 从分组中移除已删除的文件，并过滤掉只剩一个文件的组
                    val updatedGroups = state.similarGroups
                        .map { group ->
                            group.copy(
                                files = group.files.filter { it.path !in state.selectedDuplicateFiles }
                            )
                        }
                        .filter { it.files.size > 1 }
                    state.copy(
                        isDeletingDuplicates = false,
                        duplicatesCleanResult = result,
                        similarGroups = updatedGroups,
                        selectedDuplicateFiles = emptySet()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeletingDuplicates = false,
                        duplicatesError = e.message ?: "删除失败"
                    )
                }
            }
        }
    }

    // ==================== 通用 ====================

    /**
     * 清除扫描结果中的错误信息
     */
    fun clearScanError() {
        _uiState.update { it.copy(scanError = null) }
    }

    /**
     * 清除搜索错误信息
     */
    fun clearSearchError() {
        _uiState.update { it.copy(searchError = null) }
    }

    /**
     * 清除大文件错误信息
     */
    fun clearLargeFilesError() {
        _uiState.update { it.copy(largeFilesError = null) }
    }

    /**
     * 清除重复文件错误信息
     */
    fun clearDuplicatesError() {
        _uiState.update { it.copy(duplicatesError = null) }
    }

    /**
     * 清除清理结果提示
     */
    fun clearCleanResult() {
        _uiState.update { it.copy(cleanResult = null) }
    }

    /**
     * 清除大文件清理结果提示
     */
    fun clearLargeFilesCleanResult() {
        _uiState.update { it.copy(largeFilesCleanResult = null) }
    }

    /**
     * 清除重复文件清理结果提示
     */
    fun clearDuplicatesCleanResult() {
        _uiState.update { it.copy(duplicatesCleanResult = null) }
    }
}

// ==================== UI 状态 ====================

/**
 * 垃圾清理页面 UI 状态
 */
data class CleanerUiState(
    // 通用
    val activeTab: CleanerTab = CleanerTab.JUNK_SCAN,

    // 垃圾扫描
    val isScanning: Boolean = false,
    val scanResult: JunkScanResult? = null,
    val selectedCategories: Set<JunkCategory> = emptySet(),
    val isCleaning: Boolean = false,
    val cleanResult: com.banana.toolbox.domain.usecase.tools.CleanResult? = null,
    val scanError: String? = null,

    // 文件搜索
    val searchQuery: String = "",
    val fileTypeFilter: FileTypeFilter = FileTypeFilter.ALL,
    val sortMode: FileSortMode = FileSortMode.DATE_DESC,
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,

    // 大文件管理
    val minFileSizeMB: Float = 50f,
    val largeFiles: List<SearchResult> = emptyList(),
    val selectedLargeFiles: Set<String> = emptySet(),
    val isScanningLargeFiles: Boolean = false,
    val isDeletingLargeFiles: Boolean = false,
    val largeFilesCleanResult: com.banana.toolbox.domain.usecase.tools.CleanResult? = null,
    val largeFilesError: String? = null,

    // 重复文件
    val similarGroups: List<SimilarGroup> = emptyList(),
    val selectedDuplicateFiles: Set<String> = emptySet(),
    val isScanningDuplicates: Boolean = false,
    val isDeletingDuplicates: Boolean = false,
    val duplicatesCleanResult: com.banana.toolbox.domain.usecase.tools.CleanResult? = null,
    val duplicatesError: String? = null
)

/**
 * 清理器 Tab 枚举
 */
enum class CleanerTab(val label: String) {
    JUNK_SCAN("垃圾扫描"),
    FILE_SEARCH("文件搜索"),
    LARGE_FILES("大文件管理"),
    DUPLICATES("重复文件")
}

/**
 * 文件类型过滤枚举
 */
enum class FileTypeFilter(val label: String) {
    ALL("全部"),
    IMAGE("图片"),
    VIDEO("视频"),
    AUDIO("音频"),
    DOC("文档"),
    ARCHIVE("压缩包")
}

/**
 * 文件排序方式枚举
 */
enum class FileSortMode(val label: String) {
    NAME("按名称"),
    SIZE_DESC("大小降序"),
    SIZE_ASC("大小升序"),
    DATE_DESC("时间降序"),
    DATE_ASC("时间升序")
}

// ==================== 工具函数 ====================

/**
 * 格式化文件大小为可读字符串
 */
fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

/**
 * 格式化时间戳为日期字符串
 */
fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
