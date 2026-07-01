package com.poetry.ui.profile;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.UserProfile;

public class ProfileViewModel extends AndroidViewModel {

    private final LearningDatabase db;
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<Integer> favoriteCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> learnedCount = new MutableLiveData<>();

    public ProfileViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    public void loadData() {
        new Thread(() -> {
            UserProfile profile = db.poemDao().getUserProfileSync();
            int favCount = db.poemDao().getFavCountSync();
            int learned = db.poemDao().getLearnedCountSync();
            userProfile.postValue(profile);
            favoriteCount.postValue(favCount);
            learnedCount.postValue(learned);
        }).start();
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    public LiveData<Integer> getFavoriteCount() {
        return favoriteCount;
    }

    public LiveData<Integer> getLearnedCount() {
        return learnedCount;
    }
}
