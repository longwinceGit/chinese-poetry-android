package com.poetry.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * 数据访问对象（DAO） —— Room 数据库的所有 SQL 操作入口。
 *
 * 涵盖三张表的 CRUD：
 * - learning_records：诗词学习记录（标记已学/收藏/答题/游戏）
 * - daily_stats：每日统计（签到/答题数/游戏数）
 * - user_profile：用户档案（积分/等级/成就/主题）
 *
 * 注意：积分相关操作使用原子 SQL（如 addTotalPoints），避免并发竞态。
 */
@Dao
public interface PoemDao {

    // ==================== learning_records 操作 ====================

    /** 插入或更新学习记录 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLearningRecord(LearningRecord record);

    @Update
    void updateLearningRecord(LearningRecord record);

    /** 按诗词 ID 查询学习记录 */
    @Query("SELECT * FROM learning_records WHERE poemId = :poemId")
    LearningRecord getLearningRecord(String poemId);

    /** 获取所有收藏的诗词（实时观察） */
    @Query("SELECT * FROM learning_records WHERE favorite = 1")
    LiveData<List<LearningRecord>> getFavorites();

    /** 获取所有学习记录（按时间倒序，实时观察） */
    @Query("SELECT * FROM learning_records ORDER BY learnedAt DESC")
    LiveData<List<LearningRecord>> getAllLearningRecords();

    /** 已学诗词总数（实时观察） */
    @Query("SELECT COUNT(*) FROM learning_records")
    LiveData<Integer> getLearnedCount();

    // ==================== daily_stats 操作 ====================

    /** 插入或更新每日统计 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDailyStats(DailyStats stats);

    /** 查询某天的统计 */
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    DailyStats getDailyStats(String date);

    /** 查询当月所有统计 */
    @Query("SELECT * FROM daily_stats WHERE date LIKE :month || '%' ORDER BY date ASC")
    List<DailyStats> getMonthlyStats(String month);

    // ==================== user_profile 操作 ====================

    /** 插入或更新用户档案 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserProfile(UserProfile profile);

    /** 获取用户档案（实时观察） */
    @Query("SELECT * FROM user_profile WHERE id = 1")
    LiveData<UserProfile> getUserProfile();

    /** 获取用户档案（同步调用，后台线程使用） */
    @Query("SELECT * FROM user_profile WHERE id = 1")
    UserProfile getUserProfileSync();

    /** 设置总积分（覆盖式，已弃用 → 用 addTotalPoints） */
    @Query("UPDATE user_profile SET totalPoints = :points WHERE id = 1")
    void updatePoints(int points);

    /** 原子增量积分：避免读-改-写竞态导致积分丢失（B2 修复） */
    @Query("UPDATE user_profile SET totalPoints = totalPoints + :points WHERE id = 1")
    void addTotalPoints(int points);

    /** 更新等级 */
    @Query("UPDATE user_profile SET level = :level WHERE id = 1")
    void updateLevel(int level);

    /** 更新连续天数和最后活跃日期 */
    @Query("UPDATE user_profile SET streak = :streak, lastActiveDate = :date WHERE id = 1")
    void updateStreak(int streak, String date);

    /** 更新成就 JSON */
    @Query("UPDATE user_profile SET achievements = :achv WHERE id = 1")
    void updateAchievements(String achv);

    // ==================== 统计查询 ====================

    /** 查询所有有游戏记录的诗词 */
    @Query("SELECT * FROM learning_records WHERE gamePlayed > 0")
    List<LearningRecord> getGameRecords();

    /** 已学诗词总数（同步） */
    @Query("SELECT COUNT(*) FROM learning_records")
    int getLearnedCountSync();

    /** 满分答题次数 */
    @Query("SELECT COUNT(*) FROM learning_records WHERE quizScore >= :score")
    int getPerfectQuizCountSync(int score);

    /** 收藏数量（同步） */
    @Query("SELECT COUNT(*) FROM learning_records WHERE favorite = 1")
    int getFavCountSync();

