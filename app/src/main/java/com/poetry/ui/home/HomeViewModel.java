package com.poetry.ui.home;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.PoemRepository;
import com.poetry.data.UserProfile;
import com.poetry.data.model.Poem;
import com.poetry.domain.LearningEngine;

import java.util.List;

/**
 * 首页 ViewModel，负责诗词数据加载、分类筛选、搜索、分页等核心逻辑。
 * 维护诗词列表、每日推荐、分类列表等 LiveData 供 Fragment 观察。
 */
public class HomeViewModel extends AndroidViewModel {

    private PoemRepository repo = PoemRepository.getInstance();
    private LearningDatabase db;

    private MutableLiveData<List<Poem>> poems = new MutableLiveData<>();
    private MutableLiveData<String> loadingMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Poem> dailyPoem = new MutableLiveData<>();
    private MutableLiveData<List<String>> categories = new MutableLiveData<>();
    private MutableLiveData<List<String>> categoryIcons = new MutableLiveData<>();

    private MutableLiveData<Integer> totalCount = new MutableLiveData<>(0);

    private String currentCategory = "全部";
    private String searchQuery = "";
    private boolean searchMode = false;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 30;

    /**
     * 构造函数，初始化数据库实例。
     *
     * @param app 应用上下文
     */
    public HomeViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    /**
     * 异步加载所有诗词数据。
     * 在后台线程中从 assets 加载诗词、分类列表、分类图标及每日推荐，
     * 加载完成后按当前分类（默认"全部"）展示第一页数据。
     */
    public void loadPoems() {
        isLoading.setValue(true);
        loadingMessage.setValue("正在加载诗词数据...");
        new Thread(() -> {
            try {
                List<Poem> result = repo.loadPoemsAsync(getApplication().getAssets()).get();
                java.util.List<String> cats = repo.getCategories();
                java.util.List<String> icons = repo.getCategoryIcons();
                Poem daily = repo.getDailyPoem();
                categories.postValue(cats);
                categoryIcons.postValue(icons);
                dailyPoem.postValue(daily);
                isLoading.postValue(false);
                if (result != null && !result.isEmpty()) {
                    currentPage = 0;
                    List<Poem> filtered = repo.getPoemsByCategory(currentCategory);
                    totalCount.postValue(filtered.size());
                    int end = Math.min(PAGE_SIZE, filtered.size());
                    List<Poem> page;
                    if (end > 0) {
                        page = new java.util.ArrayList<>(filtered.subList(0, end));
                    } else {
                        page = new java.util.ArrayList<>();
                    }
                    poems.postValue(page);
                }
                // 后台构建搜索索引（不阻塞 UI）
                repo.warmupIndices();
            } catch (Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("数据加载失败：" + e.getMessage());
            }
        }).start();
    }

    /**
     * 重新选择每日推荐诗词，更新 dailyPoem LiveData。
     */
    public void selectDailyPoem() {
        Poem p = repo.getDailyPoem();
        if (p != null) {
            dailyPoem.setValue(p);
        }
    }

    /**
     * 设置当前诗词分类，退出搜索模式，重置到第一页。
     *
     * @param category 分类名称
     */
    public void setCategory(String category) {
        currentCategory = category;
        searchMode = false;
        searchQuery = "";
        List<Poem> filtered = repo.getPoemsByCategory(category);
        totalCount.setValue(filtered.size());
        showPage(0);
    }

    /**
     * 搜索诗词，自动判断搜索模式切换。
     * <p>
     * 当查询非空时进入搜索模式，直接展示搜索结果；
     * 当查询为空时退出搜索模式，回到当前分类的第一页。
     *
     * @param query 搜索关键词
     */
    public void search(String query) {
        searchQuery = query.trim();
        searchMode = !searchQuery.isEmpty();
        if (searchMode) {
            List<Poem> results = repo.search(searchQuery);
            poems.setValue(results);
        } else {
            showPage(0);
        }
    }

    /**
     * 加载更多（翻页），仅在非搜索模式下有效。
     * <p>
     * 追加下一页数据到当前列表，创建新的 List 实例以触发 LiveData 通知。
     * 若数据不足一页则回退页码。
     */
    public void loadMore() {
        if (searchMode) return;
        currentPage++;
        int start = currentPage * PAGE_SIZE;
        List<Poem> filtered = repo.getPoemsByCategory(currentCategory);
        int end = Math.min(start + PAGE_SIZE, filtered.size());
        if (start < filtered.size()) {
            List<Poem> more = filtered.subList(start, end);
            List<Poem> current = poems.getValue();
            if (current != null) {
                // 必须创建新 List —— LiveData 同引用不通知
                List<Poem> updated = new java.util.ArrayList<>(current);
                updated.addAll(more);
                poems.setValue(updated);
            }
        } else {
            // 回退页码（数据不足一页）
            currentPage--;
        }
    }

    /**
     * 显示指定页码的数据，生成新的 List 实例以确保 LiveData 能检测到变化。
     *
     * @param page 页码（从 0 开始）
     */
    private void showPage(int page) {
        currentPage = page;
        List<Poem> filtered = repo.getPoemsByCategory(currentCategory);
        int end = Math.min(PAGE_SIZE, filtered.size());
        if (end > 0) {
            poems.setValue(new java.util.ArrayList<>(filtered.subList(0, end)));
        } else {
            poems.setValue(new java.util.ArrayList<>());
        }
    }

    /** @return 当前选中的分类 */
    public String getCurrentCategory() { return currentCategory; }
    /** @return 是否处于搜索模式 */
    public boolean isSearchMode() { return searchMode; }
    /** @return 当前搜索关键词 */
    public String getSearchQuery() { return searchQuery; }
    /** @return 当前页码（从 0 开始） */
    public int getCurrentPage() { return currentPage; }

    /** @return 当前显示的诗词列表 LiveData */
    public LiveData<List<Poem>> getPoems() { return poems; }
    /** @return 加载提示信息 LiveData */
    public LiveData<String> getLoadingMessage() { return loadingMessage; }
    /** @return 是否正在加载 LiveData */
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    /** @return 错误信息 LiveData（非空时显示错误状态） */
    public LiveData<String> getErrorMessage() { return errorMessage; }
    /** @return 每日推荐诗词 LiveData */
    public LiveData<Poem> getDailyPoem() { return dailyPoem; }
    /** @return 分类名称列表 LiveData */
    public LiveData<List<String>> getCategories() { return categories; }
    /** @return 分类图标列表 LiveData */
    public LiveData<List<String>> getCategoryIcons() { return categoryIcons; }

    /** @return 用户学习档案 LiveData */
    public LiveData<UserProfile> getUserProfile() {
        return db.poemDao().getUserProfile();
    }

    /** @return 当前分类下的诗词总数 LiveData */
    public LiveData<Integer> getTotalCount() { return totalCount; }
    /** @return 当前分类下的诗词总数（同步取值，含 null 保护） */
    public int getTotalCountValue() {
        Integer v = totalCount.getValue();
        return v != null ? v : 0;
    }
    /** @return 每页显示数量 */
    public static int getPageSize() { return PAGE_SIZE; }
}
