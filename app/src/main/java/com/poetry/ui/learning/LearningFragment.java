package com.poetry.ui.learning;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.poetry.R;
import com.poetry.data.UserProfile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class LearningFragment extends Fragment {

    private TextView tvStreakCount, tvStreakSub, tvLearnedCount, tvLevel;
    private LinearLayout llCalendar, llTasks;
    private LearningViewModel viewModel;
    private boolean[] todayTasks = new boolean[]{false, false, false};

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_learning, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(this).get(LearningViewModel.class);
        observeData();
        viewModel.loadData();
    }

    private void initViews(View v) {
        tvStreakCount = v.findViewById(R.id.tv_streak_count);
        tvStreakSub = v.findViewById(R.id.tv_streak_sub);
        tvLearnedCount = v.findViewById(R.id.tv_learned_count);
        tvLevel = v.findViewById(R.id.tv_level);
        llCalendar = v.findViewById(R.id.ll_calendar);
        llTasks = v.findViewById(R.id.ll_tasks);
    }

    private void observeData() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) updateStats(profile);
        });

        viewModel.getLearnedCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) tvLearnedCount.setText(String.valueOf(count));
        });

        viewModel.getCheckinDates().observe(getViewLifecycleOwner(), this::buildCalendar);

        viewModel.getTodayTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                todayTasks = tasks;
                buildTasks();
            }
        });
    }

    private void updateStats(UserProfile profile) {
        tvStreakCount.setText(String.valueOf(profile.streak));
        tvStreakSub.setText("连续" + profile.streak + "天");
        tvLevel.setText(String.valueOf(profile.level));
    }

    /**
     * 构建本周打卡日历（7天），基于真实签到数据
     */
    private void buildCalendar(Set<String> checkinDates) {
        llCalendar.removeAllViews();
        LocalDate today = LocalDate.now();
        // 计算本周一
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);

        String[] weekLabels = {"一", "二", "三", "四", "五", "六", "日"};

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            String dateStr = day.format(DATE_FMT);
            boolean isChecked = checkinDates != null && checkinDates.contains(dateStr);
            boolean isToday = day.equals(today);
            boolean isPast = day.isBefore(today) && !isToday;

            LinearLayout dayItem = new LinearLayout(requireContext());
            dayItem.setOrientation(LinearLayout.VERTICAL);
            dayItem.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            dayItem.setLayoutParams(params);

            // 星期标签
            TextView label = new TextView(requireContext());
            label.setText(weekLabels[i]);
            label.setTextSize(11);
            label.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant));
            label.setGravity(android.view.Gravity.CENTER);
            dayItem.addView(label);

            // 日期圆圈
            TextView dayNum = new TextView(requireContext());
            dayNum.setText(day.format(DAY_FMT));
            dayNum.setTextSize(14);
            dayNum.setGravity(android.view.Gravity.CENTER);

            int daySize = (int) (36 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams numParams = new LinearLayout.LayoutParams(daySize, daySize);
            numParams.setMargins(0, 6, 0, 0);

            if (isToday) {
                dayNum.setBackgroundResource(R.drawable.bg_checkin_today);
                dayNum.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_primary));
            } else if (isChecked) {
                // 已签到（过往日期且已签到）
                dayNum.setBackgroundResource(R.drawable.bg_checkin_done);
                dayNum.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_primary));
                dayNum.setText("✓");
            } else if (isPast) {
                // 过往日期但未签到（断签）
                dayNum.setBackgroundResource(R.drawable.bg_checkin_future);
                dayNum.setTextColor(ContextCompat.getColor(requireContext(), R.color.outline));
            } else {
                // 未来日期
                dayNum.setBackgroundResource(R.drawable.bg_checkin_future);
                dayNum.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant));
            }

            dayNum.setLayoutParams(numParams);
            dayItem.addView(dayNum);

            // 签到状态小标记
            if (isChecked && !isToday) {
                TextView checkMark = new TextView(requireContext());
                checkMark.setText("已签");
                checkMark.setTextSize(10);
                checkMark.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_500));
                checkMark.setGravity(android.view.Gravity.CENTER);
                dayItem.addView(checkMark);
            }

            llCalendar.addView(dayItem);
        }
    }

    /**
     * 构建每日任务列表
     */
    private void buildTasks() {
        llTasks.removeAllViews();

        String[] taskNames = {
            getString(R.string.learn_task_poem),
            getString(R.string.learn_task_quiz),
            getString(R.string.learn_task_game)
        };

        for (int i = 0; i < taskNames.length; i++) {
            MaterialCardView card = new MaterialCardView(requireContext());
            card.setCardElevation(0);
            card.setStrokeWidth(1);
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.outline_variant));
            card.setRadius(12);
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_variant));

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 12, 16, 12);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // 图标
            TextView icon = new TextView(requireContext());
            icon.setText(getTaskEmoji(i));
            icon.setTextSize(20);
            row.addView(icon);

            // 名称
            TextView name = new TextView(requireContext());
            name.setText(taskNames[i]);
            name.setTextSize(14);
            name.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface));
            name.setPadding(12, 0, 0, 0);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            name.setLayoutParams(nameParams);
            row.addView(name);

            // 状态 — 基于真实数据
            TextView status = new TextView(requireContext());
            boolean done = (i < todayTasks.length) && todayTasks[i];
            status.setText(done ? getString(R.string.learn_task_done) : "+10");
            status.setTextSize(12);
            status.setTextColor(done
                ? ContextCompat.getColor(requireContext(), R.color.outline)
                : ContextCompat.getColor(requireContext(), R.color.score_gold));
            row.addView(status);

            card.addView(row);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) cardParams.setMargins(0, 8, 0, 0);
            card.setLayoutParams(cardParams);

            llTasks.addView(card);
        }
    }

    private String getTaskEmoji(int index) {
        switch (index) {
            case 0: return "📖";
            case 1: return "✏️";
            case 2: return "🎮";
            default: return "✅";
        }
    }
}
