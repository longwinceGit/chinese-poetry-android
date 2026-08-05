# 诗词乐园 · 游戏模块重设计最终合并版

> **最终合并版（v1.0 机制 + v2.0 趣味友好迭代）**
> 版本：FINAL（合并 v1.0 初稿 + v2.0 增量优化）｜日期：2026-08-05
> 范围：`com.poetry` 下游戏相关模块的重设计，纯 Java 8+、Android View System + Material 3、完全离线、复用 91,196 首诗词数据。
> 本文档为**活文档**，每次修订须更新版本号与变更日志。
> 标注约定：相对 v1.0 的增量改动以「🟢保留 / 🔧新增 / ❌推翻」标注；所有新阈值标 `[待测试]`。

---

## 0. 变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-08-05 | 初稿：基于对现有代码的逐文件核对，产出游戏体系重设计（7 轮对诗、连连看、同音干扰、飞花令、作者连线、GameSettlement、GameHistory） |
| v2.0 | 2026-08-05 | 趣味友好迭代：全局趣味反馈层、小学生易操作设计、各游戏多模态降门槛、完成感结算页、成就小目标化、单局缩短至 1.5-3 分钟。完整保留 v1.0 机制升级 |
| **FINAL** | 2026-08-05 | **最终合并版**：将 v1.0 机制正确性重设计与 v2.0 面向 6-12 岁小学生的趣味/易操作优化合并为单一自洽文档，消除重复表述，可直接落地 |

---

## 1. 设计哲学（为什么这样设计对目标用户有激励）

### 1.1 目标用户与他们的动机密码

本 App 目标用户是**中小学生（主力）+ 文学爱好者（次力）+ 家长 / 教师（引导者）**。对照儿童动机研究框架，孩子愿意反复玩一个学习类游戏，靠的是四件事：

| 动机原力 | 对应当前需求 | 本方案的对策 |
|---------|-------------|-------------|
| **胜任感（Mastery）**：我能学会、越学越好 | 现有游戏没有"难度梯度"，要么一次性随机，要么固定 5 题 | 每个游戏引入**难度分级 + 成就里程碑**，让"变强"可被看见 |
| **自主感（Autonomy）**：由我选择 | 三个游戏卡片平铺，没有"选什么"的动机 | 引入**每日主题挑战 + 不同玩法轮换**，让孩子自己决定今晚玩哪种 |
| **联结感（Relatedness）**：我跟同学/家人比 | 完全单人、无任何分享/回放 | 引入**成绩留痕（最高分榜）**，为未来"好友 PK"埋点，且不依赖后端 |
| **即时反馈（Immediacy）**：点一下就有反应 | 得分是"静悄悄的数字"，连击只在内心 | 引入**章节式即时反馈 + 得分动画 + 花式庆祝** |

### 1.2 六条设计支柱（Design Pillars）

所有后续决策以这六条不可妥协的体验为准绳。前三条来自 v1.0，后三条由 v2.0 叠加，合并为同一体系：

1. **零挫败的挑战感**：孩子第一个回合的每一次尝试都必须"有点收获"——答错要温柔、答对要响亮。**不允许出现"扣分到负"或"一题答错就全盘皆输"的设计。**
2. **把"背诵"做成"游戏"而非"测验"**：出题必须以**真实诗词**为数据源，难的题也要让孩子**通过已知的上文或联想**能答对，而不是逼他死记。
3. **每一次投入都沉淀到成长体系**：玩一局游戏必须（a）给积分、（b）给成就计数、（c）贡献每日游戏任务、（d）可能解锁主题。**游戏不能是"学了没用"的孤岛。**
4. **「爽感优先（Juice First）」**：孩子的每一次正确动作，**0.3 秒内**要有视觉 + 声音 + 数字三重反应。答对要"响亮"，答错要"温柔"。
5. **「能点绝不打字（Tap Over Type）」**：面向小学生的所有交互，**优先点选、翻牌、连接**，键盘输入仅作为高年级进阶选项，且必配"候选句降级"兜底。
6. **「最小上手成本（Low-Floor）」**：第一关等于"走流程"也能拿 1 星；状态、目标、还剩几题**始终一眼可见**；任何清空/重来都有二次确认。

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

> **"扫一眼大厅 → 挑今天想攻的关 → 玩 1 局（约 1.5-3 分钟）→ 得积分/星级 → 看最高分和成就前进一格 → 次日有每日挑战"** 的轻量 roguelite 循环。

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

> 每个游戏在同一节内同时含 v1.0 机制设计 + v2.0 趣味/易操作细化。

### 4.1 🟢 对诗（Couplet）—— 升级版

#### 玩法规则（目标 1.5-2.5 分钟可打一局）
- 改为 **7 轮**（`TOTAL_ROUNDS` 从 10 → 7），一次结算，降低疲劳。
- 每题显示**上句**，从 4 个候选中选下句。
- **答对**：得 `base(12) + combo`；**答错**：不扣分，但展示正确下句 + 一行白话（若有释义）。
- **半对奖励**：若选错的干扰项与正确答案**首字相同**（语义近似记忆命中），给 `+2` "差一点！" 提示，**孩子记得住半句也算有奖励**（对应 §1.1 胜任感）。
- 每对结算一次星级：**满分档 → 3 星；答对 ≥5 → 2 星；完成 → 至少 1 星**（只要有参与就不零星，呼应零挫败支柱）。

#### 难度梯度（🔧 修订 v1.0：等长干扰推迟到冲刺轮）
按轮次递增干扰项"迷惑度"，但对小学生**等长干扰可能反而难**（读不出来），故把 v1.0 的"轮 3-5 就上等长"推迟：
- 轮 1-2（暖手）：干扰项字数与答案**明显不同**（秒排除，保成功）；
- 轮 3-5（进阶）：干扰项字数与答案**相近但对语言长度肉眼可辨**（开始有迷惑）；
- 轮 6-7（冲刺）：干扰项字数相同 **且** 与上句压头韵/尾韵的优先（最难），**且给语气词提示辅助**。

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

