package com.poetry.ui.game;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.poetry.R;
import com.poetry.domain.GameEngine;

import java.util.ArrayList;

public class MatchGameFragment extends Fragment {

    private GameViewModel viewModel;
    private Handler handler = new Handler(Looper.getMainLooper());

    private GridView gridView;
    private TextView tvProgress, tvFeedback;
    private View btnBack;

    private MatchCardAdapter adapter;
    private GameEngine.MatchCard firstSelected;

    public static MatchGameFragment newInstance() {
        return new MatchGameFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game_match, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);
        observeData();
        viewModel.startMatchGame();
    }

    private void initViews(View v) {
        btnBack = v.findViewById(R.id.btn_match_back);
        gridView = v.findViewById(R.id.grid_match_cards);
        tvProgress = v.findViewById(R.id.tv_match_progress);
        tvFeedback = v.findViewById(R.id.tv_match_feedback);

        btnBack.setOnClickListener(v2 -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void observeData() {
        viewModel.getMatchGame().observe(getViewLifecycleOwner(), game -> {
            if (game != null) {
                adapter = new MatchCardAdapter(getContext(), game.cards, this::onCardClick);
                gridView.setAdapter(adapter);
            }
        });

        viewModel.getMatchedCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                tvProgress.setText(count + "/" + (viewModel.getMatchGame().getValue() != null ? viewModel.getMatchGame().getValue().totalPairs : 6));
            }
        });

        viewModel.getMatchFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished != null && finished) {
                Integer attempts = viewModel.getMatchAttempts().getValue();
                tvFeedback.setText("🎉 全部配对成功！共尝试 " + (attempts != null ? attempts : 0) + " 次");
                tvFeedback.setTextColor(getResources().getColor(R.color.purple));
                handler.postDelayed(() -> {
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                }, 3000);
            }
        });
    }

    private void onCardClick(GameEngine.MatchCard card) {
        if (card.matched) return;

        if (firstSelected == null) {
            firstSelected = card;
            return;
        }

        if (firstSelected == card) return;

        boolean success = viewModel.tryMatch(firstSelected, card);

        if (success) {
            tvFeedback.setText("✅ 配对成功！");
            tvFeedback.setTextColor(getResources().getColor(R.color.teal));
        } else {
            tvFeedback.setText("❌ 配对失败，再试一次");
            tvFeedback.setTextColor(getResources().getColor(R.color.coral));
        }

        firstSelected = null;
        // 刷新 GridView
        GameEngine.MatchGame game = viewModel.getMatchGame().getValue();
        if (game != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
