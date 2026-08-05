package com.poetry.ui.game;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.poetry.R;
import com.poetry.domain.GameEngine;
import com.poetry.ui.widget.GameFeedback;
import com.poetry.util.GameSnapshot;
import com.poetry.util.TtsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 诗词接龙 Fragment
 * <p>
 * 实现诗词接龙游戏界面，玩家需要根据给定的诗句（上句或下句）
 * 从多个选项中选择正确的下一句。
 * </p>
 * <p>
 * 核心交互逻辑（M3 升级）：
 * 1. 动态生成选项按钮（数量根据题目而定）
 * 2. 点击选项后禁用所有按钮，防止重复答题
 * 3. 显示答题结果反馈（正确/错误），用颜色区分
 * 4. 1.5秒后自动进入下一轮，或全部完成后导航到结算页
 * 5. 顶部 GameFeedback：星星进度环 + 连击条 + 飞分
 * 6. 🔊 读题按钮 + 进题自动轻声朗读
 * 7. 💡 半句提示（首字 → 首末字）
 * 8. 意象 emoji 标签 + 作者/朝代小字区分干扰项
 * </p>
 */
public class CoupletGameFragment extends Fragment {

    private GameViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvScore, tvProgress, tvQuestion, tvFeedback, tvHint;
    private LinearLayout llOptions;
    private View btnNext;
    private final List<MaterialButton> optionButtons = new ArrayList<>();

    private GameFeedback gameFeedback;
    private TtsManager ttsManager;

