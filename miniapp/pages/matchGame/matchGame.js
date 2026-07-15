var app = getApp();
var gameEngine = require('../../domain/GameEngine');
Page({
  data: { cards: [], attempts: 0, score: 0, totalPairs: 6, gameOver: false, lockInput: false, selectedCard: null },
  onLoad: function() { this.startGame(); },
  startGame: function() {
    var poems = app.globalData.poems;
    if (!poems || poems.length === 0) { poems = require('../../services/poemLoader').FAMOUS_POEMS; }
    var cards = gameEngine.generateMatchGame(poems, 6);
    this.setData({ cards: cards, attempts: 0, score: 0, gameOver: false, lockInput: false, selectedCard: null });
  },
  onCardTap: function(e) {
    if (this.data.lockInput || this.data.gameOver) return;
    var idx = e.currentTarget.dataset.index;
    var cards = this.data.cards;
    if (cards[idx].matched) return;
    if (this.data.selectedCard === null) {
      cards[idx].selected = true;
      this.setData({ cards: cards, selectedCard: idx });
      return;
    }
    var firstIdx = this.data.selectedCard;
    if (firstIdx === idx) { cards[idx].selected = false; this.setData({ cards: cards, selectedCard: null }); return; }
    this.setData({ lockInput: true });
    var that = this;
    var match = gameEngine.checkMatch(cards[firstIdx], cards[idx]);
    if (match) {
      cards[firstIdx].selected = false;
      cards[firstIdx].matched = true;
      cards[idx].matched = true;
      var attempts = this.data.attempts + 1;
      var score = gameEngine.calculateScore(attempts, this.data.totalPairs);
      this.setData({ cards: cards, attempts: attempts, score: score, selectedCard: null, lockInput: false });
      if (gameEngine.isGameComplete(cards)) { this.gameComplete(score); }
    } else {
      cards[firstIdx].shake = true;
      cards[idx].shake = true;
      var that2 = this;
      setTimeout(function() {
        cards[firstIdx].shake = false;
        cards[firstIdx].selected = false;
        cards[idx].shake = false;
        that2.setData({ cards: cards, attempts: that2.data.attempts + 1, selectedCard: null, lockInput: false });
      }, 600);
      this.setData({ cards: cards });
    }
  },
  gameComplete: function(score) {
    this.setData({ gameOver: true, score: score });
    var storage = require('../../services/storage');
    var profile = storage.getUserProfile() || {};
    profile.gamesPlayed = (profile.gamesPlayed || 0) + 1;
    profile.totalPoints = (profile.totalPoints || 0) + score;
    storage.saveUserProfile(profile);
    app.updateUserProfile(profile);
  },
  restartGame: function() { this.startGame(); },
});
