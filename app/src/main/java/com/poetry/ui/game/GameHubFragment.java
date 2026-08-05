package com.poetry.ui.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.poetry.R;
import com.poetry.ui.game.GameHubViewModel.GameHubCard;
import com.poetry.util.GameSnapshot;

import java.util.List;

/**
 * 游戏大厅 Fragment —— 「今日挑战 + 成绩回廊」。
 *
 * <p>顶部标题 + 最近有趣视线；三张大游戏卡（对诗/消消乐/填空）展示历史最高星、
 * 最高分与今日角标（今日挑战 🔥 金色高亮 / 今天玩过 ✓）。点击卡片导航到对应游戏。
 */
public class GameHubFragment extends Fragment {

    private GameHubViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController nav = Navigation.findNavController(view);

        view.findViewById(R.id.card_couplet)
            .setOnClickListener(v -> nav.navigate(R.id.nav_game_couplet));
        view.findViewById(R.id.card_match)
            .setOnClickListener(v -> nav.navigate(R.id.nav_game_match));
        view.findViewById(R.id.card_quiz)
            .setOnClickListener(v -> nav.navigate(R.id.nav_quiz));
        view.findViewById(R.id.card_flyflower)
            .setOnClickListener(v -> nav.navigate(R.id.nav_game_flyflower));
        view.findViewById(R.id.card_author)
            .setOnClickListener(v -> nav.navigate(R.id.nav_game_author));

        // M9 容错续局：未完成局快照存在时显示「继续上次」横幅
        bindContinueBanner(nav);

        // M10 自动读题开关：持久化到 SharedPreferences("game_settings", key "auto_read"，默认开)
        SharedPreferences prefs = requireContext().getSharedPreferences("game_settings", Context.MODE_PRIVATE);
        MaterialSwitch switchAutoRead = view.findViewById(R.id.switch_auto_read);
        switchAutoRead.setChecked(prefs.getBoolean("auto_read", true));
        switchAutoRead.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("auto_read", isChecked).apply());

        viewModel = new ViewModelProvider(this).get(GameHubViewModel.class);

        viewModel.getCards().observe(getViewLifecycleOwner(), this::bindCards);
        viewModel.getRecall().observe(getViewLifecycleOwner(), this::bindRecall);

        viewModel.load();
    }

    /**
     * M9：绑定「继续上次」横幅（§8.5）。
     * <p>读取 {@link GameSnapshot#getGameType(Context)}，非 null 时显示横幅并映射到对应游戏；
     * 点击后清除快照并导航。无未完成局时横幅 GONE。
     */
    private void bindContinueBanner(NavController nav) {
        View banner = requireView().findViewById(R.id.card_continue);
        String gameType = GameSnapshot.getGameType(requireContext());
        if (gameType == null) {
            banner.setVisibility(View.GONE);
            return;
        }
        int nameRes;
        int destId;
        switch (gameType) {
            case GameHubViewModel.TYPE_COUPLET:
                nameRes = R.string.game_couplet;
                destId = R.id.nav_game_couplet;
                break;
            case GameHubViewModel.TYPE_MATCH:
                nameRes = R.string.game_match;
                destId = R.id.nav_game_match;
                break;
            case GameHubViewModel.TYPE_FLYFLOWER:
                nameRes = R.string.game_flyflower;
                destId = R.id.nav_game_flyflower;
                break;
            case GameHubViewModel.TYPE_AUTHOR:
                nameRes = R.string.game_author;
                destId = R.id.nav_game_author;
                break;
            case GameHubViewModel.TYPE_QUIZ:
            default:
                nameRes = R.string.game_quiz;
                destId = R.id.nav_quiz;
                break;
        }
        TextView hint = requireView().findViewById(R.id.tv_continue_hint);
        hint.setText(getString(R.string.continue_game_hint, getString(nameRes)));
        banner.setVisibility(View.VISIBLE);
        banner.setOnClickListener(v -> {
            // 点击继续 → 清除快照并导航到对应游戏
            GameSnapshot.clear(requireContext());
            nav.navigate(destId);
        });
    }

    private void bindCards(@Nullable List<GameHubCard> cards) {
        if (cards == null) return;
        for (GameHubCard card : cards) {
            bindCard(card);
        }
    }

    private void bindCard(GameHubCard card) {
        View root = requireView();
        int scoreId;
        int badgeId;
        int cardId;
        switch (card.gameType) {
            case GameHubViewModel.TYPE_COUPLET:
                scoreId = R.id.score_couplet;
                badgeId = R.id.badge_couplet;
                cardId = R.id.card_couplet;
                break;
            case GameHubViewModel.TYPE_MATCH:
                scoreId = R.id.score_match;
                badgeId = R.id.badge_match;
                cardId = R.id.card_match;
                break;
            case GameHubViewModel.TYPE_QUIZ:
                scoreId = R.id.score_quiz;
                badgeId = R.id.badge_quiz;
                cardId = R.id.card_quiz;
                break;
            case GameHubViewModel.TYPE_FLYFLOWER:
                scoreId = R.id.score_flyflower;
                badgeId = R.id.badge_flyflower;
                cardId = R.id.card_flyflower;
                break;
            case GameHubViewModel.TYPE_AUTHOR:
                scoreId = R.id.score_author;
                badgeId = R.id.badge_author;
                cardId = R.id.card_author;
                break;
            default:
                return;
        }

        TextView score = root.findViewById(scoreId);
        TextView badge = root.findViewById(badgeId);
        MaterialCardView cardView = root.findViewById(cardId);

        // 星标 + 最高分
        String starText = getString(R.string.game_star_progress, card.bestStars);
        String bestText = getString(R.string.game_best_score, card.bestScore);
        score.setText(starText + "  ·  " + bestText);
        score.setVisibility(View.VISIBLE);

        // 今日角标
        if (card.playedToday) {
            badge.setText(getString(R.string.game_today_played));
            badge.setTextColor(getColorCompat(R.color.on_surface_variant));
            badge.setBackgroundResource(R.drawable.bg_chip);
            badge.setVisibility(View.VISIBLE);
        } else if (card.isTodayChallenge) {
            badge.setText(getString(R.string.game_today_challenge));
            badge.setTextColor(getColorCompat(R.color.combo_fire));
            badge.setBackgroundResource(R.drawable.bg_chip);
            badge.setVisibility(View.VISIBLE);
            // 今日挑战卡高亮背景
            cardView.setStrokeColor(getColorCompat(R.color.combo_fire));
            cardView.setStrokeWidth(dp(1));
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private void bindRecall(@Nullable String gameType) {
        if (gameType == null) {
            requireView().findViewById(R.id.tv_game_recall).setVisibility(View.GONE);
            return;
        }
        int nameRes;
        switch (gameType) {
            case GameHubViewModel.TYPE_COUPLET: nameRes = R.string.game_couplet; break;
            case GameHubViewModel.TYPE_MATCH:   nameRes = R.string.game_match;   break;
            case GameHubViewModel.TYPE_QUIZ:    nameRes = R.string.game_quiz;    break;
            case GameHubViewModel.TYPE_FLYFLOWER: nameRes = R.string.game_flyflower; break;
            case GameHubViewModel.TYPE_AUTHOR:  nameRes = R.string.game_author;  break;
            default: nameRes = R.string.game_couplet; break;
        }
        TextView recall = requireView().findViewById(R.id.tv_game_recall);
        recall.setText(getString(R.string.game_recall, getString(nameRes)));
        recall.setVisibility(View.VISIBLE);
    }

    private int getColorCompat(int resId) {
        return getResources().getColor(resId, requireContext().getTheme());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
