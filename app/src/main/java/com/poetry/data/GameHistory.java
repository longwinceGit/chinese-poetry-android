package com.poetry.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 游戏历史 —— Room 实体，记录单局游戏的成绩留痕，用于最高分 / 星级回廊展示。
 *
 * <p>表结构见 GAME_REDESIGN_FINAL.md §11.1。只存最克制的成绩摘要，不加复杂排行榜。
 * 关联数据（玩家偏好、当日去重、难度选择）不在此表，统一走 SharedPreferences。
 */
@Entity(tableName = "game_history",
    indices = {
        @Index("gameType"),
        @Index("playedAt")
    })
public class GameHistory {

    /** 主键（自增） */
    @PrimaryKey(autoGenerate = true)
    public long id;

    /** 游戏类型：couplet | match | quiz | flyflower | author */
    @NonNull
    public String gameType;

    /** 本局得分 */
    public int score;

    /** 星级（1-3） */
    public int stars;

    /** 答对数 */
    public int correctCount;

    /** 总题数 */
    public int totalCount;

    /** 完成时间戳（毫秒） */
    public long playedAt;

    /** 本局耗时（毫秒） */
    public long durationMillis;

    /** Room 要求无参构造 */
    public GameHistory() {
        this.gameType = "";
    }

    /**
     * 创建一条游戏历史记录。
     *
     * @param gameType       游戏类型
     * @param score          本局得分
     * @param stars          星级
     * @param correctCount   答对数
     * @param totalCount     总题数
     * @param playedAt       完成时间戳
     * @param durationMillis 本局耗时
     */
    @Ignore
    public GameHistory(String gameType, int score, int stars,
                       int correctCount, int totalCount,
                       long playedAt, long durationMillis) {
        this.gameType = gameType;
        this.score = score;
        this.stars = stars;
        this.correctCount = correctCount;
        this.totalCount = totalCount;
        this.playedAt = playedAt;
        this.durationMillis = durationMillis;
    }
}