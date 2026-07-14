# 诗词乐园 · 交互与界面设计优化方案

> **编写人**：UI 设计师 · 像素君
> **日期**：2026-07-08
> **项目版本**：v2.0 (versionCode 2)
> **审计范围**：13 个布局 XML + 6 个 drawable + 6 个 anim + styles/colors/strings + nav_graph

---

## 一、现状审计：17 项发现

### 1.1 严重问题（影响体验/编译）

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| S1 | `item_poem_card.xml` 缺少约束，Lint 报 MissingConstraints error | item_poem_card.xml:21 | 编译 Lint error |
| S2 | 详情页操作按钮（收藏/朗读/分享/学习）在页面最底部，需滚动到底才能触达 | fragment_detail.xml:166-241 | 核心操作可达性差 |
| S3 | 详情页无 Toolbar / 返回按钮，依赖系统返回键 | fragment_detail.xml 全文 | 导航心智负担 |
| S4 | 首页用 NestedScrollView + RecyclerView(nestedScrollingEnabled=false)，9 万首列表全量渲染 | fragment_home.xml:336-347 | 滚动卡顿 + 内存浪费 |

### 1.2 一致性问题

| # | 问题 | 现状 | 应为 |
|---|------|------|------|
| C1 | 圆角半径不统一 | 8/10/12/16/20/24dp 混用 | 4 级体系：8/12/16/24dp |
| C2 | 外边距不统一 | 12/16/20/24dp 混用 | 2 级：16dp(标准)/24dp(宽松) |
| C3 | 按钮高度不统一 | 40dp/48dp 混用 | 统一 48dp |
| C4 | 图标体系混乱 | Material 矢量图标 + Emoji(📝🃏✏️🌸📭📚❤️📖) 混用 | 统一 Material 矢量 + 朝代印章色块 |
| C5 | 硬编码字符串 | "对诗"/"再来一局"/"点击查看详情 →" 等 12+ 处 | 全部迁移至 strings.xml |
| C6 | 无 dimens.xml | 所有间距/字号/圆角硬编码 | 建立设计令牌系统 |

### 1.3 交互缺陷

| # | 问题 | 说明 |
|---|------|------|
| I1 | 无骨架屏 | 加载态只有转圈+文字，页面结构不可预知 |
| I2 | 无屏幕转场动画 | Fragment 切换为默认 fade，无方向感 |
| I3 | 无触觉反馈 | 收藏/答题/配对等关键操作无 haptic feedback |
| I4 | 收藏页无操作能力 | 无搜索/排序/滑动删除，只能点击查看 |
| I5 | 游戏中心卡片同质化 | 三张卡片结构完全相同，仅 emoji+文字不同 |
| I6 | 个人中心头像为 emoji 方块 | 📚 emoji + bg_checkin_future 背景，缺乏品牌识别度 |

### 1.4 无障碍缺陷

| # | 问题 | 说明 |
|---|------|------|
| A1 | 触控目标过小 | 搜索清除按钮 36dp(<48dp)、朝代 Chip 18dp 高(<48dp) |
| A2 | 无字号缩放 | 未支持 100/125/150% 三档系统字号 |
| A3 | 无减少动画开关 | spring_scale / heart_beat 动画无法关闭 |

---

## 二、设计令牌系统

### 2.1 新建 `dimens.xml`