#### v2.0 趣味/易操作细化（🔧 新增）
1. **半句/语气词提示**：题干下加一行浅灰"半句提示"（如题面"床前明月光" → 提示"下一个字是『__？___』开头"仅显示**首字**，点 💡 灯泡再显示**末字** `[待测试]` 半句提示=显首字，全提示=首末字）。效果：孩子"想不起整句但想起首字"也有抓力，专治"卡住"。
2. **语音读题**：题干旁 🔊 一键 `TtsManager` 朗读上句；进题自动 1.5s 轻声读一次（§8.4）。
3. **看"语气/意象"选项替代图片**：不用真图（零素材），用 **emoji + 关键词** 帮助分辨干扰项。规则：干扰项与答案**首字相同**时，给候选句前缀一个小 emoji 意象标签（🌙/🌸/❄️），并标注朝代/作者小字——让差的选项"一眼不同"，对字长的。例：答案"举头望明月"(🌙)，干扰"低头思故乡"(🙏)，孩子靠图标+情感能感到差异。
4. **干扰项差异低龄可见**：见上方"难度梯度"修订——把"等长"推迟到冲刺轮，前面用"长度+意象"保成功。

> **为什么更好**：三个问题（半句提示/语音读题/替代图片）分别解决了"记不住整句""不认字""分不出相似句"三挡低龄痛点；而干扰项调整把 v1.0"等长迷惑"的难度留给了高年级冲刺，避免 6 岁直接劝退。

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

#### v2.0 针对"翻开式太难"的补强（🔧 新增）
- **翻开 2 张自动记忆「短暂预览」**：开启后，每次翻开两张判定前，若此第二张此前已在记忆中的另一位置配对过，则**短暂让它俩"闪一下"变亮**（0.8s 预览后归位）作为"帮你记"的提示 `[待测试]` 默认开启，可在大厅/设置关（高年级可关）。这对 6 岁极友善：不是靠冷记忆，而是"系统帮他记一瞬"，把负担从 recall 降到 recognition。
- **首局无计时（自适应 §8.3）**：第一局完全无倒计时，避免新手慌乱；从第 2 局起按表现逐步引入 90s→75s 倒计时。
- **每对成功时**：`GameFeedback` 火苗 +1、句子用 TTS 轻声读一遍（把"配对游戏"顺便变成"复读刷耳"）。
- **试错轻惩罚**：配对失败**不扣分**（v1.0 如此），仅温柔抖动 + 归位，并提供"这两句像朋友的提醒"。

> **为什么更好**：翻开式对低龄记忆负荷确实偏大，但**短暂预览**把难度从"纯 recall"降到"recognition + 一点记忆"，配合首局无计时，让 6 岁也能顺畅配对——这是"机制有趣但门槛高"的最佳中和。

#### 备注（实现成本)
- 翻开式用 `item_match_card.xml` 加一张"背面"drawable（现有 `bg_card` 家族可仿制），`MatchCardAdapter` 增加 `flipped` 状态位即可——**增量小**。
- 保留现有 `GameEngine.checkMatch` / `isGameComplete` / 动画 / `lockInput` 机制，只改 `startMatchGame` 的选句逻辑与 Fragment 的翻面交互。

### 4.3 🟢 填空（Quiz）—— 小幅升级 + 点选化

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

#### v2.0 针对"低龄打字难"的根本改造（🔧 新增）
- **点选缺字，而非键盘输入**：题干挖空处显示「＿＿」，下方给一排**候选字 Chip（点选）**，点中即填入该空（可点空撤销重选，复用 `undoBlank`）。没有软键盘弹出，没有拼音输入负担。
- **候选字数量可随难度**：Easy 给 5 个（含 1 答案）`[待测试]`；Normal 给 6-8 个；Hard 给 8-10 个且同音干扰更多。
- **干扰字也做"低龄可见"**：Easy/Normal 的干扰字优先选**与答案意象/结构差异明显**的（如答案"山"，优先放"水/海"而非难辨的"岗"），Hard 才上同音（§6.3 同音算法保留，但 Easy 降级）。
- **读题辅助**：题干挖空前后整句可 🔊 朗读；答对自动读完整句。
- **保留键盘输入**作为高难度/高年级选项（双输入并存），点选只作 Easy/Normal 默认。

> **为什么更好**：低龄孩子中文拼音打字极慢且易错，**点选绝对是零门槛**。同时点选消除了键盘遮挡屏、误触按键、拼音选字等一连串挫败源，让"默写"变成"选字大挑战"——学习目的（回忆缺字）保留，交互负担清零。

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

#### v2.0 针对"低龄输不出字"的降级入口（🔧 新增）
1. **候选句模式（主答入口，默认开启）**：当孩子点「📖 提示一句」时，系统从数据池里**筛出含主题字的诗句**，列出 **4 句候选**让他**点选**其中一句（点选即答对，并 TTS 朗读）。效果：**既教会他背这一句，又完全规避打字**——对 6 岁孩子，这是"把默写降级成选读"的自然入口。
2. **仍保留"自己打"（高年级进阶）**：手动输入与候选句并存；输入含主题字仍判命中（保留 v1.0）。
3. **友好计数**：候选句点选也计入命中数与连击（不让选命、不自惩自打两种玩法失衡）；「提示一句」每局限 2 次 `[待测试]`，用完可点"再玩一局"。
4. **TTS 引导**：题目"说一句带『月』的诗"自带 🔊 读题；答出后朗读全句。

