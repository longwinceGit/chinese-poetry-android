package com.poetry.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.poetry.R;
import com.poetry.data.UserProfile;

import java.util.List;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;

    private TextView tvLevelName, tvPoints, tvTodayLearned, tvTodayQuiz, tvTodayStreak;
    private TextView tvTotalLearned, tvTotalFav, tvTotalGames;
    private ProgressBar progressLevel;
    private LinearLayout weeklyChart;
    private View btnBack;

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        observeData();
        viewModel.loadProfile();
    }

    private void initViews(View v) {
        btnBack = v.findViewById(R.id.btn_profile_back);
        tvLevelName = v.findViewById(R.id.tv_level_name);
        progressLevel = v.findViewById(R.id.progress_level);
        tvPoints = v.findViewById(R.id.tv_points);
        tvTodayLearned = v.findViewById(R.id.tv_today_learned);
        tvTodayQuiz = v.findViewById(R.id.tv_today_quiz);
        tvTodayStreak = v.findViewById(R.id.tv_today_streak);
        tvTotalLearned = v.findViewById(R.id.tv_total_learned);
        tvTotalFav = v.findViewById(R.id.tv_total_fav);
        tvTotalGames = v.findViewById(R.id.tv_total_games);
        weeklyChart = v.findViewById(R.id.weekly_chart);

        btnBack.setOnClickListener(v2 -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void observeData() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) updateProfile(profile);
        });

        viewModel.getTodayLearned().observe(getViewLifecycleOwner(), v -> {
            tvTodayLearned.setText(String.valueOf(v != null ? v : 0));
        });

        viewModel.getTodayQuiz().observe(getViewLifecycleOwner(), v -> {
            tvTodayQuiz.setText(String.valueOf(v != null ? v : 0));
        });

        viewModel.getTotalLearned().observe(getViewLifecycleOwner(), v -> {
            tvTotalLearned.setText(String.valueOf(v != null ? v : 0));
        });

        viewModel.getTotalFav().observe(getViewLifecycleOwner(), v -> {
            tvTotalFav.setText(String.valueOf(v != null ? v : 0));
        });

        viewModel.getTotalGames().observe(getViewLifecycleOwner(), v -> {
            tvTotalGames.setText(String.valueOf(v != null ? v : 0));
        });

        viewModel.getWeeklyStats().observe(getViewLifecycleOwner(), this::renderWeeklyChart);
    }

    private void updateProfile(UserProfile profile) {
        tvLevelName.setText(viewModel.getLevelName(profile.level));
        progressLevel.setProgress(viewModel.getLevelProgress(profile.totalPoints));
        tvPoints.setText("积分：" + profile.totalPoints);
        tvTodayStreak.setText(profile.streak + "天");
    }

    private void renderWeeklyChart(List<Integer> stats) {
        if (stats == null || stats.isEmpty()) return;
        weeklyChart.removeAllViews();
        int max = 1;
        for (int v : stats) if (v > max) max = v;
        String[] days = {"一", "二", "三", "四", "五", "六", "日"};

        for (int i = 0; i < 7; i++) {
            LinearLayout col = new LinearLayout(requireContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, -2);
            colLp.weight = 1;
            col.setLayoutParams(colLp);

            int count = stats.get(i);
            int barHeight = Math.max(8, count * 60 / max);

            View bar = new View(requireContext());
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(24, barHeight);
            barLp.setMargins(0, 0, 0, 4);
            bar.setLayoutParams(barLp);
            bar.setBackgroundResource(R.drawable.bg_chip_active);
            bar.setAlpha(0.3f + 0.7f * count / Math.max(1, max));

            TextView label = new TextView(requireContext());
            label.setText(days[i]);
            label.setTextSize(10);
            label.setTextColor(getResources().getColor(R.color.text_light));

            col.addView(bar);
            col.addView(label);
            weeklyChart.addView(col);
        }
    }
}
