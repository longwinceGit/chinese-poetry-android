package com.poetry.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_stats")
public class DailyStats {
    @PrimaryKey
    @NonNull
    public String date;
    public int poemsLearned;
    public int quizCompleted;
    public int pointsEarned;
    public int gamesPlayed;

    public DailyStats() {}

    @Ignore
    public DailyStats(String date) {
        this.date = date;
        this.poemsLearned = 0;
        this.quizCompleted = 0;
        this.pointsEarned = 0;
        this.gamesPlayed = 0;
    }
}
