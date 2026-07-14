# 诗词乐园 UI 焕颜任务清单 v1.0

> **生成时间**：2026-07-03 17:23 GMT+8
> **基于**：UI 设计系统 v1.0 + 现有代码现状
> **原则**：复用已有资源 · 优先补缺口 · 最小变更出最大效果

## 现状速览（先读一遍，避免重复造轮子）

| 已有 | 路径 | 状态 |
|------|------|------|
| 调色板（11 色 + 10 朝代色） | `res/values/colors.xml` | ✅ 完成 |
| 古风 strings（126 行） | `res/values/strings.xml` | ✅ 已铺底 |
| 深色模式 | `res/values-night/colors.xml` | ✅ 完成 |
| 撒花动画 | `ui/widget/ConfettiView.java` | ✅ 完成 |
| 统计图表 | `ui/widget/StatsBarChart.java` | ✅ 完成 |
| 基础动效 | `res/anim/` (spring_scale/slide/heart_beat) | ✅ 7 个动画 |
| 基础图形 | `res/drawable/` (bg_card/bg_button/ic_*) | ✅ 30+ 资源 |
| 拼音逐字视图 | `util/PinyinLineView.java` | ✅ 完成 |
| 详情 Canvas 卡片 | `ui/detail/DetailFragment.java` | ✅ 已有古风卡 |

> **结论**：本轮任务以"补缺口 + 强化品牌一致性 + 包容性完善"为主，不再重建已有能力。

---

## 任务总览（按 ROI 排序）

| 优先级 | 任务组 | 工时 | 风险 | 状态 |
|--------|--------|------|------|------|
| 🟥 P0-A | 修复紧急 Bug | 1d | 低 | 必修 |
| 🟧 P1-B | 包容性基础（a11y） | 2d | 低 | 必修 |
| 🟨 P1-C | 设计令牌补全 | 1.5d | 低 | 必修 |
| 🟩 P2-D | 核心页面视觉焕颜 | 4d | 中 | 推荐 |
| 🟦 P2-E | 微互动强化 | 3d | 中 | 推荐 |
| 🟪 P3-F | 主题与彩蛋 | 3d | 中 | 选做 |
| ⬜ P3-G | 文档与验收 | 1d | — | 必做 |

**总计**：主路径 ~12 工作日，落地可分 3 个 Sprint。

---

## 🟥 P0-A · 修复紧急 Bug（1 工作日）

> 阻断发布 / 潜在崩溃 / 用户感知

### A1. PinyinLineView 长句滚动卡顿

| 字段 | 内容 |
|------|------|
| 文件 | `app/src/main/java/com/poetry/util/PinyinLineView.java` |
| 现象 | 7 字以上诗句首次测量耗时 200ms+ |
| 根因 | `onMeasure` 每次重新计算 `maxCharsPerRow`，未缓存 |
| 任务 | 1. 加 `mLastWidth` 缓存；2. 仅当宽度变化时重算；3. 单测验证 7/14/28 字诗句 |
| 验收 | 启动 → 详情页 < 50ms 渲染完首屏 |
| 工时 | 0.5d |
| 风险 | 低（纯局部缓存） |

### A2. DetailFragment 分享图片偶发 OOM

| 字段 | 内容 |
|------|------|
| 文件 | `app/src/main/java/com/poetry/ui/detail/DetailFragment.java` |
| 现象 | 长文（>200 字）分享时 OOM 概率 ~3% |
| 任务 | 1. Bitmap 复用 `Bitmap.Config.RGB_565`；2. 缩放至 1080×1920；3. 加 try-catch + 友好提示 |
| 验收 | 反复分享 100 次无 OOM |
| 工时 | 0.5d |
| 风险 | 低 |

---

## 🟧 P1-B · 包容性基础（2 工作日）

> 视障用户可用 · 老年用户友好 · 无障碍合规

### B1. 字号缩放（3 档：标准/大/特大）

| 字段 | 内容 |
|------|------|
| 文件 | `ui/profile/ProfileFragment.java`、`SettingsActivity.java`（如无则新建） |
| 任务 | 1. 加 `SeekBar` 三档；2. `Configuration.fontScale` 拦截；3. 重启生效；4. strings 加提示 |
| 验收 | 切换档位 → 全文（标题/正文/列表项）按比例放大 |
| 工时 | 1d |
| 风险 | 中（涉及全局 Configuration） |

### B2. 减少动画开关

