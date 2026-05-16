package com.banana.toolbox.ui.screens.filemanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banana.toolbox.domain.model.FileItem
import com.banana.toolbox.domain.model.SortBy
import com.banana.toolbox.domain.usecase.file.FileUseCases
import com.banana.toolbox.domain.usecase.file.StorageInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 文件管理 ViewModel
 */
@HiltViewModel
class FileManagerViewModel @Inject constructor(
    private val fileUseCases: FileUseCases
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FileManagerUiState())
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()
    
    init {
        loadFiles(fileUseCases.getRootPath())
    }
    
    /**
     * 加载文件列表
     */
    fun loadFiles(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentPath = path) }
            
            fileUseCases.getFiles(path, _uiState.value.sortBy)
                .onSuccess { files ->
                    _uiState.update { 
                        it.copy(
                            files = files,
                            isLoading = false,
                            error = null,
                            selectedFiles = emptySet()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "加载失败"
                        )
                    }
                }
            
            // 加载存储信息
            loadStorageInfo(path)
        }
    }
    
    /**
     * 加载存储信息
     */
    private fun loadStorageInfo(path: String) {
        viewModelScope.launch {
            val storageInfo = fileUseCases.getStorageInfo(path)
            _uiState.update { it.copy(storageInfo = storageInfo) }
        }
    }
    
    /**
     * 进入目录
     */
    fun enterDirectory(path: String) {
        loadFiles(path)
    }
    
    /**
     * 返回上级目录
     */
    fun navigateUp(): Boolean {
        val parentPath = fileUseCases.getParentPath(_uiState.value.currentPath)
        return if (parentPath != null) {
            loadFiles(parentPath)
            true
        } else {
            false
        }
    }
    
    /**
     * 切换排序方式
     */
    fun setSortBy(sortBy: SortBy) {
        _uiState.update { it.copy(sortBy = sortBy) }
        loadFiles(_uiState.value.currentPath)
    }
    
    /**
     * 切换视图模式
     */
    fun toggleViewMode() {
        _uiState.update { 
            it.copy(
                viewMode = if (it.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
            )
        }
    }
    
    /**
     * 搜索文件
     */
    fun search(query: String) {
        if (query.isBlank()) {
            loadFiles(_uiState.value.currentPath)
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isSearching = true) }
            
            fileUseCases.searchFiles(query, _uiState.value.currentPath)
                .onSuccess { files ->
                    _uiState.update { it.copy(files = files, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
    
    /**
     * 退出搜索
     */
    fun exitSearch() {
        _uiState.update { it.copy(isSearching = false) }
        loadFiles(_uiState.value.currentPath)
    }
    
    /**
     * 选择文件
     */
    fun toggleFileSelection(path: String) {
        _uiState.update { state ->
            val newSelection = if (path in state.selectedFiles) {
                state.selectedFiles - path
            } else {
                state.selectedFiles + path
            }
            state.copy(
                selectedFiles = newSelection,
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
                selectedFiles = state.files.map { it.path }.toSet(),
                isSelectionMode = true
            )
        }
    }
    
    /**
     * 取消选择
     */
    fun clearSelection() {
        _uiState.update { 
            it.copy(selectedFiles = emptySet(), isSelectionMode = false)
        }
    }
    
    /**
     * 删除选中文件
     */
    fun deleteSelected() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            fileUseCases.deleteFiles(_uiState.value.selectedFiles.toList())
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    loadFiles(_uiState.value.currentPath)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
    
    /**
     * 新建文件夹
     */
    fun createFolder(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            fileUseCases.createDirectory(_uiState.value.currentPath, name)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    loadFiles(_uiState.value.currentPath)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
    
    /**
     * 重命名
     */
    fun rename(oldPath: String, newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            fileUseCases.renameFile(oldPath, newName)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    loadFiles(_uiState.value.currentPath)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
    
    /**
     * 复制选中文件
     */
    fun copySelected(targetPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val results = _uiState.value.selectedFiles.map { sourcePath ->
                val fileName = sourcePath.substringAfterLast("/")
                val targetFile = "$targetPath/$fileName"
                fileUseCases.copyFile(sourcePath, targetFile)
            }
            
            val hasError = results.any { it.isFailure }
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    error = if (hasError) "部分文件复制失败" else null
                )
            }
            clearSelection()
        }
    }
    
    /**
     * 移动选中文件
     */
    fun moveSelected(targetPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val results = _uiState.value.selectedFiles.map { sourcePath ->
                val fileName = sourcePath.substringAfterLast("/")
                val targetFile = "$targetPath/$fileName"
                fileUseCases.moveFile(sourcePath, targetFile)
            }
            
            val hasError = results.any { it.isFailure }
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    error = if (hasError) "部分文件移动失败" else null
                )
            }
            loadFiles(_uiState.value.currentPath)
        }
    }
    
    /**
     * 显示错误
     */
    fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI 状态
 */
data class FileManagerUiState(
    val currentPath: String = "/storage/emulated/0",
    val files: List<FileItem> = emptyList(),
    val selectedFiles: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val sortBy: SortBy = SortBy.NAME,
    val viewMode: ViewMode = ViewMode.LIST,
    val storageInfo: StorageInfo? = null,
    val error: String? = null
)

enum class ViewMode {
    LIST, GRID
}
