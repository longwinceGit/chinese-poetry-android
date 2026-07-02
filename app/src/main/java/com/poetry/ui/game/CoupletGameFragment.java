package com.poetry.ui.game;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.poetry.R;
import com.poetry.domain.GameEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * 诗词接龙 Fragment
 * <p>
 * 实现诗词接龙游戏界面，玩家需要根据给定的诗句（上句或下句）
 * 从多个选项中选择正确的下一句。
 * </p>
 * <p>
 * 核心交互逻辑：
 * 1. 动态生成选项按钮（数量根据题目而定）
 * 2. 点击选项后禁用所有按钮，防止重复答题
 * 3. 显示答题结果反馈（正确/错误），用颜色区分
 * 4. 1.5秒后自动进入下一轮，或全部完成后显示结果
 * </p>
 */
public class CoupletGameFragment extends Fragment {

    private GameViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvScore, tvProgress, tvQuestion, tvFeedback;
    private LinearLayout llOptions;
    private View btnNext;
    private final List<MaterialButton> optionButtons = new ArrayList<>();

    /**
     * 创建 Fragment 的视图。
     * <p>
     * 从 XML 布局文件 fragment_game_couplet.xml 加载界面布局。
     * </p>
     *
     * @param inflater 布局填充器
     * @param container 父容器
     * @param savedInstanceState 之前保存的状态
     * @return 填充后的视图，或 null
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game_couplet, container, false);
    }

    /**
     * Fragment 视图创建完成后的回调。
     * <p>
     * 在此方法中执行以下初始化操作：
     * 1. 初始化界面控件
     * 2. 获取 GameViewModel 实例
     * 3. 观察 ViewModel 中的数据变化
     * 4. 开始新的接龙游戏
     * </p>
     *
     * @param view 已创建的 Fragment 视图
     * @param savedInstanceState 之前保存的状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);
        observeData();
        viewModel.startCoupletGame();
    }

    /**
     * 初始化界面控件。
     * <p>
     * 绑定布局中的视图元素：得分文本、进度文本、题目文本、
     * 反馈文本、选项容器和下一题按钮（初始隐藏）。
     * </p>
     *
     * @param v Fragment 的根视图
     */
    private void initViews(View v) {
        tvScore = v.findViewById(R.id.tv_score);
        tvProgress = v.findViewById(R.id.tv_progress);
        tvQuestion = v.findViewById(R.id.tv_question);
        tvFeedback = v.findViewById(R.id.tv_feedback);
        llOptions = v.findViewById(R.id.ll_options);
        btnNext = v.findViewById(R.id.btn_next);
        btnNext.setVisibility(View.GONE);
    }

    /**
     * 观察 ViewModel 中的数据变化。
     * <p>
     * 注册以下 LiveData 观察者：
     * 1. 接龙回合列表 - 首次加载时渲染第一题
     * 2. 当前回合索引 - 渲染对应轮次的题目
     * 3. 得分 - 更新得分显示
     * 4. 回合结果 - 显示答题反馈（正确/错误），1.5秒后自动下一题
     * 5. 游戏完成状态 - 显示最终结果
     * </p>
     */
    private void observeData() {
        viewModel.getCoupletRounds().observe(getViewLifecycleOwner(), rounds -> {
            if (rounds != null && !rounds.isEmpty()) {
                renderRound(rounds, 0);
            }
        });

        viewModel.getCurrentRound().observe(getViewLifecycleOwner(), round -> {
            List<GameEngine.CoupletRound> rounds = viewModel.getCoupletRounds().getValue();
            if (round != null && rounds != null && round < rounds.size()) {
                renderRound(rounds, round);
            }
        });

        viewModel.getCoupletScore().observe(getViewLifecycleOwner(), score -> {
            if (score != null) tvScore.setText(getString(R.string.game_score) + ": " + score);
        });

        viewModel.getRoundResult().observe(getViewLifecycleOwner(), correct -> {
            if (correct != null) {
                int color = correct ? R.color.answer_correct : R.color.answer_wrong;
                tvFeedback.setText(correct ? "✅ 回答正确！" : "❌ 再接再厉！");
                tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), color));
                tvFeedback.setVisibility(View.VISIBLE);

                handler.postDelayed(() -> {
                    tvFeedback.setVisibility(View.GONE);
                    viewModel.nextCoupletRound();
                    Boolean finished = viewModel.getCoupletFinished().getValue();
                    if (finished != null && finished) {
                        showResult();
                    }
                }, 1500);
            }
        });

        viewModel.getCoupletFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished != null && finished) showResult();
        });
    }

    /**
     * 渲染当前回合的题目和选项。
     * <p>
     * 动态生成选项按钮，每个按钮对应一个可能的答案。
     * 按钮样式使用 Material Design 的 outlined 风格。
     * </p>
     * <p>
     * 每次调用会清除之前的选项并重新创建，
     * 同时隐藏上一轮的反馈信息。
     * </p>
     *
     * @param rounds 所有回合的数据
     * @param index 当前要渲染的回合索引
     */
    private void renderRound(List<GameEngine.CoupletRound> rounds, int index) {
        if (index >= rounds.size()) return;
        GameEngine.CoupletRound round = rounds.get(index);
        tvProgress.setText(getString(R.string.game_progress, index + 1, rounds.size()));
        tvQuestion.setText(round.givenLine);
        tvFeedback.setVisibility(View.GONE);

        // Build option buttons dynamically
        llOptions.removeAllViews();
        optionButtons.clear();

        for (int i = 0; i < round.options.size(); i++) {
            MaterialButton btn = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(round.options.get(i));
            btn.setTextSize(16);
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface));
            btn.setStrokeColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.outline_variant)));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) params.setMargins(0, 10, 0, 0);
            btn.setLayoutParams(params);

            final int idx = i;
            btn.setOnClickListener(v -> {
                disableOptions();
                viewModel.answerCouplet(idx);
            });

            llOptions.addView(btn);
            optionButtons.add(btn);
        }
    }

    /**
     * 禁用所有选项中按钮。
     * <p>
     * 在用户选择答案后调用，防止重复答题。
     * 按钮会被禁用直到进入下一轮。
     * </p>
     */
    private void disableOptions() {
        for (MaterialButton btn : optionButtons) btn.setEnabled(false);
    }

    /**
     * 显示游戏最终结果。
     * <p>
     * 在游戏全部完成后调用，显示总得分，
     * 3秒后自动返回上一界面。
     * </p>
     */
    private void showResult() {
        Integer score = viewModel.getCoupletScore().getValue();
        tvFeedback.setText("🎉 游戏结束！你得了 " + (score != null ? score : 0) + " 分！");
        tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.score_gold));
        tvFeedback.setVisibility(View.VISIBLE);

        handler.postDelayed(() -> requireActivity().onBackPressed(), 3000);
    }

    /**
     * Fragment 销毁时的回调。
     * <p>
     * 清理 Handler 中的待执行消息和回调，防止内存泄漏。
     * </p>
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
