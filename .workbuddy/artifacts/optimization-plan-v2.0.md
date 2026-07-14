# 诗词乐园 Android · 优化方案 v2.0

> **编写人**：UI 设计师 · 像素君
> **日期**：2026-07-03 18:20 GMT+8
> **项目版本**：v2.0 (versionCode 2)，完成度 86%
> **方案目标**：止血 → 健壮 → 焕颜 → 留白，四层递进，每层可独立交付

---

## 一、执行摘要

### 1.1 已完成工作盘点

| 阶段 | 内容 | 状态 | 验证 |
|------|------|------|------|
| P0-A1 | PinyinHelper LRU 字符级缓存（250x+ 加速） | ✅ | 自测 7/7 + 基准测试 |
| P0-A2 | PinyinLineView 延迟构建（mBuilt 标志） | ✅ | 编译通过 |
| P0-A3 | DetailFragment 分享卡 OOM 防护（RGB_565 + recycle） | ✅ | 编译通过 |
| P0-A4 | DetailFragment 生命周期安全（ApplicationContext + Handler） | ✅ | 编译通过 |
| Hotfix | ContextCompat.getColor 全量替换（16 处 / 4 文件） | ✅ | Lint 通过 |
| P0-B | GameEngine / QuizGenerator 空数组守卫 | ✅ | 代码已含 `length == 0` 检查 |

**结论**：所有 P0 崩溃风险已消除，项目处于「稳定但可更好」状态。

### 1.2 当前剩余问题全景

```
代码健壮性 (4 项)
  ├─ P1: findPoemById O(n) → O(1) HashMap
  ├─ P1: PoemRepository loaded 非 volatile
  ├─ P1: nav_graph.xml 残留 3 个废弃 argument
  └─ P2: ThemeManager.requireAllPoems 死字段

设计系统基建 (3 项)
  ├─ P1: 缺少 dimens.xml 间距/字号令牌
  ├─ P1: styles.xml 缺少组件级样式抽象
  └─ P2: colors.xml 语义层级待补充

无障碍 (4 项)
  ├─ P1: 无字号缩放（100/125/150% 三档）
  ├─ P1: 无减少动画开关
  ├─ P2: contentDescription 覆盖不全
  └─ P2: 触控目标部分 < 48dp

视觉焕颜 (5 项)
  ├─ P2: 首页缺「今日诗笺」Hero 卡片
  ├─ P2: 详情页工具栏未 Chip 化
  ├─ P2: 朝代标签缺印章视觉
  ├─ P2: 状态栏未沉浸式
  └─ P2: 缺统一加载/空/错误三态

微互动 (5 项)
  ├─ P2: 收藏无粒子反馈
  ├─ P2: 答题无印章动画
  ├─ P2: 列表项无落墨涟漪
  ├─ P2: 拼音切换无过渡动画
  └─ P2: 学习完成无卷轴庆祝

测试与验收 (3 项)
  ├─ P2: Domain 层无正式 JUnit 测试
  ├─ P2: 无回归测试基线
  └─ P3: 走查截图集未建立
```

---

## 二、优化方案分层架构

### 设计理念

```
L1 止血 ─── 已完成 ✅
  └─ 消除崩溃、OOM、API 兼容性

L2 健壮 ─── 本轮重点
  └─ 代码正确性、线程安全、数据结构优化

L3 基建 ─── 本轮重点
  └─ 设计令牌、组件样式、无障碍基础

L4 焕颜 ─── 后续 Sprint
  └─ 核心页面视觉升级、微互动、品牌强化

L5 留白 ─── 选做
  └─ 季节主题、节日彩蛋、测试自动化
```

---

## 三、L2 · 代码健壮性（2 工作日）

> 目标：消除 4 个已知代码质量问题，确保数据层和导航层零隐患。

### T2-1 · findPoemById O(n) → O(1)

| 维度 | 详情 |
|------|------|
| **文件** | `app/src/main/java/com/poetry/data/PoemRepository.java:257-262` |
| **现状** | 线性遍历 91,154 首诗词查找 `id` 匹配项 |
| **方案** | 在 `loadAll()` 完成后构建 `HashMap<String, Poem> poemIdIndex`，`findPoemById()` 改为 `return poemIdIndex.get(id)` |
| **内存代价** | 91K × (String 引用 + Poem 引用) ≈ 1.5MB，可接受 |
| **验收** | `findPoemById("已知ID")` 返回正确对象；`findPoemById("不存在")` 返回 null |
| **工时** | 0.5d |
| **风险** | 低 — 纯数据结构替换，API 不变 |

