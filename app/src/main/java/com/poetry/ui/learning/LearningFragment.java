package com.poetry.ui.learning;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.poetry.R;
import com.poetry.data.LearningRecord;
import com.poetry.data.UserProfile;
import com.poetry.ui.adapter.AchievementAdapter;

public class LearningFragment extends Fragment {

    private LearningViewModel viewModel;

    private TextView tvLevelName, tvPoints, tvStreak, tvLearnedCount, tvFavCount;
    private ProgressBar progressLevel;
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
        tvLevelName = v.findViewById(R.id.tv_level_name);
        progressLevel = v.findViewById(R.id.progress_level);
        tvPoints = v.findViewById(R.id.tv_points);
        tvStreak = v.findViewById(R.id.tv_streak);
        tvLearnedCount = v.findViewById(R.id.tv_learned_count);
        tvFavCount = v.findViewById(R.id.tv_fav_count);
        recyclerFav = v.findViewById(R.id.recycler_favorites);
        recyclerFav.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerAchievements = v.findViewById(R.id.recycler_achievements);
        recyclerAchievements.setLayoutManager(new LinearLayoutManager(requireContext()));
        achievementAdapter = new AchievementAdapter();
        recyclerAchievements.setAdapter(achievementAdapter);
    }

    private void observeData() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                updateProfile(profile);
                viewModel.loadAchievements();
            }
        });

        viewModel.getLearnedCount().observe(getViewLifecycleOwner(), count -> {
            tvLearnedCount.setText(String.valueOf(count != null ? count : 0));
        });

        viewModel.getFavorites().observe(getViewLifecycleOwner(), favs -> {
            tvFavCount.setText(String.valueOf(favs != null ? favs.size() : 0));
        });

        viewModel.getAchievements().observe(getViewLifecycleOwner(), entries -> {
            if (entries != null) achievementAdapter.setEntries(entries);
        });
    }

    private void updateProfile(UserProfile profile) {
        String name = viewModel.getLevelName(profile.level);
        tvLevelName.setText(name);
        int progress = viewModel.getLevelProgress(profile.totalPoints);
        progressLevel.setProgress(progress);
        tvPoints.setText("积分：" + profile.totalPoints);
        tvStreak.setText(String.valueOf(profile.streak));
    }
}
