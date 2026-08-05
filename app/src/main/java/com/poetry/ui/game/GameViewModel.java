package com.poetry.ui.game;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.PoemRepository;
import com.poetry.util.AppExecutors;
import com.poetry.util.DifficultyProfile;
import com.poetry.util.MoonBlessing;
import com.poetry.data.model.Poem;
import com.poetry.domain.AchievementEngine;
import com.poetry.domain.GameEngine;
import com.poetry.domain.GameResult;
import com.poetry.domain.GameSettlement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 游戏 ViewModel（接龙模式 + 消消乐模式 + 积分/成就/主题）
 * <p>
 * 负责管理两种游戏模式的数据和状态：
 * 1. 诗词接龙模式 - 根据给定的诗句选择正确的下一句
 * 2. 消消乐模式 - 配对古诗词的上句和下句
 * </p>
 * <p>
 * 同时处理积分计算、成就解锁和主题同步等通用逻辑。
 * </p>
 */
public class GameViewModel extends AndroidViewModel {

    private PoemRepository repo = PoemRepository.getInstance();
    private LearningDatabase db;

    // 接龙模式
    private MutableLiveData<List<GameEngine.CoupletRound>> coupletRounds = new MutableLiveData<>();
    private MutableLiveData<Integer> currentRound = new MutableLiveData<>(0);
    private MutableLiveData<Integer> coupletScore = new MutableLiveData<>(0);
    private MutableLiveData<Boolean> roundResult = new MutableLiveData<>(null);
    private MutableLiveData<Boolean> coupletFinished = new MutableLiveData<>(false);
    private int coupletStreak = 0;

    // 消消乐模式
    private MutableLiveData<List<GameEngine.MatchCard>> matchCards = new MutableLiveData<>();
    private MutableLiveData<Integer> matchedCount = new MutableLiveData<>(0);
    private MutableLiveData<Integer> matchAttempts = new MutableLiveData<>(0);
    private MutableLiveData<Boolean> matchFinished = new MutableLiveData<>(false);
    private MutableLiveData<String> matchTip = new MutableLiveData<>();  // 配对成功提示（诗词名）
    private GameEngine.MatchGame currentMatchGame;  // 内部持有完整游戏状态

    // M4：消消乐倒计时 / 结算状态
    private MutableLiveData<Integer> matchRemaining = new MutableLiveData<>();  // 剩余秒数（无计时器时为 null/负）
    private final Handler matchTicker = new Handler(Looper.getMainLooper());
    private boolean matchTimedOut = false;          // 是否超时
    private int matchTimeLimitSeconds = 0;          // 本局限时（0=不限时）
    private long matchStartElapsed = 0L;            // 本局开始时的 elapsedRealtime
    private int matchFinalScore = 0;                // 结算得分（供 Fragment 读取）
    private int matchFinalStars = 0;                // 结算星级（供 Fragment 读取）
    private boolean matchFinishedFlag = false;      // 是否已结算（防止重复结算）
    /** 本局种子（消消乐月光祝福判定用，M10 §7.3） */
    private long matchSeed = 0L;
    /** 本局命中月光祝福的累计加成（每命中一对 +30，纯增益） */
    private int matchBlessedBonus = 0;
    /** 最近一次配对是否命中月光祝福（供 UI 🌕 badge，M10） */
    private boolean lastMatchBlessed = false;

    // 🔴 B4 修复：成就解锁通知
    private MutableLiveData<AchievementEngine.AchievementDef> newAchievement = new MutableLiveData<>();

    private static final int MATCH_PAIRS = 6;
    private static final int TOTAL_ROUNDS = 7;

    // 接龙结算累积（M3）
    private GameResult coupletResult;
    private int coupletCorrectCount = 0;
    private long coupletStartTime = 0L;
    private final Set<String> coupletTouchedIds = new HashSet<>();
    /** 最近一轮接龙得分（供 Fragment 飞分展示） */
    private int lastCoupletPoints = 0;
    /** 最近一轮是否命中月光祝福（供 Fragment 🌕 badge 展示，M10） */
    private boolean lastCoupletBlessed = false;

