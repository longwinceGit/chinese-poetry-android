package com.poetry.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {LearningRecord.class, DailyStats.class, UserProfile.class}, version = 2, exportSchema = false)
public abstract class LearningDatabase extends RoomDatabase {

    private static volatile LearningDatabase instance;

    public abstract PoemDao poemDao();

    /** v1 → v2: 新增 daily_stats 表 */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `daily_stats` ("
                    + "`date` TEXT NOT NULL, "
                    + "`poemsLearned` INTEGER NOT NULL DEFAULT 0, "
                    + "`quizCompleted` INTEGER NOT NULL DEFAULT 0, "
                    + "`pointsEarned` INTEGER NOT NULL DEFAULT 0, "
                    + "`gamesPlayed` INTEGER NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY(`date`))");
        }
    };

    public static LearningDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (LearningDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        LearningDatabase.class,
                        "poetry_learning.db"
                    ).addMigrations(MIGRATION_1_2).build();
                }
            }
        }
        return instance;
    }
}
