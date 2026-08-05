package com.poetry.ui.game;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.PoemRepository;
import com.poetry.data.model.Poem;
import com.poetry.domain.AchievementEngine;
import com.poetry.domain.AuthorMatchEngine;
import com.poetry.domain.GameResult;
import com.poetry.domain.GameSettlement;
import com.poetry.util.AppExecutors;
import com.poetry.util.DifficultyProfile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 作者连线（Author Match）ViewModel —— M8（§4.5）。
 *
 * <p>管理作者连线游戏的完整生命周期：随机出题 → 显示题目 → 点选作者连线 →
 * 判定得分 → 下一题 → 结算。误连可一键撤销（{@link #undoLast()}），
 * 撤销后重答仍计入连击（零挫败、无锁定惩罚，§8.1）。</p>
 *
 * <p>结算统一经 {@link GameSettlement}（gameType="author"），完成积分/等级/
 * 每日统计/学习沉淀/游戏历史/成就/主题同步。</p>
 */
public class AuthorMatchViewModel extends AndroidViewModel {

    private final PoemRepository repo = PoemRepository.getInstance();
    private final LearningDatabase db;

    private final MutableLiveData<List<AuthorMatchEngine.AuthorRound>> rounds = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentRound = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> score = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> roundResult = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> finished = new MutableLiveData<>(false);
    private final MutableLiveData<AchievementEngine.AchievementDef> newAchievement = new MutableLiveData<>();

    private int streak = 0;
    private int correctCount = 0;
    private long startTime = 0L;
    private final Set<String> touchedIds = new HashSet<>();
    /** 最近一轮得分（供 Fragment 飞分展示） */
    private int lastPoints = 0;
    /** 最近一轮答对的是哪位作者（供 Fragment 反馈文案） */
    private String lastCorrectAuthor = "";
    /** 本局是否已结算（防止重复结算） */
    private boolean settled = false;

    public AuthorMatchViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    /**
     * 开始新的作者连线游戏。
     * <p>从诗词库生成 {@value AuthorMatchEngine#DEFAULT_ROUNDS} 题，
     * 重置分数、连击、回合结果与完成标志，记录开局时间戳。</p>
     */
    public void startGame() {
        List<Poem> pool = repo.getAllPoems();
        List<AuthorMatchEngine.AuthorRound> game =
                AuthorMatchEngine.generateGame(pool, AuthorMatchEngine.DEFAULT_ROUNDS);
        rounds.setValue(game);
        currentRound.setValue(0);
        score.setValue(0);
        streak = 0;
        correctCount = 0;
        roundResult.setValue(null);
        finished.setValue(false);
        settled = false;
        touchedIds.clear();
        startTime = System.currentTimeMillis();
    }

    /**
     * 提交当前题的连线答案（点选一位候选作者）。
     *
     * @param optionIndex 用户点选的作者选项索引
     * @return 是否成功提交（false = 题目不可用或已作答）
     */
    public boolean answerAuthor(int optionIndex) {
        List<AuthorMatchEngine.AuthorRound> list = rounds.getValue();
        Integer round = currentRound.getValue();
        if (list == null || round == null || round >= list.size()) return false;
        // 已作答（含撤销中）不允许重复提交
        if (roundResult.getValue() != null) return false;

        AuthorMatchEngine.AuthorRound r = list.get(round);
        if (optionIndex < 0 || optionIndex >= r.options.size()) return false;

        boolean correct = r.options.get(optionIndex).correct;
        lastCorrectAuthor = r.correctAuthor;

        // 学习沉淀：本局涉及的诗词 id
        if (r.poem != null && r.poem.id != null) {
            touchedIds.add(r.poem.id);
        }

        int points;
        if (correct) {
            streak++;
            correctCount++;
            points = AuthorMatchEngine.calcScore(true, streak);
        } else {
            streak = 0;
            points = AuthorMatchEngine.calcScore(false, 0);
        }
        lastPoints = points;
        score.setValue((score.getValue() != null ? score.getValue() : 0) + points);
        roundResult.setValue(correct);
        return true;
    }

    /**
     * 撤销误连：回到当前题待选状态（§8.1 无锁定惩罚）。
     * <p>仅允许撤销 <b>答错</b> 的连线；答对的连线不撤销（保护连击）。
     * 撤销后重答，答对仍按当前连击计分。</p>
     *
     * @return 是否成功撤销
     */
    public boolean undoLast() {
        Boolean result = roundResult.getValue();
        if (result == null || result) return false; // 未作答或答对，不可撤销
        // 答错时 streak 已被清零；撤销后回到待选，重答可重新建立连击
        roundResult.setValue(null);
        return true;
    }

    /**
     * 进入下一题。若已到最后一题，则触发结算。
     */
    public void nextRound() {
        Integer round = currentRound.getValue();
        List<AuthorMatchEngine.AuthorRound> list = rounds.getValue();
        if (round == null || list == null) return;
        roundResult.setValue(null);
        if (round + 1 < list.size()) {
            currentRound.setValue(round + 1);
        } else {
            finishGame();
        }
    }

    /**
     * 整局结束统一结算：构造 {@link GameResult} 交给 {@link GameSettlement}
     * 完成积分/等级/每日统计/学习沉淀/游戏历史/成就/主题同步，
     * 并按 M9 自适应排等记录本局表现（主线程）。
     */
    private void finishGame() {
        if (settled) return;
        settled = true;
        // 标记完成：触发 Fragment 观察者导航到结算页（此标志此前从未置 true，
        // 导致最后一题作答后无任何结算展示 —— 玩家看到的"无后续"即此原因）
        finished.setValue(true);
        // 标记全部完成 → Fragment 据此导航到结算页（此前该标志从未置 true，导致最后一题后无响应）
        finished.setValue(true);

        Integer sc = score.getValue();
        int finalScore = sc != null ? sc : 0;
        int stars = AuthorMatchEngine.calcStars(finalScore);
        int total = rounds.getValue() != null ? rounds.getValue().size() : 0;

        GameResult result = new GameResult();
        result.gameType = "author";
        result.score = finalScore;
        result.stars = stars;
        result.correctCount = correctCount;
        result.totalCount = total;
        result.perfect = total > 0 && correctCount == total;
        result.durationMillis = System.currentTimeMillis() - startTime;
        for (String id : touchedIds) {
            result.addTouchedPoem(id);
        }

        // M9 自适应排等：按答对数/总题数/星级记录表现并升降档
        DifficultyProfile.recordGame(getApplication(), correctCount, total, stars);

        final GameResult finalResult = result;
        AppExecutors.io(() -> {
            GameSettlement.settle(db, finalResult, def -> newAchievement.postValue(def));
        });
    }

    // ==================== Getters ====================

    /** @return 本局所有题目 */
    public LiveData<List<AuthorMatchEngine.AuthorRound>> getRounds() { return rounds; }

    /** @return 当前题索引（0-based） */
    public LiveData<Integer> getCurrentRound() { return currentRound; }

    /** @return 当前得分 */
    public LiveData<Integer> getScore() { return score; }

    /** @return 最近一次连线结果（null=未作答/已撤销，true=答对，false=答错） */
    public LiveData<Boolean> getRoundResult() { return roundResult; }

    /** @return 是否已全部完成 */
    public LiveData<Boolean> getFinished() { return finished; }

    /** @return 成就解锁事件（B4） */
    public LiveData<AchievementEngine.AchievementDef> getNewAchievement() { return newAchievement; }

    /** 消费成就事件后清空，防止 LiveData 回放导致重复庆祝 */
    public void clearAchievement() { newAchievement.setValue(null); }

    /** @return 最近一轮得分（供 Fragment 飞分展示） */
    public int getLastPoints() { return lastPoints; }

    /** @return 最近一轮答对/答错涉及的正确作者（供反馈文案） */
    public String getLastCorrectAuthor() { return lastCorrectAuthor; }

    /** @return 本局题数（实际生成数） */
    public int getTotalRounds() {
        List<AuthorMatchEngine.AuthorRound> list = rounds.getValue();
        return list != null ? list.size() : AuthorMatchEngine.DEFAULT_ROUNDS;
    }

    /**
     * 本局"最美一句"所属诗词 id（§9.2）。
     * <p>统一经 {@link GameSettlement#pickBestPoemId} 挑选（优先有释义，否则最长句）。</p>
     *
     * @return 选中的诗词 id，无可用时返回 null
     */
    public String getLastPoemIdForSettlement() {
        if (touchedIds.isEmpty()) return null;
        return GameSettlement.pickBestPoemId(new java.util.ArrayList<>(touchedIds));
    }
}
