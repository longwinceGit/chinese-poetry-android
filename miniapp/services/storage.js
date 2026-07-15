/**
 * storage.js - 微信存储服务
 * 替代 Android Room 数据库，使用 wx.getStorageSync / wx.setStorageSync
 * 数据表映射：user_profile → 单个 key, learning_records → 对象映射, daily_stats → 对象映射
 */

const STORAGE_KEYS = {
  USER_PROFILE: 'poetry_user_profile',
  LEARNING_RECORDS: 'poetry_learning_records',
  DAILY_STATS: 'poetry_daily_stats',
  SETTINGS: 'poetry_settings',
};

function init() {
  try { wx.getStorageInfoSync(); } catch (e) { console.error('[Storage] init failed:', e); }
}

// ==================== 用户档案 ====================
function getUserProfile() {
  try { return wx.getStorageSync(STORAGE_KEYS.USER_PROFILE) || null; } catch (e) { return null; }
}
function saveUserProfile(profile) {
  try { wx.setStorageSync(STORAGE_KEYS.USER_PROFILE, profile); return true; } catch (e) { return false; }
}

// ==================== 学习记录 ====================
function getLearningRecords() {
  try { return wx.getStorageSync(STORAGE_KEYS.LEARNING_RECORDS) || {}; } catch (e) { return {}; }
}
function getLearningRecord(poemId) {
  const records = getLearningRecords();
  return records[poemId] || null;
}
function saveLearningRecord(poemId, record) {
  const records = getLearningRecords();
  records[poemId] = Object.assign({}, records[poemId] || {}, record, { poemId: poemId });
  try { wx.setStorageSync(STORAGE_KEYS.LEARNING_RECORDS, records); return true; } catch (e) { return false; }
}
function removeLearningRecord(poemId) {
  const records = getLearningRecords();
  delete records[poemId];
  try { wx.setStorageSync(STORAGE_KEYS.LEARNING_RECORDS, records); return true; } catch (e) { return false; }
}
function getFavorites() {
  const records = getLearningRecords();
  return Object.values(records).filter(function(r) { return r.favorite; });
}
function toggleFavorite(poemId, poem) {
  var record = getLearningRecord(poemId);
  if (record) {
    record.favorite = !record.favorite;
    saveLearningRecord(poemId, record);
    return record.favorite;
  }
  saveLearningRecord(poemId, {
    poemId: poemId, title: poem.title || '', dynasty: poem.dynasty || '', author: poem.author || '',
    learnedAt: Date.now(), quizScore: 0, favorite: true, gamePlayed: 0
  });
  return true;
}
function markAsLearned(poemId, poem) {
  var record = getLearningRecord(poemId);
  if (record) { record.learnedAt = Date.now(); saveLearningRecord(poemId, record); return; }
  saveLearningRecord(poemId, {
    poemId: poemId, title: poem.title || '', dynasty: poem.dynasty || '', author: poem.author || '',
    learnedAt: Date.now(), quizScore: 0, favorite: false, gamePlayed: 0
  });
}
function getLearnedCount() {
  return Object.values(getLearningRecords()).filter(function(r) { return r.learnedAt; }).length;
}

// ==================== 每日统计 ====================
function getDailyStats() {
  try { return wx.getStorageSync(STORAGE_KEYS.DAILY_STATS) || {}; } catch (e) { return {}; }
}
function getDailyStat(date) {
  return getDailyStats()[date] || null;
}
function saveDailyStat(date, stat) {
  var stats = getDailyStats();
  stats[date] = Object.assign({}, stats[date] || {}, stat, { date: date });
  try { wx.setStorageSync(STORAGE_KEYS.DAILY_STATS, stats); return true; } catch (e) { return false; }
}
function getWeekStats() {
  var stats = getDailyStats();
  var result = [];
  var today = new Date();
  for (var i = 6; i >= 0; i--) {
    var d = new Date(today);
    d.setDate(d.getDate() - i);
    var key = d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0');
    result.push(stats[key] || { date: key, poemsLearned: 0, quizCompleted: 0, pointsEarned: 0, gamesPlayed: 0 });
  }
  return result;
}
function getTodayStat() {
  var d = new Date();
  var key = d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0');
  var stats = getDailyStats();
  return stats[key] || { date: key, poemsLearned: 0, quizCompleted: 0, pointsEarned: 0, gamesPlayed: 0 };
}

module.exports = {
  init: init,
  getUserProfile: getUserProfile, saveUserProfile: saveUserProfile,
  getLearningRecords: getLearningRecords, getLearningRecord: getLearningRecord,
  saveLearningRecord: saveLearningRecord, removeLearningRecord: removeLearningRecord,
  getFavorites: getFavorites, toggleFavorite: toggleFavorite,
  markAsLearned: markAsLearned, getLearnedCount: getLearnedCount,
  getDailyStats: getDailyStats, getDailyStat: getDailyStat,
  saveDailyStat: saveDailyStat, getWeekStats: getWeekStats, getTodayStat: getTodayStat,
};
