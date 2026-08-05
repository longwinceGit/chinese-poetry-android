package com.poetry.ui.game;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.poetry.R;
import com.poetry.domain.AuthorMatchEngine;
import com.poetry.ui.widget.GameFeedback;
import com.poetry.util.GameSnapshot;
import com.poetry.util.TtsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者连线（Author Match）Fragment —— M8（§4.5，P2 可选、低龄友好）。
 *
 * <p>三列卡片：<b>作者 → 代表作一句 → 朝代</b>。题干卡展示代表作一句（{@code Poem.lines[0]}）
 * 与诗题小字；下方横向 3-4 张候选作者卡（大图标 emoji 头像 + 作者名 + 朝代色块）。
 * 玩家<b>点选一位作者即完成连线</b>（点起点到终点即连，无需拖拽）：</p>
 * <ul>
 *   <li>答对 → 绿色反馈 + 飞分 + 连击 + TTS 短读（§7.1 爽感反馈层）；</li>
 *   <li>答错 → 温柔提示正确答案 + 显示「↩️ 撤销连线」按钮（§8.1 误连可撤销，
 *       撤销后回到待选重连，无锁定惩罚、不扣分）；</li>
 * </ul>
 *
 * <p>朝代色块复用现有 {@code R.color.tag_*}（10 色），按 {@code Poem.tag} 映射；
 * 分辨率自适应由权重列 + NestedScrollView 保证；全交互含 contentDescription（无障碍）。</p>
 */
public class AuthorMatchFragment extends Fragment {

    private AuthorMatchViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvScore, tvProgress, tvQuestion, tvPoemTitle, tvFeedback;
    private LinearLayout llOptions;
    private View btnUndo;

    private GameFeedback gameFeedback;
    private TtsManager ttsManager;

