package com.poetry.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {
    @PrimaryKey
    public int id;
    public int totalPoints;
    public int level;
    public int streak;
    @NonNull
    public String lastActiveDate;
    @NonNull
    public String unlockedThemes;
    @NonNull
    public String achievements;

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
