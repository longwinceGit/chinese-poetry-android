package com.poetry.ui.debug;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.poetry.R;
import com.poetry.util.DebugLogger;

import java.io.File;
import java.util.List;

/**
 * 调试日志面板 —— 手机上实时查看应用运行日志，用于分析问题。
 * <p>
 * 功能：实时滚动刷新（{@link DebugLogger.Listener}，100ms 节流）、级别过滤（Chip）、
 * 关键词过滤（tag/内容）、一键清空、复制全部、导出为 txt 文件。
 * 顶部开关同步 {@link DebugLogger#isEnabled()}，关闭后停止内存收集。
 */
public class DebugLogFragment extends Fragment {

    private TextView tvLogCount;
    private SwitchMaterial switchCollect;
    private EditText etFilter;
    private ChipGroup chipLevel;
    private RecyclerView recyclerLog;
    private TextView tvEmpty;

    private DebugLogAdapter adapter;
    private LinearLayoutManager layoutManager;

    private int currentMinLevel = Log.VERBOSE;
    private String currentKeyword = "";

    private final DebugLogger.Listener loggerListener = this::refresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debug_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        bindListeners();
    }

    private void initViews(View v) {
        tvLogCount = v.findViewById(R.id.tv_log_count);
        switchCollect = v.findViewById(R.id.switch_collect);
        etFilter = v.findViewById(R.id.et_log_filter);
        chipLevel = v.findViewById(R.id.chip_group_level);
        recyclerLog = v.findViewById(R.id.recycler_log);
        tvEmpty = v.findViewById(R.id.tv_log_empty);

        layoutManager = new LinearLayoutManager(requireContext());
        recyclerLog.setLayoutManager(layoutManager);
        adapter = new DebugLogAdapter();
        recyclerLog.setAdapter(adapter);
    }

    private void bindListeners() {
        switchCollect.setChecked(DebugLogger.isEnabled());
        switchCollect.setOnCheckedChangeListener((button, checked) ->
                DebugLogger.setEnabled(checked));

        chipLevel.setOnCheckedStateChangeListener((group, checkedIds) -> {
            currentMinLevel = resolveMinLevel();
            refresh();
        });

        etFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                currentKeyword = s == null ? "" : s.toString();
                refresh();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        View root = requireView();
        root.findViewById(R.id.btn_log_clear).setOnClickListener(x -> {
            DebugLogger.clear();
            refresh();
            Toast.makeText(requireContext(), R.string.debug_log_cleared, Toast.LENGTH_SHORT).show();
        });
        root.findViewById(R.id.btn_log_copy).setOnClickListener(x -> copyAll());
        root.findViewById(R.id.btn_log_export).setOnClickListener(x -> exportFile());
    }

    /** 根据当前选中的级别 Chip 解析最低日志级别。 */
    private int resolveMinLevel() {
        int checkedId = chipLevel.getCheckedChipId();
        if (checkedId == R.id.chip_level_debug) return Log.DEBUG;
        if (checkedId == R.id.chip_level_info) return Log.INFO;
        if (checkedId == R.id.chip_level_warn) return Log.WARN;
        if (checkedId == R.id.chip_level_error) return Log.ERROR;
        return Log.VERBOSE; // 全部
    }

    /** 拉取过滤后的日志并刷新列表（监听器在主线程回调）。 */
    private void refresh() {
        List<DebugLogger.Entry> list = DebugLogger.query(currentMinLevel, currentKeyword);
        adapter.setEntries(list);

        tvLogCount.setText(getString(R.string.debug_log_count, list.size()));
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

        // 用户停留在顶部附近时自动滚到底部，避免打断阅读
        if (!recyclerLog.canScrollVertically(-1)) {
            layoutManager.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    /** 复制过滤后的日志文本到剪贴板。 */
    private void copyAll() {
        String text = DebugLogger.exportText(currentMinLevel, currentKeyword);
        ClipboardManager cm = (ClipboardManager) requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("debug_log", text));
        Toast.makeText(requireContext(),
                getString(R.string.debug_log_copied, adapter.getEntryCount()),
                Toast.LENGTH_SHORT).show();
    }

    /** 导出过滤后的日志为 txt 文件。 */
    private void exportFile() {
        File file = DebugLogger.exportFile(currentMinLevel, currentKeyword);
        if (file != null) {
            Toast.makeText(requireContext(),
                    getString(R.string.debug_log_exported, file.getAbsolutePath()),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), R.string.debug_log_export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        DebugLogger.addListener(loggerListener);
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        DebugLogger.removeListener(loggerListener);
    }
}