> **为什么更好**：飞花令是 v1.0 判定"最像真游戏"的模式，但**纯打字输入对低龄是死穴**。候选句降级让它对全体小学生可用，甚至 6 岁也能靠"看线索选一句"参与，同时不牺牲高年级的打字挑战——**一把钥匙开两扇门**。

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
- v2.0 补强（🔧）：候选配**大图标作者头像占位**（用现有 emoji，如李白 🍶）、朝代色块（复用朝代配色），连线时**点起点到终点即连**（无需拖拽——拖拽对小屏幕孩子难），误连可一键撤销。
- 评分 / 星级参照 §4.1 模板，遵循全局 §7 反馈层。**作为 P2 可选**，不影响主线交付。

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
        // 干扰项按 roundNumber 升级迷惑度（v2.0 修订：等长推迟到冲刺轮）：
        //   round<3 → 随便挑一句（不要求同字数，字长明显不同）
        //   round 3-5 → 只挑与 answer 字长相近、肉眼可辨者
        //   round>=6 → 等长 且 与 given 首字相同者优先（压头字），并配语气词提示
        while options.size < 4:
            other = random poem (pool)
            line = random usable line of other
            if notUsed and not duplicate:
                if round>=6 and len(line) != len(answer): continue  // 等长约束（冲刺轮）
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

### 6.4 自适应排等（🔧 新增，v2.0）

v1.0 有难度分级（`QuizDifficulty`）但依赖玩家手动选。v2.0 加**自动排等**，保证"入门必过"：
- **首局默认 Easy（几乎零门槛）**：第一局不引入计时/长句/难干扰，就算闭眼点也大概率 1 星 → 建立"我能玩"的信心（对应 §5.2 入门段，且更刻意地保证首过）。
- **表现自适应**：记录最近 5 局成绩（`SharedPreferences` 键 `diff_profile`），正确率 `≥85%` → 升一档难度；`<50%` → 降一档。调整**只在局部生效**（如飞花令的候选句数量、对诗的干扰项迷惑度），不全局打扰。
- **`[待测试]`**：首局保证 1 星的成功率目标 > 95%；5 局内难度可达舒适的"恰好 2 星"区间。

---

## 7. 趣味性全局设计（v2.0 §2）

> 目标：v1.0 有"机制"，v2.0 要让它"好玩到会想再来一局"。核心手段是 **Juice（反馈层） + 即时正反馈 + 惊喜时刻**，全部复用现有资产（ConfettiView、View 动画、TTS、Vitals 图标与 emoji），零重资产外购。

### 7.1 统一「爽感反馈层」`GameFeedback`（🔧 新增全局组件）

设计一个**与具体游戏解耦的局内反馈层**，每个游戏都挂载同一套反馈语言：

| 事件 | 视觉 | 声音 | 动机（为什么对小学生的脑管用） |
|------|------|------|-------------------------------|
| **答对** | 得分数字**从答案处飞到左上角总分** + 该选项短暂高亮金/绿 + 火苗连击条 +1 | TTS 短读一次正确句尾（复用 `TtsManager`，语速 0.85x） | 数字"飞起来"制造物理性奖励感；连击条把孩子当"自己的进度"背在身上 |
| **连击 ≥3**（同一局内连续答对） | ConfettiView 二次触发（轻量 burst，复用现有粒子） + 火花 emoji（✨🔥）在连击条上 | 复用 `TtsManager` 一个开心的短促拟声（"真棒！"），或系统自带 `Toast` 音效 | 连击是可量化的即时奖励，孩子会为了"不断"而专注 |
| **答错** | 候选字进入"错误"的温柔抖动 + 变灰（**不闪红那种吓人的**，用柔和的暗色），随后自动给出正确答案并高亮 | 不判死，改用 TTS 轻声重读**带答案的完整句**（"没关系，是这句→…"），唤醒记忆 | 减少羞辱感，把"答错"转化为"多听一遍"的学习事件 |
| **本局完成** | 结算页启动（见 §9） | 触发 `MainActivity.celebrate()` Confetti 全屏 | 结算是闭环高潮 |

> **实现落点**：新建 `ui/widget/GameFeedback.java`（自绘小组件组：飞分 TextView 动画、连击条 View、进度环 View），复用在 4 个游戏 Fragment 顶部。动画全部用 `ValueAnimator`/`ObjectAnimator`（现有动画基础），**不新增任何图片素材**。

### 7.2 局内状态可视化：火苗连击条 + 星星进度环（🔧 新增组件）

- **火苗连击条 🔥**：局内顶部一根横向分段条，每次答对前进一格，连击满格时燃烧动画 + 额外积分。低龄孩子能直观看到"我连着答对了几道"。
- **星星进度环 ⭐**：环形进度（复用 Canvas 自绘能力，仿 `StatsBarChart`/`ConfettiView` 思路），显示本局已攒星数 / 目标星数。
- **复用资产**：`View` 自绘 + `colors.xml` 现有 `score_gold`、火焰橙 `emoji_color` 色，`ObjectAnimator`。零新素材。
- **为什么对孩子更好**：进度从"抽象数字"变成"看得见在增长的圆环和条"，唤起完成感的内在驱动（Achievement 心理学）；不依赖文字。

### 7.3 惊喜时刻（Juicy Surprises）—— 零挫败彩蛋（🔧 新增）

给核心游戏注入 1-2 个**纯增益、永不亏分**的随机彩蛋，制造"哇"时刻：

