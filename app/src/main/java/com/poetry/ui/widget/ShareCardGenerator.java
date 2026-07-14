package com.poetry.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.core.content.ContextCompat;

import com.poetry.R;

/**
 * 诗词分享卡片生成器 —— 使用 Canvas 绘制古风分享卡片。
 *
 * <p>从 DetailFragment 中提取，遵循单一职责原则。
 * 卡片宽 720px、高度自适应内容，宣纸色背景、墨色文字、朱红装饰线。</p>
 */
public class ShareCardGenerator {

    private static final int WIDTH = 720;
    private static final int PADDING = 48;
    private static final int PADDING_SMALL = 32;
    private static final int CONTENT_WIDTH = WIDTH - PADDING * 2;
    private static final int MAX_HEIGHT = 2400;

    private final Context context;
    private final String title;
    private final String author;
    private final String dynasty;
    private final String[] lines;

    public ShareCardGenerator(Context context, String title, String author,
                              String dynasty, String[] lines) {
        this.context = context;
        this.title = title;
        this.author = author;
        this.dynasty = dynasty;
        this.lines = lines;
    }

    /**
     * 生成古风分享卡片 Bitmap。
     *
     * @return 生成好的分享卡片，失败返回 null
     */
    public Bitmap generate() {
        int bgColor      = getColor(R.color.surface, 0xFFFAF7F0);
        int inkColor     = getColor(R.color.primary, 0xFF5D4037);
        int subColor     = getColor(R.color.secondary, 0xFF795548);
        int accentColor  = getColor(R.color.tertiary, 0xFFC62828);
        int waterColor   = getColor(R.color.on_surface_variant, 0xFF4A4540);
        int dividerColor = getColor(R.color.surface_variant, 0xFFF0EDE6);

        Paint titlePaint = createPaint(inkColor, 40f, true, Paint.Align.CENTER);
        Paint authorPaint = createPaint(subColor, 26f, false, Paint.Align.CENTER);
        Paint linePaint = createPaint(inkColor, 32f, false, Paint.Align.CENTER);
        Paint footerPaint = createPaint(waterColor, 22f, false, Paint.Align.CENTER);
        Paint accentPaint = createStrokePaint(accentColor, 3f);
        Paint dividerPaint = createStrokePaint(dividerColor, 1.5f);

        Rect titleBounds = new Rect();
        float titleY = 0;
        if (title != null && !title.isEmpty()) {
            titlePaint.getTextBounds(title, 0, title.length(), titleBounds);
            titleY = 80 + titleBounds.height();
        }

        Rect authorBounds = new Rect();
        String authorText = author + " · " + dynasty;
        authorPaint.getTextBounds(authorText, 0, authorText.length(), authorBounds);

        boolean isLongPoem = lines != null && lines.length > 14;
        if (isLongPoem) linePaint.setTextSize(28f);

        float linesHeight = 0;
        float[] lineWidths = null;
        if (lines != null && lines.length > 0) {
            lineWidths = new float[lines.length];
            for (int i = 0; i < lines.length; i++) {
                lineWidths[i] = linePaint.measureText(lines[i]);
                linesHeight += 46;
            }
        }

        float footerY = titleY + 40 + authorBounds.height() + 48 + linesHeight + 60;
        float totalHeight = Math.min(footerY + 60, MAX_HEIGHT);

        Bitmap bitmap;
        try {
            bitmap = Bitmap.createBitmap(WIDTH, (int) totalHeight, Bitmap.Config.RGB_565);
        } catch (OutOfMemoryError e) {
            return null;
        }

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(bgColor);

        float lineY = PADDING;
        canvas.drawLine(PADDING, lineY, WIDTH - PADDING, lineY, accentPaint);

        float curY = lineY + PADDING_SMALL + titleBounds.height();
        canvas.drawText(title, WIDTH / 2f, curY, titlePaint);

        curY += PADDING_SMALL + authorBounds.height();
        canvas.drawText(authorText, WIDTH / 2f, curY, authorPaint);

        curY += PADDING_SMALL;
        canvas.drawLine(PADDING, curY, WIDTH - PADDING, curY, dividerPaint);

        curY += 40;
        if (lines != null) {
            for (String line : lines) {
                Paint drawPaint = linePaint;
                if (lineWidths != null && lineWidths[0] > CONTENT_WIDTH) {
                    drawPaint = createPaint(drawPaint.getColor(), 28f, false, Paint.Align.CENTER);
                }
                canvas.drawText(line, WIDTH / 2f, curY, drawPaint);
                curY += 46;
            }
        }

        curY += 12;
        canvas.drawText("—— 来自「诗词乐园」", WIDTH / 2f, curY, footerPaint);
        canvas.drawLine(PADDING, totalHeight - PADDING, WIDTH - PADDING, totalHeight - PADDING, accentPaint);

        return bitmap;
    }

    private int getColor(int resId, int fallback) {
        if (context == null) return fallback;
        Integer color = ContextCompat.getColor(context, resId);
        return color != null ? color : fallback;
    }

    private static Paint createPaint(int color, float textSize, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(textSize);
        p.setFakeBoldText(bold);
        p.setTextAlign(align);
        return p;
    }

    private static Paint createStrokePaint(int color, float strokeWidth) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStrokeWidth(strokeWidth);
        p.setStyle(Paint.Style.STROKE);
        return p;
    }
}
