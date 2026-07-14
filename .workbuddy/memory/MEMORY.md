# 诗词乐园 Android 项目 — 长期记忆

## 项目标识
- **名称**: 诗词乐园（Chinese Poetry）
- **包名**: `com.poetry`
- **语言**: Java（用户要求不迁移 Kotlin/Compose）
- **构建**: AGP 8.2.2 + Gradle 8.7 + JDK 17 + Material Design 3
- **最低 API**: 21（Android 5.0）
- **目标 API**: 34（Android 14）

## 架构约定
- 架构模式: MVVM（ViewModel + LiveData + Room + Repository）
- 导航: Navigation Component（8 Fragment + Safe Args）
- 布局: 全量 ConstraintLayout（不嵌套 LinearLayout）
- 依赖注入: 无 DI 框架（手动构造，用户未要求引入）
- 数据存储: Room（learning_records / daily_stats）+ assets JSON（诗词库）
- 颜色: Material 3 中国风调色板（墨色茶褐 + 宣纸 + 印章朱红）

## 已知数据源
- `assets/web/data/`: 91,154 首诗词（先秦~近现代 11 朝代，JSON 分片，2026-07-02清洗后）
- `assets/poem_explanations.json`: 404 首著名诗词释义（key=`标题|作者`），覆盖 313 首核心著名诗词清单（100%）
- `assets/web/data/nav.json`: 朝代-文件索引（计数与实际文件一致）
- PoemLoader 使用 `JSONTokener(InputStreamReader)` 流式解析，避免全量 String
- 新增朝代文件: `poems_明代.json`、`poems_近现代.json`
- 诗词入库工具: `tools/build_batchN.py` → `tools/merge_poems.py` → `tools/add_explanations_batchN.py` → `tools/check_missing.py`
- 数据校验工具: `tools/validate_db.py`(字段完整性) + `tools/deep_validate.py`(朝代/重复/释义关联/特殊字符)
- 朝代→tag 映射: 唐代→tang, 宋代→song, 先秦/春秋/春秋战国→qin, 魏晋→wei, 五代→wu, 元代→yuan, 明代→ming, 清代→qing, 近现代→modern, 默认→other
- tag 颜色: tag_tang/song/qin/wei/yuan/ming/qing/modern/wu/other (values + values-night)

## 关键组件
- `PinyinLineView`: 自定义 View，逐字拼音标注（weight 等分 + 自动折行）
- `TtsManager`: Android TTS 封装，带 OnInitListener 回调
- `GameEngine`: 消消乐 MatchCard 引擎（全可见配对，非记忆翻牌）
- `PoemRepository`: 单例数据源，按释义排序（著名诗词前置）；findPoemById 使用 HashMap O(1) 查找；loaded/indicesBuilt 为 volatile
- `DetailFragment`: Canvas 绘制 750px 古风卡片 → FileProvider 分享 PNG

## 用户偏好
- 不使用 Kotlin、Compose、Hilt 等 Kotlin-first 生态
- 保持纯 Java + XML View System
- 注释以中文为主
- 诗词详情拼音模式：每个汉字正上方显示拼音，标点上方留空占位
- 消消乐：全可见 3×4 网格，配对消除（非记忆翻牌）

## 常见问题及修复方式
1. **LiveData 不通知**: 使用 `setValue(new ArrayList<>(current))` 创建新引用
2. **释义不显示**: 检查 `explanationKey()` 格式是否为 `标题|作者`，与 JSON key 完全匹配
3. **拼音显示不全**: `PinyinLineView` 使用 layout_weight 等分，非固定 dp
4. **长诗句显示错乱**: 动态计算 `maxCharsPerRow = (屏幕宽-48dp) / MIN_CELL_DP`
5. **Detail 布局重叠**: 确保所有 View 有完整 constraintTop/Start/End 约束链
6. **搜索卡顿**: 搜索必须在后台线程执行，主线程 `repo.search()` 对 91K 诗词做 contains 会阻塞 UI；用 `volatile searchCancelled` 取消旧搜索，`DiffUtil` 替代 `notifyDataSetChanged`
3. **拼音显示不全**: `PinyinLineView` 使用 layout_weight 等分，非固定 dp
4. **长诗句显示错乱**: 动态计算 `maxCharsPerRow = (屏幕宽-48dp) / MIN_CELL_DP`
5. **Detail 布局重叠**: 确保所有 View 有完整 constraintTop/Start/End 约束链

