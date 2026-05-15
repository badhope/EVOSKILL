package com.banana.toolbox.ui.screens.game

import android.graphics.drawable.Drawable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banana.toolbox.domain.usecase.game.GameInfo
import com.banana.toolbox.domain.usecase.game.GameLauncherUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 游戏中心ViewModel
 * 管理游戏中心界面的状态和业务逻辑
 */
@HiltViewModel
class GameCenterViewModel @Inject constructor(
    private val gameLauncherUseCases: GameLauncherUseCases
) : ViewModel() {

    private val _uiState = mutableStateOf(GameCenterUiState())
    val uiState: State<GameCenterUiState> = _uiState

    private val _events = MutableSharedFlow<GameCenterEvent>()
    val events: SharedFlow<GameCenterEvent> = _events.asSharedFlow()

    init {
        loadGameCenterData()
    }

    /**
     * 加载游戏中心数据
     */
    private fun loadGameCenterData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 模拟加载延迟
            delay(800)

            // 加载精选游戏
            val featuredGames = loadFeaturedGames()

            // 加载最近游戏
            val recentGames = loadRecentGames()

            // 加载统计数据
            val stats = loadGameStats()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                featuredGames = featuredGames,
                recentGames = recentGames,
                todayPlayTime = stats.todayPlayTime,
                weekPlayTime = stats.weekPlayTime,
                favoriteCount = stats.favoriteCount
            )
        }
    }

    /**
     * 加载精选游戏
     */
    private suspend fun loadFeaturedGames(): List<FeaturedGame> {
        return listOf(
            FeaturedGame(
                packageName = "com.example.game1",
                name = "王者荣耀",
                description = "5V5英雄公平对战手游，体验极致竞技乐趣",
                gradientStart = Color(0xFF667eea),
                gradientEnd = Color(0xFF764ba2)
            ),
            FeaturedGame(
                packageName = "com.example.game2",
                name = "和平精英",
                description = "战术竞技手游，百人同场竞技",
                gradientStart = Color(0xFFf093fb),
                gradientEnd = Color(0xFFf5576c)
            ),
            FeaturedGame(
                packageName = "com.example.game3",
                name = "原神",
                description = "开放世界冒险游戏，探索提瓦特大陆",
                gradientStart = Color(0xFF4facfe),
                gradientEnd = Color(0xFF00f2fe)
            ),
            FeaturedGame(
                packageName = "com.example.game4",
                name = "英雄联盟手游",
                description = "经典MOBA手游，随时随地开黑",
                gradientStart = Color(0xFF43e97b),
                gradientEnd = Color(0xFF38f9d7)
            )
        )
    }

    /**
     * 加载最近游戏
     */
    private suspend fun loadRecentGames(): List<RecentGame> {
        // 从用例获取已安装游戏
        val result = gameLauncherUseCases.scanInstalledGames()

        return if (result.isSuccess) {
            val games = result.getOrNull() ?: emptyList()
            games
                .filter { it.lastPlayed > 0 }
                .sortedByDescending { it.lastPlayed }
                .take(10)
                .map { game ->
                    RecentGame(
                        packageName = game.packageName,
                        name = game.name,
                        lastPlayed = formatLastPlayed(game.lastPlayed)
                    )
                }
        } else {
            // 返回示例数据
            listOf(
                RecentGame("com.game1", "王者荣耀", "2小时前"),
                RecentGame("com.game2", "和平精英", "昨天"),
                RecentGame("com.game3", "原神", "3天前"),
                RecentGame("com.game4", "英雄联盟", "1周前")
            )
        }
    }

    /**
     * 加载游戏统计
     */
    private suspend fun loadGameStats(): GameStats {
        // 从SharedPreferences获取统计数据
        val todayMillis = loadTodayPlayTime()
        val weekMillis = loadWeekPlayTime()
        val favorites = loadFavoriteCount()

        return GameStats(
            todayPlayTime = formatDuration(todayMillis),
            weekPlayTime = formatDuration(weekMillis),
            favoriteCount = favorites
        )
    }

    /**
     * 启动游戏
     */
    fun launchGame(packageName: String) {
        viewModelScope.launch {
            _events.emit(GameCenterEvent.ShowSnackbar("正在启动游戏..."))

            val result = gameLauncherUseCases.launchGame(packageName)

            if (result.isSuccess) {
                _events.emit(GameCenterEvent.ShowSnackbar("游戏已启动"))
            } else {
                _events.emit(
                    GameCenterEvent.ShowSnackbar(
                        result.exceptionOrNull()?.message ?: "启动失败"
                    )
                )
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadGameCenterData()
    }

    /**
     * 切换游戏收藏状态
     */
    fun toggleFavorite(packageName: String, isFavorite: Boolean) {
        if (isFavorite) {
            gameLauncherUseCases.addGameToFavorites(packageName)
        } else {
            gameLauncherUseCases.removeGameFromFavorites(packageName)
        }

        viewModelScope.launch {
            _events.emit(
                GameCenterEvent.ShowSnackbar(
                    if (isFavorite) "已添加到收藏" else "已取消收藏"
                )
            )
            // 刷新统计数据
            val stats = loadGameStats()
            _uiState.value = _uiState.value.copy(
                favoriteCount = stats.favoriteCount
            )
        }
    }

    // ============== 私有辅助方法 ==============

    private fun formatLastPlayed(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "刚刚"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}分钟前"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}小时前"
            diff < TimeUnit.DAYS.toMillis(2) -> "昨天"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}天前"
            diff < TimeUnit.DAYS.toMillis(30) -> "${TimeUnit.MILLISECONDS.toDays(diff) / 7}周前"
            else -> {
                val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
                sdf.format(java.util.Date(timestamp))
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

        return when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            minutes > 0 -> "${minutes}分钟"
            else -> "<1分钟"
        }
    }

    private fun loadTodayPlayTime(): Long {
        // 从SharedPreferences获取今日游戏时长
        // 实际实现中需要按日期存储
        return (2 * 60 * 60 * 1000) + (45 * 60 * 1000) // 2小时45分钟示例
    }

    private fun loadWeekPlayTime(): Long {
        // 从SharedPreferences获取本周游戏时长
        return (15 * 60 * 60 * 1000) + (30 * 60 * 1000) // 15小时30分钟示例
    }

    private fun loadFavoriteCount(): Int {
        // 从SharedPreferences获取收藏数量
        return 12 // 示例数据
    }
}

/**
 * 游戏中心UI状态
 */
data class GameCenterUiState(
    val isLoading: Boolean = false,
    val featuredGames: List<FeaturedGame> = emptyList(),
    val recentGames: List<RecentGame> = emptyList(),
    val todayPlayTime: String = "0分钟",
    val weekPlayTime: String = "0小时",
    val favoriteCount: Int = 0
)

/**
 * 游戏统计
 */
private data class GameStats(
    val todayPlayTime: String,
    val weekPlayTime: String,
    val favoriteCount: Int
)

/**
 * 游戏中心事件
 */
sealed class GameCenterEvent {
    data class ShowSnackbar(val message: String) : GameCenterEvent()
    data class NavigateToGame(val packageName: String) : GameCenterEvent()
}
