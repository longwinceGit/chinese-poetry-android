package com.poetry.ui.game;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.DailyStats;
import com.poetry.data.LearningDatabase;
import com.poetry.data.PoemRepository;
import com.poetry.data.UserProfile;
import com.poetry.data.model.Poem;
import com.poetry.domain.AchievementEngine;
import com.poetry.domain.GameEngine;
import com.poetry.domain.LearningEngine;
import com.poetry.domain.ThemeManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    // 🔴 B4 修复：成就解锁通知
    private MutableLiveData<AchievementEngine.AchievementDef> newAchievement = new MutableLiveData<>();

    private static final int MATCH_PAIRS = 6;
    private static final int TOTAL_ROUNDS = 10;

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
        List<Poem> pool = repo.getAllPoems();
        List<GameEngine.CoupletRound> rounds = GameEngine.generateCoupletGame(pool, TOTAL_ROUNDS);
        coupletRounds.setValue(rounds);
        currentRound.setValue(0);
        coupletScore.setValue(0);
        coupletStreak = 0;
        roundResult.setValue(null);
        coupletFinished.setValue(false);
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
        boolean correct = cr.options.get(optionIndex).equals(cr.correctAnswer);

        if (correct) {
            coupletStreak++;
        } else {
            coupletStreak = 0;
        }

        int points = GameEngine.calcCoupletScore(round, correct, coupletStreak);
        coupletScore.setValue((coupletScore.getValue() != null ? coupletScore.getValue() : 0) + points);
        roundResult.setValue(correct);

        savePoints(points);

        if (round + 1 < rounds.size()) {
            currentRound.setValue(round + 1);
        } else {
            recordGameActivity();
            coupletFinished.setValue(true);
        }
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
        List<Poem> pool = repo.getAllPoems();
        currentMatchGame = GameEngine.generateMatchGame(pool, MATCH_PAIRS);
        // LiveData 需要新 List 实例才能触发通知
        matchCards.setValue(new ArrayList<>(currentMatchGame.cards));
        matchedCount.setValue(0);
        matchAttempts.setValue(0);
        matchFinished.setValue(false);
        matchTip.setValue(null);
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

            // 提示诗词名
            String info = currentMatchGame.poemInfo != null
                    ? currentMatchGame.poemInfo.get(a.pairId) : null;
            matchTip.setValue(info != null ? info : (a.poemTitle + " · " + a.poemAuthor));

            // 通知 UI 更新（新 List 实例）
            matchCards.setValue(new ArrayList<>(currentMatchGame.cards));

            if (GameEngine.isGameComplete(currentMatchGame)) {
                int score = GameEngine.calcMatchScore(
                        matchAttempts.getValue() != null ? matchAttempts.getValue() : 0,
                        MATCH_PAIRS);
                savePoints(score);
                recordGameActivity();
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

    // ==================== 通用 ====================

    /**
     * 保存积分并更新等级和成就。
     * <p>
     * 在后台线程中执行以下操作：
     * 1. 使用原子 SQL 增量更新总积分（避免竞态条件）
     * 2. 重新读取最新积分并计算等级，如果等级提升则更新
     * 3. 检测并解锁新成就
     * 4. 同步主题解锁状态
     * </p>
     *
     * @param points 要添加的积分数量
     */
    private void savePoints(int points) {
        new Thread(() -> {
            // 🔴 B2 修复：使用原子 SQL 增量，避免读-改-写竞态
            db.poemDao().addTotalPoints(points);
            // 重新读取最新积分以计算等级
            UserProfile profile = db.poemDao().getUserProfileSync();
            if (profile != null) {
                int newLevel = LearningEngine.calcLevel(profile.totalPoints);
                if (newLevel != profile.level) {
                    db.poemDao().updateLevel(newLevel);
                }
            }
            // 🔴 B4 修复：每次积分变更后检测成就
            AchievementEngine.checkAndUnlock(db, def -> {
                newAchievement.postValue(def);
            });
            // 🔴 B5 修复：等级提升后同步主题解锁
            ThemeManager.syncUnlockedThemes(db);
        }).start();
    }

    /**
     * 记录游戏活动到每日统计。
     * <p>
     * 在后台线程中执行以下操作：
     * 1. 检查今天的统计记录是否存在，不存在则创建
     * 2. 增加今天的游戏次数计数
     * </p>
     * <p>
     * 用于追踪用户的每日游戏活跃度。
     * </p>
     */
    private void recordGameActivity() {
        final String today = LocalDate.now().toString();
        new Thread(() -> {
            DailyStats existing = db.poemDao().getDailyStatsSync(today);
            if (existing == null) {
                db.poemDao().upsertDailyStats(new DailyStats(today));
            }
            db.poemDao().incrementGamesPlayed(today);
        }).start();
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

    /** 🔴 B4 修复：成就解锁事件 */
    public LiveData<AchievementEngine.AchievementDef> getNewAchievement() { return newAchievement; }

    /** 消费成就事件后清空，防止 LiveData 回放导致重复庆祝 */
    public void clearAchievement() { newAchievement.setValue(null); }

    /**
     * @return 接龙游戏的总回合数
     */
    public int getTotalRounds() { return TOTAL_ROUNDS; }

    /**
     * @return 消消乐游戏的配对总数（对数）
     */
    public int getMatchPairs() { return MATCH_PAIRS; }
}
