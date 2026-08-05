package com.poetry.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * 自适应排等（M9，GAME_REDESIGN_FINAL.md §6.4 / §8.3 / §16.2）。
 *
 * <p>基于 {@code SharedPreferences} 文件 {@code diff_profile} 记录玩家最近表现，
 * 自动升降难度档位，保证「入门必过」：
 * <ul>
 *   <li><b>首局默认 Easy</b>：{@code gamesPlayed == 0} 时 {@link #getLevel(Context)} 恒返回 0（EASY），
 *       几乎零门槛，闭眼点也大概率 1 星（§6.4 首局保证）。</li>
 *   <li><b>表现自适应</b>：记录最近 5 局正确率（0-100 整数），平均 ≥85% → 升一档；
 *       &lt;50% → 降一档（§6.4）。</li>
 *   <li><b>只在局部生效</b>：难度只影响飞花令候选句数量、对诗干扰项迷惑度、填空挖空数、
 *       消消乐计时等局部参数，不全局打扰（§6.4）。</li>
 *   <li><b>Easy 达到 3 星才解锁升档</b>：从 0 档升 1 档时，要求最近一局星级 ≥3（§16.2 风险行 778）。</li>
 * </ul>
 *
 * <p>存储格式（prefs 名 {@code diff_profile}）：
 * <ul>
 *   <li>{@code currentLevel}：int，0=EASY / 1=NORMAL / 2=HARD。</li>
 *   <li>{@code lastScores}：String，最近 ≤5 局正确率（0-100 整数），以 {@code |} 分隔。</li>
 *   <li>{@code gamesPlayed}：int，累计已玩局数。</li>
 * </ul>
 *
 * <p>纯静态工具类，所有方法线程安全（SharedPreferences 内部同步）。
 */
public final class DifficultyProfile {

    /** SharedPreferences 文件名（§6.4 键 diff_profile）。 */
    private static final String PREFS_NAME = "diff_profile";

    private static final String KEY_LEVEL = "currentLevel";
    private static final String KEY_SCORES = "lastScores";
    private static final String KEY_PLAYED = "gamesPlayed";

    /** 难度档位常量。 */
    public static final int LEVEL_EASY = 0;
    public static final int LEVEL_NORMAL = 1;
    public static final int LEVEL_HARD = 2;

    /** 只统计最近 5 局（§16.2）。 */
    private static final int MAX_RECENT = 5;
    /** 升档正确率阈值（§6.4）。 */
    private static final int UPGRADE_RATIO = 85;
    /** 降档正确率阈值（§6.4）。 */
    private static final int DOWNGRADE_RATIO = 50;
    /** 升档所需星级（Easy 达到 3 星才解锁升档，§16.2）。 */
    private static final int UPGRADE_MIN_STARS = 3;

    private DifficultyProfile() {
        // 工具类，禁止实例化
    }

    /**
     * 读取当前难度档位。
     *
     * <p>首局（{@code gamesPlayed == 0}）强制返回 EASY（§6.4 首局默认 Easy）；
     * 否则返回存储的 {@code currentLevel}。
     *
     * @param ctx 上下文（用于访问 SharedPreferences）
     * @return 0=EASY / 1=NORMAL / 2=HARD
     */
    public static int getLevel(Context ctx) {
        SharedPreferences prefs = prefs(ctx);
        int played = prefs.getInt(KEY_PLAYED, 0);
        if (played == 0) return LEVEL_EASY; // 首局强制 Easy
        int level = prefs.getInt(KEY_LEVEL, LEVEL_EASY);
        return clampLevel(level);
    }

    /**
     * 记录一局成绩并据此自适应升降档。
     *
     * <p>正确率 = {@code correctCount * 100 / totalCount}（{@code totalCount <= 0} 时记 0）。
     * 追加到 {@code lastScores} 只保留最近 5 局；随后：
     * <ul>
     *   <li>5 局全满且平均正确率 ≥85% 且当前档 &lt;2 → 升一档；</li>
     *   <li>平均正确率 &lt;50% 且当前档 &gt;0 → 降一档。</li>
     * </ul>
     * 从 EASY 升 NORMAL 额外要求本局星级 ≥3（§16.2 风险行 778）。
     * 最后 {@code gamesPlayed++}。
     *
     * @param ctx          上下文
     * @param correctCount 本局答对数
     * @param totalCount   本局总题数
     * @param stars        本局星级（1-3，用于 Easy 升档守卫）
     */
    public static void recordGame(Context ctx, int correctCount, int totalCount, int stars) {
        SharedPreferences prefs = prefs(ctx);
        int ratio = totalCount > 0 ? correctCount * 100 / totalCount : 0;
        ratio = Math.max(0, Math.min(100, ratio));

        // 追加最近 5 局正确率
        List<Integer> scores = readScores(prefs);
        scores.add(ratio);
        while (scores.size() > MAX_RECENT) {
            scores.remove(0);
        }

        int played = prefs.getInt(KEY_PLAYED, 0) + 1;
        int level = clampLevel(prefs.getInt(KEY_LEVEL, LEVEL_EASY));

        // 5 局全满才参与升降档判定
        if (scores.size() >= MAX_RECENT) {
            int avg = average(scores);
            if (avg >= UPGRADE_RATIO && level < LEVEL_HARD) {
                // Easy 升档守卫：从 0 档升 1 档需本局 3 星（§16.2）
                if (level > LEVEL_EASY || stars >= UPGRADE_MIN_STARS) {
                    level++;
                }
            } else if (avg < DOWNGRADE_RATIO && level > LEVEL_EASY) {
                level--;
            }
        }

        prefs.edit()
                .putInt(KEY_LEVEL, level)
                .putString(KEY_SCORES, join(scores))
                .putInt(KEY_PLAYED, played)
                .apply();
    }

    /**
     * 飞花令候选句数量（局部生效，§6.4）。
     *
     * <p>Easy=6 / Normal=5 / Hard=4，随难度档位减少候选句，提升点选难度。
     *
     * @param ctx 上下文
     * @return 候选句数量 6/5/4
     */
    public static int flyflowerCandidateCount(Context ctx) {
        switch (getLevel(ctx)) {
            case LEVEL_EASY: return 6;
            case LEVEL_HARD: return 4;
            default: return 5;
        }
    }

    /**
     * 消消乐限时秒数（局部生效，§9.1 默认 75s）。
     *
     * <p>首局无计时（由调用方 {@code played == 0} 决定），次局起默认 90s，
     * HARD 档缩短为 75s。
     *
     * @param ctx 上下文
     * @return 次局起的限时秒数（90 或 75）
     */
    public static int matchTimeLimitSeconds(Context ctx) {
        return getLevel(ctx) == LEVEL_HARD ? 75 : 90;
    }

    /** 读取 prefs 实例。 */
    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** 解析最近 5 局正确率列表。 */
    private static List<Integer> readScores(SharedPreferences prefs) {
        List<Integer> scores = new ArrayList<>();
        String raw = prefs.getString(KEY_SCORES, "");
        if (raw == null || raw.isEmpty()) return scores;
        for (String part : raw.split("\\|")) {
            try {
                scores.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略损坏数据
            }
        }
        return scores;
    }

    /** 用 {@code |} 拼接正确率列表。 */
    private static String join(List<Integer> scores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scores.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(scores.get(i));
        }
        return sb.toString();
    }

    private static int average(List<Integer> scores) {
        if (scores.isEmpty()) return 0;
        int sum = 0;
        for (int s : scores) sum += s;
        return sum / scores.size();
    }

    private static int clampLevel(int level) {
        if (level < LEVEL_EASY) return LEVEL_EASY;
        if (level > LEVEL_HARD) return LEVEL_HARD;
        return level;
    }
}
