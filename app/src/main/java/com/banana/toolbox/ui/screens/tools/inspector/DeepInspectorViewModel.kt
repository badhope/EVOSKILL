package com.banana.toolbox.ui.screens.tools.inspector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banana.toolbox.domain.usecase.tools.AutoStartApp
import com.banana.toolbox.domain.usecase.tools.BatteryHealth
import com.banana.toolbox.domain.usecase.tools.CpuInfo
import com.banana.toolbox.domain.usecase.tools.DeepInspectorUseCases
import com.banana.toolbox.domain.usecase.tools.MemoryInfo
import com.banana.toolbox.domain.usecase.tools.NetworkDetails
import com.banana.toolbox.domain.usecase.tools.ProcessInfo
import com.banana.toolbox.domain.usecase.tools.SensorInfo
import com.banana.toolbox.domain.usecase.tools.StorageDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 深层检测 ViewModel
 *
 * 封装 [DeepInspectorUseCases]，管理硬件信息、传感器、进程、自启动应用、网络详情的 UI 状态。
 * 所有数据采集在 IO 线程执行，避免阻塞主线程。
 */
@HiltViewModel
class DeepInspectorViewModel @Inject constructor(
    private val useCases: DeepInspectorUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeepInspectorUiState())
    val uiState: StateFlow<DeepInspectorUiState> = _uiState.asStateFlow()

    // ==================== Tab 切换 ====================

    fun setActiveTab(tab: InspectorTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // ==================== 数据加载 ====================

    /**
     * 加载硬件信息（CPU、内存、存储、电池）
     */
    fun loadHardwareInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingHardware = true, hardwareError = null) }
            try {
                val cpuInfo = useCases.getCpuInfo()
                val memoryInfo = useCases.getMemoryInfo()
                val storageDetails = useCases.getStorageDetails()
                val batteryHealth = useCases.getBatteryHealth()
                _uiState.update {
                    it.copy(
                        isLoadingHardware = false,
                        cpuInfo = cpuInfo,
                        memoryInfo = memoryInfo,
                        storageDetails = storageDetails,
                        batteryHealth = batteryHealth
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingHardware = false,
                        hardwareError = e.message ?: "加载硬件信息失败"
                    )
                }
            }
        }
    }

    /**
     * 加载传感器列表
     */
    fun loadSensors() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingSensors = true, sensorsError = null) }
            try {
                val sensors = useCases.getSensors()
                _uiState.update {
                    it.copy(
                        isLoadingSensors = false,
                        sensors = sensors
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingSensors = false,
                        sensorsError = e.message ?: "加载传感器信息失败"
                    )
                }
            }
        }
    }

    /**
     * 加载运行进程列表
     */
    fun loadProcesses() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingProcesses = true, processesError = null) }
            try {
                val processes = useCases.getRunningProcesses()
                _uiState.update {
                    it.copy(
                        isLoadingProcesses = false,
                        processes = processes
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingProcesses = false,
                        processesError = e.message ?: "加载进程信息失败"
                    )
                }
            }
        }
    }

    /**
     * 加载自启动应用列表
     */
    fun loadAutoStartApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingAutoStart = true, autoStartError = null) }
            try {
                val apps = useCases.getAutoStartApps()
                _uiState.update {
                    it.copy(
                        isLoadingAutoStart = false,
                        autoStartApps = apps
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingAutoStart = false,
                        autoStartError = e.message ?: "加载自启动应用失败"
                    )
                }
            }
        }
    }

    /**
     * 加载网络详情
     */
    fun loadNetworkDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingNetwork = true, networkError = null) }
            try {
                val network = useCases.getNetworkDetails()
                _uiState.update {
                    it.copy(
                        isLoadingNetwork = false,
                        networkDetails = network
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingNetwork = false,
                        networkError = e.message ?: "加载网络信息失败"
                    )
                }
            }
        }
    }

    // ==================== 进程排序 ====================

    /**
     * 设置进程排序方式
     */
    fun setProcessSortMode(sortMode: ProcessSortMode) {
        _uiState.update { state ->
            val sorted = sortProcesses(state.processes, sortMode)
            state.copy(processSortMode = sortMode, processes = sorted)
        }
    }

    /**
     * 根据排序方式对进程列表排序
     */
    private fun sortProcesses(
        processes: List<ProcessInfo>,
        sortMode: ProcessSortMode
    ): List<ProcessInfo> {
        return when (sortMode) {
            ProcessSortMode.MEMORY_DESC -> processes.sortedByDescending { it.memory }
            ProcessSortMode.MEMORY_ASC -> processes.sortedBy { it.memory }
            ProcessSortMode.PID_ASC -> processes.sortedBy { it.pid }
            ProcessSortMode.PID_DESC -> processes.sortedByDescending { it.pid }
            ProcessSortMode.NAME_ASC -> processes.sortedBy { it.processName.lowercase() }
            ProcessSortMode.NAME_DESC -> processes.sortedByDescending { it.processName.lowercase() }
        }
    }

    // ==================== 自启动管理 ====================

    /**
     * 切换自启动应用启用/禁用状态（仅更新本地 UI 状态）
     */
    fun toggleAutoStartApp(packageName: String) {
        _uiState.update { state ->
            val updated = state.autoStartApps.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isEnabled = !app.isEnabled)
                } else {
                    app
                }
            }
            state.copy(autoStartApps = updated)
        }
    }

    // ==================== 错误清除 ====================

    fun clearHardwareError() {
        _uiState.update { it.copy(hardwareError = null) }
    }

    fun clearSensorsError() {
        _uiState.update { it.copy(sensorsError = null) }
    }

    fun clearProcessesError() {
        _uiState.update { it.copy(processesError = null) }
    }

    fun clearAutoStartError() {
        _uiState.update { it.copy(autoStartError = null) }
    }

    fun clearNetworkError() {
        _uiState.update { it.copy(networkError = null) }
    }
}

