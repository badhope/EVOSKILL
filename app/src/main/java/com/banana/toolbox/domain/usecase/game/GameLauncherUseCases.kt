package com.banana.toolbox.domain.usecase.game

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 游戏启动器用例
 * 管理游戏库、游戏启动、收藏夹和游戏时长统计
 */
@Singleton
class GameLauncherUseCases @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val packageManager: PackageManager = context.packageManager
    private val gamePreferences = context.getSharedPreferences("game_launcher_prefs", Context.MODE_PRIVATE)

    /**
     * 游戏类别枚举
     */
    enum class GameCategory {
        MOBA,       // 多人在线战术竞技
        FPS,        // 第一人称射击
        RPG,        // 角色扮演
        STRATEGY,   // 策略
        CASUAL,     // 休闲
        ACTION      // 动作
    }

    /**
     * 扫描已安装的游戏
     * 通过识别游戏特征来筛选已安装的游戏应用
     * @return 游戏信息列表
     */
    suspend fun scanInstalledGames(): Result<List<GameInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
                val games = packages.mapNotNull { packageInfo ->
                    try {
                        val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                        
                        // 检查是否为游戏
                        if (!isGameApp(appInfo)) return@mapNotNull null
                        
                        val packageName = packageInfo.packageName
                        GameInfo(
                            packageName = packageName,
                            name = appInfo.loadLabel(packageManager).toString(),
                            icon = appInfo.loadIcon(packageManager),
                            category = detectGameCategory(packageName, appInfo),
                            isFavorite = isFavoriteGame(packageName),
                            playTime = getGamePlayTime(packageName),
                            lastPlayed = getLastPlayedTime(packageName)
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.playTime }
                
                Result.success(games)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 启动游戏
     * @param packageName 游戏包名
     * @return 启动结果
     */
    suspend fun launchGame(packageName: String): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                    ?: return@withContext Result.failure(Exception("无法找到游戏启动入口"))
                
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                
                // 更新最后游玩时间
                updateLastPlayedTime(packageName)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 添加游戏到收藏夹
     * @param packageName 游戏包名
     */
    fun addGameToFavorites(packageName: String) {
        gamePreferences.edit()
            .putBoolean("favorite_$packageName", true)
            .apply()
    }

    /**
     * 从收藏夹移除游戏
     * @param packageName 游戏包名
     */
    fun removeGameFromFavorites(packageName: String) {
        gamePreferences.edit()
            .putBoolean("favorite_$packageName", false)
            .apply()
    }

    /**
     * 获取游戏游玩时长
     * @param packageName 游戏包名
     * @return 游玩时长（毫秒）
     */
    fun getGamePlayTime(packageName: String): Long {
        return gamePreferences.getLong("playtime_$packageName", 0L)
    }

    /**
     * 记录游戏会话时长
     * @param packageName 游戏包名
     * @param duration 会话时长（毫秒）
     */
    fun recordGameSession(packageName: String, duration: Long) {
        val currentPlayTime = getGamePlayTime(packageName)
        gamePreferences.edit()
            .putLong("playtime_$packageName", currentPlayTime + duration)
            .apply()
    }

    /**
     * 检查是否为游戏应用
     */
    private fun isGameApp(appInfo: ApplicationInfo): Boolean {
        // 检查应用类别是否为游戏（Android 26+）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
                return true
            }
        }
        
        // 通过包名关键词识别游戏
        val packageName = appInfo.packageName.lowercase()
        val gameKeywords = listOf(
            "game", "play", "gaming", "moba", "fps", "rpg", "arena",
            "battle", "war", "legend", "hero", "king", "clash", "royale"
        )
        
        return gameKeywords.any { packageName.contains(it) }
    }

    /**
     * 检测游戏类别
     */
    private fun detectGameCategory(packageName: String, appInfo: ApplicationInfo): GameCategory {
        val name = packageName.lowercase()
        val appName = appInfo.loadLabel(packageManager).toString().lowercase()
        
        return when {
            name.contains("moba") || name.contains("arena") || 
            appName.contains("arena") || appName.contains("moba") -> GameCategory.MOBA
            
            name.contains("fps") || name.contains("shooter") || 
            name.contains("gun") || appName.contains("射击") -> GameCategory.FPS
            
            name.contains("rpg") || name.contains("role") || 
            appName.contains("角色") || appName.contains("rpg") -> GameCategory.RPG
            
            name.contains("strategy") || name.contains("war") || 
            appName.contains("策略") || appName.contains("战略") -> GameCategory.STRATEGY
            
            name.contains("casual") || name.contains("puzzle") || 
            appName.contains("休闲") || appName.contains("益智") -> GameCategory.CASUAL
            
            else -> GameCategory.ACTION
        }
    }

    /**
     * 检查是否为收藏游戏
     */
    private fun isFavoriteGame(packageName: String): Boolean {
        return gamePreferences.getBoolean("favorite_$packageName", false)
    }

    /**
     * 获取最后游玩时间
     */
    private fun getLastPlayedTime(packageName: String): Long {
        return gamePreferences.getLong("lastplayed_$packageName", 0L)
    }

    /**
     * 更新最后游玩时间
     */
    private fun updateLastPlayedTime(packageName: String) {
        gamePreferences.edit()
            .putLong("lastplayed_$packageName", System.currentTimeMillis())
            .apply()
    }
}

/**
 * 游戏信息数据类
 * @property packageName 包名
 * @property name 游戏名称
 * @property icon 游戏图标
 * @property category 游戏类别
 * @property isFavorite 是否收藏
 * @property playTime 游玩时长（毫秒）
 * @property lastPlayed 最后游玩时间戳
 */
data class GameInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable,
    val category: GameLauncherUseCases.GameCategory,
    val isFavorite: Boolean,
    val playTime: Long,
    val lastPlayed: Long
)
