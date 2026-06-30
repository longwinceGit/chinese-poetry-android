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
}
