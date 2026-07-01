package com.poetry.data.model;

public class Poem {
    public String id;
    public String title;
    public String author;
    public String dynasty;
    public String pinyin;
    public String tag;
    public String category;
    public String emoji;
    public String[] lines;
    public String explanation;

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
        this.explanation = "";
    }

    public String getFirstLine() {
        if (lines == null || lines.length == 0) return null;
        return lines[0];
    }

    public String getFullText() {
        if (lines == null || lines.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            sb.append(l).append("\n");
        }
        return sb.toString().trim();
    }

    /** 是否有释义 */
    public boolean hasExplanation() {
        return explanation != null && !explanation.isEmpty();
    }

    /** 生成释义查找 key：title|author */
    public String explanationKey() {
        return (title != null ? title : "") + "|" + (author != null ? author : "");
    }
}
