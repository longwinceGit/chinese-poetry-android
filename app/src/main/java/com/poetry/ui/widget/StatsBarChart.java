package com.poetry.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.poetry.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 简易柱状图 View（Canvas 自绘）。
 * <p>
 * 用于展示学习统计数据（如最近 7 天的每日已学诗词数），风格协调 Material 3 中国风调色板。
 * 支持柱状 + 顶部数值标注 + 底部日期标签。
 * </p>
 */
public class StatsBarChart extends View {

    /** 单条数据 */
    public static class BarData {
        public String label;    // 日期标签，如 "周一"
        public float value;     // 数值
        public BarData(String label, float value) {
            this.label = label;
            this.value = value;
        }
    }

    private final List<BarData> data = new ArrayList<>();
    private float maxValue = 0;

    // 颜色
    private final int barColor;
    private final int labelColor;
    private final int valueColor;
    private final int baseLineColor;

    // 画笔
    private final Paint barPaint;
    private final Paint labelPaint;
    private final Paint valuePaint;
    private final Paint basePaint;

    // 尺寸
    private final float barCornerRadius;
    private static final float BAR_MAX_WIDTH_DP = 40f;
    private static final float LABEL_TEXT_SIZE_SP = 10f;
    private static final float VALUE_TEXT_SIZE_SP = 10f;
    private static final float TOP_PADDING_DP = 28f;
    private static final float BOTTOM_LABEL_PADDING_DP = 8f;

    public StatsBarChart(Context context) {
        this(context, null);
    }

    public StatsBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        barColor = ContextCompat.getColor(context, R.color.primary);
        labelColor = ContextCompat.getColor(context, R.color.on_surface_variant);
        valueColor = ContextCompat.getColor(context, R.color.on_surface);
        baseLineColor = ContextCompat.getColor(context, R.color.outline_variant);

        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(barColor);
        barPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(labelColor);
        labelPaint.setTextSize(dp2px(LABEL_TEXT_SIZE_SP));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(valueColor);
        valuePaint.setTextSize(dp2px(VALUE_TEXT_SIZE_SP));
        valuePaint.setTextAlign(Paint.Align.CENTER);

        basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        basePaint.setColor(baseLineColor);
        basePaint.setStrokeWidth(1f);

        barCornerRadius = dp2px(4);
    }

    /**
     * 设置图表数据并触发重绘。
     *
     * @param bars 数据列表
     */
    public void setData(List<BarData> bars) {
        data.clear();
        if (bars != null) {
            data.addAll(bars);
            maxValue = 0;
            for (BarData b : data) {
                if (b.value > maxValue) maxValue = b.value;
            }
            // 确保最大值至少有 1，避免除零
            if (maxValue == 0) maxValue = 1;
            // 向上取整到整数
            maxValue = (float) Math.ceil(maxValue);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data.isEmpty()) return;

        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (viewWidth <= 0 || viewHeight <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float topPadding = dp2px(TOP_PADDING_DP) + getPaddingTop();
        float bottomLabelPadding = dp2px(BOTTOM_LABEL_PADDING_DP) + getPaddingBottom();

        int barCount = data.size();
        float totalBarWidth = Math.min(dp2px(BAR_MAX_WIDTH_DP) * barCount, viewWidth * 0.85f);
        float spacing = (viewWidth - totalBarWidth) / (barCount + 1);
        float barWidth = totalBarWidth / barCount;

        float chartBottom = viewHeight - bottomLabelPadding - dp2px(LABEL_TEXT_SIZE_SP + 2);
        float chartHeight = chartBottom - topPadding;

        // 绘制基线
        float startX = getPaddingLeft() + spacing + barWidth / 2;
        canvas.drawLine(getPaddingLeft(), chartBottom,
            getPaddingLeft() + viewWidth, chartBottom, basePaint);

        for (int i = 0; i < barCount; i++) {
            BarData bar = data.get(i);
            float barHeight = (bar.value / maxValue) * chartHeight * 0.85f;

            float left = startX + i * (barWidth + spacing) - barWidth / 2;
            float top = chartBottom - barHeight;
            float right = left + barWidth;
            float bottom = chartBottom;

            // 绘制柱体（圆角矩形）
            RectF barRect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, barPaint);

            // 顶部数值标注
            String valueText = bar.value == (int) bar.value
                ? String.valueOf((int) bar.value) : String.valueOf(bar.value);
            canvas.drawText(valueText, left + barWidth / 2, top - dp2px(6), valuePaint);

            // 底部日期标签
            canvas.drawText(bar.label, left + barWidth / 2,
                chartBottom + dp2px(LABEL_TEXT_SIZE_SP + 4), labelPaint);
        }
    }

    private float dp2px(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