## 设计令牌系统 v1.0（2026-07-03 落地）
- **核心文件**: `values/dimens.xml`（唯一定义点）+ `values/colors.xml`（17语义色）+ `values/styles.xml`（组件样式）
- **三层架构**: L1原始令牌(dimen) → L2组件令牌(Widget.Poetry.*) → L3语义令牌(spacing_*)
- **间距**: 9级(2/4/8/12/16/20/24/32/48dp) + 9语义别名
- **字号**: 14级(10~48sp) + 3级行高 + 10个TextAppearance样式
- **圆角**: 5级(0/8/12/16/24dp) + 4个ShapeAppearance
- **约定**: 所有布局文件禁止硬编码 textSize/padding/margin/color，必须引用 @dimen/@color 令牌
- **values-night**: 语义色引用的原始色已有night覆盖，无需额外定义语义色night版
- **windowLightStatusBar**: 需API 23，用 `tools:targetApi="23"` 声明（values + values-night 两处）

## 当前版本
- versionCode: 2
- versionName: "2.0"
- 完成度: 51/59 = 86%

## 2026-07-01 五大阻塞修复
解决代码审查中发现的 5 个 P0 问题（详见 .workbuddy/artifacts/blocker-fixes-2026-07-01.md）：
- **B1 ViewModel 作用域**: MatchGame/Couplet 的 `requireActivity()` → `this`，各自独立
- **B2 积分竞态**: PoemDao 新增 `addTotalPoints(int)` 原子 SQL UPDATE，消除读-改-写窗口
- **B3 搜索性能**: 字符级倒排索引（titleCharIndex/authorCharIndex）+ Poem.fullTextCached 缓存
- **B4 成就引擎**: GameViewModel/QuizViewModel/LearningViewModel 接入 `checkAndUnlock()`，新增 `newAchievement` LiveData
- **B5 主题管理**: 积分变更后 `syncUnlockedThemes()` + ProfileFragment 主题展示卡片

## 2026-07-03 P0-A 紧急 Bug 修复
- **A1 PinyinHelper 字符级 LRU 缓存**（CACHE_CAPACITY=4096, accessOrder=true），性能 250x+
- **A2 PinyinLineView 延迟构建**：加 mBuilt 标志避免多次 measure 重建 view tree
- **A3 DetailFragment 分享卡优化**：
  - 后台线程改用 ApplicationContext（避免 Fragment 引用泄漏）
  - Bitmap ARGB_8888 → RGB_565（省 50% 内存）
  - 捕获 OutOfMemoryError + 古风 Toast「墨未干，稍后再试」
  - 颜色改用 values/colors.xml 资源引用
- **测试方式**：javac + java 纯 SE 跑（无需 Android），self-test 7/7 通过
- **编译验证**：`./gradlew assembleDebug` BUILD SUCCESSFUL in 1m25s（2026-07-03 17:48，含 Lint）
- **产出 APK**：`app/build/outputs/apk/debug/app-debug.apk` 21.3 MB
- **兼容性 hotfix**：P0-A 引入的 `Context.getColor(int)` 改用 `ContextCompat.getColor()`，16 处替换，4 个文件加 import。**约定**：项目 minSdk=21，凡色值读取必须用 ContextCompat。其他文件（LearningFragment/MatchCardAdapter/ProfileFragment/CoupletGameFragment/StatsBarChart）已遵循此约定。
