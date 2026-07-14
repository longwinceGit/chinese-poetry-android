# 诗词乐园 v2.0 重构完整状态

> **最后更新**: 2026-07-01 17:25  
> **构建状态**: ✅ BUILD SUCCESSFUL（clean rebuild 36 tasks）  
> **APK**: app/build/outputs/apk/debug/app-debug.apk（31.5 MB，含 54 MB 诗词数据）  
> **技术栈**: Java 17 + AGP 8.2.2 + Gradle 8.7 + Material 3 + Room + Navigation Component  
> **目标**: targetSdk 34 / minSdk 21 / versionCode 2

---

## 📊 项目总览

| 维度 | 数据 |
|------|------|
| **Java 文件** | 35 个，4,679 行 |
| **Layout XML** | 11 个（全部 ConstraintLayout） |
| **Drawable XML** | 21 个（11 图标 + 3 打卡背景 + 2 Chip 背景 + 3 卡片/按钮 + 启动器 + FileProvider） |
| **导航** | 8 Fragment（4 Tab + 3 游戏 + 1 详情） |
| **诗词数据** | 91,196 首（11 朝代 × JSON 分片） |
| **释义** | 88 首著名诗词（100% 匹配） |
| **构建** | AGP 8.2.2 / Gradle 8.7 / JDK 17 / Material 1.11.0 |

---

## ✅ 已完成（51 项）

### 1. 构建配置

| # | 项 | 变更 |
|---|-----|------|
| 1 | `build.gradle`（根） | AGP 4.2.2 → **8.2.2**；阿里云镜像保留 |
| 2 | `app/build.gradle` | compileSdk/targetSdk 34；minSdk 21；versionCode 2；JDK 17 |
| 3 | Release 混淆 | `minifyEnabled true` + `shrinkResources true` + R8 |
| 4 | `proguard-rules.pro` | Room / Pinyin4j / Lottie 保留规则 |
| 5 | `gradle.properties` | `nonTransitiveRClass` + R8 full mode |
| 6 | `gradle-wrapper.properties` | Gradle 6.7.1 → **8.7** |
| 7 | Gradle wrapper | gradlew / gradlew.bat / gradle-wrapper.jar 生成 |

### 2. 安全与清单

| # | 项 | 变更 |
|---|-----|------|
| 8 | `usesCleartextTraffic` | 移除（HTTP 明文漏洞） |
| 9 | `FileProvider` | 新增，支持分享卡片图片 |

### 3. 主题与资源（Material 3 中国风）

| # | 项 | 变更 |
|---|-----|------|
| 10 | `colors.xml` | 全新调色板：墨色茶褐 + 宣纸色 + 印章朱红，66 色值 |
| 11 | `styles.xml` | `Theme.Material3.Light.NoActionBar` + `Widget.Poetry.*` |
| 12 | `strings.xml` | 99 行全中文文案提取 |
| 13 | 21 个 drawable | 11 图标 + 3 打卡背景 + bg_chip* + bg_card* + bg_button |
| 14 | 颜色硬编码 → 资源引用 | `bg_card`/`bg_button_primary`/`MatchCardAdapter` 全部使用 `@color/*` |

### 4. 导航架构（Navigation Component）

| # | 项 | 变更 |
|---|-----|------|
| 15 | `nav_graph.xml` | 8 个 Fragment（4 Tab + Detail + Quiz + Couplet + Match） |
| 16 | `bottom_nav_menu.xml` | 首页 / 学习 / 游戏 / 我的 |
| 17 | `activity_main.xml` | ConstraintLayout + NavHostFragment + BottomNavigationView |
| 18 | `MainActivity.java` | Navigation Component + LocalDate 日活统计 |

### 5. 首页

| # | 项 | 变更 |
|---|-----|------|
| 19 | `fragment_home.xml` | 搜索框 + 分类 Chip + RecyclerView + 每日推荐卡片 |
| 20 | `HomeFragment.java` | 搜索 debounce 300ms；著名诗词优先；加载更多；骨架屏/空状态 |
| 21 | `HomeViewModel.java` | LiveData 通知修复（新 ArrayList 副本）；每日推荐固定化 |
| 22 | `PoemRepository.java` | 按释义有无排序（著名诗词前置）；DailyStats/收藏/like 全功能 |

### 6. 详情页

