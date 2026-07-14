# L2 代码健壮性修复报告

> **日期**：2026-07-03 18:25 GMT+8
> **范围**：v2.0 优化方案 L2 层（T2-1 ~ T2-5）
> **状态**：全部完成，编译通过

---

## 修复清单

### T2-1 · findPoemById O(n) → O(1)

| 项目 | 详情 |
|------|------|
| **文件** | `PoemRepository.java` |
| **改动** | 新增 `HashMap<String, Poem> poemIdIndex` 字段；`loadPoemsAsync()` 完成后构建索引；`findPoemById()` 改为 `poemIdIndex.get(id)` |
| **效果** | 91,154 首诗词查找从最坏 91K 次比较 → 1 次 HashMap 查找 |
| **额外** | 增加 null 参数守卫（原代码 `p.id.equals(id)` 在 id=null 时不会 NPE 但会遍历全量） |

### T2-2 · PoemRepository 线程可见性

| 项目 | 详情 |
|------|------|
| **文件** | `PoemRepository.java` |
| **改动** | `loaded` 和 `indicesBuilt` 均改为 `volatile boolean` |
| **原理** | volatile 写建立 happens-before 屏障：后台线程写 `loaded=true` 前，对 `allPoems`/`poemIdIndex`/`famousPoems` 的赋值对 UI 线程可见 |

### T2-3 · nav_graph.xml 清理废弃参数

| 项目 | 详情 |
|------|------|
| **文件** | `nav_graph.xml` |
| **删除** | `poem_category`、`poem_tag`、`poem_emoji` 三个 `<argument>` 节点 |
| **验证** | grep 全工程无引用；HomeFragment/FavoritesFragment 的 navigate 调用均未传递这三个参数 |

### T2-4 · ThemeManager 死字段清理

| 项目 | 详情 |
|------|------|
| **文件** | `ThemeManager.java` |
| **删除** | `ThemeDef.requireAllPoems` 字段声明 + 构造函数初始化 |
| **验证** | grep 全工程仅 ThemeManager.java 自身引用（声明+初始化），无外部使用 |

### T2-5 · PoemLoader Javadoc 修正

| 项目 | 详情 |
|------|------|
| **文件** | `PoemLoader.java` |
| **改动** | 类级 Javadoc + `readJsonArray()` 方法级 Javadoc，将"流式解析"修正为"全量读取后通过 JSONTokener 解析" |
| **原因** | 实际实现是 `BufferedReader` 按行读取拼接为完整 String，再交给 `JSONTokener` 解析，并非流式 |

---

## 编译验证

| 命令 | 结果 | 耗时 |
|------|------|------|
| `./gradlew assembleDebug` | BUILD SUCCESSFUL | 42s |
| `./gradlew lintDebug` | 3 errors（预存）+ 133 warnings（预存） | 1m38s |

**Lint 3 个 error 均为预存问题，与本次修改无关**：
1. `item_poem_card.xml:21` MissingConstraints — 未改动此文件
2. `values-night/styles.xml:8` NewApi windowLightStatusBar — 未改动此文件
3. `values/styles.xml:29` NewApi windowLightStatusBar — 未改动此文件

---

## 修改文件清单

| 文件 | 操作 | 行数变化 |
|------|------|----------|
| `PoemRepository.java` | 修改（T2-1 + T2-2） | +8 行（索引字段+构建+null守卫），-4 行（旧遍历），volatile 关键字 ×2 |
| `nav_graph.xml` | 修改（T2-3） | -9 行（3 个 argument 节点） |
| `ThemeManager.java` | 修改（T2-4） | -3 行（字段声明+初始化） |
| `PoemLoader.java` | 修改（T2-5） | 约 ±5 行（Javadoc 文案修正） |

---

**下一步**：L3 设计系统基建（dimens.xml 令牌 + styles.xml 组件样式 + 字号缩放）
