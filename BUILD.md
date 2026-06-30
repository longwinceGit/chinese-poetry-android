# 诗词乐园 构建指南

## 快速开始

### 前置条件

| 工具 | 版本要求 |
|------|----------|
| Android Studio | Arctic Fox (2020.3.1+) |
| JDK | 11+ |
| Android SDK | API 21–33 |
| Gradle | 4.2.2（构建时自动处理） |

---

## 方式一：Android Studio（推荐）

1. 打开 Android Studio
2. **File → Open** → 选择本项目根目录
3. 等待 Gradle 同步（首次需下载依赖，约 2-5 分钟）
4. 连接手机（USB 调试）或启动模拟器
5. 点击 **Run ▶**

---

## 方式二：命令行构建 APK

```bash
# Windows
gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

APK 输出路径：
```
app/build/outputs/apk/debug/app-debug.apk
```

安装到手机：
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 方式三：一键脚本（Windows）

双击 `build-apk.bat`，脚本会自动：
1. 检测 / 下载 Gradle 7.5
2. 执行 `assembleDebug`
3. 输出 APK 路径

---

## 自定义构建

### 修改应用名称

编辑 `app/src/main/res/values/strings.xml`：
```xml
<string name="app_name">你的应用名</string>
```

### 修改包名

编辑 `app/build.gradle` 中的 `applicationId`：
```gradle
defaultConfig {
    applicationId "com.your.package"
}
```

### 修改最低版本

```gradle
minSdkVersion 21   # 最低 Android 5.0
targetSdkVersion 33
```

---

## 目录结构

```
.
├── app/
│   ├── build.gradle        # 模块配置（compileSdk=33, minSdk=21）
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/poetry/          # 源码
│       │   ├── MainActivity.java     # 主界面（入口）
│       │   ├── Poem.java             # 数据模型
│       │   ├── PoemAdapter.java      # RecyclerView 适配器
│       │   ├── PoemLoader.java       # JSON 加载器
│       │   └── ConfettiView.java     # 彩纸动画 View
│       ├── res/
│       │   ├── layout/               # 布局文件
│       │   ├── drawable/             # 形状/按钮背景
│       │   └── values/               # 颜色/样式/字符串
│       └── assets/web/data/          # 诗词 JSON 数据
├── build.gradle              # 根配置
├── settings.gradle           # 模块设置
└── build-apk.bat             # Windows 构建脚本
```

---

## 故障排除

**Gradle 同步失败**：检查 Android SDK 路径，在项目根目录创建 `local.properties`：
```
sdk.dir=C:\\Users\\你的用户名\\AppData\\Local\\Android\\sdk
```

**构建失败 / 依赖下载慢**：`build.gradle` 已配置阿里云 Maven 镜像加速。

**TTS 无声**：手机需安装中文 TTS 语音包（设置 → 文字转语音 → 首选引擎 → 安装中文语音数据）。

**Apk 体积大**（~55 MB）：因诗词 JSON 数据文件较多，`release` 构建可使用 ProGuard 缩减。
