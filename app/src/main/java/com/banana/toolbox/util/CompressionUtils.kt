package com.banana.toolbox.util

import java.io.*
import java.util.zip.*

/**
 * 压缩解压工具类
 */
object CompressionUtils {
    
    /**
     * 压缩文件或文件夹为 ZIP
     */
    suspend fun zip(
        sourcePath: String,
        destPath: String,
        onProgress: (Int) -> Unit = {}
    ): Result<String> {
        return try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)
            
            ZipOutputStream(BufferedOutputStream(FileOutputStream(destFile))).use { zos ->
                if (sourceFile.isDirectory) {
                    zipDirectory(sourceFile, sourceFile.parentFile, zos, onProgress)
                } else {
                    zipFile(sourceFile, zos)
                }
            }
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun zipDirectory(
        dir: File,
        baseDir: File,
        zos: ZipOutputStream,
        onProgress: (Int) -> Unit
    ) {
        val files = dir.listFiles() ?: return
        var count = 0
        for (file in files) {
            if (file.isDirectory) {
                zipDirectory(file, baseDir, zos, onProgress)
            } else {
                zipFile(file, zos, getRelativePath(file, baseDir))
                count++
                onProgress(count)
            }
        }
    }
    
    private fun zipFile(file: File, zos: ZipOutputStream, relativePath: String = file.name) {
        BufferedInputStream(FileInputStream(file)).use { bis ->
            val entry = ZipEntry(relativePath)
            zos.putNextEntry(entry)
            bis.copyTo(zos, 8192)
            zos.closeEntry()
        }
    }
    
    private fun getRelativePath(file: File, baseDir: File): String {
        return file.absolutePath.removePrefix(baseDir.absolutePath).removePrefix("/")
    }
    
    /**
     * 解压 ZIP 文件
     */
    suspend fun unzip(
        zipPath: String,
        destPath: String,
        onProgress: (Int) -> Unit = {}
    ): Result<List<String>> {
        return try {
            val extractedFiles = mutableListOf<String>()
            val zipFile = File(zipPath)
            val destDir = File(destPath)
            
            if (!destDir.exists()) destDir.mkdirs()
            
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries().toList()
                var count = 0
                
                for (entry in entries) {
                    if (entry.isDirectory) {
                        File(destDir, entry.name).mkdirs()
                    } else {
                        val outputFile = File(destDir, entry.name)
                        outputFile.parentFile?.mkdirs()
                        
                        BufferedInputStream(zip.getInputStream(entry)).use { bis ->
                            BufferedOutputStream(FileOutputStream(outputFile)).use { bos ->
                                bis.copyTo(bos, 8192)
                            }
                        }
                        extractedFiles.add(outputFile.absolutePath)
                    }
                    count++
                    onProgress((count * 100) / entries.size)
                }
            }
            Result.success(extractedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取 ZIP 文件内文件列表
     */
    fun listZipContents(zipPath: String): Result<List<ZipEntryInfo>> {
        return try {
            val zipFile = File(zipPath)
            val entries = mutableListOf<ZipEntryInfo>()
            
            ZipFile(zipFile).use { zip ->
                for (entry in zip.entries()) {
                    entries.add(
                        ZipEntryInfo(
                            name = entry.name,
                            isDirectory = entry.isDirectory,
                            size = entry.size,
                            compressedSize = entry.compressedSize
                        )
                    )
                }
            }
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class ZipEntryInfo(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long
)
