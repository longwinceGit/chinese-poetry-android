package com.poetry.ui.quiz;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.PoemRepository;
import com.poetry.data.UserProfile;
import com.poetry.data.model.Poem;
import com.poetry.domain.AchievementEngine;
import com.poetry.domain.LearningEngine;
import com.poetry.domain.QuizGenerator;
import com.poetry.domain.ThemeManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 答题 ViewModel（AndroidViewModel）。
 * <p>
 * 管理填空答题的完整生命周期：随机选题 → 显示题目 → 提交答案 → 下一题 → 完成结算。
 * 负责计分、等级升级、每日统计更新、成就检测和主题解锁等后端逻辑。
 * 通过 LiveData 向 Fragment 暴露题目、分数、正确性等 UI 状态。
 * </p>
 */
public class QuizViewModel extends AndroidViewModel {

    private PoemRepository repo = PoemRepository.getInstance();
    private LearningDatabase db;

    private MutableLiveData<QuizGenerator.QuizQuestion> currentQuestion = new MutableLiveData<>();
    private MutableLiveData<Integer> score = new MutableLiveData<>(0);
    private MutableLiveData<Integer> questionIndex = new MutableLiveData<>(0);
    private MutableLiveData<Boolean> isCorrect = new MutableLiveData<>(null);
    private MutableLiveData<Boolean> isFinished = new MutableLiveData<>(false);
    private MutableLiveData<Integer> totalCorrect = new MutableLiveData<>(0);

    // 🔴 B4 修复：成就解锁通知
    private MutableLiveData<AchievementEngine.AchievementDef> newAchievement = new MutableLiveData<>();

    private List<QuizGenerator.QuizQuestion> questions = new ArrayList<>();
    private static final int TOTAL_QUESTIONS = 5;

    public QuizViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    /**
     * 启动一轮新的答题：从诗词库中随机选取 {@value #TOTAL_QUESTIONS} 首诗词生成填空题，
     * 重置所有分数和状态，然后显示第一题。
     */
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

    /**
     * 显示指定索引的题目。若索引超出范围（所有题目已完成），则调用 {@link #finishQuiz()}。
     *
     * @param index 题目在 questions 列表中的索引
     */
    public void showQuestion(int index) {
        if (index < questions.size()) {
            currentQuestion.setValue(questions.get(index));
            questionIndex.setValue(index + 1);
            isCorrect.setValue(null);
        } else {
            finishQuiz();
        }
    }

    /**
     * 提交用户答案，逐个比对标准答案。
     * <p>
     * 若全对则增加正确计数；随后在后台线程中执行：
     * 计算积分（原子增量）、更新等级、持久化答题分数、
     * 检测成就解锁、同步主题解锁。
     * </p>
     *
     * @param userAnswers 用户填写的答案列表
     */
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
            // 🔴 B2 修复：原子增量代替读-改-写
            int points = LearningEngine.calcPointsForQuiz(finalCorrect ? blanksCount : 0, blanksCount);
            db.poemDao().addTotalPoints(points);
            UserProfile profile = db.poemDao().getUserProfileSync();
            if (profile != null) {
                int newLevel = LearningEngine.calcLevel(profile.totalPoints);
                if (newLevel != profile.level) {
                    db.poemDao().updateLevel(newLevel);
                }
                score.postValue(profile.totalPoints);
            }

            // 持久化 quizScore 到 learning_records，供每日任务检测
            db.poemDao().ensureRecordExists(poemId);
            int newScore = (finalCorrect ? blanksCount : 0);
            db.poemDao().updateQuizScore(poemId, newScore);

            // 🔴 B4 修复：每次答题后检测成就
            AchievementEngine.checkAndUnlock(db, def -> {
                newAchievement.postValue(def);
            });
            // 🔴 B5 修复：等级提升后同步主题解锁
            ThemeManager.syncUnlockedThemes(db);
        }).start();
    }

    /**
     * 加载下一道题目，使用当前 questionIndex（即下一题的索引）。
     */
    public void nextQuestion() {
        Integer idx = questionIndex.getValue();
        if (idx != null) {
            showQuestion(idx);
        }
    }

    /**
     * 完成本轮答题：增量更新当日统计中的答题完成次数，并标记 isFinished 为 true。
     */
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

    /**
     * 重新开始答题，等同于再次调用 {@link #startQuiz()}。
     */
    public void restartQuiz() {
        startQuiz();
    }

    // ==================== LiveData Getters ====================

    /** @return 当前显示的题目 LiveData */
    public LiveData<QuizGenerator.QuizQuestion> getCurrentQuestion() { return currentQuestion; }
    /** @return 当前总积分 LiveData */
    public LiveData<Integer> getScore() { return score; }
    /** @return 当前题目序号（1-based）LiveData */
    public LiveData<Integer> getQuestionIndex() { return questionIndex; }
    /** @return 最近一次提交是否正确 LiveData（null = 尚未提交） */
    public LiveData<Boolean> getIsCorrect() { return isCorrect; }
    /** @return 本轮答题是否已完成 LiveData */
    public LiveData<Boolean> getIsFinished() { return isFinished; }
    /** @return 本轮累计正确数 LiveData */
    public LiveData<Integer> getTotalCorrect() { return totalCorrect; }
    /**
     * @return 成就解锁事件 LiveData
     * @see com.poetry.domain.AchievementEngine
     */
    public LiveData<AchievementEngine.AchievementDef> getNewAchievement() { return newAchievement; }
    /** @return 本轮总题数 */
    public int getTotalQuestions() { return TOTAL_QUESTIONS; }
}
