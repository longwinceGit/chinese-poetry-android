package com.poetry.domain;

import com.poetry.data.model.Poem;

/**
 * PoemUtils 过滤规则纯逻辑验证（不依赖 JUnit，沿用项目现有 main() + 手动断言风格）。
 *
 * <h3>运行方式</h3>
 * <pre>IDE 中直接以 Java Application 运行 main()</pre>
 *
 * <h3>覆盖项</h3>
 * <ul>
 *   <li>isUsableLine：空串 / 单字 / 省略号 / 空白 / 正常两句</li>
 *   <li>isUsablePair：越界 / null lines / 上句或下句不可用 / 正常可配对</li>
 *   <li>hasUsablePair：无任何可用对 / 至少一对可用</li>
 * </ul>
 */
public class PoemUtilsSelfTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testIsUsableLine();
        testIsUsablePair();
        testHasUsablePair();
        finish();
    }

    private static void finish() {
        System.out.println("\n========== PoemUtils 测试结果 ==========");
        System.out.println("通过: " + passed);
        System.out.println("失败: " + failed);
        if (failed == 0) {
            System.out.println("✅ 全部通过 — M1 PoemUtils 过滤规则验证通过");
        } else {
            System.err.println("❌ 有失败用例");
            System.exit(1);
        }
    }

    private static void assertEquals(String name, Object expected, Object actual) {
        if ((expected == null && actual == null) || (expected != null && expected.equals(actual))) {
            passed++;
            System.out.println("✓ " + name);
        } else {
            failed++;
            System.err.println("✗ " + name + " | 期望=" + expected + " 实际=" + actual);
        }
    }

    private static void assertTrue(String name, boolean cond) {
        assertEquals(name, true, cond);
    }

    private static void assertFalse(String name, boolean cond) {
        assertEquals(name, false, cond);
    }

    // ==================== isUsableLine ====================

    private static void testIsUsableLine() {
        assertFalse("null → 不可用", PoemUtils.isUsableLine(null));
        assertFalse("空串 → 不可用", PoemUtils.isUsableLine(""));
        assertFalse("单字 → 不可用", PoemUtils.isUsableLine("月"));
        assertFalse("省略号… → 不可用", PoemUtils.isUsableLine("床前明月光…"));
        assertFalse("省略号…… → 不可用", PoemUtils.isUsableLine("床前明月光……"));
        assertFalse("纯空白 → 不可用", PoemUtils.isUsableLine("   "));
        assertTrue("正常五言 → 可用", PoemUtils.isUsableLine("床前明月光"));
        assertTrue("正常七言 → 可用", PoemUtils.isUsableLine("春眠不觉晓处处闻啼鸟"));
    }

    // ==================== isUsablePair ====================

    /** 构造一首诗句数组的诗。 */
    private static Poem poem(String... lines) {
        Poem p = new Poem();
        p.id = "d-test";
        p.lines = lines;
        return p;
    }

    private static void testIsUsablePair() {
        assertFalse("null poem → false", PoemUtils.isUsablePair(null, 0));
        assertFalse("null lines → false", PoemUtils.isUsablePair(poem((String[]) null), 0));
        assertFalse("仅 1 句 → 越界 false", PoemUtils.isUsablePair(poem("床前明月光"), 0));
        assertFalse("下句单字 → false", PoemUtils.isUsablePair(poem("床前明月光", "月"), 0));
        assertFalse("下句省略号 → false", PoemUtils.isUsablePair(poem("床前明月光", "低头…"), 0));
        assertFalse("末位索引起点 → 越界 false", PoemUtils.isUsablePair(poem("a", "b", "c"), 2));
        assertTrue("正常相邻对 → true", PoemUtils.isUsablePair(poem("床前明月光", "疑是地上霜"), 0));
        assertTrue("后半联也可用 → true", PoemUtils.isUsablePair(poem("床前明月光", "疑是地上霜", "举头望明月", "低头思故乡"), 2));
    }

    // ==================== hasUsablePair ====================

    private static void testHasUsablePair() {
        assertFalse("null lines → false", PoemUtils.hasUsablePair(poem((String[]) null)));
        assertFalse("少于 2 句 → false", PoemUtils.hasUsablePair(poem("床前明月光")));
        assertFalse("全是不可用句 → false", PoemUtils.hasUsablePair(poem("月", "花")));
        assertTrue("含一对可用 → true", PoemUtils.hasUsablePair(poem("床前明月光", "疑是地上霜")));
        assertTrue("多联任一对可用 → true", PoemUtils.hasUsablePair(poem("床前明月光", "疑是地上霜", "举头望明月", "低头思故乡")));
    }
}