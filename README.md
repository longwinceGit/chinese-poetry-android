# 诗词乐园 — 儿童古诗词学习 Android App

## 项目说明

纯原生 Android 古诗词学习应用，面向儿童设计。数据离线打包，无需网络。从 [chinese-poetry](https://github.com/chinese-poetry/chinese-poetry) 数据集提取整理，采用 MVVM 架构。

## 技术栈

- **MVVM**（LiveData + ViewModel + Room）
- **Java** 原生开发
- 最低支持 Android 5.0（API 21），目标 SDK 33
- **Fragment** 导航（无 WebView）
- **TextToSpeech** 朗读 + **TinyPinyin** 拼音标注
- **Room** 本地持久化
- 自定义 **ConfettiView** 庆祝动画（粒子+Emoji）

## 项目结构

```
android/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/poetry/
│       │   ├── MainActivity.java          ← Fragment 容器 + 导航入口
│       │   ├── data/
│       │   │   ├── PoemRepository.java    ← 统一数据源
│       │   │   ├── PoemDao.java           ← Room DAO
│       │   │   ├── LearningDatabase.java  ← Room 数据库
│       │   │   ├── UserProfile.java       ← 用户档案实体
│       │   │   ├── LearningRecord.java    ← 学习记录实体
│       │   │   ├── DailyStats.java        ← 每日统计实体
│       │   │   └── model/
│       │   │       └── Poem.java          ← 诗词数据模型
│       │   ├── domain/
│       │   │   ├── LearningEngine.java    ← 积分/等级/连续学习算法
│       │   │   ├── QuizGenerator.java     ← 填空出题算法
│       │   │   ├── GameEngine.java        ← 接龙+配对游戏引擎
│       │   │   ├── AchievementEngine.java ← 12项成就自动检测
│       │   │   └── ThemeManager.java      ← 9个主题解锁管理
│       │   ├── ui/
│           │   │   ├── home/  HomeFragment       ← 首页(推荐/分类/网格/搜索)
│           │   │   ├── detail/ DetailFragment    ← 诗词详情+收藏+拼音
│           │   │   ├── learning/ LearningFragment← 成就+收藏
│           │   │   ├── profile/ ProfileFragment  ← 档案(等级/统计/周图)
│           │   │   ├── quiz/ QuizFragment        ← 填空背诵
│       │   │   ├── game/
│       │   │   │   ├── CoupletGameFragment   ← 诗词接龙
│       │   │   │   ├── MatchGameFragment     ← 翻翻卡配对
│       │   │   │   └── MatchCardAdapter
│       │   │   ├── adapter/
│           │   │   │   ├── PoemAdapter.java      ← 点击弹簧动画
│           │   │   │   └── AchievementAdapter.java
│           │   │   └── widget/
│           │   │       └── ConfettiView.java  ← 升级粒子+Emoji动画
│           │   └── util/
│           │       ├── TtsManager.java        ← TTS 朗读封装
│           │       └── PinyinHelper.java      ← 拼音标注(TinyPinyin)
│       ├── res/
│       │   ├── layout/                    ← 布局文件
│       │   ├── drawable/                  ← 形状/按钮/图标资源
│       │   ├── anim/                      ← Fragment 过渡动画
│       │   ├── values/ (colors/strings/styles)
│       │   └── mipmap-anydpi-v26/
│       └── assets/web/data/
│           ├── nav.json                   ← 分类索引
│           ├── poems_*.json               ← 诗词数据
│           └── search_*.json              ← 搜索索引
├── build.gradle
├── settings.gradle
├── build-apk.bat
├── README.md
└── BUILD.md
```

## 功能特性

| 功能 | 实现 |
|------|------|
| 每日推荐 | ✅ 随机每日一首，连续学习记录 |
| 朝代分类筛选 | ✅ 先秦/春秋/魏晋/唐代/五代/宋代/元代/清代 |
| 诗词搜索 | ✅ 标题/作者/内容全文搜索（实时过滤） |
| 诗词朗读 | ✅ TextToSpeech 中文语音 |
| 收藏功能 | ✅ 每首诗词可收藏 |
| 学习记录 | ✅ Room 本地持久化 |
| 庆祝动画 | ✅ 里程碑触发彩纸动画 |
| 连续学习 | ✅ 连续天数统计 + 7 天提醒 |
| 分页加载 | ✅ 每页 30 首，逐步加载 |
| 离线使用 | ✅ 全部数据打包在 Apk 内 |
| 填空背诵 | ✅ 智能挖空 + 候选词点选 |
| 诗词接龙 | ✅ 多选答题 + 连击加分 |
| 翻翻卡配对 | ✅ 记忆配对游戏 |
| 成就系统 | ✅ 12 项成就自动解锁 |
| 主题角色 | ✅ 9 个主题按等级解锁 |
| 等级系统 | ✅ 9 级称号 + 积分进度条 |
| 个人档案 | ✅ 专属 ProfileFragment + 周统计柱状图 |
| 拼音标注 | ✅ TinyPinyin 逐字注音 + 一键切换 |
| 点击动效 | ✅ 卡片弹簧缩放 + 收藏❤️弹性动画 |
| 彩纸动画 | ✅ 粒子+Emoji 混合升级版 ConfettiView |

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
- `poems_*.json` — 各朝代诗词正文
- `search_*.json` — 搜索索引文件

## 注意事项

- 数据文件仅在构建时打包，运行时从 `assets` 读取
- TTS 朗读依赖系统 TTS 引擎（中文语音包需先安装）
- Room 数据库在首次运行时创建，开发阶段使用 `fallbackToDestructiveMigration()`
