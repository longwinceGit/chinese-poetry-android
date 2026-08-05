# 诗词乐园 · 游戏模块重设计方案 (GAME_REDESIGN)

> 版本：v1.0（初稿）｜日期：2026-08-05
> 范围：`com.poetry` 下游戏相关模块的重设计，纯 Java 8+、Android View System、完全离线。
> 本文档为**活文档**，每次修订须更新版本号与变更日志。

---

## 0. 变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-08-05 | 初稿：基于对现有代码的逐文件核对，产出游戏体系重设计 |

---

## 1. 设计哲学（为什么这样设计对目标用户有激励）

### 1.1 目标用户与他们的动机密码

本 App 目标用户是**中小学生（主力）+ 文学爱好者（次力）+ 家长 / 教师（引导者）**。对照儿童动机研究框架，孩子愿意反复玩一个学习类游戏，靠的是四件事：

| 动机原力 | 对应当前需求 | 本方案的对策 |
|---------|-------------|-------------|
| **胜任感（Mastery）**：我能学会、越学越好 | 现有游戏没有"难度梯度"，要么一次性随机，要么固定 5 题 | 每个游戏引入**难度分级 + 成就里程碑**，让"变强"可被看见 |
| **自主感（Autonomy）**：由我选择 | 三个游戏卡片平铺，没有"选什么"的动机 | 引入**每日主题挑战 + 不同玩法轮换**，让孩子自己决定今晚玩哪种 |
| **联结感（Relatedness）**：我跟同学/家人比 | 完全单人、无任何分享/回放 | 引入**成绩留痕（最高分榜）**，为未来"好友 PK"埋点，且不依赖后端 |
| **即时反馈（Immediacy）**：点一下就有反应 | 得分是"静悄悄的数字"，连击只在内心 | 引入**章节式即时反馈 + 得分动画 + 花式庆祝**

### 1.2 三条设计支柱（Design Pillars）

所有后续决策以这三条不可妥协的体验为准绳：

1. **零挫败的挑战感**：孩子第一个回合的每一次尝试都必须"有点收获"——答错要温柔、答对要响亮。**不允许出现"扣分到负"或"一题答错就全盘皆输"的设计。**
2. **把"背诵"做成"游戏"而非"测验"**：出题必须以**真实诗词**为数据源，难的题也要让孩子**通过已知的上文或联想**能答对，而不是逼他死记。
3. **每一次投入都沉淀到成长体系**：玩一局游戏必须（a）给积分、（b）给成就计数、（c）贡献每日游戏任务、（d）可能解锁主题。**游戏不能是"学了没用"的孤岛。**

### 1.3 一个可验证的总体目标

> **"玩 5 分钟后想再来一局"的可复玩性** 的可量化代理指标：
> - **单次会话游戏时长 ≥ 3 分钟**（当前约 40 秒/局，见 §3.2）
> - **同一会话内二次进入游戏的比例 ≥ 40%**
> - **游戏造成的"重背率"提升**：进入游戏前先看题目的诗词，游戏结束后 7 天内被再次标记"已学"的比例 ≥ 现有基线 +5pt

（这些是目标；具体衡量埋点方案见 §10。）

---

## 2. 现状诊断（必须来自代码事实）

我逐文件读了 `GameEngine.java`、`QuizGenerator.java`、`GameViewModel.java`、`QuizViewModel.java`、`GameHubFragment.java`、`CoupletGameFragment.java`、`MatchGameFragment.java`、`QuizFragment.java`、`AchievementEngine.java`、`LearningEngine.java`、`LearningViewModel.java`、`nav_graph.xml`、`fragment_game_hub.xml`，以及数据层 `PoemRepository` / `Poem` / `LearningRecord` / `UserProfile` / `DailyStats`。以下是*由代码事实支撑*的缺陷诊断。

### 2.1 接龙（Couplet）—— 结构性缺陷

**代码事实 1：出题固定 10 轮，但只允许内部相邻对。**
`generateCoupletGame(pool, 5)` 在 `GameViewModel.startCoupletGame()` 中被调用时传的却是 `TOTAL_ROUNDS = 10`（`GameViewModel.java:60,81`）。每轮随机取 `poem.lines[i]` 与 `poem.lines[i+1]`。这导致：
- 孩子要连续 10 对才结算，对低龄用户过长、易疲劳；
- 干扰项（`generateCoupletGame` 第 90 行）是从**全池随机**取的 `other.lines[idx]`，**没有按"长度/节律"过滤**——干扰项常与上句字数相差悬殊，孩子靠"字数对不齐"就能秒排除，**毫无考验价值**。

**代码事实 2：得分公式对"答对"没有区分度。**
`calcCoupletScore(round, correct, streakBonus) = (correct ? 10 : 0) + (correct ? streakBonus * 2 : 0)`（`GameEngine.java:249-253`）。由此：
- **满分只要答对 5 对即可**（10+2+4+6+8+10=40 vs 对 10 对满分 40+...），区分度极低；
- 答错不扣分 → 孩子可以"无脑点"，**没有策略张力**；
- 连击上限没有封顶，理论上可无限滚分。

**代码事实 3：答案反馈是"整句对/错"二值。** `answerCouplet` 只判断 `cr.options.get(idx).equals(cr.correctAnswer)`（`GameViewModel.java:111`），错误时只显示正确下句，**没有"提示性纠错"**。对"半对"（孩子记得后半句但不记得下一句）零奖励。

