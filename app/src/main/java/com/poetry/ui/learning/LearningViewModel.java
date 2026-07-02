package com.poetry.ui.learning;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.DailyStats;
import com.poetry.data.LearningDatabase;
import com.poetry.data.UserProfile;
import com.poetry.domain.AchievementEngine;
import com.poetry.domain.ThemeManager;
import com.poetry.ui.widget.StatsBarChart;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 学习页面 ViewModel，管理学习页的状态和数据。
 * 核心逻辑：页面打开时自动签到、连续签到计算（基于昨日签到状态判断是否断签）、
 * 每 7 天升级（最高 20 级）、成就检测与主题解锁同步。
 */
public class LearningViewModel extends AndroidViewModel {

    private final LearningDatabase db;
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<Integer> learnedCount = new MutableLiveData<>(0);
    /** 本周已签到日期集合 (yyyy-MM-dd) */
    private final MutableLiveData<Set<String>> checkinDates = new MutableLiveData<>(new HashSet<>());
    /** 当日任务完成状态 [学诗词, 答题, 玩游戏]，true=已完成 */
    private final MutableLiveData<boolean[]> todayTasks = new MutableLiveData<>(new boolean[]{false, false, false});

    // 🔴 B4 修复：成就解锁通知
    private final MutableLiveData<AchievementEngine.AchievementDef> newAchievement = new MutableLiveData<>();

    // 最近 7 天学习统计柱状图数据
    private final MutableLiveData<List<StatsBarChart.BarData>> chartData = new MutableLiveData<>();

    public LearningViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    /**
     * 加载学习页所有数据：自动签到、用户信息、已学数量、本周签到日历、当日任务状态、
     * 成就检测和主题解锁。
     */
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

            // 🔴 B4 修复：加载学习页时检测成就（签到触发连续天数成就）
            AchievementEngine.checkAndUnlock(db, def -> {
                newAchievement.postValue(def);
            });
            // 🔴 B5 修复：签到/等级提升后同步主题解锁
            ThemeManager.syncUnlockedThemes(db);

            // 加载最近 7 天学习统计柱状图数据
            loadChartData();
        }).start();
    }

    /**
     * 查询最近 7 天的每日统计数据，构建柱状图数据。
     */
    private void loadChartData() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);
        String startDate = sevenDaysAgo.toString();

        List<DailyStats> statsList = db.poemDao().getRecentStats(startDate);
        String[] weekLabels = {"一", "二", "三", "四", "五", "六", "日"};

        List<StatsBarChart.BarData> bars = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = sevenDaysAgo.plusDays(i);
            String dateStr = day.toString();

            // 查找该日期的统计数据
            DailyStats found = null;
            for (DailyStats s : statsList) {
                if (s.date.equals(dateStr)) {
                    found = s;
                    break;
                }
            }

            float value = found != null ? found.poemsLearned : 0;
            String label = weekLabels[day.getDayOfWeek().getValue() - 1];
            bars.add(new StatsBarChart.BarData(label, value));
        }

        chartData.postValue(bars);
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

    /**
     * 获取用户信息的 LiveData。
     *
     * @return 用户信息 LiveData
     */
    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    /**
     * 获取已学诗词数量的 LiveData。
     *
     * @return 已学数量 LiveData
     */
    public LiveData<Integer> getLearnedCount() {
        return learnedCount;
    }

    /**
     * 获取本周已签到日期集合的 LiveData。
     *
     * @return 签到日期集合 LiveData（格式 yyyy-MM-dd）
     */
    public LiveData<Set<String>> getCheckinDates() {
        return checkinDates;
    }

    /**
     * 获取当日任务完成状态的 LiveData。
     *
     * @return 任务状态数组 [学诗词, 答题, 玩游戏]，true=已完成
     */
    public LiveData<boolean[]> getTodayTasks() {
        return todayTasks;
    }

    /**
     * 获取新成就解锁事件的 LiveData。
     *
     * @return 新成就 LiveData
     */
    public LiveData<AchievementEngine.AchievementDef> getNewAchievement() {
        return newAchievement;
    }

    /** 消费成就事件后清空，防止 LiveData 回放导致重复庆祝 */
    public void clearAchievement() {
        newAchievement.setValue(null);
    }

    /**
     * 获取最近 7 天学习统计柱状图数据的 LiveData。
     *
     * @return 柱状图数据
     */
    public LiveData<List<StatsBarChart.BarData>> getChartData() {
        return chartData;
    }
}
