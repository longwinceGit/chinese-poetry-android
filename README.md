# 中华诗词库 Android App

## 项目说明

这是一个将中华诗词库网站打包为 Android 原生应用的工程。

## 技术栈

- **WebView** 加载本地 HTML（离线可用，无需网络）
- **Java** 原生开发
- 最低支持 Android 5.0（API 21）

## 目录结构

```
android/
├── app/
│   ├── build.gradle          ← 模块构建配置
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/poetry/
│   │   │   └── MainActivity.java   ← 主 Activity
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── values/strings.xml
│   │   │   └── values/styles.xml
│   │   └── assets/web/            ← 网站文件（复制自 web/）
│   │       ├── index.html
│   │       └── data/
│   └── libs/                       ← 依赖库（如有）
├── build.gradle                    ← 根构建配置
└── settings.gradle                 ← 项目设置
```

## 构建步骤

### 方式一：Android Studio（推荐）

1. 用 Android Studio 打开 `android/` 目录
2. 等待 Gradle 同步完成
3. 连接手机或启动模拟器
4. 点击 **Run** ▶ 按钮

### 方式二：命令行构建

```bash
cd android
./gradlew assembleDebug        # Linux/Mac
gradlew.bat assembleDebug      # Windows
```

输出 APK：`app/build/outputs/apk/debug/app-debug.apk`

### 安装到手机

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 更新网站内容

每次修改 `web/` 目录后，需要重新复制到 `assets/web/`：

```bash
# Windows
xcopy /E /Y web\* android\app\src\main\assets\web\

# Linux/Mac
cp -r web/* android/app/src/main/assets/web/
```

## 注意事项

- `index.html` 中使用 `fetch()` 加载 `data/` 下的 JSON 文件，**Android WebView 不支持 `file://` 协议的 fetch**
- 需要改用**本地 HTTP 服务器**方案，或改用 `WebViewAssetLoader`
- 见下方「修复 fetch 问题」章节

---

## 修复 fetch 问题（重要）

Android WebView 的 `file://` 协议不支持 `fetch()` API，需要改用以下方案之一：

### 方案 A：使用 WebViewAssetLoader（推荐）

修改 `MainActivity.java`，使用 `WebViewAssetLoader` 提供 `https://` 虚拟域：

```java
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

// 在 onCreate() 中：
WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
    .addPathHandler("/web/", new WebViewAssetLoader.AssetsPathHandler(this))
    .build();

webView.setWebViewClient(new WebViewClientCompat() {
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return assetLoader.shouldInterceptRequest(request.getUrl());
    }
});

// 同时修改 index.html 中所有 fetch 路径为 https://appassets.androidplatform.net/web/data/...
```

### 方案 B：内置 NanoHTTPD 服务器

在 App 内启动一个微型 HTTP 服务器（端口 8765），让 WebView 访问 `http://127.0.0.1:8765/`。

见 `server/` 目录下的集成说明。

---

## 功能特性

| 功能 | 状态 |
|------|------|
| 朝代浏览 | ✅ |
| 作者筛选 | ✅ |
| 全文搜索 | ✅ |
| 侧栏折叠 | ✅ |
| 诗词详情弹窗 | ✅（需修复 fetch 后可用）|
| 离线使用 | ✅（数据已打包在 App 内）|
