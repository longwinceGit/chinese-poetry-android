package com.poetry.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 飞花令引擎 —— 纯 Java 领域逻辑，无任何 Android 依赖。
 *
 * <p>实现 GAME_REDESIGN_FINAL.md §4.4 的飞花令核心规则：
 * <ul>
 *   <li>主题字表：硬编码 20-40 个常见意象字，供 {@link #pickThemeChar(Set)} 随机选取；</li>
 *   <li>判定：{@link #judge(String, String)} 用 {@code String.contains} 做模糊判定（永不判错死路）；</li>
 *   <li>评分：{@link #calcScore(int, int, int)} 基础分 + 连击奖励（封顶 +200）+ 长度加成；</li>
 *   <li>星级：{@link #calcStars(int)} 命中 ≥10 → 3★，≥6 → 2★，≥3 → 1★，否则 1★（零挫败）。</li>
 * </ul>
 */
public final class FlyflowerEngine {

    /** 连击奖励封顶值（每连续 3 句命中 +20，封顶 +200）。 */
    public static final int MAX_TRIPLET_BONUS = 200;
    /** 每连续 3 句命中的连击奖励。 */
    public static final int TRIPLET_BONUS = 20;
    /** 基础分：每命中一句。 */
    public static final int BASE_SCORE_PER_HIT = 30;
    /** 长度加成：命中句去空白后 ≥5 字额外加分。 */
    public static final int LONG_LINE_BONUS = 10;
    /** 长度加成阈值（去空白后字数）。 */
    public static final int LONG_LINE_THRESHOLD = 5;

    /** 常见意象主题字表（20-40 个）。 */
    public static final String[] THEMES = {
            "月", "花", "雪", "风", "云", "山", "水", "日", "春", "江",
            "天", "夜", "星", "雨", "秋", "暮", "愁", "归", "烟", "柳",
            "梅", "竹", "松", "鸟", "雁", "舟", "酒", "梦", "心", "情",
            "人", "家", "故", "明", "清", "寒", "红", "绿", "白", "金"
    };

    private FlyflowerEngine() {
        // 工具类，禁止实例化
    }

    /**
     * @return 全部主题字列表（不可变视图）。
     */
    public static List<String> themeChars() {
        return Arrays.asList(THEMES);
    }

    /**
     * 从主题字表中随机选取一个本 session 尚未用过的字。
     *
     * <p>优先从未用过的字中随机选；若全部已用，则回退到任意字（保证始终有主题字可用）。
     * 线程安全非必需（仅在主线程调用）。
     *
     * @param usedThisSession 本 session 已用过的主题字集合（可为 null）
     * @return 选中的主题字
     */
    public static String pickThemeChar(Set<String> usedThisSession) {
        List<String> unused = new ArrayList<>();
        for (String theme : THEMES) {
            if (usedThisSession == null || !usedThisSession.contains(theme)) {
                unused.add(theme);
            }
        }
        List<String> pool = unused.isEmpty() ? Arrays.asList(THEMES) : unused;
        int idx = (int) (Math.random() * pool.size());
        return pool.get(idx);
    }

    /**
     * 判定输入是否命中主题字。
     *
     * <p>成功条件：输入非空、去首尾空白后长度 ≥ 2、且包含主题字（{@code String.contains}）。
     * 纯 contains 判定，务实不做 NLP，永不判错死路。
     *
     * @param input   玩家输入
     * @param keyword 主题字
     * @return 命中返回 true
     */
    public static boolean judge(String input, String keyword) {
        if (input == null || keyword == null) return false;
        String trimmed = input.trim();
        return trimmed.length() >= 2 && trimmed.contains(keyword);
    }

    /**
     * 清理诗句用于展示：去除首尾空白。
     *
     * @param line 原始诗句
     * @return 去首尾空白后的诗句；null 输入返回 null
     */
    public static String cleanLineForDisplay(String line) {
        return line == null ? null : line.trim();
    }

    /**
     * 计算飞花令得分。
     *
     * <p>公式（GAME_REDESIGN_FINAL.md §4.4）：
     * <pre>
     *   基础分 = 命中句数 × 30
     *   连招   = 每连续 3 句命中 +20（封顶 +200）
     *   长度加成 = 命中句去空白后 ≥5 字额外 +10
     * </pre>
     *
     * @param hitCount      命中句数
     * @param triplets      连击组数（每连续 3 句命中计一组）
     * @param longLineCount 命中句中去空白后 ≥5 字的句数
     * @return 本局总分
     */
    public static int calcScore(int hitCount, int triplets, int longLineCount) {
        int base = hitCount * BASE_SCORE_PER_HIT;
        int tripletBonus = Math.min(triplets * TRIPLET_BONUS, MAX_TRIPLET_BONUS);
        int lengthBonus = longLineCount * LONG_LINE_BONUS;
        return base + tripletBonus + lengthBonus;
    }

    /**
     * 计算星级（零挫败：任何参与至少 1★）。
     *
     * @param hitCount 命中句数
     * @return 1-3 星
     */
    public static int calcStars(int hitCount) {
        if (hitCount >= 10) return 3;
        if (hitCount >= 6) return 2;
        return 1;
    }
}
