package com.poetry.domain;

import com.poetry.data.DailyStats;
import com.poetry.data.GameHistory;
import com.poetry.data.LearningDatabase;
import com.poetry.data.PoemRepository;
import com.poetry.data.UserProfile;
import com.poetry.data.model.Poem;

import java.time.LocalDate;
import java.util.List;

/**
 * 统一游戏结算引擎 —— 收敛所有游戏结束时的重复结算逻辑。
 *
 * <p>取代原先分散在 {@code GameViewModel.savePoints / recordGameActivity / finishCoupletGame}
 * 与 {@code QuizViewModel.submitAnswer / finishQuiz} 中的重复代码（见 GAME_REDESIGN_FINAL.md §10.1）。
 * 所有游戏（对诗 / 消消乐 / 飞花令）结束时构造一个 {@link GameResult}，交给
 * {@link #settle(LearningDatabase, GameResult, AchievementEngine.AchievementListener)}
 * 在调用方的 IO 线程上统一完成：积分累加、等级提升、每日统计、学习沉淀、成就检测、主题同步。
 *
 * <p><b>线程约定：</b>本方法<b>不会</b>自行创建线程，调用方（ViewModel / Fragment）必须
 * 已在 {@code AppExecutors.io()} 线程上调用。
 *
 * <p><b>Quiz 排除说明：</b>填空（Quiz）采用"逐题计分"语义（每题独立 {@code calcPointsForQuiz}
 * 并按空命中比例写 {@code quizScore}，驱动 {@code quiz_perfect_5} 成就），与整局结算语义不同，
 * 故本里程碑<b>不</b>将 Quiz 路由到本引擎（M9/M10 可能重新评估）。Quiz 的积分/等级/成就/主题
 * 逻辑保持原样。
 */
public final class GameSettlement {

    private GameSettlement() {
        // 工具类，禁止实例化
    }

    /**
     * 统一结算一局游戏。
     *
     * <p>按顺序执行：
     * <ol>
     *   <li>积分原子累加 {@code addTotalPoints(r.score)}，随后重读档案计算等级，若提升则
     *       {@code updateLevel}（镜像原 {@code QuizViewModel.submitAnswer} 语义，不改积分规则）；</li>
     *   <li>每日统计：确保今日行存在后 {@code incrementGamesPlayed} 与 {@code incrementPointsEarned}；</li>
     *   <li>学习沉淀：对 {@code r.getTouchedPoemIds()} 逐 id {@code ensureRecordExists}（标记"玩过"）；</li>
     *   <li>写 {@code game_history} 成绩留痕（当 {@code r} 携带 playedAt/duration 时）；</li>
     *   <li>成就检测 {@code AchievementEngine.checkAndUnlock}（覆盖累计型新成就）；</li>
     *   <li>触发型成就判定（3 星 / 对诗全对）；</li>
     *   <li>主题同步 {@code ThemeManager.syncUnlockedThemes}。</li>
     * </ol>
     *
     * @param db       数据库实例
     * @param r        本局结算结果（非空）
     * @param listener 成就解锁回调（用于 UI 通知，可为 null）
     */
    public static void settle(LearningDatabase db, GameResult r,
                              AchievementEngine.AchievementListener listener) {
        if (db == null || r == null) return;

        // 1. 积分原子累加 + 等级提升（镜像 QuizViewModel.submitAnswer 语义）
        db.userProfileDao().addTotalPoints(r.score);
        UserProfile profile = db.userProfileDao().getUserProfileSync();
        if (profile != null) {
            int newLevel = LearningEngine.calcLevel(profile.totalPoints);
            if (newLevel != profile.level) {
                db.userProfileDao().updateLevel(newLevel);
            }
        }

        // 2. 每日统计：确保今日行存在，再递增游戏次数与当日积分
        final String today = LocalDate.now().toString();
        DailyStats existing = db.dailyStatsDao().getDailyStatsSync(today);
        if (existing == null) {
            db.dailyStatsDao().upsertDailyStats(new DailyStats(today));
        }
        db.dailyStatsDao().incrementGamesPlayed(today);
        db.dailyStatsDao().incrementPointsEarned(today, r.score);

        // 3. 学习沉淀：本局涉及的诗词 id 标记"玩过"（非"已学"）
        for (String id : r.getTouchedPoemIds()) {
            db.learningRecordDao().ensureRecordExists(id);
        }

        // 4. 写游戏历史成绩留痕
        db.gameHistoryDao().insert(new GameHistory(
                r.gameType, r.score, r.stars,
                r.correctCount, r.totalCount,
                System.currentTimeMillis(), r.durationMillis));

        // 5. 成就检测（覆盖累计型新成就：flyflower_20 / star_30 / play_3_days / play_7_days / month_poem_collect）
        AchievementEngine.checkAndUnlock(db, listener);

        // 6. 触发型成就判定（仅当本局满足触发条件时解锁）
        if (r.stars >= 3) {
            AchievementEngine.unlockDirect(db, "star_first_3", listener);
        }
        if ("couplet".equals(r.gameType) && r.perfect) {
            AchievementEngine.unlockDirect(db, "couplet_first_perfect", listener);
        }

        // 7. 主题同步
        ThemeManager.syncUnlockedThemes(db);
    }

    /**
     * 从本局涉及的诗词 id 中挑选"最美一句"所属诗词 id（§9.2）。
     *
     * <p>优先级：优先选择有白话释义（{@code explanation != null}）的诗词；
     * 否则选择诗句最长的一首；否则取列表第一首。列表为空时返回 null。
     *
     * @param touchedIds 本局涉及的诗词 id 列表
     * @return 选中的诗词 id，无可用时返回 null
     */
    public static String pickBestPoemId(List<String> touchedIds) {
        if (touchedIds == null || touchedIds.isEmpty()) return null;

        String best = null;
        String bestLine = null;
        for (String id : touchedIds) {
            if (id == null || id.isEmpty()) continue;
            Poem poem = PoemRepository.getInstance().findPoemById(id);
            if (poem == null) continue;

            // 优先有释义的名篇
            if (poem.explanation != null && !poem.explanation.isEmpty()) {
                return id;
            }
            // 否则记录最长诗句
            String line = longestLine(poem);
            if (line != null && (bestLine == null || line.length() > bestLine.length())) {
                bestLine = line;
                best = id;
            }
        }
        return best;
    }

    /** 取一首诗中最长的一行（无行时返回 null）。 */
    private static String longestLine(Poem poem) {
        if (poem == null || poem.lines == null || poem.lines.length == 0) return null;
        String longest = null;
        for (String line : poem.lines) {
            if (line == null) continue;
            if (longest == null || line.length() > longest.length()) {
                longest = line;
            }
        }
        return longest;
    }
}
