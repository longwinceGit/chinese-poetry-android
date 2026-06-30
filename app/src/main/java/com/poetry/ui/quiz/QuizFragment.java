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

import com.poetry.R;
import com.poetry.data.model.Poem;
import com.poetry.domain.QuizGenerator;

import java.util.ArrayList;
import java.util.List;

public class QuizFragment extends Fragment {

    private QuizViewModel viewModel;

    private TextView tvPoemTitle, tvPoemAuthor, tvProgress, tvScore;
    private LinearLayout layoutLines, layoutCandidates;
    private View btnBack, btnTip, btnSubmit, btnNext;
    private List<TextView> blankViews = new ArrayList<>();
    private List<String> userAnswers = new ArrayList<>();
    private int currentBlankIndex = 0;

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
        btnBack = v.findViewById(R.id.btn_quiz_back);
        tvPoemTitle = v.findViewById(R.id.tv_quiz_poem_title);
        tvPoemAuthor = v.findViewById(R.id.tv_quiz_poem_author);
        tvProgress = v.findViewById(R.id.tv_quiz_progress);
        tvScore = v.findViewById(R.id.tv_quiz_progress);
        layoutLines = v.findViewById(R.id.layout_quiz_lines);
        layoutCandidates = v.findViewById(R.id.layout_candidates);
        btnTip = v.findViewById(R.id.btn_quiz_tip);
        btnSubmit = v.findViewById(R.id.btn_quiz_submit);
        btnNext = v.findViewById(R.id.btn_quiz_next);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        btnSubmit.setOnClickListener(v -> {
            if (blankViews.isEmpty()) return;
            // 检查所有空位是否已填
            boolean allFilled = true;
            for (TextView tv : blankViews) {
                if ("____".equals(tv.getText().toString())) {
                    allFilled = false;
                    break;
                }
            }
            if (!allFilled) {
                Toast.makeText(requireContext(), "请完成所有填空", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.submitAnswer(userAnswers);
        });

        btnTip.setOnClickListener(v -> {
            // 自动填入当前空位的正确答案
            QuizGenerator.QuizQuestion q = viewModel.getCurrentQuestion().getValue();
            if (q != null && currentBlankIndex < q.blanks.size()) {
                fillBlank(currentBlankIndex, q.blanks.get(currentBlankIndex).answer);
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
            if (idx != null) {
                tvProgress.setText("第" + idx + "/" + viewModel.getTotalQuestions() + "题");
            }
        });

        viewModel.getIsCorrect().observe(getViewLifecycleOwner(), correct -> {
            if (correct != null) {
                btnSubmit.setVisibility(View.GONE);
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
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });
    }

    private void renderQuestion(QuizGenerator.QuizQuestion q) {
        Poem p = q.poem;
        tvPoemTitle.setText(p.title);
        tvPoemAuthor.setText(p.author + " · " + p.dynasty);

        layoutLines.removeAllViews();
        blankViews.clear();
        userAnswers.clear();
        currentBlankIndex = 0;

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
        tv.setTextColor(getResources().getColor(R.color.coral));
        tv.setBackgroundResource(R.drawable.bg_chip);
        tv.setPadding(8, 4, 8, 4);
        tv.setTag(index);
        return tv;
    }

    private void addTextSegment(LinearLayout parent, String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(20);
        tv.setTextColor(getResources().getColor(R.color.text_primary));
        tv.setLineSpacing(0f, 1.6f);
        parent.addView(tv);
    }

    private void renderCandidates(List<String> candidates, QuizGenerator.QuizQuestion q) {
        layoutCandidates.removeAllViews();
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
                chip.setTextColor(getResources().getColor(R.color.text_primary));
                chip.setBackgroundResource(R.drawable.bg_chip_active);
                chip.setPadding(24, 12, 24, 12);
                chip.setClickable(true);
                chip.setFocusable(true);

                chip.setOnClickListener(v -> {
                    if (currentBlankIndex < blankViews.size()) {
                        fillBlank(currentBlankIndex, word);
                        // 标记已使用（变灰）
                        chip.setAlpha(0.4f);
                        chip.setClickable(false);
                    }
                });

                row.addView(chip);
            }
            layoutCandidates.addView(row);
        }
    }

    private void fillBlank(int index, String word) {
        if (index < blankViews.size()) {
            blankViews.get(index).setText(word);
            blankViews.get(index).setTextColor(getResources().getColor(R.color.teal));
            while (userAnswers.size() <= index) {
                userAnswers.add("");
            }
            userAnswers.set(index, word);
            currentBlankIndex = index + 1;
            // 自动跳到下一个空位
            if (currentBlankIndex < blankViews.size()) {
                blankViews.get(currentBlankIndex).setTextColor(getResources().getColor(R.color.coral));
            }
        }
    }
}
