package com.poetry.ui.learning;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.LearningRecord;
import com.poetry.data.UserProfile;
import com.poetry.domain.AchievementEngine;
import com.poetry.domain.LearningEngine;
import com.poetry.ui.adapter.AchievementAdapter;

import java.util.ArrayList;
import java.util.List;

public class LearningViewModel extends AndroidViewModel {

    private LearningDatabase db;
    private MutableLiveData<List<AchievementAdapter.AchievementEntry>> achievements = new MutableLiveData<>();

    public LearningViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    public LiveData<UserProfile> getUserProfile() {
        return db.poemDao().getUserProfile();
    }

    public LiveData<Integer> getLearnedCount() {
        return db.poemDao().getLearnedCount();
    }

    public LiveData<List<LearningRecord>> getFavorites() {
        return db.poemDao().getFavorites();
    }

    public LiveData<List<AchievementAdapter.AchievementEntry>> getAchievements() {
        loadAchievements();
        return achievements;
    }

    public void loadAchievements() {
        new Thread(() -> {
            UserProfile profile = db.poemDao().getUserProfileSync();
            if (profile == null) return;
            List<String> unlockedIds = AchievementEngine.getUnlockedIds(profile);
            List<AchievementAdapter.AchievementEntry> entries = new ArrayList<>();
            for (AchievementEngine.AchievementDef def : AchievementEngine.ALL_ACHIEVEMENTS) {
                entries.add(new AchievementAdapter.AchievementEntry(def, unlockedIds.contains(def.id)));
            }
            achievements.postValue(entries);
        }).start();
    }

    public String getLevelName(int level) {
        return LearningEngine.getLevelName(level);
    }

    public int getLevelProgress(int totalPoints) {
        return LearningEngine.getLevelProgress(totalPoints);
    }
}
