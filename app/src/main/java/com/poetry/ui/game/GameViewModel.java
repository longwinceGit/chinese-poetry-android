package com.poetry.ui.game;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.PoemRepository;
import com.poetry.data.UserProfile;
import com.poetry.data.model.Poem;
import com.poetry.domain.GameEngine;
import com.poetry.domain.LearningEngine;

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

    // 配对模式
    private MutableLiveData<GameEngine.MatchGame> matchGame = new MutableLiveData<>();
    private MutableLiveData<Integer> matchedCount = new MutableLiveData<>(0);
    private MutableLiveData<Integer> matchAttempts = new MutableLiveData<>(0);
    private MutableLiveData<Boolean> matchFinished = new MutableLiveData<>(false);

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

        // 保存积分
        savePoints(points);

        // 自动进入下一题（延迟）
        if (round + 1 < rounds.size()) {
            currentRound.setValue(round + 1);
        } else {
            coupletFinished.setValue(true);
        }
    }

    public void nextCoupletRound() {
        roundResult.setValue(null);
    }

    // ==================== 配对 ====================

    public void startMatchGame() {
        List<Poem> pool = repo.getAllPoems();
        GameEngine.MatchGame game = GameEngine.generateMatchGame(pool, 6);
        matchGame.setValue(game);
        matchedCount.setValue(0);
        matchAttempts.setValue(0);
        matchFinished.setValue(false);
    }

    public boolean tryMatch(GameEngine.MatchCard a, GameEngine.MatchCard b) {
        GameEngine.MatchGame game = matchGame.getValue();
        if (game == null) return false;

        matchAttempts.setValue((matchAttempts.getValue() != null ? matchAttempts.getValue() : 0) + 1);
        boolean success = GameEngine.checkMatch(game, a, b);

        if (success) {
            matchedCount.setValue((matchedCount.getValue() != null ? matchedCount.getValue() : 0) + 1);
            matchGame.setValue(game);

            if (GameEngine.isGameComplete(game)) {
                int score = GameEngine.calcMatchScore(matchAttempts.getValue() != null ? matchAttempts.getValue() : 0);
                savePoints(score);
                matchFinished.setValue(true);
            }
        }
        return success;
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

    public LiveData<List<GameEngine.CoupletRound>> getCoupletRounds() { return coupletRounds; }
    public LiveData<Integer> getCurrentRound() { return currentRound; }
    public LiveData<Integer> getCoupletScore() { return coupletScore; }
    public LiveData<Boolean> getRoundResult() { return roundResult; }
    public LiveData<Boolean> getCoupletFinished() { return coupletFinished; }

    public LiveData<GameEngine.MatchGame> getMatchGame() { return matchGame; }
    public LiveData<Integer> getMatchedCount() { return matchedCount; }
    public LiveData<Integer> getMatchAttempts() { return matchAttempts; }
    public LiveData<Boolean> getMatchFinished() { return matchFinished; }

    public int getTotalRounds() { return TOTAL_ROUNDS; }
}
