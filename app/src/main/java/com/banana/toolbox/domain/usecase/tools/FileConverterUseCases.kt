package com.banana.toolbox.domain.usecase.tools

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.nio.charset.Charset
import java.security.SecureRandom
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件格式转换用例
 */
@Singleton
class FileConverterUseCases @Inject constructor(
) {
    
    // ==================== 图片格式转换 ====================
    
    /**
     * 图片格式转换
     * 支持：JPG ↔ PNG ↔ WebP ↔ BMP
     */
    suspend fun convertImage(
        inputPath: String,
        outputPath: String,
        targetFormat: ImageFormat,
        quality: Int = 85
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeFile(inputPath)
                    ?: return@withContext Result.failure(Exception("无法解码图片"))
                
                val format = when (targetFormat) {
                    ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                    ImageFormat.WEBP -> Bitmap.CompressFormat.WEBP
                }
                
                val outputFile = if (outputPath.endsWith(targetFormat.extension)) {
                    File(outputPath)
                } else {
                    val baseName = inputPath.substringBeforeLast(".")
                    File("$baseName.${targetFormat.extension}")
                }
                
                FileOutputStream(outputFile).use { fos ->
                    bitmap.compress(format, quality, fos)
                }
                
                bitmap.recycle()
                Result.success(outputFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 图片压缩
     */
    suspend fun compressImage(
        inputPath: String,
        outputPath: String,
        maxWidth: Int = 1920,
        maxHeight: Int = 1080,
        quality: Int = 80
    ): Result<ImageCompressResult> {
        return withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(inputPath, options)
                
                val originalWidth = options.outWidth
                val originalHeight = options.outHeight
                val originalSize = File(inputPath).length()
                
                // 计算采样率
                val inSampleSize = calculateInSampleSize(originalWidth, originalHeight, maxWidth, maxHeight)
                
                val decodeOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    this.inSampleSize = inSampleSize
                }
                
                val bitmap = BitmapFactory.decodeFile(inputPath, decodeOptions)
                    ?: return@withContext Result.failure(Exception("无法解码图片"))
                
                // 缩放到目标尺寸
                val scaledBitmap = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                    val ratio = minOf(
                        maxWidth.toFloat() / bitmap.width,
                        maxHeight.toFloat() / bitmap.height
                    )
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                } else {
                    bitmap
                }
                
                val outputFile = File(outputPath)
                FileOutputStream(outputFile).use { fos ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                }
                
                val compressedSize = outputFile.length()
                val savedPercent = ((1 - compressedSize.toFloat() / originalSize) * 100).toInt()
                
                bitmap.recycle()
                scaledBitmap.recycle()
                
                Result.success(ImageCompressResult(
                    inputPath = inputPath,
                    outputPath = outputFile.absolutePath,
                    originalSize = originalSize,
                    compressedSize = compressedSize,
                    savedPercent = savedPercent,
                    originalWidth = originalWidth,
                    originalHeight = originalHeight,
                    newWidth = scaledBitmap.width,
                    newHeight = scaledBitmap.height
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 图片添加水印
     */
    suspend fun addWatermark(
        inputPath: String,
        outputPath: String,
        watermarkText: String,
        position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
        textSize: Float = 40f,
        color: Int = (Color.WHITE and 0x00FFFFFF) or (128 shl 24) // WHITE with 50% alpha
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeFile(inputPath)
                    ?: return@withContext Result.failure(Exception("无法解码图片"))
                
                val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutableBitmap)
                
                val paint = Paint().apply {
                    this.textSize = textSize
                    this.color = color
                    this.isAntiAlias = true
                    this.typeface = Typeface.DEFAULT_BOLD
                }
                
                val textWidth = paint.measureText(watermarkText)
                val textHeight = paint.fontMetrics.let { it.descent - it.ascent }
                
                val x = when (position) {
                    WatermarkPosition.TOP_LEFT -> 20f
                    WatermarkPosition.TOP_RIGHT -> mutableBitmap.width - textWidth - 20f
                    WatermarkPosition.BOTTOM_LEFT -> 20f
                    WatermarkPosition.BOTTOM_RIGHT -> mutableBitmap.width - textWidth - 20f
                    WatermarkPosition.CENTER -> (mutableBitmap.width - textWidth) / 2f
                }
                
                val y = when (position) {
                    WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> textHeight + 20f
                    WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> mutableBitmap.height - 20f
                    WatermarkPosition.CENTER -> (mutableBitmap.height + textHeight) / 2f
                }
                
                canvas.drawText(watermarkText, x, y, paint)
                
                val outputFile = File(outputPath)
                FileOutputStream(outputFile).use { fos ->
                    mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                
                bitmap.recycle()
                mutableBitmap.recycle()
                
                Result.success(outputFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 图片裁剪
     */
    suspend fun cropImage(
        inputPath: String,
        outputPath: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeFile(inputPath)
                    ?: return@withContext Result.failure(Exception("无法解码图片"))
                
                val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                
                val outputFile = File(outputPath)
                FileOutputStream(outputFile).use { fos ->
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                
                bitmap.recycle()
                croppedBitmap.recycle()
                
                Result.success(outputFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    
    // ==================== 音频格式转换 ====================
    
    /**
     * 获取音频文件信息
     */
    fun getAudioInfo(path: String): AudioInfo {
        val file = File(path)
        return AudioInfo(
            path = path,
            name = file.name,
            size = file.length(),
            extension = file.extension,
            lastModified = file.lastModified()
        )
    }
    
    // ==================== 视频格式转换 ====================
    
    /**
     * 获取视频文件信息
     */
    fun getVideoInfo(path: String): VideoInfo {
        val file = File(path)
        return VideoInfo(
            path = path,
            name = file.name,
            size = file.length(),
            extension = file.extension,
            lastModified = file.lastModified()
        )
    }
    
    // ==================== 文档格式转换 ====================
    
    /**
     * 文本文件编码转换
     */
    suspend fun convertTextEncoding(
        inputPath: String,
        outputPath: String,
        fromEncoding: String,
        toEncoding: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val content = File(inputPath).readText(Charset.forName(fromEncoding))
                File(outputPath).writeText(content, Charset.forName(toEncoding))
                Result.success(outputPath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 文本文件格式转换
     * CSV ↔ JSON ↔ XML ↔ Markdown
     */
    suspend fun convertTextFormat(
        inputPath: String,
        outputPath: String,
        targetFormat: TextFormat
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val content = File(inputPath).readText()
                val converted = when (targetFormat) {
                    TextFormat.JSON -> convertToJson(content)
                    TextFormat.CSV -> convertToCsv(content)
                    TextFormat.XML -> convertToXml(content)
                    TextFormat.MARKDOWN -> convertToMarkdown(content)
                    TextFormat.HTML -> convertToHtml(content)
                }
                
                val outputFile = if (outputPath.endsWith(targetFormat.extension)) {
                    File(outputPath)
                } else {
                    val baseName = inputPath.substringBeforeLast(".")
                    File("$baseName.${targetFormat.extension}")
                }
                
                outputFile.writeText(converted)
                Result.success(outputFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun convertToJson(content: String): String {
        // 简单的 CSV 转 JSON
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return "[]"
        
        val headers = lines[0].split(",").map { it.trim() }
        val jsonRows = lines.drop(1).map { line ->
            val values = line.split(",").map { it.trim() }
            headers.mapIndexed { index, header ->
                "\"$header\": \"${values.getOrElse(index) { "" }}\""
            }.joinToString(", ", "{ ", " }")
        }
        
        return "[\n${jsonRows.joinToString(",\n")}\n]"
    }
    
    private fun convertToCsv(content: String): String {
        // 简单的 JSON 转 CSV
        return try {
            val lines = content.trim().lines()
            if (lines.first() == "[") {
                // JSON 数组
                val items = content.trim().removeSurrounding("[", "]").split("},").map { 
                    if (it.endsWith("}")) it else "$it}"
                }
                val headers = mutableListOf<String>()
                val regex = Regex(""""(\w+)":\s*"([^"]*)"""")
                items.firstOrNull()?.let { regex.findAll(it).forEach { headers.add(it.groupValues[1]) } }
                
                val csvRows = mutableListOf(headers.joinToString(","))
                items.forEach { item ->
                    val values = headers.map { header ->
                        regex.find(item)?.let { 
                            if (it.groupValues[1] == header) it.groupValues[2] else ""
                        } ?: ""
                    }
                    csvRows.add(values.joinToString(","))
                }
                csvRows.joinToString("\n")
            } else {
                content
            }
        } catch (e: Exception) {
            content
        }
    }
    
    private fun convertToXml(content: String): String {
        val lines = content.lines().filter { it.isNotBlank() }
        return buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<root>")
            lines.forEach { line ->
                val parts = line.split(",", limit = 2)
                if (parts.size == 2) {
                    appendLine("  <item>")
                    appendLine("    <key>${escapeXml(parts[0].trim())}</key>")
                    appendLine("    <value>${escapeXml(parts[1].trim())}</value>")
                    appendLine("  </item>")
                }
            }
            appendLine("</root>")
        }
    }
    
    private fun convertToMarkdown(content: String): String {
        // 简单的文本转 Markdown 表格
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return content
        
        return buildString {
            appendLine("| ${lines[0].split(",").joinToString(" | ")} |")
            appendLine("| ${lines[0].split(",").joinToString(" | ") { "---" }} |")
            lines.drop(1).forEach { line ->
                appendLine("| ${line.split(",").joinToString(" | ")} |")
            }
        }
    }
    
    private fun convertToHtml(content: String): String {
        val lines = content.lines().filter { it.isNotBlank() }
        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html><head><meta charset=\"UTF-8\"><title>转换结果</title></head><body>")
            appendLine("<table border=\"1\">")
            lines.forEachIndexed { index, line ->
                val cells = line.split(",")
                if (index == 0) {
                    appendLine("<tr>${cells.joinToString("") { "<th>${it.trim()}</th>" }}</tr>")
                } else {
                    appendLine("<tr>${cells.joinToString("") { "<td>${it.trim()}</td>" }}</tr>")
                }
            }
            appendLine("</table></body></html>")
        }
    }
    
    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    }
}

// ==================== 数据类 ====================

enum class ImageFormat(val extension: String) {
    JPEG("jpg"),
    PNG("png"),
    WEBP("webp")
}

enum class TextFormat(val extension: String) {
    JSON("json"),
    CSV("csv"),
    XML("xml"),
    MARKDOWN("md"),
    HTML("html")
}

enum class WatermarkPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
}

data class ImageCompressResult(
    val inputPath: String,
    val outputPath: String,
    val originalSize: Long,
    val compressedSize: Long,
    val savedPercent: Int,
    val originalWidth: Int,
    val originalHeight: Int,
    val newWidth: Int,
    val newHeight: Int
) {
    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1073741824 -> "%.1f GB".format(bytes / 1073741824.0)
            bytes >= 1048576 -> "%.1f MB".format(bytes / 1048576.0)
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}

data class AudioInfo(
    val path: String,
    val name: String,
    val size: Long,
    val extension: String,
    val lastModified: Long
)

data class VideoInfo(
    val path: String,
    val name: String,
    val size: Long,
    val extension: String,
    val lastModified: Long
)
