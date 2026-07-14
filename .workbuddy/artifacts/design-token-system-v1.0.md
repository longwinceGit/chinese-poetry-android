# 诗词乐园 · 设计令牌系统 v1.0

> **设计者**: UI Designer (像素君)  
> **日期**: 2026-07-08  
> **适用范围**: Android View System (Java + XML)  
> **基础框架**: Material Design 3  
> **最小 API**: 21

---

## 一、设计哲学

### 1.1 三层令牌架构

```
┌─────────────────────────────────────────────────┐
│  L3 语义令牌 (Semantic Token)                    │
│  → 描述"用途"，不关心具体值                        │
│  → 如: text_color_title, spacing_card_padding    │
├─────────────────────────────────────────────────┤
│  L2 组件令牌 (Component Token)                    │
│  → 描述"组件属性"，组合多个语义令牌                 │
│  → 如: Widget.Poetry.Card, Widget.Poetry.Button  │
├─────────────────────────────────────────────────┤
│  L1 原始令牌 (Raw Token)                          │
│  → 描述"具体值"，唯一的值定义点                     │
│  → 如: color_primary, space_16, text_size_14     │
└─────────────────────────────────────────────────┘
```

### 1.2 命名规范

| 类别 | 前缀 | 格式 | 示例 |
|------|------|------|------|
| 间距 | `spacing_` | `spacing_{语义}` | `spacing_card_padding` |
| 字号 | `text_size_` | `text_size_{语义}` | `text_size_title` |
| 行高 | `line_height_` | `line_height_{语义}` | `line_height_body` |
| 圆角 | `radius_` | `radius_{级别}` | `radius_medium` |
| 阴影 | `elevation_` | `elevation_{级别}` | `elevation_card` |
| 动效 | `duration_` | `duration_{级别}` | `duration_normal` |
| 触摸目标 | `touch_target_` | `touch_target_{级别}` | `touch_target_min` |

---

## 二、令牌定义

### 2.1 间距令牌 (Spacing)

**基准单位**: 4dp（Material 4pt grid）  
**原则**: 所有间距必须从以下 8 级中选取，禁止使用其他值。

| 令牌名 | 值 | 语义 | 当前散落值映射 |
|--------|-----|------|---------------|
| `spacing_xs` | 2dp | 最小间距（图标内边距） | 2dp |
| `spacing_sm` | 4dp | 紧凑间距（卡片内元素间） | 4dp |
| `spacing_sm_md` | 6dp | 小中间距 → **废弃，归入 spacing_sm 或 spacing_md** | 6dp → 4dp 或 8dp |
| `spacing_md` | 8dp | 标准内间距（图标与文字间） | 8dp |
| `spacing_lg` | 12dp | 卡片间间距、Chip 间距 | 12dp |
| `spacing_xl` | 16dp | 页面边距、卡片内边距 | 16dp |
| `spacing_xl_2` | 20dp | 区块间距（卡片之间的大间距） | 20dp |
| `spacing_xxl` | 24dp | 详情页边距、页面顶部 | 24dp |
| `spacing_xxxl` | 32dp | 错误/空状态区域间距 | 32dp |
| `spacing_huge` | 48dp | 大型空状态图标区域 | 48dp |

> **迁移规则**: 6dp → 8dp（向上归入），10dp → 8dp 或 12dp（就近），14dp → 16dp（向上归入）

```xml
<!-- dimens.xml -->
<dimen name="spacing_xs">2dp</dimen>
<dimen name="spacing_sm">4dp</dimen>
<dimen name="spacing_md">8dp</dimen>
<dimen name="spacing_lg">12dp</dimen>
<dimen name="spacing_xl">16dp</dimen>
<dimen name="spacing_xl_2">20dp</dimen>
<dimen name="spacing_xxl">24dp</dimen>
<dimen name="spacing_xxxl">32dp</dimen>
<dimen name="spacing_huge">48dp</dimen>
```

**语义间距**:

```xml
<!-- 页面级 -->
<dimen name="spacing_page_horizontal">@dimen/spacing_xl</dimen>      <!-- 16dp -->
<dimen name="spacing_page_top">@dimen/spacing_xl_2</dimen>           <!-- 20dp -->
<dimen name="spacing_page_bottom">@dimen/spacing_xxl</dimen>         <!-- 24dp -->

<!-- 卡片级 -->
<dimen name="spacing_card_padding">@dimen/spacing_xl</dimen>         <!-- 16dp -->
<dimen name="spacing_card_gap">@dimen/spacing_lg</dimen>             <!-- 12dp -->
<dimen name="spacing_card_gap_large">@dimen/spacing_xl_2</dimen>     <!-- 20dp -->

<!-- 组件内 -->
<dimen name="spacing_icon_text">@dimen/spacing_md</dimen>            <!-- 8dp -->
<dimen name="spacing_chip_gap">@dimen/spacing_md</dimen>             <!-- 8dp -->
<dimen name="spacing_section_gap">@dimen/spacing_xl_2</dimen>        <!-- 20dp -->

<!-- 触摸目标 -->
<dimen name="touch_target_min">48dp</dimen>                          <!-- WCAG 44px min -->
<dimen name="touch_target_small">36dp</dimen>                         <!-- 紧凑型按钮 -->
```

---

### 2.2 排版令牌 (Typography)

**现状**: 15 种 textSize 散落（9/11/12/13/14/15/16/18/20/22/24/28/32/36/48sp）  
**目标**: 收敛为 11 级语义化梯度，覆盖全部使用场景。

