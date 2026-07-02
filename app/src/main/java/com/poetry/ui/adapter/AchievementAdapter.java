package com.poetry.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.poetry.R;
import com.poetry.domain.AchievementEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * 成就列表适配器。
 * <p>
 * 用于展示成就条目列表，每项包含图标、名称、描述与达成状态。
 * 已达成的成就显示为正常亮度（alpha=1）和绿色状态文字；
 * 未达成的成就显示为半透明（alpha=0.5）和灰色状态文字，
 * 通过视觉区分形成解锁/未解锁的明显对比。
 * </p>
 */
public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {

    private List<AchievementEntry> entries = new ArrayList<>();

    /**
     * 成就条目数据模型，封装成就定义与解锁状态。
     */
    public static class AchievementEntry {
        /** 成就定义（含图标、名称、描述等元信息） */
        public AchievementEngine.AchievementDef def;
        /** 是否已解锁达成 */
        public boolean unlocked;
        /**
         * 构造成就条目。
         *
         * @param def     成就定义
         * @param unlocked 是否已解锁
         */
        public AchievementEntry(AchievementEngine.AchievementDef def, boolean unlocked) {
            this.def = def; this.unlocked = unlocked;
        }
    }

    /**
     * 替换当前成就条目列表并刷新全部数据。
     * <p>
     * 若传入列表为 null，则清空为空列表。
     * </p>
     *
     * @param entries 新的成就条目列表
     */
    public void setEntries(List<AchievementEntry> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return entries.size(); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(v);
    }

    /**
     * 绑定成就数据到卡片视图，根据解锁状态切换视觉样式。
     * <p>
     * 已解锁：状态文字为"已达成"并使用绿色（{@link R.color#answer_correct}），
     * 整体透明度 1.0；
     * 未解锁：状态文字为"未达成"并使用灰色（{@link R.color#on_surface_variant}），
     * 整体透明度 0.5，形成低调的视觉提示。
     * </p>
     *
     * @param holder  ViewHolder
     * @param position 列表位置
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AchievementEntry entry = entries.get(position);
        holder.tvIcon.setText(entry.def.icon);
        holder.tvName.setText(entry.def.name);
        holder.tvDesc.setText(entry.def.desc);

        if (entry.unlocked) {
            holder.tvStatus.setText("已达成");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.answer_correct));
            holder.itemView.setAlpha(1f);
        } else {
            holder.tvStatus.setText("未达成");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.on_surface_variant));
            holder.itemView.setAlpha(0.5f);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvDesc, tvStatus;
        ViewHolder(View v) {
            super(v);
            tvIcon = v.findViewById(R.id.tv_achv_icon);
            tvName = v.findViewById(R.id.tv_achv_name);
            tvDesc = v.findViewById(R.id.tv_achv_desc);
            tvStatus = v.findViewById(R.id.tv_achv_status);
        }
    }
}
