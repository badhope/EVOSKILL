# 🏗️ Banana Toolbox - 技术架构文档

## 一、架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                        Presentation Layer                    │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  UI (Jetpack Compose) + ViewModel + Navigation          ││
│  └─────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│                         Domain Layer                         │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  UseCase + Repository Interface + Domain Model          ││
│  └─────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│                          Data Layer                          │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  Repository Impl + Local DataSource + Remote DataSource ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

**架构模式：** Clean Architecture + MVVM

---

## 二、模块划分

### 2.1 模块依赖关系

```
                    app
                     │
         ┌───────────┼───────────┐
         │           │           │
    feature-*   feature-*   feature-*
         │           │           │
         └───────────┼───────────┘
                     │
               core-ui
                     │
               core-common
```

### 2.2 模块职责

| 模块 | 职责 |
|------|------|
| `app` | 应用入口、导航、全局配置 |
| `feature-filemanager` | 文件管理功能 |
| `feature-appmanager` | 应用管理功能 |
| `feature-network` | 网络工具功能 |
| `feature-tools` | 实用小工具集合 |
| `core-ui` | 通用 UI 组件、主题 |
| `core-common` | 工具类、基础扩展 |

---

## 三、核心技术栈

### 3.1 依赖清单

```kotlin
// build.gradle.kts (Project)
plugins {
    id("com.android.application") version "8.2.0"
    id("org.jetbrains.kotlin.android") version "1.9.20"
    id("com.google.dagger.hilt.android") version "2.48"
}

// build.gradle.kts (app)
dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.4")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Network
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Compression
    implementation("org.apache.commons:commons-compress:1.24.0")
    
    // QR Code
    implementation("com.google.zxing:core:3.5.2")
}
```

---

## 四、数据流设计

### 4.1 文件操作数据流

```
UI Event
    │
    ▼
ViewModel
    │
    ▼
UseCase (FileOperationUseCase)
    │
    ▼
FileRepository
    │
    ├──▶ LocalDataSource (FileSystemService)
    │
    └──▶ 返回结果
            │
            ▼
        ViewModel (StateFlow)
            │
            ▼
        UI (Compose)
```

### 4.2 状态管理

```kotlin
// UI State
data class FileListState(
    val path: String = "/",
    val files: List<FileItem> = emptyList(),
    val selectedFiles: Set<String> = emptySet(),
    val viewMode: ViewMode = ViewMode.LIST,
    val sortBy: SortBy = SortBy.NAME,
    val isLoading: Boolean = false,
    val error: String? = null
)

// ViewModel
@HiltViewModel
class FileListViewModel @Inject constructor(
    private val getFileListUseCase: GetFileListUseCase,
    private val deleteFilesUseCase: DeleteFilesUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(FileListState())
    val state: StateFlow<FileListState> = _state.asStateFlow()
    
    fun loadFiles(path: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getFileListUseCase(path)
                .onSuccess { files ->
                    _state.update { it.copy(files = files, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }
}
```

---

## 五、关键类设计

### 5.1 文件实体

```kotlin
// Domain Model
data class FileItem(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val mimeType: String?,
    val thumbnail: String?
)

// Repository Interface
interface FileRepository {
    suspend fun listFiles(path: String): Result<List<FileItem>>
    suspend fun copyFile(source: String, dest: String): Result<Unit>
    suspend fun moveFile(source: String, dest: String): Result<Unit>
    suspend fun deleteFiles(paths: List<String>): Result<Unit>
    suspend fun renameFile(path: String, newName: String): Result<Unit>
    suspend fun createDirectory(parentPath: String, name: String): Result<Unit>
    suspend fun searchFiles(query: String, basePath: String): Result<List<FileItem>>
}
```

### 5.2 应用实体

```kotlin
data class AppItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val size: Long,
    val installTime: Long,
    val updateTime: Long,
    val isSystemApp: Boolean,
    val icon: Drawable?,
    val apkPath: String
)

interface AppRepository {
    suspend fun getInstalledApps(): Result<List<AppItem>>
    suspend fun uninstallApp(packageName: String): Result<Unit>
    suspend fun backupApp(packageName: String, destPath: String): Result<String>
    suspend fun getAppInfo(packageName: String): Result<AppItem>
}
```

---

## 六、权限设计

| 权限 | 用途 | 必需 |
|------|------|------|
| READ_EXTERNAL_STORAGE | 读取文件 | 是 |
| WRITE_EXTERNAL_STORAGE | 写入文件 | 是 |
| MANAGE_EXTERNAL_STORAGE | 所有文件访问(Android 11+) | 是 |
| REQUEST_INSTALL_PACKAGES | 安装 APK | 否 |
| ACCESS_WIFI_STATE | 网络工具 | 否 |
| CHANGE_WIFI_STATE | 网络工具 | 否 |
| INTERNET | 网络请求 | 否 |
| CAMERA | 二维码扫描 | 否 |
| VIBRATE | 反馈震动 | 否 |

---

## 七、性能优化策略

### 7.1 文件列表加载
- 分页加载（LazyColumn）
- 缓存已加载目录
- 后台预加载缩略图

### 7.2 大文件操作
- 使用协程异步执行
- 显示进度通知
- 支持取消操作

### 7.3 内存管理
- 图片使用 Coil 自动管理
- 及时释放不用的资源
- 避免内存泄漏（WeakReference）

---

## 八、测试策略

### 8.1 单元测试
- UseCase 逻辑测试
- Repository 测试（Mock）
- ViewModel 状态测试

### 8.2 UI 测试
- Compose UI 测试
- 端到端流程测试

### 8.3 测试框架
- JUnit 5
- MockK
- Turbine (Flow 测试)
- Compose Testing