```xml
<!-- ===== 间距体系 (4dp 基准) ===== -->
<dimen name="space_xs">4dp</dimen>     <!-- 元素内微调 -->
<dimen name="space_sm">8dp</dimen>     <!-- 元素间 -->
<dimen name="space_md">12dp</dimen>    <!-- 组件内 padding -->
<dimen name="space_base">16dp</dimen>  <!-- 标准外边距 / 组件间 -->
<dimen name="space_lg">20dp</dimen>    <!-- 卡片间 -->
<dimen name="space_xl">24dp</dimen>    <!-- 区段间 -->
<dimen name="space_2xl">32dp</dimen>   <!-- 大区段间 -->

<!-- ===== 圆角体系 (4 级) ===== -->
<dimen name="radius_sm">8dp</dimen>    <!-- Chip / 小标签 -->
<dimen name="radius_md">12dp</dimen>   <!-- 按钮 / 输入框 -->
<dimen name="radius_lg">16dp</dimen>   <!-- 卡片 -->
<dimen name="radius_xl">24dp</dimen>   <!-- 搜索栏 / 大卡片 -->

<!-- ===== 字号体系 (sp, 支持系统缩放) ===== -->
<dimen name="text_caption">11sp</dimen>  <!-- 辅助说明 -->
<dimen name="text_label">12sp</dimen>    <!-- 标签 / Chip -->
<dimen name="text_body_sm">13sp</dimen>  <!-- 次要正文 -->
<dimen name="text_body">14sp</dimen>     <!-- 正文 -->
<dimen name="text_body_lg">15sp</dimen>  <!-- 释义正文 -->
<dimen name="text_title_sm">16sp</dimen> <!-- 小标题 -->
<dimen name="text_title">18sp</dimen>    <!-- 卡片标题 -->
<dimen name="text_title_lg">20sp</dimen> <!-- 游戏标题 -->
<dimen name="text_headline">22sp</dimen> <!-- 页面标题 -->
<dimen name="text_display">28sp</dimen>  <!-- 诗词标题 -->
<dimen name="text_stat">36sp</dimen>     <!-- 统计数字 -->

<!-- ===== 组件尺寸 ===== -->
<dimen name="touch_target">48dp</dimen>     <!-- 最小触控目标 -->
<dimen name="btn_height">48dp</dimen>       <!-- 按钮高度 -->
<dimen name="search_height">48dp</dimen>    <!-- 搜索栏高度 -->
<dimen name="avatar_size">64dp</dimen>      <!-- 头像尺寸 -->
<dimen name="game_icon_size">48dp</dimen>   <!-- 游戏图标尺寸 -->
<dimen name="card_elevation">2dp</dimen>    <!-- 卡片默认高度 -->
<dimen name="card_elevation_pressed">4dp</dimen> <!-- 卡片按下高度 -->

<!-- ===== 动画时长 ===== -->
<integer name="anim_fast">150</integer>     <!-- 微交互 -->
<integer name="anim_normal">300</integer>   <!-- 标准过渡 -->
<integer name="anim_slow">500</integer>     <!-- 强调动效 -->
```

### 2.2 令牌迁移映射

| 硬编码值 | 令牌引用 | 涉及文件数 |
|----------|----------|-----------|
| `16dp` (margin) | `@dimen/space_base` | 11 |
| `12dp` (margin/padding) | `@dimen/space_md` | 8 |
| `20dp` (margin) | `@dimen/space_lg` | 6 |
| `24dp` (margin) | `@dimen/space_xl` | 5 |
| `8dp` (padding/radius) | `@dimen/space_sm` / `@dimen/radius_sm` | 7 |
| `12dp` (cornerRadius) | `@dimen/radius_md` | 4 |
| `16dp` (cornerRadius) | `@dimen/radius_lg` | 6 |
| `48dp` (按钮高度) | `@dimen/btn_height` | 5 |
| `14sp` / `15sp` / `16sp` | 对应 text 令牌 | 15+ |

---

## 三、信息架构与导航优化

### 3.1 当前架构

```
底部导航(4 Tab)
├─ 首页 → 详情页
├─ 学习 → (无二级)
├─ 游戏 → 对诗 / 消消乐 / 填空
└─ 我的 → 收藏列表 → 详情页
```

### 3.2 优化后架构

```
底部导航(4 Tab)
├─ 首页
│   ├─ → 详情页（带 Toolbar 返回）
│   └─ → 收藏列表（从我的移入首页快捷入口）
├─ 学习
│   ├─ → 详情页（点击每日任务"学一首诗"）
│   └─ → 游戏（点击每日任务"玩一局游戏"）
├─ 游戏
│   ├─ → 对诗
│   ├─ → 消消乐
│   └─ → 填空
└─ 我的
    ├─ → 收藏列表（保留入口）
    ├─ → 成就详情页（新增）
    └─ → 设置页（新增）
```

**核心变更**：
- 详情页增加 `MaterialToolbar`，含返回箭头 + 诗词标题 + 收藏快捷图标
- 收藏列表在首页每日推荐卡下方增加快捷入口横幅
- 我的页面"设置"项拆为独立页面，承载字号缩放/减少动画/深色模式开关

