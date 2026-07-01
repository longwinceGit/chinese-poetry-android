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

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {

    private List<AchievementEntry> entries = new ArrayList<>();

    public static class AchievementEntry {
        public AchievementEngine.AchievementDef def;
        public boolean unlocked;
        public AchievementEntry(AchievementEngine.AchievementDef def, boolean unlocked) {
            this.def = def; this.unlocked = unlocked;
        }
    }

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
