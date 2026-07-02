package com.poetry.ui.game;

import android.animation.ValueAnimator;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.poetry.R;
import com.poetry.domain.GameEngine;

public class MatchGameFragment extends Fragment {

    private GameViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private RecyclerView recyclerCards;
    private TextView tvProgress, tvTip;
    private MaterialButton btnRestart;
    private View layoutComplete;
    private TextView tvCompleteScore;

    private MatchCardAdapter adapter;
    private GameEngine.MatchCard firstSelected;    // 第一张选中的卡片
    private int firstSelectedPosition = -1;          // 第一张卡片的位置
    private boolean lockInput = false;               // 动画播放期间锁定输入

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
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);
        observeData();
        viewModel.startMatchGame();
    }

    private void initViews(View v) {
        recyclerCards = v.findViewById(R.id.recycler_cards);
        tvProgress = v.findViewById(R.id.tv_match_progress);
        tvTip = v.findViewById(R.id.tv_match_tip);
        btnRestart = v.findViewById(R.id.btn_match_restart);
        layoutComplete = v.findViewById(R.id.layout_match_complete);
        tvCompleteScore = v.findViewById(R.id.tv_match_complete_score);

        recyclerCards.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        adapter = new MatchCardAdapter(this::onCardClick);
        recyclerCards.setAdapter(adapter);

        btnRestart.setOnClickListener(v2 -> {
            firstSelected = null;
            firstSelectedPosition = -1;
            lockInput = false;
            layoutComplete.setVisibility(View.GONE);
            tvTip.setVisibility(View.GONE);
            viewModel.startMatchGame();
        });
    }

    private void observeData() {
        // 卡片列表更新
        viewModel.getMatchCards().observe(getViewLifecycleOwner(), cards -> {
            if (cards != null) {
                adapter.setCards(cards);
            }
        });

        // 已配对数量
        viewModel.getMatchedCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                int total = viewModel.getMatchPairs();
                tvProgress.setText(getString(R.string.game_progress, count, total));
            }
        });

        // 配对成功提示（诗词名）
        viewModel.getMatchTip().observe(getViewLifecycleOwner(), tip -> {
            if (tip != null && !tip.isEmpty()) {
                tvTip.setText("✨ " + tip);
                tvTip.setVisibility(View.VISIBLE);
                // 3秒后自动隐藏
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(() -> tvTip.setVisibility(View.GONE), 3000);
            }
        });

        // 游戏完成
        viewModel.getMatchFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished != null && finished) {
                Integer attempts = viewModel.getMatchAttempts().getValue();
                int score = GameEngine.calcMatchScore(
                        attempts != null ? attempts : 0,
                        viewModel.getMatchPairs());
                tvCompleteScore.setText("得分 " + score + "  ·  尝试 " + (attempts != null ? attempts : 0) + " 次");
                layoutComplete.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(),
                        "🎉 全部消除！共尝试 " + (attempts != null ? attempts : 0) + " 次",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 卡片点击处理：
     * 1. 第一张：选中高亮
     * 2. 第二张同张：取消选中
     * 3. 第二张不同：尝试配对 → 成功消除/失败抖动
     */
    private void onCardClick(GameEngine.MatchCard card, int position) {
        if (card.matched || lockInput) return;

        if (firstSelected == null) {
            // === 选中第一张 ===
            firstSelected = card;
            firstSelectedPosition = position;
            viewModel.toggleSelect(card);
            return;
        }

        if (firstSelected == card) {
            // === 点击同一张，取消选中 ===
            viewModel.toggleSelect(card);
            firstSelected = null;
            firstSelectedPosition = -1;
            return;
        }

        // === 选中第二张，尝试配对 ===
        lockInput = true;
        viewModel.toggleSelect(card);  // 高亮第二张

        // 短暂延迟让用户看到选中状态，然后判断结果
        handler.postDelayed(() -> {
            int result = viewModel.tryMatch(firstSelected, card);

            if (result == 0) {
                // 配对成功 → 消除动画
                View v1 = recyclerCards.getLayoutManager() != null
                        ? recyclerCards.getLayoutManager().findViewByPosition(firstSelectedPosition) : null;
                View v2 = recyclerCards.getLayoutManager() != null
                        ? recyclerCards.getLayoutManager().findViewByPosition(position) : null;

                if (v1 != null && v2 != null) {
                    MatchCardAdapter.animateEliminate(v1, null);
                    MatchCardAdapter.animateEliminate(v2, () -> {
                        lockInput = false;
                    });
                } else {
                    lockInput = false;
                }
            } else if (result == 1) {
                // 配对失败 → 抖动动画
                View v1 = recyclerCards.getLayoutManager() != null
                        ? recyclerCards.getLayoutManager().findViewByPosition(firstSelectedPosition) : null;
                View v2 = recyclerCards.getLayoutManager() != null
                        ? recyclerCards.getLayoutManager().findViewByPosition(position) : null;

                if (v1 != null) MatchCardAdapter.animateShake(v1);
                if (v2 != null) MatchCardAdapter.animateShake(v2);

                handler.postDelayed(() -> lockInput = false, 300);
            } else {
                lockInput = false;
            }

            firstSelected = null;
            firstSelectedPosition = -1;
        }, 200);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}