### T2-2 · PoemRepository 线程可见性

| 维度 | 详情 |
|------|------|
| **文件** | `app/src/main/java/com/poetry/data/PoemRepository.java` |
| **现状** | `loaded` 标志位非 volatile；`allPoems` 等列表在后台线程赋值，UI 线程直接读取 |
| **方案** | ① `loaded` 改为 `volatile boolean`；② `allPoems`/`famousPoems`/`categories` 赋值后通过 `MutableLiveData.postValue()` 通知（已有 LiveData 机制，确认覆盖完整） |
| **验收** | 极端场景（后台加载未完成时 UI 线程访问）不返回 null 或旧引用 |
| **工时** | 0.5d |
| **风险** | 低 — 仅加关键字，逻辑不变 |

### T2-3 · nav_graph.xml 清理废弃参数

| 维度 | 详情 |
|------|------|
| **文件** | `app/src/main/res/navigation/nav_graph.xml:59-66` |
| **现状** | 残留 `poem_category`、`poem_tag`、`poem_emoji` 三个 `<argument>` 声明，DetailFragment 已不再使用 |
| **方案** | 删除这三个 `<argument>` 节点 |
| **验收** | 导航到详情页正常；`assembleDebug` + Lint 无 warning |
| **工时** | 0.25d |
| **风险** | 极低 — 纯删除废弃声明 |

### T2-4 · ThemeManager 死字段清理

| 维度 | 详情 |
|------|------|
| **文件** | `app/src/main/java/com/poetry/domain/ThemeManager.java:39` |
| **现状** | `ThemeDef.requireAllPoems` 字段已定义但 `isUnlocked()` 未使用 |
| **方案** | 确认无其他引用后删除字段，或补充解锁逻辑（若设计意图是「学完全部诗词」解锁条件） |
| **验收** | 编译通过；主题解锁行为不变 |
| **工时** | 0.25d |
| **风险** | 极低 |

### T2-5 · PoemLoader Javadoc 修正

| 维度 | 详情 |
|------|------|
| **文件** | `app/src/main/java/com/poetry/PoemLoader.java:144-146` |
| **现状** | Javadoc 声称"流式解析"但实际是全量读取 + JSONTokener 解析 |
| **方案** | 修正 Javadoc 为"全量读取后通过 JSONTokener 解析，单文件 < 10MB 内存可控" |
| **验收** | 文档与实现一致 |
| **工时** | 0.1d |
| **风险** | 无 |

---

## 四、L3 · 设计系统基建（3 工作日）

> 目标：建立可维护的设计令牌体系，为后续焕颜和无障碍铺路。

### T3-1 · 新建 dimens.xml 令牌系统

| 维度 | 详情 |
|------|------|
| **文件** | 新建 `app/src/main/res/values/dimens.xml` |
| **内容** | 见下方令牌定义 |
| **验收** | 所有 layout 中 hard-coded dp/sp 引用 ≤ 5% 残存 |
| **工时** | 0.5d |
| **风险** | 低 — 纯新增资源文件 |

**令牌定义**：

```xml
<!-- 间距（4pt 网格） -->
<dimen name="space_xs">4dp</dimen>
<dimen name="space_sm">8dp</dimen>
<dimen name="space_md">12dp</dimen>
<dimen name="space_lg">16dp</dimen>
<dimen name="space_xl">24dp</dimen>
<dimen name="space_2xl">32dp</dimen>
<dimen name="space_3xl">48dp</dimen>

<!-- 字号 -->
<dimen name="text_caption">10sp</dimen>
<dimen name="text_body_s">12sp</dimen>
<dimen name="text_body_m">14sp</dimen>
<dimen name="text_body_l">16sp</dimen>
<dimen name="text_h2">16sp</dimen>
<dimen name="text_h1">20sp</dimen>
<dimen name="text_display">24sp</dimen>

<!-- 圆角 -->
<dimen name="radius_xs">4dp</dimen>
<dimen name="radius_sm">8dp</dimen>
<dimen name="radius_md">12dp</dimen>
<dimen name="radius_lg">16dp</dimen>
<dimen name="radius_xl">20dp</dimen>

<!-- 阴影/高度 -->
<dimen name="elevation_sm">1dp</dimen>
<dimen name="elevation_md">2dp</dimen>
<dimen name="elevation_lg">4dp</dimen>
<dimen name="elevation_xl">8dp</dimen>

<!-- 组件尺寸 -->
<dimen name="touch_target_min">48dp</dimen>
<dimen name="button_height">48dp</dimen>
<dimen name="chip_height">32dp</dimen>
<dimen name="appbar_height">48dp</dimen>
<dimen name="tabbar_height">56dp</dimen>
<dimen name="card_padding">16dp</dimen>
<dimen name="card_radius">12dp</dimen>
```

