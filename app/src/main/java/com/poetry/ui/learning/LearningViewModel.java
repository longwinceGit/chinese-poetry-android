package com.poetry.ui.learning;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.DailyStats;
import com.poetry.data.LearningDatabase;
import com.poetry.data.UserProfile;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LearningViewModel extends AndroidViewModel {

    private final LearningDatabase db;
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<Integer> learnedCount = new MutableLiveData<>(0);
    /** 本周已签到日期集合 (yyyy-MM-dd) */
    private final MutableLiveData<Set<String>> checkinDates = new MutableLiveData<>(new HashSet<>());
    /** 当日任务完成状态 [学诗词, 答题, 玩游戏]，true=已完成 */
    private final MutableLiveData<boolean[]> todayTasks = new MutableLiveData<>(new boolean[]{false, false, false});

    public LearningViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    public void loadData() {
        new Thread(() -> {
            // 自动签到当日
            doAutoCheckin();

            UserProfile profile = db.poemDao().getUserProfileSync();
            int count = db.poemDao().getLearnedCountSync();
            Set<String> dates = loadWeekCheckins();

            // 查询当日任务完成状态（使用 DailyStats 表）
            String today = LocalDate.now().toString();
            long startOfDay = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli();
            boolean[] tasks = new boolean[]{
                db.poemDao().getTodayLearnedCount(startOfDay) > 0,
                db.poemDao().hasQuizToday(today),
                db.poemDao().hasGameToday(today)
            };

            userProfile.postValue(profile);
            learnedCount.postValue(count);
            checkinDates.postValue(dates);
            todayTasks.postValue(tasks);
        }).start();
    }

    /**
     * 自动签到：打开学习页面即视为当日签到，写入 daily_stats + 更新 streak
     */
    private void doAutoCheckin() {
        String today = LocalDate.now().toString();
        DailyStats existing = db.poemDao().getDailyStats(today);
        if (existing == null) {
            DailyStats stats = new DailyStats(today);
            db.poemDao().upsertDailyStats(stats);
        }

        // 更新连续签到天数
        UserProfile profile = db.poemDao().getUserProfileSync();
        String lastActive = profile.lastActiveDate;
        LocalDate yesterday = LocalDate.now().minusDays(1);

        int newStreak;
        if (lastActive.equals(yesterday.toString())) {
            newStreak = profile.streak + 1;
        } else if (lastActive.equals(today)) {
            newStreak = profile.streak; // 今天已经签过
        } else {
            newStreak = 1; // 断签，重新开始
        }

        // 升级逻辑（连续7天升1级，最高20级）
        int newLevel = profile.level;
        if (newStreak > 0 && newStreak % 7 == 0 && profile.level < 20) {
            newLevel = profile.level + 1;
        }

        db.poemDao().updateStreak(newStreak, today);
        if (newLevel != profile.level) {
            db.poemDao().updateLevel(newLevel);
        }
    }

    /**
     * 查询本周签到日期（周一 ~ 周日）
     */
    private Set<String> loadWeekCheckins() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate sunday = monday.plusDays(6);

        List<String> dates = db.poemDao().getCheckinDates(
            monday.toString(), sunday.toString());
        return new HashSet<>(dates);
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    public LiveData<Integer> getLearnedCount() {
        return learnedCount;
    }

    public LiveData<Set<String>> getCheckinDates() {
        return checkinDates;
    }

    public LiveData<boolean[]> getTodayTasks() {
        return todayTasks;
    }
}
