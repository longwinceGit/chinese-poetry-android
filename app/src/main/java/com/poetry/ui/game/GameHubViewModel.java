package com.poetry.ui.game;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.GameHistory;
import com.poetry.data.LearningDatabase;
import com.poetry.util.AppExecutors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 游戏大厅 ViewModel —— 为「今日挑战 + 成绩回廊」提供卡片数据。
 *
 * <p>每张游戏卡聚合：历史最高星、最高分、今日是否玩过、是否为今日挑战。
 * 今日挑战按 {@code LocalDate.now().getDayOfYear() % 3} 简单轮转（0=对诗,1=消消乐,2=填空），
 * 纯内存判定，不做持久化。
 *
 * <p>历史最高星/分查询是同步 DAO 方法，必须在 {@link AppExecutors#io()} 线程执行，
 * 完成后通过 {@code postValue} 回主线程通知 UI。
 */
public class GameHubViewModel extends AndroidViewModel {

    /** 游戏类型常量（与 game_history 表 gameType 字段一致）。 */
    public static final String TYPE_COUPLET = "couplet";
    public static final String TYPE_MATCH = "match";
    public static final String TYPE_QUIZ = "quiz";
    public static final String TYPE_FLYFLOWER = "flyflower";
    /** M8 作者连线 */
    public static final String TYPE_AUTHOR = "author";

    private final LearningDatabase db;

    private final MutableLiveData<List<GameHubCard>> cards = new MutableLiveData<>();
    private final MutableLiveData<String> recall = new MutableLiveData<>();

    public GameHubViewModel(@NonNull Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    /**
     * 在 IO 线程并行查询 6 次 DAO（3 游戏 × 最高星/最高分）+ 今日玩过状态，
     * 组装成卡片列表后 postValue。
     */
    public void load() {
        AppExecutors.io(() -> {
            final String today = LocalDate.now().toString();
            final int todayChallenge = LocalDate.now().getDayOfYear() % 3;

            List<GameHubCard> result = new ArrayList<>();
            result.add(buildCard(TYPE_COUPLET, today, todayChallenge));
            result.add(buildCard(TYPE_MATCH, today, todayChallenge));
            result.add(buildCard(TYPE_QUIZ, today, todayChallenge));
            result.add(buildCard(TYPE_FLYFLOWER, today, todayChallenge));
            result.add(buildCard(TYPE_AUTHOR, today, todayChallenge));

            // LiveData 需要新 List 实例才触发通知
            cards.postValue(new ArrayList<>(result));
            recall.postValue(buildRecall());
        });
    }

    private GameHubCard buildCard(String gameType, String today, int todayChallenge) {
        int bestScore = db.gameHistoryDao().getBestScoreByType(gameType);
        int bestStars = db.gameHistoryDao().getBestStarsByType(gameType);
        boolean playedToday = db.dailyStatsDao().hasGameToday(today);
        boolean isTodayChallenge = challengeIndex(gameType) == todayChallenge;
        return new GameHubCard(gameType, bestScore, bestStars, playedToday, isTodayChallenge);
    }

    private int challengeIndex(String gameType) {
        switch (gameType) {
            case TYPE_COUPLET: return 0;
            case TYPE_MATCH:   return 1;
            case TYPE_QUIZ:    return 2;
            default:           return -1;
        }
    }

    /** 读取最近一条游戏记录，生成「最近有趣视线」文案。 */
    private String buildRecall() {
        GameHistory latest = db.gameHistoryDao().getLatestByTypeSync(TYPE_COUPLET);
        if (latest == null) {
            latest = db.gameHistoryDao().getLatestByTypeSync(TYPE_MATCH);
        }
        if (latest == null) {
            latest = db.gameHistoryDao().getLatestByTypeSync(TYPE_QUIZ);
        }
        if (latest == null) {
            latest = db.gameHistoryDao().getLatestByTypeSync(TYPE_FLYFLOWER);
        }
        if (latest == null) {
            latest = db.gameHistoryDao().getLatestByTypeSync(TYPE_AUTHOR);
        }
        if (latest == null) {
            return null;
        }
        return latest.gameType;
    }

    public LiveData<List<GameHubCard>> getCards() { return cards; }

    /** 最近一次游戏类型（用于 game_recall 文案），无记录时为 null。 */
    public LiveData<String> getRecall() { return recall; }

    /**
     * 单张游戏卡的数据模型。
     */
    public static class GameHubCard {
        public final String gameType;
        public final int bestScore;
        public final int bestStars;
        public final boolean playedToday;
        public final boolean isTodayChallenge;

        GameHubCard(String gameType, int bestScore, int bestStars,
                    boolean playedToday, boolean isTodayChallenge) {
            this.gameType = gameType;
            this.bestScore = bestScore;
            this.bestStars = bestStars;
            this.playedToday = playedToday;
            this.isTodayChallenge = isTodayChallenge;
        }
    }
}
