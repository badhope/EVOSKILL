package com.banana.toolbox.ui.screens.tools.cleaner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banana.toolbox.domain.usecase.tools.CleanResult
import com.banana.toolbox.domain.usecase.tools.JunkCategory
import com.banana.toolbox.domain.usecase.tools.JunkItem
import com.banana.toolbox.domain.usecase.tools.SearchResult

/**
 * 垃圾清理页面
 *
 * 提供垃圾扫描、文件搜索、大文件管理、重复文件查找四大功能模块。
 * 使用 TabRow 切换不同功能。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(
    onNavigateBack: () -> Unit,
    viewModel: CleanerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 错误提示 - 垃圾扫描
    LaunchedEffect(uiState.scanError) {
        uiState.scanError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearScanError()
        }
    }

    // 错误提示 - 文件搜索
    LaunchedEffect(uiState.searchError) {
        uiState.searchError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSearchError()
        }
    }

    // 错误提示 - 大文件
    LaunchedEffect(uiState.largeFilesError) {
        uiState.largeFilesError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearLargeFilesError()
        }
    }

    // 错误提示 - 重复文件
    LaunchedEffect(uiState.duplicatesError) {
        uiState.duplicatesError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearDuplicatesError()
        }
    }

    // 清理成功提示
    LaunchedEffect(uiState.cleanResult) {
        uiState.cleanResult?.let { result ->
            val message = "清理完成：成功 ${result.successCount} 项，释放 ${formatFileSize(result.freedSize)}"
            snackbarHostState.showSnackbar(message)
            viewModel.clearCleanResult()
        }
    }

    // 大文件删除成功提示
    LaunchedEffect(uiState.largeFilesCleanResult) {
        uiState.largeFilesCleanResult?.let { result ->
            val message = "删除完成：成功 ${result.successCount} 项，释放 ${formatFileSize(result.freedSize)}"
            snackbarHostState.showSnackbar(message)
            viewModel.clearLargeFilesCleanResult()
        }
    }

    // 重复文件删除成功提示
    LaunchedEffect(uiState.duplicatesCleanResult) {
        uiState.duplicatesCleanResult?.let { result ->
            val message = "删除完成：成功 ${result.successCount} 项，释放 ${formatFileSize(result.freedSize)}"
            snackbarHostState.showSnackbar(message)
            viewModel.clearDuplicatesCleanResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("垃圾清理") },
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
                CleanerTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = uiState.activeTab.ordinal == index,
                        onClick = { viewModel.setActiveTab(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }

            // Tab 内容区域
            when (uiState.activeTab) {
                CleanerTab.JUNK_SCAN -> JunkScanTab(viewModel = viewModel, uiState = uiState)
                CleanerTab.FILE_SEARCH -> FileSearchTab(viewModel = viewModel, uiState = uiState)
                CleanerTab.LARGE_FILES -> LargeFilesTab(viewModel = viewModel, uiState = uiState)
                CleanerTab.DUPLICATES -> DuplicatesTab(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}

// ==================== 垃圾扫描 Tab ====================

/**
 * 垃圾扫描 Tab
 *
 * 功能：扫描垃圾文件 -> 分类展示 -> 选择分类 -> 一键清理
 * 包含圆形进度指示器展示总垃圾大小
 */
