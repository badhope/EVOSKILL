package com.banana.toolbox.data.repository

import android.content.Context
import com.banana.toolbox.domain.model.FileItem
import com.banana.toolbox.domain.model.FileType
import com.banana.toolbox.domain.model.SortBy
import com.banana.toolbox.domain.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件仓库实现
 */
@Singleton
class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileRepository {
    
    override suspend fun listFiles(path: String, sortBy: SortBy): Result<List<FileItem>> {
        return try {
            val directory = File(path)
            if (!directory.exists() || !directory.isDirectory) {
                return Result.failure(Exception("目录不存在或不是有效目录"))
            }
            
            val files = directory.listFiles()
                ?.map { file ->
                    FileItem(
                        path = file.absolutePath,
                        name = file.name,
                        size = if (file.isFile) file.length() else 0L,
                        lastModified = file.lastModified(),
                        isDirectory = file.isDirectory,
                        extension = if (file.isFile) file.extension else null
                    )
                }
                ?.sortedWith(getComparator(sortBy))
                ?: emptyList()
            
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun copyFile(source: String, dest: String): Result<Unit> {
        return try {
            val sourceFile = File(source)
            val destFile = File(dest)
            sourceFile.copyTo(destFile, overwrite = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun moveFile(source: String, dest: String): Result<Unit> {
        return try {
            val sourceFile = File(source)
            val destFile = File(dest)
            sourceFile.renameTo(destFile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteFiles(paths: List<String>): Result<Unit> {
        return try {
            paths.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    if (file.isDirectory) {
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun renameFile(path: String, newName: String): Result<Unit> {
        return try {
            val file = File(path)
            val newFile = File(file.parentFile, newName)
            file.renameTo(newFile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createDirectory(parentPath: String, name: String): Result<Unit> {
        return try {
            val dir = File(parentPath, name)
            dir.mkdirs()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun searchFiles(query: String, basePath: String): Result<List<FileItem>> {
        return try {
            val results = mutableListOf<FileItem>()
            val baseDir = File(basePath)
            
            fun searchRecursive(dir: File) {
                dir.listFiles()?.forEach { file ->
                    if (file.name.contains(query, ignoreCase = true)) {
                        results.add(
                            FileItem(
                                path = file.absolutePath,
                                name = file.name,
                                size = if (file.isFile) file.length() else 0L,
                                lastModified = file.lastModified(),
                                isDirectory = file.isDirectory,
                                extension = if (file.isFile) file.extension else null
                            )
                        )
                    }
                    if (file.isDirectory) {
                        searchRecursive(file)
                    }
                }
            }
            
            searchRecursive(baseDir)
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getFileInfo(path: String): Result<FileItem> {
        return try {
            val file = File(path)
            if (!file.exists()) {
                return Result.failure(Exception("文件不存在"))
            }
            
            Result.success(
                FileItem(
                    path = file.absolutePath,
                    name = file.name,
                    size = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified(),
                    isDirectory = file.isDirectory,
                    extension = if (file.isFile) file.extension else null
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getComparator(sortBy: SortBy): Comparator<FileItem> {
        return when (sortBy) {
            SortBy.NAME -> compareBy { it.name.lowercase() }
            SortBy.SIZE -> compareByDescending { it.size }
            SortBy.DATE -> compareByDescending { it.lastModified }
            SortBy.TYPE -> compareBy { it.getFileType().name }
        }
    }
}
