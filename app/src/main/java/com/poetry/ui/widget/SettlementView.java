package com.poetry.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.poetry.R;

/**
 * 游戏结算视图（自定义 LinearLayout）。
 * <p>
 * 展示本局结算信息：大颗星（逐个弹跳放大）、滚动得分、本局最美一句卡片。
 * 提供 {@link #setResult(int, int, String)} 设置数据并触发动画，
 * 以及 {@link #setListeners(Runnable, Runnable, Runnable, Runnable, Runnable)} 注册交互回调。
 * </p>
 * <p>
 * 动画风格参考 {@link ConfettiView}：使用 {@link ValueAnimator} 驱动，
 * 星星逐个错开 150ms 弹跳放大（scale 0 → 1.15 → 1.0 + alpha），
 * 得分从 0 滚动到实际分（约 800ms）。
 * </p>
 */
public class SettlementView extends LinearLayout {

    /** 星星逐个弹跳的错开间隔（毫秒） */
    private static final long STAR_STAGGER_MS = 150L;
    /** 单颗星星弹跳动画时长（毫秒） */
    private static final long STAR_BOUNCE_MS = 450L;
    /** 得分滚动动画时长（毫秒） */
    private static final long SCORE_ROLL_MS = 800L;

    /** 星星数量 */
    private static final int STAR_COUNT = 3;

    private TextView tvTitle;
    private LinearLayout llStars;
    private TextView tvScoreLabel;
    private TextView tvScoreValue;
    private TextView tvBestLine;
    private Button btnFavorite;
    private Button btnListen;
    private Button btnShare;

    private StarView[] starViews = new StarView[STAR_COUNT];
    private int score = 0;
    private int stars = 0;
    private String bestLine = null;

    private Runnable onFavorite;
    private Runnable onListen;
    private Runnable onShare;
    private Runnable onReplay;
    private Runnable onBackHome;

    public SettlementView(Context context) {
        super(context);
        init();
    }