| 令牌名 | sp | serif | 用途 | 当前值映射 |
|--------|-----|-------|------|-----------|
| `text_size_caption` | 10sp | no | Chip 最小标签（原 9sp → 上调至 10sp 保证可读性） | 9sp |
| `text_size_label_sm` | 11sp | no | 次要标签（统计标签、打卡提示） | 11sp |
| `text_size_label` | 12sp | no | 标签文本（每日推荐标签、副标题、Chip） | 12sp |
| `text_size_body_sm` | 13sp | no | 次要正文（描述、作者、提示文字） | 13sp |
| `text_size_body` | 14sp | no | 标准正文（搜索框、按钮文字、错误信息） | 14sp |
| `text_size_title_card` | 15sp | serif | 卡片标题（成就标题、任务标题、趋势标题） | 15sp |
| `text_size_title` | 16sp | serif | 区块标题（全部诗词、释义标题、作者） | 16sp |
| `text_size_headline` | 20sp | serif | 每日推荐诗题、游戏图标、对诗题目 | 20sp |
| `text_size_display_sm` | 22sp | serif | 页面标题（学习、游戏页标题、统计大数字） | 22sp |
| `text_size_display` | 28sp | serif | 详情页诗题 | 28sp |
| `text_size_stat` | 36sp | serif | 学习统计大数字（连续天数、已学数、等级） | 36sp |
| `text_size_emoji` | 48sp | no | 空状态/错误状态 Emoji | 48sp |

> **特殊值处理**: 18sp → 归入 `text_size_headline`(20sp)；24sp → 归入 `text_size_emoji`(24sp→保留为 `text_size_icon_emoji`); 32sp → 保留为 `text_size_avatar`(32sp)

```xml
<!-- dimens.xml: 字号 -->
<dimen name="text_size_caption">10sp</dimen>
<dimen name="text_size_label_sm">11sp</dimen>
<dimen name="text_size_label">12sp</dimen>
<dimen name="text_size_body_sm">13sp</dimen>
<dimen name="text_size_body">14sp</dimen>
<dimen name="text_size_title_card">15sp</dimen>
<dimen name="text_size_title">16sp</dimen>
<dimen name="text_size_headline">20sp</dimen>
<dimen name="text_size_display_sm">22sp</dimen>
<dimen name="text_size_display">28sp</dimen>
<dimen name="text_size_stat">36sp</dimen>
<dimen name="text_size_emoji">48sp</dimen>
<dimen name="text_size_icon_emoji">24sp</dimen>
<dimen name="text_size_avatar">32sp</dimen>
<dimen name="text_size_game_icon">28sp</dimen>

<!-- dimens.xml: 行高 -->
<dimen name="line_height_compact">4dp</dimen>      <!-- 紧凑行距（标题） -->
<dimen name="line_height_body">6dp</dimen>          <!-- 标准行距（释义正文） -->
<dimen name="line_height_loose">8dp</dimen>         <!-- 宽松行距（长文本） -->
```

**TextAppearance 样式映射**:

```xml
<!-- styles.xml: 排版样式 -->

<!-- 展示级：诗题、大标题 -->
<style name="TextAppearance.Poetry.Display" parent="TextAppearance.Material3.DisplaySmall">
    <item name="android:textSize">@dimen/text_size_display</item>
    <item name="android:textColor">@color/primary</item>
    <item name="android:fontFamily">serif</item>
    <item name="android:textStyle">bold</item>
    <item name="android:lineSpacingExtra">@dimen/line_height_compact</item>
</style>

<!-- 页面标题：学习/游戏页 -->
<style name="TextAppearance.Poetry.PageTitle" parent="TextAppearance.Material3.HeadlineMedium">
    <item name="android:textSize">@dimen/text_size_display_sm</item>
    <item name="android:textColor">@color/primary</item>
    <item name="android:fontFamily">serif</item>
    <item name="android:textStyle">bold</item>
</style>

<!-- 区块标题 -->
<style name="TextAppearance.Poetry.SectionTitle" parent="TextAppearance.Material3.TitleLarge">
    <item name="android:textSize">@dimen/text_size_title</item>
    <item name="android:textColor">@color/primary</item>
    <item name="android:fontFamily">serif</item>
    <item name="android:textStyle">bold</item>
</style>

<!-- 卡片标题 -->
<style name="TextAppearance.Poetry.CardTitle" parent="TextAppearance.Material3.TitleMedium">
    <item name="android:textSize">@dimen/text_size_title_card</item>
    <item name="android:textColor">@color/on_surface</item>
    <item name="android:fontFamily">serif</item>
    <item name="android:textStyle">bold</item>
</style>

<!-- 标准正文 -->
<style name="TextAppearance.Poetry.Body" parent="TextAppearance.Material3.BodyLarge">
    <item name="android:textSize">@dimen/text_size_body</item>
    <item name="android:textColor">@color/on_surface</item>
</style>

<!-- 次要正文（描述、作者） -->
<style name="TextAppearance.Poetry.BodySecondary" parent="TextAppearance.Material3.BodyMedium">
    <item name="android:textSize">@dimen/text_size_body_sm</item>
    <item name="android:textColor">@color/on_surface_variant</item>
</style>

<!-- 标签 -->
<style name="TextAppearance.Poetry.Label" parent="TextAppearance.Material3.LabelMedium">
    <item name="android:textSize">@dimen/text_size_label</item>
    <item name="android:textColor">@color/on_surface_variant</item>
</style>

<!-- 释义正文（特殊：较大字号 + 宽行距） -->
<style name="TextAppearance.Poetry.Explanation" parent="TextAppearance.Material3.BodyLarge">
    <item name="android:textSize">15sp</item>
    <item name="android:textColor">@color/on_surface</item>
    <item name="android:lineSpacingExtra">@dimen/line_height_body</item>
    <item name="android:textAlignment">viewStart</item>
</style>

<!-- 统计数字（特殊：超大号 + 各自颜色） -->
<style name="TextAppearance.Poetry.StatNumber" parent="TextAppearance.Material3.DisplayLarge">
    <item name="android:textSize">@dimen/text_size_stat</item>
    <item name="android:textStyle">bold</item>
</style>

<!-- 每日推荐标签（朱红 + 加粗 + 字间距） -->
<style name="TextAppearance.Poetry.DailyLabel" parent="TextAppearance.Material3.LabelSmall">
    <item name="android:textSize">@dimen/text_size_label</item>
    <item name="android:textColor">@color/tertiary</item>
    <item name="android:textStyle">bold</item>
    <item name="android:letterSpacing">0.1</item>
</style>
```

