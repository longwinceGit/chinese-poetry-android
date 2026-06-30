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

public class ConfettiView extends View {

    private static final int COLORS[] = {
        0xFFFF6B6B, 0xFF4ECDC4, 0xFFFFD93D, 0xFFA78BFA,
        0xFFFB923C, 0xFF60A5FA, 0xFFF472B6, 0xFF34D399
    };

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
    private static final int MAX_FRAMES = 180;

    public ConfettiView(Context context) { super(context); init(); }
    public ConfettiView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        textPaint.setTextSize(24);
        textPaint.setTextAlign(Paint.Align.CENTER);
        setVisibility(GONE);
    }

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

    private void update() {
        int w = getWidth() > 0 ? getWidth() : 720;
        int h = getHeight() > 0 ? getHeight() : 1280;
        for (Particle p : particles) {
            p.update(w, h, frameCount);
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    private class Particle {
        float x, y;
        float vx, vy;
        float rotation, rotationSpeed;
        float size;
        int color;
        float alpha = 1f;
        int shape;
        boolean isEmoji;
        String emoji;

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
