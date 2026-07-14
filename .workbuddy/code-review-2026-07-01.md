# 诗词乐园 v2.0 — 代码审查报告

> 审查日期: 2026-07-01 | 审查人: 火眼眼 | 范围: 35 Java / 4,679 行
> 
> BUILD SUCCESSFUL · APK 31.5MB · minSdk 21 · targetSdk 34

---

## 一、总体印象

整体架构清晰：MVVM + Room + Navigation Component，分包合理。关键路径（诗词加载/详情/游戏/学习记录）已打通并编译通过。主要问题是**工程化深度不足**——线程管理散乱、部分数据竞争、核心引擎未接入 UI。

| 维度 | 评级 | 得分 |
|------|------|------|
| 架构设计 | 🟢 良好 | 8/10 |
| 正确性 | 🟡 有风险 | 6/10 |
| 性能 | 🟡 可优化 | 6/10 |
| 安全性 | 🟢 良好 | 8/10 |
| 可维护性 | 🟡 有改善空间 | 7/10 |
| 完成度 | 🟡 部分功能未激活 | 6/10 |

---

## 二、🔴 阻塞项（必须修复）

### 🔴 B1: 游戏 ViewModel 作用域过宽 — 状态串扰

**文件**: `CoupletGameFragment.java:48` / `MatchGameFragment.java:52`

```java
// ❌ 当前代码：两个游戏 Fragment 共享同一个 ViewModel
viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);
```

**问题**: 如果用户在消消乐和接龙之间通过回退栈切换，两个 Fragment 会同时观察 `GameViewModel`，LiveData 更新会同时影响双方——配对数量变化、得分变化、游戏结束等事件会"泄漏"到另一个游戏页面。

**建议修复**:
```java
// ✅ 改为 Fragment 自身作用域
viewModel = new ViewModelProvider(this).get(GameViewModel.class);
```

**影响**: 中等 — 目前仅在回退栈同时存在两个游戏时触发，属低频但确定性的 bug。

---

### 🔴 B2: 积分/等级保存无事务 — 数据竞争

**文件**: `GameViewModel.java:164-173` / `QuizViewModel.java:98-113` / `LearningViewModel.java:62-94`

```java
// ❌ 三个线程同时读写 user_profile 表
private void savePoints(int points) {
    new Thread(() -> {
        UserProfile profile = db.poemDao().getUserProfileSync();  // READ
        profile.totalPoints += points;                              // MODIFY
        profile.level = LearningEngine.calcLevel(profile.totalPoints);
        db.poemDao().insertUserProfile(profile);                   // WRITE with REPLACE
    }).start();
}
```

**问题**: GameViewModel 和 QuizViewModel 可能同一时刻调用 `savePoints()`（如在游戏中边答题边得分），两个线程同时读到 `totalPoints=100` 后各自写回 `105` 和 `110`，最终积分为 `110` 而非 `115`，丢失 5 分。

**建议修复**:
```java
// ✅ 使用 Room @Query 原子更新
@Query("UPDATE user_profile SET totalPoints = totalPoints + :points WHERE id = 1")
void addPoints(int points);

@Query("UPDATE user_profile SET level = :level WHERE id = 1 AND totalPoints >= :threshold")
void updateLevelIfQualified(int level, int threshold);
```

**影响**: 严重 — 在多线程环境下导致确定性积分丢失，用户感知为"明明得了分怎么不见涨？"

---

### 🔴 B3: PoemRepository.search() — 91K 全量线性扫描

**文件**: `PoemRepository.java:80-97`

```java
// ❌ 每次搜索遍历全部 91,196 首诗词
public List<Poem> search(String query) {
    for (Poem p : allPoems) {  // 91K iterations
        if (p.title.contains(query) || p.author.contains(query)
            || p.getFullText().contains(query)) {  // getFullText 每次拼接字符串!
            results.add(p);
        }
    }
}
```

**问题**:
1. 每输入一个字符触发一次 `contains` 遍历 91K 对象（虽然有 500ms debounce）
2. `p.getFullText()` 内部用 `StringBuilder` 拼接所有行，**每次搜索重新拼接** — 91K 次 `StringBuilder` 创建/销毁
3. 搜索结果上限 500 但用 `subList` 截断前扫描全部

**建议修复**:
```java
// ✅ 建立倒排索引（加载时一次性构建）
private Map<String, Set<Integer>> titleIndex;   // "静夜" → {id0, id5}
private Map<String, Set<Integer>> authorIndex;

// 搜索时：对每个查询词做索引交集
```

**影响**: 严重 — 在低端设备上搜索卡顿可达 1-2 秒，用户感知为"键盘卡住了"。

---

### 🔴 B4: 成就引擎已编码但从未被调用

**文件**: `AchievementEngine.java` / 多个 Fragment