### 2.2 消消乐（Match）—— 作为"学习"几乎无用

**代码事实 4：卡片永远是同一首诗的 `lines[0]` + `lines[1]`。**
`generateMatchGame(pool, 6)` 只取 `poem.lines[0]`（上句）与 `poem.lines[1]`（下句）（`GameEngine.java:169-191`）。也就是说它**只能考"每首诗的第一联"**。91,000 首里 99% 的联句（`lines[2..]`、绝句中后一联等）对孩子来说**永远不会被考到**，且大量名句的配对根本不在题面。

**代码事实 5：全可见 + 无干扰、无失败压力 → 退化。**
12 张卡全正面可见，每对只要"扫一遍就能配"，得分 `max(5, 50-(attempts-6)*3)`（`GameEngine.java:266-271`）只奖励"少点错"，**没有计时、没有回合上限、没有正反馈层级**。6 对往往 40 秒内结束，无复玩动力。

**代码事实 6：消消乐对"已消除"无任何学习沉淀。** 配对成功后只是 `matchTip` 闪现"标题 · 作者"（`GameViewModel.java:207`），**没有"这首诗你配对过"的记忆，也没有把该诗推进"已学"关联**。

### 2.3 填空（Quiz）—— 干扰项质量 & 难度不可控

**代码事实 7：干扰项是 40 个固定汉字的随机剔除。**
`QuizGenerator.java:108-111` 使用硬编码 `distractors = {"天","人","山","水",...}` 40 字池补足候选。它**与诗句原文、同音字、相近字无关**，且这些常见字本身就可能出现在正确答案之外，导致**干扰项与答案"傻傻分不清"或用意不明**。

**代码事实 8：挖空"完全随机"，无难度区分，也无"易错字优先"。**
`generateFillBlank` 遍历所有 `line.length()>=4` 的中段（`QuizGenerator.java:73-81`）`Collections.shuffle` 后取前 `1..3` 个，**完全没有"优先挖记忆难点（如生僻字、易混字、押韵字）"的策略**，也没有按难度（字词 vs 整句、五言 vs 七言）做分级。

**代码事实 9：填空题的"正确判定"是整字命中，且 `correctNextLine` 语义混乱。**
`submitAnswer` 逐个 `blank.answer.equals(...)`（`QuizViewModel.java:115`），正确率按"题"而非"空"结算，`calcPointsForQuiz` 又按整题比例打分（`LearningEngine.java:104-111`），**颗粒度粗**。此外 `QuizGenerator.generateCouplet` 与 `GameEngine.generateCoupletGame` 存在**两套并行出题实现**（`QuizGenerator.java:142` vs `GameEngine.java:61`），是历史冗余——应统一。

### 2.4 大厅（GameHub）—— 是"陈列柜"而非"动机场"

**代码事实 10：`GameHubFragment` 只做 `navigate` 三个硬编码卡片**（`GameHubFragment.java:34-40`），无主题、无入口状态（今天这关玩过没）、无"今日挑战"、无最高分展示、无成就进度。孩子**没有任何"今天先玩哪个"的理由或"上次玩得怎样"的牵引**。

### 2.5 成长联动 —— 缺"游戏专属"环

**代码事实 11：** `GameViewModel.savePoints()` / `recordGameActivity()` 与 `QuizViewModel` 各自实现了一套积分/成就/每日统计逻辑（**重复代码**）。且**没有任何按"游戏类型"（对诗/消消乐/填空）存历史或最高分的持久化**——`LearningRecord.gamePlayed` 只是"某首诗被玩过 N 次"的计数，`DailyStats.gamesPlayed` 只是每日总次数。**"你消消乐最高 460 分"这种信息全无。**

### 2.6 干净的、可复用的资产（结论：基础不差）

- `PoemRepository.getAllPoems()` / `getPoemsByCategory()` / `getDailyPoem()` / `getRandomPoem()` / `findPoemById()` 齐备（`PoemRepository.java:150,167,281,298,266`）。
- 成就体系 `AchievementEngine`（12 项）+ 主题 `ThemeManager`（9 套）+ 等级 `LearningEngine`（9 级）机制成熟、可扩展。
- 既有"每日任务"已预留 `gamesPlayed`/`hasGameToday` 钩子（`LearningViewModel.java:67-71`）。
- 动画基础好：`MatchCardAdapter.animateShake`、`ConfettiView`、得分色 `score_gold` 等现成可复用。

---

## 3. 游戏体系总览

### 3.1 保留 / 升级 / 新增矩阵

| 游戏 | 决策 | 一句话理由 | 主要改动面 |
|------|------|-----------|-----------|
| **对诗（Couplet）** | 🟢 **保留但重度升级** | 最贴合"背诵"场景，但当前 10 轮过长、干扰项无脑 | 改轮数、改干扰项、加分级、加"半对"提示、加最高分 |
| **消消乐（Match）** | 🟡 **升级为"诗词连连看"** | 保留最易上手的配对乐趣，但扩展选句范围 + 加计时/回合 | 选句上句/下句混合、计时、星级，去掉全可见 |
| **填空（Quiz）** | 🟢 **保留并小幅升级** | 是"默写"最强题型，提升干扰项质量 + 难度可调即可 | 干扰项算法、难度分级、按空结算 |
| **（新增）飞花令** | 🔵 **新增** | 复用 91K 数据、TTS、拼音，做成最像"真游戏"的限时模式 | 全新 FlyflowerGameFragment |
| **（新增）作者连线** | 🔵 **新增（P2 可选）** | 低龄友好、素材现成、一眼看得懂的认知配对 | 全新 AuthorMatchFragment |
| **游戏大厅** | 🟢 **升级为"今日挑战 + 成绩回廊"** | 解决"没理由点进游戏"的入口问题 | GameHub 重写成卡片 + 挑战区 + 最高分区 |

