package com.banana.toolbox.ui.screens.tools.inspector

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 深层检测页面
 *
 * 提供设备深层信息查看功能，包含硬件信息、传感器、进程管理、自启动管理、网络详情五大模块。
 * 使用 TabRow 切换不同信息类别。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepInspectorScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeepInspectorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 错误提示
    LaunchedEffect(uiState.hardwareError) {
        uiState.hardwareError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearHardwareError()
        }
    }
    LaunchedEffect(uiState.sensorsError) {
        uiState.sensorsError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSensorsError()
        }
    }
    LaunchedEffect(uiState.processesError) {
        uiState.processesError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearProcessesError()
        }
    }
    LaunchedEffect(uiState.autoStartError) {
        uiState.autoStartError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearAutoStartError()
        }
    }
    LaunchedEffect(uiState.networkError) {
        uiState.networkError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearNetworkError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("深层检测") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab 切换栏
            TabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                InspectorTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = uiState.activeTab.ordinal == index,
                        onClick = { viewModel.setActiveTab(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    InspectorTab.HARDWARE -> Icons.Default.Memory
                                    InspectorTab.SENSORS -> Icons.Default.Sensors
                                    InspectorTab.PROCESSES -> Icons.Default.Settings
                                    InspectorTab.AUTOSTART -> Icons.Default.Terminate
                                    InspectorTab.NETWORK -> Icons.Default.Router
                                },
                                contentDescription = tab.label
                            )
                        }
                    )
                }
            }

            // Tab 内容区域
            when (uiState.activeTab) {
                InspectorTab.HARDWARE -> HardwareTab(viewModel = viewModel, uiState = uiState)
                InspectorTab.SENSORS -> SensorsTab(viewModel = viewModel, uiState = uiState)
                InspectorTab.PROCESSES -> ProcessesTab(viewModel = viewModel, uiState = uiState)
                InspectorTab.AUTOSTART -> AutoStartTab(viewModel = viewModel, uiState = uiState)
                InspectorTab.NETWORK -> NetworkTab(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}

// ==================== 硬件信息 Tab ====================

/**
 * 硬件信息 Tab
 *
 * 展示 CPU 信息、内存信息、存储信息、电池健康四张卡片。
 * 首次进入时自动加载数据。
 */
@Composable
private fun HardwareTab(
    viewModel: DeepInspectorViewModel,
    uiState: DeepInspectorUiState
) {
    // 首次加载
    LaunchedEffect(Unit) {
        if (uiState.cpuInfo == null) {
            viewModel.loadHardwareInfo()
        }
    }

    if (uiState.isLoadingHardware) {
        LoadingIndicator()
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // CPU 信息卡片
        uiState.cpuInfo?.let { cpuInfo ->
            CpuInfoCard(cpuInfo = cpuInfo)
        }

        // 内存信息卡片
        uiState.memoryInfo?.let { memoryInfo ->
            MemoryInfoCard(memoryInfo = memoryInfo)
        }

        // 存储信息卡片
        if (uiState.storageDetails.isNotEmpty()) {
            StorageInfoCard(storageDetails = uiState.storageDetails)
        }

        // 电池健康卡片
        uiState.batteryHealth?.let { batteryHealth ->
            BatteryHealthCard(batteryHealth = batteryHealth)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * CPU 信息卡片
 */
@Composable
private fun CpuInfoCard(cpuInfo: com.banana.toolbox.domain.usecase.tools.CpuInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CPU信息",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 信息行
            InfoRow(label = "核心数", value = "${cpuInfo.cores} 核")
            InfoRow(label = "处理器", value = cpuInfo.processor)
            InfoRow(label = "硬件", value = cpuInfo.hardware)
            InfoRow(label = "ABI", value = cpuInfo.abis.joinToString(", "))
            InfoRow(label = "最大频率", value = formatFrequency(cpuInfo.maxFreqKHz))
            InfoRow(label = "最小频率", value = formatFrequency(cpuInfo.minFreqKHz))
            InfoRow(label = "当前频率", value = formatFrequency(cpuInfo.currentFreqKHz))
            InfoRow(label = "调度器", value = cpuInfo.governor)
        }
    }
}

/**
 * 内存信息卡片
 */
@Composable
private fun MemoryInfoCard(memoryInfo: com.banana.toolbox.domain.usecase.tools.MemoryInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "内存信息",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // RAM 使用情况
            Text(
                text = "RAM",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow(label = "总内存", value = formatFileSize(memoryInfo.totalRam))
            InfoRow(label = "可用内存", value = formatFileSize(memoryInfo.availableRam))
            InfoRow(label = "已用内存", value = formatFileSize(memoryInfo.usedRam))
            Spacer(modifier = Modifier.height(4.dp))
            RamProgressBar(
                used = memoryInfo.usedRam,
                total = memoryInfo.totalRam,
                label = "RAM 使用率"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Swap 使用情况
            Text(
                text = "交换空间",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            val usedSwap = memoryInfo.totalSwap - memoryInfo.freeSwap
            InfoRow(label = "总交换空间", value = formatFileSize(memoryInfo.totalSwap))
            InfoRow(label = "已用交换空间", value = formatFileSize(usedSwap))
            if (memoryInfo.totalSwap > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                RamProgressBar(
                    used = usedSwap,
                    total = memoryInfo.totalSwap,
                    label = "Swap 使用率"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // JVM 内存
            Text(
                text = "JVM内存",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow(label = "JVM 最大内存", value = formatFileSize(memoryInfo.jvmMaxMemory))
            InfoRow(label = "JVM 已分配", value = formatFileSize(memoryInfo.jvmTotalMemory))
            InfoRow(label = "JVM 空闲", value = formatFileSize(memoryInfo.jvmFreeMemory))
            if (memoryInfo.jvmMaxMemory > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                RamProgressBar(
                    used = memoryInfo.jvmTotalMemory - memoryInfo.jvmFreeMemory,
                    total = memoryInfo.jvmMaxMemory,
                    label = "JVM 使用率"
                )
            }
        }
    }
}

/**
 * 存储信息卡片
 */
@Composable
private fun StorageInfoCard(storageDetails: List<com.banana.toolbox.domain.usecase.tools.StorageDetail>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "存储信息",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            storageDetails.forEach { storage ->
                Text(
                    text = storage.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow(label = "总存储", value = formatFileSize(storage.totalBytes))
                InfoRow(label = "已用", value = formatFileSize(storage.usedBytes))
                InfoRow(label = "可用", value = formatFileSize(storage.availableBytes))
                Spacer(modifier = Modifier.height(4.dp))

                // 存储使用进度条
                StorageProgressBar(
                    used = storage.usedBytes,
                    total = storage.totalBytes
                )

                if (storage != storageDetails.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * 电池健康卡片
 */
@Composable
private fun BatteryHealthCard(batteryHealth: com.banana.toolbox.domain.usecase.tools.BatteryHealth) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "电池健康",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 电量进度条
            BatteryLevelIndicator(level = batteryHealth.level, status = batteryHealth.status)

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(label = "电量", value = "${batteryHealth.level}%")
            InfoRow(label = "状态", value = batteryHealth.status)
            InfoRow(label = "健康", value = batteryHealth.health)
            InfoRow(label = "温度", value = "${batteryHealth.temperature}°C")
            InfoRow(label = "电压", value = "${batteryHealth.voltage}V")
            InfoRow(label = "技术", value = batteryHealth.technology)
            InfoRow(label = "充电方式", value = batteryHealth.plugged)

            if (batteryHealth.designCapacity > 0) {
                InfoRow(label = "设计容量", value = "${batteryHealth.designCapacity} mAh")
                InfoRow(label = "当前容量", value = "${batteryHealth.currentCapacity} mAh")
            }
            if (batteryHealth.batteryWear > 0) {
                InfoRow(
                    label = "磨损程度",
                    value = "${batteryHealth.batteryWear}%",
                    valueColor = if (batteryHealth.batteryWear > 20) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            if (batteryHealth.cycleCount > 0) {
                InfoRow(label = "循环次数", value = "${batteryHealth.cycleCount} 次")
            }
        }
    }
}

// ==================== 传感器 Tab ====================

/**
 * 传感器列表 Tab
 *
 * 展示设备所有传感器信息，包含名称、类型、厂商、量程、分辨率、功耗。
 */
@Composable
private fun SensorsTab(
    viewModel: DeepInspectorViewModel,
    uiState: DeepInspectorUiState
) {
    // 首次加载
    LaunchedEffect(Unit) {
        if (uiState.sensors.isEmpty()) {
            viewModel.loadSensors()
        }
    }

    if (uiState.isLoadingSensors) {
        LoadingIndicator()
        return
    }

    if (uiState.sensors.isEmpty() && !uiState.isLoadingSensors) {
        EmptyState(message = "未检测到传感器")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = uiState.sensors,
            key = { "${it.name}_${it.type}" }
        ) { sensor ->
            SensorItemCard(sensor = sensor)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 传感器信息卡片
 */
@Composable
private fun SensorItemCard(sensor: com.banana.toolbox.domain.usecase.tools.SensorInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 传感器名称和类型
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sensor.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text(sensor.type, style = MaterialTheme.typography.labelSmall) },
                    enabled = false
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 传感器详情
            InfoRow(label = "厂商", value = sensor.vendor)
            InfoRow(label = "版本", value = "v${sensor.version}")
            InfoRow(label = "最大量程", value = "${sensor.maxRange}")
            InfoRow(label = "分辨率", value = "${sensor.resolution}")
            InfoRow(label = "功耗", value = "${sensor.power} mA")
        }
    }
}

// ==================== 进程管理 Tab ====================

/**
 * 进程管理 Tab
 *
 * 展示运行中的进程列表，支持按内存、PID、名称排序。
 */
@Composable
private fun ProcessesTab(
    viewModel: DeepInspectorViewModel,
    uiState: DeepInspectorUiState
) {
    // 首次加载
    LaunchedEffect(Unit) {
        if (uiState.processes.isEmpty()) {
            viewModel.loadProcesses()
        }
    }

    if (uiState.isLoadingProcesses) {
        LoadingIndicator()
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 排序选项
        SortOptionsRow(
            currentSort = uiState.processSortMode,
            onSortSelected = { viewModel.setProcessSortMode(it) }
        )

        // 进程数量
        Text(
            text = "共 ${uiState.processes.size} 个进程",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (uiState.processes.isEmpty() && !uiState.isLoadingProcesses) {
            EmptyState(message = "未检测到运行进程")
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(
                items = uiState.processes,
                key = { it.pid }
            ) { process ->
                ProcessItemCard(process = process)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 排序选项行
 */
@Composable
private fun SortOptionsRow(
    currentSort: ProcessSortMode,
    onSortSelected: (ProcessSortMode) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ProcessSortMode.entries.forEach { sortMode ->
            FilterChip(
                selected = currentSort == sortMode,
                onClick = { onSortSelected(sortMode) },
                label = { Text(sortMode.label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

/**
 * 进程信息卡片
 */
@Composable
private fun ProcessItemCard(process: com.banana.toolbox.domain.usecase.tools.ProcessInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = process.processName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "PID: ${process.pid}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatFileSize(process.memory),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = process.importance,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==================== 自启动管理 Tab ====================

/**
 * 自启动管理 Tab
 *
 * 展示开机自启动应用列表，支持开关切换。
 */
@Composable
private fun AutoStartTab(
    viewModel: DeepInspectorViewModel,
    uiState: DeepInspectorUiState
) {
    // 首次加载
    LaunchedEffect(Unit) {
        if (uiState.autoStartApps.isEmpty()) {
            viewModel.loadAutoStartApps()
        }
    }

    if (uiState.isLoadingAutoStart) {
        LoadingIndicator()
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 应用数量
        Text(
            text = "共 ${uiState.autoStartApps.size} 个自启动应用",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (uiState.autoStartApps.isEmpty() && !uiState.isLoadingAutoStart) {
            EmptyState(message = "未检测到自启动应用")
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(
                items = uiState.autoStartApps,
                key = { it.packageName + it.receiverName }
            ) { app ->
                AutoStartItemCard(
                    app = app,
                    onToggle = { viewModel.toggleAutoStartApp(app.packageName) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 自启动应用卡片
 */
@Composable
private fun AutoStartItemCard(
    app: com.banana.toolbox.domain.usecase.tools.AutoStartApp,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.receiverName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = app.isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

// ==================== 网络详情 Tab ====================

/**
 * 网络详情 Tab
 *
 * 展示当前网络连接的详细信息，包含 IP、网关、DNS、MAC、WiFi 等。
 */
@Composable
private fun NetworkTab(
    viewModel: DeepInspectorViewModel,
    uiState: DeepInspectorUiState
) {
    // 首次加载
    LaunchedEffect(Unit) {
        if (uiState.networkDetails == null) {
            viewModel.loadNetworkDetails()
        }
    }

    if (uiState.isLoadingNetwork) {
        LoadingIndicator()
        return
    }

    val network = uiState.networkDetails ?: return

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // IP 信息卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IP 信息",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoRow(label = "IP地址", value = network.ipAddress)
                InfoRow(label = "网关", value = network.gateway)
                InfoRow(label = "子网掩码", value = network.netmask)
                InfoRow(label = "DNS 1", value = network.dns1)
                InfoRow(label = "DNS 2", value = network.dns2)
                InfoRow(label = "DHCP 服务器", value = network.dhcpServer)
            }
        }

        // WiFi 信息卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WiFi 信息",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoRow(label = "WiFi名称", value = network.ssid)
                InfoRow(label = "BSSID", value = network.bssid)
                InfoRow(label = "链接速度", value = "${network.linkSpeed} Mbps")
                InfoRow(label = "信号强度", value = "${network.signalStrength} dBm")
                InfoRow(label = "频率", value = "${network.frequency} MHz")
                InfoRow(label = "网络 ID", value = "${network.networkId}")
                InfoRow(label = "MAC地址", value = network.bssid)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==================== 通用组件 ====================

/**
 * 信息行：图标 + 标签 + 值
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

/**
 * RAM / Swap / JVM 使用率进度条
 */
@Composable
private fun RamProgressBar(
    used: Long,
    total: Long,
    label: String
) {
    val progress = if (total > 0) used.toFloat() / total.toFloat() else 0f
    val progressPercent = (progress * 100).toInt()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    progressPercent > 90 -> MaterialTheme.colorScheme.error
                    progressPercent > 70 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = when {
                progressPercent > 90 -> MaterialTheme.colorScheme.error
                progressPercent > 70 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

/**
 * 存储使用进度条（类似甜甜圈效果，使用双色进度条）
 */
@Composable
private fun StorageProgressBar(
    used: Long,
    total: Long
) {
    val progress = if (total > 0) used.toFloat() / total.toFloat() else 0f
    val progressPercent = (progress * 100).toInt()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "使用率",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    progressPercent > 90 -> MaterialTheme.colorScheme.error
                    progressPercent > 80 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            color = when {
                progressPercent > 90 -> MaterialTheme.colorScheme.error
                progressPercent > 80 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

/**
 * 电池电量指示器
 */
@Composable
private fun BatteryLevelIndicator(
    level: Int,
    status: String
) {
    val progress = level.toFloat() / 100f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$level%",
                style = MaterialTheme.typography.titleLarge,
                color = when {
                    level <= 15 -> MaterialTheme.colorScheme.error
                    level <= 30 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            color = when {
                level <= 15 -> MaterialTheme.colorScheme.error
                level <= 30 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

/**
 * 加载中指示器
 */
@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "正在加载...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 空状态提示
 */
@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== 工具函数 ====================

/**
 * 格式化文件大小为可读字符串
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

/**
 * 格式化 CPU 频率
 */
private fun formatFrequency(freqKHz: Long): String {
    return when {
        freqKHz <= 0 -> "未知"
        freqKHz < 1000 -> "$freqKHz KHz"
        freqKHz < 1000000 -> String.format("%.1f MHz", freqKHz / 1000.0)
        else -> String.format("%.2f GHz", freqKHz / 1000000.0)
    }
}


