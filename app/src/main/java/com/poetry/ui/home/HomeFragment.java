package com.poetry.ui.home;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.poetry.R;
import com.poetry.data.model.Poem;
import com.poetry.ui.adapter.PoemAdapter;
import com.poetry.util.PinyinHelper;

import java.util.List;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;

    private TextView tvSubtitle, tvDailyEmoji, tvDailyPinyinToggle;
    private LinearLayout llDailyTitle, llDailyAuthor;
    private TextView tvPoemCount, tvSearchResultTitle, btnLoadMore;
    private LinearLayout categoryChips, loadMoreArea, searchBar;
    private EditText etSearch;
    private View btnSearchToggle, btnSearchClose, btnLearning, btnGame, sectionAllTitle;
    private CardView dailyCard;
    private RecyclerView recyclerPoems;
    private View progressLoad;
    private PoemAdapter adapter;

    private boolean dailyPinyinVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

    private void initViews(View v) {
        tvSubtitle = v.findViewById(R.id.tv_subtitle);
        dailyCard = v.findViewById(R.id.daily_card);
        tvDailyEmoji = v.findViewById(R.id.tv_daily_emoji);
        llDailyTitle = v.findViewById(R.id.ll_daily_title);
        llDailyAuthor = v.findViewById(R.id.ll_daily_author);
        tvDailyPinyinToggle = v.findViewById(R.id.tv_daily_pinyin_toggle);
        categoryChips = v.findViewById(R.id.category_chips);
        tvPoemCount = v.findViewById(R.id.tv_poem_count);
        recyclerPoems = v.findViewById(R.id.recycler_poems);
        loadMoreArea = v.findViewById(R.id.load_more_area);
        btnLoadMore = v.findViewById(R.id.btn_load_more);
        progressLoad = v.findViewById(R.id.progress_load);
        searchBar = v.findViewById(R.id.search_bar);
        etSearch = v.findViewById(R.id.et_search);
        btnSearchToggle = v.findViewById(R.id.btn_search_toggle);
        btnSearchClose = v.findViewById(R.id.btn_search_close);
        btnLearning = v.findViewById(R.id.btn_learning);
        btnGame = v.findViewById(R.id.btn_game);
        tvSearchResultTitle = v.findViewById(R.id.tv_search_result_title);
        sectionAllTitle = v.findViewById(R.id.section_all_title);

        GridLayoutManager glm = new GridLayoutManager(requireContext(), 3);
        recyclerPoems.setLayoutManager(glm);
        adapter = new PoemAdapter((poem, pos) -> {
            if (getActivity() instanceof OnPoemClickListener) {
                ((OnPoemClickListener) getActivity()).onPoemClick(poem);
            }
        });
        recyclerPoems.setAdapter(adapter);
    }

    private void setupListeners() {
        btnSearchToggle.setOnClickListener(v -> toggleSearch());
        btnSearchClose.setOnClickListener(v -> closeSearch());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                viewModel.search(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnLoadMore.setOnClickListener(v -> {
            progressLoad.setVisibility(View.VISIBLE);
            btnLoadMore.setVisibility(View.GONE);
            recyclerPoems.postDelayed(() -> {
                viewModel.loadMore();
                progressLoad.setVisibility(View.GONE);
                btnLoadMore.setVisibility(View.VISIBLE);
            }, 300);
        });

        dailyCard.setOnClickListener(v -> {
            Poem p = viewModel.getDailyPoem().getValue();
            if (p != null && getActivity() instanceof OnPoemClickListener) {
                ((OnPoemClickListener) getActivity()).onPoemClick(p);
            }
        });

        tvDailyPinyinToggle.setOnClickListener(v -> {
            dailyPinyinVisible = !dailyPinyinVisible;
            tvDailyPinyinToggle.setTextColor(dailyPinyinVisible
                    ? getResources().getColor(R.color.coral)
                    : getResources().getColor(R.color.text_secondary));
            Poem p = viewModel.getDailyPoem().getValue();
            if (p != null) {
                buildDailyCard(p, dailyPinyinVisible);
            }
        });

        btnLearning.setOnClickListener(v -> {
            if (getActivity() instanceof OnNavigateListener) {
                ((OnNavigateListener) getActivity()).openLearning();
            }
        });

        btnGame.setOnClickListener(v -> {
            if (getActivity() instanceof OnNavigateListener) {
                ((OnNavigateListener) getActivity()).openGameMenu();
            }
        });
    }

    private void observeData() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading) {
                tvSubtitle.setText("加载中...");
            } else {
                int total = viewModel.getTotalCountValue();
                tvSubtitle.setText("共 " + String.format("%,d", total) + " 首古诗");
            }
        });

        viewModel.getTotalCount().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                tvSubtitle.setText("共 " + String.format("%,d", total) + " 首古诗");
            }
        });

        viewModel.getPoems().observe(getViewLifecycleOwner(), poems -> {
            adapter.setPoems(poems);
            if (!viewModel.isSearchMode()) {
                int total = viewModel.getTotalCountValue();
                tvPoemCount.setText(String.format("%,d首", total));
                tvSearchResultTitle.setVisibility(View.GONE);
                sectionAllTitle.setVisibility(View.VISIBLE);
                int current = poems != null ? poems.size() : 0;
                boolean hasMore = current < total;
                loadMoreArea.setVisibility(hasMore ? View.VISIBLE : View.GONE);
                if (hasMore) {
                    btnLoadMore.setText("📖 加载更多（" + current + " / " + total + "）");
                }
            } else {
                int count = poems != null ? poems.size() : 0;
                tvSearchResultTitle.setVisibility(View.VISIBLE);
                tvSearchResultTitle.setText("搜索「" + viewModel.getSearchQuery() + "」找到 " + count + " 首");
                sectionAllTitle.setVisibility(View.GONE);
                loadMoreArea.setVisibility(View.GONE);
            }
        });

        viewModel.getDailyPoem().observe(getViewLifecycleOwner(), poem -> {
            if (poem != null) {
                tvDailyEmoji.setText(poem.emoji);
                buildDailyCard(poem, dailyPinyinVisible);
            }
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), cats -> {
            if (cats != null) {
                setupCategoryChips(cats, viewModel.getCategoryIcons().getValue());
            }
        });
    }

    private void setupCategoryChips(List<String> categories, List<String> icons) {
        if (icons == null) return;
        categoryChips.removeAllViews();
        for (int i = 0; i < categories.size(); i++) {
            final String cat = categories.get(i);
            TextView chip = new TextView(requireContext());
            chip.setText(icons.get(i) + " " + cat);
            chip.setTextSize(13);
            chip.setPadding(20, 10, 20, 10);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(8, 0, 8, 0);
            chip.setLayoutParams(lp);

            if (cat.equals(viewModel.getCurrentCategory())) {
                chip.setBackgroundResource(R.drawable.bg_chip_active);
                chip.setTextColor(getResources().getColor(R.color.coral));
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip);
                chip.setTextColor(getResources().getColor(R.color.text_secondary));
            }

            chip.setOnClickListener(v -> {
                viewModel.setCategory(cat);
                setupCategoryChips(categories, icons);
                closeSearch();
            });
            categoryChips.addView(chip);
        }
    }

    private void toggleSearch() {
        if (searchBar.getVisibility() == View.VISIBLE) {
            closeSearch();
        } else {
            searchBar.setVisibility(View.VISIBLE);
            etSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void closeSearch() {
        searchBar.setVisibility(View.GONE);
        etSearch.setText("");
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }

    /**
     * 构建每日推荐卡片的标题和作者（支持拼音切换）
     */
    private void buildDailyCard(Poem poem, boolean showPinyin) {
        llDailyTitle.removeAllViews();
        llDailyAuthor.removeAllViews();

        int titleColor = ContextCompat.getColor(requireContext(), R.color.text_primary);
        int authorColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        int pinyinColor = ContextCompat.getColor(requireContext(), R.color.text_light);

        // 标题（逐字拼音）
        buildCharLineInline(llDailyTitle, poem.title, showPinyin,
                dpToPx(2), 20, 13, titleColor, pinyinColor, true);

        // 作者 · 朝代
        String authorText = poem.author + " · " + poem.dynasty;
        buildCharLineInline(llDailyAuthor, authorText, showPinyin,
                dpToPx(1), 13, 10, authorColor, pinyinColor, false);
    }

    private void buildCharLineInline(LinearLayout parent, String text, boolean showPinyin,
                                     int charSpacing, float charSize, float pinyinSize,
                                     int charColor, int pinyinColor, boolean bold) {
        if (text == null || text.isEmpty()) return;

        java.util.List<String> pinyins = showPinyin ? PinyinHelper.toPinyinList(text) : null;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isPunct = isPunctChar(c) || c == ' ' || c == '·';

            LinearLayout charBlock = new LinearLayout(getContext());
            charBlock.setOrientation(LinearLayout.VERTICAL);
            charBlock.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (!isPunct) {
                bp.leftMargin = charSpacing;
                bp.rightMargin = charSpacing;
            }
            charBlock.setLayoutParams(bp);

            if (showPinyin && !isPunct && pinyins != null && i < pinyins.size()) {
                String py = pinyins.get(i);
                TextView tvPy = new TextView(getContext());
                tvPy.setText(py.isEmpty() ? " " : py);
                tvPy.setTextSize(pinyinSize);
                tvPy.setTextColor(pinyinColor);
                tvPy.setGravity(Gravity.CENTER);
                tvPy.setSingleLine(true);
                charBlock.addView(tvPy);
            } else if (showPinyin && isPunct) {
                TextView tvSpacer = new TextView(getContext());
                tvSpacer.setText(" ");
                tvSpacer.setTextSize(pinyinSize);
                tvSpacer.setSingleLine(true);
                charBlock.addView(tvSpacer);
            }

            TextView tvChar = new TextView(getContext());
            tvChar.setText(String.valueOf(c));
            tvChar.setTextSize(charSize);
            tvChar.setTextColor(charColor);
            tvChar.setGravity(Gravity.CENTER);
            tvChar.setSingleLine(true);
            tvChar.setLineSpacing(0, 1f);
            if (bold) {
                tvChar.setTypeface(tvChar.getTypeface(), android.graphics.Typeface.BOLD);
            }
            charBlock.addView(tvChar);

            parent.addView(charBlock);
        }
    }

    private boolean isPunctChar(char c) {
        return c == '，' || c == '。' || c == '、' || c == '；'
                || c == '：' || c == '？' || c == '！' || c == '"'
                || c == '"' || c == '\'' || c == '\''
                || c == '(' || c == ')' || c == '（' || c == '）'
                || c == ',' || c == '.' || c == '!' || c == '?'
                || c == ';' || c == ':';
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public interface OnPoemClickListener {
        void onPoemClick(Poem poem);
    }

    public interface OnNavigateListener {
        void openLearning();
        void openGameMenu();
    }
}