| 彩蛋 | 触发（`[待测试]`） | 效果 | 零挫败保证 |
|------|------------------|------|-----------|
| **🌙 月光祝福** | 每局至多 1-2 次，在某一轮开始时随机带"满月"角标 | 该轮答对 **×2 分**（金币"双倍"视觉） | 彩蛋出现前**不额外扣分**，未触发也不影响正常得分 → 无挫败 |
| **📜 稀有句彩卡** | 当本局遇到含"生僻字/名句"的诗（可用释义 88 首判定）时，结算页额外弹一张"名句卡" | 该句可一键收藏 / 再听 TTS / 分享 | 只是**多送**，不是条件成就，拿不到也不亏 |
| **🎨 主题闪一瞬** | 每 3 天登录首次进大厅时，主题卡短暂高亮（复用 ThemeManager） | 提示"你今天有主题在等你" | 周期性、非随机炫耀，不诱导 |

> **设计原则**：所有惊喜**只加不减**，绝不出现"彩蛋没触发就少 10 分"。这守住了第一支柱「零挫败挑战感」。

### 7.4 音效 / 节奏（🔧 轻量，复用 TTS + Toast）

- 不引入音频文件（约束）。正确/错误反馈用：
  - **正确短促**：`TtsManager.speak("太棒了！", QUEUE_FLUSH)`（~0.8s，不打断下一题输入）`[待测试]` 是否过长则改用无音。
  - **连击高潮**：复用 `ConfettiView` 的视觉 + 一条"叮"——若系统支持用 `AudioManager` 播放一个预置的短音失败，则**降级为纯视觉 + Toast**，不阻塞。
- **节奏控制**：每局结束强制 1.5s 结算动画"定住"成果，再出现"再来一局"大按钮，制造仪式感沉淀。

---

## 8. 易操作性全局设计（v2.0 §3）

> 目标：低龄孩子「不认字也能玩八成」「手小也点得准」「随时捡起来不迷路」。三句话：**能点不打字 / 颜色+图标双通道 / 状态永远可见**。

### 8.1 扫码式交互规范（🔧 全局标准，落地到每个 Fragment）

| 规范 | 具体标准 `[待测试]` | 复用资产 |
|------|-------------------|---------|
| **按钮 ≥ 56dp 触控区** | 所有可点元素最小触控高 56dp（Material touch target），点击目标就近（相邻按钮间距 ≥ 8dp，防误触） | Material Components `minHeight` / `TouchDelegate` |
| **文字按钮给图标** | 纯文本按钮 → 前缀 emoji/矢量图标（🔊 听一下、✅ 确认、↩️ 撤销、🔁 再来一局） | `emoji` 字段、现有质感 Icon（`@drawable` 矢量） |
| **误触可撤销** | 填空题点错候选字 → 无惩罚，撤销按钮 `↩️` 常驻；连线/翻牌点错 → 自动回到待选（无锁定惩罚） | 复用 `undoBlank`（现有） |
| **大字号 ≥ 16sp** | 题干与选项字号 ≥ 16sp（题面 18sp、选项 16-18sp、按钮 18sp）；长句**自动断行**（换行算法） | TextView `lineSpacingExtra` + `PinyinLineView` 折行思路 |
| **无多余操作** | 每局"开始前 1 屏说明图（图标+短句）→ 开始后绝无弹窗打断"；游戏中**禁任何需长按/双击**的手势 | — |
| **全屏防误触返回** | 游戏中按返回需"再确认一次"（否则极易误退让低龄孩子白玩） | Navigation + 二次确认 Dialog |

### 8.2 视觉引导：状态永远可见 + 双通道（🔧 全局）

- **顶部常驻状态栏**（复用 §7.2 反馈层）：**倒计时 / 已答对 N 题 / 还剩 N 题 / 当前星数**。
- **颜色 + 图标双通道**：
  - 可点：`🔵 蓝色 + 边框`；不可点（锁）：`⬜ 灰色 + 🔒`；已完成：`🟢 绿 + ✓`；当前：`🟡 黄 + 闪烁`。
  - 不依赖文字辨认状态——色盲/低龄孩子靠图形也能走流程。
- **对照 v1.0**：v1.0 只在结算给星级，v2.0 在**局内实时**显示进度环与连击条（🔧新增）。

### 8.3 难度自适应「首局排等 + 表现微调」（🔧 新增）

见 §6.4 自适应排等算法。要点：首局强制 Easy 必拿 1 星；最近 5 局正确率驱动升降档；调整只在局部生效。

### 8.4 读题辅助「一键听一下」（🔧 新增）

- **每个题干/候选句旁放 🔊 喇叭图标**，点击即 `TtsManager` 朗读该句（复用现有朗读）。
- 对低龄孩子：**自动读题**（进题后 1.5s 自动轻声朗读上句，`[待测试]` 关闭自动朗读的开关可设）。
- **不打断输入**：用 `QUEUE_FLUSH` 短读，不侵入答题进程——音量小、速度快。

### 8.5 容错保护：意外退出续局 + 清空二次确认（🔧 全量执行）

- **保留进度**：利用现有 `ViewModel` 存活（`GameViewModel`/各 Fragment 的子 `ViewModel`），旋转/暂退回时 `LiveData` 状态在；**彻底退出再进**：在 `SharedPreferences` 存"未完成局快照（gameType+当前进度+剩余题）"，下次进大厅提示"继续上次！"（`[待测试]`，50% 提议免费续）。
- **二次确认统一枚举**："重来""退出""清空记录"三类操作，全部走确认对话框（"真的重来吗？会丢掉这局的星星哦"）——守零挫败与低龄防误触。

---

## 9. 完成感与成就设计（v2.0 §4）

> 目标：让"玩完一局"本身就是一个完整、可收藏、分享得出去的小确幸。把 v1.0 偏"报告式"的结算改成"仪式式"的获奖时刻。

### 9.1 单局时长压缩：1.5-3 分钟（🔧 修订 v1.0）

