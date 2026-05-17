package com.banana.toolbox.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

/**
 * 首页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFileManager: () -> Unit,
    onNavigateToAppManager: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToGameCenter: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部标题
        Text(
            text = "Banana Toolbox",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "全能工具箱",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 功能模块卡片
        val modules = listOf(
            ModuleItem("文件管理", "浏览、管理手机文件", Icons.Default.Folder, onNavigateToFileManager),
            ModuleItem("应用管理", "卸载、备份应用", Icons.Default.Apps, onNavigateToAppManager),
            ModuleItem("网络工具", "测速、诊断网络", Icons.Default.Wifi, onNavigateToNetwork),
            ModuleItem("实用工具", "二维码、换算等", Icons.Default.Build, onNavigateToTools),
            ModuleItem("游戏中心", "加速、录屏、助手", Icons.Default.SportsEsports, onNavigateToGameCenter, true)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(modules) { module ->
                ModuleCard(module)
            }
        }
    }
}

data class ModuleItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isNew: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCard(module: ModuleItem) {
    ElevatedCard(
        onClick = module.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = module.icon,
                    contentDescription = module.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )

                Column {
                    Text(
                        text = module.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = module.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // New badge
            if (module.isNew) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text("NEW")
                }
            }
        }
    }
}
