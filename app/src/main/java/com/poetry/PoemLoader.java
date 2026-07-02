package com.poetry;

import android.content.res.AssetManager;

import com.poetry.data.model.Poem;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 诗词数据加载器。
 *
 * <p>负责从 Android assets 目录中加载诗词 JSON 数据，并解析为 {@link Poem} 对象列表。
 * 数据文件结构：
 * <ul>
 *   <li><b>web/data/nav.json</b> - 导航文件，定义各朝代对应的诗词数据文件列表</li>
 *   <li><b>web/data/*.json</b> - 各朝代的诗词数据文件</li>
 *   <li><b>poem_explanations.json</b> - 诗词释义数据（可选）</li>
 * </ul>
 *
 * <p>优化说明：使用 {@link JSONTokener} 进行流式解析，避免将整个文件读入内存后再解析，
 * 减少中间 {@link String} 对象的内存占用，适合处理较大的诗词数据文件。
 *
 * @author Poetry App Team
 * @version 2.0
 * @since 2024
 */
public class PoemLoader {

    /** 随机数生成器，用于随机 emoji 选择 */
    private static final Random RANDOM = new Random();

    /**
     * 从 assets 目录加载全部诗词数据。
     *
     * <p>加载流程：
     * <ol>
     *   <li>解析 <b>nav.json</b> 导航文件，获取各朝代对应的数据文件列表</li>
     *   <li>遍历每个朝代的所有数据文件，解析其中的诗词数据</li>
     *   <li>尝试加载释义数据（poem_explanations.json），并为匹配的诗词设置释义</li>
     * </ol>
     *
     * <p>释义匹配规则：使用 {@code "标题|作者"} 作为 key，与释义文件中的 {@code "k"} 字段进行匹配。
     * 若释义数据不存在或不完整，会捕获异常并继续执行，不影响主流程。
     *
     * @param assets Android AssetManager，用于访问 assets 目录中的文件
     * @return 包含所有诗词的列表，每个诗词解析为 {@link Poem} 对象
     * @throws Exception 当必要的诗词数据文件无法读取或解析失败时抛出
     * @see #readJsonArray(AssetManager, String)
     * @see #parsePoem(JSONObject, String, int)
     * @see #loadExplanations(AssetManager)
     */
    public static List<Poem> loadAll(AssetManager assets) throws Exception {
        List<Poem> all = new ArrayList<>();

        // 1. 解析 nav.json 导航文件
        // nav.json 结构: [{"dynasty": "唐代", "files": ["tang/poem_0.json", ...]}, ...]
        JSONArray nav = readJsonArray(assets, "web/data/nav.json");

        for (int i = 0; i < nav.length(); i++) {
            JSONObject dyn = nav.getJSONObject(i);
            String dynasty = dyn.getString("dynasty");  // 朝代名称，如 "唐代"
            JSONArray files = dyn.getJSONArray("files");  // 该朝代对应的所有数据文件

            for (int j = 0; j < files.length(); j++) {
                String file = "web/data/" + files.getString(j);  // 拼接完整文件路径
                JSONArray poems = readJsonArray(assets, file);

                for (int k = 0; k < poems.length(); k++) {
                    JSONObject p = poems.getJSONObject(k);
                    Poem poem = parsePoem(p, dynasty, all.size());
                    all.add(poem);
                }
            }
        }

        // 2. 加载释义数据并匹配
        // 释义匹配使用 "title|author" 作为 key
        try {
            Map<String, String> explanations = loadExplanations(assets);
            for (Poem poem : all) {
                String key = poem.explanationKey();  // 格式: "标题|作者"
                String exp = explanations.get(key);
                if (exp != null) {
                    poem.explanation = exp;
                }
            }
        } catch (Exception e) {
            // 释义数据不存在或不完整，不影响主流程
            e.printStackTrace();
        }

        return all;
    }