| 游戏 | v1.0 单局 | v2.0 目标 | 怎么压 |
|------|----------|----------|-------|
| 对诗 | ≈ 2-3 分钟（7 轮） | **1.5-2.5 分钟** | 保留 7 轮但去掉空载等待，每题命中即秒进下一题 `[待测试]` 确认 7 轮 >= 1.5min 体验份量 |
| 连连看 | ≤ 90s | **≤ 60-90s（默认 75s）** | 缩短倒计时 + 首局无计时(自适应 §8.3) |
| 填空 | ≈ 2 分钟（5 题） | **≤ 2.5 分钟** | 5 题保留，点选缺字提速（§4.3） |
| 飞花令 | 60s | **45-60s** | `[待测试]` 低龄 45s 更易坚持完 |

> **理由**：小学生注意力窗口碎片化，"短而能完整玩完"比"长到能深钻"更容易建立"再来一局的冲动"。三分钟能一局 + 一个五分钟小憩能三局，正好对齐 §1.3 的"会话 ≥ 3 分钟"目标（再多局更稳）。

### 9.2 结算页可视化（🔧 新增专属结算视图）

每局结束统一走 `GameResult` → `GameSettlement`，结算页（新增 `SettlementView`，复用 `ConfettiView` + `ValueAnimator`）：

1. **大颗星（★★☆）**：1-3 星，出现时**逐个弹跳放大**（staggered scale-in），配简短音效/Toast。
2. **得分数字增大动画**：从 0 滚到实际分（`ValueAnimator`，唯一可信的数字，让"滚分"本身有爽感）。
3. **收藏本局最爱的 1 句**：结算页从 `touchedPoemIds` 里随机/按"出现过且最接近名句"挑一句，做成"本局最美的一句"卡片 → 可**一键加入收藏**（复用 `toggleFavorite`）/ **再听一遍**（`TtsManager`）/ **分享**（复用 `ShareCardGenerator`）。
4. **再来一局 / 回大厅**：两个 ≥ 56dp 大按钮，明确、近、无多余选项。

> **这也是学习沉淀钩子**：结算收藏即把该诗接入成长体系（§10.2 的"游戏→学习回流"在这里得到最自然的触达——不是强推，而是"把刚学的句子留下来"）。

### 9.3 成就/徽章「小目标化」（🔧 修订 v1.0 §7.3，合并为一个成就表）

v1.0 加了 3 个成就，偏"总量型"（累计）。v2.0 让成就**裂度更细、更早兑现**，覆盖"坚持来玩"而非只看技术。合并后的新增成就表（在 `AchievementEngine.ALL_ACHIEVEMENTS` 追加即可，不动现有 12 个，保证老用户不回退）：

| 新增成就 ID | 名称 | 条件 | 类型 |
|------------|------|------|------|
| `flyflower_20` | 飞花令主 | 飞花令累计命中 20 句 | 玩法（v1.0，保留） |
| `star_30` | 三星集邮家 | 累计获得 30 颗星（跨全部游戏） | 玩法（v1.0，保留） |
| `couplet_first_perfect` | 一气呵成 | 对诗一局全对 | 玩法（v1.0，保留） |
| `play_3_days` 👟 | 天天报到 | 连续 3 天各玩 ≥1 局游戏 | **坚持**（🔧新增，专治"靠任务而非兴趣"） |
| `play_7_days` 📅 | 七日诗虫 | 连续 7 天各玩 ≥1 局 | 坚持（🔧新增） |
| `star_first_3` ⭐ | 三连星 | 任一游戏首获 3 星 | 鼓励型（🔧新增，几乎白送，建立入手成功感） |
| `month_poem_collect` 🏷️ | 收藏达人·进阶 | 累计从"游戏结算最美一句"收藏 10 句 | **游戏→收藏回流**（🔧新增，把游玩和成长绑定） |

> **判定标准（修订 v1.0）**：新成就（含 v1.0 三项 + 新增坚持/鼓励类）发布后 1 个月内被 ≥12% 活跃用户各解锁 ≥1 个（加"坚持类"后预期易达成，因门槛低），且 `play_3_days` 覆盖 ≥15% 用户。

> **复用**：`AchievementEngine.ALL_ACHIEVEMENTS` 追加即可，不动现有 12 个；`checkAndUnlock` 检测时机不变。碎成就靠 `GameResult.stars` + 连续天数查询（现有 `DailyStats`/`LearningRecord` 数据已有）驱动。

---

## 10. 与成长体系（积分/等级/成就/主题/连续打卡）联动

