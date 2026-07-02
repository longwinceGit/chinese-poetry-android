package com.poetry.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.poetry.R;
import com.poetry.data.model.Poem;
import com.poetry.ui.adapter.PoemAdapter;

import java.util.List;

/**
 * 首页 Fragment，展示每日推荐卡片、分类筛选 Chip、诗词网格列表及搜索入口。
 * <p>
 * 核心交互：
 * <ul>
 *   <li>搜索输入 500ms 防抖处理</li>
 *   <li>诗词网格 3 列布局</li>
 *   <li>"加载更多" 按钮实现分页加载</li>
 *   <li>每日卡片点击跳转详情页</li>
 * </ul>
 */
public class HomeFragment extends Fragment {

    // Views
    private View dailyCard, loadingContainer, errorContainer, loadMoreArea, btnLoadMore;
    private View progressLoad, btnSearchClear, tvEmpty, tvNoMore, btnRetry;
    private TextView tvErrorIcon, tvErrorMessage;
    private TextView tvDailyEmoji, tvDailyTitle, tvDailyAuthor, tvDailyExcerpt;
    private TextView tvSectionTitle, tvPoemCount, tvLoadingMessage;
    private EditText etSearch;
    private ChipGroup chipCategories;
    private RecyclerView recyclerPoems;
    private ProgressBar progressLoading;

    private HomeViewModel viewModel;
    private PoemAdapter adapter;
    private NavController navController;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    /**
     * 创建 Fragment 视图，加载 fragment_home 布局。
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    /**
     * 视图创建完成回调，初始化控件、ViewModel、数据观察和事件监听。
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        observeData();
        viewModel.loadPoems();
        setupListeners();
    }

    /**
     * 销毁视图时取消待执行的搜索延时任务，防止内存泄漏。
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    /**
     * 初始化所有视图组件，包括 NavController、RecyclerView（3 列网格）、Adapter 等。
     */
    private void initViews(View v) {
        navController = Navigation.findNavController(v);

        dailyCard = v.findViewById(R.id.daily_card);
        tvDailyEmoji = v.findViewById(R.id.tv_daily_emoji);
        tvDailyTitle = v.findViewById(R.id.tv_daily_title);
        tvDailyAuthor = v.findViewById(R.id.tv_daily_author);
        tvDailyExcerpt = v.findViewById(R.id.tv_daily_excerpt);

        chipCategories = v.findViewById(R.id.chip_categories);
        tvSectionTitle = v.findViewById(R.id.tv_section_title);
        tvPoemCount = v.findViewById(R.id.tv_poem_count);
        tvEmpty = v.findViewById(R.id.tv_empty);
        tvNoMore = v.findViewById(R.id.tv_no_more);

        loadingContainer = v.findViewById(R.id.loading_container);
        progressLoading = v.findViewById(R.id.progress_loading);
        tvLoadingMessage = v.findViewById(R.id.tv_loading_message);

        loadMoreArea = v.findViewById(R.id.load_more_area);
        btnLoadMore = v.findViewById(R.id.btn_load_more);
        progressLoad = v.findViewById(R.id.progress_load_more);

        errorContainer = v.findViewById(R.id.error_container);
        tvErrorIcon = v.findViewById(R.id.tv_error_icon);
        tvErrorMessage = v.findViewById(R.id.tv_error_message);
        btnRetry = v.findViewById(R.id.btn_retry);

        etSearch = v.findViewById(R.id.et_search);
        btnSearchClear = v.findViewById(R.id.btn_search_clear);

        recyclerPoems = v.findViewById(R.id.recycler_poems);
        recyclerPoems.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        adapter = new PoemAdapter((poem, pos) -> navigateToDetail(poem));
        recyclerPoems.setAdapter(adapter);
    }

