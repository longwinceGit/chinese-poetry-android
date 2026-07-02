package com.poetry.util;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

/**
 * TTS 语音管理器，封装 Android TextToSpeech，提供中文朗读功能。
 * 支持整首诗词朗读，内置语速调节（0.85x）。
 */
public class TtsManager {

    /**
     * TTS 初始化状态回调
     */
    public interface OnInitListener {
        void onReady();
        void onError(String reason);
    }

    private TextToSpeech tts;
    private boolean ready = false;
    private OnInitListener initListener;

    /**
     * 构建 TTS 管理器（无初始化回调）。
     *
     * @param context 上下文
     */
    public TtsManager(Context context) {
        this(context, null);
    }

    /**
     * 构建 TTS 管理器。
     *
     * @param context  上下文
     * @param listener 初始化完成回调，可为 null
     */
    public TtsManager(Context context, OnInitListener listener) {
        this.initListener = listener;
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.CHINESE);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        ready = false;
                        if (initListener != null) {
                            initListener.onError("中文语音数据不可用，请安装中文语音包");
                        }
                    } else {
                        tts.setSpeechRate(0.85f);
                        ready = true;
                        if (initListener != null) {
                            initListener.onReady();
                        }
                    }
                } else {
                    ready = false;
                    if (initListener != null) {
                        initListener.onError("语音引擎初始化失败");
                    }
                }
            }
        });
    }

    /**
     * 朗读指定文本。
     *
     * @param text        待朗读的文本
     * @param utteranceId 语音任务唯一标识
     */
    public void speak(String text, String utteranceId) {
        if (tts != null && ready) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        }
    }

    /**
     * 朗读完整诗词（标题 + 作者 + 各诗句，以句号分隔）。
     *
     * @param title  诗词标题
     * @param author 作者
     * @param lines  诗句数组
     */
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

    /**
     * @return true 表示当前正在朗读
     */
    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    /**
     * 停止当前朗读（不释放引擎资源）。
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    /**
     * 停止朗读并释放 TTS 引擎资源，应在不再使用时调用。
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    /**
     * @return true 表示 TTS 引擎已初始化成功，可以朗读
     */
    public boolean isReady() {
        return ready;
    }
}