> **为什么新增飞花令而非其他更花哨的游戏**：飞花令（含"字"）是《中国诗词大会》的招牌玩法，小学生认知度高；且它**不依赖后端、不依赖动画、不依赖新素材**——只靠 `String.contains` + 一句 TTS + 一组倒计时，完全契合"务实、离线、低门槛"约束，却提供了**限时 + 反应 + 词库广度**这三种现有关卡最缺的元素。

### 3.2 总体循环设计（一句话）

> **"扫一眼大厅 → 挑今天想攻的关 → 玩 1 局（约 2-4 分钟）→ 得积分/星级 → 看最高分和成就前进一格 → 次日有每日挑战"** 的轻量 roguelite 循环。

### 3.3 统一的"星级 + 积分 + 成就"结算契约（新注入）

为避免 §2.5 指出的三套重复逻辑，先定义一个**领域层中立结算结果对象**（纯 Java）：

```java
/** domain/GameResult.java（新增，纯 Java 领域对象） */
public class GameResult {
    public String gameType;        // "couplet" | "match" | "quiz" | "flyflower" | "author"
    public int score;              // 本局得分（0-1000）
    public int stars;              // 1-3 星（由规则换算，见各游戏）
    public int correctCount;       // 答对数
    public int totalCount;         // 总题数
    public boolean perfect;        // 是否完美（满分）
    public long durationMillis;    // 本局耗时
    public List<String> touchedPoemIds; // 本局涉及到的诗词 id（用于学习沉淀）
    // 经济结算（积分 / 成就 / 每日任务 / 主题）统一在此集中完成，见 §7
}
```

所有游戏结束时统一走 `GameSettlement`（见 §7），**消灭 `savePoints`/`recordGameActivity`/`finishQuiz` 三处重复**。

---

## 4. 各游戏详设

### 4.1 🟢 对诗（Couplet）—— 升级版

#### 玩法规则（目标 3 分钟可打一局）
- 改为 **7 轮**（`TOTAL_ROUNDS` 从 10 → 7），一次结算，降低疲劳。
- 每题显示**上句**，从 4 个候选中选下句。
- **答对**：得 `base(12) + combo`；**答错**：不扣分，但展示正确下句 + 一行白话（若有释义）。
- **半对奖励**：若选错的干扰项与正确答案**首字相同**（语义近似记忆命中），给 `+2` "差一点！" 提示，**孩子记得住半句也算有奖励**（对应 §1.1 胜任感）。
- 每对结算一次星级：**满分档 → 3 星；答对 ≥5 → 2 星；完成 → 至少 1 星**（只要有参与就不零星，呼应零挫败支柱）。

#### 难度梯度
按轮次递增干扰项"迷惑度"：
- 轮 1-2（暖手）：干扰项字数与答案**不相同**（易排除，保成功）；
- 轮 3-5（进阶）：干扰项字数与答案**相同**（开始有迷惑）；
- 轮 6-7（冲刺）：干扰项字数相同 **且** 与上句压头韵/尾韵的优先（最难）。

由 `GameEngine` 按 `roundNumber` 调用不同干扰项策略（见 §6.1 伪代码）。

#### 评分公式（`calcCoupletScore` 升级）
```
单轮得分 = (答对 ? 12 : 0) + (答对 ? combo : 0)      // combo = 连续答对数（1,2,...无上限但有下乘）
         + (首字相同干扰项被选中 ? 2 : 0)            // "差一点"善意补偿
满分参考：7 轮全对全连 = 12 + (1+2+3+4+5+6+7) = 40 ... 实际 12*7 + 28 = 112
星级：score ≥ 84 (答对≥6 且连击高) → 3星；≥56 → 2星；完成 → 1星
```

#### 随机出题 & 防重复
- **选诗**：先 `Collections.shuffle(allPoems)`（复用现成 shuffle），再顺序取可用诗，直到凑满 7 首，天然不重复。
- **选联**：同一首诗内存一个 `visitedPairIdx` set，避免重复出同一联。
- **防跨局重复（公平性）**：内存存一个"今日本局用过的诗 id 集合"（`SharedPreferences` 的 `date->idList`），当日同一游戏模式**不再重复出该首**；跨日自动重置——既不破坏随机主权，又保证短期不腻。

#### 触发成长沉淀
把 7 首诗的 `id` 放入 `GameResult.touchedPoemIds`，收尾时调 `AchievementEngine.checkAndUnlock`（复用）+ `ensureRecordExists` 标记"玩过"（`LearningRecordDao` 现成）+ `recordGameActivity` 计入 `hasGameToday`。

### 4.2 🟡 消消乐 → 升级为「诗词连连看」

