Page({
  data: { gamesPlayed: 0, bestScore: 0 },
  onLoad: function() {
    var storage = require('../../services/storage');
    var profile = storage.getUserProfile() || {};
    this.setData({ gamesPlayed: profile.gamesPlayed || 0 });
  },
  goToCouplet: function() { wx.navigateTo({ url: '/pages/coupletGame/coupletGame' }); },
  goToMatch: function() { wx.navigateTo({ url: '/pages/matchGame/matchGame' }); },
  goToQuiz: function() { wx.navigateTo({ url: '/pages/quiz/quiz' }); },
});