### 10.1 统一结算器 `GameSettlement`（新增，替代三处重复）

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
    // 6) 可选：写最高分到 GameHistory（见 §11 Room 改动）
}
```
> **保守建议**：积分结算规则**不改变**（沿用 `LearningEngine.calcPointsForQuiz` 语义、`addTotalPoints` 原子写），只**集中调用**，避免动到已验证的成长曲线。若某游戏要加"完成即 +N"的保底分，也只在 `GameResult` 层面折算，不改 `LEVEL_THRESHOLDS`。

### 10.2 各游戏对成长环的贡献（确认现状已接通的 + 需补的）

| 成长点 | 现状是否接通 | 升级后 |
|--------|------------|--------|
| **每日任务「玩一局游戏」** | ✅ 由 `recordGameActivity → incrementGamesPlayed → hasGameToday` 接通 | 沿用，`GameSettlement` 统一触发 |
| **成就 `game_10`「游戏高手」** | ✅ `AchievementEngine.getGameCount` 用 `LearningRecord.gamePlayed` 累加 | 沿用，飞花令/作者连线也计入 |
| **成就 `quiz_perfect_5`「满分达人」** | ✅ `getPerfectQuizCountSync(10)` | 沿用，填空满分才计 |
| **等级积分** | ✅ `savePoints` / `submitAnswer` → `addTotalPoints` | 统一到 `GameSettlement` |
| **主题解锁** | ✅ `ThemeManager.syncUnlockedThemes` | 沿用 |
| **游戏专属最高分 / 历史** | ❌ 现状无 | **新增**：`GameHistory` 表（见 §11）|
| **游戏→学习回流（玩过的诗）** | ❌ `gamePlayed` 只计数，不回流学习 | **新增**：`touchedPoemIds` 关联（不强制"已学"，但可在大厅"本局遇到的诗"入口直达详情页）|

### 10.3 趣味化回流补强（🔧 新增，v2.0）

- **游戏→收藏回流**在结算页落地（§9.2），并新增成就 `month_poem_collect`；
- **连续"来玩"成就**（`play_3_days` / `play_7_days`）驱动每日任务式的坚持；
- **主题解锁即时反馈**：解锁新主题时大厅闪烁提示，把成长体系变成"玩游戏的惊喜彩蛋"之一。

---

## 11. 数据模型改动（Room）

### 11.1 新增 `game_history` 表（最克制的方案）
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

### 11.2 不改现有表
`learning_records` / `user_profile` / `daily_stats` **字段不动**（避免破坏已验证的 `MIGRATION` 与语义）。所有新增状态（当日去重、难度选择、星级累计）走：
- `SharedPreferences`（当日去重、最近难度、玩家偏好）——免迁移；
- `game_history`（历史/最高分）——一次性迁移。

> **决策依据**：宁可新表不上复杂 migration，也不改既有表结构降低回归风险。

---

## 12. 大厅升级（今日挑战 + 成绩回廊 + 低龄直读）

### 12.1 大厅：大图标 + 星级 + 今日角标（🔧 修订 v1.0 大厅升级）

v1.0 把 `GameHub` 升级为"今日挑战 + 成绩回廊"。v2.0 在此基础上让它**对 6 岁孩子更直白读取**：

- **大图标卡（≥ 88dp）**：每个游戏一张大卡，含 `emoji` 图标 + 名称大字（≥ 20sp）+ 一句"玩法一句话"。
- **星级回廊**：卡上直接显示**该游戏历史最高星（★ 1-3）**与**最高分**（来自 `GameHistory.getBestStarsByType` / `getBestScoreByType`）——孩子一眼看到"我还差一颗到 3 星"，不用点进去。
- **今日角标**：`DailyStats` 当日已玩 → 卡上 `✓ 今天玩过`；未玩且是"今日挑战"指定 → `🔥 今日挑战` 发光角标（优先推荐，拉动每日任务 `gamesPlayed`）。
- **最近有趣视线**：大厅顶部一句滚动/静态的"上次你最爱的是『飞花令：月』⭐⭐"（读 `GameHistory.getRecentByType` 最高记录），制造回访牵引。

### 12.2 第一眼层级（对不认字的孩子也成立）

```
大厅一眼信息（颜色+图形双通道）:
  │  哪个最好玩   → 星级最多的卡 + 大号 emoji（⭐ 数即"好玩度"）
  │  我上次得几星  → 卡上星星回廊
  │  今天该玩哪个  → 🔥 今日挑战角标（唯一高亮）
  │  难易提示     → 卡角难度色点（绿=入门/黄=进阶/红=挑战），对应自适应排等
```

---

## 13. 复用 vs 新增清单（合并去重）

### 13.1 机制层（v1.0 §9）

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

### 13.2 趣味/易操作层（v2.0 §8）

| v2.0 诉求 | 复用现状 | 需要新增 |
|-----------|---------|---------|
| 全局爽感反馈层 | `ValueAnimator`/`ObjectAnimator`、`ConfettiView`、`score_gold` 等色 | `ui/widget/GameFeedback.java`（飞分/连击条/进度环）|
| 结算可视化 | `ConfettiView`、`ValueAnimator`、`ShareCardGenerator`、`TtsManager`、`toggleFavorite` | `SettlementView`（大星/滚分/收藏最美一句）|
| 点选缺字填空 | `QuizFragment` Chip 交互、`undoBlank` | 把键盘输入替换为候选字 Chip 填入（改造 `QuizViewModel` + `fragment_quiz.xml`）|
| 对诗半句提示/意象标签 | `TtsManager`、`emoji`、`Poem.dynasty/author` | 半句提示逻辑、意象 emoji 标签映射 |
| 连连看短暂预览 | `MatchCardAdapter` 动画、`bg_card` 背面 | 预览高亮状态 + 时序（翻开两张先亮后归位）|
| 飞花令候选句降级 | `PoemRepository`、`TtsManager`、`String.contains` | 候选句筛选（含主题字的诗句池）+ 点选交互 |
| 自适应排等 | `SharedPreferences` | 难度档案（最近 5 局）+ 升降档逻辑 |
| 容错续局 | `GameViewModel`/子 ViewModel 存活 | 未完成快照持久化 + 「继续上次」入口 |
| 碎成就 | `AchievementEngine`、`DailyStatsDao`、`GameHistory` | `play_3_days`/`play_7_days`/`star_first_3`/`month_poem_collect` 追加 |
| 惊喜彩蛋 | 现有出题流程、ThemeManager | 月光祝福双倍、名句彩卡、主题闪提示逻辑 |

### 13.3 需**注意的重复实现**（建议在升级时顺手收敛）

- `QuizGenerator.generateCouplet()`（`QuizGenerator.java:142-176`）与 `GameEngine.generateCoupletGame()`（`GameEngine.java:61-107`）是**两套并行接龙实现**——统一保留 `GameEngine` 作为唯一接龙出题源，`QuizGenerator.generateCouplet` 标注 `@Deprecated` 或直接删除（确认无调用方后）。
- `GameViewModel.savePoints` 与 `QuizViewModel.submitAnswer` 的积分/成就代码重复——收敛至 `GameSettlement`。
- 本版**不新增第三套重复**。

---

## 14. 包结构 / 类职责建议

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
  ├─ GameHistory.java                 [新增] Room 实体（§11.1）
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

com.poetry.ui/widget/                 ← 新增趣味/易操作组件（v2.0）
  ├─ GameFeedback.java                [新增] 飞分/连击条/进度环 自绘反馈层（§7.1）
  └─ SettlementView.java              [新增] 结算页：大星/滚分/收藏最美一句（§9.2）

res/navigation/nav_graph.xml          [改] 新增 nav_game_flyflower（与不存在 nav_game_author 先占位）
res/layout/
  ├─ fragment_game_hub.xml             [改] 挑战区 + 最高分区 + 状态角标
  ├─ fragment_flyflower.xml            [新增]
  └─ item_match_card.xml               [改] 翻面背面子视图
strings.xml                            [改] 新增星级/挑战/飞花令相关文案
```

