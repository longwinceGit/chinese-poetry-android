package com.poetry.data;

import android.content.res.AssetManager;

import com.poetry.data.model.Poem;
import com.poetry.PoemLoader;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 诗词数据仓库（单例）。
 *
 * <p>负责管理诗词数据的加载、分类、搜索与缓存。
 * 内部维护诗词列表、分类列表、倒排索引等核心数据结构，
 * 对外提供统一的诗词数据访问接口。
 *
 * <p>使用方式：
 * <pre>{@code
 *   PoemRepository repo = PoemRepository.getInstance();
 *   Future<List<Poem>> future = repo.loadPoemsAsync(assets);
 *   List<Poem> results = repo.search("李白");
 * }</pre>
 *
 * @see Poem
 * @see PoemLoader
 */
public class PoemRepository {

    private static PoemRepository instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private List<Poem> allPoems = new ArrayList<>();
    /** 著名诗词列表（有释义的），用于每日推荐优先选取 */
    private List<Poem> famousPoems = new ArrayList<>();
    private List<String> categories = new ArrayList<>();
    private List<String> categoryIcons = new ArrayList<>();
    private boolean loaded = false;
    private boolean indicesBuilt = false;

    // 🔴 B3 修复：搜索倒排索引（字符级）
    private final Map<Character, Set<Integer>> titleCharIndex = new HashMap<>();
    private final Map<Character, Set<Integer>> authorCharIndex = new HashMap<>();

    private PoemRepository() {}

    /**
     * 获取仓库的唯一实例。
     *
     * @return PoemRepository 单例实例
     */
    public static synchronized PoemRepository getInstance() {
        if (instance == null) {
            instance = new PoemRepository();
        }
        return instance;
    }

    /**
     * 异步加载所有诗词数据。
     *
     * <p>从 assets 中加载诗词资源，加载完成后自动按"著名诗词优先"排序
     * （有释义的诗词排在前面），并构建分类列表。
     * 搜索倒排索引在后台延迟构建，通过 {@link #warmupIndices()} 触发。
     *
     * @param assets 安卓 AssetManager，用于读取内置诗词资源文件
     * @return 返回一个 {@link Future}，可通过 {@code get()} 获取加载完成后的全部诗词列表
     */
    public Future<List<Poem>> loadPoemsAsync(AssetManager assets) {
        return executor.submit(new Callable<List<Poem>>() {
            @Override
            public List<Poem> call() throws Exception {
                List<Poem> poems = PoemLoader.loadAll(assets);
                // 著名诗词（有释义）排在前面
                Collections.sort(poems, new Comparator<Poem>() {
                    @Override
                    public int compare(Poem a, Poem b) {
                        boolean aFamous = a.explanation != null && !a.explanation.isEmpty();
                        boolean bFamous = b.explanation != null && !b.explanation.isEmpty();
                        if (aFamous && !bFamous) return -1;
                        if (!aFamous && bFamous) return 1;
                        return 0;
                    }
                });
                allPoems = poems;
                // 构建著名诗词列表（有释义的诗词，用于每日推荐）
                famousPoems = new ArrayList<>();
                for (Poem p : poems) {
                    if (p.explanation != null && !p.explanation.isEmpty()) {
                        famousPoems.add(p);
                    }
                }
                loaded = true;
                buildCategories();
                return poems;
            }
        });
    }

