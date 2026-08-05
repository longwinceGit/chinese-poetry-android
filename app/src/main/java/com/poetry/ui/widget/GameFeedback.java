package com.poetry.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.poetry.R;

/**
 * 局内反馈层（与具体游戏解耦）。
 * <p>
 * 供对诗 / 消消乐 / 填空 / 飞花令等游戏 Fragment 顶部复用，横向排列三个自绘小组件：
 * <ul>
 *   <li>左侧 {@link StarRingView} —— 星星进度环（圆形，中心五角星，右侧 "N/3" 文本）；</li>
 *   <li>中间 {@link ComboBarView} —— 火苗连击条（横向分段条，每格圆角矩形，满格触发燃烧动画）；</li>
 *   <li>飞分文本层 —— 从指定坐标飞到左上角总分锚点的 "🔥+N" 金色飘字。</li>
 * </ul>
 * 三个小组件全部 View 自绘（onDraw + Canvas），不依赖任何图片素材，动画使用
 * {@link ValueAnimator} / {@link ObjectAnimator}，兼容 minSdk 21。
 * </p>
 * <p>
 * 本类不引用任何 GameEngine / QuizGenerator 等游戏逻辑类，仅暴露纯状态接口，
 * 由游戏 Fragment 负责把游戏结果映射为星星 / 连击 / 得分。
 * </p>
 */
public class GameFeedback extends LinearLayout {

    /** 星星进度环目标星数默认值 */
    private static final int DEFAULT_MAX_STARS = 3;
    /** 连击条总格数默认值 */
    private static final int DEFAULT_MAX_COMBO = 5;
    /** 连击条满格燃烧动画时长（毫秒） */
    private static final long COMBO_BURN_DURATION_MS = 300L;
    /** 飞分动画时长（毫秒） */
    private static final long FLY_DURATION_MS = 700L;
    /** 飞分目标锚点（左上角总分位置），单位 dp */
    private static final float FLY_ANCHOR_X_DP = 56f;
    private static final float FLY_ANCHOR_Y_DP = 24f;
    /** 整体高度建议值，单位 dp（touch_target_min） */
    private static final float VIEW_HEIGHT_DP = 48f;

    private final StarRingView starRingView;
    private final ComboBarView comboBarView;
    private final FlyingScoreView flyingScoreView;

    public GameFeedback(Context context) {
        this(context, null);
    }

