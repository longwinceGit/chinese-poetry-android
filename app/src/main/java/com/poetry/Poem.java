package com.poetry;

/**
 * 诗词数据模型
 */
public class Poem {
    public String id;
    public String title;
    public String author;
    public String dynasty;
    public String pinyin;
    public String tag;       // tang, song, qin, wei, wu, yuan, qing
    public String category;  // 朝代分类（唐代/宋代/先秦/魏晋/五代/元代/清代等）
    public String emoji;

    // 诗句数组
    public String[] lines;

    public Poem() {}

    public Poem(String id, String title, String author, String dynasty,
                String category, String tag, String emoji, String[] lines) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.dynasty = dynasty;
        this.category = category;
        this.tag = tag;
        this.emoji = emoji;
        this.lines = lines;
        this.pinyin = "";
    }

    /** 获取诗句原文（拼接） */
    public String getFullText() {
        if (lines == null || lines.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            sb.append(l).append("\n");
        }
        return sb.toString().trim();
    }
}
