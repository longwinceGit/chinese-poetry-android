package com.poetry.util;

import android.os.Bundle;

import com.poetry.data.model.Poem;

/**
 * 导航参数工具类 —— 统一诗词详情页的 Bundle 打包/解包。
 *
 * <p>消除所有 Fragment 中散落的魔字符串 key（"poem_id", "poem_title"...），
 * 提供类型安全的参数传递。修改字段名只需改此一处。</p>
 *
 * <p>使用方式：
 * <pre>{@code
 *   // 发送方
 *   Bundle args = PoemArgs.fromPoem(poem).toBundle();
 *   navController.navigate(R.id.nav_detail, args);
 *
 *   // 接收方
 *   PoemArgs args = PoemArgs.fromBundle(getArguments());
 *   textView.setText(args.getTitle());
 * }</pre>
 */
public class PoemArgs {

    private static final String KEY_ID = "poem_id";
    private static final String KEY_TITLE = "poem_title";
    private static final String KEY_AUTHOR = "poem_author";
    private static final String KEY_DYNASTY = "poem_dynasty";
    private static final String KEY_LINES = "poem_lines";
    private static final String KEY_EXPLANATION = "poem_explanation";

    public final String id;
    public final String title;
    public final String author;
    public final String dynasty;
    public final String[] lines;
    public final String explanation;

    private PoemArgs(String id, String title, String author, String dynasty,
                     String[] lines, String explanation) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.dynasty = dynasty;
        this.lines = lines;
        this.explanation = explanation;
    }

    /** 从 Poem 对象构建导航参数 */
    public static PoemArgs fromPoem(Poem poem) {
        return new PoemArgs(
            poem.id,
            poem.title,
            poem.author,
            poem.dynasty,
            poem.lines,
            poem.explanation != null ? poem.explanation : ""
        );
    }

    /** 从 Bundle 解包导航参数 */
    public static PoemArgs fromBundle(Bundle bundle) {
        if (bundle == null) {
            return new PoemArgs("", "", "", "", new String[0], "");
        }
        return new PoemArgs(
            bundle.getString(KEY_ID, ""),
            bundle.getString(KEY_TITLE, ""),
            bundle.getString(KEY_AUTHOR, ""),
            bundle.getString(KEY_DYNASTY, ""),
            bundle.getStringArray(KEY_LINES),
            bundle.getString(KEY_EXPLANATION, "")
        );
    }

    /** 打包到 Bundle（供 Navigation Component 传递） */
    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putString(KEY_ID, id);
        b.putString(KEY_TITLE, title);
        b.putString(KEY_AUTHOR, author);
        b.putString(KEY_DYNASTY, dynasty);
        b.putStringArray(KEY_LINES, lines);
        b.putString(KEY_EXPLANATION, explanation);
        return b;
    }

    // 便捷 getter（用于 DetailFragment 解包）
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getDynasty() { return dynasty; }
    public String[] getLines() { return lines; }
    public String getExplanation() { return explanation; }

    public boolean hasExplanation() {
        return explanation != null && !explanation.isEmpty();
    }
}
