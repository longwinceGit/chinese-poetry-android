package com.poetry.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 未完成局快照（M9，GAME_REDESIGN_FINAL.md §8.5）。
 *
 * <p>当玩家在游戏中意外退出（返回/杀进程/切后台被回收）时，把「未完成局」写入
 * {@code SharedPreferences} 文件 {@code game_snapshot}，下次进大厅提示「继续上次！」。
 *
 * <p>存储格式（prefs 名 {@code game_snapshot}）：
 * <ul>
 *   <li>{@code gameType}：String，未完成局的游戏类型（couplet / match / flyflower / quiz）。</li>
 *   <li>{@code startedAt}：long，该局开始时间戳（毫秒）。</li>
 * </ul>
 *
 * <p>生命周期：
 * <ul>
 *   <li>游戏 {@code onViewCreated} / 开局时 {@link #save(Context, String, long)}；</li>
 *   <li>游戏完成 / 结算导航时 {@link #clear(Context)}；</li>
 *   <li>{@code onDestroyView} 不清理（这正是「意外退出保留进度」的意义）。</li>
 * </ul>
 *
 * <p>纯静态工具类，线程安全（SharedPreferences 内部同步）。
 */
public final class GameSnapshot {

    /** SharedPreferences 文件名。 */
    private static final String PREFS_NAME = "game_snapshot";

    private static final String KEY_TYPE = "gameType";
    private static final String KEY_STARTED = "startedAt";

    private GameSnapshot() {
        // 工具类，禁止实例化
    }

    /**
     * 保存未完成局快照。
     *
     * @param ctx            上下文
     * @param gameType       游戏类型（couplet / match / flyflower / quiz）
     * @param startedAtMillis 该局开始时间戳（毫秒）
     */
    public static void save(Context ctx, String gameType, long startedAtMillis) {
        prefs(ctx).edit()
                .putString(KEY_TYPE, gameType)
                .putLong(KEY_STARTED, startedAtMillis)
                .apply();
    }

    /**
     * 读取未完成局的游戏类型。
     *
     * @param ctx 上下文
     * @return 游戏类型；无未完成局时返回 null
     */
    public static String getGameType(Context ctx) {
        return prefs(ctx).getString(KEY_TYPE, null);
    }

    /**
     * 清除未完成局快照（游戏完成 / 大厅「继续上次」点击后调用）。
     *
     * @param ctx 上下文
     */
    public static void clear(Context ctx) {
        prefs(ctx).edit().remove(KEY_TYPE).remove(KEY_STARTED).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