    /**
     * 在后台构建搜索倒排索引（耗时操作，应在 UI 展示后异步调用）。
     * <p>
     * 索引构建完成后，后续搜索将使用倒排索引加速；
     * 构建完成前搜索自动降级为线性扫描。
     */
    public void warmupIndices() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                buildIndices();
                indicesBuilt = true;
            }
        });
    }

    /**
     * 判断搜索倒排索引是否已构建完成。
     *
     * @return {@code true} 表示索引已就绪，搜索将使用加速路径
     */
    public boolean isIndicesBuilt() {
        return indicesBuilt;
    }

    /**
     * 获取全部诗词列表。
     *
     * @return 所有已加载诗词的列表，若尚未加载则返回空列表
     */
    public List<Poem> getAllPoems() {
        return allPoems;
    }

    /**
     * 按分类获取诗词列表。
     *
     * <p>分类筛选规则：
     * <ul>
     *   <li>"全部" → 返回所有诗词的副本</li>
     *   <li>"其他" → 返回不属于任何已知朝代（先秦~近现代）的诗词</li>
     *   <li>其他 → 返回朝代与分类名称精确匹配的诗词</li>
     * </ul>
     *
     * @param category 分类名称，如"唐代"、"宋代"、"全部"、"其他"等
     * @return 匹配该分类的诗词列表
     */
    public List<Poem> getPoemsByCategory(String category) {
        if ("全部".equals(category)) {
            return new ArrayList<>(allPoems);
        }
        List<Poem> result = new ArrayList<>();
        if ("其他".equals(category)) {
            // "其他"分类：收录不属于任何已知朝代的诗词
            Set<String> knownDynasties = new HashSet<>(java.util.Arrays.asList(
                "先秦", "春秋", "春秋战国", "魏晋", "唐代", "五代",
                "宋代", "元代", "明代", "清代", "近现代"
            ));
            for (Poem p : allPoems) {
                if (p.dynasty == null || p.dynasty.isEmpty()
                    || !knownDynasties.contains(p.dynasty)) {
                    result.add(p);
                }
            }
            return result;
        }
        for (Poem p : allPoems) {
            if (category.equals(p.category)) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * 搜索诗词（B3 修复：倒排索引 + 缓存 fullText）。
     * 1. 字符级倒排索引缩小候选集
     * 2. 全文缓存避免重复 StringBuilder 拼接
     * 3. contains 精确验证
     *
     * <p>采用四级搜索策略：
     * <ol>
     *   <li>标题字符交集：用标题倒排索引取所有查询字符的交集</li>
     *   <li>作者字符并集：并上作者倒排索引的匹配结果</li>
     *   <li>退化全量扫描：若候选集为空，退化为全量遍历</li>
     *   <li>精确验证：在候选集中用 contains 匹配标题、作者及全文缓存</li>
     * </ol>
     * 搜索结果上限为 500 条。
     *
     * @param query 搜索关键词，支持标题、作者、全文内容匹配
     * @return 匹配的诗词列表，若查询为空则返回空列表
     */
    public List<Poem> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String q = query.trim();
        char[] chars = q.toCharArray();

        Set<Integer> candidates = new HashSet<>();

        // 第一步：用标题字符索引做交集，缩小候选范围
        for (int i = 0; i < chars.length; i++) {
            Set<Integer> charMatches = titleCharIndex.get(chars[i]);
            if (charMatches != null) {
                if (i == 0 && candidates.isEmpty()) {
                    candidates = new HashSet<>(charMatches);
                } else {
                    candidates.retainAll(charMatches);
                }
            }
        }

        // 第二步：并上作者字符索引的匹配
        for (char c : chars) {
            Set<Integer> m = authorCharIndex.get(c);
            if (m != null) candidates.addAll(m);
        }

        // 第三步：如果没有候选（如全符号查询），退化为全量扫描
        if (candidates.isEmpty()) {
            for (int i = 0; i < allPoems.size(); i++) candidates.add(i);
        }

        // 第四步：在候选集中精确匹配（fullTextCached 无需拼接）
        List<Poem> results = new ArrayList<>();
        for (int idx : candidates) {
            Poem p = allPoems.get(idx);
            if (p.title.contains(q) || p.author.contains(q)
                || p.fullTextCached.contains(q)) {
                results.add(p);
                if (results.size() >= 500) break;
            }
        }
        return results;
    }

    /**
     * 根据诗词 ID 查找诗词。
     *
     * <p>在线性时间复杂度 O(n) 下遍历全量诗词进行匹配。
     *
     * @param id 诗词的唯一标识符
     * @return 匹配的诗词对象，若未找到则返回 {@code null}
     */
    public Poem findPoemById(String id) {
        for (Poem p : allPoems) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    /**
     * 基于日期 Hash 固定每日推荐 —— 同一天始终返回同一首诗。
     * 保证"每日推荐"概念名副其实。
     *
     * <p>优先从著名诗词（有释义）中选取：将当日日期字符串的 hashCode
     * 取模著名诗词总数来确定索引。若著名诗词列表为空，则降级为全量选取。
     * 确保同一天内多次调用返回相同诗词。
     *
     * @return 当日推荐的诗词，若诗词库为空则返回 {@code null}
     */
    public Poem getDailyPoem() {
        // 优先从著名诗词中选取
        List<Poem> pool = famousPoems.isEmpty() ? allPoems : famousPoems;
        if (pool.isEmpty()) return null;
        String dateKey = LocalDate.now().toString();
        int idx = Math.abs(dateKey.hashCode()) % pool.size();
        return pool.get(idx);
    }

    /**
     * 获取一首随机诗词（保留随机接口供其他场景使用，如游戏等）。
     *
     * <p>使用 {@link Math#random()} 生成随机索引，每次调用结果可能不同。
     * 如需每日固定推荐，请使用 {@link #getDailyPoem()}。
     *
     * @return 随机选取的诗词，若诗词库为空则返回 {@code null}
     */
    public Poem getRandomPoem() {
        if (allPoems.isEmpty()) return null;
        int idx = (int) (Math.random() * allPoems.size());
        return allPoems.get(idx);
    }

    /**
     * 获取所有诗词分类名称列表。
     *
     * <p>分类由 {@link #buildCategories()} 在数据加载完成后自动构建，
     * 首项固定为"全部"。
     *
     * @return 分类名称字符串列表
     * @see #getCategoryIcons()
     */
    public List<String> getCategories() {
        return categories;
    }

    /**
     * 获取与 {@link #getCategories()} 一一对应的分类图标列表。
     *
     * <p>每个分类对应一个 emoji 图标，按相同索引对齐。
     *
     * @return 图标字符串列表
     * @see #getCategories()
     */
    public List<String> getCategoryIcons() {
        return categoryIcons;
    }

    /**
     * 判断诗词数据是否已加载完成。
     *
     * @return {@code true} 表示数据已加载完毕，{@code false} 表示尚未加载或加载中
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * 构建搜索倒排索引 + 缓存 fullText（B3 修复）。
     * 字符级索引：键=单个汉字，值=包含该字的诗词 index 集合。
     *
     * <p>在诗词加载完成后调用，建立两套字符级倒排索引：
     * <ul>
     *   <li>{@code titleCharIndex}：标题字符 → 诗词位置集合</li>
     *   <li>{@code authorCharIndex}：作者字符 → 诗词位置集合</li>
     * </ul>
     * 同时为每首诗词缓存其全文内容，避免后续搜索时重复拼接。
     */
    private void buildIndices() {
        titleCharIndex.clear();
        authorCharIndex.clear();

        for (int i = 0; i < allPoems.size(); i++) {
            Poem p = allPoems.get(i);
            // 缓存全文（避免每次搜索 StringBuilder 拼接）
            p.fullTextCached = p.getFullText();

            // 标题字符索引
            for (char c : p.title.toCharArray()) {
                titleCharIndex
                    .computeIfAbsent(c, k -> new HashSet<>())
                    .add(i);
            }
            // 作者字符索引
            for (char c : p.author.toCharArray()) {
                authorCharIndex
                    .computeIfAbsent(c, k -> new HashSet<>())
                    .add(i);
            }
        }
    }

    /**
     * 构建分类列表与对应图标。
     *
     * <p>基于诗词数据中的朝代信息，按照预设的朝代展示顺序构建分类。
     * 分类列表首项固定为"全部"，后续按历史顺序排列各朝代，
     * 末尾固定添加"其他"分类（收录不属于任何已知朝代的诗词）。
     * 同时为每个分类分配对应的 emoji 图标。
     *
     * @see #getDynastyIcon(String)
     */
    private void buildCategories() {
        Set<String> dynasties = new LinkedHashSet<>();
        java.util.Map<String, Integer> dynastyCounts = new java.util.HashMap<>();
        for (Poem p : allPoems) {
            if (p.dynasty != null && !p.dynasty.isEmpty()) {
                dynasties.add(p.dynasty);
                Integer cnt = dynastyCounts.get(p.dynasty);
                dynastyCounts.put(p.dynasty, cnt == null ? 1 : cnt + 1);
            }
        }
        String[] navOrder = {"先秦", "春秋", "春秋战国", "魏晋", "唐代", "五代", "宋代", "元代", "明代", "清代", "近现代"};

        List<String> ordered = new ArrayList<>();
        for (String d : navOrder) {
            if (dynasties.contains(d)) ordered.add(d);
        }

        categories.clear();
        categoryIcons.clear();
        categories.add("全部");
        categoryIcons.add("\uD83D\uDCDA");
        for (String d : ordered) {
            categories.add(d);
            categoryIcons.add(getDynastyIcon(d));
        }
        // 末尾固定添加"其他"分类
        categories.add("其他");
        categoryIcons.add("\uD83D\uDCD6");
    }

    /**
     * 根据朝代名称返回对应的 emoji 图标。
     *
     * <p>各朝代与其代表图标映射如下：
     * <ul>
     *   <li>先秦 / 春秋 / 春秋战国 → 📜（竹简）</li>
     *   <li>魏晋 → 🍂（落叶）</li>
     *   <li>唐代 → 🏯（城堡）</li>
     *   <li>五代 → 🌊（波浪）</li>
     *   <li>宋代 → 🎋（竹）</li>
     *   <li>元代 → 🐎（马）</li>
     *   <li>明代 → 🎭（戏剧面具，明代戏曲繁荣）</li>
     *   <li>清代 → 🏮（灯笼）</li>
     *   <li>近现代 → 🌅（日出，新时代）</li>
     *   <li>其他 → 📖（书本，默认）</li>
     * </ul>
     *
     * @param dynasty 朝代名称
     * @return 对应朝代的 emoji 图标字符串
     */
    private String getDynastyIcon(String dynasty) {
        switch (dynasty) {
            case "先秦": case "春秋": case "春秋战国": return "\uD83D\uDCDC";
            case "魏晋": return "\uD83C\uDF42";
            case "唐代": return "\uD83C\uDFEF";
            case "五代": return "\uD83C\uDF0A";
            case "宋代": return "\uD83C\uDF8B";
            case "元代": return "\uD83D\uDC0E";
            case "明代": return "\uD83C\uDFAD";
            case "清代": return "\uD83C\uDFEE";
            case "近现代": return "\uD83C\uDF05";
            default: return "\uD83D\uDCD6";
        }
    }
}
