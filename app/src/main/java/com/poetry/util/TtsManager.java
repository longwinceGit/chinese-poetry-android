package com.poetry.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TTS 语音管理器 —— 首选百度 AI 语音合成（云端），失败时降级到 Android 本地 TTS。
 *
 * 百度 API 音质更自然，但需要网络；Android TTS 离线可用，音质取决于系统语音包。
 * 两者在构造时并行初始化，自动选择可用引擎。
 *
 * 百度 API 文档：https://ai.baidu.com/ai-doc/SPEECH/Vk38lxily
 */
public class TtsManager {

    // ===== 百度 AI 应用凭证 =====
    private static final String API_KEY = "9RSpMQzW0u29vuDk7Sav3xTW";
    private static final String SECRET_KEY = "gogDQ4BAdJx60HiZepZQW67J5z1AZEGD";

    // ===== 静音间隔常量 =====
    private static final long SILENCE_TITLE_AUTHOR = 400;
    private static final long SILENCE_AUTHOR_LINES = 600;
    private static final long SILENCE_LINE_GAP = 380;
    private static final float SPEECH_RATE = 0.78f;

    /** TTS 初始化回调 */
    public interface OnInitListener {
        void onReady();
        void onError(String reason);
    }

    /** 逐句朗读回调 */
    public interface LineReadListener {
        void onLineComplete(int index, int total);
        void onAllComplete();
    }

    // ===== 引擎状态 =====
    private static final int MODE_NONE    = 0;
    private static final int MODE_BAIDU   = 1;
    private static final int MODE_ANDROID = 2;

    private final Context context;
    private int activeMode = MODE_NONE;          // 当前使用的引擎
    private boolean baiduReady = false;
    private boolean androidReady = false;
    private String errorMsg;
    private OnInitListener initListener;
    private LineReadListener lineListener;

    // Baidu 引擎
    private String accessToken;
    private long tokenExpireTime;
    private MediaPlayer mediaPlayer;

    // Android 引擎（降级）
    private TextToSpeech androidTts;

    private final ExecutorService executor;
    private final Handler mainHandler;
    private volatile boolean isPlaying = false;
    private int totalLines;

    /* ===================================================================
     * 构造与初始化
     * =================================================================== */

    public TtsManager(Context context) {
        this(context, null);
    }

    /**
     * 构造后并行初始化百度云端 TTS 和 Android 本地 TTS。
     * 任一引擎就绪即回调 onReady()。
     */
    public TtsManager(Context context, OnInitListener listener) {
        this.context = context.getApplicationContext();
        this.initListener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();

        // 并行初始化两个引擎
        fetchTokenAsync();
        initAndroidTts();
    }

