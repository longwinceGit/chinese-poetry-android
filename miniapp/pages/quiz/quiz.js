var app = getApp();
var quizGen = require('../../domain/QuizGenerator');
Page({
  data: { quizzes: [], currentRound: 0, totalRounds: 10, score: 0, correctCount: 0,
    selectedOption: null, showResult: false, isCorrect: false, gameOver: false, poem: null },
  onLoad: function() { this.startQuiz(); },
  startQuiz: function() {
    var poems = app.globalData.poems;
    if (!poems || poems.length === 0) { poems = require('../../services/poemLoader').FAMOUS_POEMS; }
    var quizzes = [];
    var attempts = 0;
    while (quizzes.length < 10 && attempts < 100) {
      attempts++;
      var idx = Math.floor(Math.random() * poems.length);
      var quiz = quizGen.generateQuiz(poems[idx]);
      if (quiz) {
        quiz.poemTitle = poems[idx].title;
        quiz.poemAuthor = poems[idx].author;
        quizzes.push(quiz);
      }
    }
    this.setData({ quizzes: quizzes, totalRounds: quizzes.length, currentRound: 0,
      score: 0, correctCount: 0, selectedOption: null, showResult: false, gameOver: false });
  },
  selectOption: function(e) {
    if (this.data.showResult) return;
    var idx = parseInt(e.currentTarget.dataset.index);
    var q = this.data.quizzes[this.data.currentRound];
    var correct = q.options[idx] === q.answer;
    var newCorrect = this.data.correctCount + (correct ? 1 : 0);
    var newScore = this.data.score + (correct ? 10 : 0);
    this.setData({ selectedOption: idx, showResult: true, isCorrect: correct, correctCount: newCorrect, score: newScore });
  },
  nextRound: function() {
    var next = this.data.currentRound + 1;
    if (next >= this.data.totalRounds) {
      this.setData({ gameOver: true });
      this.saveStats();
    } else {
      this.setData({ currentRound: next, selectedOption: null, showResult: false });
    }
  },
  saveStats: function() {
    var storage = require('../../services/storage');
    var todayStat = storage.getTodayStat();
    todayStat.quizCompleted = (todayStat.quizCompleted || 0) + 1;
    storage.saveDailyStat(todayStat.date, todayStat);
    var profile = storage.getUserProfile() || {};
    var le = require('../../domain/LearningEngine');
    var pts = le.calcQuizPoints(this.data.correctCount, this.data.totalRounds);
    profile.totalPoints = (profile.totalPoints || 0) + pts;
    storage.saveUserProfile(profile);
    app.updateUserProfile(profile);
  },
  restartQuiz: function() { this.startQuiz(); },
});
