package com.poetry.ui.detail;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.LearningRecord;
import com.poetry.util.AppExecutors;

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
    private String currentTitle;
    private String currentAuthor;
    private String currentDynasty;

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
     * 查询指定诗词的收藏和已学状态，同时存储诗词元数据。
     * 在后台线程中查询数据库，结果通过 LiveData 推送。
     *
     * @param poemId  诗词 ID
     * @param title   诗词标题
     * @param author  作者
     * @param dynasty 朝代
     */
    public void checkStatus(String poemId, String title, String author, String dynasty) {
        this.currentPoemId = poemId;
        this.currentTitle = title;
        this.currentAuthor = author;
        this.currentDynasty = dynasty;
        AppExecutors.io(() -> {
            boolean fav = db.learningRecordDao().isFavorite(poemId);
            boolean learned = db.learningRecordDao().isLearned(poemId);
            isFavorite.postValue(fav);
            isLearned.postValue(learned);
        });
    }

    /**
     * 切换收藏状态：在子线程中写入/移除收藏记录。
     * 写入时携带诗词元数据（标题/作者/朝代），确保列表页面正确显示。
     * 如果已存在学习记录则合并，避免覆盖 quizScore 等其他字段。
     * 操作完成后通过 LiveData 推送新状态。
     */
    public void toggleFavorite() {
        Boolean current = isFavorite.getValue();
        boolean newFav = current == null || !current;
        String poemId = this.currentPoemId;
        if (poemId == null) return;

        AppExecutors.io(() -> {
            if (newFav) {
                LearningRecord existing = db.learningRecordDao().getLearningRecord(poemId);
                if (existing != null) {
                    existing.favorite = true;
                    existing.title = currentTitle != null ? currentTitle : "";
                    existing.dynasty = currentDynasty != null ? currentDynasty : "";
                    existing.author = currentAuthor != null ? currentAuthor : "";
                    db.learningRecordDao().insertLearningRecord(existing);
                } else {
                    LearningRecord record = new LearningRecord(poemId,
                        currentTitle != null ? currentTitle : "",
                        currentDynasty != null ? currentDynasty : "",
                        currentAuthor != null ? currentAuthor : "");
                    record.favorite = true;
                    record.learnedAt = 0L; // 纯收藏，不标记为已学
                    db.learningRecordDao().insertLearningRecord(record);
                }
            } else {
                db.learningRecordDao().removeFavorite(poemId);
            }
            isFavorite.postValue(newFav);
        });
    }

    /**
     * 标记该诗词为"已学"：写入完整的学习记录（含诗词元数据）和时间戳，
     * 如果已存在则合并（保留 quizScore/gamePlayed 等字段），
     * 同时更新今日 DailyStats 的 poemsLearned 计数（供学习趋势图表使用）。
     * 操作完成后通过 LiveData 推送新状态。
     */
    public void markAsLearned() {
        String poemId = this.currentPoemId;
        if (poemId == null) return;

        AppExecutors.io(() -> {
            LearningRecord existing = db.learningRecordDao().getLearningRecord(poemId);
            if (existing != null) {
                existing.learnedAt = System.currentTimeMillis();
                existing.title = currentTitle != null ? currentTitle : "";
                existing.dynasty = currentDynasty != null ? currentDynasty : "";
                existing.author = currentAuthor != null ? currentAuthor : "";
                db.learningRecordDao().insertLearningRecord(existing);
            } else {
                LearningRecord record = new LearningRecord(poemId,
                    currentTitle != null ? currentTitle : "",
                    currentDynasty != null ? currentDynasty : "",
                    currentAuthor != null ? currentAuthor : "");
                record.favorite = db.learningRecordDao().isFavorite(poemId);
                db.learningRecordDao().insertLearningRecord(record);
            }

            // 更新每日统计：确保今日行存在后递增已学诗词数
            String today = java.time.LocalDate.now().toString();
            com.poetry.data.DailyStats ds = db.dailyStatsDao().getDailyStatsSync(today);
            if (ds == null) {
                db.dailyStatsDao().upsertDailyStats(new com.poetry.data.DailyStats(today));
            }
            db.dailyStatsDao().incrementPoemsLearned(today);

            isLearned.postValue(true);
        });
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