**问题**: `AchievementEngine.checkAndUnlock()` 定义了完整的 12 种成就检测逻辑，但**在整个项目中无任何代码调用它**。这意味着：

- 用户学到第 1 首、第 10 首、第 100 首诗时什么都没发生
- 连续学习 7 天、30 天没有庆祝
- 收藏 10 首诗没有反馈
- ProfileFragment 显示的成就全部为锁定状态

**建议修复**:
```java
// ✅ 在以下关键节点插入成就检测：
// DetailFragment.markAsLearned() 之后
// QuizViewModel.finishQuiz() 之后  
// GameViewModel recordGameActivity() 之后
// LearningViewModel.doAutoCheckin() 之后
// 每次触发时：
AchievementEngine.checkAndUnlock(db, def -> {
    // 触发纸屑动画 + Toast
    if (getActivity() instanceof MainActivity) {
        getActivity().runOnUiThread(() -> {
            ((MainActivity) getActivity()).celebrate();
            Toast.makeText(getActivity(), "🏆 解锁成就: " + def.name, Toast.LENGTH_LONG).show();
        });
    }
});
```

**影响**: 严重 — 整个趣味性系统的核心引擎处于"僵尸代码"状态。

---

### 🔴 B5: 主题系统定义了 9 套主题但无 UI 接入

**文件**: `ThemeManager.java` / 全量 Fragment

**问题**: `ThemeManager` 定义了墨韵/春意/夏荷/秋月/冬雪/竹韵/莲心/金榜/传奇共 9 套主题，带等级/连签解锁条件。但是：

- 无任何 Activity 或 Fragment 调用 `ThemeManager`
- 无切换主题的 UI 入口
- `res/values/styles.xml` 只有单一 Light 主题，无多主题定义
- `colors.xml` 未按主题拆分

**建议修复**:
```
1. res/values/themes.xml          ← 墨韵（默认）
2. res/values/themes_spring.xml   ← 春意
3. res/values/themes_summer.xml   ← 夏荷
...
4. ProfileFragment 加"切换主题"区域
5. 主题切换时通过 setTheme() + recreate() 应用
```

**影响**: 中高 — 9 套主题纯定义无实现，用户看不到差异化。

---

## 三、🟡 建议项（应该修复）

### 🟡 S1: ProfileFragment 经验条计算与 LearningEngine 等级体系不一致

**文件**: `ProfileFragment.java:81-82`

```java
// ❌ 简单粗暴：每级 100 经验
int expNeeded = profile.level * 100;
int currentExp = profile.totalPoints % expNeeded;
```

`LearningEngine.LEVEL_THRESHOLDS` 实际为 `{0, 100, 300, 600, 1000, ...}`，即 1 级需要 100 分、2 级需要 200 分、3 级需要 300 分……而经验条显示的是"每级 100"，与实际严重不一致。

**建议修复**: 统一使用 `LearningEngine.getLevelProgress()` 计算进度。

---

### 🟡 S2: 多处 `new Thread()` 无线程池 — 资源泄漏

**文件**: `HomeViewModel.java:45` / `DetailFragment.java:235,249,264,275` / `GameViewModel.java:165,178` / `QuizViewModel.java:98,126` / `LearningViewModel.java:34` / `ProfileViewModel.java:25`

**问题**: 全项目共 9+ 处裸 `new Thread().start()`，在游戏 + 学习同时进行时可能创建 5-6 个并发线程。

**建议修复**: 统一使用 `PoemRepository` 中已有的 `ExecutorService executor`，或新建一个共享线程池。

---

### 🟡 S3: PoemRepository.getPoemsByCategory() 无缓存

**文件**: `PoemRepository.java:67-78`

```java
// ❌ setCategory / showPage / loadMore 每次都重建 List
public List<Poem> getPoemsByCategory(String category) {
    List<Poem> result = new ArrayList<>();
    for (Poem p : allPoems) {
        if (category.equals(p.category)) result.add(p);  // 遍历 91K
    }
    return result;
}
```

**建议修复**: 在 `buildCategories()` 时同时构建 `Map<String, List<Poem>> categoryCache`。

---

### 🟡 S4: 详情页通过 Bundle 传递全文 — 大序列化开销

**文件**: `HomeFragment.java:207-218`

```java
// ❌ 将整首诗的所有数据序列化到 Bundle
args.putStringArray("poem_lines", poem.lines);  // 长诗可达 100+ 行
```

**建议修复**: 只传 `poemId`，在 `DetailFragment` 中通过 `PoemRepository.findPoemById(id)` 获取数据。

---

### 🟡 S5: MatchCardAdapter.onBindViewHolder 创建新 GradientDrawable

**文件**: `MatchCardAdapter.java:62-101`

```java
// ❌ 每次绑定创建新 GradientDrawable
GradientDrawable bg = new GradientDrawable();
```

