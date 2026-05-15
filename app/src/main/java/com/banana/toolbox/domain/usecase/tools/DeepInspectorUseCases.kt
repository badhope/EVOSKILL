package com.banana.toolbox.domain.usecase.tools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 深层手机查看用例
 */
@Singleton
class DeepInspectorUseCases @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    // ==================== CPU 信息 ====================
    
    fun getCpuInfo(): CpuInfo {
        val cores = Runtime.getRuntime().availableProcessors()
        val abis = Build.SUPPORTED_ABIS.toList()
        
        // 读取 CPU 信息
        val cpuInfo = try {
            val process = Runtime.getRuntime().exec("cat /proc/cpuinfo")
            val reader = process.inputStream.bufferedReader()
            val info = reader.readText()
            reader.close()
            process.waitFor()
            parseCpuInfo(info)
        } catch (e: Exception) {
            CpuInfoModel()
        }
        
        // CPU 频率
        val maxFreq = try {
            File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").readText().trim().toLong() / 1000
        } catch (e: Exception) { 0 }
        
        val minFreq = try {
            File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq").readText().trim().toLong() / 1000
        } catch (e: Exception) { 0 }
        
        val currentFreq = try {
            File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq").readText().trim().toLong() / 1000
        } catch (e: Exception) { 0 }
        
        return CpuInfo(
            cores = cores,
            abis = abis,
            processor = cpuInfo.processor,
            hardware = cpuInfo.hardware,
            maxFreqKHz = maxFreq,
            minFreqKHz = minFreq,
            currentFreqKHz = currentFreq,
            governor = try {
                File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").readText().trim()
            } catch (e: Exception) { "Unknown" }
        )
    }
    
    private fun parseCpuInfo(info: String): CpuInfoModel {
        var processor = "Unknown"
        var hardware = "Unknown"
        
        info.lines().forEach { line ->
            when {
                line.startsWith("Hardware") -> hardware = line.substringAfter(":").trim()
                line.startsWith("Processor") -> processor = line.substringAfter(":").trim()
                line.startsWith("model name") -> processor = line.substringAfter(":").trim()
            }
        }
        
        return CpuInfoModel(processor = processor, hardware = hardware)
    }
    
    // ==================== 内存信息 ====================
    
    fun getMemoryInfo(): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        // 读取详细内存信息
        val memInfo = try {
            val process = Runtime.getRuntime().exec("cat /proc/meminfo")
            val reader = process.inputStream.bufferedReader()
            val info = reader.readText()
            reader.close()
            process.waitFor()
            parseMemInfo(info)
        } catch (e: Exception) {
            MemInfoModel()
        }
        
        val runtime = Runtime.getRuntime()
        
        return MemoryInfo(
            totalRam = memoryInfo.totalMem,
            availableRam = memoryInfo.availMem,
            usedRam = memoryInfo.totalMem - memoryInfo.availMem,
            isLowMemory = memoryInfo.lowMemory,
            threshold = memoryInfo.threshold,
            totalSwap = memInfo.swapTotal,
            freeSwap = memInfo.swapFree,
            jvmMaxMemory = runtime.maxMemory(),
            jvmTotalMemory = runtime.totalMemory(),
            jvmFreeMemory = runtime.freeMemory(),
            memTotal = memInfo.memTotal,
            memFree = memInfo.memFree,
            memAvailable = memInfo.memAvailable,
            buffers = memInfo.buffers,
            cached = memInfo.cached
        )
    }
    
    private fun parseMemInfo(info: String): MemInfoModel {
        var memTotal = 0L
        var memFree = 0L
        var memAvailable = 0L
        var buffers = 0L
        var cached = 0L
        var swapTotal = 0L
        var swapFree = 0L
        
        info.lines().forEach { line ->
            val parts = line.split(":")
            if (parts.size == 2) {
                val value = parts[1].trim().split(" ").first().toLongOrNull() ?: 0
                when (parts[0].trim()) {
                    "MemTotal" -> memTotal = value * 1024
                    "MemFree" -> memFree = value * 1024
                    "MemAvailable" -> memAvailable = value * 1024
                    "Buffers" -> buffers = value * 1024
                    "Cached" -> cached = value * 1024
                    "SwapTotal" -> swapTotal = value * 1024
                    "SwapFree" -> swapFree = value * 1024
                }
            }
        }
        
        return MemInfoModel(memTotal, memFree, memAvailable, buffers, cached, swapTotal, swapFree)
    }
    
    // ==================== 传感器列表 ====================
    
    fun getSensors(): List<SensorInfo> {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        
        return sensors.map { sensor ->
            SensorInfo(
                name = sensor.name,
                vendor = sensor.vendor,
                type = getSensorTypeName(sensor.type),
                version = sensor.version,
                maxRange = sensor.maximumRange,
                resolution = sensor.resolution,
                power = sensor.power
            )
        }.sortedBy { it.type }
    }
    
    private fun getSensorTypeName(type: Int): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> "加速度传感器"
            Sensor.TYPE_MAGNETIC_FIELD -> "磁场传感器"
            Sensor.TYPE_ORIENTATION -> "方向传感器"
            Sensor.TYPE_GYROSCOPE -> "陀螺仪"
            Sensor.TYPE_LIGHT -> "光线传感器"
            Sensor.TYPE_PRESSURE -> "气压传感器"
            Sensor.TYPE_TEMPERATURE -> "温度传感器"
            Sensor.TYPE_PROXIMITY -> "距离传感器"
            Sensor.TYPE_GRAVITY -> "重力传感器"
            Sensor.TYPE_LINEAR_ACCELERATION -> "线性加速度"
            Sensor.TYPE_ROTATION_VECTOR -> "旋转矢量"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "湿度传感器"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "环境温度"
            Sensor.TYPE_STEP_COUNTER -> "计步器"
            Sensor.TYPE_STEP_DETECTOR -> "步数检测"
            Sensor.TYPE_HEART_RATE -> "心率传感器"
            Sensor.TYPE_GAME_ROTATION_VECTOR -> "游戏旋转矢量"
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "地磁旋转矢量"
            else -> "未知传感器 ($type)"
        }
    }
    
    // ==================== 运行进程 ====================
    
    fun getRunningProcesses(): List<ProcessInfo> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = mutableListOf<ProcessInfo>()
        
        // 通过 ActivityManager 获取
        try {
            @Suppress("DEPRECATION")
            val runningProcesses = activityManager.runningAppProcesses
            runningProcesses?.forEach { process ->
                processes.add(ProcessInfo(
                    pid = process.pid,
                    processName = process.processName,
                    importance = getImportanceName(process.importance),
                    memory = try {
                        @Suppress("DEPRECATION")
                        val pids = intArrayOf(process.pid)
                        @Suppress("DEPRECATION")
                        val memInfo = activityManager.getProcessMemoryInfo(pids)
                        memInfo?.get(0)?.getTotalPss()?.toLong() ?: 0
                    } catch (e: Exception) { 0 }
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 通过 /proc 补充
        try {
            val procDir = File("/proc")
            procDir.listFiles()?.filter { it.name.matches(Regex("\\d+")) }?.forEach { dir ->
                val pid = dir.name.toInt()
                if (processes.none { it.pid == pid }) {
                    try {
                        val cmdline = File(dir, "cmdline").readText().trim().replace("\u0000", " ")
                        val status = File(dir, "status").readLines()
                        val memory = status.find { it.startsWith("VmRSS:") }
                            ?.substringAfter(":")?.trim()?.split(" ")?.first()?.toLongOrNull() ?: 0
                        
                        if (cmdline.isNotBlank()) {
                            processes.add(ProcessInfo(
                                pid = pid,
                                processName = cmdline,
                                importance = "Unknown",
                                memory = memory * 1024
                            ))
                        }
                    } catch (e: Exception) { }
                }
            }
        } catch (e: Exception) { }
        
        return processes.sortedByDescending { it.memory }
    }
    
    private fun getImportanceName(importance: Int): String {
        return when (importance) {
            100 -> "前台"
            200 -> "可见"
            300 -> "服务"
            400 -> "后台"
            500 -> "空"
            else -> "Unknown ($importance)"
        }
    }
    
    // ==================== 自启动应用 ====================
    
    fun getAutoStartApps(): List<AutoStartApp> {
        val apps = mutableListOf<AutoStartApp>()
        val pm = context.packageManager
        
        try {
            // 检查接收 BOOT_COMPLETED 的应用
            val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
            val receivers = pm.queryBroadcastReceivers(intent, 0)
            
            receivers?.forEach { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val appName = try {
                    pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
                } catch (e: Exception) { packageName }
                
                apps.add(AutoStartApp(
                    packageName = packageName,
                    appName = appName,
                    receiverName = resolveInfo.activityInfo.name,
                    isEnabled = resolveInfo.activityInfo.enabled
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return apps.sortedBy { it.appName.lowercase() }
    }
    
    // ==================== 电池健康 ====================
    
    fun getBatteryHealth(): BatteryHealth {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        
        // 电池容量（需要 root）
        val capacity = try {
            File("/sys/class/power_supply/battery/capacity").readText().trim().toIntOrNull() ?: level
        } catch (e: Exception) { level }
        
        // 电池设计容量
        val designCapacity = try {
            File("/sys/class/power_supply/battery/charge_full_design").readText().trim().toIntOrNull() ?: 0
        } catch (e: Exception) { 0 }
        
        val currentCapacity = try {
            File("/sys/class/power_supply/battery/charge_full").readText().trim().toIntOrNull() ?: 0
        } catch (e: Exception) { 0 }
        
        val batteryWear = if (designCapacity > 0 && currentCapacity > 0) {
            ((1 - currentCapacity.toFloat() / designCapacity) * 100).toInt()
        } else { 0 }
        
        return BatteryHealth(
            level = if (scale > 0) (level * 100 / scale) else 0,
            status = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
                BatteryManager.BATTERY_STATUS_FULL -> "已充满"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
                else -> "未知"
            },
            health = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
                BatteryManager.BATTERY_HEALTH_DEAD -> "已损坏"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "过压"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "故障"
                else -> "未知"
            },
            voltage = voltage / 1000.0, // mV -> V
            temperature = temperature / 10.0, // 0.1°C -> °C
            technology = technology,
            plugged = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC 充电器"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线充电"
                else -> "未连接"
            },
            designCapacity = designCapacity,
            currentCapacity = currentCapacity,
            batteryWear = batteryWear,
            cycleCount = try {
                File("/sys/class/power_supply/battery/cycle_count").readText().trim().toIntOrNull() ?: 0
            } catch (e: Exception) { 0 }
        )
    }
    
    // ==================== 存储详情 ====================
    
    fun getStorageDetails(): List<StorageDetail> {
        val storages = mutableListOf<StorageDetail>()
        
        // 内部存储
        val internalPath = Environment.getDataDirectory()
        val internalStat = StatFs(internalPath.path)
        storages.add(StorageDetail(
            path = "/storage/emulated/0",
            label = "内部存储",
            totalBytes = internalStat.totalBytes,
            availableBytes = internalStat.availableBytes,
            usedBytes = internalStat.totalBytes - internalStat.availableBytes,
            isRemovable = false
        ))
        
        // 外部存储（SD卡等）
        try {
            val externalDirs = context.getExternalFilesDirs(null)
            externalDirs.forEach { dir ->
                if (dir != null && dir.canRead()) {
                    val stat = StatFs(dir.path)
                    val isRemovable = Environment.isExternalStorageRemovable(dir)
                    storages.add(StorageDetail(
                        path = dir.absolutePath,
                        label = if (isRemovable) "SD 卡" else "外部存储",
                        totalBytes = stat.totalBytes,
                        availableBytes = stat.availableBytes,
                        usedBytes = stat.totalBytes - stat.availableBytes,
                        isRemovable = isRemovable
                    ))
                }
            }
        } catch (e: Exception) { }
        
        return storages
    }
    
    // ==================== 网络详细信息 ====================
    
    fun getNetworkDetails(): NetworkDetails {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo
        
        @Suppress("DEPRECATION")
        val wifiInfo = wifiManager.connectionInfo
        
        val ip = intToIp(dhcpInfo.ipAddress)
        val gateway = intToIp(dhcpInfo.gateway)
        val netmask = intToIp(dhcpInfo.netmask)
        val dns1 = intToIp(dhcpInfo.dns1)
        val dns2 = intToIp(dhcpInfo.dns2)
        val server = intToIp(dhcpInfo.serverAddress)
        val leaseDuration = dhcpInfo.leaseDuration
        
        return NetworkDetails(
            ipAddress = ip,
            gateway = gateway,
            netmask = netmask,
            dns1 = dns1,
            dns2 = dns2,
            dhcpServer = server,
            leaseDuration = leaseDuration,
            ssid = wifiInfo?.ssid?.replace("\"", "") ?: "未连接",
            bssid = wifiInfo?.bssid ?: "Unknown",
            linkSpeed = wifiInfo?.linkSpeed ?: 0,
            signalStrength = wifiInfo?.rssi ?: 0,
            frequency = wifiInfo?.frequency ?: 0,
            networkId = wifiInfo?.networkId ?: -1
        )
    }
    
    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}

// ==================== 数据类 ====================

data class CpuInfo(
    val cores: Int,
    val abis: List<String>,
    val processor: String,
    val hardware: String,
    val maxFreqKHz: Long,
    val minFreqKHz: Long,
    val currentFreqKHz: Long,
    val governor: String
)

data class CpuInfoModel(val processor: String = "Unknown", val hardware: String = "Unknown")

data class MemoryInfo(
    val totalRam: Long,
    val availableRam: Long,
    val usedRam: Long,
    val isLowMemory: Boolean,
    val threshold: Long,
    val totalSwap: Long,
    val freeSwap: Long,
    val jvmMaxMemory: Long,
    val jvmTotalMemory: Long,
    val jvmFreeMemory: Long,
    val memTotal: Long,
    val memFree: Long,
    val memAvailable: Long,
    val buffers: Long,
    val cached: Long
)

data class SensorInfo(
    val name: String,
    val vendor: String,
    val type: String,
    val version: Int,
    val maxRange: Float,
    val resolution: Float,
    val power: Float
)

data class ProcessInfo(
    val pid: Int,
    val processName: String,
    val importance: String,
    val memory: Long
)

data class AutoStartApp(
    val packageName: String,
    val appName: String,
    val receiverName: String,
    val isEnabled: Boolean
)

data class BatteryHealth(
    val level: Int,
    val status: String,
    val health: String,
    val voltage: Double,
    val temperature: Double,
    val technology: String,
    val plugged: String,
    val designCapacity: Int,
    val currentCapacity: Int,
    val batteryWear: Int,
    val cycleCount: Int
)

data class StorageDetail(
    val path: String,
    val label: String,
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val isRemovable: Boolean
)

data class NetworkDetails(
    val ipAddress: String,
    val gateway: String,
    val netmask: String,
    val dns1: String,
    val dns2: String,
    val dhcpServer: String,
    val leaseDuration: Int,
    val ssid: String,
    val bssid: String,
    val linkSpeed: Int,
    val signalStrength: Int,
    val frequency: Int,
    val networkId: Int
)

data class MemInfoModel(
    val memTotal: Long = 0,
    val memFree: Long = 0,
    val memAvailable: Long = 0,
    val buffers: Long = 0,
    val cached: Long = 0,
    val swapTotal: Long = 0,
    val swapFree: Long = 0
)