@Composable
private fun JunkScanTab(
    viewModel: CleanerViewModel,
    uiState: CleanerUiState
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 扫描中动画
        if (uiState.isScanning) {
            ScanningAnimation()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "正在扫描垃圾文件...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(0.6f),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else if (uiState.scanResult != null) {
            // 扫描结果展示
            JunkSizeCircle(totalSize = uiState.scanResult.totalSize)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "共发现 ${uiState.scanResult.totalFiles} 个垃圾文件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 全选控制
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "选择清理项",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "全选",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Switch(
                        checked = uiState.selectedCategories.size == uiState.scanResult.categories.size,
                        onCheckedChange = { viewModel.toggleSelectAll() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 分类列表
            uiState.scanResult.categories.forEach { (category, size) ->
                val isSelected = category in uiState.selectedCategories
                CategoryCard(
                    category = category,
                    size = size,
                    isSelected = isSelected,
                    onToggle = { viewModel.toggleCategory(category) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 选中大小统计
            val selectedSize = viewModel.getSelectedSize()
            Text(
                text = "已选择: ${formatFileSize(selectedSize)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 一键清理按钮
            Button(
                onClick = { viewModel.cleanSelected() },
                enabled = uiState.selectedCategories.isNotEmpty() && !uiState.isCleaning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (uiState.isCleaning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("清理中...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("一键清理")
                }
            }
        } else {
            // 初始状态 - 未扫描
            Spacer(modifier = Modifier.height(80.dp))
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "点击下方按钮开始扫描",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "将扫描应用缓存、临时文件、日志等 10 类垃圾",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 开始扫描按钮（未扫描时显示）
        if (uiState.scanResult == null && !uiState.isScanning) {
            Button(
                onClick = { viewModel.scanJunk() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始扫描")
            }
        }

        // 清理中进度
        if (uiState.isCleaning) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "正在清理...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 扫描中旋转动画
 */
@Composable
private fun ScanningAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    CircularProgressIndicator(
        modifier = Modifier.size(120.dp),
        progress = { 0.75f },
        strokeWidth = 8.dp,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round
    )
}

/**
 * 垃圾大小圆形指示器
 */
@Composable
private fun JunkSizeCircle(totalSize: Long) {
    val sizeText = formatFileSize(totalSize)

    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(160.dp),
            progress = { 1f },
            strokeWidth = 12.dp,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = sizeText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "垃圾文件",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 分类卡片
 */
@Composable
private fun CategoryCard(
    category: JunkCategory,
    size: Long,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Text(
                text = category.icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            // 名称和大小
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatFileSize(size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 选中指示
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ==================== 文件搜索 Tab ====================

/**
 * 文件搜索 Tab
 *
 * 功能：输入关键词搜索文件，支持文件类型过滤和排序
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileSearchTab(
    viewModel: CleanerViewModel,
    uiState: CleanerUiState
) {
    var sortExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 搜索输入区域
        Column(modifier = Modifier.padding(16.dp)) {
            // 搜索框
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("搜索文件") },
                placeholder = { Text("输入文件名关键词") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 文件类型过滤 Chips
            Text(
                text = "文件类型",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FileTypeFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.fileTypeFilter == filter,
                        onClick = { viewModel.setFileTypeFilter(filter) },
                        label = { Text(filter.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = when (filter) {
                                    FileTypeFilter.ALL -> Icons.Default.Folder
                                    FileTypeFilter.IMAGE -> Icons.Default.Image
                                    FileTypeFilter.VIDEO -> Icons.Default.VideoFile
                                    FileTypeFilter.AUDIO -> Icons.Default.MusicNote
                                    FileTypeFilter.DOC -> Icons.Default.Description
                                    FileTypeFilter.ARCHIVE -> Icons.Default.FolderZip
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 排序方式 Chips
            Text(
                text = "排序方式",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FileSortMode.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.sortMode == mode,
                        onClick = { viewModel.setSortMode(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 搜索按钮
            Button(
                onClick = { viewModel.searchFiles() },
                enabled = uiState.searchQuery.isNotBlank() && !uiState.isSearching,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("搜索中...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("搜索文件")
                }
            }
        }

        Divider()

        // 搜索结果
        if (uiState.searchResults.isNotEmpty()) {
            Text(
                text = "找到 ${uiState.searchResults.size} 个文件",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = uiState.searchResults,
                    key = { it.path }
                ) { result ->
                    SearchResultItem(
                        result = result,
                        onCopyPath = {
                            clipboardManager.setText(AnnotatedString(result.path))
                        }
                    )
                }
            }
        } else if (!uiState.isSearching && uiState.searchQuery.isNotBlank()) {
            // 搜索完成但无结果
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "未找到匹配的文件",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // 初始状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "输入关键词搜索文件",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * 搜索结果项
 */
@Composable
private fun SearchResultItem(
    result: SearchResult,
    onCopyPath: () -> Unit
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件图标
            Icon(
                imageVector = if (result.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (result.isDirectory) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            // 文件信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatFileSize(result.size)}  |  ${formatTimestamp(result.lastModified)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = result.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // 复制路径按钮
            IconButton(onClick = onCopyPath) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制路径",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== 大文件管理 Tab ====================

/**
 * 大文件管理 Tab
 *
 * 功能：设置最小文件大小 -> 扫描大文件 -> 展示列表 -> 选择并删除
 */
@Composable
private fun LargeFilesTab(
    viewModel: CleanerViewModel,
    uiState: CleanerUiState
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 设置区域
        Column(modifier = Modifier.padding(16.dp)) {
            // 最小文件大小滑块
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "最小文件大小",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${uiState.minFileSizeMB.toInt()} MB",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = uiState.minFileSizeMB,
                        onValueChange = { viewModel.setMinFileSizeMB(it) },
                        valueRange = 10f..1024f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("10 MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("1 GB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 扫描按钮
            Button(
                onClick = { viewModel.scanLargeFiles() },
                enabled = !uiState.isScanningLargeFiles,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isScanningLargeFiles) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("扫描中...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("扫描大文件")
                }
            }
        }

        Divider()

        // 结果区域
        if (uiState.largeFiles.isNotEmpty()) {
            // 顶部操作栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "找到 ${uiState.largeFiles.size} 个大文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.selectedLargeFiles.isNotEmpty()) {
                    Text(
                        text = "已选 ${uiState.selectedLargeFiles.size} 项",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = uiState.largeFiles,
                    key = { it.path }
                ) { file ->
                    LargeFileItem(
                        result = file,
                        isSelected = file.path in uiState.selectedLargeFiles,
                        onToggle = { viewModel.toggleLargeFileSelection(file.path) }
                    )
                }

                // 底部删除按钮
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedVisibility(
                        visible = uiState.selectedLargeFiles.isNotEmpty()
                    ) {
                        Button(
                            onClick = { viewModel.deleteSelectedLargeFiles() },
                            enabled = !uiState.isDeletingLargeFiles,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            if (uiState.isDeletingLargeFiles) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onError,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("删除中...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("删除选中 (${uiState.selectedLargeFiles.size})")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else if (!uiState.isScanningLargeFiles) {
            // 无结果
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "设置最小文件大小后点击扫描",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * 大文件项
 */
@Composable
private fun LargeFileItem(
    result: SearchResult,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选中指示
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.width(32.dp))
            }

            // 文件信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = result.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // 文件大小
            Text(
                text = formatFileSize(result.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== 重复文件 Tab ====================

/**
 * 重复文件 Tab
 *
 * 功能：扫描相似图片 -> 分组展示 -> 选择并删除重复项
 */
@Composable
private fun DuplicatesTab(
    viewModel: CleanerViewModel,
    uiState: CleanerUiState
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 扫描按钮
        Column(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = { viewModel.scanSimilarImages() },
                enabled = !uiState.isScanningDuplicates,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isScanningDuplicates) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("查找重复...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("查找重复")
                }
            }
        }

        Divider()

        // 结果区域
        if (uiState.similarGroups.isNotEmpty()) {
            // 顶部信息
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "找到 ${uiState.similarGroups.size} 组重复文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.selectedDuplicateFiles.isNotEmpty()) {
                    Text(
                        text = "已选 ${uiState.selectedDuplicateFiles.size} 项",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = uiState.similarGroups,
                    key = { index, group -> "group_$index" }
                ) { groupIndex, group ->
                    SimilarGroupCard(
                        groupIndex = groupIndex,
                        group = group,
                        selectedFiles = uiState.selectedDuplicateFiles,
                        onToggleFile = { path -> viewModel.toggleDuplicateFileSelection(path) },
                        onSelectGroupExceptFirst = { viewModel.selectGroupExceptFirst(groupIndex) }
                    )
                }

                // 底部删除按钮
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedVisibility(
                        visible = uiState.selectedDuplicateFiles.isNotEmpty()
                    ) {
                        Button(
                            onClick = { viewModel.deleteSelectedDuplicates() },
                            enabled = !uiState.isDeletingDuplicates,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            if (uiState.isDeletingDuplicates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onError,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("删除中...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("删除选中 (${uiState.selectedDuplicateFiles.size})")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else if (!uiState.isScanningDuplicates) {
            // 无结果
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "点击上方按钮查找重复文件",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * 相似文件分组卡片
 */
@Composable
private fun SimilarGroupCard(
    groupIndex: Int,
    group: com.banana.toolbox.domain.usecase.tools.SimilarGroup,
    selectedFiles: Set<String>,
    onToggleFile: (String) -> Unit,
    onSelectGroupExceptFirst: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 组标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "重复组 ${groupIndex + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "${group.files.size} 个文件",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                // 快速选择（保留第一个，删除其余）
                OutlinedButton(
                    onClick = onSelectGroupExceptFirst,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "保留最新",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 文件列表
            group.files.forEachIndexed { fileIndex, file ->
                val isSelected = file.path in selectedFiles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onToggleFile(file.path) }
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 序号
                    Text(
                        text = "${fileIndex + 1}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(20.dp)
                    )
                    // 文件信息
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (fileIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${formatFileSize(file.size)}  |  ${formatTimestamp(file.lastModified)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 选中指示
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "已选择",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
