package com.banana.toolbox

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Banana Toolbox 应用入口
 * 
 * 一款多功能手机工具箱应用，集文件管理、应用管理、网络工具、实用小工具于一体。
 */
@HiltAndroidApp
class BananaToolboxApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 应用初始化逻辑
    }
}
