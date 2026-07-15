/**
 * AchievementEngine.js - 成就引擎
 * Port from Android AchievementEngine.java
 * 12 项成就自动检测 + 解锁回调
 */
var ACHIEVEMENTS = [
  { id: 'first_poem', name: '初出茅庐', icon: '🌱', desc: '学习 1 首诗词', check: function(s) { return s.totalLearned >= 1; } },
  { id: 'poem_10', name: '小有积累', icon: '📚', desc: '学习 10 首诗词', check: function(s) { return s.totalLearned >= 10; } },
  { id: 'poem_50', name: '学富五车', icon: '📖', desc: '学习 50 首诗词', check: function(s) { return s.totalLearned >= 50; } },
  { id: 'poem_100', name: '诗词达人', icon: '🏆', desc: '学习 100 首诗词', check: function(s) { return s.totalLearned >= 100; } },
  { id: 'streak_7', name: '坚持不懈', icon: '🔥', desc: '连续 7 天学习', check: function(s) { return s.streak >= 7; } },
  { id: 'streak_30', name: '持之以恒', icon: '💪', desc: '连续 30 天学习', check: function(s) { return s.streak >= 30; } },
  { id: 'favorite_10', name: '初代收藏家', icon: '⭐', desc: '收藏 10 首诗词', check: function(s) { return s.totalFavorites >= 10; } },
  { id: 'favorite_20', name: '收藏达人', icon: '🌟', desc: '收藏 20 首诗词', check: function(s) { return s.totalFavorites >= 20; } },
  { id: 'quiz_perfect_5', name: '满分达人', icon: '🎯', desc: '填空满分 5 次', check: function(s) { return s.quizPerfectCount >= 5; } },
  { id: 'game_10', name: '游戏高手', icon: '🎮', desc: '游戏 10 次', check: function(s) { return s.gamesPlayed >= 10; } },
  { id: 'level_5', name: '小有名气', icon: '🎓', desc: '达到等级 5', check: function(s) { return s.level >= 5; } },
  { id: 'level_9', name: '千古诗圣', icon: '👑', desc: '达到等级 9', check: function(s) { return s.level >= 9; } },
];

function checkAndUnlock(profile, storageSvc, callback) {
  if (!profile) return;
  var achievements = profile.achievements || [];
  var unlockedMap = {};
  for (var i = 0; i < achievements.length; i++) {
    unlockedMap[achievements[i]] = true;
  }
  var stats = {
    totalLearned: storageSvc.getLearnedCount ? storageSvc.getLearnedCount() : 0,
    streak: profile.streak || 0,
    totalFavorites: 0,
    quizPerfectCount: 0,
    gamesPlayed: 0,
    level: profile.level || 1,
  };
  // count favorites
  var favs = storageSvc.getFavorites ? storageSvc.getFavorites() : [];
  stats.totalFavorites = favs.length;
  // count quiz perfect (placeholder - would need quiz history)
  var newAchievements = [];
  for (var j = 0; j < ACHIEVEMENTS.length; j++) {
    var ach = ACHIEVEMENTS[j];
    if (!unlockedMap[ach.id] && ach.check(stats)) {
      newAchievements.push(ach.id);
      achievements.push(ach.id);
    }
  }
  if (newAchievements.length > 0) {
    profile.achievements = achievements;
    if (storageSvc.saveUserProfile) storageSvc.saveUserProfile(profile);
    if (callback) {
      for (var k = 0; k < newAchievements.length; k++) {
        var achId = newAchievements[k];
        for (var m = 0; m < ACHIEVEMENTS.length; m++) {
          if (ACHIEVEMENTS[m].id === achId) {
            callback(ACHIEVEMENTS[m]);
            break;
          }
        }
      }
    }
  }
}

function getAchievementById(id) {
  for (var i = 0; i < ACHIEVEMENTS.length; i++) {
    if (ACHIEVEMENTS[i].id === id) return ACHIEVEMENTS[i];
  }
  return null;
}

module.exports = {
  ACHIEVEMENTS: ACHIEVEMENTS,
  checkAndUnlock: checkAndUnlock,
  getAchievementById: getAchievementById,
};
