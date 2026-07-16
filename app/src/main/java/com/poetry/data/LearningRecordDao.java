package com.poetry.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * 学习记录 DAO —— 管理 learning_records 表。
 * 记录每首诗词的学习状态：收藏、已学、答题得分、游戏参与。
 */
@Dao
public interface LearningRecordDao {

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

    @Query("SELECT COUNT(*) FROM learning_records WHERE learnedAt > 0")
    LiveData<Integer> getLearnedCount();

    @Query("SELECT COUNT(*) > 0 FROM learning_records WHERE poemId = :poemId AND favorite = 1")
    boolean isFavorite(String poemId);

    @Query("SELECT COUNT(*) > 0 FROM learning_records WHERE poemId = :poemId AND learnedAt > 0")
    boolean isLearned(String poemId);

    @Query("UPDATE learning_records SET favorite = 1 WHERE poemId = :poemId")
    void addFavorite(String poemId);

    @Query("UPDATE learning_records SET favorite = 0 WHERE poemId = :poemId")
    void removeFavorite(String poemId);

    @Query("UPDATE learning_records SET learnedAt = :timestamp WHERE poemId = :poemId")
    void markLearned(String poemId, long timestamp);

    @Query("UPDATE learning_records SET quizScore = :score WHERE poemId = :poemId")
    void updateQuizScore(String poemId, int score);

    @Query("INSERT OR IGNORE INTO learning_records(poemId, favorite, learnedAt, quizScore, gamePlayed) VALUES (:poemId, 0, 0, 0, 0)")
    void ensureRecordExists(String poemId);

    @Query("SELECT COUNT(*) FROM learning_records WHERE learnedAt > 0")
    int getLearnedCountSync();

    @Query("SELECT COUNT(*) FROM learning_records WHERE quizScore >= :score")
    int getPerfectQuizCountSync(int score);

    @Query("SELECT COUNT(*) FROM learning_records WHERE favorite = 1")
    int getFavCountSync();

    @Query("SELECT * FROM learning_records WHERE learnedAt > 0 ORDER BY learnedAt DESC")
    LiveData<List<LearningRecord>> getLearnedPoems();

    @Query("SELECT * FROM learning_records WHERE learnedAt >= :startOfDay")
    List<LearningRecord> getTodayRecords(long startOfDay);

    @Query("SELECT * FROM learning_records WHERE gamePlayed > 0")
    List<LearningRecord> getGameRecords();

    @Query("SELECT COUNT(*) FROM learning_records WHERE learnedAt >= :startOfDay")
    int getTodayLearnedCount(long startOfDay);

    @Query("SELECT COUNT(*) FROM learning_records WHERE quizScore > 0 AND learnedAt >= :startOfDay")
    int getTodayQuizCount(long startOfDay);

    @Query("SELECT COUNT(*) FROM learning_records WHERE gamePlayed > 0 AND learnedAt >= :startOfDay")
    int getTodayGameCount(long startOfDay);
}