#### 玩法规则
- 保留"上下句配对消除"核心，但**规则升级**：
  1. 卡片仍是 12 张（6 对），但 **不再强制第一联**：每首诗从 `lines` 中**随机抽取 2 行（或 1 组上下句）**，只要两行相邻且均 ≥2 字、非省略号即可（复用 `GenerateMatchGame` 已有的过滤）。
  2. 消除一张**不是上句/下句关系的两行**（`checkMatch`）仍判失败并抖动（复用）。
  3. **新增：限时 90 秒**（右上倒计时）。到点未完成 → 结算当前进度。
  4. **新增：回合数计入星级**：`≤7 次尝试完成 → 3 星；≤9 → 2 星；完成 → 1 星`。
- **移除"全部正面可见"**：改为**翻开式**（默认背面朝上，点开一张翻面，再点另一张判定）。专注记忆配对，而不是"扫一眼"。这是把"消消乐"从"反射"拉回"记忆"的关键，也让配对更有意义。

#### 评分公式
```
连消加分：每次成功配对 +40，失败不扣分（但累计尝试）
基础分 = 40 × 成功对数
用时加分（仅限限时内完成）：(90 - 用秒) × 缩放系数，封顶 +300
总分 = 40×6 + 用时加分 - 错误缓冲   （下限保底 40，未完成按进度给分）
星级：完成且 ≤7 次尝试 → 3星；≤9 → 2星；完成 → 1星
```

#### 备注（实现成本)
- 翻开式用 `item_match_card.xml` 加一张"背面"drawable（现有 `bg_card` 家族可仿制），`MatchCardAdapter` 增加 `flipped` 状态位即可——**增量小**。
- 保留现有 `GameEngine.checkMatch` / `isGameComplete` / 动画 / `lockInput` 机制，只改 `startMatchGame` 的选句逻辑与 Fragment 的翻面交互。

### 4.3 🟢 填空（Quiz）—— 小幅升级

#### 玩法规则（不变骨架）
保留 `endless→5 题` 结构、候选词 Chip 交互、撤销（`undoBlank`）——这些都是好的。

#### 关键升级点
1. **干扰项算法**（替换 `QuizGenerator` 硬编码 40 字）：
   - 从**数据池里的真实汉字**抽样（`PoemRepository.getRandomPoem()` 若干首取它们的汉字），优先取**与正确答案同音**（pinyin4j `PinyinHelper` 复用）或**结构相似**的字，其次随机。
   - 兜底：仍可保留少量文化用字，但不再只从 40 字硬编码取。
2. **难度分级**（新增 `QuizDifficulty` 维度）：
   - Easy：挖 1 空、选自**有释义的 88 首名篇**（`poem.explanation != null`），干扰项字数偏离 → 好答；
   - Normal：挖 1-2 空、任意诗、同音干扰；
   - Hard：挖 2-3 空、同音+结构相似干扰、答案多义字优先。
   由大厅或关卡进入时传入难度。
3. **按空结算**（`calcPointsForQuiz` 扩展：按"空"命中的比例给分，而非整题二值），让"答对 2/3 个空"也有积分反馈。

#### 评分 / 星级
```
每题：命中的空数 / 总空数 × 20 分（满分单题 20，对应现有 quiz_score 语义）
整局（5 题）：
  满分（20×5=100）→ 3星 + 计入 "quiz_perfect_5" 成就（已存在，见 AchievementEngine）
  正确率≥80% → 2星
  完成 → 1星
```

### 4.4 🔵 新增：飞花令（Flyflower）—— 主打明星玩法

#### 玩法规则
- **单人限时飞花令**：系统给一个**主题字**（如"月""花""雪""风""山"），显示题目 **"说一句带『月』的诗"**。
- 玩家在**输入框**输入一句或半句（最多 20 字），点击"对令"。
- 系统用 `String.contains` 做**模糊判定**（`text.contains(keyword)`），命中即算成功，播放 TTS 朗读 + 加分。
- 倒计时 **60 秒**，看你能接多少句。
- **永不判"错"的死路**：输入不含主题字时给出温柔提示"这句没有『月』哦，再想想～"并扣除一次"尝试机会"（共 3 次尝试，超出即时该句作废但不结束游戏）。

#### 为什么它最好落地
- 不依赖任何新素材：主题字表硬编码 20-40 个常见意象字；判定只是 `String.contains`。
- 复用 `TtsManager.speakPoemStructured` 朗读（已存在）。
- 复用 `GameResult` + `GameSettlement`，成就计数直接走 `game_10` 那套。
- 完美承接"复现古诗/接句"与"自己造句"两个层次的创作感，是孩子最可能"自己会额外来一局"的模式。

#### 评分
```
基础分 = 命中句数 × 30
连招：每连续 3 句命中 +20 连击奖励（封顶 +200）
长度加成：命中句 ≥5 字额外 +10（鼓励整句而非单字）
总星：命中 ≥10 → 3星；≥6 → 2星；≥3 → 1星
```

#### 防重复
同一主题字限出现一次（session 内主题字集合去重）；连续会话可换主题字。

### 4.5 🔵 新增：作者连线（Author Match）—— P2 可选、低龄友好

- 三列卡片：**作者 → 代表作选一句 → 朝代**，点选正确作者与其一句代表作构成连线。
- 数据全来自 `Poem.author` / `Poem.title` / `Poem.lines[0]` / `Poem.dynasty`，零新数据。
- 评分 / I 星级参照 §4.1 模板。**作为 P2 可选**，不影响主线交付。

---

## 5. 难度梯度与内容池治理（贯穿所有游戏）

### 5.1 统一"可用诗句"过滤规则（沉淀现有散落过滤）

