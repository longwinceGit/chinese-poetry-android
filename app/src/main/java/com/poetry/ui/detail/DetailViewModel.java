package com.poetry.ui.detail;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.LearningDatabase;

/**
 * 详情页 ViewModel，管理诗词的收藏和已学状态。
 * <p>
 * 将 Fragment 中的数据库操作（查询收藏/已学状态、切换收藏、标记已学）
 * 迁移到此 ViewModel，遵循 MVVM 架构。LiveData 驱动的 UI 更新确保
 * 配置变更时状态不丢失。
 * </p>
 */
public class DetailViewModel extends AndroidViewModel {

    private final LearningDatabase db;
    private final MutableLiveData<Boolean> isFavorite = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLearned = new MutableLiveData<>();

    /** 当前查看的诗词 ID */
    private String currentPoemId;

    /**
     * 构造方法，获取数据库实例。
     *
     * @param app Application 上下文
     */
    public DetailViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    /**
     * 查询指定诗词的收藏和已学状态。
     * 在后台线程中查询数据库，结果通过 LiveData 推送。
     *
     * @param poemId 诗词 ID
     */
    public void checkStatus(String poemId) {
        this.currentPoemId = poemId;
        new Thread(() -> {
            boolean fav = db.poemDao().isFavorite(poemId);
            boolean learned = db.poemDao().isLearned(poemId);
            isFavorite.postValue(fav);
            isLearned.postValue(learned);
        }).start();
    }

    /**
     * 切换收藏状态：在子线程中写入/移除收藏记录。
     * 操作完成后通过 LiveData 推送新状态。
     */
    public void toggleFavorite() {
        Boolean current = isFavorite.getValue();
        boolean newFav = current == null || !current;
        String poemId = this.currentPoemId;
        if (poemId == null) return;

        new Thread(() -> {
            db.poemDao().ensureRecordExists(poemId);
            if (newFav) {
                db.poemDao().addFavorite(poemId);
            } else {
                db.poemDao().removeFavorite(poemId);
            }
            isFavorite.postValue(newFav);
        }).start();
    }

    /**
     * 标记该诗词为"已学"：写入学习记录和时间戳，
     * 同时更新今日 DailyStats 的 poemsLearned 计数（供学习趋势图表使用）。
     * 操作完成后通过 LiveData 推送新状态。
     */
    public void markAsLearned() {
        String poemId = this.currentPoemId;
        if (poemId == null) return;

        new Thread(() -> {
            db.poemDao().ensureRecordExists(poemId);
            db.poemDao().markLearned(poemId, System.currentTimeMillis());

            // 更新每日统计：确保今日行存在后递增已学诗词数
            String today = java.time.LocalDate.now().toString();
            com.poetry.data.DailyStats existing = db.poemDao().getDailyStatsSync(today);
            if (existing == null) {
                db.poemDao().upsertDailyStats(new com.poetry.data.DailyStats(today));
            }
            db.poemDao().incrementPoemsLearned(today);

            isLearned.postValue(true);
        }).start();
    }

    /**
     * 获取收藏状态的 LiveData。
     *
     * @return 收藏状态 LiveData
     */
    public LiveData<Boolean> getIsFavorite() {
        return isFavorite;
    }

    /**
     * 获取已学状态的 LiveData。
     *
     * @return 已学状态 LiveData
     */
    public LiveData<Boolean> getIsLearned() {
        return isLearned;
    }
}