### T3-2 · styles.xml 组件样式抽象

| 维度 | 详情 |
|------|------|
| **文件** | `app/src/main/res/values/styles.xml`（已有，增量修改） |
| **方案** | 新增以下组件级样式，替换 layout 内联属性 |
| **验收** | 改主色/字号一处，全 app 同步生效 |
| **工时** | 1d |
| **风险** | 中 — 需回归所有页面，但可分批迁移 |

**新增样式**：

```xml
<!-- 诗词卡片 -->
<style name="Widget.Poetry.PoemCard" parent="">
    <item name="android:background">@drawable/bg_card</item>
    <item name="android:padding">@dimen/card_padding</item>
    <item name="android:layout_marginBottom">@dimen/space_sm</item>
</style>

<!-- 朝代标签 -->
<style name="Widget.Poetry.DynastyTag" parent="">
    <item name="android:layout_height">@dimen/chip_height</item>
    <item name="android:paddingHorizontal">@dimen/space_md</item>
    <item name="android:textSize">@dimen/text_body_s</item>
    <item name="android:gravity">center</item>
</style>

<!-- 印章按钮 -->
<style name="Widget.Poetry.StampButton" parent="">
    <item name="android:layout_width">wrap_content</item>
    <item name="android:layout_height">@dimen/button_height</item>
    <item name="android:paddingHorizontal">@dimen/space_xl</item>
    <item name="android:textSize">@dimen/text_body_m</item>
    <item name="android:textColor">@color/on_primary</item>
    <item name="android:background">@drawable/bg_button</item>
</style>

<!-- 正文文字 -->
<style name="TextAppearance.Poetry.Body" parent="">
    <item name="android:textSize">@dimen/text_body_l</item>
    <item name="android:lineSpacingMultiplier">1.8</item>
    <item name="android:textColor">@color/on_surface</item>
</style>

<!-- 标题文字 -->
<style name="TextAppearance.Poetry.Title" parent="">
    <item name="android:textSize">@dimen/text_h1</item>
    <item name="android:textStyle">bold</item>
    <item name="android:textColor">@color/on_surface</item>
</style>
```

### T3-3 · 无障碍 — 字号缩放（3 档）

| 维度 | 详情 |
|------|------|
| **文件** | `ProfileFragment.java` + `ProfileViewModel.java` + 新建 `FontSizeUtil.java` |
| **方案** | ① SharedPreferences key `pref_font_scale`（1.0/1.25/1.5）；② `MainActivity.onCreate()` 读取后设 `resources.configuration.fontScale` + `createConfigurationContext`；③ ProfileFragment 加 3 档 SeekBar 或 RadioGroup |
| **验收** | 切换档位 → 全 app 文字按比例放大；重启后保持设置 |
| **工时** | 1d |
| **风险** | 中 — 全局 Configuration 变更需仔细处理 Activity 重建 |

### T3-4 · 无障碍 — 减少动画开关

| 维度 | 详情 |
|------|------|
| **文件** | `ProfileFragment.java` + SharedPreferences key `pref_reduce_motion` |
| **方案** | ① `SwitchMaterial` 控件；② 统一 `MotionUtil.shouldAnimate(Context)` 方法；③ 所有 `ObjectAnimator`/`ConfettiView`/动画资源调用前检查 |
| **验收** | 开启后所有 Spring/粒子/缩放动画 ≤ 100ms 瞬切 |
| **工时** | 0.5d |
| **风险** | 低 |