---

### 2.3 颜色令牌 (Color)

**现状**: colors.xml 已有 71 个颜色，Material 3 语义命名完整。  
**优化**: 新增语义层令牌，让布局引用语义名而非原始色名。

#### 2.3.1 原始色（已有，不变）

```xml
<!-- values/colors.xml 保持不变 -->
<color name="primary">#5D4037</color>
<color name="tertiary">#C62828</color>
<color name="surface">#FAF7F0</color>
<!-- ... 其余 71 个色值不变 ... -->
```

#### 2.3.2 新增语义色令牌

```xml
<!-- values/colors.xml: 新增语义层 -->

<!-- 文字语义色 -->
<color name="text_title">@color/primary</color>                    <!-- 标题文字 -->
<color name="text_body">@color/on_surface</color>                  <!-- 正文文字 -->
<color name="text_body_secondary">@color/on_surface_variant</color><!-- 次要文字 -->
<color name="text_caption">@color/outline</color>                  <!-- 提示/占位文字 -->
<color name="text_accent">@color/tertiary</color>                  <!-- 强调文字（朱红） -->
<color name="text_on_primary">@color/on_primary</color>            <!-- 主色按钮上的文字 -->
<color name="text_gold">@color/score_gold</color>                  <!-- 金色文字（等级） -->

<!-- 背景语义色 -->
<color name="bg_page">@color/background</color>                    <!-- 页面背景 -->
<color name="bg_card">@color/surface</color>                       <!-- 卡片背景 -->
<color name="bg_card_elevated">@color/surface_variant</color>      <!-- 凸起卡片背景 -->

<!-- 分隔线语义色 -->
<color name="divider_default">@color/divider</color>
<color name="divider_decorative">@color/tertiary</color>           <!-- 装饰条（朱红） -->

<!-- 状态语义色 -->
<color name="state_success">@color/answer_correct</color>
<color name="state_error">@color/answer_wrong</color>
<color name="state_warning">@color/tertiary</color>
<color name="state_disabled">@color/answer_disabled</color>
```

#### 2.3.3 暗色模式语义色覆盖

```xml
<!-- values-night/colors.xml: 语义层自动继承 night 覆盖 -->
<!-- 无需额外定义，因为语义色引用的原始色已有 night 版本 -->
```

#### 2.3.4 朝代标签色系（已有，保留）

朝代标签色保持 `tag_tang` / `tag_song` / ... 命名，不归入语义层（它们是领域特定色，不是通用语义色）。

---

### 2.4 圆角令牌 (Shape)

**现状**: 6 种圆角值散落（8/10/12/16/20/24dp）  
**目标**: 收敛为 5 级。

| 令牌名 | 值 | 用途 | 当前值映射 |
|--------|-----|------|-----------|
| `radius_none` | 0dp | 无圆角（分隔线） | — |
| `radius_small` | 8dp | 小型组件（Chip、小按钮） | 8dp |
| `radius_medium` | 12dp | 中型组件（标准按钮、诗词卡片） | 10dp→12dp, 12dp |
| `radius_large` | 16dp | 大型组件（内容卡片、游戏卡片） | 16dp |
| `radius_xlarge` | 24dp | 胶囊型（搜索框、Pill） | 20dp→24dp, 24dp |

```xml
<!-- dimens.xml: 圆角 -->
<dimen name="radius_none">0dp</dimen>
<dimen name="radius_small">8dp</dimen>
<dimen name="radius_medium">12dp</dimen>
<dimen name="radius_large">16dp</dimen>
<dimen name="radius_xlarge">24dp</dimen>
```

**ShapeAppearance 样式**:

```xml
<!-- styles.xml: 形状（更新现有定义） -->

<style name="ShapeAppearance.Poetry.Small" parent="">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">@dimen/radius_small</item>           <!-- 8dp -->
</style>

<style name="ShapeAppearance.Poetry.Medium" parent="">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">@dimen/radius_medium</item>          <!-- 12dp -->
</style>

<style name="ShapeAppearance.Poetry.Large" parent="">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">@dimen/radius_large</item>           <!-- 16dp -->
</style>

<style name="ShapeAppearance.Poetry.Pill" parent="">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">@dimen/radius_xlarge</item>          <!-- 24dp -->
</style>
```

---

### 2.5 阴影令牌 (Elevation)

**现状**: 仅 1dp 和 2dp 两个值  
**目标**: 定义 4 级，覆盖 Material 3 elevation 规范。

| 令牌名 | 值 | 用途 |
|--------|-----|------|
| `elevation_none` | 0dp | 平面元素（列表项） |
| `elevation_card` | 1dp | 标准卡片 |
| `elevation_card_raised` | 2dp | 凸起卡片（可点击的） |
| `elevation_overlay` | 4dp | 浮层（底部固定操作栏、Dialog） |

```xml
<!-- dimens.xml: 阴影 -->
<dimen name="elevation_none">0dp</dimen>
<dimen name="elevation_card">1dp</dimen>
<dimen name="elevation_card_raised">2dp</dimen>
<dimen name="elevation_overlay">4dp</dimen>
```

---

### 2.6 动效令牌 (Motion)

