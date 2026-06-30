package com.poetry.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {LearningRecord.class, DailyStats.class, UserProfile.class}, version = 2, exportSchema = false)
public abstract class LearningDatabase extends RoomDatabase {

    private static volatile LearningDatabase instance;

    public abstract PoemDao poemDao();

    public static LearningDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (LearningDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        LearningDatabase.class,
                        "poetry_learning.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return instance;
    }
}