    /** 后台获取百度 access_token */
    private void fetchTokenAsync() {
        executor.execute(() -> {
            try {
                String body = "grant_type=client_credentials"
                        + "&client_id=" + URLEncoder.encode(API_KEY, "UTF-8")
                        + "&client_secret=" + URLEncoder.encode(SECRET_KEY, "UTF-8");

                HttpURLConnection conn = (HttpURLConnection)
                        new URL("https://aip.baidubce.com/oauth/2.0/token").openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes("UTF-8"));
                }

                String json = new String(readAllBytes(conn.getInputStream()), "UTF-8");
                conn.disconnect();

                accessToken = extractJsonString(json, "access_token");
                int expiresIn = Integer.parseInt(extractJsonString(json, "expires_in"));
                tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300L) * 1000L;

                if (accessToken == null || accessToken.isEmpty()) {
                    throw new RuntimeException("access_token 为空");
                }

                baiduReady = true;
                tryFireReady();
            } catch (Exception ignored) {
                // 百度不可用，等 Android TTS 兜底
            }
        });
    }

    /** 初始化 Android TTS（降级备用） */
    private void initAndroidTts() {
        androidTts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = androidTts.setLanguage(Locale.CHINESE);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    errorMsg = "中文语音数据不可用";
                } else {
                    androidTts.setSpeechRate(SPEECH_RATE);
                    androidReady = true;
                    tryFireReady();
                    return;
                }
            } else {
                errorMsg = "语音引擎初始化失败";
            }
            // Android TTS 失败时如果百度也未就绪，通知错误
            mainHandler.post(() -> {
                if (!baiduReady && initListener != null) {
                    initListener.onError(errorMsg);
                }
            });
        });
    }

    /** 任一引擎就绪时触发 */
    private void tryFireReady() {
        if (baiduReady || androidReady) {
            activeMode = baiduReady ? MODE_BAIDU : MODE_ANDROID;
            mainHandler.post(() -> {
                if (initListener != null) initListener.onReady();
            });
        }
    }

    /* ===================================================================
     * 公开 API
     * =================================================================== */

    /**
     * 分句朗读诗词。
     *
     * 策略：
     * 1. 百度云端优先（需网络）— 整段合成 MP3 播放
     * 2. 百度失败 / 无网络 → 降级到 Android 本地 TTS
     */
    public void speakPoemStructured(String title, String author, String[] lines,
                                    LineReadListener listener) {
        stop();
        this.lineListener = listener;

        int lineCount = (lines != null) ? lines.length : 0;
        totalLines = 2 + lineCount;

        if (baiduReady) {
            // 尝试百度云端
            String fullText = buildFullText(title, author, lines);
            requestBaiduTts(fullText, () -> {
                // 百度失败 → 降级到 Android TTS
                if (androidReady) {
                    speakAndroidStructured(title, author, lines, listener);
                } else {
                    fireAllComplete();
                }
            });
        } else if (androidReady) {
            speakAndroidStructured(title, author, lines, listener);
        } else {
            fireAllComplete();
        }
    }

    /** 整段朗读（兼容旧调用） */
    public void speakPoem(String title, String author, String[] lines) {
        speakPoemStructured(title, author, lines, null);
    }

    /** 简单朗读（兼容旧调用） */
    public void speak(String text, String utteranceId) {
        stop();
        this.lineListener = null;
        String ttsText = text;
        if (ttsText.length() > 400) ttsText = ttsText.substring(0, 397) + "。。。";
        final String finalText = ttsText;
        final String finalUttId = utteranceId;

        if (baiduReady) {
            requestBaiduTts(finalText, () -> {
                if (androidReady && androidTts != null) {
                    androidTts.speak(finalText, TextToSpeech.QUEUE_FLUSH, null, finalUttId);
                }
            });
        } else if (androidReady && androidTts != null) {
            androidTts.speak(finalText, TextToSpeech.QUEUE_FLUSH, null, finalUttId);
        }
    }

    public boolean isReady() {
        return baiduReady || androidReady;
    }

    public boolean isSpeaking() {
        return isPlaying;
    }

    /** 停止播放 */
    public void stop() {
        // 停止 MediaPlayer（百度模式）
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        // 停止 Android TTS
        if (androidTts != null) {
            try {
                androidTts.stop();
            } catch (Exception ignored) {}
        }
        isPlaying = false;
        lineListener = null;
    }

    /** 释放全部资源 */
    public void shutdown() {
        stop();
        if (androidTts != null) {
            try {
                androidTts.shutdown();
            } catch (Exception ignored) {}
            androidTts = null;
        }
        executor.shutdownNow();
    }

    /* ===================================================================
     * 百度 TTS 实现
     * =================================================================== */

    /** 构造完整朗读文本 */
    private String buildFullText(String title, String author, String[] lines) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("。").append(author).append("。");
        if (lines != null) {
            for (String line : lines) {
                sb.append(line).append("。");
            }
        }
        String result = sb.toString();
        if (result.length() > 400) {
            result = result.substring(0, 397) + "。。。";
        }
        return result;
    }

    /**
     * 请求百度 TTS API，成功后播放 MP3，失败时调用 fallback。
     */
    private void requestBaiduTts(String text, Runnable fallback) {
        executor.execute(() -> {
            try {
                if (System.currentTimeMillis() >= tokenExpireTime) {
                    fetchTokenSync();
                }
                if (accessToken == null) {
                    mainHandler.post(() -> { if (fallback != null) fallback.run(); });
                    return;
                }

                String body = "tok=" + URLEncoder.encode(accessToken, "UTF-8")
                        + "&cuid=tts_android_001&ctp=1&lan=zh"
                        + "&spd=3&pit=5&vol=5&per=1&aue=3"
                        + "&tex=" + URLEncoder.encode(text, "UTF-8");

                HttpURLConnection conn = (HttpURLConnection)
                        new URL("https://tsn.baidu.com/text2audio").openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes("UTF-8"));
                }

                int code = conn.getResponseCode();
                String contentType = conn.getContentType();
                boolean isAudio = contentType != null
                        && (contentType.contains("audio") || contentType.contains("octet-stream"));

                if (code == 200 && isAudio) {
                    byte[] audioData = readAllBytes(conn.getInputStream());
                    conn.disconnect();

                    if (audioData.length < 200) {
                        mainHandler.post(() -> { if (fallback != null) fallback.run(); });
                        return;
                    }

                    File tmp = new File(context.getCacheDir(),
                            "tts_" + System.currentTimeMillis() + ".mp3");
                    try (FileOutputStream fos = new FileOutputStream(tmp)) {
                        fos.write(audioData);
                    }
                    mainHandler.post(() -> playMp3(tmp, fallback));
                } else {
                    conn.disconnect();
                    mainHandler.post(() -> { if (fallback != null) fallback.run(); });
                }
            } catch (Exception e) {
                mainHandler.post(() -> { if (fallback != null) fallback.run(); });
            }
        });
    }

    /** 用 MediaPlayer 播放 MP3 */
    private void playMp3(File file, Runnable fallback) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.setOnPreparedListener(mp -> {
                isPlaying = true;
                mp.start();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                file.delete();
                releaseMediaPlayer();
                fireAllComplete();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isPlaying = false;
                file.delete();
                releaseMediaPlayer();
                // MediaPlayer 解码失败 → 降级
                if (fallback != null) fallback.run();
                return true;
            });
            mediaPlayer.prepare();
        } catch (Exception e) {
            isPlaying = false;
            file.delete();
            releaseMediaPlayer();
            if (fallback != null) fallback.run();
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try { mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    /** 同步获取 access_token（过期刷新用） */
    private void fetchTokenSync() throws Exception {
        String body = "grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(API_KEY, "UTF-8")
                + "&client_secret=" + URLEncoder.encode(SECRET_KEY, "UTF-8");

        HttpURLConnection conn = (HttpURLConnection)
                new URL("https://aip.baidubce.com/oauth/2.0/token").openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }

        String json = new String(readAllBytes(conn.getInputStream()), "UTF-8");
        conn.disconnect();

        accessToken = extractJsonString(json, "access_token");
        int expiresIn = Integer.parseInt(extractJsonString(json, "expires_in"));
        tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300L) * 1000L;
    }

    /* ===================================================================
     * Android TTS 兜底实现（恢复原始逐句行为）
     * =================================================================== */

    /**
     * 使用 Android 原生 TTS 朗读（逐句 + 静音间隔），
     * 行为与原始实现完全一致。
     */
    private void speakAndroidStructured(String title, String author,
                                        String[] lines, LineReadListener listener) {
        if (androidTts == null) {
            fireAllComplete();
            return;
        }

        this.lineListener = listener;
        int lineCount = (lines != null) ? lines.length : 0;
        totalLines = 2 + lineCount;
        long ts = System.currentTimeMillis();

        // 1. 标题（FLUSH 清空旧队列）
        androidTts.speak(title, TextToSpeech.QUEUE_FLUSH, null, "pi_0_" + ts);

        // 2. 静音 → 作者
        androidTts.playSilentUtterance(SILENCE_TITLE_AUTHOR, TextToSpeech.QUEUE_ADD, null);
        androidTts.speak(author, TextToSpeech.QUEUE_ADD, null, "pi_1_" + ts);

        // 3. 静音 → 逐句
        if (lineCount > 0) {
            androidTts.playSilentUtterance(SILENCE_AUTHOR_LINES, TextToSpeech.QUEUE_ADD, null);
            for (int i = 0; i < lineCount; i++) {
                androidTts.speak(lines[i], TextToSpeech.QUEUE_ADD, null,
                        "pi_" + (i + 2) + "_" + ts);
                if (i < lineCount - 1) {
                    androidTts.playSilentUtterance(SILENCE_LINE_GAP,
                            TextToSpeech.QUEUE_ADD, null);
                }
            }
        }

        // 4. 进度监听
        androidTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                if (lineListener == null) return;
                if (utteranceId.startsWith("pi_")) {
                    int idx = Integer.parseInt(
                            utteranceId.substring(3, utteranceId.indexOf('_', 3)));
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

    /* ===================================================================
     * 辅助方法
     * =================================================================== */

    private void fireAllComplete() {
        if (lineListener != null) {
            LineReadListener l = lineListener;
            lineListener = null;
            l.onAllComplete();
        }
    }

    private byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        is.close();
        return baos.toByteArray();
    }

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start >= 0) {
            start += search.length();
            int end = json.indexOf("\"", start);
            if (end > start) return json.substring(start, end);
        }
        search = "\"" + key + "\":";
        start = json.indexOf(search);
        if (start >= 0) {
            start += search.length();
            int end = json.indexOf(",", start);
            if (end < 0) end = json.indexOf("}", start);
            if (end > start) return json.substring(start, end).trim();
        }
        return "";
    }
}
