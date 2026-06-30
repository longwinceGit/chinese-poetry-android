package com.poetry.ui.learning;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.poetry.R;
import com.poetry.ui.adapter.AchievementAdapter;

public class LearningFragment extends Fragment {

    private LearningViewModel viewModel;
    private RecyclerView recyclerFav, recyclerAchievements;
    private AchievementAdapter achievementAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_learning, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(this).get(LearningViewModel.class);
        observeData();
    }

    private void initViews(View v) {
        recyclerFav = v.findViewById(R.id.recycler_favorites);
        recyclerFav.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerAchievements = v.findViewById(R.id.recycler_achievements);
        recyclerAchievements.setLayoutManager(new LinearLayoutManager(requireContext()));
        achievementAdapter = new AchievementAdapter();
        recyclerAchievements.setAdapter(achievementAdapter);
    }

    private void observeData() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) viewModel.loadAchievements();
        });

        viewModel.getFavorites().observe(getViewLifecycleOwner(), favs -> {
            if (favs != null) viewModel.loadAchievements();
        });

        viewModel.getAchievements().observe(getViewLifecycleOwner(), entries -> {
            if (entries != null) achievementAdapter.setEntries(entries);
        });
    }
}
