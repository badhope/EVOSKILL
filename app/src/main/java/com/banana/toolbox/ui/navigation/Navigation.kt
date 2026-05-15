package com.banana.toolbox.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.banana.toolbox.ui.screens.home.HomeScreen

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
                    onNavigateToTools = { navController.navigate(Screen.UtilityTools.route) }
                )
            }
            composable(Screen.FileManager.route) {
                // TODO: 文件管理页面
                PlaceholderScreen("文件管理")
            }
            composable(Screen.AppManager.route) {
                // TODO: 应用管理页面
                PlaceholderScreen("应用管理")
            }
            composable(Screen.NetworkTools.route) {
                // TODO: 网络工具页面
                PlaceholderScreen("网络工具")
            }
            composable(Screen.UtilityTools.route) {
                // TODO: 实用工具页面
                PlaceholderScreen("实用工具")
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
        BottomNavItem(Screen.UtilityTools.route, "工具", "build")
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
    "home" -> androidx.compose.material.icons.Icons.Default.Home
    "folder" -> androidx.compose.material.icons.Icons.Default.Folder
    "apps" -> androidx.compose.material.icons.Icons.Default.Apps
    "wifi" -> androidx.compose.material.icons.Icons.Default.Wifi
    "build" -> androidx.compose.material.icons.Icons.Default.Build
    else -> androidx.compose.material.icons.Icons.Default.Info
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
        )
    }
}
