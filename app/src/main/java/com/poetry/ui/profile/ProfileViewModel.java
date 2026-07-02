package com.poetry.ui.profile;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.UserProfile;

/**
 * 个人中心 ViewModel，管理个人中心的用户数据。
 * 提供用户信息、收藏数和已学数量的 LiveData 供 Fragment 观察。
 */
public class ProfileViewModel extends AndroidViewModel {

    private final LearningDatabase db;
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<Integer> favoriteCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> learnedCount = new MutableLiveData<>();

    /**
     * 构造方法，获取数据库实例。
     *
     * @param app Application 上下文
     */
    public ProfileViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    /**
     * 在后台线程加载用户信息、收藏数和已学数量。
     */
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

    /**
     * 获取用户信息的 LiveData。
     *
     * @return 用户信息 LiveData
     */
    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    /**
     * 获取收藏数量的 LiveData。
     *
     * @return 收藏数量 LiveData
     */
    public LiveData<Integer> getFavoriteCount() {
        return favoriteCount;
    }

    /**
     * 获取已学数量的 LiveData。
     *
     * @return 已学数量 LiveData
     */
    public LiveData<Integer> getLearnedCount() {
        return learnedCount;
    }
}
