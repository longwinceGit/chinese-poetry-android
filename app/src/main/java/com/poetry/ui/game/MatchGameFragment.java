package com.poetry.ui.game;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.poetry.R;
import com.poetry.domain.GameEngine;

public class MatchGameFragment extends Fragment {

    private GameViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private RecyclerView recyclerCards;
    private TextView tvProgress;
    private View btnRestart;

    private MatchCardAdapter adapter;
    private GameEngine.MatchCard firstSelected;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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
        recyclerCards = v.findViewById(R.id.recycler_cards);
        tvProgress = v.findViewById(R.id.tv_progress);
        btnRestart = v.findViewById(R.id.btn_restart);

        recyclerCards.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        btnRestart.setOnClickListener(v2 -> {
            firstSelected = null;
            viewModel.startMatchGame();
        });
    }

    private void observeData() {
        viewModel.getMatchGame().observe(getViewLifecycleOwner(), game -> {
            if (game != null) {
                adapter = new MatchCardAdapter(game.cards, this::onCardClick);
                recyclerCards.setAdapter(adapter);
            }
        });

        viewModel.getMatchedCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                GameEngine.MatchGame game = viewModel.getMatchGame().getValue();
                int total = game != null ? game.totalPairs : 6;
                tvProgress.setText(getString(R.string.game_progress, count, total));
            }
        });

        viewModel.getMatchFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished != null && finished) {
                Integer attempts = viewModel.getMatchAttempts().getValue();
                Toast.makeText(requireContext(),
                    "🎉 全部配对成功！共尝试 " + (attempts != null ? attempts : 0) + " 次",
                    Toast.LENGTH_LONG).show();
                handler.postDelayed(() -> requireActivity().onBackPressed(), 3000);
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
        Toast.makeText(requireContext(),
            success ? "✅ 配对成功！" : "❌ 再试试",
            Toast.LENGTH_SHORT).show();

        firstSelected = null;
        GameEngine.MatchGame game = viewModel.getMatchGame().getValue();
        if (game != null && adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
