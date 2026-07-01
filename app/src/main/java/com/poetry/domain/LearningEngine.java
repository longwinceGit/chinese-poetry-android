package com.poetry.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LearningEngine {

    public static final int[] LEVEL_THRESHOLDS = {0, 100, 300, 600, 1000, 2000, 3500, 5000, 8000};
    public static final String[] LEVEL_NAMES = {"诗词小学徒", "小秀才", "小举人", "小进士", "小翰林", "大诗仙", "诗词宗师", "一代文豪", "千古诗圣"};
    public static final int[] LEVEL_ICONS = {0xFFA78BFA, 0xFF60A5FA, 0xFF34D399, 0xFFFBBF24, 0xFFFB923C, 0xFFF472B6, 0xFFEF4444, 0xFF8B5CF6, 0xFFD97706};

    public static int calcLevel(int totalPoints) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (totalPoints >= LEVEL_THRESHOLDS[i]) {
                return i + 1;
            }
        }
        return 1;
    }

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

    public static String getLevelName(int level) {
        int idx = Math.min(level - 1, LEVEL_NAMES.length - 1);
        return LEVEL_NAMES[idx];
    }

    public static int getLevelColor(int level) {
        int idx = Math.min(level - 1, LEVEL_ICONS.length - 1);
        return LEVEL_ICONS[idx];
    }

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

    public static boolean checkStreakMilestone(int streak) {
        return streak >= 3 && streak % 7 == 0;
    }

    public static int calcPointsForLearning() {
        return 10;
    }

    public static int calcPointsForQuiz(int correctCount, int totalCount) {
        if (totalCount <= 0) return 0;
        float ratio = (float) correctCount / totalCount;
        if (ratio >= 1.0f) return 20;
        if (ratio >= 0.8f) return 15;
        if (ratio >= 0.5f) return 10;
        return 5;
    }

    public static int calcPointsForStreak(int streak) {
        if (streak == 3) return 10;
        if (streak == 7) return 30;
        if (streak == 14) return 50;
        if (streak == 21) return 80;
        if (streak == 30) return 100;
        if (streak > 30 && streak % 30 == 0) return 100;
        return 0;
    }

    public static String getToday() {
        return LocalDate.now().toString();
    }

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
