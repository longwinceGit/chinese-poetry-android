package com.poetry.domain;

import com.poetry.data.model.Poem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 题目生成器 —— 生成三种游戏题型的纯逻辑层。
 *
 * 支持题型：
 * 1. 填空题（FillBlank）：随机挖掉诗句中的 1-3 个汉字，提供候选词
 * 2. 接龙题（Couplet）：给出上句，从 4 个选项中选下句
 * 3. 配对题（Matching）：上半句 ↔ 下半句配对
 */
public class QuizGenerator {

    private static final Random RANDOM = new Random();

    /** 填空题题目结构 */
    public static class QuizQuestion {
        /** 原始诗句 */
        public String[] originalLines;
        /** 显示诗句（空格替换为 ____） */
        public String[] displayLines;
        /** 挖空位置列表 */
        public List<Blank> blanks;
        /** 候选字列表（含正确答案 + 干扰项） */
        public List<String> candidates;
        /** 所属诗词 */
        public Poem poem;

        /** 单个挖空位 */
        public static class Blank {
            /** 所在行索引 */
            public int lineIndex;
            /** 所在字符索引 */
            public int charIndex;
            /** 正确答案 */
            public String answer;
            /** 答案长度（扩展预留） */
            public int length;
        }
    }

    /**
     * 生成一道填空题。
     *
     * @param poem 诗词数据源
     * @return 题目对象，poem.lines 不满足条件时返回 null
     */
    public static QuizQuestion generateFillBlank(Poem poem) {
        if (poem.lines == null || poem.lines.length == 0) return null;

        QuizQuestion q = new QuizQuestion();
        q.poem = poem;
        q.originalLines = poem.lines.clone();
        q.blanks = new ArrayList<>();
        q.displayLines = new String[poem.lines.length];

        // 决定挖几个空（1-3个）
        int blankCount = Math.min(1 + RANDOM.nextInt(3), poem.lines.length);

        // 收集所有可挖的位置（排除过短的行和单字行）
        List<int[]> candidates = new ArrayList<>();
        for (int i = 0; i < poem.lines.length; i++) {
            String line = poem.lines[i];
            if (line.length() >= 4) {
                // 跳过前1个字符和后1个字符（保留首尾字提示）
                for (int j = 1; j < line.length() - 1; j++) {
                    if (Character.isLetterOrDigit(line.charAt(j))) {
                        candidates.add(new int[]{i, j});
                    }
                }
            }
        }

        // 随机选空位
        Collections.shuffle(candidates, RANDOM);
        Set<String> blankAnswers = new LinkedHashSet<>();
        for (int k = 0; k < blankCount && k < candidates.size(); k++) {
            int[] pos = candidates.get(k);
            QuizQuestion.Blank blank = new QuizQuestion.Blank();
            blank.lineIndex = pos[0];
            blank.charIndex = pos[1];
            blank.answer = String.valueOf(poem.lines[pos[0]].charAt(pos[1]));
            blank.length = 1;
            q.blanks.add(blank);
            blankAnswers.add(blank.answer);
        }

        // 构建显示文本（用____代替空格）
        String[] display = poem.lines.clone();
        for (QuizQuestion.Blank b : q.blanks) {
            StringBuilder sb = new StringBuilder(display[b.lineIndex]);
            sb.replace(b.charIndex, b.charIndex + b.length, "____");
            display[b.lineIndex] = sb.toString();
        }
        q.displayLines = display;

        // 生成候选词列表（正确答案 + 干扰项，最多6个）
        Set<String> candidateSet = new LinkedHashSet<>(blankAnswers);
        String[] distractors = {"天", "人", "山", "水", "月", "风", "云", "花", "春", "秋",
                                "江", "河", "海", "日", "夜", "明", "白", "青", "金", "玉",
                                "长", "高", "深", "远", "归", "行", "来", "去", "上", "下",
                                "千", "万", "一", "三", "五", "大", "小", "新", "故", "寒"};
        while (candidateSet.size() < Math.min(6, blankAnswers.size() + 4)) {
            String d = distractors[RANDOM.nextInt(distractors.length)];
            candidateSet.add(d);
        }
        q.candidates = new ArrayList<>(candidateSet);
        Collections.shuffle(q.candidates, RANDOM);

        return q;
    }

    /** 接龙题结构 */
    public static class CoupletQuestion {
        public String title;
        public String author;
        /** 给出的上句 */
        public String givenLine;
        /** 正确的下句 */
        public String correctNextLine;
        /** 选项列表（4个，含正确答案） */
        public List<String> options;
        /** 所属诗词 */
        public Poem poem;
    }

    /**
     * 从诗词池中随机生成一道接龙题。
     *
     * @param poemPool 诗词池
     * @return 接龙题，poemPool 为空或无足够诗句时返回 null
     */
    public static CoupletQuestion generateCouplet(List<Poem> poemPool) {
        if (poemPool == null || poemPool.isEmpty()) return null;

        Poem poem = poemPool.get(RANDOM.nextInt(poemPool.size()));
        if (poem.lines == null || poem.lines.length < 2) return null;

        CoupletQuestion q = new CoupletQuestion();
        q.poem = poem;
        q.title = poem.title;
        q.author = poem.author;

        // 选一对相邻句
        int pairIdx = RANDOM.nextInt(poem.lines.length - 1);
        q.givenLine = poem.lines[pairIdx];
        q.correctNextLine = poem.lines[pairIdx + 1];

        // 从其他诗中找错误选项（凑满4个）
        q.options = new ArrayList<>();
        q.options.add(q.correctNextLine);
        Set<Integer> used = new HashSet<>();
        used.add(pairIdx);
        used.add(pairIdx + 1);
        while (q.options.size() < 4) {
            Poem other = poemPool.get(RANDOM.nextInt(poemPool.size()));
            if (other.lines == null) continue;
            int idx = RANDOM.nextInt(other.lines.length);
            if (!used.contains(idx) && !q.options.contains(other.lines[idx])) {
                q.options.add(other.lines[idx]);
                used.add(idx);
            }
        }
        Collections.shuffle(q.options, RANDOM);
        return q;
    }

    /** 配对题：一对上下句 */
    public static class MatchPair {
        public String left;
        public String right;
        public int pairId;
    }

    /**
     * 生成 N 组配对题（每首取第一对相邻句）。
     *
     * @param poemPool  诗词池
     * @param pairCount 需要的配对数
     * @return 配对列表
     */
    public static List<MatchPair> generateMatchingPairs(List<Poem> poemPool, int pairCount) {
        List<MatchPair> pairs = new ArrayList<>();
        List<Poem> shuffled = new ArrayList<>(poemPool);
        Collections.shuffle(shuffled, RANDOM);

        int count = 0;
        for (Poem poem : shuffled) {
            if (poem.lines == null || poem.lines.length < 2) continue;
            MatchPair mp = new MatchPair();
            mp.left = poem.lines[0];
            mp.right = poem.lines[1];
            mp.pairId = count;
            pairs.add(mp);
            count++;
            if (count >= pairCount) break;
        }
        return pairs;
    }
}
