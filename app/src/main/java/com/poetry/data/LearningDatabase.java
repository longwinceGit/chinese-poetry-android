package com.poetry.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * 学习数据库 —— Room 数据库单例，管理 3 张表：learning_records、daily_stats、user_profile。
 *
 * 使用 DCL（双重检查锁定）单例模式，保证线程安全。
 * 数据库文件：poetry_learning.db（存储在应用私有目录）。
 */
@Database(entities = {LearningRecord.class, DailyStats.class, UserProfile.class}, version = 4, exportSchema = false)
public abstract class LearningDatabase extends RoomDatabase {

    /** 单例（volatile 保证可见性） */
    private static volatile LearningDatabase instance;

    /** 获取 PoemDao 实例 */
    public abstract PoemDao poemDao();

    /** v1 → v2 数据库迁移：新增 daily_stats 表 */
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

    /** v2 → v3 数据库迁移：为 learning_records 添加查询索引 */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE INDEX IF NOT EXISTS index_learning_records_learnedAt ON learning_records(learnedAt)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_learning_records_favorite ON learning_records(favorite)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_learning_records_quizScore ON learning_records(quizScore)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_learning_records_gamePlayed ON learning_records(gamePlayed)");
        }
    };

    /** v3 → v4 数据库迁移：为 user_profile 添加 currentTheme 字段 */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE user_profile ADD COLUMN currentTheme TEXT NOT NULL DEFAULT 'default'");
        }
    };

    /**
     * 获取数据库实例（DCL 单例）。
     * 首次调用时创建数据库文件并添加迁移策略。
     *
     * @param context 应用上下文
     * @return 数据库实例
     */
    public static LearningDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (LearningDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        LearningDatabase.class,
                        "poetry_learning.db"
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build();
                }
            }
        }
        return instance;
    }
}
