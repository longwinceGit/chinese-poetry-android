var app = getApp();
var gameEngine = require('../../domain/GameEngine');
Page({
  data: { questions: [], currentRound: 0, totalRounds: 5, score: 0, streak: 0,
    selectedAnswer: null, showResult: false, isCorrect: false, gameOver: false },
  onLoad: function() { this.startGame(); },
  startGame: function() {
    var poems = app.globalData.poems;
    if (!poems || poems.length === 0) { poems = require('../../services/poemLoader').FAMOUS_POEMS; }
    var questions = gameEngine.generateCoupletGame(poems, 5);
    this.setData({ questions: questions, currentRound: 0, totalRounds: questions.length,
      score: 0, streak: 0, selectedAnswer: null, showResult: false, gameOver: false });
  },
  selectAnswer: function(e) {
    if (this.data.showResult) return;
    var idx = parseInt(e.currentTarget.dataset.index);
    var q = this.data.questions[this.data.currentRound];
    var correct = q.options[idx] === q.correctLine;
    var bonus = correct ? gameEngine.calculateCoupletScore(10, this.data.streak) : 0;
    var newScore = this.data.score + (correct ? (10 + this.data.streak * 2) : 0);
    var newStreak = correct ? this.data.streak + 1 : 0;
    this.setData({ selectedAnswer: idx, showResult: true, isCorrect: correct, score: newScore, streak: newStreak });
  },
  nextRound: function() {
    var next = this.data.currentRound + 1;
    if (next >= this.data.totalRounds) {
      this.setData({ gameOver: true });
      this.saveGameStats();
    } else {
      this.setData({ currentRound: next, selectedAnswer: null, showResult: false });
    }
  },
  saveGameStats: function() {
    var storage = require('../../services/storage');
    var profile = storage.getUserProfile() || {};
    profile.gamesPlayed = (profile.gamesPlayed || 0) + 1;
    profile.totalPoints = (profile.totalPoints || 0) + this.data.score;
    storage.saveUserProfile(profile);
    app.updateUserProfile(profile);
  },
  restartGame: function() { this.startGame(); },
});