    public SettlementView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SettlementView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化视图结构：标题、星星行、得分区、最美一句卡片与操作按钮。
     */
    private void init() {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp2px(24);
        setPadding(pad, pad, pad, pad);

        // 标题
        tvTitle = new TextView(getContext());
        tvTitle.setText(R.string.settlement_title);
        tvTitle.setTextSize(28);
        tvTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.text_title));
        tvTitle.setGravity(Gravity.CENTER);
        addView(tvTitle, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 星星行
        llStars = new LinearLayout(getContext());
        llStars.setOrientation(HORIZONTAL);
        llStars.setGravity(Gravity.CENTER);
        for (int i = 0; i < STAR_COUNT; i++) {
            StarView star = new StarView(getContext());
            star.setVisibility(View.INVISIBLE);
            starViews[i] = star;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp2px(56), dp2px(56));
            lp.setMargins(dp2px(6), dp2px(16), dp2px(6), dp2px(8));
            llStars.addView(star, lp);
        }
        addView(llStars, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 得分标签
        tvScoreLabel = new TextView(getContext());
        tvScoreLabel.setText(R.string.settlement_score);
        tvScoreLabel.setTextSize(14);
        tvScoreLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.text_body_secondary));
        tvScoreLabel.setGravity(Gravity.CENTER);
        addView(tvScoreLabel, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 得分大字
        tvScoreValue = new TextView(getContext());
        tvScoreValue.setText("0");
        tvScoreValue.setTextSize(48);
        tvScoreValue.setTextColor(ContextCompat.getColor(getContext(), R.color.score_gold));
        tvScoreValue.setGravity(Gravity.CENTER);
        addView(tvScoreValue, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 最美一句卡片
        tvBestLine = new TextView(getContext());
        tvBestLine.setTextSize(18);
        tvBestLine.setTextColor(ContextCompat.getColor(getContext(), R.color.text_body));
        tvBestLine.setGravity(Gravity.CENTER);
        tvBestLine.setLineSpacing(8, 1);
        tvBestLine.setPadding(dp2px(16), dp2px(20), dp2px(16), dp2px(20));
        tvBestLine.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_card));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, dp2px(20), 0, dp2px(16));
        addView(tvBestLine, cardLp);

        // 操作按钮行
        LinearLayout btnRow = new LinearLayout(getContext());
        btnRow.setOrientation(HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);

        btnFavorite = createActionButton(R.string.settlement_favorite_line);
        btnListen = createActionButton(R.string.settlement_listen_again);
        btnShare = createActionButton(R.string.settlement_share);

        btnRow.addView(btnFavorite, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        btnRow.addView(btnListen, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        btnRow.addView(btnShare, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        addView(btnRow, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 无障碍描述
        btnFavorite.setContentDescription(getContext().getString(R.string.settlement_favorite_line));
        btnListen.setContentDescription(getContext().getString(R.string.settlement_listen_again));
        btnShare.setContentDescription(getContext().getString(R.string.settlement_share));

        // 点击监听
        btnFavorite.setOnClickListener(v -> {
            if (onFavorite != null) onFavorite.run();
        });
        btnListen.setOnClickListener(v -> {
            if (onListen != null) onListen.run();
        });
        btnShare.setOnClickListener(v -> {
            if (onShare != null) onShare.run();
        });
    }

    /**
     * 创建一个操作按钮（收藏/再听/分享）。
     *
     * @param textRes 按钮文本资源
     * @return 配置好的按钮
     */
    private Button createActionButton(int textRes) {
        Button btn = new Button(getContext());
        btn.setText(textRes);
        btn.setTextSize(13);
        btn.setTextColor(ContextCompat.getColor(getContext(), R.color.text_on_primary));
        btn.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primary));
        btn.setAllCaps(false);
        btn.setMinHeight(dp2px(56));
        btn.setPadding(dp2px(4), dp2px(8), dp2px(4), dp2px(8));
        return btn;
    }

    /**
     * 设置结算结果并触发动画。
     *
     * @param score   本局得分
     * @param stars   获得的星星数（1-3）
     * @param bestLine 本局最美一句（可为 null，显示通用文案）
     */
    public void setResult(int score, int stars, String bestLine) {
        this.score = Math.max(0, score);
        this.stars = Math.max(0, Math.min(STAR_COUNT, stars));
        this.bestLine = bestLine;

        // 最美一句
        if (bestLine != null && !bestLine.isEmpty()) {
            tvBestLine.setText("「" + bestLine + "」");
        } else {
            tvBestLine.setText(R.string.settlement_best_line_fallback);
        }

        // 重置星星
        for (StarView star : starViews) {
            star.setLit(false);
            star.setScaleX(0f);
            star.setScaleY(0f);
            star.setAlpha(0f);
            star.setVisibility(View.INVISIBLE);
        }

        // 逐个弹跳放大
        for (int i = 0; i < STAR_COUNT; i++) {
            final StarView star = starViews[i];
            final boolean lit = i < this.stars;
            star.setLit(lit);
            star.setVisibility(View.VISIBLE);
            animateStar(star, i * STAR_STAGGER_MS);
        }

        // 滚动得分
        rollScore();
    }

    /**
     * 单颗星星的弹跳放大动画（scale 0 → 1.15 → 1.0 + alpha）。
     *
     * @param star      目标星星
     * @param startDelay 错开延迟（毫秒）
     */
    private void animateStar(final StarView star, long startDelay) {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1.15f, 1.0f);
        anim.setDuration(STAR_BOUNCE_MS);
        anim.setStartDelay(startDelay);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(animation -> {
            float v = (float) animation.getAnimatedValue();
            star.setScaleX(v);
            star.setScaleY(v);
            star.setAlpha(Math.min(1f, v * 2f));
        });
        anim.start();
    }

    /**
     * 得分从 0 滚动到实际分。
     */
    private void rollScore() {
        ValueAnimator anim = ValueAnimator.ofInt(0, score);
        anim.setDuration(SCORE_ROLL_MS);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(animation -> {
            int v = (int) animation.getAnimatedValue();
            tvScoreValue.setText(String.valueOf(v));
        });
        anim.start();
    }

    /**
     * 注册交互回调。
     *
     * @param onFavorite 收藏本句
     * @param onListen   再听一遍（TTS）
     * @param onShare    分享
     * @param onReplay   再来一局
     * @param onBackHome 回大厅
     */
    public void setListeners(Runnable onFavorite, Runnable onListen, Runnable onShare,
                             Runnable onReplay, Runnable onBackHome) {
        this.onFavorite = onFavorite;
        this.onListen = onListen;
        this.onShare = onShare;
        this.onReplay = onReplay;
        this.onBackHome = onBackHome;
    }

    /**
     * 收藏成功后切换按钮文本为"已收藏"并禁用。
     */
    public void markCollected() {
        btnFavorite.setText(R.string.settlement_collected);
        btnFavorite.setEnabled(false);
        btnFavorite.setAlpha(0.6f);
    }

    /**
     * 获取当前最美一句。
     *
     * @return 最美一句文本，可能为 null
     */
    @Nullable
    public String getBestLine() {
        return bestLine;
    }

    /**
     * 获取当前得分。
     *
     * @return 得分
     */
    public int getScore() {
        return score;
    }

    private int dp2px(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    /**
     * 自绘五角星 View。
     * <p>
     * 点亮时使用金色 {@code star_ring_progress}，未点亮使用 {@code star_ring_track}。
     * </p>
     */
    private static class StarView extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path starPath = new Path();
        private boolean lit = false;

        StarView(Context context) {
            super(context);
        }

        void setLit(boolean lit) {
            this.lit = lit;
            invalidate();
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float outer = Math.min(getWidth(), getHeight()) / 2f - 2f;
            float inner = outer * 0.45f;

            paint.setColor(ContextCompat.getColor(getContext(),
                    lit ? R.color.star_ring_progress : R.color.star_ring_track));

            starPath.reset();
            for (int i = 0; i < 10; i++) {
                float radius = (i % 2 == 0) ? outer : inner;
                double angle = Math.toRadians(-90 + i * 36);
                float x = cx + (float) (radius * Math.cos(angle));
                float y = cy + (float) (radius * Math.sin(angle));
                if (i == 0) {
                    starPath.moveTo(x, y);
                } else {
                    starPath.lineTo(x, y);
                }
            }
            starPath.close();
            canvas.drawPath(starPath, paint);
        }
    }
}
