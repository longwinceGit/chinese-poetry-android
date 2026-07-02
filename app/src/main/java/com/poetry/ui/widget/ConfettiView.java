package com.poetry.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 撒花庆祝动画 View（自定义 View）。
 * <p>
 * 通过粒子系统实现撒花效果，粒子包含 Emoji 和几何图形（矩形、圆形、菱形）两种类型。
 * 粒子从屏幕顶部飘落，模拟重力加速度，并在帧数超过 130 后逐渐淡出，
 * 动画总帧数上限为 180（{@link #MAX_FRAMES}），达到上限后自动停止并隐藏自身。
 * </p>
 */
public class ConfettiView extends View {

    /** 粒子可选颜色数组 */
    private static final int COLORS[] = {
        0xFFFF6B6B, 0xFF4ECDC4, 0xFFFFD93D, 0xFFA78BFA,
        0xFFFB923C, 0xFF60A5FA, 0xFFF472B6, 0xFF34D399
    };

    /** 粒子可选 Emoji 数组 */
    private static final String EMOJIS[] = {
        "✨", "🌸", "🎉", "🎊", "⭐", "❤️", "🎵", "🏮"
    };

    private List<Particle> particles = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Random random = new Random();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private boolean animating = false;
    private int frameCount = 0;
    /** 动画最大帧数，超过此值后自动淡出停止 */
    private static final int MAX_FRAMES = 180;

    public ConfettiView(Context context) { super(context); init(); }
    public ConfettiView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    /**
     * 初始化画笔与视图可见性。
     * <p>
     * 设置 Emoji 文字画笔大小为 24、居中对齐，并将视图初始状态设为 {@link View#GONE}。
     * </p>
     */
    private void init() {
        textPaint.setTextSize(24);
        textPaint.setTextAlign(Paint.Align.CENTER);
        setVisibility(GONE);
    }

    /**
     * 触发撒花庆祝动画。
     * <p>
     * 清空已有粒子、重置帧计数，生成 60 个新粒子并开始动画循环，
     * 同时将视图可见性切换为 {@link View#VISIBLE}。
     * </p>
     */
    public void celebrate() {
        particles.clear();
        frameCount = 0;
        int w = getWidth() > 0 ? getWidth() : 720;
        for (int i = 0; i < 60; i++) {
            particles.add(new Particle(w));
        }
        animating = true;
        setVisibility(VISIBLE);
        startAnimating();
    }

    /**
     * 启动动画帧循环。
     * <p>
     * 使用 {@link Handler} 以约 16ms（≈60fps）间隔逐帧推进动画，
     * 每帧递增帧计数，超过 {@link #MAX_FRAMES} 时自动调用 {@link #stop()}；
     * 否则调用 {@link #update()} 更新粒子位置并触发重绘。
     * </p>
     */
    private void startAnimating() {
        if (runnable != null) handler.removeCallbacks(runnable);
        runnable = new Runnable() {
            @Override
            public void run() {
                if (!animating) return;
                frameCount++;
                if (frameCount > MAX_FRAMES) {
                    stop();
                    return;
                }
                update();
                invalidate();
                handler.postDelayed(this, 16);
            }
        };
        handler.post(runnable);
    }

    /**
     * 更新所有粒子状态。
     * <p>
     * 获取当前视图宽高（默认 720×1280）并逐一调用
     * {@link Particle#update(int, int, int)} 更新粒子的位置与透明度。
     * </p>
     */
    private void update() {
        int w = getWidth() > 0 ? getWidth() : 720;
        int h = getHeight() > 0 ? getHeight() : 1280;
        for (Particle p : particles) {
            p.update(w, h, frameCount);
        }
    }

    /**
     * 停止动画并隐藏视图。
     * <p>
     * 将 {@link #animating} 置为 false，移除 Handler 回调，
     * 并将视图可见性恢复为 {@link View#GONE}。
     * </p>
     */
    public void stop() {
        animating = false;
        if (runnable != null) handler.removeCallbacks(runnable);
        setVisibility(GONE);
    }

    /**
     * 绘制所有可见粒子。
     * <p>
     * 透明度低于 0.01 的粒子跳过绘制；Emoji 类型粒子使用文字画笔绘制，
     * 几何图形粒子根据 {@link Particle#shape} 绘制圆角矩形、圆形或菱形，
     * 均应用旋转与透明度变换。
     * </p>
     *
     * @param canvas 画布
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Particle p : particles) {
            if (p.alpha < 0.01f) continue;

            if (p.isEmoji) {
                textPaint.setAlpha((int)(p.alpha * 255));
                canvas.save();
                canvas.translate(p.x, p.y);
                canvas.rotate(p.rotation);
                canvas.drawText(p.emoji, 0, 0, textPaint);
                canvas.restore();
            } else {
                paint.setColor(p.color);
                paint.setAlpha((int)(p.alpha * 255));
                canvas.save();
                canvas.translate(p.x, p.y);
                canvas.rotate(p.rotation);
                switch (p.shape) {
                    case 0: // rect
                        canvas.drawRoundRect(new RectF(-p.size / 2, -p.size / 3, p.size / 2, p.size / 3), 3, 3, paint);
                        break;
                    case 1: // circle
                        canvas.drawCircle(0, 0, p.size / 2, paint);
                        break;
                    case 2: // triangle-ish (diamond)
                        canvas.rotate(45);
                        canvas.drawRect(new RectF(-p.size / 2, -p.size / 4, p.size / 2, p.size / 4), paint);
                        break;
                }
                canvas.restore();
            }
        }
    }

    /**
     * 视图从窗口分离时停止动画，防止 Handler 泄漏。
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    /**
     * 粒子内部类，表示单个撒花粒子的状态与物理属性。
     * <p>
     * 粒子包含位置 (x, y)、速度 (vx, vy)、旋转角/旋转速度、尺寸、颜色、透明度，
     * 以及类型标识（Emoji 或几何图形 shape=0/1/2）。
     * 约 15% 的粒子为 Emoji 类型，其余为几何图形。
     * </p>
     */
    private class Particle {
        float x, y;
        float vx, vy;
        float rotation, rotationSpeed;
        float size;
        int color;
        float alpha = 1f;
        /** 图形类型：0=圆角矩形, 1=圆形, 2=菱形 */
        int shape;
        boolean isEmoji;
        String emoji;

        /**
         * 构造一个新粒子。
         * <p>
         * 随机生成初始位置（屏幕宽度的 10%~90% 范围，从屏幕上方飘落），
         * 初始速度向下方为主（vy=3~9），水平速度随机，
         * 约 15% 概率选择 Emoji 类型，其余为几何图形。
         * </p>
         *
         * @param screenW 屏幕宽度，用于限定粒子横向分布范围
         */
        Particle(int screenW) {
            x = screenW * 0.1f + random.nextInt((int)(screenW * 0.8f));
            y = -random.nextInt(400) - 50;
            vx = (random.nextFloat() - 0.5f) * 4;
            vy = 3 + random.nextFloat() * 6;
            rotation = random.nextFloat() * 360;
            rotationSpeed = (random.nextFloat() - 0.5f) * 10;
            size = 10 + random.nextFloat() * 16;
            shape = random.nextInt(3);
            isEmoji = random.nextFloat() < 0.15f;
            if (isEmoji) {
                emoji = EMOJIS[random.nextInt(EMOJIS.length)];
            }
            color = COLORS[random.nextInt(COLORS.length)];
        }

        /**
         * 更新粒子物理状态。
         * <p>
         * 位移按速度推进，模拟重力（vy 每帧增加 0.08）；帧 30~100 期间施加随机水平扰动；
         * 粒子超出屏幕底部时重置到顶部；帧超过 130 后透明度逐渐衰减（每帧 -0.025）。
         * </p>
         *
         * @param screenW 屏幕宽度，用于粒子重置时的横向随机分布
         * @param screenH 屏幕高度，用于判定粒子是否飘出底部
         * @param frame   当前动画帧数，控制重力扰动与淡出逻辑
         */
        void update(int screenW, int screenH, int frame) {
            x += vx;
            y += vy;
            rotation += rotationSpeed;
            vy += 0.08f;

            if (frame > 30 && frame <= 100) {
                vx += (random.nextFloat() - 0.5f) * 0.3f;
            }

            if (y > screenH + 50) {
                y = -random.nextInt(100) - 50;
                x = random.nextInt(screenW);
                vy = 3 + random.nextFloat() * 4;
            }

            if (frame > 130) {
                alpha = Math.max(0, alpha - 0.025f);
            }
        }
    }
}