    /**
     * 判断接龙的某一轮（roundIndex）是否命中月光祝福（M10 §7.3）。
     * <p>种子取本局起始时间戳 {@link #coupletStartTime}，确定性判定，重开局不漂移。</p>
     */
    private boolean isCoupletRoundBlessed(int roundIndex) {
        return MoonBlessing.isBlessedRound(coupletStartTime, roundIndex, TOTAL_ROUNDS);
    }

    public GameViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    // ==================== 接龙 ====================

    /**
     * 开始新的诗词接龙游戏。
     * <p>
     * 从诗词库中随机生成指定轮数的接龙题目，
     * 每轮包含一句给定诗句和多个选项（其中一个为正确答案）。
     * </p>
     * <p>
     * 重置所有游戏状态：当前轮次、得分、连击数、回合结果和完成标志。
     * </p>
     */
    public void startCoupletGame() {
        // 游戏题池：著名优先（88 首释义名篇占 80%，普通诗词兜底 20%）
        List<Poem> pool = repo.getGamePool();
        // 当日去重：跳过今日已用过的诗词 id，跨日自动重置
        List<Poem> filtered = filterTodayUsed(pool);
        // M9 自适应排等：难度档位 → 干扰项难度阶段（局部生效，§6.4）
        // 0(EASY)→phase 1 前期简单（长度不同优先）；1(NORMAL)→phase 0 自动按轮次推导；
        // 2(HARD)→phase 2 后期冲刺（等长+语气词）
        int difficultyPhase = mapCoupletPhase(DifficultyProfile.getLevel(getApplication()));
        List<GameEngine.CoupletRound> rounds = GameEngine.generateCoupletGame(filtered, TOTAL_ROUNDS, difficultyPhase);
        // 记录本局实际用到的诗词 id（当日去重写回）
        recordTodayUsed(rounds);
        coupletRounds.setValue(rounds);
        currentRound.setValue(0);
        coupletScore.setValue(0);
        coupletStreak = 0;
        roundResult.setValue(null);
        coupletFinished.setValue(false);
        // 重置结算累积
        coupletResult = new GameResult();
        coupletResult.gameType = "couplet";
        coupletResult.totalCount = rounds.size();
        coupletCorrectCount = 0;
        coupletStartTime = System.currentTimeMillis();
        coupletTouchedIds.clear();
    }

    /**
     * M9：把难度档位（0/1/2）映射为接龙干扰项难度阶段（GameEngine.generateCoupletGame 的
     * difficultyPhase 参数）。0=EASY→1（前期简单，长度不同优先）；1=NORMAL→0（自动按轮次推导）；
     * 2=HARD→2（后期冲刺，等长+语气词）。局部生效，不改核心算法。
     */
    private static int mapCoupletPhase(int level) {
        switch (level) {
            case DifficultyProfile.LEVEL_EASY: return 1;
            case DifficultyProfile.LEVEL_HARD: return 2;
            default: return 0;
        }
    }

    /**
     * 当日去重：从诗词池中过滤掉今日已用过的诗词 id。
     * <p>
     * 使用 SharedPreferences("couplet_dedup") 存储 "date=id1,id2,..."。
     * 日期不同（跨日）时清空重写；同日则跳过已用 id。
     * </p>
     *
     * @param pool 原始诗词池
     * @return 过滤后的诗词池（不含今日已用 id）
     */
    private List<Poem> filterTodayUsed(List<Poem> pool) {
        if (pool == null || pool.isEmpty()) return pool;
        String today = LocalDate.now().toString();
        SharedPreferences prefs = getApplication().getSharedPreferences("couplet_dedup", Context.MODE_PRIVATE);
        String stored = prefs.getString("date_idlist", "");
        Set<String> usedIds = new HashSet<>();
        if (stored != null && !stored.isEmpty()) {
            int eq = stored.indexOf('=');
            if (eq > 0) {
                String date = stored.substring(0, eq);
                if (today.equals(date)) {
                    String ids = stored.substring(eq + 1);
                    if (!ids.isEmpty()) {
                        for (String id : ids.split(",")) {
                            if (!id.isEmpty()) usedIds.add(id);
                        }
                    }
                }
            }
        }
        if (usedIds.isEmpty()) return pool;
        List<Poem> filtered = new ArrayList<>();
        for (Poem p : pool) {
            if (p.id == null || !usedIds.contains(p.id)) {
                filtered.add(p);
            }
        }
        return filtered;
    }

