package com.poetry.util;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

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

    public TtsManager(Context context) {
        this(context, null);
    }

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

    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    public boolean isReady() {
        return ready;
    }
}
