package com.poetry.ui.game;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.poetry.R;
import com.poetry.ui.widget.GameFeedback;
import com.poetry.util.GameSnapshot;
import com.poetry.util.TtsManager;

import java.util.List;

/**
 * 飞花令 Fragment（M6 新增）。
 *
 * <p>实现 GAME_REDESIGN_FINAL.md §4.4 的限时飞花令玩法：
 * <ul>
 *   <li>顶部大主题字 + 题目「说一句带『月』的诗」+ 🔊 读题（进题自动轻声朗读）；</li>
 *   <li>右上角 60 秒倒计时（≤10s 变红）；</li>
 *   <li>GameFeedback 反馈层（星星进度环 + 连击条 + 飞分）；</li>
 *   <li>输入框 + 「对令」手动输入（命中加分，未命中温柔提示不结束游戏）；</li>
 *   <li>「📖 提示一句」每局限 2 次，点选候选句即命中并 TTS 朗读；</li>
 *   <li>命中后 TTS 朗读该句；倒计时结束按当前进度结算并导航到结算页。</li>
 * </ul>
 */
public class FlyflowerGameFragment extends Fragment {

    private FlyflowerViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvTimer, tvProgress, tvKeyword, tvQuestion;
    private EditText etInput;
    private MaterialButton btnSubmit, btnHint, btnRestart;
    private LinearLayout llCandidates;

