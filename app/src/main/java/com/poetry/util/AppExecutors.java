package com.poetry.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 全局线程池管理器 —— 统一管理应用中所有异步操作。
 *
 * <p>替代所有散落的 {@code new Thread()} 调用，提供：
 * <ul>
 *   <li>{@link #io(Runnable)} — 后台 IO / 数据库操作</li>
 *   <li>{@link #main(Runnable)} — 切回主线程执行</li>
 * </ul>
 *
 * <p>使用方式：
 * <pre>{@code
 *   AppExecutors.io(() -> {
 *       // 后台操作
 *       AppExecutors.main(() -> {
 *           // 主线程更新 UI
 *       });
 *   });
 * }</pre>
 */
public class AppExecutors {

    private static final ExecutorService IO_POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private AppExecutors() {}

    /**
     * 在后台 IO 线程池中执行任务。
     * 适用于 Room 数据库操作、文件读写等。
     * 内部捕获所有异常并通过 {@link #main(Runnable)} 回调。
     */
    public static void io(Runnable task) {
        IO_POOL.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                android.util.Log.e("AppExecutors", "Background task failed", e);
            }
        });
    }

    /**
     * 在主线程执行任务。
     * 适用于在 IO 操作完成后更新 UI。
     */
    public static void main(Runnable task) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            MAIN_HANDLER.post(task);
        }
    }
}
