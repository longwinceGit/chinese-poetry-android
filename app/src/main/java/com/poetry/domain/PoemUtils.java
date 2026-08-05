package com.poetry.domain;

import com.poetry.data.model.Poem;

/**
 * 诗词工具 —— 统一的可用于游戏的诗句/联句过滤规则。
 *
 * <p>收敛原有散落在 {@code GameEngine} / {@code QuizGenerator} 中重复的判空逻辑，
 * 所有游戏模式统一调用本类，避免"哪些诗句能入题"规则四处漂移。
 */
public final class PoemUtils {

    private PoemUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 判断一行诗句是否可用于出题。
     *
     * <p>可用条件：非空、长度 ≥ 2、不含省略号（…/……）、去除首尾空白后不为空。
     *
     * @param line 诗句文本
     * @return 可用返回 true
     */
    public static boolean isUsableLine(String line) {
        return line != null
                && line.length() >= 2
                && !line.contains("…")
                && !line.contains("……")
                && !line.trim().isEmpty();
    }

    /**
     * 判断从第 {@code i} 句开始的相邻两句（i 与 i+1）是否构成一对可用联句。
     *
     * @param poem 诗词对象
     * @param i    起始行索引（上句索引）
     * @return 上下句均可用返回 true
     */
    public static boolean isUsablePair(Poem poem, int i) {
        return poem != null
                && poem.lines != null
                && i + 1 < poem.lines.length
                && isUsableLine(poem.lines[i])
                && isUsableLine(poem.lines[i + 1]);
    }

    /**
     * 判断一首诗是否至少存在一对可用于出题的联句。
     *
     * @param poem 诗词对象
     * @return 至少有一对可用返回 true
     */
    public static boolean hasUsablePair(Poem poem) {
        if (poem == null || poem.lines == null || poem.lines.length < 2) return false;
        for (int i = 0; i < poem.lines.length - 1; i++) {
            if (isUsablePair(poem, i)) return true;
        }
        return false;
    }
}