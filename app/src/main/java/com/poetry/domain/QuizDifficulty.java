package com.poetry.domain;

/**
 * 填空题型难度分级 —— 由大厅 / 关卡进入时传入，驱动挖空数量、候选字数量与干扰项策略。
 *
 * <p>- {@link #EASY}：挖 1 空、选自"有释义的名篇"、干扰项字数偏离 → 好答，几乎零门槛；
 * - {@link #NORMAL}：挖 1-2 空、任意诗、同音干扰适度；
 * - {@link #HARD}：挖 2-3 空、同音 + 结构相似干扰、答案多义字优先。
 */
public enum QuizDifficulty {

    /** 入门（低龄首局默认，几乎零门槛） */
    EASY(1, 5),
    /** 普通（默认难度） */
    NORMAL(2, 8),
    /** 挑战（高年级 / 高难度进阶） */
    HARD(3, 10);

    /** 默认挖空数量上限 */
    public final int maxBlanks;
    /** 候选字（含干扰项）数量 */
    public final int candidateCount;

    QuizDifficulty(int maxBlanks, int candidateCount) {
        this.maxBlanks = maxBlanks;
        this.candidateCount = candidateCount;
    }
}