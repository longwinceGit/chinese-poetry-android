package com.poetry.ui.game;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.DailyStats;
import com.poetry.data.GameHistory;
import com.poetry.data.LearningDatabase;
import com.poetry.data.PoemRepository;
import com.poetry.data.model.Poem;
import com.poetry.domain.AchievementEngine;
import com.poetry.domain.FlyflowerEngine;
import com.poetry.domain.GameSettlement;
import com.poetry.domain.ThemeManager;
import com.poetry.util.AppExecutors;
import com.poetry.util.DifficultyProfile;
import com.poetry.util.MoonBlessing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 飞花令 ViewModel（AndroidViewModel，独立实例）。
 *
 * <p>管理飞花令单局完整生命周期：选主题字 → 60 秒倒计时 → 手动输入 / 候选句点选命中 →
 * 计分（基础分 + 连击 + 长度加成）→ 结算（GameHistory + 学习沉淀 + 每日统计 + 成就 + 主题）。
 *
 * <p>结算模式镜像 {@link GameViewModel#finishCoupletGame()}：写 GameHistory、逐 id
 * {@code ensureRecordExists}、{@code recordGameActivity()}、{@code AchievementEngine.checkAndUnlock}
 * + {@code ThemeManager.syncUnlockedThemes}，全部在 {@link AppExecutors#io()} 线程执行。
 */
public class FlyflowerViewModel extends AndroidViewModel {

    /** 飞花令单局限时（秒），GAME_REDESIGN_FINAL.md §4.4 / §9.1。 */
    public static final int FLY_TIMER_SECONDS = 60;
    /** 「提示一句」每局限用次数。 */
    public static final int MAX_HINTS = 2;
    /** 候选句数量（点选即命中）。 */
    public static final int CANDIDATE_COUNT = 4;

    private final PoemRepository repo = PoemRepository.getInstance();
    private final LearningDatabase db;

    // 局内状态
    private final MutableLiveData<String> keyword = new MutableLiveData<>();
    private final MutableLiveData<Integer> remainingSeconds = new MutableLiveData<>(FLY_TIMER_SECONDS);
    private final MutableLiveData<Integer> hitCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> score = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> comboStreak = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> finished = new MutableLiveData<>(false);
    private final MutableLiveData<String> lastHitLine = new MutableLiveData<>();
    private final MutableLiveData<String> missMessage = new MutableLiveData<>();
    private final MutableLiveData<AchievementEngine.AchievementDef> newAchievement = new MutableLiveData<>();

    // 结算累积
    private final Set<String> usedThemes = new HashSet<>();
    private final Set<String> touchedPoemIds = new HashSet<>();
    private int hintRemaining = MAX_HINTS;
    private int tripletCount = 0;      // 已累计的连击组数（每连续 3 句命中 +1）
    private int longLineCount = 0;     // 命中句去空白后 ≥5 字的句数
    private long startTime = 0L;
    /** M10 月光祝福：本局累计加成（每命中一次祝福 +30 基础分，纯增益） */
    private int blessedBonus = 0;
    /** M10 月光祝福：最近一次命中是否触发祝福（供 UI 🌕 badge） */
    private boolean lastHitBlessed = false;
    private boolean timedOut = false;
    private boolean finishedFlag = false;
    private int finalScore = 0;
    private int finalStars = 1;

    private final Handler ticker = new Handler(Looper.getMainLooper());

    public FlyflowerViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    /**
     * 开始一局新的飞花令：选主题字、重置计数、启动 60 秒倒计时。
     */
    public void startFlyflower() {
        ticker.removeCallbacksAndMessages(null);
        finishedFlag = false;
        timedOut = false;
        finalScore = 0;
        finalStars = 1;
        hintRemaining = MAX_HINTS;
        tripletCount = 0;
        longLineCount = 0;
        touchedPoemIds.clear();
        startTime = System.currentTimeMillis();
        // M10 月光祝福：重置本局加成与最近命中标记
        blessedBonus = 0;
        lastHitBlessed = false;

        String theme = FlyflowerEngine.pickThemeChar(usedThemes);
        usedThemes.add(theme);
        keyword.setValue(theme);
        remainingSeconds.setValue(FLY_TIMER_SECONDS);
        hitCount.setValue(0);
        score.setValue(0);
        comboStreak.setValue(0);
        finished.setValue(false);
        lastHitLine.setValue(null);
        missMessage.setValue(null);

        startTicker();
    }

    /** 启动 1 秒倒计时 ticker，到 0 触发结算。 */
    private void startTicker() {
        ticker.removeCallbacksAndMessages(null);
        final long startElapsed = SystemClock.elapsedRealtime();
        ticker.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (finishedFlag) return;
                long elapsed = SystemClock.elapsedRealtime() - startElapsed;
                int remaining = FLY_TIMER_SECONDS - (int) (elapsed / 1000L);
                if (remaining <= 0) {
                    remainingSeconds.setValue(0);
                    handleTimeout();
                    return;
                }
                remainingSeconds.setValue(remaining);
                ticker.postDelayed(this, 1000L);
            }
        }, 1000L);
    }

    /**
     * 提交手动输入。
     *
     * @param input 玩家输入
     * @return 0=命中，1=未命中（不结束游戏，仅温柔提示）
     */
    public int submitLine(String input) {
        if (finishedFlag) return 1;
        String k = keyword.getValue();
        if (k == null) return 1;
        if (!FlyflowerEngine.judge(input, k)) {
            missMessage.setValue(input == null ? "" : input);
            return 1;
        }
        registerHit(FlyflowerEngine.cleanLineForDisplay(input));
        return 0;
    }

    /**
     * 候选句点选命中（计入命中与连击）。
     *
     * @param line 被点选的候选句
     */
    public void selectCandidate(String line) {
        if (finishedFlag) return;
        registerHit(FlyflowerEngine.cleanLineForDisplay(line));
    }

    /** 统一登记一次命中：更新计数、连击、得分、连击组、长度加成与最后命中句。 */
    private void registerHit(String line) {
        int hits = (hitCount.getValue() != null ? hitCount.getValue() : 0) + 1;
        hitCount.setValue(hits);

        int streak = (comboStreak.getValue() != null ? comboStreak.getValue() : 0) + 1;
        comboStreak.setValue(streak);

        // 每连续 3 句命中计一组连击（封顶 +200）
        if (streak % 3 == 0) {
            tripletCount++;
        }

        // 长度加成：去空白后 ≥5 字
        String clean = FlyflowerEngine.cleanLineForDisplay(line);
        if (clean != null && clean.replaceAll("\\s", "").length() >= FlyflowerEngine.LONG_LINE_THRESHOLD) {
            longLineCount++;
        }

        int newScore = FlyflowerEngine.calcScore(hits, tripletCount, longLineCount);

        // M10 月光祝福：第 hits 次命中触发 → 仅基础分 30 翻倍追加（纯增益，≤2次/局）
        lastHitBlessed = MoonBlessing.isBlessedRound(startTime, hits - 1, 10);
        if (lastHitBlessed) {
            blessedBonus += FlyflowerEngine.BASE_SCORE_PER_HIT;
        }
        newScore += blessedBonus;
        score.setValue(newScore);

        lastHitLine.setValue(clean != null ? clean : line);
        missMessage.setValue(null);
    }

    /**
     * 构建候选句列表（点「📖 提示一句」时调用）。
     *
     * <p>从诗词库中收集含主题字且可用的诗句，去重后随机取 {@value #CANDIDATE_COUNT} 句。
     * 同时把候选句所属诗词 id 记入本局学习沉淀。
     *
     * @return 候选句列表（可能少于 4 句）
     */
    public List<String> buildCandidates() {
        List<String> result = new ArrayList<>();
        String k = keyword.getValue();
        if (k == null) return result;
        List<Poem> all = repo.getAllPoems();
        if (all == null) return result;

        List<String> pool = new ArrayList<>();
        for (Poem p : all) {
            if (p.lines == null) continue;
            for (String line : p.lines) {
                if (line != null && line.contains(k) && com.poetry.domain.PoemUtils.isUsableLine(line)) {
                    String clean = FlyflowerEngine.cleanLineForDisplay(line);
                    if (clean != null && !pool.contains(clean)) {
                        pool.add(clean);
                        if (p.id != null) touchedPoemIds.add(p.id);
                    }
                }
            }
        }

        // 随机取候选句（M9 自适应排等：候选句数量随难度局部调整，Easy=6/Normal=5/Hard=4）
        java.util.Collections.shuffle(pool);
        int candidateCount = DifficultyProfile.flyflowerCandidateCount(getApplication());
        int n = Math.min(candidateCount, pool.size());
        for (int i = 0; i < n; i++) {
            result.add(pool.get(i));
        }
        return result;
    }

    /**
     * 消耗一次「提示一句」机会。
     *
     * @return 是否还有剩余机会（调用前判断）
     */
    public boolean useHint() {
        if (hintRemaining <= 0) return false;
        hintRemaining--;
        return true;
    }

    /** 超时结算当前进度。 */
    private void handleTimeout() {
        if (finishedFlag) return;
        finishedFlag = true;
        timedOut = true;
        ticker.removeCallbacksAndMessages(null);
        finish();
    }

    /** 统一结算：写历史 + 学习沉淀 + 每日统计 + 成就 + 主题。 */
    private void finish() {
        int hits = hitCount.getValue() != null ? hitCount.getValue() : 0;
        finalScore = score.getValue() != null ? score.getValue() : 0;
        finalStars = FlyflowerEngine.calcStars(hits);
        long duration = System.currentTimeMillis() - startTime;

        // M9 自适应排等：本局结束，按命中数/目标命中数记录表现并升降档。
        // 目标命中数取 10（3 星阈值，FlyflowerEngine.calcStars），使正确率语义有意义：
        // 命中 10 句 = 100%，命中 5 句 = 50%。星级用 FlyflowerEngine 计算的 finalStars。
        DifficultyProfile.recordGame(getApplication(), hits, 10, finalStars);

        final int fScore = finalScore;
        final int fStars = finalStars;
        final int fHits = hits;
        final long fDuration = duration;
        final List<String> touched = new ArrayList<>(touchedPoemIds);

        AppExecutors.io(() -> {
            // 1. 写游戏历史
            GameHistory history = new GameHistory(
                    "flyflower", fScore, fStars,
                    fHits, 0,
                    System.currentTimeMillis(), fDuration);
            db.gameHistoryDao().insert(history);

            // 2. 学习沉淀：本局涉及的诗词 id 标记"玩过"
            for (String id : touched) {
                db.learningRecordDao().ensureRecordExists(id);
            }

            // 3. 每日统计（镜像 GameViewModel.recordGameActivity）
            recordGameActivity();

            // 4. 成就检测 + 主题同步
            AchievementEngine.checkAndUnlock(db, def -> newAchievement.postValue(def));
            ThemeManager.syncUnlockedThemes(db);
        });

        finished.setValue(true);
    }

    /** 记录游戏活动到每日统计（镜像 GameViewModel.recordGameActivity）。 */
    private void recordGameActivity() {
        final String today = LocalDate.now().toString();
        AppExecutors.io(() -> {
            DailyStats existing = db.dailyStatsDao().getDailyStatsSync(today);
            if (existing == null) {
                db.dailyStatsDao().upsertDailyStats(new DailyStats(today));
            }
            db.dailyStatsDao().incrementGamesPlayed(today);
        });
    }

    // ==================== Getters ====================

    public LiveData<String> getKeyword() { return keyword; }
    public LiveData<Integer> getRemainingSeconds() { return remainingSeconds; }
    public LiveData<Integer> getHitCount() { return hitCount; }
    public LiveData<Integer> getScore() { return score; }
    public LiveData<Integer> getComboStreak() { return comboStreak; }
    public LiveData<Boolean> getFinished() { return finished; }
    public LiveData<String> getLastHitLine() { return lastHitLine; }
    public LiveData<String> getMissMessage() { return missMessage; }
    public LiveData<AchievementEngine.AchievementDef> getNewAchievement() { return newAchievement; }

    public String getKeywordValue() { return keyword.getValue(); }
    public boolean isLastHitBlessed() { return lastHitBlessed; }
    public int getRemainingSecondsValue() {
        Integer v = remainingSeconds.getValue();
        return v != null ? v : 0;
    }
    public int getHitCountValue() {
        Integer v = hitCount.getValue();
        return v != null ? v : 0;
    }
    public int getScoreValue() {
        Integer v = score.getValue();
        return v != null ? v : 0;
    }
    public int getComboStreakValue() {
        Integer v = comboStreak.getValue();
        return v != null ? v : 0;
    }
    public int getHintRemaining() { return hintRemaining; }
    public String getLastHitLineValue() { return lastHitLine.getValue(); }
    public boolean isFinished() {
        Boolean v = finished.getValue();
        return v != null && v;
    }
    public int getFinalScore() { return finalScore; }
    public int getFinalStars() { return finalStars; }
    public boolean isTimedOut() { return timedOut; }

    /**
     * 本局"最美一句"所属诗词 id（§9.2）。
     *
     * <p>从本局涉及的诗词 id（{@code touchedPoemIds}）中经
     * {@link GameSettlement#pickBestPoemId} 挑选（优先有释义，否则最长句，否则第一首）。
     *
     * @return 选中的诗词 id，无可用时返回 null
     */
    public String getBestPoemId() {
        if (touchedPoemIds.isEmpty()) return null;
        return GameSettlement.pickBestPoemId(new ArrayList<>(touchedPoemIds));
    }

    /** 消费成就事件后清空，防止 LiveData 回放导致重复庆祝。 */
    public void clearAchievement() { newAchievement.setValue(null); }
}