    /** 累计星数（用于 GameFeedback 星星进度环） */
    private int accumulatedStars = 0;
    /** 进题自动朗读的延迟任务 */
    private Runnable autoReadRunnable;
    /** 最近一次点击的选项卡（用于飞分起点坐标） */
    private View lastClickedView = null;
    /** 结算是否已导航（防止 finished observer 与 postDelayed 双重导航） */
    private boolean navigatedToSettlement = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_author_match, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(this).get(AuthorMatchViewModel.class);
        observeData();
        // M9 容错续局：开局保存未完成局快照
        GameSnapshot.save(requireContext(), "author", System.currentTimeMillis());
        registerBackCallback();
        viewModel.startGame();
    }

    /**
     * M9：全屏防误触返回（§8.1）——游戏中按返回需二次确认。
     */
    private void registerBackCallback() {
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        Boolean finished = viewModel.getFinished().getValue();
                        if (finished != null && finished) {
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
        tvScore = v.findViewById(R.id.tv_score);
        tvProgress = v.findViewById(R.id.tv_progress);
        tvQuestion = v.findViewById(R.id.tv_question);
        tvPoemTitle = v.findViewById(R.id.tv_poem_title);
        tvFeedback = v.findViewById(R.id.tv_feedback);
        llOptions = v.findViewById(R.id.ll_options);
        btnUndo = v.findViewById(R.id.btn_undo);
        btnUndo.setVisibility(View.GONE);
        btnUndo.setOnClickListener(vv -> {
            if (viewModel.undoLast()) {
                btnUndo.setVisibility(View.GONE);
                tvFeedback.setVisibility(View.GONE);
                renderOptions(); // 重新渲染候选（恢复可点）
            }
        });

        // M3：顶部 GameFeedback 反馈层
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

        // 🔊 读题按钮
        TextView btnRead = v.findViewById(R.id.btn_read);
        btnRead.setOnClickListener(vv -> readQuestion());

        ttsManager = new TtsManager(requireContext());
    }

    /**
     * 朗读当前题的代表作一句（🔊 按钮 / 自动读题）。
     */
    private void readQuestion() {
        List<AuthorMatchEngine.AuthorRound> rounds = viewModel.getRounds().getValue();
        Integer round = viewModel.getCurrentRound().getValue();
        if (rounds == null || round == null || round >= rounds.size()) return;
        AuthorMatchEngine.AuthorRound r = rounds.get(round);
        if (ttsManager != null) {
            ttsManager.speakPoem(r.poem != null ? r.poem.title : "",
                    r.correctAuthor, new String[]{r.questionLine});
        }
    }

    private void observeData() {
        viewModel.getRounds().observe(getViewLifecycleOwner(), rounds -> {
            if (rounds != null && !rounds.isEmpty()) {
                renderRound(rounds, 0);
            }
        });

        viewModel.getCurrentRound().observe(getViewLifecycleOwner(), round -> {
            List<AuthorMatchEngine.AuthorRound> rounds = viewModel.getRounds().getValue();
            if (round != null && rounds != null && round < rounds.size()) {
                renderRound(rounds, round);
            }
        });

        viewModel.getScore().observe(getViewLifecycleOwner(), score -> {
            if (score != null) tvScore.setText(getString(R.string.game_score) + ": " + score);
        });

        viewModel.getRoundResult().observe(getViewLifecycleOwner(), correct -> {
            if (correct != null) {
                if (correct) {
                    // 答对：绿色反馈 + 连击 + 飞分 + 星星
                    tvFeedback.setText(getString(R.string.author_match_correct));
                    tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.answer_correct));
                    gameFeedback.addCombo();
                    if (lastClickedView != null) {
                        int[] loc = new int[2];
                        lastClickedView.getLocationOnScreen(loc);
                        int[] feedbackLoc = new int[2];
                        gameFeedback.getLocationOnScreen(feedbackLoc);
                        gameFeedback.playFlyingScore(
                                loc[0] - feedbackLoc[0] + lastClickedView.getWidth() / 2,
                                loc[1] - feedbackLoc[1] + lastClickedView.getHeight() / 2,
                                viewModel.getLastPoints());
                    }
                    accumulatedStars = Math.min(3, accumulatedStars + 1);
                    gameFeedback.setStars(accumulatedStars);
                    // §7.4：正确短促 TTS（不打断下一题输入）
                    if (ttsManager != null) {
                        ttsManager.speak("太棒了！", "author_praise");
                    }
                } else {
                    // 答错：温柔反馈 + 正确答案 + 撤销按钮（零挫败）
                    tvFeedback.setText(getString(R.string.author_match_wrong,
                            viewModel.getLastCorrectAuthor()));
                    tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.answer_wrong));
                    gameFeedback.resetCombo();
                    btnUndo.setVisibility(View.VISIBLE);
                }
                tvFeedback.setVisibility(View.VISIBLE);

                handler.postDelayed(() -> {
                    tvFeedback.setVisibility(View.GONE);
                    btnUndo.setVisibility(View.GONE);
                    viewModel.nextRound();
                    Boolean finished = viewModel.getFinished().getValue();
                    if (finished != null && finished) {
                        navigateToSettlement();
                    }
                }, 1500);
            }
        });

        viewModel.getFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished != null && finished) navigateToSettlement();
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

    /**
     * 渲染当前题：题干卡 + 候选作者三列卡。
     */
    private void renderRound(List<AuthorMatchEngine.AuthorRound> rounds, int index) {
        if (index >= rounds.size()) return;
        AuthorMatchEngine.AuthorRound round = rounds.get(index);
        tvProgress.setText(getString(R.string.author_match_progress,
                index + 1, rounds.size()));
        tvQuestion.setText(round.questionLine);
        tvPoemTitle.setText("《" + safe(round.poem != null ? round.poem.title : "") + "》");
        tvFeedback.setVisibility(View.GONE);
        btnUndo.setVisibility(View.GONE);

        // M10 自动读题开关门控：进题 1.5s 轻声朗读
        handler.removeCallbacks(autoReadRunnable);
        autoReadRunnable = this::readQuestion;
        if (requireContext().getSharedPreferences("game_settings", android.content.Context.MODE_PRIVATE)
                .getBoolean("auto_read", true)) {
            handler.postDelayed(autoReadRunnable, 1500);
        }

        renderOptions();
    }

    /**
     * 渲染候选作者卡（横向权重均分 → 分辨率自适应）。
     * <p>每张卡：大 emoji 头像 + 作者名 + 朝代色块（{@code R.color.tag_*}）+ 朝代小字。
     * 卡片高度按内容自适应（wrap_content 内含 minHeight 56dp 触控区）。</p>
     */
    private void renderOptions() {
        llOptions.removeAllViews();
        List<AuthorMatchEngine.AuthorRound> rounds = viewModel.getRounds().getValue();
        Integer round = viewModel.getCurrentRound().getValue();
        if (rounds == null || round == null || round >= rounds.size()) return;
        AuthorMatchEngine.AuthorRound r = rounds.get(round);

        for (int i = 0; i < r.options.size(); i++) {
            AuthorMatchEngine.AuthorOption opt = r.options.get(i);
            MaterialCardView card = new MaterialCardView(requireContext());
            card.setCardElevation(2 * getResources().getDisplayMetrics().density);
            card.setRadius(getResources().getDimension(R.dimen.radius_medium));
            card.setMinimumHeight((int) (56 * getResources().getDisplayMetrics().density));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) params.setMarginStart((int) (8 * getResources().getDisplayMetrics().density));
            card.setLayoutParams(params);

            LinearLayout inner = new LinearLayout(requireContext());
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setGravity(android.view.Gravity.CENTER);
            int pad = (int) (12 * getResources().getDisplayMetrics().density);
            inner.setPadding(pad, pad, pad, pad);
            card.addView(inner);

            // 大图标作者头像 emoji
            TextView tvEmoji = new TextView(requireContext());
            tvEmoji.setText(opt.emoji != null ? opt.emoji : "📜");
            tvEmoji.setTextSize(32);
            tvEmoji.setGravity(android.view.Gravity.CENTER);
            tvEmoji.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            inner.addView(tvEmoji);

            // 作者名
            TextView tvAuthor = new TextView(requireContext());
            tvAuthor.setText(opt.author);
            tvAuthor.setTextSize(16);
            tvAuthor.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_body));
            tvAuthor.setGravity(android.view.Gravity.CENTER);
            tvAuthor.setTypeface(null, android.graphics.Typeface.BOLD);
            inner.addView(tvAuthor);

            // 朝代色块（颜色 + 小字双通道，§8.2）
            TextView tvDynasty = new TextView(requireContext());
            tvDynasty.setText(opt.dynasty != null ? opt.dynasty : "");
            tvDynasty.setTextSize(12);
            tvDynasty.setGravity(android.view.Gravity.CENTER);
            tvDynasty.setPadding(0, (int) (4 * getResources().getDisplayMetrics().density), 0, 0);
            int dynastyColor = dynastyColorRes(opt.dynastyTag);
            tvDynasty.setTextColor(ContextCompat.getColor(requireContext(), dynastyColor));
            inner.addView(tvDynasty);

            // 朝代色块条（色块 + 文字双通道，低龄可见）
            View colorBar = new View(requireContext());
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (4 * getResources().getDisplayMetrics().density));
            barParams.topMargin = (int) (6 * getResources().getDisplayMetrics().density);
            colorBar.setLayoutParams(barParams);
            colorBar.setBackgroundColor(ContextCompat.getColor(requireContext(), dynastyColor));
            colorBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            inner.addView(colorBar);

            // 无障碍：作者 + 朝代
            card.setContentDescription(getString(R.string.a11y_author_option,
                    safe(opt.author), safe(opt.dynasty)));

            final int idx = i;
            card.setOnClickListener(v -> {
                lastClickedView = v;
                disableOptions();
                viewModel.answerAuthor(idx);
            });

            llOptions.addView(card);
        }
    }

    /**
     * 朝代 tag → 色块颜色（复用现有 10 色，§8.2 双通道）。
     */
    private int dynastyColorRes(String tag) {
        if (tag == null) return R.color.tag_other;
        switch (tag) {
            case "tang": return R.color.tag_tang;
            case "song": return R.color.tag_song;
            case "qin": return R.color.tag_qin;
            case "wei": return R.color.tag_wei;
            case "wu": return R.color.tag_wu;
            case "yuan": return R.color.tag_yuan;
            case "ming": return R.color.tag_ming;
            case "qing": return R.color.tag_qing;
            case "modern": return R.color.tag_modern;
            default: return R.color.tag_other;
        }
    }

    /** 禁用所有候选卡（防止重复连线）。 */
    private void disableOptions() {
        for (int i = 0; i < llOptions.getChildCount(); i++) {
            llOptions.getChildAt(i).setEnabled(false);
        }
    }

    /**
     * 游戏完成后导航到结算页（复用 nav_game_settlement 通用参数）。
     * <p>传参 game_type="author" / score / stars / poem_id（本局最美一句，§9.2）。</p>
     */
    private void navigateToSettlement() {
        if (navigatedToSettlement) return;
        navigatedToSettlement = true;
        Integer score = viewModel.getScore().getValue();
        int finalScore = score != null ? score : 0;
        int stars = AuthorMatchEngine.calcStars(finalScore);

        // 3 星 → 庆祝
        if (stars >= 3) {
            if (getActivity() instanceof com.poetry.MainActivity) {
                ((com.poetry.MainActivity) getActivity()).celebrate();
            }
        }

        Bundle args = new Bundle();
        args.putString("game_type", "author");
        args.putInt("score", finalScore);
        args.putInt("stars", stars);
        args.putString("poem_id", viewModel.getLastPoemIdForSettlement());
        // M9 容错续局：完成即清除未完成局快照
        GameSnapshot.clear(requireContext());
        if (getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.nav_game_settlement, args);
        }
    }

    private static String safe(String s) {
        return s != null && !s.isEmpty() ? s : "佚名";
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
