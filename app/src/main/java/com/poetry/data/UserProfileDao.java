package com.poetry.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * 用户档案 DAO —— 管理 user_profile 表。
 * 包含积分、等级、连续学习天数、成就、主题等用户核心数据。
 */
@Dao
public interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserProfile(UserProfile profile);

    @Query("SELECT * FROM user_profile WHERE id = 1")
    LiveData<UserProfile> getUserProfile();

    @Query("SELECT * FROM user_profile WHERE id = 1")
    UserProfile getUserProfileSync();

    @Deprecated
    @Query("UPDATE user_profile SET totalPoints = :points WHERE id = 1")
    void updatePoints(int points);

    @Query("UPDATE user_profile SET totalPoints = totalPoints + :points WHERE id = 1")
    void addTotalPoints(int points);

    @Query("UPDATE user_profile SET level = :level WHERE id = 1")
    void updateLevel(int level);

    @Query("UPDATE user_profile SET streak = :streak, lastActiveDate = :date WHERE id = 1")
    void updateStreak(int streak, String date);

    @Query("UPDATE user_profile SET achievements = :achv WHERE id = 1")
    void updateAchievements(String achv);
}
