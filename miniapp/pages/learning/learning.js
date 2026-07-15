var storage = require('../../services/storage');
var learningEngine = require('../../domain/LearningEngine');
Page({
  data: { stats: null, weekStats: [], levelInfo: null, progress: 0, todayStat: null },
  onLoad: function() { this.loadStats(); },
  onShow: function() { this.loadStats(); },
  loadStats: function() {
    var profile = storage.getUserProfile() || {};
    var streak = learningEngine.calcStreak(profile);
    var learnedCount = storage.getLearnedCount();
    var weekStats = storage.getWeekStats();
    var todayStat = storage.getTodayStat();
    var levelInfo = learningEngine.getLevel(profile.totalPoints || 0);
    var progress = learningEngine.getLevelProgress(profile.totalPoints || 0);
    var progressPercent = Math.min(100, Math.round(progress * 100));
    this.setData({
      stats: { poemsLearned: learnedCount, streak: streak, totalPoints: profile.totalPoints || 0, level: profile.level || 1 },
      weekStats: weekStats, levelInfo: levelInfo, progress: progress, progressPercent: progressPercent, todayStat: todayStat,
    });
    // Update streak in profile
    if (profile) {
      profile.streak = streak;
      storage.saveUserProfile(profile);
    }
  },
  goToFavorites: function() { wx.navigateTo({ url: '/pages/favorites/favorites' }); },
});
