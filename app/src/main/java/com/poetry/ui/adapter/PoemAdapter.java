package com.poetry.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.poetry.data.model.Poem;

import java.util.ArrayList;
import java.util.List;

/**
 * 诗词列表适配器（RecyclerView 适配器，3 列网格）。
 * <p>
 * 用于展示诗词卡片列表，每项包含 Emoji、标题与作者·朝代信息。
 * 点击时使用 {@link OvershootInterpolator} 实现按压回弹动画效果
 * （先缩小至 0.92 再弹回 1.0，张力系数 2）。
 * </p>
 */
public class PoemAdapter extends RecyclerView.Adapter<PoemAdapter.ViewHolder> {

    private List<Poem> poems = new ArrayList<>();
    private OnPoemClickListener listener;

    /** 诗词点击回调接口 */
    public interface OnPoemClickListener {
        void onPoemClick(Poem poem, int position);
    }

    /**
     * 构造适配器并绑定点击回调。
     *
     * @param listener 诗词点击事件回调
     */
    public PoemAdapter(OnPoemClickListener listener) {
        this.listener = listener;
    }

    /**
     * 替换当前诗词列表并刷新全部数据。
     * <p>
     * 若传入列表为 null，则清空为空列表。
     * </p>
     *
     * @param poems 新的诗词列表
     */
    public void setPoems(List<Poem> poems) {
        this.poems = poems != null ? poems : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * 在当前列表末尾追加诗词并通知增量更新。
     *
     * @param poems 待追加的诗词列表
     */
    public void addPoems(List<Poem> poems) {
        int start = this.poems.size();
        this.poems.addAll(poems);
        notifyItemRangeInserted(start, poems.size());
    }

    /**
     * 获取指定位置的诗词数据。
     *
     * @param position 列表位置
     * @return 对应位置的 {@link Poem} 对象
     */
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
            .inflate(com.poetry.R.layout.item_poem_card, parent, false);
        return new ViewHolder(view);
    }

    /**
     * 绑定诗词数据到卡片视图，并设置点击动画与回调。
     * <p>
     * 点击时先缩小 {@code scaleX/Y} 至 0.92（80ms），再使用
     * {@link OvershootInterpolator}(张力=2) 弹回 1.0（250ms），
     * 随后触发 {@link OnPoemClickListener#onPoemClick} 回调。
     * </p>
     *
     * @param holder  ViewHolder
     * @param position 列表位置
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Poem p = poems.get(position);
        holder.tvEmoji.setText(p.emoji);
        holder.tvTitle.setText(p.title);
        holder.tvAuthor.setText(p.author + " · " + p.dynasty);

        // 为诗词卡片设置无障碍描述
        holder.itemView.setContentDescription(
            holder.itemView.getContext().getString(
                com.poetry.R.string.a11y_poem_card_format,
                p.title, p.author, p.dynasty));

        holder.itemView.setOnClickListener(v -> {
            v.animate()
                .scaleX(0.92f).scaleY(0.92f).setDuration(80)
                .withEndAction(() ->
                    v.animate()
                        .scaleX(1f).scaleY(1f).setDuration(250)
                        .setInterpolator(new OvershootInterpolator(2f))
                        .start()
                ).start();
            if (listener != null) listener.onPoemClick(p, position);
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvTitle, tvAuthor;
        ViewHolder(View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(com.poetry.R.id.tv_emoji);
            tvTitle = itemView.findViewById(com.poetry.R.id.tv_title);
            tvAuthor = itemView.findViewById(com.poetry.R.id.tv_author);
        }
    }
}