现有代码在多处重复判 `lines.length < 2`、`length()<2`、`contains("…")`（`GameEngine.java:70,168-171`、`QuizGenerator.java:58,146,199`）。建议沉淀为 **`PoemUtils.isUsablePair(Poem)`**：

```java
/** domain/PoemUtils.java（新增） */
public static boolean isUsableLine(String l) {
    return l != null && l.length() >= 2
        && !l.contains("…") && !l.contains("……") && !l.trim().isEmpty();
}
public static boolean isUsablePair(Poem p, int i) {
    return p.lines != null && i + 1 < p.lines.length
        && isUsableLine(p.lines[i]) && isUsableLine(p.lines[i+1]);
}
```
所有游戏改为调用它，**消除散落的重复判空**。

### 5.2 三级内容策略

| 级别 | 内容来源 | 用途 |
|------|---------|------|
| **入门段** | 88 首有释义名篇（`explanation != null`）+ 短诗（五言） | Easy 难度、前 2 轮暖手 |
| **中段** | 全部 91,196 首中可用联句 | Normal 难度 |
| **深段** | 长诗（律诗/绝句的中继句、七言、含生僻字） | Hard / 冲刺轮 |

### 5.3 防重复与公平性统一策略
- **局内**：已选题 `Set<String> usedPoemIds` 全程去重。
- **当日**：`SharedPreferences` 存 `date -> 各游戏今日用过的 poemId set`，同日不重复出，跨日重置。
- **选项不重复**：沿用现有 `used[idx]` + `options.contains()` 校验（`GameEngine.java:84-98`），补一条**干扰项不得与正确答案为空串**的守卫。

---

## 6. 随机出题算法设计（重点）

### 6.1 对诗出题伪代码（升级版）

```text
function generateCoupletGame(pool, rounds, difficulty) ->
    shuffled = shuffle(pool)
    game = []
    usedPoemIds = loadTodayUsed("couplet")      // SharedPreferences date-scoped
    for poem in shuffled:
        if not PoU.isUsablePair(poem, 0): continue   // 至少有一对可用
        if poem.id in usedPoemIds: continue          // 当日防重复
        pick a pair (i, i+1) not visited in this poem for this round
        given = poem.lines[i]
        answer = poem.lines[i+1]
        options = [answer]
        // 干扰项按 roundNumber 升级迷惑度：
        //   round<3 → 随便挑一句（不要求同字数）
        //   round>=3 → 只挑与 answer 等长的行（len equal）
        //   round>=6 → 等长 且 与 given 首字相同者优先（压头字）
        while options.size < 4:
            other = random poem (pool)
            line = random usable line of other
            if notUsed and not duplicate:
                if round>=3 and len(line) != len(answer): continue  // 等长约束
                options.add(line)
        shuffle(options)
        game.add(round(given,answer,options,roundNumber))
        usedPoemIds.add(poem.id)
        clearTodayUsed("couplet", usedPoemIds)        // 持久化当日进度
        if game.size == rounds: break
    return game
```

### 6.2 消消乐出题伪代码（改造：不强制第一联 + 翻面）

```text
generateMatchGame(pool, pairs) ->
    shuffle(pool)
    for poem in pool:
        if poem.id in todayUsed("match"): continue
        candidates = all indices i where isUsablePair(poem, i)
        if empty: continue
        i = random(candidates)                         // 随机联（而非固定 lines[0]/lines[1]）
        a = MatchCard(lines[i], isFirstHalf=true)
        b = MatchCard(lines[i+1], isFirstHalf=false)
        add a,b (shared pairId)
        todayUsed("match").add(poem.id)
        if done pairs: break
    shuffle all cards  // 复用
```

### 6.3 填空干扰项伪代码（优化同音/相近）

```text
generateFillBlank(poem, difficulty) ->
    ... (保留现有挖空逻辑，见 §4.3 难度：挖 1/2/3 空)
    answerChars = 已挖空的答案字符集合
    candidateSet = answerChars
    pool = 从 random 5 首诗中收集的汉字 multiset
    while candidateSet.size < min(6, answerChars+4):
        d = 空（从未用）
        // 优先：找一个与某答案字符同音的可用汉字
        for c in pool where PinyinHelper.initial(c) == PinyinHelper.initial(某answerChar):
            candidateSet.add(c); break
        // 其次：等结构（同字数常用意象）
        if d == 空: d = random from pool
        candidateSet.add(d)
    shuffle
```
（`PinyinHelper` 已存在于 `com.poetry.util`，复用其 `toTonePinyin` / 拼音同音判定。）

---

## 7. 与成长体系（积分/等级/成就/主题/连续打卡）联动

### 7.1 统一结算器 `GameSettlement`（新增，替代三处重复）

把 `GameViewModel.savePoints/recordGameActivity`、`QuizViewModel.submitAnswer/finishQuiz` 里的重复逻辑收敛到**一个领域层 + DAO 层**：

