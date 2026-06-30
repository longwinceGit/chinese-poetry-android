package com.poetry;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * 诗词卡片网格适配器
 */
public class PoemAdapter extends RecyclerView.Adapter<PoemAdapter.ViewHolder> {

    private List<Poem> poems = new ArrayList<>();
    private OnPoemClickListener listener;

    public interface OnPoemClickListener {
        void onPoemClick(Poem poem, int position);
    }

    public PoemAdapter(OnPoemClickListener listener) {
        this.listener = listener;
    }

    public void setPoems(List<Poem> poems) {
        this.poems = poems != null ? poems : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addPoems(List<Poem> poems) {
        int start = this.poems.size();
        this.poems.addAll(poems);
        notifyItemRangeInserted(start, poems.size());
    }

    public Poem getItem(int position) {
        return poems.get(position);
    }

    @Override
    public int getItemCount() {
        return poems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_poem_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Poem p = poems.get(position);
        holder.tvEmoji.setText(p.emoji);
        holder.tvTitle.setText(p.title);
        holder.tvAuthor.setText(p.author + " · " + p.dynasty);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPoemClick(p, position);
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvTitle, tvAuthor;
        ViewHolder(View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tv_emoji);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
        }
    }
}
