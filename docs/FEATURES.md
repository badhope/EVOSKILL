# 📦 Banana Toolbox - 功能详细设计

## 一、文件管理器

### 1.1 文件浏览

**功能描述：** 浏览设备存储中的文件和文件夹

**输入：** 当前路径（默认根目录）

**输出：** 文件列表（名称、大小、类型、修改时间、图标）

**流程：**
```
1. 用户进入目录
2. 检查权限（存储权限）
3. 读取目录内容
4. 按排序规则排序
5. 渲染列表
```

**技术实现：**
```kotlin
class FileListUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(path: String, sortBy: SortBy): Result<List<FileItem>> {
        return fileRepository.listFiles(path)
            .map { files -> sortFiles(files, sortBy) }
    }
}
```

---

### 1.2 文件操作

#### 复制文件
```
输入：源路径列表、目标路径
输出：操作结果（成功/失败）
进度：显示进度对话框，支持取消
```

#### 移动文件
```
输入：源路径列表、目标路径
输出：操作结果
注意：同分区移动使用 rename，跨分区使用复制+删除
```

#### 删除文件
```
输入：要删除的路径列表
输出：操作结果
确认：弹出确认对话框
回收站：可选移入回收站而非直接删除
```

#### 重命名
```
输入：文件路径、新名称
输出：操作结果
校验：名称合法性检查
```

---

### 1.3 搜索功能

**搜索维度：**
- 文件名（支持通配符）
- 文件类型（扩展名）
- 文件大小范围
- 修改时间范围

**实现：**
```kotlin
data class SearchQuery(
    val name: String? = null,
    val extensions: List<String>? = null,
    val minSize: Long? = null,
    val maxSize: Long? = null,
    val startTime: Long? = null,
    val endTime: Long? = null
)

suspend fun search(query: SearchQuery, basePath: String): List<FileItem>
```

---

### 1.4 压缩解压

**支持格式：** ZIP、RAR、7Z、TAR、GZ

**压缩：**
```
输入：源文件列表、压缩包路径、格式、压缩级别
输出：压缩包文件
进度：显示进度条
```

**解压：**
```
输入：压缩包路径、目标目录
输出：解压后的文件列表
编码：自动检测或手动指定（解决中文乱码）
```

---

### 1.5 文件分类

**分类规则：**
| 类别 | 扩展名 |
|------|--------|
| 图片 | jpg, jpeg, png, gif, webp, bmp, svg |
| 视频 | mp4, mkv, avi, mov, wmv, flv |
| 音频 | mp3, wav, flac, aac, ogg, m4a |
| 文档 | pdf, doc, docx, xls, xlsx, ppt, pptx, txt |
| 安装包 | apk |
| 压缩包 | zip, rar, 7z, tar, gz |

---

### 1.6 存储分析

**大文件扫描：**
```
阈值：可配置（默认 100MB）
输出：大文件列表，按大小排序
操作：支持批量删除/移动
```

**重复文件：**
```
算法：MD5/SHA1 哈希比对
输出：重复文件组列表
操作：保留一个，删除其他
```

**垃圾清理：**
```
扫描项：
- 缓存目录
- 临时文件
- 日志文件
- 空文件夹
输出：可清理大小，详细列表
```

---

## 二、应用管理

### 2.1 应用列表

**数据获取：**
```kotlin
fun getInstalledApps(): List<AppItem> {
    val pm = context.packageManager
    return pm.getInstalledPackages(PackageManager.GET_META_DATA)
        .map { packageInfo ->
            AppItem(
                packageName = packageInfo.packageName,
                appName = packageInfo.applicationInfo.loadLabel(pm).toString(),
                versionName = packageInfo.versionName,
                // ...
            )
        }
}
```

**分组：**
- 用户应用
- 系统应用
- 最近更新

---

### 2.2 批量卸载

```
输入：选中的应用列表
流程：
1. 确认对话框
2. 逐个调用系统卸载 Intent
3. 记录结果
输出：卸载结果报告
```

---

### 2.3 应用备份

```
输入：应用包名、备份目录
输出：APK 文件路径
实现：
1. 获取 APK 路径
2. 复制到目标目录
3. 可选：同时备份数据（需要 root）
```

