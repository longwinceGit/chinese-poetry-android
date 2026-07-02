package com.poetry.data.model;

/**
 * 诗词数据模型 —— 诗词的核心数据结构。
 *
 * 字段来源：assets/web/data/*.json 分片文件，经 PoemLoader 解析后填充。
 * 释义字段从 poem_explanations.json 独立加载，通过 explanationKey() 匹配。
 *
 * @see com.poetry.PoemLoader  数据加载器
 */
public class Poem {

    /** 唯一标识，格式 "d{index}"，如 "d0" */
    public String id;

    /** 诗词标题 */
    public String title;

    /** 作者 */
    public String author;

    /** 朝代（唐代/宋代/先秦/魏晋/五代/元代/明代/清代/近现代） */
    public String dynasty;

    /** 拼音字符串（预留，暂未使用） */
    public String pinyin;

    /** 标签（英文简称，用于颜色映射：tang/song/qin/wei/yuan/ming/qing/modern/wu/other） */
    public String tag;

    /** 分类（= dynasty，兼容旧字段） */
    public String category;

    /** Emoji 图标 */
    public String emoji;

    /** 诗句数组，每行一句 */
    public String[] lines;

    /** 诗词释义（从 poem_explanations.json 加载） */
    public String explanation;

    /** 缓存全文字符串，避免每次搜索重复拼接（B3 修复） */
    public String fullTextCached;

    public Poem() {}

    /**
     * 构造一首诗词。
     *
     * @param id       唯一标识
     * @param title    标题
     * @param author   作者
     * @param dynasty  朝代
     * @param category 分类
     * @param tag      标签（英文简称）
     * @param emoji    emoji 图标
     * @param lines    诗句数组
     */
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

    /** @return 第一句诗句，用于列表摘要展示 */
    public String getFirstLine() {
        if (lines == null || lines.length == 0) return null;
        return lines[0];
    }

    /**
     * 获取诗词全文（所有诗句用换行符拼接）。
     * 注：已有 fullTextCached 缓存，搜索等高频场景应直接读缓存字段。
     *
     * @return 全文，不含首尾空白
     */
    public String getFullText() {
        if (lines == null || lines.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            sb.append(l).append("\n");
        }
        return sb.toString().trim();
    }

    /** @return true 如果该诗词有释义数据 */
    public boolean hasExplanation() {
        return explanation != null && !explanation.isEmpty();
    }

    /**
     * 生成释义查找 key，格式为 "标题|作者"。
     * 与 poem_explanations.json 中每条记录的 "k" 字段完全一致。
     *
     * @return 释义匹配 key
     */
    public String explanationKey() {
        return (title != null ? title : "") + "|" + (author != null ? author : "");
    }
}
