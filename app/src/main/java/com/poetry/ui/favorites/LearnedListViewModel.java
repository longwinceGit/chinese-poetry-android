package com.poetry.ui.favorites;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.LearningRecord;

import java.util.List;

/**
 * 已学诗词列表 ViewModel。
 * <p>
 * 管理已学诗词列表的加载。利用 Room 的 LiveData 自动刷新机制，
 * 当用户标记新诗词为已学时，列表自动更新。
 * </p>
 */
public class LearnedListViewModel extends AndroidViewModel {

    private final LearningDatabase db;
    private final LiveData<List<LearningRecord>> learnedPoems;

    /**
     * 构造方法，获取数据库实例并绑定已学诗词 LiveData。
     *
     * @param app Application 上下文
     */
    public LearnedListViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
        learnedPoems = db.learningRecordDao().getLearnedPoems();
    }

    /**
     * 获取已学诗词列表 LiveData（Room 自动刷新）。
     *
     * @return 已学诗词列表（按学习时间降序）
     */
    public LiveData<List<LearningRecord>> getLearnedPoems() {
        return learnedPoems;
    }
}