### 3.3 导航转场规范

| 场景 | 动画 | 时长 |
|------|------|------|
| Tab 切换 | 无（即时） | 0ms |
| 进入详情页 | slide_in_right + slide_out_left | 300ms |
| 返回详情页 | slide_in_left + slide_out_right | 300ms |
| 进入游戏页 | slide_in_right + fade | 300ms |
| 弹窗出现 | spring_scale (overshoot) | 300ms |
| 成就解锁 | ConfettiView + scale_up | 500ms |

---

## 四、逐页面交互优化

### 4.1 首页 (fragment_home.xml)

#### 现状问题
- NestedScrollView 包裹 RecyclerView 导致全量渲染
- 搜索栏、每日推荐、分类标签、诗词列表纵向堆叠，滚动深度大
- 加载态只有转圈，无法预知页面结构

#### 优化方案

**A. 布局重构**
```
NestedScrollView (保留)
└─ ConstraintLayout
   ├─ 搜索栏 (固定在顶部，不随滚动消失)
   ├─ 每日推荐卡片 (Hero Card，增强视觉)
   ├─ 朝代分类标签 (横向滚动，不变)
   └─ 诗词列表区域
       ├─ 骨架屏 (加载态)
       ├─ 空状态 (搜索无结果)
       └─ RecyclerView (限制 maxHeight = 3 屏高，超出用"查看更多"按钮)
```

> **注意**：保留 NestedScrollView + nestedScrollingEnabled=false 的方案，因为首页内容总量可控（每日推荐 + 朝代标签 + 30 条分页诗词）。但需将 RecyclerView 的 `setMaxRecycledViews` 调高，并确保 DiffUtil 增量更新生效。

**B. 每日推荐卡片增强**
- 增加朝代色条（左侧 4dp 竖条，颜色随诗词朝代变化）
- 增加"已学"标记（若该诗已标记学习，右上角显示 ✓ 图标）
- 底部"点击查看详情 →"改为整个卡片可点击，去掉文字提示
- 卡片增加 `android:foreground="?attr/selectableItemBackground"` 水波纹

**C. 搜索栏优化**
- 清除按钮从 36dp → 48dp（满足触控目标）
- 搜索进度转圈尺寸保持 20dp，但增加 `android:indeterminateTint="@color/primary"`
- 搜索框获取焦点时增加 subtle elevation 变化（2dp → 4dp）

**D. 骨架屏**
- 加载态用 Skeleton 布局替换 ProgressBar + 文字
- 骨架结构：搜索栏轮廓 + 每日推荐卡轮廓 + 3 个诗词卡轮廓
- 骨架色：`surface_variant` → `divider` 闪烁渐变

**E. 诗词卡片 (item_poem_card.xml)**
- 修复 MissingConstraints：`tv_emoji` 缺少 `constraintTop_toTopOf` 和 `constraintBottom_toTopOf`
- 朝代 Chip 高度从 18dp → `@dimen/touch_target`(48dp 背景区域，Chip 视觉高度不变但触控扩大)
- 诗词标题字号从 15sp → `@dimen/text_title_sm`(16sp)
- 摘要文字增加 `android:lineSpacingExtra="2dp"` 提升可读性

### 4.2 详情页 (fragment_detail.xml)

#### 现状问题
- 无 Toolbar，无法直观返回
- 操作按钮在页面最底部，长诗需滚动很久才能触达
- 拼音切换按钮位置在诗句和释义之间，不够直观

#### 优化方案

**A. 增加顶部 Toolbar**
```xml
<com.google.android.material.appbar.AppBarLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/background"
    app:liftOnScroll="true">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        app:title="@string/detail_title_info"
        app:titleTextAppearance="@style/TextAppearance.Poetry.Title"
        app:navigationIcon="@drawable/ic_arrow_back"
        app:menu="@menu/menu_detail" />
</com.google.android.material.appbar.AppBarLayout>
```

Toolbar 菜单：
- `ic_favorite` / `ic_favorite_filled`（收藏切换，始终可见）
- `ic_share`（分享，始终可见）
- 溢出菜单：标记已学 / 朗读

