package com.poetry.util;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「月光祝福」—— M10 惊喜彩蛋工具（GAME_REDESIGN_FINAL.md §7.3）。
 * <p>
 * 纯增益计分：命中祝福轮/次的基础得分翻倍（×2），从不会扣分。
 * 每局<strong>至多触发 2 次</strong>，且判定是 <strong>确定性的</strong>：
 * 对给定 (seed, totalRounds)，先给每个索引一个稳定哈希分，再取分数最小的
 * {@link #MAX_BLESSINGS_PER_GAME} 个索引作为祝福轮。因此无论玩家如何重开此局，
 * 祝福轮次集合都不变，体验一致、可复现。
 * </p>
 * <p>
 * 用法：开局把本局起始时间戳作为 seed 传入；对每一轮/次（roundIndex 从 0 起）
 * 调用 {@link #isBlessedRound(long, int, int)} 判断是否命中；命中且答对/配对成功时，
 * 用 {@link #applyDouble(int, boolean)} 把该次所得基础分翻倍追加。
 * </p>
 */
public final class MoonBlessing {

    /** 每局最多命中祝福的次数。 */
    public static final int MAX_BLESSINGS_PER_GAME = 2;

    /** 每局祝福索引缓存：key = seed + ":" + totalRounds → 升序索引数组。 */
    private static final ConcurrentHashMap<String, int[]> CACHE = new ConcurrentHashMap<>();

    private MoonBlessing() {
    }

    /**
     * 判断某一轮/次是否命中「月光祝福」。
     * <p>
     * 确定性实现：对给定 seed 与 totalRounds，对每个索引计算稳定哈希分，
     * 分数最小的 {@link #MAX_BLESSINGS_PER_GAME} 个索引为祝福轮。结果缓存于
     * {@link #CACHE}，同参调用恒定返回，不因重开局漂移。
     * </p>
     *
     * @param seed        本局种子（推荐开局 System.currentTimeMillis()）
     * @param roundIndex  本轮/次索引（从 0 开始）
     * @param totalRounds 本局总轮/次上限（≥ 2）
     * @return true 表示这一轮/次命中月光祝福
     */
    public static boolean isBlessedRound(long seed, int roundIndex, int totalRounds) {
        if (roundIndex < 0 || totalRounds < 2) return false;
        if (roundIndex >= totalRounds) return false;
        int[] blessed = CACHE.computeIfAbsent(
                seed + ":" + totalRounds, k -> computeBlessed(seed, totalRounds));
        return Arrays.binarySearch(blessed, roundIndex) >= 0;
    }

    /**
     * 计算给定 (seed, totalRounds) 的祝福索引集合（升序，长度 = min(2, totalRounds)）。
     */
    private static int[] computeBlessed(long seed, int totalRounds) {
        long[] scores = new long[totalRounds];
        Integer[] indexes = new Integer[totalRounds];
        for (int i = 0; i < totalRounds; i++) {
            indexes[i] = i;
            scores[i] = hashScore(seed, i);
        }
        // 稳定排序：分小者优先；同分按索引小者优先（保证确定性）
        Arrays.sort(indexes, (a, b) -> {
            int c = Long.compare(scores[a], scores[b]);
            return c != 0 ? c : Integer.compare(a, b);
        });
        int count = Math.min(MAX_BLESSINGS_PER_GAME, totalRounds);
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            result[i] = indexes[i];
        }
        Arrays.sort(result);
        return result;
    }

    /**
     * 稳定哈希分：Mix 64 位乘性哈希（Knuth 常数），取高 32 位保证分布均匀。
     */
    private static long hashScore(long seed, int roundIndex) {
        long h = seed ^ (roundIndex * 0x9E3779B97F4A7C15L);
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        return h >>> 32;
    }

    /**
     * 应用月光祝福：命中时把基础分翻倍（纯增益，非命中时原样返回）。
     *
     * @param baseScore 该次所得基础分（≥ 0）
     * @param blessed   是否命中祝福（建议传 {@link #isBlessedRound} 的结果）
     * @return baseScore 的 ×2 或原值
     */
    public static int applyDouble(int baseScore, boolean blessed) {
        int base = Math.max(0, baseScore);
        return blessed ? base * 2 : base;
    }
}