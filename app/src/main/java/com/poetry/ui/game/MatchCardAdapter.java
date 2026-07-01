package com.poetry.ui.game;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.poetry.R;
import com.poetry.domain.GameEngine;

import java.util.ArrayList;
import java.util.List;

public class MatchCardAdapter extends RecyclerView.Adapter<MatchCardAdapter.CardViewHolder> {

    private List<GameEngine.MatchCard> cards = new ArrayList<>();
    private OnCardClickListener listener;

    public interface OnCardClickListener {
        void onCardClick(GameEngine.MatchCard card, int position);
    }

    public MatchCardAdapter(OnCardClickListener listener) {
        this.listener = listener;
    }

    public void setCards(List<GameEngine.MatchCard> newCards) {
        this.cards = newCards != null ? newCards : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 用程序化创建 TextView 确保无布局依赖
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

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        GameEngine.MatchCard card = cards.get(position);
        TextView tv = holder.textView;
        Context ctx = tv.getContext();

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp2px(ctx, 6));

        // 状态决定外观
        if (card.matched) {
            // 已消除：半透明、绿色背景
            bg.setColor(ContextCompat.getColor(ctx, R.color.match_card_success_bg));
            bg.setStroke(1, ContextCompat.getColor(ctx, R.color.answer_correct));
            tv.setText(card.text);
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.answer_correct));
            tv.setAlpha(0.45f);
            tv.setClickable(false);
        } else if (card.selected) {
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

        // 清除旧监听器，重新设置
        tv.setOnClickListener(v -> {
            if (listener != null && !card.matched) {
                listener.onCardClick(card, holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    // ==================== 动画辅助 ====================

    /** 消除动画：卡片缩小 + 淡出 */
    public static void animateEliminate(View view, Runnable onEnd) {
        view.animate()
                .scaleX(0.3f)
                .scaleY(0.3f)
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .withEndAction(onEnd)
                .start();
    }

    /** 错误抖动动画 */
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
