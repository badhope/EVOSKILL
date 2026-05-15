# 📦 Banana Toolbox - 编译指南

## 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 17+ |
| Gradle | 8.4+ |
| Android SDK | API 34 (Android 14) |
| Kotlin | 1.9.20 |

---

## 一、本地编译（推荐）

### 1. 安装 JDK 17

**macOS:**
```bash
brew install openjdk@17
```

**Linux:**
```bash
sudo apt install openjdk-17-jdk
```

**Windows:**
从 [Adoptium](https://adoptium.net/) 下载 JDK 17

### 2. 安装 Android SDK

**方式一：Android Studio（推荐）**
1. 下载 [Android Studio](https://developer.android.com/studio)
2. 打开 Android Studio → Settings → SDK Manager
3. 安装 SDK Platform 34
4. 安装 Build Tools 34.0.0

**方式二：命令行工具**
```bash
# 下载命令行工具
wget https://dl.google.com/android/repository/commandlinetools-linux-11066523_latest.zip
unzip commandlinetools-linux-11066523_latest.zip

# 设置环境变量
export ANDROID_HOME=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# 安装 SDK
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

### 3. 编译项目

```bash
# 克隆项目
git clone https://github.com/badhope/Banana-Community.git
cd Banana-Community

# 编译 Debug APK
./gradlew assembleDebug

# 编译 Release APK
./gradlew assembleRelease
```

### 4. APK 输出位置

```
app/build/outputs/apk/
├── debug/
│   └── app-debug.apk          # Debug 版本
└── release/
    └── app-release.apk        # Release 版本（未签名）
```

---

## 二、签名 Release APK

### 1. 生成签名密钥

```bash
keytool -genkey -v -keystore banana-toolbox.keystore \
  -alias banana \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD
```

### 2. 配置签名

在 `app/build.gradle.kts` 中添加：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../banana-toolbox.keystore")
            storePassword = "YOUR_STORE_PASSWORD"
            keyAlias = "banana"
            keyPassword = "YOUR_KEY_PASSWORD"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
        }
    }
}
```

### 3. 编译签名 APK

```bash
./gradlew assembleRelease
```

---

## 三、CI/CD 自动编译

项目已配置 GitHub Actions，推送代码后自动编译：

```yaml
# .github/workflows/build.yml
name: Build APK

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Android SDK
        uses: android-actions/setup-android@v3
      
      - name: Build Debug APK
        run: ./gradlew assembleDebug
      
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
```

---

## 四、常见问题

### Q: Gradle 下载太慢？
```bash
# 使用国内镜像
echo "distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.4-bin.zip" > gradle/wrapper/gradle-wrapper.properties
```

### Q: 依赖下载失败？
在 `settings.gradle.kts` 中添加镜像：
```kotlin
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/central") }
    google()
    mavenCentral()
}
```

### Q: SDK 未找到？
```bash
export ANDROID_HOME=$HOME/Android/Sdk  # Linux/macOS
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk  # Windows
```

---

## 五、安装测试

编译完成后，通过 ADB 安装到设备：

```bash
# 连接设备
adb devices

# 安装 APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 或直接推送到设备
adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/
```
