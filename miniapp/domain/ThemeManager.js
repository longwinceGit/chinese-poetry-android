/**
 * ThemeManager.js - 主题管理器
 * Port from Android ThemeManager.java
 * 9 个中国风主题按等级/连续天数解锁
 */
var THEMES = [
  { id: 'default', name: '墨韵', icon: '🖋️', desc: '默认主题', check: function(level, streak) { return true; } },
  { id: 'spring', name: '春意', icon: '🌸', desc: '春意盎然', check: function(level, streak) { return level >= 2; } },
  { id: 'summer', name: '夏荷', icon: '🌻', desc: '夏日荷香', check: function(level, streak) { return level >= 3 && streak >= 3; } },
  { id: 'autumn', name: '秋月', icon: '🌙', desc: '秋月皎洁', check: function(level, streak) { return level >= 4 && streak >= 7; } },
  { id: 'winter', name: '冬雪', icon: '❄️', desc: '冬雪皑皑', check: function(level, streak) { return level >= 5 && streak >= 14; } },
  { id: 'bamboo', name: '竹韵', icon: '🎋', desc: '竹韵清幽', check: function(level, streak) { return level >= 6; } },
  { id: 'lotus', name: '莲心', icon: '🪷', desc: '莲心禅意', check: function(level, streak) { return level >= 7; } },
  { id: 'golden', name: '金榜', icon: '🏅', desc: '金榜题名', check: function(level, streak) { return level >= 8; } },
  { id: 'legend', name: '传奇', icon: '👑', desc: '千古传奇', check: function(level, streak) { return level >= 9; } },
];

function getUnlockedThemes(level, streak) {
  var result = [];
  for (var i = 0; i < THEMES.length; i++) {
    if (THEMES[i].check(level, streak)) result.push(THEMES[i]);
  }
  return result;
}

function getThemeById(id) {
  for (var i = 0; i < THEMES.length; i++) {
    if (THEMES[i].id === id) return THEMES[i];
  }
  return THEMES[0];
}

function getThemeColors(themeId) {
  var colors = {
    default: { primary: '#8B4513', bg: '#FFF8F0', card: '#FFFFFF', accent: '#D4A574' },
    spring: { primary: '#E91E63', bg: '#FFF0F5', card: '#FFF8FA', accent: '#F48FB1' },
    summer: { primary: '#F57C00', bg: '#FFF8E1', card: '#FFFDF5', accent: '#FFB74D' },
    autumn: { primary: '#5D4037', bg: '#FFF3E0', card: '#FFFBF0', accent: '#A1887F' },
    winter: { primary: '#37474F', bg: '#F5F5F5', card: '#FAFAFA', accent: '#90A4AE' },
    bamboo: { primary: '#33691E', bg: '#F1F8E9', card: '#F7FBF0', accent: '#81C784' },
    lotus: { primary: '#6A1B9A', bg: '#F3E5F5', card: '#F8F0FA', accent: '#CE93D8' },
    golden: { primary: '#E65100', bg: '#FFF8E1', card: '#FFFDF0', accent: '#FFD54F' },
    legend: { primary: '#1A237E', bg: '#E8EAF6', card: '#F0F2FA', accent: '#7986CB' },
  };
  return colors[themeId] || colors.default;
}

module.exports = {
  THEMES: THEMES,
  getUnlockedThemes: getUnlockedThemes,
  getThemeById: getThemeById,
  getThemeColors: getThemeColors,
};