**B. 底部操作栏改为 BottomActionBar**
```xml
<!-- 固定在底部的操作栏，不随滚动消失 -->
<LinearLayout
    android:id="@+id/bottom_action_bar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/surface"
    android:elevation="8dp"
    android:orientation="horizontal"
    android:padding="8dp"
    app:layout_constraintBottom_toBottomOf="parent">

    <!-- 朗读按钮 (主操作) -->
    <MaterialButton id="btn_read" icon="ic_volume" text="朗读" style="Primary" weight=1 />

    <!-- 拼音切换 (次要操作) -->
    <MaterialButton id="btn_pinyin" icon="ic_pinyin" text="拼音" style="Outlined" weight=1 />

    <!-- 标记已学 -->
    <MaterialButton id="btn_learn" icon="ic_check_circle" text="已学" style="Outlined" weight=1 />
</LinearLayout>
```

**C. 内容区域调整**
- Toolbar 下方：诗词标题 + 装饰条 + 作者朝代（不变）
- 诗句区域：增加 `android:paddingTop="@dimen/space_xl"` 增强呼吸感
- 拼音按钮从诗句下方移到底部操作栏（与朗读并列）
- 释义区域：标题"释义"前增加 `ic_book` 图标，增强视觉锚点
- 底部留 `@dimen/btn_height + 16dp` 的 padding，避免内容被 BottomActionBar 遮挡

**D. 返回导航**
- Toolbar navigationIcon 点击执行 `navController.popBackStack()`
- 增加系统返回键联动（OnBackPressedDispatcher）

### 4.3 学习页 (fragment_learning.xml)

#### 现状问题
- 统计卡片三列数字 36sp 过大，视觉压迫
- 打卡日历为动态 LinearLayout 横向排列，无法看出趋势
- 每日任务为纯文字 + 状态，缺乏进度反馈

#### 优化方案

**A. 统计卡片**
- 数字字号 36sp → `@dimen/text_stat`(36sp)，但增加 `android:letterSpacing="-0.02"` 收紧
- 三列之间分隔线从 1dp/48dp → `@dimen/space_sm`(8dp) 间距替代分隔线（更现代）
- 连续天数数字颜色从 tertiary → 增加 `app:drawableStart` 小火焰图标
- 增加卡片点击效果：点击连续天数 → 滚动到打卡日历

**B. 打卡日历**
- 保留 7 天横向布局，但增加：
  - 已打卡日期：圆角方块 + primary_container 底色 + ✓ 图标
  - 今日：primary 底色 + 白色文字 + 脉冲动画（if !reducedMotion）
  - 未来日期：outline_variant 描边 + 半透明文字
  - 断签日期：error 色 10% 透明度底色 + 灰色文字
- 增加横向滚动指示器（左右渐变遮罩）

**C. 每日任务**
- 每个任务行增加圆形进度环（0%/100%），未完成时灰色，完成后 primary + 勾选动画
- 任务点击直接跳转对应功能页（学一首诗 → 首页，做一套题 → 填空，玩一局 → 游戏中心）
- 完成状态从"已完成"文字 → 绿色 ✓ 图标 + `answer_correct` 色

**D. 学习趋势图表**
- 图表高度从 180dp → 160dp（减少压迫）
- 增加 X 轴星期标签（一/二/三/四/五/六/日）
- 柱状图颜色从 primary → 渐变（底部 primary，顶部 primary 50% 透明）
- 增加触摸提示：点击柱子显示 "周三：3 首"

### 4.4 游戏中心 (fragment_game_hub.xml)

#### 现状问题
- 三张卡片结构完全相同，仅 emoji + 文字不同
- 卡片图标用 emoji(📝🃏✏️)，与 Material 矢量图标体系不一致
- 缺乏游戏数据反馈（历史最高分、上次得分）

#### 优化方案

**A. 卡片视觉差异化**

| 游戏 | 图标方案 | 卡片左侧色条 | 背景 |
|------|----------|-------------|------|
| 对诗 | Material `ic_edit` 矢量图标 | tertiary(朱红) | tertiary_container 10% |
| 消消乐 | Material `ic_grid_view` 矢量图标 | primary(墨褐) | primary_container 10% |
| 填空 | Material `ic_text_fields` 矢量图标 | secondary(赭石) | secondary_container 10% |