    /**
     * 将本局实际用到的诗词 id 写回当日去重存储（key 含日期，跨日自动重置）。
     */
    private void recordTodayUsed(List<GameEngine.CoupletRound> rounds) {
        if (rounds == null || rounds.isEmpty()) return;
        String today = LocalDate.now().toString();
        SharedPreferences prefs = getApplication().getSharedPreferences("couplet_dedup", Context.MODE_PRIVATE);
        String stored = prefs.getString("date_idlist", "");
        Set<String> usedIds = new HashSet<>();
        if (stored != null && !stored.isEmpty()) {
            int eq = stored.indexOf('=');
            if (eq > 0 && today.equals(stored.substring(0, eq))) {
                String ids = stored.substring(eq + 1);
                if (!ids.isEmpty()) {
                    for (String id : ids.split(",")) {
                        if (!id.isEmpty()) usedIds.add(id);
                    }
                }
            }
        }
        for (GameEngine.CoupletRound r : rounds) {
            if (r.poem != null && r.poem.id != null) usedIds.add(r.poem.id);
        }
        StringBuilder sb = new StringBuilder(today).append('=');
        boolean first = true;
        for (String id : usedIds) {
            if (!first) sb.append(',');
            sb.append(id);
            first = false;
        }
        prefs.edit().putString("date_idlist", sb.toString()).apply();
    }

    /**
     * 提交接龙答题答案并处理结果。
     * <p>
     * 根据选项索引判断答案是否正确，计算得分并更新连击数。
     * 答对时连击数+1，答错时连击数归零。
     * </p>
     * <p>
     * 得分计算规则：基础分 + 连击奖励
     * </p>
     * <p>
     * 如果还有下一轮，自动进入下一轮；否则记录游戏活动并标记游戏完成。
     * </p>
     *
     * @param optionIndex 用户选择的选项索引（从0开始）
     */
    public void answerCouplet(int optionIndex) {
        List<GameEngine.CoupletRound> rounds = coupletRounds.getValue();
        Integer round = currentRound.getValue();
        if (rounds == null || round == null || round >= rounds.size()) return;

        GameEngine.CoupletRound cr = rounds.get(round);
        String selected = cr.options.get(optionIndex);
        boolean correct = selected.equals(cr.correctAnswer);

        if (correct) {
            coupletStreak++;
            coupletCorrectCount++;
        } else {
            coupletStreak = 0;
        }

        // 善意补偿：选中与答案首字相同但非答案本体的干扰项
        boolean pickedSameFirstChar = !correct && isSameFirstCharDistractor(cr, selected);

        int points = GameEngine.calcCoupletScore(round, correct, coupletStreak, pickedSameFirstChar);
        // M10 月光祝福：命中祝福轮且答对 → 该轮基础分 ×2（纯增益，不改公式）
        lastCoupletBlessed = correct && isCoupletRoundBlessed(round);
        points = MoonBlessing.applyDouble(points, lastCoupletBlessed);
        lastCoupletPoints = points;
        coupletScore.setValue((coupletScore.getValue() != null ? coupletScore.getValue() : 0) + points);
        roundResult.setValue(correct);

        // 累积结算数据（积分/成就/历史/每日统计统一在 finishCoupletGame 结束时经 GameSettlement 结算一次）
        if (coupletResult == null) {
            coupletResult = new GameResult();
            coupletResult.gameType = "couplet";
        }
        if (cr.poem != null && cr.poem.id != null) {
            coupletResult.addTouchedPoem(cr.poem.id);
            coupletTouchedIds.add(cr.poem.id);
        }

        if (round + 1 < rounds.size()) {
            currentRound.setValue(round + 1);
        } else {
            finishCoupletGame();
            coupletFinished.setValue(true);
        }
    }

