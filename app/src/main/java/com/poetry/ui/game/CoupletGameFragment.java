package com.poetry.ui.game;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.poetry.R;
import com.poetry.domain.GameEngine;

import java.util.ArrayList;
import java.util.List;

public class CoupletGameFragment extends Fragment {

    private GameViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvScore, tvProgress, tvQuestion, tvFeedback;
    private LinearLayout llOptions;
    private View btnNext;
    private final List<MaterialButton> optionButtons = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game_couplet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);
        observeData();
        viewModel.startCoupletGame();
    }

    private void initViews(View v) {
        tvScore = v.findViewById(R.id.tv_score);
        tvProgress = v.findViewById(R.id.tv_progress);
        tvQuestion = v.findViewById(R.id.tv_question);
        tvFeedback = v.findViewById(R.id.tv_feedback);
        llOptions = v.findViewById(R.id.ll_options);
        btnNext = v.findViewById(R.id.btn_next);
        btnNext.setVisibility(View.GONE);
    }

    private void observeData() {
        viewModel.getCoupletRounds().observe(getViewLifecycleOwner(), rounds -> {
            if (rounds != null && !rounds.isEmpty()) {
                renderRound(rounds, 0);
            }
        });

        viewModel.getCurrentRound().observe(getViewLifecycleOwner(), round -> {
            List<GameEngine.CoupletRound> rounds = viewModel.getCoupletRounds().getValue();
            if (round != null && rounds != null && round < rounds.size()) {
                renderRound(rounds, round);
            }
        });

        viewModel.getCoupletScore().observe(getViewLifecycleOwner(), score -> {
            if (score != null) tvScore.setText(getString(R.string.game_score) + ": " + score);
        });

        viewModel.getRoundResult().observe(getViewLifecycleOwner(), correct -> {
            if (correct != null) {
                int color = correct ? R.color.answer_correct : R.color.answer_wrong;
                tvFeedback.setText(correct ? "✅ 回答正确！" : "❌ 再接再厉！");
                tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), color));
                tvFeedback.setVisibility(View.VISIBLE);

                handler.postDelayed(() -> {
                    tvFeedback.setVisibility(View.GONE);
                    viewModel.nextCoupletRound();
                    Boolean finished = viewModel.getCoupletFinished().getValue();
                    if (finished != null && finished) {
                        showResult();
                    }
                }, 1500);
            }
        });

        viewModel.getCoupletFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished != null && finished) showResult();
        });
    }

    private void renderRound(List<GameEngine.CoupletRound> rounds, int index) {
        if (index >= rounds.size()) return;
        GameEngine.CoupletRound round = rounds.get(index);
        tvProgress.setText(getString(R.string.game_progress, index + 1, rounds.size()));
        tvQuestion.setText(round.givenLine);
        tvFeedback.setVisibility(View.GONE);

        // Build option buttons dynamically
        llOptions.removeAllViews();
        optionButtons.clear();

        for (int i = 0; i < round.options.size(); i++) {
            MaterialButton btn = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(round.options.get(i));
            btn.setTextSize(16);
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface));
            btn.setStrokeColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.outline_variant)));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) params.setMargins(0, 10, 0, 0);
            btn.setLayoutParams(params);

            final int idx = i;
            btn.setOnClickListener(v -> {
                disableOptions();
                viewModel.answerCouplet(idx);
            });

            llOptions.addView(btn);
            optionButtons.add(btn);
        }
    }

    private void disableOptions() {
        for (MaterialButton btn : optionButtons) btn.setEnabled(false);
    }

    private void showResult() {
        Integer score = viewModel.getCoupletScore().getValue();
        tvFeedback.setText("🎉 游戏结束！你得了 " + (score != null ? score : 0) + " 分！");
        tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.score_gold));
        tvFeedback.setVisibility(View.VISIBLE);

        handler.postDelayed(() -> requireActivity().onBackPressed(), 3000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