    /** 查询今日学习记录 */
    @Query("SELECT * FROM learning_records WHERE learnedAt >= :startOfDay")
    List<LearningRecord> getTodayRecords(long startOfDay);

    /** 查询最近 N 天的统计 */
    @Query("SELECT * FROM daily_stats WHERE date >= :startDate ORDER BY date ASC")
    List<DailyStats> getRecentStats(String startDate);

    // ==================== 签到相关 ====================

    /** 签到：插入或更新当日统计数据 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertDailyStats(DailyStats stats);

    /** 查询指定日期范围的签到日期列表（用于日历展示） */
    @Query("SELECT date FROM daily_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    List<String> getCheckinDates(String startDate, String endDate);

    // ==================== 收藏/已学标记 ====================

    /** 检查诗词是否已收藏 */
    @Query("SELECT COUNT(*) > 0 FROM learning_records WHERE poemId = :poemId AND favorite = 1")
    boolean isFavorite(String poemId);

    /** 检查诗词是否已学过 */
    @Query("SELECT COUNT(*) > 0 FROM learning_records WHERE poemId = :poemId AND learnedAt > 0")
    boolean isLearned(String poemId);

    /** 添加收藏 */
    @Query("UPDATE learning_records SET favorite = 1 WHERE poemId = :poemId")
    void addFavorite(String poemId);

    /** 取消收藏 */
    @Query("UPDATE learning_records SET favorite = 0 WHERE poemId = :poemId")
    void removeFavorite(String poemId);

    /** 标记为已学 */
    @Query("UPDATE learning_records SET learnedAt = :timestamp WHERE poemId = :poemId")
    void markLearned(String poemId, long timestamp);

    /** 更新答题得分 */
    @Query("UPDATE learning_records SET quizScore = :score WHERE poemId = :poemId")
    void updateQuizScore(String poemId, int score);

    // ==================== 原子操作 ====================

    /** 确保 learning_records 中存在该诗词行（INSERT OR IGNORE 幂等） */
    @Query("INSERT OR IGNORE INTO learning_records(poemId, favorite, learnedAt, quizScore, gamePlayed) VALUES (:poemId, 0, 0, 0, 0)")
    void ensureRecordExists(String poemId);

    // ==================== 每日任务检测 ====================

    /** 查询今日已学诗词数 */
    @Query("SELECT COUNT(*) FROM learning_records WHERE learnedAt >= :startOfDay")
    int getTodayLearnedCount(long startOfDay);

    /** 今日答过题的诗数（quizScore > 0 表示参与过答题） */
    @Query("SELECT COUNT(*) FROM learning_records WHERE quizScore > 0 AND learnedAt >= :startOfDay")
    int getTodayQuizCount(long startOfDay);

    /** 今日玩过游戏的诗数 */
    @Query("SELECT COUNT(*) FROM learning_records WHERE gamePlayed > 0 AND learnedAt >= :startOfDay")
    int getTodayGameCount(long startOfDay);

    /** 原子递增当日答题完成数 */
    @Query("UPDATE daily_stats SET quizCompleted = quizCompleted + 1 WHERE date = :date")
    void incrementQuizCompleted(String date);

    /** 原子递增当日已学诗词数 */
    @Query("UPDATE daily_stats SET poemsLearned = poemsLearned + 1 WHERE date = :date")
    void incrementPoemsLearned(String date);

    /** 原子递增当日游戏完成数 */
    @Query("UPDATE daily_stats SET gamesPlayed = gamesPlayed + 1 WHERE date = :date")
    void incrementGamesPlayed(String date);

    /** 同步查询当日 DailyStats */
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    DailyStats getDailyStatsSync(String date);

    /** 检查今日是否已答题（用于任务检测） */
    @Query("SELECT COUNT(*) > 0 FROM daily_stats WHERE date = :today AND quizCompleted > 0")
    boolean hasQuizToday(String today);

    /** 检查今日是否已玩游戏（用于任务检测） */
    @Query("SELECT COUNT(*) > 0 FROM daily_stats WHERE date = :today AND gamesPlayed > 0")
    boolean hasGameToday(String today);
}