| 字段 | 内容 |
|------|------|
| 文件 | SharedPreferences key `pref_reduce_motion`（在 `ProfileFragment` 已有） |
| 任务 | 1. `SwitchMaterial` 控件；2. 读取后传入 `ObjectAnimator.setDuration(0)`；3. 关闭 `heart_beat`、`spring_scale`；4. `ConfettiView` 直接隐藏 |
| 验收 | 开启后所有 Spring/粒子/缩放动画 < 50ms 瞬切 |
| 工时 | 0.5d |
| 风险 | 低 |

### B3. 关键组件 contentDescription 补全

| 字段 | 内容 |
|------|------|
| 文件 | 全 Fragment + 关键 ImageView |
| 任务 | 1. 收藏按钮："收藏/取消收藏，{title}"；2. 朝代 tag："{dynasty}朝"；3. 诗词卡片："{title}，{author}，{dynasty}朝"；4. 朗读按钮："朗读全文" |
| 验收 | TalkBack 朗读无生硬"按钮"字样 |
| 工时 | 0.5d |
| 风险 | 低 |

### B4. 焦点顺序与可点击区域

| 字段 | 内容 |
|------|------|
| 文件 | 各布局 |
| 任务 | 1. 触控目标 ≥ 48dp；2. `nextFocusForward` 显式声明；3. 关键按钮加 `stateListAnimator` |
| 验收 | D-pad / TalkBack 焦点路径符合阅读顺序 |
| 工时 | 0.5d（嵌入式） |
| 风险 | 低 |

---

## 🟨 P1-C · 设计令牌补全（1.5 工作日）

> 建立可维护的设计系统基础

### C1. dimens.xml 间距令牌化

| 字段 | 内容 |
|------|------|
| 文件 | 新建 `res/values/dimens.xml` |
| 任务 | 1. spacing 4/8/12/16/20/24/32/48dp；2. text size 12/14/16/18/22/28sp；3. radius 4/8/12/16dp；4. elevation 1/2/4/8dp |
| 验收 | 所有 hard-coded 间距 ≤ 5% 残存 |
| 工时 | 0.5d |
| 风险 | 低（纯增资源） |

### C2. 主题与样式统一

| 字段 | 内容 |
|------|------|
| 文件 | `res/values/styles.xml`、`res/values-night/styles.xml` |
| 任务 | 1. `Theme.App` 统一文字/按钮基样式；2. `Widget.PoemCard` / `Widget.DynastyTag` / `Widget.StampButton` 组件样式；3. 替换所有 layout 内联的 `android:textColor` |
| 验收 | 改主色一处全 app 同步 |
| 工时 | 1d |
| 风险 | 中（需回归所有页面） |

---

## 🟩 P2-D · 核心页面视觉焕颜（4 工作日）

> 截图级提升 · 品牌强化

### D1. 首页「今日诗笺」Hero 卡片

| 字段 | 内容 |
|------|------|
| 文件 | `ui/home/HomeFragment.java` + `res/layout/fragment_home.xml` |
| 设计 | 见 `ui-design-system-v1.0.md` § 4.1 |
| 任务 | 1. 顶部加 240dp 高度的「今日诗词」Hero；2. 渐变背景 + 朱红印章角标；3. 点击 → 详情页；4. 长按 → 收藏 |
| 验收 | 首屏 50% 用户注意力落在 Hero |
| 工时 | 1d |
| 风险 | 中（需新增 layout + 动画） |

### D2. 详情页长按扇形菜单

| 字段 | 内容 |
|------|------|
| 文件 | `ui/detail/DetailFragment.java` |
| 设计 | 详见设计规范 § 4.2 |
| 任务 | 1. `GestureDetector` 拦截长按；2. 360° 圆周分布 3-4 项；3. 弹性入场（spring_scale + delay 30ms × index） |
| 验收 | 长按 500ms 触发，菜单弹出 200ms，触感反馈 CONFIRM |
| 工时 | 1.5d |
| 风险 | 中 |

### D3. 朝代色 → 朝代色 + 印章

| 字段 | 内容 |
|------|------|
| 文件 | `ui/adapter/PoemAdapter.java` + `res/drawable/bg_dynasty_tag_*.xml`（10 个） |
| 任务 | 1. 圆形 8dp 印章 + 朝代单字（"唐""宋""元"等）；2. 复用现有 `tag_tang` 等颜色；3. 减小色块、提高识别度 |
| 验收 | 色盲模拟下仍可分辨朝代 |
| 工时 | 0.5d |
| 风险 | 低 |

