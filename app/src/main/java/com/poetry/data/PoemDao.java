package com.poetry.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PoemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLearningRecord(LearningRecord record);

    @Update
    void updateLearningRecord(LearningRecord record);

    @Query("SELECT * FROM learning_records WHERE poemId = :poemId")
    LearningRecord getLearningRecord(String poemId);

    @Query("SELECT * FROM learning_records WHERE favorite = 1")
    LiveData<List<LearningRecord>> getFavorites();

    @Query("SELECT * FROM learning_records ORDER BY learnedAt DESC")
    LiveData<List<LearningRecord>> getAllLearningRecords();

    @Query("SELECT COUNT(*) FROM learning_records")
    LiveData<Integer> getLearnedCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDailyStats(DailyStats stats);

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    DailyStats getDailyStats(String date);

    @Query("SELECT * FROM daily_stats WHERE date LIKE :month || '%' ORDER BY date ASC")
    List<DailyStats> getMonthlyStats(String month);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserProfile(UserProfile profile);

    @Query("SELECT * FROM user_profile WHERE id = 1")
    LiveData<UserProfile> getUserProfile();

    @Query("SELECT * FROM user_profile WHERE id = 1")
    UserProfile getUserProfileSync();

    @Query("UPDATE user_profile SET totalPoints = :points WHERE id = 1")
    void updatePoints(int points);

    @Query("UPDATE user_profile SET level = :level WHERE id = 1")
    void updateLevel(int level);

    @Query("UPDATE user_profile SET streak = :streak, lastActiveDate = :date WHERE id = 1")
    void updateStreak(int streak, String date);

    @Query("UPDATE user_profile SET achievements = :achv WHERE id = 1")
    void updateAchievements(String achv);

    @Query("SELECT * FROM learning_records WHERE gamePlayed > 0")
    List<LearningRecord> getGameRecords();

    @Query("SELECT COUNT(*) FROM learning_records")
    int getLearnedCountSync();

    @Query("SELECT COUNT(*) FROM learning_records WHERE quizScore >= :score")
    int getPerfectQuizCountSync(int score);

    @Query("SELECT COUNT(*) FROM learning_records WHERE favorite = 1")
    int getFavCountSync();

    @Query("SELECT * FROM learning_records WHERE learnedAt >= :startOfDay")
    List<LearningRecord> getTodayRecords(long startOfDay);

    @Query("SELECT * FROM daily_stats WHERE date >= :startDate ORDER BY date ASC")
    List<DailyStats> getRecentStats(String startDate);

    /** 签到：插入或更新当日统计数据 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertDailyStats(DailyStats stats);

    /** 查询指定日期范围的签到日期列表（用于日历展示） */
    @Query("SELECT date FROM daily_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    List<String> getCheckinDates(String startDate, String endDate);

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

    /** 标记已学 */
    @Query("UPDATE learning_records SET learnedAt = :timestamp WHERE poemId = :poemId")
    void markLearned(String poemId, long timestamp);

    /** 更新答题得分 */
    @Query("UPDATE learning_records SET quizScore = :score WHERE poemId = :poemId")
    void updateQuizScore(String poemId, int score);

    /** 确保 learning_records 中存在该诗词行（INSERT OR IGNORE 幂等） */
    @Query("INSERT OR IGNORE INTO learning_records(poemId, favorite, learnedAt, quizScore, gamePlayed) VALUES (:poemId, 0, 0, 0, 0)")
    void ensureRecordExists(String poemId);

    /** 查询当日任务完成状态：学习过诗词数 */
    @Query("SELECT COUNT(*) FROM learning_records WHERE learnedAt >= :startOfDay")
    int getTodayLearnedCount(long startOfDay);

    /** 查询当日任务完成状态：答过题数（quizScore > 0 表示参与过答题） */
    @Query("SELECT COUNT(*) FROM learning_records WHERE quizScore > 0 AND learnedAt >= :startOfDay")
    int getTodayQuizCount(long startOfDay);

    /** 查询当日任务完成状态：玩过游戏次数 */
    @Query("SELECT COUNT(*) FROM learning_records WHERE gamePlayed > 0 AND learnedAt >= :startOfDay")
    int getTodayGameCount(long startOfDay);

    /** 原子递增当日 quizCompleted（先确保行存在） */
    @Query("UPDATE daily_stats SET quizCompleted = quizCompleted + 1 WHERE date = :date")
    void incrementQuizCompleted(String date);

    /** 原子递增当日 gamesPlayed（先确保行存在） */
    @Query("UPDATE daily_stats SET gamesPlayed = gamesPlayed + 1 WHERE date = :date")
    void incrementGamesPlayed(String date);

    /** 查询当日 DailyStats */
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    DailyStats getDailyStatsSync(String date);

    /** 检查今日是否已答题（用于任务检测） */
    @Query("SELECT COUNT(*) > 0 FROM daily_stats WHERE date = :today AND quizCompleted > 0")
    boolean hasQuizToday(String today);

    /** 检查今日是否已玩游戏（用于任务检测） */
    @Query("SELECT COUNT(*) > 0 FROM daily_stats WHERE date = :today AND gamesPlayed > 0")
    boolean hasGameToday(String today);
}
