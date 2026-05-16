package com.banana.toolbox.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.banana.toolbox.ui.screens.appmanager.AppManagerScreen
import com.banana.toolbox.ui.screens.filemanager.FileManagerScreen
import com.banana.toolbox.ui.screens.home.HomeScreen
import com.banana.toolbox.ui.screens.network.NetworkToolsScreen
import com.banana.toolbox.ui.screens.tools.UtilityToolsScreen
import com.banana.toolbox.ui.screens.tools.converter.FileConverterScreen
import com.banana.toolbox.ui.screens.tools.crypto.CryptoScreen
import com.banana.toolbox.ui.screens.tools.cleaner.CleanerScreen
import com.banana.toolbox.ui.screens.tools.inspector.DeepInspectorScreen
import com.banana.toolbox.ui.screens.tools.generator.GeneratorScreen
import com.banana.toolbox.ui.screens.game.GameCenterScreen
import com.banana.toolbox.ui.screens.game.GameLibraryScreen
import com.banana.toolbox.ui.screens.game.GameBoosterScreen
import com.banana.toolbox.ui.screens.game.GameRecorderScreen
import com.banana.toolbox.ui.screens.game.GameAssistantScreen

/**
 * 主导航图
 */
@Composable
fun BananaNavigation(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = {
            BananaBottomBar(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToFileManager = { navController.navigate(Screen.FileManager.route) },
                    onNavigateToAppManager = { navController.navigate(Screen.AppManager.route) },
                    onNavigateToNetwork = { navController.navigate(Screen.NetworkTools.route) },
                    onNavigateToTools = { navController.navigate(Screen.UtilityTools.route) },
                    onNavigateToGameCenter = { navController.navigate(Screen.GameCenter.route) }
                )
            }
            composable(Screen.FileManager.route) {
                FileManagerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AppManager.route) {
                AppManagerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.NetworkTools.route) {
                NetworkToolsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.UtilityTools.route) {
                UtilityToolsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToConverter = { navController.navigate(Screen.FileConverter.route) },
                    onNavigateToCrypto = { navController.navigate(Screen.Crypto.route) },
                    onNavigateToCleaner = { navController.navigate(Screen.Cleaner.route) },
                    onNavigateToInspector = { navController.navigate(Screen.DeepInspector.route) },
                    onNavigateToGenerator = { navController.navigate(Screen.Generator.route) }
                )
            }
            // 扩展工具页面
            composable(Screen.FileConverter.route) {
                FileConverterScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Crypto.route) {
                CryptoScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Cleaner.route) {
                CleanerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.DeepInspector.route) {
                DeepInspectorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Generator.route) {
                GeneratorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            // 游戏中心页面
            composable(Screen.GameCenter.route) {
                GameCenterScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLibrary = { navController.navigate(Screen.GameLibrary.route) },
                    onNavigateToBooster = { navController.navigate(Screen.GameBooster.route) },
                    onNavigateToRecorder = { navController.navigate(Screen.GameRecorder.route) },
                    onNavigateToAssistant = { navController.navigate(Screen.GameAssistant.route) }
                )
            }
            composable(Screen.GameLibrary.route) {
                GameLibraryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.GameBooster.route) {
                GameBoosterScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.GameRecorder.route) {
                GameRecorderScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.GameAssistant.route) {
                GameAssistantScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * 底部导航栏
 */
@Composable
fun BananaBottomBar(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        BottomNavItem(Screen.Home.route, "首页", "home"),
        BottomNavItem(Screen.FileManager.route, "文件", "folder"),
        BottomNavItem(Screen.AppManager.route, "应用", "apps"),
        BottomNavItem(Screen.NetworkTools.route, "网络", "wifi"),
        BottomNavItem(Screen.UtilityTools.route, "工具", "build"),
        BottomNavItem(Screen.GameCenter.route, "游戏", "sports_esports")
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = getIconForName(item.iconName),
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val iconName: String
)

@Composable
fun getIconForName(name: String) = when (name) {
    "home" -> Icons.Default.Home
    "folder" -> androidx.compose.material.icons.Icons.Default.Folder
    "apps" -> androidx.compose.material.icons.Icons.Default.Apps
    "wifi" -> androidx.compose.material.icons.Icons.Default.Wifi
    "build" -> androidx.compose.material.icons.Icons.Default.Build
    "sports_esports" -> androidx.compose.material.icons.Icons.Default.Info
    else -> androidx.compose.material.icons.Icons.Default.Info
}