```java
/** domain/GameSettlement.java（新增，纯逻辑 + 依赖 db） */
public static void settle(LearningDatabase db, GameResult r) {
    // 1) 积分原子累加（UserProfileDao.addTotalPoints），并计算/更新等级（LearningEngine）
    // 2) 每日统计：incrementGamesPlayed(今日) + pointsEarned（DailyStatsDao 原子）
    // 3) 学习沉淀：r.touchedPoemIds 每个 id → learningRecordDao.ensureRecordExists(id)
    //    （不自动"已学"，只是"玩过"，保持与收藏/已学语义解耦）
    // 4) 成就检测：AchievementEngine.checkAndUnlock(db, callback)
    // 5) 主题解锁同步：ThemeManager.syncUnlockedThemes(db)
    // 6) 可选：写最高分到 GameHistory（见 §8 Room 改动）
}
```
> **保守建议**：积分结算规则**不改变**（沿用 `LearningEngine.calcPointsForQuiz` 语义、`addTotalPoints` 原子写），只**集中调用**，避免动到已验证的成长曲线。若某游戏要加"完成即 +N"的保底分，也只在 `GameResult` 层面折算，不改 `LEVEL_THRESHOLDS`。

### 7.2 各游戏对成长环的贡献（确认现状已接通的 + 需补的）

| 成长点 | 现状是否接通 | 升级后 |
|--------|------------|--------|
| **每日任务「玩一局游戏」** | ✅ 由 `recordGameActivity → incrementGamesPlayed → hasGameToday` 接通 | 沿用，`GameSettlement` 统一触发 |
| **成就 `game_10`「游戏高手」** | ✅ `AchievementEngine.getGameCount` 用 `LearningRecord.gamePlayed` 累加 | 沿用，飞花令/作者连线也计入 |
| **成就 `quiz_perfect_5`「满分达人」** | ✅ `getPerfectQuizCountSync(10)` | 沿用，填空满分才计 |
| **等级积分** | ✅ `savePoints` / `submitAnswer` → `addTotalPoints` | 统一到 `GameSettlement` |
| **主题解锁** | ✅ `ThemeManager.syncUnlockedThemes` | 沿用 |
| **游戏专属最高分 / 历史** | ❌ 现状无 | **新增**：`GameHistory` 表（见 §8）|
| **游戏→学习回流（玩过的诗）** | ❌ `gamePlayed` 只计数，不回流学习 | **新增**：`touchedPoemIds` 关联（不强制"已学"，但可在大厅"本局遇到的诗"入口直达详情页）|

### 7.3 新增成就（可选，P1 建议加 2-3 个，P2 起）
建议追加（在 `AchievementEngine.ALL_ACHIEVEMENTS` 追加即可，不动现有 12 个，保证老用户不回退）：
- `flyflower_20`「飞花令主」：飞花令累计命中 20 句；
- `star_30`「三星集邮家」：累计获得 30 颗星（跨全部游戏）；
- `couplet_first_perfect`「一气呵成」：对诗一局全对。

> 判定标准：这些成就在发布后 1 个月内被至少 10% 活跃用户各解锁 ≥1 个。

---

## 8. 数据模型改动（Room）

### 8.1 新增 `game_history` 表（最克制的方案）
用于"成绩留痕 + 最高分展示"，**不加复杂排行榜**。

```java
@Entity(tableName = "game_history",
    indices = {@Index("gameType"), @Index("playedAt")})
public class GameHistory {
    @PrimaryKey(autoGenerate = true) public long id;
    public String gameType;      // "couplet"|"match"|"quiz"|"flyflower"|"author"
    public int score;            // 本局得分
    public int stars;            // 1-3
    public int correctCount;
    public int totalCount;
    public long playedAt;        // 完成时间戳
    public long durationMillis;  // 本局耗时
}
```
- 每个新 `GameHistoryDao`：`insert`、`getBestScoreByType(type)`、`getBestStarsByType(type)`、`getRecentByType(type, limit)`。
- **记最高分**：结算时手写 `SELECT max(score)` 比对或直接由 `GameResult` 先存后取。
- **Migration v4 → v5**：在 `LearningDatabase` 加一张新表（非破坏性，参照现有 MIGRATION_3_4 模式），需在数据库中 `Migration object` 里 `execSQL("CREATE TABLE IF NOT EXISTS game_history ...")`。

### 8.2 不改现有表
`learning_records` / `user_profile` / `daily_stats` **字段不动**（避免破坏已验证的 `MIGRATION` 与语义）。所有新增状态（当日去重、难度选择、星级累计）走：
- `SharedPreferences`（当日去重、最近难度、玩家偏好）——免迁移；
- `game_history`（历史/最高分）——一次性迁移。

> **决策依据**：宁可新表不上复杂 migration，也不改既有表结构降低回归风险。

---

## 9. 复用 vs 新增清单（让实现者不再猜）

| 诉求 | 复用现状 | 需要新增 |
|------|---------|---------|
| 对诗出题 | `GameEngine.generateCoupletGame`（改轮数/等长干扰） | 等长/头字干扰策略函数、半对判定 |
| 消消乐 | `GameEngine.generateMatchGame/checkMatch/isGameComplete/calcMatchScore` + `MatchCardAdapter` 动画 | 随机联选择、翻面状态、倒计时 |
| 填空 | `QuizGenerator.generateFillBlank`（改干扰项）+ `QuizFragment` 交互 | 同音干扰器、难度维度、按空计分 |
| 飞花令 | `TtsManager`、`PoemRepository` | `FlyflowerGameFragment` + 判定逻辑 |
| 作者连线 | `Poem.author/dynasty/lines[0]` | `AuthorMatchFragment`（P2）|
| 统计/成就 | `AchievementEngine`、`DailyStatsDao`、`LearningRecordDao` | `GameSettlement` 统一结算器 |
| 主题/等级/签到 | `ThemeManager`、`LearningEngine`、`UserProfileDao` | 无（沿用）|
| 数据池过滤 | 散落多处 | `PoemUtils.isUsablePair`（收敛）|
| 大厅 | `GameHubFragment` + `fragment_game_hub.xml` | 挑战区 + 最高分区 + 今日状态 |
| 导航 | `nav_graph.xml`（`nav_game_couplet/match/quiz`） | `nav_game_flyflower`（飞花令）、可选 `nav_game_author`、大厅复用 |

