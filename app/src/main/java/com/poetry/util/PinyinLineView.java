package com.poetry.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

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
    private final String sourceLine;

    /**
     * 标记子 View 层级是否已构建。P0-A 优化：避免父布局多次 onMeasure 时重复创建大量 TextView
     * （长诗 28 字 × 2 行 = 56 个 TextView，重建成本不可忽视）。
     */
    private boolean mBuilt = false;

    /**
     * 构建多行拼音视图。
     *
     * @param context        上下文
     * @param line           整行诗句文本
     * @param maxCharsPerRow 每子行最大字符数（≤0 时自动取 10）
     */
    public PinyinLineView(Context context, String line, int maxCharsPerRow) {
        super(context);
        this.maxCharsPerRow = maxCharsPerRow > 0 ? maxCharsPerRow : 10;
        this.sourceLine = line != null ? line : "";
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        // 不在构造时 buildRows，等首次 onMeasure 时构建（避免父布局尚未 measure 时无谓创建）
    }

    /**
     * 首次测量时构建子 View 层级；后续 measure 直接复用。
     * <p>
     * 修复背景（P0-A A1）：原版在构造函数中调用 buildRows，
     * 父布局 ConstraintLayout 多次 measure 会触发 view tree 重建，
     * 长诗 28 字 × 2 行 = 56 个 TextView 创建代价集中爆发。
     * </p>
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!mBuilt) {
            buildRows(sourceLine);
            mBuilt = true;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * 按 {@link #maxCharsPerRow} 将文本拆分为多子行，每行水平排列若干拼音-汉字单元格。
     *
     * @param line 整行诗句文本
     */
    private void buildRows(String line) {
        String text = line != null ? line : "";
        if (text.isEmpty()) return;

        int total = text.length();
        // ceil 除法：计算需要的行数
        int rowCount = (total + maxCharsPerRow - 1) / maxCharsPerRow;

        int pinyinColor = ContextCompat.getColor(getContext(), R.color.on_surface_variant);
        int charColor   = ContextCompat.getColor(getContext(), R.color.on_surface);
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

    /**
     * 构建单列单元格：上方显示拼音（若有），下方显示汉字，垂直居中排列。
     *
     * @param ch          当前汉字字符
     * @param index       字符在原字符串中的位置，用于从 pinyinList 取对应拼音
     * @param pinyinList  拼音列表
     * @param pinyinColor 拼音文字颜色
     * @param charColor   汉字文字颜色
     * @return 拼音-汉字纵向排列的 LinearLayout 单元格
     */
    private LinearLayout buildCell(char ch, int index, List<String> pinyinList,
                                   int pinyinColor, int charColor) {
        String py = (index < pinyinList.size()) ? pinyinList.get(index) : "";
        boolean hasPinyin = !TextUtils.isEmpty(py) && !py.equals(String.valueOf(ch));

        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        // 使用 weight=1 使各列等分行宽，实现均匀分布
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
            topTv.setAlpha(0.75f); // 拼音半透明，避免抢汉字的视觉权重
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
