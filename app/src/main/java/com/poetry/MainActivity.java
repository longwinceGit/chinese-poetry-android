package com.poetry;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * 童趣诗园 — 儿童古诗词学习 App（纯原生 UI）
 */
public class MainActivity extends AppCompatActivity {

    // ==================== UI 组件 ====================
    private View mainContent, loadingOverlay;
    private TextView tvLoading, tvSubtitle;
    private CardView dailyCard;
    private TextView tvDailyEmoji, tvDailyTitle, tvDailyAuthor;
    private LinearLayout categoryChips;
    private TextView tvPoemCount;
    private RecyclerView recyclerPoems;
    private PoemAdapter adapter;
    private LinearLayout loadMoreArea;
    private TextView btnLoadMore;
    private ProgressBar progressLoad;
    private View searchBar;
    private EditText etSearch;
    private View btnSearchToggle, btnSearchClose;
    private TextView tvSearchResultTitle;
    private View sectionAllTitle;
    private ConfettiView confettiView;

    // ==================== 数据 ====================
    private List<Poem> allPoems = new ArrayList<>();
    private List<Poem> displayedPoems = new ArrayList<>();
    private int currentPage = 0;
    private static final int PAGE_SIZE = 30;
    private String currentCategory = "全部";
    private boolean isSearchMode = false;
    private String searchQuery = "";

    // ==================== 状态 ====================
    private Poem dailyPoem;       // 当前今日推荐的诗词对象
    private Set<String> favorites = new HashSet<>();
    private Set<String> learned = new HashSet<>();
    private int learnCount = 0;
    private int todayLearnCount = 0;
    private TextToSpeech tts;
    private Random random = new Random();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // 分类定义（数据加载后动态构建）
    private List<String> categories = new ArrayList<>();
    private List<String> categoryIcons = new ArrayList<>();

