package com.banana.toolbox.ui.screens.game

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banana.toolbox.domain.usecase.game.GameAssistantUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 游戏助手ViewModel
 * 管理游戏助手界面的状态和业务逻辑
 */
@HiltViewModel
class GameAssistantViewModel @Inject constructor(
    private val gameAssistantUseCases: GameAssistantUseCases
) : ViewModel() {

    private val _uiState = mutableStateOf(GameAssistantUiState())
    val uiState: State<GameAssistantUiState> = _uiState

    private val _events = MutableSharedFlow<GameAssistantEvent>()
    val events: SharedFlow<GameAssistantEvent> = _events.asSharedFlow()

    init {
        loadQuickActions()
        loadRecordedMacros()
    }

    /**
     * 加载快捷操作
     */
    private fun loadQuickActions() {
        val actions = listOf(
            QuickAction(
                id = "1",
                name = "一键加速",
                icon = Icons.Default.Bolt,
                color = Color(0xFFFF9800)
            ),
            QuickAction(
                id = "2",
                name = "截图",
                icon = Icons.Default.CameraAlt,
                color = Color(0xFF2196F3)
            ),
            QuickAction(
                id = "3",
                name = "录屏",
                icon = Icons.Default.Brush,
                color = Color(0xFFE91E63)
            ),
            QuickAction(
                id = "4",
                name = "静音",
                icon = Icons.Default.VolumeUp,
                color = Color(0xFF9C27B0)
            ),
            QuickAction(
                id = "5",
                name = "防打扰",
                icon = Icons.Default.NotificationsOff,
                color = Color(0xFF607D8B)
            ),
            QuickAction(
                id = "6",
                name = "手电筒",
                icon = Icons.Default.FlashOn,
                color = Color(0xFFFFEB3B)
            ),
            QuickAction(
                id = "7",
                name = "计时器",
                icon = Icons.Default.Timer,
                color = Color(0xFF4CAF50)
            ),
            QuickAction(
                id = "8",
                name = "旋转锁定",
                icon = Icons.Default.ScreenRotation,
                color = Color(0xFF795548)
            )
        )

        _uiState.value = _uiState.value.copy(quickActions = actions)
    }

    /**
     * 加载已录制的宏
     */
    private fun loadRecordedMacros() {
        val macros = listOf(
            MacroItem(
                id = "1",
                name = "自动刷副本",
                actionCount = 12,
                duration = "02:35"
            ),
            MacroItem(
                id = "2",
                name = "一键连招",
                actionCount = 8,
                duration = "01:20"
            ),
            MacroItem(
                id = "3",
                name = "自动签到",
                actionCount = 5,
                duration = "00:45"
            )
        )

        _uiState.value = _uiState.value.copy(recordedMacros = macros)
    }

    /**
     * 切换悬浮窗
     */
    fun toggleFloatingWindow() {
        val newState = !_uiState.value.floatingWindowEnabled
        _uiState.value = _uiState.value.copy(floatingWindowEnabled = newState)

        viewModelScope.launch {
            _events.emit(
                GameAssistantEvent.ShowSnackbar(
                    if (newState) "游戏悬浮窗已开启" else "游戏悬浮窗已关闭"
                )
            )
        }
    }

    /**
     * 执行快捷操作
     */
    fun executeQuickAction(action: QuickAction) {
        viewModelScope.launch {
            when (action.id) {
                "1" -> _events.emit(GameAssistantEvent.ShowSnackbar("正在加速..."))
                "2" -> _events.emit(GameAssistantEvent.ShowSnackbar("截图已保存"))
                "3" -> _events.emit(GameAssistantEvent.ShowSnackbar("开始录屏"))
                "4" -> {
                    val newActions = _uiState.value.quickActions.map {
                        if (it.id == action.id) {
                            it.copy(
                                isActive = !it.isActive,
                                name = if (!it.isActive) "取消静音" else "静音"
                            )
                        } else it
                    }
                    _uiState.value = _uiState.value.copy(quickActions = newActions)
                    _events.emit(GameAssistantEvent.ShowSnackbar(if (!action.isActive) "已静音" else "已取消静音"))
                }
                "5" -> {
                    val newActions = _uiState.value.quickActions.map {
                        if (it.id == action.id) it.copy(isActive = !it.isActive) else it
                    }
                    _uiState.value = _uiState.value.copy(quickActions = newActions)
                    _events.emit(GameAssistantEvent.ShowSnackbar(if (!action.isActive) "防打扰已开启" else "防打扰已关闭"))
                }
                "6" -> _events.emit(GameAssistantEvent.ShowSnackbar("手电筒已开启"))
                "7" -> _events.emit(GameAssistantEvent.ShowSnackbar("计时器已启动"))
                "8" -> {
                    val newActions = _uiState.value.quickActions.map {
                        if (it.id == action.id) it.copy(isActive = !it.isActive) else it
                    }
                    _uiState.value = _uiState.value.copy(quickActions = newActions)
                    _events.emit(GameAssistantEvent.ShowSnackbar(if (!action.isActive) "屏幕旋转已锁定" else "屏幕旋转已解锁"))
                }
                else -> _events.emit(GameAssistantEvent.ShowSnackbar("执行: ${action.name}"))
            }
        }
    }

    /**
     * 开始宏录制
     */
    fun startMacroRecording() {
        _uiState.value = _uiState.value.copy(isRecordingMacro = true)

        viewModelScope.launch {
            _events.emit(GameAssistantEvent.ShowSnackbar("开始录制宏，请执行操作序列"))
        }
    }

    /**
     * 停止宏录制
     */
    fun stopMacroRecording() {
        _uiState.value = _uiState.value.copy(isRecordingMacro = false)

        // 创建新宏
        val newMacro = MacroItem(
            id = UUID.randomUUID().toString(),
            name = "宏 ${_uiState.value.recordedMacros.size + 1}",
            actionCount = (5..15).random(),
            duration = "${(1..3).random()}:${(10..59).random()}"
        )

        val updatedMacros = listOf(newMacro) + _uiState.value.recordedMacros

        _uiState.value = _uiState.value.copy(
            isRecordingMacro = false,
            recordedMacros = updatedMacros
        )

        viewModelScope.launch {
            _events.emit(GameAssistantEvent.ShowSnackbar("宏录制完成: ${newMacro.name}"))
        }
    }

    /**
     * 播放宏
     */
    fun playMacro(macroId: String) {
        val macro = _uiState.value.recordedMacros.find { it.id == macroId }
        macro?.let {
            viewModelScope.launch {
                _events.emit(GameAssistantEvent.ShowSnackbar("正在播放: ${it.name}"))
                delay(1000)
                _events.emit(GameAssistantEvent.ShowSnackbar("宏执行完成"))
            }
        }
    }

    /**
     * 删除宏
     */
    fun deleteMacro(macroId: String) {
        val updatedMacros = _uiState.value.recordedMacros.filter { it.id != macroId }
        _uiState.value = _uiState.value.copy(recordedMacros = updatedMacros)

        viewModelScope.launch {
            _events.emit(GameAssistantEvent.ShowSnackbar("宏已删除"))
        }
    }

    /**
     * 切换性能监控
     */
    fun togglePerformanceMonitor() {
        val newState = !_uiState.value.performanceMonitorEnabled
        _uiState.value = _uiState.value.copy(performanceMonitorEnabled = newState)

        viewModelScope.launch {
            _events.emit(
                GameAssistantEvent.ShowSnackbar(
                    if (newState) "性能监控已开启" else "性能监控已关闭"
                )
            )
        }
    }

    /**
     * 切换FPS显示
     */
    fun toggleFpsDisplay() {
        val newState = !_uiState.value.showFps
        _uiState.value = _uiState.value.copy(showFps = newState)
    }

    /**
     * 切换CPU温度显示
     */
    fun toggleCpuTempDisplay() {
        val newState = !_uiState.value.showCpuTemp
        _uiState.value = _uiState.value.copy(showCpuTemp = newState)
    }

    /**
     * 切换内存使用显示
     */
    fun toggleRamUsageDisplay() {
        val newState = !_uiState.value.showRamUsage
        _uiState.value = _uiState.value.copy(showRamUsage = newState)
    }

    /**
     * 改变监控位置
     */
    fun changeMonitorPosition(position: MonitorPosition) {
        _uiState.value = _uiState.value.copy(monitorPosition = position)

        viewModelScope.launch {
            val positionName = when (position) {
                MonitorPosition.TOP_LEFT -> "左上"
                MonitorPosition.TOP_RIGHT -> "右上"
                MonitorPosition.BOTTOM_LEFT -> "左下"
                MonitorPosition.BOTTOM_RIGHT -> "右下"
            }
            _events.emit(GameAssistantEvent.ShowSnackbar("监控位置已切换至: $positionName"))
        }
    }
}

/**
 * 游戏助手UI状态
 */
data class GameAssistantUiState(
    val floatingWindowEnabled: Boolean = false,
    val quickActions: List<QuickAction> = emptyList(),
    val isRecordingMacro: Boolean = false,
    val recordedMacros: List<MacroItem> = emptyList(),
    val performanceMonitorEnabled: Boolean = false,
    val showFps: Boolean = true,
    val showCpuTemp: Boolean = true,
    val showRamUsage: Boolean = true,
    val monitorPosition: MonitorPosition = MonitorPosition.TOP_LEFT
)

/**
 * 游戏助手事件
 */
sealed class GameAssistantEvent {
    data class ShowSnackbar(val message: String) : GameAssistantEvent()
}
