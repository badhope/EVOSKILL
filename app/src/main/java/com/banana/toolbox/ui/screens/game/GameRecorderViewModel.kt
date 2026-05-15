package com.banana.toolbox.ui.screens.game

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banana.toolbox.domain.usecase.game.GameRecorderUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 游戏录制ViewModel
 * 管理游戏录制界面的状态和业务逻辑
 */
@HiltViewModel
class GameRecorderViewModel @Inject constructor(
    private val gameRecorderUseCases: GameRecorderUseCases
) : ViewModel() {

    private val _uiState = mutableStateOf(GameRecorderUiState())
    val uiState: State<GameRecorderUiState> = _uiState

    private val _events = MutableSharedFlow<GameRecorderEvent>()
    val events: SharedFlow<GameRecorderEvent> = _events.asSharedFlow()

    private var recordingStartTime: Long = 0
    private var recordingJob: kotlinx.coroutines.Job? = null

    init {
        loadRecordings()
        loadScreenshots()
    }

    /**
     * 加载录制列表
     */
    private fun loadRecordings() {
        viewModelScope.launch {
            // 模拟加载录制列表
            val sampleRecordings = getSampleRecordings()
            _uiState.value = _uiState.value.copy(recordings = sampleRecordings)
        }
    }

    /**
     * 加载截图列表
     */
    private fun loadScreenshots() {
        viewModelScope.launch {
            // 模拟加载截图列表
            val sampleScreenshots = getSampleScreenshots()
            _uiState.value = _uiState.value.copy(screenshots = sampleScreenshots)
        }
    }

    /**
     * 开始录制倒计时
     */
    fun startRecordingCountdown() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(countdown = 3)

            // 倒计时 3-2-1
            for (i in 3 downTo 1) {
                _uiState.value = _uiState.value.copy(countdown = i)
                delay(1000)
            }

            _uiState.value = _uiState.value.copy(countdown = 0)
            startRecording()
        }
    }

    /**
     * 开始录制
     */
    private fun startRecording() {
        recordingStartTime = System.currentTimeMillis()

        _uiState.value = _uiState.value.copy(
            isRecording = true,
            recordingDuration = 0
        )

        viewModelScope.launch {
            _events.emit(GameRecorderEvent.ShowSnackbar("开始录制"))
            _events.emit(GameRecorderEvent.StartRecording)
        }

        // 启动计时器
        recordingJob = viewModelScope.launch {
            while (_uiState.value.isRecording) {
                delay(1000)
                val duration = System.currentTimeMillis() - recordingStartTime
                _uiState.value = _uiState.value.copy(recordingDuration = duration)
            }
        }
    }

    /**
     * 停止录制
     */
    fun stopRecording() {
        recordingJob?.cancel()

        val duration = System.currentTimeMillis() - recordingStartTime
        val newRecording = createRecordingItem(duration)

        val updatedRecordings = listOf(newRecording) + _uiState.value.recordings

        _uiState.value = _uiState.value.copy(
            isRecording = false,
            recordingDuration = 0,
            recordings = updatedRecordings
        )

        viewModelScope.launch {
            _events.emit(GameRecorderEvent.ShowSnackbar("录制已保存"))
            _events.emit(GameRecorderEvent.StopRecording)
        }
    }

    /**
     * 播放录制
     */
    fun playRecording(recordingId: String) {
        viewModelScope.launch {
            _events.emit(GameRecorderEvent.ShowSnackbar("正在打开播放器..."))
        }
    }

    /**
     * 分享录制
     */
    fun shareRecording(recordingId: String) {
        viewModelScope.launch {
            _events.emit(GameRecorderEvent.ShowSnackbar("准备分享..."))
        }
    }

    /**
     * 删除录制
     */
    fun deleteRecording(recordingId: String) {
        val updatedRecordings = _uiState.value.recordings.filter { it.id != recordingId }
        _uiState.value = _uiState.value.copy(recordings = updatedRecordings)

        viewModelScope.launch {
            _events.emit(GameRecorderEvent.ShowSnackbar("录制已删除"))
        }
    }

    /**
     * 查看截图
     */
    fun viewScreenshot(screenshotId: String) {
        viewModelScope.launch {
            _events.emit(GameRecorderEvent.ShowSnackbar("正在打开图片..."))
        }
    }

    /**
     * 分享截图
     */
    fun shareScreenshot(screenshotId: String) {
        viewModelScope.launch {
            _events.emit(GameRecorderEvent.ShowSnackbar("准备分享..."))
        }
    }

    /**
     * 删除截图
     */
    fun deleteScreenshot(screenshotId: String) {
        val updatedScreenshots = _uiState.value.screenshots.filter { it.id != screenshotId }
        _uiState.value = _uiState.value.copy(screenshots = updatedScreenshots)

        viewModelScope.launch {
            _events.emit(GameRecorderEvent.ShowSnackbar("截图已删除"))
        }
    }

    /**
     * 截取屏幕
     */
    fun takeScreenshot() {
        viewModelScope.launch {
            val newScreenshot = createScreenshotItem()
            val updatedScreenshots = listOf(newScreenshot) + _uiState.value.screenshots

            _uiState.value = _uiState.value.copy(screenshots = updatedScreenshots)
            _events.emit(GameRecorderEvent.ShowSnackbar("截图已保存"))
        }
    }

    // ============== 私有辅助方法 ==============

    private fun createRecordingItem(duration: Long): RecordingItem {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val durationFormat = SimpleDateFormat("mm:ss", Locale.getDefault())

        return RecordingItem(
            id = UUID.randomUUID().toString(),
            name = "录制_${dateFormat.format(Date())}",
            gameName = "王者荣耀",
            duration = durationFormat.format(Date(duration)),
            resolution = "1920x1080",
            fileSize = formatFileSize((duration / 1000) * 5 * 1024 * 1024), // 假设每秒5MB
            createTime = dateFormat.format(Date()),
            thumbnailPath = null
        )
    }

    private fun createScreenshotItem(): ScreenshotItem {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        return ScreenshotItem(
            id = UUID.randomUUID().toString(),
            gameName = "王者荣耀",
            createTime = timeFormat.format(Date()),
            resolution = "1920x1080",
            fileSize = formatFileSize(2 * 1024 * 1024), // 假设2MB
            path = "/storage/screenshots/${UUID.randomUUID()}.jpg"
        )
    }

    private fun formatFileSize(bytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            bytes >= gb -> "%.1f GB".format(bytes / gb)
            bytes >= mb -> "%.1f MB".format(bytes / mb)
            bytes >= kb -> "%.1f KB".format(bytes / kb)
            else -> "$bytes B"
        }
    }

    private fun getSampleRecordings(): List<RecordingItem> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        return listOf(
            RecordingItem(
                id = "1",
                name = "五杀时刻",
                gameName = "王者荣耀",
                duration = "02:35",
                resolution = "1920x1080",
                fileSize = "156 MB",
                createTime = dateFormat.format(Date(System.currentTimeMillis() - 86400000))
            ),
            RecordingItem(
                id = "2",
                name = "吃鸡精彩操作",
                gameName = "和平精英",
                duration = "05:12",
                resolution = "1920x1080",
                fileSize = "312 MB",
                createTime = dateFormat.format(Date(System.currentTimeMillis() - 172800000))
            ),
            RecordingItem(
                id = "3",
                name = "原神风景",
                gameName = "原神",
                duration = "01:45",
                resolution = "2560x1440",
                fileSize = "245 MB",
                createTime = dateFormat.format(Date(System.currentTimeMillis() - 259200000))
            )
        )
    }

    private fun getSampleScreenshots(): List<ScreenshotItem> {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        return listOf(
            ScreenshotItem(
                id = "1",
                gameName = "王者荣耀",
                createTime = timeFormat.format(Date(System.currentTimeMillis() - 3600000)),
                resolution = "1920x1080",
                fileSize = "2.1 MB",
                path = "/storage/screenshots/1.jpg"
            ),
            ScreenshotItem(
                id = "2",
                gameName = "王者荣耀",
                createTime = timeFormat.format(Date(System.currentTimeMillis() - 7200000)),
                resolution = "1920x1080",
                fileSize = "1.8 MB",
                path = "/storage/screenshots/2.jpg"
            ),
            ScreenshotItem(
                id = "3",
                gameName = "和平精英",
                createTime = timeFormat.format(Date(System.currentTimeMillis() - 10800000)),
                resolution = "1920x1080",
                fileSize = "2.3 MB",
                path = "/storage/screenshots/3.jpg"
            ),
            ScreenshotItem(
                id = "4",
                gameName = "原神",
                createTime = timeFormat.format(Date(System.currentTimeMillis() - 14400000)),
                resolution = "2560x1440",
                fileSize = "3.5 MB",
                path = "/storage/screenshots/4.jpg"
            ),
            ScreenshotItem(
                id = "5",
                gameName = "原神",
                createTime = timeFormat.format(Date(System.currentTimeMillis() - 18000000)),
                resolution = "2560x1440",
                fileSize = "3.2 MB",
                path = "/storage/screenshots/5.jpg"
            ),
            ScreenshotItem(
                id = "6",
                gameName = "英雄联盟手游",
                createTime = timeFormat.format(Date(System.currentTimeMillis() - 21600000)),
                resolution = "1920x1080",
                fileSize = "1.9 MB",
                path = "/storage/screenshots/6.jpg"
            )
        )
    }
}

/**
 * 游戏录制UI状态
 */
data class GameRecorderUiState(
    val isRecording: Boolean = false,
    val recordingDuration: Long = 0,
    val countdown: Int = 0,
    val recordings: List<RecordingItem> = emptyList(),
    val screenshots: List<ScreenshotItem> = emptyList()
)

/**
 * 游戏录制事件
 */
sealed class GameRecorderEvent {
    data class ShowSnackbar(val message: String) : GameRecorderEvent()
    data object StartRecording : GameRecorderEvent()
    data object StopRecording : GameRecorderEvent()
}
