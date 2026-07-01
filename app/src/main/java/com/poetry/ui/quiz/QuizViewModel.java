package com.poetry.ui.quiz;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.PoemRepository;
import com.poetry.data.UserProfile;
import com.poetry.data.model.Poem;
import com.poetry.domain.LearningEngine;
import com.poetry.domain.QuizGenerator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class QuizViewModel extends AndroidViewModel {

    private PoemRepository repo = PoemRepository.getInstance();
    private LearningDatabase db;

    private MutableLiveData<QuizGenerator.QuizQuestion> currentQuestion = new MutableLiveData<>();
    private MutableLiveData<Integer> score = new MutableLiveData<>(0);
    private MutableLiveData<Integer> questionIndex = new MutableLiveData<>(0);
    private MutableLiveData<Boolean> isCorrect = new MutableLiveData<>(null);
    private MutableLiveData<Boolean> isFinished = new MutableLiveData<>(false);
    private MutableLiveData<Integer> totalCorrect = new MutableLiveData<>(0);

    private List<QuizGenerator.QuizQuestion> questions = new ArrayList<>();
    private static final int TOTAL_QUESTIONS = 5;

    public QuizViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    public void startQuiz() {
        questions.clear();
        List<Poem> all = repo.getAllPoems();
        if (all.isEmpty()) return;

        // 随机选5首诗生成填空题
        List<Poem> shuffled = new ArrayList<>(all);
        java.util.Collections.shuffle(shuffled);
        int count = 0;
        for (Poem p : shuffled) {
            QuizGenerator.QuizQuestion q = QuizGenerator.generateFillBlank(p);
            if (q != null) {
                questions.add(q);
                count++;
                if (count >= TOTAL_QUESTIONS) break;
            }
        }

        score.setValue(0);
        questionIndex.setValue(0);
        totalCorrect.setValue(0);
        isCorrect.setValue(null);
        isFinished.setValue(false);
        showQuestion(0);
    }

    public void showQuestion(int index) {
        if (index < questions.size()) {
            currentQuestion.setValue(questions.get(index));
            questionIndex.setValue(index + 1);
            isCorrect.setValue(null);
        } else {
            finishQuiz();
        }
    }

    public void submitAnswer(List<String> userAnswers) {
        QuizGenerator.QuizQuestion q = currentQuestion.getValue();
        if (q == null) return;

        boolean allCorrect = true;
        for (int i = 0; i < q.blanks.size(); i++) {
            if (i >= userAnswers.size() || !userAnswers.get(i).equals(q.blanks.get(i).answer)) {
                allCorrect = false;
                break;
            }
        }

        isCorrect.setValue(allCorrect);
        if (allCorrect) {
            int correct = (totalCorrect.getValue() != null ? totalCorrect.getValue() : 0) + 1;
            totalCorrect.setValue(correct);
        }

        // 保存积分 + 更新每日统计
        final boolean finalCorrect = allCorrect;
        final int blanksCount = q.blanks.size();
        final String poemId = q.poem.id;
        new Thread(() -> {
            UserProfile profile = db.poemDao().getUserProfileSync();
            if (profile == null) {
                profile = new UserProfile();
            }
            int points = LearningEngine.calcPointsForQuiz(finalCorrect ? blanksCount : 0, blanksCount);
            profile.totalPoints += points;
            profile.level = LearningEngine.calcLevel(profile.totalPoints);
            db.poemDao().insertUserProfile(profile);
            score.postValue(profile.totalPoints);

            // 持久化 quizScore 到 learning_records，供每日任务检测
            db.poemDao().ensureRecordExists(poemId);
            int newScore = (finalCorrect ? blanksCount : 0);
            db.poemDao().updateQuizScore(poemId, newScore);
        }).start();
    }

    public void nextQuestion() {
        Integer idx = questionIndex.getValue();
        if (idx != null) {
            showQuestion(idx);
        }
    }

    private void finishQuiz() {
        // 更新每日统计：答题完成
        final String today = LocalDate.now().toString();
        new Thread(() -> {
            // 确保今日行存在
            com.poetry.data.DailyStats existing = db.poemDao().getDailyStatsSync(today);
            if (existing == null) {
                db.poemDao().upsertDailyStats(new com.poetry.data.DailyStats(today));
            }
            db.poemDao().incrementQuizCompleted(today);
        }).start();

        isFinished.setValue(true);
    }

    public void restartQuiz() {
        startQuiz();
    }

    public LiveData<QuizGenerator.QuizQuestion> getCurrentQuestion() { return currentQuestion; }
    public LiveData<Integer> getScore() { return score; }
    public LiveData<Integer> getQuestionIndex() { return questionIndex; }
    public LiveData<Boolean> getIsCorrect() { return isCorrect; }
    public LiveData<Boolean> getIsFinished() { return isFinished; }
    public LiveData<Integer> getTotalCorrect() { return totalCorrect; }
    public int getTotalQuestions() { return TOTAL_QUESTIONS; }
}
