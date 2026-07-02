package com.poetry.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 每日统计 —— Room 实体，记录用户每天的学习活动汇总。
 *
 * 每天打开应用或首次活动时自动创建当日行。
 * 各计数器通过原子 SQL 增量更新（如 {@code incrementQuizCompleted}），保证并发安全。
 */
@Entity(tableName = "daily_stats")
public class DailyStats {

    /** 日期（主键），格式 yyyy-MM-dd */
    @PrimaryKey
    @NonNull
    public String date;

    /** 当日学过的诗词数 */
    public int poemsLearned;

    /** 当日完成的答题数 */
    public int quizCompleted;

    /** 当日获得的积分 */
    public int pointsEarned;

    /** 当日玩过的游戏次数 */
    public int gamesPlayed;

    /** Room 要求无参构造 */
    public DailyStats() {}

    /**
     * 创建当日的空白统计行。
     * @param date 日期字符串，格式 yyyy-MM-dd
     */
    @Ignore
    public DailyStats(String date) {
        this.date = date;
        this.poemsLearned = 0;
        this.quizCompleted = 0;
        this.pointsEarned = 0;
        this.gamesPlayed = 0;
    }
}