    /** 半句提示状态：0=未提示，1=已显示首字，2=已显示首末字 */
    private int hintState = 0;
    /** 当前回合的提示首/末字 */
    private String hintFirstChar = "";
    private String hintLastChar = "";
    /** 当前回合的意象 emoji 标签 */
    private String currentEmojiTag = null;
    /** 当前回合是否存在首字相同干扰项 */
    private boolean currentSameFirstChar = false;
    /** 累计星数（用于 GameFeedback 星星进度环） */
    private int accumulatedStars = 0;
    /** 进题自动朗读的延迟任务 */
    private Runnable autoReadRunnable;
    /** 最近一次点击选项的坐标（相对 GameFeedback，用于飞分起点） */
    private int lastClickX = 0;
    private int lastClickY = 0;

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
        // M9 容错续局：开局保存未完成局快照
        GameSnapshot.save(requireContext(), "couplet", System.currentTimeMillis());
        registerBackCallback();
        viewModel.startCoupletGame();
    }

    /**
     * M9：全屏防误触返回（§8.1）——游戏中按返回需二次确认。
     * <p>游戏进行中（未完成）拦截返回并弹确认框；完成后直接返回。
     * 注意：完成路径由 {@link #navigateToSettlement()} 接管，不破坏结算流程。
     */
    private void registerBackCallback() {
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        Boolean finished = viewModel.getCoupletFinished().getValue();
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

    /**
     * 初始化界面控件。
     * <p>
     * 绑定布局中的视图元素：得分文本、进度文本、题目文本、
     * 反馈文本、提示文本、选项容器、读题/提示按钮和下一题按钮（初始隐藏）。
     * 同时初始化 GameFeedback 与 TTS。
     * </p>
     *
     * @param v Fragment 的根视图
     */
    private void initViews(View v) {
        tvScore = v.findViewById(R.id.tv_score);
        tvProgress = v.findViewById(R.id.tv_progress);
        tvQuestion = v.findViewById(R.id.tv_question);
        tvFeedback = v.findViewById(R.id.tv_feedback);
        tvHint = v.findViewById(R.id.tv_hint);
        llOptions = v.findViewById(R.id.ll_options);
        btnNext = v.findViewById(R.id.btn_next);
        btnNext.setVisibility(View.GONE);

        // M3：顶部 GameFeedback 层
        FrameLayout flFeedback = v.findViewById(R.id.fl_feedback);
        gameFeedback = new GameFeedback(requireContext());
        gameFeedback.setMaxStars(3);
        gameFeedback.setMaxCombo(5);
        gameFeedback.setOnComboFullListener(() -> {
            // 连击满格：触发庆祝
            if (getActivity() instanceof com.poetry.MainActivity) {
                ((com.poetry.MainActivity) getActivity()).celebrate();
            } else {
                Toast.makeText(requireContext(), "🔥 连击满格！", Toast.LENGTH_SHORT).show();
            }
        });
        flFeedback.addView(gameFeedback, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        // M3：读题按钮
        TextView btnRead = v.findViewById(R.id.btn_read);
        btnRead.setOnClickListener(vv -> readQuestion());

        // M3：半句提示按钮
        TextView btnHint = v.findViewById(R.id.btn_hint);
        btnHint.setOnClickListener(vv -> showHint());

        // M3：TTS 初始化
        ttsManager = new TtsManager(requireContext());
    }

    /**
     * 朗读当前题目的上句（🔊 按钮）。
     */
    private void readQuestion() {
        List<GameEngine.CoupletRound> rounds = viewModel.getCoupletRounds().getValue();
        Integer round = viewModel.getCurrentRound().getValue();
        if (rounds == null || round == null || round >= rounds.size()) return;
        GameEngine.CoupletRound cr = rounds.get(round);
        if (ttsManager != null) {
            ttsManager.speakPoem(cr.poem != null ? cr.poem.title : "",
                    cr.poem != null ? cr.poem.author : "",
                    new String[]{cr.givenLine});
        }
    }

    /**
     * 半句提示：第一次点击显示首字，第二次点击显示首末字，之后保持。
     */
    private void showHint() {
        if (hintState == 0) {
            tvHint.setText(getString(R.string.couplet_hint_first, hintFirstChar));
            hintState = 1;
        } else if (hintState == 1) {
            tvHint.setText(getString(R.string.couplet_hint_full, hintFirstChar, hintLastChar));
            hintState = 2;
        }
        tvHint.setVisibility(View.VISIBLE);
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
                if (correct) {
                    tvFeedback.setText(getString(R.string.couplet_correct));
                    // M3：答对 → 连击 +1 + 飞分 + 更新星星
                    gameFeedback.addCombo();
                    int points = viewModel.getLastCoupletPoints();
                    gameFeedback.playFlyingScore(lastClickX, lastClickY, points);
                    accumulatedStars = Math.min(3, accumulatedStars + 1);
                    gameFeedback.setStars(accumulatedStars);
                } else {
                    // M3：答错 → 连击清零
                    gameFeedback.resetCombo();
                    // 若选中了"首字相同干扰项"，显示善意补偿文案
                    String closeGuess = getCloseGuessAnswer();
                    if (closeGuess != null) {
                        tvFeedback.setText(getString(R.string.couplet_close_guess, closeGuess));
                    } else {
                        tvFeedback.setText(getString(R.string.couplet_wrong));
                    }
                }
                tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), color));
                tvFeedback.setVisibility(View.VISIBLE);

                handler.postDelayed(() -> {
                    tvFeedback.setVisibility(View.GONE);
                    viewModel.nextCoupletRound();
                    Boolean finished = viewModel.getCoupletFinished().getValue();
                    if (finished != null && finished) {
                        navigateToSettlement();
                    }
                }, 1500);
            }
        });

        viewModel.getCoupletFinished().observe(getViewLifecycleOwner(), finished -> {
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
                viewModel.clearAchievement(); // 消费后清空，防止 LiveData 回放
            }
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

        // M3：重置半句提示状态
        hintState = 0;
        hintFirstChar = round.hintFirstChar != null ? round.hintFirstChar : "";
        hintLastChar = round.hintLastChar != null ? round.hintLastChar : "";
        tvHint.setVisibility(View.GONE);
        tvHint.setText(getString(R.string.couplet_hint));

        // M3：记录当前回合的意象标签与首字相同标记
        currentEmojiTag = round.emojiTag;
        currentSameFirstChar = round.sameFirstChar;

        // M3：进题自动轻声朗读一次（1.5s 延迟，QUEUE_FLUSH 打断旧朗读）
        // M10：受「自动读题」开关门控（SharedPreferences "game_settings"/"auto_read"，默认开）
        handler.removeCallbacks(autoReadRunnable);
        autoReadRunnable = () -> {
            if (ttsManager != null) {
                ttsManager.speakPoem(round.poem != null ? round.poem.title : "",
                        round.poem != null ? round.poem.author : "",
                        new String[]{round.givenLine});
            }
        };
        if (requireContext().getSharedPreferences("game_settings", android.content.Context.MODE_PRIVATE)
                .getBoolean("auto_read", true)) {
            handler.postDelayed(autoReadRunnable, 1500);
        }

        // Build option buttons dynamically
        llOptions.removeAllViews();
        optionButtons.clear();

        for (int i = 0; i < round.options.size(); i++) {
            String optionText = round.options.get(i);
            MaterialButton btn = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);

            // M3：意象 emoji 标签（仅当该选项与答案首字相同且存在标签时前缀）
            String displayText = optionText;
            if (currentSameFirstChar && currentEmojiTag != null
                    && sameFirstChar(optionText, round.correctAnswer)) {
                displayText = currentEmojiTag + " " + optionText;
            }
            btn.setText(displayText);
            btn.setTextSize(16);
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface));
            btn.setStrokeColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.outline_variant)));

            // M3：意象标签选项下方小字（作者 · 朝代）——MaterialButton 1.11 无 setSupportingText，
            // 改用 SpannableString 第二行小字（RelativeSizeSpan 缩小 + 次要颜色）
            if (currentSameFirstChar && currentEmojiTag != null
                    && sameFirstChar(optionText, round.correctAnswer)) {
                String subtitle = getString(R.string.couplet_option_subtitle,
                        safe(round.poem != null ? round.poem.author : ""),
                        safe(round.poem != null ? round.poem.dynasty : ""));
                SpannableString sp = new SpannableString(displayText + "\n" + subtitle);
                int start = displayText.length() + 1;
                sp.setSpan(new RelativeSizeSpan(0.72f), start, sp.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sp.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.on_surface_variant)),
                        start, sp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                btn.setText(sp);
                btn.setSingleLine(false);
                btn.setMaxLines(2);
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) params.setMargins(0, 10, 0, 0);
            btn.setLayoutParams(params);
            // M10 56dp 触控区规范：选项按钮最小高度
            btn.setMinHeight((int) (56 * getResources().getDisplayMetrics().density));

            final int idx = i;
            btn.setOnClickListener(v -> {
                disableOptions();
                // M3：记录飞分起点（被点按钮坐标，相对 GameFeedback）
                int[] loc = new int[2];
                v.getLocationOnScreen(loc);
                int[] feedbackLoc = new int[2];
                gameFeedback.getLocationOnScreen(feedbackLoc);
                lastClickX = loc[0] - feedbackLoc[0] + v.getWidth() / 2;
                lastClickY = loc[1] - feedbackLoc[1] + v.getHeight() / 2;
                viewModel.answerCouplet(idx);
            });

            llOptions.addView(btn);
            optionButtons.add(btn);
        }
    }

    /** 判断两个句子去掉标点后的首字是否相同。 */
    private boolean sameFirstChar(String a, String b) {
        String fa = firstChar(a);
        String fb = firstChar(b);
        return !fa.isEmpty() && fa.equals(fb);
    }

    /** 取字符串去掉标点后的首字符。 */
    private static String firstChar(String s) {
        if (s == null) return "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isIdeographic(c)) {
                return String.valueOf(c);
            }
        }
        return "";
    }

    private static String safe(String s) {
        return s != null && !s.isEmpty() ? s : "佚名";
    }

    /**
     * 若本轮选中了"首字相同干扰项"，返回正确答案用于善意补偿文案；否则返回 null。
     */
    private String getCloseGuessAnswer() {
        List<GameEngine.CoupletRound> rounds = viewModel.getCoupletRounds().getValue();
        Integer round = viewModel.getCurrentRound().getValue();
        if (rounds == null || round == null || round >= rounds.size()) return null;
        GameEngine.CoupletRound cr = rounds.get(round);
        // 仅当存在首字相同干扰项时，答错才可能触发善意补偿
        if (!cr.sameFirstChar) return null;
        return cr.correctAnswer;
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
     * 游戏完成后导航到结算页（替代原 onBackPressed）。
     * <p>
     * 传参 game_type="couplet" / score / stars，nav_graph 已定义。
     * 3 星或全对时触发庆祝。
     * </p>
     */
    private void navigateToSettlement() {
        Integer score = viewModel.getCoupletScore().getValue();
        int finalScore = score != null ? score : 0;
        int stars = GameEngine.calcCoupletStars(finalScore);

        // 3 星或全对 → 庆祝
        com.poetry.domain.GameResult result = viewModel.getCoupletResult();
        boolean perfect = result != null && result.perfect;
        if (stars >= 3 || perfect) {
            if (getActivity() instanceof com.poetry.MainActivity) {
                ((com.poetry.MainActivity) getActivity()).celebrate();
            }
        }

        Bundle args = new Bundle();
        args.putString("game_type", "couplet");
        args.putInt("score", finalScore);
        args.putInt("stars", stars);
        args.putString("poem_id", viewModel.getBestPoemId());
        // M9 容错续局：完成即清除未完成局快照
        GameSnapshot.clear(requireContext());
        if (getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.nav_game_settlement, args);
        }
    }

    /**
     * Fragment 视图销毁时的回调。
     * <p>
     * 清理 Handler 中的待执行消息和回调，移除自动朗读任务，释放 TTS，防止内存泄漏。
     * </p>
     */
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
