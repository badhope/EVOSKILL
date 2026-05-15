package com.banana.toolbox.domain.usecase.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网络工具用例
 */
@Singleton
class NetworkUseCases @Inject constructor() {
    
    /**
     * 获取当前网络信息
     */
    suspend fun getNetworkInfo(context: Context): NetworkInfo {
        return withContext(Dispatchers.IO) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            
            val network = connectivityManager.activeNetwork
            val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
            
            val wifiInfo = try {
                @Suppress("DEPRECATION")
                wifiManager.connectionInfo
            } catch (e: Exception) { null }
            
            NetworkInfo(
                isConnected = capabilities != null,
                networkType = when {
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> NetworkType.WIFI
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> NetworkType.CELLULAR
                    else -> NetworkType.NONE
                },
                wifiSsid = wifiInfo?.ssid?.replace("\"", "") ?: "未连接",
                wifiSignalStrength = wifiInfo?.rssi ?: 0,
                wifiLinkSpeed = wifiInfo?.linkSpeed ?: 0,
                ipAddress = getLocalIpAddress(),
                isUsingVPN = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            )
        }
    }
    
    private fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "Unknown"
    }
    
    /**
     * Ping 测试
     */
    suspend fun ping(host: String, count: Int = 4): PingResult {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<PingResult.PingItem>()
            var packetLoss = 0
            var totalTime = 0L
            
            for (i in 1..count) {
                val startTime = System.currentTimeMillis()
                try {
                    val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W 2 $host")
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    var latency: Long? = null
                    
                    while (reader.readLine().also { line = it } != null) {
                        if (line?.contains("time=") == true) {
                            val timeStr = line!!.substringAfter("time=").substringBefore(" ")
                            latency = timeStr.toDoubleOrNull()?.toLong()
                        }
                    }
                    
                    val exitCode = process.waitFor()
                    val endTime = System.currentTimeMillis()
                    
                    if (exitCode == 0 && latency != null) {
                        results.add(PingResult.PingItem(
                            sequence = i,
                            success = true,
                            latency = latency!!,
                            time = endTime - startTime
                        ))
                        totalTime += latency!!
                    } else {
                        results.add(PingResult.PingItem(
                            sequence = i,
                            success = false,
                            latency = null,
                            time = endTime - startTime
                        ))
                        packetLoss++
                    }
                } catch (e: Exception) {
                    results.add(PingResult.PingItem(
                        sequence = i,
                        success = false,
                        latency = null,
                        time = 0
                    ))
                    packetLoss++
                }
            }
            
            PingResult(
                host = host,
                packetsSent = count,
                packetsReceived = count - packetLoss,
                packetLossPercent = (packetLoss * 100) / count,
                minLatency = results.filter { it.success }.minOfOrNull { it.latency } ?: 0,
                avgLatency = if (count - packetLoss > 0) totalTime / (count - packetLoss) else 0,
                maxLatency = results.filter { it.success }.maxOfOrNull { it.latency } ?: 0,
                pingItems = results
            )
        }
    }
    
    /**
     * DNS 查询
     */
    suspend fun dnsLookup(domain: String): DnsResult {
        return withContext(Dispatchers.IO) {
            try {
                val addresses = InetAddress.getAllByName(domain)
                val ipv4Addresses = addresses.filterIsInstance<Inet4Address>().map { it.hostAddress ?: "" }
                val ipv6Addresses = addresses.filter { it !is Inet4Address }.map { it.hostAddress ?: "" }
                
                DnsResult(
                    domain = domain,
                    success = true,
                    ipv4Addresses = ipv4Addresses,
                    ipv6Addresses = ipv6Addresses,
                    error = null
                )
            } catch (e: Exception) {
                DnsResult(
                    domain = domain,
                    success = false,
                    ipv4Addresses = emptyList(),
                    ipv6Addresses = emptyList(),
                    error = e.message
                )
            }
        }
    }
    
    /**
     * 端口扫描
     */
    suspend fun scanPorts(host: String, ports: List<Int>, timeout: Int = 1000): List<PortResult> {
        return withContext(Dispatchers.IO) {
            ports.map { port ->
                val startTime = System.currentTimeMillis()
                val isOpen = try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, port), timeout)
                        true
                    }
                } catch (e: Exception) {
                    false
                }
                
                PortResult(
                    port = port,
                    serviceName = getServiceName(port),
                    isOpen = isOpen,
                    responseTime = System.currentTimeMillis() - startTime
                )
            }
        }
    }
    
    /**
     * 扫描常用端口
     */
    suspend fun scanCommonPorts(host: String): List<PortResult> {
        return scanPorts(host, COMMON_PORTS)
    }
    
    private fun getServiceName(port: Int): String {
        return SERVICE_NAMES[port] ?: "Unknown"
    }
    
    /**
     * 局域网扫描
     */
    suspend fun scanLocalNetwork(context: Context): List<DeviceInfo> {
        return withContext(Dispatchers.IO) {
            val devices = mutableListOf<DeviceInfo>()
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            
            @Suppress("DEPRECATION")
            val dhcpInfo = wifiManager.dhcpInfo
            val gateway = dhcpInfo.gateway
            val subnet = getSubnetPrefix(dhcpInfo.ipAddress)
            
            // 获取本地 IP
            val localIp = intToIp(dhcpInfo.ipAddress)
            devices.add(DeviceInfo(
                ip = localIp,
                mac = getMacAddress(),
                hostname = "本机",
                isLocal = true,
                isReachable = true
            ))
            
            // 扫描网关
            devices.add(DeviceInfo(
                ip = intToIp(gateway),
                mac = "Unknown",
                hostname = "网关",
                isLocal = false,
                isReachable = true
            ))
            
            // 并发扫描其他设备
            val jobs = (1..254).map { i ->
                val ip = "$subnet.$i"
                async(Dispatchers.IO) {
                    try {
                        val address = InetAddress.getByName(ip)
                        val reachable = address.isReachable(200)
                        if (reachable) {
                            DeviceInfo(
                                ip = ip,
                                mac = "Unknown",
                                hostname = try { address.hostName } catch (e: Exception) { "Unknown" },
                                isLocal = false,
                                isReachable = true
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            
            jobs.forEach { job ->
                job.await()?.let { devices.add(it) }
            }
            
            devices.sortedBy { it.ip }
        }
    }
    
    private fun getSubnetPrefix(ip: Int): String {
        return intToIp(ip).substringBeforeLast(".")
    }
    
    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
    
    @Suppress("DEPRECATION")
    private fun getMacAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.name.equals("wlan0", ignoreCase = true)) {
                    val mac = networkInterface.hardwareAddress
                    return mac.joinToString(":") { byte -> "%02X".format(byte) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "Unknown"
    }
    
    /**
     * 网速测试
     */
    suspend fun speedTest(): SpeedTestResult {
        return withContext(Dispatchers.IO) {
            try {
                // 测试下载速度
                val downloadUrl = URL("https://speed.cloudflare.com/__down?bytes=10000000")
                val downloadStart = System.currentTimeMillis()
                val connection = downloadUrl.openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 30000
                
                val buffer = ByteArray(8192)
                var totalBytes = 0L
                connection.getInputStream().use { input ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead
                    }
                }
                val downloadTime = System.currentTimeMillis() - downloadStart
                val downloadSpeed = (totalBytes * 1000 / downloadTime) / 1024.0 // KB/s
                
                SpeedTestResult(
                    success = true,
                    downloadSpeed = downloadSpeed,
                    uploadSpeed = 0.0, // 需要服务器支持
                    latency = 0,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                SpeedTestResult(
                    success = false,
                    downloadSpeed = 0.0,
                    uploadSpeed = 0.0,
                    latency = 0,
                    error = e.message
                )
            }
        }
    }
}

data class NetworkInfo(
    val isConnected: Boolean,
    val networkType: NetworkType,
    val wifiSsid: String,
    val wifiSignalStrength: Int,
    val wifiLinkSpeed: Int,
    val ipAddress: String,
    val isUsingVPN: Boolean
)

enum class NetworkType {
    WIFI, CELLULAR, NONE
}

data class PingResult(
    val host: String,
    val packetsSent: Int,
    val packetsReceived: Int,
    val packetLossPercent: Int,
    val minLatency: Long,
    val avgLatency: Long,
    val maxLatency: Long,
    val pingItems: List<PingItem> = emptyList()
) {
    data class PingItem(
        val sequence: Int,
        val success: Boolean,
        val latency: Long?,
        val time: Long
    )
}

data class DnsResult(
    val domain: String,
    val success: Boolean,
    val ipv4Addresses: List<String>,
    val ipv6Addresses: List<String>,
    val error: String?
)

data class PortResult(
    val port: Int,
    val serviceName: String,
    val isOpen: Boolean,
    val responseTime: Long
)

data class DeviceInfo(
    val ip: String,
    val mac: String,
    val hostname: String,
    val isLocal: Boolean,
    val isReachable: Boolean
)

data class SpeedTestResult(
    val success: Boolean,
    val downloadSpeed: Double, // KB/s
    val uploadSpeed: Double,
    val latency: Long,
    val timestamp: Long = 0,
    val error: String? = null
)

private val COMMON_PORTS = listOf(
    20, 21, 22, 23, 25, 53, 80, 110, 143, 443, 
    445, 993, 995, 3306, 3389, 5432, 5900, 8080, 8443
)

private val SERVICE_NAMES = mapOf(
    20 to "FTP Data",
    21 to "FTP",
    22 to "SSH",
    23 to "Telnet",
    25 to "SMTP",
    53 to "DNS",
    80 to "HTTP",
    110 to "POP3",
    143 to "IMAP",
    443 to "HTTPS",
    445 to "SMB",
    993 to "IMAPS",
    995 to "POP3S",
    3306 to "MySQL",
    3389 to "RDP",
    5432 to "PostgreSQL",
    5900 to "VNC",
    8080 to "HTTP Proxy",
    8443 to "HTTPS Alt"
)

private fun <T> async(block: () -> T): kotlinx.coroutines.Deferred<T> {
    return kotlinx.coroutines.GlobalScope.async(kotlinx.coroutines.Dispatchers.IO) { block() }
}