    private GameFeedback gameFeedback;
    private TtsManager ttsManager;
    private Runnable autoReadRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_flyflower, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(this).get(FlyflowerViewModel.class);
        observeData();
        // M9 容错续局：开局保存未完成局快照
        GameSnapshot.save(requireContext(), "flyflower", System.currentTimeMillis());
        registerBackCallback();
        viewModel.startFlyflower();
    }

    /**
     * M9：全屏防误触返回（§8.1）——游戏中按返回需二次确认。
     * <p>游戏进行中（未完成）拦截返回并弹确认框；完成后直接返回。
     */
    private void registerBackCallback() {
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (viewModel.isFinished()) {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            return;
                        }
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.confirm_exit_title)
                                .setMessage(R.string.confirm_exit_message)
                                .setPositiveButton(R.string.confirm_action_yes, (d, w) -> {
                                    setEnabled(false);
                                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                                })
                                .setNegativeButton(R.string.confirm_action_no, null)
                                .show();
                    }
                });
    }

    private void initViews(View v) {
        tvTimer = v.findViewById(R.id.tv_fly_timer);
        tvProgress = v.findViewById(R.id.tv_fly_progress);
        tvKeyword = v.findViewById(R.id.tv_fly_keyword);
        tvQuestion = v.findViewById(R.id.tv_fly_question);
        etInput = v.findViewById(R.id.et_fly_input);
        btnSubmit = v.findViewById(R.id.btn_fly_submit);
        btnHint = v.findViewById(R.id.btn_fly_hint);
        btnRestart = v.findViewById(R.id.btn_fly_restart);
        llCandidates = v.findViewById(R.id.ll_candidates);

        // 顶部 GameFeedback 层
        FrameLayout flFeedback = v.findViewById(R.id.fl_feedback);
        gameFeedback = new GameFeedback(requireContext());
        gameFeedback.setMaxStars(3);
        gameFeedback.setMaxCombo(5);
        gameFeedback.setOnComboFullListener(() -> {
            if (getActivity() instanceof com.poetry.MainActivity) {
                ((com.poetry.MainActivity) getActivity()).celebrate();
            } else {
                Toast.makeText(requireContext(), "🔥 连击满格！", Toast.LENGTH_SHORT).show();
            }
        });
        flFeedback.addView(gameFeedback, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        // TTS 初始化
        ttsManager = new TtsManager(requireContext());

        // 🔊 读题按钮
        TextView btnRead = v.findViewById(R.id.btn_fly_read);
        btnRead.setOnClickListener(vv -> readQuestion());

        // 「对令」按钮
        btnSubmit.setOnClickListener(vv -> submitInput());

        // 「📖 提示一句」按钮
        btnHint.setOnClickListener(vv -> showCandidates());

        // 再来一局（M9 二次确认，§8.5）
        btnRestart.setOnClickListener(vv -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.confirm_restart_title)
                    .setMessage(R.string.confirm_restart_message)
                    .setPositiveButton(R.string.confirm_action_yes, (d, w) -> restartFlyflower())
                    .setNegativeButton(R.string.confirm_action_no, null)
                    .show();
        });
    }

    /** 执行飞花令重开逻辑（M9 确认后调用）。 */
    private void restartFlyflower() {
        llCandidates.setVisibility(View.GONE);
        llCandidates.removeAllViews();
        etInput.setText("");
        if (gameFeedback != null) {
            gameFeedback.resetCombo();
            gameFeedback.setStars(0);
        }
        // M9 容错续局：重开即重新保存快照
        GameSnapshot.save(requireContext(), "flyflower", System.currentTimeMillis());
        viewModel.startFlyflower();
    }

    /** 朗读当前题目（🔊 按钮 / 进题自动朗读）。 */
    private void readQuestion() {
        String k = viewModel.getKeywordValue();
        if (k == null || ttsManager == null) return;
        String question = getString(R.string.flyflower_question, k);
        ttsManager.speakPoem("", "", new String[]{question});
    }

    /** 提交手动输入。 */
    private void submitInput() {
        String input = etInput.getText() != null ? etInput.getText().toString() : "";
        if (input.trim().isEmpty()) {
            String k = viewModel.getKeywordValue();
            Toast.makeText(requireContext(),
                    getString(R.string.flyflower_miss, k != null ? k : ""), Toast.LENGTH_SHORT).show();
            return;
        }
        int result = viewModel.submitLine(input);
        if (result == 0) {
            etInput.setText("");
        }
        // 未命中时由 missMessage observer 温柔提示，不结束游戏
    }

    /** 点「📖 提示一句」：消耗机会并展示候选句。 */
    private void showCandidates() {
        if (viewModel.getHintRemaining() <= 0) {
            Toast.makeText(requireContext(), R.string.flyflower_hint_exhausted, Toast.LENGTH_LONG).show();
            return;
        }
        if (!viewModel.useHint()) {
            Toast.makeText(requireContext(), R.string.flyflower_hint_exhausted, Toast.LENGTH_LONG).show();
            return;
        }
        updateHintButton();
        List<String> candidates = viewModel.buildCandidates();
        renderCandidates(candidates);
    }

    /** 渲染候选句 chips（点选即命中 + TTS 朗读）。 */
    private void renderCandidates(List<String> candidates) {
        llCandidates.removeAllViews();
        if (candidates == null || candidates.isEmpty()) {
            Toast.makeText(requireContext(), R.string.flyflower_hint_exhausted, Toast.LENGTH_LONG).show();
            return;
        }
        for (String line : candidates) {
            MaterialButton chip = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            chip.setText(line);
            chip.setTextSize(14);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface));
            chip.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.outline_variant)));
            chip.setContentDescription(getString(R.string.a11y_flyflower_candidate, line));
            chip.setMinHeight(dp(56));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            if (llCandidates.getChildCount() > 0) params.setMargins(0, dp(8), 0, 0);
            chip.setLayoutParams(params);

            chip.setOnClickListener(v -> {
                viewModel.selectCandidate(line);
                if (ttsManager != null) {
                    ttsManager.speakPoem("", "", new String[]{line});
                }
            });
            llCandidates.addView(chip);
        }
        llCandidates.setVisibility(View.VISIBLE);
    }

    /** 更新「提示一句」按钮文案 / 禁用状态。 */
    private void updateHintButton() {
        int remaining = viewModel.getHintRemaining();
        if (remaining <= 0) {
            btnHint.setEnabled(false);
            btnHint.setText(getString(R.string.flyflower_hint_exhausted));
        } else {
            btnHint.setText(getString(R.string.flyflower_hint_button, remaining));
        }
    }

    private void observeData() {
        // 主题字 + 题目
        viewModel.getKeyword().observe(getViewLifecycleOwner(), k -> {
            if (k != null) {
                tvKeyword.setText(k);
                tvKeyword.setContentDescription(getString(R.string.a11y_flyflower_keyword, k));
                tvQuestion.setText(getString(R.string.flyflower_question, k));
                etInput.setHint(getString(R.string.flyflower_input_hint, k));
                // 进题自动轻声朗读（1.5s 延迟）
                // M10：受「自动读题」开关门控（SharedPreferences "game_settings"/"auto_read"，默认开）
                handler.removeCallbacks(autoReadRunnable);
                autoReadRunnable = () -> readQuestion();
                if (requireContext().getSharedPreferences("game_settings", android.content.Context.MODE_PRIVATE)
                        .getBoolean("auto_read", true)) {
                    handler.postDelayed(autoReadRunnable, 1500);
                }
            }
        });

        // 倒计时
        viewModel.getRemainingSeconds().observe(getViewLifecycleOwner(), remaining -> {
            if (remaining == null) return;
            tvTimer.setText(getString(R.string.match_timer_format, remaining));
            if (remaining <= 10) {
                tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.answer_wrong));
            } else {
                tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gold));
            }
        });

        // 命中计数
        viewModel.getHitCount().observe(getViewLifecycleOwner(), hits -> {
            if (hits != null) {
                tvProgress.setText(getString(R.string.flyflower_progress, hits));
            }
        });

        // 命中 → 连击 + 飞分 + TTS 朗读该句
        viewModel.getLastHitLine().observe(getViewLifecycleOwner(), line -> {
            if (line != null && !line.isEmpty()) {
                if (gameFeedback != null) {
                    gameFeedback.addCombo();
                    int[] loc = new int[2];
                    tvKeyword.getLocationOnScreen(loc);
                    int[] feedbackLoc = new int[2];
                    gameFeedback.getLocationOnScreen(feedbackLoc);
                    int flyX = loc[0] - feedbackLoc[0] + tvKeyword.getWidth() / 2;
                    int flyY = loc[1] - feedbackLoc[1] + tvKeyword.getHeight() / 2;
                    gameFeedback.playFlyingScore(flyX, flyY, 30);
                }
                if (ttsManager != null) {
                    ttsManager.speakPoem("", "", new String[]{line});
                }
                Toast.makeText(requireContext(),
                        getString(R.string.flyflower_hit, line), Toast.LENGTH_SHORT).show();
            }
        });

        // 未命中 → 温柔提示（不结束游戏）
        viewModel.getMissMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                String k = viewModel.getKeywordValue();
                Toast.makeText(requireContext(),
                        getString(R.string.flyflower_miss, k != null ? k : ""), Toast.LENGTH_SHORT).show();
            }
        });

        // 游戏完成 → 结算导航
        viewModel.getFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished != null && finished) {
                if (viewModel.isTimedOut()) {
                    Toast.makeText(requireContext(),
                            getString(R.string.flyflower_timeout, viewModel.getHitCountValue()),
                            Toast.LENGTH_LONG).show();
                }
                // M9 容错续局：完成即清除未完成局快照
                GameSnapshot.clear(requireContext());
                navigateToSettlement();
            }
        });

        // 成就解锁：撒花 + Toast
        viewModel.getNewAchievement().observe(getViewLifecycleOwner(), def -> {
            if (def != null) {
                Toast.makeText(requireContext(),
                        "🎉 成就解锁：" + def.name, Toast.LENGTH_LONG).show();
                if (getActivity() instanceof com.poetry.MainActivity) {
                    ((com.poetry.MainActivity) getActivity()).celebrate();
                }
                viewModel.clearAchievement();
            }
        });
    }

    /** 游戏完成后导航到结算页。 */
    private void navigateToSettlement() {
        int score = viewModel.getFinalScore();
        int stars = viewModel.getFinalStars();

        // 3 星 → 庆祝
        if (stars >= 3) {
            if (getActivity() instanceof com.poetry.MainActivity) {
                ((com.poetry.MainActivity) getActivity()).celebrate();
            }
        }

        Bundle args = new Bundle();
        args.putString("game_type", "flyflower");
        args.putInt("score", score);
        args.putInt("stars", stars);
        args.putString("poem_id", viewModel.getBestPoemId());
        if (getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.nav_game_settlement, args);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (autoReadRunnable != null) {
            handler.removeCallbacks(autoReadRunnable);
            autoReadRunnable = null;
        }
        if (ttsManager != null) {
            ttsManager.shutdown();
            ttsManager = null;
        }
    }
}