    public GameFeedback(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumHeight((int) dp2px(VIEW_HEIGHT_DP));

        starRingView = new StarRingView(context);
        comboBarView = new ComboBarView(context);
        flyingScoreView = new FlyingScoreView(context);

        addView(starRingView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(comboBarView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // 飞分文本层覆盖在反馈层之上，不参与布局测量
        addView(flyingScoreView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // ---------------------------------------------------------------------
    // 公开 API（供游戏 Fragment 调用）
    // ---------------------------------------------------------------------

    /** 设置星星进度环目标星数（默认 3）。 */
    public void setMaxStars(int maxStars) {
        starRingView.setMaxStars(maxStars);
    }

    /** 设置当前已攒星数，进度环按 0→stars/maxStars 动画填充。 */
    public void setStars(int stars) {
        starRingView.setStars(stars);
    }

    /** 设置连击条总格数（默认 5）。 */
    public void setMaxCombo(int maxCombo) {
        comboBarView.setMaxCombo(maxCombo);
    }

    /** 连击 +1，对应格点亮，满格触发燃烧动画。 */
    public void addCombo() {
        comboBarView.addCombo();
    }

    /** 连击清零（答错时调用）。 */
    public void resetCombo() {
        comboBarView.resetCombo();
    }

    /**
     * 从 (startX, startY) 到左上角总分位置的飞分动画。
     *
     * @param startX 起点 X（相对本 View 的坐标）
     * @param startY 起点 Y（相对本 View 的坐标）
     * @param points 得分点数
     */
    public void playFlyingScore(int startX, int startY, int points) {
        flyingScoreView.play(startX, startY, points);
    }

    /** 读取当前连击值。 */
    public int getCombo() {
        return comboBarView.getCombo();
    }

    /** 读取当前已攒星数。 */
    public int getStars() {
        return starRingView.getStars();
    }

    /** 设置连击条满格事件回调。 */
    public void setOnComboFullListener(Runnable listener) {
        comboBarView.setOnComboFullListener(listener);
    }

    // ---------------------------------------------------------------------
    // 星星进度环
    // ---------------------------------------------------------------------

    /**
     * 星星进度环：圆形圆环 + 中心五角星 + 右侧 "N/3" 文本。
     * 未点亮用 {@code star_ring_track}，点亮用 {@code star_ring_progress}（金色）。
     */
    private static class StarRingView extends View {

        private static final float RING_SIZE_DP = 40f;
        private static final float RING_STROKE_DP = 4f;
        private static final float STAR_TEXT_SIZE_DP = 16f;
        private static final float LABEL_TEXT_SIZE_DP = 11f;
        private static final float LABEL_GAP_DP = 6f;

        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final int trackColor;
        private final int progressColor;
        private final int labelColor;

        private int maxStars = DEFAULT_MAX_STARS;
        private int stars = 0;
        /** 当前动画进度 0..1，用于进度环填充动画 */
        private float progress = 0f;
        private ValueAnimator progressAnimator;

        StarRingView(Context context) {
            super(context);
            trackColor = ContextCompat.getColor(context, R.color.star_ring_track);
            progressColor = ContextCompat.getColor(context, R.color.star_ring_progress);
            labelColor = ContextCompat.getColor(context, R.color.on_surface);

            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(dp2px(RING_STROKE_DP));
            ringPaint.setStrokeCap(Paint.Cap.ROUND);

            starPaint.setTextAlign(Paint.Align.CENTER);
            starPaint.setTextSize(dp2px(STAR_TEXT_SIZE_DP));

            labelPaint.setTextAlign(Paint.Align.LEFT);
            labelPaint.setTextSize(dp2px(LABEL_TEXT_SIZE_DP));
            labelPaint.setColor(labelColor);

            setContentDescription("星星 0/" + maxStars);
        }

        void setMaxStars(int maxStars) {
            if (maxStars <= 0) return;
            this.maxStars = maxStars;
            if (stars > maxStars) stars = maxStars;
            updateContentDescription();
            invalidate();
        }

        void setStars(int stars) {
            if (stars < 0) stars = 0;
            if (stars > maxStars) stars = maxStars;
            this.stars = stars;
            animateProgressTo(stars / (float) maxStars);
            updateContentDescription();
        }

        int getStars() {
            return stars;
        }

        private void animateProgressTo(float target) {
            if (progressAnimator != null) progressAnimator.cancel();
            float from = progress;
            progressAnimator = ValueAnimator.ofFloat(from, target);
            progressAnimator.setDuration(400L);
            progressAnimator.setInterpolator(new DecelerateInterpolator());
            progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    progress = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            progressAnimator.start();
        }

        private void updateContentDescription() {
            setContentDescription("星星 " + stars + "/" + maxStars);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            float ringSize = dp2px(RING_SIZE_DP);
            float labelWidth = labelPaint.measureText(maxStars + "/" + maxStars);
            int width = (int) (ringSize + dp2px(LABEL_GAP_DP) + labelWidth);
            int height = (int) ringSize;
            setMeasuredDimension(width, height);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float ringSize = dp2px(RING_SIZE_DP);
            float stroke = dp2px(RING_STROKE_DP);
            float radius = ringSize / 2f - stroke / 2f;
            float cx = ringSize / 2f;
            float cy = ringSize / 2f;

            // 轨道圆环
            ringPaint.setColor(trackColor);
            canvas.drawCircle(cx, cy, radius, ringPaint);

            // 进度圆环（按 progress 填充）
            if (progress > 0f) {
                ringPaint.setColor(progressColor);
                RectF ringRect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
                canvas.drawArc(ringRect, -90f, 360f * progress, false, ringPaint);
            }

            // 中心五角星
            starPaint.setColor(progressColor);
            canvas.drawText("★", cx, cy - (starPaint.ascent() + starPaint.descent()) / 2f, starPaint);

            // 右侧 "N/3" 文本
            String label = stars + "/" + maxStars;
            float labelX = ringSize + dp2px(LABEL_GAP_DP);
            float labelY = cy - (labelPaint.ascent() + labelPaint.descent()) / 2f;
            canvas.drawText(label, labelX, labelY, labelPaint);
        }
    }

    // ---------------------------------------------------------------------
    // 火苗连击条
    // ---------------------------------------------------------------------

    /**
     * 火苗连击条：横向分段条，每格圆角矩形。
     * 未点亮用 {@code combo_fire_soft}，点亮用 {@code combo_fire}；
     * 满格触发一次 300ms 燃烧动画（高亮 + scaleY 1.0→1.15→1.0）。
     */
    private static class ComboBarView extends View {

        private static final float BAR_HEIGHT_DP = 12f;
        private static final float SEGMENT_GAP_DP = 4f;
        private static final float SEGMENT_WIDTH_DP = 14f;
        private static final float CORNER_RADIUS_DP = 4f;
        private static final float LABEL_TEXT_SIZE_DP = 11f;
        private static final float LABEL_GAP_DP = 6f;

        private final Paint segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final int fireColor;
        private final int fireSoftColor;
        private final int labelColor;

        private int maxCombo = DEFAULT_MAX_COMBO;
        private int combo = 0;
        /** 燃烧动画 scaleY 因子 */
        private float burnScale = 1f;
        /** 燃烧动画高亮（叠加白色透明度） */
        private int burnHighlight = 0;
        private ValueAnimator burnAnimator;
        /** 连击条满格事件回调 */
        private Runnable onComboFullListener;

        ComboBarView(Context context) {
            super(context);
            fireColor = ContextCompat.getColor(context, R.color.combo_fire);
            fireSoftColor = ContextCompat.getColor(context, R.color.combo_fire_soft);
            labelColor = ContextCompat.getColor(context, R.color.on_surface);

            segmentPaint.setStyle(Paint.Style.FILL);

            labelPaint.setTextAlign(Paint.Align.LEFT);
            labelPaint.setTextSize(dp2px(LABEL_TEXT_SIZE_DP));
            labelPaint.setColor(labelColor);

            setContentDescription("连击 0/" + maxCombo);
        }

        void setMaxCombo(int maxCombo) {
            if (maxCombo <= 0) return;
            this.maxCombo = maxCombo;
            if (combo > maxCombo) combo = maxCombo;
            updateContentDescription();
            invalidate();
        }

        void addCombo() {
            if (combo >= maxCombo) return;
            combo++;
            updateContentDescription();
            if (combo >= maxCombo) {
                triggerBurn();
            }
            invalidate();
        }

        void resetCombo() {
            combo = 0;
            burnScale = 1f;
            burnHighlight = 0;
            if (burnAnimator != null) burnAnimator.cancel();
            updateContentDescription();
            invalidate();
        }

        int getCombo() {
            return combo;
        }

        void setOnComboFullListener(Runnable listener) {
            this.onComboFullListener = listener;
        }

        private void triggerBurn() {
            if (burnAnimator != null) burnAnimator.cancel();
            burnAnimator = ValueAnimator.ofFloat(0f, 1f);
            burnAnimator.setDuration(COMBO_BURN_DURATION_MS);
            burnAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float t = (Float) animation.getAnimatedValue();
                    // scaleY: 1.0 → 1.15 → 1.0
                    burnScale = 1f + 0.15f * (float) Math.sin(t * Math.PI);
                    // 高亮：先升后降
                    burnHighlight = (int) (255 * (float) Math.sin(t * Math.PI));
                    invalidate();
                }
            });
            burnAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    burnScale = 1f;
                    burnHighlight = 0;
                    invalidate();
                }
            });
            burnAnimator.start();
            if (onComboFullListener != null) {
                onComboFullListener.run();
            }
        }

        private void updateContentDescription() {
            setContentDescription("连击 " + combo + "/" + maxCombo);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            float barWidth = maxCombo * dp2px(SEGMENT_WIDTH_DP)
                + (maxCombo - 1) * dp2px(SEGMENT_GAP_DP);
            float labelWidth = labelPaint.measureText(maxCombo + "/" + maxCombo);
            int width = (int) (barWidth + dp2px(LABEL_GAP_DP) + labelWidth);
            int height = (int) dp2px(BAR_HEIGHT_DP);
            setMeasuredDimension(width, height);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float segWidth = dp2px(SEGMENT_WIDTH_DP);
            float segGap = dp2px(SEGMENT_GAP_DP);
            float corner = dp2px(CORNER_RADIUS_DP);
            float barHeight = dp2px(BAR_HEIGHT_DP);
            float cy = barHeight / 2f;

            canvas.save();
            canvas.scale(1f, burnScale, 0f, cy);

            for (int i = 0; i < maxCombo; i++) {
                float left = i * (segWidth + segGap);
                float top = 0f;
                float right = left + segWidth;
                float bottom = barHeight;
                RectF seg = new RectF(left, top, right, bottom);

                if (i < combo) {
                    segmentPaint.setColor(fireColor);
                    // 燃烧高亮叠加
                    if (burnHighlight > 0) {
                        segmentPaint.setColor(blend(fireColor, 0xFFFFFFFF, burnHighlight));
                    }
                } else {
                    segmentPaint.setColor(fireSoftColor);
                }
                canvas.drawRoundRect(seg, corner, corner, segmentPaint);
            }
            canvas.restore();

            // 右侧 "N/5" 文本
            String label = combo + "/" + maxCombo;
            float labelX = maxCombo * segWidth + (maxCombo - 1) * segGap + dp2px(LABEL_GAP_DP);
            float labelY = cy - (labelPaint.ascent() + labelPaint.descent()) / 2f;
            canvas.drawText(label, labelX, labelY, labelPaint);
        }

        /** 按 alpha(0..255) 将 base 与 overlay 混合。 */
        private static int blend(int base, int overlay, int alpha) {
            int a = alpha;
            int inv = 255 - a;
            int r = ((base >> 16 & 0xFF) * inv + (overlay >> 16 & 0xFF) * a) / 255;
            int g = ((base >> 8 & 0xFF) * inv + (overlay >> 8 & 0xFF) * a) / 255;
            int b = ((base & 0xFF) * inv + (overlay & 0xFF) * a) / 255;
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    // ---------------------------------------------------------------------
    // 飞分文本层
    // ---------------------------------------------------------------------

    /**
     * 飞分文本层：覆盖在反馈层之上，从指定坐标飞到左上角总分锚点。
     * 文字 "🔥+N" 金色，指数减速，结束时淡出。
     */
    private static class FlyingScoreView extends View {

        private static final float TEXT_SIZE_DP = 16f;

        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int goldColor;

        private String text = "";
        private float curX = 0f;
        private float curY = 0f;
        private float alpha = 0f;
        private ValueAnimator flyAnimator;

        FlyingScoreView(Context context) {
            super(context);
            goldColor = ContextCompat.getColor(context, R.color.score_gold);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(dp2px(TEXT_SIZE_DP));
            textPaint.setColor(goldColor);
            setVisibility(GONE);
        }

        void play(int startX, int startY, int points) {
            if (flyAnimator != null) flyAnimator.cancel();

            text = "🔥+" + points;
            float endX = dp2px(FLY_ANCHOR_X_DP);
            float endY = dp2px(FLY_ANCHOR_Y_DP);
            curX = startX;
            curY = startY;
            alpha = 1f;
            setVisibility(VISIBLE);

            flyAnimator = ValueAnimator.ofFloat(0f, 1f);
            flyAnimator.setDuration(FLY_DURATION_MS);
            flyAnimator.setInterpolator(new DecelerateInterpolator(2f));
            flyAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float t = (Float) animation.getAnimatedValue();
                    curX = startX + (endX - startX) * t;
                    curY = startY + (endY - startY) * t;
                    // 后 40% 淡出
                    if (t > 0.6f) {
                        alpha = 1f - (t - 0.6f) / 0.4f;
                    }
                    invalidate();
                }
            });
            flyAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    alpha = 0f;
                    setVisibility(GONE);
                }
            });
            flyAnimator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (alpha <= 0f || text.isEmpty()) return;
            textPaint.setAlpha((int) (alpha * 255));
            float baseline = curY - (textPaint.ascent() + textPaint.descent()) / 2f;
            canvas.drawText(text, curX, baseline, textPaint);
        }
    }

    // ---------------------------------------------------------------------
    // 工具
    // ---------------------------------------------------------------------

    private static float dp2px(float dp) {
        return dp * Resources.getSystem().getDisplayMetrics().density;
    }
}
