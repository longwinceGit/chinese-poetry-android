# 诗词乐园 Android 工程 — 代码分析报告

> 分析时间：2026-07-03 | 版本：v2.0 (versionCode 2) | 完成度：86%

---

## 一、项目概况

| 维度 | 数据 |
|------|------|
| 包名 | `com.poetry` |
| 语言 | Java 17（纯 Java，无 Kotlin/Compose） |
| 构建 | AGP 8.2.2 + Gradle 8.7 + JDK 17 |
| 最低 API | 21（Android 5.0） |
| 目标 API | 34（Android 14） |
| Java 文件 | 39 个，7,665 行 |
| 布局 XML | 13 个，2,093 行 |
| 诗词数据 | 91,154 首（11 朝代，22 个 JSON 文件） |
| 释义数据 | 404 条（覆盖 313 首核心著名诗词，100%） |
| 数据库版本 | v4（3 次迁移） |
| TODO/FIXME | 0 |
| 游戏模式 | 3 种（接龙、消消乐、填空） |
| 成就系统 | 12 种 |
| 主题系统 | 9 套 |
| 等级系统 | 9 级 |

---

## 二、架构分析

### 2.1 分层架构（MVVM）

项目严格遵循 MVVM 四层架构：

```
UI Layer (10 Fragments)
    ↓
ViewModel Layer (7 ViewModels + LiveData)
    ↓
Domain Layer (5 Engines/Managers — 纯逻辑)
    ↓
Data Layer (Repository + Room DAO + Database)
    ↓
Data Sources (assets JSON + Room DB)
```

**分层评价**：
- UI → ViewModel：通过 LiveData 观察单向数据流，Fragment 不直接操作数据
- ViewModel → Domain：游戏/成就/等级等业务逻辑抽离为独立 Engine 类，可测试性好
- Domain → Data：通过 PoemRepository 单例 + PoemDao 访问数据，职责清晰
- 数据源分离：诗词数据（assets JSON 只读）与用户数据（Room DB 可写）完全隔离

### 2.2 导航结构

```
MainActivity
  └─ NavHostFragment (nav_graph.xml, 10 destinations)
       ├─ 底部 Tab：Home / Learning / Game / Profile
       └─ 二级页面：Detail / Favorites / Quiz / MatchGame / CoupletGame
```

- 使用 Navigation Component + BottomNavigationView
- Fragment 间传参使用 Bundle（未使用 Safe Args 生成类）
- 回退栈由 NavigationUI 自动管理

### 2.3 数据流

**诗词数据（只读）**：
```
assets/web/data/*.json
  → PoemLoader.loadAll() (JSONTokener 解析)
  → PoemRepository (排序 + 分类 + 倒排索引)
  → ViewModel LiveData
  → Fragment UI
```

**用户数据（读写）**：
```
Fragment → ViewModel → PoemDao (Room) → SQLite (poetry_learning.db)
                                     ↑
                    AchievementEngine.checkAndUnlock()
                    ThemeManager.syncUnlockedThemes()
```

---

## 三、核心组件分析

### 3.1 PoemRepository（单例，440 行）

诗词数据仓库，管理 91K 首诗词的加载、分类、搜索。

**亮点**：
- 字符级倒排索引（titleCharIndex / authorCharIndex）加速搜索
- fullTextCached 缓存避免重复 StringBuilder 拼接
- famousPoems 列表确保每日推荐优先命中著名诗词
- 四级搜索策略：标题交集 → 作者并集 → 退化全量 → contains 精确验证

**问题**：
- `findPoemById()` 使用 O(n) 线性扫描 91K 首诗词，应用 HashMap 可达 O(1)
- `loaded` 标志位非 volatile，后台线程修改列表后 UI 线程可能读到旧值
- 搜索标题交集逻辑：首字符不在索引中时整个标题交集失效（边缘 case）

### 3.2 PoemDao（Room DAO，203 行）

涵盖 3 张表的完整 CRUD，40+ SQL 查询方法。

**亮点**：
- 积分操作使用原子 SQL（`addTotalPoints`、`incrementPoemsLearned` 等），消除读-改-写竞态
- `ensureRecordExists()` 幂等插入，避免主键冲突
- LiveData 自动刷新收藏列表和学习记录
- 索引覆盖：learnedAt / favorite / quizScore / gamePlayed 四列建索引

### 3.3 LearningDatabase（v4，81 行）

