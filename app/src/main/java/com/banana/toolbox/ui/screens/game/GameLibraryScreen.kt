package com.banana.toolbox.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.UninstallDesktop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.banana.toolbox.domain.usecase.game.GameLauncherUseCases
import kotlinx.coroutines.launch

/**
 * 游戏库界面
 * 展示所有已安装游戏的网格布局，支持分类筛选和搜索
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLibraryScreen(
    onNavigateBack: () -> Unit,
    viewModel: GameLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullToRefreshState()

    // 处理下拉刷新
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
            pullRefreshState.endRefresh()
        }
    }

    // 监听事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GameLibraryEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is GameLibraryEvent.LaunchGame -> {
                    // 游戏启动逻辑已在ViewModel中处理
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "游戏库",
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
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(Icons.Default.GridView, "切换视图")
                    }
                    IconButton(onClick = { viewModel.toggleFilterMenu() }) {
                        Icon(Icons.Default.FilterList, "筛选")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 搜索栏
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // 分类筛选
                CategoryFilterChips(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { viewModel.selectCategory(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // 游戏网格
                if (uiState.isLoading) {
                    LoadingGridPlaceholder()
                } else if (uiState.filteredGames.isEmpty()) {
                    EmptyState(
                        isSearching = uiState.searchQuery.isNotEmpty(),
                        onClearSearch = { viewModel.updateSearchQuery("") }
                    )
                } else {
                    GameGrid(
                        games = uiState.filteredGames,
                        onGameClick = { game ->
                            viewModel.launchGame(game.packageName)
                        },
                        onGameLongClick = { game ->
                            viewModel.showGameOptions(game)
                        }
                    )
                }
            }

            // 下拉刷新指示器
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // 游戏选项底部弹窗
    if (uiState.selectedGame != null) {
        GameOptionsBottomSheet(
            game = uiState.selectedGame!!,
            onDismiss = { viewModel.dismissGameOptions() },
            onFavorite = { viewModel.toggleFavorite(it) },
            onUninstall = { viewModel.uninstallGame(it) },
            onAddToDesktop = { viewModel.addToDesktop(it) }
        )
    }
}

/**
 * 搜索栏
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("搜索游戏...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "清除",
                        modifier = Modifier.graphicsLayer { rotationZ = 180f }
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            unfocusedBorderColor = Color.Transparent
        )
    )
}

/**
 * 分类筛选Chip
 */
@Composable
private fun CategoryFilterChips(
    selectedCategory: GameCategory?,
    onCategorySelected: (GameCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        null to "全部",
        GameCategory.MOBA to "MOBA",
        GameCategory.FPS to "FPS",
        GameCategory.RPG to "RPG",
        GameCategory.STRATEGY to "策略",
        GameCategory.CASUAL to "休闲",
        GameCategory.ACTION to "动作"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (category, label) ->
            val isSelected = selectedCategory == category
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

/**
 * 游戏网格
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameGrid(
    games: List<GameItem>,
    onGameClick: (GameItem) -> Unit,
    onGameLongClick: (GameItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(games, key = { it.packageName }) { game ->
            GameCard(
                game = game,
                onClick = { onGameClick(game) },
                onLongClick = { onGameLongClick(game) },
                modifier = Modifier.animateItemPlacement(
                    animationSpec = tween(300)
                )
            )
        }
    }
}

/**
 * 游戏卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameCard(
    game: GameItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150)
    )

    ElevatedCard(
        modifier = modifier
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            // 游戏图标
            Box(
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = game.categoryColor.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = game.categoryColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // 收藏标记
                if (game.isFavorite) {
                    Surface(
                        modifier = Modifier
                            .size(20.dp)
                            .offset(x = 4.dp, y = (-4).dp),
                        shape = CircleShape,
                        color = Color(0xFFE91E63)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 游戏名称
            Text(
                text = game.name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // 分类标签
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = game.categoryColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = game.categoryLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = game.categoryColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * 游戏选项底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameOptionsBottomSheet(
    game: GameItem,
    onDismiss: () -> Unit,
    onFavorite: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onAddToDesktop: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // 游戏信息头部
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = game.categoryColor.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = game.categoryColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = game.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = game.categoryLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // 操作选项
            GameOptionItem(
                icon = if (game.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (game.isFavorite) "取消收藏" else "添加收藏",
                color = Color(0xFFE91E63),
                onClick = {
                    onFavorite(game.packageName)
                    onDismiss()
                }
            )

            GameOptionItem(
                icon = Icons.Default.AddToHomeScreen,
                label = "添加到桌面",
                color = MaterialTheme.colorScheme.primary,
                onClick = {
                    onAddToDesktop(game.packageName)
                    onDismiss()
                }
            )

            GameOptionItem(
                icon = Icons.Default.UninstallDesktop,
                label = "卸载游戏",
                color = MaterialTheme.colorScheme.error,
                onClick = {
                    onUninstall(game.packageName)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 游戏选项项
 */
@Composable
private fun GameOptionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(150)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = color
        )
    }
}

/**
 * 加载中占位符
 */
@Composable
private fun LoadingGridPlaceholder() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(9) {
            ShimmerGameCard()
        }
    }
}

/**
 * 骨架屏游戏卡片
 */
@Composable
private fun ShimmerGameCard() {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = shimmerColors,
                            start = androidx.compose.ui.geometry.Offset(
                                translateAnim - 100f,
                                translateAnim - 100f
                            ),
                            end = androidx.compose.ui.geometry.Offset(
                                translateAnim,
                                translateAnim
                            )
                        )
                    )
            )
        }
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyState(
    isSearching: Boolean,
    onClearSearch: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSearching) Icons.Default.Search else Icons.Default.Games,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isSearching) "未找到匹配的游戏" else "暂无游戏",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isSearching) "尝试其他关键词" else "安装游戏后将自动显示",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            if (isSearching) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onClearSearch) {
                    Text("清除搜索")
                }
            }
        }
    }
}

// ============== 数据类 ==============

enum class GameCategory {
    MOBA, FPS, RPG, STRATEGY, CASUAL, ACTION
}

data class GameItem(
    val packageName: String,
    val name: String,
    val category: GameCategory,
    val isFavorite: Boolean,
    val playTime: Long = 0
) {
    val categoryLabel: String
        get() = when (category) {
            GameCategory.MOBA -> "MOBA"
            GameCategory.FPS -> "FPS"
            GameCategory.RPG -> "RPG"
            GameCategory.STRATEGY -> "策略"
            GameCategory.CASUAL -> "休闲"
            GameCategory.ACTION -> "动作"
        }

    val categoryColor: Color
        get() = when (category) {
            GameCategory.MOBA -> Color(0xFF667eea)
            GameCategory.FPS -> Color(0xFFf093fb)
            GameCategory.RPG -> Color(0xFF4facfe)
            GameCategory.STRATEGY -> Color(0xFF43e97b)
            GameCategory.CASUAL -> Color(0xFFFFB74D)
            GameCategory.ACTION -> Color(0xFFff6b6b)
        }
}