    /**
     * 加载诗词释义映射表。
     *
     * <p>从 <b>poem_explanations.json</b> 文件中读取释义数据，解析为 Map 结构。
     * 文件格式：JSON 数组，每个元素包含：
     * <ul>
     *   <li><b>k</b> - 键值，格式为 "标题|作者"</li>
     *   <li><b>e</b> - 释义内容</li>
     * </ul>
     *
     * @param assets Android AssetManager，用于访问 assets 目录
     * @return 释义映射表，key 为 "标题|作者"，value 为释义内容
     * @throws Exception 当释义文件无法读取或解析失败时抛出
     * @see Poem#explanationKey()
     */
    private static Map<String, String> loadExplanations(AssetManager assets) throws Exception {
        Map<String, String> map = new HashMap<>();
        JSONArray arr = readJsonArray(assets, "poem_explanations.json");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            String key = obj.getString("k");  // "title|author" 格式
            String explanation = obj.getString("e");  // 释义内容
            map.put(key, explanation);
        }
        return map;
    }

    /**
     * 从 assets 目录读取 JSON 文件并解析为 JSONArray。
     *
     * <p>实现说明：
     * <ol>
     *   <li>使用 {@link BufferedReader} 按行读取文件内容</li>
     *   <li>通过 {@link JSONTokener} 进行流式解析，避免一次性加载大文件到内存</li>
     *   <li>确保 {@link InputStream} 在方法结束时被正确关闭</li>
     * </ol>
     *
     * <p>注意：{@link JSONTokener} 不支持直接从 {@link InputStream} 构造，
     * 因此需要先读取为 {@link String} 再解析。这是 org.json 库的限制。
     *
     * @param assets Android AssetManager，用于访问 assets 目录
     * @param path   assets 目录中的文件路径，如 "web/data/nav.json"
     * @return 解析后的 JSONArray 对象
     * @throws Exception 当文件无法读取或解析结果不是 JSONArray 时抛出
     */
    private static JSONArray readJsonArray(AssetManager assets, String path) throws Exception {
        InputStream is = assets.open(path);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            // 按行读取文件内容
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            // 使用 JSONTokener 流式解析
            JSONTokener tokener = new JSONTokener(sb.toString());
            Object value = tokener.nextValue();
            if (value instanceof JSONArray) {
                return (JSONArray) value;
            }
            throw new Exception("Expected JSONArray but got " + value.getClass().getSimpleName());
        } finally {
            is.close();  // 确保流被关闭
        }
    }

    /**
     * 解析单首诗词的 JSON 对象。
     *
     * <p>JSON 对象字段说明：
     * <ul>
     *   <li><b>t</b> - 诗词标题（title）</li>
     *   <li><b>a</b> - 作者名称（author）</li>
     *   <li><b>p</b> - 诗句数组（poem lines）</li>
     * </ul>
     *
     * <p>标签映射规则（根据朝代映射到对应的标签，用于 UI 筛选）：
     * <table border="1">
     *   <caption>朝代与标签映射</caption>
     *   <tr><th>朝代</th><th>标签</th></tr>
     *   <tr><td>宋代</td><td>song</td></tr>
     *   <tr><td>先秦/春秋/春秋战国</td><td>qin</td></tr>
     *   <tr><td>魏晋</td><td>wei</td></tr>
     *   <tr><td>五代</td><td>wu</td></tr>
     *   <tr><td>元代</td><td>yuan</td></tr>
     *   <tr><td>明代</td><td>ming</td></tr>
     *   <tr><td>清代</td><td>qing</td></tr>
     *   <tr><td>近现代</td><td>modern</td></tr>
     *   <tr><td>其他（未识别朝代）</td><td>other</td></tr>
     * </table>
     *
     * <p>Emoji 分配规则：根据诗词在列表中的索引，按顺序循环分配预设的 emoji 数组。
     *
     * @param p       诗词 JSON 对象，包含标题、作者、诗句等字段
     * @param dynasty 朝代名称，用于标签映射
     * @param index   诗词在全局列表中的索引，用于生成唯一 ID 和 emoji 分配
     * @return 解析后的 {@link Poem} 对象
     */
    private static Poem parsePoem(JSONObject p, String dynasty, int index) {
        String title = p.optString("t", "无题");
        String author = p.optString("a", "佚名");

        // 解析诗句数组
        // "p" 字段存储诗词的每一行
        JSONArray pArr = p.optJSONArray("p");
        String[] lines;
        if (pArr != null && pArr.length() > 0) {
            lines = new String[pArr.length()];
            for (int i = 0; i < pArr.length(); i++) {
                lines[i] = pArr.optString(i, "");
            }
        } else {
            lines = new String[0];  // 无诗句时返回空数组
        }

        String category = dynasty;  // 分类名称使用朝代

        // 根据朝代映射到对应的标签（用于 UI 分类筛选）
        String tag;
        switch (dynasty) {
            case "唐代":
                tag = "tang";
                break;
            case "宋代":
                tag = "song";
                break;
            case "先秦":
            case "春秋":
            case "春秋战国":
                tag = "qin";
                break;
            case "魏晋":
                tag = "wei";
                break;
            case "五代":
                tag = "wu";
                break;
            case "元代":
                tag = "yuan";
                break;
            case "明代":
                tag = "ming";
                break;
            case "清代":
                tag = "qing";
                break;
            case "近现代":
                tag = "modern";
                break;
            default:
                tag = "other";  // 未识别朝代使用中性标签，不归属唐代
                break;
        }

        // 预定义的 emoji 数组，按顺序循环分配
        String[] emojis = {"📖", "🌸", "🌙", "🏔️", "🌊", "🍃", "🎋", "🦋", "🐦", "⭐"};
        String emoji = emojis[index % emojis.length];

        return new Poem(
                "d" + index, title, author, dynasty, category, tag, emoji, lines
        );
    }

    /**
     * 随机返回一个 emoji。
     *
     * <p>从预定义的 emoji 数组中随机选择一个返回，用于为诗词提供随机图标。
     *  emoji 数组包含 15 个与诗词主题相关的表情符号。
     *
     * @return 随机选择的 emoji 字符串
     */
    public static String randomEmoji() {
        String[] e = {"📖", "🌸", "🌙", "🏔️", "🌊", "🍃", "🎋", "🦋", "🐦", "⭐", "🎵", "💫", "🍂", "🌿", "🕊️"};
        return e[RANDOM.nextInt(e.length)];
    }
}