**现状**: anim 目录有 spring_scale.xml 和 heart_beat.xml，但无统一时长令牌。  
**目标**: 定义 3 级标准时长 + 缓动函数。

| 令牌名 | 值 | 用途 |
|--------|-----|------|
| `duration_fast` | 150ms | 按钮按压、Chip 选中 |
| `duration_normal` | 300ms | 卡片展开、页面过渡 |
| `duration_slow` | 500ms | 成就解锁动画、分享卡生成 |

```xml
<!-- dimens.xml: 动效时长 -->
<integer name="duration_fast">150</integer>
<integer name="duration_normal">300</integer>
<integer name="duration_slow">500</integer>
```

**缓动函数** (res/anim/ 新增):

```xml
<!-- res/anim/ease_fast_out_slow_in.xml -->
<pathInterpolator xmlns:android="http://schemas.android.com/apk/res/android"
    android:pathData="M 0,0 C 0.05,0,0.133333,0.06,0.166666,0.4 C 0.208333,0.82,0.25,1,1,1" />

<!-- res/anim/ease_linear.xml -->
<pathInterpolator xmlns:android="http://schemas.android.com/apk/res/android"
    android:pathData="M 0,0 L 1,1" />
```

---

### 2.7 描边令牌 (Stroke)

| 令牌名 | 值 | 用途 |
|--------|-----|------|
| `stroke_none` | 0dp | 无描边 |
| `stroke_thin` | 0.5dp | 卡片描边（默认） |
| `stroke_normal` | 1dp | Chip 描边、输入框 |

```xml
<!-- dimens.xml: 描边 -->
<dimen name="stroke_none">0dp</dimen>
<dimen name="stroke_thin">0.5dp</dimen>
<dimen name="stroke_normal">1dp</dimen>
```

---

### 2.8 组件尺寸令牌 (Component Size)

| 令牌名 | 值 | 用途 |
|--------|-----|------|
| `height_button` | 48dp | 标准按钮高度 |
| `height_button_small` | 36dp | 小型按钮（拼音切换） |
| `height_search_bar` | 48dp | 搜索栏高度 |
| `height_divider` | 1dp | 分隔线粗细 |
| `height_divider_decorative` | 2dp | 装饰条粗细 |
| `size_icon_sm` | 20dp | 小图标（搜索图标） |
| `size_icon_md` | 24dp | 中图标（导航图标） |
| `size_icon_lg` | 48dp | 大图标（游戏入口图标） |
| `size_avatar` | 64dp | 头像尺寸 |
| `size_progress_sm` | 20dp | 小型 ProgressBar |
| `size_progress_md` | 24dp | 中型 ProgressBar |
| `size_progress_lg` | 32dp | 大型 ProgressBar |
| `width_divider_decorative` | 48dp | 装饰条宽度 |

```xml
<!-- dimens.xml: 组件尺寸 -->
<dimen name="height_button">48dp</dimen>
<dimen name="height_button_small">36dp</dimen>
<dimen name="height_search_bar">48dp</dimen>
<dimen name="height_divider">1dp</dimen>
<dimen name="height_divider_decorative">2dp</dimen>
<dimen name="size_icon_sm">20dp</dimen>
<dimen name="size_icon_md">24dp</dimen>
<dimen name="size_icon_lg">48dp</dimen>
<dimen name="size_avatar">64dp</dimen>
<dimen name="size_progress_sm">20dp</dimen>
<dimen name="size_progress_md">24dp</dimen>
<dimen name="size_progress_lg">32dp</dimen>
<dimen name="width_divider_decorative">48dp</dimen>
```

---

## 三、组件样式规范

### 3.1 卡片 (已更新)

```xml
<style name="Widget.Poetry.Card" parent="Widget.Material3.CardView.Elevated">
    <item name="cardBackgroundColor">@color/bg_card</item>
    <item name="cardElevation">@dimen/elevation_card</item>
    <item name="cardCornerRadius">@dimen/radius_large</item>
    <item name="strokeColor">@color/outline_variant</item>
    <item name="strokeWidth">@dimen/stroke_thin</item>
    <item name="contentPadding">@dimen/spacing_card_padding</item>
</style>

<!-- 凸起卡片（可点击的游戏入口卡片等） -->
<style name="Widget.Poetry.Card.Raised" parent="Widget.Poetry.Card">
    <item name="cardElevation">@dimen/elevation_card_raised</item>
</style>

<!-- 搜索框卡片（胶囊型） -->
<style name="Widget.Poetry.Card.Search" parent="Widget.Poetry.Card">
    <item name="cardCornerRadius">@dimen/radius_xlarge</item>
    <item name="cardElevation">@dimen/elevation_card_raised</item>
    <item name="strokeWidth">@dimen/stroke_none</item>
    <item name="contentPadding">0dp</item>
</style>
```

### 3.2 按钮 (已更新)

```xml
<!-- 标准按钮（主操作：朗读、学习） -->
<style name="Widget.Poetry.Button" parent="Widget.Material3.Button">
    <item name="android:minHeight">@dimen/height_button</item>
    <item name="android:textSize">@dimen/text_size_body</item>
    <item name="shapeAppearance">@style/ShapeAppearance.Poetry.Medium</item>
    <item name="iconGravity">textStart</item>
    <item name="iconPadding">@dimen/spacing_md</item>
</style>

<!-- 描边按钮（次操作：收藏、分享） -->
<style name="Widget.Poetry.Button.Outlined" parent="Widget.Material3.Button.OutlinedButton">
    <item name="android:minHeight">@dimen/height_button</item>
    <item name="android:textSize">@dimen/text_size_body</item>
    <item name="shapeAppearance">@style/ShapeAppearance.Poetry.Medium</item>
    <item name="strokeColor">@color/primary</item>
    <item name="iconGravity">textStart</item>
    <item name="iconPadding">@dimen/spacing_md</item>
</style>

<!-- 小型按钮（拼音切换等辅助操作） -->
<style name="Widget.Poetry.Button.Small" parent="Widget.Poetry.Button.Outlined">
    <item name="android:minHeight">@dimen/height_button_small</item>
    <item name="android:textSize">@dimen/text_size_label</item>
</style>
```

