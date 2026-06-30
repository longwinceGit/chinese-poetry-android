package com.poetry.ui.profile;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.DailyStats;
import com.poetry.data.LearningDatabase;
import com.poetry.data.LearningRecord;
import com.poetry.data.UserProfile;
import com.poetry.domain.LearningEngine;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileViewModel extends AndroidViewModel {

    private LearningDatabase db;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private MutableLiveData<Integer> todayLearned = new MutableLiveData<>(0);
    private MutableLiveData<Integer> todayQuiz = new MutableLiveData<>(0);
    private MutableLiveData<Integer> totalLearned = new MutableLiveData<>(0);
    private MutableLiveData<Integer> totalFav = new MutableLiveData<>(0);
    private MutableLiveData<Integer> totalGames = new MutableLiveData<>(0);
    private MutableLiveData<List<Integer>> weeklyStats = new MutableLiveData<>();

    public ProfileViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    public void loadProfile() {
        new Thread(() -> {
            UserProfile profile = db.poemDao().getUserProfileSync();
            if (profile != null) {
                userProfile.postValue(profile);
            }

            // 今日数据
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();

            List<LearningRecord> todayRecords = db.poemDao().getTodayRecords(startOfDay);
            int learned = todayRecords.size();
            int quiz = 0;
            for (LearningRecord r : todayRecords) {
                if (r.quizScore > 0) quiz += r.quizScore;
            }
            todayLearned.postValue(learned);
            todayQuiz.postValue(quiz);

            // 累计数据
            totalLearned.postValue(db.poemDao().getLearnedCountSync());
            totalFav.postValue(db.poemDao().getFavCountSync());

            int games = 0;
            List<LearningRecord> gameRecords = db.poemDao().getGameRecords();
            for (LearningRecord r : gameRecords) games += r.gamePlayed;
            totalGames.postValue(games);

            // 周统计
            Calendar weekCal = Calendar.getInstance();
            weekCal.add(Calendar.DAY_OF_YEAR, -6);
            String startDate = dateFormat.format(weekCal.getTime());
            List<DailyStats> stats = db.poemDao().getRecentStats(startDate);

            List<Integer> weekData = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                weekData.add(0);
            }
            for (DailyStats ds : stats) {
                for (int i = 0; i < 7; i++) {
                    Calendar day = Calendar.getInstance();
                    day.add(Calendar.DAY_OF_YEAR, -6 + i);
                    if (dateFormat.format(day.getTime()).equals(ds.date)) {
                        weekData.set(i, ds.poemsLearned + ds.quizCompleted);
                        break;
                    }
                }
            }
            weeklyStats.postValue(weekData);
        }).start();
    }

    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<Integer> getTodayLearned() { return todayLearned; }
    public LiveData<Integer> getTodayQuiz() { return todayQuiz; }
    public LiveData<Integer> getTotalLearned() { return totalLearned; }
    public LiveData<Integer> getTotalFav() { return totalFav; }
    public LiveData<Integer> getTotalGames() { return totalGames; }
    public LiveData<List<Integer>> getWeeklyStats() { return weeklyStats; }

    public String getLevelName(int level) {
        return LearningEngine.getLevelName(level);
    }

    public int getLevelProgress(int totalPoints) {
        return LearningEngine.getLevelProgress(totalPoints);
    }
}
