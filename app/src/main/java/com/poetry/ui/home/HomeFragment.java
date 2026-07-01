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

public class HomeFragment extends Fragment {

    // Views
    private View dailyCard, loadingContainer, loadMoreArea, btnLoadMore;
    private View progressLoad, btnSearchClear, tvEmpty, tvNoMore;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        observeData();
        viewModel.loadPoems();
        setupListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

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

        etSearch = v.findViewById(R.id.et_search);
        btnSearchClear = v.findViewById(R.id.btn_search_clear);

        recyclerPoems = v.findViewById(R.id.recycler_poems);
        recyclerPoems.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        adapter = new PoemAdapter((poem, pos) -> navigateToDetail(poem));
        recyclerPoems.setAdapter(adapter);
    }

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
    }

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
    }

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
