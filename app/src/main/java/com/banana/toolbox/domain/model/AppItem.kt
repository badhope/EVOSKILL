package com.banana.toolbox.domain.model

/**
 * 应用项领域模型
 */
data class AppItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val size: Long,
    val installTime: Long,
    val updateTime: Long,
    val isSystemApp: Boolean,
    val apkPath: String
) {
    /**
     * 格式化大小显示
     */
    fun getFormattedSize(): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            size >= gb -> "%.1f GB".format(size / gb)
            size >= mb -> "%.1f MB".format(size / mb)
            size >= kb -> "%.1f KB".format(size / kb)
            else -> "$size B"
        }
    }
}
