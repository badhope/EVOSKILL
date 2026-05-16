package com.banana.toolbox.ui.screens.game

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banana.toolbox.domain.usecase.game.GameLauncherUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 游戏库ViewModel
 * 管理游戏库界面的状态和业务逻辑
 */
@HiltViewModel
class GameLibraryViewModel @Inject constructor(
    private val gameLauncherUseCases: GameLauncherUseCases
) : ViewModel() {

    private val _uiState = mutableStateOf(GameLibraryUiState())
    val uiState: State<GameLibraryUiState> = _uiState

    private val _events = MutableSharedFlow<GameLibraryEvent>()
    val events: SharedFlow<GameLibraryEvent> = _events.asSharedFlow()

    init {
        loadGames()
    }

    /**
     * 加载游戏列表
     */
    private fun loadGames() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 模拟加载延迟
            delay(600)

            // 从用例获取已安装游戏
            val result = gameLauncherUseCases.scanInstalledGames()

            val games = if (result.isSuccess) {
                result.getOrNull()?.map { gameInfo ->
                    GameItem(
                        packageName = gameInfo.packageName,
                        name = gameInfo.name,
                        category = mapCategory(gameInfo.category),
                        isFavorite = gameInfo.isFavorite,
                        playTime = gameInfo.playTime
                    )
                } ?: emptyList()
            } else {
                // 返回示例数据
                getSampleGames()
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                allGames = games,
                filteredGames = games
            )
        }
    }

    /**
     * 更新搜索查询
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    /**
     * 选择分类
     */
    fun selectCategory(category: GameCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters()
    }

    /**
     * 应用筛选条件
     */
    private fun applyFilters() {
        val currentState = _uiState.value
        var filtered = currentState.allGames

        // 应用分类筛选
        currentState.selectedCategory?.let { category ->
            filtered = filtered.filter { it.category == category }
        }

        // 应用搜索筛选
        if (currentState.searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(currentState.searchQuery, ignoreCase = true)
            }
        }

        _uiState.value = currentState.copy(filteredGames = filtered)
    }

    /**
     * 启动游戏
     */
    fun launchGame(packageName: String) {
        viewModelScope.launch {
            _events.emit(GameLibraryEvent.ShowSnackbar("正在启动游戏..."))

            val result = gameLauncherUseCases.launchGame(packageName)

            if (result.isSuccess) {
                _events.emit(GameLibraryEvent.ShowSnackbar("游戏已启动"))
                _events.emit(GameLibraryEvent.LaunchGame(packageName))
            } else {
                _events.emit(
                    GameLibraryEvent.ShowSnackbar(
                        result.exceptionOrNull()?.message ?: "启动失败"
                    )
                )
            }
        }
    }

    /**
     * 显示游戏选项
     */
    fun showGameOptions(game: GameItem) {
        _uiState.value = _uiState.value.copy(selectedGame = game)
    }

    /**
     * 关闭游戏选项
     */
    fun dismissGameOptions() {
        _uiState.value = _uiState.value.copy(selectedGame = null)
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(packageName: String) {
        val game = _uiState.value.allGames.find { it.packageName == packageName }
        game?.let {
            val newFavoriteState = !it.isFavorite

            if (newFavoriteState) {
                gameLauncherUseCases.addGameToFavorites(packageName)
            } else {
                gameLauncherUseCases.removeGameFromFavorites(packageName)
            }

            // 更新游戏列表
            val updatedGames = _uiState.value.allGames.map { g ->
                if (g.packageName == packageName) g.copy(isFavorite = newFavoriteState) else g
            }

            _uiState.value = _uiState.value.copy(
                allGames = updatedGames,
                selectedGame = null
            )

            applyFilters()

            viewModelScope.launch {
                _events.emit(
                    GameLibraryEvent.ShowSnackbar(
                        if (newFavoriteState) "已添加到收藏" else "已取消收藏"
                    )
                )
            }
        }
    }

    /**
     * 卸载游戏
     */
    fun uninstallGame(packageName: String) {
        viewModelScope.launch {
            _events.emit(GameLibraryEvent.ShowSnackbar("准备卸载游戏..."))
            // 实际卸载逻辑需要系统权限，这里仅作示例
            _events.emit(GameLibraryEvent.ShowSnackbar("请前往系统设置卸载"))
        }
    }

    /**
     * 添加到桌面
     */
    fun addToDesktop(packageName: String) {
        viewModelScope.launch {
            _events.emit(GameLibraryEvent.ShowSnackbar("已添加到桌面"))
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadGames()
    }

    /**
     * 切换视图模式
     */
    fun toggleViewMode() {
        // 切换网格/列表视图
        viewModelScope.launch {
            _events.emit(GameLibraryEvent.ShowSnackbar("切换视图模式"))
        }
    }

    /**
     * 切换筛选菜单
     */
    fun toggleFilterMenu() {
        // 显示更多筛选选项
    }

    // ============== 私有辅助方法 ==============

    private fun mapCategory(category: GameLauncherUseCases.GameCategory): GameCategory {
        return when (category) {
            GameLauncherUseCases.GameCategory.MOBA -> GameCategory.MOBA
            GameLauncherUseCases.GameCategory.FPS -> GameCategory.FPS
            GameLauncherUseCases.GameCategory.RPG -> GameCategory.RPG
            GameLauncherUseCases.GameCategory.STRATEGY -> GameCategory.STRATEGY
            GameLauncherUseCases.GameCategory.CASUAL -> GameCategory.CASUAL
            GameLauncherUseCases.GameCategory.ACTION -> GameCategory.ACTION
        }
    }

    private fun getSampleGames(): List<GameItem> {
        return listOf(
            GameItem("com.tencent.tmgp.sgame", "王者荣耀", GameCategory.MOBA, true, 3600000 * 120),
            GameItem("com.tencent.tmgp.pubgmhd", "和平精英", GameCategory.FPS, true, 3600000 * 80),
            GameItem("com.miHoYo.GenshinImpact", "原神", GameCategory.RPG, false, 3600000 * 200),
            GameItem("com.tencent.lolm", "英雄联盟手游", GameCategory.MOBA, false, 3600000 * 50),
            GameItem("com.netease.onmyoji", "阴阳师", GameCategory.RPG, true, 3600000 * 300),
            GameItem("com.supercell.clashroyale", "皇室战争", GameCategory.STRATEGY, false, 3600000 * 40),
            GameItem("com.kiloo.subwaysurf", "地铁跑酷", GameCategory.CASUAL, false, 3600000 * 20),
            GameItem("com.ea.game.pvz2_row", "植物大战僵尸2", GameCategory.STRATEGY, false, 3600000 * 30),
            GameItem("com.tencent.KiHan", "火影忍者", GameCategory.ACTION, false, 3600000 * 60),
            GameItem("com.netease.hyxd", "荒野行动", GameCategory.FPS, false, 3600000 * 25),
            GameItem("com.happyelements.AndroidAnimal", "开心消消乐", GameCategory.CASUAL, true, 3600000 * 150),
            GameItem("com.tencent.qt.sns", "QQ飞车", GameCategory.ACTION, false, 3600000 * 35)
        )
    }
}

/**
 * 游戏库UI状态
 */
data class GameLibraryUiState(
    val isLoading: Boolean = false,
    val allGames: List<GameItem> = emptyList(),
    val filteredGames: List<GameItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: GameCategory? = null,
    val selectedGame: GameItem? = null,
    val isGridView: Boolean = true
)

/**
 * 游戏库事件
 */
sealed class GameLibraryEvent {
    data class ShowSnackbar(val message: String) : GameLibraryEvent()
    data class LaunchGame(val packageName: String) : GameLibraryEvent()
}
