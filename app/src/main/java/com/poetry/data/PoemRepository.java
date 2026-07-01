package com.poetry.data;

import android.content.res.AssetManager;

import com.poetry.data.model.Poem;
import com.poetry.PoemLoader;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PoemRepository {

    private static PoemRepository instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private List<Poem> allPoems = new ArrayList<>();
    private List<String> categories = new ArrayList<>();
    private List<String> categoryIcons = new ArrayList<>();
    private boolean loaded = false;

    private PoemRepository() {}

    public static synchronized PoemRepository getInstance() {
        if (instance == null) {
            instance = new PoemRepository();
        }
        return instance;
    }

    public Future<List<Poem>> loadPoemsAsync(AssetManager assets) {
        return executor.submit(new Callable<List<Poem>>() {
            @Override
            public List<Poem> call() throws Exception {
                List<Poem> poems = PoemLoader.loadAll(assets);
                // 著名诗词（有释义）排在前面
                Collections.sort(poems, new Comparator<Poem>() {
                    @Override
                    public int compare(Poem a, Poem b) {
                        boolean aFamous = a.explanation != null && !a.explanation.isEmpty();
                        boolean bFamous = b.explanation != null && !b.explanation.isEmpty();
                        if (aFamous && !bFamous) return -1;
                        if (!aFamous && bFamous) return 1;
                        return 0;
                    }
                });
                allPoems = poems;
                loaded = true;
                buildCategories();
                return poems;
            }
        });
    }

    public List<Poem> getAllPoems() {
        return allPoems;
    }

    public List<Poem> getPoemsByCategory(String category) {
        if ("全部".equals(category)) {
            return new ArrayList<>(allPoems);
        }
        List<Poem> result = new ArrayList<>();
        for (Poem p : allPoems) {
            if (category.equals(p.category)) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Poem> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String lower = query.trim().toLowerCase();
        List<Poem> results = new ArrayList<>();
        for (Poem p : allPoems) {
            if (p.title.contains(query) || p.title.toLowerCase().contains(lower)
                || p.author.contains(query) || p.author.toLowerCase().contains(lower)
                || p.getFullText().contains(query)) {
                results.add(p);
            }
        }
        if (results.size() > 500) {
            results = new ArrayList<>(results.subList(0, 500));
        }
        return results;
    }

    public Poem findPoemById(String id) {
        for (Poem p : allPoems) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    /**
     * 基于日期 Hash 固定每日推荐 —— 同一天始终返回同一首诗。
     * 保证"每日推荐"概念名副其实。
     */
    public Poem getDailyPoem() {
        if (allPoems.isEmpty()) return null;
        String dateKey = LocalDate.now().toString();
        int idx = Math.abs(dateKey.hashCode()) % allPoems.size();
        return allPoems.get(idx);
    }

    /** 保留随机接口供其他场景使用（游戏等） */
    public Poem getRandomPoem() {
        if (allPoems.isEmpty()) return null;
        int idx = (int) (Math.random() * allPoems.size());
        return allPoems.get(idx);
    }

    public List<String> getCategories() {
        return categories;
    }

    public List<String> getCategoryIcons() {
        return categoryIcons;
    }

    public boolean isLoaded() {
        return loaded;
    }

    private void buildCategories() {
        Set<String> dynasties = new LinkedHashSet<>();
        java.util.Map<String, Integer> dynastyCounts = new java.util.HashMap<>();
        for (Poem p : allPoems) {
            if (p.dynasty != null && !p.dynasty.isEmpty()) {
                dynasties.add(p.dynasty);
                Integer cnt = dynastyCounts.get(p.dynasty);
                dynastyCounts.put(p.dynasty, cnt == null ? 1 : cnt + 1);
            }
        }
        String[] navOrder = {"先秦", "春秋", "春秋战国", "魏晋", "唐代", "五代", "宋代", "元代", "清代"};
        List<String> ordered = new ArrayList<>();
        for (String d : navOrder) {
            if (dynasties.contains(d)) ordered.add(d);
        }

        categories.clear();
        categoryIcons.clear();
        categories.add("全部");
        categoryIcons.add("📚");
        for (String d : ordered) {
            categories.add(d);
            categoryIcons.add(getDynastyIcon(d));
        }
    }

    private String getDynastyIcon(String dynasty) {
        switch (dynasty) {
            case "先秦": case "春秋": case "春秋战国": return "📜";
            case "魏晋": return "🍂";
            case "唐代": return "🏯";
            case "五代": return "🌊";
            case "宋代": return "🎋";
            case "元代": return "🐎";
            case "清代": return "🏮";
            default: return "📖";
        }
    }
}