    // SharedPreferences key
    private static final String PREFS_NAME = "poetry_prefs";
    private static final String KEY_FAVORITES = "favorites";
    private static final String KEY_LEARNED = "learned";
    private static final String KEY_LEARN_COUNT = "learn_count";
    private static final String KEY_TODAY_LEARN = "today_learn";
    private static final String KEY_STREAK = "streak";
    private static final String KEY_LAST_DATE = "last_date";
    private static final String KEY_DAILY_POEM = "daily_poem";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadState();
        setupListeners();
        loadPoems();
    }

    // ==================== 初始化视图 ====================
    private void initViews() {
        mainContent = findViewById(R.id.main_content);
        loadingOverlay = findViewById(R.id.loading_overlay);
        tvLoading = findViewById(R.id.tv_loading);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        dailyCard = findViewById(R.id.daily_card);
        tvDailyEmoji = findViewById(R.id.tv_daily_emoji);
        tvDailyTitle = findViewById(R.id.tv_daily_title);
        tvDailyAuthor = findViewById(R.id.tv_daily_author);
        categoryChips = findViewById(R.id.category_chips);
        tvPoemCount = findViewById(R.id.tv_poem_count);
        recyclerPoems = findViewById(R.id.recycler_poems);
        loadMoreArea = findViewById(R.id.load_more_area);
        btnLoadMore = findViewById(R.id.btn_load_more);
        progressLoad = findViewById(R.id.progress_load);
        searchBar = findViewById(R.id.search_bar);
        etSearch = findViewById(R.id.et_search);
        btnSearchToggle = findViewById(R.id.btn_search_toggle);
        btnSearchClose = findViewById(R.id.btn_search_close);
        tvSearchResultTitle = findViewById(R.id.tv_search_result_title);
        sectionAllTitle = findViewById(R.id.section_all_title);
        confettiView = findViewById(R.id.confetti_view);

        // RecyclerView 配置 — 3列网格
        GridLayoutManager glm = new GridLayoutManager(this, 3);
        recyclerPoems.setLayoutManager(glm);
        adapter = new PoemAdapter(this::onPoemClick);
        recyclerPoems.setAdapter(adapter);

        // TextToSpeech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.CHINESE);
                tts.setSpeechRate(0.85f);
            }
        });
    }

    // ==================== 事件监听 ====================
    private void setupListeners() {
        // 搜索按钮
        btnSearchToggle.setOnClickListener(v -> toggleSearch());
        btnSearchClose.setOnClickListener(v -> closeSearch());

        // 搜索输入
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                doSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 加载更多
        btnLoadMore.setOnClickListener(v -> loadMore());

        // 每日推荐卡片点击
        dailyCard.setOnClickListener(v -> {
            if (dailyPoem != null) {
                markLearned(dailyPoem);
                showDetailDialog(dailyPoem, 0);
            }
        });
    }

    // ==================== 加载状态 ====================
    private void loadState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        favorites = new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
        learned = new HashSet<>(prefs.getStringSet(KEY_LEARNED, new HashSet<>()));
        learnCount = prefs.getInt(KEY_LEARN_COUNT, 0);
        todayLearnCount = prefs.getInt(KEY_TODAY_LEARN, 0);
    }

    private void saveState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putStringSet(KEY_FAVORITES, new HashSet<>(favorites))
            .putStringSet(KEY_LEARNED, new HashSet<>(learned))
            .putInt(KEY_LEARN_COUNT, learnCount)
            .putInt(KEY_TODAY_LEARN, todayLearnCount)
            .apply();
    }

    // ==================== 加载诗词 ====================
    private void loadPoems() {
        new AsyncTask<Void, String, List<Poem>>() {
            @Override
            protected void onPreExecute() {
                loadingOverlay.setVisibility(View.VISIBLE);
                mainContent.setVisibility(View.GONE);
                tvLoading.setText("正在加载诗词...");
            }

            @Override
            protected List<Poem> doInBackground(Void... params) {
                try {
                    return PoemLoader.loadAll(getAssets());
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Poem> result) {
                loadingOverlay.setVisibility(View.GONE);
                if (result == null || result.isEmpty()) {
                    Toast.makeText(MainActivity.this, "诗词加载失败，请重试", Toast.LENGTH_LONG).show();
                    return;
                }
                allPoems = result;
                tvSubtitle.setText("共 " + String.format("%,d", allPoems.size()) + " 首古诗");
                mainContent.setVisibility(View.VISIBLE);

                setupCategories();
                checkStreakAndDaily();
                showPage(0, currentCategory);
            }
        }.execute();
    }

    // ==================== 连续学习 & 每日推荐 ====================
    private void checkStreakAndDaily() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String today = dateFormat.format(new Date());
        String lastDate = prefs.getString(KEY_LAST_DATE, "");
        int streak = prefs.getInt(KEY_STREAK, 0);

        if (!today.equals(lastDate)) {
            // 新的一天
            if (isYesterday(lastDate, today)) {
                streak++;
            } else {
                streak = 1;
            }
            todayLearnCount = 0;
            prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_STREAK, streak)
                .putInt(KEY_TODAY_LEARN, 0)
                .apply();

            // 选每日推荐
            selectDailyPoem();
        } else {
            // 同一天，恢复每日推荐
            String savedId = prefs.getString(KEY_DAILY_POEM, "");
            Poem found = findPoemById(savedId);
            if (found != null) {
                showDailyPoem(found);
            } else {
                selectDailyPoem();
            }
        }

        // 连续学习 >= 7天提示
        if (streak >= 7 && streak % 7 == 0) {
            Toast.makeText(this, "🎉 你已经连续学习 " + streak + " 天啦！太棒了！", Toast.LENGTH_LONG).show();
        }
    }

    private void selectDailyPoem() {
        if (allPoems.isEmpty()) return;
        Poem poem = allPoems.get(random.nextInt(allPoems.size()));
        showDailyPoem(poem);
        // 保存
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putString(KEY_DAILY_POEM, poem.id).apply();
    }

    private void showDailyPoem(Poem poem) {
        dailyPoem = poem;
        tvDailyEmoji.setText(poem.emoji);
        tvDailyTitle.setText(poem.title);
        tvDailyAuthor.setText(poem.author + " · " + poem.dynasty);
    }

    private boolean isYesterday(String lastDate, String today) {
        try {
            Date last = dateFormat.parse(lastDate);
            Date now = dateFormat.parse(today);
            long diff = now.getTime() - last.getTime();
            return diff > 0 && diff < 48 * 60 * 60 * 1000;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 分类标签 ====================
    private void setupCategories() {
        // 从已加载的诗词中提取所有朝代 + 统计数量
        Set<String> dynasties = new LinkedHashSet<>();
        java.util.Map<String, Integer> dynastyCounts = new java.util.HashMap<>();
        for (Poem p : allPoems) {
            if (p.dynasty != null && !p.dynasty.isEmpty()) {
                dynasties.add(p.dynasty);
                Integer cnt = dynastyCounts.get(p.dynasty);
                dynastyCounts.put(p.dynasty, cnt == null ? 1 : cnt + 1);
            }
        }
        // 按 nav.json 顺序排序
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

        categoryChips.removeAllViews();
        for (int i = 0; i < categories.size(); i++) {
            final String cat = categories.get(i);
            TextView chip = new TextView(this);

            // 显示朝代 + 诗词数量
            String countStr;
            if ("全部".equals(cat)) {
                countStr = formatCount(allPoems.size());
            } else {
                Integer cnt = dynastyCounts.get(cat);
                countStr = formatCount(cnt == null ? 0 : cnt);
            }
            chip.setText(categoryIcons.get(i) + " " + cat + " " + countStr);

            chip.setTextSize(13);
            chip.setPadding(20, 10, 20, 10);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(8, 0, 8, 0);
            chip.setLayoutParams(lp);

            if (cat.equals(currentCategory)) {
                chip.setBackgroundResource(R.drawable.bg_chip_active);
                chip.setTextColor(getResources().getColor(R.color.coral));
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip);
                chip.setTextColor(getResources().getColor(R.color.text_secondary));
            }

            chip.setOnClickListener(v -> {
                currentCategory = cat;
                isSearchMode = false;
                setupCategories();
                showPage(0, cat);
                closeSearch();
            });

            categoryChips.addView(chip);
        }
    }

    /** 格式化数字，>= 10000 用万为单位（保留1位小数） */
    private String formatCount(int n) {
        if (n >= 10000) {
            double w = n / 10000.0;
            return String.format("%.1f万首", w);
        }
        return n + "首";
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

    // ==================== 分页显示 ====================
    private void showPage(int page, String category) {
        // 筛选
        List<Poem> filtered;
        if ("全部".equals(category)) {
            filtered = new ArrayList<>(allPoems);
        } else {
            filtered = new ArrayList<>();
            for (Poem p : allPoems) {
                if (category.equals(p.category)) filtered.add(p);
            }
        }

        displayedPoems = filtered;
        currentPage = 0;

        // 更新计数
        tvPoemCount.setText(String.format("%,d首", displayedPoems.size()));
        tvSearchResultTitle.setVisibility(View.GONE);
        sectionAllTitle.setVisibility(View.VISIBLE);

        // 重置 RecyclerView
        adapter.setPoems(new ArrayList<>());

        // 加载第一页
        loadMoreInternal();
    }

    private void loadMore() {
        progressLoad.setVisibility(View.VISIBLE);
        btnLoadMore.setVisibility(View.GONE);
        recyclerPoems.postDelayed(() -> {
            loadMoreInternal();
            progressLoad.setVisibility(View.GONE);
            btnLoadMore.setVisibility(View.VISIBLE);
        }, 300);
    }

    private void loadMoreInternal() {
        if (isSearchMode) return;

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, displayedPoems.size());
        if (start >= displayedPoems.size()) {
            loadMoreArea.setVisibility(View.GONE);
            return;
        }

        List<Poem> page = displayedPoems.subList(start, end);
        adapter.addPoems(new ArrayList<>(page)); // new list to avoid subList issues
        currentPage++;

        if (end >= displayedPoems.size()) {
            loadMoreArea.setVisibility(View.GONE);
        } else {
            loadMoreArea.setVisibility(View.VISIBLE);
            btnLoadMore.setText("📖 加载更多（" + String.format("%,d", end) + " / " + String.format("%,d", displayedPoems.size()) + "）");
        }
    }

    // ==================== 搜索 ====================
    private void toggleSearch() {
        if (searchBar.getVisibility() == View.VISIBLE) {
            closeSearch();
        } else {
            searchBar.setVisibility(View.VISIBLE);
            etSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void closeSearch() {
        searchBar.setVisibility(View.GONE);
        etSearch.setText("");
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);

        if (isSearchMode) {
            isSearchMode = false;
            showPage(0, currentCategory);
        }
    }

    private void doSearch(String query) {
        searchQuery = query.trim();
        if (searchQuery.isEmpty()) {
            isSearchMode = false;
            showPage(0, currentCategory);
            return;
        }

        isSearchMode = true;
        String lower = searchQuery.toLowerCase();
        List<Poem> results = new ArrayList<>();
        for (Poem p : allPoems) {
            if (p.title.contains(searchQuery) || p.title.toLowerCase().contains(lower)
                || p.author.contains(searchQuery) || p.author.toLowerCase().contains(lower)
                || p.getFullText().contains(searchQuery)) {
                results.add(p);
            }
        }

        // 限制最多显示500条搜索结果
        if (results.size() > 500) {
            results = results.subList(0, 500);
        }

        adapter.setPoems(results);
        tvSearchResultTitle.setVisibility(View.VISIBLE);
        tvSearchResultTitle.setText("搜索「" + searchQuery + "」找到 " + results.size() + " 首");
        sectionAllTitle.setVisibility(View.GONE);
        loadMoreArea.setVisibility(View.GONE);
    }

    // ==================== 诗词点击 → 详情弹窗 ====================
    private void onPoemClick(Poem poem, int position) {
        markLearned(poem);
        showDetailDialog(poem, position);
    }

    /** 标记诗词为已学（收藏/列表卡片/每日推荐通用） */
    private void markLearned(Poem poem) {
        if (!learned.contains(poem.id)) {
            learned.add(poem.id);
            learnCount++;
            todayLearnCount++;
            saveState();

            // 每学3首触发庆祝动画
            if (learnCount % 3 == 0) {
                confettiView.celebrate();
            }
        }
    }

    private void showDetailDialog(Poem poem, int position) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_poem_detail, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_detail_title);
        TextView tvAuthor = dialogView.findViewById(R.id.tv_detail_author);
        TextView tvLines = dialogView.findViewById(R.id.tv_detail_lines);
        View btnVoice = dialogView.findViewById(R.id.btn_voice);
        View btnFavorite = dialogView.findViewById(R.id.btn_favorite);
        TextView tvFavText = dialogView.findViewById(R.id.tv_fav_text);

        tvTitle.setText(poem.title);
        tvAuthor.setText(poem.author + " · " + poem.dynasty);

        // 诗句
        StringBuilder linesBuilder = new StringBuilder();
        if (poem.lines != null) {
            for (String line : poem.lines) {
                linesBuilder.append(line).append("\n");
            }
        }
        tvLines.setText(linesBuilder.toString().trim());

        // 收藏状态
        boolean isFav = favorites.contains(poem.id);
        tvFavText.setText(isFav ? "❤️ 已收藏" : "🤍 收藏");

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create();

        // 朗读
        btnVoice.setOnClickListener(v -> {
            if (tts != null && !tts.isSpeaking()) {
                String text = poem.title + "。" + poem.author + "。"
                    + linesBuilder.toString().replace("\n", "。");
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "poem_" + poem.id);
            }
        });

        // 收藏
        btnFavorite.setOnClickListener(v -> {
            if (favorites.contains(poem.id)) {
                favorites.remove(poem.id);
                tvFavText.setText("🤍 收藏");
            } else {
                favorites.add(poem.id);
                tvFavText.setText("❤️ 已收藏");
            }
            saveState();
        });

        dialog.show();
    }

    // ==================== 辅助方法 ====================
    private Poem findPoemById(String id) {
        for (Poem p : allPoems) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
