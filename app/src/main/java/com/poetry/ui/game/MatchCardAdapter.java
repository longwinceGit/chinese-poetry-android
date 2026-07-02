package com.poetry.ui.game;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.poetry.R;
import com.poetry.domain.GameEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * 消消乐卡片适配器（RecyclerView 适配器）。
 * <p>
 * 管理匹配卡片的列表渲染，根据卡片状态（正常/选中/已消除）动态设置不同的
 * {@link GradientDrawable} 背景：正常态为 surface 色 + 分割线边框，选中态为
 * tertiary 配色 + 微缩放效果，已消除态为绿色半透明。同时提供消除动画和错误抖动动画。
 * </p>
 */
public class MatchCardAdapter extends RecyclerView.Adapter<MatchCardAdapter.CardViewHolder> {

    private List<GameEngine.MatchCard> cards = new ArrayList<>();
    private OnCardClickListener listener;

    public interface OnCardClickListener {
        void onCardClick(GameEngine.MatchCard card, int position);
    }

    public MatchCardAdapter(OnCardClickListener listener) {
        this.listener = listener;
    }

    /**
     * 设置卡片数据列表并刷新 RecyclerView。
     *
     * @param newCards 新的卡片数据列表，为 null 时自动替换为空列表
     */
    public void setCards(List<GameEngine.MatchCard> newCards) {
        this.cards = newCards != null ? newCards : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * 程序化创建卡片视图，使用 {@link TextView} 作为卡片容器，确保无布局依赖。
     * 设置居中、内边距、字号等样式属性。
     *
     * @param parent   父 ViewGroup
     * @param viewType 视图类型（未使用）
     * @return 持有 TextView 的 CardViewHolder
     */
    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        int dp8 = dp2px(parent.getContext(), 4);
        int dp12 = dp2px(parent.getContext(), 6);
        tv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp2px(parent.getContext(), 64)
        ));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp12, dp8, dp12, dp8);
        tv.setTextSize(14f);
        tv.setMaxLines(2);
        tv.setLineSpacing(2f, 1.0f);
        return new CardViewHolder(tv);
    }

    /**
     * 绑定卡片数据到视图，根据卡片的三态设置不同的 {@link GradientDrawable} 背景：
     * <ul>
     *   <li><b>已消除（matched）</b>：绿色半透明背景，不可点击</li>
     *   <li><b>选中（selected）</b>：tertiary 配色边框 + 浅色背景 + 1.04 倍微缩放</li>
     *   <li><b>正常</b>：surface 色背景 + divider 边框</li>
     * </ul>
     * 每次绑定时重新创建背景 drawable 并清除旧监听器后再设置点击事件。
     *
     * @param holder   CardViewHolder
     * @param position 卡片在列表中的位置
     */
    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        GameEngine.MatchCard card = cards.get(position);
        TextView tv = holder.textView;
        Context ctx = tv.getContext();

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp2px(ctx, 6));
        // 状态决定外观
        if (card.matched) {
            // 已消除：隐藏卡片，不占视觉空间
            tv.setVisibility(View.INVISIBLE);
            tv.setScaleX(1.0f);
            tv.setScaleY(1.0f);
            tv.setAlpha(1.0f);
            tv.setClickable(false);
        } else if (card.selected) {
            // 选中状态前先确保可见
            tv.setVisibility(View.VISIBLE);
            // 选中状态：赭石色边框 + 浅色背景 + 微缩放
            bg.setColor(ContextCompat.getColor(ctx, R.color.tertiary_container));
            bg.setStroke(dp2px(ctx, 1.5f), ContextCompat.getColor(ctx, R.color.tertiary));
            tv.setText(card.isFirstHalf ? "📜 " + card.text : "🎋 " + card.text);
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.on_tertiary_container));
            tv.setAlpha(1.0f);
            tv.setClickable(true);
            tv.setScaleX(1.04f);
            tv.setScaleY(1.04f);
        } else {
            // 正常状态
            tv.setVisibility(View.VISIBLE);
            bg.setColor(ContextCompat.getColor(ctx, R.color.surface));
            bg.setStroke(1, ContextCompat.getColor(ctx, R.color.divider));
            tv.setText(card.isFirstHalf ? "📜 " + card.text : "🎋 " + card.text);
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.on_surface));
            tv.setAlpha(1.0f);
            tv.setClickable(true);
            tv.setScaleX(1.0f);
            tv.setScaleY(1.0f);
        }

        tv.setBackground(bg);

        // 为消消乐卡片设置无障碍描述
        String cardLabel = card.isFirstHalf ? "上句：" : "下句：";
        tv.setContentDescription(ctx.getString(R.string.a11y_match_card_format, cardLabel + card.text));

        // 清除旧监听器，重新设置
        tv.setOnClickListener(v -> {
            if (listener != null && !card.matched) {
                listener.onCardClick(card, holder.getBindingAdapterPosition());
            }
        });
    }

    /**
     * 返回卡片列表总数。
     *
     * @return 当前卡片数量
     */
    @Override
    public int getItemCount() {
        return cards.size();
    }

    // ==================== 动画辅助 ====================

    /**
     * 错误抖动动画：依次执行右→左→右→原位的水平位移序列，
     * 每步 50ms，产生类似手机振动反馈的效果。
     *
     * @param view 要执行抖动动画的视图
     */
    public static void animateShake(View view) {
        view.animate()
                .translationX(0f)
                .setDuration(50)
                .withEndAction(() ->
                    view.animate().translationX(12f).setDuration(50).withEndAction(() ->
                        view.animate().translationX(-12f).setDuration(50).withEndAction(() ->
                            view.animate().translationX(8f).setDuration(50).withEndAction(() ->
                                view.animate().translationX(0f).setDuration(50).start()
                            )
                        )
                    )
                );
    }

    /**
     * 将 dp 单位转换为像素值。
     *
     * @param ctx 上下文，用于获取屏幕密度
     * @param dp  dp 值
     * @return 对应的像素值（四舍五入取整）
     */
    private static int dp2px(Context ctx, float dp) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }
}
