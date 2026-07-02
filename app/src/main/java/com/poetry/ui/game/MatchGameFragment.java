package com.poetry.ui.game;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.poetry.R;
import com.poetry.domain.GameEngine;

/**
 * 消消乐 Fragment（3×4 网格，选中配对消除）
 * <p>
 * 实现诗词消消乐游戏界面，玩家需要在 3×4 的网格中
 * 找到并配对属于同一首诗的上句和下句。
 * </p>
 * <p>
 * 核心交互逻辑：
 * 1. 点击第一张卡片选中高亮
 * 2. 点击第二张卡片尝试配对
 * 3. 配对成功：播放消除动画，卡片消失
 * 4. 配对失败：播放抖动动画，取消选中
 * 5. 动画播放期间锁定输入，防止误操作
 * </p>
 */
public class MatchGameFragment extends Fragment {

    private GameViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private RecyclerView recyclerCards;
    private TextView tvProgress, tvTip;
    private MaterialButton btnRestart;
    private View layoutComplete;
    private TextView tvCompleteScore;

    private MatchCardAdapter adapter;
    private GameEngine.MatchCard firstSelected;    // 第一张选中的卡片
    private int firstSelectedPosition = -1;          // 第一张卡片的位置
    private boolean lockInput = false;               // 动画播放期间锁定输入

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
        viewModel.startMatchGame();
    }

    /**
     * 初始化界面控件。
     * <p>
     * 绑定布局中的视图元素，设置 RecyclerView 的网格布局管理器和适配器，
     * 配置重新开始按钮的点击事件。
     * </p>
     *
     * @param v Fragment 的根视图
     */
    private void initViews(View v) {
        recyclerCards = v.findViewById(R.id.recycler_cards);
        tvProgress = v.findViewById(R.id.tv_match_progress);
        tvTip = v.findViewById(R.id.tv_match_tip);
        btnRestart = v.findViewById(R.id.btn_match_restart);
        layoutComplete = v.findViewById(R.id.layout_match_complete);
        tvCompleteScore = v.findViewById(R.id.tv_match_complete_score);

        recyclerCards.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        adapter = new MatchCardAdapter(this::onCardClick);
        recyclerCards.setAdapter(adapter);

        btnRestart.setOnClickListener(v2 -> {
            firstSelected = null;
            firstSelectedPosition = -1;
            lockInput = false;
            layoutComplete.setVisibility(View.GONE);
            tvTip.setVisibility(View.GONE);
            viewModel.startMatchGame();
        });
    }

    /**
     * 观察 ViewModel 中的数据变化。
     * <p>
     * 注册以下 LiveData 观察者：
     * 1. 卡片列表 - 更新 RecyclerView 显示
     * 2. 已配对数量 - 更新进度文本
     * 3. 配对成功提示 - 显示诗词信息（3秒后自动隐藏）
     * 4. 游戏完成状态 - 显示完成界面和得分
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

        // 游戏完成
        viewModel.getMatchFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished != null && finished) {
                Integer attempts = viewModel.getMatchAttempts().getValue();
                int score = GameEngine.calcMatchScore(
                        attempts != null ? attempts : 0,
                        viewModel.getMatchPairs());
                tvCompleteScore.setText("得分 " + score + "  ·  尝试 " + (attempts != null ? attempts : 0) + " 次");
                layoutComplete.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(),
                        "🎉 全部消除！共尝试 " + (attempts != null ? attempts : 0) + " 次",
                        Toast.LENGTH_LONG).show();
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
     * 卡片点击处理。
     * <p>
     * 处理消消乐游戏中的卡片点击逻辑：
     * 1. 如果尚未选中任何卡片，选中当前卡片并高亮
     * 2. 如果点击已选中的卡片，取消选中
     * 3. 如果已选中一张卡片，尝试与第二张卡片配对
     * </p>
     * <p>
     * 配对结果处理：
     * - 成功：播放消除动画，卡片标记为已匹配
     * - 失败：播放抖动动画，取消两张卡片的选中状态
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

        // 短暂延迟让用户看到选中状态，然后判断结果
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
                // 走原有逻辑：取消选中 + 抖动 + 更新尝试次数
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
     * Fragment 视图销毁时的回调。
     * <p>
     * 清理 Handler 中的待执行消息和回调，防止内存泄漏。
     * </p>
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}