### 3.3 Chip (已更新)

```xml
<style name="Widget.Poetry.Chip" parent="Widget.Material3.Chip.Filter">
    <item name="chipBackgroundColor">@color/bg_card_elevated</item>
    <item name="checkedIconVisible">false</item>
    <item name="shapeAppearance">@style/ShapeAppearance.Poetry.Pill</item>
    <item name="android:textColor">@color/text_body_secondary</item>
    <item name="chipStrokeColor">@color/outline_variant</item>
    <item name="chipStrokeWidth">@dimen/stroke_thin</item>
    <item name="chipMinHeight">32dp</item>
    <item name="android:textSize">@dimen/text_size_body</item>
</style>

<!-- 朝代标签 Chip（小型） -->
<style name="Widget.Poetry.Chip.Tag" parent="Widget.Material3.Chip.Assist">
    <item name="checkedIconVisible">false</item>
    <item name="shapeAppearance">@style/ShapeAppearance.Poetry.Pill</item>
    <item name="chipMinHeight">18dp</item>
    <item name="android:textSize">@dimen/text_size_caption</item>
    <item name="chipStartPadding">6dp</item>
    <item name="chipEndPadding">6dp</item>
    <item name="android:clickable">false</item>
    <item name="android:checkable">false</item>
</style>
```

---

## 四、完整 dimens.xml

> 以下为可直接创建的 `app/src/main/res/values/dimens.xml` 完整内容。

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <!-- ================================================================ -->
    <!-- 间距令牌 (Spacing) — 基准 4dp grid                              -->
    <!-- ================================================================ -->
    <dimen name="spacing_xs">2dp</dimen>
    <dimen name="spacing_sm">4dp</dimen>
    <dimen name="spacing_md">8dp</dimen>
    <dimen name="spacing_lg">12dp</dimen>
    <dimen name="spacing_xl">16dp</dimen>
    <dimen name="spacing_xl_2">20dp</dimen>
    <dimen name="spacing_xxl">24dp</dimen>
    <dimen name="spacing_xxxl">32dp</dimen>
    <dimen name="spacing_huge">48dp</dimen>

    <!-- 语义间距 -->
    <dimen name="spacing_page_horizontal">@dimen/spacing_xl</dimen>
    <dimen name="spacing_page_top">@dimen/spacing_xl_2</dimen>
    <dimen name="spacing_page_bottom">@dimen/spacing_xxl</dimen>
    <dimen name="spacing_card_padding">@dimen/spacing_xl</dimen>
    <dimen name="spacing_card_gap">@dimen/spacing_lg</dimen>
    <dimen name="spacing_card_gap_large">@dimen/spacing_xl_2</dimen>
    <dimen name="spacing_icon_text">@dimen/spacing_md</dimen>
    <dimen name="spacing_chip_gap">@dimen/spacing_md</dimen>
    <dimen name="spacing_section_gap">@dimen/spacing_xl_2</dimen>

    <!-- ================================================================ -->
    <!-- 排版令牌 (Typography)                                           -->
    <!-- ================================================================ -->
    <dimen name="text_size_caption">10sp</dimen>
    <dimen name="text_size_label_sm">11sp</dimen>
    <dimen name="text_size_label">12sp</dimen>
    <dimen name="text_size_body_sm">13sp</dimen>
    <dimen name="text_size_body">14sp</dimen>
    <dimen name="text_size_title_card">15sp</dimen>
    <dimen name="text_size_title">16sp</dimen>
    <dimen name="text_size_headline">20sp</dimen>
    <dimen name="text_size_display_sm">22sp</dimen>
    <dimen name="text_size_display">28sp</dimen>
    <dimen name="text_size_stat">36sp</dimen>
    <dimen name="text_size_emoji">48sp</dimen>
    <dimen name="text_size_icon_emoji">24sp</dimen>
    <dimen name="text_size_avatar">32sp</dimen>
    <dimen name="text_size_game_icon">28sp</dimen>

    <!-- 行高 -->
    <dimen name="line_height_compact">4dp</dimen>
    <dimen name="line_height_body">6dp</dimen>
    <dimen name="line_height_loose">8dp</dimen>

    <!-- ================================================================ -->
    <!-- 圆角令牌 (Shape)                                                -->
    <!-- ================================================================ -->
    <dimen name="radius_none">0dp</dimen>
    <dimen name="radius_small">8dp</dimen>
    <dimen name="radius_medium">12dp</dimen>
    <dimen name="radius_large">16dp</dimen>
    <dimen name="radius_xlarge">24dp</dimen>

    <!-- ================================================================ -->
    <!-- 阴影令牌 (Elevation)                                            -->
    <!-- ================================================================ -->
    <dimen name="elevation_none">0dp</dimen>
    <dimen name="elevation_card">1dp</dimen>
    <dimen name="elevation_card_raised">2dp</dimen>
    <dimen name="elevation_overlay">4dp</dimen>

    <!-- ================================================================ -->
    <!-- 描边令牌 (Stroke)                                               -->
    <!-- ================================================================ -->
    <dimen name="stroke_none">0dp</dimen>
    <dimen name="stroke_thin">0.5dp</dimen>
    <dimen name="stroke_normal">1dp</dimen>

    <!-- ================================================================ -->
    <!-- 组件尺寸令牌 (Component Size)                                   -->
    <!-- ================================================================ -->
    <dimen name="height_button">48dp</dimen>
    <dimen name="height_button_small">36dp</dimen>
    <dimen name="height_search_bar">48dp</dimen>
    <dimen name="height_divider">1dp</dimen>
    <dimen name="height_divider_decorative">2dp</dimen>
    <dimen name="size_icon_sm">20dp</dimen>
    <dimen name="size_icon_md">24dp</dimen>
    <dimen name="size_icon_lg">48dp</dimen>
    <dimen name="size_avatar">64dp</dimen>
    <dimen name="size_progress_sm">20dp</dimen>
    <dimen name="size_progress_md">24dp</dimen>
    <dimen name="size_progress_lg">32dp</dimen>
    <dimen name="width_divider_decorative">48dp</dimen>

    <!-- ================================================================ -->
    <!-- 触摸目标 (Touch Target) — WCAG 44px minimum                     -->
    <!-- ================================================================ -->
    <dimen name="touch_target_min">48dp</dimen>
    <dimen name="touch_target_small">36dp</dimen>

    <!-- ================================================================ -->
    <!-- 动效时长 (Motion Duration)                                      -->
    <!-- ================================================================ -->
    <integer name="duration_fast">150</integer>
    <integer name="duration_normal">300</integer>
    <integer name="duration_slow">500</integer>

