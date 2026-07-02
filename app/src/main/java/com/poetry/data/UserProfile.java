package com.poetry.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 用户档案 —— Room 实体（单例，始终 id=1）。
 *
 * 存储游戏化系统的核心状态：积分、等级、连续学习天数、已解锁主题、已达成成就。
 * 主题和成就以 JSON 数组字符串形式存储，通过 ThemeManager / AchievementEngine 解析。
 */
@Entity(tableName = "user_profile")
public class UserProfile {

    /** 固定主键 = 1（全局唯一一行） */
    @PrimaryKey
    public int id;

    /** 累计积分 */
    public int totalPoints;

    /** 用户等级（1-9），由 LearningEngine.calcLevel() 计算 */
    public int level;

    /** 连续学习天数 */
    public int streak;

    /** 最后活跃日期，格式 yyyy-MM-dd */
    @NonNull
    public String lastActiveDate;

    /** 已解锁主题 ID 列表，JSON 数组如 ["default","spring"] */
    @NonNull
    public String unlockedThemes;

    /** 已达成成就 ID 列表，JSON 数组如 ["first_poem","poem_10"] */
    @NonNull
    public String achievements;

    /** 新用户默认初始状态 */
    public UserProfile() {
        this.id = 1;
        this.totalPoints = 0;
        this.level = 1;
        this.streak = 0;
        this.lastActiveDate = "";
        this.unlockedThemes = "[]";
        this.achievements = "[]";
    }
}
