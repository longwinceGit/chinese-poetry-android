package com.poetry;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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
 * 彩纸庆祝动画 View
 */
public class ConfettiView extends View {

    private static final int COLORS[] = {
        0xFFFF6B6B, 0xFF4ECDC4, 0xFFFFD93D, 0xFFA78BFA,
        0xFFFB923C, 0xFF60A5FA, 0xFFF472B6, 0xFF34D399
    };

    private List<Confetti> confettiList = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Random random = new Random();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private boolean animating = false;
    private int frameCount = 0;
    private static final int MAX_FRAMES = 150; // ~2.5秒 @ 60fps

    public ConfettiView(Context context) { super(context); init(); }
    public ConfettiView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        setVisibility(GONE);
    }

    /** 开始庆祝动画 */
    public void celebrate() {
        confettiList.clear();
        frameCount = 0;
        for (int i = 0; i < 80; i++) {
            confettiList.add(new Confetti());
        }
        animating = true;
        setVisibility(VISIBLE);
        startAnimating();
    }

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
                handler.postDelayed(this, 16); // ~60fps
            }
        };
        handler.post(runnable);
    }

    private void update() {
        for (Confetti c : confettiList) {
            c.update(getWidth(), getHeight(), frameCount);
        }
    }

    public void stop() {
        animating = false;
        if (runnable != null) handler.removeCallbacks(runnable);
        setVisibility(GONE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Confetti c : confettiList) {
            paint.setColor(c.color);
            paint.setAlpha((int)(c.alpha * 255));
            canvas.save();
            canvas.translate(c.x, c.y);
            canvas.rotate(c.rotation);
            canvas.drawRect(new RectF(0, 0, c.size, c.size * 0.6f), paint);
            canvas.restore();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    private class Confetti {
        float x, y;
        float speedX, speedY;
        float rotation, rotationSpeed;
        float size;
        int color;
        float alpha = 1f;

        Confetti() {
            x = random.nextInt(200) - 100; // 屏幕上方随机位置
            y = -random.nextInt(300);
            speedX = (random.nextFloat() - 0.5f) * 6;
            speedY = 2 + random.nextFloat() * 5;
            rotation = random.nextFloat() * 360;
            rotationSpeed = (random.nextFloat() - 0.5f) * 12;
            size = 8 + random.nextFloat() * 16;
            color = COLORS[random.nextInt(COLORS.length)];
        }

        void update(int screenW, int screenH, int frame) {
            x += speedX + screenW / 2f;
            y += speedY;
            rotation += rotationSpeed;
            speedX *= 0.99f;

            // 减速 + 淡出
            if (frame > 120) {
                alpha = Math.max(0, alpha - 0.03f);
                speedY *= 0.95f;
            }
        }
    }
}
