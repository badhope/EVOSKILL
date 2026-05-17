package com.banana.toolbox.ui.screens.network

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 网络工具页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkToolsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("网络工具") },
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
                .padding(16.dp)
        ) {
            // 网络状态卡片
            NetworkStatusCard()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 工具列表
            ToolCard(
                icon = Icons.Default.Speed,
                title = "网速测试",
                description = "测量当前网络下载/上传速度",
                color = MaterialTheme.colorScheme.primaryContainer
            ) { /* TODO */ }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ToolCard(
                icon = Icons.Default.NetworkPing,
                title = "网络诊断",
                description = "Ping / TraceRoute / DNS 查询",
                color = MaterialTheme.colorScheme.secondaryContainer
            ) { /* TODO */ }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ToolCard(
                icon = Icons.Default.Router,
                title = "局域网扫描",
                description = "发现同一网络下的设备",
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) { /* TODO */ }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ToolCard(
                icon = Icons.Default.SettingsEthernet,
                title = "端口扫描",
                description = "检测目标主机开放端口",
                color = MaterialTheme.colorScheme.errorContainer
            ) { /* TODO */ }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ToolCard(
                icon = Icons.Default.Language,
                title = "Whois 查询",
                description = "查询域名注册信息",
                color = MaterialTheme.colorScheme.surfaceVariant
            ) { /* TODO */ }
        }
    }
}

/**
 * 网络状态卡片
 */
@Composable
fun NetworkStatusCard() {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "当前网络状态",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusItem("WiFi", "已连接", Icons.Default.Wifi)
                StatusItem("IP", "192.168.1.100", Icons.Default.Language)
                StatusItem("速度", "867 Mbps", Icons.Default.Speed)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Divider()
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusItem("网关", "192.168.1.1", Icons.Default.Router)
                StatusItem("DNS", "8.8.8.8", Icons.Default.Dns)
                StatusItem("延迟", "12 ms", Icons.Default.NetworkPing)
            }
        }
    }
}

@Composable
fun StatusItem(label: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 工具卡片
 */
@Composable
fun ToolCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = color
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.padding(12.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "进入",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
