# 诗词乐园 — 儿童古诗词学习 Android App

## 项目说明

纯原生 Android 古诗词学习应用，面向儿童设计。91,196 首诗词数据离线打包，无需网络。从 [chinese-poetry](https://github.com/chinese-poetry/chinese-poetry) 数据集提取整理，采用 MVVM 架构 + Navigation Component 单 Activity 多 Fragment 模式。

---

## 一、技术栈

| 层次 | 技术选型 | 用途 |
|------|---------|------|
| **语言** | Java 8+ | 原生 Android 开发 |
| **架构** | MVVM | ViewModel + LiveData + Repository |
| **导航** | Navigation Component | 单 Activity + 多 Fragment + BottomNavigation |
| **数据库** | Room (v2) | 用户档案 / 学习记录 / 每日统计 |
| **UI** | ConstraintLayout + Material Components | 全量 ConstraintLayout 重写 |
| **数据解析** | org.json (JSONTokener 流式) | 54 MB 诗词 JSON 分片解析 |
| **语音** | Android TextToSpeech | 中文朗读（语速 0.85x） |
| **拼音** | pinyin4j | 逐字带音调拼音标注 |
| **构建** | AGP 8.2.2 + Gradle 8.7 + JDK 17 | targetSdk 34, minSdk 21 |
| **动画** | 自定义 ConfettiView | 粒子 + Emoji 混合庆祝动画 |

---

## 二、工程架构

### 2.1 分层架构图

```
┌─────────────────────────────────────────────────────┐
│                    UI 层 (Fragment)                   │
│  Home │ Detail │ Learning │ Quiz │ Profile │ Game   │
│                ViewModel + LiveData                  │
├─────────────────────────────────────────────────────┤
│                 领域层 (domain)                       │
│  GameEngine │ LearningEngine │ AchievementEngine     │
│  QuizGenerator │ ThemeManager                        │
├─────────────────────────────────────────────────────┤
│                 数据层 (data)                         │
│  PoemRepository → PoemLoader (JSON assets)           │
│  LearningDatabase → PoemDao (Room SQLite)            │
│  Poem / LearningRecord / UserProfile / DailyStats    │
├─────────────────────────────────────────────────────┤
│                 工具层 (util)                         │
│  PinyinHelper │ TtsManager │ PinyinLineView          │
│  ConfettiView (widget)                               │
├─────────────────────────────────────────────────────┤
│                 资源层 (assets)                       │
│  91,196 首诗词 JSON (54 MB 分片)                    │
│  88 首著名诗词释义 (poem_explanations.json)          │
└─────────────────────────────────────────────────────┘
```

### 2.2 包结构

```
com.poetry/
├── MainActivity.java                    ← 唯一 Activity：容器 + BottomNavigation + DB 初始化
│
├── PoemLoader.java                      ← assets JSON 流式解析 + 释义匹配
│
├── data/                                ← 数据层
│   ├── PoemRepository.java              ← 诗词数据仓库（单例，线程池异步加载）
│   ├── LearningDatabase.java            ← Room 数据库定义 (v2, 3 表 + Migration)
│   ├── PoemDao.java                     ← Room DAO（148 行，30+ 查询方法）
│   ├── UserProfile.java                 ← 用户档案实体（积分/等级/连续天数/成就/主题）
│   ├── LearningRecord.java              ← 学习记录实体（收藏/已学/答题/游戏）
│   ├── DailyStats.java                  ← 每日统计实体（打卡日历数据源）
│   └── model/
│       └── Poem.java                    ← 诗词数据模型（11 个字段）
│
├── domain/                              ← 领域逻辑层（纯计算，无 Android 依赖）
│   ├── GameEngine.java                  ← 游戏引擎：消消乐配对 + 接龙出题 + 积分
│   ├── LearningEngine.java              ← 学习引擎：9 级称号 / 进度 / 连续学习算法
│   ├── QuizGenerator.java               ← 填空出题：智能挖空 + 干扰项生成
│   ├── AchievementEngine.java           ← 12 项成就自动检测 + 解锁回调
│   └── ThemeManager.java                ← 9 个主题按等级/连续天数解锁
│
├── ui/                                  ← 表现层（8 Fragment + 2 Adapter）
│   ├── home/ HomeFragment + HomeViewModel           ← 首页 Tab
│   ├── detail/ DetailFragment + DetailViewModel     ← 诗词详情页
│   ├── learning/ LearningFragment        ← 学习成就 Tab
│   ├── profile/ ProfileFragment          ← 个人档案 Tab
│   ├── quiz/ QuizFragment                ← 填空挑战（二级页面）
│   ├── game/
│   │   ├── GameHubFragment               ← 游戏大厅 Tab
│   │   ├── CoupletGameFragment           ← 诗词接龙（二级页面）
│   │   ├── MatchGameFragment             ← 消消乐配对（二级页面）
│   │   └── GameViewModel                 ← 游戏共用 ViewModel
│   ├── adapter/
│   │   ├── PoemAdapter.java              ← 首页网格适配器（弹簧动画）
│   │   └── MatchCardAdapter.java         ← 消消乐卡片适配器（3 种状态）
│   └── widget/
│       └── ConfettiView.java             ← 庆祝动画（60 粒子，180 帧）
│
└── util/                                ← 工具层
    ├── PinyinHelper.java                 ← pinyin4j 封装（带声调标注）
    ├── PinyinLineView.java               ← 逐字拼音视图（自定义 LinearLayout）
    └── TtsManager.java                   ← TTS 朗读封装
```

### 2.3 模块依赖关系

```
MainActivity
  ├─→ BottomNavigation (4 Tab: Home / Learn / Game / Profile)
  ├─→ LearningDatabase (Room 初始化 + 连续签到检测)
  └─→ ConfettiView (全局庆祝入口)
       │
HomeFragment ←→ HomeViewModel ←→ PoemRepository ←→ PoemLoader
  │  ↓ LiveData                                        ↓ JSON assets (54 MB)
  │  poems / categories / dailyPoem                nav.json → dynasty_index.json → poems
  │  totalCount / loadingState                     poem_explanations.json → Map<title|author, text>
  └─→ navigate(DetailFragment) [Safe Args 传 9 参]
       │
DetailFragment ←→ DetailViewModel
  ├─→ PinyinLineView (逐字拼音, MIN_CELL_DP=20dp)
  ├─→ TtsManager (TTS 朗读)
  ├─→ LearningDatabase (收藏/已学状态)
  └─→ generateShareCard() (Canvas 绘制分享卡片)

GameHubFragment
  ├─→ navigate(CoupletGameFragment) [对诗]
  └─→ navigate(MatchGameFragment) [消消乐]
       │
MatchGameFragment ←→ GameViewModel ←→ GameEngine
  └─→ MatchCardAdapter (选中/配对/消除/抖动 动画)

LearningFragment / ProfileFragment
  └─→ LearningDatabase (LiveData 实时查询)
       ↓
AchievementEngine (成就检测 → 回调触发庆祝)
LearningEngine   (积分/等级/连续学习算法)
ThemeManager     (9 主题按等级/天数解锁)
```

---

## 三、运行时逻辑详解

### 3.1 冷启动流程

```
App 启动
  │
  ├─ Application.onCreate()
  │   └─ （无特殊初始化）
  │
  └─ MainActivity.onCreate()
      ├─ setContentView(R.layout.activity_main)
      │   ├─ NavHostFragment (Navigation Component)
      │   ├─ BottomNavigationView (4 Tab)
      │   └─ ConfettiView (浮层覆盖)
      │
      ├─ NavigationUI.setupWithNavController(bottomNav, navController)
      │   └─ 自动绑定 tab 切换 → navController.navigate()
      │
      ├─ initDatabase()
      │   └─ 后台线程:
      │       ├─ LearningDatabase.getInstance(context)
      │       │   └─ Room.databaseBuilder("poetry_learning.db").build()
      │       │
      │       ├─ poemDao().getUserProfileSync()
      │       │   ├─ null → 新建 UserProfile (streak=1, level=1)
      │       │   └─ 存在 → LearningEngine.calcStreak() 续签
      │       │       └─ streak ≥ 7 且每周倍数 → Toast 🎉
      │       └─ （数据库就绪，无需等待即可渲染 UI）
      │
      └─ startDestination: nav_home (HomeFragment)
```

### 3.2 诗词数据加载流程

```
HomeFragment.onViewCreated()
  └─ viewModel.loadPoems()
      └─ PoemRepository.loadPoemsAsync(assets)
          └─ ExecutorService.submit(Callable):
              │
              ├─ 1. PoemLoader.loadAll(assets)         ← 后台线程
              │   ├─ 1a. readJsonArray("nav.json")
              │   │   └─ 遍历朝代 → 遍历文件 → 逐文件 readJsonArray
              │   │       └─ JSONTokener 流式解析（复用 StringBuilder）
              │   │
              │   ├─ 1b. parsePoem()  × 91,196 次
              │   │   └─ Poem(id, title, author, dynasty, category, tag, emoji, lines)
              │   │       ├─ tag: 朝代→简写 (宋代→"song", 唐代→"tang")
              │   │       └─ emoji: index % 10 循环分配
              │   │
              │   └─ 1c. loadExplanations("poem_explanations.json")
              │       └─ Map<title|author, text> (88 条)
              │       └─ 遍历 allPoems 按 explanationKey() 匹配
              │
              ├─ 2. Collections.sort(poems, comparator)
              │   └─ 有释义的著名诗词排在前面（explanation != null 优先）
              │
              └─ 3. buildCategories()
                  └─ 按年代顺序: 先秦→春秋→魏晋→唐代→五代→宋代→元代→清代
                  └─ 首项 "全部"，各朝代带图标 (📜🍂🏯🎋🐎🏮)

加载中状态          loaded=false → loadingContainer VISIBLE
加载完成回调        poems.postValue(allPoems) → Observer 触发渲染
                 categories.postValue(list)  → ChipGroup 重建
                 dailyPoem.postValue(poem)   → 每日卡片刷新
```

### 3.3 首页诗词排序

```
PoemRepository.loadPoemsAsync() 完成后:
  ┌─────────────────────────────────────────┐
  │  Comparator:                             │
  │    if (a.explanation != null &&           │
  │        b.explanation == null) return -1  │ ← a 有释义，排前面
  │    if (a.explanation == null &&           │
  │        b.explanation != null) return 1   │ ← b 有释义，排前面
  │    return 0                              │ ← 同等，保序
  └─────────────────────────────────────────┘
  结果: 88 首著名诗词 → 91,108 首普通诗词

分页: PAGE_SIZE = 30
  第 1 页: [0..29]   (全部是著名诗词)
  第 2 页: [30..59]  (部分著名 + 普通)
  第 3 页: [60..89]  (最后 28 首著名 + 2 首普通)
  ...
  第 N 页: 普通诗词

加载更多:
  loadMore() → 创建 new ArrayList<>(old) 副本
            → newList.addAll(subList(start, end))
            → poems.setValue(newList)  ← 新引用触发 LiveData 通知
            → Observer → adapter.setPoems() → notifyDataSetChanged()
            → updateLoadMoreUI() 恢复按钮状态
```

### 3.4 搜索流程

```
用户输入 → TextWatcher.onTextChanged()
  └─ 清除旧 Runnable → postDelayed(500ms)
      └─ viewModel.search(query)
          └─ PoemRepository.search(query)
              ├─ 扫描 title / author / fullText 含 query
              ├─ O(n) 线性扫描 91K 条（纯内存，无索引）
              └─ 结果 > 500 → 截断至 500

搜索模式:
  searchMode = true
  ├─ 禁用分类筛选
  ├─ 禁用加载更多
  └─ 全部加载时显示 "已加载全部结果" 提示
```

### 3.5 详情页渲染流程

```
HomeFragment.navigateToDetail(poem)
  └─ Safe Args → NavController.navigate(R.id.nav_detail, bundle)
      └─ DetailFragment.readArgs()
          └─ 从 Bundle 取 9 个参数（id/title/author/dynasty/category/tag/emoji/lines/explanation）
              │
              ├─ 基础信息: tvTitle / tvEmoji / tvAuthor / tvDynasty
              │
              ├─ 标签: chipTag (朝代色 + tag 简写)
              │   └─ 唐→tang→#C62828, 宋→song→#2E7D32, 先秦→qin→#5D4037
              │
              ├─ 诗句: renderPoemLines(showPinyin=false)
              │   └─ 每行 → createLineTextView() → addView
              │
              ├─ 释义: renderExplanation()
              │   └─ hasExplanation() ? VISIBLE : GONE（自动显隐）
              │
              └─ 操作按钮:
                  ├─ 拼音: 切换 showPinyin → 重建 PinyinLineView 或 TextView
                  ├─ 朗读: TtsManager.speak(标题+作者+诗句) / stop()
                  ├─ 收藏: Room 异步 addFavorite/removeFavorite
                  ├─ 已学: Room 异步 markLearned
                  └─ 分享: Canvas 手绘 750×N px 古风卡片 → FileProvider → Intent.ACTION_SEND
```

### 3.6 游戏引擎运行逻辑

#### 消消乐 (MatchGame)

```
MatchGameFragment.onViewCreated()
  └─ GameViewModel.loadMatchGame()
      └─ PoemRepository.getAllPoems()
          └─ GameEngine.generateMatchGame(pool, pairs=6)
              ├─ 随机 6 首诗词（≥2 行且非省略号）
              ├─ 每首取 lines[0]（上句）+ lines[1]（下句）
              ├─ 共 12 张卡片，全部可见
              └─ Collections.shuffle() 打乱

玩家操作:
  ├─ 点击卡片 A → selected=true → 红色高亮 + 缩放动画
  │   └─ Adapter: bg_card_selected (红边框) + scaleX/Y=1.05
  │
  ├─ 点击卡片 B (同为 A) → 取消选中 (toggle)
  ├─ 点击卡片 B (不同) → checkMatch()
  │   ├─ pairId 相同且 isFirstHalf != → ✅ 配对成功
  │   │   └─ 200ms 延迟 → 绿色消除动画（缩小+旋转+淡出）
  │   │   └─ matched=true → 4f alpha=0 + scale=0.5
  │   │   └─ matchTip "「标题 · 作者」 配对成功！"
  │   │
  │   └─ 否则 → ❌ 失败
  │       └─ 红色抖动动画 (translateX ±6dp × 3 次, 100ms)
  │       └─ 重置 selected=false
  │
  └─ GameEngine.isGameComplete() → 弹层：得分 + 尝试次数

输入保护: lockInput 在动画期间阻止再次点击
得分: max(5, 50 - (attempts - totalPairs) * 3)
```

#### 诗词接龙 (Couplet)

```
CoupletGameFragment.onViewCreated()
  └─ GameViewModel.loadCoupletGame(rounds=5)
      └─ GameEngine.generateCoupletGame(pool, 5)
          ├─ 随机 5 首诗词
          ├─ 每首随机取相邻两句 lines[i] + lines[i+1]
          └─ 从其他诗词取 3 个干扰句 → shuffle 成 4 选项

每轮:
  ├─ 显示上句 (givenLine)
  ├─ 点选下句 (4 选 1)
  │   ├─ 正确 → 绿底 + 得分 (10 + streak×2) + streak++
  │   └─ 错误 → 红底 + 显示正确答案 + streak=0
  └─ 5 轮后结算总分

得分公式: base(10) + streakBonus(streak×2)
```

---

## 四、数据流全景

### 4.1 数据源

| 数据 | 位置 | 格式 | 大小 |
|------|------|------|------|
| 诗词正文 | `assets/web/data/` | JSON 分片（按朝代） | ~54 MB |
| 分类索引 | `assets/web/data/nav.json` | JSON 数组 | < 1 KB |
| 著名诗词释义 | `assets/poem_explanations.json` | JSON 数组（k/e） | ~6 KB |
| 用户数据 | `poetry_learning.db` | SQLite (Room) | < 1 MB |

### 4.2 数据表结构 (Room)

```
┌─ learning_records ───────────────────────────────────┐
│ poemId (PK) │ title │ dynasty │ author                │
│ learnedAt │ quizScore │ favorite │ gamePlayed         │
│ 用途: 收藏列表 / 已学标记 / 答题得分 / 游戏参与记录   │
└──────────────────────────────────────────────────────┘

┌─ user_profile ───────────────────────────────────────┐
│ id=1 (PK) │ totalPoints │ level │ streak              │
│ lastActiveDate │ unlockedThemes │ achievements        │
│ 用途: 等级/积分/连续学习/主题解锁/成就解锁 (JSON 列)    │
└──────────────────────────────────────────────────────┘

┌─ daily_stats ────────────────────────────────────────┐
│ date (PK) │ poemsLearned │ quizCompleted               │
│ pointsEarned │ gamesPlayed                            │
│ 用途: 每日统计 / 打卡日历 / 学习趋势                    │
└──────────────────────────────────────────────────────┘
```

### 4.3 LiveData 观察链

```
PoemRepository (后台线程池, Future<List<Poem>>)
  └→ HomeViewModel.loadPoems()
      ├── poems          ← MutableLiveData<List<Poem>>     → RecyclerView 渲染
      ├── categories     ← MutableLiveData<List<String>>   → ChipGroup 重建
      ├── dailyPoem      ← MutableLiveData<Poem>           → 每日卡片
      ├── totalCount     ← MutableLiveData<Integer>        → "共 N 首"
      ├── isLoading      ← MutableLiveData<Boolean>        → 加载动画
      └── loadingMessage ← MutableLiveData<String>         → 加载提示文字

LearningDatabase (Room)
  └→ Fragment 直接 observe LiveData:
      ├── getFavorites()         → LearningFragment
      ├── getLearnedCount()      → ProfileFragment
      └── getUserProfile()       → ProfileFragment
```

---

## 五、UI 组件与布局

### 5.1 底部导航 (4 Tab)

| Tab | Fragment | 功能 |
|-----|----------|------|
| 📖 诗词 | HomeFragment | 每日推荐 + 朝代分类 + 网格浏览 + 搜索 |
| 🎯 学习 | LearningFragment | 成就展示 + 我的收藏 |
| 🎮 游戏 | GameHubFragment | 游戏大厅 → 对诗 / 消消乐 |
| 👤 我的 | ProfileFragment | 等级进度 + 连续学习 + 周统计 |

### 5.2 二级页面（导航栈）

| 页面 | 导航方式 | 传参 |
|------|---------|------|
| 诗词详情 | Safe Args (9 参数) | id/title/author/dynasty/category/tag/emoji/lines/explanation |
| 填空挑战 | nav_quiz | 全局 repo 取数据 |
| 对诗 | nav_game_couplet | 全局 repo 取数据 |
| 消消乐 | nav_game_match | 全局 repo 取数据 |

### 5.3 布局文件 (11 文件)

```
fragment_home.xml          ← 首页（每日卡片 + 分类 ChipGroup + RecyclerView）
fragment_detail.xml        ← 诗词详情（ConstraintLayout, 逐字拼音/释义 动态渲染）
fragment_learning.xml      ← 学习成就
fragment_profile.xml       ← 个人档案（等级进度条 + 周统计柱状图）
fragment_quiz.xml          ← 填空挑战
fragment_game_hub.xml      ← 游戏大厅
fragment_game_match.xml    ← 消消乐（3 列网格 + 完成弹层）
fragment_game_couplet.xml  ← 诗词接龙
activity_main.xml          ← 根布局（NavHost + BottomNav + ConfettiView）
item_poem_card.xml         ← 首页网格卡片（ConstraintLayout）
item_match_card.xml        ← 消消乐卡片
```

---

## 六、领域引擎详解

### 6.1 LearningEngine — 学习引擎

```
等级系统 (9 级):
  L1 诗词小学徒 (0)   → L2 小秀才 (100)
  L3 小举人 (300)     → L4 小进士 (600)
  L5 小翰林 (1000)    → L6 大诗仙 (2000)
  L7 诗词宗师 (3500)  → L8 一代文豪 (5000)
  L9 千古诗圣 (8000)

连续学习算法 calcStreak():
  昨天活跃 → streak + 1
  今天已活跃 → streak 不变
  间隔跳过 → streak 重置为 1

积分规则:
  学习诗词        +10
  答题 (all correct) +20
  答题 (≥80%)      +15
  答题 (≥50%)      +10
  答题 (<50%)      +5
  连续 3/7/14/21/30 天里程碑 → +10/+30/+50/+80/+100
```

### 6.2 AchievementEngine — 成就系统 (12 项)

| ID | 名称 | 条件 |
|----|------|------|
| first_poem | 初出茅庐 | 学习 1 首 |
| poem_10 | 小有积累 | 学习 10 首 |
| poem_50 | 学富五车 | 学习 50 首 |
| poem_100 | 诗词达人 | 学习 100 首 |
| streak_7 | 坚持不懈 | 连续 7 天 |
| streak_30 | 持之以恒 | 连续 30 天 |
| favorite_10 | 初代收藏家 | 收藏 10 首 |
| favorite_20 | 收藏达人 | 收藏 20 首 |
| quiz_perfect_5 | 满分达人 | 填空满分 5 次 |
| game_10 | 游戏高手 | 游戏 10 次 |
| level_5 | 小有名气 | 等级 5 |
| level_9 | 千古诗圣 | 等级 9 |

### 6.3 ThemeManager — 主题系统 (9 个)

| ID | 名称 | 解锁条件 |
|----|------|---------|
| default | 墨韵 🖋️ | 默认 |
| spring | 春意 🌸 | L2 |
| summer | 夏荷 🌻 | L3 + streak≥3 |
| autumn | 秋月 🌙 | L4 + streak≥7 |
| winter | 冬雪 ❄️ | L5 + streak≥14 |
| bamboo | 竹韵 🎋 | L6 |
| lotus | 莲心 🪷 | L7 |
| golden | 金榜 🏅 | L8 |
| legend | 传奇 👑 | L9 |

---

## 七、拼音与朗读系统

### 7.1 拼音标注

```
PinyinHelper (pinyin4j 引擎):
  toTonePinyin('中') → "zhōng"  (带声调 Unicode)
  toPinyinList("床前明月光")
    → ["chuáng","qián","míng","yuè","guāng"]
  标点/符号 → "" 空串（占位）

PinyinLineView (自定义 View):
  结构: 外层 VERTICAL → 内层多行 HORIZONTAL
  每行拆分: ceil(len / maxCharsPerRow)
  maxCharsPerRow: (屏幕宽 - 48dp) / 20dp → 360dp 屏 ≈ 15 字/行
  每列: 拼音(10sp, α=0.75) + 汉字(20sp), layout_weight=1 等分
```

### 7.2 TTS 朗读

```
TtsManager:
  初始化: TextToSpeech(context, OnInitListener)
  语言: Locale.CHINESE
  语速: 0.85x (适合学习)
  朗读模式: QUEUE_FLUSH (打断当前朗读)
  拼接格式: "标题。作者。诗句1。诗句2。……"
  双击朗读按钮 → stop() 暂停
  失败提示: "中文语音数据不可用，请安装中文语音包"
```

---

## 八、构建与运行

### 8.1 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 17 |
| Gradle | 8.7 |
| Android Gradle Plugin | 8.2.2 |
| compileSdk | 34 |
| minSdk | 21 (Android 5.0) |
| targetSdk | 34 |

### 8.2 构建命令

```bash
# 调试版
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

# 清理重建
./gradlew clean assembleDebug
```

### 8.3 一键构建 (Windows)

直接双击运行 `build-apk.bat`。

---

## 九、核心设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 导航方式 | Navigation Component + Safe Args | 类型安全传参，Fragment 事务自动管理 |
| 数据加载 | ExecutorService 单线程池 + Future | 避免阻塞主线程，Future 可取消 |
| 诗词排序 | explanation 非空优先 | 著名诗词（88 首）永远在第一页 |
| 释义匹配 | `title\|author` key | 唯一索引，O(1) 查找 |
| 分页策略 | Adapter 全量 + 切片显示 | 无网络请求，纯内存操作 |
| 拼音渲染 | 自定义 LinearLayout (layout_weight) | 自适应屏幕宽度 + 多行折行 |
| 游戏输入锁 | boolean lockInput + 动画回调 | 防止动画期间的重复点击 |
| LiveData 通知 | 每次创建新 ArrayList 副本 | 解决 LiveData.setValue(sameRef) 不通知 Bug |
| JSON 解析 | JSONTokener (非流式,但复用 SB) | org.json 简单可靠，54 MB 可在 3-5s 内完成 |
| 数据库版本 | v2 + MIGRATION_1_2 | daily_stats 表非破坏性迁移 |

---

## 十、性能指标

| 指标 | 当前值 | 目标 |
|------|-------|------|
| 冷启动加载 | 3-5s (91K 首全量) | < 2s (首屏预加载 500 首) |
| APK 大小 | ~28 MB | < 30 MB |
| 内存占用 | ~150 MB (全量加载) | < 100 MB |
| 页面切换 | < 16ms (60fps) | ✅ |
| 搜索响应 | < 50ms (内存扫描) | ✅ |
| 拼音切换 | < 16ms (View 重建) | ✅ |

---

## 十一、待优化项 (P0)

1. **启动性能**: 首屏只加载 500 首著名诗词，其余延迟加载
2. **暗色主题**: 新建 `values-night/` 适配暗色模式
3. **主题切换**: ProfileFragment 加 9 主题入口
4. **成就庆祝**: 触发 ConfettiView + Snackbar 通知
5. **无障碍**: 所有 View 添加 contentDescription

---

## 十二、数据说明

诗词数据来自 [chinese-poetry](https://github.com/chinese-poetry/chinese-poetry)，存放在 `app/src/main/assets/web/data/`：

- `nav.json` — 朝代 → 文件名映射
- `唐代.0.json` ~ `唐代.254.json` — 各朝代诗词正文（JSON 分片）
- `app/src/main/assets/poem_explanations.json` — 88 首著名诗词白话释义
