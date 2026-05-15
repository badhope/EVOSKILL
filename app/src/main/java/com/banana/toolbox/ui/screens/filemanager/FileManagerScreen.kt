package com.banana.toolbox.ui.screens.filemanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banana.toolbox.domain.model.FileItem
import com.banana.toolbox.domain.model.FileType
import com.banana.toolbox.domain.model.SortBy
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件管理页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    onNavigateBack: () -> Unit
) {
    var currentPath by remember { mutableStateOf("/sdcard") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf(SortBy.NAME) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf(setOf<String>()) }
    var showOperationMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索文件...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("文件管理")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, "搜索")
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, "排序")
                    }
                    IconButton(onClick = { showOperationMenu = true }) {
                        Icon(Icons.Default.MoreVert, "更多")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (selectedFiles.isNotEmpty()) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { /* 复制 */ }
                        ) {
                            Icon(Icons.Default.ContentCopy, "复制")
                            Text("复制", fontSize = 12.sp)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { /* 移动 */ }
                        ) {
                            Icon(Icons.Default.DriveFileMove, "移动")
                            Text("移动", fontSize = 12.sp)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { /* 删除 */ }
                        ) {
                            Icon(Icons.Default.Delete, "删除")
                            Text("删除", fontSize = 12.sp)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedFiles = emptySet() }
                        ) {
                            Icon(Icons.Default.Close, "取消")
                            Text("取消", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* 新建文件夹 */ }
            ) {
                Icon(Icons.Default.CreateNewFolder, "新建文件夹")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 路径导航栏
            PathBar(currentPath = currentPath)
            
            // 文件列表
            FileListContent(
                currentPath = currentPath,
                searchQuery = searchQuery,
                sortBy = sortBy,
                selectedFiles = selectedFiles,
                onFileClick = { file ->
                    if (file.isDirectory) {
                        currentPath = file.path
                    }
                },
                onFileLongClick = { file ->
                    selectedFiles = if (file.path in selectedFiles) {
                        selectedFiles - file.path
                    } else {
                        selectedFiles + file.path
                    }
                },
                onPathChange = { currentPath = it }
            )
        }
    }
    
    // 排序菜单
    SortMenu(
        showMenu = showSortMenu,
        currentSort = sortBy,
        onDismiss = { showSortMenu = false },
        onSortSelected = {
            sortBy = it
            showSortMenu = false
        }
    )
    
    // 更多操作菜单
    MoreOptionsMenu(
        showMenu = showOperationMenu,
        onDismiss = { showOperationMenu = false },
        onRefresh = { /* 刷新 */ showOperationMenu = false },
        onSelectAll = { /* 全选 */ showOperationMenu = false },
        onStorageAnalysis = { /* 存储分析 */ showOperationMenu = false }
    )
}

/**
 * 路径导航栏
 */
