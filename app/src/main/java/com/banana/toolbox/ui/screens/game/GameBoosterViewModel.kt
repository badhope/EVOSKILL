package com.banana.toolbox.ui.screens.game

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banana.toolbox.domain.usecase.game.GameBoosterUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import javax.inject.Inject

/**
 * 游戏加速ViewModel
 * 管理游戏加速界面的状态和业务逻辑
 */
@HiltViewModel
class GameBoosterViewModel @Inject constructor(
    private val gameBoosterUseCases: GameBoosterUseCases
) : ViewModel() {

    private val _uiState = mutableStateOf(GameBoosterUiState())
    val uiState: State<GameBoosterUiState> = _uiState

    private val _events = MutableSharedFlow<GameBoosterEvent>()
    val events: SharedFlow<GameBoosterEvent> = _events.asSharedFlow()

    init {
        // 启动实时状态监控
        startMonitoring()
    }

    /**
     * 开始监控实时状态
     */
    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                updateRealTimeStats()
                delay(2000) // 每2秒更新一次
            }
        }
    }

    /**
     * 更新实时状态
     */
    private fun updateRealTimeStats() {
        // 获取系统状态
        val ramUsage = getCurrentRamUsage()
        val cpuTemp = getCurrentCpuTemp()
        val networkLatency = getNetworkLatency()

        _uiState.value = _uiState.value.copy(
            ramUsage = ramUsage,
            cpuTemp = cpuTemp,
            networkLatency = networkLatency
        )
    }

    /**
     * 切换加速状态
     */
    fun toggleBoost() {
        val currentState = _uiState.value

        if (currentState.isBoosting) {
            // 停止加速
            stopBoost()
        } else {
            // 开始加速
            startBoost()
        }
    }

    /**
     * 开始加速
     */
    private fun startBoost() {
        viewModelScope.launch {
            // 保存优化前状态
            val beforeRam = _uiState.value.ramUsage
            val beforeTemp = _uiState.value.cpuTemp

            _uiState.value = _uiState.value.copy(
                isBoosting = true,
                beforeRam = beforeRam,
                beforeTemp = beforeTemp,
                showComparison = false
            )

            _events.emit(GameBoosterEvent.ShowSnackbar("正在优化游戏性能..."))

            // 模拟加速进度
            var progress = 0f
            while (progress < 1f) {
                delay(50)
                progress += 0.02f
                _uiState.value = _uiState.value.copy(
                    boostProgress = progress.coerceIn(0f, 1f)
                )
            }

            // 执行各项优化
            optimizeMemory()
            optimizeCpu()
            optimizeNetwork()

            // 显示优化结果
            val afterRam = (beforeRam * 0.6).toInt().coerceAtLeast(20)
            val afterTemp = beforeTemp * 0.85f

            _uiState.value = _uiState.value.copy(
                isBoosting = true,
                boostProgress = 1f,
                afterRam = afterRam,
                afterTemp = afterTemp,
                showComparison = true,
                memoryOptimized = true,
                cpuOptimized = true,
                networkOptimized = true
            )

            _events.emit(GameBoosterEvent.ShowSnackbar("游戏加速完成！"))
        }
    }

    /**
     * 停止加速
     */
    private fun stopBoost() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBoosting = false,
                boostProgress = 0f,
                showComparison = false,
                memoryOptimized = false,
                cpuOptimized = false,
                networkOptimized = false
            )

            _events.emit(GameBoosterEvent.ShowSnackbar("已停止加速"))
        }
    }

    /**
     * 优化内存
     */
    fun optimizeMemory() {
        viewModelScope.launch {
            _events.emit(GameBoosterEvent.ShowSnackbar("正在清理内存..."))

            // 模拟内存清理
            delay(800)

            val currentRam = _uiState.value.ramUsage
            val optimizedRam = (currentRam * 0.7).toInt().coerceAtLeast(25)

            _uiState.value = _uiState.value.copy(
                ramUsage = optimizedRam,
                memoryOptimized = true
            )

            _events.emit(GameBoosterEvent.ShowSnackbar("内存清理完成，释放 ${currentRam - optimizedRam}% 内存"))
        }
    }

    /**
     * 优化CPU
     */
    fun optimizeCpu() {
        viewModelScope.launch {
            _events.emit(GameBoosterEvent.ShowSnackbar("正在优化CPU..."))

            // 模拟CPU优化
            delay(800)

            val currentTemp = _uiState.value.cpuTemp
            val optimizedTemp = currentTemp * 0.9f

            _uiState.value = _uiState.value.copy(
                cpuTemp = optimizedTemp,
                cpuOptimized = true
            )

            _events.emit(GameBoosterEvent.ShowSnackbar("CPU性能模式已开启"))
        }
    }

    /**
     * 优化网络
     */
    fun optimizeNetwork() {
        viewModelScope.launch {
            _events.emit(GameBoosterEvent.ShowSnackbar("正在优化网络..."))

            // 模拟网络优化
            delay(1000)

            val currentLatency = _uiState.value.networkLatency
            val optimizedLatency = (currentLatency * 0.6).toInt().coerceAtLeast(20)

            _uiState.value = _uiState.value.copy(
                networkLatency = optimizedLatency,
                networkOptimized = true
            )

            _events.emit(GameBoosterEvent.ShowSnackbar("网络优化完成，延迟降低 ${currentLatency - optimizedLatency}ms"))
        }
    }

    /**
     * 切换防打扰模式
     */
    fun toggleDoNotDisturb() {
        val newState = !_uiState.value.doNotDisturb
        _uiState.value = _uiState.value.copy(doNotDisturb = newState)

        viewModelScope.launch {
            _events.emit(
                GameBoosterEvent.ShowSnackbar(
                    if (newState) "防打扰模式已开启" else "防打扰模式已关闭"
                )
            )
        }
    }

    /**
     * 切换游戏模式
     */
    fun toggleGameMode() {
        val newState = !_uiState.value.gameModeEnabled
        _uiState.value = _uiState.value.copy(gameModeEnabled = newState)

        viewModelScope.launch {
            if (newState) {
                _events.emit(GameBoosterEvent.ShowSnackbar("游戏模式已开启"))
                // 自动开始加速
                if (!_uiState.value.isBoosting) {
                    startBoost()
                }
            } else {
                _events.emit(GameBoosterEvent.ShowSnackbar("游戏模式已关闭"))
                if (_uiState.value.isBoosting) {
                    stopBoost()
                }
            }
        }
    }

    // ============== 私有辅助方法 ==============

    private fun getCurrentRamUsage(): Int {
        // 实际实现中应该从系统获取
        return (45..85).random()
    }

    private fun getCurrentCpuTemp(): Float {
        // 实际实现中应该从系统获取
        return Random.nextDouble(35.0, 65.0).toFloat()
    }

    private fun getNetworkLatency(): Int {
        // 实际实现中应该测试网络延迟
        return (30..120).random()
    }
}

/**
 * 游戏加速UI状态
 */
data class GameBoosterUiState(
    val isBoosting: Boolean = false,
    val boostProgress: Float = 0f,
    val ramUsage: Int = 65,
    val cpuTemp: Float = 48f,
    val networkLatency: Int = 65,
    val memoryOptimized: Boolean = false,
    val cpuOptimized: Boolean = false,
    val networkOptimized: Boolean = false,
    val doNotDisturb: Boolean = false,
    val gameModeEnabled: Boolean = false,
    val showComparison: Boolean = false,
    val beforeRam: Int = 0,
    val afterRam: Int = 0,
    val beforeTemp: Float = 0f,
    val afterTemp: Float = 0f
)

/**
 * 游戏加速事件
 */
sealed class GameBoosterEvent {
    data class ShowSnackbar(val message: String) : GameBoosterEvent()
}
