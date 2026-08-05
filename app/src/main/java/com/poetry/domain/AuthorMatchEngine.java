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
 * 作者连线（Author Match）游戏引擎 —— M8（P2 可选、低龄友好）。
 *
 * <p>玩法（GAME_REDESIGN_FINAL.md §4.5）：三列卡片 —— <b>作者 → 代表作一句 → 朝代</b>，
 * 点选正确作者与其一句代表作构成连线。数据全部来自现有
 * {@code Poem.author} / {@code Poem.title} / {@code Poem.lines[0]} / {@code Poem.dynasty}，
 * 零新数据。</p>
 *
 * <p>v2.0 补强：候选配<b>大图标作者头像占位</b>（现有 emoji，如李白 🍶）、
 * <b>朝代色块</b>（复用朝代配色，UI 层按 {@link Poem#tag} 映射 {@code R.color.tag_*}）；
 * 连线时<b>点起点到终点即连</b>（无需拖拽），误连可一键撤销（Fragment 侧）。
 * 评分 / 星级参照 §4.1 接龙模板：单轮 = (答对 ? 12 + combo : 0)，combo = 连续答对数。</p>
 *
 * <p>所有方法均为静态方法，纯 Java，无 Android 依赖。</p>
 */
public final class AuthorMatchEngine {

    private AuthorMatchEngine() {
        // 工具类，禁止实例化
    }

    /** 随机数生成器 */
    private static final Random RANDOM = new Random();

    /** 每局默认轮数（题数） */
    public static final int DEFAULT_ROUNDS = 5;

    /** 每轮候选作者数（1 个正确答案 + 3 个干扰） */
    public static final int CANDIDATES = 4;

    /**
     * 常见诗人 → 头像 emoji（§4.5 "用现有 emoji，如李白 🍶"）。
     * <p>未收录的作者回退到 {@code Poem.emoji}，再回退 📜。</p>
     */
    private static final Map<String, String> AUTHOR_EMOJI = buildAuthorEmoji();

    private static Map<String, String> buildAuthorEmoji() {
        Map<String, String> map = new HashMap<>();
        map.put("李白", "🍶");
        map.put("杜甫", "🌾");
        map.put("白居易", "🍚");
        map.put("苏轼", "🌊");
        map.put("辛弃疾", "⚔️");
        map.put("李清照", "🌸");
        map.put("王维", "🖌️");
        map.put("孟浩然", "🏔️");
        map.put("陶渊明", "🏡");
        map.put("屈原", "🌊");
        map.put("杜牧", "🍁");
        map.put("李商隐", "🕯️");
        map.put("陆游", "🐎");
        map.put("杨万里", "🌾");
        map.put("王安石", "🏛️");
        map.put("欧阳修", "📖");
        map.put("范仲淹", "🏰");
        map.put("柳宗元", "🎣");
        map.put("刘禹锡", "⛰️");
        map.put("韩愈", "📚");
        map.put("王之涣", "🌅");
        map.put("王昌龄", "🏹");
        map.put("高适", "🐫");
        map.put("岑参", "🏜️");
        map.put("骆宾王", "🦢");
        map.put("贺知章", "🍀");
        map.put("张继", "⛵");
        map.put("曹植", "🐉");
        map.put("曹操", "🏯");
        map.put("岳飞", "🗡️");
        map.put("文天祥", "🏹");
        return map;
    }

    /**
     * 一轮作者连线的题目数据。
     *
     * <p>展示：代表作一句（{@link #questionLine}，取自 {@code poem.lines[0]}）+
     * 诗词标题小字（{@link #poemTitle}）；下方 3 列候选作者卡（{@link #options}），
     * 每张卡含作者头像 emoji、作者名、朝代与朝代 tag（供 UI 上色块）。</p>
     */
    public static class AuthorRound {
        /** 该题所属诗词（含作者/朝代/emoji/释义等完整信息） */
        public Poem poem;
        /** 代表作一句（题目，即 poem.lines[0]） */
        public String questionLine;
        /** 正确答案作者名 */
        public String correctAuthor;
        /** 候选作者列表（含正确答案，共 {@value #CANDIDATES} 项） */
        public List<AuthorOption> options;
        /** 当前轮次编号，从 1 开始 */
        public int roundNumber;
    }

    /**
     * 候选作者卡数据。
     */
    public static class AuthorOption {
        /** 作者名 */
        public String author;
        /** 头像 emoji（现有 emoji，如李白 🍶） */
        public String emoji;
        /** 朝代（如 唐代） */
        public String dynasty;
        /** 朝代 tag（如 tang，供 UI 映射 {@code R.color.tag_*} 色块） */
        public String dynastyTag;
        /** 是否为正确答案 */
        public boolean correct;

        AuthorOption(String author, String emoji, String dynasty, String dynastyTag, boolean correct) {
            this.author = author;
            this.emoji = emoji;
            this.dynasty = dynasty;
            this.dynastyTag = dynastyTag;
            this.correct = correct;
        }
    }

    /**
     * 生成一轮作者连线题目（含正确答案与干扰作者）。
     *
     * @param poem  题目诗词（必须含非空 author 与可用 lines[0]）
     * @param all   全量诗词池（用于采样干扰作者）
     * @return 题目数据；poem 不可用时返回 null
     */
    public static AuthorRound generateRound(Poem poem, List<Poem> all) {
        if (poem == null) return null;
        if (poem.author == null || poem.author.trim().isEmpty()) return null;
        if (!isUsableLine(poem.getFirstLine())) return null;
        if (all == null || all.isEmpty()) return null;

        AuthorRound round = new AuthorRound();
        round.poem = poem;
        round.questionLine = poem.getFirstLine();
        round.correctAuthor = poem.author.trim();
        round.options = new ArrayList<>();

        // 正确答案
        round.options.add(new AuthorOption(round.correctAuthor,
                authorEmoji(poem), safeDynasty(poem.dynasty), poem.tag, true));

        // 采样干扰作者：从全池中随机找与正确答案不同的作者，去重、最多 3 个
        Set<String> usedAuthors = new HashSet<>();
        usedAuthors.add(round.correctAuthor);
        List<Poem> shuffled = new ArrayList<>(all);
        Collections.shuffle(shuffled, RANDOM);
        int tries = 0;
        for (Poem other : shuffled) {
            if (round.options.size() >= CANDIDATES) break;
            if (tries >= 200) break; // 防呆：池中作者过少时提前结束
            tries++;
            if (other == null || other.author == null) continue;
            String author = other.author.trim();
            if (author.isEmpty() || usedAuthors.contains(author)) continue;
            usedAuthors.add(author);
            round.options.add(new AuthorOption(author,
                    authorEmoji(other), safeDynasty(other.dynasty), other.tag, false));
        }

        // 打乱候选顺序，使正确答案位置随机
        Collections.shuffle(round.options, RANDOM);
        return round;
    }

    /**
     * 生成整局作者连线游戏（默认 {@value #DEFAULT_ROUNDS} 题）。
     *
     * <p>优先选用<b>有释义的著名诗词</b>（88 首名篇，保证"代表作"熟悉度），
     * 不足时回退到任意有作者的可用诗。同局内作者不重复出现（防重复）。</p>
     *
     * @param pool   诗词池
     * @param rounds 题数
     * @return 题目列表（可能少于 rounds，若可用诗不足）
     */
    public static List<AuthorRound> generateGame(List<Poem> pool, int rounds) {
        List<AuthorRound> game = new ArrayList<>();
        if (pool == null || pool.isEmpty()) return game;

        // 可用候选：有作者 + 有可用 lines[0]
        List<Poem> candidates = new ArrayList<>();
        for (Poem p : pool) {
            if (p != null && p.author != null && !p.author.trim().isEmpty()
                    && isUsableLine(p.getFirstLine())) {
                candidates.add(p);
            }
        }
        if (candidates.isEmpty()) return game;

        // 著名优先：有释义的排前面（代表作更"耳熟能详"）
        List<Poem> famous = new ArrayList<>();
        List<Poem> normal = new ArrayList<>();
        for (Poem p : candidates) {
            (p.hasExplanation() ? famous : normal).add(p);
        }
        List<Poem> source = new ArrayList<>();
        source.addAll(famous);
        source.addAll(normal);
        Collections.shuffle(source, RANDOM);

        Set<String> usedAuthors = new HashSet<>();
        int found = 0;
        for (Poem p : source) {
            // 同局作者去重：避免同一位作者连续出题
            if (usedAuthors.contains(p.author.trim())) continue;
            AuthorRound round = generateRound(p, pool);
            if (round == null) continue;
            round.roundNumber = found + 1;
            game.add(round);
            usedAuthors.add(p.author.trim());
            found++;
            if (found >= rounds) break;
        }
        return game;
    }

    /**
     * 计算作者连线单轮得分（§4.1 接龙模板缩放）。
     *
     * <p>单轮 = (答对 ? 12 + combo : 0)，combo = 连续答对数（答错归零）。
     * 满分参考 5 轮全对 = 12*5 + (1+2+3+4+5) = 75。</p>
     *
     * @param correct 是否答对
     * @param combo   当前连击数（答对后 +1 前的连续答对数，与接龙语义一致）
     * @return 本轮得分
     */
    public static int calcScore(boolean correct, int combo) {
        return correct ? 12 + combo : 0;
    }

    /**
     * 根据总分换算星级（§4.1 接龙阈值按 5 轮满分 75 等比缩放）。
     *
     * <p>满分 75：≥ 58（≈ 接龙 84/112）→ 3 星；≥ 38（≈ 接龙 56/112）→ 2 星；
     * 完成 → 1 星（零挫败：只要有参与至少 1 星）。</p>
     *
     * @param score 本局总分
     * @return 星级 1..3
     */
    public static int calcStars(int score) {
        if (score >= 58) return 3;
        if (score >= 38) return 2;
        return 1;
    }

    /**
     * 取作者头像 emoji：先查常见作者静态映射，回退 {@code poem.emoji}，再回退 📜。
     */
    public static String authorEmoji(Poem poem) {
        if (poem == null) return "📜";
        String author = poem.author;
        if (author != null) {
            String mapped = AUTHOR_EMOJI.get(author.trim());
            if (mapped != null) return mapped;
        }
        return (poem.emoji != null && !poem.emoji.isEmpty()) ? poem.emoji : "📜";
    }

    /** 朝代兜底：null/空 → "佚代"（数据异常保护）。 */
    private static String safeDynasty(String dynasty) {
        return dynasty != null && !dynasty.trim().isEmpty() ? dynasty.trim() : "佚代";
    }

    /** 判断一句诗是否可用（非空、≥2 字、不含省略号），复用 §5.1 语义。 */
    private static boolean isUsableLine(String line) {
        if (line == null) return false;
        String trimmed = line.trim();
        return trimmed.length() >= 2
                && !trimmed.contains("…") && !trimmed.isEmpty();
    }
}
