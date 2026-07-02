package com.poetry.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 学习引擎 —— 等级、积分、连续学习天数的计算逻辑。
 *
 * 等级体系：9 级（诗词小学徒 → 千古诗圣），基于累计积分。
 * 积分获取：学习 +10 / 答题满分 +20 / 连续签到奖励 / 游戏得分。
 */
public class LearningEngine {

    /** 等级积分阈值（累计积分达到即升级） */
    public static final int[] LEVEL_THRESHOLDS = {0, 100, 300, 600, 1000, 2000, 3500, 5000, 8000};
    /** 等级称号 */
    public static final String[] LEVEL_NAMES = {"诗词小学徒", "小秀才", "小举人", "小进士", "小翰林", "大诗仙", "诗词宗师", "一代文豪", "千古诗圣"};
    /** 等级对应颜色（ARGB） */
    public static final int[] LEVEL_ICONS = {0xFFA78BFA, 0xFF60A5FA, 0xFF34D399, 0xFFFBBF24, 0xFFFB923C, 0xFFF472B6, 0xFFEF4444, 0xFF8B5CF6, 0xFFD97706};

    /**
     * 根据累计积分计算当前等级。
     * @param totalPoints 累计积分
     * @return 等级（1-9）
     */
    public static int calcLevel(int totalPoints) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (totalPoints >= LEVEL_THRESHOLDS[i]) {
                return i + 1;
            }
        }
        return 1;
    }

    /**
     * 计算当前等级的经验进度百分比。
     * @param totalPoints 累计积分
     * @return 0-100 的进度百分比
     */
    public static int getLevelProgress(int totalPoints) {
        int level = calcLevel(totalPoints);
        int idx = level - 1;
        int currentThreshold = LEVEL_THRESHOLDS[idx];
        int nextThreshold = (idx + 1 < LEVEL_THRESHOLDS.length) ? LEVEL_THRESHOLDS[idx + 1] : currentThreshold + 1000;
        int progress = totalPoints - currentThreshold;
        int range = nextThreshold - currentThreshold;
        if (range <= 0) return 100;
        return Math.min(100, progress * 100 / range);
    }

    /** @return 等级对应的中文称号 */
    public static String getLevelName(int level) {
        int idx = Math.min(level - 1, LEVEL_NAMES.length - 1);
        return LEVEL_NAMES[idx];
    }

    /** @return 等级对应的颜色值 */
    public static int getLevelColor(int level) {
        int idx = Math.min(level - 1, LEVEL_ICONS.length - 1);
        return LEVEL_ICONS[idx];
    }

    /**
     * 计算新的连续学习天数。
     * 规则：今天 = 昨天 → +1；今天 = 今天 → 不变；否则 → 重置为 1（断签）。
     *
     * @param lastActiveDate 上次活跃日期
     * @param currentStreak  当前连续天数
     * @return 新的连续天数
     */
    public static int calcStreak(String lastActiveDate, int currentStreak) {
        String today = getToday();
        if (today.equals(lastActiveDate)) {
            return currentStreak;
        }
        if (isYesterday(lastActiveDate, today)) {
            return currentStreak + 1;
        }
        return 1;
    }

    /**
     * 检查是否达到连续签到里程碑（每 7 天一次庆祝）。
     * @param streak 当前连续天数
     * @return true 如果 streak >= 3 且是 7 的倍数
     */
    public static boolean checkStreakMilestone(int streak) {
        return streak >= 3 && streak % 7 == 0;
    }

    /** 学习一首诗的基础积分 */
    public static int calcPointsForLearning() {
        return 10;
    }

    /**
     * 计算答题得分。
     * 满分 20，正确率 >= 80% → 15，>= 50% → 10，否则保底 5。
     *
     * @param correctCount 正确数
     * @param totalCount   总数
     * @return 得分
     */
    public static int calcPointsForQuiz(int correctCount, int totalCount) {
        if (totalCount <= 0) return 0;
        float ratio = (float) correctCount / totalCount;
        if (ratio >= 1.0f) return 20;
        if (ratio >= 0.8f) return 15;
        if (ratio >= 0.5f) return 10;
        return 5;
    }

    /**
     * 连续签到额外奖励积分。
     * @param streak 连续天数
     * @return 里程碑奖励积分
     */
    public static int calcPointsForStreak(int streak) {
        if (streak == 3) return 10;
        if (streak == 7) return 30;
        if (streak == 14) return 50;
        if (streak == 21) return 80;
        if (streak == 30) return 100;
        if (streak > 30 && streak % 30 == 0) return 100;
        return 0;
    }

    /** @return 今天的日期字符串（yyyy-MM-dd） */
    public static String getToday() {
        return LocalDate.now().toString();
    }

    /**
     * 判断 lastDate 是否为 today 的前一天。
     * @param lastDate 上次活跃日期
     * @param today    今天的日期
     * @return true 如果是昨天
     */
    public static boolean isYesterday(String lastDate, String today) {
        try {
            LocalDate last = LocalDate.parse(lastDate);
            LocalDate now = LocalDate.parse(today);
            long daysBetween = ChronoUnit.DAYS.between(last, now);
            return daysBetween == 1;
        } catch (Exception e) {
            return false;
        }
    }
}
