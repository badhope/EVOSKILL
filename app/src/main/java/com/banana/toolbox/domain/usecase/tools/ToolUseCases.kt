package com.banana.toolbox.domain.usecase.tools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.Adler32
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 实用工具用例
 */
@Singleton
class ToolUseCases @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    // ==================== 二维码工具 ====================
    
    /**
     * 生成二维码
     */
    fun generateQRCode(content: String, size: Int = 512): Result<BitMatrix> {
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 2
            )
            val matrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            Result.success(matrix)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 单位换算 ====================
    
    object UnitConverter {
        
        // 长度单位换算（以米为基准）
        fun convertLength(value: Double, from: LengthUnit, to: LengthUnit): Double {
            return value * from.toMeter / to.toMeter
        }
        
        // 重量单位换算（以千克为基准）
        fun convertWeight(value: Double, from: WeightUnit, to: WeightUnit): Double {
            return value * from.toKg / to.toKg
        }
        
        // 温度单位换算
        fun convertTemperature(value: Double, from: TemperatureUnit, to: TemperatureUnit): Double {
            return when {
                from == to -> value
                from == TemperatureUnit.CELSIUS && to == TemperatureUnit.FAHRENHEIT -> value * 9 / 5 + 32
                from == TemperatureUnit.CELSIUS && to == TemperatureUnit.KELVIN -> value + 273.15
                from == TemperatureUnit.FAHRENHEIT && to == TemperatureUnit.CELSIUS -> (value - 32) * 5 / 9
                from == TemperatureUnit.FAHRENHEIT && to == TemperatureUnit.KELVIN -> (value - 32) * 5 / 9 + 273.15
                from == TemperatureUnit.KELVIN && to == TemperatureUnit.CELSIUS -> value - 273.15
                from == TemperatureUnit.KELVIN && to == TemperatureUnit.FAHRENHEIT -> (value - 273.15) * 9 / 5 + 32
                else -> value
            }
        }
        
        // 数据单位换算（以字节为基准）
        fun convertDataSize(value: Double, from: DataUnit, to: DataUnit): Double {
            return value * from.toBytes / to.toBytes
        }
    }
    
    enum class LengthUnit(val displayName: String, val toMeter: Double) {
        METER("米", 1.0),
        KILOMETER("千米", 1000.0),
        CENTIMETER("厘米", 0.01),
        MILLIMETER("毫米", 0.001),
        MILE("英里", 1609.344),
        YARD("码", 0.9144),
        FOOT("英尺", 0.3048),
        INCH("英寸", 0.0254)
    }
    
    enum class WeightUnit(val displayName: String, val toKg: Double) {
        KILOGRAM("千克", 1.0),
        GRAM("克", 0.001),
        MILLIGRAM("毫克", 0.000001),
        POUND("磅", 0.453592),
        OUNCE("盎司", 0.0283495),
        TON("吨", 1000.0)
    }
    
    enum class TemperatureUnit(val displayName: String) {
        CELSIUS("摄氏度"),
        FAHRENHEIT("华氏度"),
        KELVIN("开尔文")
    }
    
    enum class DataUnit(val displayName: String, val toBytes: Long) {
        BYTE("字节", 1),
        KILOBYTE("KB", 1024),
        MEGABYTE("MB", 1048576),
        GIGABYTE("GB", 1073741824),
        TERABYTE("TB", 1099511627776)
    }
    
    // ==================== 颜色工具 ====================
    
    /**
     * 颜色格式转换
     */
    fun parseColor(colorString: String): ColorInfo? {
        return try {
            when {
                colorString.startsWith("#") -> parseHexColor(colorString)
                colorString.startsWith("rgb") -> parseRgbColor(colorString)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseHexColor(hex: String): ColorInfo {
        val cleanHex = hex.removePrefix("#")
        val color = when (cleanHex.length) {
            3 -> {
                val r = cleanHex[0].toString().repeat(2)
                val g = cleanHex[1].toString().repeat(2)
                val b = cleanHex[2].toString().repeat(2)
                "FF$r$g$b"
            }
            6 -> "FF$cleanHex"
            8 -> cleanHex
            else -> "FF000000"
        }
        
        val r = color.substring(2, 4).toInt(16)
        val g = color.substring(4, 6).toInt(16)
        val b = color.substring(6, 8).toInt(16)
        val a = color.substring(0, 2).toInt(16)
        
        return ColorInfo(
            hex = "#${color.substring(2)}",
            rgba = "rgba($r, $g, $b, ${a / 255.0})",
            rgb = "rgb($r, $g, $b)",
            red = r,
            green = g,
            blue = b,
            alpha = a
        )
    }
    
    private fun parseRgbColor(rgb: String): ColorInfo {
        val match = Regex("""rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)""").find(rgb)
            ?: return ColorInfo("", "", "", 0, 0, 0, 255)
        
        val r = match.groupValues[1].toInt()
        val g = match.groupValues[2].toInt()
        val b = match.groupValues[3].toInt()
        val a = (match.groupValues.getOrNull(4)?.toDouble()?.times(255) ?: 255.0).toInt()
        
        return ColorInfo(
            hex = "#%02X%02X%02X".format(r, g, b),
            rgba = "rgba($r, $g, $b, ${a / 255.0})",
            rgb = "rgb($r, $g, $b)",
            red = r,
            green = g,
            blue = b,
            alpha = a
        )
    }
    
    data class ColorInfo(
        val hex: String,
        val rgba: String,
        val rgb: String,
        val red: Int,
        val green: Int,
        val blue: Int,
        val alpha: Int
    )
    
    // ==================== 文本处理 ====================
    
    object TextProcessor {
        
        fun base64Encode(text: String): String {
            return Base64.getEncoder().encodeToString(text.toByteArray())
        }
        
        fun base64Decode(text: String): String {
            return String(Base64.getDecoder().decode(text))
        }
        
        fun urlEncode(text: String): String {
            return java.net.URLEncoder.encode(text, "UTF-8")
        }
        
        fun urlDecode(text: String): String {
            return java.net.URLDecoder.decode(text, "UTF-8")
        }
        
        fun md5(text: String): String {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(text.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
        
        fun sha1(text: String): String {
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(text.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
        
        fun sha256(text: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(text.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
        
        fun adler32(text: String): String {
            val checksum = Adler32()
            checksum.update(text.toByteArray())
            return checksum.value.toString()
        }
        
        fun countWords(text: String): Int {
            return text.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }.size
        }
        
        fun countChars(text: String): Int {
            return text.length
        }
        
        fun countCharsNoSpaces(text: String): Int {
            return text.replace(Regex("""\s"""), "").length
        }
        
        fun countLines(text: String): Int {
            return text.split("\n").size
        }
        
        fun toUpperCase(text: String): String = text.uppercase()
        fun toLowerCase(text: String): String = text.lowercase()
        fun reverseText(text: String): String = text.reversed()
        fun removeDuplicates(text: String): String = text.toSet().joinToString("")
        fun reverseWords(text: String): String = text.split(" ").reversed().joinToString(" ")
    }
    
    // ==================== 设备信息 ====================
    
    fun getDeviceInfo(): DeviceInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        
        val statFs = StatFs(Environment.getDataDirectory().path)
        
        return DeviceInfo(
            // 基本信息
            brand = Build.BRAND,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            
            // 系统版本
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH
            } else "N/A",
            
            // CPU
            cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
            cpuCores = Runtime.getRuntime().availableProcessors(),
            
            // 内存
            totalRam = memoryInfo.totalMem,
            availableRam = memoryInfo.availMem,
            usedRam = memoryInfo.totalMem - memoryInfo.availMem,
            isLowRam = memoryInfo.lowMemory,
            
            // 存储
            totalStorage = statFs.totalBytes,
            availableStorage = statFs.availableBytes,
            usedStorage = statFs.totalBytes - statFs.availableBytes,
            
            // 屏幕
            screenWidth = displayMetrics.widthPixels,
            screenHeight = displayMetrics.heightPixels,
            screenDensity = displayMetrics.densityDpi,
            screenResolution = "${displayMetrics.widthPixels} x ${displayMetrics.heightPixels}",
            
            // 电池
            batteryLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1,
            batteryScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1,
            batteryStatus = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1,
            isCharging = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING,
            
            // WiFi
            wifiSsid = wifiInfo?.ssid?.replace("\"", "") ?: "未连接",
            wifiSignalStrength = wifiInfo?.rssi ?: -100,
            wifiLinkSpeed = wifiInfo?.linkSpeed ?: 0,
            
            // 时间
            currentTime = System.currentTimeMillis(),
            uptime = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()
        )
    }
    
    data class DeviceInfo(
        val brand: String,
        val model: String,
        val manufacturer: String,
        val device: String,
        val product: String,
        val androidVersion: String,
        val sdkVersion: Int,
        val securityPatch: String,
        val cpuAbi: String,
        val cpuCores: Int,
        val totalRam: Long,
        val availableRam: Long,
        val usedRam: Long,
        val isLowRam: Boolean,
        val totalStorage: Long,
        val availableStorage: Long,
        val usedStorage: Long,
        val screenWidth: Int,
        val screenHeight: Int,
        val screenDensity: Int,
        val screenResolution: String,
        val batteryLevel: Int,
        val batteryScale: Int,
        val batteryStatus: Int,
        val isCharging: Boolean,
        val wifiSsid: String,
        val wifiSignalStrength: Int,
        val wifiLinkSpeed: Int,
        val currentTime: Long,
        val uptime: Long
    ) {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1099511627776 -> "%.2f TB".format(bytes / 1099511627776.0)
                bytes >= 1073741824 -> "%.2f GB".format(bytes / 1073741824.0)
                bytes >= 1048576 -> "%.2f MB".format(bytes / 1048576.0)
                bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
                else -> "$bytes B"
            }
        }
        
        fun getBatteryPercent(): Int {
            return if (batteryScale > 0) (batteryLevel * 100 / batteryScale) else 0
        }
        
        fun getWifiSignalLevel(): String {
            return when {
                wifiSignalStrength >= -50 -> "极好"
                wifiSignalStrength >= -60 -> "很好"
                wifiSignalStrength >= -70 -> "一般"
                wifiSignalStrength >= -80 -> "较差"
                else -> "很差"
            }
        }
    }
    
    // ==================== 计算器 ====================
    
    object Calculator {
        
        fun evaluate(expression: String): Double? {
            return try {
                // 简单的表达式计算
                val sanitized = expression
                    .replace("×", "*")
                    .replace("÷", "/")
                    .replace("−", "-")
                    .replace(" ", "")
                
                if (sanitized.contains(Regex("[^0-9+\\-*/.()^]"))) {
                    return null
                }
                
                // 使用递归下降解析
                parseExpression(sanitized.iterator())
            } catch (e: Exception) {
                null
            }
        }
        
        private fun parseExpression(chars: Iterator<Char>): Double {
            var pos = 0
            val s = chars.asSequence().joinToString("")
            
            return object {
                fun parse(): Double = parseAddSub()
                
                fun parseAddSub(): Double {
                    var left = parseMulDiv()
                    while (true) {
                        when (nextOp()) {
                            '+' -> { consume(); left = left + parseMulDiv() }
                            '-' -> { consume(); left = left - parseMulDiv() }
                            else -> return left
                        }
                    }
                }
                
                fun parseMulDiv(): Double {
                    var left = parsePower()
                    while (true) {
                        when (nextOp()) {
                            '*' -> { consume(); left = left * parsePower() }
                            '/' -> { consume(); left = left / parsePower() }
                            else -> return left
                        }
                    }
                }
                
                fun parsePower(): Double {
                    var left = parseUnary()
                    if (nextOp() == '^') {
                        consume()
                        left = Math.pow(left, parsePower())
                    }
                    return left
                }
                
                fun parseUnary(): Double {
                    if (nextOp() == '-') {
                        consume()
                        return -parseUnary()
                    }
                    return parsePrimary()
                }
                
                fun parsePrimary(): Double {
                    if (nextOp() == '(') {
                        consume()
                        val result = parse()
                        expect(')')
                        return result
                    }
                    return parseNumber()
                }
                
                fun parseNumber(): Double {
                    val sb = StringBuilder()
                    while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) {
                        sb.append(s[pos++])
                    }
                    return sb.toString().toDoubleOrNull() ?: 0.0
                }
                
                fun nextOp(): Char = if (pos < s.length) s[pos] else '\u0000'
                
                fun consume() { if (pos < s.length) pos++ }
                
                fun expect(c: Char) { if (nextOp() == c) consume() }
            }.parse()
        }
    }
    
    // ==================== 时间戳工具 ====================
    
    object TimeUtils {
        
        fun timestampToDate(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
        
        fun dateToTimestamp(date: String, pattern: String = "yyyy-MM-dd HH:mm:ss"): Long? {
            return try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.parse(date)?.time
            } catch (e: Exception) {
                null
            }
        }
        
        fun nowTimestamp(): Long = System.currentTimeMillis()
        
        fun nowDateString(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
            return timestampToDate(System.currentTimeMillis(), pattern)
        }
    }
}