### D4. 状态栏 / 导航栏沉浸式

| 字段 | 内容 |
|------|------|
| 文件 | `MainActivity.java` |
| 任务 | 1. `WindowCompat.setDecorFitsSystemWindows(false)`；2. 状态栏透明 + 文字自适应；3. 适配 Android 15 edge-to-edge |
| 验收 | 截图无白边 |
| 工时 | 0.5d |
| 风险 | 低（需 minSdk ≥ 21 已满足） |

### D5. 加载 / 空 / 错误三态统一

| 字段 | 内容 |
|------|------|
| 文件 | `res/layout/include_state_*.xml`（3 个新文件） |
| 任务 | 1. 全屏宣纸背景 + 居中插画/文字；2. 加载："墨香氤氲..." + 旋转的毛笔 icon；3. 空："今日诗笺尚未揭开" + 按钮"再探一首"；4. 错误："墨线断了" + 按钮"重新铺纸" |
| 验收 | 9 个 Fragment 全部接入（可分批） |
| 工时 | 0.5d（基建）+ 0.5d（接入） |
| 风险 | 低 |

---

## 🟦 P2-E · 微互动强化（3 工作日）

> 让修复"看起来有温度"

### E1. 收藏粒子（❤️ 飞心）

| 字段 | 内容 |
|------|------|
| 文件 | `ui/widget/ConfettiView.java`（已存在） + 收藏按钮集成 |
| 任务 | 1. 复刻现有 `ConfettiView` API；2. 收藏点击 → 触发 6 颗 ❤️ 抛物线；3. 200ms 完成；4. 失败回滚 |
| 验收 | 收藏动画在首页 / 详情 / 收藏页均生效 |
| 工时 | 0.5d |
| 风险 | 低（组件已存在） |

### E2. 答题印章动画

| 字段 | 内容 |
|------|------|
| 文件 | `ui/quiz/QuizFragment.java` + 新建 `ui/widget/StampView.java` |
| 任务 | 1. 答对：选项上盖朱红"妙"印章（Canvas 绘制）；2. 答错：墨色晕染（alpha 0→0.5 200ms）；3. 触感反馈 CONFIRM/REJECT |
| 验收 | 答对全屏有 0.5s 庆祝感 |
| 工时 | 1d |
| 风险 | 中 |

### E3. 落墨涟漪（列表项点击）

| 字段 | 内容 |
|------|------|
| 文件 | `res/drawable/bg_ripple_ink.xml`（新建） |
| 任务 | 1. 自定义 RippleDrawable：圆形扩散 + 茶褐 + alpha 0.3→0；2. 替换 `bg_card.xml` 的 `?attr/selectableItemBackground` |
| 验收 | 列表项点击有墨滴感 |
| 工时 | 0.5d |
| 风险 | 低 |

### E4. 拼音切换提笔动画

| 字段 | 内容 |
|------|------|
| 文件 | `ui/detail/DetailFragment.java` + `util/PinyinLineView.java` |
| 任务 | 1. 切换按钮 → `TransitionManager.beginDelayedTransition`；2. 拼音行从底部 8dp 滑入 + alpha；3. 250ms ease-out |
| 验收 | 切换瞬间有"翻牌"感 |
| 工时 | 0.5d |
| 风险 | 低 |

### E5. 完成学习卷轴庆祝

| 字段 | 内容 |
|------|------|
| 文件 | `ui/learning/LearningFragment.java` |
| 任务 | 1. 全屏覆盖层；2. 两侧宣纸从屏幕外向内收拢（300ms）；3. 中间显示"今日已学 X 首" + 古风字体；4. 1000ms 后两侧向外展开退出 |
| 验收 | 每日学习任务完成时触发一次 |
| 工时 | 0.5d |
| 风险 | 低 |

---

## 🟪 P3-F · 主题与彩蛋（3 工作日 · 选做）

### F1. 季节主题框架

| 字段 | 内容 |
|------|------|
| 文件 | `domain/ThemeManager.java`（已有，加方法） |
| 任务 | 1. 枚举 Season {SPRING/SUMMER/AUTUMN/WINTER}；2. `Calendar` 判断当季；3. 4 套 colors.xml 变体（values-spring 等） |
| 验收 | 切换系统时间可见主题切换 |
| 工时 | 1d |
| 风险 | 中 |

