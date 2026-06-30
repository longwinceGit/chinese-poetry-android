package com.poetry.util;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

public class TtsManager {

    private TextToSpeech tts;
    private boolean ready = false;

    public TtsManager(Context context) {
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.CHINESE);
                    tts.setSpeechRate(0.85f);
                    ready = true;
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
