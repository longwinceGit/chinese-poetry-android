package com.poetry.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 诗词学习记录 —— Room 实体，持久化用户对每首诗词的学习状态。
 *
 * 每首被标记"已学""收藏""已答题""已游戏"的诗词在此表中占一行。
 * 空行由 {@code ensureRecordExists()} 幂等插入。
 */
@Entity(tableName = "learning_records",
    indices = {
        @Index("learnedAt"),
        @Index("favorite"),
        @Index("quizScore"),
        @Index("gamePlayed")
    })
public class LearningRecord {

    /** 诗词 ID（主键），格式 "d{index}" */
    @PrimaryKey
    @NonNull
    public String poemId;

    /** 诗词标题（冗余存储，加速列表渲染） */
    public String title;

    /** 朝代 */
    public String dynasty;

    /** 作者 */
    public String author;

    /** 标记为已学的时间戳（毫秒） */
    public long learnedAt;

    /** 答题最高分 */
    public int quizScore;

    /** 是否已收藏 */
    public boolean favorite;

    /** 该诗词参与游戏次数 */
    public int gamePlayed;

    /** Room 要求无参构造 */
    public LearningRecord() {}

    /**
     * 创建一条新的学习记录（初始状态：未学/未答/未收藏/未游戏）。
     *
     * @param poemId  诗词 ID
     * @param title   标题
     * @param dynasty 朝代
     * @param author  作者
     */
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