    /**
     * 判断选中项是否为"与答案首字相同但非答案本体"的干扰项。
     */
    private boolean isSameFirstCharDistractor(GameEngine.CoupletRound cr, String selected) {
        if (selected == null || cr.correctAnswer == null) return false;
        String answerFirst = firstChar(cr.correctAnswer);
        if (answerFirst.isEmpty()) return false;
        return firstChar(selected).equals(answerFirst);
    }

    /** 取字符串去掉标点后的首字符。 */
    private static String firstChar(String s) {
        if (s == null) return "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isIdeographic(c)) {
                return String.valueOf(c);
            }
        }
        return "";
    }

    /**
     * 接龙游戏结束时统一结算：经 {@link GameSettlement} 完成积分/等级/每日统计/学习沉淀/
     * 游戏历史/成就/主题同步（取代原 savePoints + recordGameActivity + 内联重复块）。
     */
    private void finishCoupletGame() {
        Integer score = coupletScore.getValue();
        int finalScore = score != null ? score : 0;
        int stars = GameEngine.calcCoupletStars(finalScore);
        int totalCount = coupletResult != null ? coupletResult.totalCount : 0;
        long duration = System.currentTimeMillis() - coupletStartTime;

        if (coupletResult == null) {
            coupletResult = new GameResult();
            coupletResult.gameType = "couplet";
        }
        coupletResult.score = finalScore;
        coupletResult.stars = stars;
        coupletResult.correctCount = coupletCorrectCount;
        coupletResult.totalCount = totalCount;
        coupletResult.durationMillis = duration;
        coupletResult.perfect = totalCount > 0 && coupletCorrectCount == totalCount;

        // M9 自适应排等：本局结束，按答对数/总题数/星级记录表现并升降档（主线程，用 application context）
        DifficultyProfile.recordGame(getApplication(), coupletCorrectCount, totalCount, stars);

        final GameResult result = coupletResult;
        AppExecutors.io(() -> {
            GameSettlement.settle(db, result, def -> newAchievement.postValue(def));
        });
    }

    /**
     * @return 本局接龙结算结果（未完成时为 null）
     */
    public GameResult getCoupletResult() {
        return coupletResult;
    }

    /**
     * @return 最近一轮接龙得分（供 Fragment 飞分展示）
     */
    public int getLastCoupletPoints() {
        return lastCoupletPoints;
    }

    /**
     * @return 最近一轮接龙是否命中月光祝福（供 Fragment 🌕 badge 展示，M10）
     */
    public boolean isLastCoupletBlessed() {
        return lastCoupletBlessed;
    }

    /**
     * 进入下一轮接龙。
     * <p>
     * 清除上一轮的答题结果（正确/错误标记），
     * 使 UI 可以显示新一轮的题目。
     * </p>
     */
    public void nextCoupletRound() {
        roundResult.setValue(null);
    }

    // ==================== 消消乐 ====================

    /**
     * 开始新的消消乐游戏。
     * <p>
     * 从诗词库中生成指定对数的配对卡片（默认6对，共12张）。
     * 每张卡片包含诗句的一部分（上句或下句），需要将对应的上下句配对。
     * </p>
     * <p>
     * 重置所有游戏状态：卡片列表、已配对数量、尝试次数、完成标志和提示信息。
     * </p>
     * <p>
     * 注意：通过创建新的 List 实例来触发 LiveData 的通知机制。
     * </p>
     */
    public void startMatchGame() {
        // 游戏题池：著名优先（88 首释义名篇占 80%，普通诗词兜底 20%）
        List<Poem> pool = repo.getGamePool();
        // M4：当日去重（独立 prefs "match_dedup"，key "date_idlist"）
        List<Poem> filtered = filterTodayUsedMatch(pool);
        currentMatchGame = GameEngine.generateMatchGame(filtered, MATCH_PAIRS);
        // M4：记录本局实际用到的诗词 id（当日去重写回）
        recordTodayUsedMatch(currentMatchGame);
        // LiveData 需要新 List 实例才能触发通知
        matchCards.setValue(new ArrayList<>(currentMatchGame.cards));
        matchedCount.setValue(0);
        matchAttempts.setValue(0);
        matchFinished.setValue(false);
        matchTip.setValue(null);

        // M4：倒计时 —— 首局无计时器（儿童友好），之后每局限时 90s
        matchTicker.removeCallbacksAndMessages(null);
        matchTimedOut = false;
        matchFinishedFlag = false;
        matchFinalScore = 0;
        matchFinalStars = 0;
        // M10 月光祝福：本局种子 = 开局时间戳；重置加成与最近命中标记
        matchSeed = System.currentTimeMillis();
        matchBlessedBonus = 0;
        lastMatchBlessed = false;
        int played = getMatchPlayedCount();
        // M9 自适应排等：首局无计时（儿童友好），次局起默认 90s，HARD 档缩短为 75s（§9.1）
        matchTimeLimitSeconds = played > 0 ? DifficultyProfile.matchTimeLimitSeconds(getApplication()) : 0;
        matchStartElapsed = SystemClock.elapsedRealtime();
        if (matchTimeLimitSeconds > 0) {
            matchRemaining.setValue(matchTimeLimitSeconds);
            startMatchTicker();
        } else {
            matchRemaining.setValue(null);
        }
    }

    /**
     * M4：当日去重（消消乐专用，独立 prefs "match_dedup"）。
     * 从诗词池中过滤掉今日已用过的诗词 id，跨日自动重置。
     */
    private List<Poem> filterTodayUsedMatch(List<Poem> pool) {
        if (pool == null || pool.isEmpty()) return pool;
        String today = LocalDate.now().toString();
        SharedPreferences prefs = getApplication().getSharedPreferences("match_dedup", Context.MODE_PRIVATE);
        String stored = prefs.getString("date_idlist", "");
        Set<String> usedIds = new HashSet<>();
        if (stored != null && !stored.isEmpty()) {
            int eq = stored.indexOf('=');
            if (eq > 0) {
                String date = stored.substring(0, eq);
                if (today.equals(date)) {
                    String ids = stored.substring(eq + 1);
                    if (!ids.isEmpty()) {
                        for (String id : ids.split(",")) {
                            if (!id.isEmpty()) usedIds.add(id);
                        }
                    }
                }
            }
        }
        if (usedIds.isEmpty()) return pool;
        List<Poem> filtered = new ArrayList<>();
        for (Poem p : pool) {
            if (p.id == null || !usedIds.contains(p.id)) {
                filtered.add(p);
            }
        }
        return filtered;
    }

    /**
     * M4：将本局实际用到的诗词 id 写回当日去重存储（key 含日期，跨日自动重置）。
     */
    private void recordTodayUsedMatch(GameEngine.MatchGame game) {
        if (game == null || game.cards == null || game.cards.isEmpty()) return;
        String today = LocalDate.now().toString();
        SharedPreferences prefs = getApplication().getSharedPreferences("match_dedup", Context.MODE_PRIVATE);
        String stored = prefs.getString("date_idlist", "");
        Set<String> usedIds = new HashSet<>();
        if (stored != null && !stored.isEmpty()) {
            int eq = stored.indexOf('=');
            if (eq > 0 && today.equals(stored.substring(0, eq))) {
                String ids = stored.substring(eq + 1);
                if (!ids.isEmpty()) {
                    for (String id : ids.split(",")) {
                        if (!id.isEmpty()) usedIds.add(id);
                    }
                }
            }
        }
        for (GameEngine.MatchCard c : game.cards) {
            if (c.poemTitle != null && !c.poemTitle.isEmpty()) {
                // 用标题作为去重键（MatchCard 无 poemId，标题可唯一标识）
                usedIds.add(c.poemTitle);
            }
        }
        StringBuilder sb = new StringBuilder(today).append('=');
        boolean first = true;
        for (String id : usedIds) {
            if (!first) sb.append(',');
            sb.append(id);
            first = false;
        }
        prefs.edit().putString("date_idlist", sb.toString()).apply();
    }

    /** M4：读取消消乐已玩局数（prefs "match_state" key "played_count"）。 */
    private int getMatchPlayedCount() {
        SharedPreferences prefs = getApplication().getSharedPreferences("match_state", Context.MODE_PRIVATE);
        return prefs.getInt("played_count", 0);
    }

    /** M4：消消乐一局结束（完成或超时）时递增已玩局数。 */
    private void incrementMatchPlayedCount() {
        SharedPreferences prefs = getApplication().getSharedPreferences("match_state", Context.MODE_PRIVATE);
        int count = prefs.getInt("played_count", 0);
        prefs.edit().putInt("played_count", count + 1).apply();
    }

    /** M4：启动 1 秒倒计时 ticker。 */
    private void startMatchTicker() {
        matchTicker.removeCallbacksAndMessages(null);
        matchTicker.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (matchFinishedFlag) return; // 已结算，停止
                long elapsed = SystemClock.elapsedRealtime() - matchStartElapsed;
                int remaining = matchTimeLimitSeconds - (int) (elapsed / 1000L);
                if (remaining <= 0) {
                    matchRemaining.setValue(0);
                    handleMatchTimeout();
                    return;
                }
                matchRemaining.setValue(remaining);
                matchTicker.postDelayed(this, 1000L);
            }
        }, 1000L);
    }

    /** M4：超时结算当前进度。 */
    private void handleMatchTimeout() {
        if (matchFinishedFlag) return; // 已结算，忽略
        matchFinishedFlag = true;
        matchTimedOut = true;
        matchTicker.removeCallbacksAndMessages(null);

        int attempts = matchAttempts.getValue() != null ? matchAttempts.getValue() : 0;
        int matched = matchedCount.getValue() != null ? matchedCount.getValue() : 0;
        int score = GameEngine.calcMatchScore(attempts, MATCH_PAIRS, matched,
                matchTimeLimitSeconds, matchTimeLimitSeconds, false);
        // M10 月光祝福：叠加纯增益加成（结算兜底，超时也生效）
        score += matchBlessedBonus;
        int stars = GameEngine.calcMatchStars(attempts, MATCH_PAIRS, false, matched);
        matchFinalScore = score;
        matchFinalStars = stars;
        settleMatch(score, stars, matched, false);
        incrementMatchPlayedCount();
        matchFinished.setValue(true);
    }

    /**
     * 尝试配对两张卡片。
     * <p>
     * 判断两张卡片是否匹配（是否为同一首诗的上句和下句）。
     * </p>
     * <p>
     * 返回值和处理的逻辑：
     * - 返回 0：配对成功，标记两张卡片为已匹配，更新已配对数量，
     *   显示诗词信息提示，检查游戏是否完成
     * - 返回 1：配对失败，取消两张卡片的选中状态
     * - 返回 -1：无效操作（同一张卡片或卡片已匹配）
     * </p>
     *
     * @param a 第一张选中的卡片
     * @param b 第二张选中的卡片
     * @return 0=配对成功, 1=配对失败, -1=无效操作
     */
    public int tryMatch(GameEngine.MatchCard a, GameEngine.MatchCard b) {
        if (currentMatchGame == null || a == b) return -1;
        if (a.matched || b.matched) return -1;

        matchAttempts.setValue((matchAttempts.getValue() != null ? matchAttempts.getValue() : 0) + 1);

        boolean success = GameEngine.checkMatch(a, b);

        if (success) {
            a.matched = true;
            b.matched = true;
            a.selected = false;
            b.selected = false;

            int newCount = (matchedCount.getValue() != null ? matchedCount.getValue() : 0) + 1;
            matchedCount.setValue(newCount);

            // M10 月光祝福：第 newCount 次成功配对命中祝福 → 每对 +30（纯增益，≤2次/局）
            lastMatchBlessed = MoonBlessing.isBlessedRound(matchSeed, newCount - 1, MATCH_PAIRS);
            if (lastMatchBlessed) {
                matchBlessedBonus += 30;
            }

            // 提示诗词名
            String info = currentMatchGame.poemInfo != null
                    ? currentMatchGame.poemInfo.get(a.pairId) : null;
            matchTip.setValue(info != null ? info : (a.poemTitle + " · " + a.poemAuthor));

            // 通知 UI 更新（新 List 实例）
            matchCards.setValue(new ArrayList<>(currentMatchGame.cards));

            if (GameEngine.isGameComplete(currentMatchGame)) {
                // M4：完成结算 —— 新评分公式 + 星级 + 停止计时器 + 递增局数
                matchFinishedFlag = true;
                matchTicker.removeCallbacksAndMessages(null);
                int attempts = matchAttempts.getValue() != null ? matchAttempts.getValue() : 0;
                long usedSeconds = (SystemClock.elapsedRealtime() - matchStartElapsed) / 1000L;
                int score = GameEngine.calcMatchScore(attempts, MATCH_PAIRS, newCount,
                        usedSeconds, matchTimeLimitSeconds, true);
                // M10 月光祝福：叠加纯增益加成
                score += matchBlessedBonus;
                int stars = GameEngine.calcMatchStars(attempts, MATCH_PAIRS, true, newCount);
                matchFinalScore = score;
                matchFinalStars = stars;
                settleMatch(score, stars, newCount, true);
                incrementMatchPlayedCount();
                matchFinished.setValue(true);
            }
            return 0;
        } else {
            // 配对失败，取消选中
            a.selected = false;
            b.selected = false;
            matchCards.setValue(new ArrayList<>(currentMatchGame.cards));
            return 1;
        }
    }

    /**
     * 选中或取消选中卡片。
     * <p>
     * 切换卡片的选中状态。如果卡片已匹配，则忽略操作。
     * 选中状态改变后会通知 UI 更新卡片显示。
     * </p>
     *
     * @param card 要切换选中状态的卡片
     */
    public void toggleSelect(GameEngine.MatchCard card) {
        if (card.matched) return;
        card.selected = !card.selected;
        matchCards.setValue(new ArrayList<>(currentMatchGame.cards));
    }

    /**
     * @return 最近一次配对是否命中月光祝福（供 UI 🌕 badge，M10）
     */
    public boolean isLastMatchBlessed() {
        return lastMatchBlessed;
    }

    // ==================== 通用 ====================

    /**
     * 消消乐一局结束（完成或超时）时统一结算：构造 {@link GameResult} 并交给
     * {@link GameSettlement} 完成积分/等级/每日统计/学习沉淀/游戏历史/成就/主题同步。
     *
     * @param score   本局得分
     * @param stars   本局星级
     * @param matched 已配对对数（作为答对数）
     * @param perfect 是否全对（全部配对成功）
     */
    private void settleMatch(int score, int stars, int matched, boolean perfect) {
        GameResult result = new GameResult();
        result.gameType = "match";
        result.score = score;
        result.stars = stars;
        result.correctCount = matched;
        result.totalCount = MATCH_PAIRS;
        result.perfect = perfect;
        result.durationMillis = System.currentTimeMillis() - matchStartElapsed;
        // 学习沉淀：本局涉及的诗词 id（由卡片标题/作者解析）
        for (String id : resolveMatchPoemIds()) {
            result.addTouchedPoem(id);
        }
        // M9 自适应排等：本局结束，按配对对数/总对数/星级记录表现并升降档（主线程，用 application context）
        DifficultyProfile.recordGame(getApplication(), matched, MATCH_PAIRS, stars);
        final GameResult finalResult = result;
        AppExecutors.io(() -> {
            GameSettlement.settle(db, finalResult, def -> newAchievement.postValue(def));
        });
    }

    /**
     * 解析消消乐本局涉及的诗词 id。
     *
     * <p>MatchCard 仅携带标题/作者（无 poemId），故通过标题+作者在诗词库中反查 id。
     * 标题+作者可能重复，这里按"标题+作者"去重后返回首个匹配的 id。
     */
    private List<String> resolveMatchPoemIds() {
        List<String> ids = new ArrayList<>();
        if (currentMatchGame == null || currentMatchGame.cards == null) return ids;
        List<Poem> all = repo.getAllPoems();
        if (all == null || all.isEmpty()) return ids;

        // 构建 title|author -> id 映射（一次遍历，避免逐卡 O(n)）
        java.util.Map<String, String> byTitleAuthor = new java.util.HashMap<>();
        for (Poem p : all) {
            if (p == null || p.id == null) continue;
            String key = (p.title == null ? "" : p.title) + "\u0000" + (p.author == null ? "" : p.author);
            if (!byTitleAuthor.containsKey(key)) {
                byTitleAuthor.put(key, p.id);
            }
        }

        java.util.Set<String> seen = new HashSet<>();
        for (GameEngine.MatchCard c : currentMatchGame.cards) {
            if (c == null) continue;
            String key = (c.poemTitle == null ? "" : c.poemTitle) + "\u0000" + (c.poemAuthor == null ? "" : c.poemAuthor);
            String id = byTitleAuthor.get(key);
            if (id != null && seen.add(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * 本局"最美一句"所属诗词 id（§9.2）。
     *
     * <p>接龙取 {@code coupletResult} 涉及的诗词 id，消消乐取本局卡片解析出的诗词 id，
     * 统一经 {@link GameSettlement#pickBestPoemId} 挑选（优先有释义，否则最长句，否则第一首）。
     *
     * @return 选中的诗词 id，无可用时返回 null
     */
    public String getBestPoemId() {
        if (coupletResult != null && !coupletResult.getTouchedPoemIds().isEmpty()) {
            return GameSettlement.pickBestPoemId(coupletResult.getTouchedPoemIds());
        }
        List<String> matchIds = resolveMatchPoemIds();
        if (!matchIds.isEmpty()) {
            return GameSettlement.pickBestPoemId(matchIds);
        }
        return null;
    }

    // ==================== Getters ====================

    /**
     * @return 接龙游戏的所有回合数据
     */
    public LiveData<List<GameEngine.CoupletRound>> getCoupletRounds() { return coupletRounds; }

    /**
     * @return 当前回合的索引（从0开始）
     */
    public LiveData<Integer> getCurrentRound() { return currentRound; }

    /**
     * @return 接龙游戏的当前得分
     */
    public LiveData<Integer> getCoupletScore() { return coupletScore; }

    /**
     * @return 当前回合的答题结果（null=未答题, true=正确, false=错误）
     */
    public LiveData<Boolean> getRoundResult() { return roundResult; }

    /**
     * @return 接龙游戏是否已完成
     */
    public LiveData<Boolean> getCoupletFinished() { return coupletFinished; }

    /**
     * @return 消消乐游戏的所有卡片数据
     */
    public LiveData<List<GameEngine.MatchCard>> getMatchCards() { return matchCards; }

    /**
     * @return 已成功配对的卡片对数
     */
    public LiveData<Integer> getMatchedCount() { return matchedCount; }

    /**
     * @return 消消乐游戏的尝试次数
     */
    public LiveData<Integer> getMatchAttempts() { return matchAttempts; }

    /**
     * @return 消消乐游戏是否已完成
     */
    public LiveData<Boolean> getMatchFinished() { return matchFinished; }

    /**
     * @return 最近一次成功配对的诗词信息提示
     */
    public LiveData<String> getMatchTip() { return matchTip; }

    /**
     * @return 消消乐剩余秒数（无计时器时为 null）
     */
    public LiveData<Integer> getMatchRemaining() { return matchRemaining; }

    /**
     * @return 消消乐本局限时秒数（0=不限时）
     */
    public int getMatchTimeLimitSeconds() { return matchTimeLimitSeconds; }

    /**
     * @return 消消乐本局是否超时
     */
    public boolean isMatchTimedOut() { return matchTimedOut; }

    /**
     * @return 消消乐结算得分（未结算时为 0）
     */
    public int getMatchFinalScore() { return matchFinalScore; }

    /**
     * @return 消消乐结算星级（未结算时为 0）
     */
    public int getMatchFinalStars() { return matchFinalStars; }

    /** 🔴 B4 修复：成就解锁事件 */
    public LiveData<AchievementEngine.AchievementDef> getNewAchievement() { return newAchievement; }

    /** 消费成就事件后清空，防止 LiveData 回放导致重复庆祝 */
    public void clearAchievement() { newAchievement.setValue(null); }

    /**
     * @return 接龙游戏的总回合数（实际生成数，当日去重后可能少于常量）
     */
    public int getTotalRounds() {
        List<GameEngine.CoupletRound> rounds = coupletRounds.getValue();
        if (rounds != null && !rounds.isEmpty()) return rounds.size();
        return TOTAL_ROUNDS;
    }

    /**
     * @return 消消乐游戏的配对总数（对数）
     */
    public int getMatchPairs() { return MATCH_PAIRS; }
}