- 图标背景从 `bg_checkin_future`(灰色方框) → 圆形 48dp + 对应游戏色 10% 底色
- 卡片左侧增加 4dp 竖向色条（朝代标签同款视觉语言）
- 卡片右上角增加"最高分"小标签（若有记录）：`🏆 42`，无记录则不显示

**B. 卡片交互**
- 增加水波纹 `android:foreground="?attr/selectableItemBackground"`
- 按下时卡片 elevation 2dp → 4dp + scale 0.98
- 增加 `android:stateListAnimator` 按压动效

**C. 页面顶部**
- 副标题"趣味挑战，寓教于乐"字号从 13sp → `@dimen/text_body_sm`(13sp)
- 增加总计游戏次数统计行："已挑战 12 次 · 最高得分 85"（若 > 0 才显示）

### 4.5 我的页面 (fragment_profile.xml)

#### 现状问题
- 头像为 📚 emoji + 灰色方框，缺乏品牌识别度
- 等级/经验区域信息密度低
- 成就/主题区域为水平 LinearLayout，项目多时会被截断
- 无设置入口

#### 优化方案

**A. 头像区域**
- 头像从 emoji 方框 → 圆形 64dp + primary_container 底色 + serif 字体显示等级数字
  - 例：Lv.5 → 圆形底色 + 白色 "5" 字
  - 视觉上更像"印章"，契合古风主题
- 等级标签从 score_gold 色文字 → primary_container 底色圆角标签
- 经验进度条从 6dp → 8dp 高度，圆角 4dp
- 增加等级名称（1级=新手 / 3级=学童 / 5级=秀才 / 7级=举人 / 9级=诗圣）

**B. 统计卡片**
- 收藏/已学卡片保留双列布局
- 每张卡片增加点击效果 → 跳转对应列表页
- 卡片内 emoji(❤️📖) → Material 矢量图标(ic_favorite / ic_learn) + 对应色

**C. 成就区域**
- 从横向 LinearLayout → 横向 RecyclerView + LinearLayoutManager(HORIZONTAL)
- 每个成就项：48dp 圆形图标 + 名称 + 已解锁/未解锁状态
- 未解锁成就：灰度 + 锁图标
- 已解锁成就：achievement_unlocked(金色) + 光晕背景
- 增加右上角"全部"文字按钮 → 跳转成就详情页

**D. 主题区域**
- 同上改为横向 RecyclerView
- 每个主题项：48dp 圆形色块（主题代表色） + 名称
- 当前主题：primary 色描边 2dp
- 未解锁主题：灰度 + 锁图标

**E. 新增设置入口**
- 底部分享按钮下方增加"设置"行：
  - `ic_settings` 图标 + "设置" 文字 + 右箭头
  - 点击跳转设置页（字号缩放 / 减少动画 / 深色模式 / 关于）

### 4.6 收藏页 (fragment_favorites.xml)

#### 现状问题
- 仅有列表，无搜索/排序/删除能力
- 空状态用 emoji 📭，与品牌不符

#### 优化方案

**A. 增加顶部排序栏**
- 排序选项：收藏时间(默认) / 朝代 / 标题
- 用 ChipGroup 实现，单选

**B. 列表项增加滑动删除**
- ItemTouchHelper 实现左滑删除 + 右滑取消收藏
- 删除后 Snackbar 提示"已取消收藏" + "撤销"按钮

**C. 空状态优化**
- emoji 📭 → Material `ic_favorite_border` 矢量图标 64dp + outline 色
- 提示文字保留，增加"去发现诗词"按钮 → 跳转首页

---

## 五、组件规范统一

### 5.1 按钮规范

| 类型 | 样式 | 高度 | 圆角 | 用途 |
|------|------|------|------|------|
| Primary | 填充 primary 色 | 48dp | 12dp | 主操作（朗读/提交） |
| Outlined | 描边 primary + 透明底 | 48dp | 12dp | 次要操作（拼音/分享） |
| Text | 无背景 | 48dp | - | 文字按钮（查看更多） |
| Icon | 无背景 + selectableItemBackground | 48dp | - | 工具栏图标 |

### 5.2 卡片规范

