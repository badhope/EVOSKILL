package com.banana.toolbox.ui.navigation

/**
 * 应用导航路由
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object FileManager : Screen("file_manager")
    object AppManager : Screen("app_manager")
    object NetworkTools : Screen("network_tools")
    object UtilityTools : Screen("utility_tools")
    object GameCenter : Screen("game_center")

    // 文件管理子页面
    object FileList : Screen("file_list/{path}") {
        fun createRoute(path: String) = "file_list/$path"
    }

    // 应用管理子页面
    object AppDetail : Screen("app_detail/{packageName}") {
        fun createRoute(packageName: String) = "app_detail/$packageName"
    }

    // 扩展工具页面
    object FileConverter : Screen("file_converter")
    object Crypto : Screen("crypto")
    object Cleaner : Screen("cleaner")
    object DeepInspector : Screen("deep_inspector")
    object Generator : Screen("generator")

    // 游戏中心子页面
    object GameLibrary : Screen("game_library")
    object GameBooster : Screen("game_booster")
    object GameRecorder : Screen("game_recorder")
    object GameAssistant : Screen("game_assistant")
}
