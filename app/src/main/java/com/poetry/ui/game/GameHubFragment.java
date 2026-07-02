package com.poetry.ui.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.card.MaterialCardView;
import com.poetry.R;

/**
 * 游戏大厅 Fragment，提供三个游戏入口卡片：
 * 对联游戏、连连看、诗词答题。每个卡片导航到对应的游戏页面。
 */
public class GameHubFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game_hub, container, false);
    }

    /**
     * 设置三个游戏入口卡片的点击导航事件：对联游戏、连连看、诗词答题。
     */
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
    }
}