| 类型 | 圆角 | 高度 | 描边 | 用途 |
|------|------|------|------|------|
| Standard | 16dp | 2dp | 0.5dp outline_variant | 通用卡片 |
| Elevated | 16dp | 4dp | 无 | 首页每日推荐 |
| Surface | 12dp | 1dp | 0.5dp outline_variant | 列表项卡片 |
| Interactive | 16dp | 2dp→4dp | 0.5dp outline_variant | 可点击卡片(游戏中心) |

### 5.3 间距规范

| 场景 | 间距值 |
|------|--------|
| 页面左右边距 | `@dimen/space_base` (16dp) |
| 卡片间垂直间距 | `@dimen/space_base` (16dp) |
| 区段间垂直间距 | `@dimen/space_lg` (20dp) |
| 卡片内 padding | `@dimen/space_base` (16dp) |
| 按钮内图标与文字间距 | `@dimen/space_sm` (8dp) |
| Chip 间水平间距 | `@dimen/space_sm` (8dp) |
| 标题与副标题间距 | `@dimen/space_xs` (4dp) |

### 5.4 图标规范

**替换所有 Emoji 为 Material 矢量图标**：

| 当前 Emoji | 替换为 | 用途 |
|-----------|--------|------|
| 📝 | `ic_edit` (Material) | 对诗游戏 |
| 🃏 | `ic_grid_view` (Material) | 消消乐 |
| ✏️ | `ic_text_fields` (Material) | 填空 |
| 🌸 | 朝代色圆点 12dp | 诗词卡片装饰 |
| 📭 | `ic_favorite_border` (Material) | 空状态 |
| 📚 | 等级数字圆形底色 | 头像 |
| ❤️ | `ic_favorite` (Material) | 收藏统计 |
| 📖 | `ic_menu_book` (Material) | 已学统计 |
| 🎉 | `ic_celebration` (Material) | 完成弹窗 |
| 😿 | `ic_cloud_off` (Material) | 错误状态 |

> **例外**：诗词卡片的朝代 emoji(🌸🎋🍂❄️) 可保留，因其在诗词语境中有文化含义。但需替换为自定义 VectorDrawable 以保证跨设备一致性。

---

## 六、动效设计规范

### 6.1 动效原则

1. **有意义**：每个动画都要传达状态变化或空间关系
2. **克制**：200-300ms 为最佳感知区间，不超过 500ms
3. **可关闭**：所有非必要动画尊重"减少动画"开关

### 6.2 动效清单

| 场景 | 动效 | 时长 | 缓动 | 可关闭 |
|------|------|------|------|--------|
| Tab 切换 | 无 | - | - | - |
| 进入详情页 | slide_in_right | 300ms | Decelerate | 否 |
| 返回详情页 | slide_out_right | 300ms | Decelerate | 否 |
| 卡片点击 | scale 0.98 + elevation↑ | 150ms | FastOutSlowIn | 是 |
| 收藏点击 | heart_beat (已有) | 500ms | Overshoot | 是 |
| 成就解锁 | ConfettiView (已有) + scale | 500ms | Overshoot | 是 |
| 游戏配对成功 | spring_scale (已有) | 300ms | Overshoot | 是 |
| 答题正确 | 绿色闪烁 + scale | 300ms | FastOutSlowIn | 是 |
| 答题错误 | 左右摇晃 3 次 | 300ms | Linear | 是 |
| 列表项出现 | fade_in + slide_up 8dp | 200ms | Decelerate | 是 |
| 骨架屏闪烁 | shimmer 渐变 | 1200ms | Linear | 是 |
| 打卡成功 | 圆形缩放 + ✓ 淡入 | 400ms | Overshoot | 是 |

### 6.3 减少动画开关

```java
// 在设置页持久化
SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
boolean reducedMotion = prefs.getBoolean("pref_reduced_motion", false);

// 在各 Fragment 中判断
if (!reducedMotion) {
    view.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.heart_beat));
}
```

---

## 七、无障碍增强方案

### 7.1 字号缩放（三档）

| 档位 | scale | sp 倍率 | 实现 |
|------|-------|---------|------|
| 标准 | 100% | 1.0x | 默认 |
| 大 | 125% | 1.25x | `fontScale = 1.25f` |
| 超大 | 150% | 1.5x | `fontScale = 1.5f` |