    /**
     * 观察 ViewModel 的 LiveData，包括加载状态、诗词列表、每日推荐、分类列表和总数，
     * 并据此更新 UI 状态。
     */
    private void observeData() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                loadingContainer.setVisibility(View.VISIBLE);
                recyclerPoems.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.GONE);
            } else {
                loadingContainer.setVisibility(View.GONE);
            }
        });

        viewModel.getLoadingMessage().observe(getViewLifecycleOwner(), msg ->
            tvLoadingMessage.setText(msg != null ? msg : getString(R.string.loading)));

        viewModel.getPoems().observe(getViewLifecycleOwner(), poems -> {
            if (poems == null) return;
            recyclerPoems.setVisibility(poems.isEmpty() ? View.GONE : View.VISIBLE);
            adapter.setPoems(poems);

            updateLoadMoreUI();
        });

        viewModel.getDailyPoem().observe(getViewLifecycleOwner(), this::updateDailyCard);

        viewModel.getCategories().observe(getViewLifecycleOwner(), this::setupCategoryChips);

        viewModel.getTotalCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                tvPoemCount.setText(getString(R.string.home_poems_count, count));
            }
        });

        // 错误状态观察
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                errorContainer.setVisibility(View.VISIBLE);
                recyclerPoems.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.GONE);
                tvErrorMessage.setText(error);
            } else {
                errorContainer.setVisibility(View.GONE);
            }
        });
    }

    /**
     * 设置事件监听器：
     * <ul>
     *   <li>每日卡片点击 → 跳转详情</li>
     *   <li>搜索输入防抖：输入停止 500ms 后触发搜索</li>
     *   <li>清除按钮：清空搜索框并退出搜索模式</li>
     *   <li>加载更多按钮：触发分页加载，等待 LiveData 更新后恢复按钮状态</li>
     * </ul>
     */
    private void setupListeners() {
        // 每日卡片点击 → 详情
        dailyCard.setOnClickListener(v -> {
            Poem poem = viewModel.getDailyPoem().getValue();
            if (poem != null) navigateToDetail(poem);
        });

        // 搜索：debounce 500ms
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                boolean hasText = s.length() > 0;
                btnSearchClear.setVisibility(hasText ? View.VISIBLE : View.GONE);
                searchRunnable = () -> viewModel.search(s.toString());
                searchHandler.postDelayed(searchRunnable, 500);
            }
        });

        btnSearchClear.setOnClickListener(v -> {
            etSearch.setText("");
            viewModel.search("");
        });

        // 加载更多
        btnLoadMore.setOnClickListener(v -> {
            progressLoad.setVisibility(View.VISIBLE);
            btnLoadMore.setVisibility(View.GONE);
            viewModel.loadMore();
            // 观察者收到 poems 更新后会调用 updateLoadMoreUI() 恢复按钮状态
        });

        // 重试按钮
        btnRetry.setOnClickListener(v -> {
            errorContainer.setVisibility(View.GONE);
            viewModel.loadPoems();
        });
    }

    /**
     * 更新每日推荐卡片视图，显示 emoji、标题、作者、朝代及首行摘录。
     * 摘录超过 20 字时自动截断并添加省略号。
     *
     * @param poem 每日推荐诗词对象
     */
    private void updateDailyCard(Poem poem) {
        if (poem == null) return;
        tvDailyEmoji.setText(poem.emoji != null ? poem.emoji : "📖");
        tvDailyTitle.setText(poem.title);
        tvDailyAuthor.setText(poem.author + " · " + poem.dynasty);
        String excerpt = poem.getFirstLine();
        if (excerpt != null && excerpt.length() > 20) {
            excerpt = excerpt.substring(0, 20) + "...";
        }
        tvDailyExcerpt.setText(excerpt);
        tvDailyExcerpt.setVisibility(excerpt != null ? View.VISIBLE : View.GONE);
    }

    /**
     * 动态创建分类 Chip 组，第一项默认选中，选中后触发 ViewModel 分类切换。
     *
     * @param categories 分类名称列表
     */
    private void setupCategoryChips(List<String> categories) {
        if (categories == null || categories.isEmpty()) return;
        chipCategories.removeAllViews();
        for (int i = 0; i < categories.size(); i++) {
            String cat = categories.get(i);
            Chip chip = new Chip(requireContext());
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (checked) viewModel.setCategory(cat);
            });
            chipCategories.addView(chip);
        }
    }

    /**
     * 跳转到诗词详情页，通过 Bundle 传递诗词的全部字段信息。
     *
     * @param poem 待查看详情的诗词对象
     */
    private void navigateToDetail(Poem poem) {
        if (poem == null) return;
        Bundle args = new Bundle();
        args.putString("poem_id", poem.id);
        args.putString("poem_title", poem.title);
        args.putString("poem_author", poem.author);
        args.putString("poem_dynasty", poem.dynasty);
        args.putString("poem_category", poem.category != null ? poem.category : "");
        args.putString("poem_tag", poem.tag != null ? poem.tag : "");
        args.putString("poem_emoji", poem.emoji != null ? poem.emoji : "");
        args.putStringArray("poem_lines", poem.lines);
        args.putString("poem_explanation", poem.explanation != null ? poem.explanation : "");
        navController.navigate(R.id.nav_detail, args);
    }

    /**
     * 更新"加载更多"区域 UI：
     * <ul>
     *   <li>计算已加载数量与总数，判断是否还有更多数据</li>
     *   <li>有更多数据时显示按钮并隐藏进度条</li>
     *   <li>搜索模式下结果全部展示后显示"没有更多"提示</li>
     * </ul>
     */
    private void updateLoadMoreUI() {
        int total = viewModel.getTotalCountValue();
        int loaded = (viewModel.getCurrentPage() + 1) * HomeViewModel.getPageSize();
        boolean hasMore = loaded < total;
        loadMoreArea.setVisibility(hasMore ? View.VISIBLE : View.GONE);
        if (hasMore) {
            // 恢复按钮状态
            btnLoadMore.setVisibility(View.VISIBLE);
            progressLoad.setVisibility(View.GONE);
        }
        tvNoMore.setVisibility(loaded >= total && viewModel.isSearchMode() ? View.VISIBLE : View.GONE);
    }
}