Room 数据库，DCL 单例，3 次迁移：
- v1→v2：新增 daily_stats 表
- v2→v3：learning_records 添加查询索引
- v3→v4：user_profile 添加 currentTheme 字段

### 3.4 GameEngine（272 行）

三种游戏模式核心逻辑：
- **接龙模式**：随机选联，4 选项（1 正确 + 3 干扰）
- **消消乐模式**：全可见 3×4 网格配对消除（非记忆翻牌）
- **积分计算**：接龙 = 基础分 + 连击奖励；消消乐 = 50 - 失误扣分

### 3.5 AchievementEngine（156 行）

12 种成就，覆盖学习/收藏/答题/游戏/等级/连续天数六个维度。
通过 `checkAndUnlock()` 在关键节点被调用，JSON 数组持久化已解锁列表。

### 3.6 DetailFragment（499 行，最大文件）

诗词详情页，集成展示、TTS 朗读、收藏、已学标记、拼音切换、古风卡片分享。
Canvas 绘制 750px 宽分享卡片（宣纸色 + 墨色 + 朱红装饰线），FileProvider 分享 PNG。

### 3.7 PinyinLineView（134 行）

自定义 View，逐字拼音标注。weight 等分列宽 + 自动折行，支持标点上方留空占位。

---

## 四、发现的问题

### P0 — 潜在崩溃

#### 1. GameEngine / QuizGenerator 干扰项选取缺少空数组检查

**文件**：`GameEngine.java:92`，`QuizGenerator.java:167`

```java
// GameEngine.generateCoupletGame() line 92
Poem other = pool.get(RANDOM.nextInt(pool.size()));
if (other.lines == null) continue;
int idx = RANDOM.nextInt(other.lines.length); // ← 若 other.lines.length == 0 抛异常
```

**根因**：有 `null` 检查但无 `length == 0` 检查。若数据中存在 `lines: []` 的诗词，`RANDOM.nextInt(0)` 抛出 `IllegalArgumentException`。

**修复建议**：改为 `if (other.lines == null || other.lines.length == 0) continue;`

### P1 — 正确性 / 性能

#### 2. nav_graph.xml 残留已废弃的导航参数

**文件**：`nav_graph.xml:59-66`

```xml
<argument android:name="poem_category" app:argType="string" />
<argument android:name="poem_tag" app:argType="string" />
<argument android:name="poem_emoji" app:argType="string" />
```

**现状**：DetailFragment 已删除 `ARG_CATEGORY` / `ARG_TAG` / `ARG_EMOJI` 常量，HomeFragment / FavoritesFragment 已不再传递这三个参数。但 nav_graph 仍声明为必填参数（无 defaultValue）。

**影响**：当前使用 Bundle 导航不会崩溃（Navigation Component 不强制校验 Bundle 方式的参数），但若未来改用 Safe Args Directions 会直接崩溃。属于代码卫生问题。

**修复建议**：从 nav_graph.xml 中删除这三个 `<argument>` 声明。

#### 3. PoemRepository.findPoemById() O(n) 线性扫描

**文件**：`PoemRepository.java:257-262`

对 91,154 首诗词做线性遍历查找。虽然当前调用频率不高（详情页跳转时），但构建 `HashMap<String, Poem>` 或 `HashMap<String, Integer>` 可将复杂度降至 O(1)，几乎零成本。

#### 4. DetailFragment.sharePoem() 生命周期风险

**文件**：`DetailFragment.java:311-345`

```java
new Thread(() -> {
    // ...
    requireActivity().runOnUiThread(() -> ...);  // ← Fragment 可能已 destroy
}).start();
```

**根因**：`requireActivity()` 在 Fragment detach 后抛 `IllegalStateException`。后台线程执行期间用户可能已退出详情页。

**修复建议**：改用 `Activity activity = getActivity();` + null 检查，或使用 `view.post()` 替代。

#### 5. PoemRepository 线程可见性

**文件**：`PoemRepository.java`

`allPoems`、`famousPoems`、`categories` 等列表在后台线程（executor.submit）中赋值，UI 线程直接读取。`loaded` 标志非 volatile。

**影响**：理论上存在内存可见性问题（UI 线程可能读到旧引用）。实际中由于 Future.get() 提供了 happens-before 关系，且 ViewModel 通过 LiveData 通知，风险较低。但不符合最佳实践。

**修复建议**：将 `loaded` 改为 `volatile`，列表赋值后通过 LiveData 通知（已有此机制），或将列表引用改为 volatile。

