package com.poetry.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * 游戏历史 DAO —— 管理 game_history 表。
 *
 * <p>只做"成绩留痕 + 最高分展示"的轻量查询，避免全表加载：
 * {@link #getRecentByType} 只查最近 N=50 条供大厅/结算展示，杜绝内存膨胀。
 */
@Dao
public interface GameHistoryDao {

    /** 插入一条游戏历史记录。 */
    @Insert
    void insert(GameHistory history);

    /** 获取某类型游戏的最高分（无记录时返回 0）。 */
    @Query("SELECT COALESCE(MAX(score), 0) FROM game_history WHERE gameType = :gameType")
    int getBestScoreByType(String gameType);

    /** 获取某类型游戏的最高星（无记录时返回 0）。 */
    @Query("SELECT COALESCE(MAX(stars), 0) FROM game_history WHERE gameType = :gameType")
    int getBestStarsByType(String gameType);

    /** 获取某类型游戏最近（按时间倒序）的 N 条记录，供回廊/LiveData 展示。 */
    @Query("SELECT * FROM game_history WHERE gameType = :gameType ORDER BY playedAt DESC LIMIT :limit")
    LiveData<List<GameHistory>> getRecentByType(String gameType, int limit);

    /** 获取某类型游戏最近（按时间倒序）的记录，供结算页快速读取最新一局。 */
    @Query("SELECT * FROM game_history WHERE gameType = :gameType ORDER BY playedAt DESC LIMIT 1")
    GameHistory getLatestByTypeSync(String gameType);

    /** 获取指定日期区间内录得所有游戏记录（用于会话时长/二次进入率聚合）。 */
    @Query("SELECT * FROM game_history WHERE playedAt >= :startAt ORDER BY playedAt DESC")
    List<GameHistory> getSince(long startAt);

    /** 飞花令累计命中句数（跨全部飞花令局，用于 flyflower_20 成就）。 */
    @Query("SELECT COALESCE(SUM(correctCount),0) FROM game_history WHERE gameType = 'flyflower'")
    long getFlyflowerHitSum();

    /** 跨全部游戏的累计星级总和（用于 star_30 成就）。 */
    @Query("SELECT COALESCE(SUM(stars),0) FROM game_history")
    long getStarSum();

    /**
     * 结算页收藏计数（gameType='settlement_collect' 的合成行数量，用于 month_poem_collect 成就）。
     *
     * <p>该合成行不参与任何展示，仅作为纯领域层可查询的累计计数器（见 AchievementEngine 文档）。
     */
    @Query("SELECT COUNT(*) FROM game_history WHERE gameType = 'settlement_collect'")
    int getCollectCount();
}