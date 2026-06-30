package com.poetry.ui.detail;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.poetry.R;
import com.poetry.data.model.Poem;
import com.poetry.util.TtsManager;

public class DetailFragment extends Fragment {

    private static final String ARG_POEM_ID = "poem_id";
    private static final String ARG_POEM_TITLE = "poem_title";
    private static final String ARG_POEM_AUTHOR = "poem_author";
    private static final String ARG_POEM_DYNASTY = "poem_dynasty";
    private static final String ARG_POEM_EMOJI = "poem_emoji";
    private static final String ARG_POEM_LINES = "poem_lines";

    private DetailViewModel viewModel;
    private TtsManager ttsManager;

    private TextView tvTitle, tvAuthor, tvLines, tvPinyin, tvFavText, tvPinyinBtn;
    private View btnBack, btnVoice, btnPinyin, btnFavorite, btnQuiz;

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
        tvTitle = v.findViewById(R.id.tv_detail_title);
        tvPinyin = v.findViewById(R.id.tv_detail_pinyin);
        tvAuthor = v.findViewById(R.id.tv_detail_author);
        tvLines = v.findViewById(R.id.tv_detail_lines);
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
            Poem p = viewModel.getPoem().getValue();
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
            if (show != null && show) {
                tvPinyin.setVisibility(View.VISIBLE);
                tvPinyinBtn.setTextColor(getResources().getColor(R.color.coral));
            } else {
                tvPinyin.setVisibility(View.GONE);
                tvPinyinBtn.setTextColor(getResources().getColor(R.color.text_secondary));
            }
        });

        viewModel.getPinyinText().observe(getViewLifecycleOwner(), text -> {
            if (text != null) {
                tvPinyin.setText(text);
            }
        });
    }

    private void renderPoem(Poem poem) {
        tvTitle.setText(poem.title);
        tvAuthor.setText(poem.author + " · " + poem.dynasty);

        StringBuilder linesBuilder = new StringBuilder();
        if (poem.lines != null) {
            for (String line : poem.lines) {
                linesBuilder.append(line).append("\n");
            }
        }
        tvLines.setText(linesBuilder.toString().trim());
        tvPinyin.setText("");
        tvPinyin.setVisibility(View.GONE);
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