---

## 15. 工作量与里程碑（M1-M10 总表）

v1.0 里程碑 M1-M8 的**机制部分全部保留不变**。v2.0 在对应里程碑中**夹塞"趣味+易操作"改动**，并新增两个专门里程碑 M9 / M10。

| 里程碑 | 交付内容（v1.0 机制 + v2.0 趣味/易操作叠加） | 预估 | 验收可验证标准 |
|--------|--------------------------------------------|------|--------------|
| **M1 · 基建** | `PoemUtils` / `QuizDifficulty` / `GameResult` / `GameHistory` + DAO + v5 迁移 | 0.5 周 | `gradlew assembleDebug` 通过；老用户升级无数据丢失；单元测试覆盖 `PoemUtils` 过滤 |
| **M2 · 大厅升级 + 趣味层基建**（v1.0 M2 扩大） | 挑战区/最高分/今日角标（v1.0）**+** `GameFeedback` 组件 + 火苗连击条 + 星星进度环 + 结算页骨架（v2.0 §7/§9） | 1 周 | 大厅星级/今日角标可见；任意测试局有飞分/连击条/进度环动画；结算页有滚动加分+收藏句按钮 |
| **M3 · 对诗升级（趣味化）** | 7 轮 + 干扰项（🔧 等长推迟）+ 半对提示 + 半句提示+语音读题 + emoji 意象选项标签 + 星级 + 当日去重 | 1 周 | 手测：轮 1-2 干扰字长不同、轮 6-7 等长且带语气词辅助；半句提示一次点击可见首字；🔊 读题可触发；`[待测试]` 首局成功率 > 95% |
| **M4 · 连连看升级（趣味化）** | 随机联 + 翻开式 + 短暂预览 + 首局无计时(自适应) + 倒计时 + 星级 + 配对 TTS | 1 周 | 手测：翻开两张可短暂预览记忆；首局无倒计时、次局起 60-90s；配对成功读句 + 火苗 +1 |
| **M5 · 填空升级（点选化）**（🔧 大规模改交互） | 同音干扰 + 难度 + 按空计分（v1.0）**+ 点选缺字替代键盘 + 撤销 + 候选字数量分级 + 读题** | 1 周 | 手测：全程无软键盘弹出；点候选字填入 / 点空撤销；Easy=5 候选、Hard=8-10；答对 2/3 空有部分积分；60 秒完成 5 题无卡顿 |
| **M6 · 飞花令（降级化）** | `FlyflowerGameFragment` + `FlyflowerEngine`（v1.0）**+ 候选句点选模式 + 「提示一句」限 2 次 + 手动输入并存 + TTS 读题** | 1-1.5 周 | 手测：点「提示一句」出 4 句候选可点选命中并朗读；手动输入含字也判命中；45-60s 倒计时；`GameHistory` 入账 |
| **M7 · 结算+成就小目标化 + 回归** | 结算页落地（大星/滚分/收藏最美一句/分享）+ 追加坚持/鼓励成就（§9.3）+ 全量回归 + 联调 `GameSettlement` | 0.5 周 | 新成就可用；结算收藏句能进收藏并 TTS；历史 12 成就 + 主题 + 等级曲线全量回归无损坏；`[待测试]` 新成就 1 月解锁 ≥12% 活跃 |
| **M8（可选 P2）· 作者连线**（🔧 点对点连线） | `AuthorMatchFragment` + 大图标/朝代色块 + 点起点终点连线 + 撤销 | 1 周 | 独立验收；无障碍 contentDescription 覆盖 |
| **M9（🔧 新增）· 自适应排等 + 容错续局** | 首局排等 + 最近 5 局自适应升降档 + 未完成局快照/「继续上次」+ 清空重来二次确认 | 0.5-1 周 | 新用户首局强制 Easy 且能拿 ≥1 星；正确率驱动难度升降可测；中途退出再进有"继续上次"入口；重来有确认弹窗 |
| **M10（🔧 新增）· 结算音效/惊喜彩蛋 + 全局视觉规范** | 月光祝福（×2 分）/名句彩卡/主题闪 + 自动读题开关 + 56dp 触控区全量审计 + 双通道图标规范 | 0.5-1 周 | 彩蛋全程不出现"亏分"路径；触控区抽样 ≥ 40 个控件全部 ≥ 56dp；惊喜时刻手测不扣分 |

> **主线总工期**：约 **6.5-8 周**（单人全栈，因 M2/M3/M4/M5/M6 各夹入趣味/易操作工作量）。**建议顺序**：M1 → **M5（填空点选，最早让"打字难"消失）** → M2 → M3 → M4 → M6 → M7 → M9 → M10 → M8(P2)。
> **让最影响小学生的点优先落地**：填空点选化（M5）与结算反馈层（M2）排在前面，因为这两项分别解决"最难的操作"和"最大的爽感缺口"。

