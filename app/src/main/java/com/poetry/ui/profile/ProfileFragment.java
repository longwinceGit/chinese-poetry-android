package com.poetry.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.card.MaterialCardView;
import com.poetry.R;
import com.poetry.data.UserProfile;
import com.poetry.domain.AchievementEngine;
import com.poetry.domain.ThemeManager;

import java.util.List;

/**
 * 个人中心 Fragment，展示用户等级、经验值、收藏数、已学数、成就和主题。
 * 核心功能：经验条计算（等级×100 满）、成就网格展示（已解锁/未解锁）、
 * 主题网格展示、分享应用功能。
 */
public class ProfileFragment extends Fragment {

    private TextView tvAvatar, tvLevelLabel, tvExp, tvFavoritesCount, tvLearnedCount;
    private TextView tvAchievementCount;
    private ProgressBar progressExp;
    private LinearLayout llAchievements;
    // 🔴 B5 修复：主题展示容器
    private LinearLayout llThemes;
    private TextView tvThemeCount;
    private MaterialCardView cardFavorites;
    private ProfileViewModel viewModel;

    /**
     * 创建 Fragment 视图，加载 fragment_profile 布局。
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    /**
     * 初始化视图和 ViewModel，设置分享按钮点击事件。
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        observeData();
        viewModel.loadData();

        view.findViewById(R.id.btn_share_app).setOnClickListener(v -> shareApp());
        cardFavorites.setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.nav_favorites));
    }

    /**
     * 初始化所有视图控件引用。
     */
    private void initViews(View v) {
        tvAvatar = v.findViewById(R.id.tv_avatar);
        tvLevelLabel = v.findViewById(R.id.tv_level_label);
        tvExp = v.findViewById(R.id.tv_exp);
        tvFavoritesCount = v.findViewById(R.id.tv_favorites_count);
        tvLearnedCount = v.findViewById(R.id.tv_learned_count);
        tvAchievementCount = v.findViewById(R.id.tv_achievement_count);
        progressExp = v.findViewById(R.id.progress_exp);
        llAchievements = v.findViewById(R.id.ll_achievements);
        llThemes = v.findViewById(R.id.ll_themes);
        tvThemeCount = v.findViewById(R.id.tv_theme_count);
        cardFavorites = v.findViewById(R.id.card_favorites);
    }

