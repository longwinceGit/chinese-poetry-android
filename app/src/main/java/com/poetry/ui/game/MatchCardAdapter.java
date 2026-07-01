package com.poetry.ui.game;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.poetry.R;
import com.poetry.domain.GameEngine;

import java.util.List;

public class MatchCardAdapter extends RecyclerView.Adapter<MatchCardAdapter.CardViewHolder> {

    private List<GameEngine.MatchCard> cards;
    private OnCardClickListener listener;

    public interface OnCardClickListener {
        void onCardClick(GameEngine.MatchCard card);
    }

    public MatchCardAdapter(List<GameEngine.MatchCard> cards, OnCardClickListener listener) {
        this.cards = cards;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                120
        ));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(8, 6, 8, 6);
        tv.setTextSize(13f);
        tv.setMaxLines(2);
        return new CardViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        GameEngine.MatchCard card = cards.get(position);
        TextView tv = holder.textView;
        Context ctx = tv.getContext();

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(12);
        drawable.setStroke(2, ContextCompat.getColor(ctx, R.color.divider));

        if (card.matched) {
            drawable.setColor(ContextCompat.getColor(ctx, R.color.match_card_success_bg));
            tv.setText(card.text);
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.answer_correct));
            tv.setClickable(false);
        } else {
            drawable.setColor(ContextCompat.getColor(ctx, R.color.surface));
            tv.setText(card.text);
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.on_surface));
            tv.setClickable(true);
        }

        tv.setBackground(drawable);
        tv.setOnClickListener(v -> {
            if (listener != null && !card.matched) {
                listener.onCardClick(card);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }
}
