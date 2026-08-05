package com.poetry.domain;

import com.poetry.data.model.Poem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 诗词游戏引擎，提供三种游戏模式的核心逻辑。
 *
 * <p>支持的游戏模式：
 * <ol>
 *   <li><b>接龙模式</b>：给定诗句上句，从选项中选择正确下句</li>
 *   <li><b>消消乐模式</b>：配对诗词的上句和下句进行消除</li>
 *   <li><b>积分计算</b>：根据游戏表现计算得分</li>
 * </ol>
 *
 * <p>所有方法均为静态方法，可直接通过类名调用。
 */
public class GameEngine {

    /** 随机数生成器，用于随机选择诗句和打乱顺序 */
    private static final Random RANDOM = new Random();

    // ==================== 接龙模式 ====================

    /**
     * 接龙模式的一轮游戏数据。
     *
     * <p>包含该题所属的诗词、给出的上句、正确答案、选项列表以及轮次编号。
     */
    public static class CoupletRound {
        /** 该题所属的诗词对象 */
        public Poem poem;
        /** 给出的上句（需要接对的句子） */
        public String givenLine;
        /** 正确的下句答案 */
        public String correctAnswer;
        /** 供选择的答案列表（包含1个正确答案和3个干扰项） */
        public List<String> options;
        /** 当前轮次编号，从1开始 */
        public int roundNumber;
        /** 本回合是否存在与答案首字相同的干扰项（用于善意补偿 +2 分判定） */
        public boolean sameFirstChar;
        /** 意象 emoji 标签（可为 null），当存在与答案首字相同的干扰项时用于区分选项 */
        public String emojiTag;
        /** 半句提示首字（由正确答案去掉标点后的首字符） */
        public String hintFirstChar;
        /** 半句提示末字（由正确答案去掉标点后的末字符） */
        public String hintLastChar;
    }

    /**
     * 生成接龙模式游戏数据。
     *
     * <p>从诗词池中随机选取诗词，每首诗词随机选取一联（相邻两句），
     * 以上句作为题目，下句作为正确答案，并从其他诗词中抽取干扰项组成4个选项。
     *
     * @param pool   诗词池，从中选取题目
     * @param rounds 需要生成的轮次数
     * @return 接龙游戏 rounds 轮的题目列表，若池中可用诗词不足则少于 rounds 轮
     */
    public static List<CoupletRound> generateCoupletGame(List<Poem> pool, int rounds) {
        return generateCoupletGame(pool, rounds, 0);
    }

    /**
     * 生成接龙模式游戏数据（带难度梯度）。
     *
     * <p>干扰项难度梯度策略（M3 方案）：
     * <ul>
     *   <li>轮 1-2（前期）：干扰项尽量与答案<b>字长不同</b>（长度差 ≥2 或首字不同），
     *       用"长度 + 意象"保成功，降低前期挫败；</li>
     *   <li>轮 6-7（最后两轮，冲刺轮）：干扰项优先<b>等长</b>，且带语气词辅助，
     *       提升冲刺难度；</li>
     *   <li>中间轮：混合策略。</li>
     * </ul>
     * 干扰项一律排除省略号行（"…"）、空行、与题目/答案重复的句子。
     *
     * @param pool           诗词池，从中选取题目
     * @param rounds         需要生成的轮次数
     * @param difficultyPhase 难度阶段：0=自动按轮次推导，1=前期（长度不同优先），
     *                        2=冲刺（等长优先），其他值按轮次自动推导
     * @return 接龙游戏 rounds 轮的题目列表，若池中可用诗词不足则少于 rounds 轮
     */
    public static List<CoupletRound> generateCoupletGame(List<Poem> pool, int rounds, int difficultyPhase) {
        List<CoupletRound> game = new ArrayList<>();
        // 打乱诗词池顺序，确保每次生成的题目随机
        List<Poem> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, RANDOM);

