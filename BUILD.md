# 中华诗词库 Android App 构建说明

## 快速开始

### 前置条件

- Android Studio（推荐）或 Android SDK
- JDK 11+
- Android 5.0（API 21）及以上手机

---

## 方式一：Android Studio 构建（推荐）

1. 打开 Android Studio
2. **File → Open** → 选择 `android/` 目录
3. 等待 Gradle 同步完成（首次需下载依赖，约 2-5 分钟）
4. 连接手机（开启 USB 调试）或启动模拟器
5. 点击 **Run ▶** 按钮

---

## 方式二：命令行构建 APK

```bash
cd android

# Windows
gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

APK 输出路径：
```
android/app/build/outputs/apk/debug/app-debug.apk
```

安装到手机：
```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 方式三：直接生成 APK（无需 Android Studio）

下载项目后在 `android/` 目录执行：

```bash
# 先生成 gradlew
gradle wrapper --gradle-version 8.5

# 构建
./gradlew assembleDebug
```

---

## 更新诗词数据

每次修改 `web/` 目录后，重新复制到 assets：

```bash
# 先重新生成数据
python build_data.py

# 复制到 assets
cp -r web/* android/app/src/main/assets/web/

# 重新构建 APK
cd android && ./gradlew assembleDebug
```

---

## 技术说明

| 项目 | 说明 |
|------|------|
|  WebView 方案 | 内置 NanoHTTPD 服务器（端口 8765） |
| 离线使用 | ✅ 所有数据打包在 App 内 |
| 最低系统 | Android 5.0（API 21）|
| 目标系统 | Android 14（API 34）|
| APK 大小 | 约 55 MB（含全部诗词数据）|

---

## 目录结构

```
android/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/poetry/MainActivity.java
│       ├── res/
│       │   ├── layout/activity_main.xml
│       │   └── values/{strings.xml,styles.xml}
│       └── assets/web/    ← 网站文件（自动复制）
├── build.gradle
└── settings.gradle
```

---

## 故障排除

**Gradle 同步失败**：检查 Android SDK 路径是否正确，在 `local.properties` 中设置：
```
sdk.dir=C:\\Users\\你的用户名\\AppData\\Local\\Android\\sdk
```

**WebView 白屏**：确认 `assets/web/` 目录已正确复制，且 `index.html` 存在。

**fetch 失败**：确认 `MainActivity.java` 中的 NanoHTTPD 服务器已启动（Logcat 中查看）。