### T3-5 · 无障碍 — contentDescription 补全

| 维度 | 详情 |
|------|------|
| **文件** | 全 Fragment + 关键 ImageView/ImageButton |
| **方案** | ① 收藏按钮 → "收藏/取消收藏，{title}"；② 朝代 tag → "{dynasty}朝诗词"；③ 诗词卡片 → "{title}，{author}，{dynasty}朝"；④ 朗读按钮 → "朗读全文"；⑤ 分享按钮 → "分享诗笺" |
| **验收** | TalkBack 朗读无生硬"按钮"字样，焦点路径符合阅读顺序 |
| **工时** | 0.5d |
| **风险** | 低 |

---

## 五、L4 · 视觉焕颜（5 工作日）

> 目标：核心 4 页截图级提升，品牌一致性强化。

### T4-1 · 首页「今日诗笺」Hero 卡片

| 维度 | 详情 |
|------|------|
| **文件** | `HomeFragment.java` + `fragment_home.xml` |
| **设计** | 顶部 240dp 高度 Hero 区：宣纸渐变背景 + 朱红 4dp 左侧装饰条 + 楷体标题 + 4 句预览 + "轻触展开 →" |
| **交互** | 点击 → 详情页；长按 → 收藏 |
| **验收** | 首屏 50%+ 用户注意力落在 Hero；每日推荐著名诗词 |
| **工时** | 1d |
| **风险** | 中 — 需新增 layout + 自定义背景 drawable |

### T4-2 · 详情页工具栏 Chip 化

| 维度 | 详情 |
|------|------|
| **文件** | `DetailFragment.java` + `fragment_detail.xml` |
| **设计** | 工具栏 4 个 Chip：拼音/朗读/收藏/分享，圆角 16dp，选中态朱红描边 |
| **交互** | 拼音 Chip 切换 → `TransitionManager.beginDelayedTransition` + 滑入动画 |
| **验收** | 工具栏视觉统一；拼音切换有"翻牌"过渡感 |
| **工时** | 1d |
| **风险** | 中 |

### T4-3 · 朝代印章组件

| 维度 | 详情 |
|------|------|
| **文件** | `PoemAdapter.java` + 新建 `res/drawable/bg_dynasty_stamp.xml`（10 个朝代变体） |
| **设计** | 圆形 28dp 印章 + 朝代单字（"唐""宋""元"等）+ 朝代色填充 |
| **验收** | 色盲模拟下仍可分辨朝代；列表项视觉识别度提升 |
| **工时** | 0.5d |
| **风险** | 低 |

### T4-4 · 状态栏沉浸式

| 维度 | 详情 |
|------|------|
| **文件** | `MainActivity.java` |
| **方案** | ① `WindowCompat.setDecorFitsSystemWindows(false)`；② 状态栏透明 + 文字色自适应（`SystemBarCompat`）；③ 适配 Android 15 edge-to-edge |
| **验收** | 截图无白边；状态栏文字在浅/深模式下可读 |
| **工时** | 0.5d |
| **风险** | 低 |

### T4-5 · 加载/空/错误三态统一

| 维度 | 详情 |
|------|------|
| **文件** | 新建 `res/layout/include_state_loading.xml`、`include_state_empty.xml`、`include_state_error.xml` |
| **设计** | 全屏宣纸背景 + 居中文案 + 古风插画/icon |
| **文案** | 加载："研墨中..."；空："今日诗笺尚未揭开"；错误："墨线断了" + 按钮"重新铺纸" |
| **验收** | 9 个 Fragment 全部接入三态（可分批） |
| **工时** | 1d（基建 0.5d + 接入 0.5d） |
| **风险** | 低 |

### T4-6 · 收藏粒子反馈

| 维度 | 详情 |
|------|------|
| **文件** | `ConfettiView.java`（已有）+ 收藏按钮集成 |
| **方案** | 收藏点击 → 触发 6 颗 ❤ 粒子抛物线扩散，200ms 完成 |
| **验收** | 首页/详情/收藏页收藏动画均生效 |
| **工时** | 0.5d |
| **风险** | 低 — 组件已存在 |

### T4-7 · 答题印章动画

