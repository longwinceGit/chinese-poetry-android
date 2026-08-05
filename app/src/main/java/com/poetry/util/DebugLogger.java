package com.poetry.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 调试日志收集器（DebugLogger）。
 * <p>
 * 供「调试日志」面板（{@link com.poetry.ui.debug.DebugLogFragment}）使用：
 * <ul>
 *     <li>提供与 {@link Log} 相同签名的静态方法（v/d/i/w/e），始终透传 logcat；</li>
 *     <li>收集开关开启时写入<b>内存环形缓冲</b>（上限 {@link #MAX_ENTRIES} 条），供面板实时读取；</li>
 *     <li>文件开关开启时异步追加到 {@code files/debug_logs/log_yyyyMMdd.txt}；</li>
 *     <li>{@link #init(Context)} 安装全局 {@link Thread.UncaughtExceptionHandler}，
 *         未捕获异常写入 "CRASH" 日志并同步落盘，方便事后分析；</li>
 *     <li>面板通过 {@link Listener} 实时刷新，主线程回调（已做 100ms 节流防刷屏）。</li>
 * </ul>
 * 开关状态存于 {@code SharedPreferences("debug_settings")}，默认收集开启、文件关闭。
 */
public final class DebugLogger {

    /** 收集开关 / 文件开关的 SharedPreferences 文件与键。 */
    public static final String PREFS = "debug_settings";
    public static final String KEY_ENABLED = "log_enabled";
    public static final String KEY_FILE_ENABLED = "file_enabled";

    /** 内存环形缓冲上限（条）。 */
    private static final int MAX_ENTRIES = 2000;

    /** 面板监听器（主线程回调，100ms 节流）。 */
    public interface Listener {
        void onLogChanged();
    }

    /** 一条日志。 */
    public static final class Entry {
        public final long timeMillis;
        public final int level;
        public final String tag;
        public final String message;

        Entry(long timeMillis, int level, String tag, String message) {
            this.timeMillis = timeMillis;
            this.level = level;
            this.tag = tag;
            this.message = message;
        }
    }

    private static Context appContext;
    private static boolean inited = false;

    private static final ArrayDeque<Entry> buffer = new ArrayDeque<>(MAX_ENTRIES);
    private static final Object LOCK = new Object();
    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static Thread.UncaughtExceptionHandler defaultHandler;
    private static boolean notifyScheduled = false;

    private DebugLogger() {
    }

    /**
     * 初始化日志收集器（建议在 {@code MainActivity.onCreate} 或自定义 Application 中调用）。
     * <p>安装全局崩溃捕获（链式保留原 Handler），并写入一条启动标记日志。
     *
     * @param context 任意 Context（内部取 applicationContext 防泄漏）
     */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
        inited = true;

        // 链式安装全局未捕获异常 Handler：先记日志（同步落盘），再交给原 Handler 兜底
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            String stack = Log.getStackTraceString(throwable);
            log(Log.ERROR, "CRASH", "未捕获异常 @ " + thread.getName() + ":\n" + stack, true);
            // 崩溃进程即将终止，异步写可能来不及 → 同步落盘崩溃现场（含历史缓冲）
            syncWriteCrash(thread, stack);
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });

        i("DebugLogger", "调试日志已初始化（收集=" + isEnabled() + "，文件=" + isFileEnabled() + "）");
    }

    // ==================== 开关 ====================

    private static SharedPreferences prefs() {
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 内存收集开关（默认开启）。 */
    public static boolean isEnabled() {
        return appContext == null || prefs().getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (enabled) {
            i("DebugLogger", "日志收集已开启");
        } else {
            i("DebugLogger", "日志收集已关闭");
        }
    }

    /** 文件落盘开关（默认关闭，防止长期运行日志文件膨胀）。 */
    public static boolean isFileEnabled() {
        return appContext != null && prefs().getBoolean(KEY_FILE_ENABLED, false);
    }

    public static void setFileEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_FILE_ENABLED, enabled).apply();
        if (enabled) {
            // 开启时立即把当前缓冲落盘，保证面板切走也不丢
            i("DebugLogger", "日志文件落盘已开启");
            flushToFile(snapshot());
        } else {
            i("DebugLogger", "日志文件落盘已关闭");
        }
    }

    // ==================== 日志 API（与 android.util.Log 同签名） ====================

    public static void v(String tag, String msg) {
        log(Log.VERBOSE, tag, msg, false);
    }

    public static void d(String tag, String msg) {
        log(Log.DEBUG, tag, msg, false);
    }

    public static void i(String tag, String msg) {
        log(Log.INFO, tag, msg, false);
    }

    public static void w(String tag, String msg) {
        log(Log.WARN, tag, msg, false);
    }

    public static void e(String tag, String msg) {
        log(Log.ERROR, tag, msg, false);
    }

    /**
     * 统一写入：logcat 始终透传；收集开启时进内存缓冲；文件开启时异步落盘。
     *
     * @param level      {@link Log#VERBOSE} 等
     * @param tag        日志标签
     * @param msg        日志内容
     * @param forceFile  是否无视文件开关强制落盘（仅崩溃路径使用）
     */
    private static void log(int level, String tag, String msg, boolean forceFile) {
        if (msg == null) msg = "null";
        Log.println(level, tag, msg);

        if (!inited) return;
        boolean fileEnabled = isFileEnabled();
        if (!isEnabled() && !(forceFile || fileEnabled)) return;

        Entry entry = new Entry(System.currentTimeMillis(), level, tag, msg);
        synchronized (LOCK) {
            if (buffer.size() >= MAX_ENTRIES) {
                buffer.pollFirst();
            }
            buffer.addLast(entry);
        }
        if (fileEnabled || forceFile) {
            appendToFile(entry);
        }
        scheduleNotify();
    }

    // ==================== 读取 / 清空 ====================

    /** 返回缓冲快照（时间升序，最新在末尾）。 */
    public static List<Entry> snapshot() {
        synchronized (LOCK) {
            return new ArrayList<>(buffer);
        }
    }

    /**
     * 按级别与关键词过滤查询。
     *
     * @param minLevel 最低级别（{@link Log#VERBOSE}=2 表示全部），传 0 也视为全部
     * @param keyword  关键词（tag 或内容，忽略大小写；null/空串 不过滤）
     * @return 过滤后的日志列表（时间升序）
     */
    public static List<Entry> query(int minLevel, String keyword) {
        int level = minLevel >= Log.VERBOSE ? minLevel : Log.VERBOSE;
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Entry> result = new ArrayList<>();
        synchronized (LOCK) {
            for (Entry e : buffer) {
                if (e.level < level) continue;
                if (!kw.isEmpty()
                        && !e.tag.toLowerCase(Locale.ROOT).contains(kw)
                        && !e.message.toLowerCase(Locale.ROOT).contains(kw)) {
                    continue;
                }
                result.add(e);
            }
        }
        return result;
    }

    /** 清空内存缓冲。 */
    public static void clear() {
        synchronized (LOCK) {
            buffer.clear();
        }
        scheduleNotify();
    }

    // ==================== 面板监听 ====================

    public static void addListener(Listener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public static void removeListener(Listener l) {
        listeners.remove(l);
    }

    /** 100ms 节流通知，避免高频日志导致 UI 反复刷新。 */
    private static void scheduleNotify() {
        if (notifyScheduled) return;
        notifyScheduled = true;
        mainHandler.postDelayed(() -> {
            notifyScheduled = false;
            for (Listener l : listeners) {
                l.onLogChanged();
            }
        }, 100);
    }

    // ==================== 文件落盘 ====================

    private static final SimpleDateFormat FILE_NAME_FMT =
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT);
    private static final SimpleDateFormat LINE_TIME_FMT =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT);

    private static File logDir() {
        File dir = new File(appContext.getFilesDir(), "debug_logs");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    /** 按日期归档的日志文件。 */
    private static File dailyLogFile() {
        SimpleDateFormat day = new SimpleDateFormat("yyyyMMdd", Locale.ROOT);
        return new File(logDir(), "log_" + day.format(new Date()) + ".txt");
    }

    private static void appendToFile(Entry e) {
        final Entry entry = e;
        AppExecutors.io(() -> {
            try (FileWriter fw = new FileWriter(dailyLogFile(), true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println(formatLine(entry));
            } catch (IOException ex) {
                Log.w("DebugLogger", "日志文件写入失败", ex);
            }
        });
    }

    /** 把一批日志一次性落盘（文件开关开启时先写历史缓冲）。 */
    private static void flushToFile(final List<Entry> entries) {
        AppExecutors.io(() -> {
            try (FileWriter fw = new FileWriter(dailyLogFile(), true);
                 PrintWriter pw = new PrintWriter(fw)) {
                for (Entry e : entries) {
                    pw.println(formatLine(e));
                }
            } catch (IOException ex) {
                Log.w("DebugLogger", "日志文件批量写入失败", ex);
            }
        });
    }

    /**
     * 崩溃现场同步落盘：写 {@code crash_yyyyMMdd_HHmmss.txt}（崩溃堆栈 + 崩溃前历史缓冲）。
     * <p>仅在未捕获异常路径调用（此时线程即将终止，必须同步写保证不丢）。
     */
    private static void syncWriteCrash(Thread thread, String stack) {
        if (appContext == null) return;
        try {
            File file = new File(logDir(),
                    "crash_" + FILE_NAME_FMT.format(new Date()) + ".txt");
            StringBuilder sb = new StringBuilder();
            sb.append("===== 崩溃现场 ").append(LINE_TIME_FMT.format(new Date())).append(" =====\n");
            sb.append("线程: ").append(thread.getName()).append('\n');
            sb.append("堆栈:\n").append(stack).append('\n');
            sb.append("---- 崩溃前日志（最近 ").append(MAX_ENTRIES).append(" 条）----\n");
            for (Entry e : snapshot()) {
                sb.append(formatLine(e)).append('\n');
            }
            try (FileWriter fw = new FileWriter(file);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.print(sb);
            }
        } catch (IOException ex) {
            Log.w("DebugLogger", "崩溃现场写入失败", ex);
        }
    }

    private static String formatLine(Entry e) {
        return LINE_TIME_FMT.format(new Date(e.timeMillis))
                + " " + levelChar(e.level)
                + "/" + e.tag + ": " + e.message;
    }

    private static char levelChar(int level) {
        switch (level) {
            case Log.DEBUG: return 'D';
            case Log.INFO:  return 'I';
            case Log.WARN:  return 'W';
            case Log.ERROR: return 'E';
            default:        return 'V';
        }
    }

    // ==================== 导出 ====================

    /**
     * 生成过滤后的完整日志文本（用于复制 / 分享）。
     *
     * @param minLevel 最低级别
     * @param keyword  关键词过滤
     * @return 日志文本
     */
    public static String exportText(int minLevel, String keyword) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 诗词乐园调试日志 ").append(FILE_NAME_FMT.format(new Date())).append(" =====\n");
        List<Entry> list = query(minLevel, keyword);
        for (Entry e : list) {
            sb.append(formatLine(e)).append('\n');
        }
        sb.append("===== 共 ").append(list.size()).append(" 条 =====\n");
        return sb.toString();
    }

    /**
     * 把过滤后的日志导出为文件并返回。
     *
     * @param minLevel 最低级别
     * @param keyword  关键词过滤
     * @return 导出的文件（失败时返回 null）
     */
    public static File exportFile(int minLevel, String keyword) {
        try {
            File file = new File(logDir(), "export_" + FILE_NAME_FMT.format(new Date()) + ".txt");
            try (FileWriter fw = new FileWriter(file);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.print(exportText(minLevel, keyword));
            }
            return file;
        } catch (IOException ex) {
            Log.w("DebugLogger", "日志导出失败", ex);
            return null;
        }
    }
}
