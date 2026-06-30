package com.poetry;

import android.content.res.AssetManager;

import com.poetry.data.model.Poem;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 从 assets 目录加载诗词 JSON 数据
 */
public class PoemLoader {

    private static final Random RANDOM = new Random();

    /** 从 assets 加载全部诗词（后台线程调用） */
    public static List<Poem> loadAll(AssetManager assets) throws Exception {
        List<Poem> all = new ArrayList<>();

        // 1. 读取 nav.json
        String navJson = readAsset(assets, "web/data/nav.json");
        JSONArray nav = new JSONArray(navJson);

        for (int i = 0; i < nav.length(); i++) {
            JSONObject dyn = nav.getJSONObject(i);
            String dynasty = dyn.getString("dynasty");
            JSONArray files = dyn.getJSONArray("files");

            for (int j = 0; j < files.length(); j++) {
                String file = "web/data/" + files.getString(j);
                String poemJson = readAsset(assets, file);
                JSONArray poems = new JSONArray(poemJson);

                for (int k = 0; k < poems.length(); k++) {
                    JSONObject p = poems.getJSONObject(k);
                    Poem poem = parsePoem(p, dynasty, all.size());
                    all.add(poem);
                }
            }
        }
        return all;
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

        // 分类 = 朝代
        String category = dynasty;

        // tag 用于卡片标签颜色
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

        // emoji
        String[] emojis = {"📖", "🌸", "🌙", "🏔️", "🌊", "🍃", "🎋", "🦋", "🐦", "⭐"};
        String emoji = emojis[index % emojis.length];

        Poem poem = new Poem(
            "d" + index, title, author, dynasty, category, tag, emoji, lines
        );
        return poem;
    }

    /** 从 assets 读取整个文件为字符串 */
    public static String readAsset(AssetManager assets, String path) throws Exception {
        InputStream is = assets.open(path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        is.close();
        return sb.toString();
    }

    /** 随机 emoji */
    public static String randomEmoji() {
        String[] e = {"📖","🌸","🌙","🏔️","🌊","🍃","🎋","🦋","🐦","⭐","🎵","💫","🍂","🌿","🕊️"};
        return e[RANDOM.nextInt(e.length)];
    }
}
