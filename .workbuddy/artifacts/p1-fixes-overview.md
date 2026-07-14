# P1 修复概览

## 修复的 4 个 P1 问题

| # | 问题 | 严重性 | 根因 | 修复方案 |
|---|------|--------|------|----------|
| P1-1 | 答一题永远检测不到 | 高 | QuizViewModel 未写 quizScore 和 DailyStats | submitAnswer 持久化 quizScore + finishQuiz 增量 daily_stats.quizCompleted |
| P1-2 | 玩游戏永远检测不到 | 高 | GameViewModel 未写 DailyStats | couplet/match 完成时调用 recordGameActivity() 增量 daily_stats.gamesPlayed |
| P1-3 | 朗读无反应无提示 | 中 | TTS 初始化失败静默 | OnInitListener 回调 + Toast 提示 |
| P1-4 | 填空无法撤销 | 中 | 空位无点击事件 | undoBlank() 点空位清空+恢复候选词 |

## 架构影响

```
之前: 任务检测 query learning_records (错误)
      ❌ quizScore never written
      ❌ gamePlayed never written  
      ❌ 查询条件 learnedAt >= today AND gamePlayed > 0 基本不可能为真

之后: 任务检测 query daily_stats (正确)
      ✅ quizCompleted 由 QuizViewModel.finishQuiz 原子递增
      ✅ gamesPlayed 由 GameViewModel.recordGameActivity 原子递增
      ✅ hasQuizToday/hasGameToday 直接检查 daily_stats
```

## 涉及文件 (7 个)

- `PoemDao.java` — +5 SQL 方法
- `QuizViewModel.java` — finishQuiz 增加 statistics, submitAnswer 持久化 quizScore
- `QuizFragment.java` — +undoBlank 撤销逻辑, +candidateChips 追踪
- `GameViewModel.java` — +recordGameActivity, couplet/match 完成时调用
- `LearningViewModel.java` — 任务检测改用 hasQuizToday/hasGameToday
- `TtsManager.java` — +OnInitListener, setLanguage 结果检查
- `DetailFragment.java` — TTS initListener 传入, 未就绪 Toast

## 编译结果

BUILD SUCCESSFUL — 14 tasks executed, 20 up-to-date
