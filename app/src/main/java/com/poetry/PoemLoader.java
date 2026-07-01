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
 * 从 assets 目录加载诗词 JSON 数据。
 * 优化版：使用 JSONTokener 流式解析，避免中间 String 对象的双重内存占用。
 */
public class PoemLoader {

    private static final Random RANDOM = new Random();

    /** 从 assets 加载全部诗词（后台线程调用） */
    public static List<Poem> loadAll(AssetManager assets) throws Exception {
        List<Poem> all = new ArrayList<>();

        // 1. 解析 nav.json
        JSONArray nav = readJsonArray(assets, "web/data/nav.json");

        for (int i = 0; i < nav.length(); i++) {
            JSONObject dyn = nav.getJSONObject(i);
            String dynasty = dyn.getString("dynasty");
            JSONArray files = dyn.getJSONArray("files");

            for (int j = 0; j < files.length(); j++) {
                String file = "web/data/" + files.getString(j);
                JSONArray poems = readJsonArray(assets, file);

                for (int k = 0; k < poems.length(); k++) {
                    JSONObject p = poems.getJSONObject(k);
                    Poem poem = parsePoem(p, dynasty, all.size());
                    all.add(poem);
                }
            }
        }

        // 2. 加载释义数据并匹配
        try {
            Map<String, String> explanations = loadExplanations(assets);
            for (Poem poem : all) {
                String key = poem.explanationKey();
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

    /** 加载释义映射表 */
    private static Map<String, String> loadExplanations(AssetManager assets) throws Exception {
        Map<String, String> map = new HashMap<>();
        JSONArray arr = readJsonArray(assets, "poem_explanations.json");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            String key = obj.getString("k");  // "title|author"
            String explanation = obj.getString("e");
            map.put(key, explanation);
        }
        return map;
    }

    /** 从 assets 读取 JSONArray。org.json.JSONTokener 不支持 InputStream 构造，先读为 String */
    private static JSONArray readJsonArray(AssetManager assets, String path) throws Exception {
        InputStream is = assets.open(path);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONTokener tokener = new JSONTokener(sb.toString());
            Object value = tokener.nextValue();
            if (value instanceof JSONArray) {
                return (JSONArray) value;
            }
            throw new Exception("Expected JSONArray but got " + value.getClass().getSimpleName());
        } finally {
            is.close();
        }
    }

    /** 解析单首诗 */
    private static Poem parsePoem(JSONObject p, String dynasty, int index) {
        String title = p.optString("t", "无题");
        String author = p.optString("a", "佚名");

        // 解析诗句
        JSONArray pArr = p.optJSONArray("p");
        String[] lines;
        if (pArr != null && pArr.length() > 0) {
            lines = new String[pArr.length()];
            for (int i = 0; i < pArr.length(); i++) {
                lines[i] = pArr.optString(i, "");
            }
        } else {
            lines = new String[0];
        }

        String category = dynasty;

        String tag;
        switch (dynasty) {
            case "宋代": tag = "song"; break;
            case "先秦":
            case "春秋":
            case "春秋战国": tag = "qin"; break;
            case "魏晋": tag = "wei"; break;
            case "五代": tag = "wu"; break;
            case "元代": tag = "yuan"; break;
            case "清代": tag = "qing"; break;
            default: tag = "tang"; break;
        }

        String[] emojis = {"📖", "🌸", "🌙", "🏔️", "🌊", "🍃", "🎋", "🦋", "🐦", "⭐"};
        String emoji = emojis[index % emojis.length];

        return new Poem(
            "d" + index, title, author, dynasty, category, tag, emoji, lines
        );
    }

    /** 随机 emoji */
    public static String randomEmoji() {
        String[] e = {"📖","🌸","🌙","🏔️","🌊","🍃","🎋","🦋","🐦","⭐","🎵","💫","🍂","🌿","🕊️"};
        return e[RANDOM.nextInt(e.length)];
    }
}