</resources>
```

---

## 五、colors.xml 新增语义色

> 在现有 `values/colors.xml` 末尾 `</resources>` 前追加：

```xml
    <!-- ================================================================ -->
    <!-- 语义色令牌 (Semantic Color) — 引用原始色，不直接定义值            -->
    <!-- ================================================================ -->

    <!-- 文字语义色 -->
    <color name="text_title">@color/primary</color>
    <color name="text_body">@color/on_surface</color>
    <color name="text_body_secondary">@color/on_surface_variant</color>
    <color name="text_caption">@color/outline</color>
    <color name="text_accent">@color/tertiary</color>
    <color name="text_on_primary">@color/on_primary</color>
    <color name="text_gold">@color/score_gold</color>

    <!-- 背景语义色 -->
    <color name="bg_page">@color/background</color>
    <color name="bg_card">@color/surface</color>
    <color name="bg_card_elevated">@color/surface_variant</color>

    <!-- 分隔线语义色 -->
    <color name="divider_default">@color/divider</color>
    <color name="divider_decorative">@color/tertiary</color>

    <!-- 状态语义色 -->
    <color name="state_success">@color/answer_correct</color>
    <color name="state_error">@color/answer_wrong</color>
    <color name="state_warning">@color/tertiary</color>
    <color name="state_disabled">@color/answer_disabled</color>
```

---

## 六、styles.xml 更新

> 更新现有 styles.xml 中的硬编码值为 dimens 引用：

```xml
<!-- 更新卡片样式 -->
<style name="Widget.Poetry.Card" parent="Widget.Material3.CardView.Elevated">
    <item name="cardBackgroundColor">@color/bg_card</item>
    <item name="cardElevation">@dimen/elevation_card</item>
    <item name="cardCornerRadius">@dimen/radius_large</item>
    <item name="strokeColor">@color/outline_variant</item>
    <item name="strokeWidth">@dimen/stroke_thin</item>
    <item name="contentPadding">@dimen/spacing_card_padding</item>
</style>

<!-- 新增凸起卡片 -->
<style name="Widget.Poetry.Card.Raised" parent="Widget.Poetry.Card">
    <item name="cardElevation">@dimen/elevation_card_raised</item>
</style>

<!-- 新增搜索框卡片 -->
<style name="Widget.Poetry.Card.Search" parent="Widget.Poetry.Card">
    <item name="cardCornerRadius">@dimen/radius_xlarge</item>
    <item name="cardElevation">@dimen/elevation_card_raised</item>
    <item name="strokeWidth">@dimen/stroke_none</item>
    <item name="contentPadding">0dp</item>
</style>

<!-- 更新按钮样式 -->
<style name="Widget.Poetry.Button" parent="Widget.Material3.Button">
    <item name="android:minHeight">@dimen/height_button</item>
    <item name="android:textSize">@dimen/text_size_body</item>
    <item name="shapeAppearance">@style/ShapeAppearance.Poetry.Medium</item>
    <item name="iconGravity">textStart</item>
    <item name="iconPadding">@dimen/spacing_md</item>
</style>

<style name="Widget.Poetry.Button.Outlined" parent="Widget.Material3.Button.OutlinedButton">
    <item name="android:minHeight">@dimen/height_button</item>
    <item name="android:textSize">@dimen/text_size_body</item>
    <item name="shapeAppearance">@style/ShapeAppearance.Poetry.Medium</item>
    <item name="strokeColor">@color/primary</item>
    <item name="iconGravity">textStart</item>
    <item name="iconPadding">@dimen/spacing_md</item>
</style>

<!-- 新增小型按钮 -->
<style name="Widget.Poetry.Button.Small" parent="Widget.Poetry.Button.Outlined">
    <item name="android:minHeight">@dimen/height_button_small</item>
    <item name="android:textSize">@dimen/text_size_label</item>
</style>

<!-- 更新 ShapeAppearance -->
<style name="ShapeAppearance.Poetry.Small" parent="">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">@dimen/radius_small</item>
</style>

<style name="ShapeAppearance.Poetry.Medium" parent="">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">@dimen/radius_medium</item>
</style>

<style name="ShapeAppearance.Poetry.Large" parent="">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">@dimen/radius_large</item>
</style>

<style name="ShapeAppearance.Poetry.Pill" parent="">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">@dimen/radius_xlarge</item>
</style>

