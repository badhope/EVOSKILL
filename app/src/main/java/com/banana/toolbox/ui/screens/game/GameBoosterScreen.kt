package com.banana.toolbox.ui.screens.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

/**
 * 游戏加速界面
 * 提供一键加速、性能优化、实时监控等功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameBoosterScreen(
    onNavigateBack: () -> Unit,
    viewModel: GameBoosterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    // 监听事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GameBoosterEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "游戏加速",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 设置 */ }) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 加速仪表盘
            item {
                BoostGaugeSection(
                    isBoosting = uiState.isBoosting,
                    boostProgress = uiState.boostProgress,
                    onBoostClick = { viewModel.toggleBoost() }
                )
            }

            // 实时状态
            item {
                RealTimeStatsSection(
                    ramUsage = uiState.ramUsage,
                    cpuTemp = uiState.cpuTemp,
                    networkLatency = uiState.networkLatency
                )
            }

            // 优化卡片
            item {
                OptimizationCardsSection(
                    memoryOptimized = uiState.memoryOptimized,
                    cpuOptimized = uiState.cpuOptimized,
                    networkOptimized = uiState.networkOptimized,
                    doNotDisturb = uiState.doNotDisturb,
                    onMemoryOptimize = { viewModel.optimizeMemory() },
                    onCpuOptimize = { viewModel.optimizeCpu() },
                    onNetworkOptimize = { viewModel.optimizeNetwork() },
                    onToggleDoNotDisturb = { viewModel.toggleDoNotDisturb() }
                )
            }

            // 游戏模式开关
            item {
                GameModeCard(
                    isEnabled = uiState.gameModeEnabled,
                    onToggle = { viewModel.toggleGameMode() }
                )
            }

            // 优化前后对比
            if (uiState.showComparison) {
                item {
                    BeforeAfterComparison(
                        beforeRam = uiState.beforeRam,
                        afterRam = uiState.afterRam,
                        beforeTemp = uiState.beforeTemp,
                        afterTemp = uiState.afterTemp
                    )
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 加速仪表盘区域
 */
@Composable
private fun BoostGaugeSection(
    isBoosting: Boolean,
    boostProgress: Float,
    onBoostClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            // 外圈进度
            CircularProgressIndicator(
                progress = boostProgress,
                modifier = Modifier.size(220.dp),
                color = if (isBoosting) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round
            )

            // 内圈装饰
            CircularProgressIndicator(
                progress = 1f,
                modifier = Modifier.size(180.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                strokeWidth = 2.dp,
                trackColor = Color.Transparent
            )

            // 中心按钮
            BoostButton(
                isBoosting = isBoosting,
                onClick = onBoostClick
            )
        }

        // 状态文字
        AnimatedContent(
            targetState = isBoosting,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                fadeOut(animationSpec = tween(300))
            },
            label = "boost_status"
        ) { boosting ->
            if (boosting) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "加速中",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    )
                    Text(
                        text = "游戏性能已优化",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "点击加速",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "一键优化游戏性能",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 加速按钮
 */
@Composable
private fun BoostButton(
    isBoosting: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150)
    )

    // 脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isBoosting) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        modifier = Modifier
            .size(140.dp)
            .scale(scale * pulseScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = if (isBoosting) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (isBoosting) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已加速",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "加速",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * 实时状态区域
 */
@Composable
private fun RealTimeStatsSection(
    ramUsage: Int,
    cpuTemp: Float,
    networkLatency: Int
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "实时状态",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                RealTimeStatItem(
                    icon = Icons.Default.Memory,
                    label = "内存使用",
                    value = "$ramUsage%",
                    color = when {
                        ramUsage < 60 -> Color(0xFF4CAF50)
                        ramUsage < 80 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    progress = ramUsage / 100f
                )
                RealTimeStatItem(
                    icon = Icons.Default.DeviceThermostat,
                    label = "CPU温度",
                    value = "${cpuTemp.toInt()}°C",
                    color = when {
                        cpuTemp < 45 -> Color(0xFF4CAF50)
                        cpuTemp < 60 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    progress = cpuTemp / 100f
                )
                RealTimeStatItem(
                    icon = Icons.Default.NetworkCheck,
                    label = "网络延迟",
                    value = "${networkLatency}ms",
                    color = when {
                        networkLatency < 50 -> Color(0xFF4CAF50)
                        networkLatency < 100 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    progress = (networkLatency / 200f).coerceIn(0f, 1f)
                )
            }
        }
    }
}

/**
 * 实时状态项
 */
@Composable
private fun RealTimeStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    progress: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 进度条
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.width(60.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

/**
 * 优化卡片区域
 */
@Composable
private fun OptimizationCardsSection(
    memoryOptimized: Boolean,
    cpuOptimized: Boolean,
    networkOptimized: Boolean,
    doNotDisturb: Boolean,
    onMemoryOptimize: () -> Unit,
    onCpuOptimize: () -> Unit,
    onNetworkOptimize: () -> Unit,
    onToggleDoNotDisturb: () -> Unit
) {
    Column {
        Text(
            text = "优化选项",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptimizationCard(
                icon = Icons.Default.CleaningServices,
                title = "内存优化",
                subtitle = "清理后台",
                isOptimized = memoryOptimized,
                onClick = onMemoryOptimize,
                modifier = Modifier.weight(1f)
            )
            OptimizationCard(
                icon = Icons.Default.Speed,
                title = "CPU加速",
                subtitle = "性能模式",
                isOptimized = cpuOptimized,
                onClick = onCpuOptimize,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptimizationCard(
                icon = Icons.Default.NetworkCheck,
                title = "网络优化",
                subtitle = "降低延迟",
                isOptimized = networkOptimized,
                onClick = onNetworkOptimize,
                modifier = Modifier.weight(1f)
            )
            OptimizationCard(
                icon = Icons.Default.DoNotDisturbOn,
                title = "防打扰",
                subtitle = "屏蔽通知",
                isOptimized = doNotDisturb,
                onClick = onToggleDoNotDisturb,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 优化卡片
 */
@Composable
private fun OptimizationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isOptimized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(150)
    )

    val backgroundColor = if (isOptimized) {
        Color(0xFF4CAF50).copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    val iconColor = if (isOptimized) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .height(100.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (isOptimized) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 游戏模式卡片
 */
@Composable
private fun GameModeCard(
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = if (isEnabled) Color(0xFF4CAF50).copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (isEnabled) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "游戏模式",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = if (isEnabled) "已开启 - 自动优化游戏性能"
                        else "已关闭 - 点击开启",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50),
                    checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                )
            )
        }
    }
}

/**
 * 优化前后对比
 */
@Composable
private fun BeforeAfterComparison(
    beforeRam: Int,
    afterRam: Int,
    beforeTemp: Float,
    afterTemp: Float
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(animationSpec = tween(500)) + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "优化效果",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 内存对比
                ComparisonBar(
                    label = "内存占用",
                    beforeValue = "$beforeRam%",
                    afterValue = "$afterRam%",
                    beforeProgress = beforeRam / 100f,
                    afterProgress = afterRam / 100f,
                    isLowerBetter = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 温度对比
                ComparisonBar(
                    label = "CPU温度",
                    beforeValue = "${beforeTemp.toInt()}°C",
                    afterValue = "${afterTemp.toInt()}°C",
                    beforeProgress = beforeTemp / 100f,
                    afterProgress = afterTemp / 100f,
                    isLowerBetter = true
                )
            }
        }
    }
}

/**
 * 对比进度条
 */
@Composable
private fun ComparisonBar(
    label: String,
    beforeValue: String,
    afterValue: String,
    beforeProgress: Float,
    afterProgress: Float,
    isLowerBetter: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 优化前
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "优化前",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(48.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = beforeProgress,
                modifier = Modifier.weight(1f),
                color = Color(0xFFFF9800),
                trackColor = Color(0xFFFF9800).copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = beforeValue,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFF9800),
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 优化后
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "优化后",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(48.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = afterProgress,
                modifier = Modifier.weight(1f),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = afterValue,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF4CAF50),
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

// ============== 预览数据 ==============

private fun getSampleBeforeAfter(): Pair<Int, Int> {
    return 78 to 45
}
