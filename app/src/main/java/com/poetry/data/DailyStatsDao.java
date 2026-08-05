package com.poetry.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * 每日统计 DAO —— 管理 daily_stats 表。
 * 追踪用户每日的学习、答题、游戏活跃度，用于签到日历和学习趋势图表。
 */
@Dao
public interface DailyStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertDailyStats(DailyStats stats);

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    DailyStats getDailyStats(String date);

    @Query("SELECT * FROM daily_stats WHERE date LIKE :month || '%' ORDER BY date ASC")
    List<DailyStats> getMonthlyStats(String month);

    @Query("SELECT * FROM daily_stats WHERE date >= :startDate ORDER BY date ASC")
    List<DailyStats> getRecentStats(String startDate);

    @Query("SELECT date FROM daily_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    List<String> getCheckinDates(String startDate, String endDate);

    @Query("UPDATE daily_stats SET quizCompleted = quizCompleted + 1 WHERE date = :date")
    void incrementQuizCompleted(String date);

    @Query("UPDATE daily_stats SET poemsLearned = poemsLearned + 1 WHERE date = :date")
    void incrementPoemsLearned(String date);

    @Query("UPDATE daily_stats SET gamesPlayed = gamesPlayed + 1 WHERE date = :date")
    void incrementGamesPlayed(String date);

    @Query("UPDATE daily_stats SET pointsEarned = pointsEarned + :points WHERE date = :date")
    void incrementPointsEarned(String date, int points);

    /** 获取最近 N 天的每日统计（按日期倒序），供连续游戏天数成就判定。 */
    @Query("SELECT * FROM daily_stats WHERE date >= :startDate ORDER BY date DESC LIMIT :limit")
    List<DailyStats> getRecentDailyStats(String startDate, int limit);

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    DailyStats getDailyStatsSync(String date);

    @Query("SELECT COUNT(*) > 0 FROM daily_stats WHERE date = :today AND quizCompleted > 0")
    boolean hasQuizToday(String today);

    @Query("SELECT COUNT(*) > 0 FROM daily_stats WHERE date = :today AND gamesPlayed > 0")
    boolean hasGameToday(String today);
}
