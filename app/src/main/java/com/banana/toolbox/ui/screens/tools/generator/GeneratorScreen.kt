package com.banana.toolbox.ui.screens.tools.generator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banana.toolbox.domain.usecase.tools.GeneratorUseCases

/**
 * 生成工具页面
 *
 * 提供 UUID 生成、条形码、调色板、随机数据、批量生成五大功能模块。
 * 使用 TabRow 切换不同功能类型。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    onNavigateBack: () -> Unit,
    viewModel: GeneratorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    // 错误提示
    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // 复制确认回调
    var copyMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(copyMessage) {
        copyMessage?.let {
            snackbarHostState.showSnackbar(it)
            copyMessage = null
        }
    }

    fun copyToClipboard(text: String, label: String = "已复制") {
        clipboardManager.setText(AnnotatedString(text))
        copyMessage = label
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生成工具") },
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
                GeneratorTab.entries.forEachIndexed { index, tab ->
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
                                    GeneratorTab.UUID -> Icons.Default.Fingerprint
                                    GeneratorTab.BARCODE -> Icons.Default.QrCode2
                                    GeneratorTab.PALETTE -> Icons.Default.Palette
                                    GeneratorTab.RANDOM_DATA -> Icons.Default.Casino
                                    GeneratorTab.BATCH -> Icons.Default.ListAlt
                                },
                                contentDescription = tab.label
                            )
                        }
                    )
                }
            }

            // Tab 内容区域
            when (uiState.activeTab) {
                GeneratorTab.UUID -> UuidGeneratorTab(
                    viewModel = viewModel,
                    uiState = uiState,
                    copyToClipboard = ::copyToClipboard
                )
                GeneratorTab.BARCODE -> BarcodeGeneratorTab(
                    viewModel = viewModel,
                    uiState = uiState
                )
                GeneratorTab.PALETTE -> PaletteGeneratorTab(
                    viewModel = viewModel,
                    uiState = uiState,
                    copyToClipboard = ::copyToClipboard
                )
                GeneratorTab.RANDOM_DATA -> RandomDataTab(
                    viewModel = viewModel,
                    uiState = uiState,
                    copyToClipboard = ::copyToClipboard
                )
                GeneratorTab.BATCH -> BatchGeneratorTab(
                    viewModel = viewModel,
                    uiState = uiState,
                    copyToClipboard = ::copyToClipboard
                )
            }
        }
    }
}

// ==================== UUID 生成 Tab ====================

/**
 * UUID 生成 Tab
 *
 * 功能：选择版本 -> 设置数量 -> 生成 -> 查看结果 -> 复制
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UuidGeneratorTab(
    viewModel: GeneratorViewModel,
    uiState: GeneratorUiState,
    copyToClipboard: (String, String) -> Unit
) {
    val scrollState = rememberScrollState()

    // 版本下拉菜单
    var versionExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- 版本选择 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "版本",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GeneratorUseCases.UuidVersion.entries.forEach { version ->
                        val label = when (version) {
                            GeneratorUseCases.UuidVersion.V4 -> "V4"
                            GeneratorUseCases.UuidVersion.V4_NO_DASHES -> "V4无横线"
                            GeneratorUseCases.UuidVersion.V4_UPPER -> "V4大写"
                            GeneratorUseCases.UuidVersion.V3 -> "V3"
                        }
                        FilterChip(
                            selected = uiState.uuidVersion == version,
                            onClick = { viewModel.setUuidVersion(version) },
                            label = { Text(label) },
                            leadingIcon = if (uiState.uuidVersion == version) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        // ---- 数量输入 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "数量",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = { viewModel.setUuidCount(uiState.uuidCount - 1) }) {
                        Icon(Icons.Default.Remove, contentDescription = "减少")
                    }
                    OutlinedTextField(
                        value = uiState.uuidCount.toString(),
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { viewModel.setUuidCount(it) }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.foundation.text.KeyboardType.Number
                        )
                    )
                    IconButton(onClick = { viewModel.setUuidCount(uiState.uuidCount + 1) }) {
                        Icon(Icons.Default.Add, contentDescription = "增加")
                    }
                }
            }
        }

        // ---- 生成按钮 ----
        Button(
            onClick = { viewModel.generateUuids() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("生成")
        }

        // ---- 结果列表 ----
        if (uiState.uuidResults.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "生成结果 (${uiState.uuidResults.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(onClick = {
                            copyToClipboard(
                                uiState.uuidResults.joinToString("\n"),
                                "已复制全部 ${uiState.uuidResults.size} 条"
                            )
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("全部复制")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    uiState.uuidResults.forEachIndexed { index, uuid ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(28.dp)
                            )
                            Text(
                                text = uuid,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = { copyToClipboard(uuid, "已复制 UUID") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "复制",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (index < uiState.uuidResults.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 28.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==================== 条形码 Tab ====================

/**
 * 条形码生成 Tab
 *
 * 功能：输入内容 -> 选择类型 -> 生成 -> 预览 -> 验证状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarcodeGeneratorTab(
    viewModel: GeneratorViewModel,
    uiState: GeneratorUiState
) {
    val scrollState = rememberScrollState()

    // 类型下拉菜单
    var typeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- 内容输入 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "内容",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.barcodeContent,
                    onValueChange = { viewModel.setBarcodeContent(it) },
                    placeholder = { Text("请输入条形码内容") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (uiState.barcodeContent.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setBarcodeContent("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    }
                )
            }
        }

        // ---- 类型选择 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "类型",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (uiState.barcodeType) {
                            GeneratorUseCases.BarcodeType.CODE128 -> "CODE128"
                            GeneratorUseCases.BarcodeType.EAN13 -> "EAN-13"
                            GeneratorUseCases.BarcodeType.EAN8 -> "EAN-8"
                            GeneratorUseCases.BarcodeType.UPC_A -> "UPC-A"
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        GeneratorUseCases.BarcodeType.entries.forEach { type ->
                            val label = when (type) {
                                GeneratorUseCases.BarcodeType.CODE128 -> "CODE128"
                                GeneratorUseCases.BarcodeType.EAN13 -> "EAN-13"
                                GeneratorUseCases.BarcodeType.EAN8 -> "EAN-8"
                                GeneratorUseCases.BarcodeType.UPC_A -> "UPC-A"
                            }
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.setBarcodeType(type)
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (uiState.barcodeType) {
                        GeneratorUseCases.BarcodeType.CODE128 -> "支持所有 ASCII 字符 (32-126)"
                        GeneratorUseCases.BarcodeType.EAN13 -> "需要 13 位数字"
                        GeneratorUseCases.BarcodeType.EAN8 -> "需要 8 位数字"
                        GeneratorUseCases.BarcodeType.UPC_A -> "需要 12 位数字"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- 生成按钮 ----
        Button(
            onClick = { viewModel.generateBarcode() },
            enabled = uiState.barcodeContent.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("生成")
        }

        // ---- 结果预览 ----
        uiState.barcodeData?.let { data ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(
                    1.dp,
                    if (data.isValid) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (data.isValid)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (data.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (data.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (data.isValid) "验证通过" else "验证失败",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (data.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // 条形码预览区域
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        // 模拟条形码条纹
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            data.content.forEach { char ->
                                val width = (char.code % 3 + 1).dp
                                Box(
                                    modifier = Modifier
                                        .width(width)
                                        .height(80.dp)
                                        .clip(MaterialTheme.shapes.extraSmall),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.onSurface
                                    ) {}
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 详细信息
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("类型", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(data.type, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("内容", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(data.content, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("宽度", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${data.width}px", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==================== 调色板 Tab ====================

/**
 * 调色板生成 Tab
 *
 * 功能：基色选择 -> 调色板生成 -> 随机调色板 -> 渐变生成 -> 颜色预览与复制
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaletteGeneratorTab(
    viewModel: GeneratorViewModel,
    uiState: GeneratorUiState,
    copyToClipboard: (String, String) -> Unit
) {
    val scrollState = rememberScrollState()
    var showColorDialog by remember { mutableStateOf(false) }
    var colorDialogTarget by remember { mutableStateOf("base") } // "base", "gradientStart", "gradientEnd"

    // 颜色选择对话框
    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("选择颜色") },
            text = {
                // 预设颜色网格
                val presetColors = listOf(
                    0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5,
                    0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50,
                    0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800,
                    0xFFFF5722, 0xFF795548, 0xFF607D8B, 0xFF000000, 0xFFFFFFFF
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetColors.chunked(5).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { colorInt ->
                                Surface(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable {
                                            when (colorDialogTarget) {
                                                "base" -> viewModel.setBaseColor(colorInt)
                                                "gradientStart" -> viewModel.setGradientStartColor(colorInt)
                                                "gradientEnd" -> viewModel.setGradientEndColor(colorInt)
                                            }
                                            showColorDialog = false
                                        },
                                    color = Color(colorInt),
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {}
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- 调色板生成 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "调色板",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 基础颜色选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "基础颜色",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(72.dp)
                    )
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                colorDialogTarget = "base"
                                showColorDialog = true
                            },
                        color = Color(uiState.baseColor),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {}
                    Text(
                        text = String.format("#%06X", uiState.baseColor and 0xFFFFFF),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 数量滑块
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "数量",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${uiState.paletteCount}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = uiState.paletteCount.toFloat(),
                    onValueChange = { viewModel.setPaletteCount(it.toInt()) },
                    valueRange = 3f..10f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 生成按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.generatePalette() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("生成调色板")
                    }
                    OutlinedButton(
                        onClick = { viewModel.generateRandomPalette() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("随机调色板")
                    }
                }
            }
        }

        // ---- 调色板结果 ----
        if (uiState.paletteColors.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "调色板结果",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 颜色色块网格
                    uiState.paletteColors.forEach { paletteColor ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(MaterialTheme.shapes.small),
                                color = Color(paletteColor.color),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {}
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = paletteColor.hex,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "H:${paletteColor.hue.toInt()} S:${(paletteColor.saturation * 100).toInt()}% L:${(paletteColor.lightness * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                copyToClipboard(paletteColor.hex, "已复制 ${paletteColor.hex}")
                            }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "复制",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- 渐变生成 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "渐变",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 起始颜色
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "起始颜色",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(72.dp)
                    )
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                colorDialogTarget = "gradientStart"
                                showColorDialog = true
                            },
                        color = Color(uiState.gradientStartColor),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {}
                    Text(
                        text = String.format("#%06X", uiState.gradientStartColor and 0xFFFFFF),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 结束颜色
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "结束颜色",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(72.dp)
                    )
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                colorDialogTarget = "gradientEnd"
                                showColorDialog = true
                            },
                        color = Color(uiState.gradientEndColor),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {}
                    Text(
                        text = String.format("#%06X", uiState.gradientEndColor and 0xFFFFFF),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 步数
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "步数",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${uiState.gradientStepsCount}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = uiState.gradientStepsCount.toFloat(),
                    onValueChange = { viewModel.setGradientSteps(it.toInt()) },
                    valueRange = 2f..20f,
                    steps = 17,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.generateGradient() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Gradient, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("生成渐变")
                }
            }
        }

        // ---- 渐变结果 ----
        if (uiState.gradientSteps.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "渐变结果",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 渐变预览条
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(MaterialTheme.shapes.medium)
                    ) {
                        uiState.gradientSteps.forEach { step ->
                            Surface(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                color = Color(step.color)
                            ) {}
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 渐变色列表
                    uiState.gradientSteps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(MaterialTheme.shapes.extraSmall),
                                color = Color(step.color),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step.hex,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(step.position * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = { copyToClipboard(step.hex, "已复制 ${step.hex}") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "复制",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==================== 随机数据 Tab ====================

/**
 * 随机数据生成 Tab
 *
 * 功能：多种随机数据生成，包括姓名、手机号、邮箱、IP/MAC/URL、日期/地址、文本、数字/Hex/Base64
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RandomDataTab(
    viewModel: GeneratorViewModel,
    uiState: GeneratorUiState,
    copyToClipboard: (String, String) -> Unit
) {
    val scrollState = rememberScrollState()

    // 运营商下拉菜单
    var carrierExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- 随机姓名 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "随机姓名",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // 单个生成
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.randomNameResult.isNotEmpty()) {
                        Text(
                            text = uiState.randomNameResult,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { copyToClipboard(uiState.randomNameResult, "已复制姓名") }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.generateChineseName() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("随机姓名")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 批量生成
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("数量:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = uiState.randomNameCount.toString(),
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { viewModel.setRandomNameCount(it) }
                        },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.generateChineseNames() }) {
                        Text("批量生成")
                    }
                }

                if (uiState.randomNameBatchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            uiState.randomNameBatchResults.chunked(4).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { name ->
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- 随机手机号 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "随机手机号",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 运营商选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "运营商",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(56.dp)
                    )
                    listOf("", "移动", "联通", "电信").forEach { carrier ->
                        FilterChip(
                            selected = uiState.phoneCarrier == carrier,
                            onClick = { viewModel.setPhoneCarrier(carrier) },
                            label = { Text(if (carrier.isEmpty()) "全部" else carrier) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 单个生成
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.randomPhoneResult.isNotEmpty()) {
                        Text(
                            text = uiState.randomPhoneResult,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { copyToClipboard(uiState.randomPhoneResult, "已复制手机号") }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.generatePhoneNumber() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("随机手机号")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 批量生成
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("数量:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = uiState.randomPhoneCount.toString(),
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { viewModel.setRandomPhoneCount(it) }
                        },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.generatePhoneNumbers() }) {
                        Text("批量生成")
                    }
                }

                if (uiState.randomPhoneBatchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            uiState.randomPhoneBatchResults.chunked(3).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { number ->
                                        Text(
                                            text = number,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- 随机邮箱 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "随机邮箱",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.randomEmailResult.isNotEmpty()) {
                        Text(
                            text = uiState.randomEmailResult,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { copyToClipboard(uiState.randomEmailResult, "已复制邮箱") }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.generateEmail() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("随机邮箱")
                }
            }
        }

        // ---- 随机 IP / MAC / URL ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "随机IP / MAC / URL",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // IP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("随机IP", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(72.dp))
                    if (uiState.randomIpResult.isNotEmpty()) {
                        Text(
                            text = uiState.randomIpResult,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { copyToClipboard(uiState.randomIpResult, "已复制IP") }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.generateRandomIp() }) {
                        Text("生成")
                    }
                }

                // MAC
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("随机MAC", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(72.dp))
                    if (uiState.randomMacResult.isNotEmpty()) {
                        Text(
                            text = uiState.randomMacResult,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { copyToClipboard(uiState.randomMacResult, "已复制MAC") }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.generateRandomMac() }) {
                        Text("生成")
                    }
                }

                // URL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("随机URL", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(72.dp))
                    if (uiState.randomUrlResult.isNotEmpty()) {
                        Text(
                            text = uiState.randomUrlResult,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { copyToClipboard(uiState.randomUrlResult, "已复制URL") }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.generateRandomUrl() }) {
                        Text("生成")
                    }
                }
            }
        }

        // ---- 随机日期 / 地址 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "随机日期 / 地址",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 日期
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("随机日期", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(72.dp))
                    if (uiState.randomDateResult.isNotEmpty()) {
                        Text(
                            text = uiState.randomDateResult,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { copyToClipboard(uiState.randomDateResult, "已复制日期") }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.generateRandomDate() }) {
                        Text("生成")
                    }
                }

                // 地址
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("随机地址", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(72.dp))
                    if (uiState.randomAddressResult.isNotEmpty()) {
                        Text(
                            text = uiState.randomAddressResult,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { copyToClipboard(uiState.randomAddressResult, "已复制地址") }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.generateRandomAddress() }) {
                        Text("生成")
                    }
                }
            }
        }

        // ---- Lorem Ipsum / 中文文本 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Lorem Ipsum / 中文文本",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Lorem Ipsum
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Lorem Ipsum", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = uiState.loremIpsumWords.toString(),
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { viewModel.setLoremIpsumWords(it) }
                        },
                        modifier = Modifier.width(72.dp),
                        singleLine = true,
                        label = { Text("词数") }
                    )
                    OutlinedButton(onClick = { viewModel.generateLoremIpsum() }) {
                        Text("生成")
                    }
                }

                if (uiState.loremIpsumResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = uiState.loremIpsumResult,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = { copyToClipboard(uiState.loremIpsumResult, "已复制文本") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 中文文本
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("中文文本", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = uiState.chineseTextChars.toString(),
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { viewModel.setChineseTextChars(it) }
                        },
                        modifier = Modifier.width(72.dp),
                        singleLine = true,
                        label = { Text("字数") }
                    )
                    OutlinedButton(onClick = { viewModel.generateChineseText() }) {
                        Text("生成")
                    }
                }

                if (uiState.chineseTextResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = uiState.chineseTextResult,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = { copyToClipboard(uiState.chineseTextResult, "已复制文本") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // ---- 随机数字 / Hex / Base64 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "随机数字 / Hex / Base64",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 随机数字
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("随机数字", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(72.dp))
                    OutlinedTextField(
                        value = uiState.randomNumberMin.toString(),
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { viewModel.setRandomNumberMin(it) }
                        },
                        modifier = Modifier.width(64.dp),
                        singleLine = true,
                        label = { Text("最小") }
                    )
                    Text("~", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = uiState.randomNumberMax.toString(),
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { viewModel.setRandomNumberMax(it) }
                        },
                        modifier = Modifier.width(64.dp),
                        singleLine = true,
                        label = { Text("最大") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.generateRandomNumber() }) {
                        Text("生成")
                    }
                }
                if (uiState.randomNumberResult.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.randomNumberResult,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { copyToClipboard(uiState.randomNumberResult, "已复制数字") }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 随机 Hex
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("随机Hex", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(72.dp))
                    OutlinedTextField(
                        value = uiState.randomHexLength.toString(),
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { viewModel.setRandomHexLength(it) }
                        },
                        modifier = Modifier.width(72.dp),
                        singleLine = true,
                        label = { Text("长度") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.generateRandomHex() }) {
                        Text("生成")
                    }
                }
                if (uiState.randomHexResult.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.randomHexResult,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { copyToClipboard(uiState.randomHexResult, "已复制Hex") }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 随机 Base64
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("随机Base64", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(72.dp))
                    OutlinedTextField(
                        value = uiState.randomBase64Length.toString(),
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { viewModel.setRandomBase64Length(it) }
                        },
                        modifier = Modifier.width(72.dp),
                        singleLine = true,
                        label = { Text("长度") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.generateRandomBase64() }) {
                        Text("生成")
                    }
                }
                if (uiState.randomBase64Result.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.randomBase64Result,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { copyToClipboard(uiState.randomBase64Result, "已复制Base64") }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==================== 批量生成 Tab ====================

/**
 * 批量生成 Tab
 *
 * 功能：选择类型 -> 设置数量 -> 批量生成 -> 全选/复制选中/导出
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchGeneratorTab(
    viewModel: GeneratorViewModel,
    uiState: GeneratorUiState,
    copyToClipboard: (String, String) -> Unit
) {
    // 类型下拉菜单
    var typeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ---- 设置区域 ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 类型选择
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "类型",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.batchType.label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            BatchDataType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label) },
                                    onClick = {
                                        viewModel.setBatchType(type)
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 数量输入
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "数量",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = { viewModel.setBatchCount(uiState.batchCount - 10) }) {
                            Icon(Icons.Default.Remove, contentDescription = "减少")
                        }
                        OutlinedTextField(
                            value = uiState.batchCount.toString(),
                            onValueChange = { text ->
                                text.toIntOrNull()?.let { viewModel.setBatchCount(it) }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.foundation.text.KeyboardType.Number
                            )
                        )
                        IconButton(onClick = { viewModel.setBatchCount(uiState.batchCount + 10) }) {
                            Icon(Icons.Default.Add, contentDescription = "增加")
                        }
                    }
                }
            }

            // 生成按钮
            Button(
                onClick = { viewModel.generateBatch() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("生成")
            }
        }

        // ---- 操作栏 ----
        if (uiState.batchResults.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 全选
                    TextButton(onClick = { viewModel.toggleSelectAllBatch() }) {
                        Icon(
                            if (uiState.selectedBatchIndices.size == uiState.batchResults.size)
                                Icons.Default.Deselect
                            else
                                Icons.Default.SelectAll,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (uiState.selectedBatchIndices.size == uiState.batchResults.size) "取消全选"
                            else "全选"
                        )
                    }

                    Text(
                        text = "已选 ${uiState.selectedBatchIndices.size}/${uiState.batchResults.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 复制选中
                    TextButton(
                        onClick = {
                            val text = viewModel.getSelectedBatchText()
                            if (text.isNotEmpty()) {
                                copyToClipboard(text, "已复制 ${uiState.selectedBatchIndices.size} 条")
                            }
                        },
                        enabled = uiState.selectedBatchIndices.isNotEmpty()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制选中")
                    }

                    // 复制全部
                    TextButton(
                        onClick = {
                            val text = viewModel.getAllBatchText()
                            if (text.isNotEmpty()) {
                                copyToClipboard(text, "已复制全部 ${uiState.batchResults.size} 条")
                            }
                        }
                    ) {
                        Icon(Icons.Default.CopyAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("全部复制")
                    }
                }
            }

            // ---- 结果列表 ----
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(uiState.batchResults) { index, item ->
                    val isSelected = uiState.selectedBatchIndices.contains(index)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleBatchItemSelection(index) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected)
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        else
                            null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleBatchItemSelection(index) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(32.dp)
                            )
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = { copyToClipboard(item, "已复制") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "复制",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            // 空状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ListAlt,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "选择类型并生成批量数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
