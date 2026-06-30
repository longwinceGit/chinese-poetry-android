package com.poetry.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "learning_records")
public class LearningRecord {
    @PrimaryKey
    @NonNull
    public String poemId;
    public String title;
    public String dynasty;
    public String author;
    public long learnedAt;
    public int quizScore;
    public boolean favorite;
    public int gamePlayed;

    public LearningRecord() {}

    @Ignore
    public LearningRecord(String poemId, String title, String dynasty, String author) {
        this.poemId = poemId;
        this.title = title;
        this.dynasty = dynasty;
        this.author = author;
        this.learnedAt = System.currentTimeMillis();
        this.quizScore = 0;
        this.favorite = false;
        this.gamePlayed = 0;
    }
}