**建议修复**: 预创建 3 种状态的背景 Drawable 或使用对象池。

---

### 🟡 S6: CoupletGameFragment 生命周期不一致

**文件**: `CoupletGameFragment.java:155-158`

```java
// ❌ 使用 onDestroy() 清理 Handler
@Override
public void onDestroy() {
    super.onDestroy();
    handler.removeCallbacksAndMessages(null);
}
```

其他 Fragment 统一使用 `onDestroyView()`。应保持一致以避免 Fragment 重建时不清理的潜在内存泄漏。

---

## 四、💭 小修（锦上添花）

| # | 文件 | 问题 | 建议 |
|---|------|------|------|
| N1 | `DetailViewModel.java:6` | 空类，注释"future expansion" | 删除或加入分享卡片生成逻辑 |
| N2 | `ConfettiView.java:46` | `celebrate()` 只被 `MainActivity` 暴露，但无人调用 | 在成就解锁时触发（见 B4） |
| N3 | `MatchCardAdapter.java:131-144` | `animateShake` 回调嵌套 4 层可读性差 | 改用 `AnimatorSet` |
| N4 | `PoemRepository.java:147` | "春秋"既在 navOrder 又不在实际朝代中 | 检查数据源，避免空 Chip |
| N5 | `QuizGenerator.java:83-86` | 干扰词固定 40 个高频汉字 | 可改为从当前诗词池同部首/同语义动态选字 |
| N6 | `PoemRepository.java:68` | `"全部".equals(category)` 硬编码 | 提取到 strings.xml |
| N7 | `LearningEngine.java:54` | `checkStreakMilestone` 方法未被任何代码调用 | 在签到逻辑中使用 |
| N8 | `DetailFragment.java:453` | `getView().findViewById` 在 `renderExplanation` 中查找 divider | 应在 `initViews` 中一次性查找 |

---

## 五、✅ 代码亮点

| # | 文件 | 亮点 |
|---|------|------|
| P1 | `PinyinLineView.java` | 自定义 View 设计优雅，layout_weight 等分布局自适应屏幕，多行自动折行算法正确 |
| P2 | `MatchCardAdapter.animateShake()` | 抖动动画还原感强，消除动画配合 O overshootInterpolator 自然 |
| P3 | `PoemRepository.java:110-115` | 每日推荐基于 `dateKey.hashCode()` 固定化，同一天同一首，符合"每日"语义 |
| P4 | `QuizFragment.undoBlank()` | 撤销单个空位逻辑完整——恢复候选词、回退索引、恢复提交按钮 |
| P5 | `TtsManager.OnInitListener` | TTS 初始化失败可选回调，给用户明确 "需安装语音包" 提示，而非静默失败 |
| P6 | `LearningDatabase.MIGRATION_1_2` | 数据库升级保留数据，非 `fallbackToDestructiveMigration` |
| P7 | `MatchGameFragment.onCardClick()` | 同张取消、不同配对、lockInput 锁状态机设计完整 |

---

## 六、修复优先级排序

```
┌──────┬─────────────────────────────────────────────┬──────┬────────┐
│  优先级 │ 修复项                                       │ 工作量 │ 影响范围 │
├──────┼─────────────────────────────────────────────┼──────┼────────┤
│  1   │ B4 成就引擎接入 UI（最少代码改动最大效果）        │ 30m  │ 全局    │
│  2   │ B1 游戏 ViewModel 作用域修复                   │ 5m   │ 2 文件  │
│  3   │ B2 积分原子更新（防数据丢失）                   │ 20m  │ 3 文件  │
│  4   │ B3 搜索倒排索引                               │ 1h   │ 2 文件  │
│  5   │ B5 主题系统接入浅层 UI                         │ 1h   │ 3 文件  │
│  6   │ S1 经验条计算修正                              │ 10m  │ 1 文件  │
│  7   │ S2 统一线程池                                  │ 30m  │ 全局    │
│  8   │ S3 分类缓存                                    │ 15m  │ 1 文件  │
│  9   │ S4 Bundle 传 ID 替代全文                       │ 10m  │ 2 文件  │
│  10  │ S5 GradientDrawable 预创建                     │ 10m  │ 1 文件  │
│  11  │ S6 生命周期统一                                 │ 5m   │ 1 文件  │
└──────┴─────────────────────────────────────────────┴──────┴────────┘
```

---

## 七、审查结论

项目 v2.0 基础扎实：MVVM 分层合理、Room 持久化完整、三种游戏可玩、拼音/分享/日历等细节到位。核心短板在**系统集成深度**——有成就引擎但没有接入、有主题系统但没有 UI、有纸屑动画但没有触发点。修复 5 个阻塞项后可达 **v2.1 生产就绪** 状态。

---

*代码审查人: 火眼眼 · 2026-07-01*
*审查范围: 35 Java / 1,582 注解行 / 0 测试文件*
