package com.banana.toolbox.domain.usecase.tools

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 系统清理用例
 */
@Singleton
class CleanerUseCases @Inject constructor(
) {
    
    /**
     * 扫描垃圾文件
     */
    suspend fun scanJunkFiles(context: Context): JunkScanResult {
        return withContext(Dispatchers.IO) {
            val junkItems = mutableListOf<JunkItem>()
            
            // 1. 应用缓存
            scanAppCache(context, junkItems)
            
            // 2. 临时文件
            scanTempFiles(junkItems)
            
            // 3. 空文件夹
            scanEmptyFolders(junkItems)
            
            // 4. 大文件
            scanLargeFiles(junkItems)
            
            // 5. 重复文件
            scanDuplicateFiles(junkItems)
            
            // 6. 日志文件
            scanLogFiles(junkItems)
            
            // 7. 缩略图缓存
            scanThumbnailCache(context, junkItems)
            
            // 8. APK 安装包
            scanApkFiles(junkItems)
            
            // 9. 下载文件夹中的旧文件
            scanOldDownloads(junkItems)
            
            // 10. .nomedia 和 .DS_Store
            scanHiddenJunkFiles(junkItems)
            
            val totalSize = junkItems.sumOf { it.size }
            
            JunkScanResult(
                items = junkItems.sortedByDescending { it.size },
                totalSize = totalSize,
                totalFiles = junkItems.size,
                categories = junkItems.groupBy { it.category }
                    .mapValues { (category, items) -> items.sumOf { it.size } }
            )
        }
    }
    
    private fun scanAppCache(context: Context, items: MutableList<JunkItem>) {
        val cacheDir = context.cacheDir
        val size = getDirectorySize(cacheDir)
        if (size > 0) {
            items.add(JunkItem(
                path = cacheDir.absolutePath,
                name = "应用缓存",
                size = size,
                category = JunkCategory.APP_CACHE
            ))
        }
    }
    
    private fun scanTempFiles(items: MutableList<JunkItem>) {
        val tempDirs = listOf(
            "/data/local/tmp",
            "/sdcard/Android/data",
        )
        
        tempDirs.forEach { dirPath ->
            val dir = File(dirPath)
            if (dir.exists()) {
                dir.walkTopDown().forEach { file ->
                    if (file.isFile && file.name.endsWith(".tmp")) {
                        items.add(JunkItem(
                            path = file.absolutePath,
                            name = file.name,
                            size = file.length(),
                            category = JunkCategory.TEMP_FILES
                        ))
                    }
                }
            }
        }
    }
    
    private fun scanEmptyFolders(items: MutableList<JunkItem>) {
        val root = File("/sdcard")
        if (root.exists()) {
            root.walkTopDown().forEach { dir ->
                if (dir.isDirectory && dir.listFiles()?.isEmpty() == true) {
                    items.add(JunkItem(
                        path = dir.absolutePath,
                        name = dir.name,
                        size = 0,
                        category = JunkCategory.EMPTY_FOLDERS
                    ))
                }
            }
        }
    }
    
    private fun scanLargeFiles(items: MutableList<JunkItem>) {
        val threshold = 100 * 1024 * 1024L // 100MB
        val root = File("/sdcard")
        
        if (root.exists()) {
            root.walkTopDown().maxDepth(5).forEach { file ->
                if (file.isFile && file.length() >= threshold) {
                    items.add(JunkItem(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        category = JunkCategory.LARGE_FILES
                    ))
                }
            }
        }
    }
    
    private fun scanDuplicateFiles(items: MutableList<JunkItem>) {
        val hashMap = mutableMapOf<String, MutableList<String>>()
        val root = File("/sdcard/Download")
        
        if (root.exists()) {
            root.walkTopDown().maxDepth(3).forEach { file ->
                if (file.isFile && file.length() > 1024) {
                    // 使用大小+文件名作为简易哈希（实际应用中应使用 MD5）
                    val key = "${file.length()}_${file.name}"
                    hashMap.getOrPut(key) { mutableListOf() }.add(file.absolutePath)
                }
            }
            
            hashMap.values.filter { it.size > 1 }.forEach { duplicates ->
                duplicates.forEach { path ->
                    val file = File(path)
                    items.add(JunkItem(
                        path = path,
                        name = file.name,
                        size = file.length(),
                        category = JunkCategory.DUPLICATE_FILES
                    ))
                }
            }
        }
    }
    
    private fun scanLogFiles(items: MutableList<JunkItem>) {
        val root = File("/sdcard")
        if (root.exists()) {
            root.walkTopDown().maxDepth(4).forEach { file ->
                if (file.isFile && (file.name.endsWith(".log") || file.name == "debug.log" || file.name == "error.log")) {
                    items.add(JunkItem(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        category = JunkCategory.LOG_FILES
                    ))
                }
            }
        }
    }
    
    private fun scanThumbnailCache(context: Context, items: MutableList<JunkItem>) {
        val thumbnailDirs = listOf(
            "/sdcard/DCIM/.thumbnails",
            "/sdcard/Pictures/.thumbnails",
        )
        
        thumbnailDirs.forEach { dirPath ->
            val dir = File(dirPath)
            if (dir.exists()) {
                val size = getDirectorySize(dir)
                if (size > 0) {
                    items.add(JunkItem(
                        path = dir.absolutePath,
                        name = "缩略图缓存",
                        size = size,
                        category = JunkCategory.THUMBNAILS
                    ))
                }
            }
        }
    }
    
    private fun scanApkFiles(items: MutableList<JunkItem>) {
        val root = File("/sdcard/Download")
        if (root.exists()) {
            root.listFiles()?.filter { it.isFile && it.name.endsWith(".apk") }?.forEach { file ->
                items.add(JunkItem(
                    path = file.absolutePath,
                    name = file.name,
                    size = file.length(),
                    category = JunkCategory.APK_FILES
                ))
            }
        }
    }
    
    private fun scanOldDownloads(items: MutableList<JunkItem>) {
        val downloadDir = File("/sdcard/Download")
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        
        if (downloadDir.exists()) {
            downloadDir.listFiles()?.filter {
                it.isFile && it.lastModified() < thirtyDaysAgo && !it.name.endsWith(".apk")
            }?.forEach { file ->
                items.add(JunkItem(
                    path = file.absolutePath,
                    name = file.name,
                    size = file.length(),
                    category = JunkCategory.OLD_DOWNLOADS
                ))
            }
        }
    }
    
    private fun scanHiddenJunkFiles(items: MutableList<JunkItem>) {
        val root = File("/sdcard")
        if (root.exists()) {
            root.walkTopDown().maxDepth(3).forEach { file ->
                if (file.name == ".DS_Store" || file.name == "Thumbs.db" || file.name == "desktop.ini") {
                    items.add(JunkItem(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        category = JunkCategory.HIDDEN_JUNK
                    ))
                }
            }
        }
    }
    
    /**
     * 清理选中项
     */
    suspend fun cleanItems(items: List<JunkItem>): CleanResult {
        return withContext(Dispatchers.IO) {
            var successCount = 0
            var failCount = 0
            var freedSize = 0L
            
            items.forEach { item ->
                try {
                    val file = File(item.path)
                    if (file.exists()) {
                        if (file.isDirectory) {
                            val deleted = file.deleteRecursively()
                            if (deleted) {
                                freedSize += item.size
                                successCount++
                            } else {
                                failCount++
                            }
                        } else {
                            val deleted = file.delete()
                            if (deleted) {
                                freedSize += item.size
                                successCount++
                            } else {
                                failCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    failCount++
                }
            }
            
            CleanResult(
                successCount = successCount,
                failCount = failCount,
                freedSize = freedSize
            )
        }
    }
    
    /**
     * 搜索文件（全局搜索）
     */
    suspend fun searchFiles(
        query: String,
        basePath: String = "/sdcard",
        maxResults: Int = 100
    ): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<SearchResult>()
            val root = File(basePath)
            
            if (!root.exists()) return@withContext results
            
            root.walkTopDown().maxDepth(10).forEach { file ->
                if (results.size >= maxResults) return@forEach
                
                if (file.name.contains(query, ignoreCase = true)) {
                    results.add(SearchResult(
                        path = file.absolutePath,
                        name = file.name,
                        size = if (file.isFile) file.length() else getDirectorySize(file),
                        isDirectory = file.isDirectory,
                        lastModified = file.lastModified()
                    ))
                }
            }
            
            results.sortedByDescending { it.lastModified }
        }
    }
    
    /**
     * 查找大文件
     */
    suspend fun findLargeFiles(
        minSize: Long = 50 * 1024 * 1024,
        basePath: String = "/sdcard"
    ): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<SearchResult>()
            val root = File(basePath)
            
            if (!root.exists()) return@withContext results
            
            root.walkTopDown().maxDepth(8).forEach { file ->
                if (file.isFile && file.length() >= minSize) {
                    results.add(SearchResult(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        isDirectory = false,
                        lastModified = file.lastModified()
                    ))
                }
            }
            
            results.sortedByDescending { it.size }
        }
    }
    
    /**
     * 查找相似图片（按大小和名称）
     */
    suspend fun findSimilarImages(basePath: String = "/sdcard/DCIM"): List<SimilarGroup> {
        return withContext(Dispatchers.IO) {
            val groups = mutableMapOf<String, MutableList<SearchResult>>()
            val root = File(basePath)
            
            if (!root.exists()) return@withContext emptyList()
            
            root.walkTopDown().maxDepth(5).forEach { file ->
                if (file.isFile && file.extension.lowercase() in listOf("jpg", "jpeg", "png")) {
                    // 简易分组：按文件大小
                    val sizeGroup = (file.length() / 1024).toString()
                    groups.getOrPut(sizeGroup) { mutableListOf() }.add(
                        SearchResult(
                            path = file.absolutePath,
                            name = file.name,
                            size = file.length(),
                            isDirectory = false,
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
            
            groups.values.filter { it.size > 1 }.map { files ->
                SimilarGroup(files = files.sortedByDescending { it.size })
            }
        }
    }
    
    private fun getDirectorySize(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) return 0
        return dir.walkTopDown().sumOf { if (it.isFile) it.length() else 0 }
    }
}

// ==================== 数据类 ====================

enum class JunkCategory(val displayName: String, val icon: String) {
    APP_CACHE("应用缓存", "📦"),
    TEMP_FILES("临时文件", "📄"),
    EMPTY_FOLDERS("空文件夹", "📁"),
    LARGE_FILES("大文件", "💾"),
    DUPLICATE_FILES("重复文件", "📋"),
    LOG_FILES("日志文件", "📝"),
    THUMBNAILS("缩略图缓存", "🖼️"),
    APK_FILES("安装包", "🤖"),
    OLD_DOWNLOADS("旧下载", "📥"),
    HIDDEN_JUNK("隐藏垃圾", "🙈")
}

data class JunkItem(
    val path: String,
    val name: String,
    val size: Long,
    val category: JunkCategory,
    val isSelected: Boolean = false
)

data class JunkScanResult(
    val items: List<JunkItem>,
    val totalSize: Long,
    val totalFiles: Int,
    val categories: Map<JunkCategory, Long>
)

data class CleanResult(
    val successCount: Int,
    val failCount: Int,
    val freedSize: Long
)

data class SearchResult(
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long
)

data class SimilarGroup(
    val files: List<SearchResult>
)
