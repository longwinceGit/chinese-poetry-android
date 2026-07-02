package com.poetry.ui.favorites;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.LearningRecord;

import java.util.List;

/**
 * 收藏列表 ViewModel。
 * <p>
 * 管理收藏诗词列表的加载与取消收藏操作。
 * 利用 Room 的 LiveData 自动刷新机制，数据库变更时 UI 自动更新。
 * </p>
 */
public class FavoritesViewModel extends AndroidViewModel {

    private final LearningDatabase db;
    private final LiveData<List<LearningRecord>> favorites;

    /**
     * 构造方法，获取数据库实例并绑定 Room LiveData。
     *
     * @param app Application 上下文
     */
    public FavoritesViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
        favorites = db.poemDao().getFavorites();
    }

    /**
     * 取消收藏指定诗词。
     * <p>
     * 在后台线程中更新数据库，Room LiveData 自动触发 UI 刷新。
     * </p>
     *
     * @param poemId 诗词 ID
     */
    public void removeFavorite(String poemId) {
        new Thread(() -> {
            db.poemDao().removeFavorite(poemId);
        }).start();
    }

    /**
     * 获取收藏列表 LiveData（Room 自动刷新）。
     *
     * @return 收藏列表
     */
    public LiveData<List<LearningRecord>> getFavorites() {
        return favorites;
    }
}