// ==================== UI 状态 ====================

/**
 * 深层检测页面 UI 状态
 */
data class DeepInspectorUiState(
    // 通用
    val activeTab: InspectorTab = InspectorTab.HARDWARE,

    // 硬件信息
    val isLoadingHardware: Boolean = false,
    val cpuInfo: CpuInfo? = null,
    val memoryInfo: MemoryInfo? = null,
    val storageDetails: List<StorageDetail> = emptyList(),
    val batteryHealth: BatteryHealth? = null,
    val hardwareError: String? = null,

    // 传感器
    val isLoadingSensors: Boolean = false,
    val sensors: List<SensorInfo> = emptyList(),
    val sensorsError: String? = null,

    // 进程管理
    val isLoadingProcesses: Boolean = false,
    val processes: List<ProcessInfo> = emptyList(),
    val processSortMode: ProcessSortMode = ProcessSortMode.MEMORY_DESC,
    val processesError: String? = null,

    // 自启动管理
    val isLoadingAutoStart: Boolean = false,
    val autoStartApps: List<AutoStartApp> = emptyList(),
    val autoStartError: String? = null,

    // 网络详情
    val isLoadingNetwork: Boolean = false,
    val networkDetails: NetworkDetails? = null,
    val networkError: String? = null
)

// ==================== 枚举 ====================

/**
 * 深层检测 Tab 枚举
 */
enum class InspectorTab(val label: String) {
    HARDWARE("硬件信息"),
    SENSORS("传感器"),
    PROCESSES("进程管理"),
    AUTOSTART("自启动管理"),
    NETWORK("网络详情")
}

/**
 * 进程排序方式枚举
 */
enum class ProcessSortMode(val label: String) {
    MEMORY_DESC("内存降序"),
    MEMORY_ASC("内存升序"),
    PID_ASC("PID 升序"),
    PID_DESC("PID 降序"),
    NAME_ASC("名称 A-Z"),
    NAME_DESC("名称 Z-A")
}
