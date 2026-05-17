package com.banana.toolbox.ui.screens.appmanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Divider
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banana.toolbox.domain.model.AppItem

/**
 * 应用管理页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var selectedApps by remember { mutableStateOf(setOf<String>()) }
    
    val tabs = listOf("全部", "用户应用", "系统应用")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索应用...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("应用管理")
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
            if (selectedApps.isNotEmpty()) {
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
                            modifier = Modifier.clickable { /* 卸载 */ }
                        ) {
                            Icon(Icons.Default.Delete, "卸载")
                            Text("卸载", fontSize = 12.sp)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { /* 备份 */ }
                        ) {
                            Icon(Icons.Default.Backup, "备份")
                            Text("备份", fontSize = 12.sp)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { /* 分享 */ }
                        ) {
                            Icon(Icons.Default.Share, "分享")
                            Text("分享", fontSize = 12.sp)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedApps = emptySet() }
                        ) {
                            Icon(Icons.Default.Close, "取消")
                            Text("取消", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab 切换
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // 应用统计
            AppStatisticsBar()
            
            // 应用列表
            AppListContent(
                selectedTab = selectedTab,
                searchQuery = searchQuery,
                selectedApps = selectedApps,
                onAppClick = { /* 查看详情 */ },
                onAppLongClick = { app ->
                    selectedApps = if (app.packageName in selectedApps) {
                        selectedApps - app.packageName
                    } else {
                        selectedApps + app.packageName
                    }
                }
            )
        }
    }
}

/**
 * 应用统计栏
 */
@Composable
fun AppStatisticsBar() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "已安装", value = "45")
            StatItem(label = "用户应用", value = "32")
            StatItem(label = "系统应用", value = "13")
            StatItem(label = "总大小", value = "3.2 GB")
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 应用列表内容
 */
@Composable
fun AppListContent(
    selectedTab: Int,
    searchQuery: String,
    selectedApps: Set<String>,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit
) {
    // 模拟数据
    val allApps = remember {
        listOf(
            AppItem("com.tencent.mm", "微信", "8.0.33", 2680, 268435456, System.currentTimeMillis() - 86400000, System.currentTimeMillis(), false, "/data/app/com.tencent.mm/base.apk"),
            AppItem("com.eg.android.AlipayGphone", "支付宝", "10.3.50", 1035, 207618048, System.currentTimeMillis() - 172800000, System.currentTimeMillis(), false, "/data/app/com.eg.android.AlipayGphone/base.apk"),
            AppItem("com.tencent.mobileqq", "QQ", "9.0.20", 1560, 314572800, System.currentTimeMillis() - 259200000, System.currentTimeMillis(), false, "/data/app/com.tencent.mobileqq/base.apk"),
            AppItem("com.android.chrome", "Chrome", "120.0", 6090, 104857600, System.currentTimeMillis() - 345600000, System.currentTimeMillis() - 86400000, true, "/system/app/Chrome/Chrome.apk"),
            AppItem("com.android.settings", "设置", "14", 34, 15728640, System.currentTimeMillis() - 518400000, System.currentTimeMillis() - 172800000, true, "/system/app/Settings/Settings.apk"),
            AppItem("com.android.camera2", "相机", "14", 34, 52428800, System.currentTimeMillis() - 604800000, System.currentTimeMillis() - 259200000, true, "/system/app/Camera2/Camera2.apk"),
            AppItem("com.android.vending", "Play 商店", "38.0", 3800, 62914560, System.currentTimeMillis() - 691200000, System.currentTimeMillis() - 345600000, true, "/system/app/Vending/Vending.apk"),
            AppItem("tv.danmaku.bili", "哔哩哔哩", "7.50.0", 7500, 188743680, System.currentTimeMillis() - 777600000, System.currentTimeMillis() - 432000000, false, "/data/app/tv.danmaku.bili/base.apk"),
            AppItem("com.ss.android.ugc.aweme", "抖音", "29.0.0", 2900, 209715200, System.currentTimeMillis() - 864000000, System.currentTimeMillis() - 518400000, false, "/data/app/com.ss.android.ugc.aweme/base.apk"),
            AppItem("com.netease.cloudmusic", "网易云音乐", "8.10.00", 8100, 104857600, System.currentTimeMillis() - 950400000, System.currentTimeMillis() - 604800000, false, "/data/app/com.netease.cloudmusic/base.apk"),
        )
    }
    
    val filteredApps = remember(allApps, selectedTab, searchQuery) {
        val byTab = when (selectedTab) {
            1 -> allApps.filter { !it.isSystemApp }
            2 -> allApps.filter { it.isSystemApp }
            else -> allApps
        }
        if (searchQuery.isBlank()) byTab
        else byTab.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(filteredApps, key = { it.packageName }) { app ->
            AppListItem(
                app = app,
                isSelected = app.packageName in selectedApps,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) }
            )
        }
    }
}

/**
 * 应用列表项
 */
@Composable
fun AppListItem(
    app: AppItem,
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
            // 应用图标占位
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = app.appName.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 应用信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    Text(
                        text = "v${app.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = app.getFormattedSize(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 系统应用标签
            if (app.isSystemApp) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "系统",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // 选中指示器
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    Divider()
}
