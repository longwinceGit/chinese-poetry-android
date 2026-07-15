var app = getApp();
var tts = require('../../utils/tts');
Page({
  data: { poem: null, showPinyin: false, pinyinLines: [], isPlaying: false, isFavorite: false, isLearned: false, showExplanation: false },
  onLoad: function(options) {
    if (options.poem) {
      try {
        var poem = JSON.parse(decodeURIComponent(options.poem));
        this.setData({ poem: poem });
        this.loadPinyin(poem);
        this.checkStatus(poem);
      } catch(e) {}
    }
  },
  loadPinyin: function(poem) {
    var pinyinUtil = require('../../utils/pinyin');
    var lines = [];
    if (poem && poem.lines) {
      for (var i = 0; i < poem.lines.length; i++) {
        lines.push(pinyinUtil.toPinyinList(poem.lines[i]));
      }
    }
    this.setData({ pinyinLines: lines });
  },
  checkStatus: function(poem) {
    var storage = require('../../services/storage');
    var record = storage.getLearningRecord(poem.id);
    if (record) {
      this.setData({ isFavorite: !!record.favorite, isLearned: !!record.learnedAt });
    }
  },
  togglePinyin: function() { this.setData({ showPinyin: !this.data.showPinyin }); },
  toggleTts: function() {
    if (this.data.isPlaying) { tts.stop(); this.setData({ isPlaying: false }); return; }
    var poem = this.data.poem;
    var text = poem.title + '。' + poem.author + '。' + poem.lines.join('。');
    var that = this;
    this.setData({ isPlaying: true });
    tts.speak(text, function() { that.setData({ isPlaying: false }); });
  },
  toggleFavorite: function() {
    var storage = require('../../services/storage');
    var poem = this.data.poem;
    var fav = storage.toggleFavorite(poem.id, poem);
    this.setData({ isFavorite: fav });
    wx.showToast({ title: fav ? '已收藏' : '取消收藏', icon: 'none' });
  },
  markLearned: function() {
    if (this.data.isLearned) return;
    var storage = require('../../services/storage');
    storage.markAsLearned(this.data.poem.id, this.data.poem);
    this.setData({ isLearned: true });
    wx.showToast({ title: '已标记为已学', icon: 'none' });
    this.checkAchievements();
  },
  checkAchievements: function() {
    var achievementEngine = require('../../domain/AchievementEngine');
    var storage = require('../../services/storage');
    var profile = storage.getUserProfile();
    var app = getApp();
    achievementEngine.checkAndUnlock(profile, storage, function(ach) {
      wx.showToast({ title: '🎉 成就解锁: ' + ach.name, icon: 'none', duration: 3000 });
      app.celebrate();
    });
    app.updateUserProfile(profile);
  },
  toggleExplanation: function() { this.setData({ showExplanation: !this.data.showExplanation }); },
});
