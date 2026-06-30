package com.poetry.ui.detail;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.poetry.R;
import com.poetry.data.model.Poem;
import com.poetry.util.PinyinHelper;
import com.poetry.util.TtsManager;

import java.util.List;

public class DetailFragment extends Fragment {

    private static final String ARG_POEM_ID = "poem_id";
    private static final String ARG_POEM_TITLE = "poem_title";
    private static final String ARG_POEM_AUTHOR = "poem_author";
    private static final String ARG_POEM_DYNASTY = "poem_dynasty";
    private static final String ARG_POEM_EMOJI = "poem_emoji";
    private static final String ARG_POEM_LINES = "poem_lines";

    private DetailViewModel viewModel;
    private TtsManager ttsManager;

    private LinearLayout llTitleContainer, llAuthorContainer, llPoemContainer;
    private TextView tvFavText, tvPinyinBtn;
    private View btnBack, btnVoice, btnPinyin, btnFavorite, btnQuiz;

    private Poem currentPoem;
    private boolean pinyinVisible = false;

    public static DetailFragment newInstance(Poem poem) {
        DetailFragment f = new DetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_POEM_ID, poem.id);
        args.putString(ARG_POEM_TITLE, poem.title);
        args.putString(ARG_POEM_AUTHOR, poem.author);
        args.putString(ARG_POEM_DYNASTY, poem.dynasty);
        args.putString(ARG_POEM_EMOJI, poem.emoji);
        if (poem.lines != null) {
            args.putStringArray(ARG_POEM_LINES, poem.lines);
        }
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        ttsManager = new TtsManager(requireContext());
        observeData();
        setupListeners();