### P2 — 代码质量

#### 6. ThemeManager.requireAllPoems 死字段

**文件**：`ThemeManager.java:39`

`ThemeDef.requireAllPoems` 字段已定义但 `isUnlocked()` 方法中未使用，属于遗留代码。

#### 7. PoemLoader.readJsonArray() Javadoc 误导

**文件**：`PoemLoader.java:144-146`

Javadoc 声称"使用 JSONTokener 进行流式解析，避免一次性加载大文件到内存"，但实际实现是先将整个文件读入 StringBuilder，再传入 JSONTokener 解析。这是全量读取 + 全量解析，并非流式。

**影响**：性能影响可接受（单个朝代文件通常 < 10MB），但文档与实现不符。

#### 8. 内存占用

91,154 个 Poem 对象 + 倒排索引（HashMap<Character, Set<Integer>>，估计 5万+ key、数百万 entry） + 每个 Poem 的 fullTextCached 字符串。总内存估算 50-100MB。`largeHeap="true"` 已设置，但低端设备仍可能 OOM。

#### 9. 无单元测试

项目无 `test/` 或 `androidTest/` 目录，所有验证依赖手动编译运行。Domain 层（GameEngine / QuizGenerator / LearningEngine）是纯逻辑代码，非常适合单元测试。

#### 10. Poem.emoji 字段使用不一致

`emoji` 在 PoemLoader 中赋值，在 HomeFragment 每日卡片中使用，但已从 DetailFragment 中移除。字段仍存在于数据模型中但使用场景有限。

---

## 五、近期工作回顾（2026-07-01 ~ 07-02）

### 已完成

| 日期 | 工作 | 状态 |
|------|------|------|
| 07-01 | 5 个 P0 阻塞修复（ViewModel 作用域/积分竞态/搜索性能/成就引擎/主题管理） | ✅ |
| 07-01 | E5 DetailViewModel / E6 收藏功能 / E7 统计图表 | ✅ |
| 07-01 | 著名诗词库 5 批入库完成（313 首 100% 覆盖） | ✅ |
| 07-02 | 消消乐配对消失 Bug 修复 | ✅ |
| 07-02 | 成就庆祝重复触发 Bug 修复 | ✅ |
| 07-02 | 学习趋势图表不刷新 Bug 修复 | ✅ |
| 07-02 | 繁简体转换（91K 首诗词 + UI 字符串） | ✅ |
| 07-02 | 每日推荐著名诗词优先 | ✅ |
| 07-02 | 释义数据扩充 88→128 首 | ✅ |
| 07-02 | 数据完整性校验与修复（6 类问题） | ✅ |
| 07-02 | 明代/近现代朝代分类支持 | ✅ |
| 07-02 | 详情页精简（移除 emoji/分类标签） | ✅ |
| 07-02 | 消消乐配对后闪退 Bug 修复 | ✅ |

### 仍需优化

1. **P0 修复**：GameEngine/QuizGenerator 空数组检查
2. **代码卫生**：nav_graph.xml 清理废弃参数
3. **性能**：findPoemById 改用 HashMap
4. **稳定性**：DetailFragment.sharePoem() 生命周期保护
5. **测试**：补充 Domain 层单元测试
6. **内存**：考虑按需加载/分页（当前全量加载 91K 首到内存）

---

## 六、总体评价

### 优势

1. **架构清晰**：MVVM 四层分离彻底，Domain 层纯逻辑无 Android 依赖，可测试性高
2. **代码规范**：全量中文 Javadoc，0 TODO/FIXME，命名一致
3. **性能优化到位**：倒排索引、原子 SQL、fullTextCached 缓存等针对性优化
4. **数据质量高**：91K 首诗词经 6 类问题清洗，释义 100% 覆盖核心著名诗词
5. **游戏化完整**：12 成就 + 9 主题 + 9 级 + 3 游戏，形成完整的学习激励闭环
6. **Bug 修复迅速**：07-01/02 两天内修复 7 个 Bug，根因分析到位

### 待改进

1. **测试缺失**：无任何自动化测试，回归风险靠人工
2. **内存管理**：91K 首全量加载对低端设备不友好
3. **ProGuard 配置**：已有基础规则但未验证 release 包运行
4. **线程安全**：Repository 的列表可见性、Fragment 的生命周期边界需加固
5. **数据工具链**：21 个 Python 脚本可整合为 CLI 工具，减少碎片化
