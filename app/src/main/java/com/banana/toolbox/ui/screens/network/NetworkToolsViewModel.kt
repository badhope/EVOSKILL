package com.banana.toolbox.ui.screens.network

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banana.toolbox.domain.usecase.network.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 网络工具 ViewModel
 */
@HiltViewModel
class NetworkToolsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkUseCases: NetworkUseCases
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NetworkToolsUiState())
    val uiState: StateFlow<NetworkToolsUiState> = _uiState.asStateFlow()
    
    init {
        loadNetworkInfo()
    }
    
    /**
     * 加载网络信息
     */
    fun loadNetworkInfo() {
        viewModelScope.launch {
            val info = networkUseCases.getNetworkInfo(context)
            _uiState.update { it.copy(networkInfo = info) }
        }
    }
    
    /**
     * Ping 测试
     */
    fun ping(host: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPinging = true, pingResult = null) }
            
            val result = networkUseCases.ping(host)
            _uiState.update { 
                it.copy(
                    isPinging = false,
                    pingResult = result,
                    error = null
                )
            }
        }
    }
    
    /**
     * DNS 查询
     */
    fun dnsLookup(domain: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDnsLookingUp = true, dnsResult = null) }
            
            val result = networkUseCases.dnsLookup(domain)
            _uiState.update { 
                it.copy(
                    isDnsLookingUp = false,
                    dnsResult = result,
                    error = if (!result.success) result.error else null
                )
            }
        }
    }
    
    /**
     * 端口扫描
     */
    fun scanPorts(host: String, ports: List<Int>? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningPorts = true, portResults = emptyList()) }
            
            val results = if (ports != null) {
                networkUseCases.scanPorts(host, ports)
            } else {
                networkUseCases.scanCommonPorts(host)
            }
            
            _uiState.update { 
                it.copy(
                    isScanningPorts = false,
                    portResults = results.sortedBy { r -> r.port },
                    error = null
                )
            }
        }
    }
    
    /**
     * 局域网扫描
     */
    fun scanLocalNetwork() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningNetwork = true, devices = emptyList()) }
            
            val devices = networkUseCases.scanLocalNetwork(context)
            _uiState.update { 
                it.copy(
                    isScanningNetwork = false,
                    devices = devices,
                    error = null
                )
            }
        }
    }
    
    /**
     * 网速测试
     */
    fun runSpeedTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSpeedTesting = true, speedTestResult = null) }
            
            val result = networkUseCases.speedTest()
            _uiState.update { 
                it.copy(
                    isSpeedTesting = false,
                    speedTestResult = result,
                    error = if (!result.success) result.error else null
                )
            }
        }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI 状态
 */
data class NetworkToolsUiState(
    val networkInfo: NetworkInfo? = null,
    val isPinging: Boolean = false,
    val pingResult: PingResult? = null,
    val isDnsLookingUp: Boolean = false,
    val dnsResult: DnsResult? = null,
    val isScanningPorts: Boolean = false,
    val portResults: List<PortResult> = emptyList(),
    val isScanningNetwork: Boolean = false,
    val devices: List<DeviceInfo> = emptyList(),
    val isSpeedTesting: Boolean = false,
    val speedTestResult: SpeedTestResult? = null,
    val error: String? = null
)