        Bundle args = getArguments();
        if (args != null) {
            String id = args.getString(ARG_POEM_ID, "");
            String title = args.getString(ARG_POEM_TITLE, "");
            String author = args.getString(ARG_POEM_AUTHOR, "");
            String dynasty = args.getString(ARG_POEM_DYNASTY, "");
            String emoji = args.getString(ARG_POEM_EMOJI, "📖");
            String[] lines = args.getStringArray(ARG_POEM_LINES);
            Poem poem = new Poem(id, title, author, dynasty, dynasty, "", emoji, lines != null ? lines : new String[0]);
            renderPoem(poem);
            viewModel.loadPoem(poem);
        }
    }

    private void initViews(View v) {
        btnBack = v.findViewById(R.id.btn_back);
        llTitleContainer = v.findViewById(R.id.ll_title_container);
        llAuthorContainer = v.findViewById(R.id.ll_author_container);
        llPoemContainer = v.findViewById(R.id.ll_poem_container);
        btnVoice = v.findViewById(R.id.btn_voice);
        btnPinyin = v.findViewById(R.id.btn_pinyin);
        tvPinyinBtn = v.findViewById(R.id.tv_pinyin_text);
        btnFavorite = v.findViewById(R.id.btn_favorite);
        tvFavText = v.findViewById(R.id.tv_fav_text);
        btnQuiz = v.findViewById(R.id.btn_quiz);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        btnVoice.setOnClickListener(v -> {
            Poem p = currentPoem;
            if (p != null) {
                ttsManager.speakPoem(p.title, p.author, p.lines);
            }
        });

        btnPinyin.setOnClickListener(v -> viewModel.togglePinyin());

        btnFavorite.setOnClickListener(v -> {
            viewModel.toggleFavorite();
            springView(tvFavText);
        });

        btnQuiz.setOnClickListener(v -> {
            if (getActivity() != null) {
                com.poetry.ui.quiz.QuizFragment qf = new com.poetry.ui.quiz.QuizFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                         R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragment_container, qf)
                    .addToBackStack(null)
                    .commit();
            }
        });
    }

    private void observeData() {
        viewModel.getIsFavorite().observe(getViewLifecycleOwner(), fav -> {
            if (fav != null && fav) {
                tvFavText.setText("❤️");
                tvFavText.setTextColor(getResources().getColor(R.color.coral));
            } else {
                tvFavText.setText("🤍");
                tvFavText.setTextColor(getResources().getColor(R.color.text_secondary));
            }
        });

        viewModel.getShowPinyin().observe(getViewLifecycleOwner(), show -> {
            pinyinVisible = show != null && show;
            if (currentPoem != null) {
                rebuildAllViews(currentPoem, pinyinVisible);
            }
            if (pinyinVisible) {
                tvPinyinBtn.setTextColor(getResources().getColor(R.color.coral));
            } else {
                tvPinyinBtn.setTextColor(getResources().getColor(R.color.text_secondary));
            }
        });

        viewModel.getPoem().observe(getViewLifecycleOwner(), poem -> {
            if (poem != null && currentPoem == null) {
                currentPoem = poem;
            }
        });
    }

    private void renderPoem(Poem poem) {
        currentPoem = poem;
        rebuildAllViews(poem, pinyinVisible);
    }

    /**
     * 统一重建标题、作者朝代、诗句的视图（根据拼音开关状态）
     */
    private void rebuildAllViews(Poem poem, boolean showPinyin) {
        llTitleContainer.removeAllViews();
        llAuthorContainer.removeAllViews();
        llPoemContainer.removeAllViews();

        if (poem == null) return;

        int titleColor = ContextCompat.getColor(requireContext(), R.color.text_primary);
        int authorColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        int poemColor = ContextCompat.getColor(requireContext(), R.color.text_primary);
        int pinyinColor = ContextCompat.getColor(requireContext(), R.color.text_light);

        // 标题
        buildCharLine(llTitleContainer, poem.title, showPinyin,
                dpToPx(3), 26, 14, titleColor, pinyinColor, true);

        // 作者 · 朝代
        String authorText = poem.author + " · " + poem.dynasty;
        buildCharLine(llAuthorContainer, authorText, showPinyin,
                dpToPx(2), 14, 11, authorColor, pinyinColor, false);

        // 空行间距
        if (poem.lines != null && poem.lines.length > 0) {
            TextView spacer = new TextView(getContext());
            spacer.setHeight(dpToPx(8));
            llPoemContainer.addView(spacer);
        }

        // 诗句逐行
        int lineSpacing = dpToPx(6);
        for (String line : poem.lines) {
            LinearLayout lineWrap = new LinearLayout(getContext());
            lineWrap.setOrientation(LinearLayout.HORIZONTAL);
            lineWrap.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = lineSpacing;
            lineWrap.setLayoutParams(lp);
            buildCharLine(lineWrap, line, showPinyin,
                    dpToPx(2), showPinyin ? 18 : 20, 12, poemColor, pinyinColor, false);
            llPoemContainer.addView(lineWrap);
        }
    }

    /**
     * 构建一排逐字显示的区域（拼音在上，汉字在下）
     * @param parent   父容器
     * @param text     要显示的文本（含汉字、标点）
     * @param showPinyin 是否显示拼音
     * @param charSpacing 字符间距(px)
     * @param charSize   汉字字号(sp)
     * @param pinyinSize 拼音字号(sp)
     * @param charColor  汉字颜色
     * @param pinyinColor 拼音颜色
     * @param bold       汉字是否加粗
     */
    private void buildCharLine(LinearLayout parent, String text, boolean showPinyin,
                               int charSpacing, float charSize, float pinyinSize,
                               int charColor, int pinyinColor, boolean bold) {
        if (text == null || text.isEmpty()) return;

        List<String> pinyins = showPinyin ? PinyinHelper.toPinyinList(text) : null;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isPunct = isPunctuationChar(c) || c == ' ' || c == '·';

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

    private boolean isPunctuationChar(char c) {
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

    private void springView(View v) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.3f, 1f);
        scaleX.setDuration(400);
        scaleX.setInterpolator(new OvershootInterpolator(2f));
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.3f, 1f);
        scaleY.setDuration(400);
        scaleY.setInterpolator(new OvershootInterpolator(2f));
        scaleX.start();
        scaleY.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }
}