### 需**注意的重复实现**（建议在升级时顺手收敛）
- `QuizGenerator.generateCouplet()`（`QuizGenerator.java:142-176`）与 `GameEngine.generateCoupletGame()`（`GameEngine.java:61-107`）是**两套并行接龙实现**——统一保留 `GameEngine` 作为唯一接龙出题源，`QuizGenerator.generateCouplet` 标注 `@Deprecated` 或直接删除（确认无调用方后）。
- `GameViewModel.savePoints` 与 `QuizViewModel.submitAnswer` 的积分/成就代码重复——收敛至 `GameSettlement`。

---

## 10. 包结构 / 类职责建议

```
com.poetry.domain/                    ← 纯逻辑层（无 Android 依赖，保持现状）
  ├─ GameEngine.java                  [改] 接龙出题升级 + 等长干扰 + 半对；消消乐随机联
  ├─ QuizGenerator.java               [改] 同音干扰器 + 难度维度 + 按空计分接口
  ├─ QuizDifficulty.java              [新增] enum {EASY, NORMAL, HARD}
  ├─ PoemUtils.java                   [新增] isUsablePair 等统一过滤
  ├─ GameResult.java                  [新增] 中立结算结果（§3.3）
  ├─ GameSettlement.java              [新增] 统一积分/成就/每日/主题结算
  └─ FlyflowerEngine.java             [新增] 飞花令判定 + 主题字表 + 计分（纯逻辑）

com.poetry.data/                      ← Room 层
  ├─ GameHistory.java                 [新增] Room 实体（§8.1）
  ├─ GameHistoryDao.java              [新增] DAO
  ├─ LearningDatabase.java            [改] v5 迁移 + 注入 GameHistoryDao
  └─ (其余不变)

com.poetry.ui/game/                   ← 表现层
  ├─ GameHubFragment.java             [改] 升级为"今日挑战 + 成绩回廊"入口
  ├─ CoupletGameFragment.java         [改] 轮数/难度/半对提示/星级结算
  ├─ MatchGameFragment.java           [改] 翻面 + 倒计时 + 星级
  ├─ FlyflowerGameFragment.java       [新增] 飞花令页（含输入框 + 主题字 + 倒计时）
  ├─ AuthorMatchFragment.java         [新增] 作者连线（P2）
  ├─ GameViewModel.java               [改] 接入 GameSettlement；暴露星级/倒计时 LiveData
  ├─ GameHubViewModel.java            [新增] 大厅：今日挑战、各游戏最高分、今日已玩状态
  ├─ MatchCardAdapter.java            [改] 增加 flipped 状态 + 背面子视图
  └─ (QuizFragment/QuizViewModel 在 ui/quiz/)  [改] 问难度、按空结算、接入 GameSettlement

res/navigation/nav_graph.xml          [改] 新增 nav_game_flyflower（与不存在 nav_game_author 先占位）
res/layout/
  ├─ fragment_game_hub.xml             [改] 挑战区 + 最高分区 + 状态角标
  ├─ fragment_flyflower.xml            [新增]
  └─ item_match_card.xml               [改] 翻面背面子视图
strings.xml                            [改] 新增星级/挑战/飞花令相关文案
```

---

## 11. 工作量拆解与里程碑（可分批交付）

| 里程碑 | 交付内容 | 预估工作量 | 验收可验证标准 |
|--------|---------|-----------|--------------|
| **M1 · 基建（0.5 周）** | `PoemUtils`、`QuizDifficulty`、`GameResult`、`GameHistory` + DAO + v5 迁移 | 0.5 周 | `gradlew assembleDebug` 通过；老用户升级无数据丢失；单元测试覆盖 `PoemUtils` 过滤 |
| **M2 · 大厅升级（0.5 周）** | `GameHub` → 今日挑战 + 最高分区 + 今日已玩状态 + `GameHubViewModel` | 0.5 周 | 大厅能看到上次各游戏最高分与星级、今日游戏是否已玩（改 code + 手测） |
| **M3 · 对诗升级（1 周）** | 7 轮 + 等长/头字干扰 + 半对提示 + 星级结算 + 当日去重 | 1 周 | 手测：干扰项与正确答案字数一致（轮 3+）；答对才给分；不重复出当日同诗 |
| **M4 · 消消乐升级（1 周）** | 随机联 + 翻开式 + 90s 倒计时 + 星级 | 1 周 | 手测：卡片不再第一联；翻开配对；倒计时归零结算；动画无异常 |
| **M5 · 填空升级（0.5 周）** | 同音干扰器 + 难度 + 按空计分 | 0.5 周 | 手测：干扰项优先同音；EASY 取自名篇；答对 2/3 空有部分积分 |
| **M6 · 新增飞花令（1 周）** | `FlyflowerGameFragment` + `FlyflowerEngine` + 导航 + TTS 接入 | 1-1.5 周 | 手测：输入含主题字的诗句判命中、TTS 朗读、60s 倒计时、计分入 `GameHistory` |
| **M7 · 新成就 + 回归（0.5 周）** | 追加 2-3 成就 + 全量回归 + 联调 `GameSettlement` | 0.5 周 | 新成就解锁有 Toast + Confetti；历史 12 成就不受影响 |
| **M8（可选 P2）· 作者连线** | `AuthorMatchFragment` | 1 周 | 独立验收同上 |

