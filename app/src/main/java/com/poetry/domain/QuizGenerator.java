package com.poetry.domain;

import com.poetry.data.model.Poem;
import com.poetry.util.PinyinHelper;

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
 * 1. 填空题（FillBlank）：随机挖掉诗句中的 1-3 个汉字，提供候选词（干扰项按难度策略：Easy 差异明显 / Normal+Hard 优先同音）
 * 2. 接龙题（Couplet）：给出上句，从 4 个选项中选下句
 * 3. 配对题（Matching）：上半句 ↔ 下半句配对
 */
public class QuizGenerator {

    private static final Random RANDOM = new Random();

    /** 兜底文化用字池：数据池采样不足时补足候选字（不再作为唯一来源） */
    private static final String[] FALLBACK_CHARS = {
        "天", "人", "山", "水", "月", "风", "云", "花", "春", "秋",
        "江", "河", "海", "日", "夜", "明", "白", "青", "金", "玉",
        "长", "高", "深", "远", "归", "行", "来", "去", "上", "下",
        "千", "万", "一", "三", "五", "大", "小", "新", "故", "寒"};

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
     * 生成一道填空题（默认普通难度，向后兼容）。
     *
     * @param poem 诗词数据源
     * @return 题目对象，poem.lines 不满足条件时返回 null
     */
    public static QuizQuestion generateFillBlank(Poem poem) {
        return generateFillBlank(poem, QuizDifficulty.NORMAL, Collections.emptyList());
    }

    /**
     * 生成一道填空题（指定难度，使用内置兜底字池）。
     *
     * @param poem       诗词数据源
     * @param difficulty 难度分级
     * @return 题目对象，poem.lines 不满足条件时返回 null
     */
    public static QuizQuestion generateFillBlank(Poem poem, QuizDifficulty difficulty) {
        return generateFillBlank(poem, difficulty, Collections.emptyList());
    }

