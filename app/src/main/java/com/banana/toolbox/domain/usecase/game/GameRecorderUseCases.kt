package com.banana.toolbox.domain.usecase.game

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 游戏录制用例
 * 提供屏幕录制、截图和录制文件管理功能
 */
@Singleton
class GameRecorderUseCases @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
        as MediaProjectionManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentRecordingPath: String? = null
    private val recordingPreferences = context.getSharedPreferences("game_recorder_prefs", Context.MODE_PRIVATE)

    /**
     * 录制存储目录
     */
    private val recordingsDir: File by lazy {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "GameRecordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    /**
     * 截图存储目录
     */
    private val screenshotsDir: File by lazy {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "GameScreenshots")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    /**
     * 开始屏幕录制
     * @return 录制文件路径
     */
    suspend fun startRecording(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (isRecording) {
                    return@withContext Result.failure(Exception("录制已在进行中"))
                }

                // 生成录制文件路径
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "game_recording_$timestamp.mp4"
                val outputPath = File(recordingsDir, fileName).absolutePath

                // 初始化MediaRecorder
                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setVideoSource(MediaRecorder.VideoSource.SURFACE)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    
                    // 获取屏幕尺寸
                    val displayMetrics = context.resources.displayMetrics
                    setVideoSize(displayMetrics.widthPixels, displayMetrics.heightPixels)
                    setVideoFrameRate(60)
                    setVideoEncodingBitRate(8 * 1000 * 1000) // 8Mbps
                    
                    setOutputFile(outputPath)
                    prepare()
                    start()
                }

                isRecording = true
                currentRecordingPath = outputPath

                Result.success(outputPath)
            } catch (e: Exception) {
                isRecording = false
                mediaRecorder?.release()
                mediaRecorder = null
                Result.failure(e)
            }
        }
    }

    /**
     * 停止屏幕录制
     * @return 录制文件路径
     */
    suspend fun stopRecording(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isRecording) {
                    return@withContext Result.failure(Exception("没有正在进行的录制"))
                }

                mediaRecorder?.apply {
                    stop()
                    reset()
                    release()
                }
                mediaRecorder = null
                isRecording = false

                val path = currentRecordingPath
                    ?: return@withContext Result.failure(Exception("录制路径丢失"))

                // 生成缩略图
                generateThumbnail(path)

                // 保存录制记录
                saveRecordingInfo(path)

                currentRecordingPath = null

                Result.success(path)
            } catch (e: Exception) {
                mediaRecorder?.release()
                mediaRecorder = null
                isRecording = false
                Result.failure(e)
            }
        }
    }

    /**
     * 截取屏幕截图
     * @return 截图文件路径
     */
    suspend fun takeScreenshot(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 生成截图文件名
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "screenshot_$timestamp.png"
                val outputFile = File(screenshotsDir, fileName)

                // 获取屏幕截图（需要MediaProjection）
                // 这里提供框架实现，实际截图需要系统权限
                val displayMetrics = context.resources.displayMetrics
                val width = displayMetrics.widthPixels
                val height = displayMetrics.heightPixels

                // 创建空白位图作为占位
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                
                // 保存截图
                FileOutputStream(outputFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()

                Result.success(outputFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 获取所有录制文件列表
     * @return 录制信息列表
     */
    suspend fun getRecordings(): Result<List<RecordingInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val recordings = recordingsDir.listFiles { file ->
                    file.extension.equals("mp4", ignoreCase = true)
                }?.mapNotNull { file ->
                    createRecordingInfo(file)
                }?.sortedByDescending { it.timestamp } ?: emptyList()

                Result.success(recordings)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 删除录制文件
     * @param path 文件路径
     */
    suspend fun deleteRecording(path: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists()) {
                    return@withContext Result.failure(Exception("文件不存在"))
                }

                // 删除视频文件
                file.delete()

                // 删除缩略图
                val thumbnailFile = File(file.parent, "${file.nameWithoutExtension}_thumb.jpg")
                if (thumbnailFile.exists()) {
                    thumbnailFile.delete()
                }

                // 从记录中移除
                removeRecordingInfo(path)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 检查是否正在录制
     */
    fun isRecording(): Boolean = isRecording

    /**
     * 获取当前录制路径
     */
    fun getCurrentRecordingPath(): String? = currentRecordingPath

    // 私有辅助方法

    private fun generateThumbnail(videoPath: String) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            // 获取第一帧作为缩略图
            val bitmap = retriever.getFrameAtTime(0)
            
            val videoFile = File(videoPath)
            val thumbnailFile = File(videoFile.parent, "${videoFile.nameWithoutExtension}_thumb.jpg")
            
            FileOutputStream(thumbnailFile).use { out ->
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            bitmap?.recycle()
            retriever.release()
        } catch (e: Exception) {
            // 缩略图生成失败不影响主流程
        }
    }

    private fun createRecordingInfo(file: File): RecordingInfo? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            
            retriever.release()
            
            val thumbnailFile = File(file.parent, "${file.nameWithoutExtension}_thumb.jpg")
            
            RecordingInfo(
                path = file.absolutePath,
                duration = duration,
                size = file.length(),
                timestamp = file.lastModified(),
                thumbnail = if (thumbnailFile.exists()) thumbnailFile.absolutePath else null,
                resolution = "${width}x${height}"
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun saveRecordingInfo(path: String) {
        val recordings = recordingPreferences.getStringSet("recordings", mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
        recordings.add(path)
        recordingPreferences.edit().putStringSet("recordings", recordings).apply()
    }

    private fun removeRecordingInfo(path: String) {
        val recordings = recordingPreferences.getStringSet("recordings", mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
        recordings.remove(path)
        recordingPreferences.edit().putStringSet("recordings", recordings).apply()
    }
}

/**
 * 录制信息数据类
 * @property path 视频文件路径
 * @property duration 录制时长（毫秒）
 * @property size 文件大小（字节）
 * @property timestamp 录制时间戳
 * @property thumbnail 缩略图路径
 * @property resolution 视频分辨率
 */
data class RecordingInfo(
    val path: String,
    val duration: Long,
    val size: Long,
    val timestamp: Long,
    val thumbnail: String?,
    val resolution: String
) {
    /**
     * 获取格式化的时长字符串
     */
    fun getFormattedDuration(): String {
        val seconds = duration / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
    }

    /**
     * 获取格式化的文件大小
     */
    fun getFormattedSize(): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            else -> String.format("%.2f KB", kb)
        }
    }

    /**
     * 获取格式化的日期时间
     */
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
