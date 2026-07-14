# 朝代分类代码更新 — 明代 + 近现代

## 背景

诗词库此前新增了 `poems_明代.json`（2首）和 `poems_近现代.json`（7首），但分类代码未同步更新：
- PoemLoader 的 tag 映射中没有明代/近现代 case，走 default 被误标为 "tang"
- PoemRepository 的 navOrder 没有这两个朝代，分类筛选列表不显示
- DetailFragment 的颜色映射也没有对应颜色
- 默认 fallback 一律归为唐代，不符合实际朝代

## 修改清单（6 个文件）

### 1. PoemLoader.java — `parsePoem()` tag 映射

| 朝代 | 旧 tag | 新 tag |
|------|--------|--------|
| 唐代 | (default) tang | tang（显式 case） |
| 宋代 | song | song |
| 先秦/春秋/春秋战国 | qin | qin |
| 魏晋 | wei | wei |
| 五代 | wu | wu |
| 元代 | yuan | yuan |
| **明代** | ~~tang (default)~~ | **ming** |
| 清代 | qing | qing |
| **近现代** | ~~tang (default)~~ | **modern** |
| **其他** | ~~tang~~ | **other**（中性标签，不再误归唐代） |

### 2. PoemRepository.java — `buildCategories()`

navOrder 数组增加 "明代"、"近现代"，分类筛选 Tab 将显示这两个朝代。

### 3. PoemRepository.java — `getDynastyIcon()`

| 朝代 | emoji | 寓意 |
|------|-------|------|
| 明代 | 🎭 | 戏曲面具 — 明代戏曲繁荣（牡丹亭等） |
| 近现代 | 🌅 | 日出 — 新时代 |

### 4. DetailFragment.java — `getTagColorRes()`

- 新增 `case "tang"` 显式映射
- 新增 `case "ming": R.color.tag_ming`
- 新增 `case "modern": R.color.tag_modern`
- `default: R.color.tag_tang` → `default: R.color.tag_other`（中性灰）
- null 检查也改为 `R.color.tag_other`

### 5. colors.xml（values + values-night）

| 颜色 | Light | Night | 朝代 |
|------|-------|-------|------|
| tag_ming | #AD1457 | #F48FB1 | 明代（玫红） |
| tag_modern | #37474F | #90A4AE | 近现代（蓝灰） |
| tag_other | #546E7A | #78909C | 默认（中性蓝灰） |

### 6. Poem.java — Javadoc

朝代列表和标签列表补充明代/近现代/other。

## 编译验证

```
./gradlew compileDebugJavaWithJavac
BUILD SUCCESSFUL
```

## nav.json 朝代全览

```
先秦       372首
春秋        20首
春秋战国      14首
魏晋        45首
唐代     57623首
五代       543首
宋代     21129首
元代     10911首
清代       488首
明代         2首  ← 新增
近现代        7首  ← 新增
总计    91,154首
```
