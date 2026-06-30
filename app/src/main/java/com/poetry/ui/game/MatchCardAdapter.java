package com.poetry.ui.game;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.poetry.R;
import com.poetry.domain.GameEngine;

import java.util.List;

public class MatchCardAdapter extends BaseAdapter {

    private Context context;
    private List<GameEngine.MatchCard> cards;
    private OnCardClickListener listener;

    public interface OnCardClickListener {
        void onCardClick(GameEngine.MatchCard card);
    }

    public MatchCardAdapter(Context context, List<GameEngine.MatchCard> cards, OnCardClickListener listener) {
        this.context = context;
        this.cards = cards;
        this.listener = listener;
    }

    @Override
    public int getCount() { return cards.size(); }

    @Override
    public Object getItem(int position) { return cards.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv;
        if (convertView == null) {
            tv = new TextView(context);
            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    120
            ));
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(8, 6, 8, 6);
            tv.setTextSize(13f);
            tv.setMaxLines(2);
        } else {
            tv = (TextView) convertView;
        }

        GameEngine.MatchCard card = cards.get(position);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(12);
        drawable.setStroke(2, Color.parseColor("#E0E0E0"));

        if (card.matched) {
            drawable.setColor(Color.parseColor("#C8E6C9"));
            tv.setText(card.text);
            tv.setTextColor(Color.parseColor("#2E7D32"));
            tv.setClickable(false);
        } else {
            drawable.setColor(Color.parseColor("#FFFFFF"));
            tv.setText(card.text);
            tv.setTextColor(Color.parseColor("#333333"));
            tv.setClickable(true);
        }

        tv.setBackground(drawable);
        tv.setOnClickListener(v -> {
            if (listener != null && !card.matched) {
                listener.onCardClick(card);
            }
        });

        return tv;
    }
}
