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

            round.options = new ArrayList<>();
            round.options.add(round.correctAnswer); // 加入正确答案

            // 用于去重，避免干扰项与题目或答案重复
            Set<Integer> used = new HashSet<>();
            used.add(pairIdx);
            used.add(pairIdx + 1);

            // 从诗词池中随机抽取干扰项，凑满4个选项
            while (round.options.size() < 4) {
                Poem other = pool.get(RANDOM.nextInt(pool.size()));
                if (other.lines == null) continue;
                int idx = RANDOM.nextInt(other.lines.length);
                // 确保干扰项不重复且未被使用
                if (!used.contains(idx) && !round.options.contains(other.lines[idx])) {
                    round.options.add(other.lines[idx]);
                    used.add(idx);
                }
            }
            // 打乱选项顺序，使正确答案位置随机
            Collections.shuffle(round.options, RANDOM);
            game.add(round);
            found++;
            if (found >= rounds) break;
        }
        return game;
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
            // 跳过太短或含省略号的行（避免显示异常）
            if (poem.lines[0].length() < 2 || poem.lines[1].length() < 2) continue;
            if (poem.lines[0].contains("…") || poem.lines[1].contains("…")) continue;

            // 创建上句卡片
            MatchCard first = new MatchCard();
            first.text = poem.lines[0];
            first.pairId = count;
            first.isFirstHalf = true;
            first.matched = false;
            first.selected = false;
            first.poemTitle = poem.title;
            first.poemAuthor = poem.author;

            // 创建下句卡片
            MatchCard second = new MatchCard();
            second.text = poem.lines[1];
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
     * 计算接龙模式的得分。
     *
     * <p>得分由基础分和连击奖励分组成：
     * <ul>
     *   <li>答对：基础分 10 分 + 连击奖励（连击数 × 2）</li>
     *   <li>答错：0 分</li>
     * </ul>
     *
     * @param roundNumber  当前轮次编号（保留参数，可用于扩展）
     * @param correct      是否答对
     * @param streakBonus  当前连击数，用于计算连击奖励分
     * @return 本轮得分
     */
    public static int calcCoupletScore(int roundNumber, boolean correct, int streakBonus) {
        int base = correct ? 10 : 0;
        int streak = correct ? streakBonus * 2 : 0;
        return base + streak;
    }

    /**
     * 计算消消乐模式的得分。
     *
     * <p>得分逻辑：理想情况下 totalPairs 次尝试即可完成（每次都对），
     * 实际尝试次数越多，扣分越多。基础分 50 分，每次额外尝试扣 3 分，
     * 最低不低于 5 分。
     *
     * @param attempts    实际尝试次数（点击配对的次数）
     * @param totalPairs  需要配对的总对数
     * @return 消消乐模式得分，最低 5 分
     */
    public static int calcMatchScore(int attempts, int totalPairs) {
        // 理想次数 = totalPairs（每次都对），实际次数越多分越低
        int ideal = totalPairs;
        int base = 50;
        int penalty = Math.max(0, attempts - ideal) * 3;
        return Math.max(5, base - penalty);
    }
}
