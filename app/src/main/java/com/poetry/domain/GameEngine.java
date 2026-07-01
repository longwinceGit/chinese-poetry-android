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

public class GameEngine {

    private static final Random RANDOM = new Random();

    // ==================== 接龙模式 ====================

    public static class CoupletRound {
        public Poem poem;
        public String givenLine;
        public String correctAnswer;
        public List<String> options;
        public int roundNumber;
    }

    public static List<CoupletRound> generateCoupletGame(List<Poem> pool, int rounds) {
        List<CoupletRound> game = new ArrayList<>();
        List<Poem> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, RANDOM);

        int found = 0;
        for (Poem poem : shuffled) {
            if (poem.lines == null || poem.lines.length < 2) continue;

            int pairIdx = RANDOM.nextInt(poem.lines.length - 1);
            CoupletRound round = new CoupletRound();
            round.poem = poem;
            round.givenLine = poem.lines[pairIdx];
            round.correctAnswer = poem.lines[pairIdx + 1];
            round.roundNumber = found + 1;

            round.options = new ArrayList<>();
            round.options.add(round.correctAnswer);
            Set<Integer> used = new HashSet<>();
            used.add(pairIdx);
            used.add(pairIdx + 1);
            while (round.options.size() < 4) {
                Poem other = pool.get(RANDOM.nextInt(pool.size()));
                if (other.lines == null) continue;
                int idx = RANDOM.nextInt(other.lines.length);
                if (!used.contains(idx) && !round.options.contains(other.lines[idx])) {
                    round.options.add(other.lines[idx]);
                    used.add(idx);
                }
            }
            Collections.shuffle(round.options, RANDOM);
            game.add(round);
            found++;
            if (found >= rounds) break;
        }
        return game;
    }

    // ==================== 消消乐模式（诗词对句配对消除） ====================

    public static class MatchCard {
        public String text;          // 诗句文本
        public int pairId;           // 所属配对组（同一首诗）
        public boolean isFirstHalf;  // true=上句, false=下句
        public boolean matched;      // 已消除
        public boolean selected;     // 当前被选中（高亮）
        public String poemTitle;     // 所属诗词标题
        public String poemAuthor;    // 所属诗词作者
    }

    public static class MatchGame {
        public List<MatchCard> cards;
        public int totalPairs;
        /** pairId -> "标题 - 作者" 的映射，用于配对成功时展示 */
        public Map<Integer, String> poemInfo;
    }

    /**
     * 生成消消乐游戏：选取 N 首诗词，每首取上句+下句共 2N 张卡片，全部打乱排列。
     * 所有卡片正面可见，玩家需要找到上句和下句配对消除。
     */
    public static MatchGame generateMatchGame(List<Poem> pool, int pairs) {
        MatchGame game = new MatchGame();
        game.cards = new ArrayList<>();
        game.totalPairs = pairs;
        game.poemInfo = new HashMap<>();

        List<Poem> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, RANDOM);

        int count = 0;
        for (Poem poem : shuffled) {
            if (poem.lines == null || poem.lines.length < 2) continue;
            // 跳过太短或含省略号的行
            if (poem.lines[0].length() < 2 || poem.lines[1].length() < 2) continue;
            if (poem.lines[0].contains("…") || poem.lines[1].contains("…")) continue;

            // 上句卡片
            MatchCard first = new MatchCard();
            first.text = poem.lines[0];
            first.pairId = count;
            first.isFirstHalf = true;
            first.matched = false;
            first.selected = false;
            first.poemTitle = poem.title;
            first.poemAuthor = poem.author;

            // 下句卡片
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
            game.poemInfo.put(count, poem.title + " · " + poem.author);
            count++;
            if (count >= pairs) break;
        }

        // 打乱所有卡片顺序
        Collections.shuffle(game.cards, RANDOM);
        return game;
    }

    /**
     * 检查两张卡片是否配对成功：同一 pairId 且分别为上句/下句
     */
    public static boolean checkMatch(MatchCard a, MatchCard b) {
        if (a == b) return false;
        return a.pairId == b.pairId && a.isFirstHalf != b.isFirstHalf;
    }

    /**
     * 判断游戏是否完成（所有卡片均已消除）
     */
    public static boolean isGameComplete(MatchGame game) {
        for (MatchCard c : game.cards) {
            if (!c.matched) return false;
        }
        return true;
    }

    // ==================== 积分计算 ====================

    public static int calcCoupletScore(int roundNumber, boolean correct, int streakBonus) {
        int base = correct ? 10 : 0;
        int streak = correct ? streakBonus * 2 : 0;
        return base + streak;
    }

    public static int calcMatchScore(int attempts, int totalPairs) {
        // 理想次数 = totalPairs（每次都对），实际次数越多分越低
        int ideal = totalPairs;
        int base = 50;
        int penalty = Math.max(0, attempts - ideal) * 3;
        return Math.max(5, base - penalty);
    }
}
