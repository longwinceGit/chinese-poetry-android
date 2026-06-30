package com.poetry.ui.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private TextView tvTitle, tvAuthor, tvLines, tvPinyin, tvFavText;
    private View btnBack, btnVoice, btnFavorite, btnQuiz, btnFavHeader;

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
        btnFavorite = v.findViewById(R.id.btn_favorite);
        tvFavText = v.findViewById(R.id.tv_fav_text);
        btnQuiz = v.findViewById(R.id.btn_quiz);
        btnFavHeader = v.findViewById(R.id.btn_favorite_header);
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

        btnFavorite.setOnClickListener(v -> viewModel.toggleFavorite());
        btnFavHeader.setOnClickListener(v -> viewModel.toggleFavorite());
    }

    private void observeData() {
        viewModel.getIsFavorite().observe(getViewLifecycleOwner(), fav -> {
            if (fav != null && fav) {
                tvFavText.setText("❤️ 已收藏");
                if (btnFavHeader instanceof TextView) {
                    ((TextView) btnFavHeader).setText("❤️");
                }
            } else {
                tvFavText.setText("🤍 收藏");
                if (btnFavHeader instanceof TextView) {
                    ((TextView) btnFavHeader).setText("🤍");
                }
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }
}
