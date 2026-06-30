# 诗词乐园 — 儿童古诗词学习 Android App

## 项目说明

纯原生 Android 古诗词学习应用，面向儿童设计。数据离线打包，无需网络。从 [chinese-poetry](https://github.com/chinese-poetry/chinese-poetry) 数据集提取整理。

## 技术栈

- **纯原生 UI**（RecyclerView + CardView），无 WebView
- **Java** 原生开发
- 最低支持 Android 5.0（API 21），目标 SDK 33
- **TextToSpeech** 朗读
- 自定义 **ConfettiView** 庆祝动画

## 项目结构

```
android/
├── app/
│   ├── build.gradle              ← 模块构建配置
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/poetry/
│       │   ├── MainActivity.java     ← 主界面（分类/搜索/每日推荐）
│       │   ├── Poem.java             ← 诗词数据模型
│       │   ├── PoemAdapter.java      ← 诗词卡片适配器
│       │   ├── PoemLoader.java       ← JSON 数据加载
│       │   └── ConfettiView.java     ← 庆祝彩纸动画
│       ├── res/
│       │   ├── layout/
│       │   │   ├── activity_main.xml      ← 主界面布局
│       │   │   ├── item_poem_card.xml     ← 诗词网格卡片
│       │   │   └── dialog_poem_detail.xml ← 诗词详情弹窗
│       │   ├── drawable/                  ← 形状/按钮/图标资源
│       │   ├── values/
│       │   │   ├── colors.xml
│       │   │   ├── strings.xml
│       │   │   └── styles.xml
│       │   └── mipmap-anydpi-v26/
│       └── src/main/assets/web/
│           ├── data/nav.json             ← 分类索引
│           ├── data/poems_*.json         ← 诗词数据
│           └── data/search_*.json        ← 搜索索引
├── build.gradle                  ← 根构建配置（Gradle 4.2.2）
├── settings.gradle               ← 项目设置
├── build-apk.bat                 ← Windows 一键构建脚本
├── README.md
└── BUILD.md
```

## 功能特性

| 功能 | 实现 |
|------|------|
| 每日推荐 | ✅ 随机每日一首，连续学习记录 |
| 朝代分类筛选 | ✅ 先秦/春秋/魏晋/唐代/五代/宋代/元代/清代 |
| 诗词搜索 | ✅ 标题/作者/内容全文搜索 |
| 诗词朗读 | ✅ TextToSpeech 中文语音 |
| 收藏功能 | ✅ 每首诗词可收藏 |
| 学习记录 | ✅ 本地持久化（SharedPreferences） |
| 庆祝动画 | ✅ 每学 3 首触发彩纸动画 |
| 连续学习 | ✅ 连续天数统计，7 天提醒 |
| 分页加载 | ✅ 每页 30 首，逐步加载 |
| 离线使用 | ✅ 全部数据打包在 Apk 内 |

## 构建步骤

### 前置条件

- Android Studio（推荐）或 Android SDK
- JDK 11+
- Android 5.0+ 手机或模拟器

### Android Studio（推荐）

1. 用 Android Studio 打开本项目目录
2. 等待 Gradle 同步完成
3. 连接手机或启动模拟器
4. 点击 **Run ▶**

### 命令行构建

```bash
# Windows
gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

### 一键构建（Windows）

直接双击运行 `build-apk.bat`，自动下载 Gradle 7.5 并构建。

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 数据说明

诗词 JSON 文件存放在 `app/src/main/assets/web/data/` 下：

- `nav.json` — 朝代与文件名映射
- `poems_*.json` — 各朝代诗词正文（JSON 数组，字段见 `Poem.java`）
- `search_*.json` — 搜索索引文件
- `index.html` — **不再使用**，仅保留兼容

## 注意事项

- 数据文件仅在构建时打包，运行时从 `assets` 读取
- TTS 朗读依赖系统 TTS 引擎（中文语音包需先安装）
