package com.poetry.ui.favorites;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.poetry.R;
import com.poetry.data.LearningRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * 收藏列表适配器。
 * <p>
 * 以简单的卡片列表形式展示收藏的诗词，每项包含标题、作者·朝代信息，
 * 以及取消收藏的心形按钮。点击卡片跳转详情页。
 * </p>
 */
public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {

    private List<LearningRecord> records = new ArrayList<>();
    private OnItemClickListener listener;
    private boolean showUnfavButton = true;

    /** 列表项点击回调接口 */
    public interface OnItemClickListener {
        /** 点击卡片跳转诗词详情 */
        void onItemClick(LearningRecord record, int position);
        /** 点击心形按钮取消收藏 */
        void onUnfavoriteClick(LearningRecord record, int position);
    }

    /**
     * 构造适配器并绑定回调。
     *
     * @param listener 点击事件回调
     */
    public FavoriteAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * 替换当前数据并刷新列表。
     *
     * @param records 新的收藏记录列表
     */
    public void setRecords(List<LearningRecord> records) {
        this.records = records != null ? records : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * 设置是否显示取消收藏/操作按钮。
     *
     * @param show true 显示按钮（收藏列表），false 隐藏（已学列表）
     */
    public void setShowUnfavButton(boolean show) {
        this.showUnfavButton = show;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LearningRecord r = records.get(position);
        holder.tvTitle.setText(r.title != null ? r.title : "未知诗词");
        holder.tvAuthor.setText(
            (r.author != null ? r.author : "佚名")
            + " · "
            + (r.dynasty != null ? r.dynasty : "未知朝代"));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(r, position);
        });

        holder.btnUnfav.setVisibility(showUnfavButton ? View.VISIBLE : View.GONE);
        holder.btnUnfav.setOnClickListener(v -> {
            if (listener != null) listener.onUnfavoriteClick(r, position);
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor, btnUnfav;
        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            btnUnfav = itemView.findViewById(R.id.btn_unfav);
        }
    }
}
