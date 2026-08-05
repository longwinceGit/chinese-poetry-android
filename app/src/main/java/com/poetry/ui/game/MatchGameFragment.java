package com.poetry.ui.game;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.poetry.R;
import com.poetry.domain.GameEngine;
import com.poetry.ui.widget.GameFeedback;
import com.poetry.util.GameSnapshot;
import com.poetry.util.TtsManager;

/**
 * 消消乐 Fragment（M4 升级：翻牌记忆玩法）
 * <p>
 * 实现诗词消消乐游戏界面，玩家需要在 3×4 的网格中
 * 找到并配对属于同一首诗的上句和下句。
 * </p>
 * <p>
 * 核心交互逻辑（全正面可见配对）：
 * 1. 点击第一张卡片选中高亮
 * 2. 点击同一张卡片可取消选中
 * 3. 点击第二张挑选配对消除
 * 4. 配对成功：GameFeedback 连击 + 飞分 + TTS 朗读 + 消除动画
 * 5. 配对失败：温柔抖动归位，不扣分
 * 6. 顶部 GameFeedback：星星进度环 + 连击条 + 飞分
 * 7. 倒计时（第二局起 90s），超时按当前进度结算
 * </p>
 */
public class MatchGameFragment extends Fragment {

    private GameViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private RecyclerView recyclerCards;
    private TextView tvProgress, tvTip, tvTimer;
    private MaterialButton btnRestart;
    private View layoutComplete;
    private TextView tvCompleteScore;

    private MatchCardAdapter adapter;
    private GameEngine.MatchCard firstSelected;    // 第一张选中的卡片
    private int firstSelectedPosition = -1;          // 第一张卡片的位置
    private boolean lockInput = false;               // 动画播放期间锁定输入

    private GameFeedback gameFeedback;
    private TtsManager ttsManager;