<!-- 新增排版样式 -->
<style name="TextAppearance.Poetry.PageTitle" parent="TextAppearance.Material3.HeadlineMedium">
    <item name="android:textSize">@dimen/text_size_display_sm</item>
    <item name="android:textColor">@color/text_title</item>
    <item name="android:fontFamily">serif</item>
    <item name="android:textStyle">bold</item>
</style>

<style name="TextAppearance.Poetry.SectionTitle" parent="TextAppearance.Material3.TitleLarge">
    <item name="android:textSize">@dimen/text_size_title</item>
    <item name="android:textColor">@color/text_title</item>
    <item name="android:fontFamily">serif</item>
    <item name="android:textStyle">bold</item>
</style>

<style name="TextAppearance.Poetry.CardTitle" parent="TextAppearance.Material3.TitleMedium">
    <item name="android:textSize">@dimen/text_size_title_card</item>
    <item name="android:textColor">@color/text_body</item>
    <item name="android:fontFamily">serif</item>
    <item name="android:textStyle">bold</item>
</style>

<style name="TextAppearance.Poetry.BodySecondary" parent="TextAppearance.Material3.BodyMedium">
    <item name="android:textSize">@dimen/text_size_body_sm</item>
    <item name="android:textColor">@color/text_body_secondary</item>
</style>

<style name="TextAppearance.Poetry.Explanation" parent="TextAppearance.Material3.BodyLarge">
    <item name="android:textSize">15sp</item>
    <item name="android:textColor">@color/text_body</item>
    <item name="android:lineSpacingExtra">@dimen/line_height_body</item>
    <item name="android:textAlignment">viewStart</item>
</style>

<style name="TextAppearance.Poetry.StatNumber" parent="TextAppearance.Material3.DisplayLarge">
    <item name="android:textSize">@dimen/text_size_stat</item>
    <item name="android:textStyle">bold</item>
</style>

<style name="TextAppearance.Poetry.DailyLabel" parent="TextAppearance.Material3.LabelSmall">
    <item name="android:textSize">@dimen/text_size_label</item>
    <item name="android:textColor">@color/text_accent</item>
    <item name="android:textStyle">bold</item>
    <item name="android:letterSpacing">0.1</item>
</style>

<!-- 更新 Chip 样式 -->
<style name="Widget.Poetry.Chip" parent="Widget.Material3.Chip.Filter">
    <item name="chipBackgroundColor">@color/bg_card_elevated</item>
    <item name="checkedIconVisible">false</item>
    <item name="shapeAppearance">@style/ShapeAppearance.Poetry.Pill</item>
    <item name="android:textColor">@color/text_body_secondary</item>
    <item name="chipStrokeColor">@color/outline_variant</item>
    <item name="chipStrokeWidth">@dimen/stroke_thin</item>
    <item name="chipMinHeight">32dp</item>
    <item name="android:textSize">@dimen/text_size_body</item>
</style>

<style name="Widget.Poetry.Chip.Tag" parent="Widget.Material3.Chip.Assist">
    <item name="checkedIconVisible">false</item>
    <item name="shapeAppearance">@style/ShapeAppearance.Poetry.Pill</item>
    <item name="chipMinHeight">18dp</item>
    <item name="android:textSize">@dimen/text_size_caption</item>
    <item name="chipStartPadding">6dp</item>
    <item name="chipEndPadding">6dp</item>
    <item name="android:clickable">false</item>
    <item name="android:checkable">false</item>
</style>
```

---

## 七、迁移映射表

### 7.1 字号迁移

| 当前值 | → | 令牌名 | 令牌值 | 备注 |
|--------|---|--------|--------|------|
| 9sp | → | `text_size_caption` | 10sp | 上调 1sp 保证可读性 |
| 11sp | → | `text_size_label_sm` | 11sp | 不变 |
| 12sp | → | `text_size_label` | 12sp | 不变 |
| 13sp | → | `text_size_body_sm` | 13sp | 不变 |
| 14sp | → | `text_size_body` | 14sp | 不变 |
| 15sp | → | `text_size_title_card` | 15sp | 不变 |
| 16sp | → | `text_size_title` | 16sp | 不变 |
| 18sp | → | `text_size_headline` | 20sp | 上调 2sp，归入 headline |
| 20sp | → | `text_size_headline` | 20sp | 不变 |
| 22sp | → | `text_size_display_sm` | 22sp | 不变 |
| 24sp | → | `text_size_icon_emoji` | 24sp | 新令牌 |
| 28sp | → | `text_size_display` | 28sp | 不变 |
| 32sp | → | `text_size_avatar` | 32sp | 新令牌 |
| 36sp | → | `text_size_stat` | 36sp | 不变 |
| 48sp | → | `text_size_emoji` | 48sp | 不变 |

### 7.2 间距迁移

| 当前值 | → | 令牌名 | 备注 |
|--------|---|--------|------|
| 2dp | → | `spacing_xs` | — |
| 4dp | → | `spacing_sm` | — |
| 6dp | → | `spacing_sm` 或 `spacing_md` | 就近归入 |
| 8dp | → | `spacing_md` | — |
| 10dp | → | `spacing_md` 或 `spacing_lg` | 就近归入 |
| 12dp | → | `spacing_lg` | — |
| 14dp | → | `spacing_xl` | 向上归入 |
| 16dp | → | `spacing_xl` | — |
| 20dp | → | `spacing_xl_2` | — |
| 24dp | → | `spacing_xxl` | — |
| 32dp | → | `spacing_xxxl` | — |
| 40dp | → | `spacing_xxxl` 或 `spacing_huge` | 就近归入 |
| 48dp | → | `spacing_huge` | — |

### 7.3 圆角迁移

| 当前值 | → | 令牌名 | 令牌值 | 备注 |
|--------|---|--------|--------|------|
| 8dp | → | `radius_small` | 8dp | 不变 |
| 10dp | → | `radius_medium` | 12dp | 上调 2dp 统一 |
| 12dp | → | `radius_medium` | 12dp | 不变 |
| 16dp | → | `radius_large` | 16dp | 不变 |
| 20dp | → | `radius_xlarge` | 24dp | 上调 4dp 统一 |
| 24dp | → | `radius_xlarge` | 24dp | 不变 |

### 7.4 按钮高度迁移

| 当前值 | → | 令牌名 | 备注 |
|--------|---|--------|------|
| 36dp | → | `height_button_small` | 小型按钮 |
| 40dp | → | `height_button` | 上调 8dp 统一（旧"加载更多"按钮） |
| 48dp | → | `height_button` | 标准按钮 |

---

## 八、使用指南

### 8.1 布局 XML 中使用

```xml
<!-- ❌ 禁止：硬编码值 -->
<TextView
    android:textSize="16sp"
    android:textColor="@color/primary"
    android:layout_marginStart="16dp"
    android:layout_marginTop="20dp" />

