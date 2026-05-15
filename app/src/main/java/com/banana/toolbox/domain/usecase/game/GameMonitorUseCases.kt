package com.banana.toolbox.domain.usecase.game

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * 游戏监控用例
 * 提供实时性能监控功能，包括FPS、CPU、GPU、内存和温度监控
 */
@Singleton
class GameMonitorUseCases @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    // 性能数据流
    private val _performanceSnapshot = MutableStateFlow<PerformanceSnapshot?>(null)
    val performanceSnapshot: StateFlow<PerformanceSnapshot?> = _performanceSnapshot.asStateFlow()
    
    private val _fpsFlow = MutableStateFlow(0)
    val fpsFlow: StateFlow<Int> = _fpsFlow.asStateFlow()
    
    private val _cpuUsageFlow = MutableStateFlow(0f)
    val cpuUsageFlow: StateFlow<Float> = _cpuUsageFlow.asStateFlow()
    
    private val _gpuUsageFlow = MutableStateFlow(0f)
    val gpuUsageFlow: StateFlow<Float> = _gpuUsageFlow.asStateFlow()
    
    private val _memoryStatsFlow = MutableStateFlow<MemoryStats?>(null)
    val memoryStatsFlow: StateFlow<MemoryStats?> = _memoryStatsFlow.asStateFlow()
    
    private val _temperatureFlow = MutableStateFlow(0f)
    val temperatureFlow: StateFlow<Float> = _temperatureFlow.asStateFlow()

    private var isMonitoring = false
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var lastCpuTime: Long = 0
    private var lastAppCpuTime: Long = 0

    /**
     * 开始性能监控
     * 启动后台监控循环，定期采集性能数据
     */
    suspend fun startMonitoring(): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                if (isMonitoring) {
                    return@withContext Result.success(Unit)
                }

                isMonitoring = true
                lastFrameTime = System.nanoTime()
                frameCount = 0

                // 初始化CPU时间
                val appCpuTime = getAppCpuTime()
                val totalCpuTime = getTotalCpuTime()
                lastAppCpuTime = appCpuTime
                lastCpuTime = totalCpuTime

                // 监控循环
                while (isActive && isMonitoring) {
                    // 采集各项性能指标
                    val fps = getCurrentFPS()
                    val cpuUsage = getCpuUsage()
                    val gpuUsage = getGpuUsage()
                    val memoryStats = getMemoryUsage()
                    val temperature = getTemperature()

                    // 更新数据流
                    _fpsFlow.value = fps
                    _cpuUsageFlow.value = cpuUsage
                    _gpuUsageFlow.value = gpuUsage
                    _memoryStatsFlow.value = memoryStats
                    _temperatureFlow.value = temperature

                    // 创建性能快照
                    val snapshot = PerformanceSnapshot(
                        fps = fps,
                        cpu = cpuUsage,
                        gpu = gpuUsage,
                        memory = memoryStats,
                        temperature = temperature,
                        timestamp = System.currentTimeMillis()
                    )
                    _performanceSnapshot.value = snapshot

                    // 采样间隔 500ms
                    delay(500)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                isMonitoring = false
                Result.failure(e)
            }
        }
    }

    /**
     * 停止性能监控
     */
    fun stopMonitoring(): Result<Unit> {
        return try {
            isMonitoring = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取当前FPS
     * 通过帧时间计算实时帧率
     * @return 当前FPS
     */
    fun getCurrentFPS(): Int {
        val currentTime = System.nanoTime()
        val elapsedTime = (currentTime - lastFrameTime) / 1_000_000_000.0 // 转换为秒
        
        return if (elapsedTime > 0) {
            val fps = (frameCount / elapsedTime).roundToInt()
            frameCount = 0
            lastFrameTime = currentTime
            fps.coerceIn(0, 999)
        } else {
            0
        }
    }

    /**
     * 获取CPU使用率
     * 计算应用进程CPU占用率
     * @return CPU使用率 (0-100)
     */
    fun getCpuUsage(): Float {
        return try {
            val appCpuTime = getAppCpuTime()
            val totalCpuTime = getTotalCpuTime()

            if (lastCpuTime > 0 && lastAppCpuTime > 0) {
                val appCpuDelta = appCpuTime - lastAppCpuTime
                val totalCpuDelta = totalCpuTime - lastCpuTime

                if (totalCpuDelta > 0) {
                    val usage = (appCpuDelta.toFloat() / totalCpuDelta.toFloat()) * 100f
                    lastAppCpuTime = appCpuTime
                    lastCpuTime = totalCpuTime
                    return usage.coerceIn(0f, 100f)
                }
            }

            lastAppCpuTime = appCpuTime
            lastCpuTime = totalCpuTime
            0f
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * 获取GPU使用率
     * 读取GPU频率和负载信息
     * @return GPU使用率 (0-100)
     */
    fun getGpuUsage(): Float {
        return try {
            // 尝试读取GPU使用率（需要特定设备支持）
            val gpuStatsPaths = listOf(
                "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
                "/sys/class/misc/mali0/device/utilization",
                "/sys/class/misc/mali0/utilization",
                "/sys/module/ged/parameters/gpu_loading"
            )

            for (path in gpuStatsPaths) {
                val file = File(path)
                if (file.exists()) {
                    val value = file.readText().trim().toFloatOrNull()
                    if (value != null) {
                        return value.coerceIn(0f, 100f)
                    }
                }
            }

            // 如果无法读取，返回估算值
            0f
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * 获取内存使用情况
     * @return 内存统计信息
     */
    fun getMemoryUsage(): MemoryStats {
        return try {
            // 获取系统内存信息
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            val totalMemory = memoryInfo.totalMem
            val availableMemory = memoryInfo.availMem
            val usedMemory = totalMemory - availableMemory

            // 获取应用内存信息
            val runtime = Runtime.getRuntime()
            val appTotalMemory = runtime.totalMemory()
            val appFreeMemory = runtime.freeMemory()
            val appUsedMemory = appTotalMemory - appFreeMemory

            MemoryStats(
                total = totalMemory,
                used = usedMemory,
                free = availableMemory,
                appUsed = appUsedMemory,
                appTotal = appTotalMemory
            )
        } catch (e: Exception) {
            MemoryStats(0, 0, 0, 0, 0)
        }
    }

    /**
     * 获取设备温度
     * 读取电池和CPU温度传感器
     * @return 温度（摄氏度）
     */
    fun getTemperature(): Float {
        return try {
            var maxTemp = 0f

            // 读取电池温度
            val batteryTemp = readBatteryTemperature()
            if (batteryTemp > maxTemp) maxTemp = batteryTemp

            // 读取CPU温度
            val cpuTemp = readCpuTemperature()
            if (cpuTemp > maxTemp) maxTemp = cpuTemp

            // 读取GPU温度
            val gpuTemp = readGpuTemperature()
            if (gpuTemp > maxTemp) maxTemp = gpuTemp

            maxTemp
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * 检查监控是否运行中
     */
    fun isMonitoring(): Boolean = isMonitoring

    /**
     * 记录帧绘制
     * 在游戏渲染循环中调用以计算FPS
     */
    fun recordFrame() {
        frameCount++
    }

    // 私有辅助方法

    private fun getAppCpuTime(): Long {
        return try {
            val pid = Process.myPid()
            val statFile = File("/proc/$pid/stat")
            if (!statFile.exists()) return 0L

            val content = statFile.readText()
            val parts = content.split(" ")
            
            // utime + stime (用户态时间 + 内核态时间)
            if (parts.size > 14) {
                val utime = parts[13].toLongOrNull() ?: 0L
                val stime = parts[14].toLongOrNull() ?: 0L
                utime + stime
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun getTotalCpuTime(): Long {
        return try {
            val statFile = File("/proc/stat")
            if (!statFile.exists()) return 0L

            val reader = RandomAccessFile(statFile, "r")
            val line = reader.readLine() ?: return 0L
            reader.close()

            // 解析第一行 "cpu  user nice system idle iowait irq softirq steal guest guest_nice"
            val parts = line.split(" ").filter { it.isNotEmpty() }
            if (parts.size > 1 && parts[0] == "cpu") {
                var total = 0L
                for (i in 1 until parts.size) {
                    total += parts[i].toLongOrNull() ?: 0L
                }
                total
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun readBatteryTemperature(): Float {
        return try {
            val batteryTempPath = "/sys/class/power_supply/battery/temp"
            val file = File(batteryTempPath)
            if (file.exists()) {
                val temp = file.readText().trim().toFloatOrNull() ?: 0f
                // 通常电池温度以0.1度为单位存储
                temp / 10f
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    private fun readCpuTemperature(): Float {
        return try {
            val cpuTempPaths = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp",
                "/sys/devices/platform/omap/omap_temp_sensor.0/temperature"
            )

            var maxTemp = 0f
            for (path in cpuTempPaths) {
                val file = File(path)
                if (file.exists()) {
                    val temp = file.readText().trim().toFloatOrNull() ?: 0f
                    // 温度通常以千分之一度存储
                    val actualTemp = if (temp > 1000) temp / 1000f else temp
                    if (actualTemp > maxTemp) maxTemp = actualTemp
                }
            }
            maxTemp
        } catch (e: Exception) {
            0f
        }
    }

    private fun readGpuTemperature(): Float {
        return try {
            val gpuTempPaths = listOf(
                "/sys/class/thermal/thermal_zone10/temp",
                "/sys/class/kgsl/kgsl-3d0/temp",
                "/sys/class/misc/mali0/device/temp"
            )

            for (path in gpuTempPaths) {
                val file = File(path)
                if (file.exists()) {
                    val temp = file.readText().trim().toFloatOrNull() ?: 0f
                    return if (temp > 1000) temp / 1000f else temp
                }
            }
            0f
        } catch (e: Exception) {
            0f
        }
    }
}

/**
 * 内存统计信息数据类
 * @property total 总内存（字节）
 * @property used 已使用内存（字节）
 * @property free 可用内存（字节）
 * @property appUsed 应用使用内存（字节）
 * @property appTotal 应用分配内存（字节）
 */
data class MemoryStats(
    val total: Long,
    val used: Long,
    val free: Long,
    val appUsed: Long = 0,
    val appTotal: Long = 0
) {
    /**
     * 获取系统内存使用率（百分比）
     */
    fun getSystemUsagePercent(): Float {
        return if (total > 0) {
            (used.toFloat() / total.toFloat()) * 100f
        } else {
            0f
        }
    }

    /**
     * 获取应用内存使用率（百分比）
     */
    fun getAppUsagePercent(): Float {
        return if (appTotal > 0) {
            (appUsed.toFloat() / appTotal.toFloat()) * 100f
        } else {
            0f
        }
    }

    /**
     * 获取格式化的总内存
     */
    fun getFormattedTotal(): String = formatBytes(total)

    /**
     * 获取格式化的已使用内存
     */
    fun getFormattedUsed(): String = formatBytes(used)

    /**
     * 获取格式化的可用内存
     */
    fun getFormattedFree(): String = formatBytes(free)

    /**
     * 获取格式化的应用使用内存
     */
    fun getFormattedAppUsed(): String = formatBytes(appUsed)

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }
}

/**
 * 性能快照数据类
 * @property fps 当前帧率
 * @property cpu CPU使用率（0-100）
 * @property gpu GPU使用率（0-100）
 * @property memory 内存统计
 * @property temperature 设备温度（摄氏度）
 * @property timestamp 时间戳
 */
data class PerformanceSnapshot(
    val fps: Int,
    val cpu: Float,
    val gpu: Float,
    val memory: MemoryStats,
    val temperature: Float,
    val timestamp: Long
) {
    /**
     * 获取格式化的FPS字符串
     */
    fun getFormattedFPS(): String = "${fps} FPS"

    /**
     * 获取格式化的CPU使用率
     */
    fun getFormattedCPU(): String = String.format("%.1f%%", cpu)

    /**
     * 获取格式化的GPU使用率
     */
    fun getFormattedGPU(): String = String.format("%.1f%%", gpu)

    /**
     * 获取格式化的温度
     */
    fun getFormattedTemperature(): String = String.format("%.1f°C", temperature)

    /**
     * 获取性能等级
     */
    fun getPerformanceLevel(): PerformanceLevel {
        return when {
            fps >= 55 && cpu < 70 && temperature < 45 -> PerformanceLevel.EXCELLENT
            fps >= 45 && cpu < 80 && temperature < 50 -> PerformanceLevel.GOOD
            fps >= 30 && cpu < 90 && temperature < 55 -> PerformanceLevel.NORMAL
            else -> PerformanceLevel.POOR
        }
    }
}

/**
 * 性能等级枚举
 */
enum class PerformanceLevel {
    EXCELLENT, // 优秀
    GOOD,      // 良好
    NORMAL,    // 一般
    POOR       // 较差
}
