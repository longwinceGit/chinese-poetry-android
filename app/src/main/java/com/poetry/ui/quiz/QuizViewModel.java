package com.poetry.ui.quiz;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.PoemRepository;
import com.poetry.util.AppExecutors;
import com.poetry.data.UserProfile;
import com.poetry.data.model.Poem;
import com.poetry.domain.AchievementEngine;
import com.poetry.domain.LearningEngine;
import com.poetry.domain.QuizDifficulty;
import com.poetry.domain.QuizGenerator;
import com.poetry.domain.ThemeManager;
import com.poetry.util.DifficultyProfile;
import com.poetry.util.MoonBlessing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    // M3 对诗化改造：本局得分（不同于全局 totalPoints，供顶部显示 + 结算页）
    private final MutableLiveData<Integer> gameScore = new MutableLiveData<>(0);
    private int gameScoreValue = 0;
    /** 本局涉及诗词 id（供结算页"最美一句"挑选，§9.2） */
    private final Set<String> touchedPoemIds = new HashSet<>();
    /** 最近一题得分（供 Fragment 飞分展示） */
    private int lastPoints = 0;

    /** 本局种子（填空月光祝福判定用，M10 §7.3） */
    private long quizSeed = 0L;
    /** 最近提交的一题是否命中月光祝福（供 UI 🌕 badge，M10） */
    private boolean lastQuestionBlessed = false;

    /** 当前难度（默认普通；M9 自适应排等会按最近表现写入此值） */
    private QuizDifficulty difficulty = QuizDifficulty.NORMAL;

    // M9 自适应排等：本局累计命中空数 / 总空数（finishQuiz 时写入 DifficultyProfile）
    private int totalHitBlanks = 0;
    private int totalBlanksCount = 0;

    public QuizViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    /**
     * 设置本轮填空的难度（用于驱动挖空数 / 候选字数量 / 干扰项策略）。
     *
     * @param difficulty 难度分级
     */
    public void setDifficulty(QuizDifficulty difficulty) {
        if (difficulty != null) this.difficulty = difficulty;
    }

    /**
     * 启动一轮新的答题：从诗词库中随机选取 {@value #TOTAL_QUESTIONS} 首诗词生成填空题，
     * 重置所有分数和状态，然后显示第一题。
     *
     * <p>Easy 难度优先选自有释义的名篇（${code explanation != null}），保证低龄零门槛；
     * 干扰字池从数据池随机采样（复用真实汉字，替代原硬编码 40 字）。
     */
    public void startQuiz() {
        questions.clear();
        // M10 月光祝福：本局种子 = 开局时间戳
        quizSeed = System.currentTimeMillis();
        List<Poem> all = repo.getAllPoems();
        if (all.isEmpty()) return;
        if (difficulty == null) difficulty = QuizDifficulty.NORMAL;

        // 从数据池采样一批汉字，作为填空干扰项来源（GAME_REDESIGN_FINAL.md §6.3）
        List<Character> distractorPool = sampleDistractorPool(all);

        List<Poem> shuffled = new ArrayList<>(all);
        // Easy 优先有释义名篇 → 把有释义的诗排到最前
        if (difficulty == QuizDifficulty.EASY) {
            Collections.sort(shuffled, (a, b) -> {
                boolean ea = a.explanation != null && !a.explanation.isEmpty();
                boolean eb = b.explanation != null && !b.explanation.isEmpty();
                if (ea != eb) return ea ? -1 : 1;
                return 0;
            });
        }
        Collections.shuffle(shuffled);

        int count = 0;
        for (Poem p : shuffled) {
            QuizGenerator.QuizQuestion q = QuizGenerator.generateFillBlank(p, difficulty, distractorPool);
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
        totalHitBlanks = 0;
        totalBlanksCount = 0;

        // 打散题目顺序（否则 Easy 会连着出多首名篇）
        if (questions.size() > 1) Collections.shuffle(questions);
        showQuestion(0);
    }

    /**
     * 从数据池随机取若干首诗的汉字作为干扰字池（去重、仅保留汉字）。
     *
     * @param all 全部诗词
     * @return 汉字字符池（可能为空，此时生成器退回内置兜底字池）
     */
    private List<Character> sampleDistractorPool(List<Poem> all) {
        List<Character> chars = new ArrayList<>();
        Set<Character> seen = new HashSet<>();
        List<Poem> shuffled = new ArrayList<>(all);
        Collections.shuffle(shuffled);
        int sampled = 0;
        for (Poem p : shuffled) {
            if (sampled >= 8) break; // 采样 8 首足矣，避免遍历全库
            if (p.lines == null) continue;
            for (String line : p.lines) {
                if (line == null) continue;
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    if (Character.isLetter(c) && !seen.contains(c)) {
                        seen.add(c);
                        chars.add(c);
                    }
                }
            }
            sampled++;
        }
        return chars;
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

        // 按空结算（GAME_REDESIGN_FINAL.md §4.3）：统计命中的空数，而非整题二值
        int totalBlanks = q.blanks.size();
        int hitBlanks = 0;
        for (int i = 0; i < totalBlanks; i++) {
            if (i < userAnswers.size() && userAnswers.get(i) != null
                    && userAnswers.get(i).equals(q.blanks.get(i).answer)) {
                hitBlanks++;
            }
        }

        boolean allCorrect = hitBlanks == totalBlanks && totalBlanks > 0;
        isCorrect.setValue(allCorrect);
        if (allCorrect) {
            int correct = (totalCorrect.getValue() != null ? totalCorrect.getValue() : 0) + 1;
            totalCorrect.setValue(correct);
        }

        // M9 自适应排等：累计本局命中空数 / 总空数
        totalHitBlanks += hitBlanks;
        totalBlanksCount += totalBlanks;

        // M10 月光祝福：整题全对且命中祝福题 → 该题积分 ×2（纯增益，≤2题/局）
        final int questionIdx = (questionIndex.getValue() != null ? questionIndex.getValue() : 1) - 1;
        lastQuestionBlessed = allCorrect && MoonBlessing.isBlessedRound(quizSeed, questionIdx, TOTAL_QUESTIONS);

        // 保存积分 + 更新每日统计
        final boolean finalCorrect = allCorrect;
        final boolean finalBlessed = lastQuestionBlessed;
        final int finalHit = hitBlanks;
        final int finalBlanksCount = totalBlanks;
        final String poemId = q.poem.id;
        AppExecutors.io(() -> {
            // 按空命中比例计分（calcPointsForQuiz 天然按比例返回 20/15/10/5）
            int points = LearningEngine.calcPointsForQuiz(finalHit, finalBlanksCount);
            points = MoonBlessing.applyDouble(points, finalBlessed);
            db.userProfileDao().addTotalPoints(points);
            UserProfile profile = db.userProfileDao().getUserProfileSync();
            if (profile != null) {
                int newLevel = LearningEngine.calcLevel(profile.totalPoints);
                if (newLevel != profile.level) {
                    db.userProfileDao().updateLevel(newLevel);
                }
                score.postValue(profile.totalPoints);
            }

            // 持久化 quizScore 到 learning_records，供每日任务检测与 quiz_perfect_5 成就判定。
            // 单题全对写 20（对应"满分单题 20"语义），非全对写 0，使 getPerfectQuizCountSync(10) 可达。
            db.learningRecordDao().ensureRecordExists(poemId);
            db.learningRecordDao().updateQuizScore(poemId, finalCorrect ? 20 : 0);

            // 🔴 B4 修复：每次答题后检测成就
            AchievementEngine.checkAndUnlock(db, def -> {
                newAchievement.postValue(def);
            });
            // 🔴 B5 修复：等级提升后同步主题解锁
            ThemeManager.syncUnlockedThemes(db);
        });
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
        // M9 自适应排等：本局结束，按累计命中空数/总空数记录表现并升降档。
        // 星级映射：满分（20 分/题 × 5 题 = 100）→ 3 星；≥15 分/题 → 2 星；否则 1 星。
        int stars = calcQuizStars();
        DifficultyProfile.recordGame(getApplication(), totalHitBlanks, totalBlanksCount, stars);

        // 更新每日统计：答题完成
        final String today = LocalDate.now().toString();
        AppExecutors.io(() -> {
            // 确保今日行存在
            com.poetry.data.DailyStats existing = db.dailyStatsDao().getDailyStatsSync(today);
            if (existing == null) {
                db.dailyStatsDao().upsertDailyStats(new com.poetry.data.DailyStats(today));
            }
            db.dailyStatsDao().incrementQuizCompleted(today);
        });

        isFinished.setValue(true);
    }

    /**
     * 计算本局填空星级（M9 自适应排等用）。
     *
     * <p>按平均每题得分映射：满分（每题 20 分）→ 3 星；≥15 分/题 → 2 星；否则 1 星。
     * 星级恒 ≥1（零挫败）。
     *
     * @return 1-3 星
     */
    private int calcQuizStars() {
        if (totalBlanksCount <= 0) return 1;
        int avgPointsPerQuestion = totalHitBlanks * 20 / totalBlanksCount;
        if (avgPointsPerQuestion >= 20) return 3;
        if (avgPointsPerQuestion >= 15) return 2;
        return 1;
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
    /** 消费成就事件后清空，防止 LiveData 回放导致重复庆祝 */
    public void clearAchievement() { newAchievement.setValue(null); }
    /** @return 本轮总题数 */
    public int getTotalQuestions() { return TOTAL_QUESTIONS; }
}