| # | 项 | 变更 |
|---|-----|------|
| 23 | `fragment_detail.xml` | 古风排版：装饰条 + 标题/作者/朝代 + 诗句正文 + 拼音按钮 + 释义区 + 操作栏 |
| 24 | `DetailFragment.java` | 收藏/TTS/分享（Canvas 古风卡片→FileProvider PNG）/学习标记；markLearned 先 ensureRecordExists 幂等 |
| 25 | `DetailViewModel.java` | 收藏状态管理 |
| 26 | `PinyinLineView.java` | **自定义 View**：每汉字正上方显示拼音，多行自动折行，列等分自适应 |
| 27 | `TtsManager.java` | `OnInitListener` 回调区分就绪/错误，Toast 提示 |

### 7. 学习中心

| # | 项 | 变更 |
|---|-----|------|
| 28 | `fragment_learning.xml` | 打卡日历（7 列 Grid）+ 统计卡片 + 每日任务 |
| 29 | `LearningFragment.java` | 接入真实打卡数据（已签✓/断签灰/未签到浅色） |
| 30 | `LearningViewModel.java` | 自动签到 / streak 连签（断签重置、每 7 天升 1 级）/ 任务检测 |
| 31 | `LearningEngine.java` | `SimpleDateFormat` → `LocalDate` |

### 8. 游戏中心

| # | 项 | 变更 |
|---|-----|------|
| 32 | `fragment_game_hub.xml` | AlertDialog → 卡片式大厅 |
| 33 | `GameHubFragment.java` | 3 游戏卡片入口 |
| 34 | `GameEngine.java` | `MatchCard` 增强：selected / poemTitle / poemAuthor / poemInfo |
| 35 | `GameViewModel.java` | `toggleSelect()` / `matchTip` / `recordGameActivity()` |
| 36 | `MatchGameFragment.java` | **完全重写**：全可见 3×4 网格 + 选中高亮 + 消除动画 + 抖动 + 完成弹层 + lockInput |
| 37 | `MatchCardAdapter.java` | RecyclerView.Adapter；三种视觉状态 + animateEliminate/animateShake |
| 38 | `fragment_game_match.xml` | 玩法说明 + 配对提示 + 3 列网格 + 完成弹层 |
| 39 | `CoupletGameFragment.java` | 适配新 ID + Material 3 |
| 40 | `fragment_game_couplet.xml` | 重写布局 |

### 9. 答题系统

| # | 项 | 变更 |
|---|-----|------|
| 41 | `QuizFragment.java` | 填空可撤销（undoBlank）+ 提交按钮 + 全部填满自动提交；ViewGroup 类型修复 |
| 42 | `QuizViewModel.java` | 答题持久化 quizScore + DailyStats 更新 |

### 10. 个人中心

| # | 项 | 变更 |
|---|-----|------|
| 43 | `fragment_profile.xml` | 统计卡片 + 成就列表 + 设置入口 |
| 44 | `ProfileFragment.java` + `ProfileViewModel.java` | 数据绑定 |

### 11. 数据层

| # | 项 | 变更 |
|---|-----|------|
| 45 | `PoemLoader.java` | `readAsset(String)` → `JSONTokener(InputStreamReader)` 流式解析，消除 54 MB 双重内存 |
| 46 | `PoemDao.java` | 完整 15+ DAO 方法：签到/统计/收藏/学习/题目/释义 |
| 47 | `LearningDatabase.java` | MIGRATION_1_2（daily_stats 表），fallbackToDestructive 替换为安全迁移 |
| 48 | `poem_explanations.json` | 修复 3 个错键：靜夜思/春曉/涼州詞，88/88 全量匹配 ✅ |

### 12. 安全修复

| # | 项 | 变更 |
|---|-----|------|
| 49 | `SimpleDateFormat` 全局清除 | `LearningEngine` + `MainActivity` → `java.time.LocalDate` |
| 50 | `DetailFragment` | `markLearned`/`addFavorite` 先 ensureRecordExists() 幂等 |
| 51 | 资源冲突清理 | 删除重复 `ic_launcher_background`(values)/`ic_learning`/`dialog_poem_detail` |

---

## 🔲 未完成（8 项）

### 🟡 P1 — 1 项

| # | 项 | 详情 |
|---|-----|------|
| 1 | 暗色主题 | `values-night/` 未创建 |

### 🟢 P2 — 5 项

| # | 项 | 详情 |
|---|-----|------|
| 2 | 成就解锁动画 | 弹窗/撒花/ConfettiView 未接入 |
| 3 | Crashlytics | 无崩溃监控 |
| 4 | 单元测试 | 零覆盖 |
| 5 | CI/CD | 仅 `build-apk.bat` |
| 6 | Lottie 动画 | 依赖已加，未实际使用 |