### F2. 节日 Banner

| 字段 | 内容 |
|------|------|
| 文件 | `ui/home/HomeFragment.java` |
| 任务 | 1. 春节/端午/中秋/重阳 4 个 banner；2. `Calendar` 自动判断；3. 节日相关诗词置顶 |
| 验收 | 春节时 banner 显示"新年诗会" |
| 工时 | 1d |
| 风险 | 中 |

### F3. 彩蛋 · 诗仙称号

| 字段 | 内容 |
|------|------|
| 文件 | `ui/quiz/QuizViewModel.java` |
| 任务 | 1. 检测连击 5 连；2. 触发 `ConfettiView` + 弹窗"诗仙"；3. 头像加金边（已有 `achievement_unlocked` 颜色） |
| 验收 | 答对 5 题连击触发 |
| 工时 | 0.5d |
| 风险 | 低 |

### F4. 彩蛋 · 长按 Logo 召唤彩蛋诗句

| 字段 | 内容 |
|------|------|
| 文件 | `MainActivity.java` |
| 任务 | 1. 长按 logo 3s 检测；2. 随机从 `PoemRepository` 选 1 首；3. 弹"彩蛋"小卡片 |
| 验收 | 长按触发彩蛋率 100% |
| 工时 | 0.5d |
| 风险 | 低 |

---

## ⬜ P3-G · 文档与验收（1 工作日 · 必做）

### G1. 走查截图集

| 字段 | 内容 |
|------|------|
| 任务 | 1. 8 个核心页 × 浅/深 × 3 档字号 = 48 张截图；2. 归档到 `artifacts/screenshots/`；3. 标注问题清单 |
| 工时 | 0.5d |

### G2. 设计-实现对照表

| 字段 | 内容 |
|------|------|
| 任务 | 1. 对照 `ui-design-system-v1.0.md` 各项；2. 标记已实现/未实现/有偏差；3. 偏差 < 10% 算合格 |
| 工时 | 0.5d |

---

## 🎯 执行建议（3 个 Sprint）

### Sprint 1：止血 + 基础（5 工作日）
- P0-A：1d
- P1-B：2d
- P1-C：1.5d
- G1：0.5d（边走边收截图）
- **交付**：可发布的稳定版 + a11y 达标 + 设计令牌就绪

### Sprint 2：焕颜 + 微互动（5 工作日）
- P2-D：4d
- P2-E（核心 3 项）：1d
- G2：0.5d
- **交付**：截图级提升的视觉体验

### Sprint 3：主题 + 彩蛋 + 验收（3 工作日）
- P2-E 剩余：1.5d
- P3-F（按优先级选 2-3 项）：1.5d
- **交付**：差异化品牌资产

---

## 📊 验收指标

| 维度 | 目标 | 测量方式 |
|------|------|----------|
| 启动 → 首诗词 | < 500ms | logcat + Perfetto |
| 详情页打开 | < 100ms | logcat |
| 内存峰值（冷启动） | < 120MB | Android Profiler |
| a11y 焦点路径 | 9/9 页面通过 | TalkBack 手动 |
| 字号 3 档 | 文字实际比例 100/125/150% | 截图对照 |
| 深色模式 | 无白底残留 | 9/9 页面截图 |
| TalkBack 朗读 | 无生硬"按钮" | 录音回放 |
| 触感反馈 | 关键场景 100% 触发 | 设置检查 |

---

## 🛠️ 工具与脚手架

| 工具 | 用途 |
|------|------|
| Android Studio Layout Inspector | 检查约束冲突 |
| Layout Validation | lint 校验 |
| Accessibility Scanner | a11y 自动检查 |
| StrictMode | 主线程 IO 检测 |
| Macrobenchmark | 启动性能基线 |
| Paparazzi | 截图测试（可选） |

---

## ❌ 不在范围（避免范围蔓延）

- 重写为 Kotlin / Compose（用户明确要求保持 Java + View System）
- 新增诗词数据（91K 已是上限）
- 多语言扩展（i18n 框架先就位，文案留 strings key）
- 后端 / 账号系统（本地优先）
- 性能 Pro 大型框架（Choreographer 已够）

---

**任务清单版本**：v1.0
**关联文档**：`ui-design-system-v1.0.md`、`code-analysis-2026-07-03.md`
**下一步建议**：从 Sprint 1 开始执行，先跑 P0-A 两个 Bug，再开 a11y 工单。