<!-- ✅ 正确：引用令牌 -->
<TextView
    android:textAppearance="@style/TextAppearance.Poetry.SectionTitle"
    android:layout_marginStart="@dimen/spacing_page_horizontal"
    android:layout_marginTop="@dimen/spacing_page_top" />
```

### 8.2 Java 代码中使用

```java
// ❌ 禁止：硬编码值
float textSize = 14f;

// ✅ 正确：引用令牌
float textSize = getResources().getDimension(R.dimen.text_size_body);
int spacing = getResources().getDimensionPixelSize(R.dimen.spacing_md);
```

### 8.3 颜色引用规范

```xml
<!-- ❌ 禁止：直接引用原始色 -->
<TextView android:textColor="@color/primary" />
<View android:background="@color/surface" />

<!-- ✅ 正确：引用语义色 -->
<TextView android:textColor="@color/text_title" />
<View android:background="@color/bg_card" />
```

### 8.4 颜色读取兼容性（minSdk 21）

```java
// ❌ 禁止：API 23+
int color = context.getColor(R.color.text_title);

// ✅ 正确：兼容 API 21
int color = ContextCompat.getColor(context, R.color.text_title);
```

---

## 九、无障碍合规

### 9.1 颜色对比度 (WCAG AA)

| 组合 | 亮色模式 | 暗色模式 | 对比度 | 合规 |
|------|---------|---------|--------|------|
| text_title on bg_page | #5D4037 on #FCFAF5 | #D4A745 on #121212 | 7.8:1 / 8.2:1 | ✅ |
| text_body on bg_card | #1C1B1A on #FAF7F0 | #E8E0D5 on #1E1E1E | 15.3:1 / 12.1:1 | ✅ |
| text_body_secondary on bg_card | #4A4540 on #FAF7F0 | #C8C3BE on #1E1E1E | 8.9:1 / 9.2:1 | ✅ |
| text_accent on bg_card | #C62828 on #FAF7F0 | #FF8A80 on #1E1E1E | 5.8:1 / 5.2:1 | ✅ |
| text_caption on bg_page | #7A7570 on #FCFAF5 | #8D8D8D on #121212 | 3.9:1 / 4.1:1 | ⚠️ 仅大文本 |

### 9.2 触摸目标

- 所有可点击元素 ≥ `touch_target_min` (48dp)
- 紧凑型按钮使用 `touch_target_small` (36dp)，但需确保间距充足

### 9.3 字号缩放

- 所有字号使用 sp 单位，支持系统字号缩放
- `text_size_caption` (10sp) 是最小值，不再使用 9sp
- 建议设置页提供 3 档字号缩放：标准 / 大 / 超大

---

## 十、令牌全景总览

```
设计令牌系统 v1.0
├── 间距 (9 级 + 9 语义)
│   ├── 原始: 2/4/8/12/16/20/24/32/48 dp
│   └── 语义: page_h/page_t/page_b/card_p/card_g/card_g_l/icon_text/chip_g/section_g
├── 排版 (14 字号 + 3 行高 + 10 TextAppearance)
│   ├── 字号: 10/11/12/13/14/15/16/20/22/28/36/48 sp + icon_emoji/avatar/game_icon
│   ├── 行高: 4/6/8 dp
│   └── 样式: Display/PageTitle/SectionTitle/CardTitle/Body/BodySecondary/Label/Explanation/StatNumber/DailyLabel
├── 颜色 (71 原始 + 17 语义)
│   ├── 原始: primary/secondary/tertiary/surface/background/error/outline/功能色/朝代色/游戏色/成就色
│   └── 语义: text_title/body/body_secondary/caption/accent/on_primary/gold + bg_page/card/card_elevated + divider_default/decorative + state_success/error/warning/disabled
├── 圆角 (5 级 + 4 ShapeAppearance)
│   └── 0/8/12/16/24 dp → Small/Medium/Large/Pill
├── 阴影 (4 级)
│   └── 0/1/2/4 dp → none/card/card_raised/overlay
├── 描边 (3 级)
│   └── 0/0.5/1 dp → none/thin/normal
├── 组件尺寸 (13 项)
│   └── height_button/button_small/search_bar + height_divider/divider_decorative + size_icon_sm/md/lg + size_avatar + size_progress_sm/md/lg + width_divider_decorative
├── 触摸目标 (2 级)
│   └── 48/36 dp → min/small
└── 动效 (3 级 + 缓动)
    └── 150/300/500 ms → fast/normal/slow + ease_fast_out_slow_in/linear
```

---

**文档版本**: v1.0  
**最后更新**: 2026-07-08  
**维护者**: UI Designer (像素君)  
**下次审查**: 实施完成后 2 周