| 维度 | 详情 |
|------|------|
| **文件** | `QuizFragment.java` + 新建 `ui/widget/StampView.java` |
| **方案** | 答对 → 选项上盖朱红"妙"印章（Canvas 绘制，700ms 落下）；答错 → 墨色晕染（alpha 0→0.5, 200ms） |
| **验收** | 答对有 0.5s 庆祝感；触感反馈 CONFIRM |
| **工时** | 1d |
| **风险** | 中 |

### T4-8 · 落墨涟漪（列表项点击）

| 维度 | 详情 |
|------|------|
| **文件** | 新建 `res/drawable/bg_ripple_ink.xml` |
| **方案** | 自定义 RippleDrawable：圆形扩散 + 茶褐色 + alpha 0.3→0，替换 `bg_card.xml` 的 `?attr/selectableItemBackground` |
| **验收** | 列表项点击有墨滴扩散感 |
| **工时** | 0.5d |
| **风险** | 低 |

---

## 六、L5 · 测试与验收（2 工作日）

### T5-1 · Domain 层 JUnit 测试

| 维度 | 详情 |
|------|------|
| **文件** | `app/build.gradle`（加 testImplementation）+ 新建 `app/src/test/java/com/poetry/domain/` |
| **范围** | `GameEngine`（接龙/消消乐生成逻辑）、`QuizGenerator`（题型生成）、`LearningEngine`（等级计算）、`AchievementEngine`（解锁条件） |
| **验收** | 覆盖核心路径 + 边界 case（空数组、单元素、满配对） |
| **工时** | 1d |
| **风险** | 低 — Domain 层无 Android 依赖，纯 Java 可测 |

### T5-2 · 编译验证 + Lint 全检

| 维度 | 详情 |
|------|------|
| **命令** | `./gradlew assembleDebug`（含 Lint） |
| **验收** | BUILD SUCCESSFUL + 0 error + 0 warning（P0/P1 级） |
| **工时** | 0.25d |
| **风险** | 无 |

### T5-3 · 走查截图集

| 维度 | 详情 |
|------|------|
| **范围** | 8 核心页 × 浅/深 × 3 档字号 = 48 张截图 |
| **工具** | Android Studio Layout Inspector + Accessibility Scanner |
| **产出** | 归档到 `.workbuddy/artifacts/screenshots/` |
| **工时** | 0.5d |

### T5-4 · 设计-实现对照表

| 维度 | 详情 |
|------|------|
| **内容** | 对照 `ui-design-system-v1.0.md` 各项，标记已实现/未实现/有偏差 |
| **标准** | 偏差 < 10% 算合格 |
| **工时** | 0.25d |

---

## 七、优先级矩阵

```
高影响
  │   ┌─────────────────────────┐
  │   │  T3-1 dimens.xml 令牌   │ ← 先做，铺路
  │   │  T3-3 字号缩放           │
  │   │  T4-1 首页 Hero 卡片     │
  │   │  T2-1 findPoemById      │
  │   └────────────┬────────────┘
  │                │
  │   ┌────────────┴────────────┐
  │   │  T3-2 styles.xml 样式   │
  │   │  T3-4 减少动画开关       │
  │   │  T4-2 详情页 Chip 化     │
  │   │  T4-5 三态统一           │
  │   └────────────┬────────────┘
  │                │
  │   ┌────────────┴────────────┐
  │   │  T4-3 朝代印章           │
  │   │  T4-4 沉浸式状态栏       │
  │   │  T4-6 收藏粒子           │
  │   │  T4-7 答题印章           │
  │   └────────────┬────────────┘
  │                │
  │   ┌────────────┴────────────┐
  │   │  T4-8 落墨涟漪           │
  │   │  T5-1 JUnit 测试         │
  │   │  T2-4 死字段清理          │
  │   │  T2-5 Javadoc 修正        │
  │   └─────────────────────────┘
  │
  └──────────────────────────────────→ 高工时
     低工时
```

---

## 八、执行路线图（3 Sprint）

### Sprint 1：健壮 + 基建（4 工作日）

| 任务 | 工时 | 交付物 |
|------|------|--------|
| T2-1 findPoemById HashMap | 0.5d | O(1) 查找 |
| T2-2 volatile + LiveData | 0.5d | 线程安全 |
| T2-3 nav_graph 清理 | 0.25d | 零废弃参数 |
| T2-4 死字段清理 | 0.25d | 零死代码 |
| T2-5 Javadoc 修正 | 0.1d | 文档一致 |
| T3-1 dimens.xml | 0.5d | 令牌系统 |
| T3-2 styles.xml | 1d | 组件样式 |
| T3-3 字号缩放 | 1d | 3 档可调 |

