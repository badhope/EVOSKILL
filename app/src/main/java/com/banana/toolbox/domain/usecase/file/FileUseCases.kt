package com.banana.toolbox.domain.usecase.file

import com.banana.toolbox.domain.model.FileItem
import com.banana.toolbox.domain.model.SortBy
import com.banana.toolbox.domain.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 文件管理用例
 */
class FileUseCases @Inject constructor(
    private val fileRepository: FileRepository
) {
    
    /**
     * 获取文件列表
     */
    suspend fun getFiles(path: String, sortBy: SortBy = SortBy.NAME): Result<List<FileItem>> {
        return withContext(Dispatchers.IO) {
            fileRepository.listFiles(path, sortBy)
        }
    }
    
    /**
     * 获取根目录路径
     */
    fun getRootPath(): String {
        return "/storage/emulated/0"
    }
    
    /**
     * 获取父目录路径
     */
    fun getParentPath(path: String): String? {
        val file = File(path)
        return file.parentFile?.absolutePath
    }
    
    /**
     * 搜索文件
     */
    suspend fun searchFiles(query: String, basePath: String): Result<List<FileItem>> {
        return withContext(Dispatchers.IO) {
            fileRepository.searchFiles(query, basePath)
        }
    }
    
    /**
     * 复制文件
     */
    suspend fun copyFile(source: String, dest: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            fileRepository.copyFile(source, dest)
        }
    }
    
    /**
     * 移动文件
     */
    suspend fun moveFile(source: String, dest: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            fileRepository.moveFile(source, dest)
        }
    }
    
    /**
     * 删除文件
     */
    suspend fun deleteFiles(paths: List<String>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            fileRepository.deleteFiles(paths)
        }
    }
    
    /**
     * 重命名文件
     */
    suspend fun renameFile(path: String, newName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            fileRepository.renameFile(path, newName)
        }
    }
    
    /**
     * 新建文件夹
     */
    suspend fun createDirectory(parentPath: String, name: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            fileRepository.createDirectory(parentPath, name)
        }
    }
    
    /**
     * 获取文件信息
     */
    suspend fun getFileInfo(path: String): Result<FileItem> {
        return withContext(Dispatchers.IO) {
            fileRepository.getFileInfo(path)
        }
    }
    
    /**
     * 获取存储信息
     */
    suspend fun getStorageInfo(path: String): StorageInfo {
        return withContext(Dispatchers.IO) {
            val file = File(path)
            val stat = android.os.StatFs(file.absolutePath)
            
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            val usedBytes = totalBytes - freeBytes
            
            StorageInfo(
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                freeBytes = freeBytes,
                usagePercent = if (totalBytes > 0) (usedBytes * 100 / totalBytes).toInt() else 0
            )
        }
    }
}

data class StorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val usagePercent: Int
) {
    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1099511627776 -> "%.1f TB".format(bytes / 1099511627776.0)
            bytes >= 1073741824 -> "%.1f GB".format(bytes / 1073741824.0)
            bytes >= 1048576 -> "%.1f MB".format(bytes / 1048576.0)
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
