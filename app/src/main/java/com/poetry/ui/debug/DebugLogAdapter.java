package com.poetry.ui.debug;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.poetry.R;
import com.poetry.util.DebugLogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 调试日志列表适配器。
 * <p>每行展示：时间（HH:mm:ss.SSS）＋级别色块徽标（V/D/I/W/E）＋tag（粗体）＋消息。
 * 行视图程序化创建，避免新增 item 布局文件。级别色与现有主题色对齐：V 灰 / D 绿 / I 蓝 / W 橙 / E 红。
 */
public class DebugLogAdapter extends RecyclerView.Adapter<DebugLogAdapter.LogViewHolder> {

    private final List<DebugLogger.Entry> entries = new ArrayList<>();
    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT);

    /**
     * 刷新数据（替换全量列表）。
     *
     * @param newEntries 过滤后的日志列表
     */
    public void setEntries(List<DebugLogger.Entry> newEntries) {
        entries.clear();
        if (newEntries != null) {
            entries.addAll(newEntries);
        }
        notifyDataSetChanged();
    }

    public int getEntryCount() {
        return entries.size();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LogViewHolder(createRowView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        DebugLogger.Entry e = entries.get(position);

        holder.tvTime.setText(TIME_FMT.format(new Date(e.timeMillis)));

        holder.tvLevel.setText(String.valueOf(levelChar(e.level)));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp2px(holder.tvLevel.getContext(), 4));
        bg.setColor(levelColor(holder.tvLevel.getContext(), e.level));
        holder.tvLevel.setBackground(bg);

        holder.tvTag.setText(e.tag);
        holder.tvMessage.setText(e.message);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    /** 程序化创建一行日志视图（横向：时间 / 级别徽标 / 纵向 tag+消息）。 */
    private static View createRowView(Context ctx) {
        int dp4 = dp2px(ctx, 4);
        int dp8 = dp2px(ctx, 8);
        int dp12 = dp2px(ctx, 12);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.TOP);
        root.setPadding(dp12, dp8, dp12, dp8);

        TextView tvTime = new TextView(ctx);
        tvTime.setTextSize(10f);
        tvTime.setTextColor(ContextCompat.getColor(ctx, R.color.text_caption));
        tvTime.setPadding(0, dp4, dp8, 0);
        root.addView(tvTime, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvLevel = new TextView(ctx);
        tvLevel.setTextSize(11f);
        tvLevel.setTextColor(ContextCompat.getColor(ctx, R.color.white));
        tvLevel.setGravity(Gravity.CENTER);
        tvLevel.setPadding(dp8, dp2px(ctx, 2), dp8, dp2px(ctx, 2));
        root.addView(tvLevel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp8, 0, 0, 0);

        TextView tvTag = new TextView(ctx);
        tvTag.setTextSize(12f);
        tvTag.setTextColor(ContextCompat.getColor(ctx, R.color.text_title));
        tvTag.setTypeface(tvTag.getTypeface(), android.graphics.Typeface.BOLD);
        body.addView(tvTag, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvMessage = new TextView(ctx);
        tvMessage.setTextSize(13f);
        tvMessage.setTextColor(ContextCompat.getColor(ctx, R.color.text_body));
        tvMessage.setLineSpacing(2f, 1.0f);
        body.addView(tvMessage, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(body, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return root;
    }

    private static char levelChar(int level) {
        switch (level) {
            case Log.DEBUG: return 'D';
            case Log.INFO:  return 'I';
            case Log.WARN:  return 'W';
            case Log.ERROR: return 'E';
            default:        return 'V';
        }
    }

    private static int levelColor(Context ctx, int level) {
        switch (level) {
            case Log.DEBUG: return ContextCompat.getColor(ctx, R.color.answer_correct);
            case Log.INFO:  return ContextCompat.getColor(ctx, R.color.tag_yuan);
            case Log.WARN:  return ContextCompat.getColor(ctx, R.color.tag_wei);
            case Log.ERROR: return ContextCompat.getColor(ctx, R.color.answer_wrong);
            default:        return ContextCompat.getColor(ctx, R.color.tag_other);
        }
    }

    private static int dp2px(Context ctx, float dp) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTime;
        final TextView tvLevel;
        final TextView tvTag;
        final TextView tvMessage;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            LinearLayout root = (LinearLayout) itemView;
            tvTime = (TextView) root.getChildAt(0);
            tvLevel = (TextView) root.getChildAt(1);
            LinearLayout body = (LinearLayout) root.getChildAt(2);
            tvTag = (TextView) body.getChildAt(0);
            tvMessage = (TextView) body.getChildAt(1);
        }
    }
}