### ⚪ P3 — 2 项

| # | 项 | 详情 |
|---|-----|------|
| 7 | PoemAdapter 颜色复查 | 无硬编码，需人工确认 |
| 8 | MatchCardAdapter 颜色复查 | 已在 P3 修复中完成 |

---

## 📊 完成度统计

| 类别 | 已完成 | 未完成 | 完成率 |
|------|--------|--------|--------|
| 构建配置 | 7 | 0 | **100%** |
| 安全与清单 | 2 | 0 | **100%** |
| 主题与资源 | 5 | 0 | **100%** |
| 导航架构 | 4 | 0 | **100%** |
| 首页/详情 | 9 | 0 | **100%** |
| 学习中心 | 4 | 0 | **100%** |
| 游戏中心 | 9 | 0 | **100%** |
| 答题系统 | 2 | 0 | **100%** |
| 个人中心 | 2 | 0 | **100%** |
| 数据层 | 4 | 0 | **100%** |
| 安全修复 | 3 | 0 | **100%** |
| 功能增强 | 0 | 6 | **0%** |
| 遗留清理 | 0 | 2 | **0%** |
| **合计** | **51** | **8** | **86%** |

---

## 🗺️ 代码地图

```
com.poetry/
├── MainActivity.java              # 入口，Navigation + 日活统计
├── PoemLoader.java                # 流式 JSON 解析（54 MB assets）
├── data/
│   ├── PoemRepository.java        # 核心数据源（排序/分页/每日推荐/释义）
│   ├── PoemDao.java               # Room DAO（15+ 查询方法）
│   ├── LearningDatabase.java      # Room DB（MIGRATION_1_2）
│   ├── LearningRecord.java        # 学习记录实体
│   ├── DailyStats.java            # 每日统计实体
│   ├── UserProfile.java           # 用户档案实体
│   └── model/Poem.java            # 诗词模型
├── domain/
│   ├── GameEngine.java            # 消消乐 MatchCard 引擎
│   ├── QuizGenerator.java         # 填空题目生成
│   ├── AchievementEngine.java     # 成就系统
│   ├── LearningEngine.java        # 学习引擎
│   └── ThemeManager.java          # 主题管理器
├── ui/
│   ├── home/HomeFragment.java     # 首页（搜索/分类/列表/每日推荐）
│   ├── home/HomeViewModel.java
│   ├── detail/DetailFragment.java # 详情（拼音/收藏/朗读/分享/学习）
│   ├── detail/DetailViewModel.java
│   ├── learning/LearningFragment.java  # 学习中心（打卡/任务/统计）
│   ├── learning/LearningViewModel.java
│   ├── game/GameHubFragment.java  # 游戏大厅
│   ├── game/MatchGameFragment.java    # 消消乐
│   ├── game/CoupletGameFragment.java  # 对诗
│   ├── game/GameViewModel.java
│   ├── game/MatchCardAdapter.java     # 消消乐卡片适配器
│   ├── quiz/QuizFragment.java     # 填空答题
│   ├── quiz/QuizViewModel.java
│   ├── profile/ProfileFragment.java   # 个人中心
│   ├── profile/ProfileViewModel.java
│   ├── adapter/PoemAdapter.java       # 诗词列表适配器
│   ├── adapter/AchievementAdapter.java # 成就列表适配器
│   └── widget/ConfettiView.java       # 撒花效果（未接入）
└── util/
    ├── PinyinLineView.java        # 逐字拼音自定义 View
    ├── PinyinHelper.java          # 拼音转换工具
    └── TtsManager.java            # TTS 语音合成管理
```

---

## 🔧 关键设计决策

| 决策 | 原因 |
|------|------|
| 纯 Java + XML View | 用户要求，不迁移 Kotlin/Compose |
| ConstraintLayout 全部布局 | 替代原 LinearLayout 嵌套，减少层级 |
| 释义在详情页自动展示 | 代码链路：PoemLoader→PoemRepository→DetailFragment，有释义则显示，无则隐藏 |
| 拼音逐字 display | `PinyinLineView` 每汉字正上方显示拼音，weight 等分 + 自动折行 |
| 消消乐 = 全可见配对消除 | 非记忆翻牌，3×4 网格选上句配对下句 |
| LiveData 通知修复 | 每次创建新 ArrayList 副本触发 observer |
| 每日推荐固定化 | `LocalDate.now().hashCode()` 种子固定（非 Math.random()） |
