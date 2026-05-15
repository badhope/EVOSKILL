package com.banana.toolbox.ui.screens.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 实用工具页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityToolsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConverter: () -> Unit = {},
    onNavigateToCrypto: () -> Unit = {},
    onNavigateToCleaner: () -> Unit = {},
    onNavigateToInspector: () -> Unit = {},
    onNavigateToGenerator: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("实用工具") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ========== 强力工具 ==========
            SectionTitle("强力工具")

            val powerTools = listOf(
                ToolEntry("文件转换", Icons.Default.SwapHoriz, "图片/文本/文档格式转换", onNavigateToConverter),
                ToolEntry("加密解密", Icons.Default.Lock, "文件/文本加密与密码工具", onNavigateToCrypto),
                ToolEntry("垃圾清理", Icons.Default.CleaningServices, "扫描清理系统垃圾文件", onNavigateToCleaner),
                ToolEntry("深层检测", Icons.Default.BugReport, "CPU/内存/传感器/进程", onNavigateToInspector),
                ToolEntry("生成工具", Icons.Default.AutoAwesome, "UUID/密码/调色板/随机数据", onNavigateToGenerator),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(powerTools) { tool ->
                    ToolGridItem(tool = tool)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // ========== 常用工具 ==========
            SectionTitle("常用工具")

            val commonTools = listOf(
                ToolEntry("单位换算", Icons.Default.Straighten, "长度、重量、温度等"),
                ToolEntry("颜色取值", Icons.Default.Palette, "HEX、RGB、HSL 转换"),
                ToolEntry("设备信息", Icons.Default.PhoneAndroid, "手机硬件和系统信息"),
                ToolEntry("文本处理", Icons.Default.TextFields, "编码、加解密、格式化"),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(commonTools) { tool ->
                    ToolGridItem(tool = tool)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // ========== 更多工具 ==========
            SectionTitle("更多工具")

            val moreTools = listOf(
                ToolEntry("二维码", Icons.Default.QrCode2, "生成和扫描二维码"),
                ToolEntry("图片处理", Icons.Default.Image, "压缩、格式转换、水印"),
                ToolEntry("剪贴板", Icons.Default.ContentPaste, "历史记录管理"),
                ToolEntry("计算器", Icons.Default.Calculate, "科学计算器"),
                ToolEntry("时间戳", Icons.Default.Schedule, "时间戳与日期互转"),
                ToolEntry("网络工具", Icons.Default.Language, "IP查询、DNS、端口扫描"),
                ToolEntry("APK提取", Icons.Default.Android, "提取已安装应用APK"),
                ToolEntry("屏幕适配", Icons.Default.PhoneIphone, "屏幕尺寸与分辨率查看"),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(moreTools) { tool ->
                    ToolGridItem(tool = tool)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

data class ToolEntry(
    val name: String,
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit = {}
)

/**
 * 工具网格项
 */
@Composable
fun ToolGridItem(tool: ToolEntry) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { tool.onClick() }
            .padding(vertical = 8.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tool.name,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