    /**
     * 创建 Fragment 的视图。
     * <p>
     * 从 XML 布局文件 fragment_game_match.xml 加载界面布局。
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
        return inflater.inflate(R.layout.fragment_game_match, container, false);
    }

    /**
     * Fragment 视图创建完成后的回调。
     * <p>
     * 在此方法中执行以下初始化操作：
     * 1. 初始化界面控件
     * 2. 获取 GameViewModel 实例
     * 3. 观察 ViewModel 中的数据变化
     * 4. 开始新的消消乐游戏
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
        GameSnapshot.save(requireContext(), "match", System.currentTimeMillis());
        registerBackCallback();
        viewModel.startMatchGame();
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
                        Boolean finished = viewModel.getMatchFinished().getValue();
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
     * 绑定布局中的视图元素，设置 RecyclerView 的网格布局管理器和适配器，
     * 配置重新开始按钮的点击事件，初始化 GameFeedback 与 TTS。
     * </p>
     *
     * @param v Fragment 的根视图
     */
    private void initViews(View v) {
        recyclerCards = v.findViewById(R.id.recycler_cards);
        tvProgress = v.findViewById(R.id.tv_match_progress);
        tvTip = v.findViewById(R.id.tv_match_tip);
        tvTimer = v.findViewById(R.id.tv_match_timer);
        btnRestart = v.findViewById(R.id.btn_match_restart);
        layoutComplete = v.findViewById(R.id.layout_match_complete);
        tvCompleteScore = v.findViewById(R.id.tv_match_complete_score);

        recyclerCards.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        adapter = new MatchCardAdapter(this::onCardClick);
        recyclerCards.setAdapter(adapter);

        // M4：顶部 GameFeedback 层
        FrameLayout flFeedback = v.findViewById(R.id.fl_feedback);
        gameFeedback = new GameFeedback(requireContext());
        gameFeedback.setMaxStars(3);
        gameFeedback.setMaxCombo(6);
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

        // M4：TTS 初始化
        ttsManager = new TtsManager(requireContext());

        // M9 二次确认（§8.5）：重来需确认，防止误触丢星
        btnRestart.setOnClickListener(v2 -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.confirm_restart_title)
                    .setMessage(R.string.confirm_restart_message)
                    .setPositiveButton(R.string.confirm_action_yes, (d, w) -> restartMatch())
                    .setNegativeButton(R.string.confirm_action_no, null)
                    .show();
        });
    }

    /** 执行消消乐重开逻辑（M9 确认后调用）。 */
    private void restartMatch() {
        firstSelected = null;
        firstSelectedPosition = -1;
        lockInput = false;
        layoutComplete.setVisibility(View.GONE);
        tvTip.setVisibility(View.GONE);
        if (gameFeedback != null) {
            gameFeedback.resetCombo();
            gameFeedback.setStars(0);
        }
        // M9 容错续局：重开即重新保存快照
        GameSnapshot.save(requireContext(), "match", System.currentTimeMillis());
        viewModel.startMatchGame();
    }

    /**
     * 观察 ViewModel 中的数据变化。
     * <p>
     * 注册以下 LiveData 观察者：
     * 1. 卡片列表 - 更新 RecyclerView 显示
     * 2. 已配对数量 - 更新进度文本
     * 3. 配对成功提示 - 显示诗词信息（3秒后自动隐藏）
     * 4. 游戏完成状态 - 显示完成界面和得分
     * 5. 倒计时 - 更新计时器文本
     * </p>
     */
    private void observeData() {
        // 卡片列表更新
        viewModel.getMatchCards().observe(getViewLifecycleOwner(), cards -> {
            if (cards != null) {
                adapter.setCards(cards);
            }
        });

        // 已配对数量
        viewModel.getMatchedCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                int total = viewModel.getMatchPairs();
                tvProgress.setText(getString(R.string.game_progress, count, total));
            }
        });

        // 配对成功提示（诗词名）
        viewModel.getMatchTip().observe(getViewLifecycleOwner(), tip -> {
            if (tip != null && !tip.isEmpty()) {
                tvTip.setText("✨ " + tip);
                tvTip.setVisibility(View.VISIBLE);
                // 3秒后自动隐藏
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(() -> tvTip.setVisibility(View.GONE), 3000);
            }
        });

        // M4：倒计时
        viewModel.getMatchRemaining().observe(getViewLifecycleOwner(), remaining -> {
            if (remaining == null || remaining < 0) {
                tvTimer.setVisibility(View.GONE);
                return;
            }
            tvTimer.setVisibility(View.VISIBLE);
            tvTimer.setText(getString(R.string.match_timer_format, remaining));
            // ≤10s 变红提示
            if (remaining <= 10) {
                tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.answer_wrong));
            } else {
                tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_title));
            }
        });

        // 游戏完成
        viewModel.getMatchFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished != null && finished) {
                int score = viewModel.getMatchFinalScore();
                int stars = viewModel.getMatchFinalStars();
                Integer attempts = viewModel.getMatchAttempts().getValue();
                tvCompleteScore.setText("得分 " + score + "  ·  尝试 " + (attempts != null ? attempts : 0) + " 次");
                layoutComplete.setVisibility(View.VISIBLE);

                if (viewModel.isMatchTimedOut()) {
                    Toast.makeText(requireContext(),
                            getString(R.string.match_timeout_tip),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(),
                            getString(R.string.match_complete_toast) + " 共尝试 " + (attempts != null ? attempts : 0) + " 次",
                            Toast.LENGTH_LONG).show();
                }

                // M4：结算导航（3 星或全对 → 庆祝）
                // M9 容错续局：完成即清除未完成局快照
                GameSnapshot.clear(requireContext());
                navigateToSettlement(score, stars);
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
                viewModel.clearAchievement(); // 消费后清空，防止 LiveData 回放
            }
        });
    }

    /**
     * 卡片点击处理（全正面可见配对玩法）。
     * <p>
     * 处理消消乐游戏中的卡片点击逻辑：
     * 1. 如果尚未选中任何卡片，选中当前卡片并高亮
     * 2. 如果点击已选中的卡片，取消选中
     * 3. 如果已选中一张卡片，尝试与第二张卡片配对
     * </p>
     * <p>
     * 配对结果处理：
     * - 成功：GameFeedback 连击 + 飞分 + TTS 朗读 + 消除动画
     * - 失败：温柔抖动归位，不扣分
     * </p>
     * <p>
     * 注意：动画播放期间会锁定输入（lockInput=true），防止用户误操作。
     * </p>
     *
     * @param card 被点击的卡片
     * @param position 卡片在列表中的位置
     */
    private void onCardClick(GameEngine.MatchCard card, int position) {
        if (card.matched || lockInput) return;

        if (firstSelected == null) {
            // === 选中第一张 ===
            firstSelected = card;
            firstSelectedPosition = position;
            viewModel.toggleSelect(card);
            return;
        }

        if (firstSelected == card) {
            // === 点击同一张，取消选中 ===
            viewModel.toggleSelect(card);
            firstSelected = null;
            firstSelectedPosition = -1;
            return;
        }

        // === 选中第二张，尝试配对 ===
        lockInput = true;
        viewModel.toggleSelect(card);  // 高亮第二张

        // 短暂延迟让用户看到两张卡片，然后判断结果
        handler.postDelayed(() -> {
            // 🔴 捕获局部 final 引用，防止后续 firstSelected 置 null 导致 NPE
            final GameEngine.MatchCard card1 = firstSelected;
            final int pos1 = firstSelectedPosition;
            final GameEngine.MatchCard card2 = card;
            final int pos2 = position;

            // 先用 GameEngine 预判配对结果，不修改数据
            boolean matchSuccess = GameEngine.checkMatch(card1, card2);

            // 在 tryMatch 触发 Rebind 之前抓取视图引用（否则 Rebind 后视图被替换）
            View v1 = recyclerCards.getLayoutManager() != null
                    ? recyclerCards.getLayoutManager().findViewByPosition(pos1) : null;
            View v2 = recyclerCards.getLayoutManager() != null
                    ? recyclerCards.getLayoutManager().findViewByPosition(pos2) : null;

            if (matchSuccess) {
                // === 配对成功 ===
                // M4：连击 + 飞分 + TTS 朗读
                if (gameFeedback != null) {
                    gameFeedback.addCombo();
                    int[] loc = new int[2];
                    if (v1 != null) v1.getLocationOnScreen(loc);
                    int[] feedbackLoc = new int[2];
                    gameFeedback.getLocationOnScreen(feedbackLoc);
                    int flyX = (v1 != null ? loc[0] - feedbackLoc[0] + v1.getWidth() / 2 : 0);
                    int flyY = (v1 != null ? loc[1] - feedbackLoc[1] + v1.getHeight() / 2 : 0);
                    gameFeedback.playFlyingScore(flyX, flyY, 40);
                }
                if (ttsManager != null) {
                    ttsManager.speakPoem(card1.poemTitle, card1.poemAuthor,
                            new String[]{card1.text});
                }

                // 先播放消除动画，动画结束后用 handler.post() 推迟数据更新
                // 避免在 ViewPropertyAnimator 生命周期内触发 notifyDataSetChanged() 导致闪退
                if (v1 != null && v2 != null) {
                    v1.animate().scaleX(0f).scaleY(0f).alpha(0f)
                            .setDuration(250).start();
                    v2.animate().scaleX(0f).scaleY(0f).alpha(0f)
                            .setDuration(250).withEndAction(() -> {
                                handler.post(() -> {
                                    viewModel.tryMatch(card1, card2);
                                    lockInput = false;
                                });
                            }).start();
                } else {
                    // 视图不可用时直接走数据更新
                    handler.post(() -> {
                        viewModel.tryMatch(card1, card2);
                        lockInput = false;
                    });
                }
            } else {
                // === 配对失败 ===
                // 全正面可见：直接抖动归位 + 计一次尝试（不扣分）
                viewModel.tryMatch(card1, card2);

                if (v1 != null) MatchCardAdapter.animateShake(v1);
                if (v2 != null) MatchCardAdapter.animateShake(v2);

                handler.postDelayed(() -> lockInput = false, 300);
            }

            firstSelected = null;
            firstSelectedPosition = -1;
        }, 200);
    }

    /**
     * 游戏完成后导航到结算页。
     * <p>
     * 传参 game_type="match" / score / stars，nav_graph 已定义。
     * 3 星或全对时触发庆祝。
     * </p>
     */
    private void navigateToSettlement(int score, int stars) {
        // 3 星 → 庆祝
        if (stars >= 3) {
            if (getActivity() instanceof com.poetry.MainActivity) {
                ((com.poetry.MainActivity) getActivity()).celebrate();
            }
        }

        Bundle args = new Bundle();
        args.putString("game_type", "match");
        args.putInt("score", score);
        args.putInt("stars", stars);
        args.putString("poem_id", viewModel.getBestPoemId());
        if (getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.nav_game_settlement, args);
        }
    }

    /**
     * Fragment 视图销毁时的回调。
     * <p>
     * 清理 Handler 中的待执行消息和回调，释放 TTS，防止内存泄漏。
     * </p>
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (ttsManager != null) {
            ttsManager.shutdown();
            ttsManager = null;
        }
    }
}
