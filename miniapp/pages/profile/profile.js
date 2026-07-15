var storage = require('../../services/storage');
var learningEngine = require('../../domain/LearningEngine');
var achievementEngine = require('../../domain/AchievementEngine');
var themeManager = require('../../domain/ThemeManager');
Page({
  data: { profile: null, levelInfo: null, progress: 0, achievements: [], themeList: [], currentTheme: 'default' },
  onLoad: function() { this.loadProfile(); },
  onShow: function() { this.loadProfile(); },
  loadProfile: function() {
    var profile = storage.getUserProfile() || {};
    var levelInfo = learningEngine.getLevel(profile.totalPoints || 0);
    var progress = learningEngine.getLevelProgress(profile.totalPoints || 0);
    var progressPercent = Math.min(100, Math.round(progress * 100));
    var unachieved = [];
    var allAch = achievementEngine.ACHIEVEMENTS;
    var userAch = profile.achievements || [];
    var userAchMap = {};
    for (var i = 0; i < userAch.length; i++) userAchMap[userAch[i]] = true;
    var unlockedCount = 0;
    for (var j = 0; j < allAch.length; j++) {
      var isUnlocked = !!userAchMap[allAch[j].id];
      if (isUnlocked) unlockedCount++;
      unachieved.push({ id: allAch[j].id, name: allAch[j].name, icon: allAch[j].icon, desc: allAch[j].desc, unlocked: isUnlocked });
    }
    var unlocked = themeManager.getUnlockedThemes(profile.level || 1, profile.streak || 0);
    this.setData({
      profile: profile, levelInfo: levelInfo, progress: progress, progressPercent: progressPercent,
      achievements: unachieved, unlockedCount: unlockedCount,
      themeList: unlocked, currentTheme: profile.currentTheme || 'default',
    });
  },
  selectTheme: function(e) {
    var themeId = e.currentTarget.dataset.theme;
    if (themeId === this.data.currentTheme) return;
    var profile = this.data.profile;
    profile.currentTheme = themeId;
    storage.saveUserProfile(profile);
    this.setData({ currentTheme: themeId, profile: profile });
    wx.showToast({ title: '主题已切换', icon: 'none' });
  },
  goToFavorites: function() { wx.navigateTo({ url: '/pages/favorites/favorites' }); },
});