**Sprint 1 交付**：代码零隐患 + 设计令牌就绪 + 字号可调

### Sprint 2：无障碍 + 焕颜（5 工作日）

| 任务 | 工时 | 交付物 |
|------|------|--------|
| T3-4 减少动画 | 0.5d | 开关可用 |
| T3-5 contentDescription | 0.5d | TalkBack 友好 |
| T4-1 首页 Hero | 1d | 首屏定锚 |
| T4-2 详情页 Chip | 1d | 工具栏统一 |
| T4-3 朝代印章 | 0.5d | 视觉识别 |
| T4-4 沉浸式 | 0.5d | 无白边 |
| T4-5 三态统一 | 1d | 加载/空/错误 |

**Sprint 2 交付**：a11y 达标 + 核心 4 页截图级提升

### Sprint 3：微互动 + 验收（4 工作日）

| 任务 | 工时 | 交付物 |
|------|------|--------|
| T4-6 收藏粒子 | 0.5d | ❤ 反馈 |
| T4-7 答题印章 | 1d | 印章动画 |
| T4-8 落墨涟漪 | 0.5d | 墨滴感 |
| T5-1 JUnit 测试 | 1d | Domain 层覆盖 |
| T5-2 编译验证 | 0.25d | 0 error |
| T5-3 截图集 | 0.5d | 48 张 |
| T5-4 对照表 | 0.25d | 偏差 < 10% |

**Sprint 3 交付**：微互动完成 + 测试基线 + 验收报告

---

## 九、不在范围（Scope Exclusions）

| 排除项 | 原因 |
|--------|------|
| Kotlin / Compose 迁移 | 用户明确要求保持 Java + View System |
| Hilt / DI 框架 | 用户未要求，手动构造足够 |
| 诗词数据扩充 | 91K 首已是上限 |
| 后端 / 账号系统 | 本地优先 |
| 按需加载 / 分页 | 91K 全量 + largeHeap=true 当前可接受，留给 v3.0 |
| ProGuard release 验证 | 独立任务，不混入本轮 |
| i18n 多语言 | 框架先就位，文案留 key |

---

## 十、风险与应对

| 风险 | 等级 | 应对策略 |
|------|------|----------|
| styles.xml 迁移导致视觉回归 | 中 | 分批迁移，每批跑 `assembleDebug` + 截图对比 |
| 字号 150% 导致布局溢出 | 中 | ConstraintLayout 约束链已处理，但需走查 ScrollView |
| HashMap 额外 1.5MB 内存 | 低 | 91K Poem 对象已占 50-100MB，1.5MB 可忽略 |
| JUnit 依赖引入构建冲突 | 低 | testImplementation 不影响 main 编译 |
| 沉浸式适配 Android 15 | 低 | `WindowCompat` 已处理 API 21+ 兼容 |

---

## 十一、验收指标

| 维度 | 目标 | 测量方式 |
|------|------|----------|
| 崩溃率 | 0 P0 崩溃 | 手动测试 + logcat |
| 启动 → 首诗词 | < 500ms | logcat 时间戳 |
| 详情页打开 | < 100ms | logcat |
| findPoemById | O(1) | 代码审查 |
| 内存峰值 | < 120MB | Android Profiler |
| a11y 焦点路径 | 9/9 页面通过 | TalkBack 手动 |
| 字号 3 档 | 文字 100/125/150% | 截图对照 |
| 深色模式 | 无白底残留 | 9/9 页面截图 |
| TalkBack 朗读 | 无生硬"按钮" | 录音回放 |
| 设计令牌覆盖率 | hard-coded ≤ 5% | grep 审查 |
| Lint | 0 error + 0 P1 warning | `assembleDebug` |

---

**方案版本**：v2.0
**关联文档**：`code-analysis-2026-07-03.md`、`ui-design-system-v1.0.md`、`task-breakdown-v1.0.md`、`p0a-fix-report-2026-07-03.md`
**下一步**：用户确认后，从 Sprint 1 开始执行
