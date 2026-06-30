package com.poetry.domain;

import com.poetry.data.model.Poem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

            // 生成4个选项
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

    // ==================== 配对模式 ====================

    public static class MatchCard {
        public String text;
        public int pairId;
        public boolean isLeft;
        public boolean matched;
    }

    public static class MatchGame {
        public List<MatchCard> cards;
        public int totalPairs;
    }

    public static MatchGame generateMatchGame(List<Poem> pool, int pairs) {
        MatchGame game = new MatchGame();
        game.cards = new ArrayList<>();
        game.totalPairs = pairs;

        List<Poem> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, RANDOM);

        int count = 0;
        for (Poem poem : shuffled) {
            if (poem.lines == null || poem.lines.length < 2) continue;
            if (poem.lines[0].length() < 3 || poem.lines[1].length() < 3) continue;
            if (poem.lines[0].contains("……") || poem.lines[1].contains("……")) continue;

            MatchCard left = new MatchCard();
            left.text = poem.lines[0];
            left.pairId = count;
            left.isLeft = true;
            left.matched = false;

            MatchCard right = new MatchCard();
            right.text = poem.lines[1];
            right.pairId = count;
            right.isLeft = false;
            right.matched = false;

            game.cards.add(left);
            game.cards.add(right);
            count++;
            if (count >= pairs) break;
        }

        Collections.shuffle(game.cards, RANDOM);
        return game;
    }

    public static boolean checkMatch(MatchGame game, MatchCard a, MatchCard b) {
        if (a == b) return false;
        if (a.pairId == b.pairId && a.isLeft != b.isLeft) {
            a.matched = true;
            b.matched = true;
            return true;
        }
        return false;
    }

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

    public static int calcMatchScore(int attempts) {
        return Math.max(5, 30 - attempts);
    }
}
