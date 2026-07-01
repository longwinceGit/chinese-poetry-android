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

public class HomeViewModel extends AndroidViewModel {

    private PoemRepository repo = PoemRepository.getInstance();
    private LearningDatabase db;

    private MutableLiveData<List<Poem>> poems = new MutableLiveData<>();
    private MutableLiveData<String> loadingMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private MutableLiveData<Poem> dailyPoem = new MutableLiveData<>();
    private MutableLiveData<List<String>> categories = new MutableLiveData<>();
    private MutableLiveData<List<String>> categoryIcons = new MutableLiveData<>();

    private MutableLiveData<Integer> totalCount = new MutableLiveData<>(0);

    private String currentCategory = "全部";
    private String searchQuery = "";
    private boolean searchMode = false;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 30;

    public HomeViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    public void loadPoems() {
        isLoading.setValue(true);
        loadingMessage.setValue("正在加载诗词...");
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
            } catch (Exception e) {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void selectDailyPoem() {
        Poem p = repo.getDailyPoem();
        if (p != null) {
            dailyPoem.setValue(p);
        }
    }

    public void setCategory(String category) {
        currentCategory = category;
        searchMode = false;
        searchQuery = "";
        List<Poem> filtered = repo.getPoemsByCategory(category);
        totalCount.setValue(filtered.size());
        showPage(0);
    }

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
                current.addAll(more);
                poems.setValue(current);
            }
        }
    }

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

    public String getCurrentCategory() { return currentCategory; }
    public boolean isSearchMode() { return searchMode; }
    public String getSearchQuery() { return searchQuery; }
    public int getCurrentPage() { return currentPage; }

    public LiveData<List<Poem>> getPoems() { return poems; }
    public LiveData<String> getLoadingMessage() { return loadingMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Poem> getDailyPoem() { return dailyPoem; }
    public LiveData<List<String>> getCategories() { return categories; }
    public LiveData<List<String>> getCategoryIcons() { return categoryIcons; }

    public LiveData<UserProfile> getUserProfile() {
        return db.poemDao().getUserProfile();
    }

    public LiveData<Integer> getTotalCount() { return totalCount; }
    public int getTotalCountValue() {
        Integer v = totalCount.getValue();
        return v != null ? v : 0;
    }
    public static int getPageSize() { return PAGE_SIZE; }
}