---

### 2.4 权限查看

```
输入：应用包名
输出：权限列表
分组：
- 危险权限（定位、相机、存储等）
- 普通权限
- 未请求的权限
```

---

## 三、网絡工具

### 3.1 网速测试

```
流程：
1. 选择测试服务器
2. 下载测试（多线程）
3. 上传测试（多线程）
4. 计算延迟
输出：下载速度、上传速度、延迟、抖动
```

---

### 3.2 网络诊断

**Ping：**
```kotlin
suspend fun ping(host: String, count: Int = 4): PingResult {
    val results = mutableListOf<PingItem>()
    repeat(count) {
        val startTime = System.currentTimeMillis()
        val reachable = InetAddress.getByName(host).isReachable(5000)
        val latency = System.currentTimeMillis() - startTime
        results.add(PingItem(reachable, latency))
    }
    return PingResult(results)
}
```

**DNS 查询：**
```kotlin
suspend fun dnsLookup(domain: String): DnsResult {
    val addresses = InetAddress.getAllByName(domain)
    return DnsResult(
        domain = domain,
        ipv4 = addresses.filter { it is Inet4Address }.map { it.hostAddress },
        ipv6 = addresses.filter { it is Inet6Address }.map { it.hostAddress }
    )
}
```

---

### 3.3 局域网扫描

```
流程：
1. 获取本机 IP 和网段
2. 并发 Ping 网段内所有 IP
3. 对响应的 IP 进行端口扫描
4. 尝试识别设备类型
输出：设备列表（IP、MAC、设备名、开放端口）
```

---

### 3.4 端口扫描

```
输入：目标 IP、端口范围
输出：开放端口列表
常用端口识别：
- 21: FTP
- 22: SSH
- 80: HTTP
- 443: HTTPS
- 3306: MySQL
- 8080: HTTP Proxy
```

---

## 四、实用工具

### 4.1 单位换算器

**支持类型：**
- 长度（m, km, mi, ft, in）
- 重量（kg, g, lb, oz）
- 温度（C, F, K）
- 面积（m², km², acre）
- 体积（L, mL, gal）
- 数据（B, KB, MB, GB, TB）
- 时间（s, min, h, day）

---

### 4.2 颜色取值器

**功能：**
- 从图片取色
- 颜色格式转换（HEX、RGB、HSL、CMYK）
- 颜色历史记录
- 调色板生成

---

### 4.3 二维码工具

**生成：**
```kotlin
fun generateQRCode(
    content: String,
    size: Int = 512,
    format: String = "PNG"
): Bitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 2
    )
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    return toBitmap(matrix)
}
```

**扫描：**
- 调用相机实时扫描
- 支持从图片识别
- 识别结果自动分类（URL、文本、WiFi 等）

---

### 4.4 文本处理

**功能：**
- Base64 编码/解码
- URL 编码/解码
- JSON 格式化
- MD5/SHA 哈希
- 大小写转换
- 字数统计
- 去重/排序

---

### 4.5 图片处理

**功能：**
- 压缩（质量/尺寸）
- 格式转换（JPG、PNG、WebP）
- 裁剪
- 添加水印
- 批量处理

---

### 4.6 设备信息

**信息项：**
- 设备型号、品牌
- Android 版本
- CPU 信息（核心数、频率）
- 内存信息（总量、可用）
- 存储信息（内部/SD卡）
- 电池信息
- 屏幕信息（分辨率、密度）
- 网络 信息

---

### 4.7 剪贴板历史

**功能：**
- 监听剪贴板变化
- 保存历史记录
- 快速粘贴
- 搜索历史
- 清除历史

**实现：**
```kotlin
class ClipboardMonitor(private val context: Context) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    fun startMonitoring(onNewClip: (ClipData) -> Unit) {
        clipboardManager.addPrimaryClipChangedListener {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                onNewClip(clip)
            }
        }
    }
}
```

---

### 4.8 计算器

**功能：**
- 基础四则运算
- 科学计算（三角函数、对数）
- 历史记录
- 表达式编辑
