package com.poetry.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

/**
 * TTS 语音管理器 —— 封装 Android TextToSpeech，提供中文朗读功能。
 *
 * 支持两种朗读模式：
 * - 整段朗读（speakPoem），兼容旧调用
 * - 分句朗读（speakPoemStructured），逐句入队 + 句间静音，自然流畅
 */
public class TtsManager {

    /** TTS 初始化回调 */
    public interface OnInitListener {
        void onReady();
        void onError(String reason);
    }

    /** 逐句朗读回调 */
    public interface LineReadListener {
        /** 第 index 句朗读完成（0 = 标题, 1 = 作者, 2+ = 诗句） */
        void onLineComplete(int index, int total);
        /** 全部朗读完成 */
        void onAllComplete();
    }

    // 语速：0.78 对古诗更自然（默认 1.0）
    private static final float SPEECH_RATE = 0.78f;
    // 标题→作者静音 ms
    private static final long SILENCE_TITLE_AUTHOR = 400;
    // 作者→首句静音 ms
    private static final long SILENCE_AUTHOR_LINES = 600;
    // 句间静音 ms
    private static final long SILENCE_LINE_GAP = 380;

    private TextToSpeech tts;
    private boolean ready = false;
    private OnInitListener initListener;
    private LineReadListener lineListener;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** 无回调构造 */
    public TtsManager(Context context) {
        this(context, null);
    }

    /**
     * @param context  上下文
     * @param listener 初始化回调，可为 null
     */
    public TtsManager(Context context, OnInitListener listener) {
        this.initListener = listener;
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.CHINESE);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ready = false;
                    if (initListener != null) initListener.onError("中文语音数据不可用");
                } else {
                    tts.setSpeechRate(SPEECH_RATE);
                    ready = true;
                    if (initListener != null) initListener.onReady();
                }
            } else {
                ready = false;
                if (initListener != null) initListener.onError("语音引擎初始化失败");
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                if (lineListener == null) return;
                if (utteranceId.startsWith("pi_")) {
                    int idx = Integer.parseInt(utteranceId.substring(3));
                    mainHandler.post(() -> lineListener.onLineComplete(idx, totalLines));
                }
            }

            @Override
            public void onError(String utteranceId) {}

            @Override
            public void onStop(String utteranceId, boolean interrupted) {
                if (interrupted) {
                    mainHandler.post(() -> {
                        if (lineListener != null) lineListener.onAllComplete();
                    });
                }
            }
        });
    }

    // ----- 旧 API：整段朗读（保留兼容） -----

    public void speak(String text, String utteranceId) {
        if (tts != null && ready) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        }
    }

    public void speakPoem(String title, String author, String[] lines) {
        if (tts == null || !ready) return;
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("。").append(author).append("。");
        if (lines != null) {
            for (String line : lines) {
                sb.append(line).append("。");
            }
        }
        tts.speak(sb.toString(), TextToSpeech.QUEUE_FLUSH, null, "poem_" + System.currentTimeMillis());
    }

    // ----- 新 API：分句朗读 -----

    private int totalLines = 0;

    /**
     * 分句朗读诗词，逐句入队 + 句间静音。
     *
     * 朗读序列：
     *   标题 → [400ms静音] → 作者 → [600ms静音] → 诗句1 → [380ms] → 诗句2 → ...
     *
     * 调用此方法会自动中断之前未完成的朗读。
     *
     * @param title    诗词标题
     * @param author   作者
     * @param lines    诗句数组
     * @param listener 逐句完成回调，可为 null
     */
    public void speakPoemStructured(String title, String author, String[] lines, LineReadListener listener) {
        if (tts == null || !ready) return;
        stop();
        this.lineListener = listener;

        int lineCount = (lines != null) ? lines.length : 0;
        // 总数 = 标题 + 作者 + N条诗句
        totalLines = 2 + lineCount;

        long ts = System.currentTimeMillis();

        // 1. 标题（FLUSH 清空旧队列）
        tts.speak(title, TextToSpeech.QUEUE_FLUSH, null, "pi_0_" + ts);

        // 2. 静音 → 作者
        tts.playSilentUtterance(SILENCE_TITLE_AUTHOR, TextToSpeech.QUEUE_ADD, null);
        tts.speak(author, TextToSpeech.QUEUE_ADD, null, "pi_1_" + ts);

        // 3. 静音 → 逐句
        if (lineCount > 0) {
            tts.playSilentUtterance(SILENCE_AUTHOR_LINES, TextToSpeech.QUEUE_ADD, null);
            for (int i = 0; i < lineCount; i++) {
                tts.speak(lines[i], TextToSpeech.QUEUE_ADD, null, "pi_" + (i + 2) + "_" + ts);
                if (i < lineCount - 1) {
                    tts.playSilentUtterance(SILENCE_LINE_GAP, TextToSpeech.QUEUE_ADD, null);
                }
            }
        }

        // 4. 全部结束监听
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                if (lineListener == null) return;
                if (utteranceId.startsWith("pi_")) {
                    // 格式: "pi_{index}_{timestamp}"
                    int idx = Integer.parseInt(utteranceId.substring(3, utteranceId.indexOf('_', 3)));
                    mainHandler.post(() -> lineListener.onLineComplete(idx, totalLines));
                    if (idx == totalLines - 1) {
                        mainHandler.post(() -> lineListener.onAllComplete());
                    }
                }
            }

            @Override
            public void onError(String utteranceId) {}

            @Override
            public void onStop(String utteranceId, boolean interrupted) {
                if (interrupted && lineListener != null) {
                    mainHandler.post(() -> lineListener.onAllComplete());
                }
            }
        });
    }

    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
        lineListener = null;
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    public boolean isReady() { return ready; }
}