**实现方式**：
- 设置页提供三档选择
- 通过 `Resources.getConfiguration().fontScale` 在 `attachBaseContext` 中设置
- 所有 textSize 使用 sp 单位（已满足），令牌化后统一管理

### 7.2 触控目标修复

| 组件 | 当前 | 修复后 |
|------|------|--------|
| 搜索清除按钮 | 36dp | 48dp (视觉图标 20dp + padding 14dp) |
| 朝代 Chip | 18dp 高 | 视觉不变，`android:minHeight="48dp"` + `android:paddingVertical="15dp"` |
| 选项按钮(对诗) | 无明确最小值 | `android:minHeight="48dp"` |
| 分享卡操作 | 无 | 确保所有 IconButton 48dp |

### 7.3 contentDescription 补全

需补充的位置：
- `fragment_detail.xml` 装饰条 → `android:importantForAccessibility="no"` (已有)
- `fragment_learning.xml` 分隔线 → 补充 `android:importantForAccessibility="no"`
- `fragment_game_hub.xml` 箭头"→" → 补充 `android:importantForAccessibility="no"`
- `item_poem_card.xml` emoji → 补充 `android:importantForAccessibility="no"`
- 统计数字 → 增加 `android:contentDescription` 描述完整信息
  - 例：`tv_streak_count` → "连续学习 7 天"

---

## 八、实施路线图

### Phase 1：基础修复（2 天）
- [ ] 新建 `dimens.xml`，定义全部令牌
- [ ] 修复 `item_poem_card.xml` MissingConstraints
- [ ] 硬编码字符串迁移至 `strings.xml`
- [ ] 触控目标 < 48dp 修复
- [ ] 补全 `importantForAccessibility="no"`

### Phase 2：详情页重构（2 天）
- [ ] 增加 `MaterialToolbar` + 返回导航
- [ ] 底部操作栏改为 `BottomActionBar` 固定底部
- [ ] 拼音按钮移至底部操作栏
- [ ] Toolbar 菜单：收藏/分享/溢出菜单

### Phase 3：视觉统一（3 天）
- [ ] 全量 Emoji → Material 矢量图标替换
- [ ] drawable 新增：`ic_edit` / `ic_grid_view` / `ic_text_fields` / `ic_favorite_border` / `ic_menu_book` / `ic_celebration` / `ic_cloud_off` / `ic_arrow_back` / `ic_settings`
- [ ] 圆角/间距/按钮高度全量替换为 `@dimen/` 令牌引用
- [ ] 游戏中心卡片视觉差异化（色条 + 图标底色 + 最高分）

### Phase 4：交互增强（3 天）
- [ ] 首页骨架屏
- [ ] 收藏页排序 + 滑动删除
- [ ] 学习页任务进度环 + 打卡日历增强
- [ ] 个人中心头像改为等级印章 + 成就/主题横向滚动
- [ ] 设置页（字号缩放 / 减少动画 / 深色模式）

### Phase 5：动效与无障碍（2 天）
- [ ] Fragment 转场动画配置
- [ ] 减少动画开关联动
- [ ] 字号缩放功能实现
- [ ] contentDescription 补全
- [ ] Lint 全量检查通过

---

## 九、验收标准

| 维度 | 标准 | 验证方式 |
|------|------|----------|
| Lint | 0 error, 0 P1 warning | `./gradlew lintDebug` |
| 令牌覆盖率 | 90%+ 间距/字号/圆角使用 @dimen 引用 | 代码搜索硬编码 dp/sp |
| 触控目标 | 所有可点击元素 ≥ 48dp | UIAutomator 布局检查 |
| 图标统一 | 0 Emoji 作为 UI 图标（诗词装饰除外） | 代码搜索 emoji |
| 字号缩放 | 150% 下无文字截断/布局溢出 | 手动测试 3 档 |
| 减少动画 | 开启后所有 P2 动效不执行 | 手动测试 |
| 编译 | assembleDebug BUILD SUCCESSFUL | Gradle 构建 |

---

*诗词乐园 — 让每一次交互，都如蘸墨落笔般自然。*
