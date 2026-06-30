package com.poetry.ui.game;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.poetry.R;
import com.poetry.domain.GameEngine;

import java.util.List;

public class CoupletGameFragment extends Fragment {

    private GameViewModel viewModel;
    private Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvTitle, tvScore, tvRound, tvGivenLine, tvFeedback;
    private TextView[] options = new TextView[4];
    private View btnBack;

    public static CoupletGameFragment newInstance() {
        return new CoupletGameFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game_couplet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);
        observeData();
        setupListeners();
        viewModel.startCoupletGame();
    }

    private void initViews(View v) {
        btnBack = v.findViewById(R.id.btn_game_back);
        tvTitle = v.findViewById(R.id.tv_game_title);
        tvScore = v.findViewById(R.id.tv_game_score);
        tvRound = v.findViewById(R.id.tv_round_info);
        tvGivenLine = v.findViewById(R.id.tv_given_line);
        tvFeedback = v.findViewById(R.id.tv_game_feedback);
        options[0] = v.findViewById(R.id.tv_option_1);
        options[1] = v.findViewById(R.id.tv_option_2);
        options[2] = v.findViewById(R.id.tv_option_3);
        options[3] = v.findViewById(R.id.tv_option_4);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            options[i].setOnClickListener(v -> {
                disableOptions();
                viewModel.answerCouplet(idx);
            });
        }
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
            if (score != null) tvScore.setText(score + "分");
        });

        viewModel.getRoundResult().observe(getViewLifecycleOwner(), correct -> {
            if (correct != null) {
                if (correct) {
                    tvFeedback.setText("✅ 回答正确！");
                    tvFeedback.setTextColor(getResources().getColor(R.color.teal));
                } else {
                    tvFeedback.setText("❌ 再接再厉！");
                    tvFeedback.setTextColor(getResources().getColor(R.color.coral));
                }
                // 2秒后自动进入下一题
                handler.postDelayed(() -> {
                    tvFeedback.setText("");
                    viewModel.nextCoupletRound();
                    Boolean finished = viewModel.getCoupletFinished().getValue();
                    if (finished != null && finished) {
                        showResult();
                    } else {
                        enableOptions();
                    }
                }, 1500);
            }
        });

        viewModel.getCoupletFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished != null && finished) {
                showResult();
            }
        });
    }

    private void renderRound(List<GameEngine.CoupletRound> rounds, int index) {
        if (index >= rounds.size()) return;
        GameEngine.CoupletRound round = rounds.get(index);
        tvRound.setText("第 " + (index + 1) + " / " + rounds.size() + " 题");
        tvGivenLine.setText(round.givenLine);
        for (int i = 0; i < options.length; i++) {
            options[i].setText(round.options.get(i));
            options[i].setEnabled(true);
            options[i].setBackgroundResource(R.drawable.bg_card);
        }
        tvFeedback.setText("");
    }

    private void disableOptions() {
        for (TextView tv : options) {
            tv.setEnabled(false);
        }
    }

    private void enableOptions() {
        for (TextView tv : options) {
            tv.setEnabled(true);
        }
    }

    private void showResult() {
        Integer score = viewModel.getCoupletScore().getValue();
        String msg = "🎉 游戏结束！你得了 " + (score != null ? score : 0) + " 分！";
        tvFeedback.setText(msg);
        tvFeedback.setTextColor(getResources().getColor(R.color.coral));
        handler.postDelayed(() -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        }, 3000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