    /**
     * 生成一道填空题（指定难度 + 数据池汉字采样）。
     *
     * <p>干扰项策略（GAME_REDESIGN_FINAL.md §4.3 / §6.3）：
     * <ul>
     *   <li>Easy：挖 1 空，干扰项优先取与答案意象/读音差异明显的字 → 好答；</li>
     *   <li>Normal：挖 1-2 空，优先取与答案同音或相近的字；</li>
     *   <li>Hard：挖 2-3 空，同音 + 结构相似干扰更多，答案多义字优先。</li>
     * </ul>
     * 候选字数量按难度分级：Easy=5 / Normal=8 / Hard=10（含正确答案）。
     *
     * @param poem         诗词数据源
     * @param difficulty   难度分级
     * @param distractorPool 从数据池采样的汉字池（用于生成真实干扰项；为空时退回内置兜底字池）
     * @return 题目对象，poem.lines 不满足条件时返回 null
     */
    public static QuizQuestion generateFillBlank(Poem poem, QuizDifficulty difficulty,
                                                 List<Character> distractorPool) {
        if (poem.lines == null || poem.lines.length == 0) return null;
        if (difficulty == null) difficulty = QuizDifficulty.NORMAL;

        QuizQuestion q = new QuizQuestion();
        q.poem = poem;
        q.originalLines = poem.lines.clone();
        q.blanks = new ArrayList<>();
        q.displayLines = new String[poem.lines.length];

        // 挖空数量按难度：Easy=1 / Normal=2 / Hard=3，且不超过行数
        int blankCount = Math.min(difficulty.maxBlanks, poem.lines.length);

        // 收集所有可挖的位置（排除过短的行和单字行）
        List<int[]> candidates = new ArrayList<>();
        for (int i = 0; i < poem.lines.length; i++) {
            String line = poem.lines[i];
            if (line != null && line.length() >= 4) {
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
        // 挖不出任何空（诗句全部过短）→ 返回 null，调用方跳过此题
        if (q.blanks.isEmpty()) return null;

        // 构建显示文本（用____代替空格）
        String[] display = poem.lines.clone();
        for (QuizQuestion.Blank b : q.blanks) {
            StringBuilder sb = new StringBuilder(display[b.lineIndex]);
            sb.replace(b.charIndex, b.charIndex + b.length, "____");
            display[b.lineIndex] = sb.toString();
        }
        q.displayLines = display;

        // 生成候选词列表（正确答案 + 干扰项，数量按难度分级）
        q.candidates = buildCandidates(blankAnswers, difficulty, distractorPool);
        Collections.shuffle(q.candidates, RANDOM);

        return q;
    }

    /**
     * 构建候选字列表：答案 + 按难度策略生成的干扰项。
     *
     * @param blankAnswers   挖空的正确答案集合
     * @param difficulty     难度分级
     * @param distractorPool 数据池采样汉字池（可为空）
     * @return 候选字列表，数量 = 难度对应的 candidateCount
     */
    private static List<String> buildCandidates(Set<String> blankAnswers, QuizDifficulty difficulty,
                                                List<Character> distractorPool) {
        Set<String> candidateSet = new LinkedHashSet<>(blankAnswers);
        int target = Math.max(difficulty.candidateCount, blankAnswers.size() + 1);

        // 可用干扰源：优先数据池采样，其次内置兜底字池
        List<Character> pool = new ArrayList<>();
        if (distractorPool != null) {
            pool.addAll(distractorPool);
        }
        List<String> fallback = new ArrayList<>();
        Collections.addAll(fallback, FALLBACK_CHARS);

        // 打乱干扰源顺序
        Collections.shuffle(pool, RANDOM);
        Collections.shuffle(fallback, RANDOM);

        // 先尝试数据池干扰（真实汉字），分两轮：
        // 第一轮优先策略字（Easy：差异明显；Normal/Hard：同音相近）
        // 第二轮补齐随机字
        for (int pass = 0; pass < 2 && candidateSet.size() < target; pass++) {
            for (char c : pool) {
                if (candidateSet.size() >= target) break;
                String s = String.valueOf(c);
                if (candidateSet.contains(s)) continue;
                boolean homophoneLike = isHomophoneLike(blankAnswers, c, difficulty);
                if (pass == 0) {
                    // 第一轮只取策略命中字
                    if (homophoneLike) candidateSet.add(s);
                } else {
                    // 第二轮补齐：Easy 取读音差异明显的字，其余随机
                    if (difficulty == QuizDifficulty.EASY && homophoneLike) continue;
                    candidateSet.add(s);
                }
            }
        }

        // 数据池不足时用兜底字池补齐（避免无限循环：只取非策略命中字，保证多样）
        Collections.shuffle(fallback, RANDOM);
        for (String s : fallback) {
            if (candidateSet.size() >= target) break;
            candidateSet.add(s);
        }

        // 极端兜底：若仍不足（极小概率），用数字字串补足
        int guard = 0;
        while (candidateSet.size() < target && guard++ < 100) {
            candidateSet.add("字" + guard);
        }

        List<String> result = new ArrayList<>(candidateSet);
        Collections.shuffle(result, RANDOM);
        return result;
    }

    /**
     * 判断候选字与答案是否构成"同音/相近"干扰（Easy 之外难度的策略字）。
     * 复用 PinyinHelper 拼音判定：完全同音（去声调后拼音一致）或首字母（声母）相同即算。
     *
     * @param blankAnswers 正确答案集合
     * @param c            候选字
     * @param difficulty   难度（Hard 放宽为声母相同也算，Easy 不启用同音策略）
     * @return true 若构成同音/相近干扰
     */
    private static boolean isHomophoneLike(Set<String> blankAnswers, char c, QuizDifficulty difficulty) {
        if (difficulty == QuizDifficulty.EASY) return false;
        String pinyinC = PinyinHelper.toTonePinyin(c);
        if (pinyinC == null || pinyinC.isEmpty()) return false;
        String normalizedC = normalizePinyin(pinyinC);
        if (normalizedC.isEmpty()) return false;

        for (String answer : blankAnswers) {
            if (answer == null || answer.isEmpty()) continue;
            String pinyinA = PinyinHelper.toTonePinyin(answer.charAt(0));
            if (pinyinA == null || pinyinA.isEmpty()) continue;
            String normalizedA = normalizePinyin(pinyinA);
            if (normalizedC.equals(normalizedA)) return true;             // 完全同音
            if (difficulty == QuizDifficulty.HARD
                    && normalizedC.charAt(0) == normalizedA.charAt(0)) {
                return true;                                              // Hard：声母相同即算相近
            }
        }
        return false;
    }

    /** 去掉拼音声调标记，用于同音判定（"月" yuè → "yue"）。 */
    private static String normalizePinyin(String pinyin) {
        if (pinyin == null || pinyin.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pinyin.length(); i++) {
            char c = pinyin.charAt(i);
            switch (c) {
                case 'ā': case 'á': case 'ǎ': case 'à': sb.append('a'); break;
                case 'ē': case 'é': case 'ě': case 'è': sb.append('e'); break;
                case 'ī': case 'í': case 'ǐ': case 'ì': sb.append('i'); break;
                case 'ō': case 'ó': case 'ǒ': case 'ò': sb.append('o'); break;
                case 'ū': case 'ú': case 'ǔ': case 'ù': sb.append('u'); break;
                case 'ǖ': case 'ǘ': case 'ǚ': case 'ǜ': sb.append('ü'); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
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
            // 空数组守卫：lines 为 null 或长度为 0 时跳过，避免 nextInt(0) 崩溃
            if (other.lines == null || other.lines.length == 0) continue;
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
