package com.poetry.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.poetry.R;

import java.util.List;

/**
 * 逐字拼音行视图 —— 每个汉字正上方显示拼音标注，支持多行自动折行。
 *
 * 内部结构：垂直 LinearLayout，内容行数 = ceil(len / maxCharsPerRow)。
 * 每行内各列等分可用宽度（layout_weight=1）。
 *
 * 效果示意（maxCharsPerRow=10, "关关雎鸠，在河之洲。窈窕淑女，君子好逑。"）：
 *   Row0:  guān  guān   jū    jiū    ，    zài    hé    zhī   zhōu   。
 *           关     关    雎     鸠     ，     在     河    之     洲     。
 *   Row1:  yǎo   tiǎo   shū   nǚ     ，    jūn    zǐ    hǎo   qiú    。
 *           窈     窕    淑     女     ，     君     子    好     逑     。
 */
public class PinyinLineView extends LinearLayout {

    private static final float PINYIN_SP = 10f;
    private static final float CHAR_SP  = 20f;

    private final int maxCharsPerRow;

    /**
     * @param context        上下文
     * @param line           整行诗句文本
     * @param maxCharsPerRow 每子行最大字符数（≤0 时自动取 10）
     */
    public PinyinLineView(Context context, String line, int maxCharsPerRow) {
        super(context);
        this.maxCharsPerRow = maxCharsPerRow > 0 ? maxCharsPerRow : 10;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        buildRows(line);
    }

    // ---- 构建多行 ----

    private void buildRows(String line) {
        String text = line != null ? line : "";
        if (text.isEmpty()) return;

        int total = text.length();
        int rowCount = (total + maxCharsPerRow - 1) / maxCharsPerRow; // ceil 除法

        int pinyinColor = getContext().getColor(R.color.on_surface_variant);
        int charColor   = getContext().getColor(R.color.on_surface);
        List<String> pinyinList = PinyinHelper.toPinyinList(text);

        for (int row = 0; row < rowCount; row++) {
            int start = row * maxCharsPerRow;
            int end   = Math.min(start + maxCharsPerRow, total);

            // ---- 一行水平容器 ----
            LinearLayout rowLayout = new LinearLayout(getContext());
            rowLayout.setOrientation(HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER);
            rowLayout.setLayoutParams(new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            for (int i = start; i < end; i++) {
                rowLayout.addView(buildCell(text.charAt(i), i, pinyinList, pinyinColor, charColor));
            }

            addView(rowLayout);
        }
    }

    private LinearLayout buildCell(char ch, int index, List<String> pinyinList,
                                   int pinyinColor, int charColor) {
        String py = (index < pinyinList.size()) ? pinyinList.get(index) : "";
        boolean hasPinyin = !TextUtils.isEmpty(py) && !py.equals(String.valueOf(ch));

        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        // 拼音
        TextView topTv = new TextView(getContext());
        topTv.setSingleLine(true);
        topTv.setEllipsize(TextUtils.TruncateAt.END);
        topTv.setGravity(Gravity.CENTER);
        topTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, PINYIN_SP);
        topTv.setText(hasPinyin ? py : " ");
        if (hasPinyin) {
            topTv.setTextColor(pinyinColor);
            topTv.setAlpha(0.75f);
        }
        cell.addView(topTv);

        // 汉字
        TextView charTv = new TextView(getContext());
        charTv.setText(String.valueOf(ch));
        charTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, CHAR_SP);
        charTv.setTextColor(charColor);
        charTv.setGravity(Gravity.CENTER);
        cell.addView(charTv);

        return cell;
    }
}