    /**
     * 观察 ViewModel 的数据变化，自动更新用户信息、收藏数和已学数量。
     */
    private void observeData() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) updateProfile(profile);
        });

        viewModel.getFavoriteCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) tvFavoritesCount.setText(String.valueOf(count));
        });

        viewModel.getLearnedCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) tvLearnedCount.setText(String.valueOf(count));
        });
    }

    /**
     * 更新用户信息展示：等级、经验条、头像、成就和主题。
     *
     * @param profile 用户信息
     */
    private void updateProfile(UserProfile profile) {
        // 等级
        tvLevelLabel.setText(getString(R.string.profile_level, profile.level));

        // 经验条
        int expNeeded = profile.level * 100;
        int currentExp = profile.totalPoints % expNeeded;
        progressExp.setMax(expNeeded);
        progressExp.setProgress(currentExp);
        tvExp.setText(getString(R.string.profile_exp, currentExp, expNeeded));

        // 头像
        updateAvatar(profile.level);

        // 成就
        buildAchievements(profile);

        // 🔴 B5 修复：主题展示
        buildThemes(profile);
    }

    /**
     * 根据等级更新头像 emoji 图标。
     *
     * @param level 用户等级
     */
    private void updateAvatar(int level) {
        String[] avatars = {"🌱", "📚", "📖", "✒️", "🎋", "🏯", "🧠", "🏆", "👑"};
        int idx = Math.min(level - 1, avatars.length - 1);
        if (idx >= 0) tvAvatar.setText(avatars[idx]);
    }

    /**
     * 构建成就展示网格，最多显示 6 个，已解锁用原图标，未解锁用锁图标并降低透明度。
     *
     * @param profile 用户信息
     */
    private void buildAchievements(UserProfile profile) {
        llAchievements.removeAllViews();
        List<AchievementEngine.AchievementDef> all = AchievementEngine.ALL_ACHIEVEMENTS;
        List<String> unlocked = AchievementEngine.getUnlockedIds(profile);

        int unlockedCount = unlocked.size();
        tvAchievementCount.setText(
            getString(R.string.profile_unlocked_count, unlockedCount, all.size()));

        // 最多显示 6 个
        int showCount = Math.min(6, all.size());
        for (int i = 0; i < showCount; i++) {
            AchievementEngine.AchievementDef def = all.get(i);
            boolean isUnlocked = unlocked.contains(def.id);

            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            item.setLayoutParams(params);

            // 图标
            TextView icon = new TextView(requireContext());
            icon.setText(isUnlocked ? def.icon : "🔒");
            icon.setTextSize(24);
            icon.setAlpha(isUnlocked ? 1.0f : 0.4f);
            icon.setGravity(android.view.Gravity.CENTER);
            item.addView(icon);

            // 名称
            TextView name = new TextView(requireContext());
            name.setText(def.name);
            name.setTextSize(10);
            name.setTextColor(ContextCompat.getColor(requireContext(),
                isUnlocked ? R.color.on_surface : R.color.outline));
            name.setGravity(android.view.Gravity.CENTER);
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            item.addView(name);

            llAchievements.addView(item);
        }
    }

    /**
     * 🔴 B5 修复：构建主题展示列表。
     * 显示全部 9 套主题的图标和名称，已解锁的用彩色高亮，未解锁的用灰色锁定。
     * 已解锁主题可点击切换，当前选中主题显示金色边框和选中标记。
     */
    private void buildThemes(UserProfile profile) {
        llThemes.removeAllViews();
        List<ThemeManager.ThemeDef> all = ThemeManager.ALL_THEMES;
        List<String> unlocked = ThemeManager.getCurrentUnlockedIds(profile);

        int unlockedCount = unlocked.size();
        tvThemeCount.setText(getString(R.string.profile_unlocked_count, unlockedCount, all.size()));

        int showCount = Math.min(6, all.size());
        for (int i = 0; i < showCount; i++) {
            ThemeManager.ThemeDef theme = all.get(i);
            boolean isUnlocked = unlocked.contains(theme.id);
            boolean isSelected = theme.id.equals(profile.currentTheme);

            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            item.setLayoutParams(params);

            // 当前选中主题高亮背景
            if (isSelected) {
                item.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_checkin_today));
                item.setPadding(4, 4, 4, 4);
            }

            // 图标
            TextView icon = new TextView(requireContext());
            icon.setText(isUnlocked ? theme.icon : "🔒");
            icon.setTextSize(24);
            icon.setAlpha(isUnlocked ? 1.0f : 0.4f);
            icon.setGravity(android.view.Gravity.CENTER);
            item.addView(icon);

            // 名称
            TextView name = new TextView(requireContext());
            name.setText(theme.name);
            name.setTextSize(10);
            name.setTextColor(ContextCompat.getColor(requireContext(),
                isUnlocked ? R.color.on_surface : R.color.outline));
            name.setGravity(android.view.Gravity.CENTER);
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            item.addView(name);

            // 已解锁主题可点击切换
            if (isUnlocked) {
                item.setClickable(true);
                item.setFocusable(true);
                item.setOnClickListener(v -> {
                    if (!theme.id.equals(profile.currentTheme)) {
                        viewModel.setCurrentTheme(profile, theme.id);
                        // 立即刷新列表视图
                        buildThemes(profile);
                    }
                });
            }

            // 无障碍描述：名称 + 状态
            String a11yDesc = theme.name;
            if (!isUnlocked) {
                a11yDesc += "，未解锁";
            } else if (isSelected) {
                a11yDesc += "，当前使用";
            }
            item.setContentDescription(a11yDesc);

            llThemes.addView(item);
        }
    }

    /**
     * 分享应用，打开系统分享面板分享文本信息。
     */
    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
            getString(R.string.share_text,
                viewModel.getLearnedCount().getValue() != null
                    ? viewModel.getLearnedCount().getValue() : 0));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.profile_share_app)));
    }
}
