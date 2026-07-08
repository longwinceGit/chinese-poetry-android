package com.poetry.util;

import java.util.List;

/**
 * P0-A A1 修复效果对比基准（修正版）。
 * <p>
 * 关键设计：每次迭代都 clearCache()，这样能测到 pinyin4j 的真实 JNI 开销。
 * </p>
 */
public class PinyinHelperBenchmark {

    public static void main(String[] args) {
        // 床前明月光，疑是地上霜。举头望明月，低头思故乡。 (28 字符：22 字 + 6 标点)
        String poem = "床前明月光，疑是地上霜。举头望明月，低头思故乡。";
        int iterations = 100;

        // ===== 无缓存基线：每次都清空，逼着走 pinyin4j =====
        PinyinHelper.clearCache();
        long t0 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            PinyinHelper.clearCache();
            List<String> list = PinyinHelper.toPinyinList(poem);
            if (list.size() != poem.length()) throw new AssertionError("size");
        }
        long t1 = System.nanoTime();
        long noCacheMs = (t1 - t0) / 1_000_000;
        System.out.println("[无缓存] " + iterations + " 次（每次清缓存）: " + noCacheMs + " ms");

        // ===== 有缓存：预热一次后连续查询 =====
        // 预热
        PinyinHelper.clearCache();
        for (int i = 0; i < 20; i++) {
            PinyinHelper.toPinyinList(poem);
        }
        long t2 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            List<String> list = PinyinHelper.toPinyinList(poem);
            if (list.size() != poem.length()) throw new AssertionError("size");
        }
        long t3 = System.nanoTime();
        long withCacheMs = (t3 - t2) / 1_000_000;
        System.out.println("[有缓存] " + iterations + " 次（命中缓存）: " + withCacheMs + " ms");

        if (withCacheMs > 0) {
            double speedup = (double) noCacheMs / withCacheMs;
            System.out.println(String.format("加速比: %.1fx", speedup));
        }
        System.out.println("最终缓存大小: " + PinyinHelper.cacheSize() + "（应等于诗中不同字符数）");
    }
}