        int found = 0;
        for (Poem poem : shuffled) {
            // 跳过诗句少于2句的诗词（无法形成对联）
            if (poem.lines == null || poem.lines.length < 2) continue;

            // 随机选取一联的起始索引（确保有下一句）
            int pairIdx = RANDOM.nextInt(poem.lines.length - 1);
            CoupletRound round = new CoupletRound();
            round.poem = poem;
            round.givenLine = poem.lines[pairIdx];      // 上句作为题目
            round.correctAnswer = poem.lines[pairIdx + 1]; // 下句作为正确答案
            round.roundNumber = found + 1;

            // 半句提示：取正确答案去掉标点后的首/末字符
            String cleanAnswer = stripPunctuation(round.correctAnswer);
            round.hintFirstChar = cleanAnswer.isEmpty() ? "" : String.valueOf(cleanAnswer.charAt(0));
            round.hintLastChar = cleanAnswer.isEmpty() ? "" : String.valueOf(cleanAnswer.charAt(cleanAnswer.length() - 1));

            round.options = new ArrayList<>();
            round.options.add(round.correctAnswer); // 加入正确答案

            // 用于去重，避免干扰项与题目或答案重复
            Set<Integer> used = new HashSet<>();
            used.add(pairIdx);
            used.add(pairIdx + 1);

            // 判定本回合难度阶段
            int phase = difficultyPhase;
            if (phase != 1 && phase != 2) {
                // 自动推导：最后两轮为冲刺轮，前两轮为前期轮
                if (found >= rounds - 2) {
                    phase = 2; // 冲刺轮：等长优先
                } else if (found < 2) {
                    phase = 1; // 前期轮：长度不同优先
                } else {
                    phase = 0; // 中间轮：混合
                }
            }

            // 从诗词池中抽取干扰项，凑满4个选项
            while (round.options.size() < 4) {
                String distractor = pickDistractor(pool, round, used, phase);
                if (distractor == null) break; // 池中无更多可用干扰项，提前结束本轮
                round.options.add(distractor);
            }
            // 打乱选项顺序，使正确答案位置随机
            Collections.shuffle(round.options, RANDOM);

            // 判定是否存在与答案首字相同的干扰项（用于善意补偿 +2 分）
            round.sameFirstChar = hasSameFirstCharDistractor(round);
            // 意象 emoji 标签：由正确答案按字面简单映射
            round.emojiTag = mapEmojiTag(round.correctAnswer);

            game.add(round);
            found++;
            if (found >= rounds) break;
        }
        return game;
    }

    /**
     * 从诗词池中按难度阶段抽取一个干扰项。
     *
     * @param pool  诗词池
     * @param round 当前回合（含正确答案、题目、已用索引）
     * @param used  已用诗句索引集合（去重）
     * @param phase 难度阶段：1=长度不同优先，2=等长优先，其他=混合
     * @return 选中的干扰项句子，无可用时返回 null
     */
    private static String pickDistractor(List<Poem> pool, CoupletRound round,
                                         Set<Integer> used, int phase) {
        int answerLen = stripPunctuation(round.correctAnswer).length();
        // 先按阶段偏好收集候选
        List<String> preferred = new ArrayList<>();
        List<String> fallback = new ArrayList<>();

        for (int attempt = 0; attempt < 40; attempt++) {
            Poem other = pool.get(RANDOM.nextInt(pool.size()));
            if (other.lines == null || other.lines.length == 0) continue;
            int idx = RANDOM.nextInt(other.lines.length);
            String line = other.lines[idx];
            // 排除省略号行、空行、与题目/答案重复、已用索引
            if (line == null || line.isEmpty()) continue;
            if (line.contains("…")) continue;
            if (used.contains(idx)) continue;
            if (round.options.contains(line)) continue;
            if (line.equals(round.givenLine)) continue;

            int lineLen = stripPunctuation(line).length();
            boolean sameLen = lineLen == answerLen;
            boolean diffLen = Math.abs(lineLen - answerLen) >= 2
                    || !firstChar(line).equals(firstChar(round.correctAnswer));

            if (phase == 1 && diffLen) {
                preferred.add(line);
            } else if (phase == 2 && sameLen) {
                preferred.add(line);
            } else if (phase == 0) {
                // 混合：随机一半概率取等长，一半取不同长
                if (RANDOM.nextBoolean() ? sameLen : diffLen) {
                    preferred.add(line);
                } else {
                    fallback.add(line);
                }
            } else {
                fallback.add(line);
            }
            if (preferred.size() >= 1) break;
        }

        if (!preferred.isEmpty()) {
            String chosen = preferred.get(RANDOM.nextInt(preferred.size()));
            markUsed(pool, round, used, chosen);
            return chosen;
        }
        if (!fallback.isEmpty()) {
            String chosen = fallback.get(RANDOM.nextInt(fallback.size()));
            markUsed(pool, round, used, chosen);
            return chosen;
        }
        return null;
    }

    /** 将选中的干扰项句子标记为已用（按内容匹配索引）。 */
    private static void markUsed(List<Poem> pool, CoupletRound round, Set<Integer> used, String line) {
        for (Poem p : pool) {
            if (p.lines == null) continue;
            for (int i = 0; i < p.lines.length; i++) {
                if (p.lines[i].equals(line)) {
                    used.add(i);
                    return;
                }
            }
        }
    }

    /** 判断是否存在与答案首字相同的干扰项（非答案本体）。 */
    private static boolean hasSameFirstCharDistractor(CoupletRound round) {
        String answerFirst = firstChar(round.correctAnswer);
        if (answerFirst.isEmpty()) return false;
        for (String opt : round.options) {
            if (opt.equals(round.correctAnswer)) continue;
            if (firstChar(opt).equals(answerFirst)) return true;
        }
        return false;
    }

    /** 按字面简单映射意象 emoji：含"月"→🌙、含"花/春"→🌸、含"雪/冬/寒"→❄️、其他→null。 */
    private static String mapEmojiTag(String answer) {
        if (answer == null) return null;
        if (answer.contains("月")) return "🌙";
        if (answer.contains("花") || answer.contains("春")) return "🌸";
        if (answer.contains("雪") || answer.contains("冬") || answer.contains("寒")) return "❄️";
        return null;
    }

    /** 去掉标点符号，返回纯汉字/字符。 */
    private static String stripPunctuation(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isIdeographic(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 取句子去掉标点后的首字符。 */
    private static String firstChar(String s) {
        String clean = stripPunctuation(s);
        return clean.isEmpty() ? "" : String.valueOf(clean.charAt(0));
    }

    // ==================== 消消乐模式（诗词对句配对消除） ====================

    /**
     * 消消乐模式的卡片数据。
     *
     * <p>每张卡片代表一句诗，需要与它的配对句（同一联的上下句）进行匹配消除。
     */
    public static class MatchCard {
        /** 诗句文本内容 */
        public String text;
        /** 所属配对组ID，同一首诗的上句和下句具有相同 pairId */
        public int pairId;
        /** 是否为上句：true=上句，false=下句 */
        public boolean isFirstHalf;
        /** 是否已消除（配对成功） */
        public boolean matched;
        /** 当前是否被选中（高亮显示） */
        public boolean selected;
        /** 所属诗词的标题 */
        public String poemTitle;
        /** 所属诗词的作者 */
        public String poemAuthor;
    }

    /**
     * 消消乐游戏的完整数据。
     *
     * <p>包含所有卡片列表、总配对数，以及配对成功时展示的诗词信息。
     */
    public static class MatchGame {
        /** 所有卡片列表 */
        public List<MatchCard> cards;
        /** 需要配对的总对数 */
        public int totalPairs;
        /** pairId -> "标题 - 作者" 的映射，用于配对成功时展示诗词信息 */
        public Map<Integer, String> poemInfo;
    }

    /**
     * 生成消消乐游戏：选取 N 首诗词，每首取上句+下句共 2N 张卡片，全部打乱排列。
     * 所有卡片正面可见，玩家需要找到上句和下句配对消除。
     *
     * @param pool  诗词池，从中选取题目
     * @param pairs 需要配对的诗词首数（即卡片对数）
     * @return 消消乐游戏数据，包含打乱后的卡片列表
     */
    public static MatchGame generateMatchGame(List<Poem> pool, int pairs) {
        MatchGame game = new MatchGame();
        game.cards = new ArrayList<>();
        game.totalPairs = pairs;
        game.poemInfo = new HashMap<>();

        // 打乱诗词池顺序，确保每次生成的卡片组合随机
        List<Poem> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, RANDOM);

        int count = 0;
        for (Poem poem : shuffled) {
            // 跳过诗句少于2句的诗词
            if (poem.lines == null || poem.lines.length < 2) continue;

            // 收集所有合法的相邻联起始索引 i：
            // lines[i] 与 lines[i+1] 都存在、长度均 >= 2、且都不含省略号"…"
            List<Integer> validIdx = new ArrayList<>();
            for (int i = 0; i + 1 < poem.lines.length; i++) {
                String up = poem.lines[i];
                String down = poem.lines[i + 1];
                if (up == null || down == null) continue;
                if (up.length() < 2 || down.length() < 2) continue;
                if (up.contains("…") || down.contains("…")) continue;
                validIdx.add(i);
            }
            if (validIdx.isEmpty()) continue;

            // 随机选取一联（不再强制 lines[0]/lines[1]）
            int pairIdx = validIdx.get(RANDOM.nextInt(validIdx.size()));

            // 创建上句卡片
            MatchCard first = new MatchCard();
            first.text = poem.lines[pairIdx];
            first.pairId = count;
            first.isFirstHalf = true;
            first.matched = false;
            first.selected = false;
            first.poemTitle = poem.title;
            first.poemAuthor = poem.author;

            // 创建下句卡片
            MatchCard second = new MatchCard();
            second.text = poem.lines[pairIdx + 1];
            second.pairId = count;
            second.isFirstHalf = false;
            second.matched = false;
            second.selected = false;
            second.poemTitle = poem.title;
            second.poemAuthor = poem.author;

            game.cards.add(first);
            game.cards.add(second);
            // 记录配对ID对应的诗词信息，用于配对成功时展示
            game.poemInfo.put(count, poem.title + " · " + poem.author);
            count++;
            if (count >= pairs) break;
        }

        // 打乱所有卡片顺序，使配对卡片位置随机分布
        Collections.shuffle(game.cards, RANDOM);
        return game;
    }

    /**
     * 检查两张卡片是否配对成功：同一 pairId 且分别为上句/下句。
     *
     * @param a 第一张卡片
     * @param b 第二张卡片
     * @return 两张卡片配对成功返回 true，否则返回 false
     */
    public static boolean checkMatch(MatchCard a, MatchCard b) {
        // 同一张卡片不能配对
        if (a == b) return false;
        // 配对条件：pairId相同且一个是上句、一个是下句
        return a.pairId == b.pairId && a.isFirstHalf != b.isFirstHalf;
    }

    /**
     * 判断游戏是否完成（所有卡片均已消除）。
     *
     * @param game 消消乐游戏数据
     * @return 所有卡片均已匹配消除返回 true，否则返回 false
     */
    public static boolean isGameComplete(MatchGame game) {
        for (MatchCard c : game.cards) {
            if (!c.matched) return false;
        }
        return true;
    }

    // ==================== 积分计算 ====================

    /**
     * 计算接龙模式的得分（旧签名，兼容其他调用）。
     *
     * <p>委托新签名 {@link #calcCoupletScore(int, boolean, int, boolean)}，
     * 其中 pickedSameFirstChar 固定为 false（无首字相同补偿）。
     *
     * @param roundNumber  当前轮次编号（保留参数，可用于扩展）
     * @param correct      是否答对
     * @param streakBonus  当前连击数，用于计算连击奖励分
     * @return 本轮得分
     */
    public static int calcCoupletScore(int roundNumber, boolean correct, int streakBonus) {
        return calcCoupletScore(roundNumber, correct, streakBonus, false);
    }

    /**
     * 计算接龙模式的得分（M3 新公式）。
     *
     * <p>单轮得分 = (答对 ? 12 : 0) + (答对 ? combo : 0) + (选中与答案首字相同的干扰项 ? 2 : 0)。
     * 其中 combo = 连续答对数。满分参考 7 轮全对 = 12*7 + (1+2+3+4+5+6+7) = 112。
     *
     * @param roundNumber        当前轮次编号（保留参数，可用于扩展）
     * @param correct            是否答对
     * @param streak             当前连击数（连续答对数）
     * @param pickedSameFirstChar 是否选中了与答案首字相同但非答案本体的干扰项（善意补偿 +2）
     * @return 本轮得分
     */
    public static int calcCoupletScore(int roundNumber, boolean correct, int streak, boolean pickedSameFirstChar) {
        int base = correct ? 12 : 0;
        int combo = correct ? streak : 0;
        int closeGuess = pickedSameFirstChar ? 2 : 0;
        return base + combo + closeGuess;
    }

    /**
     * 根据总分换算接龙星级（M3 阈值，待测试）。
     *
     * @param score 本局总分
     * @return 星级：score ≥ 84 → 3 星；≥ 56 → 2 星；否则 → 1 星
     */
    public static int calcCoupletStars(int score) {
        if (score >= 84) return 3;
        if (score >= 56) return 2;
        return 1;
    }

    /**
     * 计算消消乐模式的得分（M4 新公式）。
     *
     * <p>得分 = 40 * 已配对对数 + 时间奖励（限时内完成时），保底 40 分。
     * 时间奖励 = min((timeLimitSeconds - usedSeconds) * 3, 300)，仅在限时内完成时计入。</p>
     *
     * @param attempts          实际尝试次数（点击配对的次数）
     * @param totalPairs        需要配对的总对数
     * @param matchedPairs      已成功配对的对数
     * @param usedSeconds       本局已用秒数
     * @param timeLimitSeconds  限时秒数（0 表示不限时）
     * @param completedInTime   是否在限时内完成
     * @return 消消乐模式得分，保底 40 分
     */
    public static int calcMatchScore(int attempts, int totalPairs, int matchedPairs,
                                     long usedSeconds, int timeLimitSeconds, boolean completedInTime) {
        int base = 40 * matchedPairs;
        int timeBonus = (completedInTime && timeLimitSeconds > 0)
                ? Math.min((int) ((timeLimitSeconds - usedSeconds) * 3), 300) : 0;
        return Math.max(40, base + timeBonus);   // 保底 40
    }

    /**
     * 计算消消乐模式的星级（M4）。
     *
     * <p>未完成时：有配对成功 → 1 星，否则 0 星。
     * 完成时：≤7 次尝试 → 3 星；≤9 次 → 2 星；否则 → 1 星。</p>
     *
     * @param attempts     实际尝试次数
     * @param totalPairs   需要配对的总对数
     * @param completed    是否完成游戏
     * @param matchedPairs 已成功配对的对数
     * @return 星级 0..3
     */
    public static int calcMatchStars(int attempts, int totalPairs, boolean completed, int matchedPairs) {
        if (!completed) return matchedPairs > 0 ? 1 : 0;
        if (attempts <= 7) return 3;   // ≤7 次尝试完成 → 3星
        if (attempts <= 9) return 2;   // ≤9 → 2星
        return 1;
    }
}