> 总主线约 **5-6 周**（单人全栈），飞花令为增量明星功能。建议 M1→M3 先行，让"对诗"这个最贴合背诵的模式最先受益。

---

## 12. 风险与降级方案

| 风险 | 影响 | 降级 / 对冲 |
|------|------|-----------|
| **冷启动加载慢**（现 3-5s 全量 91K） | 进游戏时 `getAllPoems()` 若数据未就绪返回空 → 空局 | 复用 `PoemRepository.isLoaded()`：未就绪显示"墨香氤氲，题库暖身中…"，就绪后再 `start`（现状 `GameViewModel` 未校验，需补）。|
| **内存**：请求 `todayUsed` + `GameHistory` 常驻 | 手游内存紧张 | 当日去重存 `SharedPreferences`（小）；`GameHistory` 只查最近 N=50 条入 LiveData，杜绝全表加载。 |
| **TTS 引擎未装** | 飞花令朗读失败 | `TtsManager` 已有 `onInitStatus` 失败降级为 Toast 提示"语音包未装"，**不影响游戏判定**。 |
| **解析耗时**：同音干扰器遍历池 | 填空出题慢 | 干扰器从预筛的"意象字/主题字"小集合 + `random getRandomPoem` 采样，**不线扫全池**；必要时用 `AppExecutors.io()` 异步出题。 |
| **Room 迁移失败** | 升级崩溃 | 新表用 `CREATE TABLE IF NOT EXISTS`；`fallbackToDestructiveMigration` 保留即好？→ **不要**。应写显式 MIGRATION_v4_v5，配 `build()`（不加 destructive），迁移失败则走降级不崩溃。 |
| **M6 飞花令判定过松/过严** | 影响公平性 | 判定 = `contains(keyword)` 且输入非空、长度≥2；对"含字但非诗句"（如随意乱输）允许但不鼓励——不为此加 NLP，保持务实。 |
| **成就/积分重复结算**（多 Fragment 观察 `newAchievement`） | 重复庆祝 | 沿用现有 `clearAchievement()` 消费清空模式（已在我读到的 `Couplet/Game/Quiz` Fragment 中一致实现），结算统一走 `GameSettlement` 单次回调。 |
| **星级过于慷慨/吝啬** | 玩家麻木或挫败 | 所有星级阈值标注 `[待测试]`，第一版用 M3-M6 手测调参；不做"负分"设计。 |
| **每日去重失效**（用户改系统时间） | 公平性 | 去重键取"自然日"，改时区/时间被钻空属可接受取舍（离线单机场景），不投入反作弊。 |

---

## 13. 可验证的判定标准汇总（每个目标如何衡量达成）

| 目标 | 指标 | 达成线 |
|------|------|--------|
| 游戏可复玩（核心目标） | 单次会话游戏时长 / 二次进入率 | ≥3 分钟 / 二次进入 ≥40%（埋点：`GameHistory.playedAt` 时间戳聚合 + `DailyStats.gamesPlayed` 趋势） |
| 对诗有挑战性 | 干扰项与正确答案字数一致率（轮 3+） | 代码断言 + 手测 100% |
| 对诗不再疲劳 | 单局时长 | ≤3 分钟（7 轮） |
| 填空干扰项质量 | 干扰项中"同音/相近字"占比 | ≥70%（可用一次抽样测试统计） |
| 消消乐考到更广内容 | 出题联句不限于 `lines[0]/lines[1]` 的比例 | 手测/日志 ≥90% |
| 成长闭环 | 玩一局游戏 → 积分/成就/每日任务/主题 全部联动 | 端到端手测一次通过 `GameSettlement` |
| 老用户不回归 | 现有 12 成就 + 9 主题 + 等级曲线不破坏 | 升级后快照对比 `UserProfile` / `AchievementEngine` 全量回归 |
| 冷启动稳定性 | `isLoaded()` 门控 | 数据未就绪时无空局、无 ANR |

---

## 14. 总结

本方案没有引入任何后端、任何重型依赖、任何 Kotlin/Compose 迁移，**完全在现有 Java 8+ + View System + Material 3 体系中增量演进**。核心思路：

1. **收敛**：把三套重复的结算/过滤逻辑收为 `GameSettlement` + `PoemUtils`；
2. **升级**：对诗（干扰项 + 短局）、消消乐（随机联 + 翻开 + 计时）、填空（同音干扰 + 难度）各自补上"挑战性"与"反馈层级"；
3. **新增**：飞花令——唯一真正"像游戏"的限时玩法，素材零新增、落地成本最低、对小学生的吸引力最强；
4. **联动**：通过 `GameHistory` 成绩留痕 + `GameResult` 统一沉淀，把"玩游戏"与积分/成就/主题/每日任务/学习回流真正接通。

所有新增都标了复用来源与改动面，所有阈值都标了 `[待测试]` 供首版手测调参。交付按 §11 里程碑分批，每批都有可验证的达成线。
