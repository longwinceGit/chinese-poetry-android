package com.poetry.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 游戏结算结果 —— 领域层中立结算对象（纯 Java，无 Android 依赖）。
 *
 * <p>所有游戏结束时统一构造本对象，再交给 {@link GameSettlement} 统一完成
 * 积分 / 成就 / 每日任务 / 主题 / 学习沉淀结算，取代原有分散在
 * {@code GameViewModel} / {@code QuizViewModel} 中的重复逻辑。
 */
public class GameResult {

    /** 游戏类型：couplet | match | quiz | flyflower | author */
    public String gameType;

    /** 本局得分（0-1000，由各游戏规则换算） */
    public int score;

    /** 星级（1-3），由规则换算；只要有参与即至少 1 星（呼应零挫败支柱） */
    public int stars;

    /** 答对数 */
    public int correctCount;

    /** 总题数 / 总轮数 */
    public int totalCount;

    /** 是否完美（满分 / 全对） */
    public boolean perfect;

    /** 本局耗时（毫秒） */
    public long durationMillis;

    /** 本局涉及到的诗词 id（用于学习沉淀：标记"玩过"而非"已学"） */
    public final List<String> touchedPoemIds;

    public GameResult() {
        this.touchedPoemIds = new ArrayList<>();
    }

    /**
     * 将本局涉及的诗词 id 加入沉淀列表（内部去重）。
     *
     * @param poemId 诗词 id
     */
    public void addTouchedPoem(String poemId) {
        if (poemId == null || poemId.isEmpty()) return;
        if (!touchedPoemIds.contains(poemId)) {
            touchedPoemIds.add(poemId);
        }
    }

    /** 返回不可变的涉及诗词 id 列表副本，避免调用方意外修改。 */
    public List<String> getTouchedPoemIds() {
        return Collections.unmodifiableList(touchedPoemIds);
    }
}