---

## 16. 风险与降级方案

### 16.1 机制层风险（v1.0 §12 全保留）

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

### 16.2 趣味/易操作层风险（v2.0 §9 补充，无推翻）

| 风险 | 影响 | 降级 / 对冲 |
|------|------|-----------|
| **点选缺字改交互的回归风险** | 填空是既有页面，改造可能影响现有玩家手感 | 保留"键盘输入"作为高难度/高年级选项（双输入并存），点选只作 Easy/Normal 默认；回归测试覆盖两种模式 |
| **翻开式短暂预览"太简单"**（帮助过度） | 把记忆游戏变成纯识别 | 预览时长 `[待测试]`，提供关/开开关（高年级关）；默认 0.8s，若判定过甜改 0.5s |
| **自动读题干扰专注** | TTS 与孩子自选可能重叠 | 自动读题做成可关；默认开但语速 0.85x、音量小 |
| **飞花令候选句"变填空"**（原意是自己背） | 门槛低了但可能削减挑战 | 候选句点选**计入命中但不算"自己造"**；设「提示一句」限 2 次 + 高分需少用提示；高年级可满键盘打 |
| **自适应难度误判**（几局表现波动） | 升降档过快让低龄忽难忽易 | 只统计最近 5 局，档位 `[待测试]`，首局强制 Easy，且在 Easy 达到 3 星才解锁升档 |
| **惊喜彩蛋被钻空/刷分** | ×2 分的月光祝福可能被反复机制刷 | 每局至多 1-2 次、种子基于 `GameResult` 随机（不因重开局重出）、不影响总分上限设定 |
| **结算收藏句质量不稳**（乱挑冷句） | 分享/收藏价值低 | 优先取释义 88 首中本局出现的句子；否则取较完整长句，兜底随机一句 + 二次确认引导不强制收藏 |
| **56dp 触控 + 大卡片增加滚动** | 小屏拥挤 | 触控区尽量布局为竖向滚动而非挤压；`TouchDelegate` 扩大命中不扩视觉 |

---

## 17. 可验证的判定标准汇总

### 17.1 机制层（v1.0 §13 全保留）

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

### 17.2 趣味/易操作层（v2.0 §10 增补）

| 目标 | 指标 | v2.0 达成线 |
|------|------|------------|
| **玩起来有趣（核心新增）** | 二次进入率 / 单会话局数 | 单会话 ≥ 2 局比例 ≥ 30%（埋点：`GameHistory.playedAt` 聚合）；"结束后立刻再来一局"点击率 ≥ 25% `[待测试]` |
| **操作门槛降（核心新增）** | 填空全程键盘弹出次数 | = 0（默认点选模式）；对诗/连连看/飞花令触控点中率 ≥ 95%（无长按/拖拽依赖）|
| **完成感** | 结算收藏句操作率 | 结算页「收藏本句」点击率 ≥ 40%；且其中 ≥20% 真正写入收藏 |
| **首局不挫败（核心新增）** | 新用户首个游戏成功率 | 首局拿 ≥1 星比例 ≥ 95%（自适应 Easy 兜底）|
| **坚持型成就触达** | `play_3_days` 解锁 | 发布 1 月内 ≥ 15% 活跃用户解锁 |

---

## 18. 总结

本方案没有引入任何后端、任何重型依赖、任何 Kotlin/Compose 迁移，**完全在现有 Java 8+ + View System + Material 3 体系中增量演进**。核心思路：

1. **收敛**：把三套重复的结算/过滤逻辑收为 `GameSettlement` + `PoemUtils`；
2. **升级**：对诗（干扰项 + 短局）、消消乐（随机联 + 翻开 + 计时）、填空（同音干扰 + 难度 + 点选化）各自补上"挑战性"与"反馈层级"；
3. **新增**：飞花令——唯一真正"像游戏"的限时玩法，素材零新增、落地成本最低、对小学生的吸引力最强；
4. **联动**：通过 `GameHistory` 成绩留痕 + `GameResult` 统一沉淀，把"玩游戏"与积分/成就/主题/每日任务/学习回流真正接通；
5. **让游戏"好玩到想再来"（趣味性）**：`GameFeedback` + 火苗连击条/星星进度环把抽象的分数变成"在增长、在发光"的进度；结算页大星滚动分值 + 收藏本局最美一句；月光祝福 ×2 分/名句彩卡等**只加不减**的惊喜时刻守住零挫败；
6. **让低龄"点得动、看得懂"（易操作性）**：全交互**点选优先、键盘后置**——填空点选缺字、飞花令候选句降级、连连看短暂预览、对诗给半句/意象/语音三重帮助；大按钮图标化、状态双通道常驻、首局自适应排等必拿 1 星、重来二次确认；
7. **让"玩完就有所得"（完成感与成就）**：单局压缩到 1.5-3 分钟、结算成为可收藏可分享的小确幸；成就小目标化（`play_3_days` / `star_first_3` / `month_poem_collect`），把"坚持来玩"也奖励掉。

全程**无一行 Kotlin/Compose，无后端，无图片/音效重素材外购**——Fun 靠复用 `ValueAnimator` + `ConfettiView` + `TTS` + `emoji` + 自绘 View，Accessibility 靠统一触控规范 + 图标双通道，Completion 靠复用 `AchievementEngine`/`ShareCardGenerator`/`toggleFavorite`。所有新阈值标 `[待测试]`，全部改动标注「🟢保留 / 🔧新增」，v1.0 的机制升级与收敛项完整继承。

> 交付按 §15 里程碑分批（M1→M10），填空点选化（M5）与反馈层（M2）优先——因为他们分别解决"最难的操作"与"最大的爽感缺口"，是让小学生"真玩得动、真想再玩"最快见效的两刀。
