package com.banana.toolbox.domain.repository

import com.banana.toolbox.domain.model.FileItem
import com.banana.toolbox.domain.model.SortBy

/**
 * 文件仓库接口
 */
interface FileRepository {
    
    /**
     * 列出目录下的文件
     */
    suspend fun listFiles(path: String, sortBy: SortBy = SortBy.NAME): Result<List<FileItem>>
    
    /**
     * 复制文件
     */
    suspend fun copyFile(source: String, dest: String): Result<Unit>
    
    /**
     * 移动文件
     */
    suspend fun moveFile(source: String, dest: String): Result<Unit>
    
    /**
     * 删除文件
     */
    suspend fun deleteFiles(paths: List<String>): Result<Unit>
    
    /**
     * 重命名文件
     */
    suspend fun renameFile(path: String, newName: String): Result<Unit>
    
    /**
     * 创建目录
     */
    suspend fun createDirectory(parentPath: String, name: String): Result<Unit>
    
    /**
     * 搜索文件
     */
    suspend fun searchFiles(query: String, basePath: String): Result<List<FileItem>>
    
    /**
     * 获取文件信息
     */
    suspend fun getFileInfo(path: String): Result<FileItem>
}
