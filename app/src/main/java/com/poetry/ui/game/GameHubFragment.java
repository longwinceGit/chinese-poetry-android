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

public class GameHubFragment extends Fragment {

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
    }
}
