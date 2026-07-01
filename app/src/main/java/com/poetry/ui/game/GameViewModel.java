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
import com.poetry.domain.GameEngine;
import com.poetry.domain.LearningEngine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    private static final int MATCH_PAIRS = 6;
    private static final int TOTAL_ROUNDS = 10;

    public GameViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    // ==================== 接龙 ====================

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

    public void nextCoupletRound() {
        roundResult.setValue(null);
    }

    // ==================== 消消乐 ====================

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

    /** 选中/取消选中卡片 */
    public void toggleSelect(GameEngine.MatchCard card) {
        if (card.matched) return;
        card.selected = !card.selected;
        matchCards.setValue(new ArrayList<>(currentMatchGame.cards));
    }

    // ==================== 通用 ====================

    private void savePoints(int points) {
        new Thread(() -> {
            UserProfile profile = db.poemDao().getUserProfileSync();
            if (profile == null) {
                profile = new UserProfile();
            }
            profile.totalPoints += points;
            profile.level = LearningEngine.calcLevel(profile.totalPoints);
            db.poemDao().insertUserProfile(profile);
        }).start();
    }

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

    public LiveData<List<GameEngine.CoupletRound>> getCoupletRounds() { return coupletRounds; }
    public LiveData<Integer> getCurrentRound() { return currentRound; }
    public LiveData<Integer> getCoupletScore() { return coupletScore; }
    public LiveData<Boolean> getRoundResult() { return roundResult; }
    public LiveData<Boolean> getCoupletFinished() { return coupletFinished; }

    public LiveData<List<GameEngine.MatchCard>> getMatchCards() { return matchCards; }
    public LiveData<Integer> getMatchedCount() { return matchedCount; }
    public LiveData<Integer> getMatchAttempts() { return matchAttempts; }
    public LiveData<Boolean> getMatchFinished() { return matchFinished; }
    public LiveData<String> getMatchTip() { return matchTip; }

    public int getTotalRounds() { return TOTAL_ROUNDS; }
    public int getMatchPairs() { return MATCH_PAIRS; }
}