@Composable
fun PathBar(currentPath: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Home,
                contentDescription = "首页",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 文件列表内容
 */
@Composable
fun FileListContent(
    currentPath: String,
    searchQuery: String,
    sortBy: SortBy,
    selectedFiles: Set<String>,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    onPathChange: (String) -> Unit
) {
    // 模拟数据
    val sampleFiles = remember(currentPath) {
        listOf(
            FileItem("/sdcard/DCIM", "DCIM", 0, System.currentTimeMillis(), true),
            FileItem("/sdcard/Download", "Download", 0, System.currentTimeMillis(), true),
            FileItem("/sdcard/Documents", "Documents", 0, System.currentTimeMillis(), true),
            FileItem("/sdcard/Music", "Music", 0, System.currentTimeMillis(), true),
            FileItem("/sdcard/Pictures", "Pictures", 0, System.currentTimeMillis(), true),
            FileItem("/sdcard/Videos", "Videos", 0, System.currentTimeMillis(), true),
            FileItem("/sdcard/photo.jpg", "photo.jpg", 1572864, System.currentTimeMillis() - 86400000, false, extension = "jpg"),
            FileItem("/sdcard/report.pdf", "report.pdf", 2411724, System.currentTimeMillis() - 172800000, false, extension = "pdf"),
            FileItem("/sdcard/music.mp3", "music.mp3", 4404019, System.currentTimeMillis() - 259200000, false, extension = "mp3"),
            FileItem("/sdcard/backup.zip", "backup.zip", 125829120, System.currentTimeMillis() - 345600000, false, extension = "zip"),
            FileItem("/sdcard/app.apk", "toolbox.apk", 52428800, System.currentTimeMillis() - 432000000, false, extension = "apk"),
            FileItem("/sdcard/video.mp4", "video.mp4", 104857600, System.currentTimeMillis() - 518400000, false, extension = "mp4"),
        )
    }
    
    val filteredFiles = remember(sampleFiles, searchQuery) {
        if (searchQuery.isBlank()) {
            sampleFiles
        } else {
            sampleFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    val sortedFiles = remember(filteredFiles, sortBy) {
        when (sortBy) {
            SortBy.NAME -> filteredFiles.sortedBy { it.name.lowercase() }
            SortBy.SIZE -> filteredFiles.sortedByDescending { it.size }
            SortBy.DATE -> filteredFiles.sortedByDescending { it.lastModified }
            SortBy.TYPE -> filteredFiles.sortedBy { it.getFileType().name }
        }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(sortedFiles, key = { it.path }) { file ->
            FileListItem(
                file = file,
                isSelected = file.path in selectedFiles,
                onClick = { onFileClick(file) },
                onLongClick = { onFileLongClick(file) }
            )
        }
    }
}

/**
 * 文件列表项
 */
@Composable
fun FileListItem(
    file: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件图标
            FileIcon(fileType = file.getFileType())
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文件信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatFileMeta(file),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 选中指示器
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "进入",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider()
}

/**
 * 文件图标
 */
@Composable
fun FileIcon(fileType: FileType) {
    val (icon, color) = when (fileType) {
        FileType.DIRECTORY -> Icons.Default.Folder to Color(0xFF42A5F5)
        FileType.IMAGE -> Icons.Default.Image to Color(0xFFEC407A)
        FileType.VIDEO -> Icons.Default.VideoFile to Color(0xFFAB47BC)
        FileType.AUDIO -> Icons.Default.AudioFile to Color(0xFF7E57C2)
        FileType.DOCUMENT -> Icons.Default.Description to Color(0xFF42A5F5)
        FileType.ARCHIVE -> Icons.Default.FolderZip to Color(0xFFFFA726)
        FileType.APK -> Icons.Default.Android to Color(0xFF66BB6A)
        FileType.CODE -> Icons.Default.Code to Color(0xFF78909C)
        FileType.UNKNOWN -> Icons.Default.InsertDriveFile to Color(0xFF90A4AE)
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.padding(8.dp).size(24.dp)
        )
    }
}

/**
 * 格式化文件元信息
 */
fun formatFileMeta(file: FileItem): String {
    val size = if (file.isDirectory) "文件夹" else formatFileSize(file.size)
    val date = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        .format(Date(file.lastModified))
    return "$size · $date"
}

/**
 * 格式化文件大小
 */
fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1073741824 -> "%.1f GB".format(bytes / 1073741824.0)
        bytes >= 1048576 -> "%.1f MB".format(bytes / 1048576.0)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}

/**
 * 排序菜单
 */
@Composable
fun SortMenu(
    showMenu: Boolean,
    currentSort: SortBy,
    onDismiss: () -> Unit,
    onSortSelected: (SortBy) -> Unit
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismiss
    ) {
        SortBy.entries.forEach { sort ->
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (sort) {
                                SortBy.NAME -> "按名称"
                                SortBy.SIZE -> "按大小"
                                SortBy.DATE -> "按日期"
                                SortBy.TYPE -> "按类型"
                            }
                        )
                        if (sort == currentSort) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Check, "当前", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                onClick = { onSortSelected(sort) }
            )
        }
    }
}

/**
 * 更多操作菜单
 */
@Composable
fun MoreOptionsMenu(
    showMenu: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSelectAll: () -> Unit,
    onStorageAnalysis: () -> Unit
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("刷新") },
            leadingIcon = { Icon(Icons.Default.Refresh, null) },
            onClick = onRefresh
        )
        DropdownMenuItem(
            text = { Text("全选") },
            leadingIcon = { Icon(Icons.Default.SelectAll, null) },
            onClick = onSelectAll
        )
        DropdownMenuItem(
            text = { Text("新建文件夹") },
            leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) },
            onClick = onDismiss
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("存储分析") },
            leadingIcon = { Icon(Icons.Default.Storage, null) },
            onClick = onStorageAnalysis
        )
    }
}
