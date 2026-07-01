package com.poetry.ui.quiz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.ChipGroup;
import com.poetry.R;
import com.poetry.data.model.Poem;
import com.poetry.domain.QuizGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizFragment extends Fragment {

    private QuizViewModel viewModel;

    private TextView tvPoemTitle, tvScore;
    private LinearLayout layoutLines;
    private ViewGroup layoutCandidates;
    private View btnBack, btnTip, btnSubmit, btnNext;
    private List<TextView> blankViews = new ArrayList<>();
    private List<String> userAnswers = new ArrayList<>();
    /** 候选词 chip 视图，用于撤销时恢复 */
    private List<View> candidateChips = new ArrayList<>();
    private int currentBlankIndex = 0;
    /** 用于 View.setTag 的 key，标记候选词对应的空位索引 */
    private static final int TAG_KEY = 0x7f090001;

    public static QuizFragment newInstance() {
        return new QuizFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        viewModel = new ViewModelProvider(this).get(QuizViewModel.class);
        observeData();
        setupListeners();
        viewModel.startQuiz();
    }

    private void initViews(View v) {
        tvPoemTitle = v.findViewById(R.id.tv_title);
        tvScore = v.findViewById(R.id.tv_score);
        layoutLines = v.findViewById(R.id.ll_poem_display);
        layoutCandidates = v.findViewById(R.id.chip_candidates);
        btnSubmit = v.findViewById(R.id.btn_submit);
        btnNext = v.findViewById(R.id.btn_next);
        btnNext.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnSubmit.setOnClickListener(v -> {
            if (userAnswers.size() == blankViews.size()) {
                viewModel.submitAnswer(userAnswers);
                btnSubmit.setEnabled(false);
                btnSubmit.setAlpha(0.5f);
            } else {
                Toast.makeText(requireContext(), "请填完所有空位", Toast.LENGTH_SHORT).show();
            }
        });

        btnNext.setOnClickListener(v -> {
            viewModel.nextQuestion();
        });
    }

    private void observeData() {
        viewModel.getCurrentQuestion().observe(getViewLifecycleOwner(), q -> {
            if (q != null) renderQuestion(q);
        });

        viewModel.getQuestionIndex().observe(getViewLifecycleOwner(), idx -> {
            // Progress tracked in ViewModel
        });

        viewModel.getIsCorrect().observe(getViewLifecycleOwner(), correct -> {
            if (correct != null) {
                btnNext.setVisibility(View.VISIBLE);
                if (correct) {
                    Toast.makeText(requireContext(), "✅ 回答正确！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "❌ 再想想哦~", Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.getIsFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished != null && finished) {
                Integer correct = viewModel.getTotalCorrect().getValue();
                int total = viewModel.getTotalQuestions();
                String msg = "🎉 你完成了 " + total + " 题，答对 " + (correct != null ? correct : 0) + " 题！";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                requireActivity().onBackPressed();
            }
        });
    }

    private void renderQuestion(QuizGenerator.QuizQuestion q) {
        Poem p = q.poem;
        tvPoemTitle.setText(p.title + " — " + p.author + " · " + p.dynasty);

        layoutLines.removeAllViews();
        blankViews.clear();
        userAnswers.clear();
        candidateChips.clear();
        currentBlankIndex = 0;
        btnSubmit.setEnabled(true);
        btnSubmit.setAlpha(1f);
        btnNext.setVisibility(View.GONE);
        layoutCandidates.setEnabled(true);

        // 逐行渲染
        for (int i = 0; i < q.displayLines.length; i++) {
            String line = q.displayLines[i];
            LinearLayout lineLayout = new LinearLayout(requireContext());
            lineLayout.setOrientation(LinearLayout.HORIZONTAL);
            lineLayout.setGravity(android.view.Gravity.CENTER);

            // 按字符或占位符拆分
            int blankIdx = 0;
            StringBuilder current = new StringBuilder();
            for (int c = 0; c < line.length(); c++) {
                if (c + 4 <= line.length() && line.substring(c, c + 4).equals("____")) {
                    if (current.length() > 0) {
                        addTextSegment(lineLayout, current.toString());
                        current.setLength(0);
                    }
                    // 添加空位
                    TextView blankTv = createBlankView(blankIdx);
                    lineLayout.addView(blankTv);
                    blankViews.add(blankTv);
                    userAnswers.add("");
                    c += 3;
                    blankIdx++;
                } else {
                    current.append(line.charAt(c));
                }
            }
            if (current.length() > 0) {
                addTextSegment(lineLayout, current.toString());
            }

            layoutLines.addView(lineLayout);
            // 行间距
            View spacer = new View(requireContext());
            spacer.setLayoutParams(new LinearLayout.LayoutParams(1, 16));
            layoutLines.addView(spacer);
        }

        // 候选词
        renderCandidates(q.candidates, q);
    }

    private TextView createBlankView(int index) {
        TextView tv = new TextView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(4, 0, 4, 0);
        tv.setLayoutParams(lp);
        tv.setText("____");
        tv.setTextSize(18);
        tv.setTextColor(getResources().getColor(R.color.tertiary, null));
        tv.setBackgroundResource(R.drawable.bg_chip);
        tv.setPadding(8, 4, 8, 4);
        tv.setTag(index);
        // 点击已填写的空位 → 撤销
        tv.setOnClickListener(v -> undoBlank(index));
        return tv;
    }

    private void addTextSegment(LinearLayout parent, String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(20);
        tv.setTextColor(getResources().getColor(R.color.on_surface, null));
        tv.setLineSpacing(0f, 1.6f);
        parent.addView(tv);
    }

    private void renderCandidates(List<String> candidates, QuizGenerator.QuizQuestion q) {
        layoutCandidates.removeAllViews();
        candidateChips.clear();
        int rows = (candidates.size() + 2) / 3;
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 6, 0, 6);
            row.setLayoutParams(rowLp);

            for (int c = r * 3; c < Math.min((r + 1) * 3, candidates.size()); c++) {
                final String word = candidates.get(c);
                TextView chip = new TextView(requireContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(6, 0, 6, 0);
                chip.setLayoutParams(lp);
                chip.setText(word);
                chip.setTextSize(18);
                chip.setTextColor(getResources().getColor(R.color.on_surface, null));
                chip.setBackgroundResource(R.drawable.bg_chip_active);
                chip.setPadding(24, 12, 24, 12);
                chip.setClickable(true);
                chip.setFocusable(true);

                chip.setOnClickListener(v -> {
                    if (currentBlankIndex < blankViews.size()) {
                        // 标记该 chip 对应的空位索引，用于撤销
                        v.setTag(TAG_KEY, currentBlankIndex);
                        fillBlank(currentBlankIndex, word);
                        // 标记已使用（变灰）
                        chip.setAlpha(0.4f);
                        chip.setClickable(false);
                    }
                });

                row.addView(chip);
                candidateChips.add(chip);
            }
            layoutCandidates.addView(row);
        }
    }

    private void fillBlank(int index, String word) {
        if (index < blankViews.size()) {
            blankViews.get(index).setText(word);
            blankViews.get(index).setTextColor(getResources().getColor(R.color.answer_correct, null));
            while (userAnswers.size() <= index) {
                userAnswers.add("");
            }
            userAnswers.set(index, word);
            currentBlankIndex = index + 1;
            // 自动跳到下一个空位
            if (currentBlankIndex < blankViews.size()) {
                blankViews.get(currentBlankIndex).setTextColor(getResources().getColor(R.color.tertiary, null));
            }
            // 填满所有空位 → 自动提交
            if (allBlanksFilled()) {
                viewModel.submitAnswer(userAnswers);
                btnSubmit.setEnabled(false);
                btnSubmit.setAlpha(0.5f);
                disableAllCandidates();
            }
        }
    }

    /** 检查是否所有空位都已填写 */
    private boolean allBlanksFilled() {
        if (userAnswers.size() < blankViews.size()) return false;
        for (String ans : userAnswers) {
            if (ans == null || ans.isEmpty()) return false;
        }
        return true;
    }

    /** 禁用所有候选词点击 */
    private void disableAllCandidates() {
        layoutCandidates.setEnabled(false);
        for (int i = 0; i < layoutCandidates.getChildCount(); i++) {
            View child = layoutCandidates.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View chip = row.getChildAt(j);
                    chip.setClickable(false);
                }
            }
        }
    }

    /** 撤销指定空位的填写，恢复候选词 */
    private void undoBlank(int index) {
        if (index < 0 || index >= blankViews.size()) return;
        String currentAnswer = userAnswers.size() > index ? userAnswers.get(index) : "";
        if (currentAnswer == null || currentAnswer.isEmpty()) return; // 未填写，无需撤销

        // 清空空位
        blankViews.get(index).setText("____");
        blankViews.get(index).setTextColor(getResources().getColor(R.color.tertiary, null));
        userAnswers.set(index, "");

        // 恢复对应的候选词 chip
        for (View chip : candidateChips) {
            Object tag = chip.getTag(TAG_KEY);
            if (tag instanceof Integer && (Integer) tag == index) {
                chip.setAlpha(1.0f);
                chip.setClickable(true);
                chip.setTag(TAG_KEY, null);
                break;
            }
        }

        // 回退当前空位索引到该位置
        if (index < currentBlankIndex) {
            currentBlankIndex = index;
        }

        // 恢复提交按钮
        if (!allBlanksFilled()) {
            btnSubmit.setEnabled(true);
            btnSubmit.setAlpha(1.0f);
        }
    }
}
